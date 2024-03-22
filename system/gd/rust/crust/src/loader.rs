use jni::sys::JNIEnv;

#[no_mangle]
pub extern "C" fn load_initial_crust_jni(jni_env_raw: *mut JNIEnv) -> bool {
    if jni_env_raw.is_null() {
        return false;
    }
    // SAFETY: from_raw only performs a null check which we have
    // already done. Subsequent calls which would fail if the pointer
    // is otherwise invalid are Result wrapped.
    let env = unsafe { jni::JNIEnv::from_raw(jni_env_raw as *mut JNIEnv) };
    env.is_ok()
}
