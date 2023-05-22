//! FFI + JNI for Metrics

use std::{ffi::c_void, str::FromStr, time::Duration};

use jni::{
    objects::{JObject, JString},
    sys::{jboolean, jint},
    JNIEnv, NativeMethod,
};

use crate::{core::address::RawAddress, do_in_rust_thread};

use super::atoms::AppUid;

#[no_mangle]
extern "C" fn register_metrics_native_methods(env: JNIEnv) {
    env.register_native_methods(
        "com/android/bluetooth/metrics/MetricsNativeInterface",
        &[
            NativeMethod {
                name: "logRfcommClientConnection".into(),
                sig: "".into(),
                fn_ptr: log_rfcomm_client_connection as *mut c_void,
            },
            NativeMethod {
                name: "logRfcommClientDisconnection".into(),
                sig: "".into(),
                fn_ptr: log_rfcomm_client_disconnection as *mut c_void,
            },
        ],
    )
    .expect("failed to register JNI callbacks for metrics");
}

fn str_to_addr(env: JNIEnv, device: JString) -> RawAddress {
    RawAddress::from_str(
        env.get_string(device)
            .expect("string object should be valid")
            .to_str()
            .expect("string object should be valid utf-8"),
    )
    .expect("address should be a well-formed MAC addr string")
}

fn log_rfcomm_client_connection(
    env: JNIEnv,
    _object: JObject,
    device: JString,
    is_secured: jboolean,
    success: jboolean,
    socket_connection_time_millis: jint,
    app_uid: jint,
) {
    let peer_addr = str_to_addr(env, device);
    do_in_rust_thread(move |modules| {
        modules.metrics.rfcomm.log_rfcomm_client_connection(
            peer_addr,
            is_secured != 0,
            success != 0,
            Duration::from_millis(socket_connection_time_millis as u64),
            AppUid(app_uid as u32),
        );
    })
}

fn log_rfcomm_client_disconnection() {}
