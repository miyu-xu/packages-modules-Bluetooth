//! This library provides helper functions to parse info from advertising data.

use std::collections::HashMap;

use bt_topshim::bindings::root::bluetooth::Uuid;
use bt_topshim::btif::Uuid128Bit;

// Advertising data types.
const FLAGS: u8 = 0x01;
const COMPLETE_LIST_128_BIT_SERVICE_UUIDS: u8 = 0x07;
const SHORTENED_LOCAL_NAME: u8 = 0x08;
const COMPLETE_LOCAL_NAME: u8 = 0x09;
const SERVICE_DATA_128_BIT_UUID: u8 = 0x21;
const MANUFACTURER_SPECIFIC_DATA: u8 = 0xff;

// Helper function to extract flags from advertising data
pub fn extract_flags(bytes: &[u8]) -> u8 {
    let (i, len) = get_location_by_type(bytes, FLAGS);
    if (i, len) == (0, 0) || len != 1 {
        return 0;
    }

    bytes[i]
}

// Helper function to extract service uuids (128bit) from advertising data
pub fn extract_service_uuids(bytes: &[u8]) -> Vec<Uuid128Bit> {
    let mut uuids: Vec<Uuid128Bit> = Vec::new();
    let (pos, len) = get_location_by_type(bytes, COMPLETE_LIST_128_BIT_SERVICE_UUIDS);

    let mut i = pos;
    while (i + 16) <= (pos + len) {
        if i + 16 > bytes.len() {
            break;
        }
        match Uuid::try_from(bytes[i..i + 16].to_vec()) {
            Ok(uuid) => uuids.push(uuid.uu),
            Err(..) => {}
        }
        i += 16;
    }

    uuids
}

// Helper function to extract name from advertising data
pub fn extract_name(bytes: &[u8]) -> String {
    let (mut i, mut len) = get_location_by_type(bytes, COMPLETE_LOCAL_NAME);
    if (i, len) == (0, 0) {
        (i, len) = get_location_by_type(bytes, SHORTENED_LOCAL_NAME);
    }

    String::from_utf8_lossy(&bytes[i..i + len]).to_string()
}

// Helper function to extract service data from advertising data
pub fn extract_service_data(bytes: &[u8]) -> HashMap<String, Vec<u8>> {
    let mut service_data: HashMap<String, Vec<u8>> = HashMap::new();
    let (mut i, mut len) = get_location_by_type(bytes, SERVICE_DATA_128_BIT_UUID);
    let mut adv_data = bytes.clone();

    while (i, len) != (0, 0) && len >= 16 {
        match Uuid::try_from(adv_data[i..i + 16].to_vec()) {
            Ok(uuid) => {
                let data = match len - 16 {
                    0 => Vec::<u8>::new(),
                    _ => adv_data[i + 16..i + len].to_vec(),
                };
                service_data.insert(uuid.to_string(), data);
            }
            Err(..) => {}
        }
        adv_data = &adv_data[i + len..adv_data.len()];
        (i, len) = get_location_by_type(adv_data, SERVICE_DATA_128_BIT_UUID);
    }

    service_data
}

// Helper function to extract manufacturer data from advertising data
pub fn extract_manufacturer_data(bytes: &[u8]) -> HashMap<u16, Vec<u8>> {
    let mut manufacturer_data: HashMap<u16, Vec<u8>> = HashMap::new();
    let (mut i, mut len) = get_location_by_type(bytes, MANUFACTURER_SPECIFIC_DATA);
    let mut adv_data = bytes.clone();

    while (i, len) != (0, 0) && len >= 2 {
        let data = match len - 2 {
            0 => Vec::<u8>::new(),
            _ => adv_data[i + 2..i + len].to_vec(),
        };
        manufacturer_data.insert(((adv_data[i] as u16) << 8) | adv_data[i + 1] as u16, data);

        adv_data = &adv_data[i + len..adv_data.len()];
        (i, len) = get_location_by_type(adv_data, MANUFACTURER_SPECIFIC_DATA);
    }

    manufacturer_data
}

