use std::{
    collections::{hash_map::Entry, HashMap},
    fmt::Debug,
    hash::Hash,
    ops::{Deref, DerefMut},
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

enum ControlSignal<E, K> {
    Subscribe { key: K, event_tx: Sender<E>, reply_tx: oneshot::Sender<()> },
}

async fn event_loop<E, K, C>(
    mut event_rx: Receiver<E>,
    mut selector: C,
    mut control_rx: Receiver<ControlSignal<E, K>>,
) where
    E: Send + 'static,
    K: Send + Eq + Copy + Hash + Debug + 'static,
    C: Send + FnMut(&E) -> K + 'static,
{
    let mut dispatch: HashMap<K, Sender<E>> = HashMap::new();
    loop {
        select! {
            control_signal = control_rx.recv() => {
                match control_signal {
                    None => return,
                    Some(ControlSignal::Subscribe { key, event_tx, reply_tx }) => {
                        let (dispatch_tx, dispatch_rx) = channel(16);
                        let entry = dispatch.entry(key);
                        if let Entry::Occupied(ref occupied_entry) = entry {
                            // we have an entry already recorded
                            // test if it is actually active
                            // note that TOCTOU means that it is possible for the sender to be closed
                            // after this check. But then we are being invoked in a racey manner
                            // so what we do is valid behavior.
                            if (!occupied_entry.get().is_closed()) {
                                error!("attempt to register duplicate key {key:?} on demultiplexer");
                                drop(reply_tx); // explicitly fail to provide a reply
                                continue;
                            }
                            entry.and_modify(|x| *x = dispatch_tx);
                        } else {
                            entry.or_insert(dispatch_tx);
                        }
                        if let Err(_) = reply_tx.send(()) {
                            warn!("registering caller hung up while subscribing")
                        };
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
                                rx.send(event).await;
                            }
                        }
                    }
                }
            }
        }
    }
}

pub struct Demultiplexer<E, K> {
    control_tx: Sender<ControlSignal<E, K>>,
    task_handle: JoinHandle<()>,
}

impl<E, K> Demultiplexer<E, K>
where
    E: Send + 'static,
    K: Send + Eq + Copy + Hash + Debug + 'static,
{
    pub fn new<C>(event_rx: Receiver<E>, selector: C) -> Demultiplexer<E, K>
    where
        C: Send + FnMut(&E) -> K + 'static,
    {
        let (control_tx, control_rx) = channel(1);
        let task_handle = spawn(event_loop(event_rx, selector, control_rx));
        Demultiplexer { control_tx, task_handle }
    }

    pub async fn subscribe(&self, key: K) -> Result<Receiver<E>> {
        let (reply_tx, reply_rx) = oneshot::channel();
        let (event_tx, event_rx) = channel(16);
        self.control_tx
            .clone()
            .send(ControlSignal::Subscribe { key, event_tx, reply_tx })
            .await
            .map_err(|_| anyhow!("demultiplexer has shut down, unable to subscribe"))?;
        reply_rx.await.with_context(|| format!("demultiplexer failed to register key {key:?}"))?;
        Ok(event_rx)
    }
}

impl<E, K> Drop for Demultiplexer<E, K> {
    fn drop(&mut self) {
        // stop task loop to release handle on incoming stream
        self.task_handle.abort();
    }
}
