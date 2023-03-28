//! This module takes all outstanding connection attempts, as well
//! as all current connections, and determines what the acceptlist + connection
//! parameters should be.

use std::{cmp::Ordering, collections::HashSet};

use crate::core::address::AddressWithType;

use super::{
    connection_attempt_manager::{ConnectionAttempt, ConnectionMode},
    state_machine::InitiatorStableState,
};

pub(super) fn get_target_parameters(
    active_connection_attempts: impl Iterator<Item = ConnectionAttempt>,
    current_connections: &HashSet<AddressWithType>,
) -> (InitiatorStableState, HashSet<AddressWithType>) {
    // first, find those attempts that are still relevant (i.e. that we have not connected to)
    let relevant_attempts = active_connection_attempts
        .filter(|attempt| !current_connections.contains(&attempt.remote_address))
        .collect::<Vec<_>>();
    // then, figure out the attempt with the strictest connection mode
    let max_mode =
        relevant_attempts.iter().map(|attempt| attempt.mode).max_by(|x, y| match (x, y) {
            (ConnectionMode::Direct, ConnectionMode::Direct) => Ordering::Equal,
            (ConnectionMode::Background, ConnectionMode::Background) => Ordering::Equal,
            (ConnectionMode::Direct, ConnectionMode::Background) => Ordering::Less,
            (ConnectionMode::Background, ConnectionMode::Direct) => Ordering::Greater,
        });
    // now convert this to an initiator state
    let target_state = match max_mode {
        Some(ConnectionMode::Direct) => InitiatorStableState::DirectInitiation,
        Some(ConnectionMode::Background) => InitiatorStableState::BackgroundInitiation,
        None => InitiatorStableState::Stopped,
    };
    // also extract the set of addresses to put in the accept list
    let addresses_to_connect_to =
        relevant_attempts.iter().map(|attempt| attempt.remote_address).collect();

    (target_state, addresses_to_connect_to)
}
