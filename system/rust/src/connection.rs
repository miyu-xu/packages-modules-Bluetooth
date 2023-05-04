//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{
    collections::HashSet,
    fmt::Debug,
    future::Future,
    hash::Hash,
    ops::{Deref, DerefMut},
    rc::{Rc, Weak},
    time::Duration,
};

use crate::{core::address::AddressWithType, gatt::ids::ServerId};

use self::{
    acceptlist_manager::LeAcceptlistManager,
    attempt_manager::{ConnectionAttempts, ConnectionMode, LastSeen},
    le_manager::{
        AddressResolver, ErrorCode, InactiveLeAclManager, LeAclManagerConnectionCallbacks,
    },
    target_state::determine_target_state, le_scanner::{LeScanner, LeScannerFilterControls, LeScanningManagerCallbacks},
};

mod acceptlist_manager;
mod attempt_manager;
mod ffi;
pub mod le_manager;
mod mocks;
pub mod target_state;
mod le_scanner;

pub use ffi::{
    register_callbacks, AddressResolverImpl, AddressResolverShim, LeAclManagerImpl,
    LeAclManagerShim,
};
use log::info;
use scopeguard::ScopeGuard;
use tokio::{sync::Mutex, task::spawn_local, time::timeout};

/// Possible errors returned when making a connection attempt
#[derive(Debug, PartialEq, Eq)]
pub enum CreateConnectionFailure {
    /// This client is already making a connection of the same type
    /// to the same address.
    ConnectionAlreadyPending,
}

/// Errors returned if a connection successfully starts but fails afterwards.
#[derive(Debug, PartialEq, Eq)]
pub enum ConnectionFailure {
    /// The connection attempt was cancelled
    Cancelled,
    /// The connection completed but with an HCI error code
    Error(ErrorCode),
}

/// Errors returned if the client fails to cancel their connection attempt
#[derive(Debug, PartialEq, Eq)]
pub enum CancelConnectFailure {
    /// The connection attempt does not exist
    ConnectionNotPending,
}

/// Unique identifiers for a client of the connection manager
#[derive(Debug, Hash, PartialEq, Eq, Clone, Copy)]
pub enum ConnectionManagerClient {
    /// A GATT client with given client ID
    GattClient(u8),
    /// A GATT server with given server ID
    GattServer(ServerId),
}

/// An active connection
#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub struct LeConnection {
    /// The address of the peer device, as reported in the connection complete event
    /// This is guaranteed to be unique across active connections, so we can implement
    /// PartialEq/Eq on this.
    pub remote_address: AddressWithType,
}

/// Responsible for managing the initiator state and the list of
/// devices on the filter accept list
#[derive(Debug)]
pub struct ConnectionManager {
    /// Internal state
    state: Mutex<ConnectionManagerState>,
    /// Lets us resolve addresses corresponding to the same peer device
    address_resolver: Box<dyn AddressResolver>,
}

#[derive(Debug)]
struct ConnectionManagerState {
    /// All pending connection attempts (unresolved direct + all background)
    attempts: ConnectionAttempts,
    /// The addresses we are currently connected to
    current_connections: HashSet<LeConnection>,
    /// Tracks the state of the LE connect list, and updates it to drive to a
    /// specified target state
    acceptlist_manager: LeAcceptlistManager,
    /// Lets us control scan filters and receive results
    scanner: Box<dyn LeScannerFilterControls>,
}

struct ConnectionManagerCallbackHandler(Weak<ConnectionManager>);

const DIRECT_CONNECTION_TIMEOUT: Duration = Duration::from_secs(
    29, /* ugly hack to avoid fighting with le_impl timeout, until I remove that timeout */
);

impl LeAclManagerConnectionCallbacks for ConnectionManagerCallbackHandler {
    fn on_le_connect(&self, address: AddressWithType, result: Result<LeConnection, ErrorCode>) {
        self.send_to_manager(
            move |manager| async move { manager.on_le_connect(address, result).await },
        );
    }

    fn on_disconnect(&self, address: AddressWithType) {
        self.send_to_manager(move |manager| async move { manager.on_disconnect(address).await });
    }

