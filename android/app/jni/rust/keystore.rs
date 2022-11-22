// Copyright 2022, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! JNI methods wrapped by BluetoothKeystoreNativeInterface

use cxx::type_id;
use jni::objects::JClass;
use jni::JNIEnv;
use jni::{objects::JObject, JavaVM};
use log::{info, warn};
use paste::paste;
use std::cell::RefCell;
use std::collections::HashMap;
use std::sync::mpsc;
use std::thread::{self, JoinHandle};
use std::{pin::Pin, sync::Mutex};

const ENCRYPTED_STRING: &str = "encrypted";

#[allow(dead_code)]
struct GlobalModuleRegistry {
    handle: JoinHandle<()>,
    task_tx: MainThreadTx,
}

/// The ModuleViews lets us access all publicly accessible Rust modules from Java / C++ while the stack is
/// running. If a module should not be exposed outside of Rust GD, there is no need to include it here.
struct ModuleViews<'a, 'b> {
    pub keystore_module: &'b KeystoreModule<'a>,
}

struct StorageModule {
    inner: RefCell<Pin<&'static mut ffi::StorageModule>>,
}

impl StorageModule {
    unsafe fn new() -> Self {
        // TODO: get this checked by Rust experts! DO NOT SUBMIT
        Self { inner: RefCell::new(Pin::static_mut(&mut *ffi::GetStorage())) }
    }

    fn get_bonded_devices(&self) -> cxx::UniquePtr<cxx::CxxVector<ffi::Device>> {
        ffi::GetBondedDevices(&self.inner.borrow_mut())
    }

    fn modify_on_heap(&self) -> cxx::UniquePtr<ffi::Mutation> {
        ffi::ModifyOnHeap(self.inner.borrow_mut().as_mut())
    }

    fn provide_keystore_interface(
        &self,
        interface: cxx::UniquePtr<ffi::BluetoothKeystoreInterface>,
    ) {
        self.inner.borrow_mut().as_mut().ProvideKeystoreInterface(interface);
    }
}

impl GlobalModuleRegistry {
    /// Handles bringup of all Rust modules. This occurs after GD C++ modules have started, but before the legacy stack
    /// has initialized.
    pub fn new(vm: JavaVM) -> Self {
        let (tx, rx) = mpsc::channel::<BoxedMainThreadCallback>();
        let handle = thread::spawn(move || {
            // GD modules should be available at this point, load any that are needed

            // To avoid having multiple mutable references to interior modules, we wrap them in a Rust shim that owns the single mutable reference
            // see https://users.rust-lang.org/t/single-mutable-reference-rule-and-ffi/50546/6
            let storage_module = unsafe { StorageModule::new() };

            let core_jni_module = CoreJniModule::new(&vm);
            let mut keystore_jni_module = KeystoreJniModule::new(&core_jni_module);
            let keystore_module = KeystoreModule::new(&mut keystore_jni_module, &storage_module);

            let modules = ModuleViews { keystore_module: &keystore_module };

            while let Ok(f) = rx.recv() {
                f(&modules);
            }
            warn!("JNI thread queue has stopped, shutting down executor thread")
        });
        Self { handle, task_tx: tx }
    }
}

type BoxedMainThreadCallback = Box<dyn FnOnce(&ModuleViews) + Send>;
type MainThreadTx = mpsc::Sender<BoxedMainThreadCallback>;

static GLOBAL_MODULE_REGISTRY: Mutex<Option<GlobalModuleRegistry>> = Mutex::new(None);

thread_local! {
    // this will be lazily initialized on first use from each client thread
    static MAIN_THREAD_TX: MainThreadTx = GLOBAL_MODULE_REGISTRY.lock().unwrap().as_ref().expect("stack not initialized").task_tx.clone();
}

fn do_on_rust_thread(
    f: impl FnOnce(&ModuleViews) + Send + 'static,
) -> Result<(), mpsc::SendError<BoxedMainThreadCallback>> {
    MAIN_THREAD_TX.with(|tx| tx.send(Box::new(f)))
}

/// This class manages all JNI callbacks from Rust modules
///
/// On startup, it registers our thread permanently with the JVM for JNI. Then if a module
/// wishes to call into Java, it can obtain a reference to the JNIEnv from this module.
struct CoreJniModule<'a> {
    env: JNIEnv<'a>,
}

impl<'a> CoreJniModule<'a> {
    fn new(vm: &'a JavaVM) -> Self {
        Self { env: vm.attach_current_thread_permanently().expect("failed to attach JNI thread") }
    }
}

