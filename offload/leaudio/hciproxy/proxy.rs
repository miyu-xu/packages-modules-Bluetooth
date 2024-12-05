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
    arbiter::Arbiter,
    hal::Module,
    hci::{self, Command, Event, IsoData, ReturnParameters, ToBytes},
    service::{Service, StreamConfiguration},
};
use std::{
    collections::HashMap,
    sync::{Arc, Mutex},
};

const DATA_PATH_ID_SOFTWARE: u8 = 0x19; // TODO

/// LE Audio HCI-Proxy module
pub struct LeAudioModule {
    next_module: Arc<dyn Module>,
    state: Mutex<State>,
    service: Service,
}

#[derive(Default)]
struct State {
    cig_map: HashMap<u16, u8>,
    stream: HashMap<u16, Stream>,
    arbiter: Option<Arc<Arbiter>>,
}

#[derive(Debug)]
struct Stream {
    state: StreamState,
    iso_interval_us: u32,
    iso_type: IsoType,
}

#[derive(Debug, PartialEq)]
enum StreamState {
    Disabled,
    Enabling,
    Enabled,
}

#[derive(Debug)]
enum IsoType {
    Cis { c_to_p: CisInDirection, _p_to_c: CisInDirection },
    Bis { big_handle: u8 },
}

#[derive(Debug)]
struct CisInDirection {
    max_pdu: u16,
    burst_number: u8,
    flush_timeout: u8,
}

impl Stream {
    fn new_cis(evt: &hci::LeCisEstablished) -> Self {
        Self {
            state: StreamState::Disabled,
            iso_interval_us: (evt.iso_interval as u32) * 1250,
            iso_type: IsoType::Cis {
                c_to_p: CisInDirection {
                    max_pdu: evt.max_pdu_c_to_p,
                    burst_number: evt.bn_c_to_p,
                    flush_timeout: evt.ft_c_to_p,
                },
                _p_to_c: CisInDirection {
                    max_pdu: evt.max_pdu_p_to_c,
                    burst_number: evt.bn_p_to_c,
                    flush_timeout: evt.ft_p_to_c,
                },
            },
        }
    }

    fn new_bis(evt: &hci::LeCreateBigComplete) -> Self {
        Self {
            state: StreamState::Disabled,
            iso_interval_us: (evt.iso_interval as u32) * 1250,
            iso_type: IsoType::Bis { big_handle: evt.big_handle },
        }
    }
}

impl LeAudioModule {
    /// Create the HCI-Proxy module from the next module in the chain
    pub fn new(next_module: Arc<dyn Module>) -> Self {
        Self { next_module, state: Mutex::new(Default::default()), service: Service::new() }
    }
}

impl Module for LeAudioModule {
    fn next(&self) -> &dyn Module {
        &*self.next_module
    }

    fn out_cmd(&self, data: &[u8]) {
        match Command::from_bytes(data) {
            Command::LeSetupIsoDataPath(ref c) if c.data_path_id == DATA_PATH_ID_SOFTWARE => {
                assert_eq!(c.data_path_direction, 0);
                let mut state = self.state.lock().unwrap();
                let stream = state.stream.get_mut(&c.connection_handle).unwrap();
                stream.state = StreamState::Enabling;
            }

            _ => (),
        }

        self.next().out_cmd(data);
    }

