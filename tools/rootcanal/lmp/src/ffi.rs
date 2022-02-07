use std::mem::ManuallyDrop;
use std::pin::Pin;
use std::rc::Rc;
use std::slice;

use crate::manager::{LinkContext, LinkManager, LinkManagerOps};
use crate::packets::{hci, lmp};
use crate::procedure;

/// Link Manager pointer
#[repr(transparent)]
pub struct LinkManagerPtr(*const ());

impl LinkManagerPtr {
    fn new<Proc>(_marker: fn(LinkContext) -> Proc, ops: LinkManagerOps) -> Self {
        LinkManagerPtr(Rc::into_raw(Rc::new(LinkManager::<Proc>::new(ops))) as *const ())
    }

    unsafe fn get<Proc>(
        &self,
        _marker: fn(LinkContext) -> Proc,
    ) -> ManuallyDrop<Pin<Rc<LinkManager<Proc>>>> {
        ManuallyDrop::new(Pin::new_unchecked(Rc::from_raw(self.0 as *const LinkManager<Proc>)))
    }
}

/// Create a new link manager instance
/// # Arguments
/// * `ops` - Function callbacks required by the link manager
#[no_mangle]
pub extern "C" fn link_manager_create(ops: LinkManagerOps) -> LinkManagerPtr {
    LinkManagerPtr::new(procedure::run, ops)
}

/// Register a new link with a peer inside the link manager
/// # Arguments
/// * `lm` - link manager pointer
/// * `peer` - peer address as array of 6 bytes
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointer
/// - `peer` must be valid for reads for 6 bytes
#[no_mangle]
pub unsafe extern "C" fn link_manager_add_link(lm: LinkManagerPtr, peer: *const u8) {
    let peer = &*(peer as *const [u8; 6]);

    lm.get(procedure::run).add_link(hci::Address { bytes: *peer }, procedure::run);
}

/// Unregister a link with a peer inside the link manager
/// # Arguments
/// * `lm` - link manager pointer
/// * `peer` - peer address as array of 6 bytes
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointer
/// - `peer` must be valid for reads for 6 bytes
#[no_mangle]
pub unsafe extern "C" fn link_manager_remove_link(lm: LinkManagerPtr, peer: *const u8) {
    let peer = &*(peer as *const [u8; 6]);

    lm.get(procedure::run).remove_link(hci::Address { bytes: *peer });
}

/// Run the Link Manager procedures
/// # Arguments
/// * `lm` - link manager pointer
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointer
#[no_mangle]
pub unsafe extern "C" fn link_manager_tick(lm: LinkManagerPtr) {
    lm.get(procedure::run).as_ref().tick();
}

/// Process an HCI packet with the link manager
/// # Arguments
/// * `lm` - link manager pointer
/// * `data` - HCI packet data
/// * `len` - HCI packet len
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointer
/// - `data` must be valid for reads of len `len`
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_hci(lm: LinkManagerPtr, data: *const u8, len: usize) {
    let data = slice::from_raw_parts(data, len);

    lm.get(procedure::run).ingest_hci(hci::CommandPacket::parse(data).unwrap());
}

/// Process an LMP packet from a peer with the link manager
/// # Arguments
/// * `lm` - link manager pointer
/// * `from` - Address of peer as array of 6 bytes
/// * `data` - HCI packet data
/// * `len` - HCI packet len
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointers
/// - `from` must be valid pointer for reads for 6 bytes
/// - `data` must be valid for reads of len `len`
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_lmp(
    lm: LinkManagerPtr,
    from: *const u8,
    data: *const u8,
    len: usize,
) {
    let from = &*(from as *const [u8; 6]);
    let data = slice::from_raw_parts(data, len);

    lm.get(procedure::run)
        .ingest_lmp(hci::Address { bytes: *from }, lmp::PacketPacket::parse(data).unwrap());
}

/// Deallocate the link manager instance
/// # Arguments
/// * `lm` - link manager pointer
/// # Safety
/// - This should be called from the thread of creation
/// - `lm` must be a valid pointers and must not be reused afterwards
#[no_mangle]
pub unsafe extern "C" fn link_manager_destroy(lm: LinkManagerPtr) {
    let _ = ManuallyDrop::into_inner(lm.get(procedure::run));
}
