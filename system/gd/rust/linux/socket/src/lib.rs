use std::mem;
use std::os::unix::io::{AsRawFd, RawFd};

#[macro_use]
extern crate num_derive;

use libc;
use log::debug;
use num_traits::cast::{FromPrimitive, ToPrimitive};

/// Socket protocol constant for HCI.
const BTPROTO_HCI: u8 = 1;

/// Non-existent HCI device for binding BT sockets.
pub const HCI_DEV_NONE: u16 = 0xFFFF;

/// Bindable configurations for open HCI sockets.
#[derive(ToPrimitive)]
#[repr(u16)]
pub enum HciChannels {
    Raw = 0,
    User = 1,
    Monitor = 2,
    Control = 3,
    Logging = 4,

    Unbound = 0xFFFF,
}

impl From<HciChannels> for u16 {
    fn from(item: HciChannels) -> Self {
        item.to_u16().unwrap()
    }
}

#[repr(C)]
struct SockAddrHci {
    hci_family: libc::sa_family_t,
    hci_dev: u16,
    hci_channel: u16,
}

/// Maximum size of a MGMT command or event packet.
const MGMT_PKT_DATA_MAX: usize = 1024;

/// Size of MGMT packet header.
const MGMT_PKT_HEADER_SIZE: usize = 6;

/// Total size of MGMT packet.
pub const MGMT_PKT_SIZE_MAX: usize = MGMT_PKT_HEADER_SIZE + MGMT_PKT_DATA_MAX;

/// Represents a MGMT packet (either command or event) in the raw form that can
/// be read from or written to the MGMT socket.
#[derive(Debug)]
pub struct MgmtPacket {
    opcode: u16,
    index: u16,
    len: u16,
    data: Vec<u8>,
}

impl MgmtPacket {
    fn write_to_wire(&self) -> Vec<u8> {
        let mut v: Vec<u8> = Vec::new();

        v.extend_from_slice(self.opcode.to_le_bytes().as_slice());
        v.extend_from_slice(self.index.to_le_bytes().as_slice());
        v.extend_from_slice(self.len.to_le_bytes().as_slice());
        v.extend_from_slice(self.data.as_slice());

        v
    }
}

#[derive(FromPrimitive, ToPrimitive)]
pub enum MgmtCommandOpcode {
    ReadIndexList = 0x3,
}

impl From<MgmtCommandOpcode> for u16 {
    fn from(item: MgmtCommandOpcode) -> Self {
        item.to_u16().unwrap()
    }
}

impl TryFrom<u16> for MgmtCommandOpcode {
    type Error = ();

    fn try_from(item: u16) -> Result<Self, Self::Error> {
        match MgmtCommandOpcode::from_u16(item) {
            Some(v) => Ok(v),
            None => Err(()),
        }
    }
}

pub enum MgmtCommand {
    ReadIndexList,
}

impl From<MgmtCommand> for MgmtPacket {
    fn from(item: MgmtCommand) -> Self {
        match item {
            MgmtCommand::ReadIndexList => MgmtPacket {
                opcode: MgmtCommandOpcode::ReadIndexList.into(),
                index: HCI_DEV_NONE,
                len: 0,
                data: Vec::new(),
            },
        }
    }
}

#[derive(FromPrimitive, ToPrimitive, Debug)]
pub enum MgmtEventOpcode {
    CommandComplete = 0x1,
    IndexAdded = 0x4,
    IndexRemoved = 0x5,
}

impl TryFrom<u16> for MgmtEventOpcode {
    type Error = ();

    fn try_from(item: u16) -> Result<Self, Self::Error> {
        match MgmtEventOpcode::from_u16(item) {
            Some(v) => Ok(v),
            None => Err(()),
        }
    }
}

#[derive(Debug)]
pub enum MgmtCommandResponse {
    // This is a meta enum that is only used to indicate that the remaining data
    // for this response has been dropped.
    DataUnused,

