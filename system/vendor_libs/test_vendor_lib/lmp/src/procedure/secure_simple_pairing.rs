use std::convert::TryInto;

use num_traits::{FromPrimitive, ToPrimitive};

use crate::either::Either;
use crate::packets::{hci, lmp};
use crate::procedure::Context;

async fn send_public_key(ctx: &impl Context, transaction_id: u8, key: &[u8; 48]) {
    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::EncapsulatedHeaderBuilder {
                transaction_id,
                major_type: 1,
                minor_type: 1,
                payload_length: 48,
            }
            .build(),
        )
        .await;

    for chunk in key.chunks(16) {
        // TODO: handle error
        let _ = ctx
            .send_accepted_lmp_packet(
                lmp::EncapsulatedPayloadBuilder { transaction_id, data: chunk.try_into().unwrap() }
                    .build(),
            )
            .await;
    }
}

async fn receive_public_key(ctx: &impl Context, transaction_id: u8) -> [u8; 48] {
    let _ = ctx.receive_lmp_packet::<lmp::EncapsulatedHeaderPacket>().await;
    ctx.send_lmp_packet(
        lmp::AcceptedBuilder { transaction_id, accepted_opcode: lmp::Opcode::EncapsulatedHeader }
            .build(),
    );

    let mut key = [0; 48];

    for chunk in key.chunks_mut(16) {
        let payload = ctx.receive_lmp_packet::<lmp::EncapsulatedPayloadPacket>().await;
        chunk.copy_from_slice(payload.get_data().as_slice());
        ctx.send_lmp_packet(
            lmp::AcceptedBuilder {
                transaction_id,
                accepted_opcode: lmp::Opcode::EncapsulatedPayload,
            }
            .build(),
        );
    }

    key
}

pub async fn initiate(ctx: &impl Context) -> bool {
    let bd_addr = hci::Address { bytes: ctx.peer_addr() };

    ctx.send_hci_event(hci::IoCapabilityRequestBuilder { bd_addr }.build());
    {
        let reply = ctx.receive_hci_command::<hci::IoCapabilityRequestReplyPacket>().await;
        ctx.send_hci_event(
            hci::IoCapabilityRequestReplyCompleteBuilder {
                num_hci_command_packets: 1,
                status: hci::ErrorCode::Success,
                bd_addr,
            }
            .build(),
        );

        ctx.send_lmp_packet(
            lmp::IoCapabilityReqBuilder {
                transaction_id: 0,
                io_capabilities: reply.get_io_capability().to_u8().unwrap(),
                oob_authentication_data: reply.get_oob_present().to_u8().unwrap(),
                authentication_requirement: reply
                    .get_authentication_requirements()
                    .to_u8()
                    .unwrap(),
            }
            .build(),
        );
    }
    {
        let response = ctx.receive_lmp_packet::<lmp::IoCapabilityResPacket>().await;
        ctx.send_hci_event(
            hci::IoCapabilityResponseBuilder {
                bd_addr,
                io_capability: hci::IoCapability::from_u8(response.get_io_capabilities()).unwrap(),
                oob_data_present: hci::OobDataPresent::from_u8(
                    response.get_oob_authentication_data(),
                )
                .unwrap(),
                authentication_requirements: hci::AuthenticationRequirements::from_u8(
                    response.get_authentication_requirement(),
                )
                .unwrap(),
            }
            .build(),
        );
    }

    // Public Key Exchange
    {
        let public_key = [0; 48];
        send_public_key(ctx, 0, &public_key).await;
        let _key = receive_public_key(ctx, 0).await;
    }

    // Authentication Stage 1
    {
        let confirm = ctx.receive_lmp_packet::<lmp::SimplePairingConfirmPacket>().await;

        let commitment_value = [0; 16];

        if confirm.get_commitment_value() != &commitment_value {
            todo!();
        }
    }

    {
        let nonce = [0; 16];

        // TODO: handle error
        let _ = ctx
            .send_accepted_lmp_packet(
                lmp::SimplePairingNumberBuilder { transaction_id: 0, nonce }.build(),
            )
            .await;
    }
    {
        let _pairing_number = ctx.receive_lmp_packet::<lmp::SimplePairingNumberPacket>().await;
        // TODO: check pairing number
        ctx.send_lmp_packet(
            lmp::AcceptedBuilder {
                transaction_id: 0,
                accepted_opcode: lmp::Opcode::SimplePairingNumber,
            }
            .build(),
        );

        ctx.send_hci_event(
            hci::UserConfirmationRequestBuilder { bd_addr, numeric_value: 0 }.build(),
        );
    }
    match ctx
        .receive_hci_command::<Either<
            hci::UserConfirmationRequestReplyPacket,
            hci::UserConfirmationRequestNegativeReplyPacket,
        >>()
        .await
    {
        Either::Left(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestReplyCompleteBuilder {
                    num_hci_command_packets: 1,
                    status: hci::ErrorCode::Success,
                    bd_addr,
                }
                .build(),
            );
        }
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets: 1,
                    status: hci::ErrorCode::Success,
                    bd_addr,
                }
                .build(),
            );
            ctx.send_lmp_packet(lmp::NumericComparaisonFailedBuilder { transaction_id: 0 }.build());
            ctx.send_hci_event(
                hci::SimplePairingCompleteBuilder {
                    status: hci::ErrorCode::AuthenticationFailure,
                    bd_addr,
                }
                .build(),
            );
            return false;
        }
    };
    // Authentication Stage 2
    {
        let confirmation_value = [0; 16];

        let result = ctx
            .send_accepted_lmp_packet(
                lmp::DhkeyCheckBuilder { transaction_id: 0, confirmation_value }.build(),
            )
            .await;

        if result.is_err() {
            ctx.send_hci_event(
                hci::SimplePairingCompleteBuilder {
                    status: hci::ErrorCode::AuthenticationFailure,
                    bd_addr,
                }
                .build(),
            );
            return false;
        }
    }

    {
        // TODO: check dhkey
        let _dhkey = ctx.receive_lmp_packet::<lmp::DhkeyCheckPacket>().await;
        ctx.send_lmp_packet(
            lmp::AcceptedBuilder { transaction_id: 0, accepted_opcode: lmp::Opcode::DhkeyCheck }
                .build(),
        );
    }

    ctx.send_hci_event(
        hci::SimplePairingCompleteBuilder { status: hci::ErrorCode::Success, bd_addr }.build(),
    );

    true
}

