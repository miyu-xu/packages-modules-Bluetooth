//! FFI + JNI for Metrics

use std::{ffi::c_void, str::FromStr};

use anyhow::{bail, Context, Result, Ok};
use jni::{
    objects::{JObject, JValue},
    sys::{jboolean, jint},
    JNIEnv, NativeMethod,
};

use crate::{core::address::RawAddress, do_in_rust_thread};

use super::{atoms::AppUid, rfcomm::AttemptId};
use log::error;

#[allow(dead_code, missing_docs)]
#[cxx::bridge]
mod inner {
    extern "C++" {
        include!("jni.h");

        type JNIEnv;
    }

    #[namespace = "android"]
    extern "Rust" {
        unsafe fn register_metrics_native_methods(env: *const JNIEnv);
    }
}

fn extract_jni_ptr(env: *const inner::JNIEnv) -> JNIEnv<'static> {
    // SAFETY: The construction of inner::JNIEnv from FFI
    // guarantees that we receive a pointer to a JNIEnv
    unsafe { JNIEnv::from_raw(std::mem::transmute(env)).unwrap() }
}

fn register_metrics_native_methods(env: *const inner::JNIEnv) {
    let env = extract_jni_ptr(env);

    env.register_native_methods(
        "com/android/bluetooth/btservice/AdapterService$AdapterServiceBinder",
        &[
            NativeMethod {
                name: "logRfcommConnectionAttemptStart".into(),
                sig: "(ILandroid/bluetooth/BluetoothDevice;ZLandroid/os/ParcelUuid;II)V".into(),
                fn_ptr: log_rfcomm_client_connection_attempt_start as *mut c_void,
            },
            NativeMethod {
                name: "logRfcommClientConnectionComplete".into(),
                sig: "(IZ)V".into(),
                fn_ptr: log_rfcomm_client_connection_complete as *mut c_void,
            },
            NativeMethod {
                name: "logRfcommClientDisconnection".into(),
                sig: "()V".into(),
                fn_ptr: log_rfcomm_client_disconnection as *mut c_void,
            },
        ],
    )
    .expect("failed to register JNI callbacks for metrics");
}

fn device_to_addr(env: JNIEnv, device: JObject) -> Result<RawAddress> {
    let address = env
        .call_method(device, "getAddress", "()Ljava/lang/String;", &[])
        .context("failed to call getAddress")?;
    let JValue::Object(address) = address else {
        bail!("getAddress returned a non-String");
    };
    RawAddress::from_str(
        env.get_string(address.into())
            .context("string object should be valid")?
            .to_str()
            .context("string object should be valid utf-8")?,
    )
    .context("address should be a well-formed MAC addr string")
}

#[allow(clippy::too_many_arguments)]
fn log_rfcomm_client_connection_attempt_start(
    env: JNIEnv,
    _object: JObject,
    attempt_id: jint,
    device: JObject,
    is_secured: jboolean,
    target_uuid: JObject,
    target_port: jint,
    app_uid: jint,
) {
    if let Err(err) = (|| {
        let peer_addr = device_to_addr(env, device).context("got invalid device")?;

        let target_uuid = if env.is_same_object(target_uuid, JObject::null()).context("failed to compare UUID with null")? {
            None
        } else {
            let target_uuid = env
                    .call_method(target_uuid, "toString", "()Ljava/lang/String;", &[])
                    .context("failed to call toString")?;
            let JValue::Object(target_uuid) = target_uuid else {
                bail!("getAddress returned a non-String");
            };
            Some(env.get_string(target_uuid.into())
                            .context("string object should be valid")?
                            .to_str()
                            .context("string object should be valid utf-8")?
                            .to_string())
        };

        do_in_rust_thread(move |modules: &mut crate::ModuleViews| {
            modules.metrics.rfcomm.log_rfcomm_client_connection_attempt_start(
                AttemptId(attempt_id as u32),
                peer_addr,
                is_secured != 0,
                target_uuid,
                target_port,
                AppUid(app_uid as u32),
            );
        });

        Ok(())
    })() {
        error!("{err:?}");
    }
}

fn log_rfcomm_client_connection_complete(
    _env: JNIEnv,
    _object: JObject,
    attempt_id: jint,
    success: jboolean,
) {
    do_in_rust_thread(move |modules| {
        modules
            .metrics
            .rfcomm
            .log_rfcomm_client_connection_complete(AttemptId(attempt_id as u32), success != 0);
    })
}

fn log_rfcomm_client_disconnection() {}
