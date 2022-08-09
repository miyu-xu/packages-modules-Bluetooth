use std::{
    collections::{hash_map::Entry, HashMap},
    fmt::Debug,
    hash::Hash,
};

use anyhow::{anyhow, Context, Result};
use log::{error, warn};
use tokio::{
    select, spawn,
    sync::{
        mpsc::{channel, Receiver, Sender},
        oneshot,
    },
    task::JoinHandle,
};

use super::{
    nonce::{Nonce, NonceGenerator},
    owned_handle::OwnedHandle,
};

#[derive(Debug)]
enum ControlSignal<E: Debug, K: Debug + Copy> {
    Subscribe { key: K, reply_tx: oneshot::Sender<DemultiplexedReceiver<K, E>> },
    Unsubscribe { key: K, reply_tx: oneshot::Sender<()> },
    UnsubscribeWithNonce { key: K, nonce: Nonce },
}

/// A struct representing a subscription on a given key
struct SubscriptionSender<E> {
    /// The nonce is used so that, if a destructor runs after a key has been unregistered + reregistered,
    /// we don't accidentally unregister the *new* subscription
    nonce: Nonce,
    event_tx: Sender<E>,
}

async fn event_loop<E, K, C>(
    mut event_rx: Receiver<E>,
    mut selector: C,
    control_tx: Sender<ControlSignal<E, K>>,
    mut control_rx: Receiver<ControlSignal<E, K>>,
) where
    E: Send + Debug + 'static,
    K: Send + Eq + Copy + Hash + Debug + 'static,
    C: Send + FnMut(&E) -> K,
{
    let mut nonce_gen = NonceGenerator::new();
    let mut dispatch: HashMap<K, SubscriptionSender<E>> = HashMap::new();

    loop {
        select! {
            control_signal = control_rx.recv() => {
                match control_signal {
                    None => return,
                    Some(ControlSignal::Subscribe { key, reply_tx }) => {
                        let (dispatch_tx, dispatch_rx) = channel(16);
                        let nonce = nonce_gen.next();
                        let sender = SubscriptionSender { nonce, event_tx: dispatch_tx };
                        let entry = dispatch.entry(key);
                        if let Entry::Occupied(ref occupied_entry) = entry {
                            // we have an entry already recorded
                            // test if it is actually active
                            // note that TOCTOU means that it is possible for the sender to be closed
                            // after this check. But then we are being invoked in a racey manner
                            // so what we do is valid behavior.
                            if !occupied_entry.get().event_tx.is_closed() {
                                error!("attempt to register duplicate key {key:?} on demultiplexer");
                                drop(reply_tx); // explicitly fail to provide a reply
                                continue;
                            }
                            entry.and_modify(|x| *x =sender);
                        } else {
                            entry.or_insert(sender);
                        }
                        if reply_tx.send(DemultiplexedReceiver { key, control_tx: control_tx.clone(), event_rx: dispatch_rx, nonce }).is_err() {
                            warn!("registering caller hung up while subscribing - destructor will immediately run, so this is OK")
                        };
                    }
                    Some(ControlSignal::Unsubscribe { key, reply_tx }) => {
                        if dispatch.remove(&key).is_some() {
                            let _ = reply_tx.send(());
                        } else {
                            // explicitly fail to reply => failure
                            drop(reply_tx);
                        }
                    }
                    // this is only used by the destructor, so no need for a callback
                    Some(ControlSignal::UnsubscribeWithNonce { key, nonce }) => {
                        let entry = dispatch.entry(key);
                        if let Entry::Occupied(entry) = entry {
                            if entry.get().nonce == nonce {
                                entry.remove();
                            }
                        }
                    }
                }
            }
            event = event_rx.recv() => {
                match event {
                    None => {
                        warn!("event source has shut down while demultiplexer is running, shutting down demultiplexer");
                        return;
                    }
                    Some(event) => {
                        let key = selector(&event);
                        let rx = dispatch.get(&key);
                        match rx {
                            None => {
                                error!("demultiplexer received event mapping to key {key:?}, dropping it");
                                continue;
                            }
                            Some(rx) => {
                                // TODO: what happens if there is backpressure on a demultiplexer output?
                                let _ = rx.event_tx.send(event).await;
                            }
                        }
                    }
                }
            }
        }
    }
}

#[derive(Debug)]
pub struct Demultiplexer<E: Debug, K: Debug + Copy> {
    control_tx: Sender<ControlSignal<E, K>>,
    pub event_tx: Sender<E>,
    task_handle: OwnedHandle<()>,
}

impl<E, K> Demultiplexer<E, K>
where
    E: Send + Debug + 'static,
    K: Send + Eq + Copy + Hash + Debug + 'static,
{
    pub fn new<C>(selector: C) -> Demultiplexer<E, K>
    where
        C: Send + FnMut(&E) -> K + 'static,
    {
        let (control_tx, control_rx) = channel(4);
        let (event_tx, event_rx) = channel(16);
        let task_handle =
            OwnedHandle::new(spawn(event_loop(event_rx, selector, control_tx.clone(), control_rx)));
        Demultiplexer { control_tx, event_tx, task_handle }
    }

    pub async fn send(&self, data: E) -> Result<()> {
        self.event_tx.send(data).await.map_err(|_| {
            anyhow!(
                "demultiplexer internal error: somehow the worker task stopped accepting messages"
            )
        })
    }

    pub async fn subscribe(&self, key: K) -> Result<DemultiplexedReceiver<K, E>> {
        let (reply_tx, reply_rx) = oneshot::channel();
        self.control_tx
            .send(ControlSignal::Subscribe { key, reply_tx })
            .await
            .map_err(|_| anyhow!("demultiplexer has shut down, unable to subscribe"))?;
        reply_rx.await.with_context(|| format!("demultiplexer failed to register key {key:?}"))
    }

    pub async fn unsubscribe(&self, key: K) -> Result<()> {
        let (reply_tx, reply_rx) = oneshot::channel();
        self.control_tx
            .send(ControlSignal::Unsubscribe { key, reply_tx })
            .await
            .map_err(|_| anyhow!("demultiplexer has shut down, unable to unsubscribe"))?;
        reply_rx
            .await
            .with_context(|| format!("demultiplexer failed to unsubscribe from key {key:?}"))
    }
}

#[derive(Debug)]
pub struct DemultiplexedReceiver<K: Debug + Copy, E: Debug> {
    key: K,
    nonce: Nonce,
    control_tx: Sender<ControlSignal<E, K>>,
    event_rx: Receiver<E>,
}

impl<K: Debug + Copy, E: Debug> DemultiplexedReceiver<K, E> {
    pub async fn recv(&mut self) -> Option<E> {
        self.event_rx.recv().await
    }
}

impl<K: Debug + Copy, E: Debug> Drop for DemultiplexedReceiver<K, E> {
    fn drop(&mut self) {
        if let Err(err) = self
            .control_tx
            .try_send(ControlSignal::UnsubscribeWithNonce { key: self.key, nonce: self.nonce })
        {
            error!("failed to drop demultiplexed receiver {self:?} with error {err:?}");
        }
    }
}
