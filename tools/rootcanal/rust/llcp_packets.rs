/// @generated rust packets from llcp_packets.pdl.
use bytes::{Buf, BufMut, Bytes, BytesMut};
use std::convert::{TryFrom, TryInto};
use std::cell::Cell;
use std::fmt;
use std::result::Result;
use pdl_runtime::{DecodeError, EncodeError, Packet};
/// Private prevents users from creating arbitrary scalar values
/// in situations where the value needs to be validated.
/// Users can freely deref the value, but only the backend
/// may create it.
#[derive(Clone, Copy, Hash, PartialEq, Eq, PartialOrd, Ord)]
pub struct Private<T>(T);
impl<T> std::ops::Deref for Private<T> {
    type Target = T;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}
impl<T: std::fmt::Debug> std::fmt::Debug for Private<T> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        T::fmt(&self.0, f)
    }
}
#[repr(u64)]
#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(try_from = "u8", into = "u8"))]
pub enum Opcode {
    LlConnectionUpdateInd = 0x0,
    LlChannelMapInd = 0x1,
    LlTerminateInd = 0x2,
    LlEncReq = 0x3,
    LlEncRsp = 0x4,
    LlStartEncReq = 0x5,
    LlStartEncRsp = 0x6,
    LlUnknownRsp = 0x7,
    LlFeatureReq = 0x8,
    LlFeatureRsp = 0x9,
    LlPauseEncReq = 0xa,
    LlPauseEncRsp = 0xb,
    LlVersionInd = 0xc,
    LlRejectInd = 0xd,
    LlPeripheralFeatureReq = 0xe,
    LlConnectionParamReq = 0xf,
    LlConnectionParamRsp = 0x10,
    LlRejectExtInd = 0x11,
    LlPingReq = 0x12,
    LlPingRsp = 0x13,
    LlLengthReq = 0x14,
    LlLengthRsp = 0x15,
    LlPhyReq = 0x16,
    LlPhyRsp = 0x17,
    LlPhyUpdateInd = 0x18,
    LlMinUsedChannelsInd = 0x19,
    LlCteReq = 0x1a,
    LlCteRsp = 0x1b,
    LlPeriodicSyncInd = 0x1c,
    LlClockAccuracyReq = 0x1d,
    LlClockAccuracyRsp = 0x1e,
    LlCisReq = 0x1f,
    LlCisRsp = 0x20,
    LlCisInd = 0x21,
    LlCisTerminateInd = 0x22,
    LlPowerControlReq = 0x23,
    LlPowerControlRsp = 0x24,
    LlPowerChangeInd = 0x25,
    LlSubrateReq = 0x26,
    LlSubrateInd = 0x27,
    LlChannelReportingInd = 0x28,
    LlChannelStatusInd = 0x29,
}
impl TryFrom<u8> for Opcode {
    type Error = u8;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0x0 => Ok(Opcode::LlConnectionUpdateInd),
            0x1 => Ok(Opcode::LlChannelMapInd),
            0x2 => Ok(Opcode::LlTerminateInd),
            0x3 => Ok(Opcode::LlEncReq),
            0x4 => Ok(Opcode::LlEncRsp),
            0x5 => Ok(Opcode::LlStartEncReq),
            0x6 => Ok(Opcode::LlStartEncRsp),
            0x7 => Ok(Opcode::LlUnknownRsp),
            0x8 => Ok(Opcode::LlFeatureReq),
            0x9 => Ok(Opcode::LlFeatureRsp),
            0xa => Ok(Opcode::LlPauseEncReq),
            0xb => Ok(Opcode::LlPauseEncRsp),
            0xc => Ok(Opcode::LlVersionInd),
            0xd => Ok(Opcode::LlRejectInd),
            0xe => Ok(Opcode::LlPeripheralFeatureReq),
            0xf => Ok(Opcode::LlConnectionParamReq),
            0x10 => Ok(Opcode::LlConnectionParamRsp),
            0x11 => Ok(Opcode::LlRejectExtInd),
            0x12 => Ok(Opcode::LlPingReq),
            0x13 => Ok(Opcode::LlPingRsp),
            0x14 => Ok(Opcode::LlLengthReq),
            0x15 => Ok(Opcode::LlLengthRsp),
            0x16 => Ok(Opcode::LlPhyReq),
            0x17 => Ok(Opcode::LlPhyRsp),
            0x18 => Ok(Opcode::LlPhyUpdateInd),
            0x19 => Ok(Opcode::LlMinUsedChannelsInd),
            0x1a => Ok(Opcode::LlCteReq),
            0x1b => Ok(Opcode::LlCteRsp),
            0x1c => Ok(Opcode::LlPeriodicSyncInd),
            0x1d => Ok(Opcode::LlClockAccuracyReq),
            0x1e => Ok(Opcode::LlClockAccuracyRsp),
            0x1f => Ok(Opcode::LlCisReq),
            0x20 => Ok(Opcode::LlCisRsp),
            0x21 => Ok(Opcode::LlCisInd),
            0x22 => Ok(Opcode::LlCisTerminateInd),
            0x23 => Ok(Opcode::LlPowerControlReq),
            0x24 => Ok(Opcode::LlPowerControlRsp),
            0x25 => Ok(Opcode::LlPowerChangeInd),
            0x26 => Ok(Opcode::LlSubrateReq),
            0x27 => Ok(Opcode::LlSubrateInd),
            0x28 => Ok(Opcode::LlChannelReportingInd),
            0x29 => Ok(Opcode::LlChannelStatusInd),
            _ => Err(value),
        }
    }
}
impl From<&Opcode> for u8 {
    fn from(value: &Opcode) -> Self {
        match value {
            Opcode::LlConnectionUpdateInd => 0x0,
            Opcode::LlChannelMapInd => 0x1,
            Opcode::LlTerminateInd => 0x2,
            Opcode::LlEncReq => 0x3,
            Opcode::LlEncRsp => 0x4,
            Opcode::LlStartEncReq => 0x5,
            Opcode::LlStartEncRsp => 0x6,
            Opcode::LlUnknownRsp => 0x7,
            Opcode::LlFeatureReq => 0x8,
            Opcode::LlFeatureRsp => 0x9,
            Opcode::LlPauseEncReq => 0xa,
            Opcode::LlPauseEncRsp => 0xb,
            Opcode::LlVersionInd => 0xc,
            Opcode::LlRejectInd => 0xd,
            Opcode::LlPeripheralFeatureReq => 0xe,
            Opcode::LlConnectionParamReq => 0xf,
            Opcode::LlConnectionParamRsp => 0x10,
            Opcode::LlRejectExtInd => 0x11,
            Opcode::LlPingReq => 0x12,
            Opcode::LlPingRsp => 0x13,
            Opcode::LlLengthReq => 0x14,
            Opcode::LlLengthRsp => 0x15,
            Opcode::LlPhyReq => 0x16,
            Opcode::LlPhyRsp => 0x17,
            Opcode::LlPhyUpdateInd => 0x18,
            Opcode::LlMinUsedChannelsInd => 0x19,
            Opcode::LlCteReq => 0x1a,
            Opcode::LlCteRsp => 0x1b,
            Opcode::LlPeriodicSyncInd => 0x1c,
            Opcode::LlClockAccuracyReq => 0x1d,
            Opcode::LlClockAccuracyRsp => 0x1e,
            Opcode::LlCisReq => 0x1f,
            Opcode::LlCisRsp => 0x20,
            Opcode::LlCisInd => 0x21,
            Opcode::LlCisTerminateInd => 0x22,
            Opcode::LlPowerControlReq => 0x23,
            Opcode::LlPowerControlRsp => 0x24,
            Opcode::LlPowerChangeInd => 0x25,
            Opcode::LlSubrateReq => 0x26,
            Opcode::LlSubrateInd => 0x27,
            Opcode::LlChannelReportingInd => 0x28,
            Opcode::LlChannelStatusInd => 0x29,
        }
    }
}
impl From<Opcode> for u8 {
    fn from(value: Opcode) -> Self {
        (&value).into()
    }
}
impl From<Opcode> for i16 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for i32 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for i64 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u16 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u32 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u64 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum LlcpPacketDataChild {
    ConnectionUpdateInd(ConnectionUpdateIndData),
    ChannelMapInd(ChannelMapIndData),
    TerminateInd(TerminateIndData),
    EncReq(EncReqData),
    EncRsp(EncRspData),
    StartEncReq(StartEncReqData),
    StartEncRsp(StartEncRspData),
    UnknownRsp(UnknownRspData),
    FeatureReq(FeatureReqData),
    FeatureRsp(FeatureRspData),
    PauseEncReq(PauseEncReqData),
    PauseEncRsp(PauseEncRspData),
    VersionInd(VersionIndData),
    RejectInd(RejectIndData),
    PeripheralFeatureReq(PeripheralFeatureReqData),
    ConnectionParamReq(ConnectionParamReqData),
    ConnectionParamRsp(ConnectionParamRspData),
    RejectExtInd(RejectExtIndData),
    PingReq(PingReqData),
    PingRsp(PingRspData),
    LengthReq(LengthReqData),
    LengthRsp(LengthRspData),
    PhyReq(PhyReqData),
    PhyRsp(PhyRspData),
    PhyUpdateInd(PhyUpdateIndData),
    MinUsedChannelsInd(MinUsedChannelsIndData),
    CteReq(CteReqData),
    CteRsp(CteRspData),
    PeriodicSyncInd(PeriodicSyncIndData),
    ClockAccuracyReq(ClockAccuracyReqData),
    ClockAccuracyRsp(ClockAccuracyRspData),
    CisReq(CisReqData),
    CisRsp(CisRspData),
    CisInd(CisIndData),
    CisTerminateInd(CisTerminateIndData),
    PowerControlReq(PowerControlReqData),
    PowerControlRsp(PowerControlRspData),
    PowerChangeInd(PowerChangeIndData),
    SubrateReq(SubrateReqData),
    SubrateInd(SubrateIndData),
    ChannelReportingInd(ChannelReportingIndData),
    ChannelStatusInd(ChannelStatusIndData),
    Payload(Bytes),
    None,
}
impl LlcpPacketDataChild {
    fn get_total_size(&self) -> usize {
        match self {
            LlcpPacketDataChild::ConnectionUpdateInd(value) => value.get_total_size(),
            LlcpPacketDataChild::ChannelMapInd(value) => value.get_total_size(),
            LlcpPacketDataChild::TerminateInd(value) => value.get_total_size(),
            LlcpPacketDataChild::EncReq(value) => value.get_total_size(),
            LlcpPacketDataChild::EncRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::StartEncReq(value) => value.get_total_size(),
            LlcpPacketDataChild::StartEncRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::UnknownRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::FeatureReq(value) => value.get_total_size(),
            LlcpPacketDataChild::FeatureRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::PauseEncReq(value) => value.get_total_size(),
            LlcpPacketDataChild::PauseEncRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::VersionInd(value) => value.get_total_size(),
            LlcpPacketDataChild::RejectInd(value) => value.get_total_size(),
            LlcpPacketDataChild::PeripheralFeatureReq(value) => value.get_total_size(),
            LlcpPacketDataChild::ConnectionParamReq(value) => value.get_total_size(),
            LlcpPacketDataChild::ConnectionParamRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::RejectExtInd(value) => value.get_total_size(),
            LlcpPacketDataChild::PingReq(value) => value.get_total_size(),
            LlcpPacketDataChild::PingRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::LengthReq(value) => value.get_total_size(),
            LlcpPacketDataChild::LengthRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::PhyReq(value) => value.get_total_size(),
            LlcpPacketDataChild::PhyRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::PhyUpdateInd(value) => value.get_total_size(),
            LlcpPacketDataChild::MinUsedChannelsInd(value) => value.get_total_size(),
            LlcpPacketDataChild::CteReq(value) => value.get_total_size(),
            LlcpPacketDataChild::CteRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::PeriodicSyncInd(value) => value.get_total_size(),
            LlcpPacketDataChild::ClockAccuracyReq(value) => value.get_total_size(),
            LlcpPacketDataChild::ClockAccuracyRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::CisReq(value) => value.get_total_size(),
            LlcpPacketDataChild::CisRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::CisInd(value) => value.get_total_size(),
            LlcpPacketDataChild::CisTerminateInd(value) => value.get_total_size(),
            LlcpPacketDataChild::PowerControlReq(value) => value.get_total_size(),
            LlcpPacketDataChild::PowerControlRsp(value) => value.get_total_size(),
            LlcpPacketDataChild::PowerChangeInd(value) => value.get_total_size(),
            LlcpPacketDataChild::SubrateReq(value) => value.get_total_size(),
            LlcpPacketDataChild::SubrateInd(value) => value.get_total_size(),
            LlcpPacketDataChild::ChannelReportingInd(value) => value.get_total_size(),
            LlcpPacketDataChild::ChannelStatusInd(value) => value.get_total_size(),
            LlcpPacketDataChild::Payload(bytes) => bytes.len(),
            LlcpPacketDataChild::None => 0,
        }
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum LlcpPacketChild {
    ConnectionUpdateInd(ConnectionUpdateInd),
    ChannelMapInd(ChannelMapInd),
    TerminateInd(TerminateInd),
    EncReq(EncReq),
    EncRsp(EncRsp),
    StartEncReq(StartEncReq),
    StartEncRsp(StartEncRsp),
    UnknownRsp(UnknownRsp),
    FeatureReq(FeatureReq),
    FeatureRsp(FeatureRsp),
    PauseEncReq(PauseEncReq),
    PauseEncRsp(PauseEncRsp),
    VersionInd(VersionInd),
    RejectInd(RejectInd),
    PeripheralFeatureReq(PeripheralFeatureReq),
    ConnectionParamReq(ConnectionParamReq),
    ConnectionParamRsp(ConnectionParamRsp),
    RejectExtInd(RejectExtInd),
    PingReq(PingReq),
    PingRsp(PingRsp),
    LengthReq(LengthReq),
    LengthRsp(LengthRsp),
    PhyReq(PhyReq),
    PhyRsp(PhyRsp),
    PhyUpdateInd(PhyUpdateInd),
    MinUsedChannelsInd(MinUsedChannelsInd),
    CteReq(CteReq),
    CteRsp(CteRsp),
    PeriodicSyncInd(PeriodicSyncInd),
    ClockAccuracyReq(ClockAccuracyReq),
    ClockAccuracyRsp(ClockAccuracyRsp),
    CisReq(CisReq),
    CisRsp(CisRsp),
    CisInd(CisInd),
    CisTerminateInd(CisTerminateInd),
    PowerControlReq(PowerControlReq),
    PowerControlRsp(PowerControlRsp),
    PowerChangeInd(PowerChangeInd),
    SubrateReq(SubrateReq),
    SubrateInd(SubrateInd),
    ChannelReportingInd(ChannelReportingInd),
    ChannelStatusInd(ChannelStatusInd),
    Payload(Bytes),
    None,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LlcpPacketData {
    opcode: Opcode,
    child: LlcpPacketDataChild,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LlcpPacket {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LlcpPacketBuilder {
    pub opcode: Opcode,
    pub payload: Option<Bytes>,
}
impl LlcpPacketData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LlcpPacket",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let opcode = Opcode::try_from(bytes.get_mut().get_u8())
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "LlcpPacket",
                field: "opcode",
                value: unknown_val as u64,
                type_: "Opcode",
            })?;
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        let child = match (opcode) {
            (Opcode::LlConnectionUpdateInd) if ConnectionUpdateIndData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = ConnectionUpdateIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ConnectionUpdateInd(child_data)
            }
            (Opcode::LlChannelMapInd) if ChannelMapIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = ChannelMapIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ChannelMapInd(child_data)
            }
            (Opcode::LlTerminateInd) if TerminateIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = TerminateIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::TerminateInd(child_data)
            }
            (Opcode::LlEncReq) if EncReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = EncReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::EncReq(child_data)
            }
            (Opcode::LlEncRsp) if EncRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = EncRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::EncRsp(child_data)
            }
            (Opcode::LlStartEncReq) if StartEncReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = StartEncReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::StartEncReq(child_data)
            }
            (Opcode::LlStartEncRsp) if StartEncRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = StartEncRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::StartEncRsp(child_data)
            }
            (Opcode::LlUnknownRsp) if UnknownRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = UnknownRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::UnknownRsp(child_data)
            }
            (Opcode::LlFeatureReq) if FeatureReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = FeatureReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::FeatureReq(child_data)
            }
            (Opcode::LlFeatureRsp) if FeatureRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = FeatureRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::FeatureRsp(child_data)
            }
            (Opcode::LlPauseEncReq) if PauseEncReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PauseEncReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PauseEncReq(child_data)
            }
            (Opcode::LlPauseEncRsp) if PauseEncRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PauseEncRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PauseEncRsp(child_data)
            }
            (Opcode::LlVersionInd) if VersionIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = VersionIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::VersionInd(child_data)
            }
            (Opcode::LlRejectInd) if RejectIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = RejectIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::RejectInd(child_data)
            }
            (Opcode::LlPeripheralFeatureReq) if PeripheralFeatureReqData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = PeripheralFeatureReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PeripheralFeatureReq(child_data)
            }
            (Opcode::LlConnectionParamReq) if ConnectionParamReqData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = ConnectionParamReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ConnectionParamReq(child_data)
            }
            (Opcode::LlConnectionParamRsp) if ConnectionParamRspData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = ConnectionParamRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ConnectionParamRsp(child_data)
            }
            (Opcode::LlRejectExtInd) if RejectExtIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = RejectExtIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::RejectExtInd(child_data)
            }
            (Opcode::LlPingReq) if PingReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PingReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PingReq(child_data)
            }
            (Opcode::LlPingRsp) if PingRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PingRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PingRsp(child_data)
            }
            (Opcode::LlLengthReq) if LengthReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = LengthReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::LengthReq(child_data)
            }
            (Opcode::LlLengthRsp) if LengthRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = LengthRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::LengthRsp(child_data)
            }
            (Opcode::LlPhyReq) if PhyReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PhyReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PhyReq(child_data)
            }
            (Opcode::LlPhyRsp) if PhyRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PhyRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PhyRsp(child_data)
            }
            (Opcode::LlPhyUpdateInd) if PhyUpdateIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PhyUpdateIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PhyUpdateInd(child_data)
            }
            (Opcode::LlMinUsedChannelsInd) if MinUsedChannelsIndData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = MinUsedChannelsIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::MinUsedChannelsInd(child_data)
            }
            (Opcode::LlCteReq) if CteReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CteReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CteReq(child_data)
            }
            (Opcode::LlCteRsp) if CteRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CteRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CteRsp(child_data)
            }
            (Opcode::LlPeriodicSyncInd) if PeriodicSyncIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PeriodicSyncIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PeriodicSyncInd(child_data)
            }
            (Opcode::LlClockAccuracyReq) if ClockAccuracyReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = ClockAccuracyReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ClockAccuracyReq(child_data)
            }
            (Opcode::LlClockAccuracyRsp) if ClockAccuracyRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = ClockAccuracyRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ClockAccuracyRsp(child_data)
            }
            (Opcode::LlCisReq) if CisReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CisReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CisReq(child_data)
            }
            (Opcode::LlCisRsp) if CisRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CisRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CisRsp(child_data)
            }
            (Opcode::LlCisInd) if CisIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CisIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CisInd(child_data)
            }
            (Opcode::LlCisTerminateInd) if CisTerminateIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CisTerminateIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::CisTerminateInd(child_data)
            }
            (Opcode::LlPowerControlReq) if PowerControlReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PowerControlReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PowerControlReq(child_data)
            }
            (Opcode::LlPowerControlRsp) if PowerControlRspData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PowerControlRspData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PowerControlRsp(child_data)
            }
            (Opcode::LlPowerChangeInd) if PowerChangeIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PowerChangeIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::PowerChangeInd(child_data)
            }
            (Opcode::LlSubrateReq) if SubrateReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = SubrateReqData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::SubrateReq(child_data)
            }
            (Opcode::LlSubrateInd) if SubrateIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = SubrateIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::SubrateInd(child_data)
            }
            (Opcode::LlChannelReportingInd) if ChannelReportingIndData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = ChannelReportingIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ChannelReportingInd(child_data)
            }
            (Opcode::LlChannelStatusInd) if ChannelStatusIndData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = ChannelStatusIndData::parse_inner(&mut cell)?;
                LlcpPacketDataChild::ChannelStatusInd(child_data)
            }
            _ if !payload.is_empty() => {
                LlcpPacketDataChild::Payload(Bytes::copy_from_slice(payload))
            }
            _ => LlcpPacketDataChild::None,
        };
        Ok(Self { opcode, child })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(u8::from(self.opcode));
        match &self.child {
            LlcpPacketDataChild::ConnectionUpdateInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ChannelMapInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::TerminateInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::EncReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::EncRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::StartEncReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::StartEncRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::UnknownRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::FeatureReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::FeatureRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PauseEncReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PauseEncRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::VersionInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::RejectInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PeripheralFeatureReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ConnectionParamReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ConnectionParamRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::RejectExtInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PingReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PingRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::LengthReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::LengthRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PhyReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PhyRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PhyUpdateInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::MinUsedChannelsInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CteReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CteRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PeriodicSyncInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ClockAccuracyReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ClockAccuracyRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CisReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CisRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CisInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::CisTerminateInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PowerControlReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PowerControlRsp(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::PowerChangeInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::SubrateReq(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::SubrateInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ChannelReportingInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::ChannelStatusInd(child) => child.write_to(buffer)?,
            LlcpPacketDataChild::Payload(payload) => buffer.put_slice(payload),
            LlcpPacketDataChild::None => {}
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1 + self.child.get_total_size()
    }
}
impl Packet for LlcpPacket {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<LlcpPacket> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: LlcpPacket) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<LlcpPacket> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: LlcpPacket) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl LlcpPacket {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    pub fn specialize(&self) -> LlcpPacketChild {
        match &self.llcppacket.child {
            LlcpPacketDataChild::ConnectionUpdateInd(_) => {
                LlcpPacketChild::ConnectionUpdateInd(
                    ConnectionUpdateInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ChannelMapInd(_) => {
                LlcpPacketChild::ChannelMapInd(
                    ChannelMapInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::TerminateInd(_) => {
                LlcpPacketChild::TerminateInd(
                    TerminateInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::EncReq(_) => {
                LlcpPacketChild::EncReq(EncReq::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::EncRsp(_) => {
                LlcpPacketChild::EncRsp(EncRsp::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::StartEncReq(_) => {
                LlcpPacketChild::StartEncReq(
                    StartEncReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::StartEncRsp(_) => {
                LlcpPacketChild::StartEncRsp(
                    StartEncRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::UnknownRsp(_) => {
                LlcpPacketChild::UnknownRsp(
                    UnknownRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::FeatureReq(_) => {
                LlcpPacketChild::FeatureReq(
                    FeatureReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::FeatureRsp(_) => {
                LlcpPacketChild::FeatureRsp(
                    FeatureRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PauseEncReq(_) => {
                LlcpPacketChild::PauseEncReq(
                    PauseEncReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PauseEncRsp(_) => {
                LlcpPacketChild::PauseEncRsp(
                    PauseEncRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::VersionInd(_) => {
                LlcpPacketChild::VersionInd(
                    VersionInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::RejectInd(_) => {
                LlcpPacketChild::RejectInd(
                    RejectInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PeripheralFeatureReq(_) => {
                LlcpPacketChild::PeripheralFeatureReq(
                    PeripheralFeatureReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ConnectionParamReq(_) => {
                LlcpPacketChild::ConnectionParamReq(
                    ConnectionParamReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ConnectionParamRsp(_) => {
                LlcpPacketChild::ConnectionParamRsp(
                    ConnectionParamRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::RejectExtInd(_) => {
                LlcpPacketChild::RejectExtInd(
                    RejectExtInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PingReq(_) => {
                LlcpPacketChild::PingReq(PingReq::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::PingRsp(_) => {
                LlcpPacketChild::PingRsp(PingRsp::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::LengthReq(_) => {
                LlcpPacketChild::LengthReq(
                    LengthReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::LengthRsp(_) => {
                LlcpPacketChild::LengthRsp(
                    LengthRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PhyReq(_) => {
                LlcpPacketChild::PhyReq(PhyReq::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::PhyRsp(_) => {
                LlcpPacketChild::PhyRsp(PhyRsp::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::PhyUpdateInd(_) => {
                LlcpPacketChild::PhyUpdateInd(
                    PhyUpdateInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::MinUsedChannelsInd(_) => {
                LlcpPacketChild::MinUsedChannelsInd(
                    MinUsedChannelsInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::CteReq(_) => {
                LlcpPacketChild::CteReq(CteReq::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::CteRsp(_) => {
                LlcpPacketChild::CteRsp(CteRsp::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::PeriodicSyncInd(_) => {
                LlcpPacketChild::PeriodicSyncInd(
                    PeriodicSyncInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ClockAccuracyReq(_) => {
                LlcpPacketChild::ClockAccuracyReq(
                    ClockAccuracyReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ClockAccuracyRsp(_) => {
                LlcpPacketChild::ClockAccuracyRsp(
                    ClockAccuracyRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::CisReq(_) => {
                LlcpPacketChild::CisReq(CisReq::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::CisRsp(_) => {
                LlcpPacketChild::CisRsp(CisRsp::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::CisInd(_) => {
                LlcpPacketChild::CisInd(CisInd::new(self.llcppacket.clone()).unwrap())
            }
            LlcpPacketDataChild::CisTerminateInd(_) => {
                LlcpPacketChild::CisTerminateInd(
                    CisTerminateInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PowerControlReq(_) => {
                LlcpPacketChild::PowerControlReq(
                    PowerControlReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PowerControlRsp(_) => {
                LlcpPacketChild::PowerControlRsp(
                    PowerControlRsp::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::PowerChangeInd(_) => {
                LlcpPacketChild::PowerChangeInd(
                    PowerChangeInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::SubrateReq(_) => {
                LlcpPacketChild::SubrateReq(
                    SubrateReq::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::SubrateInd(_) => {
                LlcpPacketChild::SubrateInd(
                    SubrateInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ChannelReportingInd(_) => {
                LlcpPacketChild::ChannelReportingInd(
                    ChannelReportingInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::ChannelStatusInd(_) => {
                LlcpPacketChild::ChannelStatusInd(
                    ChannelStatusInd::new(self.llcppacket.clone()).unwrap(),
                )
            }
            LlcpPacketDataChild::Payload(payload) => {
                LlcpPacketChild::Payload(payload.clone())
            }
            LlcpPacketDataChild::None => LlcpPacketChild::None,
        }
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        Ok(Self { llcppacket })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl LlcpPacketBuilder {
    pub fn build(self) -> LlcpPacket {
        let llcppacket = LlcpPacketData {
            opcode: self.opcode,
            child: match self.payload {
                None => LlcpPacketDataChild::None,
                Some(bytes) => LlcpPacketDataChild::Payload(bytes),
            },
        };
        LlcpPacket::new(llcppacket).unwrap()
    }
}
impl From<LlcpPacketBuilder> for LlcpPacket {
    fn from(builder: LlcpPacketBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionUpdateIndData {
    window_size: u8,
    window_offset: u16,
    interval: u16,
    latency: u16,
    timeout: u16,
    instant: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionUpdateInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    connectionupdateind: ConnectionUpdateIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionUpdateIndBuilder {
    pub instant: u16,
    pub interval: u16,
    pub latency: u16,
    pub timeout: u16,
    pub window_offset: u16,
    pub window_size: u8,
}
impl ConnectionUpdateIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 11
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let window_size = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let window_offset = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let interval = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let latency = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let timeout = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let instant = bytes.get_mut().get_u16_le();
        Ok(Self {
            window_size,
            window_offset,
            interval,
            latency,
            timeout,
            instant,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.window_size);
        buffer.put_u16_le(self.window_offset);
        buffer.put_u16_le(self.interval);
        buffer.put_u16_le(self.latency);
        buffer.put_u16_le(self.timeout);
        buffer.put_u16_le(self.instant);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        11
    }
}
impl Packet for ConnectionUpdateInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ConnectionUpdateInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ConnectionUpdateInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ConnectionUpdateInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ConnectionUpdateInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ConnectionUpdateInd> for LlcpPacket {
    fn from(packet: ConnectionUpdateInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ConnectionUpdateInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ConnectionUpdateInd, Self::Error> {
        ConnectionUpdateInd::new(packet.llcppacket)
    }
}
impl ConnectionUpdateInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let connectionupdateind = match &llcppacket.child {
            LlcpPacketDataChild::ConnectionUpdateInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ConnectionUpdateInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            connectionupdateind,
        })
    }
    pub fn get_instant(&self) -> u16 {
        self.connectionupdateind.instant
    }
    pub fn get_interval(&self) -> u16 {
        self.connectionupdateind.interval
    }
    pub fn get_latency(&self) -> u16 {
        self.connectionupdateind.latency
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_timeout(&self) -> u16 {
        self.connectionupdateind.timeout
    }
    pub fn get_window_offset(&self) -> u16 {
        self.connectionupdateind.window_offset
    }
    pub fn get_window_size(&self) -> u8 {
        self.connectionupdateind.window_size
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.connectionupdateind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ConnectionUpdateIndBuilder {
    pub fn build(self) -> ConnectionUpdateInd {
        let connectionupdateind = ConnectionUpdateIndData {
            instant: self.instant,
            interval: self.interval,
            latency: self.latency,
            timeout: self.timeout,
            window_offset: self.window_offset,
            window_size: self.window_size,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlConnectionUpdateInd,
            child: LlcpPacketDataChild::ConnectionUpdateInd(connectionupdateind),
        };
        ConnectionUpdateInd::new(llcppacket).unwrap()
    }
}
impl From<ConnectionUpdateIndBuilder> for LlcpPacket {
    fn from(builder: ConnectionUpdateIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ConnectionUpdateIndBuilder> for ConnectionUpdateInd {
    fn from(builder: ConnectionUpdateIndBuilder) -> ConnectionUpdateInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelMapIndData {
    channel_map: u64,
    instant: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelMapInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    channelmapind: ChannelMapIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelMapIndBuilder {
    pub channel_map: u64,
    pub instant: u16,
}
impl ChannelMapIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 7
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 5 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelMapInd",
                wanted: 5,
                got: bytes.get().remaining(),
            });
        }
        let channel_map = bytes.get_mut().get_uint_le(5);
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelMapInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let instant = bytes.get_mut().get_u16_le();
        Ok(Self { channel_map, instant })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        if self.channel_map > 0xff_ffff_ffff_u64 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "ChannelMapInd",
                field: "channel_map",
                value: self.channel_map as u64,
                maximum_value: 0xff_ffff_ffff_u64,
            });
        }
        buffer.put_uint_le(self.channel_map, 5);
        buffer.put_u16_le(self.instant);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        7
    }
}
impl Packet for ChannelMapInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ChannelMapInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ChannelMapInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ChannelMapInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ChannelMapInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ChannelMapInd> for LlcpPacket {
    fn from(packet: ChannelMapInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ChannelMapInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ChannelMapInd, Self::Error> {
        ChannelMapInd::new(packet.llcppacket)
    }
}
impl ChannelMapInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let channelmapind = match &llcppacket.child {
            LlcpPacketDataChild::ChannelMapInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ChannelMapInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, channelmapind })
    }
    pub fn get_channel_map(&self) -> u64 {
        self.channelmapind.channel_map
    }
    pub fn get_instant(&self) -> u16 {
        self.channelmapind.instant
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.channelmapind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ChannelMapIndBuilder {
    pub fn build(self) -> ChannelMapInd {
        let channelmapind = ChannelMapIndData {
            channel_map: self.channel_map,
            instant: self.instant,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlChannelMapInd,
            child: LlcpPacketDataChild::ChannelMapInd(channelmapind),
        };
        ChannelMapInd::new(llcppacket).unwrap()
    }
}
impl From<ChannelMapIndBuilder> for LlcpPacket {
    fn from(builder: ChannelMapIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ChannelMapIndBuilder> for ChannelMapInd {
    fn from(builder: ChannelMapIndBuilder) -> ChannelMapInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct TerminateIndData {
    error_code: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct TerminateInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    terminateind: TerminateIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct TerminateIndBuilder {
    pub error_code: u8,
}
impl TerminateIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "TerminateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u8();
        Ok(Self { error_code })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.error_code);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for TerminateInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<TerminateInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: TerminateInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<TerminateInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: TerminateInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<TerminateInd> for LlcpPacket {
    fn from(packet: TerminateInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for TerminateInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<TerminateInd, Self::Error> {
        TerminateInd::new(packet.llcppacket)
    }
}
impl TerminateInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let terminateind = match &llcppacket.child {
            LlcpPacketDataChild::TerminateInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::TerminateInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, terminateind })
    }
    pub fn get_error_code(&self) -> u8 {
        self.terminateind.error_code
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.terminateind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl TerminateIndBuilder {
    pub fn build(self) -> TerminateInd {
        let terminateind = TerminateIndData {
            error_code: self.error_code,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlTerminateInd,
            child: LlcpPacketDataChild::TerminateInd(terminateind),
        };
        TerminateInd::new(llcppacket).unwrap()
    }
}
impl From<TerminateIndBuilder> for LlcpPacket {
    fn from(builder: TerminateIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<TerminateIndBuilder> for TerminateInd {
    fn from(builder: TerminateIndBuilder) -> TerminateInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncReqData {
    rand: u64,
    ediv: u16,
    skd_c: u64,
    iv_c: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encreq: EncReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncReqBuilder {
    pub ediv: u16,
    pub iv_c: u16,
    pub rand: u64,
    pub skd_c: u64,
}
impl EncReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 20
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncReq",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let rand = bytes.get_mut().get_u64_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let ediv = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncReq",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let skd_c = bytes.get_mut().get_u64_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let iv_c = bytes.get_mut().get_u16_le();
        Ok(Self { rand, ediv, skd_c, iv_c })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u64_le(self.rand);
        buffer.put_u16_le(self.ediv);
        buffer.put_u64_le(self.skd_c);
        buffer.put_u16_le(self.iv_c);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        20
    }
}
impl Packet for EncReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncReq> for LlcpPacket {
    fn from(packet: EncReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for EncReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<EncReq, Self::Error> {
        EncReq::new(packet.llcppacket)
    }
}
impl EncReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let encreq = match &llcppacket.child {
            LlcpPacketDataChild::EncReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::EncReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, encreq })
    }
    pub fn get_ediv(&self) -> u16 {
        self.encreq.ediv
    }
    pub fn get_iv_c(&self) -> u16 {
        self.encreq.iv_c
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_rand(&self) -> u64 {
        self.encreq.rand
    }
    pub fn get_skd_c(&self) -> u64 {
        self.encreq.skd_c
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl EncReqBuilder {
    pub fn build(self) -> EncReq {
        let encreq = EncReqData {
            ediv: self.ediv,
            iv_c: self.iv_c,
            rand: self.rand,
            skd_c: self.skd_c,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlEncReq,
            child: LlcpPacketDataChild::EncReq(encreq),
        };
        EncReq::new(llcppacket).unwrap()
    }
}
impl From<EncReqBuilder> for LlcpPacket {
    fn from(builder: EncReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<EncReqBuilder> for EncReq {
    fn from(builder: EncReqBuilder) -> EncReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncRspData {
    skd_p: u64,
    iv_p: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encrsp: EncRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncRspBuilder {
    pub iv_p: u16,
    pub skd_p: u64,
}
impl EncRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncRsp",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let skd_p = bytes.get_mut().get_u64_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let iv_p = bytes.get_mut().get_u16_le();
        Ok(Self { skd_p, iv_p })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u64_le(self.skd_p);
        buffer.put_u16_le(self.iv_p);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        10
    }
}
impl Packet for EncRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncRsp> for LlcpPacket {
    fn from(packet: EncRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for EncRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<EncRsp, Self::Error> {
        EncRsp::new(packet.llcppacket)
    }
}
impl EncRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let encrsp = match &llcppacket.child {
            LlcpPacketDataChild::EncRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::EncRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, encrsp })
    }
    pub fn get_iv_p(&self) -> u16 {
        self.encrsp.iv_p
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_skd_p(&self) -> u64 {
        self.encrsp.skd_p
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl EncRspBuilder {
    pub fn build(self) -> EncRsp {
        let encrsp = EncRspData {
            iv_p: self.iv_p,
            skd_p: self.skd_p,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlEncRsp,
            child: LlcpPacketDataChild::EncRsp(encrsp),
        };
        EncRsp::new(llcppacket).unwrap()
    }
}
impl From<EncRspBuilder> for LlcpPacket {
    fn from(builder: EncRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<EncRspBuilder> for EncRsp {
    fn from(builder: EncRspBuilder) -> EncRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncReqData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    startencreq: StartEncReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncReqBuilder {}
impl StartEncReqData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for StartEncReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<StartEncReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: StartEncReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<StartEncReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: StartEncReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<StartEncReq> for LlcpPacket {
    fn from(packet: StartEncReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for StartEncReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<StartEncReq, Self::Error> {
        StartEncReq::new(packet.llcppacket)
    }
}
impl StartEncReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let startencreq = match &llcppacket.child {
            LlcpPacketDataChild::StartEncReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::StartEncReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, startencreq })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.startencreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl StartEncReqBuilder {
    pub fn build(self) -> StartEncReq {
        let startencreq = StartEncReqData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlStartEncReq,
            child: LlcpPacketDataChild::StartEncReq(startencreq),
        };
        StartEncReq::new(llcppacket).unwrap()
    }
}
impl From<StartEncReqBuilder> for LlcpPacket {
    fn from(builder: StartEncReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<StartEncReqBuilder> for StartEncReq {
    fn from(builder: StartEncReqBuilder) -> StartEncReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncRspData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    startencrsp: StartEncRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncRspBuilder {}
impl StartEncRspData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for StartEncRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<StartEncRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: StartEncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<StartEncRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: StartEncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<StartEncRsp> for LlcpPacket {
    fn from(packet: StartEncRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for StartEncRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<StartEncRsp, Self::Error> {
        StartEncRsp::new(packet.llcppacket)
    }
}
impl StartEncRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let startencrsp = match &llcppacket.child {
            LlcpPacketDataChild::StartEncRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::StartEncRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, startencrsp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.startencrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl StartEncRspBuilder {
    pub fn build(self) -> StartEncRsp {
        let startencrsp = StartEncRspData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlStartEncRsp,
            child: LlcpPacketDataChild::StartEncRsp(startencrsp),
        };
        StartEncRsp::new(llcppacket).unwrap()
    }
}
impl From<StartEncRspBuilder> for LlcpPacket {
    fn from(builder: StartEncRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<StartEncRspBuilder> for StartEncRsp {
    fn from(builder: StartEncRspBuilder) -> StartEncRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct UnknownRspData {
    unknown_type: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct UnknownRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    unknownrsp: UnknownRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct UnknownRspBuilder {
    pub unknown_type: u8,
}
impl UnknownRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "UnknownRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let unknown_type = bytes.get_mut().get_u8();
        Ok(Self { unknown_type })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.unknown_type);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for UnknownRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<UnknownRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: UnknownRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<UnknownRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: UnknownRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<UnknownRsp> for LlcpPacket {
    fn from(packet: UnknownRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for UnknownRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<UnknownRsp, Self::Error> {
        UnknownRsp::new(packet.llcppacket)
    }
}
impl UnknownRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let unknownrsp = match &llcppacket.child {
            LlcpPacketDataChild::UnknownRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::UnknownRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, unknownrsp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_unknown_type(&self) -> u8 {
        self.unknownrsp.unknown_type
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.unknownrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl UnknownRspBuilder {
    pub fn build(self) -> UnknownRsp {
        let unknownrsp = UnknownRspData {
            unknown_type: self.unknown_type,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlUnknownRsp,
            child: LlcpPacketDataChild::UnknownRsp(unknownrsp),
        };
        UnknownRsp::new(llcppacket).unwrap()
    }
}
impl From<UnknownRspBuilder> for LlcpPacket {
    fn from(builder: UnknownRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<UnknownRspBuilder> for UnknownRsp {
    fn from(builder: UnknownRspBuilder) -> UnknownRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureReqData {
    feature_set: u64,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    featurereq: FeatureReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureReqBuilder {
    pub feature_set: u64,
}
impl FeatureReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeatureReq",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let feature_set = bytes.get_mut().get_u64_le();
        Ok(Self { feature_set })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u64_le(self.feature_set);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for FeatureReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<FeatureReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: FeatureReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<FeatureReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: FeatureReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<FeatureReq> for LlcpPacket {
    fn from(packet: FeatureReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for FeatureReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<FeatureReq, Self::Error> {
        FeatureReq::new(packet.llcppacket)
    }
}
impl FeatureReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let featurereq = match &llcppacket.child {
            LlcpPacketDataChild::FeatureReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::FeatureReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, featurereq })
    }
    pub fn get_feature_set(&self) -> u64 {
        self.featurereq.feature_set
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.featurereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl FeatureReqBuilder {
    pub fn build(self) -> FeatureReq {
        let featurereq = FeatureReqData {
            feature_set: self.feature_set,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlFeatureReq,
            child: LlcpPacketDataChild::FeatureReq(featurereq),
        };
        FeatureReq::new(llcppacket).unwrap()
    }
}
impl From<FeatureReqBuilder> for LlcpPacket {
    fn from(builder: FeatureReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<FeatureReqBuilder> for FeatureReq {
    fn from(builder: FeatureReqBuilder) -> FeatureReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureRspData {
    feature_set: u64,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    featurersp: FeatureRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeatureRspBuilder {
    pub feature_set: u64,
}
impl FeatureRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeatureRsp",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let feature_set = bytes.get_mut().get_u64_le();
        Ok(Self { feature_set })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u64_le(self.feature_set);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for FeatureRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<FeatureRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: FeatureRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<FeatureRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: FeatureRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<FeatureRsp> for LlcpPacket {
    fn from(packet: FeatureRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for FeatureRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<FeatureRsp, Self::Error> {
        FeatureRsp::new(packet.llcppacket)
    }
}
impl FeatureRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let featurersp = match &llcppacket.child {
            LlcpPacketDataChild::FeatureRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::FeatureRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, featurersp })
    }
    pub fn get_feature_set(&self) -> u64 {
        self.featurersp.feature_set
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.featurersp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl FeatureRspBuilder {
    pub fn build(self) -> FeatureRsp {
        let featurersp = FeatureRspData {
            feature_set: self.feature_set,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlFeatureRsp,
            child: LlcpPacketDataChild::FeatureRsp(featurersp),
        };
        FeatureRsp::new(llcppacket).unwrap()
    }
}
impl From<FeatureRspBuilder> for LlcpPacket {
    fn from(builder: FeatureRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<FeatureRspBuilder> for FeatureRsp {
    fn from(builder: FeatureRspBuilder) -> FeatureRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncReqData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    pauseencreq: PauseEncReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncReqBuilder {}
impl PauseEncReqData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for PauseEncReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PauseEncReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PauseEncReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PauseEncReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PauseEncReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PauseEncReq> for LlcpPacket {
    fn from(packet: PauseEncReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PauseEncReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PauseEncReq, Self::Error> {
        PauseEncReq::new(packet.llcppacket)
    }
}
impl PauseEncReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let pauseencreq = match &llcppacket.child {
            LlcpPacketDataChild::PauseEncReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PauseEncReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, pauseencreq })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.pauseencreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PauseEncReqBuilder {
    pub fn build(self) -> PauseEncReq {
        let pauseencreq = PauseEncReqData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPauseEncReq,
            child: LlcpPacketDataChild::PauseEncReq(pauseencreq),
        };
        PauseEncReq::new(llcppacket).unwrap()
    }
}
impl From<PauseEncReqBuilder> for LlcpPacket {
    fn from(builder: PauseEncReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PauseEncReqBuilder> for PauseEncReq {
    fn from(builder: PauseEncReqBuilder) -> PauseEncReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncRspData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    pauseencrsp: PauseEncRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PauseEncRspBuilder {}
impl PauseEncRspData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for PauseEncRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PauseEncRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PauseEncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PauseEncRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PauseEncRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PauseEncRsp> for LlcpPacket {
    fn from(packet: PauseEncRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PauseEncRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PauseEncRsp, Self::Error> {
        PauseEncRsp::new(packet.llcppacket)
    }
}
impl PauseEncRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let pauseencrsp = match &llcppacket.child {
            LlcpPacketDataChild::PauseEncRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PauseEncRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, pauseencrsp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.pauseencrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PauseEncRspBuilder {
    pub fn build(self) -> PauseEncRsp {
        let pauseencrsp = PauseEncRspData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPauseEncRsp,
            child: LlcpPacketDataChild::PauseEncRsp(pauseencrsp),
        };
        PauseEncRsp::new(llcppacket).unwrap()
    }
}
impl From<PauseEncRspBuilder> for LlcpPacket {
    fn from(builder: PauseEncRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PauseEncRspBuilder> for PauseEncRsp {
    fn from(builder: PauseEncRspBuilder) -> PauseEncRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct VersionIndData {
    version: u8,
    company_identifier: u16,
    subversion: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct VersionInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    versionind: VersionIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct VersionIndBuilder {
    pub company_identifier: u16,
    pub subversion: u16,
    pub version: u8,
}
impl VersionIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 5
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "VersionInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let version = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "VersionInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let company_identifier = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "VersionInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let subversion = bytes.get_mut().get_u16_le();
        Ok(Self {
            version,
            company_identifier,
            subversion,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.version);
        buffer.put_u16_le(self.company_identifier);
        buffer.put_u16_le(self.subversion);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        5
    }
}
impl Packet for VersionInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<VersionInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: VersionInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<VersionInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: VersionInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<VersionInd> for LlcpPacket {
    fn from(packet: VersionInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for VersionInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<VersionInd, Self::Error> {
        VersionInd::new(packet.llcppacket)
    }
}
impl VersionInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let versionind = match &llcppacket.child {
            LlcpPacketDataChild::VersionInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::VersionInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, versionind })
    }
    pub fn get_company_identifier(&self) -> u16 {
        self.versionind.company_identifier
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_subversion(&self) -> u16 {
        self.versionind.subversion
    }
    pub fn get_version(&self) -> u8 {
        self.versionind.version
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.versionind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl VersionIndBuilder {
    pub fn build(self) -> VersionInd {
        let versionind = VersionIndData {
            company_identifier: self.company_identifier,
            subversion: self.subversion,
            version: self.version,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlVersionInd,
            child: LlcpPacketDataChild::VersionInd(versionind),
        };
        VersionInd::new(llcppacket).unwrap()
    }
}
impl From<VersionIndBuilder> for LlcpPacket {
    fn from(builder: VersionIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<VersionIndBuilder> for VersionInd {
    fn from(builder: VersionIndBuilder) -> VersionInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectIndData {
    error_code: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    rejectind: RejectIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectIndBuilder {
    pub error_code: u16,
}
impl RejectIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "RejectInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u16_le();
        Ok(Self { error_code })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.error_code);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for RejectInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<RejectInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: RejectInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<RejectInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: RejectInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<RejectInd> for LlcpPacket {
    fn from(packet: RejectInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for RejectInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<RejectInd, Self::Error> {
        RejectInd::new(packet.llcppacket)
    }
}
impl RejectInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let rejectind = match &llcppacket.child {
            LlcpPacketDataChild::RejectInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::RejectInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, rejectind })
    }
    pub fn get_error_code(&self) -> u16 {
        self.rejectind.error_code
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.rejectind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl RejectIndBuilder {
    pub fn build(self) -> RejectInd {
        let rejectind = RejectIndData {
            error_code: self.error_code,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlRejectInd,
            child: LlcpPacketDataChild::RejectInd(rejectind),
        };
        RejectInd::new(llcppacket).unwrap()
    }
}
impl From<RejectIndBuilder> for LlcpPacket {
    fn from(builder: RejectIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<RejectIndBuilder> for RejectInd {
    fn from(builder: RejectIndBuilder) -> RejectInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeripheralFeatureReqData {
    feature_set: u64,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeripheralFeatureReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    peripheralfeaturereq: PeripheralFeatureReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeripheralFeatureReqBuilder {
    pub feature_set: u64,
}
impl PeripheralFeatureReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeripheralFeatureReq",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let feature_set = bytes.get_mut().get_u64_le();
        Ok(Self { feature_set })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u64_le(self.feature_set);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for PeripheralFeatureReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PeripheralFeatureReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PeripheralFeatureReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PeripheralFeatureReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PeripheralFeatureReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PeripheralFeatureReq> for LlcpPacket {
    fn from(packet: PeripheralFeatureReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PeripheralFeatureReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PeripheralFeatureReq, Self::Error> {
        PeripheralFeatureReq::new(packet.llcppacket)
    }
}
impl PeripheralFeatureReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let peripheralfeaturereq = match &llcppacket.child {
            LlcpPacketDataChild::PeripheralFeatureReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PeripheralFeatureReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            peripheralfeaturereq,
        })
    }
    pub fn get_feature_set(&self) -> u64 {
        self.peripheralfeaturereq.feature_set
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.peripheralfeaturereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PeripheralFeatureReqBuilder {
    pub fn build(self) -> PeripheralFeatureReq {
        let peripheralfeaturereq = PeripheralFeatureReqData {
            feature_set: self.feature_set,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPeripheralFeatureReq,
            child: LlcpPacketDataChild::PeripheralFeatureReq(peripheralfeaturereq),
        };
        PeripheralFeatureReq::new(llcppacket).unwrap()
    }
}
impl From<PeripheralFeatureReqBuilder> for LlcpPacket {
    fn from(builder: PeripheralFeatureReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PeripheralFeatureReqBuilder> for PeripheralFeatureReq {
    fn from(builder: PeripheralFeatureReqBuilder) -> PeripheralFeatureReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamReqData {
    interval_min: u16,
    interval_max: u16,
    latency: u16,
    timeout: u16,
    preferred_periodicity: u8,
    reference_conn_event_count: u16,
    offset0: u16,
    offset1: u16,
    offset2: u16,
    offset3: u16,
    offset4: u16,
    offset5: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    connectionparamreq: ConnectionParamReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamReqBuilder {
    pub interval_max: u16,
    pub interval_min: u16,
    pub latency: u16,
    pub offset0: u16,
    pub offset1: u16,
    pub offset2: u16,
    pub offset3: u16,
    pub offset4: u16,
    pub offset5: u16,
    pub preferred_periodicity: u8,
    pub reference_conn_event_count: u16,
    pub timeout: u16,
}
impl ConnectionParamReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 23
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let interval_min = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let interval_max = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let latency = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let timeout = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let preferred_periodicity = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let reference_conn_event_count = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset0 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset1 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset2 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset3 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset4 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset5 = bytes.get_mut().get_u16_le();
        Ok(Self {
            interval_min,
            interval_max,
            latency,
            timeout,
            preferred_periodicity,
            reference_conn_event_count,
            offset0,
            offset1,
            offset2,
            offset3,
            offset4,
            offset5,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.interval_min);
        buffer.put_u16_le(self.interval_max);
        buffer.put_u16_le(self.latency);
        buffer.put_u16_le(self.timeout);
        buffer.put_u8(self.preferred_periodicity);
        buffer.put_u16_le(self.reference_conn_event_count);
        buffer.put_u16_le(self.offset0);
        buffer.put_u16_le(self.offset1);
        buffer.put_u16_le(self.offset2);
        buffer.put_u16_le(self.offset3);
        buffer.put_u16_le(self.offset4);
        buffer.put_u16_le(self.offset5);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        23
    }
}
impl Packet for ConnectionParamReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ConnectionParamReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ConnectionParamReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ConnectionParamReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ConnectionParamReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ConnectionParamReq> for LlcpPacket {
    fn from(packet: ConnectionParamReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ConnectionParamReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ConnectionParamReq, Self::Error> {
        ConnectionParamReq::new(packet.llcppacket)
    }
}
impl ConnectionParamReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let connectionparamreq = match &llcppacket.child {
            LlcpPacketDataChild::ConnectionParamReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ConnectionParamReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            connectionparamreq,
        })
    }
    pub fn get_interval_max(&self) -> u16 {
        self.connectionparamreq.interval_max
    }
    pub fn get_interval_min(&self) -> u16 {
        self.connectionparamreq.interval_min
    }
    pub fn get_latency(&self) -> u16 {
        self.connectionparamreq.latency
    }
    pub fn get_offset0(&self) -> u16 {
        self.connectionparamreq.offset0
    }
    pub fn get_offset1(&self) -> u16 {
        self.connectionparamreq.offset1
    }
    pub fn get_offset2(&self) -> u16 {
        self.connectionparamreq.offset2
    }
    pub fn get_offset3(&self) -> u16 {
        self.connectionparamreq.offset3
    }
    pub fn get_offset4(&self) -> u16 {
        self.connectionparamreq.offset4
    }
    pub fn get_offset5(&self) -> u16 {
        self.connectionparamreq.offset5
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_preferred_periodicity(&self) -> u8 {
        self.connectionparamreq.preferred_periodicity
    }
    pub fn get_reference_conn_event_count(&self) -> u16 {
        self.connectionparamreq.reference_conn_event_count
    }
    pub fn get_timeout(&self) -> u16 {
        self.connectionparamreq.timeout
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.connectionparamreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ConnectionParamReqBuilder {
    pub fn build(self) -> ConnectionParamReq {
        let connectionparamreq = ConnectionParamReqData {
            interval_max: self.interval_max,
            interval_min: self.interval_min,
            latency: self.latency,
            offset0: self.offset0,
            offset1: self.offset1,
            offset2: self.offset2,
            offset3: self.offset3,
            offset4: self.offset4,
            offset5: self.offset5,
            preferred_periodicity: self.preferred_periodicity,
            reference_conn_event_count: self.reference_conn_event_count,
            timeout: self.timeout,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlConnectionParamReq,
            child: LlcpPacketDataChild::ConnectionParamReq(connectionparamreq),
        };
        ConnectionParamReq::new(llcppacket).unwrap()
    }
}
impl From<ConnectionParamReqBuilder> for LlcpPacket {
    fn from(builder: ConnectionParamReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ConnectionParamReqBuilder> for ConnectionParamReq {
    fn from(builder: ConnectionParamReqBuilder) -> ConnectionParamReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamRspData {
    interval_min: u16,
    interval_max: u16,
    latency: u16,
    timeout: u16,
    preferred_periodicity: u8,
    reference_conn_event_count: u16,
    offset0: u16,
    offset1: u16,
    offset2: u16,
    offset3: u16,
    offset4: u16,
    offset5: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    connectionparamrsp: ConnectionParamRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ConnectionParamRspBuilder {
    pub interval_max: u16,
    pub interval_min: u16,
    pub latency: u16,
    pub offset0: u16,
    pub offset1: u16,
    pub offset2: u16,
    pub offset3: u16,
    pub offset4: u16,
    pub offset5: u16,
    pub preferred_periodicity: u8,
    pub reference_conn_event_count: u16,
    pub timeout: u16,
}
impl ConnectionParamRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 23
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let interval_min = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let interval_max = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let latency = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let timeout = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let preferred_periodicity = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let reference_conn_event_count = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset0 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset1 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset2 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset3 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset4 = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ConnectionParamRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let offset5 = bytes.get_mut().get_u16_le();
        Ok(Self {
            interval_min,
            interval_max,
            latency,
            timeout,
            preferred_periodicity,
            reference_conn_event_count,
            offset0,
            offset1,
            offset2,
            offset3,
            offset4,
            offset5,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.interval_min);
        buffer.put_u16_le(self.interval_max);
        buffer.put_u16_le(self.latency);
        buffer.put_u16_le(self.timeout);
        buffer.put_u8(self.preferred_periodicity);
        buffer.put_u16_le(self.reference_conn_event_count);
        buffer.put_u16_le(self.offset0);
        buffer.put_u16_le(self.offset1);
        buffer.put_u16_le(self.offset2);
        buffer.put_u16_le(self.offset3);
        buffer.put_u16_le(self.offset4);
        buffer.put_u16_le(self.offset5);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        23
    }
}
impl Packet for ConnectionParamRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ConnectionParamRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ConnectionParamRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ConnectionParamRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ConnectionParamRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ConnectionParamRsp> for LlcpPacket {
    fn from(packet: ConnectionParamRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ConnectionParamRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ConnectionParamRsp, Self::Error> {
        ConnectionParamRsp::new(packet.llcppacket)
    }
}
impl ConnectionParamRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let connectionparamrsp = match &llcppacket.child {
            LlcpPacketDataChild::ConnectionParamRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ConnectionParamRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            connectionparamrsp,
        })
    }
    pub fn get_interval_max(&self) -> u16 {
        self.connectionparamrsp.interval_max
    }
    pub fn get_interval_min(&self) -> u16 {
        self.connectionparamrsp.interval_min
    }
    pub fn get_latency(&self) -> u16 {
        self.connectionparamrsp.latency
    }
    pub fn get_offset0(&self) -> u16 {
        self.connectionparamrsp.offset0
    }
    pub fn get_offset1(&self) -> u16 {
        self.connectionparamrsp.offset1
    }
    pub fn get_offset2(&self) -> u16 {
        self.connectionparamrsp.offset2
    }
    pub fn get_offset3(&self) -> u16 {
        self.connectionparamrsp.offset3
    }
    pub fn get_offset4(&self) -> u16 {
        self.connectionparamrsp.offset4
    }
    pub fn get_offset5(&self) -> u16 {
        self.connectionparamrsp.offset5
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_preferred_periodicity(&self) -> u8 {
        self.connectionparamrsp.preferred_periodicity
    }
    pub fn get_reference_conn_event_count(&self) -> u16 {
        self.connectionparamrsp.reference_conn_event_count
    }
    pub fn get_timeout(&self) -> u16 {
        self.connectionparamrsp.timeout
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.connectionparamrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ConnectionParamRspBuilder {
    pub fn build(self) -> ConnectionParamRsp {
        let connectionparamrsp = ConnectionParamRspData {
            interval_max: self.interval_max,
            interval_min: self.interval_min,
            latency: self.latency,
            offset0: self.offset0,
            offset1: self.offset1,
            offset2: self.offset2,
            offset3: self.offset3,
            offset4: self.offset4,
            offset5: self.offset5,
            preferred_periodicity: self.preferred_periodicity,
            reference_conn_event_count: self.reference_conn_event_count,
            timeout: self.timeout,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlConnectionParamRsp,
            child: LlcpPacketDataChild::ConnectionParamRsp(connectionparamrsp),
        };
        ConnectionParamRsp::new(llcppacket).unwrap()
    }
}
impl From<ConnectionParamRspBuilder> for LlcpPacket {
    fn from(builder: ConnectionParamRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ConnectionParamRspBuilder> for ConnectionParamRsp {
    fn from(builder: ConnectionParamRspBuilder) -> ConnectionParamRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectExtIndData {
    reject_opcode: u8,
    error_code: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectExtInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    rejectextind: RejectExtIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct RejectExtIndBuilder {
    pub error_code: u8,
    pub reject_opcode: u8,
}
impl RejectExtIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "RejectExtInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let reject_opcode = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "RejectExtInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u8();
        Ok(Self { reject_opcode, error_code })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.reject_opcode);
        buffer.put_u8(self.error_code);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for RejectExtInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<RejectExtInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: RejectExtInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<RejectExtInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: RejectExtInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<RejectExtInd> for LlcpPacket {
    fn from(packet: RejectExtInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for RejectExtInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<RejectExtInd, Self::Error> {
        RejectExtInd::new(packet.llcppacket)
    }
}
impl RejectExtInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let rejectextind = match &llcppacket.child {
            LlcpPacketDataChild::RejectExtInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::RejectExtInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, rejectextind })
    }
    pub fn get_error_code(&self) -> u8 {
        self.rejectextind.error_code
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_reject_opcode(&self) -> u8 {
        self.rejectextind.reject_opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.rejectextind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl RejectExtIndBuilder {
    pub fn build(self) -> RejectExtInd {
        let rejectextind = RejectExtIndData {
            error_code: self.error_code,
            reject_opcode: self.reject_opcode,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlRejectExtInd,
            child: LlcpPacketDataChild::RejectExtInd(rejectextind),
        };
        RejectExtInd::new(llcppacket).unwrap()
    }
}
impl From<RejectExtIndBuilder> for LlcpPacket {
    fn from(builder: RejectExtIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<RejectExtIndBuilder> for RejectExtInd {
    fn from(builder: RejectExtIndBuilder) -> RejectExtInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingReqData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    pingreq: PingReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingReqBuilder {}
impl PingReqData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for PingReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PingReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PingReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PingReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PingReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PingReq> for LlcpPacket {
    fn from(packet: PingReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PingReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PingReq, Self::Error> {
        PingReq::new(packet.llcppacket)
    }
}
impl PingReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let pingreq = match &llcppacket.child {
            LlcpPacketDataChild::PingReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PingReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, pingreq })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.pingreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PingReqBuilder {
    pub fn build(self) -> PingReq {
        let pingreq = PingReqData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPingReq,
            child: LlcpPacketDataChild::PingReq(pingreq),
        };
        PingReq::new(llcppacket).unwrap()
    }
}
impl From<PingReqBuilder> for LlcpPacket {
    fn from(builder: PingReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PingReqBuilder> for PingReq {
    fn from(builder: PingReqBuilder) -> PingReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingRspData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    pingrsp: PingRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PingRspBuilder {}
impl PingRspData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for PingRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PingRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PingRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PingRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PingRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PingRsp> for LlcpPacket {
    fn from(packet: PingRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PingRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PingRsp, Self::Error> {
        PingRsp::new(packet.llcppacket)
    }
}
impl PingRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let pingrsp = match &llcppacket.child {
            LlcpPacketDataChild::PingRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PingRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, pingrsp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.pingrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PingRspBuilder {
    pub fn build(self) -> PingRsp {
        let pingrsp = PingRspData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPingRsp,
            child: LlcpPacketDataChild::PingRsp(pingrsp),
        };
        PingRsp::new(llcppacket).unwrap()
    }
}
impl From<PingRspBuilder> for LlcpPacket {
    fn from(builder: PingRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PingRspBuilder> for PingRsp {
    fn from(builder: PingRspBuilder) -> PingRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthReqData {
    max_rx_octets: u16,
    max_rx_time: u16,
    max_tx_octets: u16,
    max_tx_time: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    lengthreq: LengthReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthReqBuilder {
    pub max_rx_octets: u16,
    pub max_rx_time: u16,
    pub max_tx_octets: u16,
    pub max_tx_time: u16,
}
impl LengthReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_rx_octets = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_rx_time = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_tx_octets = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_tx_time = bytes.get_mut().get_u16_le();
        Ok(Self {
            max_rx_octets,
            max_rx_time,
            max_tx_octets,
            max_tx_time,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.max_rx_octets);
        buffer.put_u16_le(self.max_rx_time);
        buffer.put_u16_le(self.max_tx_octets);
        buffer.put_u16_le(self.max_tx_time);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for LengthReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<LengthReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: LengthReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<LengthReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: LengthReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<LengthReq> for LlcpPacket {
    fn from(packet: LengthReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for LengthReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<LengthReq, Self::Error> {
        LengthReq::new(packet.llcppacket)
    }
}
impl LengthReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let lengthreq = match &llcppacket.child {
            LlcpPacketDataChild::LengthReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::LengthReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, lengthreq })
    }
    pub fn get_max_rx_octets(&self) -> u16 {
        self.lengthreq.max_rx_octets
    }
    pub fn get_max_rx_time(&self) -> u16 {
        self.lengthreq.max_rx_time
    }
    pub fn get_max_tx_octets(&self) -> u16 {
        self.lengthreq.max_tx_octets
    }
    pub fn get_max_tx_time(&self) -> u16 {
        self.lengthreq.max_tx_time
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lengthreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl LengthReqBuilder {
    pub fn build(self) -> LengthReq {
        let lengthreq = LengthReqData {
            max_rx_octets: self.max_rx_octets,
            max_rx_time: self.max_rx_time,
            max_tx_octets: self.max_tx_octets,
            max_tx_time: self.max_tx_time,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlLengthReq,
            child: LlcpPacketDataChild::LengthReq(lengthreq),
        };
        LengthReq::new(llcppacket).unwrap()
    }
}
impl From<LengthReqBuilder> for LlcpPacket {
    fn from(builder: LengthReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<LengthReqBuilder> for LengthReq {
    fn from(builder: LengthReqBuilder) -> LengthReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthRspData {
    max_rx_octets: u16,
    max_rx_time: u16,
    max_tx_octets: u16,
    max_tx_time: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    lengthrsp: LengthRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LengthRspBuilder {
    pub max_rx_octets: u16,
    pub max_rx_time: u16,
    pub max_tx_octets: u16,
    pub max_tx_time: u16,
}
impl LengthRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_rx_octets = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_rx_time = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_tx_octets = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "LengthRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_tx_time = bytes.get_mut().get_u16_le();
        Ok(Self {
            max_rx_octets,
            max_rx_time,
            max_tx_octets,
            max_tx_time,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.max_rx_octets);
        buffer.put_u16_le(self.max_rx_time);
        buffer.put_u16_le(self.max_tx_octets);
        buffer.put_u16_le(self.max_tx_time);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for LengthRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<LengthRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: LengthRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<LengthRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: LengthRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<LengthRsp> for LlcpPacket {
    fn from(packet: LengthRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for LengthRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<LengthRsp, Self::Error> {
        LengthRsp::new(packet.llcppacket)
    }
}
impl LengthRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let lengthrsp = match &llcppacket.child {
            LlcpPacketDataChild::LengthRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::LengthRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, lengthrsp })
    }
    pub fn get_max_rx_octets(&self) -> u16 {
        self.lengthrsp.max_rx_octets
    }
    pub fn get_max_rx_time(&self) -> u16 {
        self.lengthrsp.max_rx_time
    }
    pub fn get_max_tx_octets(&self) -> u16 {
        self.lengthrsp.max_tx_octets
    }
    pub fn get_max_tx_time(&self) -> u16 {
        self.lengthrsp.max_tx_time
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lengthrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl LengthRspBuilder {
    pub fn build(self) -> LengthRsp {
        let lengthrsp = LengthRspData {
            max_rx_octets: self.max_rx_octets,
            max_rx_time: self.max_rx_time,
            max_tx_octets: self.max_tx_octets,
            max_tx_time: self.max_tx_time,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlLengthRsp,
            child: LlcpPacketDataChild::LengthRsp(lengthrsp),
        };
        LengthRsp::new(llcppacket).unwrap()
    }
}
impl From<LengthRspBuilder> for LlcpPacket {
    fn from(builder: LengthRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<LengthRspBuilder> for LengthRsp {
    fn from(builder: LengthRspBuilder) -> LengthRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyReqData {
    tx_phys: u8,
    rx_phys: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    phyreq: PhyReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyReqBuilder {
    pub rx_phys: u8,
    pub tx_phys: u8,
}
impl PhyReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let tx_phys = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let rx_phys = bytes.get_mut().get_u8();
        Ok(Self { tx_phys, rx_phys })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.tx_phys);
        buffer.put_u8(self.rx_phys);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for PhyReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PhyReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PhyReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PhyReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PhyReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PhyReq> for LlcpPacket {
    fn from(packet: PhyReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PhyReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PhyReq, Self::Error> {
        PhyReq::new(packet.llcppacket)
    }
}
impl PhyReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let phyreq = match &llcppacket.child {
            LlcpPacketDataChild::PhyReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PhyReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, phyreq })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_rx_phys(&self) -> u8 {
        self.phyreq.rx_phys
    }
    pub fn get_tx_phys(&self) -> u8 {
        self.phyreq.tx_phys
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.phyreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PhyReqBuilder {
    pub fn build(self) -> PhyReq {
        let phyreq = PhyReqData {
            rx_phys: self.rx_phys,
            tx_phys: self.tx_phys,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPhyReq,
            child: LlcpPacketDataChild::PhyReq(phyreq),
        };
        PhyReq::new(llcppacket).unwrap()
    }
}
impl From<PhyReqBuilder> for LlcpPacket {
    fn from(builder: PhyReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PhyReqBuilder> for PhyReq {
    fn from(builder: PhyReqBuilder) -> PhyReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyRspData {
    tx_phys: u8,
    rx_phys: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    phyrsp: PhyRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyRspBuilder {
    pub rx_phys: u8,
    pub tx_phys: u8,
}
impl PhyRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let tx_phys = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let rx_phys = bytes.get_mut().get_u8();
        Ok(Self { tx_phys, rx_phys })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.tx_phys);
        buffer.put_u8(self.rx_phys);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for PhyRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PhyRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PhyRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PhyRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PhyRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PhyRsp> for LlcpPacket {
    fn from(packet: PhyRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PhyRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PhyRsp, Self::Error> {
        PhyRsp::new(packet.llcppacket)
    }
}
impl PhyRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let phyrsp = match &llcppacket.child {
            LlcpPacketDataChild::PhyRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PhyRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, phyrsp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_rx_phys(&self) -> u8 {
        self.phyrsp.rx_phys
    }
    pub fn get_tx_phys(&self) -> u8 {
        self.phyrsp.tx_phys
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.phyrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PhyRspBuilder {
    pub fn build(self) -> PhyRsp {
        let phyrsp = PhyRspData {
            rx_phys: self.rx_phys,
            tx_phys: self.tx_phys,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPhyRsp,
            child: LlcpPacketDataChild::PhyRsp(phyrsp),
        };
        PhyRsp::new(llcppacket).unwrap()
    }
}
impl From<PhyRspBuilder> for LlcpPacket {
    fn from(builder: PhyRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PhyRspBuilder> for PhyRsp {
    fn from(builder: PhyRspBuilder) -> PhyRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyUpdateIndData {
    phy_c_to_p: u8,
    phy_p_to_c: u8,
    instant: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyUpdateInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    phyupdateind: PhyUpdateIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PhyUpdateIndBuilder {
    pub instant: u16,
    pub phy_c_to_p: u8,
    pub phy_p_to_c: u8,
}
impl PhyUpdateIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 4
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyUpdateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy_c_to_p = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyUpdateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy_p_to_c = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PhyUpdateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let instant = bytes.get_mut().get_u16_le();
        Ok(Self {
            phy_c_to_p,
            phy_p_to_c,
            instant,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.phy_c_to_p);
        buffer.put_u8(self.phy_p_to_c);
        buffer.put_u16_le(self.instant);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        4
    }
}
impl Packet for PhyUpdateInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PhyUpdateInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PhyUpdateInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PhyUpdateInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PhyUpdateInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PhyUpdateInd> for LlcpPacket {
    fn from(packet: PhyUpdateInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PhyUpdateInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PhyUpdateInd, Self::Error> {
        PhyUpdateInd::new(packet.llcppacket)
    }
}
impl PhyUpdateInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let phyupdateind = match &llcppacket.child {
            LlcpPacketDataChild::PhyUpdateInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PhyUpdateInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, phyupdateind })
    }
    pub fn get_instant(&self) -> u16 {
        self.phyupdateind.instant
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phy_c_to_p(&self) -> u8 {
        self.phyupdateind.phy_c_to_p
    }
    pub fn get_phy_p_to_c(&self) -> u8 {
        self.phyupdateind.phy_p_to_c
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.phyupdateind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PhyUpdateIndBuilder {
    pub fn build(self) -> PhyUpdateInd {
        let phyupdateind = PhyUpdateIndData {
            instant: self.instant,
            phy_c_to_p: self.phy_c_to_p,
            phy_p_to_c: self.phy_p_to_c,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPhyUpdateInd,
            child: LlcpPacketDataChild::PhyUpdateInd(phyupdateind),
        };
        PhyUpdateInd::new(llcppacket).unwrap()
    }
}
impl From<PhyUpdateIndBuilder> for LlcpPacket {
    fn from(builder: PhyUpdateIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PhyUpdateIndBuilder> for PhyUpdateInd {
    fn from(builder: PhyUpdateIndBuilder) -> PhyUpdateInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct MinUsedChannelsIndData {
    phys: u8,
    min_used_channels: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct MinUsedChannelsInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    minusedchannelsind: MinUsedChannelsIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct MinUsedChannelsIndBuilder {
    pub min_used_channels: u8,
    pub phys: u8,
}
impl MinUsedChannelsIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "MinUsedChannelsInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phys = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "MinUsedChannelsInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let min_used_channels = bytes.get_mut().get_u8();
        Ok(Self { phys, min_used_channels })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.phys);
        buffer.put_u8(self.min_used_channels);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for MinUsedChannelsInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<MinUsedChannelsInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: MinUsedChannelsInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<MinUsedChannelsInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: MinUsedChannelsInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<MinUsedChannelsInd> for LlcpPacket {
    fn from(packet: MinUsedChannelsInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for MinUsedChannelsInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<MinUsedChannelsInd, Self::Error> {
        MinUsedChannelsInd::new(packet.llcppacket)
    }
}
impl MinUsedChannelsInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let minusedchannelsind = match &llcppacket.child {
            LlcpPacketDataChild::MinUsedChannelsInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::MinUsedChannelsInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            minusedchannelsind,
        })
    }
    pub fn get_min_used_channels(&self) -> u8 {
        self.minusedchannelsind.min_used_channels
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phys(&self) -> u8 {
        self.minusedchannelsind.phys
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.minusedchannelsind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl MinUsedChannelsIndBuilder {
    pub fn build(self) -> MinUsedChannelsInd {
        let minusedchannelsind = MinUsedChannelsIndData {
            min_used_channels: self.min_used_channels,
            phys: self.phys,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlMinUsedChannelsInd,
            child: LlcpPacketDataChild::MinUsedChannelsInd(minusedchannelsind),
        };
        MinUsedChannelsInd::new(llcppacket).unwrap()
    }
}
impl From<MinUsedChannelsIndBuilder> for LlcpPacket {
    fn from(builder: MinUsedChannelsIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<MinUsedChannelsIndBuilder> for MinUsedChannelsInd {
    fn from(builder: MinUsedChannelsIndBuilder) -> MinUsedChannelsInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteReqData {
    min_cte_len_req: u8,
    cte_type_req: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    ctereq: CteReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteReqBuilder {
    pub cte_type_req: u8,
    pub min_cte_len_req: u8,
}
impl CteReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CteReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let min_cte_len_req = (chunk & 0x1f);
        let cte_type_req = ((chunk >> 6) & 0x3);
        Ok(Self {
            min_cte_len_req,
            cte_type_req,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        if self.min_cte_len_req > 0x1f {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CteReq",
                field: "min_cte_len_req",
                value: self.min_cte_len_req as u64,
                maximum_value: 0x1f,
            });
        }
        if self.cte_type_req > 0x3 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CteReq",
                field: "cte_type_req",
                value: self.cte_type_req as u64,
                maximum_value: 0x3,
            });
        }
        let value = self.min_cte_len_req | (self.cte_type_req << 6);
        buffer.put_u8(value);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for CteReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CteReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CteReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CteReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CteReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CteReq> for LlcpPacket {
    fn from(packet: CteReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CteReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CteReq, Self::Error> {
        CteReq::new(packet.llcppacket)
    }
}
impl CteReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let ctereq = match &llcppacket.child {
            LlcpPacketDataChild::CteReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CteReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, ctereq })
    }
    pub fn get_cte_type_req(&self) -> u8 {
        self.ctereq.cte_type_req
    }
    pub fn get_min_cte_len_req(&self) -> u8 {
        self.ctereq.min_cte_len_req
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.ctereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CteReqBuilder {
    pub fn build(self) -> CteReq {
        let ctereq = CteReqData {
            cte_type_req: self.cte_type_req,
            min_cte_len_req: self.min_cte_len_req,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCteReq,
            child: LlcpPacketDataChild::CteReq(ctereq),
        };
        CteReq::new(llcppacket).unwrap()
    }
}
impl From<CteReqBuilder> for LlcpPacket {
    fn from(builder: CteReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CteReqBuilder> for CteReq {
    fn from(builder: CteReqBuilder) -> CteReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteRspData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    ctersp: CteRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CteRspBuilder {}
impl CteRspData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        Ok(Self {})
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for CteRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CteRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CteRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CteRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CteRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CteRsp> for LlcpPacket {
    fn from(packet: CteRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CteRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CteRsp, Self::Error> {
        CteRsp::new(packet.llcppacket)
    }
}
impl CteRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let ctersp = match &llcppacket.child {
            LlcpPacketDataChild::CteRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CteRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, ctersp })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.ctersp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CteRspBuilder {
    pub fn build(self) -> CteRsp {
        let ctersp = CteRspData {};
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCteRsp,
            child: LlcpPacketDataChild::CteRsp(ctersp),
        };
        CteRsp::new(llcppacket).unwrap()
    }
}
impl From<CteRspBuilder> for LlcpPacket {
    fn from(builder: CteRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CteRspBuilder> for CteRsp {
    fn from(builder: CteRspBuilder) -> CteRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeriodicSyncIndData {
    id: u16,
    sync_info: [u8; 18],
    conn_event_count: u16,
    last_pa_event_counter: u16,
    sid: u8,
    atype: u8,
    sca: u8,
    phy: u8,
    adva: u64,
    sync_conn_event_count: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeriodicSyncInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    periodicsyncind: PeriodicSyncIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PeriodicSyncIndBuilder {
    pub adva: u64,
    pub atype: u8,
    pub conn_event_count: u16,
    pub id: u16,
    pub last_pa_event_counter: u16,
    pub phy: u8,
    pub sca: u8,
    pub sid: u8,
    pub sync_conn_event_count: u16,
    pub sync_info: [u8; 18],
}
impl PeriodicSyncIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 34
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let id = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 18 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 18,
                got: bytes.get().remaining(),
            });
        }
        let sync_info = (0..18)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let conn_event_count = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let last_pa_event_counter = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let sid = (chunk & 0xf);
        let atype = ((chunk >> 4) & 0x1);
        let sca = ((chunk >> 5) & 0x7);
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 6 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 6,
                got: bytes.get().remaining(),
            });
        }
        let adva = bytes.get_mut().get_uint_le(6);
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PeriodicSyncInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let sync_conn_event_count = bytes.get_mut().get_u16_le();
        Ok(Self {
            id,
            sync_info,
            conn_event_count,
            last_pa_event_counter,
            sid,
            atype,
            sca,
            phy,
            adva,
            sync_conn_event_count,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.id);
        for elem in &self.sync_info {
            buffer.put_u8(*elem);
        }
        buffer.put_u16_le(self.conn_event_count);
        buffer.put_u16_le(self.last_pa_event_counter);
        if self.sid > 0xf {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PeriodicSyncInd",
                field: "sid",
                value: self.sid as u64,
                maximum_value: 0xf,
            });
        }
        if self.atype > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PeriodicSyncInd",
                field: "atype",
                value: self.atype as u64,
                maximum_value: 0x1,
            });
        }
        if self.sca > 0x7 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PeriodicSyncInd",
                field: "sca",
                value: self.sca as u64,
                maximum_value: 0x7,
            });
        }
        let value = self.sid | (self.atype << 4) | (self.sca << 5);
        buffer.put_u8(value);
        buffer.put_u8(self.phy);
        if self.adva > 0xffff_ffff_ffff_u64 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PeriodicSyncInd",
                field: "adva",
                value: self.adva as u64,
                maximum_value: 0xffff_ffff_ffff_u64,
            });
        }
        buffer.put_uint_le(self.adva, 6);
        buffer.put_u16_le(self.sync_conn_event_count);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        34
    }
}
impl Packet for PeriodicSyncInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PeriodicSyncInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PeriodicSyncInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PeriodicSyncInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PeriodicSyncInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PeriodicSyncInd> for LlcpPacket {
    fn from(packet: PeriodicSyncInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PeriodicSyncInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PeriodicSyncInd, Self::Error> {
        PeriodicSyncInd::new(packet.llcppacket)
    }
}
impl PeriodicSyncInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let periodicsyncind = match &llcppacket.child {
            LlcpPacketDataChild::PeriodicSyncInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PeriodicSyncInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            periodicsyncind,
        })
    }
    pub fn get_adva(&self) -> u64 {
        self.periodicsyncind.adva
    }
    pub fn get_atype(&self) -> u8 {
        self.periodicsyncind.atype
    }
    pub fn get_conn_event_count(&self) -> u16 {
        self.periodicsyncind.conn_event_count
    }
    pub fn get_id(&self) -> u16 {
        self.periodicsyncind.id
    }
    pub fn get_last_pa_event_counter(&self) -> u16 {
        self.periodicsyncind.last_pa_event_counter
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phy(&self) -> u8 {
        self.periodicsyncind.phy
    }
    pub fn get_sca(&self) -> u8 {
        self.periodicsyncind.sca
    }
    pub fn get_sid(&self) -> u8 {
        self.periodicsyncind.sid
    }
    pub fn get_sync_conn_event_count(&self) -> u16 {
        self.periodicsyncind.sync_conn_event_count
    }
    pub fn get_sync_info(&self) -> &[u8; 18] {
        &self.periodicsyncind.sync_info
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.periodicsyncind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PeriodicSyncIndBuilder {
    pub fn build(self) -> PeriodicSyncInd {
        let periodicsyncind = PeriodicSyncIndData {
            adva: self.adva,
            atype: self.atype,
            conn_event_count: self.conn_event_count,
            id: self.id,
            last_pa_event_counter: self.last_pa_event_counter,
            phy: self.phy,
            sca: self.sca,
            sid: self.sid,
            sync_conn_event_count: self.sync_conn_event_count,
            sync_info: self.sync_info,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPeriodicSyncInd,
            child: LlcpPacketDataChild::PeriodicSyncInd(periodicsyncind),
        };
        PeriodicSyncInd::new(llcppacket).unwrap()
    }
}
impl From<PeriodicSyncIndBuilder> for LlcpPacket {
    fn from(builder: PeriodicSyncIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PeriodicSyncIndBuilder> for PeriodicSyncInd {
    fn from(builder: PeriodicSyncIndBuilder) -> PeriodicSyncInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyReqData {
    sca: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    clockaccuracyreq: ClockAccuracyReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyReqBuilder {
    pub sca: u8,
}
impl ClockAccuracyReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ClockAccuracyReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let sca = bytes.get_mut().get_u8();
        Ok(Self { sca })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.sca);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for ClockAccuracyReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ClockAccuracyReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ClockAccuracyReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ClockAccuracyReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ClockAccuracyReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ClockAccuracyReq> for LlcpPacket {
    fn from(packet: ClockAccuracyReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ClockAccuracyReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ClockAccuracyReq, Self::Error> {
        ClockAccuracyReq::new(packet.llcppacket)
    }
}
impl ClockAccuracyReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let clockaccuracyreq = match &llcppacket.child {
            LlcpPacketDataChild::ClockAccuracyReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ClockAccuracyReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            clockaccuracyreq,
        })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_sca(&self) -> u8 {
        self.clockaccuracyreq.sca
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.clockaccuracyreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ClockAccuracyReqBuilder {
    pub fn build(self) -> ClockAccuracyReq {
        let clockaccuracyreq = ClockAccuracyReqData {
            sca: self.sca,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlClockAccuracyReq,
            child: LlcpPacketDataChild::ClockAccuracyReq(clockaccuracyreq),
        };
        ClockAccuracyReq::new(llcppacket).unwrap()
    }
}
impl From<ClockAccuracyReqBuilder> for LlcpPacket {
    fn from(builder: ClockAccuracyReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ClockAccuracyReqBuilder> for ClockAccuracyReq {
    fn from(builder: ClockAccuracyReqBuilder) -> ClockAccuracyReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyRspData {
    sca: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    clockaccuracyrsp: ClockAccuracyRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ClockAccuracyRspBuilder {
    pub sca: u8,
}
impl ClockAccuracyRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ClockAccuracyRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let sca = bytes.get_mut().get_u8();
        Ok(Self { sca })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.sca);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for ClockAccuracyRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ClockAccuracyRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ClockAccuracyRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ClockAccuracyRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ClockAccuracyRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ClockAccuracyRsp> for LlcpPacket {
    fn from(packet: ClockAccuracyRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ClockAccuracyRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ClockAccuracyRsp, Self::Error> {
        ClockAccuracyRsp::new(packet.llcppacket)
    }
}
impl ClockAccuracyRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let clockaccuracyrsp = match &llcppacket.child {
            LlcpPacketDataChild::ClockAccuracyRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ClockAccuracyRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            clockaccuracyrsp,
        })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_sca(&self) -> u8 {
        self.clockaccuracyrsp.sca
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.clockaccuracyrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ClockAccuracyRspBuilder {
    pub fn build(self) -> ClockAccuracyRsp {
        let clockaccuracyrsp = ClockAccuracyRspData {
            sca: self.sca,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlClockAccuracyRsp,
            child: LlcpPacketDataChild::ClockAccuracyRsp(clockaccuracyrsp),
        };
        ClockAccuracyRsp::new(llcppacket).unwrap()
    }
}
impl From<ClockAccuracyRspBuilder> for LlcpPacket {
    fn from(builder: ClockAccuracyRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ClockAccuracyRspBuilder> for ClockAccuracyRsp {
    fn from(builder: ClockAccuracyRspBuilder) -> ClockAccuracyRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisReqData {
    cig_id: u8,
    cis_id: u8,
    phy_c_to_p: u8,
    phy_p_to_c: u8,
    framed: u8,
    max_sdu_c_to_p: u16,
    max_sdu_p_to_c: u16,
    sdu_interval_c_to_p: u32,
    sdu_interval_p_to_c: u32,
    max_pdu_c_to_p: u16,
    max_pdu_p_to_c: u16,
    nse: u8,
    sub_interval: u32,
    bn_p_to_c: u8,
    bn_c_to_p: u8,
    ft_c_to_p: u8,
    ft_p_to_c: u8,
    iso_interval: u16,
    cis_offset_min: u32,
    cis_offset_max: u32,
    conn_event_count: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    cisreq: CisReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisReqBuilder {
    pub bn_c_to_p: u8,
    pub bn_p_to_c: u8,
    pub cig_id: u8,
    pub cis_id: u8,
    pub cis_offset_max: u32,
    pub cis_offset_min: u32,
    pub conn_event_count: u16,
    pub framed: u8,
    pub ft_c_to_p: u8,
    pub ft_p_to_c: u8,
    pub iso_interval: u16,
    pub max_pdu_c_to_p: u16,
    pub max_pdu_p_to_c: u16,
    pub max_sdu_c_to_p: u16,
    pub max_sdu_p_to_c: u16,
    pub nse: u8,
    pub phy_c_to_p: u8,
    pub phy_p_to_c: u8,
    pub sdu_interval_c_to_p: u32,
    pub sdu_interval_p_to_c: u32,
    pub sub_interval: u32,
}
impl CisReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 35
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let cig_id = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let cis_id = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy_c_to_p = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy_p_to_c = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u16_le();
        let framed = (chunk & 0x1) as u8;
        let max_sdu_c_to_p = ((chunk >> 4) & 0xfff);
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u16_le();
        let max_sdu_p_to_c = ((chunk >> 4) & 0xfff);
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_uint_le(3) as u32;
        let sdu_interval_c_to_p = ((chunk >> 4) & 0xf_ffff);
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_uint_le(3) as u32;
        let sdu_interval_p_to_c = ((chunk >> 4) & 0xf_ffff);
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_pdu_c_to_p = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_pdu_p_to_c = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let nse = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let sub_interval = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let bn_p_to_c = (chunk & 0xf);
        let bn_c_to_p = ((chunk >> 4) & 0xf);
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let ft_c_to_p = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let ft_p_to_c = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let iso_interval = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_offset_min = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_offset_max = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let conn_event_count = bytes.get_mut().get_u16_le();
        Ok(Self {
            cig_id,
            cis_id,
            phy_c_to_p,
            phy_p_to_c,
            framed,
            max_sdu_c_to_p,
            max_sdu_p_to_c,
            sdu_interval_c_to_p,
            sdu_interval_p_to_c,
            max_pdu_c_to_p,
            max_pdu_p_to_c,
            nse,
            sub_interval,
            bn_p_to_c,
            bn_c_to_p,
            ft_c_to_p,
            ft_p_to_c,
            iso_interval,
            cis_offset_min,
            cis_offset_max,
            conn_event_count,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.cig_id);
        buffer.put_u8(self.cis_id);
        buffer.put_u8(self.phy_c_to_p);
        buffer.put_u8(self.phy_p_to_c);
        if self.framed > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "framed",
                value: self.framed as u64,
                maximum_value: 0x1,
            });
        }
        if self.max_sdu_c_to_p > 0xfff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "max_sdu_c_to_p",
                value: self.max_sdu_c_to_p as u64,
                maximum_value: 0xfff,
            });
        }
        let value = (self.framed as u16) | (self.max_sdu_c_to_p << 4);
        buffer.put_u16_le(value);
        if self.max_sdu_p_to_c > 0xfff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "max_sdu_p_to_c",
                value: self.max_sdu_p_to_c as u64,
                maximum_value: 0xfff,
            });
        }
        buffer.put_u16_le((self.max_sdu_p_to_c << 4));
        if self.sdu_interval_c_to_p > 0xf_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "sdu_interval_c_to_p",
                value: self.sdu_interval_c_to_p as u64,
                maximum_value: 0xf_ffff,
            });
        }
        buffer.put_uint_le((self.sdu_interval_c_to_p << 4) as u64, 3);
        if self.sdu_interval_p_to_c > 0xf_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "sdu_interval_p_to_c",
                value: self.sdu_interval_p_to_c as u64,
                maximum_value: 0xf_ffff,
            });
        }
        buffer.put_uint_le((self.sdu_interval_p_to_c << 4) as u64, 3);
        buffer.put_u16_le(self.max_pdu_c_to_p);
        buffer.put_u16_le(self.max_pdu_p_to_c);
        buffer.put_u8(self.nse);
        if self.sub_interval > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "sub_interval",
                value: self.sub_interval as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.sub_interval as u64, 3);
        if self.bn_p_to_c > 0xf {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "bn_p_to_c",
                value: self.bn_p_to_c as u64,
                maximum_value: 0xf,
            });
        }
        if self.bn_c_to_p > 0xf {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "bn_c_to_p",
                value: self.bn_c_to_p as u64,
                maximum_value: 0xf,
            });
        }
        let value = self.bn_p_to_c | (self.bn_c_to_p << 4);
        buffer.put_u8(value);
        buffer.put_u8(self.ft_c_to_p);
        buffer.put_u8(self.ft_p_to_c);
        buffer.put_u16_le(self.iso_interval);
        if self.cis_offset_min > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "cis_offset_min",
                value: self.cis_offset_min as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_offset_min as u64, 3);
        if self.cis_offset_max > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisReq",
                field: "cis_offset_max",
                value: self.cis_offset_max as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_offset_max as u64, 3);
        buffer.put_u16_le(self.conn_event_count);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        35
    }
}
impl Packet for CisReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CisReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CisReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CisReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CisReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CisReq> for LlcpPacket {
    fn from(packet: CisReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CisReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CisReq, Self::Error> {
        CisReq::new(packet.llcppacket)
    }
}
impl CisReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let cisreq = match &llcppacket.child {
            LlcpPacketDataChild::CisReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CisReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, cisreq })
    }
    pub fn get_bn_c_to_p(&self) -> u8 {
        self.cisreq.bn_c_to_p
    }
    pub fn get_bn_p_to_c(&self) -> u8 {
        self.cisreq.bn_p_to_c
    }
    pub fn get_cig_id(&self) -> u8 {
        self.cisreq.cig_id
    }
    pub fn get_cis_id(&self) -> u8 {
        self.cisreq.cis_id
    }
    pub fn get_cis_offset_max(&self) -> u32 {
        self.cisreq.cis_offset_max
    }
    pub fn get_cis_offset_min(&self) -> u32 {
        self.cisreq.cis_offset_min
    }
    pub fn get_conn_event_count(&self) -> u16 {
        self.cisreq.conn_event_count
    }
    pub fn get_framed(&self) -> u8 {
        self.cisreq.framed
    }
    pub fn get_ft_c_to_p(&self) -> u8 {
        self.cisreq.ft_c_to_p
    }
    pub fn get_ft_p_to_c(&self) -> u8 {
        self.cisreq.ft_p_to_c
    }
    pub fn get_iso_interval(&self) -> u16 {
        self.cisreq.iso_interval
    }
    pub fn get_max_pdu_c_to_p(&self) -> u16 {
        self.cisreq.max_pdu_c_to_p
    }
    pub fn get_max_pdu_p_to_c(&self) -> u16 {
        self.cisreq.max_pdu_p_to_c
    }
    pub fn get_max_sdu_c_to_p(&self) -> u16 {
        self.cisreq.max_sdu_c_to_p
    }
    pub fn get_max_sdu_p_to_c(&self) -> u16 {
        self.cisreq.max_sdu_p_to_c
    }
    pub fn get_nse(&self) -> u8 {
        self.cisreq.nse
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phy_c_to_p(&self) -> u8 {
        self.cisreq.phy_c_to_p
    }
    pub fn get_phy_p_to_c(&self) -> u8 {
        self.cisreq.phy_p_to_c
    }
    pub fn get_sdu_interval_c_to_p(&self) -> u32 {
        self.cisreq.sdu_interval_c_to_p
    }
    pub fn get_sdu_interval_p_to_c(&self) -> u32 {
        self.cisreq.sdu_interval_p_to_c
    }
    pub fn get_sub_interval(&self) -> u32 {
        self.cisreq.sub_interval
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.cisreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CisReqBuilder {
    pub fn build(self) -> CisReq {
        let cisreq = CisReqData {
            bn_c_to_p: self.bn_c_to_p,
            bn_p_to_c: self.bn_p_to_c,
            cig_id: self.cig_id,
            cis_id: self.cis_id,
            cis_offset_max: self.cis_offset_max,
            cis_offset_min: self.cis_offset_min,
            conn_event_count: self.conn_event_count,
            framed: self.framed,
            ft_c_to_p: self.ft_c_to_p,
            ft_p_to_c: self.ft_p_to_c,
            iso_interval: self.iso_interval,
            max_pdu_c_to_p: self.max_pdu_c_to_p,
            max_pdu_p_to_c: self.max_pdu_p_to_c,
            max_sdu_c_to_p: self.max_sdu_c_to_p,
            max_sdu_p_to_c: self.max_sdu_p_to_c,
            nse: self.nse,
            phy_c_to_p: self.phy_c_to_p,
            phy_p_to_c: self.phy_p_to_c,
            sdu_interval_c_to_p: self.sdu_interval_c_to_p,
            sdu_interval_p_to_c: self.sdu_interval_p_to_c,
            sub_interval: self.sub_interval,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCisReq,
            child: LlcpPacketDataChild::CisReq(cisreq),
        };
        CisReq::new(llcppacket).unwrap()
    }
}
impl From<CisReqBuilder> for LlcpPacket {
    fn from(builder: CisReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CisReqBuilder> for CisReq {
    fn from(builder: CisReqBuilder) -> CisReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisRspData {
    cis_offset_min: u32,
    cis_offset_max: u32,
    conn_event_count: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    cisrsp: CisRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisRspBuilder {
    pub cis_offset_max: u32,
    pub cis_offset_min: u32,
    pub conn_event_count: u16,
}
impl CisRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisRsp",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_offset_min = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisRsp",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_offset_max = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisRsp",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let conn_event_count = bytes.get_mut().get_u16_le();
        Ok(Self {
            cis_offset_min,
            cis_offset_max,
            conn_event_count,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        if self.cis_offset_min > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisRsp",
                field: "cis_offset_min",
                value: self.cis_offset_min as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_offset_min as u64, 3);
        if self.cis_offset_max > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisRsp",
                field: "cis_offset_max",
                value: self.cis_offset_max as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_offset_max as u64, 3);
        buffer.put_u16_le(self.conn_event_count);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}
impl Packet for CisRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CisRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CisRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CisRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CisRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CisRsp> for LlcpPacket {
    fn from(packet: CisRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CisRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CisRsp, Self::Error> {
        CisRsp::new(packet.llcppacket)
    }
}
impl CisRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let cisrsp = match &llcppacket.child {
            LlcpPacketDataChild::CisRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CisRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, cisrsp })
    }
    pub fn get_cis_offset_max(&self) -> u32 {
        self.cisrsp.cis_offset_max
    }
    pub fn get_cis_offset_min(&self) -> u32 {
        self.cisrsp.cis_offset_min
    }
    pub fn get_conn_event_count(&self) -> u16 {
        self.cisrsp.conn_event_count
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.cisrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CisRspBuilder {
    pub fn build(self) -> CisRsp {
        let cisrsp = CisRspData {
            cis_offset_max: self.cis_offset_max,
            cis_offset_min: self.cis_offset_min,
            conn_event_count: self.conn_event_count,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCisRsp,
            child: LlcpPacketDataChild::CisRsp(cisrsp),
        };
        CisRsp::new(llcppacket).unwrap()
    }
}
impl From<CisRspBuilder> for LlcpPacket {
    fn from(builder: CisRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CisRspBuilder> for CisRsp {
    fn from(builder: CisRspBuilder) -> CisRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisIndData {
    aa: u32,
    cis_offset: u32,
    cig_sync_delay: u32,
    cis_sync_delay: u32,
    conn_event_count: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    cisind: CisIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisIndBuilder {
    pub aa: u32,
    pub cig_sync_delay: u32,
    pub cis_offset: u32,
    pub cis_sync_delay: u32,
    pub conn_event_count: u16,
}
impl CisIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 15
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 4 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisInd",
                wanted: 4,
                got: bytes.get().remaining(),
            });
        }
        let aa = bytes.get_mut().get_u32_le();
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisInd",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_offset = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisInd",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cig_sync_delay = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 3 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisInd",
                wanted: 3,
                got: bytes.get().remaining(),
            });
        }
        let cis_sync_delay = bytes.get_mut().get_uint_le(3) as u32;
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let conn_event_count = bytes.get_mut().get_u16_le();
        Ok(Self {
            aa,
            cis_offset,
            cig_sync_delay,
            cis_sync_delay,
            conn_event_count,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u32_le(self.aa);
        if self.cis_offset > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisInd",
                field: "cis_offset",
                value: self.cis_offset as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_offset as u64, 3);
        if self.cig_sync_delay > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisInd",
                field: "cig_sync_delay",
                value: self.cig_sync_delay as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cig_sync_delay as u64, 3);
        if self.cis_sync_delay > 0xff_ffff {
            return Err(EncodeError::InvalidScalarValue {
                packet: "CisInd",
                field: "cis_sync_delay",
                value: self.cis_sync_delay as u64,
                maximum_value: 0xff_ffff,
            });
        }
        buffer.put_uint_le(self.cis_sync_delay as u64, 3);
        buffer.put_u16_le(self.conn_event_count);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        15
    }
}
impl Packet for CisInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CisInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CisInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CisInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CisInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CisInd> for LlcpPacket {
    fn from(packet: CisInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CisInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CisInd, Self::Error> {
        CisInd::new(packet.llcppacket)
    }
}
impl CisInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let cisind = match &llcppacket.child {
            LlcpPacketDataChild::CisInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CisInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, cisind })
    }
    pub fn get_aa(&self) -> u32 {
        self.cisind.aa
    }
    pub fn get_cig_sync_delay(&self) -> u32 {
        self.cisind.cig_sync_delay
    }
    pub fn get_cis_offset(&self) -> u32 {
        self.cisind.cis_offset
    }
    pub fn get_cis_sync_delay(&self) -> u32 {
        self.cisind.cis_sync_delay
    }
    pub fn get_conn_event_count(&self) -> u16 {
        self.cisind.conn_event_count
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.cisind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CisIndBuilder {
    pub fn build(self) -> CisInd {
        let cisind = CisIndData {
            aa: self.aa,
            cig_sync_delay: self.cig_sync_delay,
            cis_offset: self.cis_offset,
            cis_sync_delay: self.cis_sync_delay,
            conn_event_count: self.conn_event_count,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCisInd,
            child: LlcpPacketDataChild::CisInd(cisind),
        };
        CisInd::new(llcppacket).unwrap()
    }
}
impl From<CisIndBuilder> for LlcpPacket {
    fn from(builder: CisIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CisIndBuilder> for CisInd {
    fn from(builder: CisIndBuilder) -> CisInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisTerminateIndData {
    cig_id: u8,
    cis_id: u8,
    error_code: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisTerminateInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    cisterminateind: CisTerminateIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CisTerminateIndBuilder {
    pub cig_id: u8,
    pub cis_id: u8,
    pub error_code: u8,
}
impl CisTerminateIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisTerminateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let cig_id = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisTerminateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let cis_id = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CisTerminateInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u8();
        Ok(Self { cig_id, cis_id, error_code })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.cig_id);
        buffer.put_u8(self.cis_id);
        buffer.put_u8(self.error_code);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for CisTerminateInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CisTerminateInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CisTerminateInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CisTerminateInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CisTerminateInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CisTerminateInd> for LlcpPacket {
    fn from(packet: CisTerminateInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for CisTerminateInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<CisTerminateInd, Self::Error> {
        CisTerminateInd::new(packet.llcppacket)
    }
}
impl CisTerminateInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let cisterminateind = match &llcppacket.child {
            LlcpPacketDataChild::CisTerminateInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::CisTerminateInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            cisterminateind,
        })
    }
    pub fn get_cig_id(&self) -> u8 {
        self.cisterminateind.cig_id
    }
    pub fn get_cis_id(&self) -> u8 {
        self.cisterminateind.cis_id
    }
    pub fn get_error_code(&self) -> u8 {
        self.cisterminateind.error_code
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.cisterminateind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl CisTerminateIndBuilder {
    pub fn build(self) -> CisTerminateInd {
        let cisterminateind = CisTerminateIndData {
            cig_id: self.cig_id,
            cis_id: self.cis_id,
            error_code: self.error_code,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlCisTerminateInd,
            child: LlcpPacketDataChild::CisTerminateInd(cisterminateind),
        };
        CisTerminateInd::new(llcppacket).unwrap()
    }
}
impl From<CisTerminateIndBuilder> for LlcpPacket {
    fn from(builder: CisTerminateIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<CisTerminateIndBuilder> for CisTerminateInd {
    fn from(builder: CisTerminateIndBuilder) -> CisTerminateInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlReqData {
    phy: u8,
    delta: u8,
    tx_power: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    powercontrolreq: PowerControlReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlReqBuilder {
    pub delta: u8,
    pub phy: u8,
    pub tx_power: u8,
}
impl PowerControlReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let delta = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let tx_power = bytes.get_mut().get_u8();
        Ok(Self { phy, delta, tx_power })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.phy);
        buffer.put_u8(self.delta);
        buffer.put_u8(self.tx_power);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for PowerControlReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PowerControlReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PowerControlReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PowerControlReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PowerControlReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PowerControlReq> for LlcpPacket {
    fn from(packet: PowerControlReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PowerControlReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PowerControlReq, Self::Error> {
        PowerControlReq::new(packet.llcppacket)
    }
}
impl PowerControlReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let powercontrolreq = match &llcppacket.child {
            LlcpPacketDataChild::PowerControlReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PowerControlReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            powercontrolreq,
        })
    }
    pub fn get_delta(&self) -> u8 {
        self.powercontrolreq.delta
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phy(&self) -> u8 {
        self.powercontrolreq.phy
    }
    pub fn get_tx_power(&self) -> u8 {
        self.powercontrolreq.tx_power
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.powercontrolreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PowerControlReqBuilder {
    pub fn build(self) -> PowerControlReq {
        let powercontrolreq = PowerControlReqData {
            delta: self.delta,
            phy: self.phy,
            tx_power: self.tx_power,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPowerControlReq,
            child: LlcpPacketDataChild::PowerControlReq(powercontrolreq),
        };
        PowerControlReq::new(llcppacket).unwrap()
    }
}
impl From<PowerControlReqBuilder> for LlcpPacket {
    fn from(builder: PowerControlReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PowerControlReqBuilder> for PowerControlReq {
    fn from(builder: PowerControlReqBuilder) -> PowerControlReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlRspData {
    min: u8,
    max: u8,
    delta: u8,
    tx_power: u8,
    apr: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlRsp {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    powercontrolrsp: PowerControlRspData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerControlRspBuilder {
    pub apr: u8,
    pub delta: u8,
    pub max: u8,
    pub min: u8,
    pub tx_power: u8,
}
impl PowerControlRspData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 4
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let min = (chunk & 0x1);
        let max = ((chunk >> 1) & 0x1);
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let delta = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let tx_power = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerControlRsp",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let apr = bytes.get_mut().get_u8();
        Ok(Self {
            min,
            max,
            delta,
            tx_power,
            apr,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        if self.min > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PowerControlRsp",
                field: "min",
                value: self.min as u64,
                maximum_value: 0x1,
            });
        }
        if self.max > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PowerControlRsp",
                field: "max",
                value: self.max as u64,
                maximum_value: 0x1,
            });
        }
        let value = self.min | (self.max << 1);
        buffer.put_u8(value);
        buffer.put_u8(self.delta);
        buffer.put_u8(self.tx_power);
        buffer.put_u8(self.apr);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        4
    }
}
impl Packet for PowerControlRsp {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PowerControlRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PowerControlRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PowerControlRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PowerControlRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PowerControlRsp> for LlcpPacket {
    fn from(packet: PowerControlRsp) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PowerControlRsp {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PowerControlRsp, Self::Error> {
        PowerControlRsp::new(packet.llcppacket)
    }
}
impl PowerControlRsp {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let powercontrolrsp = match &llcppacket.child {
            LlcpPacketDataChild::PowerControlRsp(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PowerControlRsp),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            powercontrolrsp,
        })
    }
    pub fn get_apr(&self) -> u8 {
        self.powercontrolrsp.apr
    }
    pub fn get_delta(&self) -> u8 {
        self.powercontrolrsp.delta
    }
    pub fn get_max(&self) -> u8 {
        self.powercontrolrsp.max
    }
    pub fn get_min(&self) -> u8 {
        self.powercontrolrsp.min
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_tx_power(&self) -> u8 {
        self.powercontrolrsp.tx_power
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.powercontrolrsp.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PowerControlRspBuilder {
    pub fn build(self) -> PowerControlRsp {
        let powercontrolrsp = PowerControlRspData {
            apr: self.apr,
            delta: self.delta,
            max: self.max,
            min: self.min,
            tx_power: self.tx_power,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPowerControlRsp,
            child: LlcpPacketDataChild::PowerControlRsp(powercontrolrsp),
        };
        PowerControlRsp::new(llcppacket).unwrap()
    }
}
impl From<PowerControlRspBuilder> for LlcpPacket {
    fn from(builder: PowerControlRspBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PowerControlRspBuilder> for PowerControlRsp {
    fn from(builder: PowerControlRspBuilder) -> PowerControlRsp {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerChangeIndData {
    phy: u8,
    min: u8,
    max: u8,
    delta: u8,
    tx_power: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerChangeInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    powerchangeind: PowerChangeIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PowerChangeIndBuilder {
    pub delta: u8,
    pub max: u8,
    pub min: u8,
    pub phy: u8,
    pub tx_power: u8,
}
impl PowerChangeIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 4
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerChangeInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let phy = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerChangeInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let min = (chunk & 0x1);
        let max = ((chunk >> 1) & 0x1);
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerChangeInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let delta = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "PowerChangeInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let tx_power = bytes.get_mut().get_u8();
        Ok(Self {
            phy,
            min,
            max,
            delta,
            tx_power,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.phy);
        if self.min > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PowerChangeInd",
                field: "min",
                value: self.min as u64,
                maximum_value: 0x1,
            });
        }
        if self.max > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "PowerChangeInd",
                field: "max",
                value: self.max as u64,
                maximum_value: 0x1,
            });
        }
        let value = self.min | (self.max << 1);
        buffer.put_u8(value);
        buffer.put_u8(self.delta);
        buffer.put_u8(self.tx_power);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        4
    }
}
impl Packet for PowerChangeInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PowerChangeInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PowerChangeInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PowerChangeInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PowerChangeInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PowerChangeInd> for LlcpPacket {
    fn from(packet: PowerChangeInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for PowerChangeInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<PowerChangeInd, Self::Error> {
        PowerChangeInd::new(packet.llcppacket)
    }
}
impl PowerChangeInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let powerchangeind = match &llcppacket.child {
            LlcpPacketDataChild::PowerChangeInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::PowerChangeInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, powerchangeind })
    }
    pub fn get_delta(&self) -> u8 {
        self.powerchangeind.delta
    }
    pub fn get_max(&self) -> u8 {
        self.powerchangeind.max
    }
    pub fn get_min(&self) -> u8 {
        self.powerchangeind.min
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_phy(&self) -> u8 {
        self.powerchangeind.phy
    }
    pub fn get_tx_power(&self) -> u8 {
        self.powerchangeind.tx_power
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.powerchangeind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl PowerChangeIndBuilder {
    pub fn build(self) -> PowerChangeInd {
        let powerchangeind = PowerChangeIndData {
            delta: self.delta,
            max: self.max,
            min: self.min,
            phy: self.phy,
            tx_power: self.tx_power,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlPowerChangeInd,
            child: LlcpPacketDataChild::PowerChangeInd(powerchangeind),
        };
        PowerChangeInd::new(llcppacket).unwrap()
    }
}
impl From<PowerChangeIndBuilder> for LlcpPacket {
    fn from(builder: PowerChangeIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<PowerChangeIndBuilder> for PowerChangeInd {
    fn from(builder: PowerChangeIndBuilder) -> PowerChangeInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateReqData {
    subrate_factor_min: u16,
    subrate_factor_max: u16,
    max_latency: u16,
    continuation_number: u16,
    timeout: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    subratereq: SubrateReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateReqBuilder {
    pub continuation_number: u16,
    pub max_latency: u16,
    pub subrate_factor_max: u16,
    pub subrate_factor_min: u16,
    pub timeout: u16,
}
impl SubrateReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let subrate_factor_min = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let subrate_factor_max = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let max_latency = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let continuation_number = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateReq",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let timeout = bytes.get_mut().get_u16_le();
        Ok(Self {
            subrate_factor_min,
            subrate_factor_max,
            max_latency,
            continuation_number,
            timeout,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.subrate_factor_min);
        buffer.put_u16_le(self.subrate_factor_max);
        buffer.put_u16_le(self.max_latency);
        buffer.put_u16_le(self.continuation_number);
        buffer.put_u16_le(self.timeout);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        10
    }
}
impl Packet for SubrateReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<SubrateReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: SubrateReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<SubrateReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: SubrateReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<SubrateReq> for LlcpPacket {
    fn from(packet: SubrateReq) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for SubrateReq {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<SubrateReq, Self::Error> {
        SubrateReq::new(packet.llcppacket)
    }
}
impl SubrateReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let subratereq = match &llcppacket.child {
            LlcpPacketDataChild::SubrateReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::SubrateReq),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, subratereq })
    }
    pub fn get_continuation_number(&self) -> u16 {
        self.subratereq.continuation_number
    }
    pub fn get_max_latency(&self) -> u16 {
        self.subratereq.max_latency
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_subrate_factor_max(&self) -> u16 {
        self.subratereq.subrate_factor_max
    }
    pub fn get_subrate_factor_min(&self) -> u16 {
        self.subratereq.subrate_factor_min
    }
    pub fn get_timeout(&self) -> u16 {
        self.subratereq.timeout
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.subratereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl SubrateReqBuilder {
    pub fn build(self) -> SubrateReq {
        let subratereq = SubrateReqData {
            continuation_number: self.continuation_number,
            max_latency: self.max_latency,
            subrate_factor_max: self.subrate_factor_max,
            subrate_factor_min: self.subrate_factor_min,
            timeout: self.timeout,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlSubrateReq,
            child: LlcpPacketDataChild::SubrateReq(subratereq),
        };
        SubrateReq::new(llcppacket).unwrap()
    }
}
impl From<SubrateReqBuilder> for LlcpPacket {
    fn from(builder: SubrateReqBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<SubrateReqBuilder> for SubrateReq {
    fn from(builder: SubrateReqBuilder) -> SubrateReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateIndData {
    subrate_factor: u16,
    subrate_base_event: u16,
    latency: u16,
    continuation_number: u16,
    timeout: u16,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    subrateind: SubrateIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SubrateIndBuilder {
    pub continuation_number: u16,
    pub latency: u16,
    pub subrate_base_event: u16,
    pub subrate_factor: u16,
    pub timeout: u16,
}
impl SubrateIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let subrate_factor = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let subrate_base_event = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let latency = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let continuation_number = bytes.get_mut().get_u16_le();
        if bytes.get().remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SubrateInd",
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let timeout = bytes.get_mut().get_u16_le();
        Ok(Self {
            subrate_factor,
            subrate_base_event,
            latency,
            continuation_number,
            timeout,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u16_le(self.subrate_factor);
        buffer.put_u16_le(self.subrate_base_event);
        buffer.put_u16_le(self.latency);
        buffer.put_u16_le(self.continuation_number);
        buffer.put_u16_le(self.timeout);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        10
    }
}
impl Packet for SubrateInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<SubrateInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: SubrateInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<SubrateInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: SubrateInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<SubrateInd> for LlcpPacket {
    fn from(packet: SubrateInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for SubrateInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<SubrateInd, Self::Error> {
        SubrateInd::new(packet.llcppacket)
    }
}
impl SubrateInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let subrateind = match &llcppacket.child {
            LlcpPacketDataChild::SubrateInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::SubrateInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self { llcppacket, subrateind })
    }
    pub fn get_continuation_number(&self) -> u16 {
        self.subrateind.continuation_number
    }
    pub fn get_latency(&self) -> u16 {
        self.subrateind.latency
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    pub fn get_subrate_base_event(&self) -> u16 {
        self.subrateind.subrate_base_event
    }
    pub fn get_subrate_factor(&self) -> u16 {
        self.subrateind.subrate_factor
    }
    pub fn get_timeout(&self) -> u16 {
        self.subrateind.timeout
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.subrateind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl SubrateIndBuilder {
    pub fn build(self) -> SubrateInd {
        let subrateind = SubrateIndData {
            continuation_number: self.continuation_number,
            latency: self.latency,
            subrate_base_event: self.subrate_base_event,
            subrate_factor: self.subrate_factor,
            timeout: self.timeout,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlSubrateInd,
            child: LlcpPacketDataChild::SubrateInd(subrateind),
        };
        SubrateInd::new(llcppacket).unwrap()
    }
}
impl From<SubrateIndBuilder> for LlcpPacket {
    fn from(builder: SubrateIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<SubrateIndBuilder> for SubrateInd {
    fn from(builder: SubrateIndBuilder) -> SubrateInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelReportingIndData {
    enable: u8,
    min_spacing: u8,
    max_delay: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelReportingInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    channelreportingind: ChannelReportingIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelReportingIndBuilder {
    pub enable: u8,
    pub max_delay: u8,
    pub min_spacing: u8,
}
impl ChannelReportingIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelReportingInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let enable = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelReportingInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let min_spacing = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelReportingInd",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let max_delay = bytes.get_mut().get_u8();
        Ok(Self {
            enable,
            min_spacing,
            max_delay,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.enable);
        buffer.put_u8(self.min_spacing);
        buffer.put_u8(self.max_delay);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for ChannelReportingInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ChannelReportingInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ChannelReportingInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ChannelReportingInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ChannelReportingInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ChannelReportingInd> for LlcpPacket {
    fn from(packet: ChannelReportingInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ChannelReportingInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ChannelReportingInd, Self::Error> {
        ChannelReportingInd::new(packet.llcppacket)
    }
}
impl ChannelReportingInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let channelreportingind = match &llcppacket.child {
            LlcpPacketDataChild::ChannelReportingInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ChannelReportingInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            channelreportingind,
        })
    }
    pub fn get_enable(&self) -> u8 {
        self.channelreportingind.enable
    }
    pub fn get_max_delay(&self) -> u8 {
        self.channelreportingind.max_delay
    }
    pub fn get_min_spacing(&self) -> u8 {
        self.channelreportingind.min_spacing
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.channelreportingind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ChannelReportingIndBuilder {
    pub fn build(self) -> ChannelReportingInd {
        let channelreportingind = ChannelReportingIndData {
            enable: self.enable,
            max_delay: self.max_delay,
            min_spacing: self.min_spacing,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlChannelReportingInd,
            child: LlcpPacketDataChild::ChannelReportingInd(channelreportingind),
        };
        ChannelReportingInd::new(llcppacket).unwrap()
    }
}
impl From<ChannelReportingIndBuilder> for LlcpPacket {
    fn from(builder: ChannelReportingIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ChannelReportingIndBuilder> for ChannelReportingInd {
    fn from(builder: ChannelReportingIndBuilder) -> ChannelReportingInd {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelStatusIndData {
    channel_classification: [u8; 10],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelStatusInd {
    #[cfg_attr(feature = "serde", serde(flatten))]
    llcppacket: LlcpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    channelstatusind: ChannelStatusIndData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ChannelStatusIndBuilder {
    pub channel_classification: [u8; 10],
}
impl ChannelStatusIndData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 10 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ChannelStatusInd",
                wanted: 10,
                got: bytes.get().remaining(),
            });
        }
        let channel_classification = (0..10)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { channel_classification })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.channel_classification {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        10
    }
}
impl Packet for ChannelStatusInd {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.llcppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ChannelStatusInd> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ChannelStatusInd) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ChannelStatusInd> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ChannelStatusInd) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ChannelStatusInd> for LlcpPacket {
    fn from(packet: ChannelStatusInd) -> LlcpPacket {
        LlcpPacket::new(packet.llcppacket).unwrap()
    }
}
impl TryFrom<LlcpPacket> for ChannelStatusInd {
    type Error = DecodeError;
    fn try_from(packet: LlcpPacket) -> Result<ChannelStatusInd, Self::Error> {
        ChannelStatusInd::new(packet.llcppacket)
    }
}
impl ChannelStatusInd {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LlcpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(llcppacket: LlcpPacketData) -> Result<Self, DecodeError> {
        let channelstatusind = match &llcppacket.child {
            LlcpPacketDataChild::ChannelStatusInd(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LlcpPacketDataChild::ChannelStatusInd),
                    actual: format!("{:?}", & llcppacket.child),
                });
            }
        };
        Ok(Self {
            llcppacket,
            channelstatusind,
        })
    }
    pub fn get_channel_classification(&self) -> &[u8; 10] {
        &self.channelstatusind.channel_classification
    }
    pub fn get_opcode(&self) -> Opcode {
        self.llcppacket.opcode
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.channelstatusind.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.llcppacket.get_size()
    }
}
impl ChannelStatusIndBuilder {
    pub fn build(self) -> ChannelStatusInd {
        let channelstatusind = ChannelStatusIndData {
            channel_classification: self.channel_classification,
        };
        let llcppacket = LlcpPacketData {
            opcode: Opcode::LlChannelStatusInd,
            child: LlcpPacketDataChild::ChannelStatusInd(channelstatusind),
        };
        ChannelStatusInd::new(llcppacket).unwrap()
    }
}
impl From<ChannelStatusIndBuilder> for LlcpPacket {
    fn from(builder: ChannelStatusIndBuilder) -> LlcpPacket {
        builder.build().into()
    }
}
impl From<ChannelStatusIndBuilder> for ChannelStatusInd {
    fn from(builder: ChannelStatusIndBuilder) -> ChannelStatusInd {
        builder.build().into()
    }
}

