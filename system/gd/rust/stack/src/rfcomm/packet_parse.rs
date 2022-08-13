use anyhow::{bail, Result};
use bt_packets::rfcomm::{
    AcknowledgementFramePacket, Basic1Child, Basic1Packet, BasicFrameBuilder, BasicFrameChild,
    BasicFramePacket, CommandChild, CommandHeaderChild, FlowControlPseudoConvergenceLayer,
    InformationFrame1Child, InformationFrame1WithCreditsChild,
    InformationFrame1WithoutCreditsChild, Packet, SabmFrameBuilder, SabmFramePacket, TestChild,
};
use bytes::Bytes;
use std::convert::TryInto;

use super::Dlci;

pub struct RfcommPacket {
    pub dlci: Dlci,
    pub contents: RfcommPacketContents,
}

pub enum RfcommPacketContents {
    StartChannel,
    Disconnect,
    Ack,
    IsDisconnected,
    Data(Bytes),
    DataAndCredits(Bytes, u8),
    ControlRequest(ControlPacketContents),
    ControlResponse(ControlPacketContents),
}

pub enum ControlPacketContents {
    Parameters {
        flow_control: bool,
        maximum_frame_size: u16,
    },
    Echo(Bytes),
    /// this is handled by individual channels
    ModemInfo {
        stop_flow: bool,
    },
    // this is handled by DLCI 0, and means that *all* flow should stop
    GlobalFlow {
        stop_flow: bool,
    },
}

pub fn parse_rfcomm_packet(packet: BasicFramePacket) -> Result<RfcommPacket> {
    let frame_dlci = packet.get_frame_dlci().try_into()?;
    Ok(match packet.specialize() {
        BasicFrameChild::Basic1(packet) => match packet.specialize() {
            Basic1Child::SabmFrame(packet) => {
                RfcommPacket { dlci: frame_dlci, contents: RfcommPacketContents::StartChannel }
            }
            Basic1Child::AcknowledgementFrame(packet) => {
                RfcommPacket { dlci: frame_dlci, contents: RfcommPacketContents::Ack }
            }
            Basic1Child::IsDisconnectedFrame(packet) => {
                RfcommPacket { dlci: frame_dlci, contents: RfcommPacketContents::IsDisconnected }
            }
            Basic1Child::DisconnectionFrame(packet) => {
                RfcommPacket { dlci: frame_dlci, contents: RfcommPacketContents::Disconnect }
            }
            Basic1Child::InformationFrame1(packet) => match packet.specialize() {
                InformationFrame1Child::InformationFrame1WithoutCredits(packet) => match packet
                    .specialize()
                {
                    InformationFrame1WithoutCreditsChild::CommandHeader(command) => {
                        let (dlci, contents) = match command.specialize() {
                            CommandHeaderChild::Command(command) => match command.specialize() {
                                CommandChild::ParameterNegotiation(command) => (
                                    command.get_message_dlci().try_into()?,
                                    ControlPacketContents::Parameters {
                                        flow_control: command.get_flow_control()
                                            == if command.get_message_command_response() != 0 {
                                                FlowControlPseudoConvergenceLayer::CommandSupported
                                            } else {
                                                FlowControlPseudoConvergenceLayer::ResponseSupported
                                            },
                                        maximum_frame_size: command.get_maximum_frame_size(),
                                    },
                                ),
                                CommandChild::ModemStatus(command) => (
                                    command.get_message_dlci().try_into()?,
                                    ControlPacketContents::ModemInfo {
                                        stop_flow: command.get_flow_control() != 0,
                                    },
                                ),
                                CommandChild::Test(command) => match command.specialize() {
                                    TestChild::Payload(payload) => {
                                        (Dlci::Control, ControlPacketContents::Echo(payload))
                                    }
                                    TestChild::None => {
                                        bail!("invalid packet type, cannot parse")
                                    }
                                },
                                CommandChild::EnableFlowControl(_) => (
                                    Dlci::Control,
                                    ControlPacketContents::GlobalFlow { stop_flow: false },
                                ),
                                CommandChild::DisableFlowControl(_) => (
                                    Dlci::Control,
                                    ControlPacketContents::GlobalFlow { stop_flow: true },
                                ),
                                CommandChild::None => {
                                    bail!("invalid packet type, cannot parse")
                                }
                            },
                            CommandHeaderChild::CommandWithExtendedLength(command) => {
                                todo!("we don't yet support extended length...")
                            }
                            CommandHeaderChild::None => {
                                bail!("invalid packet type, cannot parse")
                            }
                        };
                        RfcommPacket {
                            dlci,
                            contents: if command.get_message_command_response() != 0 {
                                RfcommPacketContents::ControlRequest(contents)
                            } else {
                                RfcommPacketContents::ControlResponse(contents)
                            },
                        }
                    }
                    InformationFrame1WithoutCreditsChild::Payload(payload) => RfcommPacket {
                        dlci: frame_dlci,
                        contents: RfcommPacketContents::Data(payload),
                    },
                    InformationFrame1WithoutCreditsChild::None => {
                        bail!("invalid packet type, cannot parse")
                    }
                },
                InformationFrame1Child::InformationFrame1WithCredits(packet) => RfcommPacket {
                    dlci: frame_dlci,
                    contents: RfcommPacketContents::DataAndCredits(
                        match packet.specialize() {
                            InformationFrame1WithCreditsChild::Payload(payload) => payload,
                            InformationFrame1WithCreditsChild::None => {
                                bail!("invalid packet type, cannot parse")
                            }
                        },
                        packet.get_num_credits(),
                    ),
                },
                InformationFrame1Child::None => bail!("invalid packet type, cannot parse"),
            },
            Basic1Child::None => bail!("invalid packet type, cannot parse"),
        },
        BasicFrameChild::Basic2(packet) => todo!("we don't yet support extended length..."),
        BasicFrameChild::None => bail!("invalid packet type, cannot parse"),
    })
}
