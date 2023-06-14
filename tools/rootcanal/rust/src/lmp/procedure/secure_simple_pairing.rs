// Bluetooth Core, Vol 2, Part C, 4.2.7

use std::convert::{TryFrom, TryInto};

use crate::either::Either;
use crate::lmp::ec::{DhKey, PrivateKey, PublicKey};
use crate::lmp::procedure::{authentication, features, Context};
use crate::packets::{hci, lmp};

use crate::num_hci_command_packets;

fn has_mitm(requirements: hci::AuthenticationRequirements) -> bool {
    use hci::AuthenticationRequirements::*;

    match requirements {
        NoBonding | DedicatedBonding | GeneralBonding => false,
        NoBondingMitmProtection | DedicatedBondingMitmProtection | GeneralBondingMitmProtection => {
            true
        }
    }
}

enum AuthenticationMethod {
    OutOfBand,
    NumericComparisonJustWork,
    NumericComparisonUserConfirm,
    PasskeyEntry,
}

#[derive(Clone, Copy)]
struct AuthenticationParams {
    io_capability: hci::IoCapability,
    oob_data_present: hci::OobDataPresent,
    authentication_requirements: hci::AuthenticationRequirements,
}

// Bluetooth Core, Vol 2, Part C, 4.2.7.3
fn authentication_method(
    initiator: AuthenticationParams,
    responder: AuthenticationParams,
) -> AuthenticationMethod {
    use hci::IoCapability::*;
    use hci::OobDataPresent::*;

    if initiator.oob_data_present != NotPresent || responder.oob_data_present != NotPresent {
        AuthenticationMethod::OutOfBand
    } else if !has_mitm(initiator.authentication_requirements)
        && !has_mitm(responder.authentication_requirements)
    {
        AuthenticationMethod::NumericComparisonJustWork
    } else if (initiator.io_capability == KeyboardOnly
        && responder.io_capability != NoInputNoOutput)
        || (responder.io_capability == KeyboardOnly && initiator.io_capability != NoInputNoOutput)
    {
        AuthenticationMethod::PasskeyEntry
    } else if initiator.io_capability == DisplayYesNo && responder.io_capability == DisplayYesNo {
        AuthenticationMethod::NumericComparisonUserConfirm
    } else {
        AuthenticationMethod::NumericComparisonJustWork
    }
}

// Bluetooth Core, Vol 3, Part C, 5.2.2.6
fn link_key_type(auth_method: AuthenticationMethod, dh_key: DhKey) -> hci::KeyType {
    use hci::KeyType::*;
    use AuthenticationMethod::*;

    match (dh_key, auth_method) {
        (DhKey::P256(_), OutOfBand | PasskeyEntry | NumericComparisonUserConfirm) => {
            AuthenticatedP256
        }
        (DhKey::P192(_), OutOfBand | PasskeyEntry | NumericComparisonUserConfirm) => {
            AuthenticatedP192
        }
        (DhKey::P256(_), NumericComparisonJustWork) => UnauthenticatedP256,
        (DhKey::P192(_), NumericComparisonJustWork) => UnauthenticatedP192,
    }
}

async fn send_public_key(ctx: &impl Context, transaction_id: u8, public_key: PublicKey) {
    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::EncapsulatedHeaderBuilder {
                transaction_id,
                major_type: 1,
                minor_type: 1,
                payload_length: public_key.size() as u8,
            }
            .build(),
        )
        .await;

    for chunk in public_key.as_slice().chunks(16) {
        // TODO: handle error
        let _ = ctx
            .send_accepted_lmp_packet(
                lmp::EncapsulatedPayloadBuilder { transaction_id, data: chunk.try_into().unwrap() }
                    .build(),
            )
            .await;
    }
}