    fn on_resolving_list_change(&self) {
        self.send_to_manager(
            move |manager| async move { manager.on_resolving_list_change().await },
        );
    }
}

impl LeScanningManagerCallbacks for ConnectionManagerCallbackHandler {
    fn on_targeted_announcement_scan_result(&self, address: AddressWithType) {
        self.send_to_manager(move |manager| async move { manager.on_targeted_announcement_scan_result(address).await });
    }
}

impl ConnectionManagerCallbackHandler {
    fn send_to_manager<F>(&self, f: impl FnOnce(Rc<ConnectionManager>) -> F + 'static)
    where
        F: Future,
    {
        self.0.upgrade().map(|manager| {
            spawn_local(async move {
                f(manager.clone()).await;
            })
        });
    }
}

impl ConnectionManager {
    /// Constructor
    pub fn new(
        le_manager: impl InactiveLeAclManager,
        address_resolver: impl AddressResolver + Clone + 'static,
        scanner: impl LeScanner + 'static,
    ) -> Rc<Self> {
        Rc::new_cyclic(|weak| {
            scanner.register_callbacks(ConnectionManagerCallbackHandler(weak.clone())).expect("Failed to register callbacks from connection manager to LE scanner");
            Self {
                state: Mutex::new(ConnectionManagerState {
                    attempts: ConnectionAttempts::new(),
                    current_connections: HashSet::new(),
                    acceptlist_manager: LeAcceptlistManager::new(
                        le_manager.register_callbacks(ConnectionManagerCallbackHandler(weak.clone())),
                    ),
                    scanner,
                }),
                address_resolver: Box::new(address_resolver),
            }})
    }

    /// Start a direct connection to a peer device from a specified client. If the peer
    /// is connected, immediately resolve the attempt.
    pub async fn start_direct_connection(
        self: &Rc<Self>,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        spawn_local(timeout(
            DIRECT_CONNECTION_TIMEOUT,
            self.direct_connection(client, address).await?,
        ));
        Ok(())
    }

    /// Test whether we are connected to a particular peer device
    async fn is_connected(
        &self,
        address: AddressWithType,
        state: &mut ConnectionManagerState,
    ) -> bool {
        let canonical = self.address_resolver.resolve_address(address).await;
        for connection in &state.current_connections {
            if canonical == self.address_resolver.resolve_address(connection.remote_address).await {
                return true;
            }
        }
        false
    }

    /// Start a direct connection to a peer device from a specified client.
    ///
    /// # Cancellation Safety
    /// If this future is dropped, the connection attempt will be cancelled. It can also be cancelled
    /// from the separate API ConnectionManager#cancel_connection.
    async fn direct_connection(
        self: &Rc<Self>,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<
        impl Future<Output = Result<LeConnection, ConnectionFailure>>,
        CreateConnectionFailure,
    > {
        info!("Client {client:?} starting direct connection to {address:?}");
        let mut state = self.state.lock().await;

        // if connected, this is a no-op
        let attempt_and_guard = if self.is_connected(address, state.deref_mut()).await {
            None
        } else {
            let pending_attempt = state.attempts.register_direct_connection(client, address)?;
            let attempt_id = pending_attempt.id;
            self.reconcile_state(&mut state).await;
            Some((
                pending_attempt,
                scopeguard::guard(self.clone(), move |this| {
                    spawn_local(async move {
                        // remove the attempt after we are cancelled
                        info!("Cancelling connection attempt {attempt_id:?} to {address:?}");
                        let mut state = this.state.lock().await;
                        state.attempts.cancel_attempt_with_id(attempt_id);
                        this.reconcile_state(&mut state).await;
                    });
                }),
            ))
        };

        Ok(async move {
            let Some((attempt, guard)) = attempt_and_guard else {
                info!("Already connected to {address:?}");
                // if we did not make an attempt, the connection must be ready
                return Ok(LeConnection { remote_address: address })
            };
            // otherwise, wait until the attempt resolves
            let ret = attempt.await;
            // defuse scopeguard (no need to cancel now)
            ScopeGuard::into_inner(guard);
            ret
        })
    }

    /// Start a background connection to a peer device with given parameters from a specified client.
    pub async fn add_background_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        info!("Client {client:?} starting background connection to {address:?}");
        let mut state = self.state.lock().await;
        state.attempts.register_background_connection(client, address)?;
        self.reconcile_state(&mut state).await;
        Ok(())
    }

