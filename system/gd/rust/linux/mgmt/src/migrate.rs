use configparser::ini::Ini;
use glob::glob;
use std::collections::HashMap;
use std::fs;
use std::path::Path;

use log::{debug, error, info, warn};

const BT_LIBDIR: &str = "/var/lib/bluetooth";
const FLOSS_CONF_FILE: &str = "/var/lib/bluetooth/bt_config.conf";

const ADAPTER_SECTION_NAME: &str = "Adapter";
const GENERAL_SECTION_NAME: &str = "General";
const LINKKEY_SECTION_NAME: &str = "LinkKey";
const DEVICEID_SECTION_NAME: &str = "DeviceID";

struct DeviceKey {
    pub key: &'static str,
    pub convert_val: Box<dyn Fn(String) -> Result<String, String>>,
    // Used in Floss to BlueZ conversion
    pub section: &'static str,
}

impl DeviceKey {
    fn new(key: &'static str) -> Self {
        Self { key: key, convert_val: Box::new(|x| Ok(x)), section: "" }
    }
    fn new_with_fn(
        key: &'static str,
        convert_val: impl Fn(String) -> Result<String, String> + 'static,
    ) -> Self {
        Self { key: key, convert_val: Box::new(convert_val), section: "" }
    }
    fn new_with_sec(key: &'static str, sec: &'static str) -> Self {
        Self { key: key, convert_val: Box::new(|x| Ok(x)), section: sec }
    }
    fn new_with_fn_sec(
        key: &'static str,
        convert_val: impl Fn(String) -> Result<String, String> + 'static,
        sec: &'static str,
    ) -> Self {
        Self { key: key, convert_val: Box::new(convert_val), section: sec }
    }
}

fn hex_str_to_dec_str(str: String) -> Result<String, String> {
    match u32::from_str_radix(str.trim_start_matches("0x"), 16) {
        Ok(str) => Ok(format!("{}", str)),
        Err(err) => Err(format!("Error converting from hex string to dec string: {}", err)),
    }
}

fn dec_str_to_hex_str(str: String) -> Result<String, String> {
    match str.parse::<u32>() {
        Ok(x) => Ok(format!("0x{:X}", x)),
        Err(err) => Err(format!("Error converting from dec string to hex string: {}", err)),
    }
}

fn base64_str_to_hex_str(str: String) -> Result<String, String> {
    match base64::decode(str) {
        Ok(bytes) => {
            let res: String = bytes.iter().map(|b| format!("{:02x}", b)).collect();
            Ok(res)
        }
        Err(err) => Err(format!("Error converting from base64 string to hex string: {}", err)),
    }
}

fn hex_str_to_base64_str(str: String) -> Result<String, String> {
    // Make vector of bytes from octets
    let mut bytes = Vec::new();
    for i in 0..(str.len() / 2) {
        let res = u8::from_str_radix(&str[2 * i..2 * i + 2], 16);
        match res {
            Ok(v) => bytes.push(v),
            Err(err) => {
                return Err(format!("Error converting from hex string to base64 string: {}", err));
            }
        }
    }

    Ok(base64::encode(&bytes))
}

fn bluez_to_floss_type(str: String) -> Result<String, String> {
    match str.as_str() {
        "BR/EDR;" => Ok("1".into()),
        "LE;" => Ok("2".into()),
        "BR/EDR;LE;" => Ok("3".into()),
        "LE;BR/EDR;" => Ok("3".into()),
        x => Err(format!("Error converting type. Unknown type: {}", x)),
    }
}

fn floss_to_bluez_type(str: String) -> Result<String, String> {
    match str.as_str() {
        "1" => Ok("BR/EDR;".into()),
        "2" => Ok("LE;".into()),
        "3" => Ok("BR/EDR;LE;".into()),
        x => Err(format!("Error converting type. Unknown type: {}", x)),
    }
}

fn reverse_endianness_link_key(str: String) -> Result<String, String> {
    match u128::from_str_radix(&str, 16) {
        // BlueZ stores link keys as little endian and Floss as big endian
        Ok(x) => Ok(format!("{:x}", x.swap_bytes())),
        Err(err) => Err(format!("Error converting link key: {}", err)),
    }
}

fn bluez_to_floss_addr_type(str: String) -> Result<String, String> {
    match str.as_str() {
        "public" => Ok("0".into()),
        "static" => Ok("1".into()),
        x => Err(format!("Error converting address type. Unknown type: {}", x)),
    }
}