struct KeystoreJniModule<'a> {
    core_jni_module: &'a CoreJniModule<'a>,
    class: Option<JClass<'a>>,
}

impl<'a> KeystoreJniModule<'a> {
    pub fn new(core_jni_module: &'a CoreJniModule) -> Self {
        Self { core_jni_module, class: None }
    }

    pub fn set_encrypt_key_or_remove_key_callback(&self, prefix: &str, decrypted: &str) {
        let env = self.core_jni_module.env;
        let prefix = env.new_string(prefix).expect("string conversion failed");
        let decrypted = env.new_string(decrypted).expect("string conversion failed");
        env.call_static_method(
            self.class.expect("KeystoreClass not initialized before callback invoked"),
            "setEncryptKeyOrRemoveKeyCallback",
            "(Ljava/lang/String;Ljava/lang/String;)V",
            &[prefix.into(), decrypted.into()],
        )
        .expect("failed to invoke set_encrypt_key_or_remove_key_callback callback");
    }

    pub fn get_key(&self, prefix: &str) -> String {
        let env = self.core_jni_module.env;
        let prefix = env.new_string(prefix).expect("string conversion failed");
        let ret = env
            .call_static_method(
                self.class.expect("KeystoreClass not initialized before callback invoked"),
                "setEncryptKeyOrRemoveKeyCallback",
                "(Ljava/lang/String;)Ljava/lang/String;",
                &[prefix.into()],
            )
            .expect("failed to invoke get_key callback");
        env.get_string(ret.l().expect("didn't get an object").into())
            .expect("failed to parse string")
            .into()
    }
}

struct KeystoreModule<'a> {
    jni_module: &'a mut KeystoreJniModule<'a>,
    storage_module: &'a StorageModule,
    key_cache: RefCell<HashMap<String, String>>,
}

macro_rules! fix_key {
    ($key : ident, $literal_key : literal, $device : ident, $specialized_device : ident, $mutation : ident, $jni : expr) => {
        paste! {
        if let Some(data) = ffi:: [<Get $key>](&$specialized_device).as_ref() {
            let key = ffi::GetAddress(&$device).to_string() + "-" + $literal_key;
            let is_encrypted = data == ENCRYPTED_STRING;
            if ffi::IsCommonCriteriaMode() {
                if !is_encrypted {
                    $jni.store_key(
                        &key,
                        data.to_str().expect("corrupt key cannot be converted to UTF-8"),
                    );
                    ffi::Add(
                        $mutation.as_mut(),
                        ffi::[<Set $key>]($specialized_device.as_mut(), ENCRYPTED_STRING),
                    );
                }
                todo!()
            } else {
                if is_encrypted {
                    ffi::Add(
                        $mutation.as_mut(),
                        ffi::[<Set $key>](
                            $specialized_device.as_mut(),
                            &$jni.get_key(&key),
                        ),
                    );
                }
            }}
        }
    };
}

impl<'a> KeystoreModule<'a> {
    pub fn new(
        jni_module: &'a mut KeystoreJniModule<'a>,
        storage_module: &'a StorageModule,
    ) -> Self {
        storage_module.provide_keystore_interface(ffi::get_interface(Box::new(
            KeystoreInterfaceImpl::new(MAIN_THREAD_TX.with(|tx| tx.clone())),
        )));
        Self { jni_module, storage_module, key_cache: RefCell::new(HashMap::new()) }
    }

    fn store_key(&self, prefix: &str, decrypted: &str) {
        self.key_cache.borrow_mut().insert(prefix.to_owned(), decrypted.to_owned());
        self.jni_module.set_encrypt_key_or_remove_key_callback(prefix, decrypted);
    }

    fn get_key(&self, prefix: &str) -> String {
        if let Some(decrypted) = self.key_cache.borrow().get(prefix) {
            decrypted.clone()
        } else {
            let decrypted = self.jni_module.get_key(prefix);
            self.key_cache.borrow_mut().insert(prefix.to_owned(), decrypted.to_owned());
            decrypted
        }
    }