    ReadIndexList { num_intf: u16, interfaces: Vec<u16> },
}

#[derive(Debug)]
pub enum MgmtEvent {
    /// Command completion event.
    CommandComplete { opcode: u16, status: u8, response: MgmtCommandResponse },

    /// HCI device was added.
    IndexAdded(u16),

    /// HCI device was removed.
    IndexRemoved(u16),
}

impl TryFrom<MgmtPacket> for MgmtEvent {
    type Error = ();

    fn try_from(item: MgmtPacket) -> Result<Self, Self::Error> {
        MgmtEventOpcode::try_from(item.opcode).and_then(|ev| {
            Ok(match ev {
                MgmtEventOpcode::CommandComplete => {
                    // Minimum 3 bytes required for opcode + status
                    if item.data.len() < 3 {
                        debug!("CommandComplete packet too small: {}", item.data.len());
                        return Err(());
                    }

                    let (opcode_arr, rest) = item.data.split_at(std::mem::size_of::<u16>());

                    let opcode = u16::from_le_bytes(opcode_arr.try_into().unwrap());
                    let status = rest[0];
                    let (_, rest) = rest.split_at(std::mem::size_of::<u8>());

                    let response = if let Ok(op) = MgmtCommandOpcode::try_from(opcode) {
                        match op {
                            MgmtCommandOpcode::ReadIndexList => {
                                if rest.len() < 2 {
                                    debug!("ReadIndexList packet too small: {}", rest.len());
                                    return Err(());
                                }

                                let (len_arr, rest) = rest.split_at(std::mem::size_of::<u16>());
                                let len = u16::from_le_bytes(len_arr.try_into().unwrap());

                                let explen = (len as usize) * 2usize;
                                if rest.len() < explen {
                                    debug!(
                                        "ReadIndexList len malformed: expect = {}, actual = {}",
                                        explen,
                                        rest.len()
                                    );
                                    return Err(());
                                }

                                let interfaces: Vec<u16> = item.data[5..]
                                    .iter()
                                    .zip(item.data[5..].iter().skip(1))
                                    .map(|bytes| u16::from_le_bytes([*bytes.0, *bytes.1]))
                                    .collect();

                                MgmtCommandResponse::ReadIndexList { num_intf: len, interfaces }
                            }
                        }
                    } else {
                        MgmtCommandResponse::DataUnused
                    };

                    MgmtEvent::CommandComplete { opcode, status, response }
                }
                MgmtEventOpcode::IndexAdded => MgmtEvent::IndexAdded(item.index),
                MgmtEventOpcode::IndexRemoved => MgmtEvent::IndexRemoved(item.index),
            })
        })
    }
}

/// This struct is used to keep track of an open Bluetooth MGMT socket and it's
/// current state. It is meant to be used in two ways: call MGMT commands that
/// don't have a open hci device requirement or support being called when the
/// device is userchannel owned.
#[repr(C)]
pub struct BtSocket {
    sock_fd: i32,
    channel_type: HciChannels,
}

// Close given file descriptor.
fn close_fd(fd: i32) -> i32 {
    unsafe { libc::close(fd) }
}

impl Drop for BtSocket {
    fn drop(&mut self) {
        if self.has_valid_fd() {
            close_fd(self.sock_fd);
        }
    }
}

impl BtSocket {
    pub fn new() -> Self {
        BtSocket { sock_fd: -1, channel_type: HciChannels::Unbound }
    }

    /// Is the current file descriptor valid?
    pub fn has_valid_fd(&self) -> bool {
        self.sock_fd >= 0
    }

    /// Open raw socket to Bluetooth. This should be the first thing called.
    pub fn open(&mut self) -> i32 {
        if self.has_valid_fd() {
            return self.sock_fd;
        }

        unsafe {
            let sockfd = libc::socket(
                libc::PF_BLUETOOTH,
                libc::SOCK_RAW | libc::SOCK_NONBLOCK,
                BTPROTO_HCI.into(),
            );

            if sockfd >= 0 {
                self.sock_fd = sockfd;
            }

            sockfd
        }
    }