fn floss_to_bluez_addr_type(str: String) -> Result<String, String> {
    match str.as_str() {
        "0" => Ok("public".into()),
        "1" => Ok("static".into()),
        x => Err(format!("Error converting address type. Unknown type: {}", x)),
    }
}

fn convert_bluez_device(filename: &str, addr: &str, floss_conf: &mut Ini) -> bool {
    // Floss device address strings need to be lower case
    let addr_lower = addr.to_lowercase();

    let mut bluez_conf = Ini::new_cs();
    // Default Ini uses ";" and "#" for comments
    bluez_conf.set_comment_symbols(&['!', '#']);
    let bluez_map = bluez_conf.load(filename).unwrap();

    for (sec, props) in bluez_map {
        // Special handling for LE keys since in Floss they are a combination of values in BlueZ
        match sec.as_str() {
            "IdentityResolvingKey" => {
                // In Floss, LE_KEY_PID = IRK + Identity Address Type (8) + Identity Address
                let irk = reverse_endianness_link_key(
                    bluez_conf.get(sec.as_str(), "Key").unwrap_or_default(),
                )
                .unwrap_or_default();
                let addr_type = bluez_to_floss_addr_type(
                    bluez_conf.get(GENERAL_SECTION_NAME, "AddressType").unwrap_or_default(),
                )
                .unwrap_or_default()
                .parse::<u8>()
                .unwrap_or_default();
                floss_conf.set(
                    addr_lower.as_str(),
                    "LE_KEY_PID",
                    Some(format!("{}{:02x}{}", irk, addr_type, addr_lower.replace(":", ""))),
                );
                continue;
            }
            "PeripheralLongTermKey" | "LongTermKey" => {
                // In Floss, LE_KEY_PENC = LTK + RAND (64) + EDIV (16) + Security Level (8) + Key Length (8)
                let ltk = bluez_conf.get(sec.as_str(), "Key").unwrap_or_default().to_lowercase();
                let rand = bluez_conf
                    .get(sec.as_str(), "Rand")
                    .unwrap_or_default()
                    .parse::<u64>()
                    .unwrap_or_default();
                let ediv = bluez_conf
                    .get(sec.as_str(), "EDiv")
                    .unwrap_or_default()
                    .parse::<u16>()
                    .unwrap_or_default();
                let sec_level = bluez_conf
                    .get(sec.as_str(), "Authenticated")
                    .unwrap_or_default()
                    .parse::<u8>()
                    .unwrap_or_default();
                let len = bluez_conf
                    .get(sec.as_str(), "EncSize")
                    .unwrap_or_default()
                    .parse::<u8>()
                    .unwrap_or_default();
                floss_conf.set(
                    addr_lower.as_str(),
                    "LE_KEY_PENC",
                    Some(format!(
                        "{}{:016x}{:04x}{:02x}{:02x}",
                        ltk,
                        rand.swap_bytes(),
                        ediv.swap_bytes(),
                        sec_level,
                        len
                    )),
                );
                continue;
            }
            _ => {}
        }
        let map: HashMap<&str, DeviceKey> = match sec.as_str() {
            GENERAL_SECTION_NAME => [
                ("Name", DeviceKey::new("Name")),
                ("Class", DeviceKey::new_with_fn("DevClass", Box::new(&hex_str_to_dec_str))),
                (
                    "SupportedTechnologies",
                    DeviceKey::new_with_fn("DevType", Box::new(&bluez_to_floss_type)),
                ),
                (
                    "Services",
                    DeviceKey::new_with_fn(
                        "Service",
                        Box::new(|v: String| Ok(v.replace(";", " "))),
                    ),
                ),
                (
                    "AddressType",
                    DeviceKey::new_with_fn("AddrType", Box::new(&bluez_to_floss_addr_type)),
                ),
            ]
            .into(),
            LINKKEY_SECTION_NAME => [
                ("Key", DeviceKey::new_with_fn("LinkKey", Box::new(&reverse_endianness_link_key))),
                ("Type", DeviceKey::new("LinkKeyType")),
                ("PINLength", DeviceKey::new("PinLength")),
            ]
            .into(),
            DEVICEID_SECTION_NAME => [
                ("Source", DeviceKey::new("SdpDiVendorIdSource")),
                ("Vendor", DeviceKey::new("SdpDiManufacturer")),
                ("Product", DeviceKey::new("SdpDiModel")),
                ("Version", DeviceKey::new("SdpDiHardwareVersion")),
            ]
            .into(),
            _ => [].into(),
        };
        for (k, v) in props {
            match map.get(k.as_str()) {
                Some(key) => {
                    let new_val = match (key.convert_val)(v.unwrap_or_default()) {
                        Ok(val) => val,
                        Err(err) => {
                            error!("Error converting BlueZ conf to Floss conf: {}. Dropping conversion for device {}", err, addr);
                            floss_conf.remove_section(addr_lower.as_str());
                            return false;
                        }
                    };
                    floss_conf.set(addr_lower.as_str(), key.key.clone(), Some(new_val));
                }
                None => {
                    debug!("No key match: {}", k);
                }
            }
        }
    }

    true
}

