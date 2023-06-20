///! Rule group for general information.
use chrono::NaiveDateTime;
use std::collections::HashMap;
use std::convert::Into;
use std::fmt;
use std::io::Write;

use crate::engine::{Rule, RuleGroup, Signal};
use crate::parser::{Packet, PacketChild};
use bt_packets::custom_types::Address;
use bt_packets::hci::{
    AclCommandChild, CommandChild, ConnectionManagementCommandChild, ErrorCode, EventChild,
    GapData, GapDataType, LeMetaEventChild,
};

/// Valid values are in the range 0x0000-0x0EFF.
pub type ConnectionHandle = u16;

pub const INVALID_TS: NaiveDateTime = NaiveDateTime::MAX;

#[derive(Copy, Clone)]
enum AddressType {
    None,
    BREDR,
    LE,
    Dual,
}

impl fmt::Display for AddressType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let str = match self {
            AddressType::None => "Unknown type",
            AddressType::BREDR => "BR/EDR",
            AddressType::LE => "LE",
            AddressType::Dual => "Dual",
        };
        write!(f, "{}", str)
    }
}

impl AddressType {
    fn update(&mut self, new_type: AddressType) {
        *self = match self {
            AddressType::None => new_type,
            AddressType::Dual => AddressType::Dual,
            AddressType::BREDR => match new_type {
                AddressType::Dual | AddressType::LE => AddressType::Dual,
                _ => AddressType::BREDR,
            },
            AddressType::LE => match new_type {
                AddressType::Dual | AddressType::BREDR => AddressType::Dual,
                _ => AddressType::LE,
            },
        }
    }
}

enum InitiatorType {
    Unknown,
    Host,
    Peer,
}

impl fmt::Display for InitiatorType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let str = match self {
            InitiatorType::Unknown => "Unknown initiator",
            InitiatorType::Host => "Host initiated",
            InitiatorType::Peer => "Peer initiated",
        };
        write!(f, "{}", str)
    }
}

#[derive(Copy, Clone)]
enum AclState {
    None,
    Initiating,
    Accepting,
    Connected,
}

impl Into<InitiatorType> for AclState {
    fn into(self) -> InitiatorType {
        match self {
            AclState::Initiating => InitiatorType::Host,
            AclState::Accepting => InitiatorType::Peer,
            _ => InitiatorType::Unknown,
        }
    }
}

struct DeviceInformation {
    names: Vec<String>,
    address: Address,
    address_type: AddressType,
    acls: Vec<AclInformation>,
    acl_state: AclState,
}

impl DeviceInformation {
    pub fn new(address: Address) -> Self {
        DeviceInformation {
            names: vec![],
            address: address,
            address_type: AddressType::None,
            acls: vec![],
            acl_state: AclState::None,
        }
    }
}

impl fmt::Display for DeviceInformation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fn print_names(names: &Vec<String>) -> String {
            if names.is_empty() {
                return "<Unknown name>".to_string();
            } else if names.len() == 1 {
                return names[0].to_owned();
            } else {
                return format!("{:?}", names);
            }
        }

        let _ = writeln!(
            f,
            "{} ({}, {}), {} connections",
            self.address,
            self.address_type,
            print_names(&self.names),
            self.acls.len()
        );
        for acl in &self.acls {
            let _ = write!(f, "{}", acl);
        }

        Ok(())
    }
}

struct AclInformation {
    start_time: NaiveDateTime,
    end_time: NaiveDateTime,
    handle: ConnectionHandle,
    initiator: InitiatorType,
}

impl fmt::Display for AclInformation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fn print_time(ts: NaiveDateTime) -> String {
            if ts == INVALID_TS {
                return "N/A".to_owned();
            }
            return format!("{}", ts.time());
        }
        fn print_timestamps(start: NaiveDateTime, end: NaiveDateTime) -> String {
            if start == end {
                return format!("{} - Failed", start.time());
            }
            return format!("{} to {}", print_time(start), print_time(end));
        }

        writeln!(
            f,
            "> Handle: {}, {}, {}",
            self.handle,
            self.initiator,
            print_timestamps(self.start_time, self.end_time)
        )
    }
}

