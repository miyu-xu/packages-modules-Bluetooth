/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************************************
 *
 *  This file contains only Bluetooth Manager (BTM) constants and defines
 *without depenencies
 *
 **************************************************************************************************/

#ifndef BTM_BLE_CONST_H
#define BTM_BLE_CONST_H

#include <stdint.h>

/* scanning enable status */
#define BTM_BLE_SCAN_ENABLE 0x01
#define BTM_BLE_SCAN_DISABLE 0x00

/* advertising enable status */
#define BTM_BLE_ADV_ENABLE 0x01
#define BTM_BLE_ADV_DISABLE 0x00

#define BTM_BLE_AD_DATA_LEN 31

#define BTM_BLE_DUPLICATE_ENABLE 1
#define BTM_BLE_DUPLICATE_DISABLE 0

/* Interval(scan_int) = 11.25 ms= 0x0010 * 0.625 ms */
#define BTM_BLE_GAP_DISC_SCAN_INT 18
/* scan_window = 11.25 ms= 0x0010 * 0.625 ms */
#define BTM_BLE_GAP_DISC_SCAN_WIN 18
/* Tgap(gen_disc) = 1.28 s= 512 * 0.625 ms */
#define BTM_BLE_GAP_ADV_INT 512
/* Tgap(lim_timeout) = 180s max */
#define BTM_BLE_GAP_LIM_TIMEOUT_MS (180 * 1000)
/* Interval(scan_int) = 5s= 8000 * 0.625 ms */
#define BTM_BLE_LOW_LATENCY_SCAN_INT 8000
/* scan_window = 5s= 8000 * 0.625 ms */
#define BTM_BLE_LOW_LATENCY_SCAN_WIN 8000

/* TGAP(adv_fast_interval1) = 30(used) ~ 60 ms  = 48 *0.625 */
#define BTM_BLE_GAP_ADV_FAST_INT_1 48
/* TGAP(adv_fast_interval2) = 100(used) ~ 150 ms = 160 * 0.625 ms */
#define BTM_BLE_GAP_ADV_FAST_INT_2 160
/* Tgap(adv_slow_interval) = 1.28 s= 512 * 0.625 ms */
#define BTM_BLE_GAP_ADV_SLOW_INT 2048
/* Tgap(dir_conn_adv_int_max) = 500 ms = 800 * 0.625 ms */
#define BTM_BLE_GAP_ADV_DIR_MAX_INT 800
/* Tgap(dir_conn_adv_int_min) = 250 ms = 400 * 0.625 ms */
#define BTM_BLE_GAP_ADV_DIR_MIN_INT 400

#define BTM_BLE_GAP_FAST_ADV_TIMEOUT_MS (30 * 1000)

typedef enum : uint8_t {
  BTM_BLE_SEC_REQ_ACT_NONE = 0,
  /* encrypt the link using current key or key refresh */
  BTM_BLE_SEC_REQ_ACT_ENCRYPT = 1,
  BTM_BLE_SEC_REQ_ACT_PAIR = 2,
  /* discard the sec request while encryption is started but not completed */
  BTM_BLE_SEC_REQ_ACT_DISCARD = 3,
} tBTM_BLE_SEC_REQ_ACT;

#ifndef CASE_RETURN_TEXT
#define CASE_RETURN_TEXT(code) \
  case code:                   \
    return #code
#endif

inline std::string btm_ble_sec_req_act_text(
    const tBTM_BLE_SEC_REQ_ACT& action) {
  switch (action) {
    CASE_RETURN_TEXT(BTM_BLE_SEC_REQ_ACT_NONE);
    CASE_RETURN_TEXT(BTM_BLE_SEC_REQ_ACT_ENCRYPT);
    CASE_RETURN_TEXT(BTM_BLE_SEC_REQ_ACT_PAIR);
    CASE_RETURN_TEXT(BTM_BLE_SEC_REQ_ACT_DISCARD);
  }
}

#undef CASE_RETURN_TEXT

#define BTM_VSC_CHIP_CAPABILITY_L_VERSION 55
#define BTM_VSC_CHIP_CAPABILITY_M_VERSION 95
#define BTM_VSC_CHIP_CAPABILITY_S_VERSION 98

/* acceptlist using state as a bit mask */
constexpr uint8_t BTM_BLE_WL_IDLE = 0;
constexpr uint8_t BTM_BLE_ACCEPTLIST_INIT = 1;

/* resolving list using state as a bit mask */
enum : uint8_t {
  BTM_BLE_RL_IDLE = 0,
  BTM_BLE_RL_INIT = (1 << 0),
  BTM_BLE_RL_SCAN = (1 << 1),
  BTM_BLE_RL_ADV = (1 << 2),
};
typedef uint8_t tBTM_BLE_RL_STATE;

typedef struct {
  void* p_param;
} tBTM_BLE_CONN_REQ;

/* LE state request */
#define BTM_BLE_STATE_INVALID 0
#define BTM_BLE_STATE_INIT 2
#define BTM_BLE_STATE_MAX 11

#define BTM_BLE_STATE_CONN_ADV_BIT 0x0001
#define BTM_BLE_STATE_INIT_BIT 0x0002
#define BTM_BLE_STATE_CENTRAL_BIT 0x0004
#define BTM_BLE_STATE_PERIPHERAL_BIT 0x0008
#define BTM_BLE_STATE_LO_DUTY_DIR_ADV_BIT 0x0010
#define BTM_BLE_STATE_HI_DUTY_DIR_ADV_BIT 0x0020
#define BTM_BLE_STATE_NON_CONN_ADV_BIT 0x0040
#define BTM_BLE_STATE_PASSIVE_SCAN_BIT 0x0080
#define BTM_BLE_STATE_ACTIVE_SCAN_BIT 0x0100
#define BTM_BLE_STATE_SCAN_ADV_BIT 0x0200
typedef uint16_t tBTM_BLE_STATE_MASK;

#define BTM_BLE_STATE_ALL_MASK 0x03ff
#define BTM_BLE_STATE_ALL_ADV_MASK                                  \
  (BTM_BLE_STATE_CONN_ADV_BIT | BTM_BLE_STATE_LO_DUTY_DIR_ADV_BIT | \
   BTM_BLE_STATE_HI_DUTY_DIR_ADV_BIT | BTM_BLE_STATE_SCAN_ADV_BIT)
#define BTM_BLE_STATE_ALL_CONN_MASK \
  (BTM_BLE_STATE_CENTRAL_BIT | BTM_BLE_STATE_PERIPHERAL_BIT)

/* BLE privacy mode */
#define BTM_PRIVACY_NONE 0 /* BLE no privacy */
#define BTM_PRIVACY_1_1 1  /* BLE privacy 1.1, do not support privacy 1.0 */
#define BTM_PRIVACY_1_2 2  /* BLE privacy 1.2 */
#define BTM_PRIVACY_MIXED \
  3 /* BLE privacy mixed mode, broadcom propietary mode */
typedef uint8_t tBTM_PRIVACY_MODE;

/* Define BLE Device Management control structure
 */
constexpr uint8_t kBTM_BLE_INQUIRY_ACTIVE = 0x10;
constexpr uint8_t kBTM_BLE_OBSERVE_ACTIVE = 0x80;
constexpr size_t kCentralAndPeripheralCount = 2;

#endif  // BTM_BLE_CONST_H