fn convert_bluez_hid(filename: &str, addr: &str, floss_conf: &mut Ini) {
    // Floss device address strings need to be lower case
    let addr_lower = addr.to_lowercase();

    let mut bluez_conf = Ini::new_cs();
    bluez_conf.set_comment_symbols(&['!', '#']);
    let bluez_map = bluez_conf.load(filename).unwrap();

    // Floss will not load the HID info unless it sees this key and BlueZ does not have a matching key
    floss_conf.set(addr_lower.as_str(), "HidAttrMask", Some("0".into()));

    for (sec, props) in bluez_map {
        let map: HashMap<&str, DeviceKey> = match sec.as_str() {
            "ReportMap" => [(
                "report_map",
                DeviceKey::new_with_fn("HidDescriptor", Box::new(&base64_str_to_hex_str)),
            )]
            .into(),
            GENERAL_SECTION_NAME => [
                ("bcdhid", DeviceKey::new("HidVersion")),
                ("bcountrycode", DeviceKey::new("HidCountryCode")),
            ]
            .into(),
            _ => [].into(),
        };
        for (k, v) in props {
            match map.get(k.as_str()) {
                Some(key) => {
                    let new_val = match (key.convert_val)(v.unwrap_or_default()) {
                        Ok(val) => val,
                        Err(err) => {
                            error!("Error converting BlueZ conf to Floss conf: {}. Dropping conversion for device {}", err, addr);
                            floss_conf.remove_section(addr_lower.as_str());
                            return;
                        }
                    };
                    floss_conf.set(addr_lower.as_str(), key.key.clone(), Some(new_val));
                }
                None => {
                    debug!("No key match: {}", k);
                }
            }
        }
    }
}

pub fn migrate_bluez_devices() {
    let mut adapter_conf_map: HashMap<String, Ini> = HashMap::new();

    // Find and parse all device files
    for entry in glob(format!("{}/*:*/*:*/info", BT_LIBDIR).as_str())
        .expect("Didn't find any BlueZ adapters to migrate")
    {
        let pathbuf = entry.unwrap_or_default();
        let addrs = pathbuf.to_str().unwrap_or_default().split('/').collect::<Vec<&str>>();
        let adapter_addr = addrs[addrs.len() - 3];
        let device_addr = addrs[addrs.len() - 2];
        // Create new Ini file if it doesn't already exist
        adapter_conf_map.entry(adapter_addr.into()).or_insert(Ini::new_cs());
        if !convert_bluez_device(
            pathbuf.to_str().unwrap_or_default(),
            device_addr,
            adapter_conf_map.get_mut(adapter_addr).unwrap(),
        ) {
            continue;
        }

        // Check if we have HID info
        let hid_path = pathbuf.to_str().unwrap_or_default().replace("info", "hog-uhid-cache");
        if Path::new(hid_path.as_str()).exists() {
            convert_bluez_hid(
                hid_path.as_str(),
                device_addr,
                adapter_conf_map.get_mut(adapter_addr).unwrap(),
            );
        }
    }

    // Write migration to appropriate adapter files
    // TODO(b/232138101): Update for multi-adapter support
    for (adapter, conf) in adapter_conf_map.iter_mut() {
        let mut existing_conf = Ini::new_cs();
        match existing_conf.load(FLOSS_CONF_FILE) {
            Ok(ini) => {
                let devices = conf.sections();
                for (sec, props) in ini {
                    // Drop devices that don't exist in BlueZ
                    if sec.contains(":") && !devices.contains(&sec) {
                        continue;
                    }
                    // Keep keys that weren't transferrable
                    for (k, v) in props {
                        if conf.get(sec.as_str(), k.as_str()) == None {
                            conf.set(sec.as_str(), k.as_str(), v);
                        }
                    }
                }
            }
            // Conf file doesn't exist yet
            Err(_) => {
                conf.set(ADAPTER_SECTION_NAME, "Address", Some(adapter.clone()));
            }
        }
        // Write contents to file
        match conf.write(FLOSS_CONF_FILE) {
            Ok(_) => {
                debug!("Successfully migrated devices from BlueZ to Floss for adapter {}", adapter);
            }
            Err(err) => {
                error!(
                    "Error migrating devices from BlueZ to Floss for adapter {}: {}",
                    adapter, err
                );
            }
        }
    }
}