pub async fn respond(ctx: &impl Context, request: lmp::IoCapabilityReqPacket) -> bool {
    let bd_addr = hci::Address { bytes: ctx.peer_addr() };

    ctx.send_hci_event(
        hci::IoCapabilityResponseBuilder {
            bd_addr,
            io_capability: hci::IoCapability::from_u8(request.get_io_capabilities()).unwrap(),
            oob_data_present: hci::OobDataPresent::from_u8(request.get_oob_authentication_data())
                .unwrap(),
            authentication_requirements: hci::AuthenticationRequirements::from_u8(
                request.get_authentication_requirement(),
            )
            .unwrap(),
        }
        .build(),
    );

    ctx.send_hci_event(hci::IoCapabilityRequestBuilder { bd_addr }.build());
    let reply = ctx.receive_hci_command::<hci::IoCapabilityRequestReplyPacket>().await;
    ctx.send_hci_event(
        hci::IoCapabilityRequestReplyCompleteBuilder {
            num_hci_command_packets: 1,
            status: hci::ErrorCode::Success,
            bd_addr,
        }
        .build(),
    );

    ctx.send_lmp_packet(
        lmp::IoCapabilityResBuilder {
            transaction_id: 0,
            io_capabilities: reply.get_io_capability().to_u8().unwrap(),
            oob_authentication_data: reply.get_oob_present().to_u8().unwrap(),
            authentication_requirement: reply.get_authentication_requirements().to_u8().unwrap(),
        }
        .build(),
    );

    // Public Key Exchange

    let public_key = [0; 48];
    let _key = receive_public_key(ctx, 0).await;
    send_public_key(ctx, 0, &public_key).await;

    // Authentication Stage 1

    let commitment_value = [0; 16];

    ctx.send_lmp_packet(
        lmp::SimplePairingConfirmBuilder { transaction_id: 0, commitment_value }.build(),
    );

    let _pairing_number = ctx.receive_lmp_packet::<lmp::SimplePairingNumberPacket>().await;
    // TODO: check pairing number
    ctx.send_lmp_packet(
        lmp::AcceptedBuilder {
            transaction_id: 0,
            accepted_opcode: lmp::Opcode::SimplePairingNumber,
        }
        .build(),
    );

    let nonce = [0; 16];

    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::SimplePairingNumberBuilder { transaction_id: 0, nonce }.build(),
        )
        .await;

    ctx.send_hci_event(hci::UserConfirmationRequestBuilder { bd_addr, numeric_value: 0 }.build());

    let confirmation_negative_reply = match ctx
        .receive_hci_command::<Either<
            hci::UserConfirmationRequestReplyPacket,
            hci::UserConfirmationRequestNegativeReplyPacket,
        >>()
        .await
    {
        Either::Left(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestReplyCompleteBuilder {
                    num_hci_command_packets: 1,
                    status: hci::ErrorCode::Success,
                    bd_addr,
                }
                .build(),
            );
            false
        }
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets: 1,
                    status: hci::ErrorCode::Success,
                    bd_addr,
                }
                .build(),
            );
            true
        }
    };

    let _dhkey = match ctx
        .receive_lmp_packet::<Either<lmp::NumericComparaisonFailedPacket, lmp::DhkeyCheckPacket>>()
        .await
    {
        Either::Left(_) => {
            // Numeric comparaison failed
            ctx.send_hci_event(
                hci::SimplePairingCompleteBuilder {
                    status: hci::ErrorCode::AuthenticationFailure,
                    bd_addr,
                }
                .build(),
            );
            return false;
        }
        Either::Right(dhkey) => dhkey,
    };

    if confirmation_negative_reply {
        ctx.send_lmp_packet(
            lmp::NotAcceptedBuilder {
                transaction_id: 0,
                not_accepted_opcode: lmp::Opcode::DhkeyCheck,
                error_code: hci::ErrorCode::AuthenticationFailure.to_u8().unwrap(),
            }
            .build(),
        );
        ctx.send_hci_event(
            hci::SimplePairingCompleteBuilder {
                status: hci::ErrorCode::AuthenticationFailure,
                bd_addr,
            }
            .build(),
        );
        return false;
    }

    // Authentication Stage 2

    let confirmation_value = [0; 16];

    ctx.send_lmp_packet(
        lmp::AcceptedBuilder { transaction_id: 0, accepted_opcode: lmp::Opcode::DhkeyCheck }
            .build(),
    );

    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::DhkeyCheckBuilder { transaction_id: 0, confirmation_value }.build(),
        )
        .await;

    ctx.send_hci_event(
        hci::SimplePairingCompleteBuilder { status: hci::ErrorCode::Success, bd_addr }.build(),
    );

    true
}
