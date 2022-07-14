// Bluetooth Core, Vol 2, Part C, 4.3.4

use crate::packets::lmp;
use crate::procedure::Context;

pub async fn initiate(ctx: &impl Context, features_page: u8) -> u64 {
    ctx.send_lmp_packet(
        lmp::FeaturesReqExtBuilder {
            transaction_id: 0,
            features_page,
            max_supported_page: 1,
            extended_features: ctx.extended_features(features_page).to_le_bytes(),
        }
        .build(),
    );

    u64::from_le_bytes(
        *ctx.receive_lmp_packet::<lmp::FeaturesResExtPacket>().await.get_extended_features(),
    )
}

pub async fn respond(ctx: &impl Context) {
    let req = ctx.receive_lmp_packet::<lmp::FeaturesReqExtPacket>().await;
    let features_page = req.get_features_page();

    ctx.send_lmp_packet(
        lmp::FeaturesResExtBuilder {
            transaction_id: 0,
            features_page,
            max_supported_page: 1,
            extended_features: ctx.extended_features(features_page).to_le_bytes(),
        }
        .build(),
    );
}

macro_rules! supported_on_both {
    ($ctx:ident, $feature_page:expr, $feature:ident) => {{
        use paste::paste;
        use num_traits::ToPrimitive;
        paste! {
            let feature_mask = crate::packets::hci::[<LMPFeaturesPage $feature_page Bits>]::[<$feature>].to_u64().unwrap();
        }
        let local_supported = $ctx.extended_features($feature_page) & feature_mask != 0;
        // Lazy peer features
        let peer_supported = async move {
            let page = if let Some(page) = $ctx.peer_extended_features($feature_page) {
                page
            } else {
                features::initiate($ctx, $feature_page).await
            };
            page & feature_mask != 0
        };
        local_supported && peer_supported.await
    }};
}

pub(crate) use supported_on_both;
