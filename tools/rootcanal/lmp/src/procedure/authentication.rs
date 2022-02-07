use crate::either::Either;
use crate::num_hci_command_packets;
use crate::packets::{hci, lmp};
use crate::procedure::features;
use crate::procedure::legacy_pairing;
use crate::procedure::secure_simple_pairing;
use crate::procedure::Context;

async fn secure_simple_pairing_supported(ctx: &impl Context) -> bool {
    let ssp_bit = 0x1;
    let local_supported = ctx.extended_features(1)[0] & ssp_bit != 0;
    // Lazy peer features
    let peer_supported = async move {
        let page = if let Some(page) = ctx.peer_extended_features(1) {
            page
        } else {
            features::initiate(ctx, 1).await
        };
        page[0] & ssp_bit != 0
    };
    local_supported && peer_supported.await
}

pub async fn send_authentication_challenge(ctx: &impl Context, transaction_id: u8) {
    ctx.send_lmp_packet(lmp::AuRandBuilder { transaction_id, random_number: [0; 16] }.build());
    let _ = ctx.receive_lmp_packet::<lmp::SresPacket>().await;
}

pub async fn initiate(ctx: &impl Context) {
    let _ = ctx.receive_hci_command::<hci::AuthenticationRequestedPacket>().await;
    ctx.send_hci_event(
        hci::AuthenticationRequestedStatusBuilder {
            num_hci_command_packets,
            status: hci::ErrorCode::Success,
        }
        .build(),
    );

    ctx.send_hci_event(hci::LinkKeyRequestBuilder { bd_addr: ctx.peer_address() }.build());

    let pairing = match ctx.receive_hci_command::<Either<
        hci::LinkKeyRequestReplyPacket,
        hci::LinkKeyRequestNegativeReplyPacket,
    >>().await {
        Either::Left(_reply) => false,
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::LinkKeyRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );

            let success = if secure_simple_pairing_supported(ctx).await {
                secure_simple_pairing::initiate(ctx).await
            } else {
                legacy_pairing::initiate(ctx).await
            };

            if !success {
                ctx.send_hci_event(
                    hci::AuthenticationCompleteBuilder {
                        status: hci::ErrorCode::AuthenticationFailure,
                        connection_handle: ctx.peer_handle(),
                    }
                    .build(),
                );
                return;
            }
            true
        }
    };

    send_authentication_challenge(ctx, 0).await;

    // Link Key Calculation
    if pairing {
        let _random_number = ctx.receive_lmp_packet::<lmp::AuRandPacket>().await;

        // TODO: Resolve authentication challenge
        ctx.send_lmp_packet(
            lmp::SresBuilder { transaction_id: 0, authentication_rsp: [0; 4] }.build(),
        );

        ctx.send_hci_event(
            hci::LinkKeyNotificationBuilder {
                bd_addr: ctx.peer_address(),
                key_type: hci::KeyType::AuthenticatedP192,
                link_key: [0; 16],
            }
            .build(),
        );
    }

    ctx.send_hci_event(
        hci::AuthenticationCompleteBuilder {
            status: hci::ErrorCode::Success,
            connection_handle: ctx.peer_handle(),
        }
        .build(),
    );
}

pub async fn respond(ctx: &impl Context) {
    let pairing = match ctx.receive_lmp_packet::<Either<
        lmp::AuRandPacket,
        Either<lmp::IoCapabilityReqPacket, lmp::InRandPacket>
    >>()
    .await
    {
        Either::Left(_au_rand) => false,
        Either::Right(pairing) => {
            let success = match pairing {
                Either::Left(io_capability_request) =>
                    secure_simple_pairing::respond(ctx, io_capability_request).await,
                Either::Right(in_rand) =>
                    legacy_pairing::respond(ctx, in_rand).await,
            };

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
                bd_addr: ctx.peer_address(),
                key_type: hci::KeyType::AuthenticatedP192,
                link_key: [0; 16],
            }
            .build(),
        );
    }
}
