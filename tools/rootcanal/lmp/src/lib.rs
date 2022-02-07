//! Link Manager implemented in Rust

mod either;
mod future;
mod packets;
mod procedure;

#[cfg(test)]
mod test;

use std::cell::{Cell, RefCell};
use std::collections::VecDeque;
use std::convert::{TryFrom, TryInto};
use std::future::Future;
use std::mem::ManuallyDrop;
use std::pin::Pin;
use std::rc::Rc;
use std::slice;
use std::task::{Context, Poll};

use future::NoopWaker;
use hci::Packet as _;
use lmp::Packet as _;
use packets::{hci, lmp};

/// Number of hci command packets used
/// in Command Complete and Command Status
#[allow(non_upper_case_globals)]
pub const num_hci_command_packets: u8 = 1;

/// Max number of Bluetooth Peer
pub const MAX_PEER_NUMBER: usize = 7;

struct Link {
    peer: Cell<hci::Address>,
    // Only store one HCI packet as our Num_HCI_Command_Packets
    // is always 1
    hci: Cell<Option<hci::CommandPacket>>,
    lmp: RefCell<VecDeque<lmp::PacketPacket>>,
}

impl Default for Link {
    fn default() -> Self {
        Link {
            peer: Cell::new(hci::EMPTY_ADDRESS),
            hci: Default::default(),
            lmp: Default::default(),
        }
    }
}

trait LinkManagerContext {
    fn ops(&self) -> &LinkManagerOps;
    fn link(&self, idx: u8) -> &Link;
}

struct LinkContext {
    index: u8,
    manager: Pin<Rc<dyn LinkManagerContext>>,
}

impl procedure::Context for LinkContext {
    fn poll_hci_command<C: TryFrom<hci::CommandPacket>>(&self) -> Poll<C> {
        let link = self.manager.link(self.index);
        let command = link.hci.take();

        if let Some(command) = command.clone().and_then(|c| c.try_into().ok()) {
            Poll::Ready(command)
        } else {
            link.hci.set(command);
            Poll::Pending
        }
    }

    fn poll_lmp_packet<P: TryFrom<lmp::PacketPacket>>(&self) -> Poll<P> {
        let link = self.manager.link(self.index);
        let mut queue = link.lmp.borrow_mut();
        let packet = queue.front().and_then(|packet| packet.clone().try_into().ok());

        if let Some(packet) = packet {
            queue.pop_front();
            Poll::Ready(packet)
        } else {
            Poll::Pending
        }
    }

    fn send_hci_event<E: Into<hci::EventPacket>>(&self, event: E) {
        self.manager.ops().send_hci_event(&*event.into().to_vec());
    }

    fn send_lmp_packet<P: Into<lmp::PacketPacket>>(&self, packet: P) {
        let link = self.manager.link(self.index);
        self.manager.ops().send_lmp_packet(link.peer.get(), &*packet.into().to_vec());
    }

    fn peer_address(&self) -> hci::Address {
        let link = self.manager.link(self.index);
        link.peer.get()
    }

    fn peer_handle(&self) -> u16 {
        let link = self.manager.link(self.index);
        self.manager.ops().get_handle(link.peer.get())
    }
}

/// Link Manager Context
pub struct LinkManager<Proc> {
    ops: LinkManagerOps,
    links: [Link; MAX_PEER_NUMBER],
    procedures: RefCell<[Option<Proc>; MAX_PEER_NUMBER]>,
}

impl<Proc> LinkManager<Proc> {
    fn new(ops: LinkManagerOps) -> Self {
        Self { ops, links: Default::default(), procedures: Default::default() }
    }

    fn ingest_lmp(&self, from: hci::Address, packet: &[u8]) {
        let link = self.links.iter().find(|link| link.peer.get() == from).expect("Unknown link");

        link.lmp.borrow_mut().push_back(lmp::PacketPacket::parse(packet).unwrap())
    }

