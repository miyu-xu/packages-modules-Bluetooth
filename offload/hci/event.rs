// Copyright 2024, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use crate::{
    reader::{Read, Reader},
    writer::{Write, Writer},
};

/// HCI Event Packet, as defined in Part E - 5.4.4
#[derive(Debug)]
pub enum Event {
    /// 7.7.5   Disconnection Complete
    DisconnectionComplete(DisconnectionComplete),
    /// 7.7.14  Command Complete
    CommandComplete(CommandComplete),
    /// 7.7.15  Command Status
    CommandStatus(CommandStatus),
    /// 7.7.19  Number Of Completed Packets
    NumberOfCompletedPackets(NumberOfCompletedPackets),
    /// 7.7.65.25  LE CIS Established
    LeCisEstablished(LeCisEstablished),
    /// 7.7.65.27  LE Create BIG Complete
    LeCreateBigComplete(LeCreateBigComplete),
    /// 7.7.65.28  LE Terminate BIG Complete
    LeTerminateBigComplete(LeTerminateBigComplete),
    /// Unhandled Event
    Other(u8, Option<u8>),
    /// Malformed Event
    Malformed(Option<u8>, Option<u8>),
}

impl Event {
    const LE_EVENT_CODE: u8 = 0x3e;

    /// Read an HCI Event packet
    pub fn from_bytes(data: &[u8]) -> Self {
        let Some((code, mut r)) = Self::parse_packet(data) else {
            return Self::Malformed(None, None);
        };

        Self::parse_event(code, &mut r).unwrap_or(Event::Malformed(Some(code), None))
    }

    fn parse_packet(data: &[u8]) -> Option<(u8, Reader)> {
        let mut r = Reader::new(data);
        let code = r.read_u8()?;
        let len = r.read_u8()? as usize;
        Some((code, Reader::new(r.get(len)?)))
    }

    fn parse_event(code: u8, r: &mut Reader) -> Option<Event> {
        Some(match code {
            CommandComplete::CODE => Self::CommandComplete(r.read()?),
            CommandStatus::CODE => Self::CommandStatus(r.read()?),
            DisconnectionComplete::CODE => Self::DisconnectionComplete(r.read()?),
            NumberOfCompletedPackets::CODE => Self::NumberOfCompletedPackets(r.read()?),
            Self::LE_EVENT_CODE => {
                let sub_code = r.read_u8()?;
                Self::parse_le_event(sub_code, r)
                    .unwrap_or(Event::Malformed(Some(code), Some(sub_code)))
            }
            code => Self::Other(code, None),
        })
    }

    fn parse_le_event(sub_code: u8, r: &mut Reader) -> Option<Event> {
        Some(match Some(sub_code) {
            LeCisEstablished::SUB_CODE => Self::LeCisEstablished(r.read()?),
            LeCreateBigComplete::SUB_CODE => Self::LeCreateBigComplete(r.read()?),
            LeTerminateBigComplete::SUB_CODE => Self::LeTerminateBigComplete(r.read()?),
            sub_code => Self::Other(Self::LE_EVENT_CODE, sub_code),
        })
    }

    fn to_bytes<T: Code + Write>(event: &T) -> Vec<u8> {
        let mut vec = Vec::with_capacity(2 + 255);
        vec.extend([T::CODE, 0u8]);
        let mut w = Writer::new(&mut vec);
        if let Some(sub_code) = T::SUB_CODE {
            w.write_u8(sub_code);
        }
        w.write(event);
        vec[1] = (vec.len() - 2).try_into().unwrap();
        vec
    }
}

/// Define event codes
pub trait Code {
    /// Code of the event
    const CODE: u8;
    /// Sub-Code when `CODE` is `Event::LE_EVENT_CODE`
    const SUB_CODE: Option<u8> = None;
}

/// Build event from definition
pub trait ToBytes: Code + Write {
    /// Output the HCI Event packet
    fn to_bytes(&self) -> Vec<u8>
    where
        Self: Sized + Code + Write,
    {
        Event::to_bytes(self)
    }
}

pub use defs::*;

