use crate::ffi;
use crate::packets::{hci, llcp};
use hci::Packet as _;
use llcp::Packet as _;
use num_traits::FromPrimitive;
use std::collections::HashMap;

#[derive(Clone, Debug)]
enum IsoDataPath {
    Hci,
}

#[derive(Clone, Debug)]
enum CisParameters {
    // Short configuration provided by HCI LE Set CIG Parameters.
    Short { rtn_c_to_p: u8, rtn_p_to_c: u8 },
    // Full configuration provided by HCI LE Set CIG Parameters Test.
    Full { nse: u8, bn_c_to_p: u8, bn_p_to_c: u8, max_pdu_c_to_p: u16, max_pdu_p_to_c: u16 },
}

impl Default for CisParameters {
    fn default() -> CisParameters {
        CisParameters::Short { rtn_c_to_p: 0, rtn_p_to_c: 0 }
    }
}

/// CIS configuration.
#[derive(Clone, Debug, Default)]
struct CisConfig {
    // CIS parameters.
    // cf Vol 6, Part B § 4.5.13.1 CIS parameters.
    max_sdu_c_to_p: u16,
    max_sdu_p_to_c: u16,
    phy_c_to_p: u8,
    phy_p_to_c: u8,
    parameters: CisParameters,
    // CIS connection handle.
    cis_connection_handle: u16,
}

/// CIG configuration.
#[derive(Clone, Debug, Default)]
struct CigConfig {
    // CIG parameters.
    sdu_interval_c_to_p: u32,
    sdu_interval_p_to_c: u32,
    iso_interval: Option<u16>,
    framed: bool,
    max_transport_latency_c_to_p: Option<u16>,
    max_transport_latency_p_to_c: Option<u16>,
    ft_c_to_p: Option<u8>,
    ft_p_to_c: Option<u8>,
    // True when the CIG can still be configured.
    configurable: bool,
}

/// CIG configuration.
#[derive(Clone, Debug, Default)]
struct CisRequest {
    cig_id: u8,
    cis_id: u8,
    acl_connection_handle: u16,
    cis_connection_handle: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
enum CisState {
    PendingRsp,
    PendingAccept,
    PendingInd,
    Connected,
}

/// Established CIS configuration.
pub struct Cis {
    pub cig_id: u8,
    pub cis_id: u8,
    pub role: hci::Role,
    pub acl_connection_handle: u16,
    pub cis_connection_handle: u16,
    state: CisState,
    /// CIS Parameters.
    cig_sync_delay: u32,
    cis_sync_delay: u32,
    phy_c_to_p: hci::PhyType,
    phy_p_to_c: hci::PhyType,
    nse: u8,
    bn_c_to_p: u8,
    bn_p_to_c: u8,
    ft_c_to_p: u8,
    ft_p_to_c: u8,
    max_pdu_c_to_p: u16,
    max_pdu_p_to_c: u16,
    iso_interval: u16,
}

/// ISO manager state.
pub struct IsoManager {
    /// CIG configuration.
    cig_config: HashMap<u8, CigConfig>,
    /// CIS configuration.
    cis_config: HashMap<(u8, u8), CisConfig>,
    // Map cis_id to the configured ISO Data Path from the
    // Central to the Peripheral.
    iso_data_path_c_to_p: HashMap<(u8, u8), IsoDataPath>,
    // Map cis_id to the configured ISO Data Path from the
    // Peripheral to the Central.
    iso_data_path_p_to_c: HashMap<(u8, u8), IsoDataPath>,
    /// Mapping from ACL connection handle to connection role.
    acl_connections: HashMap<u16, hci::Role>,
    /// Mapping from CIS connection handle to a CIS connection
    /// opened as central (initiated a LL_CIS_REQ) or peripheral
    /// (accepted with LL_CIS_RSP).
    cis_connections: HashMap<u16, Cis>,
    /// Pending CIS connection requests, initiated from the command
    /// HCI LE Create CIS.
    cis_connection_requests: Vec<CisRequest>,
    /// First unused CIS connection handle.
    next_cis_connection_handle: u16,
    /// Link layer callbacks.
    ops: ffi::ControllerOps,
}

impl IsoManager {
    pub fn new(ops: ffi::ControllerOps) -> IsoManager {
        IsoManager {
            ops,
            cig_config: Default::default(),
            cis_config: Default::default(),
            iso_data_path_c_to_p: Default::default(),
            iso_data_path_p_to_c: Default::default(),
            acl_connections: Default::default(),
            cis_connections: Default::default(),
            cis_connection_requests: Default::default(),
            next_cis_connection_handle: Default::default(),
        }
    }

    pub fn add_acl_connection(&mut self, acl_connection_handle: u16, role: hci::Role) {
        self.acl_connections.insert(acl_connection_handle, role);
    }

    pub fn remove_acl_connection(&mut self, acl_connection_handle: u16) {
        self.acl_connections.remove(&acl_connection_handle);
    }

    fn has_cis_connection_handle(&self, cis_connection_handle: u16) -> bool {
        self.cis_config.values().any(|cis| cis.cis_connection_handle == cis_connection_handle)
            || self.cis_connections.contains_key(&cis_connection_handle)
    }

    // Returns the first unused handle in the range 0x100..0x200.
    fn new_cis_connection_handle(&self) -> u16 {
        (0x100..0x200).find(|handle| !self.has_cis_connection_handle(*handle)).unwrap()
    }

