/******************************************************************************
 *
 *  Copyright 2005-2016 Broadcom Corporation
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

/******************************************************************************
 *
 *  This file contains compile-time configurable constants for advanced
 *  audio/video
 *
 ******************************************************************************/

#include "bta/include/bta_av_cfg.h"

#include <cstdint>

#include "bta/include/bta_av_api.h"
#include "internal_include/bt_target.h"
#include "stack/include/avrc_api.h"

#ifndef BTA_AV_RC_COMP_ID
#define BTA_AV_RC_COMP_ID AVRC_CO_GOOGLE
#endif

#ifndef BTA_AV_RC_PASS_RSP_CODE
#define BTA_AV_RC_PASS_RSP_CODE AVRC_RSP_NOT_IMPL
#endif

const uint32_t bta_av_meta_caps_co_ids[] = {AVRC_CO_METADATA, AVRC_CO_BROADCOM};

/* AVRCP supported categories */
#define BTA_AV_RC_SUPF_CT (AVRC_SUPF_CT_CAT2)
#define BTA_AVK_RC_SUPF_CT_V15 (AVRC_SUPF_CT_CAT1 | AVRC_SUPF_CT_BROWSE)

#define BTA_AVK_RC_SUPF_TG (AVRC_SUPF_TG_CAT2)

/* Added to modify
 *	1. flush timeout
 *	2. Remove Group navigation support in SupportedFeatures
 *	3. GetCapabilities supported event_ids list
 *	4. GetCapabilities supported event_ids count
*/

/* Note: Android doesnt support AVRC_SUPF_TG_GROUP_NAVI  */
/* Note: if AVRC_SUPF_TG_GROUP_NAVI is set, bta_av_cfg.avrc_group should be true
 */
#ifndef BTA_AV_RC_SUPF_TG
#define BTA_AV_RC_SUPF_TG                          \
  (AVRC_SUPF_TG_CAT1 | AVRC_SUPF_TG_MULTI_PLAYER | \
   AVRC_SUPF_TG_BROWSE) /* TODO: | AVRC_SUPF_TG_APP_SETTINGS) */
#endif
/*
 * If the number of event IDs is changed in this array, BTA_AV_NUM_RC_EVT_IDS
 * also needs to be changed.
 */
const uint8_t bta_av_meta_caps_evt_ids[] = {
    AVRC_EVT_PLAY_STATUS_CHANGE, AVRC_EVT_TRACK_CHANGE,
    AVRC_EVT_PLAY_POS_CHANGED,   AVRC_EVT_AVAL_PLAYERS_CHANGE,
    AVRC_EVT_ADDR_PLAYER_CHANGE, AVRC_EVT_UIDS_CHANGE,
    AVRC_EVT_NOW_PLAYING_CHANGE,
    /* TODO: Add support for these events
    AVRC_EVT_APP_SETTING_CHANGE,
    */
};

#ifndef BTA_AV_NUM_RC_EVT_IDS
#define BTA_AV_NUM_RC_EVT_IDS \
  (sizeof(bta_av_meta_caps_evt_ids) / sizeof(bta_av_meta_caps_evt_ids[0]))
#endif /* BTA_AV_NUM_RC_EVT_IDS */
const uint8_t* get_bta_avk_meta_caps_evt_ids() {
  if (avrcp_absolute_volume_is_enabled()) {
    static const uint8_t bta_avk_meta_caps_evt_ids[] = {
        AVRC_EVT_VOLUME_CHANGE,
    };
    return bta_avk_meta_caps_evt_ids;
  } else {
    return {};
  }
}

// These are the only events used with AVRCP1.3
const uint8_t bta_av_meta_caps_evt_ids_avrcp13[] = {
    AVRC_EVT_PLAY_STATUS_CHANGE, AVRC_EVT_TRACK_CHANGE,
    AVRC_EVT_PLAY_POS_CHANGED,
};

#ifndef BTA_AV_NUM_RC_EVT_IDS_AVRCP13
#define BTA_AV_NUM_RC_EVT_IDS_AVRCP13         \
  (sizeof(bta_av_meta_caps_evt_ids_avrcp13) / \
   sizeof(bta_av_meta_caps_evt_ids_avrcp13[0]))
