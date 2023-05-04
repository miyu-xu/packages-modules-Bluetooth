//! This module takes the set of attempts from the AttemptManager and determines
//! the target state of the LE manager.

use std::collections::HashSet;

use crate::core::address::AddressWithType;

use super::attempt_manager::{ConnectionAttempt, ConnectionMode};

/// This struct represents the target state of the LeManager based on the
/// set of all active connection attempts
pub struct TargetState {
    /// These addresses should go to the LE background connect list
    pub background_list: HashSet<AddressWithType>,
    /// These addresses should go to the direct list (we are not connected to any of them)
    pub direct_list: HashSet<AddressWithType>,
}

/// Takes a list of connection attempts, and determines the target state of the LE ACL manager
pub async fn determine_target_state(attempts: &[ConnectionAttempt]) -> TargetState {
    let background_list = attempts
        .iter()
        .filter(|attempt| attempt.mode == ConnectionMode::Background)
        .map(|attempt| attempt.remote_address)
        .collect();

    let direct_list = attempts
        .iter()
        .filter(|attempt| attempt.mode == ConnectionMode::Direct)
        .map(|attempt| attempt.remote_address)
        .collect();

    TargetState { background_list, direct_list }
}

#[cfg(test)]
mod test {
    use crate::{
        connection::ConnectionManagerClient,
        core::address::{AddressType, AddressWithType},
        utils::task::block_on_locally,
    };

    use super::*;

    const CLIENT: ConnectionManagerClient = ConnectionManagerClient::GattClient(1);

    const ADDRESS_1: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 6], address_type: AddressType::Public };
    const ADDRESS_2: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 6], address_type: AddressType::Random };
    const ADDRESS_3: AddressWithType =
        AddressWithType { address: [1, 2, 3, 4, 5, 7], address_type: AddressType::Random };

    #[test]
    fn test_determine_target_state() {
        let target = block_on_locally(determine_target_state(&[
            ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Background,
                remote_address: ADDRESS_1,
            },
            ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Background,
                remote_address: ADDRESS_1,
            },
            ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Background,
                remote_address: ADDRESS_2,
            },
            ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Direct,
                remote_address: ADDRESS_2,
            },
            ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Direct,
                remote_address: ADDRESS_3,
            },
        ]));

        assert_eq!(target.background_list.len(), 2);
        assert!(target.background_list.contains(&ADDRESS_1));
        assert!(target.background_list.contains(&ADDRESS_2));
        assert_eq!(target.direct_list.len(), 2);
        assert!(target.direct_list.contains(&ADDRESS_2));
        assert!(target.direct_list.contains(&ADDRESS_3));
    }
}
