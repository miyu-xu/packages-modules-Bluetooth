use std::path::Path;

/// Folder to keep files which override floss configuration
const FLOSS_SYSPROPS_OVERRIDE_DIR: &str = "/var/lib/bluetooth/sysprops.conf.d";

pub fn read_floss_ll_privacy_enabled() -> std::io::Result<bool> {
    let parent = Path::new(FLOSS_SYSPROPS_OVERRIDE_DIR);
    if !parent.is_dir() {
        return Ok(false);
    }

    let data = std::fs::read_to_string(format!(
        "{}/{}",
        FLOSS_SYSPROPS_OVERRIDE_DIR, "privacy_override.conf"
    ))?;

    Ok(data == "[Sysprops]\nbluetooth.core.gap.le.privacy.enabled=true\n")
}

pub fn read_floss_address_privacy_enabled() -> std::io::Result<bool> {
    let parent = Path::new(FLOSS_SYSPROPS_OVERRIDE_DIR);
    if !parent.is_dir() {
        return Ok(false);
    }

    let data = std::fs::read_to_string(format!(
        "{}/{}",
        FLOSS_SYSPROPS_OVERRIDE_DIR, "privacy_address_override.conf"
    ))?;

    Ok(data == "[Sysprops]\nbluetooth.core.gap.le.privacy.own_address_type.enabled=true\n")
}
