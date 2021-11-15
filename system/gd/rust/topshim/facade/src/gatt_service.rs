//! GATT service facade

use grpcio::*;

use bt_blueberry_protobuf::empty::Empty;
use bt_blueberry_protobuf::gatt_grpc::{create_gatt, Gatt};

#[derive(Clone)]
pub struct GattService {}

impl GattService {
    pub fn create() -> grpcio::Service {
        create_gatt(GattService {})
    }
}

impl Gatt for GattService {
    fn connect(&mut self, _ctx: RpcContext<'_>, _req: Empty, _sink: UnarySink<Empty>) {
       println!("charlie gatt connect");
    }
}
