#[derive(Copy, Clone, Hash, Eq, PartialEq, Debug)]
#[repr(C)]
pub struct RawAddress {
    address: [u8; 6],
}

/// either a BR/EDR address, or the identity address of an LE device (guaranteed to be the same on dual-mode)
#[derive(Copy, Clone, Hash, Eq, PartialEq, Debug)]
pub struct IdentityAddress(pub RawAddress);

/// see go/bluetooth-address for details
pub enum Address {
    Identity(IdentityAddress),
    /// RPAs are <TODO>
    ResolvablePrivate(RawAddress),
    NonResolvablePrivate(RawAddress),
}

#[derive(Copy, Clone, Hash, Eq, PartialEq, Debug)]
pub struct L2capPsm {
    pub psm: u16,
}

#[derive(Copy, Clone, Hash, Eq, PartialEq, Debug)]
pub struct L2capChannelId {
    pub cid: u16,
}