    /// Cancel connection attempt from this client to the specified address with the specified mode.
    pub async fn cancel_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
        mode: ConnectionMode,
    ) -> Result<(), CancelConnectFailure> {
        info!("Client {client:?} cancelling connection attempt to {address:?} (mode={mode:?})");
        let mut state = self.state.lock().await;
        state.attempts.cancel_attempt(client, address, mode)?;
        self.reconcile_state(&mut state).await;
        Ok(())
    }

    /// Cancel all connection attempts to this address
    pub async fn cancel_unconditionally(&self, address: AddressWithType) {
        info!("Cancelling all connection attempts to {address:?}");
        let mut state = self.state.lock().await;
        state.attempts.remove_unconditionally(address);
        self.reconcile_state(&mut state).await;
    }

    /// Cancel all connection attempts from this client
    pub async fn remove_client(&self, client: ConnectionManagerClient) {
        info!("Removing all connection attempts from {client:?}");
        let mut state = self.state.lock().await;
        state.attempts.remove_client(client);
        self.reconcile_state(&mut state).await;
    }

    async fn on_le_connect(
        &self,
        address: AddressWithType,
        result: Result<LeConnection, ErrorCode>,
    ) {
        let mut state = self.state.lock().await;
        // record this connection while it exists
        if let Ok(connection) = result {
            state.current_connections.insert(connection);
        }
        // all completed connections remove the address from the direct list
        state.acceptlist_manager.on_connect_complete(address);
        // figure out what the canonical address is
        let canonical_address = self.address_resolver.resolve_address(address).await;
        // invoke any pending callbacks, update set of attempts
        state
            .attempts
            .process_connection(canonical_address, self.address_resolver.as_ref(), result)
            .await;
        // update the acceptlist
        self.reconcile_state(&mut state).await;
    }

    async fn on_disconnect(&self, address: AddressWithType) {
        let mut state = self.state.lock().await;
        state.current_connections.retain(|conn| conn.remote_address != address);
        self.reconcile_state(&mut state).await;
    }

    async fn on_resolving_list_change(&self) {
        let mut state = self.state.lock().await;
        let state = state.deref_mut();
        // Figure out if some of our connection attempts actually match existing connections,
        // by re-processing all existing connections
        for connection in &state.current_connections {
            let canonical_address =
                self.address_resolver.resolve_address(connection.remote_address).await;
            state
                .attempts
                .process_connection(
                    canonical_address,
                    self.address_resolver.deref(),
                    Ok(*connection),
                )
                .await;
        }
        // Then, ensure that we are using canonical addresses in the connect list
        self.reconcile_state(state).await;
    }

    async fn on_targeted_announcement_scan_result(self: &Rc<Self>, address: AddressWithType) {
        let mut state = self.state.lock().await;
        let canonical_address = self.address_resolver.resolve_address(address).await;
        state.attempts.process_targeted_announcement_scan_result(canonical_address, self.address_resolver.deref()).await;
        // the scan result will go stale after a timeout, so schedule a state update afterwards to check
        // if it has indeed become stale
        let this = self.clone();
        spawn_local(async move {
            tokio::time::sleep(LastSeen::DURATION).await;
            this.reconcile_state(this.state.lock().await.deref_mut()).await;
        });
    }

    /// Make the state of the LeAcceptlistManager consistent with the attempts tracked in ConnectionAttempts
    async fn reconcile_state(&self, state: &mut ConnectionManagerState) {
        // determine the target state
        let target = determine_target_state(
            &state.attempts.active_attempts(),
            self.address_resolver.as_ref(),
            state.current_connections.iter().copied(),
        )
        .await;

        // drive the ACL + scanning managers to this target state
        state.acceptlist_manager.drive_to_state(target.target_acceptlist_state);
        state.scanner.set_targeted_announcement_filter_enabled(target.scan_for_targeted_announcements)
    }
}

