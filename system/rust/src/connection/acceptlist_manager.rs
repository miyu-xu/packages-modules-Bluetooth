//! This module takes the set of attempts from the AttemptManager, determines
//! the target state of the LE manager, and drives it to this target state

use std::collections::HashSet;

use futures::future::join_all;
use log::info;

use crate::core::address::AddressWithType;

use super::{
    attempt_manager::{ConnectionAttempt, ConnectionMode},
    le_manager::{AddressResolver, CanonicalAddress, LeAclManager},
    LeConnection,
};

/// This struct represents the target state of the LeManager based on the
/// set of all active connection attempts. Only canonical addresses can be
/// placed here. However, this module uses raw AddressWithTypes when interacting
/// with the LeAclManager, since we may need to remove an address that has ceased
/// to be canonical.
pub struct TargetState {
    /// These addresses should go to the LE background connect list
    pub background_list: HashSet<CanonicalAddress>,
    /// These addresses should go to the direct list (we are not connected to any of them)
    pub direct_list: HashSet<CanonicalAddress>,
}

/// Takes a list of connection attempts, and determines the target state of the LE ACL manager
pub async fn determine_target_state(
    attempts: &[ConnectionAttempt],
    resolver: &(impl AddressResolver + ?Sized),
    current_connections: impl Iterator<Item = LeConnection>,
) -> TargetState {
    let canonical_addr_current_connections: HashSet<CanonicalAddress> = HashSet::from_iter(
        join_all(
            current_connections
                .map(|conn| conn.remote_address)
                .map(|addr| resolver.resolve_address(addr)),
        )
        .await
        .into_iter(),
    );

    let background_list = join_all(
        attempts
            .iter()
            .filter(|attempt| attempt.mode == ConnectionMode::Background)
            .map(|attempt| attempt.remote_address)
            .map(|addr| resolver.resolve_address(addr)),
    )
    .await
    .into_iter()
    .filter(|addr| !canonical_addr_current_connections.contains(addr))
    .collect();

    let direct_list = join_all(
        attempts
            .iter()
            .filter(|attempt| attempt.mode == ConnectionMode::Direct)
            .map(|attempt| attempt.remote_address)
            .map(|addr| resolver.resolve_address(addr)),
    )
    .await
    .into_iter()
    .filter(|addr| !canonical_addr_current_connections.contains(addr))
    .collect();

    TargetState { background_list, direct_list }
}

/// This struct monitors the state of the LE connect list,
/// and drives it to the target state.
#[derive(Debug)]
pub struct LeAcceptlistManager {
    /// The connect list in the ACL manager
    direct_list: HashSet<AddressWithType>,
    /// The background connect list in the ACL manager
    background_list: HashSet<AddressWithType>,
    /// An interface into the LE ACL manager (le_impl.h)
    le_manager: Box<dyn LeAclManager>,
}

impl LeAcceptlistManager {
    /// Constructor
    pub fn new(le_manager: impl LeAclManager + 'static) -> Self {
        Self {
            direct_list: HashSet::new(),
            background_list: HashSet::new(),
            le_manager: Box::new(le_manager),
        }
    }

    /// The state of the LE connect list (as per le_impl.h) updates on a completed connection
    pub fn on_connect_complete(&mut self, address: AddressWithType) {
        if address == AddressWithType::EMPTY {
            return;
        }
        // le_impl pulls the device out of the direct connect list (but not the background list) on connection (regardless of status)
        self.direct_list.remove(&address);
    }

    /// Drive the state of the connect list to the target state
    pub fn drive_to_state(&mut self, target: TargetState) {
        let target_direct_list = target.direct_list.iter().map(CanonicalAddress::addr).collect();
        let target_background_list =
            target.background_list.iter().map(CanonicalAddress::addr).collect();

        // First, pull out anything in the ACL manager that we don't need
        // recall that cancel_connect() removes addresses from *both* lists (!)
        for address in self.direct_list.difference(&target_direct_list) {
            info!("Cancelling connection attempt to {address:?}");
            self.le_manager.remove_from_all_lists(*address);
            self.background_list.remove(address);
        }
        self.direct_list = self.direct_list.intersection(&target_direct_list).copied().collect();

        for address in self.background_list.difference(&target_background_list) {
            info!("Cancelling connection attempt to {address:?}");
            self.le_manager.remove_from_all_lists(*address);
            self.direct_list.remove(address);
        }
        self.background_list =
            self.background_list.intersection(&target_background_list).copied().collect();

        // now everything extra has been removed, we can put things back in
        for address in target_direct_list.difference(&self.direct_list) {
            info!("Starting direct connection to {address:?}");
            self.le_manager.add_to_direct_list(*address);
        }
        for address in target_background_list.difference(&self.background_list) {
            info!("Starting background connection to {address:?}");
            self.le_manager.add_to_background_list(*address);
        }

        // we should now be in a consistent state!
        self.direct_list = target_direct_list;
        self.background_list = target_background_list;
    }
}

#[cfg(test)]
mod test {
    use crate::{
        connection::{
            le_manager::ErrorCode,
            mocks::mock_le_manager::{MockActiveLeAclManager, MockLeAclManager},
            ConnectionManagerClient,
        },
        core::address::AddressType,
        utils::task::block_on_locally,
    };

    use super::*;

