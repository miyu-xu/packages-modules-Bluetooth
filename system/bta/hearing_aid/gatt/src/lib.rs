//! Bluetooth GATT Client

use std::os::fd::RawFd;

mod att;
mod client;
mod database;
mod executor;
mod gatt;
mod uuid;

use std::slice;
use std::sync::OnceLock;

use futures::channel::mpsc;
use futures::{Stream, StreamExt};
use pdl_runtime::Packet;

type Id = u32;

static UPPER_PACKETS: OnceLock<mpsc::UnboundedSender<(Id, att::packets::Pdu)>> = OnceLock::new();
static LOWER_PACKETS: OnceLock<mpsc::UnboundedSender<(Id, att::packets::Pdu)>> = OnceLock::new();

#[no_mangle]
pub extern "C" fn gatt_rs_upper_send_packet(id: Id, data: *const u8, len: usize) {
    let data = unsafe { slice::from_raw_parts(data, len) };

    let packet = att::packets::Pdu::decode_full(data).unwrap();

    let channel = UPPER_PACKETS.get().expect("gatt_rs_executor_setup not called");

    channel.unbounded_send((id, packet)).unwrap();
}

#[no_mangle]
pub extern "C" fn gatt_rs_lower_send_packet(id: Id, data: *const u8, len: usize) {
    let data = unsafe { slice::from_raw_parts(data, len) };

    let packet = att::packets::Pdu::decode_full(data).unwrap();

    let channel = LOWER_PACKETS.get().expect("gatt_rs_executor_setup not called");

    channel.unbounded_send((id, packet)).unwrap();
}

use std::future::Future;
use std::pin::{pin, Pin};
use std::task::{Context, Poll, Waker};

use futures::stream::FuturesUnordered;

struct Connection {
    id: Id,
    task: Pin<Box<dyn Future<Output = ()>>>,
    upper_sender: mpsc::UnboundedSender<att::packets::Pdu>,
    lower_sender: mpsc::UnboundedSender<att::packets::Pdu>,
}

async fn handle_read_by_group_type_req(
    first: att::packets::ReadByGroupTypeReq,
    packets: &Pin<&mut futures::stream::Peekable<impl Stream<Item = att::packets::Pdu>>>,
) {
    let client: gatt::Client = todo!();

    // According to Vol 3, Part G, 4.13 GATT PROCEDURE MAPPING TO ATT PROTOCOL OPCODES
    // ATT_READ_BY_GROUP_TYPE_REQ is only used for the "Discover All Primary Services"
    // GATT procedure.
    assert_eq!(first.starting_handle, att::AttributeHandle::MIN);
    assert_eq!(first.ending_handle, att::AttributeHandle::MAX);
    assert_eq!(first.attribute_group_type, gatt::PRIMARY_SERVICE.into());

    let services = std::pin::pin!(client.discover_all_primary_services());

    let mut last_handle = att::AttributeHandle::MIN;

    while let Some(packet) = packets.peek().await {
        let Ok(packet @ att::ReadByGroupTypeReq { .. }) = packet.try_into() else {
            // If the packet is not a ATT_READ_BY_GROUP_TYPE_REQ it means
            // that we changed procedure.
            break;
        };

        assert_eq!(packet.attribute_group_type, gatt::PRIMARY_SERVICE.into());
        assert_eq!(packet.ending_handle, att::AttributeHandle::MAX);
    }
}

impl Connection {
    fn new(
        id: Id,
        send_to_upper: impl Fn(Id, &[u8]),
        send_to_lower: impl Fn(Id, &[u8]) + 'static,
    ) -> Self {
        let (upper_sender, upper_receiver) = mpsc::unbounded();
        let (lower_sender, mut lower_receiver) = mpsc::unbounded();

        let task = async move {
            let mut upper_receiver = pin!(upper_receiver.peekable());
            loop {
                futures::select_biased! {
                    msg = upper_receiver.next() => {
                        let upper_packet: att::packets::Pdu  = msg.unwrap();

                        use att::packets::PduChild::*;

                        match upper_packet.specialize().unwrap() {
                            ReadByGroupTypeReq(req) => handle_read_by_group_type_req(req, &upper_receiver).await,
                            _ => {
                                // Packet not intercepted, just passthrough
                                send_to_lower(id, &upper_packet.encode_to_vec().unwrap())
                            },
                        }
                    },
                    msg = lower_receiver.next() => {
                        let lower_packet = msg.unwrap();
                    }
                }
            }
        };

        Connection { id, task: Box::pin(task), upper_sender, lower_sender }
    }

    fn send_upper_packet(&mut self, pdu: att::packets::Pdu) {
        self.upper_sender.unbounded_send(pdu).unwrap();
    }

    fn send_lower_packet(&mut self, pdu: att::packets::Pdu) {
        self.lower_sender.unbounded_send(pdu).unwrap();
    }
}

impl Future for Connection {
    type Output = ();

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        self.task.as_mut().poll(cx)
    }
}

async fn task(
    mut upper: mpsc::UnboundedReceiver<(Id, att::packets::Pdu)>,
    mut lower: mpsc::UnboundedReceiver<(Id, att::packets::Pdu)>,
    send_to_upper: impl Fn(Id, &[u8]) + Copy + 'static,
    send_to_lower: impl Fn(Id, &[u8]) + Copy + 'static,
) {
    let mut futures: FuturesUnordered<Connection> = FuturesUnordered::new();

    loop {
        futures::select_biased! {
            _ = futures.next() => {}
            msg = upper.next() => {
                let (id, upper_packet) = msg.unwrap();

                let connection = futures.iter_mut().find(|connection| connection.id == id);

                if let Some(connection) = connection {
                    connection.send_upper_packet(upper_packet);
                } else {
                    let mut connection = Connection::new(id, send_to_upper, send_to_lower);
                    connection.send_upper_packet(upper_packet);
                    futures.push(connection);
                }
            },
            msg = lower.next() => {
                let (id, lower_packet) = msg.unwrap();

                let connection = futures.iter_mut().find(|connection| connection.id == id);

                if let Some(connection) = connection {
                    connection.send_lower_packet(lower_packet);
                } else {
                    let mut connection = Connection::new(id, send_to_upper, send_to_lower);
                    connection.send_lower_packet(lower_packet);
                    futures.push(connection);
                }
            }
        }
    }
}

#[no_mangle]
pub extern "C" fn gatt_rs_executor_setup(
    send_packet_upper: extern "C" fn(id: Id, data: *const u8, len: usize),
    send_packet_lower: extern "C" fn(id: Id, data: *const u8, len: usize),
) -> RawFd {
    let (upper_sender, upper_receiver) = mpsc::unbounded();
    let (lower_sender, lower_receiver) = mpsc::unbounded();

    UPPER_PACKETS.set(upper_sender).unwrap();
    LOWER_PACKETS.set(lower_sender).unwrap();

    let task = task(
        upper_receiver,
        lower_receiver,
        move |id, packet| send_packet_upper(id, packet.as_ptr(), packet.len()),
        move |id, packet| send_packet_lower(id, packet.as_ptr(), packet.len()),
    );

    executor::setup(task).unwrap_or_else(|errno| {
        errno.set();
        -1
    })
}

#[no_mangle]
pub extern "C" fn gatt_rs_executor_poll() {
    let is_ready = executor::poll().is_ready();

    if is_ready {
        panic!("The task exited");
    }
}
