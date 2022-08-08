#include "callbacks.h"

#include "stack/include/bt_hdr.h"
#include "stack/include/btu.h"
#include "stack/include/l2c_api.h"

// proxies to callbacks, unwrapping structs for consumption in Rust
void incoming_data_handler_proxy(uint16_t local_cid, BT_HDR* data) {
  incoming_data_handler(local_cid, rust::Slice<const uint8_t>((uint8_t*)(data + 1) + data->offset, data->len));
}

void L2CA_Register_in_main_thread(
    uint16_t psm,
    bool enable_snoop,
    tL2CAP_ERTM_INFO* p_ertm_info,
    uint16_t my_mtu,
    uint16_t required_remote_mtu,
    OneshotU16& completion) {
  tL2CAP_APPL_INFO callbacks{};
  callbacks.pL2CA_ConnectInd_Cb = incoming_connection_handler;
  callbacks.pL2CA_ConnectCfm_Cb = outgoing_connection_handler;
  callbacks.pL2CA_DataInd_Cb = incoming_data_handler_proxy;
  callbacks.pL2ca_DisconnectInd_Cb = disconnect_connection_handler;
  auto result = L2CA_Register2(psm, callbacks, enable_snoop, p_ertm_info, my_mtu, required_remote_mtu, 0);
  oneshot_send_u16(completion, result);
}

void L2CA_Register_from_rust(
    uint16_t psm,
    bool enable_snoop,
    tL2CAP_ERTM_INFO* p_ertm_info,
    uint16_t my_mtu,
    uint16_t required_remote_mtu,
    OneshotU16& completion) {
  do_in_main_thread(
      FROM_HERE,
      base::Bind(
          L2CA_Register_in_main_thread,
          psm,
          enable_snoop,
          p_ertm_info,
          my_mtu,
          required_remote_mtu,
          std::ref(completion)));
}

void L2CA_Deregister_from_rust(uint16_t psm) {
  do_in_main_thread(FROM_HERE, base::Bind(L2CA_Deregister, psm));
}

void L2CA_ConnectReq_in_main_thread(uint16_t psm, const RawAddress& p_bd_addr, OneshotU16& completion) {
  auto result = L2CA_ConnectReq(psm, p_bd_addr);
  oneshot_send_u16(completion, result);
}

void L2CA_ConnectReq_from_rust(uint16_t psm, const RawAddress& p_bd_addr, OneshotU16& completion) {
  do_in_main_thread(
      FROM_HERE, base::Bind(L2CA_ConnectReq_in_main_thread, psm, std::ref(p_bd_addr), std::ref(completion)));
}

void L2CA_DisconnectReq_in_main_thread(uint16_t cid) {
  L2CA_DisconnectReq(cid);
}

void L2CA_DisconnectReq_from_rust(uint16_t cid) {
  do_in_main_thread(FROM_HERE, base::Bind(L2CA_DisconnectReq_in_main_thread, cid));
}

void initialize_l2cap_tx_on_main_thread(EventChannel& tx) {
  do_in_main_thread(FROM_HERE, base::Bind(initialize_l2cap_tx, std::ref(tx)));
}