    fn send_hci_event<E: Into<hci::EventPacket>>(&self, event: E) {
        self.ops.send_hci_event(&event.into().to_vec())
    }

    fn sendl_llcp_packet<P: Into<llcp::LlcpPacket>>(&self, acl_connection_handle: u16, packet: P) {
        self.ops.send_llcp_packet(acl_connection_handle, &packet.into().to_vec())
    }

    fn get_le_features(&self) -> u64 {
        self.ops.get_le_features()
    }

    fn supported_phys(&self) -> u8 {
        1 // LE 1M
    }

    fn connected_isochronous_stream_host_support(&self) -> bool {
        // TODO
        true
    }

    pub fn get_cis_connection_handle<F>(&self, predicate: F) -> Option<u16>
    where
        F: Fn(&Cis) -> bool,
    {
        self.cis_connections
            .iter()
            .filter(|(_, cis)| predicate(cis))
            .map(|(handle, _)| handle)
            .next()
            .cloned()
    }

    pub fn get_cis(&self, cis_connection_handle: u16) -> Option<&Cis> {
        self.cis_connections.get(&cis_connection_handle)
    }

    /// Start the next CIS connection request, if any.
    fn deque_cis_connection_request(&mut self) {
        if let Some(request) = self.cis_connection_requests.pop() {
            let cis_config = self.cis_config.get(&(request.cig_id, request.cis_id)).unwrap();
            let cig_config = self.cig_config.get(&request.cig_id).unwrap();
            let CisParameters::Full { nse, bn_c_to_p, bn_p_to_c, max_pdu_c_to_p, max_pdu_p_to_c } =
                cis_config.parameters else { unreachable!() };

            self.cis_connections.insert(
                request.cis_connection_handle,
                Cis {
                    cig_id: request.cig_id,
                    cis_id: request.cis_id,
                    role: hci::Role::Central,
                    acl_connection_handle: request.acl_connection_handle,
                    cis_connection_handle: request.cis_connection_handle,
                    state: CisState::PendingRsp,
                    cig_sync_delay: 0,              // TODO
                    cis_sync_delay: 0,              // TODO
                    phy_c_to_p: hci::PhyType::Le1m, // TODO
                    phy_p_to_c: hci::PhyType::Le1m, // TODO
                    nse,
                    bn_c_to_p,
                    bn_p_to_c,
                    ft_c_to_p: cig_config.ft_c_to_p.unwrap(),
                    ft_p_to_c: cig_config.ft_p_to_c.unwrap(),
                    max_pdu_c_to_p,
                    max_pdu_p_to_c,
                    iso_interval: cig_config.iso_interval.unwrap(),
                },
            );

            self.sendl_llcp_packet(
                request.acl_connection_handle,
                llcp::CisReqBuilder {
                    cig_id: request.cig_id,
                    cis_id: request.cis_id,
                    phy_c_to_p: hci::PhyType::Le1m as u8, // TODO
                    phy_p_to_c: hci::PhyType::Le1m as u8, // TODO
                    framed: cig_config.framed as u8,
                    max_sdu_c_to_p: cis_config.max_sdu_c_to_p,
                    max_sdu_p_to_c: cis_config.max_sdu_p_to_c,
                    sdu_interval_c_to_p: cig_config.sdu_interval_c_to_p,
                    sdu_interval_p_to_c: cig_config.sdu_interval_p_to_c,
                    max_pdu_c_to_p,
                    max_pdu_p_to_c,
                    nse,
                    sub_interval: 0, // TODO
                    bn_c_to_p,
                    bn_p_to_c,
                    ft_c_to_p: cig_config.ft_c_to_p.unwrap(),
                    ft_p_to_c: cig_config.ft_p_to_c.unwrap(),
                    iso_interval: cig_config.iso_interval.unwrap(),
                    cis_offset_min: 0, // TODO
                    cis_offset_max: 0, // TODO
                    conn_event_count: 0,
                },
            );
        }
    }

