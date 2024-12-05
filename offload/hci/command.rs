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

use crate::reader::{Read, Reader};

/// OpCode of HCI Command, as defined in Part E - 5.4.1
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum OpCode {
    /// 7.3.2   Reset Command
    Reset,
    /// 7.8.2   LE Read Buffer Size
    LeReadBufferSizeV2,
    /// 7.8.97  LE Set CIG Parameters
    LeSetCigParameters,
    /// 7.8.99  LE Create CIS
    LeCreateCis,
    /// 7.8.100 LE Remove CIG
    LeRemoveCig,
    /// 7.8.109 LE Setup ISO Data Path
    LeSetupIsoDataPath,
    /// 7.8.110 LE Remove ISO Data Path
    LeRemoveIsoDataPath,
    /// Unhandled OpCode
    Other {
        /// OpCode Group Field
        ogf: u16,
        /// OpCode Command Field
        ocf: u16,
    },
}

impl OpCode {
    /// OpCode from OpCode Group Field (OGF) and OpCode Command Field (OCF).
    pub fn from(ogf: u16, ocf: u16) -> Self {
        match (ogf, ocf) {
            (0x03, 0x003) => OpCode::Reset,
            (0x08, 0x060) => OpCode::LeReadBufferSizeV2,
            (0x08, 0x062) => OpCode::LeSetCigParameters,
            (0x08, 0x064) => OpCode::LeCreateCis,
            (0x08, 0x065) => OpCode::LeRemoveCig,
            (0x08, 0x06e) => OpCode::LeSetupIsoDataPath,
            (0x08, 0x06f) => OpCode::LeRemoveIsoDataPath,
            (ogf, ocf) => OpCode::Other { ogf, ocf },
        }
    }
}

impl From<u16> for OpCode {
    fn from(v: u16) -> Self {
        OpCode::from(v >> 10, v & 0x3ff)
    }
}

impl Read for OpCode {
    fn read(r: &mut Reader) -> Option<Self> {
        Some(r.read_u16()?.into())
    }
}

#[allow(missing_docs)]
#[derive(Debug)]
pub enum Command {
    Reset(Reset),
    LeReadBufferSizeV2(LeReadBufferSizeV2),
    LeSetCigParameters(LeSetCigParameters),
    LeCreateCis(LeCreateCis),
    LeRemoveCig(LeRemoveCig),
    LeSetupIsoDataPath(LeSetupIsoDataPath),
    LeRemoveIsoDataPath(LeRemoveIsoDataPath),
    Malformed(Option<OpCode>),
    Other(OpCode),
}

impl Command {
    /// Read an HCI Command packet
    pub fn from_bytes(data: &[u8]) -> Self {
        let Some((opcode, mut r)) = Self::parse_packet(data) else {
            return Self::Malformed(None);
        };

        Self::parse_command(opcode, &mut r).unwrap_or(Command::Malformed(Some(opcode)))
    }

    fn parse_packet(data: &[u8]) -> Option<(OpCode, Reader)> {
        let mut r = Reader::new(data);
        let opcode = r.read()?;
        let len = r.read_u8()? as usize;
        Some((opcode, Reader::new(r.get(len)?)))
    }

    fn parse_command(opcode: OpCode, r: &mut Reader) -> Option<Command> {
        Some(match opcode {
            OpCode::Reset => Self::Reset(Reset),
            OpCode::LeReadBufferSizeV2 => Self::LeReadBufferSizeV2(LeReadBufferSizeV2),
            OpCode::LeSetCigParameters => Self::LeSetCigParameters(r.read()?),
            OpCode::LeCreateCis => Self::LeCreateCis(r.read()?),
            OpCode::LeRemoveCig => Self::LeRemoveCig(r.read()?),
            OpCode::LeSetupIsoDataPath => Self::LeSetupIsoDataPath(r.read()?),
            OpCode::LeRemoveIsoDataPath => Self::LeRemoveIsoDataPath(r.read()?),
            OpCode::Other { ogf, ocf } => Self::Other(OpCode::Other { ogf, ocf }),
        })
    }
}

#[allow(missing_docs)]
#[derive(Debug)]
pub enum ReturnParameters {
    Reset(ResetComplete),
    LeReadBufferSizeV2(LeReadBufferSizeV2Complete),
    LeSetCigParameters(LeSetCigParametersComplete),
    LeRemoveCig(LeRemoveCigComplete),
    LeSetupIsoDataPath(LeIsoDataPathComplete),
    LeRemoveIsoDataPath(LeIsoDataPathComplete),
    Other(OpCode),
}

