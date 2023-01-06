//! This module handles "arbitration" of ATT packets, to determine whether they should be handled by the primary stack
//! or by the "Private GATT" stack

use crate::packets::{AttOpcode, OwnedAttView};

/// Test to see if a buffer contains a valid ATT packet with an opcode we are interested in intercepting
pub fn try_parse_att_server_packet(packet: Box<[u8]>) -> Option<OwnedAttView> {
    let att = OwnedAttView::try_parse(packet).ok()?;
    match att.view().get_opcode() {
        AttOpcode::FIND_INFORMATION_REQUEST
        | AttOpcode::FIND_BY_TYPE_VALUE_REQUEST
        | AttOpcode::READ_BY_TYPE_REQUEST
        | AttOpcode::READ_REQUEST
        | AttOpcode::READ_BLOB_REQUEST
        | AttOpcode::READ_MULTIPLE_REQUEST
        | AttOpcode::READ_BY_GROUP_TYPE_REQUEST => Some(att),
        _ => None,
    }
}
