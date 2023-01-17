use bluetooth_core::gatt;

mod mocks;

#[test]
fn basic() {
    let _gatt = gatt::server::GattModule::new(todo!());
}
