// Bluetooth Core, Vol 2, Part C, 4.2.1

use num_traits::ToPrimitive;
use openssl::symm::{encrypt, Cipher};
use std::convert::TryInto;

use crate::either::Either;
use crate::num_hci_command_packets;
use crate::packets::{hci, lmp};
use crate::procedure::features;
use crate::procedure::legacy_pairing;
use crate::procedure::secure_simple_pairing;
use crate::procedure::Context;

pub fn generate_sres(random_number: [u8; 16], key: [u8; 16]) -> [u8; 4] {
    let cipher = Cipher::aes_128_ecb();
    encrypt(cipher, &key, None, &random_number).unwrap()[0..4].try_into().unwrap()
}

async fn secure_simple_pairing_supported(ctx: &impl Context) -> bool {
    let ssp_bit = hci::LMPFeaturesPage1Bits::SecureSimplePairingHostSupport.to_u64().unwrap();
    let local_supported = ctx.extended_features(1) & ssp_bit != 0;
    // Lazy peer features
    let peer_supported = async move {
        let page = if let Some(page) = ctx.peer_extended_features(1) {
            page
        } else {
            features::initiate(ctx, 1).await
        };
        page & ssp_bit != 0
    };
    local_supported && peer_supported.await
}

pub async fn send_challenge(
    ctx: &impl Context,
    transaction_id: u8,
    link_key: [u8; 16],
) -> Result<(), ()> {
    let random_number = [0; 16];
    ctx.send_lmp_packet(lmp::AuRandBuilder { transaction_id, random_number }.build());

    match ctx.receive_lmp_packet::<Either<lmp::SresPacket, lmp::NotAcceptedPacket>>().await {
        Either::Left(response) => {
            if *response.get_authentication_rsp() == generate_sres(random_number, link_key) {
                Ok(())
            } else {
                Err(())
            }
        }
        Either::Right(_) => Err(()),
    }
}

pub async fn receive_challenge(ctx: &impl Context, link_key: [u8; 16]) {
    let random_number = *ctx.receive_lmp_packet::<lmp::AuRandPacket>().await.get_random_number();
    ctx.send_lmp_packet(
        lmp::SresBuilder {
            transaction_id: 0,
            authentication_rsp: generate_sres(random_number, link_key),
        }
        .build(),
    );
}

pub async fn request_link_key_from_host(ctx: &impl Context) -> Option<[u8; 16]> {
    ctx.send_hci_event(hci::LinkKeyRequestBuilder { bd_addr: ctx.peer_address() }.build());
    match ctx.receive_hci_command::<Either<
        hci::LinkKeyRequestReplyPacket,
        hci::LinkKeyRequestNegativeReplyPacket,
    >>().await {
        Either::Left(reply) => {
            ctx.send_hci_event(
                hci::LinkKeyRequestReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            Some(*reply.get_link_key())
        },
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::LinkKeyRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            None
        }
    }
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

    let result = match request_link_key_from_host(ctx).await {
        Some(link_key) => send_challenge(ctx, 0, link_key).await,
        None => {
            // No link key, start pairing
            if secure_simple_pairing_supported(ctx).await {
                secure_simple_pairing::initiate(ctx).await
            } else {
                legacy_pairing::initiate(ctx).await
            }
        }
    };

    let status = match result {
        Ok(_) => hci::ErrorCode::Success,
        Err(_) => hci::ErrorCode::AuthenticationFailure,
    };

    ctx.send_hci_event(
        hci::AuthenticationCompleteBuilder { status, connection_handle: ctx.peer_handle() }.build(),
    );
}

pub async fn respond(ctx: &impl Context) {
    match ctx.receive_lmp_packet::<Either<
        lmp::AuRandPacket,
        Either<lmp::IoCapabilityReqPacket, lmp::InRandPacket>
    >>()
    .await
    {
        Either::Left(au_rand) => {
            match request_link_key_from_host(ctx).await {
                Some(link_key) => {
                    let remote_random_number = *au_rand.get_random_number();
                    ctx.send_lmp_packet(lmp::SresBuilder { transaction_id: 0, authentication_rsp: generate_sres(remote_random_number, link_key) }.build());
                },
                None => {
                    ctx.send_lmp_packet(lmp::NotAcceptedBuilder { transaction_id: 0, not_accepted_opcode: lmp::Opcode::AuRand, error_code: hci::ErrorCode::PinOrKeyMissing.to_u8().unwrap() }.build());
                }
            }
        },
        Either::Right(pairing) => {
            let result = match pairing {
                Either::Left(io_capability_request) =>
                    secure_simple_pairing::respond(ctx, io_capability_request).await,
                Either::Right(in_rand) =>
                    legacy_pairing::respond(ctx, in_rand).await,
            };

            // Result must be used
            if result.is_err() {
                println!("Authentication failure!");
            }

            // TODO: Handle error
        }
    }
}