    /// If the keystore is enabled, then we should clear any keys
    /// present in the storage layer (since we will proxy all reads/writes). Conversely, if it is disabled, we should re-populate the storage layer with
    /// keys pulled from the keystore.
    pub fn fix_storage_layer(&self) {
        let mut mutation_ptr = self.storage_module.modify_on_heap();
        let mut mutation = mutation_ptr.pin_mut();

        let mut devices = self.storage_module.get_bonded_devices();

        for mut device in devices.pin_mut() {
            match ffi::GetDeviceType(&device) {
                ffi::DeviceType::BR_EDR => {
                    self.fix_classic_keys(mutation.as_mut(), device.as_mut());
                    self.fix_le_keys(mutation.as_mut(), device.as_mut());
                }
                ffi::DeviceType::LE => {
                    self.fix_le_keys(mutation.as_mut(), device.as_mut());
                }
                ffi::DeviceType::DUAL => {
                    self.fix_classic_keys(mutation.as_mut(), device.as_mut());
                }
                _ => {
                    warn!("Unknown DeviceType for device XYZ, skipping")
                }
            }
        }
    }

    fn fix_le_keys(
        &self,
        mut mutation: Pin<&mut ffi::Mutation>,
        mut device: Pin<&mut ffi::Device>,
    ) {
        let mut le_device_ptr = ffi::Le(device.as_mut());
        let mut le_device = le_device_ptr.pin_mut();

        fix_key!(LocalEncryptionKeys, "LE_KEY_LENC", device, le_device, mutation, self);
        fix_key!(PeerEncryptionKeys, "LE_KEY_PENC", device, le_device, mutation, self);
        fix_key!(LocalId, "LE_KEY_LID", device, le_device, mutation, self);
        fix_key!(PeerId, "LE_KEY_PID", device, le_device, mutation, self);
        fix_key!(LocalSignatureResolvingKeys, "LE_KEY_LCSRK", device, le_device, mutation, self);
        fix_key!(PeerSignatureResolvingKeys, "LE_KEY_PCSRK", device, le_device, mutation, self);
    }

    fn fix_classic_keys(
        &self,
        mut mutation: Pin<&mut ffi::Mutation>,
        mut device: Pin<&mut ffi::Device>,
    ) {
        let mut classic_device_ptr = ffi::Classic(device.as_mut());
        let mut classic_device = classic_device_ptr.pin_mut();

        fix_key!(RawLinkKey, "LinkKey", device, classic_device, mutation, self);
    }
}

#[no_mangle]
/// Callback indicating when the KeyStore class is initialized in Java
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_classInit(
    env: JNIEnv,
    _obj: JObject,
) {
    let vm = env.get_java_vm().expect("failed to get JVM");

    let prev_registry =
        GLOBAL_MODULE_REGISTRY.lock().unwrap().replace(GlobalModuleRegistry::new(vm));

    // registration should ony happen once
    assert!(prev_registry.is_none());
}

#[no_mangle]
/// Callback indicating when the KeyStore instance is initialized in Java
/// This means we can fix all the storage entries to make them consistent with our encryption mode
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_init(
    _env: JNIEnv,
    _obj: JObject,
) {
    do_on_rust_thread(|modules| modules.keystore_module.fix_storage_layer())
        .expect("stack is shutting down while initializing keystore");
}

#[no_mangle]
/// Logs some garbage
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_testLoggingInRust(
    _env: JNIEnv,
    _obj: JObject,
) {
    android_logger::init_once(
        android_logger::Config::default().with_tag("bt_jni").with_min_level(log::Level::Debug),
    );

    info!("Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_testLoggingInRust: RAHULLOG");
}

/// This struct implements BluetoothKeystoreInterface and can be passed (via a shim) to the Storage Module in C++
pub struct KeystoreInterfaceImpl {
    tx: MainThreadTx,
}

impl KeystoreInterfaceImpl {
    fn new(tx: MainThreadTx) -> Self {
        Self { tx }
    }
    /// Store the (cleartext) key in the keystore, indexed by prefix
    pub fn store_key(&self, key: &str, value: &str) {
        let key = key.to_string();
        let value = value.to_string();
        self.tx
            .send(Box::new(move |modules| modules.keystore_module.store_key(&key, &value)))
            .unwrap();
    }

    /// Retrieve a key from the keystore by prefix
    pub fn get_key(&self, key: &str) -> String {
        let key = key.to_string();
        let (tx, rx) = mpsc::channel();
        self.tx
            .send(Box::new(move |modules| tx.send(modules.keystore_module.get_key(&key)).unwrap()))
            .unwrap();
        rx.recv().unwrap()
    }
}

/// A 6-byte MAC address corresponding to a Bluetooth device
///
/// Try to avoid using in favor of an Address tagged with the AddressType
#[repr(C)]
pub struct RawAddress {
    address: [u8; 6],
}

