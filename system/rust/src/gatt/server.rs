//! This module is a simple GATT server that shares the ATT channel with the existing C++ GATT client.
//! See go/private-gatt-in-platform for the design.

mod att_database;
pub mod server_connection;
mod transaction_handler;
mod transactions;

#[cfg(test)]
mod test;
mod utils;
