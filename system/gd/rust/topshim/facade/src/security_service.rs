//! Security service facade

use bt_topshim::btif::{BluetoothInterface, BtTransport, OobData, RawAddress};

use bt_topshim_facade_protobuf::empty::Empty;
use bt_topshim_facade_protobuf::facade::{
    GenerateOobDataRequest, OobDataBondRequest, RemoveBondRequest,
};
use bt_topshim_facade_protobuf::facade_grpc::{create_security_service, SecurityService};
use grpcio::*;

use std::sync::{Arc, Mutex};
use tokio::runtime::Runtime;

pub fn decode_hex(s: &str) -> Vec<u8> {
    let mut r: Vec<u8> = Vec::new();
    let mut i = 0;
    loop {
        if i == s.len() - 2 {
            break;
        }
        if i % 2 != 0 {
            let v = u8::from_str_radix(&s[i..i + 2], 16).expect("Failed to parse hex string!");
            r.push(v);
        } else {
        }
        i += 1;
    }
    r
}

/// Main object for Adapter facade service
#[derive(Clone)]
pub struct SecurityServiceImpl {
    #[allow(dead_code)]
    rt: Arc<Runtime>,
    #[allow(dead_code)]
    btif_intf: Arc<Mutex<BluetoothInterface>>,
}

#[allow(dead_code)]
impl SecurityServiceImpl {
    /// Create a new instance of the root facade service
    pub fn create(rt: Arc<Runtime>, btif_intf: Arc<Mutex<BluetoothInterface>>) -> grpcio::Service {
        create_security_service(Self { rt, btif_intf })
    }
}

impl SecurityService for SecurityServiceImpl {
    fn remove_bond(&mut self, ctx: RpcContext<'_>, req: RemoveBondRequest, sink: UnarySink<Empty>) {
        let raw_address = RawAddress::from_string(req.address).unwrap();
        self.btif_intf.lock().unwrap().remove_bond(&raw_address);
        ctx.spawn(async move {
            sink.success(Empty::default()).await.unwrap();
        })
    }

    fn generate_local_oob_data(
        &mut self,
        ctx: RpcContext<'_>,
        req: GenerateOobDataRequest,
        sink: UnarySink<Empty>,
    ) {
        self.btif_intf.lock().unwrap().generate_local_oob_data(req.transport);
        ctx.spawn(async move {
            sink.success(Empty::default()).await.unwrap();
        })
    }

    fn create_bond_oob(
        &mut self,
        ctx: RpcContext<'_>,
        req: OobDataBondRequest,
        sink: UnarySink<Empty>,
    ) {
        //self.btif_intf.lock().unwrap().generate_local_oob_data(req.transport);
        println!("derp: {:?}", req.transport);
        println!("derp: {:?}", req.address);
        println!("derp: {:?}", req.confirmation);
        println!("derp: {:?}", req.randomizer);
        //        let vector_address =
        let address_bytes = decode_hex(&req.address);
        // Address comes in Little Endian format
        let address: RawAddress = RawAddress {
            address: [
                address_bytes[5],
                address_bytes[4],
                address_bytes[3],
                address_bytes[2],
                address_bytes[1],
                address_bytes[0],
            ],
        };
        let p192_data = OobData {
            is_valid: true,
            address: [0; 7],
            c: [0; 16],
            r: [0; 16],
            device_name: [0; 256],
            oob_data_length: [0; 2],
            class_of_device: [0; 2],
            le_device_role: 0,
            sm_tk: [0; 16],
            le_flags: 0,
            le_appearance: [0; 2],
        };
        //let transport: i32 = req.transport.parse::<i32>().expect("Failed to parse transport!");
        let transport = BtTransport::from(2);
        self.btif_intf.lock().unwrap().create_bond_oob(&address, transport, p192_data, p192_data);
        ctx.spawn(async move {
            sink.success(Empty::default()).await.unwrap();
        })
    }
}