    pub fn hci_le_set_cig_parameters(&mut self, packet: hci::LeSetCigParametersPacket) {
        let cig_id: u8 = packet.get_cig_id();
        let sdu_interval_c_to_p: u32 = packet.get_sdu_interval_c_to_p();
        let sdu_interval_p_to_c: u32 = packet.get_sdu_interval_p_to_c();
        let framed: bool = packet.get_framing() == hci::Enable::Enabled;
        let max_transport_latency_c_to_p: u16 = packet.get_max_transport_latency_c_to_p();
        let max_transport_latency_p_to_c: u16 = packet.get_max_transport_latency_p_to_c();
        let cis_config: &[hci::CisParametersConfig] = packet.get_cis_config();

        let command_complete = |status| hci::LeSetCigParametersCompleteBuilder {
            status,
            cig_id,
            connection_handle: vec![],
            num_hci_command_packets: 1,
        };

        // If the Host issues this command when the CIG is not in the configurable
        // state, the Controller shall return the error code
        // Command Disallowed (0x0C).
        if !self.cig_config.get(&cig_id).map(|cig| cig.configurable).unwrap_or(true) {
            println!("CIG ({}) is no longer in the configurable state", cig_id);
            return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
        }

        for cis in cis_config {
            // If the Host sets, in the PHY_C_To_P[i] or PHY_P_To_C[i] parameters, a bit
            // for a PHY that the Controller does not support, including a bit that is
            // reserved for future use, the Controller shall return the error code
            // Unsupported Feature or Parameter Value (0x11).
            if (cis.phy_c_to_p & !self.supported_phys()) != 0
                || (cis.phy_p_to_c & !self.supported_phys()) != 0
            {
                println!(
                    "CIS ({}) configures unsupported PHYs ({:x}, {:x})",
                    cis.cis_id, cis.phy_c_to_p, cis.phy_p_to_c
                );
                return self.send_hci_event(command_complete(
                    hci::ErrorCode::UnsupportedFeatureOrParameterValue,
                ));
            }

            // If a CIS configuration that is being modified has a data path set in
            // the Central to Peripheral direction and the Host has specified
            // that Max_SDU_C_To_P[i] shall be set to zero, the Controller shall
            // return the error code Command Disallowed (0x0C).
            if self.iso_data_path_c_to_p.contains_key(&(cig_id, cis.cis_id))
                && cis.max_sdu_c_to_p == 0
            {
                println!(
                    "CIS ({}) has a data path for C->P but Max_SDU_C_To_P is zero",
                    cis.cis_id
                );
                return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
            }

            // If a CIS configuration that is being modified has a data path set in the
            // Peripheral to Central direction and the Host has specified that
            // Max_SDU_P_To_C[i] shall be set to zero, the Controller shall return
            // the error code Command Disallowed (0x0C).
            if self.iso_data_path_p_to_c.contains_key(&(cig_id, cis.cis_id))
                && cis.max_sdu_p_to_c == 0
            {
                println!(
                    "CIS ({}) has a data path for P->C but Max_SDU_P_To_C is zero",
                    cis.cis_id
                );
                return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
            }
        }

        // If the Host specifies an invalid combination of CIS parameters, the
        // Controller shall return the error code Unsupported Feature or
        // Parameter Value (0x11).
        // TODO

        // If the Status return parameter is non-zero, then the state of the CIG
        // and its CIS configurations shall not be changed by the command.
        // If the CIG did not already exist, it shall not be created.
        let cig = self.cig_config.entry(cig_id).or_default();
        let mut cis_connection_handles = vec![];
        cig.sdu_interval_c_to_p = sdu_interval_c_to_p;
        cig.sdu_interval_p_to_c = sdu_interval_p_to_c;
        cig.max_transport_latency_c_to_p = Some(max_transport_latency_c_to_p);
        cig.max_transport_latency_p_to_c = Some(max_transport_latency_p_to_c);
        cig.framed = framed;

        for cis_config in cis_config {
            let cis_connection_handle = self
                .cis_config
                .get(&(cig_id, cis_config.cis_id))
                .map(|cis| cis.cis_connection_handle)
                .unwrap_or_else(|| self.new_cis_connection_handle());
            cis_connection_handles.push(cis_connection_handle);
            let cis = self.cis_config.entry((cig_id, cis_config.cis_id)).or_default();
            cis.cis_connection_handle = cis_connection_handle;
            cis.max_sdu_c_to_p = cis_config.max_sdu_c_to_p;
            cis.max_sdu_p_to_c = cis_config.max_sdu_p_to_c;
            cis.phy_c_to_p = cis_config.phy_c_to_p;
            cis.phy_p_to_c = cis_config.phy_p_to_c;
            cis.parameters = CisParameters::Short {
                rtn_c_to_p: cis_config.rtn_c_to_p,
                rtn_p_to_c: cis_config.rtn_p_to_c,
            };
        }

        self.send_hci_event(hci::LeSetCigParametersCompleteBuilder {
            status: hci::ErrorCode::Success,
            cig_id,
            connection_handle: cis_connection_handles,
            num_hci_command_packets: 1,
        })
    }