#endif /* BTA_AV_NUM_RC_EVT_IDS_AVRCP13 */

/* This configuration to be used when we are Src + TG + CT( only for abs vol) */
extern const BtaAvConfig bta_av_cfg = {
    AVRC_CO_METADATA, /* AVRCP Company ID */

    BTA_AV_RC_SUPF_CT, /* AVRCP controller categories */
    BTA_AV_RC_SUPF_TG, /* AVRCP target categories */
    6,                 /* AVDTP audio channel max data queue size */
    false,             /* true, to accept AVRC 1.3 group nevigation command */
    2,                 /* company id count in p_meta_co_ids */
    BTA_AV_NUM_RC_EVT_IDS,    /* event id count in p_meta_evt_ids */
    BTA_AV_RC_PASS_RSP_CODE,  /* the default response code for pass
                                 through commands */
    bta_av_meta_caps_co_ids,  /* the metadata Get Capabilities response
                                 for company id */
    bta_av_meta_caps_evt_ids, /* the the metadata Get Capabilities
                                 response for event id */
};

/* This configuration to be used when we are Sink + CT + TG( only for abs vol)
 */

const BtaAvConfig get_bta_avk_cfg() {
  static const BtaAvConfig bta_avk_cfg = {
      AVRC_CO_METADATA,       /* AVRCP Company ID */
      BTA_AVK_RC_SUPF_CT_V15, /* AVRCP controller categories */
      BTA_AVK_RC_SUPF_TG,     /* AVRCP target categories */
      6,                      /* AVDTP audio channel max data queue size */
      false, /* true, to accept AVRC 1.3 group nevigation command */
      2,     /* company id count in p_meta_co_ids */
      (uint8_t)(avrcp_absolute_volume_is_enabled()
                    ? 1
                    : 0),              /* event id count in p_meta_evt_ids */
      BTA_AV_RC_PASS_RSP_CODE,         /* the default response code for pass
                                          through commands */
      bta_av_meta_caps_co_ids,         /* the metadata Get Capabilities response
                                          for company id */
      get_bta_avk_meta_caps_evt_ids(), /* the metadata Get Capabilities response
                                          for event id */
  };
  return bta_avk_cfg;
}

/* This configuration to be used when we are using AVRCP1.3 */
extern const BtaAvConfig bta_av_cfg_compatibility = {
    AVRC_CO_METADATA, /* AVRCP Company ID */

    BTA_AV_RC_SUPF_CT, /* AVRCP controller categories */
    AVRC_SUPF_TG_CAT1, /* Only support CAT1 for AVRCP1.3 */
    6,                 /* AVDTP audio channel max data queue size */
    false,             /* true, to accept AVRC 1.3 group nevigation command */
    2,                 /* company id count in p_meta_co_ids */
    BTA_AV_NUM_RC_EVT_IDS_AVRCP13,    /* event id count for AVRCP1.3 */
    BTA_AV_RC_PASS_RSP_CODE,          /* the default response code for pass
                                         through commands */
    bta_av_meta_caps_co_ids,          /* the metadata Get Capabilities response
                                         for company id */
    bta_av_meta_caps_evt_ids_avrcp13, /* the the metadata Get Capabilities
                                         response for event id, compatible
                                         with AVRCP1.3 */
};

BtaAvConfig p_bta_av_cfg;