#[allow(missing_docs)]
#[rustfmt::skip]
mod defs {

use super::*;
use crate::derive::{Read, Write};
use crate::command::{OpCode, ReturnParameters};


/// 7.7.5 Disconnection Complete

impl Code for DisconnectionComplete {
    const CODE: u8 = 0x05;
}

#[derive(Debug, Read)]
pub struct DisconnectionComplete {
    #[N(1)] pub status: u8,
    #[N(2)] pub connection_handle: u16,
    #[N(1)] pub reason: u8,
}


/// 7.7.14 Command Complete

impl Code for CommandComplete {
    const CODE: u8 = 0x0e;
}

#[derive(Debug, Read)]
pub struct CommandComplete {
    #[N(1)] pub num_hci_command_packets: u8,
    pub return_parameters: ReturnParameters,
}


/// 7.7.15 Command Status

impl Code for CommandStatus {
    const CODE: u8 = 0x0f;
}

#[derive(Debug, Read)]
pub struct CommandStatus {
    #[N(1)] pub status: u8,
    #[N(1)] pub num_hci_command_packets: u8,
    #[N(2)] pub opcode: OpCode,
}


/// 7.7.19 Number Of Completed Packets

impl Code for NumberOfCompletedPackets {
    const CODE: u8 = 0x13;
}

#[derive(Debug, Default, Read, Write)]
pub struct NumberOfCompletedPackets {
    #[N(1)] pub handles: Vec<NumberOfCompletedPacketsHandle>,
}

#[derive(Debug, Copy, Clone, Read, Write)]
pub struct NumberOfCompletedPacketsHandle {
    #[N(2)] pub connection_handle: u16,
    #[N(2)] pub num_completed_packets: u16,
}

impl ToBytes for NumberOfCompletedPackets {}


/// 7.7.65.25 LE CIS Established

impl Code for LeCisEstablished {
    const CODE: u8 = Event::LE_EVENT_CODE;
    const SUB_CODE: Option<u8> = Some(0x19);
}

#[derive(Debug, Read)]
pub struct LeCisEstablished {
    #[N(1)] pub status: u8,
    #[N(2)] pub connection_handle: u16,
    #[N(3)] pub cig_sync_delay: u32,
    #[N(3)] pub cis_sync_delay: u32,
    #[N(3)] pub transport_latency_c_to_p: u32,
    #[N(3)] pub transport_latency_p_to_c: u32,
    #[N(1)] pub phy_c_to_p: u8,
    #[N(1)] pub phy_p_to_c: u8,
    #[N(1)] pub nse: u8,
    #[N(1)] pub bn_c_to_p: u8,
    #[N(1)] pub bn_p_to_c: u8,
    #[N(1)] pub ft_c_to_p: u8,
    #[N(1)] pub ft_p_to_c: u8,
    #[N(2)] pub max_pdu_c_to_p: u16,
    #[N(2)] pub max_pdu_p_to_c: u16,
    #[N(2)] pub iso_interval: u16,
}


/// 7.7.65.27 LE Create BIG Complete

impl Code for LeCreateBigComplete {
    const CODE: u8 = Event::LE_EVENT_CODE;
    const SUB_CODE: Option<u8> = Some(0x1B);
}

#[derive(Debug, Read)]
pub struct LeCreateBigComplete {
    #[N(1)] pub status: u8,
    #[N(1)] pub big_handle: u8,
    #[N(3)] pub big_sync_delay: u32,
    #[N(3)] pub transport_latency_big: u32,
    #[N(1)] pub phy: u8,
    #[N(1)] pub nse: u8,
    #[N(1)] pub bn: u8,
    #[N(1)] pub pto: u8,
    #[N(1)] pub irc: u8,
    #[N(2)] pub max_pdu: u16,
    #[N(2)] pub iso_interval: u16,
    #[N(1)] pub bis_handles: Vec<u16>,
}


/// 7.7.65.28 LE Terminate BIG Complete

impl Code for LeTerminateBigComplete {
    const CODE: u8 = Event::LE_EVENT_CODE;
    const SUB_CODE: Option<u8> = Some(0x1C);
}

#[derive(Debug, Read)]
pub struct LeTerminateBigComplete {
    #[N(1)] pub big_handle: u8,
    #[N(1)] pub reason: u8,
}

}
