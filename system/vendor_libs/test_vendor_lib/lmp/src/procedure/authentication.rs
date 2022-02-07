use crate::either::Either;
use crate::packets::{hci, lmp};
use crate::procedure::legacy_pairing;
use crate::procedure::secure_simple_pairing;
use crate::procedure::Context;

pub async fn send_authentication_challenge(ctx: &impl Context, transaction_id: u8) {
    ctx.send_lmp_packet(lmp::AuRandBuilder { transaction_id, random_number: [0; 16] }.build());
    let _ = ctx.receive_lmp_packet::<lmp::SresPacket>().await;
}

pub async fn initiate(ctx: &impl Context) {
    let bd_addr = hci::Address { bytes: ctx.peer_addr() };

    let request = ctx.receive_hci_command::<hci::AuthenticationRequestedPacket>().await;
    ctx.send_hci_event(
        hci::AuthenticationRequestedStatusBuilder {
            num_hci_command_packets: 1,
            status: hci::ErrorCode::Success,
        }
        .build(),
    );

    ctx.send_hci_event(hci::LinkKeyRequestBuilder { bd_addr }.build());

    match ctx.receive_hci_command::<Either<
        hci::LinkKeyRequestReplyPacket,
        hci::LinkKeyRequestNegativeReplyPacket,
    >>().await {
        Either::Left(_reply) => {},
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::LinkKeyRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets: 1,
                    status: hci::ErrorCode::Success,
                    bd_addr,
                }
                .build(),
            );

            let success = if ctx.secure_simple_pairing_supported() {
                secure_simple_pairing::initiate(ctx).await
            } else {
                legacy_pairing::initiate(ctx).await
            };

            if !success {
                ctx.send_hci_event(
                    hci::AuthenticationCompleteBuilder {
                        status: hci::ErrorCode::AuthenticationFailure,
                        connection_handle: request.get_connection_handle(),
                    }
                    .build(),
                );
                return;
            }
        }
    }

    send_authentication_challenge(ctx, 0).await;

    // Link Key Calculation

    let _random_number = ctx.receive_lmp_packet::<lmp::AuRandPacket>().await;

    // TODO: Resolve authentication challenge
    ctx.send_lmp_packet(lmp::SresBuilder { transaction_id: 0, authentication_rsp: [0; 4] }.build());

    ctx.send_hci_event(
        hci::LinkKeyNotificationBuilder {
            bd_addr,
            key_type: hci::KeyType::AuthenticatedP192,
            link_key: [0; 16],
        }
        .build(),
    );

    ctx.send_hci_event(
        hci::AuthenticationCompleteBuilder {
            status: hci::ErrorCode::Success,
            connection_handle: request.get_connection_handle(),
        }
        .build(),
    );
}

pub async fn respond(ctx: &impl Context) {
    let bd_addr = hci::Address { bytes: ctx.peer_addr() };

    let pairing = match ctx.receive_lmp_packet::<Either<
        lmp::AuRandPacket,
        Either<lmp::IoCapabilityReqPacket, lmp::InRandPacket>
    >>()
    .await
    {
        Either::Left(_au_rand) => false,
        Either::Right(Either::Left(io_capability_request)) => {
            let success = secure_simple_pairing::respond(ctx, io_capability_request).await;

            if !success {
                return;
            }

            true
        }
        Either::Right(Either::Right(in_rand)) => {
            let success = legacy_pairing::respond(ctx, in_rand).await;

            if !success {
                return;
            }

            true
        }
    };

    // Link Key Calculation

    let _random_number = ctx.receive_lmp_packet::<lmp::AuRandPacket>().await;
    // TODO: Resolve authentication challenge
    ctx.send_lmp_packet(lmp::SresBuilder { transaction_id: 0, authentication_rsp: [0; 4] }.build());

    send_authentication_challenge(ctx, 0).await;

    if pairing {
        ctx.send_hci_event(
            hci::LinkKeyNotificationBuilder {
                bd_addr,
                key_type: hci::KeyType::AuthenticatedP192,
                link_key: [0; 16],
            }
            .build(),
        );
    }
}
