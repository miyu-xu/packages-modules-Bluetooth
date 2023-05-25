//! An address with type (public / random)

use std::str::FromStr;

use macaddr::MacAddr6;

#[derive(Copy, Clone, Debug, Hash, PartialEq, Eq)]
#[repr(u8)]
/// The type of an LE address (see: 5.3 Vol 6B 1.3 Device Axddress)
pub enum AddressType {
    /// A public address
    Public = 0x0,
    /// A random address (either random static or private)
    Random = 0x1,
}

/// An address without type
#[derive(Copy, Clone, Debug, Hash, PartialEq, Eq)]
#[repr(C)]
pub struct RawAddress(pub [u8; 6]);

impl RawAddress {
    /// An empty/invalid address
    pub const EMPTY: Self = Self([0, 0, 0, 0, 0, 0]);
}

impl FromStr for RawAddress {
    type Err = macaddr::ParseError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        MacAddr6::from_str(s).map(MacAddr6::into_array).map(Self)
    }
}

/// An LE address
#[derive(Copy, Clone, Debug, Hash, PartialEq, Eq)]
#[repr(C)]
pub struct AddressWithType {
    /// The address bytes
    pub address: RawAddress,
    /// The address type, either public or random
    pub address_type: AddressType,
}

impl AddressWithType {
    /// An empty/invalid address
    pub const EMPTY: Self = Self { address: RawAddress::EMPTY, address_type: AddressType::Public };
}
