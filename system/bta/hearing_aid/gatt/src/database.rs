use std::ops::Deref;

use super::client::Cache;
use super::gatt;
use super::uuid::Uuid;

// TODO: support on android
//mod hash;

pub struct Database {
    pub primary_services: Cache<Service>,
    pub secondary_services: Cache<Service>,
    // UUIDs for which we know all the services
    pub know_all_services: Vec<Uuid>, // services_uuid_discovery_complete: Vec<u128>,
}

pub enum ServiceType {
    Primary,
    Secondary,
}

impl Database {
    pub fn empty() -> Self {
        Self {
            primary_services: Cache::empty(),
            secondary_services: Cache::empty(),
            know_all_services: vec![],
        }
    }

    pub fn iter_services(&self) -> impl Iterator<Item = (ServiceType, &Service)> {
        // TODO: sort
        self.primary_services
            .iter()
            .map(|service| (ServiceType::Primary, service))
            .chain(self.secondary_services.iter().map(|service| (ServiceType::Secondary, service)))
    }
}

pub struct Service {
    pub inner: gatt::Service,
    pub included_services: Cache<gatt::Include>,
    pub characteristics: Cache<Characteristic>,
}

impl From<gatt::Service> for Service {
    fn from(inner: gatt::Service) -> Self {
        Self { inner, characteristics: Cache::empty(), included_services: Cache::empty() }
    }
}

impl Deref for Service {
    type Target = gatt::Service;

    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}

pub struct Characteristic {
    pub inner: gatt::Characteristic,
    pub descriptors: Cache<Descriptor>,
}

impl From<gatt::Characteristic> for Characteristic {
    fn from(inner: gatt::Characteristic) -> Self {
        Self { inner, descriptors: Cache::empty() }
    }
}

impl Deref for Characteristic {
    type Target = gatt::Characteristic;

    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}

pub struct Descriptor {
    pub inner: gatt::Descriptor,
}

impl From<gatt::Descriptor> for Descriptor {
    fn from(inner: gatt::Descriptor) -> Self {
        Self { inner }
    }
}

impl Deref for Descriptor {
    type Target = gatt::Descriptor;

    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}
