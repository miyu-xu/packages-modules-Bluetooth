/*
 * Copyright (c) 2019, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "bta_ag_swb.h"

#include <unistd.h>

#include "bta/ag/bta_ag_int.h"
#include "common/init_flags.h"
#include "device/include/interop.h"
#include "internal_include/bt_trace.h"
#include "stack/include/btm_api_types.h"
#include "utl.h"

static bool aptx_swb_codec_status;

bt_status_t enable_aptx_swb_codec(bool enable) {
  if (bluetooth::common::init_flags::aptx_voice_is_enabled()) {
    LOG_INFO("%d", enable);
    aptx_swb_codec_status = enable;
    return BT_STATUS_SUCCESS;
  }
  return BT_STATUS_FAIL;
}

bool get_aptx_swb_codec_status() {
  if (bluetooth::common::init_flags::aptx_voice_is_enabled()) {
    return aptx_swb_codec_status;
  }
  return false;
}

void bta_ag_swb_handle_vs_at_events(tBTA_AG_SCB* p_scb, uint16_t cmd,
                                    int16_t int_arg, tBTA_AG_VAL* val) {
  APPL_TRACE_DEBUG("%s: p_scb : %x cmd : %d", __func__, p_scb, cmd);
  switch (cmd) {
    case BTA_AG_AT_QAC_EVT:
      if (!get_aptx_swb_codec_status()) {
        bta_ag_send_qac(p_scb, NULL);
        break;
      }
      p_scb->codec_updated = true;
      if (p_scb->peer_codecs & BTA_AG_SCO_SWB_SETTINGS_Q0_MASK) {
        p_scb->sco_codec = BTA_AG_SCO_SWB_SETTINGS_Q0;
      } else if (p_scb->peer_codecs & BTM_SCO_CODEC_MSBC) {
        p_scb->sco_codec = UUID_CODEC_MSBC;
      }
      bta_ag_send_qac(p_scb, NULL);
      APPL_TRACE_DEBUG("Received AT+QAC, updating sco codec to SWB: %d",
                       p_scb->sco_codec);
      val->num = p_scb->peer_codecs;
      break;
    case BTA_AG_AT_QCS_EVT: {
      tBTA_AG_PEER_CODEC codec_type, codec_sent;
      alarm_cancel(p_scb->codec_negotiation_timer);

      switch (int_arg) {
        case BTA_AG_SCO_SWB_SETTINGS_Q0:
          codec_type = BTA_AG_SCO_SWB_SETTINGS_Q0;
          break;
        case BTA_AG_SCO_SWB_SETTINGS_Q1:
          codec_type = BTA_AG_SCO_SWB_SETTINGS_Q1;
          break;
        case BTA_AG_SCO_SWB_SETTINGS_Q2:
          codec_type = BTA_AG_SCO_SWB_SETTINGS_Q2;
          break;
        case BTA_AG_SCO_SWB_SETTINGS_Q3:
          codec_type = BTA_AG_SCO_SWB_SETTINGS_Q3;
          break;
        default:
          APPL_TRACE_ERROR("Unknown codec_uuid %d", int_arg);
          p_scb->is_swb_codec = false;
          codec_type = BTM_SCO_CODEC_MSBC;
          p_scb->codec_fallback = true;
          p_scb->sco_codec = BTM_SCO_CODEC_MSBC;
          break;
      }

      if (p_scb->codec_fallback)
        codec_sent = BTM_SCO_CODEC_MSBC;
      else
        codec_sent = p_scb->sco_codec;

      if (codec_type == codec_sent)
        bta_ag_sco_codec_nego(p_scb, true);
      else
        bta_ag_sco_codec_nego(p_scb, false);

      /* send final codec info to callback */
      val->num = codec_sent;
      break;
    }
  }
}

tBTA_AG_PEER_CODEC bta_ag_parse_qac(tBTA_AG_SCB* p_scb, char* p_s) {
  tBTA_AG_PEER_CODEC retval = BTM_SCO_CODEC_NONE;
  uint16_t codec_modes;
  bool cont = false; /* Continue processing */
  char* p;

  while (p_s) {
    /* skip to comma delimiter */
    for (p = p_s; *p != ',' && *p != 0; p++)
      ;

    /* get integre value */
    if (*p != 0) {
      *p = 0;
      cont = true;
    } else
      cont = false;

    codec_modes = utl_str2int(p_s);
    switch (codec_modes) {
      case BTA_AG_SCO_SWB_SETTINGS_Q0:
        retval |= BTA_AG_SCO_SWB_SETTINGS_Q0_MASK;
        break;
      case BTA_AG_SCO_SWB_SETTINGS_Q1:
        retval |= BTA_AG_SCO_SWB_SETTINGS_Q1_MASK;
        break;
      case BTA_AG_SCO_SWB_SETTINGS_Q2:
        retval |= BTA_AG_SCO_SWB_SETTINGS_Q2_MASK;
        break;
      case BTA_AG_SCO_SWB_SETTINGS_Q3:
        retval |= BTA_AG_SCO_SWB_SETTINGS_Q3_MASK;
        break;
      default:
        APPL_TRACE_ERROR("Unknown Codec UUID(%d) received", codec_modes);
        break;
    }

    if (cont)
      p_s = p + 1;
    else
      break;
  }

  return (retval);
}
