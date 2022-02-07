use std::mem::ManuallyDrop;
use std::pin::Pin;
use std::rc::Rc;
use std::slice;

use crate::manager::{LinkContext, LinkManager, LinkManagerOps};
use crate::packets::{hci, lmp};
use crate::procedure;

/// TODO
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

/// TODO
#[no_mangle]
pub extern "C" fn link_manager_create(ops: LinkManagerOps) -> LinkManagerPtr {
    LinkManagerPtr::new(procedure::run, ops)
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_add_link(lm: LinkManagerPtr, peer: *const u8) {
    let peer = &*(peer as *const [u8; 6]);

    lm.get(procedure::run).add_link(hci::Address { bytes: *peer }, procedure::run);
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_remove_link(lm: LinkManagerPtr, peer: *const u8) {
    let peer = &*(peer as *const [u8; 6]);

    lm.get(procedure::run).remove_link(hci::Address { bytes: *peer });
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_tick(lm: LinkManagerPtr) {
    lm.get(procedure::run).as_ref().tick();
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_hci(lm: LinkManagerPtr, data: *const u8, len: usize) {
    let data = slice::from_raw_parts(data, len);

    lm.get(procedure::run).ingest_hci(hci::CommandPacket::parse(data).unwrap());
}

/// TODO
/// # Safety
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

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_destroy(lm: LinkManagerPtr) {
    let _ = ManuallyDrop::into_inner(lm.get(procedure::run));
}
