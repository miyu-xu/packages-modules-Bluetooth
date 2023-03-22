use log::warn;

use crate::packets::{AttOpcode, AttView, AttWriteCommandView, Packet};

use super::att_database::AttDatabase;

/// This struct handles all ATT commands.
pub struct AttCommandHandler<Db: AttDatabase> {
    db: Db,
}

impl<Db: AttDatabase> AttCommandHandler<Db> {
    pub fn new(db: Db) -> Self {
        Self { db }
    }

    pub fn process_packet(&self, packet: AttView<'_>) {
        let snapshotted_db = self.db.snapshot();
        match packet.get_opcode() {
            AttOpcode::WRITE_COMMAND => {
                let Ok(packet) = AttWriteCommandView::try_parse(packet) else {
                  warn!("failed to parse WRITE_COMMAND packet");
                  return;
                };
                snapshotted_db
                    .write_no_response_attribute(packet.get_handle().into(), packet.get_value())
            }
            _ => {
                warn!("Dropping unsupported opcode {:?}", packet.get_opcode());
            }
        }
    }
}
