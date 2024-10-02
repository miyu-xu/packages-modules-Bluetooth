//! This library provides helper functions to parse chipinfo.

const CHIPSET_INFO_WLAN_DIR: &str = "/sys/class/net/wlan0/device";

pub fn read_wlan_chipset() -> std::io::Result<&'static str> {
    let vid = std::fs::read_to_string(format!("{}/{}", CHIPSET_INFO_WLAN_DIR, "vendor"))?
        .trim()
        .to_lowercase();
    let pid = std::fs::read_to_string(format!("{}/{}", CHIPSET_INFO_WLAN_DIR, "device"))?
        .trim()
        .to_lowercase();
    match vid.as_str() {
        "0x8086" => match pid.as_str() {
            "0x51f0" => Ok("AX211"),
            "0x51f1" => Ok("AX211"),
            "0x54f0" => Ok("AX211"),
            "0x7e40" => Ok("AX211"),
            _ => Ok("Unknown"),
        },
        _ => Ok("Unknown"),
    }
}
