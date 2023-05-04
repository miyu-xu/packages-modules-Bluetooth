//! This module provides a mockable version of the address resolver, that lets us
//! configure the behavior of address resolution in test.

use std::{
    cell::RefCell,
    collections::{HashMap, HashSet},
    fmt::Debug,
    rc::Rc,
};

use async_trait::async_trait;

use crate::{
    connection::le_manager::{AddressResolver, CanonicalAddress},
    core::address::AddressWithType,
};

#[derive(Clone, Debug, Default)]
pub struct MockAddressResolver {
    address_equivalences: Rc<RefCell<HashMap<CanonicalAddress, HashSet<AddressWithType>>>>,
}

impl MockAddressResolver {
    pub fn new() -> Self {
        Self { address_equivalences: Default::default() }
    }

    pub fn set_address_equivalences(
        &self,
        equivalences: HashMap<CanonicalAddress, HashSet<AddressWithType>>,
    ) {
        *self.address_equivalences.borrow_mut() = equivalences;
    }

    pub fn associate_address(&self, address: CanonicalAddress, other: AddressWithType) {
        self.address_equivalences.borrow_mut().entry(address).or_default().insert(other);
    }

    pub fn clear_address(&self, address: CanonicalAddress) {
        self.address_equivalences.borrow_mut().remove(&address);
    }
}

#[async_trait(?Send)]
impl AddressResolver for MockAddressResolver {
    async fn resolve_address(&self, address: AddressWithType) -> CanonicalAddress {
        for (canonical, alternates) in self.address_equivalences.borrow().iter() {
            if address == canonical.addr() || alternates.contains(&address) {
                return *canonical;
            }
        }
        return CanonicalAddress::new(address);
    }
}
