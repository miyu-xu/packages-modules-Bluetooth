use std::{
    cell::RefCell,
    collections::{hash_map::Entry, HashMap},
    future::Future,
};

use tokio::sync::oneshot;

use crate::core::{address::AddressWithType, shared_box::SharedBox};

use super::{
    CancelConnectFailure, Connection, ConnectionFailure, ConnectionManagerClient,
    CreateConnectionFailure,
};

#[derive(Debug, PartialEq, Eq, Clone, Copy, Hash)]
pub enum ConnectionMode {
    Background,
    Direct,
}

#[derive(Debug)]
struct ConnectionAttemptData {
    conn_tx: Option<oneshot::Sender<Connection>>,
}

#[derive(Debug, Hash, PartialEq, Eq, Clone, Copy)]
pub struct ConnectionAttempt {
    pub client: ConnectionManagerClient,
    pub mode: ConnectionMode,
    pub remote_address: AddressWithType,
}

#[derive(Debug)]
pub struct ConnectionAttempts {
    active_attempts: SharedBox<RefCell<HashMap<ConnectionAttempt, ConnectionAttemptData>>>,
}

impl ConnectionAttempts {
    pub fn new() -> Self {
        Self { active_attempts: SharedBox::new(RefCell::new(HashMap::new())) }
    }

    pub fn direct_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<impl Future<Output = Result<(), ConnectionFailure>>, CreateConnectionFailure> {
        let attempt =
            ConnectionAttempt { client, mode: ConnectionMode::Direct, remote_address: address };
        let mut all_attempts = self.active_attempts.borrow_mut();
        let Entry::Vacant(entry) = all_attempts.entry(attempt) else {
            return Err(CreateConnectionFailure::ConnectionAlreadyPending)
        };
        let (tx, rx) = oneshot::channel();
        entry.insert(ConnectionAttemptData { conn_tx: Some(tx) });
        drop(all_attempts);

        let attempts = self.active_attempts.downgrade();
        Ok(async move {
            let guard = scopeguard::guard((), |_| {
                attempts.with(|attempts| {
                    attempts.map(|attempts| {
                        attempts.borrow_mut().remove(&attempt);
                    })
                });
            });
            rx.await.map_err(|_| ConnectionFailure::Cancelled)?;
            // defuse guard
            scopeguard::ScopeGuard::into_inner(guard);
            Ok(())
        })
    }

    /// Cancel direct connection attempts from this client to the specified address.
    pub fn cancel_direct_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        let existing = self.active_attempts.borrow_mut().remove(&ConnectionAttempt {
            client,
            mode: ConnectionMode::Direct,
            remote_address: address,
        });

        if existing.is_some() {
            Ok(())
        } else {
            Err(CancelConnectFailure::ConnectionNotPending)
        }
    }

    /// Start a background connection to a peer device with given parameters from a specified client.
    pub fn add_background_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        let attempt =
            ConnectionAttempt { client, mode: ConnectionMode::Background, remote_address: address };
        let mut all_attempts = self.active_attempts.borrow_mut();
        let existing = all_attempts.insert(attempt, ConnectionAttemptData { conn_tx: None });
        drop(all_attempts);

        if existing.is_some() {
            Err(CreateConnectionFailure::ConnectionAlreadyPending)
        } else {
            Ok(())
        }
    }

    /// Cancel background connection attempts from this client to the specified address.
    pub fn remove_background_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        let existing = self.active_attempts.borrow_mut().remove(&ConnectionAttempt {
            client,
            mode: ConnectionMode::Background,
            remote_address: address,
        });

        if existing.is_some() {
            Ok(())
        } else {
            Err(CancelConnectFailure::ConnectionNotPending)
        }
    }

    pub fn active_attempts(&self) -> impl Iterator<Item = ConnectionAttempt> {
        self.active_attempts.borrow().keys().cloned().collect::<Vec<_>>().into_iter()
    }

    pub fn process_connection(&self, conn: Connection) -> Result<(), ()> {
        let mut active_attempts = self.active_attempts.borrow_mut();

        let interested_clients = active_attempts
            .keys()
            .filter(|attempt| attempt.remote_address == conn.remote_address)
            .copied()
            .collect::<Vec<_>>();

        if interested_clients.is_empty() {
            Err(())
        } else {
            for attempt in interested_clients {
                if attempt.mode == ConnectionMode::Direct {
                    // TODO(aryarahul): clean up these unwraps
                    let _ = active_attempts.remove(&attempt).unwrap().conn_tx.unwrap().send(conn);
                } else {
                    // TODO(aryarahul): inform background clients of the connection
                }
            }
            Ok(())
        }
    }
}
