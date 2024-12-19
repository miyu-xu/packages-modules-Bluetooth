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

/// HCI Command, as defined in Part E - 5.4.1
#[derive(Debug)]
pub enum Command {
    /// 7.3.2 Reset Command
    Reset(Reset),
    /// 7.8.2 LE Read Buffer Size
    LeReadBufferSizeV2(LeReadBufferSizeV2),
    /// 7.8.97 LE Set CIG Parameters
    LeSetCigParameters(LeSetCigParameters),
    /// 7.8.99 LE Create CIS
    LeCreateCis(LeCreateCis),
    /// 7.8.100 LE Remove CIG
    LeRemoveCig(LeRemoveCig),
    /// 7.8.109 LE Setup ISO Data Path
    LeSetupIsoDataPath(LeSetupIsoDataPath),
    /// 7.8.110 LE Remove ISO Data Path
    LeRemoveIsoDataPath(LeRemoveIsoDataPath),

    /// Unhandled command
    Other(OpCode),
    /// Malformed command
    Malformed(Option<OpCode>),
}

/// HCI Command Return Parameters
#[derive(Debug)]
pub enum ReturnParameters {
    /// 7.3.2 Reset Command
    Reset(ResetComplete),
    /// 7.8.2 LE Read Buffer Size
    LeReadBufferSizeV2(LeReadBufferSizeV2Complete),
    /// 7.8.97 LE Set CIG Parameters
    LeSetCigParameters(LeSetCigParametersComplete),
    /// 7.8.100 LE Remove CIG
    LeRemoveCig(LeRemoveCigComplete),
    /// 7.8.109 LE Setup ISO Data Path
    LeSetupIsoDataPath(LeIsoDataPathComplete),
    /// 7.8.110 LE Remove ISO Data Path
    LeRemoveIsoDataPath(LeIsoDataPathComplete),

    /// Unhandled command
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
            Reset::OPCODE => Self::Reset(Reset),
            LeReadBufferSizeV2::OPCODE => Self::LeReadBufferSizeV2(LeReadBufferSizeV2),
            LeSetCigParameters::OPCODE => Self::LeSetCigParameters(r.read()?),
            LeCreateCis::OPCODE => Self::LeCreateCis(r.read()?),
            LeRemoveCig::OPCODE => Self::LeRemoveCig(r.read()?),
            LeSetupIsoDataPath::OPCODE => Self::LeSetupIsoDataPath(r.read()?),
            LeRemoveIsoDataPath::OPCODE => Self::LeRemoveIsoDataPath(r.read()?),
            _ => Self::Other(opcode),
        })
    }
}

impl ReturnParameters {
    pub(crate) fn parse(opcode: OpCode, r: &mut Reader) -> Option<Self> {
        Some(match opcode {
            Reset::OPCODE => Self::Reset(r.read()?),
            LeReadBufferSizeV2::OPCODE => Self::LeReadBufferSizeV2(r.read()?),
            LeSetCigParameters::OPCODE => Self::LeSetCigParameters(r.read()?),
            LeRemoveCig::OPCODE => Self::LeRemoveCig(r.read()?),
            LeSetupIsoDataPath::OPCODE => Self::LeSetupIsoDataPath(r.read()?),
            LeRemoveIsoDataPath::OPCODE => Self::LeRemoveIsoDataPath(r.read()?),
            _ => Self::Other(opcode),
        })
    }
}

impl Read for ReturnParameters {
    fn read(r: &mut Reader) -> Option<Self> {
        Self::parse(r.read_u16()?.into(), r)
    }
}

/// OpCode of HCI Command, as defined in Part E - 5.4.1
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct OpCode(u16);

impl OpCode {
    /// OpCode from OpCode Group Field (OGF) and OpCode Command Field (OCF).
    pub const fn from(ogf: u16, ocf: u16) -> Self {
        Self((ogf << 10) | ocf)
    }
}