impl ReturnParameters {
    pub(crate) fn parse(opcode: OpCode, r: &mut Reader) -> Option<Self> {
        Some(match opcode {
            OpCode::Reset => Self::Reset(r.read()?),
            OpCode::LeReadBufferSizeV2 => Self::LeReadBufferSizeV2(r.read()?),
            OpCode::LeSetCigParameters => Self::LeSetCigParameters(r.read()?),
            OpCode::LeCreateCis => return None,
            OpCode::LeRemoveCig => Self::LeRemoveCig(r.read()?),
            OpCode::LeSetupIsoDataPath => Self::LeSetupIsoDataPath(r.read()?),
            OpCode::LeRemoveIsoDataPath => Self::LeRemoveIsoDataPath(r.read()?),
            OpCode::Other { ogf, ocf } => Self::Other(OpCode::Other { ogf, ocf }),
        })
    }
}

impl Read for ReturnParameters {
    fn read(r: &mut Reader) -> Option<Self> {
        Self::parse(r.read_u16()?.into(), r)
    }
}

pub use defs::*;

#[allow(missing_docs)]
#[rustfmt::skip]
mod defs {

use super::*;
use crate::derive::Read;


/// 7.3.2 Reset Command

#[derive(Debug)]
pub struct Reset;

#[derive(Debug, Read)]
pub struct ResetComplete {
    #[N(1)] pub status: u8,
}


/// 7.8.2 LE Read Buffer Size

#[derive(Debug)]
pub struct LeReadBufferSizeV2;

#[derive(Debug, Read)]
pub struct LeReadBufferSizeV2Complete {
    #[N(1)] pub status: u8,
    #[N(2)] pub le_acl_data_packet_length: u16,
    #[N(1)] pub total_num_le_acl_data_packets: u8,
    #[N(2)] pub iso_data_packet_length: u16,
    #[N(1)] pub total_num_iso_data_packets: u8,
}


/// 7.8.97 LE Set CIG Parameters

#[derive(Debug, Read)]
pub struct LeSetCigParameters {
    #[N(1)] pub cig_id: u8,
    #[N(3)] pub sdu_interval_c_to_p: u32,
    #[N(3)] pub sdu_interval_p_to_c: u32,
    #[N(1)] pub worst_case_sca: u8,
    #[N(1)] pub packing: u8,
    #[N(1)] pub framing: u8,
    #[N(2)] pub max_transport_latency_c_to_p: u16,
    #[N(2)] pub max_transport_latency_p_to_c: u16,
    #[N(1)] pub cis: Vec<LeCisInCigParameters>,
}

#[derive(Debug, Read)]
pub struct LeCisInCigParameters {
    #[N(1)] pub cis_id: u8,
    #[N(2)] pub max_sdu_c_to_p: u16,
    #[N(2)] pub max_sdu_p_to_c: u16,
    #[N(1)] pub phy_c_to_p: u8,
    #[N(1)] pub phy_p_to_c: u8,
    #[N(1)] pub rtn_c_to_p: u8,
    #[N(1)] pub rtn_p_to_c: u8,
}

#[derive(Debug, Read)]
pub struct LeSetCigParametersComplete {
    #[N(1)] pub status: u8,
    #[N(1)] pub cig_id: u8,
    #[N(1)] pub connection_handle: Vec<u16>,
}


/// 7.8.99 LE Create CIS

#[derive(Debug, Read)]
pub struct LeCreateCis {
    #[N(1)] pub connection_handles: Vec<CisAclConnectionHandle>,
}

#[derive(Debug, Read)]
pub struct CisAclConnectionHandle {
    #[N(2)] pub cis: u16,
    #[N(2)] pub acl: u16,
}


/// 7.8.100 LE Remove CIG

#[derive(Debug, Read)]
pub struct LeRemoveCig {
    #[N(1)] pub cig_id: u8,
}

#[derive(Debug, Read)]
pub struct LeRemoveCigComplete {
    #[N(1)] pub status: u8,
    #[N(1)] pub cig_id: u8,
}


/// 7.8.109 LE Setup ISO Data Path

#[derive(Debug, Read)]
pub struct LeSetupIsoDataPath {
    #[N(2)] pub connection_handle: u16,
    #[N(1)] pub data_path_direction: u8,
    #[N(1)] pub data_path_id: u8,
            pub codec_id: LeCodecId,
    #[N(3)] pub controller_delay: u32,
    #[N(1)] pub codec_configuration: Vec<u8>,
}

#[derive(Debug, Read)]
pub struct LeCodecId {
    #[N(1)] pub coding_format: u8,
    #[N(2)] pub company_id: u16,
    #[N(2)] pub vendor_id: u16,
}

#[derive(Debug, Read)]
pub struct LeIsoDataPathComplete {
    #[N(1)] pub status: u8,
    #[N(2)] pub connection_handle: u16,
}


/// 7.8.110 LE Remove ISO Data Path

#[derive(Debug, Read)]
pub struct LeRemoveIsoDataPath {
    #[N(2)] pub connection_handle: u16,
    #[N(1)] pub data_path_direction: u8,
}

}
