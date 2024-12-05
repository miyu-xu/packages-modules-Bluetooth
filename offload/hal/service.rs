// Copyright 2024, The Android Open Source Project
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

use crate::{
    ffi::{CInterface, CStatus, Callbacks, DataCallbacks, Ffi},
    proxy::{Module, Proxy},
};
use android_hardware_bluetooth::aidl::android::hardware::bluetooth::{
    IBluetoothHci::IBluetoothHci, IBluetoothHciCallbacks::IBluetoothHciCallbacks, Status::Status,
};
use binder::{DeathRecipient, ExceptionCode, Interface, Result as BinderResult, Strong};
use std::sync::{Arc, RwLock};

/// Service Implementation of AIDL interface `hardware/interface/bluetoot/aidl`,
/// including a proxy interface usable by third party modules.
pub struct HciHalProxy {
    ffi: Arc<Ffi<FfiCallbacks>>,
    state: Arc<RwLock<State>>,
}

struct FfiCallbacks {
    callbacks: Strong<dyn IBluetoothHciCallbacks>,
    proxy: Arc<Proxy<FfiCallbacks, ProxyCallbacks>>,
    state: Arc<RwLock<State>>,
}

struct ProxyCallbacks {
    callbacks: Strong<dyn IBluetoothHciCallbacks>,
}

#[derive(Default)]
enum State {
    #[default]
    Closed,
    Opening {
        ffi: Arc<Ffi<FfiCallbacks>>,
        proxy: Arc<Proxy<FfiCallbacks, ProxyCallbacks>>,
    },
    Opened {
        proxy: Arc<Proxy<FfiCallbacks, ProxyCallbacks>>,
        _death_recipient: DeathRecipient,
    },
}

impl Interface for HciHalProxy {}

impl HciHalProxy {
    /// Create the HAL Proxy interface binded to the Bluetooth HCI HAL interface.
    pub fn new(cintf: CInterface) -> Self {
        Self { ffi: Arc::new(Ffi::new(cintf)), state: Default::default() }
    }
}

impl IBluetoothHci for HciHalProxy {
    fn initialize(&self, callbacks: &Strong<dyn IBluetoothHciCallbacks>) -> BinderResult<()> {
        let (ffi, callbacks) = {
            let mut state = self.state.write().unwrap();

            if !matches!(*state, State::Closed) {
                let _ = callbacks.initializationComplete(Status::ALREADY_INITIALIZED);
                return Ok(());
            }

            let proxy =
                Arc::new(Proxy::new(self.ffi.clone(), ProxyCallbacks::new(callbacks.clone())));
            let callbacks = FfiCallbacks::new(callbacks.clone(), proxy.clone(), self.state.clone());

            *state = State::Opening { ffi: self.ffi.clone(), proxy: proxy.clone() };
            (self.ffi.clone(), callbacks)
        };

        ffi.initialize(callbacks);
        Ok(())
    }

    fn close(&self) -> BinderResult<()> {
        *self.state.write().unwrap() = State::Closed;
        self.ffi.close();
        Ok(())
    }

    fn sendHciCommand(&self, data: &[u8]) -> BinderResult<()> {
        let State::Opened { ref proxy, .. } = *self.state.read().unwrap() else {
            return Err(ExceptionCode::ILLEGAL_STATE.into());
        };

        proxy.out_cmd(data);
        Ok(())
    }

    fn sendAclData(&self, data: &[u8]) -> BinderResult<()> {
        let State::Opened { ref proxy, .. } = *self.state.read().unwrap() else {
            return Err(ExceptionCode::ILLEGAL_STATE.into());
        };

        proxy.out_acl(data);
        Ok(())
    }

    fn sendScoData(&self, data: &[u8]) -> BinderResult<()> {
        let State::Opened { ref proxy, .. } = *self.state.read().unwrap() else {
            return Err(ExceptionCode::ILLEGAL_STATE.into());
        };

        proxy.out_sco(data);
        Ok(())
    }

    fn sendIsoData(&self, data: &[u8]) -> BinderResult<()> {
        let State::Opened { ref proxy, .. } = *self.state.read().unwrap() else {
            return Err(ExceptionCode::ILLEGAL_STATE.into());
        };

        proxy.out_iso(data);
        Ok(())
    }
}

impl FfiCallbacks {
    fn new(
        callbacks: Strong<dyn IBluetoothHciCallbacks>,
        proxy: Arc<Proxy<FfiCallbacks, ProxyCallbacks>>,
        state: Arc<RwLock<State>>,
    ) -> Self {
        Self { callbacks, proxy, state }
    }
}

impl Callbacks for FfiCallbacks {
    fn initialization_complete(&self, status: CStatus) {
        let mut state = self.state.write().unwrap();
        match status {
            CStatus::Success => {
                let State::Opening { ref ffi, ref proxy } = *state else {
                    panic!("Initialization completed called in bad state");
                };

                *state = State::Opened {
                    proxy: proxy.clone(),
                    _death_recipient: {
                        let (ffi, state) = (ffi.clone(), self.state.clone());
                        DeathRecipient::new(move || {
                            log::info!("Bluetooth stack has died");
                            *state.write().unwrap() = State::Closed;
                            ffi.close();
                        })
                    },
                };
            }

            CStatus::AlreadyInitialized => panic!("Initialization completed called in bad state"),
            _ => *state = State::Closed,
        };

        if let Err(e) = self.callbacks.initializationComplete(status.into()) {
            log::error!("Cannot call-back client: {:?}", e);
        }
    }
}

impl DataCallbacks for FfiCallbacks {
    fn event_received(&self, data: &[u8]) {
        self.proxy.in_evt(data);
    }

    fn acl_received(&self, data: &[u8]) {
        self.proxy.in_acl(data);
    }

    fn sco_received(&self, data: &[u8]) {
        self.proxy.in_sco(data);
    }

    fn iso_received(&self, data: &[u8]) {
        self.proxy.in_iso(data);
    }
}

impl ProxyCallbacks {
    fn new(callbacks: Strong<dyn IBluetoothHciCallbacks>) -> Self {
        Self { callbacks }
    }
}

impl DataCallbacks for ProxyCallbacks {
    fn event_received(&self, data: &[u8]) {
        if let Err(e) = self.callbacks.hciEventReceived(data) {
            log::error!("Cannot send event to client: {:?}", e);
        }
    }

    fn acl_received(&self, data: &[u8]) {
        if let Err(e) = self.callbacks.aclDataReceived(data) {
            log::error!("Cannot send ACL to client: {:?}", e);
        }
    }

    fn sco_received(&self, data: &[u8]) {
        if let Err(e) = self.callbacks.scoDataReceived(data) {
            log::error!("Cannot send SCO to client: {:?}", e);
        }
    }

    fn iso_received(&self, data: &[u8]) {
        if let Err(e) = self.callbacks.isoDataReceived(data) {
            log::error!("Cannot send ISO to client: {:?}", e);
        }
    }
}

impl From<CStatus> for Status {
    fn from(value: CStatus) -> Self {
        match value {
            CStatus::Success => Status::SUCCESS,
            CStatus::AlreadyInitialized => Status::ALREADY_INITIALIZED,
            CStatus::UnableToOpenInterface => Status::UNABLE_TO_OPEN_INTERFACE,
            CStatus::HardwareInitializationError => Status::HARDWARE_INITIALIZATION_ERROR,
            CStatus::Unknown => Status::UNKNOWN,
        }
    }
}
