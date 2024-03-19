#[cxx::bridge(namespace = bluetooth::crust::loader)]
mod ffi {
    extern "Rust" {
        fn load_initial_crust_jni();
    }
}

fn load_initial_crust_jni() {
    println!("Loading up the Crust JNI");
}
