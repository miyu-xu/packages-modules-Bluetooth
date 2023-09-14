/******************************************************************************
 *
 *  Copyright 1999-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#ifndef BTM_BLE_INT_TYPES_H
#define BTM_BLE_INT_TYPES_H

#include "btm_ble_const.h"
#include "osi/include/alarm.h"
#include "stack/btm/neighbor_inquiry.h"
#include "stack/include/btm_ble_api_types.h"
#include "types/raw_address.h"

typedef struct {
  uint16_t data_mask;
  uint8_t* p_flags;
  uint8_t ad_data[BTM_BLE_AD_DATA_LEN];
  uint8_t* p_pad;
} tBTM_BLE_LOCAL_ADV_DATA;

#define BTM_BLE_ISVALID_PARAM(x, min, max) \
  (((x) >= (min) && (x) <= (max)) || ((x) == BTM_BLE_CONN_PARAM_UNDEF))

typedef struct {
  uint16_t discoverable_mode;
  uint16_t connectable_mode;
  uint32_t scan_window;
  uint32_t scan_interval;
  uint8_t scan_type;             /* current scan type: active or passive */

  tBTM_BLE_AFP afp; /* advertising filter policy */
  tBTM_BLE_SFP sfp; /* scanning filter policy */

  tBLE_ADDR_TYPE adv_addr_type;
  uint8_t evt_type;

  uint8_t adv_mode;
  void enable_advertising_mode() { adv_mode = BTM_BLE_ADV_ENABLE; }
  void disable_advertising_mode() { adv_mode = BTM_BLE_ADV_DISABLE; }
  bool is_advertising_mode_enabled() const {
    return (adv_mode == BTM_BLE_ADV_ENABLE);
  }

  tBLE_BD_ADDR direct_bda;
  tBTM_BLE_EVT directed_conn;
  bool fast_adv_on;
  alarm_t* fast_adv_timer;

  /* inquiry BD addr database */
  tBTM_BLE_LOCAL_ADV_DATA adv_data;
  tBTM_BLE_ADV_CHNL_MAP adv_chnl_map;

  alarm_t* inquiry_timer;
  bool scan_rsp;
  uint8_t state; /* Current state that the inquiry process is in */
} tBTM_BLE_INQ_CB;

/* random address resolving complete callback */
typedef void(tBTM_BLE_RESOLVE_CBACK)(void* match_rec, void* p);

typedef void(tBTM_BLE_ADDR_CBACK)(const RawAddress& static_random, void* p);

/* random address management control block */
typedef struct {
  tBLE_ADDR_TYPE own_addr_type; /* local device LE address type */
  RawAddress private_addr;
  alarm_t* refresh_raddr_timer;
} tBTM_LE_RANDOM_CB;

typedef struct {
  RawAddress* resolve_q_random_pseudo{nullptr};
  uint8_t* resolve_q_action{nullptr};
  uint8_t q_next;
  uint8_t q_pending;
} tBTM_BLE_RESOLVE_Q;

typedef struct {
 private:
  uint8_t scan_activity_; /* LE scan activity mask */

 public:
  bool is_ble_inquiry_active() const {
    return (scan_activity_ & kBTM_BLE_INQUIRY_ACTIVE);
  }
  bool is_ble_observe_active() const {
    return (scan_activity_ & kBTM_BLE_OBSERVE_ACTIVE);
  }

  void set_ble_inquiry_active() { scan_activity_ |= kBTM_BLE_INQUIRY_ACTIVE; }
  void set_ble_observe_active() { scan_activity_ |= kBTM_BLE_OBSERVE_ACTIVE; }

  void reset_ble_inquiry() { scan_activity_ &= ~kBTM_BLE_INQUIRY_ACTIVE; }
  void reset_ble_observe() { scan_activity_ &= ~kBTM_BLE_OBSERVE_ACTIVE; }

  bool is_ble_scan_active() const {
    return (is_ble_inquiry_active() || is_ble_observe_active());
  }

  /*****************************************************
  **      BLE Inquiry
  *****************************************************/
  tBTM_BLE_INQ_CB inq_var;

  /* observer callback and timer */
  tBTM_INQ_RESULTS_CB* p_obs_results_cb;
  tBTM_CMPL_CB* p_obs_cmpl_cb;
  alarm_t* observer_timer;

  /* opportunistic observer */
  tBTM_INQ_RESULTS_CB* p_opportunistic_obs_results_cb;

  /* target announcement observer */
  tBTM_INQ_RESULTS_CB* p_target_announcement_obs_results_cb;

 private:
  enum : uint8_t { /* BLE connection state */
                   BLE_CONN_IDLE = 0,
                   BLE_CONNECTING = 2,
                   BLE_CONN_CANCEL = 3,
  } conn_state_{BLE_CONN_IDLE};

 public:
  bool is_connection_state_idle() const { return conn_state_ == BLE_CONN_IDLE; }
  bool is_connection_state_connecting() const {
    return conn_state_ == BLE_CONNECTING;
  }
  bool is_connection_state_cancelled() const {
    return conn_state_ == BLE_CONN_CANCEL;
  }
  void set_connection_state_idle() { conn_state_ = BLE_CONN_IDLE; }
  void set_connection_state_connecting() { conn_state_ = BLE_CONNECTING; }
  void set_connection_state_cancelled() { conn_state_ = BLE_CONN_CANCEL; }

  /* random address management control block */
  tBTM_LE_RANDOM_CB addr_mgnt_cb;

  tBTM_PRIVACY_MODE privacy_mode;    /* privacy mode */
  uint8_t resolving_list_avail_size; /* resolving list available size */
  tBTM_BLE_RESOLVE_Q resolving_list_pend_q; /* Resolving list queue */
  tBTM_BLE_RL_STATE suspended_rl_state;     /* Suspended resolving list state */
  /* IRK list availability mask, up to max entry bits */
  uint8_t* irk_list_mask{nullptr};
  tBTM_BLE_RL_STATE rl_state; /* Resolving list state */

  /* current BLE link state */
  tBTM_BLE_STATE_MASK cur_states; /* bit mask of tBTM_BLE_STATE */

  uint8_t link_count[kCentralAndPeripheralCount]; /* total link count central
                                                     and peripheral*/
} tBTM_BLE_CB;

#endif  // BTM_BLE_INT_TYPES_H