/// This rule prints devices names and connection/disconnection time.
struct InformationalRule {
    devices: HashMap<Address, DeviceInformation>,
    handles: HashMap<ConnectionHandle, Address>,
}

impl InformationalRule {
    pub fn new() -> Self {
        InformationalRule { devices: HashMap::new(), handles: HashMap::new() }
    }

    fn get_or_allocate_device(&mut self, address: &Address) -> &mut DeviceInformation {
        if !self.devices.contains_key(address) {
            self.devices.insert(*address, DeviceInformation::new(*address));
        }
        return self.devices.get_mut(address).unwrap();
    }

    pub fn report_address_type(&mut self, address: &Address, address_type: AddressType) {
        let info = self.get_or_allocate_device(address);
        info.address_type.update(address_type);
    }

    pub fn report_name(&mut self, address: &Address, name: &String) {
        let info = self.get_or_allocate_device(address);
        if !info.names.contains(name) {
            info.names.push(name.into());
        }
    }

    pub fn report_acl_state(&mut self, address: &Address, state: AclState) {
        let info = self.get_or_allocate_device(address);
        info.acl_state = state;
    }

    pub fn report_connection_start(
        &mut self,
        address: &Address,
        handle: ConnectionHandle,
        ts: NaiveDateTime,
    ) {
        let info = self.get_or_allocate_device(address);
        info.acls.push(AclInformation {
            start_time: ts,
            end_time: INVALID_TS,
            handle: handle,
            initiator: info.acl_state.into(),
        });
        info.acl_state = AclState::Connected;
        self.handles.insert(handle, *address);
    }

    pub fn report_connection_end(&mut self, handle: ConnectionHandle, ts: NaiveDateTime) {
        if !self.handles.contains_key(&handle) {
            // For simplicity we can't process unknown handle. This probably can be improved.
            return;
        }
        let info = self.get_or_allocate_device(&self.handles.get(&handle).unwrap().clone());

        // If we can't find the matching acl connection, create one.
        if info.acls.is_empty() || info.acls.last().unwrap().end_time != INVALID_TS {
            info.acls.push(AclInformation {
                start_time: INVALID_TS,
                end_time: ts,
                handle: handle,
                initiator: InitiatorType::Unknown,
            });
        } else {
            info.acls.last_mut().unwrap().end_time = ts;
        }
        info.acl_state = AclState::None;
        self.handles.remove(&handle);
    }

    fn process_gap_data(&mut self, address: &Address, data: &GapData) {
        match data.data_type {
            GapDataType::CompleteLocalName | GapDataType::ShortenedLocalName => {
                let name = String::from_utf8_lossy(data.data.as_slice()).into_owned();
                self.report_name(address, &name);
            }

            _ => {}
        }
    }
}