// Helper function that returns index into bytes where data_type resides as well as length of data
fn get_location_by_type(bytes: &[u8], data_type: u8) -> (usize, usize) {
    let mut i = 0;
    while i < bytes.len() {
        let len: usize = bytes[i].into();
        if (len == 0) || (i + len >= bytes.len()) {
            break;
        }
        if bytes[i + 1] == data_type {
            return (i + 2, len - 1);
        }
        i += len + 1;
    }
    (0, 0)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_extract_flags() {
        let payload: Vec<u8> = vec![
            2,
            FLAGS,
            3,
            17,
            COMPLETE_LIST_128_BIT_SERVICE_UUIDS,
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
        ];
        let flags = extract_flags(payload.as_slice());
        assert_eq!(flags, 3);
    }

    #[test]
    fn test_extract_service_uuids() {
        let payload: Vec<u8> = vec![2, FLAGS, 3];
        let uuids = extract_service_uuids(payload.as_slice());
        assert_eq!(uuids.len(), 0);

        let payload: Vec<u8> = vec![
            2,
            FLAGS,
            3,
            17,
            COMPLETE_LIST_128_BIT_SERVICE_UUIDS,
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
        ];
        let uuids = extract_service_uuids(payload.as_slice());
        assert_eq!(uuids.len(), 1);
        assert_eq!(
            uuids[0],
            Uuid::try_from(vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]).unwrap().uu
        );
    }

    #[test]
    fn test_extract_name() {
        let payload: Vec<u8> = vec![2, FLAGS, 3];
        let name = extract_name(payload.as_slice());
        assert_eq!(name, "");

        let payload: Vec<u8> = vec![2, FLAGS, 3, 5, COMPLETE_LOCAL_NAME, 116, 101, 115, 116];
        let name = extract_name(payload.as_slice());
        assert_eq!(name, "test");

        let payload: Vec<u8> = vec![2, FLAGS, 3, 5, SHORTENED_LOCAL_NAME, 116, 101, 115, 116];
        let name = extract_name(payload.as_slice());
        assert_eq!(name, "test");
    }

    #[test]
    fn test_extract_service_data() {
        let payload: Vec<u8> = vec![2, FLAGS, 3];
        let service_data = extract_service_data(payload.as_slice());
        assert_eq!(service_data.len(), 0);

        let payload: Vec<u8> = vec![
            18,
            SERVICE_DATA_128_BIT_UUID,
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            SERVICE_DATA_128_BIT_UUID,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
        ];
        let service_data = extract_service_data(payload.as_slice());
        assert_eq!(service_data.len(), 2);
        let expected_uuid =
            Uuid::try_from(vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15])
                .unwrap()
                .to_string();
        assert_eq!(service_data.get(&expected_uuid), Some(&vec![16]));
        let expected_uuid =
            Uuid::try_from(vec![1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16])
                .unwrap()
                .to_string();
        assert_eq!(service_data.get(&expected_uuid), Some(&vec![]));
    }

    #[test]
    fn test_extract_manufacturer_data() {
        let payload: Vec<u8> = vec![2, FLAGS, 3];
        let manufacturer_data = extract_manufacturer_data(payload.as_slice());
        assert_eq!(manufacturer_data.len(), 0);

        let payload: Vec<u8> =
            vec![4, MANUFACTURER_SPECIFIC_DATA, 0, 1, 2, 3, MANUFACTURER_SPECIFIC_DATA, 1, 2];
        let manufacturer_data = extract_manufacturer_data(payload.as_slice());
        assert_eq!(manufacturer_data.len(), 2);
        assert_eq!(manufacturer_data.get(&1), Some(&vec![2]));
        assert_eq!(manufacturer_data.get(&258), Some(&vec![]));
    }
}
