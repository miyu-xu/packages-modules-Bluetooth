use std::cell::{Cell, RefCell};
use std::collections::VecDeque;
use std::convert::{TryFrom, TryInto};
use std::future::Future;
use std::pin::Pin;
use std::rc::Rc;
use std::task::{Context, Poll};

use crate::future::NoopWaker;
use crate::packets::{hci, lmp};
use crate::procedure;

use hci::Packet as _;
use lmp::Packet as _;

/// Number of hci command packets used
/// in Command Complete and Command Status
#[allow(non_upper_case_globals)]
pub const num_hci_command_packets: u8 = 1;

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

impl Link {
    fn ingest_lmp(&self, packet: lmp::PacketPacket) {
        self.lmp.borrow_mut().push_back(packet);
    }

    fn ingest_hci(&self, command: hci::CommandPacket) {
        assert!(self.hci.replace(Some(command)).is_none(), "HCI flow control violation");
    }

    fn poll_hci_command<C: TryFrom<hci::CommandPacket>>(&self) -> Poll<C> {
        let command = self.hci.take();

        if let Some(command) = command.clone().and_then(|c| c.try_into().ok()) {
            Poll::Ready(command)
        } else {
            self.hci.set(command);
            Poll::Pending
        }
    }

    fn poll_lmp_packet<P: TryFrom<lmp::PacketPacket>>(&self) -> Poll<P> {
        let mut queue = self.lmp.borrow_mut();
        let packet = queue.front().and_then(|packet| packet.clone().try_into().ok());

        if let Some(packet) = packet {
            queue.pop_front();
            Poll::Ready(packet)
        } else {
            Poll::Pending
        }
    }

    fn reset(&self) {
        self.peer.set(hci::EMPTY_ADDRESS);
        self.hci.set(None);
        self.lmp.borrow_mut().clear();
    }
}

/// Max number of Bluetooth Peers
pub const MAX_PEER_NUMBER: usize = 7;

pub struct LinkManager<Proc> {
    ops: LinkManagerOps,
    links: [Link; MAX_PEER_NUMBER],
    procedures: RefCell<[Option<Proc>; MAX_PEER_NUMBER]>,
}

impl<Proc> LinkManager<Proc> {
    pub fn new(ops: LinkManagerOps) -> Self {
        Self { ops, links: Default::default(), procedures: Default::default() }
    }

    pub fn ingest_lmp(&self, from: hci::Address, packet: lmp::PacketPacket) {
        self.links
            .iter()
            .find(|link| link.peer.get() == from)
            .expect("Unknown link")
            .ingest_lmp(packet);
    }

    pub fn ingest_hci(&self, command: hci::CommandPacket) {
        // Try to find the peer address from the command arguments
        let peer = hci::command_connection_handle(&command)
            .map(|handle| self.ops.get_address(handle))
            .or_else(|| hci::command_remote_device_address(&command));

        if let Some(peer) = peer {
            self.links
                .iter()
                .find(|link| link.peer.get() == peer)
                .expect("Unknown link")
                .ingest_hci(command);
        } else {
            todo!("Unhandled hci packet");
        }
    }
}

impl<Proc: 'static> LinkManager<Proc> {
    pub fn add_link(self: &Pin<Rc<Self>>, peer: hci::Address, procedure: fn(LinkContext) -> Proc) {
        let index = self.links.iter().position(|link| link.peer.get().is_empty());

        if let Some(index) = index {
            self.links[index].peer.set(peer);
            let context = LinkContext { index: index as u8, manager: self.clone() };
            self.procedures.borrow_mut()[index] = Some(procedure(context));
        } else {
            panic!("Max number of links exceeded");
        }
    }

    pub fn remove_link(&self, peer: hci::Address) {
        let index = self.links.iter().position(|link| link.peer.get() == peer);

        if let Some(index) = index {
            self.links[index].reset();
            self.procedures.borrow_mut()[index] = None;
        } else {
            panic!("Unknown peer");
        }
    }
}

impl<Proc> LinkManager<Proc>
where
    Proc: Future<Output = ()>,
{
    pub fn tick(self: Pin<&Self>) {
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

/// Link Manager callbacks
#[repr(C)]
#[derive(Clone)]
pub struct LinkManagerOps {
    user_pointer: *const (),
    get_handle: extern "C" fn(user: *const (), address: *const u8) -> u16,
    get_address: extern "C" fn(user: *const (), handle: u16, result: *mut u8),
    extended_features: extern "C" fn(user: *const (), features_page: u8, result: *mut u8) -> bool,
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

    fn extended_features(&self, features_page: u8) -> [u8; 8] {
        let mut result = [0; 8];
        (self.extended_features)(self.user_pointer, features_page, result.as_mut_ptr());
        result
    }

    fn send_hci_event(&self, packet: &[u8]) {
        (self.send_hci_event)(self.user_pointer, packet.as_ptr(), packet.len())
    }

    fn send_lmp_packet(&self, to: hci::Address, packet: &[u8]) {
        (self.send_lmp_packet)(self.user_pointer, to.bytes.as_ptr(), packet.as_ptr(), packet.len())
    }
}

trait LinkManagerContext {
    fn ops(&self) -> &LinkManagerOps;
    fn link(&self, idx: u8) -> &Link;
}

impl<Proc> LinkManagerContext for LinkManager<Proc> {
    fn ops(&self) -> &LinkManagerOps {
        &self.ops
    }

    fn link(&self, idx: u8) -> &Link {
        &self.links[idx as usize]
    }
}

pub struct LinkContext {
    index: u8,
    manager: Pin<Rc<dyn LinkManagerContext>>,
}

impl procedure::Context for LinkContext {
    fn poll_hci_command<C: TryFrom<hci::CommandPacket>>(&self) -> Poll<C> {
        self.manager.link(self.index).poll_hci_command()
    }

    fn poll_lmp_packet<P: TryFrom<lmp::PacketPacket>>(&self) -> Poll<P> {
        self.manager.link(self.index).poll_lmp_packet()
    }

    fn send_hci_event<E: Into<hci::EventPacket>>(&self, event: E) {
        self.manager.ops().send_hci_event(&*event.into().to_vec())
    }

    fn send_lmp_packet<P: Into<lmp::PacketPacket>>(&self, packet: P) {
        self.manager.ops().send_lmp_packet(self.peer_address(), &*packet.into().to_vec())
    }

    fn peer_address(&self) -> hci::Address {
        self.manager.link(self.index).peer.get()
    }

    fn peer_handle(&self) -> u16 {
        self.manager.ops().get_handle(self.peer_address())
    }

    fn extended_features(&self, features_page: u8) -> [u8; 8] {
        self.manager.ops().extended_features(features_page)
    }
}
