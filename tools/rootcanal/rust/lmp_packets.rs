/// @generated rust packets from lmp_packets.pdl.
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
    NameReq = 0x1,
    NameRes = 0x2,
    Accepted = 0x3,
    NotAccepted = 0x4,
    ClkOffsetReq = 0x5,
    ClkOffsetRes = 0x6,
    Detach = 0x7,
    InRand = 0x8,
    CombKey = 0x9,
    UnitKey = 0xa,
    AuRand = 0xb,
    Sres = 0xc,
    TempRand = 0xd,
    TempKey = 0xe,
    EncryptionModeReq = 0xf,
    EncryptionKeySizeReq = 0x10,
    StartEncryptionReq = 0x11,
    StopEncryptionReq = 0x12,
    SwitchReq = 0x13,
    Hold = 0x14,
    HoldReq = 0x15,
    SniffReq = 0x17,
    UnsniffReq = 0x18,
    IncrPowerReq = 0x1f,
    DecrPowerReq = 0x20,
    MaxPower = 0x21,
    MinPower = 0x22,
    AutoRate = 0x23,
    PreferredRate = 0x24,
    VersionReq = 0x25,
    VersionRes = 0x26,
    FeaturesReq = 0x27,
    FeaturesRes = 0x28,
    QualityOfService = 0x29,
    QualityOfServiceReq = 0x2a,
    ScoLinkReq = 0x2b,
    RemoveScoLinkReq = 0x2c,
    MaxSlot = 0x2d,
    MaxSlotReq = 0x2e,
    TimingAccuracyReq = 0x2f,
    TimingAccuracyRes = 0x30,
    SetypComplete = 0x31,
    UseSemiPermanentKey = 0x32,
    HostConnectionReq = 0x33,
    SlotOffset = 0x34,
    PageModeReq = 0x35,
    PageScanModeReq = 0x36,
    SupervisionTimeout = 0x37,
    TestActivate = 0x38,
    TestControl = 0x39,
    EncryptionKeySizeMaskReq = 0x3a,
    EncryptionKeySizeMaskRes = 0x3b,
    SetAfh = 0x3c,
    EncapsulatedHeader = 0x3d,
    EncapsulatedPayload = 0x3e,
    SimplePairingConfirm = 0x3f,
    SimplePairingNumber = 0x40,
    DhkeyCheck = 0x41,
    PauseEncryptionAesReq = 0x42,
    Escaped = 0x7f,
}
impl TryFrom<u8> for Opcode {
    type Error = u8;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0x1 => Ok(Opcode::NameReq),
            0x2 => Ok(Opcode::NameRes),
            0x3 => Ok(Opcode::Accepted),
            0x4 => Ok(Opcode::NotAccepted),
            0x5 => Ok(Opcode::ClkOffsetReq),
            0x6 => Ok(Opcode::ClkOffsetRes),
            0x7 => Ok(Opcode::Detach),
            0x8 => Ok(Opcode::InRand),
            0x9 => Ok(Opcode::CombKey),
            0xa => Ok(Opcode::UnitKey),
            0xb => Ok(Opcode::AuRand),
            0xc => Ok(Opcode::Sres),
            0xd => Ok(Opcode::TempRand),
            0xe => Ok(Opcode::TempKey),
            0xf => Ok(Opcode::EncryptionModeReq),
            0x10 => Ok(Opcode::EncryptionKeySizeReq),
            0x11 => Ok(Opcode::StartEncryptionReq),
            0x12 => Ok(Opcode::StopEncryptionReq),
            0x13 => Ok(Opcode::SwitchReq),
            0x14 => Ok(Opcode::Hold),
            0x15 => Ok(Opcode::HoldReq),
            0x17 => Ok(Opcode::SniffReq),
            0x18 => Ok(Opcode::UnsniffReq),
            0x1f => Ok(Opcode::IncrPowerReq),
            0x20 => Ok(Opcode::DecrPowerReq),
            0x21 => Ok(Opcode::MaxPower),
            0x22 => Ok(Opcode::MinPower),
            0x23 => Ok(Opcode::AutoRate),
            0x24 => Ok(Opcode::PreferredRate),
            0x25 => Ok(Opcode::VersionReq),
            0x26 => Ok(Opcode::VersionRes),
            0x27 => Ok(Opcode::FeaturesReq),
            0x28 => Ok(Opcode::FeaturesRes),
            0x29 => Ok(Opcode::QualityOfService),
            0x2a => Ok(Opcode::QualityOfServiceReq),
            0x2b => Ok(Opcode::ScoLinkReq),
            0x2c => Ok(Opcode::RemoveScoLinkReq),
            0x2d => Ok(Opcode::MaxSlot),
            0x2e => Ok(Opcode::MaxSlotReq),
            0x2f => Ok(Opcode::TimingAccuracyReq),
            0x30 => Ok(Opcode::TimingAccuracyRes),
            0x31 => Ok(Opcode::SetypComplete),
            0x32 => Ok(Opcode::UseSemiPermanentKey),
            0x33 => Ok(Opcode::HostConnectionReq),
            0x34 => Ok(Opcode::SlotOffset),
            0x35 => Ok(Opcode::PageModeReq),
            0x36 => Ok(Opcode::PageScanModeReq),
            0x37 => Ok(Opcode::SupervisionTimeout),
            0x38 => Ok(Opcode::TestActivate),
            0x39 => Ok(Opcode::TestControl),
            0x3a => Ok(Opcode::EncryptionKeySizeMaskReq),
            0x3b => Ok(Opcode::EncryptionKeySizeMaskRes),
            0x3c => Ok(Opcode::SetAfh),
            0x3d => Ok(Opcode::EncapsulatedHeader),
            0x3e => Ok(Opcode::EncapsulatedPayload),
            0x3f => Ok(Opcode::SimplePairingConfirm),
            0x40 => Ok(Opcode::SimplePairingNumber),
            0x41 => Ok(Opcode::DhkeyCheck),
            0x42 => Ok(Opcode::PauseEncryptionAesReq),
            0x7f => Ok(Opcode::Escaped),
            _ => Err(value),
        }
    }
}
impl From<&Opcode> for u8 {
    fn from(value: &Opcode) -> Self {
        match value {
            Opcode::NameReq => 0x1,
            Opcode::NameRes => 0x2,
            Opcode::Accepted => 0x3,
            Opcode::NotAccepted => 0x4,
            Opcode::ClkOffsetReq => 0x5,
            Opcode::ClkOffsetRes => 0x6,
            Opcode::Detach => 0x7,
            Opcode::InRand => 0x8,
            Opcode::CombKey => 0x9,
            Opcode::UnitKey => 0xa,
            Opcode::AuRand => 0xb,
            Opcode::Sres => 0xc,
            Opcode::TempRand => 0xd,
            Opcode::TempKey => 0xe,
            Opcode::EncryptionModeReq => 0xf,
            Opcode::EncryptionKeySizeReq => 0x10,
            Opcode::StartEncryptionReq => 0x11,
            Opcode::StopEncryptionReq => 0x12,
            Opcode::SwitchReq => 0x13,
            Opcode::Hold => 0x14,
            Opcode::HoldReq => 0x15,
            Opcode::SniffReq => 0x17,
            Opcode::UnsniffReq => 0x18,
            Opcode::IncrPowerReq => 0x1f,
            Opcode::DecrPowerReq => 0x20,
            Opcode::MaxPower => 0x21,
            Opcode::MinPower => 0x22,
            Opcode::AutoRate => 0x23,
            Opcode::PreferredRate => 0x24,
            Opcode::VersionReq => 0x25,
            Opcode::VersionRes => 0x26,
            Opcode::FeaturesReq => 0x27,
            Opcode::FeaturesRes => 0x28,
            Opcode::QualityOfService => 0x29,
            Opcode::QualityOfServiceReq => 0x2a,
            Opcode::ScoLinkReq => 0x2b,
            Opcode::RemoveScoLinkReq => 0x2c,
            Opcode::MaxSlot => 0x2d,
            Opcode::MaxSlotReq => 0x2e,
            Opcode::TimingAccuracyReq => 0x2f,
            Opcode::TimingAccuracyRes => 0x30,
            Opcode::SetypComplete => 0x31,
            Opcode::UseSemiPermanentKey => 0x32,
            Opcode::HostConnectionReq => 0x33,
            Opcode::SlotOffset => 0x34,
            Opcode::PageModeReq => 0x35,
            Opcode::PageScanModeReq => 0x36,
            Opcode::SupervisionTimeout => 0x37,
            Opcode::TestActivate => 0x38,
            Opcode::TestControl => 0x39,
            Opcode::EncryptionKeySizeMaskReq => 0x3a,
            Opcode::EncryptionKeySizeMaskRes => 0x3b,
            Opcode::SetAfh => 0x3c,
            Opcode::EncapsulatedHeader => 0x3d,
            Opcode::EncapsulatedPayload => 0x3e,
            Opcode::SimplePairingConfirm => 0x3f,
            Opcode::SimplePairingNumber => 0x40,
            Opcode::DhkeyCheck => 0x41,
            Opcode::PauseEncryptionAesReq => 0x42,
            Opcode::Escaped => 0x7f,
        }
    }
}
impl From<Opcode> for u8 {
    fn from(value: Opcode) -> Self {
        (&value).into()
    }
}
impl From<Opcode> for i8 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
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
#[repr(u64)]
#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(try_from = "u8", into = "u8"))]
pub enum ExtendedOpcode {
    Accepted = 0x1,
    NotAccepted = 0x2,
    FeaturesReq = 0x3,
    FeaturesRes = 0x4,
    ClkAdj = 0x5,
    ClkAdjAck = 0x6,
    ClkAdjReq = 0x7,
    PacketTypeTableReq = 0xb,
    EscoLinkReq = 0xc,
    RemoveEscoLinkReq = 0xd,
    ChannelClassificationReq = 0x10,
    ChannelClassification = 0x11,
    SniffSubratingReq = 0x15,
    SniffSubratingRes = 0x16,
    PauseEncryptionReq = 0x17,
    ResumeEncryptionReq = 0x18,
    IoCapabilityReq = 0x19,
    IoCapabilityRes = 0x1a,
    NumericComparisonFailed = 0x1b,
    PasskeyFailed = 0x1c,
    OobFailed = 0x1d,
    KeypressNotification = 0x1e,
    PowerControlReq = 0x1f,
    PowerControlRes = 0x20,
    PingReq = 0x21,
    PingRes = 0x22,
    SamSetType0 = 0x23,
    SamDefineMap = 0x24,
    SamSwitch = 0x25,
}
impl TryFrom<u8> for ExtendedOpcode {
    type Error = u8;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0x1 => Ok(ExtendedOpcode::Accepted),
            0x2 => Ok(ExtendedOpcode::NotAccepted),
            0x3 => Ok(ExtendedOpcode::FeaturesReq),
            0x4 => Ok(ExtendedOpcode::FeaturesRes),
            0x5 => Ok(ExtendedOpcode::ClkAdj),
            0x6 => Ok(ExtendedOpcode::ClkAdjAck),
            0x7 => Ok(ExtendedOpcode::ClkAdjReq),
            0xb => Ok(ExtendedOpcode::PacketTypeTableReq),
            0xc => Ok(ExtendedOpcode::EscoLinkReq),
            0xd => Ok(ExtendedOpcode::RemoveEscoLinkReq),
            0x10 => Ok(ExtendedOpcode::ChannelClassificationReq),
            0x11 => Ok(ExtendedOpcode::ChannelClassification),
            0x15 => Ok(ExtendedOpcode::SniffSubratingReq),
            0x16 => Ok(ExtendedOpcode::SniffSubratingRes),
            0x17 => Ok(ExtendedOpcode::PauseEncryptionReq),
            0x18 => Ok(ExtendedOpcode::ResumeEncryptionReq),
            0x19 => Ok(ExtendedOpcode::IoCapabilityReq),
            0x1a => Ok(ExtendedOpcode::IoCapabilityRes),
            0x1b => Ok(ExtendedOpcode::NumericComparisonFailed),
            0x1c => Ok(ExtendedOpcode::PasskeyFailed),
            0x1d => Ok(ExtendedOpcode::OobFailed),
            0x1e => Ok(ExtendedOpcode::KeypressNotification),
            0x1f => Ok(ExtendedOpcode::PowerControlReq),
            0x20 => Ok(ExtendedOpcode::PowerControlRes),
            0x21 => Ok(ExtendedOpcode::PingReq),
            0x22 => Ok(ExtendedOpcode::PingRes),
            0x23 => Ok(ExtendedOpcode::SamSetType0),
            0x24 => Ok(ExtendedOpcode::SamDefineMap),
            0x25 => Ok(ExtendedOpcode::SamSwitch),
            _ => Err(value),
        }
    }
}
impl From<&ExtendedOpcode> for u8 {
    fn from(value: &ExtendedOpcode) -> Self {
        match value {
            ExtendedOpcode::Accepted => 0x1,
            ExtendedOpcode::NotAccepted => 0x2,
            ExtendedOpcode::FeaturesReq => 0x3,
            ExtendedOpcode::FeaturesRes => 0x4,
            ExtendedOpcode::ClkAdj => 0x5,
            ExtendedOpcode::ClkAdjAck => 0x6,
            ExtendedOpcode::ClkAdjReq => 0x7,
            ExtendedOpcode::PacketTypeTableReq => 0xb,
            ExtendedOpcode::EscoLinkReq => 0xc,
            ExtendedOpcode::RemoveEscoLinkReq => 0xd,
            ExtendedOpcode::ChannelClassificationReq => 0x10,
            ExtendedOpcode::ChannelClassification => 0x11,
            ExtendedOpcode::SniffSubratingReq => 0x15,
            ExtendedOpcode::SniffSubratingRes => 0x16,
            ExtendedOpcode::PauseEncryptionReq => 0x17,
            ExtendedOpcode::ResumeEncryptionReq => 0x18,
            ExtendedOpcode::IoCapabilityReq => 0x19,
            ExtendedOpcode::IoCapabilityRes => 0x1a,
            ExtendedOpcode::NumericComparisonFailed => 0x1b,
            ExtendedOpcode::PasskeyFailed => 0x1c,
            ExtendedOpcode::OobFailed => 0x1d,
            ExtendedOpcode::KeypressNotification => 0x1e,
            ExtendedOpcode::PowerControlReq => 0x1f,
            ExtendedOpcode::PowerControlRes => 0x20,
            ExtendedOpcode::PingReq => 0x21,
            ExtendedOpcode::PingRes => 0x22,
            ExtendedOpcode::SamSetType0 => 0x23,
            ExtendedOpcode::SamDefineMap => 0x24,
            ExtendedOpcode::SamSwitch => 0x25,
        }
    }
}
impl From<ExtendedOpcode> for u8 {
    fn from(value: ExtendedOpcode) -> Self {
        (&value).into()
    }
}
impl From<ExtendedOpcode> for i16 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ExtendedOpcode> for i32 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ExtendedOpcode> for i64 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ExtendedOpcode> for u16 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ExtendedOpcode> for u32 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ExtendedOpcode> for u64 {
    fn from(value: ExtendedOpcode) -> Self {
        u8::from(value) as Self
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum LmpPacketDataChild {
    ExtendedPacket(ExtendedPacketData),
    Accepted(AcceptedData),
    NotAccepted(NotAcceptedData),
    EncapsulatedHeader(EncapsulatedHeaderData),
    EncapsulatedPayload(EncapsulatedPayloadData),
    SimplePairingConfirm(SimplePairingConfirmData),
    SimplePairingNumber(SimplePairingNumberData),
    DhkeyCheck(DhkeyCheckData),
    AuRand(AuRandData),
    Sres(SresData),
    InRand(InRandData),
    CombKey(CombKeyData),
    EncryptionModeReq(EncryptionModeReqData),
    EncryptionKeySizeReq(EncryptionKeySizeReqData),
    StartEncryptionReq(StartEncryptionReqData),
    StopEncryptionReq(StopEncryptionReqData),
    Payload(Bytes),
    None,
}
impl LmpPacketDataChild {
    fn get_total_size(&self) -> usize {
        match self {
            LmpPacketDataChild::ExtendedPacket(value) => value.get_total_size(),
            LmpPacketDataChild::Accepted(value) => value.get_total_size(),
            LmpPacketDataChild::NotAccepted(value) => value.get_total_size(),
            LmpPacketDataChild::EncapsulatedHeader(value) => value.get_total_size(),
            LmpPacketDataChild::EncapsulatedPayload(value) => value.get_total_size(),
            LmpPacketDataChild::SimplePairingConfirm(value) => value.get_total_size(),
            LmpPacketDataChild::SimplePairingNumber(value) => value.get_total_size(),
            LmpPacketDataChild::DhkeyCheck(value) => value.get_total_size(),
            LmpPacketDataChild::AuRand(value) => value.get_total_size(),
            LmpPacketDataChild::Sres(value) => value.get_total_size(),
            LmpPacketDataChild::InRand(value) => value.get_total_size(),
            LmpPacketDataChild::CombKey(value) => value.get_total_size(),
            LmpPacketDataChild::EncryptionModeReq(value) => value.get_total_size(),
            LmpPacketDataChild::EncryptionKeySizeReq(value) => value.get_total_size(),
            LmpPacketDataChild::StartEncryptionReq(value) => value.get_total_size(),
            LmpPacketDataChild::StopEncryptionReq(value) => value.get_total_size(),
            LmpPacketDataChild::Payload(bytes) => bytes.len(),
            LmpPacketDataChild::None => 0,
        }
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum LmpPacketChild {
    ExtendedPacket(ExtendedPacket),
    Accepted(Accepted),
    NotAccepted(NotAccepted),
    EncapsulatedHeader(EncapsulatedHeader),
    EncapsulatedPayload(EncapsulatedPayload),
    SimplePairingConfirm(SimplePairingConfirm),
    SimplePairingNumber(SimplePairingNumber),
    DhkeyCheck(DhkeyCheck),
    AuRand(AuRand),
    Sres(Sres),
    InRand(InRand),
    CombKey(CombKey),
    EncryptionModeReq(EncryptionModeReq),
    EncryptionKeySizeReq(EncryptionKeySizeReq),
    StartEncryptionReq(StartEncryptionReq),
    StopEncryptionReq(StopEncryptionReq),
    Payload(Bytes),
    None,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LmpPacketData {
    transaction_id: u8,
    opcode: Opcode,
    child: LmpPacketDataChild,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LmpPacket {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct LmpPacketBuilder {
    pub opcode: Opcode,
    pub transaction_id: u8,
    pub payload: Option<Bytes>,
}
impl LmpPacketData {
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
                obj: "LmpPacket",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let transaction_id = (chunk & 0x1);
        let opcode = Opcode::try_from(((chunk >> 1) & 0x7f))
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "LmpPacket",
                field: "opcode",
                value: unknown_val as u64,
                type_: "Opcode",
            })?;
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        let child = match (opcode) {
            (Opcode::Escaped) if ExtendedPacketData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = ExtendedPacketData::parse_inner(&mut cell)?;
                LmpPacketDataChild::ExtendedPacket(child_data)
            }
            (Opcode::Accepted) if AcceptedData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = AcceptedData::parse_inner(&mut cell)?;
                LmpPacketDataChild::Accepted(child_data)
            }
            (Opcode::NotAccepted) if NotAcceptedData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = NotAcceptedData::parse_inner(&mut cell)?;
                LmpPacketDataChild::NotAccepted(child_data)
            }
            (Opcode::EncapsulatedHeader) if EncapsulatedHeaderData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = EncapsulatedHeaderData::parse_inner(&mut cell)?;
                LmpPacketDataChild::EncapsulatedHeader(child_data)
            }
            (Opcode::EncapsulatedPayload) if EncapsulatedPayloadData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = EncapsulatedPayloadData::parse_inner(&mut cell)?;
                LmpPacketDataChild::EncapsulatedPayload(child_data)
            }
            (Opcode::SimplePairingConfirm) if SimplePairingConfirmData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = SimplePairingConfirmData::parse_inner(&mut cell)?;
                LmpPacketDataChild::SimplePairingConfirm(child_data)
            }
            (Opcode::SimplePairingNumber) if SimplePairingNumberData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = SimplePairingNumberData::parse_inner(&mut cell)?;
                LmpPacketDataChild::SimplePairingNumber(child_data)
            }
            (Opcode::DhkeyCheck) if DhkeyCheckData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = DhkeyCheckData::parse_inner(&mut cell)?;
                LmpPacketDataChild::DhkeyCheck(child_data)
            }
            (Opcode::AuRand) if AuRandData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = AuRandData::parse_inner(&mut cell)?;
                LmpPacketDataChild::AuRand(child_data)
            }
            (Opcode::Sres) if SresData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = SresData::parse_inner(&mut cell)?;
                LmpPacketDataChild::Sres(child_data)
            }
            (Opcode::InRand) if InRandData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = InRandData::parse_inner(&mut cell)?;
                LmpPacketDataChild::InRand(child_data)
            }
            (Opcode::CombKey) if CombKeyData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = CombKeyData::parse_inner(&mut cell)?;
                LmpPacketDataChild::CombKey(child_data)
            }
            (Opcode::EncryptionModeReq) if EncryptionModeReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = EncryptionModeReqData::parse_inner(&mut cell)?;
                LmpPacketDataChild::EncryptionModeReq(child_data)
            }
            (Opcode::EncryptionKeySizeReq) if EncryptionKeySizeReqData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = EncryptionKeySizeReqData::parse_inner(&mut cell)?;
                LmpPacketDataChild::EncryptionKeySizeReq(child_data)
            }
            (Opcode::StartEncryptionReq) if StartEncryptionReqData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = StartEncryptionReqData::parse_inner(&mut cell)?;
                LmpPacketDataChild::StartEncryptionReq(child_data)
            }
            (Opcode::StopEncryptionReq) if StopEncryptionReqData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = StopEncryptionReqData::parse_inner(&mut cell)?;
                LmpPacketDataChild::StopEncryptionReq(child_data)
            }
            _ if !payload.is_empty() => {
                LmpPacketDataChild::Payload(Bytes::copy_from_slice(payload))
            }
            _ => LmpPacketDataChild::None,
        };
        Ok(Self {
            transaction_id,
            opcode,
            child,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        if self.transaction_id > 0x1 {
            return Err(EncodeError::InvalidScalarValue {
                packet: "LmpPacket",
                field: "transaction_id",
                value: self.transaction_id as u64,
                maximum_value: 0x1,
            });
        }
        let value = self.transaction_id | (u8::from(self.opcode) << 1);
        buffer.put_u8(value);
        match &self.child {
            LmpPacketDataChild::ExtendedPacket(child) => child.write_to(buffer)?,
            LmpPacketDataChild::Accepted(child) => child.write_to(buffer)?,
            LmpPacketDataChild::NotAccepted(child) => child.write_to(buffer)?,
            LmpPacketDataChild::EncapsulatedHeader(child) => child.write_to(buffer)?,
            LmpPacketDataChild::EncapsulatedPayload(child) => child.write_to(buffer)?,
            LmpPacketDataChild::SimplePairingConfirm(child) => child.write_to(buffer)?,
            LmpPacketDataChild::SimplePairingNumber(child) => child.write_to(buffer)?,
            LmpPacketDataChild::DhkeyCheck(child) => child.write_to(buffer)?,
            LmpPacketDataChild::AuRand(child) => child.write_to(buffer)?,
            LmpPacketDataChild::Sres(child) => child.write_to(buffer)?,
            LmpPacketDataChild::InRand(child) => child.write_to(buffer)?,
            LmpPacketDataChild::CombKey(child) => child.write_to(buffer)?,
            LmpPacketDataChild::EncryptionModeReq(child) => child.write_to(buffer)?,
            LmpPacketDataChild::EncryptionKeySizeReq(child) => child.write_to(buffer)?,
            LmpPacketDataChild::StartEncryptionReq(child) => child.write_to(buffer)?,
            LmpPacketDataChild::StopEncryptionReq(child) => child.write_to(buffer)?,
            LmpPacketDataChild::Payload(payload) => buffer.put_slice(payload),
            LmpPacketDataChild::None => {}
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
impl Packet for LmpPacket {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<LmpPacket> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: LmpPacket) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<LmpPacket> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: LmpPacket) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl LmpPacket {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    pub fn specialize(&self) -> LmpPacketChild {
        match &self.lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(_) => {
                LmpPacketChild::ExtendedPacket(
                    ExtendedPacket::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::Accepted(_) => {
                LmpPacketChild::Accepted(Accepted::new(self.lmppacket.clone()).unwrap())
            }
            LmpPacketDataChild::NotAccepted(_) => {
                LmpPacketChild::NotAccepted(
                    NotAccepted::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::EncapsulatedHeader(_) => {
                LmpPacketChild::EncapsulatedHeader(
                    EncapsulatedHeader::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::EncapsulatedPayload(_) => {
                LmpPacketChild::EncapsulatedPayload(
                    EncapsulatedPayload::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::SimplePairingConfirm(_) => {
                LmpPacketChild::SimplePairingConfirm(
                    SimplePairingConfirm::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::SimplePairingNumber(_) => {
                LmpPacketChild::SimplePairingNumber(
                    SimplePairingNumber::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::DhkeyCheck(_) => {
                LmpPacketChild::DhkeyCheck(
                    DhkeyCheck::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::AuRand(_) => {
                LmpPacketChild::AuRand(AuRand::new(self.lmppacket.clone()).unwrap())
            }
            LmpPacketDataChild::Sres(_) => {
                LmpPacketChild::Sres(Sres::new(self.lmppacket.clone()).unwrap())
            }
            LmpPacketDataChild::InRand(_) => {
                LmpPacketChild::InRand(InRand::new(self.lmppacket.clone()).unwrap())
            }
            LmpPacketDataChild::CombKey(_) => {
                LmpPacketChild::CombKey(CombKey::new(self.lmppacket.clone()).unwrap())
            }
            LmpPacketDataChild::EncryptionModeReq(_) => {
                LmpPacketChild::EncryptionModeReq(
                    EncryptionModeReq::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::EncryptionKeySizeReq(_) => {
                LmpPacketChild::EncryptionKeySizeReq(
                    EncryptionKeySizeReq::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::StartEncryptionReq(_) => {
                LmpPacketChild::StartEncryptionReq(
                    StartEncryptionReq::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::StopEncryptionReq(_) => {
                LmpPacketChild::StopEncryptionReq(
                    StopEncryptionReq::new(self.lmppacket.clone()).unwrap(),
                )
            }
            LmpPacketDataChild::Payload(payload) => {
                LmpPacketChild::Payload(payload.clone())
            }
            LmpPacketDataChild::None => LmpPacketChild::None,
        }
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        Ok(Self { lmppacket })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl LmpPacketBuilder {
    pub fn build(self) -> LmpPacket {
        let lmppacket = LmpPacketData {
            opcode: self.opcode,
            transaction_id: self.transaction_id,
            child: match self.payload {
                None => LmpPacketDataChild::None,
                Some(bytes) => LmpPacketDataChild::Payload(bytes),
            },
        };
        LmpPacket::new(lmppacket).unwrap()
    }
}
impl From<LmpPacketBuilder> for LmpPacket {
    fn from(builder: LmpPacketBuilder) -> LmpPacket {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum ExtendedPacketDataChild {
    AcceptedExt(AcceptedExtData),
    NotAcceptedExt(NotAcceptedExtData),
    IoCapabilityReq(IoCapabilityReqData),
    IoCapabilityRes(IoCapabilityResData),
    NumericComparisonFailed(NumericComparisonFailedData),
    PasskeyFailed(PasskeyFailedData),
    KeypressNotification(KeypressNotificationData),
    FeaturesReqExt(FeaturesReqExtData),
    FeaturesResExt(FeaturesResExtData),
    Payload(Bytes),
    None,
}
impl ExtendedPacketDataChild {
    fn get_total_size(&self) -> usize {
        match self {
            ExtendedPacketDataChild::AcceptedExt(value) => value.get_total_size(),
            ExtendedPacketDataChild::NotAcceptedExt(value) => value.get_total_size(),
            ExtendedPacketDataChild::IoCapabilityReq(value) => value.get_total_size(),
            ExtendedPacketDataChild::IoCapabilityRes(value) => value.get_total_size(),
            ExtendedPacketDataChild::NumericComparisonFailed(value) => {
                value.get_total_size()
            }
            ExtendedPacketDataChild::PasskeyFailed(value) => value.get_total_size(),
            ExtendedPacketDataChild::KeypressNotification(value) => {
                value.get_total_size()
            }
            ExtendedPacketDataChild::FeaturesReqExt(value) => value.get_total_size(),
            ExtendedPacketDataChild::FeaturesResExt(value) => value.get_total_size(),
            ExtendedPacketDataChild::Payload(bytes) => bytes.len(),
            ExtendedPacketDataChild::None => 0,
        }
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum ExtendedPacketChild {
    AcceptedExt(AcceptedExt),
    NotAcceptedExt(NotAcceptedExt),
    IoCapabilityReq(IoCapabilityReq),
    IoCapabilityRes(IoCapabilityRes),
    NumericComparisonFailed(NumericComparisonFailed),
    PasskeyFailed(PasskeyFailed),
    KeypressNotification(KeypressNotification),
    FeaturesReqExt(FeaturesReqExt),
    FeaturesResExt(FeaturesResExt),
    Payload(Bytes),
    None,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ExtendedPacketData {
    extended_opcode: ExtendedOpcode,
    child: ExtendedPacketDataChild,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ExtendedPacket {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ExtendedPacketBuilder {
    pub extended_opcode: ExtendedOpcode,
    pub transaction_id: u8,
    pub payload: Option<Bytes>,
}
impl ExtendedPacketData {
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
                obj: "ExtendedPacket",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let extended_opcode = ExtendedOpcode::try_from(bytes.get_mut().get_u8())
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "ExtendedPacket",
                field: "extended_opcode",
                value: unknown_val as u64,
                type_: "ExtendedOpcode",
            })?;
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        let child = match (extended_opcode) {
            (ExtendedOpcode::Accepted) if AcceptedExtData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = AcceptedExtData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::AcceptedExt(child_data)
            }
            (ExtendedOpcode::NotAccepted) if NotAcceptedExtData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = NotAcceptedExtData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::NotAcceptedExt(child_data)
            }
            (ExtendedOpcode::IoCapabilityReq) if IoCapabilityReqData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = IoCapabilityReqData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::IoCapabilityReq(child_data)
            }
            (ExtendedOpcode::IoCapabilityRes) if IoCapabilityResData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = IoCapabilityResData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::IoCapabilityRes(child_data)
            }
            (ExtendedOpcode::NumericComparisonFailed) if NumericComparisonFailedData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = NumericComparisonFailedData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::NumericComparisonFailed(child_data)
            }
            (ExtendedOpcode::PasskeyFailed) if PasskeyFailedData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = PasskeyFailedData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::PasskeyFailed(child_data)
            }
            (ExtendedOpcode::KeypressNotification) if KeypressNotificationData::conforms(
                &payload,
            ) => {
                let mut cell = Cell::new(payload);
                let child_data = KeypressNotificationData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::KeypressNotification(child_data)
            }
            (ExtendedOpcode::FeaturesReq) if FeaturesReqExtData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = FeaturesReqExtData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::FeaturesReqExt(child_data)
            }
            (ExtendedOpcode::FeaturesRes) if FeaturesResExtData::conforms(&payload) => {
                let mut cell = Cell::new(payload);
                let child_data = FeaturesResExtData::parse_inner(&mut cell)?;
                ExtendedPacketDataChild::FeaturesResExt(child_data)
            }
            _ if !payload.is_empty() => {
                ExtendedPacketDataChild::Payload(Bytes::copy_from_slice(payload))
            }
            _ => ExtendedPacketDataChild::None,
        };
        Ok(Self { extended_opcode, child })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(u8::from(self.extended_opcode));
        match &self.child {
            ExtendedPacketDataChild::AcceptedExt(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::NotAcceptedExt(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::IoCapabilityReq(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::IoCapabilityRes(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::NumericComparisonFailed(child) => {
                child.write_to(buffer)?
            }
            ExtendedPacketDataChild::PasskeyFailed(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::KeypressNotification(child) => {
                child.write_to(buffer)?
            }
            ExtendedPacketDataChild::FeaturesReqExt(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::FeaturesResExt(child) => child.write_to(buffer)?,
            ExtendedPacketDataChild::Payload(payload) => buffer.put_slice(payload),
            ExtendedPacketDataChild::None => {}
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
impl Packet for ExtendedPacket {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<ExtendedPacket> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: ExtendedPacket) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<ExtendedPacket> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: ExtendedPacket) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<ExtendedPacket> for LmpPacket {
    fn from(packet: ExtendedPacket) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for ExtendedPacket {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<ExtendedPacket, Self::Error> {
        ExtendedPacket::new(packet.lmppacket)
    }
}
impl ExtendedPacket {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    pub fn specialize(&self) -> ExtendedPacketChild {
        match &self.extendedpacket.child {
            ExtendedPacketDataChild::AcceptedExt(_) => {
                ExtendedPacketChild::AcceptedExt(
                    AcceptedExt::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::NotAcceptedExt(_) => {
                ExtendedPacketChild::NotAcceptedExt(
                    NotAcceptedExt::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::IoCapabilityReq(_) => {
                ExtendedPacketChild::IoCapabilityReq(
                    IoCapabilityReq::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::IoCapabilityRes(_) => {
                ExtendedPacketChild::IoCapabilityRes(
                    IoCapabilityRes::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::NumericComparisonFailed(_) => {
                ExtendedPacketChild::NumericComparisonFailed(
                    NumericComparisonFailed::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::PasskeyFailed(_) => {
                ExtendedPacketChild::PasskeyFailed(
                    PasskeyFailed::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::KeypressNotification(_) => {
                ExtendedPacketChild::KeypressNotification(
                    KeypressNotification::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::FeaturesReqExt(_) => {
                ExtendedPacketChild::FeaturesReqExt(
                    FeaturesReqExt::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::FeaturesResExt(_) => {
                ExtendedPacketChild::FeaturesResExt(
                    FeaturesResExt::new(self.lmppacket.clone()).unwrap(),
                )
            }
            ExtendedPacketDataChild::Payload(payload) => {
                ExtendedPacketChild::Payload(payload.clone())
            }
            ExtendedPacketDataChild::None => ExtendedPacketChild::None,
        }
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, extendedpacket })
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.extendedpacket.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl ExtendedPacketBuilder {
    pub fn build(self) -> ExtendedPacket {
        let extendedpacket = ExtendedPacketData {
            extended_opcode: self.extended_opcode,
            child: match self.payload {
                None => ExtendedPacketDataChild::None,
                Some(bytes) => ExtendedPacketDataChild::Payload(bytes),
            },
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        ExtendedPacket::new(lmppacket).unwrap()
    }
}
impl From<ExtendedPacketBuilder> for LmpPacket {
    fn from(builder: ExtendedPacketBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<ExtendedPacketBuilder> for ExtendedPacket {
    fn from(builder: ExtendedPacketBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AcceptedData {
    accepted_opcode: Opcode,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Accepted {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    accepted: AcceptedData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AcceptedBuilder {
    pub accepted_opcode: Opcode,
    pub transaction_id: u8,
}
impl AcceptedData {
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
                obj: "Accepted",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let accepted_opcode = Opcode::try_from((chunk & 0x7f))
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "Accepted",
                field: "accepted_opcode",
                value: unknown_val as u64,
                type_: "Opcode",
            })?;
        let fixed_value = ((chunk >> 7) & 0x1);
        if fixed_value != 0 {
            return Err(DecodeError::InvalidFixedValue {
                expected: 0,
                actual: fixed_value as u64,
            });
        }
        Ok(Self { accepted_opcode })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        let value = u8::from(self.accepted_opcode) | (0 << 7);
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
impl Packet for Accepted {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<Accepted> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: Accepted) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<Accepted> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: Accepted) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<Accepted> for LmpPacket {
    fn from(packet: Accepted) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for Accepted {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<Accepted, Self::Error> {
        Accepted::new(packet.lmppacket)
    }
}
impl Accepted {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let accepted = match &lmppacket.child {
            LmpPacketDataChild::Accepted(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::Accepted),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, accepted })
    }
    pub fn get_accepted_opcode(&self) -> Opcode {
        self.accepted.accepted_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.accepted.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl AcceptedBuilder {
    pub fn build(self) -> Accepted {
        let accepted = AcceptedData {
            accepted_opcode: self.accepted_opcode,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Accepted,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::Accepted(accepted),
        };
        Accepted::new(lmppacket).unwrap()
    }
}
impl From<AcceptedBuilder> for LmpPacket {
    fn from(builder: AcceptedBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<AcceptedBuilder> for Accepted {
    fn from(builder: AcceptedBuilder) -> Accepted {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAcceptedData {
    not_accepted_opcode: Opcode,
    error_code: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAccepted {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    notaccepted: NotAcceptedData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAcceptedBuilder {
    pub error_code: u8,
    pub not_accepted_opcode: Opcode,
    pub transaction_id: u8,
}
impl NotAcceptedData {
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
                obj: "NotAccepted",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let chunk = bytes.get_mut().get_u8();
        let not_accepted_opcode = Opcode::try_from((chunk & 0x7f))
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "NotAccepted",
                field: "not_accepted_opcode",
                value: unknown_val as u64,
                type_: "Opcode",
            })?;
        let fixed_value = ((chunk >> 7) & 0x1);
        if fixed_value != 0 {
            return Err(DecodeError::InvalidFixedValue {
                expected: 0,
                actual: fixed_value as u64,
            });
        }
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "NotAccepted",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u8();
        Ok(Self {
            not_accepted_opcode,
            error_code,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        let value = u8::from(self.not_accepted_opcode) | (0 << 7);
        buffer.put_u8(value);
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
impl Packet for NotAccepted {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<NotAccepted> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: NotAccepted) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<NotAccepted> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: NotAccepted) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<NotAccepted> for LmpPacket {
    fn from(packet: NotAccepted) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for NotAccepted {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<NotAccepted, Self::Error> {
        NotAccepted::new(packet.lmppacket)
    }
}
impl NotAccepted {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let notaccepted = match &lmppacket.child {
            LmpPacketDataChild::NotAccepted(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::NotAccepted),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, notaccepted })
    }
    pub fn get_error_code(&self) -> u8 {
        self.notaccepted.error_code
    }
    pub fn get_not_accepted_opcode(&self) -> Opcode {
        self.notaccepted.not_accepted_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.notaccepted.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl NotAcceptedBuilder {
    pub fn build(self) -> NotAccepted {
        let notaccepted = NotAcceptedData {
            error_code: self.error_code,
            not_accepted_opcode: self.not_accepted_opcode,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::NotAccepted,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::NotAccepted(notaccepted),
        };
        NotAccepted::new(lmppacket).unwrap()
    }
}
impl From<NotAcceptedBuilder> for LmpPacket {
    fn from(builder: NotAcceptedBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<NotAcceptedBuilder> for NotAccepted {
    fn from(builder: NotAcceptedBuilder) -> NotAccepted {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AcceptedExtData {
    accepted_opcode: ExtendedOpcode,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AcceptedExt {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    acceptedext: AcceptedExtData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AcceptedExtBuilder {
    pub accepted_opcode: ExtendedOpcode,
    pub transaction_id: u8,
}
impl AcceptedExtData {
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
                obj: "AcceptedExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let accepted_opcode = ExtendedOpcode::try_from(bytes.get_mut().get_u8())
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "AcceptedExt",
                field: "accepted_opcode",
                value: unknown_val as u64,
                type_: "ExtendedOpcode",
            })?;
        Ok(Self { accepted_opcode })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(u8::from(self.accepted_opcode));
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for AcceptedExt {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<AcceptedExt> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: AcceptedExt) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<AcceptedExt> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: AcceptedExt) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<AcceptedExt> for LmpPacket {
    fn from(packet: AcceptedExt) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<AcceptedExt> for ExtendedPacket {
    fn from(packet: AcceptedExt) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for AcceptedExt {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<AcceptedExt, Self::Error> {
        AcceptedExt::new(packet.lmppacket)
    }
}
impl AcceptedExt {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let acceptedext = match &extendedpacket.child {
            ExtendedPacketDataChild::AcceptedExt(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::AcceptedExt),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            acceptedext,
        })
    }
    pub fn get_accepted_opcode(&self) -> ExtendedOpcode {
        self.acceptedext.accepted_opcode
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.acceptedext.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl AcceptedExtBuilder {
    pub fn build(self) -> AcceptedExt {
        let acceptedext = AcceptedExtData {
            accepted_opcode: self.accepted_opcode,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::Accepted,
            child: ExtendedPacketDataChild::AcceptedExt(acceptedext),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        AcceptedExt::new(lmppacket).unwrap()
    }
}
impl From<AcceptedExtBuilder> for LmpPacket {
    fn from(builder: AcceptedExtBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<AcceptedExtBuilder> for ExtendedPacket {
    fn from(builder: AcceptedExtBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<AcceptedExtBuilder> for AcceptedExt {
    fn from(builder: AcceptedExtBuilder) -> AcceptedExt {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAcceptedExtData {
    not_accepted_opcode: ExtendedOpcode,
    error_code: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAcceptedExt {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    notacceptedext: NotAcceptedExtData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NotAcceptedExtBuilder {
    pub error_code: u8,
    pub not_accepted_opcode: ExtendedOpcode,
    pub transaction_id: u8,
}
impl NotAcceptedExtData {
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
                obj: "NotAcceptedExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let not_accepted_opcode = ExtendedOpcode::try_from(bytes.get_mut().get_u8())
            .map_err(|unknown_val| DecodeError::InvalidEnumValueError {
                obj: "NotAcceptedExt",
                field: "not_accepted_opcode",
                value: unknown_val as u64,
                type_: "ExtendedOpcode",
            })?;
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "NotAcceptedExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let error_code = bytes.get_mut().get_u8();
        Ok(Self {
            not_accepted_opcode,
            error_code,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(u8::from(self.not_accepted_opcode));
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
impl Packet for NotAcceptedExt {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<NotAcceptedExt> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: NotAcceptedExt) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<NotAcceptedExt> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: NotAcceptedExt) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<NotAcceptedExt> for LmpPacket {
    fn from(packet: NotAcceptedExt) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<NotAcceptedExt> for ExtendedPacket {
    fn from(packet: NotAcceptedExt) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for NotAcceptedExt {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<NotAcceptedExt, Self::Error> {
        NotAcceptedExt::new(packet.lmppacket)
    }
}
impl NotAcceptedExt {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let notacceptedext = match &extendedpacket.child {
            ExtendedPacketDataChild::NotAcceptedExt(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::NotAcceptedExt),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            notacceptedext,
        })
    }
    pub fn get_error_code(&self) -> u8 {
        self.notacceptedext.error_code
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_not_accepted_opcode(&self) -> ExtendedOpcode {
        self.notacceptedext.not_accepted_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.notacceptedext.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl NotAcceptedExtBuilder {
    pub fn build(self) -> NotAcceptedExt {
        let notacceptedext = NotAcceptedExtData {
            error_code: self.error_code,
            not_accepted_opcode: self.not_accepted_opcode,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::NotAccepted,
            child: ExtendedPacketDataChild::NotAcceptedExt(notacceptedext),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        NotAcceptedExt::new(lmppacket).unwrap()
    }
}
impl From<NotAcceptedExtBuilder> for LmpPacket {
    fn from(builder: NotAcceptedExtBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<NotAcceptedExtBuilder> for ExtendedPacket {
    fn from(builder: NotAcceptedExtBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<NotAcceptedExtBuilder> for NotAcceptedExt {
    fn from(builder: NotAcceptedExtBuilder) -> NotAcceptedExt {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityReqData {
    io_capabilities: u8,
    oob_authentication_data: u8,
    authentication_requirement: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    iocapabilityreq: IoCapabilityReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityReqBuilder {
    pub authentication_requirement: u8,
    pub io_capabilities: u8,
    pub oob_authentication_data: u8,
    pub transaction_id: u8,
}
impl IoCapabilityReqData {
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
                obj: "IoCapabilityReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let io_capabilities = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "IoCapabilityReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let oob_authentication_data = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "IoCapabilityReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let authentication_requirement = bytes.get_mut().get_u8();
        Ok(Self {
            io_capabilities,
            oob_authentication_data,
            authentication_requirement,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.io_capabilities);
        buffer.put_u8(self.oob_authentication_data);
        buffer.put_u8(self.authentication_requirement);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for IoCapabilityReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<IoCapabilityReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: IoCapabilityReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<IoCapabilityReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: IoCapabilityReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<IoCapabilityReq> for LmpPacket {
    fn from(packet: IoCapabilityReq) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<IoCapabilityReq> for ExtendedPacket {
    fn from(packet: IoCapabilityReq) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for IoCapabilityReq {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<IoCapabilityReq, Self::Error> {
        IoCapabilityReq::new(packet.lmppacket)
    }
}
impl IoCapabilityReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let iocapabilityreq = match &extendedpacket.child {
            ExtendedPacketDataChild::IoCapabilityReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::IoCapabilityReq),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            iocapabilityreq,
        })
    }
    pub fn get_authentication_requirement(&self) -> u8 {
        self.iocapabilityreq.authentication_requirement
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_io_capabilities(&self) -> u8 {
        self.iocapabilityreq.io_capabilities
    }
    pub fn get_oob_authentication_data(&self) -> u8 {
        self.iocapabilityreq.oob_authentication_data
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.iocapabilityreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl IoCapabilityReqBuilder {
    pub fn build(self) -> IoCapabilityReq {
        let iocapabilityreq = IoCapabilityReqData {
            authentication_requirement: self.authentication_requirement,
            io_capabilities: self.io_capabilities,
            oob_authentication_data: self.oob_authentication_data,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::IoCapabilityReq,
            child: ExtendedPacketDataChild::IoCapabilityReq(iocapabilityreq),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        IoCapabilityReq::new(lmppacket).unwrap()
    }
}
impl From<IoCapabilityReqBuilder> for LmpPacket {
    fn from(builder: IoCapabilityReqBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<IoCapabilityReqBuilder> for ExtendedPacket {
    fn from(builder: IoCapabilityReqBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<IoCapabilityReqBuilder> for IoCapabilityReq {
    fn from(builder: IoCapabilityReqBuilder) -> IoCapabilityReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityResData {
    io_capabilities: u8,
    oob_authentication_data: u8,
    authentication_requirement: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityRes {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    iocapabilityres: IoCapabilityResData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IoCapabilityResBuilder {
    pub authentication_requirement: u8,
    pub io_capabilities: u8,
    pub oob_authentication_data: u8,
    pub transaction_id: u8,
}
impl IoCapabilityResData {
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
                obj: "IoCapabilityRes",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let io_capabilities = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "IoCapabilityRes",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let oob_authentication_data = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "IoCapabilityRes",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let authentication_requirement = bytes.get_mut().get_u8();
        Ok(Self {
            io_capabilities,
            oob_authentication_data,
            authentication_requirement,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.io_capabilities);
        buffer.put_u8(self.oob_authentication_data);
        buffer.put_u8(self.authentication_requirement);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for IoCapabilityRes {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<IoCapabilityRes> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: IoCapabilityRes) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<IoCapabilityRes> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: IoCapabilityRes) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<IoCapabilityRes> for LmpPacket {
    fn from(packet: IoCapabilityRes) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<IoCapabilityRes> for ExtendedPacket {
    fn from(packet: IoCapabilityRes) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for IoCapabilityRes {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<IoCapabilityRes, Self::Error> {
        IoCapabilityRes::new(packet.lmppacket)
    }
}
impl IoCapabilityRes {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let iocapabilityres = match &extendedpacket.child {
            ExtendedPacketDataChild::IoCapabilityRes(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::IoCapabilityRes),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            iocapabilityres,
        })
    }
    pub fn get_authentication_requirement(&self) -> u8 {
        self.iocapabilityres.authentication_requirement
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_io_capabilities(&self) -> u8 {
        self.iocapabilityres.io_capabilities
    }
    pub fn get_oob_authentication_data(&self) -> u8 {
        self.iocapabilityres.oob_authentication_data
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.iocapabilityres.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl IoCapabilityResBuilder {
    pub fn build(self) -> IoCapabilityRes {
        let iocapabilityres = IoCapabilityResData {
            authentication_requirement: self.authentication_requirement,
            io_capabilities: self.io_capabilities,
            oob_authentication_data: self.oob_authentication_data,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::IoCapabilityRes,
            child: ExtendedPacketDataChild::IoCapabilityRes(iocapabilityres),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        IoCapabilityRes::new(lmppacket).unwrap()
    }
}
impl From<IoCapabilityResBuilder> for LmpPacket {
    fn from(builder: IoCapabilityResBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<IoCapabilityResBuilder> for ExtendedPacket {
    fn from(builder: IoCapabilityResBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<IoCapabilityResBuilder> for IoCapabilityRes {
    fn from(builder: IoCapabilityResBuilder) -> IoCapabilityRes {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedHeaderData {
    major_type: u8,
    minor_type: u8,
    payload_length: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedHeader {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encapsulatedheader: EncapsulatedHeaderData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedHeaderBuilder {
    pub major_type: u8,
    pub minor_type: u8,
    pub payload_length: u8,
    pub transaction_id: u8,
}
impl EncapsulatedHeaderData {
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
                obj: "EncapsulatedHeader",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let major_type = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncapsulatedHeader",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let minor_type = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncapsulatedHeader",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let payload_length = bytes.get_mut().get_u8();
        Ok(Self {
            major_type,
            minor_type,
            payload_length,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.major_type);
        buffer.put_u8(self.minor_type);
        buffer.put_u8(self.payload_length);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for EncapsulatedHeader {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncapsulatedHeader> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncapsulatedHeader) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncapsulatedHeader> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncapsulatedHeader) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncapsulatedHeader> for LmpPacket {
    fn from(packet: EncapsulatedHeader) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for EncapsulatedHeader {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<EncapsulatedHeader, Self::Error> {
        EncapsulatedHeader::new(packet.lmppacket)
    }
}
impl EncapsulatedHeader {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let encapsulatedheader = match &lmppacket.child {
            LmpPacketDataChild::EncapsulatedHeader(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::EncapsulatedHeader),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            encapsulatedheader,
        })
    }
    pub fn get_major_type(&self) -> u8 {
        self.encapsulatedheader.major_type
    }
    pub fn get_minor_type(&self) -> u8 {
        self.encapsulatedheader.minor_type
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_payload_length(&self) -> u8 {
        self.encapsulatedheader.payload_length
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encapsulatedheader.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl EncapsulatedHeaderBuilder {
    pub fn build(self) -> EncapsulatedHeader {
        let encapsulatedheader = EncapsulatedHeaderData {
            major_type: self.major_type,
            minor_type: self.minor_type,
            payload_length: self.payload_length,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::EncapsulatedHeader,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::EncapsulatedHeader(encapsulatedheader),
        };
        EncapsulatedHeader::new(lmppacket).unwrap()
    }
}
impl From<EncapsulatedHeaderBuilder> for LmpPacket {
    fn from(builder: EncapsulatedHeaderBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<EncapsulatedHeaderBuilder> for EncapsulatedHeader {
    fn from(builder: EncapsulatedHeaderBuilder) -> EncapsulatedHeader {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedPayloadData {
    data: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedPayload {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encapsulatedpayload: EncapsulatedPayloadData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncapsulatedPayloadBuilder {
    pub data: [u8; 16],
    pub transaction_id: u8,
}
impl EncapsulatedPayloadData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "EncapsulatedPayload",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let data = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { data })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.data {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for EncapsulatedPayload {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncapsulatedPayload> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncapsulatedPayload) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncapsulatedPayload> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncapsulatedPayload) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncapsulatedPayload> for LmpPacket {
    fn from(packet: EncapsulatedPayload) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for EncapsulatedPayload {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<EncapsulatedPayload, Self::Error> {
        EncapsulatedPayload::new(packet.lmppacket)
    }
}
impl EncapsulatedPayload {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let encapsulatedpayload = match &lmppacket.child {
            LmpPacketDataChild::EncapsulatedPayload(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::EncapsulatedPayload),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            encapsulatedpayload,
        })
    }
    pub fn get_data(&self) -> &[u8; 16] {
        &self.encapsulatedpayload.data
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encapsulatedpayload.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl EncapsulatedPayloadBuilder {
    pub fn build(self) -> EncapsulatedPayload {
        let encapsulatedpayload = EncapsulatedPayloadData {
            data: self.data,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::EncapsulatedPayload,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::EncapsulatedPayload(encapsulatedpayload),
        };
        EncapsulatedPayload::new(lmppacket).unwrap()
    }
}
impl From<EncapsulatedPayloadBuilder> for LmpPacket {
    fn from(builder: EncapsulatedPayloadBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<EncapsulatedPayloadBuilder> for EncapsulatedPayload {
    fn from(builder: EncapsulatedPayloadBuilder) -> EncapsulatedPayload {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingConfirmData {
    commitment_value: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingConfirm {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    simplepairingconfirm: SimplePairingConfirmData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingConfirmBuilder {
    pub commitment_value: [u8; 16],
    pub transaction_id: u8,
}
impl SimplePairingConfirmData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SimplePairingConfirm",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let commitment_value = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { commitment_value })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.commitment_value {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for SimplePairingConfirm {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<SimplePairingConfirm> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: SimplePairingConfirm) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<SimplePairingConfirm> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: SimplePairingConfirm) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<SimplePairingConfirm> for LmpPacket {
    fn from(packet: SimplePairingConfirm) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for SimplePairingConfirm {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<SimplePairingConfirm, Self::Error> {
        SimplePairingConfirm::new(packet.lmppacket)
    }
}
impl SimplePairingConfirm {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let simplepairingconfirm = match &lmppacket.child {
            LmpPacketDataChild::SimplePairingConfirm(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::SimplePairingConfirm),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            simplepairingconfirm,
        })
    }
    pub fn get_commitment_value(&self) -> &[u8; 16] {
        &self.simplepairingconfirm.commitment_value
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.simplepairingconfirm.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl SimplePairingConfirmBuilder {
    pub fn build(self) -> SimplePairingConfirm {
        let simplepairingconfirm = SimplePairingConfirmData {
            commitment_value: self.commitment_value,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::SimplePairingConfirm,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::SimplePairingConfirm(simplepairingconfirm),
        };
        SimplePairingConfirm::new(lmppacket).unwrap()
    }
}
impl From<SimplePairingConfirmBuilder> for LmpPacket {
    fn from(builder: SimplePairingConfirmBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<SimplePairingConfirmBuilder> for SimplePairingConfirm {
    fn from(builder: SimplePairingConfirmBuilder) -> SimplePairingConfirm {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingNumberData {
    nonce: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingNumber {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    simplepairingnumber: SimplePairingNumberData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SimplePairingNumberBuilder {
    pub nonce: [u8; 16],
    pub transaction_id: u8,
}
impl SimplePairingNumberData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "SimplePairingNumber",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let nonce = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { nonce })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.nonce {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for SimplePairingNumber {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<SimplePairingNumber> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: SimplePairingNumber) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<SimplePairingNumber> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: SimplePairingNumber) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<SimplePairingNumber> for LmpPacket {
    fn from(packet: SimplePairingNumber) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for SimplePairingNumber {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<SimplePairingNumber, Self::Error> {
        SimplePairingNumber::new(packet.lmppacket)
    }
}
impl SimplePairingNumber {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let simplepairingnumber = match &lmppacket.child {
            LmpPacketDataChild::SimplePairingNumber(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::SimplePairingNumber),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            simplepairingnumber,
        })
    }
    pub fn get_nonce(&self) -> &[u8; 16] {
        &self.simplepairingnumber.nonce
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.simplepairingnumber.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl SimplePairingNumberBuilder {
    pub fn build(self) -> SimplePairingNumber {
        let simplepairingnumber = SimplePairingNumberData {
            nonce: self.nonce,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::SimplePairingNumber,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::SimplePairingNumber(simplepairingnumber),
        };
        SimplePairingNumber::new(lmppacket).unwrap()
    }
}
impl From<SimplePairingNumberBuilder> for LmpPacket {
    fn from(builder: SimplePairingNumberBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<SimplePairingNumberBuilder> for SimplePairingNumber {
    fn from(builder: SimplePairingNumberBuilder) -> SimplePairingNumber {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct DhkeyCheckData {
    confirmation_value: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct DhkeyCheck {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    dhkeycheck: DhkeyCheckData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct DhkeyCheckBuilder {
    pub confirmation_value: [u8; 16],
    pub transaction_id: u8,
}
impl DhkeyCheckData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "DhkeyCheck",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let confirmation_value = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { confirmation_value })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.confirmation_value {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for DhkeyCheck {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<DhkeyCheck> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: DhkeyCheck) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<DhkeyCheck> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: DhkeyCheck) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<DhkeyCheck> for LmpPacket {
    fn from(packet: DhkeyCheck) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for DhkeyCheck {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<DhkeyCheck, Self::Error> {
        DhkeyCheck::new(packet.lmppacket)
    }
}
impl DhkeyCheck {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let dhkeycheck = match &lmppacket.child {
            LmpPacketDataChild::DhkeyCheck(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::DhkeyCheck),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, dhkeycheck })
    }
    pub fn get_confirmation_value(&self) -> &[u8; 16] {
        &self.dhkeycheck.confirmation_value
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.dhkeycheck.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl DhkeyCheckBuilder {
    pub fn build(self) -> DhkeyCheck {
        let dhkeycheck = DhkeyCheckData {
            confirmation_value: self.confirmation_value,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::DhkeyCheck,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::DhkeyCheck(dhkeycheck),
        };
        DhkeyCheck::new(lmppacket).unwrap()
    }
}
impl From<DhkeyCheckBuilder> for LmpPacket {
    fn from(builder: DhkeyCheckBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<DhkeyCheckBuilder> for DhkeyCheck {
    fn from(builder: DhkeyCheckBuilder) -> DhkeyCheck {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AuRandData {
    random_number: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AuRand {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    aurand: AuRandData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AuRandBuilder {
    pub random_number: [u8; 16],
    pub transaction_id: u8,
}
impl AuRandData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "AuRand",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let random_number = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { random_number })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.random_number {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for AuRand {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<AuRand> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: AuRand) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<AuRand> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: AuRand) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<AuRand> for LmpPacket {
    fn from(packet: AuRand) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for AuRand {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<AuRand, Self::Error> {
        AuRand::new(packet.lmppacket)
    }
}
impl AuRand {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let aurand = match &lmppacket.child {
            LmpPacketDataChild::AuRand(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::AuRand),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, aurand })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_random_number(&self) -> &[u8; 16] {
        &self.aurand.random_number
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.aurand.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl AuRandBuilder {
    pub fn build(self) -> AuRand {
        let aurand = AuRandData {
            random_number: self.random_number,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::AuRand,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::AuRand(aurand),
        };
        AuRand::new(lmppacket).unwrap()
    }
}
impl From<AuRandBuilder> for LmpPacket {
    fn from(builder: AuRandBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<AuRandBuilder> for AuRand {
    fn from(builder: AuRandBuilder) -> AuRand {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SresData {
    authentication_rsp: [u8; 4],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Sres {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    sres: SresData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct SresBuilder {
    pub authentication_rsp: [u8; 4],
    pub transaction_id: u8,
}
impl SresData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 4
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 4 {
            return Err(DecodeError::InvalidLengthError {
                obj: "Sres",
                wanted: 4,
                got: bytes.get().remaining(),
            });
        }
        let authentication_rsp = (0..4)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { authentication_rsp })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.authentication_rsp {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        4
    }
}
impl Packet for Sres {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<Sres> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: Sres) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<Sres> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: Sres) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<Sres> for LmpPacket {
    fn from(packet: Sres) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for Sres {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<Sres, Self::Error> {
        Sres::new(packet.lmppacket)
    }
}
impl Sres {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let sres = match &lmppacket.child {
            LmpPacketDataChild::Sres(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::Sres),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, sres })
    }
    pub fn get_authentication_rsp(&self) -> &[u8; 4] {
        &self.sres.authentication_rsp
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.sres.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl SresBuilder {
    pub fn build(self) -> Sres {
        let sres = SresData {
            authentication_rsp: self.authentication_rsp,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Sres,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::Sres(sres),
        };
        Sres::new(lmppacket).unwrap()
    }
}
impl From<SresBuilder> for LmpPacket {
    fn from(builder: SresBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<SresBuilder> for Sres {
    fn from(builder: SresBuilder) -> Sres {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NumericComparisonFailedData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NumericComparisonFailed {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    numericcomparisonfailed: NumericComparisonFailedData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct NumericComparisonFailedBuilder {
    pub transaction_id: u8,
}
impl NumericComparisonFailedData {
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
impl Packet for NumericComparisonFailed {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<NumericComparisonFailed> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: NumericComparisonFailed) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<NumericComparisonFailed> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: NumericComparisonFailed) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<NumericComparisonFailed> for LmpPacket {
    fn from(packet: NumericComparisonFailed) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<NumericComparisonFailed> for ExtendedPacket {
    fn from(packet: NumericComparisonFailed) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for NumericComparisonFailed {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<NumericComparisonFailed, Self::Error> {
        NumericComparisonFailed::new(packet.lmppacket)
    }
}
impl NumericComparisonFailed {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let numericcomparisonfailed = match &extendedpacket.child {
            ExtendedPacketDataChild::NumericComparisonFailed(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(
                        ExtendedPacketDataChild::NumericComparisonFailed
                    ),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            numericcomparisonfailed,
        })
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.numericcomparisonfailed.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl NumericComparisonFailedBuilder {
    pub fn build(self) -> NumericComparisonFailed {
        let numericcomparisonfailed = NumericComparisonFailedData {};
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::NumericComparisonFailed,
            child: ExtendedPacketDataChild::NumericComparisonFailed(
                numericcomparisonfailed,
            ),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        NumericComparisonFailed::new(lmppacket).unwrap()
    }
}
impl From<NumericComparisonFailedBuilder> for LmpPacket {
    fn from(builder: NumericComparisonFailedBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<NumericComparisonFailedBuilder> for ExtendedPacket {
    fn from(builder: NumericComparisonFailedBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<NumericComparisonFailedBuilder> for NumericComparisonFailed {
    fn from(builder: NumericComparisonFailedBuilder) -> NumericComparisonFailed {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PasskeyFailedData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PasskeyFailed {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    passkeyfailed: PasskeyFailedData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct PasskeyFailedBuilder {
    pub transaction_id: u8,
}
impl PasskeyFailedData {
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
impl Packet for PasskeyFailed {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<PasskeyFailed> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: PasskeyFailed) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<PasskeyFailed> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: PasskeyFailed) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<PasskeyFailed> for LmpPacket {
    fn from(packet: PasskeyFailed) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<PasskeyFailed> for ExtendedPacket {
    fn from(packet: PasskeyFailed) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for PasskeyFailed {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<PasskeyFailed, Self::Error> {
        PasskeyFailed::new(packet.lmppacket)
    }
}
impl PasskeyFailed {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let passkeyfailed = match &extendedpacket.child {
            ExtendedPacketDataChild::PasskeyFailed(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::PasskeyFailed),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            passkeyfailed,
        })
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.passkeyfailed.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl PasskeyFailedBuilder {
    pub fn build(self) -> PasskeyFailed {
        let passkeyfailed = PasskeyFailedData {};
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::PasskeyFailed,
            child: ExtendedPacketDataChild::PasskeyFailed(passkeyfailed),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        PasskeyFailed::new(lmppacket).unwrap()
    }
}
impl From<PasskeyFailedBuilder> for LmpPacket {
    fn from(builder: PasskeyFailedBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<PasskeyFailedBuilder> for ExtendedPacket {
    fn from(builder: PasskeyFailedBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<PasskeyFailedBuilder> for PasskeyFailed {
    fn from(builder: PasskeyFailedBuilder) -> PasskeyFailed {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct KeypressNotificationData {
    notification_type: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct KeypressNotification {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    keypressnotification: KeypressNotificationData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct KeypressNotificationBuilder {
    pub notification_type: u8,
    pub transaction_id: u8,
}
impl KeypressNotificationData {
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
                obj: "KeypressNotification",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let notification_type = bytes.get_mut().get_u8();
        Ok(Self { notification_type })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.notification_type);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for KeypressNotification {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<KeypressNotification> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: KeypressNotification) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<KeypressNotification> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: KeypressNotification) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<KeypressNotification> for LmpPacket {
    fn from(packet: KeypressNotification) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<KeypressNotification> for ExtendedPacket {
    fn from(packet: KeypressNotification) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for KeypressNotification {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<KeypressNotification, Self::Error> {
        KeypressNotification::new(packet.lmppacket)
    }
}
impl KeypressNotification {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let keypressnotification = match &extendedpacket.child {
            ExtendedPacketDataChild::KeypressNotification(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::KeypressNotification),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            keypressnotification,
        })
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_notification_type(&self) -> u8 {
        self.keypressnotification.notification_type
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.keypressnotification.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl KeypressNotificationBuilder {
    pub fn build(self) -> KeypressNotification {
        let keypressnotification = KeypressNotificationData {
            notification_type: self.notification_type,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::KeypressNotification,
            child: ExtendedPacketDataChild::KeypressNotification(keypressnotification),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        KeypressNotification::new(lmppacket).unwrap()
    }
}
impl From<KeypressNotificationBuilder> for LmpPacket {
    fn from(builder: KeypressNotificationBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<KeypressNotificationBuilder> for ExtendedPacket {
    fn from(builder: KeypressNotificationBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<KeypressNotificationBuilder> for KeypressNotification {
    fn from(builder: KeypressNotificationBuilder) -> KeypressNotification {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InRandData {
    random_number: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InRand {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    inrand: InRandData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InRandBuilder {
    pub random_number: [u8; 16],
    pub transaction_id: u8,
}
impl InRandData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "InRand",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let random_number = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { random_number })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.random_number {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for InRand {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<InRand> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: InRand) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<InRand> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: InRand) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<InRand> for LmpPacket {
    fn from(packet: InRand) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for InRand {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<InRand, Self::Error> {
        InRand::new(packet.lmppacket)
    }
}
impl InRand {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let inrand = match &lmppacket.child {
            LmpPacketDataChild::InRand(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::InRand),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, inrand })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_random_number(&self) -> &[u8; 16] {
        &self.inrand.random_number
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.inrand.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl InRandBuilder {
    pub fn build(self) -> InRand {
        let inrand = InRandData {
            random_number: self.random_number,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::InRand,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::InRand(inrand),
        };
        InRand::new(lmppacket).unwrap()
    }
}
impl From<InRandBuilder> for LmpPacket {
    fn from(builder: InRandBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<InRandBuilder> for InRand {
    fn from(builder: InRandBuilder) -> InRand {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CombKeyData {
    random_number: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CombKey {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    combkey: CombKeyData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CombKeyBuilder {
    pub random_number: [u8; 16],
    pub transaction_id: u8,
}
impl CombKeyData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CombKey",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let random_number = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { random_number })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.random_number {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for CombKey {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<CombKey> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: CombKey) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<CombKey> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: CombKey) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<CombKey> for LmpPacket {
    fn from(packet: CombKey) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for CombKey {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<CombKey, Self::Error> {
        CombKey::new(packet.lmppacket)
    }
}
impl CombKey {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let combkey = match &lmppacket.child {
            LmpPacketDataChild::CombKey(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::CombKey),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self { lmppacket, combkey })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_random_number(&self) -> &[u8; 16] {
        &self.combkey.random_number
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.combkey.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl CombKeyBuilder {
    pub fn build(self) -> CombKey {
        let combkey = CombKeyData {
            random_number: self.random_number,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::CombKey,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::CombKey(combkey),
        };
        CombKey::new(lmppacket).unwrap()
    }
}
impl From<CombKeyBuilder> for LmpPacket {
    fn from(builder: CombKeyBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<CombKeyBuilder> for CombKey {
    fn from(builder: CombKeyBuilder) -> CombKey {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionModeReqData {
    encryption_mode: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionModeReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encryptionmodereq: EncryptionModeReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionModeReqBuilder {
    pub encryption_mode: u8,
    pub transaction_id: u8,
}
impl EncryptionModeReqData {
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
                obj: "EncryptionModeReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let encryption_mode = bytes.get_mut().get_u8();
        Ok(Self { encryption_mode })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.encryption_mode);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for EncryptionModeReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncryptionModeReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncryptionModeReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncryptionModeReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncryptionModeReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncryptionModeReq> for LmpPacket {
    fn from(packet: EncryptionModeReq) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for EncryptionModeReq {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<EncryptionModeReq, Self::Error> {
        EncryptionModeReq::new(packet.lmppacket)
    }
}
impl EncryptionModeReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let encryptionmodereq = match &lmppacket.child {
            LmpPacketDataChild::EncryptionModeReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::EncryptionModeReq),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            encryptionmodereq,
        })
    }
    pub fn get_encryption_mode(&self) -> u8 {
        self.encryptionmodereq.encryption_mode
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encryptionmodereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl EncryptionModeReqBuilder {
    pub fn build(self) -> EncryptionModeReq {
        let encryptionmodereq = EncryptionModeReqData {
            encryption_mode: self.encryption_mode,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::EncryptionModeReq,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::EncryptionModeReq(encryptionmodereq),
        };
        EncryptionModeReq::new(lmppacket).unwrap()
    }
}
impl From<EncryptionModeReqBuilder> for LmpPacket {
    fn from(builder: EncryptionModeReqBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<EncryptionModeReqBuilder> for EncryptionModeReq {
    fn from(builder: EncryptionModeReqBuilder) -> EncryptionModeReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionKeySizeReqData {
    key_size: u8,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionKeySizeReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    encryptionkeysizereq: EncryptionKeySizeReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct EncryptionKeySizeReqBuilder {
    pub key_size: u8,
    pub transaction_id: u8,
}
impl EncryptionKeySizeReqData {
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
                obj: "EncryptionKeySizeReq",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let key_size = bytes.get_mut().get_u8();
        Ok(Self { key_size })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.key_size);
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}
impl Packet for EncryptionKeySizeReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<EncryptionKeySizeReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: EncryptionKeySizeReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<EncryptionKeySizeReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: EncryptionKeySizeReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<EncryptionKeySizeReq> for LmpPacket {
    fn from(packet: EncryptionKeySizeReq) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for EncryptionKeySizeReq {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<EncryptionKeySizeReq, Self::Error> {
        EncryptionKeySizeReq::new(packet.lmppacket)
    }
}
impl EncryptionKeySizeReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let encryptionkeysizereq = match &lmppacket.child {
            LmpPacketDataChild::EncryptionKeySizeReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::EncryptionKeySizeReq),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            encryptionkeysizereq,
        })
    }
    pub fn get_key_size(&self) -> u8 {
        self.encryptionkeysizereq.key_size
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encryptionkeysizereq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl EncryptionKeySizeReqBuilder {
    pub fn build(self) -> EncryptionKeySizeReq {
        let encryptionkeysizereq = EncryptionKeySizeReqData {
            key_size: self.key_size,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::EncryptionKeySizeReq,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::EncryptionKeySizeReq(encryptionkeysizereq),
        };
        EncryptionKeySizeReq::new(lmppacket).unwrap()
    }
}
impl From<EncryptionKeySizeReqBuilder> for LmpPacket {
    fn from(builder: EncryptionKeySizeReqBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<EncryptionKeySizeReqBuilder> for EncryptionKeySizeReq {
    fn from(builder: EncryptionKeySizeReqBuilder) -> EncryptionKeySizeReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncryptionReqData {
    random_number: [u8; 16],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncryptionReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    startencryptionreq: StartEncryptionReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StartEncryptionReqBuilder {
    pub random_number: [u8; 16],
    pub transaction_id: u8,
}
impl StartEncryptionReqData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 16
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "StartEncryptionReq",
                wanted: 16,
                got: bytes.get().remaining(),
            });
        }
        let random_number = (0..16)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self { random_number })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        for elem in &self.random_number {
            buffer.put_u8(*elem);
        }
        Ok(())
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        16
    }
}
impl Packet for StartEncryptionReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<StartEncryptionReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: StartEncryptionReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<StartEncryptionReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: StartEncryptionReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<StartEncryptionReq> for LmpPacket {
    fn from(packet: StartEncryptionReq) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for StartEncryptionReq {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<StartEncryptionReq, Self::Error> {
        StartEncryptionReq::new(packet.lmppacket)
    }
}
impl StartEncryptionReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let startencryptionreq = match &lmppacket.child {
            LmpPacketDataChild::StartEncryptionReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::StartEncryptionReq),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            startencryptionreq,
        })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_random_number(&self) -> &[u8; 16] {
        &self.startencryptionreq.random_number
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.startencryptionreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl StartEncryptionReqBuilder {
    pub fn build(self) -> StartEncryptionReq {
        let startencryptionreq = StartEncryptionReqData {
            random_number: self.random_number,
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::StartEncryptionReq,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::StartEncryptionReq(startencryptionreq),
        };
        StartEncryptionReq::new(lmppacket).unwrap()
    }
}
impl From<StartEncryptionReqBuilder> for LmpPacket {
    fn from(builder: StartEncryptionReqBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<StartEncryptionReqBuilder> for StartEncryptionReq {
    fn from(builder: StartEncryptionReqBuilder) -> StartEncryptionReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StopEncryptionReqData {}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StopEncryptionReq {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    stopencryptionreq: StopEncryptionReqData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct StopEncryptionReqBuilder {
    pub transaction_id: u8,
}
impl StopEncryptionReqData {
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
impl Packet for StopEncryptionReq {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<StopEncryptionReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: StopEncryptionReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<StopEncryptionReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: StopEncryptionReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<StopEncryptionReq> for LmpPacket {
    fn from(packet: StopEncryptionReq) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for StopEncryptionReq {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<StopEncryptionReq, Self::Error> {
        StopEncryptionReq::new(packet.lmppacket)
    }
}
impl StopEncryptionReq {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let stopencryptionreq = match &lmppacket.child {
            LmpPacketDataChild::StopEncryptionReq(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::StopEncryptionReq),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            stopencryptionreq,
        })
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.stopencryptionreq.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl StopEncryptionReqBuilder {
    pub fn build(self) -> StopEncryptionReq {
        let stopencryptionreq = StopEncryptionReqData {};
        let lmppacket = LmpPacketData {
            opcode: Opcode::StopEncryptionReq,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::StopEncryptionReq(stopencryptionreq),
        };
        StopEncryptionReq::new(lmppacket).unwrap()
    }
}
impl From<StopEncryptionReqBuilder> for LmpPacket {
    fn from(builder: StopEncryptionReqBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<StopEncryptionReqBuilder> for StopEncryptionReq {
    fn from(builder: StopEncryptionReqBuilder) -> StopEncryptionReq {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesReqExtData {
    features_page: u8,
    max_supported_page: u8,
    extended_features: [u8; 8],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesReqExt {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    featuresreqext: FeaturesReqExtData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesReqExtBuilder {
    pub extended_features: [u8; 8],
    pub features_page: u8,
    pub max_supported_page: u8,
    pub transaction_id: u8,
}
impl FeaturesReqExtData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesReqExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let features_page = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesReqExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let max_supported_page = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesReqExt",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let extended_features = (0..8)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self {
            features_page,
            max_supported_page,
            extended_features,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.features_page);
        buffer.put_u8(self.max_supported_page);
        for elem in &self.extended_features {
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
impl Packet for FeaturesReqExt {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<FeaturesReqExt> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: FeaturesReqExt) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<FeaturesReqExt> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: FeaturesReqExt) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<FeaturesReqExt> for LmpPacket {
    fn from(packet: FeaturesReqExt) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<FeaturesReqExt> for ExtendedPacket {
    fn from(packet: FeaturesReqExt) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for FeaturesReqExt {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<FeaturesReqExt, Self::Error> {
        FeaturesReqExt::new(packet.lmppacket)
    }
}
impl FeaturesReqExt {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let featuresreqext = match &extendedpacket.child {
            ExtendedPacketDataChild::FeaturesReqExt(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::FeaturesReqExt),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            featuresreqext,
        })
    }
    pub fn get_extended_features(&self) -> &[u8; 8] {
        &self.featuresreqext.extended_features
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_features_page(&self) -> u8 {
        self.featuresreqext.features_page
    }
    pub fn get_max_supported_page(&self) -> u8 {
        self.featuresreqext.max_supported_page
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.featuresreqext.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl FeaturesReqExtBuilder {
    pub fn build(self) -> FeaturesReqExt {
        let featuresreqext = FeaturesReqExtData {
            extended_features: self.extended_features,
            features_page: self.features_page,
            max_supported_page: self.max_supported_page,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::FeaturesReq,
            child: ExtendedPacketDataChild::FeaturesReqExt(featuresreqext),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        FeaturesReqExt::new(lmppacket).unwrap()
    }
}
impl From<FeaturesReqExtBuilder> for LmpPacket {
    fn from(builder: FeaturesReqExtBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<FeaturesReqExtBuilder> for ExtendedPacket {
    fn from(builder: FeaturesReqExtBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<FeaturesReqExtBuilder> for FeaturesReqExt {
    fn from(builder: FeaturesReqExtBuilder) -> FeaturesReqExt {
        builder.build().into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesResExtData {
    features_page: u8,
    max_supported_page: u8,
    extended_features: [u8; 8],
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesResExt {
    #[cfg_attr(feature = "serde", serde(flatten))]
    lmppacket: LmpPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    extendedpacket: ExtendedPacketData,
    #[cfg_attr(feature = "serde", serde(flatten))]
    featuresresext: FeaturesResExtData,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FeaturesResExtBuilder {
    pub extended_features: [u8; 8],
    pub features_page: u8,
    pub max_supported_page: u8,
    pub transaction_id: u8,
}
impl FeaturesResExtData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 10
    }
    fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesResExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let features_page = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesResExt",
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let max_supported_page = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 8 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FeaturesResExt",
                wanted: 8,
                got: bytes.get().remaining(),
            });
        }
        let extended_features = (0..8)
            .map(|_| Ok::<_, DecodeError>(bytes.get_mut().get_u8()))
            .collect::<Result<Vec<_>, DecodeError>>()?
            .try_into()
            .map_err(|_| DecodeError::InvalidPacketError)?;
        Ok(Self {
            features_page,
            max_supported_page,
            extended_features,
        })
    }
    fn write_to<T: BufMut>(&self, buffer: &mut T) -> Result<(), EncodeError> {
        buffer.put_u8(self.features_page);
        buffer.put_u8(self.max_supported_page);
        for elem in &self.extended_features {
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
impl Packet for FeaturesResExt {
    fn encoded_len(&self) -> usize {
        self.get_size()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.lmppacket.write_to(buf)
    }
    fn decode(_: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        unimplemented!("Rust legacy does not implement full packet trait")
    }
}
impl TryFrom<FeaturesResExt> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: FeaturesResExt) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<FeaturesResExt> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: FeaturesResExt) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl From<FeaturesResExt> for LmpPacket {
    fn from(packet: FeaturesResExt) -> LmpPacket {
        LmpPacket::new(packet.lmppacket).unwrap()
    }
}
impl From<FeaturesResExt> for ExtendedPacket {
    fn from(packet: FeaturesResExt) -> ExtendedPacket {
        ExtendedPacket::new(packet.lmppacket).unwrap()
    }
}
impl TryFrom<LmpPacket> for FeaturesResExt {
    type Error = DecodeError;
    fn try_from(packet: LmpPacket) -> Result<FeaturesResExt, Self::Error> {
        FeaturesResExt::new(packet.lmppacket)
    }
}
impl FeaturesResExt {
    pub fn parse(bytes: &[u8]) -> Result<Self, DecodeError> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self, DecodeError> {
        let data = LmpPacketData::parse_inner(&mut bytes)?;
        Self::new(data)
    }
    fn new(lmppacket: LmpPacketData) -> Result<Self, DecodeError> {
        let extendedpacket = match &lmppacket.child {
            LmpPacketDataChild::ExtendedPacket(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(LmpPacketDataChild::ExtendedPacket),
                    actual: format!("{:?}", & lmppacket.child),
                });
            }
        };
        let featuresresext = match &extendedpacket.child {
            ExtendedPacketDataChild::FeaturesResExt(value) => value.clone(),
            _ => {
                return Err(DecodeError::InvalidChildError {
                    expected: stringify!(ExtendedPacketDataChild::FeaturesResExt),
                    actual: format!("{:?}", & extendedpacket.child),
                });
            }
        };
        Ok(Self {
            lmppacket,
            extendedpacket,
            featuresresext,
        })
    }
    pub fn get_extended_features(&self) -> &[u8; 8] {
        &self.featuresresext.extended_features
    }
    pub fn get_extended_opcode(&self) -> ExtendedOpcode {
        self.extendedpacket.extended_opcode
    }
    pub fn get_features_page(&self) -> u8 {
        self.featuresresext.features_page
    }
    pub fn get_max_supported_page(&self) -> u8 {
        self.featuresresext.max_supported_page
    }
    pub fn get_opcode(&self) -> Opcode {
        self.lmppacket.opcode
    }
    pub fn get_transaction_id(&self) -> u8 {
        self.lmppacket.transaction_id
    }
    fn write_to(&self, buffer: &mut impl BufMut) -> Result<(), EncodeError> {
        self.featuresresext.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.lmppacket.get_size()
    }
}
impl FeaturesResExtBuilder {
    pub fn build(self) -> FeaturesResExt {
        let featuresresext = FeaturesResExtData {
            extended_features: self.extended_features,
            features_page: self.features_page,
            max_supported_page: self.max_supported_page,
        };
        let extendedpacket = ExtendedPacketData {
            extended_opcode: ExtendedOpcode::FeaturesRes,
            child: ExtendedPacketDataChild::FeaturesResExt(featuresresext),
        };
        let lmppacket = LmpPacketData {
            opcode: Opcode::Escaped,
            transaction_id: self.transaction_id,
            child: LmpPacketDataChild::ExtendedPacket(extendedpacket),
        };
        FeaturesResExt::new(lmppacket).unwrap()
    }
}
impl From<FeaturesResExtBuilder> for LmpPacket {
    fn from(builder: FeaturesResExtBuilder) -> LmpPacket {
        builder.build().into()
    }
}
impl From<FeaturesResExtBuilder> for ExtendedPacket {
    fn from(builder: FeaturesResExtBuilder) -> ExtendedPacket {
        builder.build().into()
    }
}
impl From<FeaturesResExtBuilder> for FeaturesResExt {
    fn from(builder: FeaturesResExtBuilder) -> FeaturesResExt {
        builder.build().into()
    }
}

