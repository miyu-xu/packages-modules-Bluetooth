#pragma once

#include <memory>

#include "rust/cxx.h"
#include "src/l2cap/bridge.rs.h"
#include "stack/include/l2c_api.h"

std::unique_ptr<tL2CAP_APPL_INFO> prepare_p_cb_info(tL2CA_CONNECT_IND_CB* incoming_connection_handler);

void L2CA_Register_from_rust(
    uint16_t psm,
    bool enable_snoop,
    tL2CAP_ERTM_INFO* p_ertm_info,
    uint16_t my_mtu,
    uint16_t required_remote_mtu,
    OneshotU16& completion);

void L2CA_Deregister_from_rust(uint16_t psm);

void L2CA_ConnectReq_from_rust(uint16_t psm, const RawAddress& p_bd_addr, OneshotU16& completion);

void L2CA_DisconnectReq_from_rust(uint16_t cid);

void L2CA_DataWrite_from_rust(uint16_t cid, rust::Slice<const uint8_t> data, OneshotU16& completion);

void initialize_l2cap_tx_on_main_thread(EventChannel& tx);