    /// Bind socket to a specific HCI channel type.
    pub fn bind_channel(&mut self, channel: HciChannels, hci_dev: u16) -> i32 {
        unsafe {
            let addr = SockAddrHci {
                // AF_BLUETOOTH can always be cast into u16
                hci_family: libc::sa_family_t::try_from(libc::AF_BLUETOOTH).unwrap(),
                hci_dev,
                hci_channel: channel.into(),
            };

            return libc::bind(
                self.sock_fd,
                (&addr as *const SockAddrHci) as *const libc::sockaddr,
                mem::size_of::<SockAddrHci>() as u32,
            );
        }
    }

    /// Take ownership of the file descriptor owned by this context. The caller
    /// is responsible for closing the underlying socket if it is open (this is
    /// intended to be used with something like AsyncFd).
    pub fn take_fd(&mut self) -> i32 {
        let fd = self.sock_fd;
        self.sock_fd = -1;

        fd
    }

    pub fn read_mgmt_packet(&mut self) -> Option<MgmtPacket> {
        if !self.has_valid_fd() {
            return None;
        }

        unsafe {
            let mut buf: [u8; MGMT_PKT_SIZE_MAX] = [0; MGMT_PKT_SIZE_MAX];
            let mut bytes_read;
            loop {
                bytes_read = libc::read(
                    self.sock_fd,
                    buf.as_mut_ptr() as *mut libc::c_void,
                    MGMT_PKT_SIZE_MAX,
                );

                // Retry if -EINTR
                let retry = (bytes_read == -1)
                    && std::io::Error::last_os_error().raw_os_error().unwrap_or(0) == libc::EINTR;

                if !retry {
                    break;
                }
            }

            // Exit early on error.
            if bytes_read == -1 {
                debug!(
                    "read_mgmt_packet failed with errno {}",
                    std::io::Error::last_os_error().raw_os_error().unwrap_or(0)
                );
                return None;
            }

            if bytes_read < (MGMT_PKT_HEADER_SIZE as isize) {
                debug!("read_mgmt_packet got {} bytes (not enough for header)", bytes_read);
                return None;
            }

            let data_size: usize =
                (bytes_read - (MGMT_PKT_HEADER_SIZE as isize)).try_into().unwrap();

            let (opcode_arr, rest) = buf.split_at(std::mem::size_of::<u16>());
            let (index_arr, rest) = rest.split_at(std::mem::size_of::<u16>());
            let (len_arr, rest) = rest.split_at(std::mem::size_of::<u16>());
            let data_arr = rest;

            Some(MgmtPacket {
                opcode: u16::from_le_bytes(opcode_arr.try_into().unwrap()),
                index: u16::from_le_bytes(index_arr.try_into().unwrap()),
                len: u16::from_le_bytes(len_arr.try_into().unwrap()),
                data: match data_size {
                    x if x > 0 => data_arr[..x].iter().map(|x| *x).collect::<Vec<u8>>(),
                    _ => Vec::new(),
                },
            })
        }
    }

    pub fn write_mgmt_packet(&mut self, packet: MgmtPacket) -> isize {
        let wire_data = packet.write_to_wire();
        unsafe {
            let mut bytes_written;
            loop {
                bytes_written = libc::write(
                    self.sock_fd,
                    wire_data.as_slice().as_ptr() as *const libc::c_void,
                    wire_data.len(),
                );

                // Retry if -EINTR
                let retry = bytes_written == -1
                    && std::io::Error::last_os_error().raw_os_error().unwrap_or(0) == libc::EINTR;

                if !retry {
                    break;
                }
            }

            bytes_written
        }
    }
}

impl AsRawFd for BtSocket {
    fn as_raw_fd(&self) -> RawFd {
        self.sock_fd
    }
}