    fn in_evt(&self, data: &[u8]) {
        match Event::from_bytes(data) {
            Event::CommandComplete(ref e) => match e.return_parameters {
                ReturnParameters::Reset(ref ret) if ret.status == 0 => {
                    let mut state = self.state.lock().unwrap();
                    *state = Default::default();
                }

                ReturnParameters::LeReadBufferSizeV2(ref ret) if ret.status == 0 => {
                    let mut state = self.state.lock().unwrap();
                    state.arbiter = Some(Arc::new(Arbiter::new(
                        self.next_module.clone(),
                        ret.iso_data_packet_length.into(),
                        ret.total_num_iso_data_packets.into(),
                    )));
                    self.service.reset(Arc::downgrade(state.arbiter.as_ref().unwrap()));
                }

                ReturnParameters::LeSetCigParameters(ref ret) if ret.status == 0 => {
                    let mut state = self.state.lock().unwrap();
                    for cis_handle in &ret.connection_handle {
                        state.cig_map.insert(*cis_handle, ret.cig_id);
                    }
                }

                ReturnParameters::LeRemoveCig(ref ret) if ret.status == 0 => {
                    let mut state = self.state.lock().unwrap();
                    let cig_map_it = state.cig_map.iter();
                    let handles: Vec<u16> = cig_map_it
                        .filter(|(_, cig_id)| *cig_id == &ret.cig_id)
                        .map(|(cis_handle, _)| *cis_handle)
                        .collect();
                    for handle in handles {
                        state.cig_map.remove(&handle);
                    }
                }

                ReturnParameters::LeSetupIsoDataPath(ref ret) => 'event: {
                    let mut state = self.state.lock().unwrap();
                    let handle = ret.connection_handle;
                    let stream = state.stream.get_mut(&handle).unwrap();
                    stream.state = if stream.state == StreamState::Enabling && ret.status == 0 {
                        StreamState::Enabled
                    } else {
                        StreamState::Disabled
                    };

                    let stream = state.stream.get(&handle).unwrap();
                    if stream.state != StreamState::Enabled {
                        break 'event;
                    }

                    let group_id = match stream.iso_type {
                        IsoType::Cis { .. } => match state.cig_map.get(&ret.connection_handle) {
                            Some(v) => *v as i32,
                            None => -1,
                        },
                        IsoType::Bis { big_handle } => big_handle as i32,
                    };

                    let (max_pdu_size, burst_number, flush_timeout) = match stream.iso_type {
                        IsoType::Cis { ref c_to_p, .. } => {
                            (c_to_p.max_pdu, c_to_p.burst_number, c_to_p.flush_timeout)
                        }
                        IsoType::Bis { .. } => unimplemented!("Broadcast stream not supported"),
                    };

                    self.service.start_stream(
                        handle,
                        StreamConfiguration {
                            groupId: group_id,
                            maxPduSize: max_pdu_size as i32,
                            isoIntervalUs: stream.iso_interval_us as i32,
                            burstNumber: burst_number as i32,
                            flushTimeout: flush_timeout as i32,
                        },
                    );
                }

                ReturnParameters::LeRemoveIsoDataPath(ref ret) if ret.status == 0 => {
                    let mut state = self.state.lock().unwrap();
                    let handle = ret.connection_handle;
                    let stream = state.stream.get_mut(&ret.connection_handle).unwrap();
                    if stream.state == StreamState::Enabled {
                        self.service.stop_stream(handle);
                    }
                    stream.state = StreamState::Disabled;
                }

                _ => (),
            },

            Event::LeCisEstablished(ref e) if e.status == 0 => {
                let mut state = self.state.lock().unwrap();
                let handle = e.connection_handle;
                if state.stream.insert(handle, Stream::new_cis(e)).is_some() {
                    log::error!("CIS already established");
                } else {
                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.add_connection(e.connection_handle);
                }
            }

            Event::DisconnectionComplete(ref e) if e.status == 0 => {
                let mut state = self.state.lock().unwrap();
                if state.stream.remove(&e.connection_handle).is_some() {
                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.remove_connection(e.connection_handle);
                }
            }

            Event::LeCreateBigComplete(ref e) if e.status == 0 => {
                let mut state = self.state.lock().unwrap();
                for handle in &e.bis_handles {
                    let stream = &mut state.stream;
                    if stream.insert(*handle, Stream::new_bis(e)).is_some() {
                        log::error!("BIS already established");
                    } else {
                        let arbiter = state.arbiter.as_ref().unwrap();
                        arbiter.add_connection(*handle);
                    }
                }
            }

            Event::LeTerminateBigComplete(ref e) => {
                let mut state = self.state.lock().unwrap();
                let stream_it = state.stream.iter();
                let handles: Vec<u16> = stream_it
                    .filter(|(_, stream)| {
                        matches!(
                            stream.iso_type,
                            IsoType::Bis { big_handle, .. } if big_handle == e.big_handle
                        )
                    })
                    .map(|(cis_handle, _)| *cis_handle)
                    .collect();
                for handle in handles {
                    state.stream.remove(&handle);
                    let arbiter = state.arbiter.as_ref().unwrap();
                    arbiter.remove_connection(handle);
                }
            }

            Event::NumberOfCompletedPackets(ref e) => {
                let (stack_event, _audio_event) = {
                    let mut stack_event = hci::NumberOfCompletedPackets::default();
                    let mut audio_event = hci::NumberOfCompletedPackets::default();
                    let state = self.state.lock().unwrap();
                    for item in &e.handles {
                        let handle = item.connection_handle;

                        let arbiter = state.arbiter.as_ref().unwrap();
                        arbiter.set_completed(handle, item.num_completed_packets.into());

                        let Some(stream) = state.stream.get(&handle) else {
                            stack_event.handles.push(*item);
                            continue;
                        };

                        if stream.state == StreamState::Enabled {
                            &mut audio_event
                        } else {
                            &mut stack_event
                        }
                        .handles
                        .push(*item);
                    }
                    (stack_event, audio_event)
                };

                if !stack_event.handles.is_empty() {
                    self.next().in_evt(&stack_event.to_bytes());
                }
                return;
            }

            Event::Malformed(code, sub_code) => {
                log::error!("Malformed event with code: ({:?}, {:?})", code, sub_code);
            }

            _ => (),
        }

        self.next().in_evt(data);
    }

    fn out_iso(&self, data: &[u8]) {
        let state = self.state.lock().unwrap();
        let arbiter = state.arbiter.as_ref().unwrap();
        arbiter.push_incoming(&IsoData::from_bytes(data).unwrap());
    }
}
