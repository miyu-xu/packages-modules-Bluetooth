//! BLE Advertising utilities.

use bt_topshim::profiles::gatt::AdvertiseParameters;

use num_traits::clamp;
use std::collections::HashMap;

use crate::uuid::parse_uuid_string;

/// Advertising parameters for each BLE advertising set.
#[derive(Debug, Default)]
pub struct AdvertisingSetParameters {
    /// Whether the advertisement will be connectable.
    pub connectable: bool,
    /// Whether the advertisement will be scannable.
    pub scannable: bool,
    /// Whether the legacy advertisement will be used.
    pub is_legacy: bool,
    /// Whether the advertisement will be anonymous.
    pub is_anonymous: bool,
    /// Whether the TX Power will be included.
    pub include_tx_power: bool,
    /// Primary advertising phy. Valid values are: 1 (1M), 2 (2M), 3 (Coded).
    pub primary_phy: i32,
    /// Secondary advertising phy. Valid values are: 1 (1M), 2 (2M), 3 (Coded).
    pub secondary_phy: i32,
    /// The advertising interval. Bluetooth LE Advertising interval, in 0.625 ms unit.
    /// The valid range is from 160 (100 ms) to 16777215 (10485.759375 sec).
    /// Recommended values are: 160 (100 ms), 400 (250 ms), 1600 (1 sec).
    pub interval: i32,
    /// Transmission power of Bluetooth LE Advertising, in dBm. The valid range is [-127, 1].
    /// Recommended values are: -21, -15, 7, 1.
    pub tx_power_level: i32,
    /// Own address type for advertising to control public or privacy mode.
    /// The valid types are: -1 (default), 0 (public), 1 (random).
    pub own_address_type: i32,
}

/// Represents the data to be advertised and the scan response data for active scans.
#[derive(Debug, Default)]
pub struct AdvertiseData {
    /// A list of service UUIDs within the advertisement that are used to identify
    /// the Bluetooth GATT services.
    pub service_uuids: Vec<String>,
    /// A list of service solicitation UUIDs within the advertisement that we invite to connect.
    pub solicit_uuids: Vec<String>,
    /// A list of transport discovery data.
    pub transport_discovery_data: Vec<Vec<u8>>,
    /// A collection of manufacturer Id and the corresponding manufacturer specific data.
    pub manufacturer_data: HashMap<i32, Vec<u8>>,
    /// A map of 128-bit UUID and its corresponding service data.
    pub service_data: HashMap<String, Vec<u8>>,
    /// Whether TX Power level will be included in the advertising packet.
    pub include_tx_power_level: bool,
    /// Whether the device name will be included in the advertisement packet.
    pub include_device_name: bool,
}

/// Parameters of the periodic advertising packet for BLE advertising set.
#[derive(Debug, Default)]
pub struct PeriodicAdvertisingParameters {
    /// Whether TX Power level will be included.
    pub include_tx_power: bool,
    /// Periodic advertising interval in 1.25 ms unit. Valid values are from 80 (100 ms) to
    /// 65519 (81.89875 sec). Value from range [interval, interval+20ms] will be picked as
    /// the actual value.
    pub interval: i32,
}

// Advertising interval range.
const INTERVAL_MAX: i32 = 0xff_ffff; // 10485.759375 sec
const INTERVAL_MIN: i32 = 160; // 100 ms
const INTERVAL_DELTA: i32 = 50; // 31.25 ms gap between min and max
                                //
                                // Periodic advertising interval range.
const PERIODIC_INTERVAL_MAX: i32 = 65519; // 81.89875 sec
const PERIODIC_INTERVAL_MIN: i32 = 80; // 100 ms
const PERIODIC_INTERVAL_DELTA: i32 = 16; // 20 ms gap betwen min and max

// PHY range.
const PHY_MIN: i32 = 1;
const PHY_MAX: i32 = 3;

// Device name length.
const DEVICE_NAME_MAX: usize = 26;

// Advertising data types.
const COMPLETE_LIST_128_BIT_SERVICE_UUIDS: u8 = 0x07;
const SHORTENED_LOCAL_NAME: u8 = 0x08;
const COMPLETE_LOCAL_NAME: u8 = 0x09;
const TX_POWER_LEVEL: u8 = 0x0A;
const LIST_128_BIT_SERVICE_SOLICITATION_UUIDS: u8 = 0x15;
const SERVICE_DATA_128_BIT_UUID: u8 = 0x21;
const TRANSPORT_DISCOVERY_DATA: u8 = 0x26;
const MANUFACTURER_SPECIFIC_DATA: u8 = 0xFF;

pub(crate) fn parse_advertising_set_parameters(
    params: AdvertisingSetParameters,
) -> AdvertiseParameters {
    let mut props: u16 = 0;
    if params.connectable {
        props |= 0x01;
    }
    if params.scannable {
        props |= 0x02;
    }
    if params.is_legacy {
        props |= 0x10;
    }
    if params.is_anonymous {
        props |= 0x20;
    }
    if params.include_tx_power {
        props |= 0x40;
    }

    let interval = clamp(params.interval, INTERVAL_MIN, INTERVAL_MAX - INTERVAL_DELTA);
    let primary_phy = clamp(params.primary_phy, PHY_MIN, PHY_MAX);
    let secondary_phy = clamp(params.secondary_phy, PHY_MIN, PHY_MAX);

    AdvertiseParameters {
        advertising_event_properties: props,
        min_interval: interval as u32,
        max_interval: (interval + INTERVAL_DELTA) as u32,
        channel_map: 0x07 as u8, // all channels
        tx_power: params.tx_power_level as i8,
        primary_advertising_phy: primary_phy as u8,
        secondary_advertising_phy: secondary_phy as u8,
        scan_request_notification_enable: 0 as u8, // false
        own_address_type: params.own_address_type as i8,
    }
}