async fn receive_public_key(ctx: &impl Context, transaction_id: u8) -> PublicKey {
    let key_size: usize =
        ctx.receive_lmp_packet::<lmp::EncapsulatedHeader>().await.get_payload_length().into();
    let mut key = PublicKey::new(key_size).unwrap();

    ctx.send_lmp_packet(
        lmp::AcceptedBuilder { transaction_id, accepted_opcode: lmp::Opcode::EncapsulatedHeader }
            .build(),
    );
    for chunk in key.as_mut_slice().chunks_mut(16) {
        let payload = ctx.receive_lmp_packet::<lmp::EncapsulatedPayload>().await;
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

const COMMITMENT_VALUE_SIZE: usize = 16;
const NONCE_SIZE: usize = 16;

async fn receive_commitment(ctx: &impl Context, skip_first: bool) {
    let commitment_value = [0; COMMITMENT_VALUE_SIZE];

    if !skip_first {
        let confirm = ctx.receive_lmp_packet::<lmp::SimplePairingConfirm>().await;
        if confirm.get_commitment_value() != &commitment_value {
            todo!();
        }
    }

    ctx.send_lmp_packet(
        lmp::SimplePairingConfirmBuilder { transaction_id: 0, commitment_value }.build(),
    );

    let _pairing_number = ctx.receive_lmp_packet::<lmp::SimplePairingNumber>().await;
    // TODO: check pairing number
    ctx.send_lmp_packet(
        lmp::AcceptedBuilder {
            transaction_id: 0,
            accepted_opcode: lmp::Opcode::SimplePairingNumber,
        }
        .build(),
    );

    let nonce = [0; NONCE_SIZE];

    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::SimplePairingNumberBuilder { transaction_id: 0, nonce }.build(),
        )
        .await;
}

async fn send_commitment(ctx: &impl Context, skip_first: bool) {
    let commitment_value = [0; COMMITMENT_VALUE_SIZE];

    if !skip_first {
        ctx.send_lmp_packet(
            lmp::SimplePairingConfirmBuilder { transaction_id: 0, commitment_value }.build(),
        );
    }

    let confirm = ctx.receive_lmp_packet::<lmp::SimplePairingConfirm>().await;

    if confirm.get_commitment_value() != &commitment_value {
        todo!();
    }
    let nonce = [0; NONCE_SIZE];

    // TODO: handle error
    let _ = ctx
        .send_accepted_lmp_packet(
            lmp::SimplePairingNumberBuilder { transaction_id: 0, nonce }.build(),
        )
        .await;

    let _pairing_number = ctx.receive_lmp_packet::<lmp::SimplePairingNumber>().await;
    // TODO: check pairing number
    ctx.send_lmp_packet(
        lmp::AcceptedBuilder {
            transaction_id: 0,
            accepted_opcode: lmp::Opcode::SimplePairingNumber,
        }
        .build(),
    );
}

async fn user_confirmation_request(ctx: &impl Context) -> Result<(), ()> {
    ctx.send_hci_event(
        hci::UserConfirmationRequestBuilder { bd_addr: ctx.peer_address(), numeric_value: 0 }
            .build(),
    );

    match ctx
        .receive_hci_command::<Either<
            hci::UserConfirmationRequestReply,
            hci::UserConfirmationRequestNegativeReply,
        >>()
        .await
    {
        Either::Left(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            Ok(())
        }
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::UserConfirmationRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            Err(())
        }
    }
}

async fn user_passkey_request(ctx: &impl Context) -> Result<(), ()> {
    ctx.send_hci_event(hci::UserPasskeyRequestBuilder { bd_addr: ctx.peer_address() }.build());

    loop {
        match ctx
            .receive_hci_command::<Either<
                Either<hci::UserPasskeyRequestReply, hci::UserPasskeyRequestNegativeReply>,
                hci::SendKeypressNotification,
            >>()
            .await
        {
            Either::Left(Either::Left(_)) => {
                ctx.send_hci_event(
                    hci::UserPasskeyRequestReplyCompleteBuilder {
                        num_hci_command_packets,
                        status: hci::ErrorCode::Success,
                        bd_addr: ctx.peer_address(),
                    }
                    .build(),
                );
                return Ok(());
            }
            Either::Left(Either::Right(_)) => {
                ctx.send_hci_event(
                    hci::UserPasskeyRequestNegativeReplyCompleteBuilder {
                        num_hci_command_packets,
                        status: hci::ErrorCode::Success,
                        bd_addr: ctx.peer_address(),
                    }
                    .build(),
                );
                return Err(());
            }
            Either::Right(_) => {
                ctx.send_hci_event(
                    hci::SendKeypressNotificationCompleteBuilder {
                        num_hci_command_packets,
                        status: hci::ErrorCode::Success,
                        bd_addr: ctx.peer_address(),
                    }
                    .build(),
                );
                // TODO: send LmpKeypressNotification
            }
        }
    }
}

