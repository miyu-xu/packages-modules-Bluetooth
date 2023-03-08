//! This module initializes the built-in services included in every
//! GATT server.

use anyhow::Result;

use self::{gap::register_gap_service, gatt::register_gatt_service};

use super::gatt_database::GattDatabase;
mod gap;
mod gatt;

/// Register all built-in services with the provided database
pub fn register_builtin_services(database: &mut GattDatabase) -> Result<()> {
    register_gap_service(database)?;
    register_gatt_service(database)?;
    Ok(())
}
