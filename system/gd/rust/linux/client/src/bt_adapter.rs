#[repr(u32)]
#[derive(Debug, Copy, Clone)]
pub enum BtDiscMode {
    // reference to system/stack/btm/neighbor_inquiry.h
    NonDiscoverable = 0,
    LimitedDiscoverable = 1,
    GeneralDiscoverable = 2,
}

impl From<BtDiscMode> for u32 {
    fn from(disc_mode: BtDiscMode) -> Self {
        disc_mode as u32
    }
}
