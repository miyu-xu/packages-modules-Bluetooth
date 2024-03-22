use std::os::raw::c_void;

#[no_mangle]
pub extern "C" fn load_initial_crust_jni(_jni_env_raw: *mut c_void) -> bool {
    // false
    panic!("failing");
    // true
}