    pub fn hci_le_set_cig_parameters_test(&mut self, packet: hci::LeSetCigParametersTestPacket) {
        let cig_id: u8 = packet.get_cig_id();
        let sdu_interval_c_to_p: u32 = packet.get_sdu_interval_c_to_p();
        let sdu_interval_p_to_c: u32 = packet.get_sdu_interval_p_to_c();
        let ft_c_to_p: u8 = packet.get_ft_c_to_p();
        let ft_p_to_c: u8 = packet.get_ft_p_to_c();
        let iso_interval: u16 = packet.get_iso_interval();
        let framed: bool = packet.get_framing() == hci::Enable::Enabled;
        let cis_config: &[hci::LeCisParametersTestConfig] = packet.get_cis_config();

        let command_complete = |status| hci::LeSetCigParametersTestCompleteBuilder {
            status,
            cig_id,
            connection_handle: vec![],
            num_hci_command_packets: 1,
        };

        // If the Host issues this command when the CIG is not in the configurable
        // state, the Controller shall return the error code
        // Command Disallowed (0x0C).
        if !self.cig_config.get(&cig_id).map(|cig| cig.configurable).unwrap_or(true) {
            println!("CIG ({}) is no longer in the configurable state", cig_id);
            return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
        }

        for cis in cis_config {
            // If the Host sets, in the PHY_C_To_P[i] or PHY_P_To_C[i] parameters, a bit
            // for a PHY that the Controller does not support, including a bit that is
            // reserved for future use, the Controller shall return the error code
            // Unsupported Feature or Parameter Value (0x11).
            if (cis.phy_c_to_p & !self.supported_phys()) != 0
                || (cis.phy_p_to_c & !self.supported_phys()) != 0
            {
                println!(
                    "CIS ({}) configures unsupported PHYs ({:x}, {:x})",
                    cis.cis_id, cis.phy_c_to_p, cis.phy_p_to_c
                );
                return self.send_hci_event(command_complete(
                    hci::ErrorCode::UnsupportedFeatureOrParameterValue,
                ));
            }

            // If a CIS configuration that is being modified has a data path set in
            // the Central to Peripheral direction and the Host has specified
            // that Max_SDU_C_To_P[i] shall be set to zero, the Controller shall
            // return the error code Command Disallowed (0x0C).
            if self.iso_data_path_c_to_p.contains_key(&(cig_id, cis.cis_id))
                && cis.max_sdu_c_to_p == 0
            {
                println!(
                    "CIS ({}) has a data path for C->P but Max_SDU_C_To_P is zero",
                    cis.cis_id
                );
                return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
            }

            // If a CIS configuration that is being modified has a data path set in the
            // Peripheral to Central direction and the Host has specified that
            // Max_SDU_P_To_C[i] shall be set to zero, the Controller shall return
            // the error code Command Disallowed (0x0C).
            if self.iso_data_path_p_to_c.contains_key(&(cig_id, cis.cis_id))
                && cis.max_sdu_p_to_c == 0
            {
                println!(
                    "CIS ({}) has a data path for P->C but Max_SDU_P_To_C is zero",
                    cis.cis_id
                );
                return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
            }
        }

        // If the Host specifies an invalid combination of CIS parameters, the
        // Controller shall return the error code Unsupported Feature or
        // Parameter Value (0x11).
        // TODO

        // If the Status return parameter is non-zero, then the state of the CIG
        // and its CIS configurations shall not be changed by the command.
        // If the CIG did not already exist, it shall not be created.
        let cig = self.cig_config.entry(cig_id).or_default();
        let mut cis_connection_handles = vec![];
        cig.sdu_interval_c_to_p = sdu_interval_c_to_p;
        cig.sdu_interval_p_to_c = sdu_interval_p_to_c;
        cig.ft_c_to_p = Some(ft_c_to_p);
        cig.ft_p_to_c = Some(ft_p_to_c);
        cig.iso_interval = Some(iso_interval);
        cig.framed = framed;

        for cis_config in cis_config {
            let cis_connection_handle = self
                .cis_config
                .get(&(cig_id, cis_config.cis_id))
                .map(|cis| cis.cis_connection_handle)
                .unwrap_or_else(|| self.new_cis_connection_handle());
            cis_connection_handles.push(cis_connection_handle);
            let cis = self.cis_config.entry((cig_id, cis_config.cis_id)).or_default();
            cis.cis_connection_handle = cis_connection_handle;
            cis.max_sdu_c_to_p = cis_config.max_sdu_c_to_p;
            cis.max_sdu_p_to_c = cis_config.max_sdu_p_to_c;
            cis.phy_c_to_p = cis_config.phy_c_to_p;
            cis.phy_p_to_c = cis_config.phy_p_to_c;
            cis.parameters = CisParameters::Full {
                nse: cis_config.nse,
                bn_c_to_p: cis_config.bn_c_to_p,
                bn_p_to_c: cis_config.bn_p_to_c,
                max_pdu_c_to_p: cis_config.max_pdu_p_to_c,
                max_pdu_p_to_c: cis_config.max_pdu_c_to_p,
            };
        }

        self.send_hci_event(hci::LeSetCigParametersTestCompleteBuilder {
            status: hci::ErrorCode::Success,
            cig_id,
            connection_handle: cis_connection_handles,
            num_hci_command_packets: 1,
        })
    }

    pub fn hci_le_remove_cig(&mut self, packet: hci::LeRemoveCigPacket) {
        let cig_id: u8 = packet.get_cig_id();

        let command_complete =
            |status| hci::LeRemoveCigCompleteBuilder { status, cig_id, num_hci_command_packets: 1 };

        // If the Host issues this command with a CIG_ID that does not exist, the
        // Controller shall return the error code Unknown Connection Identifier (0x02).
        if !self.cig_config.contains_key(&cig_id) {
            println!("CIG ({}) does not exist", cig_id);
            return self.send_hci_event(command_complete(hci::ErrorCode::UnknownConnection));
        }

        // If the Host tries to remove a CIG which is in the active state,
        // then the Controller shall return the error code
        // Command Disallowed (0x0C).
        if self
            .cis_connections
            .values()
            .any(|cis| cis.role == hci::Role::Central && cis.cig_id == cig_id)
        {
            println!("CIG ({}) cannot be removed as it is in active state", cig_id);
            return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
        }

        // This command shall also remove the isochronous data paths that are
        //  associated with the Connection_Handles of the CIS configurations.
        let iso_data_path_c_to_p: Vec<_> =
            self.iso_data_path_c_to_p.keys().filter(|key| key.0 == cig_id).cloned().collect();
        for key in iso_data_path_c_to_p {
            self.iso_data_path_c_to_p.remove(&key);
        }
        let iso_data_path_p_to_c: Vec<_> =
            self.iso_data_path_p_to_c.keys().filter(|key| key.0 == cig_id).cloned().collect();
        for key in iso_data_path_p_to_c {
            self.iso_data_path_p_to_c.remove(&key);
        }

        self.send_hci_event(command_complete(hci::ErrorCode::Success))
    }