const uint16_t bta_av_rc_id[] = {
    0x0000, /* bit mask: 0=SELECT, 1=UP, 2=DOWN, 3=LEFT,
                         4=RIGHT, 5=RIGHT_UP, 6=RIGHT_DOWN, 7=LEFT_UP,
                         8=LEFT_DOWN, 9=ROOT_MENU, 10=SETUP_MENU, 11=CONT_MENU,
                         12=FAV_MENU, 13=EXIT */

    0, /* not used */

    0x0000, /* bit mask: 0=0, 1=1, 2=2, 3=3,
                         4=4, 5=5, 6=6, 7=7,
                         8=8, 9=9, 10=DOT, 11=ENTER,
                         12=CLEAR */

    0x0000, /* bit mask: 0=CHAN_UP, 1=CHAN_DOWN, 2=PREV_CHAN, 3=SOUND_SEL,
                         4=INPUT_SEL, 5=DISP_INFO, 6=HELP, 7=PAGE_UP,
                         8=PAGE_DOWN */

/* btui_app provides an example of how to leave the decision of rejecting a
 command or not
 * based on which media player is currently addressed (this is only applicable
 for AVRCP 1.4 or later)
 * If the decision is per player for a particular rc_id, the related bit is
 clear (not set)
 * bit mask: 0=POWER, 1=VOL_UP, 2=VOL_DOWN, 3=MUTE, 4=PLAY, 5=STOP,
             6=PAUSE, 7=RECORD, 8=REWIND, 9=FAST_FOR, 10=EJECT, 11=FORWARD,
             12=BACKWARD */
#if (BTA_AV_RC_PASS_RSP_CODE == AVRC_RSP_INTERIM)
    0x0070, /* PLAY | STOP | PAUSE */
#else       /* BTA_AV_RC_PASS_RSP_CODE != AVRC_RSP_INTERIM */
    0x1b7E, /* PLAY | STOP | PAUSE | FF | RW | VOL_UP | VOL_DOWN | MUTE | FW |
               BACK */
#endif /* BTA_AV_RC_PASS_RSP_CODE */

    0x0000, /* bit mask: 0=ANGLE, 1=SUBPICT */

    0, /* not used */

    0x0000 /* bit mask: 0=not used, 1=F1, 2=F2, 3=F3,
                        4=F4, 5=F5 */
};

#if (BTA_AV_RC_PASS_RSP_CODE == AVRC_RSP_INTERIM)
const uint16_t bta_av_rc_id_ac[] = {
    0x0000, /* bit mask: 0=SELECT, 1=UP, 2=DOWN, 3=LEFT,
                         4=RIGHT, 5=RIGHT_UP, 6=RIGHT_DOWN,
               7=LEFT_UP,
                         8=LEFT_DOWN, 9=ROOT_MENU, 10=SETUP_MENU,
               11=CONT_MENU,
                         12=FAV_MENU, 13=EXIT */

    0, /* not used */

    0x0000, /* bit mask: 0=0, 1=1, 2=2, 3=3,
                         4=4, 5=5, 6=6, 7=7,
                         8=8, 9=9, 10=DOT, 11=ENTER,
                         12=CLEAR */

    0x0000, /* bit mask: 0=CHAN_UP, 1=CHAN_DOWN, 2=PREV_CHAN,
               3=SOUND_SEL,
                         4=INPUT_SEL, 5=DISP_INFO, 6=HELP,
               7=PAGE_UP,
                         8=PAGE_DOWN */

    /* btui_app provides an example of how to leave the decision of
     * rejecting a command or not
     * based on which media player is currently addressed (this is
     * only applicable for AVRCP 1.4 or later)
     * If the decision is per player for a particular rc_id, the
     * related bit is set */
    0x1800, /* bit mask: 0=POWER, 1=VOL_UP, 2=VOL_DOWN, 3=MUTE,
                         4=PLAY, 5=STOP, 6=PAUSE, 7=RECORD,
                         8=REWIND, 9=FAST_FOR, 10=EJECT, 11=FORWARD,
                         12=BACKWARD */

    0x0000, /* bit mask: 0=ANGLE, 1=SUBPICT */

    0, /* not used */

    0x0000 /* bit mask: 0=not used, 1=F1, 2=F2, 3=F3,
                        4=F4, 5=F5 */
};
uint16_t* p_bta_av_rc_id_ac = (uint16_t*)bta_av_rc_id_ac;
#else
uint16_t* p_bta_av_rc_id_ac = NULL;
#endif

uint16_t* p_bta_av_rc_id = (uint16_t*)bta_av_rc_id;

