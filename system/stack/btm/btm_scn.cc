/*
 * Copyright 2020 The Android Open Source Project
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

#define LOG_TAG "btm"

#include <cstdint>
#include "stack/btm/btm_int_types.h"  // tBTM_CB
#include "stack/include/rfcdefs.h"    // PORT_MAX_RFC_PORTS

extern tBTM_CB btm_cb;

/*******************************************************************************
 *
 * Function         BTM_AllocateSCN
 *
 * Description      Look through the Server Channel Numbers for a free one.
 *
 * Returns          Allocated SCN number (range of [2,PORT_MAX_RFC_PORTS) (1 is
 *                  reserved for HFP)) or 0 if none.
 *
 ******************************************************************************/
uint8_t BTM_AllocateSCN(void) {
  BTM_TRACE_DEBUG("BTM_AllocateSCN");

  uint8_t scn = btm_cb.next_available_scn;
  do {
    uint8_t scn_idx = scn - 1;
    if (!btm_cb.btm_scn[scn_idx]) {
      btm_cb.btm_scn[scn_idx] = true;
      btm_cb.next_available_scn = scn + 1;
      return scn;
    }
    scn++;

    // SCN can be allocated in the range of [1, PORT_MAX_RFC_PORTS).
    if (scn >= PORT_MAX_RFC_PORTS) {
      // stack reserves scn 1 for HFP, HSP we still do the
      // correct way. Hence, we start cycling from scn 2.
      scn = 2;
    }
  } while (scn != btm_cb.next_available_scn);

  return (0); /* No free ports */
}

/*******************************************************************************
 *
 * Function         BTM_TryAllocateSCN
 *
 * Description      Try to allocate a fixed server channel
 *
 * Returns          Returns true if server channel was available
 *
 ******************************************************************************/

bool BTM_TryAllocateSCN(uint8_t scn) {
  /* Make sure we don't exceed max scn range.
   * Stack reserves scn 1 for HFP and HSP
   */
  if ((scn > RFCOMM_MAX_SCN) || (scn == 1) || (scn == 0)) return false;

  /* check if this scn is available */
  if (!btm_cb.btm_scn[scn - 1]) {
    btm_cb.btm_scn[scn - 1] = true;
    return true;
  }

  return (false); /* scn was busy */
}

/*******************************************************************************
 *
 * Function         BTM_FreeSCN
 *
 * Description      Free the specified SCN.
 *
 * Returns          true or false
 *
 ******************************************************************************/
bool BTM_FreeSCN(uint8_t scn) {
  BTM_TRACE_DEBUG("BTM_FreeSCN ");
  /* Since this isn't used by HFP, this function will only free valid SCNs
   * that aren't reserved for HFP, which is range [2, RFCOMM_MAX_SCN].
   */
  if (scn < RFCOMM_MAX_SCN && scn > 1) {
    btm_cb.btm_scn[scn - 1] = false;
    return (true);
  } else {
    return (false); /* Illegal SCN passed in */
  }
}
