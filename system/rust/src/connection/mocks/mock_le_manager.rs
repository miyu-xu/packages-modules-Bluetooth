use std::{collections::HashSet, cell::RefCell, rc::Rc};

use crate::connection::le_manager::{AddressWithType, LeAclManager, LeAclManagerConnectionCallbacks};

struct InactiveLeAclManager {
  callbacks: Box<dyn LeAclManagerConnectionCallbacks>,
}

impl 

#[derive(Debug)]
pub struct MockLeManager {
  state: RefCell<MockLeManagerInternalState>,
}

#[derive(Debug)]
struct MockLeManagerInternalState {
  direct_connect_list: HashSet<AddressWithType>,
  background_connect_list: HashSet<AddressWithType>,
  currently_connected: HashSet<AddressWithType>,
}

impl LeAclManager for Rc<MockLeManager> {
    fn add_to_direct_list(&self, address: AddressWithType) {
      let mut state = self.state.borrow_mut();
      assert!(!state.currently_connected.contains(&address), "Must NOT be currently connected to this adddress");
      let ok = state.direct_connect_list.insert(address);
      assert!(ok, "Already in direct connect list");
    }

    fn add_to_background_list(&self, address: AddressWithType) {
      let mut state = self.state.borrow_mut();
      assert!(!state.currently_connected.contains(&address), "Must NOT be currently connected to this adddress");
      let ok = state.background_connect_list.insert(address);
      assert!(ok, "Already in background connect list");
    }

    fn remove_device_from_background_list(&self, address: AddressWithType) {
      let mut state = self.state.borrow_mut();
      assert!(state.currently_connected.contains(&address), "Must be CONNECTED to this address");
      let ok = state.background_connect_list.remove(&address);
      assert!(ok, "Was not present in background connect list");
    }

    fn remove_from_all_lists(&self, address: AddressWithType) {
      let mut state = self.state.borrow_mut();
      assert!(!state.currently_connected.contains(&address), "Must NOT be currently connected to this adddress");
      let ok1 = state.direct_connect_list.remove(&address);
      let ok2 = state.background_connect_list.remove(&address);
      assert!(ok1 || ok2, "Present in neither direct nor background connect list");
    }
}

impl MockLeManager {
  pub fn on_le_connect(&self, address: AddressWithType) {
    let mut state = self.state.borrow_mut();
    let ok = state.currently_connected.insert(address);
    state.direct_connect_list.remove(&address);
    assert!(ok, "Already connected");
  }

  pub fn on_le_disconnect(&self, address: AddressWithType) {
    let mut state = self.state.borrow_mut();
    let ok = state.currently_connected.remove(&address);
    assert!(ok, "Not connected");
  }}