    pub fn hci_le_create_cis(&mut self, packet: hci::LeCreateCisPacket) {
        let cis_config: &[hci::LeCreateCisConfig] = packet.get_cis_config();
        let mut cis_connection_requests: Vec<CisRequest> = vec![];

        let command_status =
            |status| hci::LeCreateCisStatusBuilder { status, num_hci_command_packets: 1 };

        for cis_config in cis_config {
            match self.acl_connections.get(&cis_config.acl_connection_handle) {
                // If any ACL_Connection_Handle[i] is not the handle of an existing ACL
                // connection, the Controller shall return the error code Unknown Connection
                // Identifier (0x02).
                None => {
                    println!(
                        "cannot create LE CIS with unknown ACL connection handle {}",
                        cis_config.acl_connection_handle
                    );
                    return self.send_hci_event(command_status(hci::ErrorCode::UnknownConnection));
                }
                // If the Host issues this command on an ACL_Connection_Handle where the
                // Controller is the Peripheral, the Controller shall return the error code
                // Command Disallowed (0x0C).
                Some(hci::Role::Peripheral) => {
                    println!(
                        "the ACL connection handle {} is for a peripheral connection",
                        cis_config.acl_connection_handle
                    );
                    return self.send_hci_event(command_status(
                        hci::ErrorCode::InvalidHciCommandParameters,
                    ));
                }
                Some(hci::Role::Central) => (),
            }

            // If the Host attempts to create a CIS that has already been created, the
            // Controller shall return the error code Connection Already Exists (0x0B).
            if self.cis_connections.contains_key(&cis_config.cis_connection_handle) {
                println!(
                    "cannot create LE CIS with CIS connection handle {} as it is already connected",
                    cis_config.cis_connection_handle
                );
                return self
                    .send_hci_event(command_status(hci::ErrorCode::ConnectionAlreadyExists));
            }

            // If two different elements of the CIS_Connection_Handle arrayed parameter
            // identify the same CIS, the Controller shall return the error code
            // Invalid HCI Command Parameters (0x12).
            if cis_connection_requests
                .iter()
                .any(|request| request.cis_connection_handle == cis_config.cis_connection_handle)
            {
                println!(
                    "the CIS connection handle {} is requested twice",
                    cis_config.cis_connection_handle
                );
                return self
                    .send_hci_event(command_status(hci::ErrorCode::InvalidHciCommandParameters));
            }

            match self
                .cis_config
                .iter()
                .find(|(_, cis)| cis.cis_connection_handle == cis_config.cis_connection_handle)
            {
                // If any CIS_Connection_Handle[i] is not the handle of a CIS or CIS
                // configuration, the Controller shall return the error code Unknown Connection
                // Identifier (0x02).
                None => {
                    println!(
                        "cannot create LE CIS with unknown CIS connection handle {}",
                        cis_config.cis_connection_handle
                    );
                    return self.send_hci_event(command_status(hci::ErrorCode::UnknownConnection));
                }
                Some(((cig_id, cis_id), _)) => cis_connection_requests.push(CisRequest {
                    cis_connection_handle: cis_config.cis_connection_handle,
                    acl_connection_handle: cis_config.acl_connection_handle,
                    cig_id: *cig_id,
                    cis_id: *cis_id,
                }),
            }
        }

        // If the Host issues this command before all the HCI_LE_CIS_Established
        // events from the previous use of the command have been generated, the
        // Controller shall return the error code Command Disallowed (0x0C).
        if !self.cis_connection_requests.is_empty() {
            println!("another LE Create CIS request is already pending");
            return self.send_hci_event(command_status(hci::ErrorCode::CommandDisallowed));
        }

        // If the Host issues this command when the Connected Isochronous Stream
        // (Host Support) feature bit (see [Vol 6] Part B, Section 4.6.27) is not set,
        // the Controller shall return the error code Command Disallowed (0x0C).
        if !self.connected_isochronous_stream_host_support() {
            println!("the feature bit Connected Isochronous Stream (Host Support) is not set");
            return self.send_hci_event(command_status(hci::ErrorCode::CommandDisallowed));
        }

        // Entering the CIG active state, the CIG configuration can no longer
        // be edited. Derive full parameters for the CIG groups which created
        // CIS connections belong to.

        // Vol 6, Part G § 2 ISOAL Features

        // Unframed PDUs shall only be used when the ISO_Interval is equal to or an
        // integer multiple of the SDU_Interval and a constant time offset alignment is
        // maintained between the SDU generation and the timing in the isochronous
        // transport. This requires the upper layer to synchronize generation of its data to
        // the effective transport timing. When the Host requests the use of framed PDUs,
        // the Controller shall use framed PDUs.

        // Vol 6, Part B § 4.5.13.2 CIS events and subevents

        // ISO_Interval shall be a multiple of 1.25 ms in the range of 5 ms to 4 s, shall be
        // at least NSE × Sub_Interval, and shall be less than half the
        // connSupervisionTimeout for the associated ACL.

        // SE_Length shall be MPT_C + T_IFS + MPT_P + T_MSS.

        // Sub_Interval shall be greater than or equal to SE_Length (also see Section 4.5.14.2).

        // BN shall be in the range 0 to 15. For a bidirectional link the value shall be
        // nonzero for both directions. For a unidirectional link it shall be non-zero in the
        // direction that data is being sent and zero in the other direction.

        // NSE shall be in the range max (BN_C_To_P, BN_P_To_C) to 31.

        // Transport_Latency_C_To_P = CIG_Sync_Delay + FT_C_To_P × ISO_Interval + SDU_Interval_C_To_P
        // Transport_Latency_P_To_C = CIG_Sync_Delay + FT_P_To_C × ISO_Interval + SDU_Interval_P_To_C

        // Update the pending CIS request list.
        self.cis_connection_requests = cis_connection_requests;

        // Send the first connection request.
        self.deque_cis_connection_request();
        self.send_hci_event(command_status(hci::ErrorCode::Success))
    }

