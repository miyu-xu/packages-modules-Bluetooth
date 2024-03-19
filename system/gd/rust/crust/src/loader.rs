#[cxx::bridge(namespace = bluetooth::crust::loader)]
mod ffi {
    extern "Rust" {
        fn load_initial_crust_jni() -> bool;
    }
}

fn load_initial_crust_jni() -> bool {
    // For now, indicate everything is loaded up.
    true
}
