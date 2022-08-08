use std::{
    collections::{hash_map::Entry, HashMap},
    fmt::Debug,
    hash::Hash,
};

use anyhow::{bail, Context, Result};



/// Utility data structure to map between persistent, reusable "keys" (e.g. channel IDs) and
/// single-use ephemeral "handles" (nonces) that will never be reused
pub struct HandleMap<T, U> {
    key_handles: HashMap<T, U>,
    handle_keys: HashMap<U, T>,
    // assumption is that this produces a unique value every time it is invoked
    gen_handle: Box<dyn Send + FnMut() -> U>,
}

impl<T: Copy + Eq + Hash + Debug, U: Copy + Eq + Hash + Debug> HandleMap<T, U> {
    pub fn new(gen_handle: Box<dyn Send + FnMut() -> U>) -> Self {
        HandleMap { key_handles: HashMap::new(), handle_keys: HashMap::new(), gen_handle }
    }

    pub fn assign_key(&mut self, key: T) -> Result<U> {
        let handle = (self.gen_handle)();
        match self.key_handles.entry(key) {
            Entry::Occupied(_) => {
                bail!("key {key:?} is currently in use, cannot assign")
            }
            Entry::Vacant(entry) => {
                entry.insert(handle);
            }
        }
        self.handle_keys.insert(handle, key);
        Ok(handle)
    }

    pub fn free_key(&mut self, key: T) -> Result<()> {
        match self.key_handles.remove(&key) {
            Some(handle) => self.handle_keys.remove(&handle),
            None => bail!("key {key:?} is not currently in use, cannot free"),
        };
        Ok(())
    }

    pub fn handle_for(&self, key: T) -> Result<U> {
        self.key_handles.get(&key).with_context(|| format!("could not find key {key:?}")).cloned()
    }

    pub fn key_for(&self, handle: U) -> Result<T> {
        self.handle_keys
            .get(&handle)
            .with_context(|| format!("could not find handle {handle:?}"))
            .cloned()
    }
}