fn merge_and_write_bluez_conf(filepath: String, conf: &mut Ini) {
    let mut existing_conf = Ini::new_cs();
    existing_conf.set_comment_symbols(&['!', '#']);
    match existing_conf.load(filepath.clone()) {
        Ok(ini) => {
            for (sec, props) in ini {
                // Keep keys that weren't transferrable
                for (k, v) in props {
                    if conf.get(sec.as_str(), k.as_str()) == None {
                        conf.set(sec.as_str(), k.as_str(), v);
                    }
                }
            }
        }
        Err(_) => {}
    }
    // Write file
    match conf.write(filepath.clone()) {
        Ok(_) => {
            info!("Successfully migrated Floss to BlueZ: {}", filepath);
        }
        Err(err) => {
            error!("Error writing Floss to BlueZ: {}: {}", filepath, err);
        }
    }
}

fn convert_floss_conf(filename: &str) {
    let mut floss_conf = Ini::new_cs();
    let floss_map = floss_conf.load(filename).unwrap();

    let adapter_addr = match floss_conf.get(ADAPTER_SECTION_NAME, "Address") {
        Some(addr) => addr.to_uppercase(),
        None => {
            warn!("No adapter address during Floss to BlueZ migration in {}", filename);
            return;
        }
    };

    let info_map: HashMap<&str, DeviceKey> = [
        // General
        ("Name", DeviceKey::new_with_sec("Name", GENERAL_SECTION_NAME)),
        (
            "DevClass",
            DeviceKey::new_with_fn_sec(
                "Class",
                Box::new(&dec_str_to_hex_str),
                GENERAL_SECTION_NAME,
            ),
        ),
        (
            "DevType",
            DeviceKey::new_with_fn_sec(
                "SupportedTechnologies",
                Box::new(&floss_to_bluez_type),
                GENERAL_SECTION_NAME,
            ),
        ),
        (
            "Service",
            DeviceKey::new_with_fn_sec(
                "Services",
                Box::new(|v: String| Ok(v.replace(" ", ";"))),
                GENERAL_SECTION_NAME,
            ),
        ),
        (
            "AddrType",
            DeviceKey::new_with_fn_sec(
                "AddressType",
                Box::new(&floss_to_bluez_addr_type),
                GENERAL_SECTION_NAME,
            ),
        ),
        // LinkKey
        (
            "LinkKey",
            DeviceKey::new_with_fn_sec(
                "Key",
                Box::new(|k: String| {
                    Ok(reverse_endianness_link_key(k).unwrap_or_default().to_uppercase())
                }),
                LINKKEY_SECTION_NAME,
            ),
        ),
        ("LinkKeyType", DeviceKey::new_with_sec("Type", LINKKEY_SECTION_NAME)),
        ("PinLength", DeviceKey::new_with_sec("PINLength", LINKKEY_SECTION_NAME)),
        // DeviceID
        ("SdpDiVendorIdSource", DeviceKey::new_with_sec("Source", DEVICEID_SECTION_NAME)),
        ("SdpDiManufacturer", DeviceKey::new_with_sec("Vendor", DEVICEID_SECTION_NAME)),
        ("SdpDiModel", DeviceKey::new_with_sec("Product", DEVICEID_SECTION_NAME)),
        ("SdpDiHardwareVersion", DeviceKey::new_with_sec("Version", DEVICEID_SECTION_NAME)),
        // In Floss, LE_KEY_PID = IRK + Identity Address Type (8) + Identity Address
        (
            "LE_KEY_PID",
            DeviceKey::new_with_fn_sec(
                "Key",
                Box::new(|k: String| {
                    Ok(reverse_endianness_link_key(String::from(&k[0..32]))
                        .unwrap_or_default()
                        .to_uppercase())
                }),
                "IdentityResolvingKey",
            ),
        ),
    ]
    .into();

    let hid_map: HashMap<&str, DeviceKey> = [
        // General
        ("HidVersion", DeviceKey::new_with_sec("bcdhid", GENERAL_SECTION_NAME)),
        ("HidCountryCode", DeviceKey::new_with_sec("bcountrycode", GENERAL_SECTION_NAME)),
        // ReportMap
        (
            "HidDescriptor",
            DeviceKey::new_with_fn_sec("report_map", Box::new(&hex_str_to_base64_str), "ReportMap"),
        ),
    ]
    .into();

    let mut devices: Vec<String> = Vec::new();
    for (sec, props) in floss_map {
        // Skip all the non-adapter sections
        if !sec.contains(":") {
            continue;
        }
        devices.push(sec.clone());
        let device_addr = sec.to_uppercase();
        let mut bluez_info = Ini::new_cs();
        let mut bluez_hid = Ini::new_cs();
        let mut is_hid: bool = false;
        for (k, v) in props {
            // Special handling since in Floss LE_KEY_PENC is a combination of values in BlueZ
            // In Floss, LE_KEY_PENC = LTK + RAND (64) + EDIV (16) + Security Level (8) + Key Length (8)
            if k == "LE_KEY_PENC" {
                let val: String = v.unwrap_or_default();
                let rand = u64::from_str_radix(&val[32..48], 16).unwrap_or_default().swap_bytes();
                let ediv = u16::from_str_radix(&val[48..52], 16).unwrap_or_default().swap_bytes();
                let auth = u8::from_str_radix(&val[52..54], 16).unwrap_or_default();
                let len = u8::from_str_radix(&val[54..56], 16).unwrap_or_default();
                bluez_info.set(
                    "LongTermKey",
                    "Key",
                    Some(String::from(&val[0..32]).to_uppercase()),
                );
                bluez_info.set("LongTermKey", "Rand", Some(format!("{}", rand)));
                bluez_info.set("LongTermKey", "EDiv", Some(format!("{}", ediv)));
                bluez_info.set("LongTermKey", "Authenticated", Some(format!("{}", auth)));
                bluez_info.set("LongTermKey", "EncSize", Some(format!("{}", len)));
                continue;
            }
            match info_map.get(k.as_str()) {
                Some(key) => {
                    let new_val = match (key.convert_val)(v.unwrap_or_default()) {
                        Ok(val) => val,
                        Err(err) => {
                            warn!("Error converting Floss to Bluez key for adapter {}, device {}, key {}: {}", adapter_addr, device_addr, k, err);
                            continue;
                        }
                    };
                    bluez_info.set(key.section, key.key.clone(), Some(new_val));
                    continue;
                }
                None => {}
            }
            match hid_map.get(k.as_str()) {
                Some(key) => {
                    is_hid = true;
                    let new_val = match (key.convert_val)(v.unwrap_or_default()) {
                        Ok(val) => val,
                        Err(err) => {
                            warn!("Error converting Floss to Bluez key for adapter {}, device {}, key {}: {}", adapter_addr, device_addr, k, err);
                            continue;
                        }
                    };
                    bluez_hid.set(key.section, key.key.clone(), Some(new_val));
                }
                None => {
                    debug!("No key match: {}", k)
                }
            }
        }

        let path = format!("{}/{}/{}", BT_LIBDIR, adapter_addr, device_addr);
        // Create device dir and all its parents if they're missing
        match fs::create_dir_all(path.clone()) {
            Ok(_) => (),
            Err(err) => {
                error!("Error creating dirs during Floss to BlueZ device migration for adapter{}, device {}: {}", adapter_addr, device_addr, err);
            }
        }
        // Write info file
        merge_and_write_bluez_conf(format!("{}/{}", path, "info"), &mut bluez_info);

        // Write hid file
        if is_hid {
            merge_and_write_bluez_conf(format!("{}/{}", path, "hog-uhid-cache"), &mut bluez_hid);
        }
    }

    // Delete devices that exist in BlueZ but not in Floss
    for entry in glob(format!("{}/{}/*:*", BT_LIBDIR, adapter_addr).as_str()).expect("") {
        let pathbuf = entry.unwrap_or_default();
        let addrs = pathbuf.to_str().unwrap_or_default().split('/').collect::<Vec<&str>>();
        let device_addr: String = addrs[addrs.len() - 1].into();
        if !devices.contains(&device_addr.to_lowercase()) {
            match fs::remove_dir_all(pathbuf) {
                Ok(_) => (),
                Err(err) => {
                    warn!(
                        "Error removing {} during Floss to BlueZ device migration: {}",
                        device_addr, err
                    );
                }
            }
        }
    }
}

pub fn migrate_floss_devices() {
    // Find and parse all conf files
    // TODO(b/232138101): Currently Floss only supports a single adapter; update here for multi-adapter support
    for entry in glob(FLOSS_CONF_FILE).expect("Didn't find Floss conf file to migrate") {
        convert_floss_conf(entry.unwrap_or_default().to_str().unwrap_or_default());
    }
}
