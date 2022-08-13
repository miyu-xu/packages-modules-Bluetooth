use std::convert::TryInto;

use crate::rfcomm::packet_parse::parse_rfcomm_packet;
use crate::{
    l2cap::{
        demultiplexer::{self, DemultiplexedReceiver, Demultiplexer},
        owned_handle::OwnedHandle,
        types::IdentityAddress,
        ChannelSendStatus, L2cap, L2capChannel,
    },
    rfcomm::Dlci,
};
use anyhow::{bail, ensure, Result};
use bytes::Bytes;
use log::error;
use tokio::{
    select, spawn,
    sync::{
        mpsc::{channel, Receiver, Sender},
        oneshot,
    },
};

use super::packet_parse::{ControlPacketContents, RfcommPacketContents};
use super::{packet_parse::RfcommPacket, RfcommChannel, RFCOMM_PSM};
use bt_packets::rfcomm::{
    AcknowledgementFramePacket, Basic1Child, Basic1Packet, BasicFrameBuilder, BasicFrameChild,
    BasicFramePacket, CommandChild, CommandHeaderChild, FlowControlPseudoConvergenceLayer,
    InformationFrame1Child, InformationFrame1WithCreditsChild,
    InformationFrame1WithoutCreditsChild, Packet, SabmFrameBuilder, SabmFramePacket, TestChild,
};

// TODO: handle clashes where both sides try to open the channel simultaneously (retry w/ random time interval)

pub enum RfcommRole {
    /// If we are initiating a connection, we need to create the channel ourselves and supply it to our caller
    Initiator(IdentityAddress),
    /// If we are the responder for an RFCOMM connection, then a channel is already opened for us and we need to establish a channel
    Responder(L2capChannel),
}

/// This loop is only responsible for events that go outside each DLCI
/// e.g. initial connection setup, global flow control (if enabled), creating a new channel (and other operations on the control DLCI that are not channel-specific)
/// All other events are forwarded to the DLCI-multiplexer (which handles both control + data packets, to keep them in order)
/// Connection shutoff is done from each channel handler, and we peek at the outgoing packets to see if we need to take down the L2CAP channel
/// After we begin L2CAP takedown (synchronously), we can process no further events. TODO: now what?
async fn event_loop(role: RfcommRole, l2cap: L2cap, event_rx: Receiver<RfcommEvent>) {
    // before accepting events, we need to open the channel
    // if the channel fails to open, we will drop the receiver, so events will be rejected and callers will know to retry (as opposed to returning with an error)

    // match role {
    //     Initiator(addr) => {
    //         let channel = l2cap.create_channel(RFCOMM_PSM, addr).await?;
    //         channel.read()?
    //     }
    // }
}

struct L2capChannelForRfcomm(L2capChannel);

impl L2capChannelForRfcomm {
    async fn read(&mut self) -> Result<BasicFramePacket> {
        Ok(BasicFramePacket::parse(&self.0.read().await?)?)
    }

    async fn write(&mut self, packet: impl Into<BasicFramePacket>) -> Result<ChannelSendStatus> {
        // TODO: generate FCS, even if PDL won't
        self.0.write(packet.into().to_vec().into_boxed_slice()).await
    }

    async fn read_sabm(&mut self) -> Result<SabmFramePacket> {
        match self.read().await?.specialize() {
            BasicFrameChild::Basic1(basic1) => match basic1.specialize() {
                Basic1Child::SabmFrame(sabm) => Ok(sabm),
                _ => bail!("expected sabm"),
            },
            _ => bail!("expected basic1"),
        }
    }

    async fn read_ack(&mut self) -> Result<AcknowledgementFramePacket> {
        match self.read().await?.specialize() {
            BasicFrameChild::Basic1(basic1) => match basic1.specialize() {
                Basic1Child::AcknowledgementFrame(ack) => Ok(ack),
                _ => bail!("expected ack"),
            },
            _ => bail!("expected basic1"),
        }
    }
}

struct RfcommMultiplexer {
    l2cap_channel: L2capChannelForRfcomm,
}

impl RfcommMultiplexer {
    pub async fn create_as_initiator(
        l2cap: L2cap,
        target: IdentityAddress,
        connected_channels: Sender<RfcommChannel>,
    ) -> Result<()> {
        let mut l2cap_channel =
            L2capChannelForRfcomm(l2cap.create_channel(RFCOMM_PSM, target).await?);

        // open control channel
        l2cap_channel
            .write(SabmFrameBuilder { frame_command_response: 1, frame_dlci: 0, fcs: 0 })
            .await?;
        let ack = l2cap_channel.read_ack().await?;
        ensure!(ack.get_frame_dlci() == 0, "received ack for unexpected channel");

        Self { l2cap_channel }.start().await;

        todo!()
        // TODO: teardown channel
        // TODO: idle timer
    }

    async fn start(mut self) -> Result<()> {
        loop {
            select! {
                incoming_packet = self.l2cap_channel.read() => {
                    match incoming_packet {
                        Ok(incoming_packet) => {
                            self.handle_incoming_packet(parse_rfcomm_packet(incoming_packet)?).await?;
                        },
                        Err(err) => {
                            error!("could not parse incoming RFCOMM packet with err: {}, dropping packet", err);
                        }
                    };
                }
                // Some(outgoing_packet) = packet_rx.recv() => {
                //     match outgoing_packet {
                //         RfcommChannelOutgoingEvent::Packet { packet, ack } => {
                //             match l2cap_channel.write(packet).await {
                //                 Ok(_) => { ack.send(()); },
                //                 Err(_) => { drop(ack); },
                //             }
                //         }
                //     }
                // }
            }
        }
    }

    async fn handle_incoming_packet(&mut self, packet: RfcommPacket) -> Result<()> {
        if packet.dlci == Dlci::Control {
            self.handle_control_packet(packet.contents).await;
        }
        let contents = match packet.contents {
            RfcommPacketContents::ControlRequest(contents) => Some(contents),
            RfcommPacketContents::ControlResponse(contents) => Some(contents),
            _ => None,
        };
        if let Some(ControlPacketContents::Parameters { flow_control, .. }) = contents {
            todo!("even though this is channel-specific, we need to use the flow_control value globally")
        }
        todo!()
    }

    async fn handle_control_packet(&mut self, packet: RfcommPacketContents) {
        todo!()
    }

    async fn dispatch_data_packet(&mut self) {}
}

pub enum RfcommEvent {}

#[derive(Debug)]
pub struct MultiplexerHandle {
    event_tx: Sender<RfcommEvent>,
    event_loop: OwnedHandle<()>,
}

impl MultiplexerHandle {
    pub fn new(role: RfcommRole, l2cap: L2cap) -> Self {
        let (event_tx, event_rx) = channel(16);
        let event_loop = spawn(event_loop(role, l2cap, event_rx)).into();
        MultiplexerHandle { event_tx, event_loop }
    }
}
