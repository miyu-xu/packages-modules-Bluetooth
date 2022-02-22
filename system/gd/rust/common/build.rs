fn main() {
    cxx_build::bridge("src/parameter_provider.rs")
        .file("src/fake_bt_keystore.cc")
        .compile("bt_common")
}