async fn remote_oob_data_request(ctx: &impl Context) -> Result<(), ()> {
    ctx.send_hci_event(hci::RemoteOobDataRequestBuilder { bd_addr: ctx.peer_address() }.build());

    match ctx
        .receive_hci_command::<Either<
            hci::RemoteOobDataRequestReply,
            hci::RemoteOobDataRequestNegativeReply,
        >>()
        .await
    {
        Either::Left(_) => {
            ctx.send_hci_event(
                hci::RemoteOobDataRequestReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            Ok(())
        }
        Either::Right(_) => {
            ctx.send_hci_event(
                hci::RemoteOobDataRequestNegativeReplyCompleteBuilder {
                    num_hci_command_packets,
                    status: hci::ErrorCode::Success,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            Err(())
        }
    }
}

const CONFIRMATION_VALUE_SIZE: usize = 16;
const PASSKEY_ENTRY_REPEAT_NUMBER: usize = 20;

pub async fn initiate(ctx: &impl Context) -> Result<(), hci::ErrorCode> {
    if ctx.pairing() {
        return Err(hci::ErrorCode::LinkLayerCollision);
    }

    ctx.set_pairing(true);

    let initiator = {
        ctx.send_hci_event(hci::IoCapabilityRequestBuilder { bd_addr: ctx.peer_address() }.build());
        match ctx
                .receive_hci_command::<Either<
                    hci::IoCapabilityRequestReply,
                    hci::IoCapabilityRequestNegativeReply,
                >>()
                .await
            {
                Either::Left(reply) => {
                    ctx.send_hci_event(
                        hci::IoCapabilityRequestReplyCompleteBuilder {
                            num_hci_command_packets,
                            status : hci::ErrorCode::Success,
                            bd_addr: ctx.peer_address(),
                        }
                        .build(),
                    );
                    ctx.send_lmp_packet(
                        lmp::IoCapabilityReqBuilder {
                            transaction_id: 0,
                            io_capabilities: reply.get_io_capability().into(),
                            oob_authentication_data: reply.get_oob_present().into(),
                            authentication_requirement: reply
                                .get_authentication_requirements()
                                .into(),
                        }
                        .build(),
                    );
                    AuthenticationParams {
                        io_capability: reply.get_io_capability(),
                        oob_data_present: reply.get_oob_present(),
                        authentication_requirements: reply.get_authentication_requirements(),
                    }
                }
                Either::Right(_) => {
                    ctx.send_hci_event(
                        hci::IoCapabilityRequestNegativeReplyCompleteBuilder {
                            num_hci_command_packets,
                            status : hci::ErrorCode::Success,
                            bd_addr: ctx.peer_address(),
                        }
                        .build(),
                    );
                    ctx.send_hci_event(
                        hci::SimplePairingCompleteBuilder {
                            status : hci::ErrorCode::AuthenticationFailure,
                            bd_addr: ctx.peer_address(),
                        }
                        .build(),
                    );
                    return Err(hci::ErrorCode::AuthenticationFailure);
                }
            }
    };
    let responder = {
        match ctx.receive_lmp_packet::<Either<lmp::IoCapabilityRes, lmp::NotAcceptedExt>>().await {
            Either::Left(response) => {
                let io_capability =
                    hci::IoCapability::try_from(response.get_io_capabilities()).unwrap();
                let oob_data_present =
                    hci::OobDataPresent::try_from(response.get_oob_authentication_data()).unwrap();
                let authentication_requirements = hci::AuthenticationRequirements::try_from(
                    response.get_authentication_requirement(),
                )
                .unwrap();

                ctx.send_hci_event(
                    hci::IoCapabilityResponseBuilder {
                        bd_addr: ctx.peer_address(),
                        io_capability,
                        oob_data_present,
                        authentication_requirements,
                    }
                    .build(),
                );

                AuthenticationParams {
                    io_capability,
                    oob_data_present,
                    authentication_requirements,
                }
            }
            Either::Right(_) => return Err(hci::ErrorCode::LinkLayerCollision),
        }
    };

    // Public Key Exchange
    let dh_key = {
        use hci::LMPFeaturesPage1Bits::SecureConnectionsHostSupport;

        let private_key =
            if features::supported_on_both_page1(ctx, SecureConnectionsHostSupport).await {
                PrivateKey::generate_p256()
            } else {
                PrivateKey::generate_p192()
            };
        ctx.set_private_key(&private_key);
        let local_public_key = private_key.derive();
        send_public_key(ctx, 0, local_public_key).await;
        let peer_public_key = receive_public_key(ctx, 0).await;
        private_key.shared_secret(peer_public_key)
    };

    // Authentication Stage 1
    let auth_method = authentication_method(initiator, responder);
    let result: Result<(), ()> = async {
        match auth_method {
            AuthenticationMethod::NumericComparisonJustWork
            | AuthenticationMethod::NumericComparisonUserConfirm => {
                send_commitment(ctx, true).await;

                user_confirmation_request(ctx).await?;
                Ok(())
            }
            AuthenticationMethod::PasskeyEntry => {
                if initiator.io_capability == hci::IoCapability::KeyboardOnly {
                    user_passkey_request(ctx).await?;
                } else {
                    ctx.send_hci_event(
                        hci::UserPasskeyNotificationBuilder {
                            bd_addr: ctx.peer_address(),
                            passkey: 0,
                        }
                        .build(),
                    );
                }
                for _ in 0..PASSKEY_ENTRY_REPEAT_NUMBER {
                    send_commitment(ctx, false).await;
                }
                Ok(())
            }
            AuthenticationMethod::OutOfBand => {
                if initiator.oob_data_present != hci::OobDataPresent::NotPresent {
                    remote_oob_data_request(ctx).await?;
                }

                send_commitment(ctx, false).await;
                Ok(())
            }
        }
    }
    .await;

    if result.is_err() {
        ctx.send_lmp_packet(lmp::NumericComparisonFailedBuilder { transaction_id: 0 }.build());
        ctx.send_hci_event(
            hci::SimplePairingCompleteBuilder {
                status: hci::ErrorCode::AuthenticationFailure,
                bd_addr: ctx.peer_address(),
            }
            .build(),
        );
        return Err(hci::ErrorCode::AuthenticationFailure);
    }

    // Authentication Stage 2
    {
        let confirmation_value = [0; CONFIRMATION_VALUE_SIZE];

        let result = ctx
            .send_accepted_lmp_packet(
                lmp::DhkeyCheckBuilder { transaction_id: 0, confirmation_value }.build(),
            )
            .await;

        if result.is_err() {
            ctx.send_hci_event(
                hci::SimplePairingCompleteBuilder {
                    status: hci::ErrorCode::AuthenticationFailure,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            return Err(hci::ErrorCode::AuthenticationFailure);
        }
    }

    {
        // TODO: check dhkey
        let _dhkey = ctx.receive_lmp_packet::<lmp::DhkeyCheck>().await;
        ctx.send_lmp_packet(
            lmp::AcceptedBuilder { transaction_id: 0, accepted_opcode: lmp::Opcode::DhkeyCheck }
                .build(),
        );
    }

    ctx.send_hci_event(
        hci::SimplePairingCompleteBuilder {
            status: hci::ErrorCode::Success,
            bd_addr: ctx.peer_address(),
        }
        .build(),
    );

    // Link Key Calculation
    let link_key = [0; 16];
    let auth_result = authentication::send_challenge(ctx, 0, link_key).await;
    authentication::receive_challenge(ctx, link_key).await;

    if auth_result.is_err() {
        return Err(hci::ErrorCode::AuthenticationFailure);
    }

    ctx.send_hci_event(
        hci::LinkKeyNotificationBuilder {
            bd_addr: ctx.peer_address(),
            key_type: link_key_type(auth_method, dh_key),
            link_key,
        }
        .build(),
    );

    Ok(())
}

pub async fn respond(ctx: &impl Context, request: lmp::IoCapabilityReq) -> Result<(), ()> {
    // Vol 2, Part C § 2.5.1 Transaction collision resolution.
    // In case of procedure collision, the Central rejects the Peripheral
    // initiated procedure. The Central-initiated procedure shall then be
    // completed.
    if ctx.pairing() && ctx.role() == hci::Role::Central {
        ctx.send_lmp_packet(
            lmp::NotAcceptedExtBuilder {
                transaction_id: 0,
                error_code: hci::ErrorCode::LinkLayerCollision.into(),
                not_accepted_opcode: lmp::ExtendedOpcode::IoCapabilityReq,
            }
            .build(),
        );
        return Err(());
    }

    let initiator = {
        let io_capability = hci::IoCapability::try_from(request.get_io_capabilities()).unwrap();
        let oob_data_present =
            hci::OobDataPresent::try_from(request.get_oob_authentication_data()).unwrap();
        let authentication_requirements =
            hci::AuthenticationRequirements::try_from(request.get_authentication_requirement())
                .unwrap();

        ctx.send_hci_event(
            hci::IoCapabilityResponseBuilder {
                bd_addr: ctx.peer_address(),
                io_capability,
                oob_data_present,
                authentication_requirements,
            }
            .build(),
        );

        AuthenticationParams { io_capability, oob_data_present, authentication_requirements }
    };

    let responder = {
        ctx.send_hci_event(hci::IoCapabilityRequestBuilder { bd_addr: ctx.peer_address() }.build());
        match ctx
                .receive_hci_command::<Either<
                    hci::IoCapabilityRequestReply,
                    hci::IoCapabilityRequestNegativeReply,
                >>()
                .await
            {
                Either::Left(reply) => {
                    ctx.send_hci_event(
                        hci::IoCapabilityRequestReplyCompleteBuilder {
                            num_hci_command_packets,
                            status: hci::ErrorCode::Success,
                            bd_addr: ctx.peer_address(),
                        }
                        .build(),
                    );
                    ctx.send_lmp_packet(
                        lmp::IoCapabilityResBuilder {
                            transaction_id: 0,
                            io_capabilities: reply.get_io_capability().into(),
                            oob_authentication_data: reply.get_oob_present().into(),
                            authentication_requirement: reply
                                .get_authentication_requirements()
                                .into(),
                        }
                        .build(),
                    );
                    AuthenticationParams {
                        io_capability: reply.get_io_capability(),
                        oob_data_present: reply.get_oob_present(),
                        authentication_requirements: reply.get_authentication_requirements(),
                    }
                }
                Either::Right(reply) => {
                    ctx.send_hci_event(
                        hci::IoCapabilityRequestNegativeReplyCompleteBuilder {
                            num_hci_command_packets,
                            status: hci::ErrorCode::Success,
                            bd_addr: ctx.peer_address(),
                        }
                        .build(),
                    );
                    ctx.send_lmp_packet(
                        lmp::NotAcceptedExtBuilder {
                            transaction_id: 0,
                            error_code: reply.get_reason().into(),
                            not_accepted_opcode: lmp::ExtendedOpcode::IoCapabilityReq,
                        }
                        .build(),
                    );
                    ctx.send_hci_event(
                        hci::SimplePairingCompleteBuilder {
                            status: hci::ErrorCode::AuthenticationFailure,
                            bd_addr: reply.get_bd_addr(),
                        }
                        .build(),
                    );
                    return Err(());
                }
            }
    };

    // Public Key Exchange
    let dh_key = {
        let peer_public_key = receive_public_key(ctx, 0).await;
        let private_key = match peer_public_key {
            PublicKey::P192(_) => PrivateKey::generate_p192(),
            PublicKey::P256(_) => PrivateKey::generate_p256(),
        };
        ctx.set_private_key(&private_key);
        let local_public_key = private_key.derive();
        send_public_key(ctx, 0, local_public_key).await;
        private_key.shared_secret(peer_public_key)
    };

    // Authentication Stage 1
    let auth_method = authentication_method(initiator, responder);
    let negative_user_confirmation = match auth_method {
        AuthenticationMethod::NumericComparisonJustWork
        | AuthenticationMethod::NumericComparisonUserConfirm => {
            receive_commitment(ctx, true).await;

            let user_confirmation = user_confirmation_request(ctx).await;
            user_confirmation.is_err()
        }
        AuthenticationMethod::PasskeyEntry => {
            if responder.io_capability == hci::IoCapability::KeyboardOnly {
                // TODO: handle error
                let _user_passkey = user_passkey_request(ctx).await;
            } else {
                ctx.send_hci_event(
                    hci::UserPasskeyNotificationBuilder { bd_addr: ctx.peer_address(), passkey: 0 }
                        .build(),
                );
            }
            for _ in 0..PASSKEY_ENTRY_REPEAT_NUMBER {
                receive_commitment(ctx, false).await;
            }
            false
        }
        AuthenticationMethod::OutOfBand => {
            if responder.oob_data_present != hci::OobDataPresent::NotPresent {
                // TODO: handle error
                let _remote_oob_data = remote_oob_data_request(ctx).await;
            }

            receive_commitment(ctx, false).await;
            false
        }
    };

    let _dhkey = match ctx
        .receive_lmp_packet::<Either<lmp::NumericComparisonFailed, lmp::DhkeyCheck>>()
        .await
    {
        Either::Left(_) => {
            // Numeric comparison failed
            ctx.send_hci_event(
                hci::SimplePairingCompleteBuilder {
                    status: hci::ErrorCode::AuthenticationFailure,
                    bd_addr: ctx.peer_address(),
                }
                .build(),
            );
            return Err(());
        }
        Either::Right(dhkey) => dhkey,
    };

    if negative_user_confirmation {
        ctx.send_lmp_packet(
            lmp::NotAcceptedBuilder {
                transaction_id: 0,
                not_accepted_opcode: lmp::Opcode::DhkeyCheck,
                error_code: hci::ErrorCode::AuthenticationFailure.into(),
            }
            .build(),
        );
        ctx.send_hci_event(
            hci::SimplePairingCompleteBuilder {
                status: hci::ErrorCode::AuthenticationFailure,
                bd_addr: ctx.peer_address(),
            }
            .build(),
        );
        return Err(());
    }
    // Authentication Stage 2

    let confirmation_value = [0; CONFIRMATION_VALUE_SIZE];

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
        hci::SimplePairingCompleteBuilder {
            status: hci::ErrorCode::Success,
            bd_addr: ctx.peer_address(),
        }
        .build(),
    );

    // Link Key Calculation
    let link_key = [0; 16];
    authentication::receive_challenge(ctx, link_key).await;
    let auth_result = authentication::send_challenge(ctx, 0, link_key).await;

    if auth_result.is_err() {
        return Err(());
    }

    ctx.send_hci_event(
        hci::LinkKeyNotificationBuilder {
            bd_addr: ctx.peer_address(),
            key_type: link_key_type(auth_method, dh_key),
            link_key,
        }
        .build(),
    );

    Ok(())
}

#[cfg(test)]
mod tests {
    use crate::lmp::ec::PrivateKey;
    use crate::lmp::procedure::Context;
    use crate::lmp::test::{sequence, TestContext};
    use crate::packets::hci;
    // simple pairing is part of authentication procedure
    use super::super::authentication::initiate;
    use super::super::authentication::respond;
    use futures::join;

    fn local_p192_public_key(context: &crate::lmp::test::TestContext) -> [[u8; 16]; 3] {
        let mut buf = [[0; 16], [0; 16], [0; 16]];
        if let Some(key) = context.get_private_key() {
            for (dst, src) in buf.iter_mut().zip(key.derive().as_slice().chunks(16)) {
                dst.copy_from_slice(src);
            }
        }
        buf
    }

    fn peer_p192_public_key() -> [[u8; 16]; 3] {
        let mut buf = [[0; 16], [0; 16], [0; 16]];
        let key = PrivateKey::generate_p192().derive();
        for (dst, src) in buf.iter_mut().zip(key.as_slice().chunks(16)) {
            dst.copy_from_slice(src);
        }
        buf
    }

    #[test]
    fn initiate_size() {
        let context = crate::lmp::test::TestContext::new();
        let procedure = super::initiate(&context);

        fn assert_max_size<T>(_value: T, limit: usize) {
            let type_name = std::any::type_name::<T>();
            let size = std::mem::size_of::<T>();
            println!("Size of {}: {}", type_name, size);
            assert!(size < limit)
        }

        assert_max_size(procedure, 512);
    }

    #[test]
    fn numeric_comparison_initiator_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-06-C.in");
    }

    #[test]
    fn numeric_comparison_responder_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-07-C.in");
    }

    #[test]
    fn numeric_comparison_initiator_failure_on_initiating_side() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-08-C.in");
    }

    #[test]
    fn numeric_comparison_responder_failure_on_initiating_side() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-09-C.in");
    }

    #[test]
    fn numeric_comparison_initiator_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-10-C.in");
    }

    #[test]
    fn numeric_comparison_responder_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-11-C.in");
    }

    #[test]
    fn passkey_entry_initiator_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-12-C.in");
    }

    #[test]
    fn passkey_entry_responder_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-13-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_initiator_failure_on_initiating_side() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-14-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_responder_failure_on_initiating_side() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-15-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_initiator_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-16-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_responder_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-17-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_initiator_iut_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-18-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_responder_iut_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-19-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_initiator_lower_tester_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-20-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_responder_lower_tester_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-21-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_initiator_iut_and_lower_tester_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-22-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_responder_iut_and_lower_tester_with_oob_auth_data_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-23-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_initiator_iut_with_oob_auth_data_failure() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-24-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_responder_iut_with_oob_auth_data_failure() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-25-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_initiator_lower_tester_with_oob_auth_data_failure() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-26-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn oob_protocol_responder_lower_tester_with_oob_auth_data_failure() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-27-C.in");
    }

    #[test]
    fn secure_simple_pairing_failed_responder() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-30-C.in");
    }

    #[test]
    fn host_rejects_secure_simple_pairing_initiator() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-31-C.in");
    }

    #[test]
    fn host_rejects_secure_simple_pairing_responder() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-32-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_with_keypress_notification_initiator_success() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-33-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_with_keypress_notification_responder_success() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-34-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_with_keypress_notification_initiator_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = initiate;

        include!("../../../test/SP/BV-35-C.in");
    }

    #[test]
    #[should_panic] // TODO: make the test pass
    fn passkey_entry_with_keypress_notificiation_responder_failure_on_responding_side() {
        let context = TestContext::new();
        let procedure = respond;

        include!("../../../test/SP/BV-36-C.in");
    }

    #[test]
    fn numeric_comparison_collision_central_initiated() {
        // Verify that the Central handles a transaction collision for the
        // LMP request IO_CAPABILITY_REQ by rejecting the Peripheral
        // request and resuming the Central request.

        let context = TestContext::new().with_role(hci::Role::Central);
        let procedure = |context| async move {
            join!(initiate(context), respond(context));
        };

        sequence! { procedure, context,
            // ACL Connection Established
            Upper Tester -> IUT: AuthenticationRequested {
                connection_handle: context.peer_handle()
            }
            IUT -> Upper Tester: AuthenticationRequestedStatus {
               num_hci_command_packets: 1,
               status: ErrorCode::Success,
            }
            IUT -> Upper Tester: LinkKeyRequest {
                bd_addr: context.peer_address(),
            }
            Upper Tester -> IUT: LinkKeyRequestNegativeReply {
                bd_addr: context.peer_address(),
            }
            IUT -> Upper Tester: LinkKeyRequestNegativeReplyComplete {
               num_hci_command_packets: 1,
               status: ErrorCode::Success,
               bd_addr: context.peer_address(),
            }
            IUT -> Upper Tester: IoCapabilityRequest {
                bd_addr: context.peer_address(),
            }
            Upper Tester -> IUT: IoCapabilityRequestReply {
                bd_addr: context.peer_address(),
                io_capability: IoCapability::DisplayYesNo,
                oob_present: OobDataPresent::NotPresent,
                authentication_requirements: AuthenticationRequirements::NoBondingMitmProtection,
            }
            IUT -> Upper Tester: IoCapabilityRequestReplyComplete {
                num_hci_command_packets: 1,
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            IUT -> Lower Tester: IoCapabilityReq {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            // Procedure collision.
            // The Central must reject the Peripheral-initiated procedure.
            Lower Tester -> IUT: IoCapabilityReq {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            IUT -> Lower Tester: NotAcceptedExt {
                not_accepted_opcode: ExtendedOpcode::IoCapabilityReq,
                error_code: hci::ErrorCode::LinkLayerCollision.into(),
            }
            Lower Tester -> IUT: IoCapabilityRes {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            IUT -> Upper Tester: IoCapabilityResponse {
                bd_addr: context.peer_address(),
                io_capability: IoCapability::DisplayYesNo,
                oob_data_present: OobDataPresent::NotPresent,
                authentication_requirements: AuthenticationRequirements::NoBondingMitmProtection,
            }
            // Public Key Exchange
            IUT -> Lower Tester: EncapsulatedHeader {
                transaction_id: 0,
                major_type: 1,
                minor_type: 1,
                payload_length: 48,
            }
            Lower Tester -> IUT: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::EncapsulatedHeader,
            }
            repeat 3 times with (part in local_p192_public_key(&context)) {
                IUT -> Lower Tester: EncapsulatedPayload {
                    transaction_id: 0,
                    data: part,
                }
                Lower Tester -> IUT: Accepted {
                    transaction_id: 0,
                    accepted_opcode: Opcode::EncapsulatedPayload,
                }
            }
            Lower Tester -> IUT: EncapsulatedHeader {
                transaction_id: 0,
                major_type: 1,
                minor_type: 1,
                payload_length: 48,
            }
            IUT -> Lower Tester: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::EncapsulatedHeader,
            }
            repeat 3 times with (part in peer_p192_public_key()) {
                Lower Tester -> IUT: EncapsulatedPayload {
                    transaction_id: 0,
                    data: part,
                }
                IUT -> Lower Tester: Accepted {
                    transaction_id: 0,
                    accepted_opcode: Opcode::EncapsulatedPayload,
                }
            }
            // Authentication Stage 1: Numeric Comparison Protocol
            Lower Tester -> IUT: SimplePairingConfirm {
                transaction_id: 0,
                commitment_value: [0; 16],
            }
            IUT -> Lower Tester: SimplePairingNumber {
                transaction_id: 0,
                nonce: [0; 16],
            }
            Lower Tester -> IUT: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::SimplePairingNumber,
            }
            Lower Tester -> IUT: SimplePairingNumber {
                transaction_id: 0,
                nonce: [0; 16],
            }
            IUT -> Upper Tester: UserConfirmationRequest { bd_addr: context.peer_address(), numeric_value: 0 }
            IUT -> Lower Tester: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::SimplePairingNumber,
            }
            Upper Tester -> IUT: UserConfirmationRequestReply { bd_addr: context.peer_address() }
            IUT -> Upper Tester: UserConfirmationRequestReplyComplete {
                num_hci_command_packets: 1,
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            // Authentication Stage 2
            IUT -> Lower Tester: DhkeyCheck {
                transaction_id: 0,
                confirmation_value: [0; 16],
            }
            Lower Tester -> IUT: Accepted { transaction_id: 0, accepted_opcode: Opcode::DhkeyCheck }
            Lower Tester -> IUT: DhkeyCheck {
                transaction_id: 0,
                confirmation_value: [0; 16],
            }
            IUT -> Lower Tester: Accepted { transaction_id: 0, accepted_opcode: Opcode::DhkeyCheck }
            IUT -> Upper Tester: SimplePairingComplete {
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            // Link Key Calculation
            IUT -> Lower Tester: AuRand {
                transaction_id: 0,
                random_number: [0; 16],
            }
            Lower Tester -> IUT: Sres {
                transaction_id: 0,
                authentication_rsp: [0; 4],
            }
            Lower Tester -> IUT: AuRand {
                transaction_id: 0,
                random_number: [0; 16],
            }
            IUT -> Lower Tester: Sres {
                transaction_id: 0,
                authentication_rsp: [0; 4],
            }
            IUT -> Upper Tester: LinkKeyNotification {
                bd_addr: context.peer_address(),
                key_type: KeyType::AuthenticatedP192,
                link_key: [0; 16],
            }
            IUT -> Upper Tester: AuthenticationComplete {
                status: ErrorCode::Success,
                connection_handle: context.peer_handle(),
            }
        }
    }

    #[test]
    fn numeric_comparison_collision_peripheral_initiated() {
        // Verify that the Peripheral handles a transaction collision for the
        // LMP request IO_CAPABILITY_REQ by abandoning the Peripheral
        // request and resuming the Central request.

        let context = TestContext::new().with_role(hci::Role::Peripheral);
        let procedure = |context| async move {
            join!(initiate(context), respond(context));
        };

        sequence! { procedure, context,
            // ACL Connection Established
            Upper Tester -> IUT: AuthenticationRequested {
                connection_handle: context.peer_handle()
            }
            IUT -> Upper Tester: AuthenticationRequestedStatus {
               num_hci_command_packets: 1,
               status: ErrorCode::Success,
            }
            IUT -> Upper Tester: LinkKeyRequest {
                bd_addr: context.peer_address(),
            }
            Upper Tester -> IUT: LinkKeyRequestNegativeReply {
                bd_addr: context.peer_address(),
            }
            IUT -> Upper Tester: LinkKeyRequestNegativeReplyComplete {
               num_hci_command_packets: 1,
               status: ErrorCode::Success,
               bd_addr: context.peer_address(),
            }
            IUT -> Upper Tester: IoCapabilityRequest {
                bd_addr: context.peer_address(),
            }
            Upper Tester -> IUT: IoCapabilityRequestReply {
                bd_addr: context.peer_address(),
                io_capability: IoCapability::DisplayYesNo,
                oob_present: OobDataPresent::NotPresent,
                authentication_requirements: AuthenticationRequirements::NoBondingMitmProtection,
            }
            IUT -> Upper Tester: IoCapabilityRequestReplyComplete {
                num_hci_command_packets: 1,
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            IUT -> Lower Tester: IoCapabilityReq {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            // Procedure collision.
            // The Peripheral must accept the Central-initiated procedure.
            // and abandon the Peripheral-initiated one.
            Lower Tester -> IUT: IoCapabilityReq {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            IUT -> Upper Tester: IoCapabilityResponse {
                bd_addr: context.peer_address(),
                io_capability: IoCapability::DisplayYesNo,
                oob_data_present: OobDataPresent::NotPresent,
                authentication_requirements: AuthenticationRequirements::NoBondingMitmProtection,
            }
            IUT -> Upper Tester: IoCapabilityRequest {
                bd_addr: context.peer_address(),
            }
            Lower Tester -> IUT: NotAcceptedExt {
                transaction_id: 0,
                not_accepted_opcode: ExtendedOpcode::IoCapabilityReq,
                error_code: hci::ErrorCode::LinkLayerCollision.into(),
            }
            IUT -> Upper Tester: AuthenticationComplete {
                status: ErrorCode::LinkLayerCollision,
                connection_handle: context.peer_handle(),
            }
            Upper Tester -> IUT: IoCapabilityRequestReply {
                bd_addr: context.peer_address(),
                io_capability: IoCapability::DisplayYesNo,
                oob_present: OobDataPresent::NotPresent,
                authentication_requirements: AuthenticationRequirements::NoBondingMitmProtection,
            }
            IUT -> Upper Tester: IoCapabilityRequestReplyComplete {
                num_hci_command_packets: 1,
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            IUT -> Lower Tester: IoCapabilityRes {
                transaction_id: 0,
                io_capabilities: 0x01,
                oob_authentication_data: 0x00,
                authentication_requirement: 0x01,
            }
            // Public Key Exchange
            Lower Tester -> IUT: EncapsulatedHeader {
                transaction_id: 0,
                major_type: 1,
                minor_type: 1,
                payload_length: 48,
            }
            IUT -> Lower Tester: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::EncapsulatedHeader,
            }
            repeat 3 times with (part in peer_p192_public_key()) {
                Lower Tester -> IUT: EncapsulatedPayload {
                    transaction_id: 0,
                    data: part,
                }
                IUT -> Lower Tester: Accepted {
                    transaction_id: 0,
                    accepted_opcode: Opcode::EncapsulatedPayload,
                }
            }
            IUT -> Lower Tester: EncapsulatedHeader {
                transaction_id: 0,
                major_type: 1,
                minor_type: 1,
                payload_length: 48,
            }
            Lower Tester -> IUT: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::EncapsulatedHeader,
            }
            repeat 3 times with (part in local_p192_public_key(&context)) {
                IUT -> Lower Tester: EncapsulatedPayload {
                    transaction_id: 0,
                    data: part,
                }
                Lower Tester -> IUT: Accepted {
                    transaction_id: 0,
                    accepted_opcode: Opcode::EncapsulatedPayload,
                }
            }
            // Authentication Stage 1: Numeric Comparison Protocol
            IUT -> Lower Tester: SimplePairingConfirm {
                transaction_id: 0,
                commitment_value: [0; 16],
            }
            Lower Tester -> IUT: SimplePairingNumber {
                transaction_id: 0,
                nonce: [0; 16],
            }
            IUT -> Lower Tester: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::SimplePairingNumber,
            }
            IUT -> Lower Tester: SimplePairingNumber {
                transaction_id: 0,
                nonce: [0; 16],
            }
            Lower Tester -> IUT: Accepted {
                transaction_id: 0,
                accepted_opcode: Opcode::SimplePairingNumber,
            }
            IUT -> Upper Tester: UserConfirmationRequest { bd_addr: context.peer_address(), numeric_value: 0 }
            Upper Tester -> IUT: UserConfirmationRequestReply { bd_addr: context.peer_address() }
            IUT -> Upper Tester: UserConfirmationRequestReplyComplete {
                num_hci_command_packets: 1,
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            // Authentication Stage 2
            Lower Tester -> IUT: DhkeyCheck {
                transaction_id: 0,
                confirmation_value: [0; 16],
            }
            IUT -> Lower Tester: Accepted { transaction_id: 0, accepted_opcode: Opcode::DhkeyCheck }
            IUT -> Lower Tester: DhkeyCheck {
                transaction_id: 0,
                confirmation_value: [0; 16],
            }
            Lower Tester -> IUT: Accepted { transaction_id: 0, accepted_opcode: Opcode::DhkeyCheck }
            IUT -> Upper Tester: SimplePairingComplete {
                status: ErrorCode::Success,
                bd_addr: context.peer_address(),
            }
            // Link Key Calculation
            Lower Tester -> IUT: AuRand {
                transaction_id: 0,
                random_number: [0; 16],
            }
            IUT -> Lower Tester: Sres {
                transaction_id: 0,
                authentication_rsp: [0; 4],
            }
            IUT -> Lower Tester: AuRand {
                transaction_id: 0,
                random_number: [0; 16],
            }
            Lower Tester -> IUT: Sres {
                transaction_id: 0,
                authentication_rsp: [0; 4],
            }
            IUT -> Upper Tester: LinkKeyNotification {
                bd_addr: context.peer_address(),
                key_type: KeyType::AuthenticatedP192,
                link_key: [0; 16],
            }
        }
    }
}
