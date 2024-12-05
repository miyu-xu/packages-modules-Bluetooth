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

use super::{
    reader::Reader,
    writer::{Write, Writer},
};

/// 5.4.5 ISO Data Packets

/// Exchange of Isochronous Data between the Host and Controller
#[derive(Debug)]
pub struct IsoData<'a> {
    /// Identify the connection
    pub connection_handle: u16,
    /// Fragmentation of the packet
    pub sdu_fragment: IsoSduFragment,
    /// Payload
    pub payload: &'a [u8],
}

/// Fragmentation indication of the SDU
#[derive(Debug)]
pub enum IsoSduFragment {
    /// First SDU Fragment
    First {
        /// SDU Header
        hdr: IsoSduHeader,
        /// Last SDU fragment indication
        is_last: bool,
    },
    /// Continuous fragment
    Continue {
        /// Last SDU fragment indication
        is_last: bool,
    },
}

/// SDU Header information, when ISO Data in a first SDU fragment
#[derive(Debug, Default)]
pub struct IsoSduHeader {
    /// Optional timestamp in microseconds
    pub timestamp: Option<u32>,
    /// Sequence number of the SDU
    pub sequence_number: u16,
    /// Total length of the SDU (sum of all fragments)
    pub sdu_length: u16,
    /// Only valid from Controller, indicate valid SDU data when 0
    pub status: u8,
}

impl<'a> IsoData<'a> {
    /// Read an HCI ISO Data packet
    pub fn from_bytes(data: &'a [u8]) -> Option<Self> {
        Self::parse(&mut Reader::new(data))
    }

    /// Output the HCI ISO Data packet
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut vec = Vec::new();
        Writer::new(&mut vec).write(self);
        vec
    }

    /// New ISO Data packet, including a complete SDU
    pub fn new(connection_handle: u16, sequence_number: u16, data: &'a [u8]) -> Self {
        Self {
            connection_handle,
            sdu_fragment: IsoSduFragment::First {
                hdr: IsoSduHeader {
                    sequence_number,
                    sdu_length: data.len().try_into().unwrap(),
                    ..Default::default()
                },
                is_last: true,
            },
            payload: data,
        }
    }

    fn parse(r: &mut Reader<'a>) -> Option<Self> {
        let (connection_handle, pb_flag, ts_present) = {
            let v = r.read_u16()?;
            (v & 0xfff, (v >> 12) & 3, ((v >> 14) & 1) != 0)
        };

        let sdu_fragment = match pb_flag {
            0b00 => {
                IsoSduFragment::First { hdr: IsoSduHeader::parse(r, ts_present)?, is_last: false }
            }
            0b10 => {
                IsoSduFragment::First { hdr: IsoSduHeader::parse(r, ts_present)?, is_last: true }
            }
            0b01 => IsoSduFragment::Continue { is_last: false },
            0b11 => IsoSduFragment::Continue { is_last: true },
            _ => unreachable!(),
        };

        let sdu_header_len = Self::sdu_header_len(&sdu_fragment);
        let data_len = (r.read_u16()? & 0x3fff) as usize;
        if data_len < sdu_header_len {
            return None;
        }

        Some(Self { connection_handle, sdu_fragment, payload: r.get(data_len - sdu_header_len)? })
    }

    fn sdu_header_len(sdu_fragment: &IsoSduFragment) -> usize {
        match sdu_fragment {
            IsoSduFragment::First { ref hdr, .. } => 4 * (1 + hdr.timestamp.is_some() as usize),
            IsoSduFragment::Continue { .. } => 0,
        }
    }
}

impl<'a> Write for IsoData<'a> {
    fn write(&self, w: &mut Writer) {
        let (pb_flag, hdr) = match self.sdu_fragment {
            IsoSduFragment::First { ref hdr, is_last: false } => (0b00, Some(hdr)),
            IsoSduFragment::First { ref hdr, is_last: true } => (0b10, Some(hdr)),
            IsoSduFragment::Continue { is_last: false } => (0b01, None),
            IsoSduFragment::Continue { is_last: true } => (0b11, None),
        };

        let ts_present = hdr.is_some() && hdr.unwrap().timestamp.is_some();
        assert_eq!(self.connection_handle & !0xfff, 0);
        w.write_u16(self.connection_handle | (pb_flag << 12) | ((ts_present as u16) << 14));

        let packet_len = Self::sdu_header_len(&self.sdu_fragment) + self.payload.len();
        assert_eq!(packet_len & !0x3fff, 0);
        w.write_u16(packet_len as u16);

        if let Some(hdr) = hdr {
            w.write(hdr);
        }
        w.put(self.payload);
    }
}

impl IsoSduHeader {
    fn parse(r: &mut Reader, ts_present: bool) -> Option<Self> {
        let timestamp = match ts_present {
            true => Some(r.read_u32::<4>()?),
            false => None,
        };
        let sequence_number = r.read_u16()?;
        let (sdu_length, status) = Self::read_length_word(r)?;
        Some(Self { timestamp, sequence_number, sdu_length, status })
    }

    fn read_length_word(r: &mut Reader) -> Option<(u16, u8)> {
        let v = r.read_u16()?;
        Some((v & 0xfff, ((v >> 14) & 3) as u8))
    }
}

impl Write for IsoSduHeader {
    fn write(&self, w: &mut Writer) {
        if let Some(timestamp) = self.timestamp {
            w.write_u32::<4>(timestamp);
        };
        w.write_u16(self.sequence_number);

        assert_eq!(self.sdu_length & !0xfff, 0);
        assert_eq!(self.status & !0x3, 0);
        w.write_u16(self.sdu_length + ((self.status as u16) << 14));
    }
}
