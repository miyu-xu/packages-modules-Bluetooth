pub trait KeystoreJniCallbacks {
    fn set_encrypt_key_or_remove_key_callback(&self, prefix: &str, decrypted: &str);
    fn get_key(&self, prefix: &str) -> String;
}