bool BtaAvConfig::operator==(const BtaAvConfig& other) const {
  return (
      company_id == other.company_id && avrc_ct_cat == other.avrc_ct_cat &&
      avrc_tg_cat == other.avrc_tg_cat && audio_mqs == other.audio_mqs &&
      avrc_group == other.avrc_group && num_co_ids == other.num_co_ids &&
      num_evt_ids == other.num_evt_ids && rc_pass_rsp == other.rc_pass_rsp &&
      std::equal(p_meta_co_ids, p_meta_co_ids + num_co_ids, other.p_meta_co_ids,
                 other.p_meta_co_ids + other.num_co_ids) &&
      std::equal(p_meta_evt_ids, p_meta_evt_ids + num_evt_ids,
                 other.p_meta_evt_ids,
                 other.p_meta_evt_ids + other.num_evt_ids));
}

const BtaAvConfig BtaAvCfgFactory::createCustomConfig(
    const bool source_enabled, const bool sink_enabled,
    const uint16_t profile_version) {
  uint16_t avrc_ct_cat = 0;
  uint16_t avrc_tg_cat = 0;
  std::vector<uint8_t> events;
  size_t total_events = 0;
  if (source_enabled) {
    total_events += 3;
    if (profile_version != AVRC_REV_1_3) {
      total_events += 4;
    }
  }
  if (sink_enabled) {
    total_events += 1;
  }
  uint8_t eventtt[total_events];
  size_t index = 0;
  if (source_enabled) {
    avrc_ct_cat |= BTA_AV_RC_SUPF_CT;
    avrc_tg_cat |= AVRC_SUPF_TG_CAT1;

    events.push_back(AVRC_EVT_PLAY_STATUS_CHANGE);
    eventtt[index++] = AVRC_EVT_PLAY_STATUS_CHANGE;
    events.push_back(AVRC_EVT_TRACK_CHANGE);
    eventtt[index++] = AVRC_EVT_TRACK_CHANGE;
    events.push_back(AVRC_EVT_PLAY_POS_CHANGED);
    eventtt[index++] = AVRC_EVT_PLAY_POS_CHANGED;
    if (profile_version != AVRC_REV_1_3) {
      avrc_tg_cat |= BTA_AV_RC_SUPF_TG;
      events.push_back(AVRC_EVT_AVAL_PLAYERS_CHANGE);
      eventtt[index++] = AVRC_EVT_AVAL_PLAYERS_CHANGE;
      events.push_back(AVRC_EVT_ADDR_PLAYER_CHANGE);
      eventtt[index++] = AVRC_EVT_ADDR_PLAYER_CHANGE;
      events.push_back(AVRC_EVT_UIDS_CHANGE);
      eventtt[index++] = AVRC_EVT_UIDS_CHANGE;
      events.push_back(AVRC_EVT_NOW_PLAYING_CHANGE);
      eventtt[index++] = AVRC_EVT_NOW_PLAYING_CHANGE;
      total_events += 4;
    }
  }
  if (sink_enabled) {
    avrc_ct_cat |= BTA_AVK_RC_SUPF_CT_V15;
    avrc_tg_cat |= BTA_AVK_RC_SUPF_TG;
    if (avrcp_absolute_volume_is_enabled()) {
      events.push_back(AVRC_EVT_VOLUME_CHANGE);
      eventtt[index++] = AVRC_EVT_VOLUME_CHANGE;
    }
  }
  return BtaAvConfig::Builder()
      .setCompanyId(AVRC_CO_METADATA)
      .setAvrcCtCat(avrc_ct_cat)
      .setAvrcTgCat(avrc_tg_cat)
      .setAudioMqs(6)
      .setAvrcGroup(false)
      .setNumCoIds(sizeof(bta_av_meta_caps_co_ids) /
                   sizeof(bta_av_meta_caps_co_ids[0]))
      .setNumEvtIds(events.size())
      .setRcPassRsp(BTA_AV_RC_PASS_RSP_CODE)
      .setMetaCoIds(bta_av_meta_caps_co_ids)
      .setMetaEvtIds(events.data())
      .setMetaEvtIds1(eventtt)
      .build();
}