    const CLIENT: ConnectionManagerClient = ConnectionManagerClient::GattClient(1);

    const ADDRESS_1: CanonicalAddress = CanonicalAddress::new(AddressWithType {
        address: [1, 2, 3, 4, 5, 6],
        address_type: AddressType::Public,
    });
    const ADDRESS_2: CanonicalAddress = CanonicalAddress::new(AddressWithType {
        address: [1, 2, 3, 4, 5, 6],
        address_type: AddressType::Random,
    });
    const ADDRESS_3: CanonicalAddress = CanonicalAddress::new(AddressWithType {
        address: [1, 2, 3, 4, 5, 7],
        address_type: AddressType::Random,
    });

    #[test]
    fn test_determine_target_state() {
        let target = block_on_locally(determine_target_state(
            &[
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_1.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_1.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_2.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Direct,
                    remote_address: ADDRESS_2.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Direct,
                    remote_address: ADDRESS_3.addr(),
                },
            ],
            &MockLeAclManager::new(),
            [].into_iter(),
        ));

        assert_eq!(target.background_list.len(), 2);
        assert!(target.background_list.contains(&ADDRESS_1));
        assert!(target.background_list.contains(&ADDRESS_2));
        assert_eq!(target.direct_list.len(), 2);
        assert!(target.direct_list.contains(&ADDRESS_2));
        assert!(target.direct_list.contains(&ADDRESS_3));
    }

    #[test]
    fn test_add_to_direct_list() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: request a device to be present in the direct list
        manager.drive_to_state(TargetState {
            background_list: [].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // assert: that the device has been added
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
        assert_eq!(mock_le_manager.current_acceptlist().len(), 1);
        assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1.addr()));
    }

    #[test]
    fn test_add_to_background_list() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: request a device to be present in the direct list
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // assert: that the device has been added
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
        assert_eq!(mock_le_manager.current_acceptlist().len(), 1);
        assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1.addr()));
    }

    #[test]
    fn test_background_connection_upgrade_to_direct() {
        // arrange: a pending background connection
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // act: initiate a direct connection to the same device
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // assert: we are now doing a direct connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
    }

    #[test]
    fn test_direct_connection_cancel_while_background() {
        // arrange: a pending background connection
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // act: initiate a direct connection to the same device, then remove it
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // assert: we have returned to a background connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
    }

    #[test]
    fn test_direct_connection_cancel_then_resume_while_background() {
        // arrange: a pending background connection
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // act: initiate a direct connection to the same device, cancel it, then resume
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // assert: we have returned to a direct connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
    }

    #[test]
    fn test_remove_background_connection_then_add() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: add then remove a background connection
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });
        manager.drive_to_state(TargetState { background_list: [].into(), direct_list: [].into() });

        // assert: we have stopped our connection
        assert_eq!(mock_le_manager.current_connection_mode(), None);
    }

    #[test]
    fn test_background_connection_remove_then_add() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: add, remove, then re-add a background connection
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });
        manager.drive_to_state(TargetState { background_list: [].into(), direct_list: [].into() });
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });

        // assert: we resume our background connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Background));
    }
    #[test]
    fn test_retry_direct_connection_after_disconnect() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: initiate a direct connection
        manager.drive_to_state(TargetState {
            background_list: [].into(),
            direct_list: [ADDRESS_1].into(),
        });
        // act: the connection succeeds (and later disconnects)
        mock_le_manager.on_le_connect(ADDRESS_1.addr(), ErrorCode::SUCCESS);
        manager.on_connect_complete(ADDRESS_1.addr());
        // the peer later disconnects
        mock_le_manager.on_le_disconnect(ADDRESS_1.addr());
        // act: retry the direct connection
        manager.drive_to_state(TargetState {
            background_list: [].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // assert: we have resumed the direct connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
        assert_eq!(mock_le_manager.current_acceptlist().len(), 1);
        assert!(mock_le_manager.current_acceptlist().contains(&ADDRESS_1.addr()));
    }

    #[test]
    fn test_background_connection_remove_then_add_while_direct() {
        // arrange: a pending direct connection
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());
        manager.drive_to_state(TargetState {
            background_list: [].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // act: add, remove, then re-add a background connection
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });
        manager.drive_to_state(TargetState {
            background_list: [].into(),
            direct_list: [ADDRESS_1].into(),
        });
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [ADDRESS_1].into(),
        });

        // assert: we remain doing our direct connection
        assert_eq!(mock_le_manager.current_connection_mode(), Some(ConnectionMode::Direct));
    }

    #[test]
    fn test_remove_background_connection_after_disconnect() {
        // arrange
        let mock_le_manager = MockActiveLeAclManager::new();
        let mut manager = LeAcceptlistManager::new(mock_le_manager.clone());

        // act: initiate a background connection
        manager.drive_to_state(TargetState {
            background_list: [ADDRESS_1].into(),
            direct_list: [].into(),
        });
        // act: the connection succeeds
        mock_le_manager.on_le_connect(ADDRESS_1.addr(), ErrorCode::SUCCESS);
        manager.on_connect_complete(ADDRESS_1.addr());
        // act: we remove the background connection
        manager.drive_to_state(TargetState { background_list: [].into(), direct_list: [].into() });

        // assert: we have returned to idle
        assert_eq!(mock_le_manager.current_connection_mode(), None);
    }
}