impl ToString for RawAddress {
    fn to_string(&self) -> String {
        todo!()
    }
}

unsafe impl cxx::ExternType for RawAddress {
    type Id = type_id!("bluetooth::hci::rust_shim::RawAddress");
    type Kind = cxx::kind::Trivial;
}

#[allow(dead_code)]
#[allow(unused_must_use)]
#[cxx::bridge]
mod ffi {
    #[namespace = "bluetooth::shim"]
    unsafe extern "C++" {
        include!("main/shim/entry.h");

        fn GetStorage() -> *mut StorageModule;
    }

    #[namespace = "bluetooth::hci"]
    #[repr(i32)]
    enum DeviceType {
        UNKNOWN,
        BR_EDR,
        LE,
        DUAL,
    }

    #[namespace = "bluetooth::hci"]
    unsafe extern "C++" {
        type DeviceType;
    }

    #[namespace = "bluetooth::hci::rust_shim"]
    unsafe extern "C++" {
        type RawAddress = super::RawAddress;
    }

    #[namespace = "bluetooth::storage"]
    unsafe extern "C++" {
        include!("gd/storage/storage_module.h");
        include!("gd/storage/mutation.h");
        include!("gd/storage/mutation_entry.h");
        include!("gd/storage/keystore_interface.h");

        type StorageModule;
        type Mutation;
        type MutationEntry;
        type Device;
        type ClassicDevice;
        type LeDevice;
        type BluetoothKeystoreInterface;

        fn Commit(self: Pin<&mut Mutation>);
        fn ProvideKeystoreInterface(
            self: Pin<&mut StorageModule>,
            interface: UniquePtr<BluetoothKeystoreInterface>,
        );
    }

    #[namespace = "bluetooth::storage::rust_shim"]
    #[repr(i32)]
    enum PropertyType {
        NORMAL,
        MEMORY_ONLY,
    }

    #[namespace = "bluetooth::storage::rust_shim"]
    unsafe extern "C++" {
        include!("storage_shim.h");
        type PropertyType;

        fn GetBondedDevices(storage: &StorageModule) -> UniquePtr<CxxVector<Device>>;

        fn GetDeviceType(device: &Device) -> DeviceType;

        fn GetAddress(device: &Device) -> RawAddress;

        fn Classic(device: Pin<&mut Device>) -> UniquePtr<ClassicDevice>;

        fn GetRawLinkKey(device: &ClassicDevice) -> UniquePtr<CxxString>;
        fn SetRawLinkKey(device: Pin<&mut ClassicDevice>, value: &str) -> UniquePtr<MutationEntry>;

        fn Le(device: Pin<&mut Device>) -> UniquePtr<LeDevice>;

        fn GetLocalId(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerId(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetLocalEncryptionKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerEncryptionKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetLocalSignatureResolvingKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerSignatureResolvingKeys(device: &LeDevice) -> UniquePtr<CxxString>;

        #[must_use]
        fn SetLocalId(device: Pin<&mut LeDevice>, value: &str) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerId(device: Pin<&mut LeDevice>, value: &str) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetLocalEncryptionKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerEncryptionKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetLocalSignatureResolvingKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerSignatureResolvingKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;

        #[must_use]
        fn ModifyOnHeap(module: Pin<&mut StorageModule>) -> UniquePtr<Mutation>;

        fn Add(mutation: Pin<&mut Mutation>, entry: UniquePtr<MutationEntry>);

        #[must_use]
        fn Set(
            property_type: PropertyType,
            section_param: &str,
            property_param: &str,
            value_param: &str,
        ) -> UniquePtr<MutationEntry>;
    }

    #[namespace = "bluetooth::os::parameter_provider"]
    unsafe extern "C++" {
        include!("parameter_provider_shim.h");
        fn IsCommonCriteriaMode() -> bool;
    }

    #[namespace = "bluetooth::keystore"]
    unsafe extern "C++" {
        include!("keystore_shim.h");

        #[cxx_name = "GetInterface"]
        #[must_use]
        fn get_interface(ptr: Box<KeystoreInterfaceImpl>) -> UniquePtr<BluetoothKeystoreInterface>;
    }

    #[namespace = "bluetooth::keystore"]
    extern "Rust" {
        type KeystoreInterfaceImpl;

        fn store_key(self: &KeystoreInterfaceImpl, key: &str, value: &str);
        fn get_key(self: &KeystoreInterfaceImpl, key: &str) -> String;
    }
}
