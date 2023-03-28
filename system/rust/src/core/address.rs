//! An address with type (public / random)

use super::{AddressTypeForFFI, AddressWithTypeForFFI};

#[derive(Copy, Clone, Debug, Hash, PartialEq, Eq)]
pub enum AddressType {
    Public,
    Random,
}

#[derive(Copy, Clone, Debug, Hash, PartialEq, Eq)]
pub struct AddressWithType {
    /// Stored in little-endian format
    pub address: [u8; 16],
    pub address_type: AddressType,
}

impl From<AddressWithType> for AddressWithTypeForFFI {
    fn from(value: AddressWithType) -> Self {
        AddressWithTypeForFFI {
            address: value.address,
            address_type: match value.address_type {
                AddressType::Public => AddressTypeForFFI::Public,
                AddressType::Random => AddressTypeForFFI::Random,
            },
        }
    }
}

impl TryFrom<AddressWithTypeForFFI> for AddressWithType {
    type Error = AddressWithTypeForFFI;

    fn try_from(value: AddressWithTypeForFFI) -> Result<Self, Self::Error> {
        Ok(AddressWithType {
            address: value.address,
            address_type: match value.address_type {
                AddressTypeForFFI::Public => AddressType::Public,
                AddressTypeForFFI::Random => AddressType::Random,
                _ => return Err(value),
            },
        })
    }
}