impl From<u16> for OpCode {
    fn from(v: u16) -> Self {
        OpCode(v)
    }
}

impl Read for OpCode {
    fn read(r: &mut Reader) -> Option<Self> {
        Some(r.read_u16()?.into())
    }
}

/// Define command OpCode
pub trait OpCodeDef {
    /// OpCode of the command
    const OPCODE: OpCode;
}

pub use defs::*;

#[allow(missing_docs)]
#[rustfmt::skip]
mod defs {

use super::*;
use crate::derive::Read;

#[cfg(test)]
use crate::Event;


// 7.3.2 Reset Command

impl OpCodeDef for Reset {
    const OPCODE: OpCode = OpCode::from(0x03, 0x003);
}

#[derive(Debug)]
pub struct Reset;

#[derive(Debug, Read)]
pub struct ResetComplete {
    pub status: u8,
}

#[test]
fn reset_complete() {
    let dump = [0x0e, 0x04, 0x01, 0x03, 0x0c, 0x00];
    let Event::CommandComplete(command_complete) = Event::from_bytes(&dump) else { panic!() };
    let ReturnParameters::Reset(r) = command_complete.return_parameters else { panic!() };
    assert_eq!(r.status, 0);
}


// 7.8.2 LE Read Buffer Size

impl OpCodeDef for LeReadBufferSizeV2 {
    const OPCODE: OpCode = OpCode::from(0x08, 0x060);
}

#[derive(Debug)]
pub struct LeReadBufferSizeV2;

#[derive(Debug, Read)]
pub struct LeReadBufferSizeV2Complete {
    pub status: u8,
    pub le_acl_data_packet_length: u16,
    pub total_num_le_acl_data_packets: u8,
    pub iso_data_packet_length: u16,
    pub total_num_iso_data_packets: u8,
}

#[test]
fn le_read_buffer_size_v2_complete() {
    let dump = [0x0e, 0x0a, 0x01, 0x60, 0x20, 0x00, 0xfb, 0x00, 0x0f, 0xfd, 0x03, 0x18];
    let Event::CommandComplete(command_complete) = Event::from_bytes(&dump) else { panic!() };
    let ReturnParameters::LeReadBufferSizeV2(r) = command_complete.return_parameters else { panic!() };
    assert_eq!(r.status, 0);
    assert_eq!(r.le_acl_data_packet_length, 251);
    assert_eq!(r.total_num_le_acl_data_packets, 15);
    assert_eq!(r.iso_data_packet_length, 1021);
    assert_eq!(r.total_num_iso_data_packets, 24);
}


// 7.8.97 LE Set CIG Parameters

impl OpCodeDef for LeSetCigParameters {
    const OPCODE: OpCode = OpCode::from(0x08, 0x062);
}

#[derive(Debug, Read)]
pub struct LeSetCigParameters {
    pub cig_id: u8,
    #[N(3)] pub sdu_interval_c_to_p: u32,
    #[N(3)] pub sdu_interval_p_to_c: u32,
    pub worst_case_sca: u8,
    pub packing: u8,
    pub framing: u8,
    pub max_transport_latency_c_to_p: u16,
    pub max_transport_latency_p_to_c: u16,
    pub cis: Vec<LeCisInCigParameters>,
}

#[derive(Debug, Read)]
pub struct LeCisInCigParameters {
    pub cis_id: u8,
    pub max_sdu_c_to_p: u16,
    pub max_sdu_p_to_c: u16,
    pub phy_c_to_p: u8,
    pub phy_p_to_c: u8,
    pub rtn_c_to_p: u8,
    pub rtn_p_to_c: u8,
}

#[test]
fn le_set_cig_parameters() {
    let dump = [
        0x62, 0x20, 0x21, 0x01, 0x10, 0x27, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x64, 0x00, 0x05,
        0x00, 0x02, 0x00, 0x78, 0x00, 0x00, 0x00, 0x02, 0x03, 0x0d, 0x00, 0x01, 0x78, 0x00, 0x00, 0x00,
        0x02, 0x03, 0x0d, 0x00
    ];
    let Command::LeSetCigParameters(c) = Command::from_bytes(&dump) else { panic!() };
    assert_eq!(c.cig_id, 0x01);
    assert_eq!(c.sdu_interval_c_to_p, 10_000);
    assert_eq!(c.sdu_interval_p_to_c, 0);
    assert_eq!(c.worst_case_sca, 1);
    assert_eq!(c.packing, 0);
    assert_eq!(c.framing, 0);
    assert_eq!(c.max_transport_latency_c_to_p, 100);
    assert_eq!(c.max_transport_latency_p_to_c, 5);
    assert_eq!(c.cis.len(), 2);
    assert_eq!(c.cis[0].cis_id, 0);
    assert_eq!(c.cis[0].max_sdu_c_to_p, 120);
    assert_eq!(c.cis[0].max_sdu_p_to_c, 0);
    assert_eq!(c.cis[0].phy_c_to_p, 0x02);
    assert_eq!(c.cis[0].phy_p_to_c, 0x03);
    assert_eq!(c.cis[0].rtn_c_to_p, 13);
    assert_eq!(c.cis[0].rtn_p_to_c, 0);
    assert_eq!(c.cis[1].cis_id, 1);
    assert_eq!(c.cis[1].max_sdu_c_to_p, 120);
    assert_eq!(c.cis[1].max_sdu_p_to_c, 0);
    assert_eq!(c.cis[1].phy_c_to_p, 0x02);
    assert_eq!(c.cis[1].phy_p_to_c, 0x03);
    assert_eq!(c.cis[1].rtn_c_to_p, 13);
    assert_eq!(c.cis[1].rtn_p_to_c, 0);
}

#[derive(Debug, Read)]
pub struct LeSetCigParametersComplete {
    pub status: u8,
    pub cig_id: u8,
    pub connection_handle: Vec<u16>,
}

#[test]
fn le_set_cig_parameters_complete() {
    let dump = [0x0e, 0x0a, 0x01, 0x62, 0x20, 0x00, 0x01, 0x02, 0x60, 0x00, 0x61, 0x00];
    let Event::CommandComplete(command_complete) = Event::from_bytes(&dump) else { panic!() };
    let ReturnParameters::LeSetCigParameters(r) = command_complete.return_parameters else { panic!() };
    assert_eq!(r.status, 0);
    assert_eq!(r.cig_id, 1);
    assert_eq!(r.connection_handle.len(), 2);
    assert_eq!(r.connection_handle[0], 0x60);
    assert_eq!(r.connection_handle[1], 0x61);
}


// 7.8.99 LE Create CIS

impl OpCodeDef for LeCreateCis {
    const OPCODE: OpCode = OpCode::from(0x08, 0x064);
}

#[derive(Debug, Read)]
pub struct LeCreateCis {
    pub connection_handles: Vec<CisAclConnectionHandle>,
}

#[derive(Debug, Read)]
pub struct CisAclConnectionHandle {
    pub cis: u16,
    pub acl: u16,
}

#[test]
fn le_create_cis () {
    let dump = [0x64, 0x20, 0x09, 0x02, 0x60, 0x00, 0x40, 0x00, 0x61, 0x00, 0x41, 0x00];
    let Command::LeCreateCis(c) = Command::from_bytes(&dump) else { panic!() };
    assert_eq!(c.connection_handles.len(), 2);
    assert_eq!(c.connection_handles[0].cis, 0x60);
    assert_eq!(c.connection_handles[0].acl, 0x40);
    assert_eq!(c.connection_handles[1].cis, 0x61);
    assert_eq!(c.connection_handles[1].acl, 0x41);
}


// 7.8.100 LE Remove CIG

impl OpCodeDef for LeRemoveCig {
    const OPCODE: OpCode = OpCode::from(0x08, 0x065);
}

#[derive(Debug, Read)]
pub struct LeRemoveCig {
    pub cig_id: u8,
}

#[test]
fn le_remove_cig() {
    let dump = [0x65, 0x20, 0x01, 0x01];
    let Command::LeRemoveCig(c) = Command::from_bytes(&dump) else { panic!() };
    assert_eq!(c.cig_id, 0x01);
}

#[derive(Debug, Read)]
pub struct LeRemoveCigComplete {
    pub status: u8,
    pub cig_id: u8,
}

#[test]
fn le_remove_cig_complete() {
    let dump = [0x0e, 0x05, 0x01, 0x65, 0x20, 0x00, 0x01];
    let Event::CommandComplete(command_complete) = Event::from_bytes(&dump) else { panic!() };
    let ReturnParameters::LeRemoveCig(r) = command_complete.return_parameters else { panic!() };
    assert_eq!(r.status, 0x00);
    assert_eq!(r.cig_id, 0x01);
}


// 7.8.109 LE Setup ISO Data Path

impl OpCodeDef for LeSetupIsoDataPath {
    const OPCODE: OpCode = OpCode::from(0x08, 0x06e);
}

#[derive(Debug, Read)]
pub struct LeSetupIsoDataPath {
    pub connection_handle: u16,
    pub data_path_direction: u8,
    pub data_path_id: u8,
    pub codec_id: LeCodecId,
    #[N(3)] pub controller_delay: u32,
    pub codec_configuration: Vec<u8>,
}

#[derive(Debug, Read)]
pub struct LeCodecId {
    pub coding_format: u8,
    pub company_id: u16,
    pub vendor_id: u16,
}

#[test]
fn le_setup_iso_data_path() {
    let dump = [
        0x6e, 0x20, 0x0d, 0x60, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    ];
    let Command::LeSetupIsoDataPath(c) = Command::from_bytes(&dump) else { panic!() };
    assert_eq!(c.connection_handle, 0x60);
    assert_eq!(c.data_path_direction, 0x00);
    assert_eq!(c.data_path_id, 0x00);
    assert_eq!(c.codec_id.coding_format, 0x03);
    assert_eq!(c.codec_id.company_id, 0);
    assert_eq!(c.codec_id.vendor_id, 0);
    assert_eq!(c.controller_delay, 0);
    assert_eq!(c.codec_configuration.len(), 0);
}

#[derive(Debug, Read)]
pub struct LeIsoDataPathComplete {
    pub status: u8,
    pub connection_handle: u16,
}

#[test]
fn le_setup_iso_data_path_complete() {
    let dump = [0x0e, 0x06, 0x01, 0x6e, 0x20, 0x00, 0x60, 0x00];
    let Event::CommandComplete(command_complete) = Event::from_bytes(&dump) else { panic!() };
    let ReturnParameters::LeSetupIsoDataPath(r) = command_complete.return_parameters else { panic!() };
    assert_eq!(r.status, 0x00);
    assert_eq!(r.connection_handle, 0x60);
}


// 7.8.110 LE Remove ISO Data Path

impl OpCodeDef for LeRemoveIsoDataPath {
    const OPCODE: OpCode = OpCode::from(0x08, 0x06f);
}

#[derive(Debug, Read)]
pub struct LeRemoveIsoDataPath {
    pub connection_handle: u16,
    pub data_path_direction: u8,
}

#[test]
fn le_remove_iso_data_path() {
    let dump = [0x6f, 0x20, 0x03, 0x60, 0x00, 0x01];
    let Command::LeRemoveIsoDataPath(c) = Command::from_bytes(&dump) else { panic!() };
    assert_eq!(c.connection_handle, 0x60);
    assert_eq!(c.data_path_direction, 0x01);
}

}
