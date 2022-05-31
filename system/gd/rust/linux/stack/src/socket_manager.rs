//! Implementation of the Socket API (IBluetoothSocketManager).

use crate::bluetooth::BluetoothDevice;
use bt_topshim::btif::{BluetoothInterface, Uuid128Bit};
use bt_topshim::profiles::socket::SocketType;

use std::sync::{Arc, Mutex};

pub trait IBluetoothSocketManager {
    /// Connects L2CAP or RFCOMM socket to remote device.
    ///
    /// # Args
    /// `device`: Remote device to connect with.
    /// `sock_type`: Type of socket to open.
    /// `uuid`: Optional service uuid for RFCOMM connections.
    /// `port`: Either channel (RFCOMM) or PSM (L2CAP).
    /// `flags`: Additional flags on the socket. Reserved for now.
    ///
    /// # Returns
    ///
    /// Optional file descriptor if the connection succeeds.
    fn connect_socket(
        &mut self,
        device: BluetoothDevice,
        sock_type: SocketType,
        uuid: Option<Uuid128Bit>,
        port: i32,
        flags: i32,
    ) -> Option<std::fs::File>;

    /// Listen to a RFCOMM UUID or L2CAP channel.
    ///
    /// # Args
    /// `sock_type`:
    /// `service_name`:
    /// `uuid`:
    /// `port`:
    /// `flags`:
    ///
    /// # Returns
    ///
    /// Optional file descriptor if listening socket was established successfully.
    fn create_socket_channel(
        &mut self,
        sock_type: i32,
        service_name: String,
        uuid: Option<Uuid128Bit>,
        port: i32,
        flags: i32,
    ) -> Option<std::fs::File>;

    /// Set the LE Data Length value for this connected peer to the maximum
    /// supported by this BT controller.
    ///
    /// # Args
    /// `device`: Connected remote device to apply this setting against.
    fn request_maximum_tx_data_length(&mut self, device: BluetoothDevice);
}

/// Implementation of the `IBluetoothSocketManager` api.
pub struct BluetoothSocketManager {
    intf: Arc<Mutex<BluetoothInterface>>,
}

impl BluetoothSocketManager {
    /// Constructs the IBluetooth implementation.
    pub fn new(intf: Arc<Mutex<BluetoothInterface>>) -> Self {
        BluetoothSocketManager { intf }
    }
}

impl IBluetoothSocketManager for BluetoothSocketManager {
    fn connect_socket(
        &mut self,
        device: BluetoothDevice,
        sock_type: SocketType,
        uuid: Option<Uuid128Bit>,
        port: i32,
        flags: i32,
    ) -> Option<std::fs::File> {
        None
    }

    fn create_socket_channel(
        &mut self,
        sock_type: i32,
        service_name: String,
        uuid: Option<Uuid128Bit>,
        port: i32,
        flags: i32,
    ) -> Option<std::fs::File> {
        None
    }

    fn request_maximum_tx_data_length(&mut self, device: BluetoothDevice) {}
}
