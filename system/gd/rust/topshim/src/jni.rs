//! Expose topshim to Java on Android systems.

use jni;
use log;
use std::ffi;

#[no_mangle]
pub extern "C" fn load_initial_rust_jni() {
}

pub extern "system" fn testFoo(env: jni::JNIEnv, class: jni::objects::JObject) {
    log::warn!("Ran testFoo");
}

pub fn make_native_method(name: &str, sig: &str, fn_ptr: *mut ()) -> jni::NativeMethod {
    return jni::NativeMethod {
        name: name.into(),
        sig: sig.into(),
        fn_ptr: fn_ptr as *mut std::ffi::c_void,
    };
}

/// Register topshim JNI interfaces.
#[no_mangle]
pub extern "system" fn register_rust_jni(env: jni::JNIEnv) -> i32 {
    let methods = vec![jni::NativeMethod {
        name: "testFoo".into(),
        sig: "()V".into(),
        fn_ptr: testFoo as *mut std::ffi::c_void,
    }];

    // Find class to register with.
    let found_class = env.find_class("com/android/bluetooth/btservice/AdapterNativeInterface");

    if let Ok(class) = found_class {
        // Register methods
        if let Err(e) = env.register_native_methods(class, methods.as_slice()) {
            log::error!("Failed to register native methods: {:?}", e);
        }
    }

    // register_gatt_methods(&env);

    // No-op for now
    return 0;
}