#[cfg(test)]
mod test {
    use crate::{
        connection::le_manager::CanonicalAddress, core::address::AddressType,
        utils::task::block_on_locally,
    };

    use super::{mocks::mock_le_manager::MockLeAclManager, *};

    const CLIENT_1: ConnectionManagerClient = ConnectionManagerClient::GattClient(1);
    const CLIENT_2: ConnectionManagerClient = ConnectionManagerClient::GattClient(2);

    const ADDRESS_1: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 6], address_type: AddressType::Public };
    const ADDRESS_2: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 7], address_type: AddressType::Public };
    const ADDRESS_3: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 8], address_type: AddressType::Public };

    const ERROR: ErrorCode = ErrorCode(1);

    async fn handle_events() {
        tokio::time::sleep(Duration::from_secs(1)).await;
    }

    #[test]
    fn test_single_direct_connection() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a direct connection
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // assert: the direct connection is pending
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
            assert_eq!(mock_le_manager.current_acceptlist().len(), 1);
            assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1));
        });
    }

    #[test]
    fn test_failed_direct_connection() {
        block_on_locally(async {
            // arrange: one pending direct connection
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // act: the connection attempt fails
            mock_le_manager.on_le_connect(ADDRESS_1, ERROR);

            // assert: the direct connection has stopped
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_single_background_connection() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a background connection
            connection_manager.add_background_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // assert: the background connection is pending
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
            assert_eq!(mock_le_manager.current_acceptlist().len(), 1);
            assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1));
        });
    }

    #[test]
    fn test_resolved_connection() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a direct connection, that succeeds
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);

            // assert: no connection is pending
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_resolved_background_connection() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a background connection, that succeeds
            connection_manager
                .as_ref()
                .add_background_connection(CLIENT_1, ADDRESS_1)
                .await
                .unwrap();
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);

            // assert: no connection is pending
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_resolved_direct_connection_after_disconnect() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a direct connection, that succeeds, then disconnects
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            mock_le_manager.on_le_disconnect(ADDRESS_1);

            // assert: no connection is pending
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_resolved_background_connection_after_disconnect() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a background connection, that succeeds, then disconnects
            connection_manager.add_background_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            mock_le_manager.on_le_disconnect(ADDRESS_1);

            // assert: the background connection has resumed
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
        });
    }

    #[test]
    fn test_background_connection_after_failing() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: initiate a background connection that fails
            connection_manager.add_background_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode(1));

            // assert: the background connection has not stopped
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
        });
    }

    #[test]
    fn test_direct_connection_timeout() {
        block_on_locally(async {
            // arrange: a pending direct connection
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // act: let it timeout
            tokio::time::sleep(DIRECT_CONNECTION_TIMEOUT).await;
            // go forward one tick to ensure all timers are fired
            // (since we are using fake time, this is not a race condition)
            tokio::time::sleep(Duration::from_millis(1)).await;

            // assert: it is cancelled and we are idle again
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_stacked_direct_connections_timeout() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: start a direct connection
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            tokio::time::sleep(DIRECT_CONNECTION_TIMEOUT * 3 / 4).await;
            // act: after some time, start a second one
            connection_manager.start_direct_connection(CLIENT_2, ADDRESS_1).await.unwrap();
            // act: wait for the first one (but not the second) to time out
            tokio::time::sleep(DIRECT_CONNECTION_TIMEOUT * 3 / 4).await;

            // assert: we are still doing a direct connection
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
        });
    }

    #[test]
    fn test_direct_connection_to_already_connected() {
        block_on_locally(async {
            // arrange: an existing connection
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            handle_events().await;

            // act: start a direct connection to the same address
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // assert: we don't do anything with the accept list
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_background_connection_to_connected_then_bonded() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: connect
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            // act: bond, so the canonical address is ADDRESS_2 but ADDRESS_1 is a valid RPA
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });

            handle_events().await;

            // act: add the canonical address to the background connect list
            connection_manager.add_background_connection(CLIENT_1, ADDRESS_2).await.unwrap();

            // assert: we don't do anything with the accept list
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_background_connection_to_connected_bonded_then_disconnected() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());

            // act: connect
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            // act: bond, so the canonical address is ADDRESS_2 but ADDRESS_1 is a valid RPA
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });

            handle_events().await;

            // act: add the canonical address to the background connect list
            connection_manager.add_background_connection(CLIENT_1, ADDRESS_2).await.unwrap();
            // act: then disconnect
            mock_le_manager.on_le_disconnect(ADDRESS_1);
            handle_events().await;

            // assert: we have added only the canonical address to the background connect list
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
            assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_2));
            assert!(!mock_le_manager.current_acceptlist().contains(&ADDRESS_1));
        });
    }

    #[test]
    fn test_connect_to_bonded_using_rpa() {
        block_on_locally(async {
            // arrange: a bonded device
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });

            handle_events().await;

            // act: start a connection attempt to an RPA
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // assert: we have added only the canonical address to the connect list
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
            assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_2));
            assert!(!mock_le_manager.current_acceptlist().contains(&ADDRESS_1));
        });
    }

    #[test]
    fn test_successful_connect_to_bonded_using_rpa() {
        block_on_locally(async {
            // arrange: a bonded device
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });

            handle_events().await;

            // act: start a connection attempt to an RPA
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            // act: get connection completion
            mock_le_manager.on_le_connect(ADDRESS_2, ErrorCode::SUCCESS);
            handle_events().await;

            // assert: we have finished initiating
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_failed_connect_to_bonded_using_rpa() {
        block_on_locally(async {
            // arrange: a bonded device
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });
            handle_events().await;

            // act: start a connection attempt to an RPA
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            // act: get connection completion with failed error code
            mock_le_manager.on_le_connect(ADDRESS_2, ErrorCode(1));
            handle_events().await;
            // act: try a reconnection
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();

            // assert: we retry the connection
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
        });
    }

    #[test]
    fn test_connect_to_bonded_using_rpa_then_unbond() {
        block_on_locally(async {
            // arrange: a bonded device
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });

            handle_events().await;

            // act: start a connection attempt to an RPA
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_1).await.unwrap();
            // act: then unbond
            mock_le_manager.with_resolver(|r| r.clear_address(CanonicalAddress::new(ADDRESS_2)));
            handle_events().await;

            // assert: we have placed the RPA in the connect list
            assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
            assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1));
        });
    }

    #[test]
    fn test_bond_to_already_connected() {
        block_on_locally(async {
            // arrange
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            // arrange: an existing connection
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            // arrange: a connection attempt to a different address
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_2).await.unwrap();
            handle_events().await;

            // act: bond, so we realize that these addresses are the same
            mock_le_manager.with_resolver(|r| {
                r.associate_address(CanonicalAddress::new(ADDRESS_2), ADDRESS_1)
            });
            handle_events().await;

            // assert: we are no longer initiating any connection
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }

    #[test]
    fn test_direct_connection_to_already_connected_with_different_address() {
        block_on_locally(async {
            // arrange: a bonded device
            let mock_le_manager = MockLeAclManager::new();
            let connection_manager =
                ConnectionManager::new(mock_le_manager.clone(), mock_le_manager.resolver());
            mock_le_manager.with_resolver(|r| {
                r.set_address_equivalences(
                    [(
                        CanonicalAddress::new(ADDRESS_3),
                        [ADDRESS_1, ADDRESS_2].into_iter().collect(),
                    )]
                    .into_iter()
                    .collect(),
                )
            });
            handle_events().await;

            // act: connect using one RPA
            mock_le_manager.on_le_connect(ADDRESS_1, ErrorCode::SUCCESS);
            handle_events().await;
            // act: try to connect to the second RPA
            connection_manager.start_direct_connection(CLIENT_1, ADDRESS_2).await.unwrap();

            // assert: we don't do anything with the accept list
            assert_eq!(mock_le_manager.current_connection_mode(), None);
        });
    }
}