fn append_adv_data(dest: &mut Vec<u8>, ad_type: u8, ad_payload: &[u8]) {
    let len = clamp(ad_payload.len(), 0, 254);
    dest.push((len + 1) as u8);
    dest.push(ad_type);
    dest.extend(&ad_payload[..len]);
}

pub(crate) fn parse_advertise_data(
    advertise_data: Option<AdvertiseData>,
    device_name: &String,
) -> Vec<u8> {
    let mut bytes = Vec::<u8>::new();

    if let Some(data) = advertise_data {
        if device_name.len() > 0 && data.include_device_name {
            let mut name: Vec<u8> = device_name.as_bytes().to_vec();
            let mut ad_type = COMPLETE_LOCAL_NAME;
            if name.len() > DEVICE_NAME_MAX {
                ad_type = SHORTENED_LOCAL_NAME;
                name.resize(DEVICE_NAME_MAX, 0);
            }
            name.push(0);
            append_adv_data(&mut bytes, ad_type, &name);
        }

        let mut manufacturers: Vec<&i32> = data.manufacturer_data.keys().collect();
        manufacturers.sort();
        for m in manufacturers {
            let len = 2 + data.manufacturer_data[m].len();
            let mut concated = Vec::<u8>::with_capacity(len);
            concated.push((m & 0xff) as u8);
            concated.push((m >> 8 & 0xff) as u8);
            concated.extend(&data.manufacturer_data[m]);
            append_adv_data(&mut bytes, MANUFACTURER_SPECIFIC_DATA, &concated);
        }

        if data.include_tx_power_level {
            // Lower layers will fill tx power level.
            append_adv_data(&mut bytes, TX_POWER_LEVEL, &[0]);
        }

        let mut uu128_services = Vec::<u8>::new();
        for uuid_str in &data.service_uuids {
            if let Some(uuid) = parse_uuid_string(uuid_str) {
                match uuid.uu.len() {
                    16 => uu128_services.extend(uuid.uu),
                    _ => (),
                };
            }
        }
        if uu128_services.len() > 0 {
            append_adv_data(&mut bytes, COMPLETE_LIST_128_BIT_SERVICE_UUIDS, &uu128_services);
        }

        let uuids: Vec<&String> = data.service_data.keys().collect();
        for uuid_str in uuids {
            if let Some(uuid) = parse_uuid_string(uuid_str) {
                let uu_len = uuid.uu.len();
                let len = uu_len + data.service_data[uuid_str].len();
                let mut concated = Vec::<u8>::with_capacity(len);
                concated.extend(uuid.uu);
                concated.extend(&data.service_data[uuid_str]);

                match uu_len {
                    16 => append_adv_data(&mut bytes, SERVICE_DATA_128_BIT_UUID, &concated),
                    _ => (),
                };
            }
        }

        let mut uu128_solicits = Vec::<u8>::new();
        for uuid_str in &data.solicit_uuids {
            if let Some(uuid) = parse_uuid_string(uuid_str) {
                match uuid.uu.len() {
                    16 => uu128_solicits.extend(uuid.uu),
                    _ => (),
                };
            }
        }
        if uu128_solicits.len() > 0 {
            append_adv_data(&mut bytes, LIST_128_BIT_SERVICE_SOLICITATION_UUIDS, &uu128_solicits);
        }

        for tdd in &data.transport_discovery_data {
            if tdd.len() > 0 {
                append_adv_data(&mut bytes, TRANSPORT_DISCOVERY_DATA, &tdd);
            }
        }
    }
    bytes
}

pub(crate) fn parse_periodic_parameters(
    params: Option<PeriodicAdvertisingParameters>,
) -> bt_topshim::profiles::gatt::PeriodicAdvertisingParameters {
    let mut p = bt_topshim::profiles::gatt::PeriodicAdvertisingParameters {
        enable: 0,
        min_interval: 0,
        max_interval: 0,
        periodic_advertising_properties: 0,
    };

    if let Some(params) = params {
        let interval = clamp(
            params.interval,
            PERIODIC_INTERVAL_MIN,
            PERIODIC_INTERVAL_MAX - PERIODIC_INTERVAL_DELTA,
        );

        p.enable = 1;
        p.min_interval = interval as u16;
        p.max_interval = p.min_interval + (PERIODIC_INTERVAL_DELTA as u16);
        if params.include_tx_power {
            p.periodic_advertising_properties |= 0x40;
        }
    }
    p
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::iter::FromIterator;

    #[test]
    fn test_append_ad_data_clamped() {
        let mut bytes = Vec::<u8>::new();
        let mut ans = Vec::<u8>::new();
        ans.push(255);
        ans.push(102);
        ans.extend(Vec::<u8>::from_iter(0..254));

        let payload = Vec::<u8>::from_iter(0..255);
        append_adv_data(&mut bytes, 102, &payload);
        assert_eq!(bytes, ans);
    }

    #[test]
    fn test_append_ad_data_multiple() {
        let mut bytes = Vec::<u8>::new();

        let payload = vec![0 as u8, 1, 2, 3, 4];
        append_adv_data(&mut bytes, 100, &payload);
        append_adv_data(&mut bytes, 101, &[0]);
        assert_eq!(bytes, vec![6 as u8, 100, 0, 1, 2, 3, 4, 2, 101, 0]);
    }
}
