#include "callbacks.h"

#include "stack/include/bt_hdr.h"
#include "stack/include/btu.h"
#include "stack/include/l2c_api.h"

std::unique_ptr<tL2CAP_APPL_INFO> prepare_p_cb_info(tL2CA_CONNECT_IND_CB* incoming_connection_handler) {
  auto out = std::make_unique<tL2CAP_APPL_INFO>();
  out->pL2CA_ConnectInd_Cb = incoming_connection_handler;
  return out;
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

void initialize_l2cap_tx_on_main_thread(EventChannel& tx) {
  do_in_main_thread(FROM_HERE, base::Bind(initialize_l2cap_tx, std::ref(tx)));
}