    pub fn hci_le_accept_cis_request(&mut self, packet: hci::LeAcceptCisRequestPacket) {
        let connection_handle: u16 = packet.get_connection_handle();

        let command_status =
            |status| hci::LeAcceptCisRequestStatusBuilder { status, num_hci_command_packets: 1 };

        // If the Peripheral’s Host issues this command with a
        // Connection_Handle that does not exist, or the Connection_Handle
        // is not for a CIS, the Controller shall return the error code
        // Unknown Connection Identifier (0x02).
        if !self.cis_connections.contains_key(&connection_handle) {
            println!(
                "cannot accept LE CIS request with invalid connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_status(hci::ErrorCode::UnknownConnection));
        }

        let cis = self.cis_connections.get_mut(&connection_handle).unwrap();

        // If the Central’s Host issues this command, the Controller shall
        // return the error code Command Disallowed (0x0C).
        if cis.role == hci::Role::Central {
            println!(
                "cannot accept LE CIS request with central connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_status(hci::ErrorCode::CommandDisallowed));
        }

        // If the Peripheral's Host issues this command with a Connection_Handle
        // for a CIS that has already been established or that already has an
        // HCI_LE_Accept_CIS_Request or HCI_LE_Reject_CIS_Request command in progress,
        // the Controller shall return the error code Command Disallowed (0x0C).
        if cis.state != CisState::PendingAccept {
            println!(
                "cannot accept LE CIS request for non-pending connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_status(hci::ErrorCode::CommandDisallowed));
        }

        // Update local state.
        cis.state = CisState::PendingInd;

        // Send back LL_CIS_RSP to accept the request.
        let acl_connection_handle = cis.acl_connection_handle;
        self.sendl_llcp_packet(
            acl_connection_handle,
            llcp::CisRspBuilder {
                cis_offset_min: 0,
                cis_offset_max: 0xffffff,
                conn_event_count: 0,
            },
        );

        self.send_hci_event(command_status(hci::ErrorCode::Success))
    }

    pub fn hci_le_reject_cis_request(&mut self, packet: hci::LeRejectCisRequestPacket) {
        let connection_handle: u16 = packet.get_connection_handle();

        let command_complete = |status| hci::LeRejectCisRequestCompleteBuilder {
            status,
            connection_handle,
            num_hci_command_packets: 1,
        };

        // If the Peripheral’s Host issues this command with a
        // Connection_Handle that does not exist, or the Connection_Handle
        // is not for a CIS, the Controller shall return the error code
        // Unknown Connection Identifier (0x02).
        if !self.cis_connections.contains_key(&connection_handle) {
            println!(
                "cannot accept LE CIS request with invalid connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_complete(hci::ErrorCode::UnknownConnection));
        }

        let cis = self.cis_connections.get(&connection_handle).unwrap();

        // If the Central’s Host issues this command, the Controller shall
        // return the error code Command Disallowed (0x0C).
        if cis.role == hci::Role::Central {
            println!(
                "cannot accept LE CIS request with central connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
        }

        // If the Peripheral's Host issues this command with a Connection_Handle
        // for a CIS that has already been established or that already has an
        // HCI_LE_Accept_CIS_Request or HCI_LE_Reject_CIS_Request command in progress,
        // the Controller shall return the error code Command Disallowed (0x0C).
        if cis.state != CisState::PendingAccept {
            println!(
                "cannot accept LE CIS request for non-pending connection handle {}",
                connection_handle
            );
            return self.send_hci_event(command_complete(hci::ErrorCode::CommandDisallowed));
        }

        // Update local state.
        let acl_connection_handle = cis.acl_connection_handle;
        self.cis_connections.remove(&connection_handle);

        // Send back LL_CIS_RSP to reject the request.
        let error_code = if packet.get_reason() == hci::ErrorCode::Success {
            hci::ErrorCode::RemoteUserTerminatedConnection
        } else {
            packet.get_reason()
        };
        self.sendl_llcp_packet(
            acl_connection_handle,
            llcp::RejectExtIndBuilder {
                reject_opcode: llcp::Opcode::LlCisReq as u8,
                error_code: error_code as u8,
            },
        );

        self.send_hci_event(command_complete(hci::ErrorCode::Success))
    }

    pub fn hci_le_setup_iso_data_path(&mut self, packet: hci::LeSetupIsoDataPathPacket) {
        let connection_handle: u16 = packet.get_connection_handle();

        let command_complete = |status| hci::LeSetupIsoDataPathCompleteBuilder {
            status,
            connection_handle,
            num_hci_command_packets: 1,
        };
        
        // If the Host attempts to set a data path with a Connection Handle that does not
        // exist or that is not for a CIS, CIS configuration, or BIS, the Controller shall
        // return the error code Unknown Connection Identifier (0x02).

        // If the Host issues this command more than once for the same
        // Connection_Handle and direction before issuing the HCI_LE_Remove_ISO_Data_-
        // Path command for that Connection_Handle and direction, the Controller shall
        // return the error code Command Disallowed (0x0C).
        
        // If the Host issues this command for a CIS on a Peripheral before it has issued
        // the HCI_LE_Accept_CIS_Request command for that CIS, then the Controller
        // shall return the error code Command Disallowed (0x0C).
        
        // If the Host issues this command for a vendor-specific data transport path that
        // has not been configured using the HCI_Configure_Data_Path command, the
        // Controller shall return the error code Command Disallowed (0x0C).
        
        // If the Host attempts to set an output data path using a connection handle that is
        // for an Isochronous Broadcaster, for an input data path on a Synchronized
        // Receiver, or for a data path for the direction on a unidirectional CIS where BN
        // is set to 0, the Controller shall return the error code Command Disallowed
        // (0x0C).
        
        // If the Host issues this command with Codec_Configuration_Length non-zero
        // and Codec_ID set to transparent air mode, the Controller shall return the error
        // code Invalid HCI Command Parameters (0x12).

        // If the Host issues this command with codec-related parameters that exceed the
        // bandwidth and latency allowed on the established CIS or BIS identified by the
        // Connection_Handle parameter, the Controller shall return the error code
        // Invalid HCI Command Parameters (0x12).
        //

        self.send_hci_event(command_complete(hci::ErrorCode::Success))
    }

    pub fn hci_le_remove_iso_data_path(&mut self, _packet: hci::LeRemoveIsoDataPathPacket) {
        todo!()
    }

    pub fn ll_cis_req(&mut self, acl_connection_handle: u16, packet: llcp::CisReq) {
        let cis_connection_handle = self.new_cis_connection_handle();
        self.cis_connections.insert(
            cis_connection_handle,
            Cis {
                cig_id: packet.get_cig_id(),
                cis_id: packet.get_cis_id(),
                role: hci::Role::Peripheral,
                acl_connection_handle,
                cis_connection_handle,
                state: CisState::PendingAccept,
                cig_sync_delay: 0,
                cis_sync_delay: 0,
                phy_c_to_p: hci::PhyType::Le1m, // TODO get from packet
                phy_p_to_c: hci::PhyType::Le1m, // TODO get from packet
                nse: packet.get_nse(),
                bn_c_to_p: packet.get_bn_c_to_p(),
                bn_p_to_c: packet.get_bn_p_to_c(),
                ft_c_to_p: packet.get_ft_c_to_p(),
                ft_p_to_c: packet.get_ft_p_to_c(),
                max_pdu_c_to_p: packet.get_max_pdu_c_to_p(),
                max_pdu_p_to_c: packet.get_max_pdu_p_to_c(),
                iso_interval: packet.get_iso_interval(),
            },
        );

        self.send_hci_event(hci::LeCisRequestBuilder {
            acl_connection_handle,
            cis_connection_handle,
            cig_id: packet.get_cig_id(),
            cis_id: packet.get_cis_id(),
        })
    }

    pub fn ll_cis_rsp(&mut self, acl_connection_handle: u16, _packet: llcp::CisRsp) {
        let cis_connection_handle = self.get_cis_connection_handle(|cis| {
            cis.acl_connection_handle == acl_connection_handle
                && cis.role == hci::Role::Central
                && cis.state == CisState::PendingRsp
        });

        if let Some(cis_connection_handle) = cis_connection_handle {
            self.cis_connections
                .entry(cis_connection_handle)
                .and_modify(|cis| cis.state = CisState::Connected);
            let cis = self.cis_connections.get(&cis_connection_handle).unwrap();
            self.sendl_llcp_packet(
                acl_connection_handle,
                llcp::CisIndBuilder {
                    aa: 0,
                    cis_offset: 0,
                    cig_sync_delay: cis.cig_sync_delay,
                    cis_sync_delay: cis.cis_sync_delay,
                    conn_event_count: 0,
                },
            );
            self.send_hci_event(hci::LeCisEstablishedBuilder {
                status: hci::ErrorCode::Success,
                connection_handle: cis_connection_handle,
                cig_sync_delay: cis.cig_sync_delay,
                cis_sync_delay: cis.cis_sync_delay,
                transport_latency_c_to_p: 0, // TODO
                transport_latency_p_to_c: 0, // TODO
                phy_c_to_p: hci::SecondaryPhyType::from_u8(cis.phy_c_to_p as u8).unwrap(),
                phy_p_to_c: hci::SecondaryPhyType::from_u8(cis.phy_p_to_c as u8).unwrap(),
                nse: cis.nse,
                bn_p_to_c: cis.bn_c_to_p,
                bn_c_to_p: cis.bn_p_to_c,
                ft_p_to_c: cis.ft_c_to_p,
                ft_c_to_p: cis.ft_p_to_c,
                max_pdu_p_to_c: cis.max_pdu_c_to_p as u8,
                max_pdu_c_to_p: cis.max_pdu_p_to_c as u8,
                iso_interval: cis.iso_interval,
            });
            // Start the next pending connection request.
            self.deque_cis_connection_request();
        } else {
            println!("skipping out of place packet LL_CIS_RSP");
        }
    }

    pub fn ll_reject_ext_ind(&mut self, acl_connection_handle: u16, packet: llcp::RejectExtInd) {
        if packet.get_reject_opcode() != llcp::Opcode::LlCisReq as u8 {
            return;
        }

        let cis_connection_handle = self.get_cis_connection_handle(|cis| {
            cis.acl_connection_handle == acl_connection_handle
                && cis.role == hci::Role::Central
                && cis.state == CisState::PendingRsp
        });

        if let Some(cis_connection_handle) = cis_connection_handle {
            self.cis_connections.remove(&cis_connection_handle);
            self.send_hci_event(hci::LeCisEstablishedBuilder {
                status: hci::ErrorCode::RemoteUserTerminatedConnection,
                connection_handle: cis_connection_handle,
                cig_sync_delay: 0,
                cis_sync_delay: 0,
                transport_latency_c_to_p: 0,
                transport_latency_p_to_c: 0,
                phy_c_to_p: hci::SecondaryPhyType::NoPackets,
                phy_p_to_c: hci::SecondaryPhyType::NoPackets,
                nse: 0,
                bn_p_to_c: 0,
                bn_c_to_p: 0,
                ft_p_to_c: 0,
                ft_c_to_p: 0,
                max_pdu_p_to_c: 0,
                max_pdu_c_to_p: 0,
                iso_interval: 0,
            });
            // Start the next pending connection request.
            self.deque_cis_connection_request();
        } else {
            println!("skipping out of place packet LL_CIS_IND");
        }
    }

    pub fn ll_cis_ind(&mut self, acl_connection_handle: u16, packet: llcp::CisInd) {
        let cis_connection_handle = self.get_cis_connection_handle(|cis| {
            cis.acl_connection_handle == acl_connection_handle
                && cis.role == hci::Role::Peripheral
                && cis.state == CisState::PendingInd
        });

        if let Some(cis_connection_handle) = cis_connection_handle {
            self.cis_connections.entry(cis_connection_handle).and_modify(|cis| {
                cis.state = CisState::Connected;
                cis.cig_sync_delay = packet.get_cig_sync_delay();
                cis.cis_sync_delay = packet.get_cis_sync_delay();
            });
            let cis = self.cis_connections.get(&cis_connection_handle).unwrap();
            self.send_hci_event(hci::LeCisEstablishedBuilder {
                status: hci::ErrorCode::Success,
                connection_handle: cis_connection_handle,
                cig_sync_delay: cis.cig_sync_delay,
                cis_sync_delay: cis.cis_sync_delay,
                transport_latency_c_to_p: 0, // TODO
                transport_latency_p_to_c: 0, // TODO
                phy_c_to_p: hci::SecondaryPhyType::from_u8(cis.phy_c_to_p as u8).unwrap(),
                phy_p_to_c: hci::SecondaryPhyType::from_u8(cis.phy_p_to_c as u8).unwrap(),
                nse: cis.nse,
                bn_p_to_c: cis.bn_c_to_p,
                bn_c_to_p: cis.bn_p_to_c,
                ft_p_to_c: cis.ft_c_to_p,
                ft_c_to_p: cis.ft_p_to_c,
                max_pdu_p_to_c: cis.max_pdu_c_to_p as u8,
                max_pdu_c_to_p: cis.max_pdu_p_to_c as u8,
                iso_interval: cis.iso_interval,
            });
        } else {
            println!("skipping out of place packet LL_CIS_IND");
        }
    }

    pub fn ll_cis_terminate_ind(
        &mut self,
        acl_connection_handle: u16,
        packet: llcp::CisTerminateInd,
    ) {
        let cis_connection_handle = self.get_cis_connection_handle(|cis| {
            cis.acl_connection_handle == acl_connection_handle
                && cis.cig_id == packet.get_cig_id()
                && cis.cis_id == packet.get_cis_id()
        });

        if let Some(cis_connection_handle) = cis_connection_handle {
            self.send_hci_event(hci::DisconnectionCompleteBuilder {
                status: hci::ErrorCode::Success,
                connection_handle: cis_connection_handle,
                reason: hci::ErrorCode::from_u8(packet.get_error_code()).unwrap(),
            });
            self.cis_connections.remove(&cis_connection_handle);
        } else {
            println!("skipping out of place packet LL_CIS_TERMINATE_IND");
        }
    }
}

/*
// Time in microseconds taken by the Central or Peripheral to transmit a
// packet containing a CIS PDU with a payload of Max_PDU octets on the PHY
// being used for the CIS.
fn mpt(phy: hci::Phy, max_pdu: u16) -> u16 {
    match phy {
        // Vol 6, Part B § 2.1 Packet Format for the Le Uncoded Phys
        hci::Phy::Le1m => (max_pdu + 10) * 8, // No CTE
        hci::Phy::Le2m => (max_pdu + 11) * 4, // No CTE
        // Vol 6, Part B § 2.2 Packet Format for the Le Coded Phy
        hci::Phy::LeCoded => 80 + 256 + 16 + 24 + (max_pdu * 8 + 24 + 3) * 8, // S=8
    }
}

// Return the range of value ISO_Interval for the required SDU_Interval_C_to_P
// and SDU_Interval_P_to_C.
fn iso_interval(sdu_interval_us: u8,
                max_transport_latency_us: u16,
                rtn: u8,
                max_sdu: u16) -> std::ops::Range<u16> {
    // Determine the minimal value of ISO_Interval based off
    // the minimal Sub_Interval and retransmissions.
    let max_pdu: u16 = 251;
    let sub_interval = 2 * mpt(hci::Phy::Le1m, max_pdu);
    let bn = max_sdu / max_pdu;
}
*/