    fn ingest_hci(&self, packet: &[u8]) {
        let command = hci::CommandPacket::parse(packet).unwrap();

        let peer = hci::command_connection_handle(&command)
            .map(|handle| self.ops.get_address(handle))
            .or_else(|| hci::command_remote_device_address(&command));

        if let Some(peer) = peer {
            let link =
                self.links.iter().find(|link| link.peer.get() == peer).expect("Unknown link");

            assert!(link.hci.replace(Some(command)).is_none(), "HCI flow control violation");
        } else {
            todo!("Unhandled hci packet");
        }
    }
}

impl<Proc: 'static> LinkManager<Proc> {
    fn add_link(self: &Pin<Rc<Self>>, peer: hci::Address, procedure: fn(LinkContext) -> Proc) {
        let slot = self.links.iter().enumerate().find(|(_, link)| link.peer.get().is_empty());

        if let Some((index, link)) = slot {
            link.peer.set(peer);
            let context = LinkContext { index: index as u8, manager: self.clone() };
            self.procedures.borrow_mut()[index] = Some(procedure(context));
        } else {
            panic!("Max number of links exceeded");
        }
    }
}

impl<Proc> LinkManager<Proc>
where
    Proc: Future<Output = ()>,
{
    fn tick(self: Pin<&Self>) {
        let waker = NoopWaker::new();

        for procedures in self.procedures.borrow_mut().iter_mut().filter_map(Option::as_mut) {
            // Safety:
            // This pin projection is safe because:
            // - This type doesn't implement Drop
            // - It's pinned forever as we are not moving the value anywhere
            // TODO: consider replacing this with a PinCell
            let procedures = unsafe { Pin::new_unchecked(procedures) };
            let _ = procedures.poll(&mut Context::from_waker(&waker));
        }
    }
}

impl<Proc> LinkManagerContext for LinkManager<Proc> {
    fn ops(&self) -> &LinkManagerOps {
        &self.ops
    }

    fn link(&self, idx: u8) -> &Link {
        &self.links[idx as usize]
    }
}

/// TODO
#[repr(C)]
#[derive(Clone)]
pub struct LinkManagerOps {
    user_pointer: *const (),
    get_handle: extern "C" fn(user: *const (), address: *const u8) -> u16,
    get_address: extern "C" fn(user: *const (), handle: u16, result: *mut u8),
    send_hci_event: extern "C" fn(user: *const (), data: *const u8, len: usize),
    send_lmp_packet: extern "C" fn(user: *const (), to: *const u8, data: *const u8, len: usize),
}

impl LinkManagerOps {
    fn get_address(&self, handle: u16) -> hci::Address {
        let mut result = hci::EMPTY_ADDRESS;
        (self.get_address)(self.user_pointer, handle, result.bytes.as_mut_ptr());
        result
    }

    fn get_handle(&self, addr: hci::Address) -> u16 {
        (self.get_handle)(self.user_pointer, addr.bytes.as_ptr())
    }

    fn send_hci_event(&self, packet: &[u8]) {
        (self.send_hci_event)(self.user_pointer, packet.as_ptr(), packet.len())
    }

    fn send_lmp_packet(&self, to: hci::Address, packet: &[u8]) {
        (self.send_lmp_packet)(self.user_pointer, to.bytes.as_ptr(), packet.as_ptr(), packet.len())
    }
}

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
pub unsafe extern "C" fn link_manager_tick(lm: LinkManagerPtr) {
    lm.get(procedure::run).as_ref().tick();
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_hci(lm: LinkManagerPtr, data: *const u8, len: usize) {
    let packet = slice::from_raw_parts(data, len);

    lm.get(procedure::run).ingest_hci(packet);
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
    let packet = slice::from_raw_parts(data, len);

    lm.get(procedure::run).ingest_lmp(hci::Address { bytes: *from }, packet);
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_destroy(lm: LinkManagerPtr) {
    let _ = ManuallyDrop::into_inner(lm.get(procedure::run));
}
