//! Starts the facade services that allow us to test the Bluetooth stack

#[macro_use]
extern crate clap;
use clap::{App, Arg};

#[macro_use]
extern crate lazy_static;

use bt_topshim::btif;

use std::convert::TryInto;

use futures::channel::mpsc;
use futures::future::FutureExt;
use futures::select;
use futures::stream::StreamExt;
use grpcio::*;
use log::debug;
use nix::fcntl;
use nix::sys::signal;
use nix::unistd;
use std::sync::{Arc, Mutex};
use tokio::runtime::{Handle, Runtime};
use tokio::sync::oneshot;

use std::env;
use std::os::unix::process::CommandExt;
use std::process::Command;

mod a2dp;
mod host;

// This is needed for linking, libbt_shim_bridge needs symbols defined by
// bt_shim, however bt_shim depends on rust crates (future, tokio) that
// we use too, if we build and link them separately we ends with duplicate
// symbols. To solve that we build bt_shim with bt_topshim_facade so the rust
// compiler share the transitive dependencies.
//
// The `::*` is here to circuvent the single_component_path_imports from
// clippy that is denied on the rust command line so we can't just allow it.
// This is fine for now since bt_shim doesn't export anything
#[allow(unused)]
use bt_shim::*;

fn main() {
    bt_common::init_logging();
    let rt = Runtime::new().unwrap();
    let reset = rt.block_on(async_main(rt.handle()));
    drop(rt);

    if reset {
        let mut args = env::args();
        let process = args.next().unwrap();

        println!("\n\nResetting stack ...\n\n");

        // Close all file descriptors
        let max_fd: i32 =
            unistd::sysconf(unistd::SysconfVar::OPEN_MAX).unwrap().unwrap().try_into().unwrap();

        for i in 3..max_fd {
            let _ = fcntl::fcntl(i, fcntl::F_SETFD(fcntl::FdFlag::FD_CLOEXEC));
        }

        // On success this will not return and replace the current
        // process. Warning: this will not run the destructors
        let err = Command::new(process).args(args).exec();
        panic!("Reset failed {:?}", err);
    }
}

async fn async_main(handle: &Handle) -> bool {
    let matches = App::new("bluetooth_topshim_facade")
        .about("The bluetooth topshim stack, with testing facades enabled and exposed via gRPC.")
        .arg(Arg::with_name("grpc-port").long("grpc-port").default_value("8899").takes_value(true))
        .arg(Arg::with_name("rootcanal-port").long("rootcanal-port").takes_value(true))
        .arg(Arg::with_name("btsnoop").long("btsnoop").takes_value(true))
        .arg(Arg::with_name("btsnooz").long("btsnooz").takes_value(true))
        .arg(Arg::with_name("btconfig").long("btconfig").takes_value(true))
        .get_matches();

    let grpc_port = value_t!(matches, "grpc-port", u16).unwrap();
    let _rootcanal_port = value_t!(matches, "rootcanal-port", u16).ok();

    let btif = btif::get_btinterface().unwrap();
    let (reset_tx, reset_rx) = oneshot::channel();

    let host = host::Host::initialize(btif, reset_tx, vec![]).await;
    let a2dp = a2dp::A2DP::initialize(handle.clone(), &host.btif.lock().unwrap());

    let env = Arc::new(Environment::new(2));
    let mut server = ServerBuilder::new(env)
        .register_service(host.into())
        .register_service(a2dp.into())
        .bind("0.0.0.0", grpc_port)
        .build()
        .unwrap();

    server.start();

    let mut sigint = install_sigint();

    let reset = select! {
        _ = reset_rx.fuse() => true,
        _ = sigint.next() => false,
    };

    server.shutdown().await.unwrap();
    reset
}

// TODO: remove as this is a temporary nix-based hack to catch SIGINT
fn install_sigint() -> mpsc::UnboundedReceiver<()> {
    let (tx, rx) = mpsc::unbounded();
    *SIGINT_TX.lock().unwrap() = Some(tx);

    let sig_action = signal::SigAction::new(
        signal::SigHandler::Handler(handle_sigint),
        signal::SaFlags::empty(),
        signal::SigSet::empty(),
    );
    unsafe {
        signal::sigaction(signal::SIGINT, &sig_action).unwrap();
    }

    rx
}

lazy_static! {
    static ref SIGINT_TX: Mutex<Option<mpsc::UnboundedSender<()>>> = Mutex::new(None);
}

extern "C" fn handle_sigint(_: i32) {
    let mut sigint_tx = SIGINT_TX.lock().unwrap();
    if let Some(tx) = &*sigint_tx {
        debug!("Stopping gRPC root server due to SIGINT");
        tx.unbounded_send(()).unwrap();
    }
    *sigint_tx = None;
}