impl Rule for InformationalRule {
    fn process(&mut self, packet: &Packet) {
        match &packet.inner {
            PacketChild::HciEvent(ev) => match ev.specialize() {
                EventChild::ConnectionComplete(ev) => {
                    self.report_connection_start(
                        &ev.get_bd_addr(),
                        ev.get_connection_handle(),
                        packet.ts,
                    );

                    // If failed, assume it's the end of connection.
                    if ev.get_status() != ErrorCode::Success {
                        self.report_connection_end(ev.get_connection_handle(), packet.ts);
                    }
                }

                EventChild::DisconnectionComplete(ev) => {
                    self.report_connection_end(ev.get_connection_handle(), packet.ts);
                }

                EventChild::ExtendedInquiryResult(ev) => {
                    for data in ev.get_extended_inquiry_response() {
                        self.process_gap_data(&ev.get_address(), data);
                    }
                    self.report_address_type(&ev.get_address(), AddressType::BREDR);
                }

                EventChild::RemoteNameRequestComplete(ev) => {
                    if ev.get_status() != ErrorCode::Success {
                        return;
                    }
                    let name = String::from_utf8_lossy(ev.get_remote_name());
                    let name = name.trim_end_matches(char::from(0));
                    self.report_name(&ev.get_bd_addr(), &name.to_owned());
                    self.report_address_type(&ev.get_bd_addr(), AddressType::BREDR);
                }

                EventChild::LeMetaEvent(ev) => match ev.specialize() {
                    LeMetaEventChild::LeConnectionComplete(ev) => {
                        if ev.get_status() != ErrorCode::Success {
                            return;
                        }

                        // Determining LE initiator is complex, for simplicity assume host inits.
                        self.report_acl_state(&ev.get_peer_address(), AclState::Initiating);
                        self.report_connection_start(
                            &ev.get_peer_address(),
                            ev.get_connection_handle(),
                            packet.ts,
                        );
                        self.report_address_type(&ev.get_peer_address(), AddressType::LE);
                    }

                    LeMetaEventChild::LeEnhancedConnectionComplete(ev) => {
                        if ev.get_status() != ErrorCode::Success {
                            return;
                        }

                        // Determining LE initiator is complex, for simplicity assume host inits.
                        self.report_acl_state(&ev.get_peer_address(), AclState::Initiating);
                        self.report_connection_start(
                            &ev.get_peer_address(),
                            ev.get_connection_handle(),
                            packet.ts,
                        );
                        self.report_address_type(&ev.get_peer_address(), AddressType::LE);
                    }

                    LeMetaEventChild::LeAdvertisingReport(ev) => {
                        for resp in ev.get_responses() {
                            for data in &resp.advertising_data {
                                match GapData::parse(data.data.as_slice()) {
                                    Ok(gap_data) => self.process_gap_data(&resp.address, &gap_data),
                                    Err(_err) => {}
                                }
                            }
                            self.report_address_type(&resp.address, AddressType::LE);
                        }
                    }

                    LeMetaEventChild::LeExtendedAdvertisingReport(ev) => {
                        for resp in ev.get_responses() {
                            for data in &resp.advertising_data {
                                match GapData::parse(data.data.as_slice()) {
                                    Ok(gap_data) => self.process_gap_data(&resp.address, &gap_data),
                                    Err(_err) => {}
                                }
                            }
                            self.report_address_type(&resp.address, AddressType::LE);
                        }
                    }

                    _ => {}
                },

                _ => {}
            },

            PacketChild::HciCommand(cmd) => match cmd.specialize() {
                CommandChild::AclCommand(cmd) => match cmd.specialize() {
                    AclCommandChild::ConnectionManagementCommand(cmd) => match cmd.specialize() {
                        ConnectionManagementCommandChild::CreateConnection(cmd) => {
                            self.report_acl_state(&cmd.get_bd_addr(), AclState::Initiating);
                            self.report_address_type(&cmd.get_bd_addr(), AddressType::BREDR);
                        }

                        ConnectionManagementCommandChild::AcceptConnectionRequest(cmd) => {
                            self.report_acl_state(&cmd.get_bd_addr(), AclState::Accepting);
                            self.report_address_type(&cmd.get_bd_addr(), AddressType::BREDR);
                        }

                        _ => {}
                    },

                    _ => {}
                },

                _ => {}
            },

            _ => {}
        }
    }

    fn report(&self, writer: &mut dyn Write) {
        if self.devices.is_empty() {
            return;
        }

        let _ = writeln!(writer, "InformationalRule report:");
        for address in self.devices.keys() {
            let info = self.devices.get(address).unwrap();
            let _ = write!(writer, "{}", info);
        }
    }

    fn report_signals(&self) -> &[Signal] {
        &[]
    }
}

/// Get a rule group with collision rules.
pub fn get_informational_group() -> RuleGroup {
    let mut group = RuleGroup::new();
    group.add_rule(Box::new(InformationalRule::new()));

    group
}
