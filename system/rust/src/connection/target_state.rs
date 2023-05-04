//! This module takes the set of attempts from the AttemptManager and determines
//! the target state of the LE manager.

use std::collections::HashSet;

use futures::future::join_all;

use super::{
    attempt_manager::{ConnectionAttempt, ConnectionMode},
    le_manager::{AddressResolver, CanonicalAddress},
    LeConnection,
};

/// This struct represents the target state of the LeManager based on the
/// set of all active connection attempts. Only canonical addresses can be
/// placed here.
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

    let direct_list: HashSet<_> = join_all(
        attempts
            .iter()
            .filter(|attempt| attempt.mode == ConnectionMode::Direct)
            .map(|attempt| attempt.remote_address)
            .map(|addr| resolver.resolve_address(addr)),
    )
    .await
    .into_iter()
    .collect();

    assert_eq!(
        direct_list.intersection(&canonical_addr_current_connections).next(),
        None,
        "We should never have a direct connection attempt to a device we are already connected to"
    );

    TargetState { background_list, direct_list }
}

#[cfg(test)]
mod test {
    use crate::{
        connection::{mocks::mock_address_resolver::MockAddressResolver, ConnectionManagerClient},
        core::address::{AddressType, AddressWithType},
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
    const ADDRESS_4: CanonicalAddress = CanonicalAddress::new(AddressWithType {
        address: [1, 2, 3, 4, 5, 8],
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
            &MockAddressResolver::new(),
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
    fn test_target_state_resolve_rpas() {
        // arrange: associate an RPA with an identity address
        let resolver: MockAddressResolver = MockAddressResolver::new();
        resolver.associate_address(ADDRESS_2, ADDRESS_1.addr());

        // act: reconcile connection attempts of both types to the RPA
        let target = block_on_locally(determine_target_state(
            &[
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Direct,
                    remote_address: ADDRESS_1.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_1.addr(),
                },
            ],
            &resolver,
            [].into_iter(),
        ));

        // assert: we resolved the RPAs to the canonical address
        assert_eq!(target.background_list.len(), 1);
        assert!(target.background_list.contains(&ADDRESS_2));
        assert_eq!(target.direct_list.len(), 1);
        assert!(target.direct_list.contains(&ADDRESS_2));
    }

    #[test]
    #[should_panic]
    fn test_target_state_with_direct_connection_to_connected_device() {
        // arrange: one bonded device
        let resolver: MockAddressResolver = MockAddressResolver::new();
        resolver.set_address_equivalences(
            [(ADDRESS_3, [ADDRESS_1.addr(), ADDRESS_2.addr()].into_iter().collect())]
                .into_iter()
                .collect(),
        );

        // act: let us be connected to ADDRESS_1, and making a direct connection to ADDRESS_2
        block_on_locally(determine_target_state(
            &[ConnectionAttempt {
                client: CLIENT,
                mode: ConnectionMode::Direct,
                remote_address: ADDRESS_2.addr(),
            }],
            &resolver,
            [LeConnection { remote_address: ADDRESS_1.addr() }].into_iter(),
        ));

        // assert: we should panic as this is an invalid scenario
    }

    #[test]
    fn test_target_state_with_background_connection_to_connected_device() {
        // arrange: one bonded device
        let resolver: MockAddressResolver = MockAddressResolver::new();
        resolver.set_address_equivalences(
            [(ADDRESS_3, [ADDRESS_1.addr(), ADDRESS_2.addr()].into_iter().collect())]
                .into_iter()
                .collect(),
        );

        // act: let us be connected to ADDRESS_1, and making background connections to ADDRESS_2 and ADDRESS_4
        let target = block_on_locally(determine_target_state(
            &[
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_2.addr(),
                },
                ConnectionAttempt {
                    client: CLIENT,
                    mode: ConnectionMode::Background,
                    remote_address: ADDRESS_4.addr(),
                },
            ],
            &resolver,
            [LeConnection { remote_address: ADDRESS_1.addr() }].into_iter(),
        ));

        // assert: only ADDRESS_4 is retained, since we are already connected to the device associated with ADDRESS_2
        assert_eq!(target.background_list.len(), 1);
        assert!(target.background_list.contains(&ADDRESS_4));
    }
}
