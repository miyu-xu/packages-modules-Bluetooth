//! Link Manager implemented in Rust

mod either;
mod future;
mod packets;
mod procedure;

use std::cell::RefCell;
use std::collections::VecDeque;
use std::convert::{TryFrom, TryInto};
use std::future::Future;
use std::mem::ManuallyDrop;
use std::pin::Pin;
use std::rc::Rc;
use std::slice;
use std::sync::Arc;
use std::task::{Context, Poll, Wake};

use hci::Packet as _;
use lmp::Packet as _;
use packets::{hci, lmp};

/// TODO
pub use procedure::run;

struct NoopWaker;

impl Wake for NoopWaker {
    fn wake(self: Arc<Self>) {}
}

struct Link {
    peer: [u8; 6],
    ops: LinkManagerOps,
    hci: RefCell<VecDeque<hci::CommandPacket>>,
    lmp: RefCell<VecDeque<lmp::PacketPacket>>,
}

impl procedure::Context for Link {
    fn poll_hci_command<C: TryFrom<hci::CommandPacket>>(&self) -> Poll<C> {
        let mut queue = self.hci.borrow_mut();
        let command = queue.front().and_then(|command| command.clone().try_into().ok());

        if let Some(command) = command {
            queue.pop_front();
            Poll::Ready(command)
        } else {
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

    fn send_hci_event<E: Into<hci::EventPacket>>(&self, event: E) {
        self.ops.send_hci_event(&*event.into().to_vec());
    }
    fn send_lmp_packet<P: Into<lmp::PacketPacket>>(&self, packet: P) {
        self.ops.send_lmp_packet(&self.peer, &*packet.into().to_vec());
    }

    fn peer_address(&self) -> hci::Address {
        hci::Address { bytes: self.peer }
    }

    fn peer_handle(&self) -> u16 {
        self.ops.get_handle(&self.peer)
    }
}

impl Link {
    fn new(peer: [u8; 6], ops: LinkManagerOps) -> Self {
        Self { peer, ops, hci: RefCell::new(VecDeque::new()), lmp: RefCell::new(VecDeque::new()) }
    }
}

/// TODO
pub struct LinkManager {
    ops: LinkManagerOps,
    links: Vec<(Rc<Link>, Pin<Box<dyn Future<Output = ()>>>)>,
}

impl LinkManager {
    /// TODO
    fn new(ops: LinkManagerOps) -> Self {
        Self { links: Vec::new(), ops }
    }

    fn add_link(&mut self, peer: [u8; 6]) {
        println!("======= new link");
        let link = Rc::new(Link::new(peer, self.ops.clone()));
        let context = link.clone();

        let procedures = Box::pin(async move { procedure::run(&*context).await });
        self.links.push((link, procedures));
    }

    /// TODO
    pub fn ingest_lmp(&self, from: &[u8; 6], packet: &[u8]) {
        let link = self.links.iter().find(|(link, _)| &link.peer == from).expect("unknown peer");
        link.0.lmp.borrow_mut().push_back(lmp::PacketPacket::parse(packet).unwrap())
    }

    fn get_command_peer(&self, command: &hci::CommandPacket) -> Option<[u8; 6]> {
        match command.specialize() {
            hci::CommandChild::SecurityCommand(command) => match command.specialize() {
                hci::SecurityCommandChild::LinkKeyRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::LinkKeyRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::PinCodeRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::PinCodeRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::IoCapabilityRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::IoCapabilityRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::UserConfirmationRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::UserConfirmationRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::UserPasskeyRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::UserPasskeyRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::RemoteOobDataRequestReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::RemoteOobDataRequestNegativeReply(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                hci::SecurityCommandChild::SendKeypressNotification(packet) => {
                    Some(packet.get_bd_addr().bytes)
                }
                _ => None,
            },
            hci::CommandChild::AclCommand(command) => match command.specialize() {
                hci::AclCommandChild::ConnectionManagementCommand(command) => {
                    match command.specialize() {
                        hci::ConnectionManagementCommandChild::AuthenticationRequested(packet) => {
                            Some(self.ops.get_address(packet.get_connection_handle()))
                        }
                        hci::ConnectionManagementCommandChild::SetConnectionEncryption(packet) => {
                            Some(self.ops.get_address(packet.get_connection_handle()))
                        }
                        _ => None,
                    }
                }
                _ => None,
            },
            _ => None,
        }
    }

    /// TODO
    pub fn ingest_hci(&self, packet: &[u8]) {
        let command = hci::CommandPacket::parse(packet).unwrap();
        println!("ingest hci packet");
        if let Some(peer) = self.get_command_peer(&command) {
            let link = self.links.iter().find(|(link, _)| link.peer == peer).expect("unknown peer");
            link.0.hci.borrow_mut().push_back(command)
        } else {
            todo!("Unhandled hci packet");
        }
    }

    /// TODO
    pub fn tick(&mut self) {
        for (_, future) in self.links.iter_mut() {
            let _ = future.as_mut().poll(&mut Context::from_waker(&Arc::new(NoopWaker).into()));
        }
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
    fn get_address(&self, handle: u16) -> [u8; 6] {
        let mut result = [0; 6];
        (self.get_address)(self.user_pointer, handle, result.as_mut_ptr());
        result
    }

    fn get_handle(&self, addr: &[u8; 6]) -> u16 {
        (self.get_handle)(self.user_pointer, addr.as_ptr())
    }

    fn send_hci_event(&self, packet: &[u8]) {
        (self.send_hci_event)(self.user_pointer, packet.as_ptr(), packet.len())
    }
    fn send_lmp_packet(&self, to: &[u8; 6], packet: &[u8]) {
        (self.send_lmp_packet)(self.user_pointer, to.as_ptr(), packet.as_ptr(), packet.len())
    }
}

/// TODO
#[no_mangle]
pub extern "C" fn link_manager_create(ops: LinkManagerOps) -> *mut LinkManager {
    Box::into_raw(Box::new(LinkManager::new(ops)))
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_add_link(lm: *mut LinkManager, peer: *const u8) {
    let mut lm = ManuallyDrop::new(Box::from_raw(lm));
    let peer = &*(peer as *const [u8; 6]);

    lm.add_link(*peer);
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_tick(lm: *mut LinkManager) {
    let mut lm = ManuallyDrop::new(Box::from_raw(lm));

    lm.tick();
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_hci(
    lm: *mut LinkManager,
    data: *const u8,
    len: usize,
) {
    let lm = ManuallyDrop::new(Box::from_raw(lm));
    let packet = slice::from_raw_parts(data, len);

    lm.ingest_hci(packet);
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_ingest_lmp(
    lm: *mut LinkManager,
    from: *const u8,
    data: *const u8,
    len: usize,
) {
    let lm = ManuallyDrop::new(Box::from_raw(lm));
    let from = &*(from as *const [u8; 6]);
    let packet = slice::from_raw_parts(data, len);

    lm.ingest_lmp(from, packet);
}

/// TODO
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn link_manager_destroy(lm: *mut LinkManager) {
    let _ = Box::from_raw(lm);
}
