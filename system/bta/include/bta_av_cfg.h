#ifndef BTA_AV_CFG_H
#define BTA_AV_CFG_H

#include <avrc_api.h>
#include <btif_av.h>

#include <cstdint>

#include "bta/include/bta_av_api.h"

#ifndef BTA_AV_RC_PASS_RSP_CODE
#define BTA_AV_RC_PASS_RSP_CODE AVRC_RSP_NOT_IMPL
#endif

/* AVRCP Controller and Targer default name */
#ifndef BTA_AV_RC_CT_NAME
#define BTA_AV_RC_CT_NAME "AVRC Controller"
#endif

#ifndef BTA_AV_RC_TG_NAME
#define BTA_AV_RC_TG_NAME "AVRC Target"
#endif

#ifndef BTA_AV_RC_COMP_ID
#define BTA_AV_RC_COMP_ID AVRC_CO_GOOGLE
#endif

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

/* AV configuration structure */
class tBTA_AV_CFG {
 public:
  class Builder;

  tBTA_AV_CFG() {
    company_id = 0;
    avrc_ct_cat = 0;
    avrc_tg_cat = 0;
    audio_mqs = 0;
    avrc_group = false;
    num_co_ids = 0;
    num_evt_ids = 0;
    rc_pass_rsp = 0;
    p_meta_co_ids = {};
    p_meta_evt_ids = {};
  }

  uint32_t getCompanyId() const { return company_id; }

  uint16_t getAvrcCtCat() const { return avrc_ct_cat; }

  uint16_t getAvrcTgCat() const { return avrc_tg_cat; }

  uint16_t getAudioMqs() const { return audio_mqs; }

  bool isAvrcGroup() const { return avrc_group; }

  uint8_t getNumCoIds() const { return num_co_ids; }

  uint8_t getNumEvtIds() const { return num_evt_ids; }

  tBTA_AV_CODE getRcPassRsp() const { return rc_pass_rsp; }

  const uint32_t* getPMetaCoIds() const { return p_meta_co_ids; }

  const uint8_t* getPMetaEvtIds() const { return p_meta_evt_ids; }
  //
  //    const char *getAvrcControllerName() const {
  //        return avrc_controller_name;
  //    }
  //
  //    const char *getAvrcTargetName() const {
  //        return avrc_target_name;
  //    }
 private:
  uint32_t company_id;
  /* AVRCP Company ID */
  uint16_t avrc_ct_cat; /* AVRCP controller categories */
  uint16_t avrc_tg_cat; /* AVRCP target categories */
  uint16_t audio_mqs;   /* AVDTP audio channel max data queue size */
  bool avrc_group;      /* true, to accept AVRC 1.3 group nevigation command */
  uint8_t num_co_ids;   /* company id count in p_meta_co_ids */
  uint8_t num_evt_ids;  /* event id count in p_meta_evt_ids */
  tBTA_AV_CODE
      rc_pass_rsp; /* the default response code for pass through commands */
  const uint32_t*
      p_meta_co_ids; /* the metadata Get Capabilities response for company id */
  const uint8_t* p_meta_evt_ids; /* the the metadata Get Capabilities response
                                  for event id */
  //    char avrc_controller_name[BTA_SERVICE_NAME_LEN]; /* Default AVRCP
  //    controller
  //
  //                                                          char
  //                                                          avrc_target_name[BTA_SERVICE_NAME_LEN];
  //                                                          /* Default AVRCP
  //                                                          target name*/name
  //                                                          */
  friend class Builder;
};
class tBTA_AV_CFG::Builder {
 private:
  tBTA_AV_CFG avConfig;

 public:
  Builder() : avConfig() {
    // Initialize default values if needed
    avConfig.company_id = 0;
    avConfig.avrc_ct_cat = 0;
    avConfig.avrc_tg_cat = 0;
    avConfig.audio_mqs = 0;
    avConfig.avrc_group = false;
    avConfig.num_co_ids = 0;
    avConfig.num_evt_ids = 0;
    avConfig.rc_pass_rsp = 0;  // Set appropriate default value
    avConfig.p_meta_co_ids = nullptr;
    avConfig.p_meta_evt_ids = nullptr;
    //        memset(avConfig.avrc_controller_name, 0, BTA_SERVICE_NAME_LEN);
    //        memset(avConfig.avrc_target_name, 0, BTA_SERVICE_NAME_LEN);
  }

  Builder& setCompanyId(uint32_t companyId) {
    avConfig.company_id = companyId;
    return *this;
  }

  Builder& setAvrcCtCat(uint16_t avrcCtCat) {
    avConfig.avrc_ct_cat = avrcCtCat;
    return *this;
  }

  Builder& setAvrcTgCat(uint16_t avrcTgCat) {
    avConfig.avrc_tg_cat = avrcTgCat;
    return *this;
  }

  Builder& setAudioMqs(uint16_t audioMqs) {
    avConfig.audio_mqs = audioMqs;
    return *this;
  }

  Builder& setAvrcGroup(bool avrcGroup) {
    avConfig.avrc_group = avrcGroup;
    return *this;
  }

  Builder& setNumCoIds(uint8_t numCoIds) {
    avConfig.num_co_ids = numCoIds;
    return *this;
  }

  Builder& setNumEvtIds(uint8_t numEvtIds) {
    avConfig.num_evt_ids = numEvtIds;
    return *this;
  }

  Builder& setRcPassRsp(tBTA_AV_CODE rcPassRsp) {
    avConfig.rc_pass_rsp = rcPassRsp;
    return *this;
  }

  Builder& setMetaCoIds(const uint32_t* metaCoIds) {
    avConfig.p_meta_co_ids = metaCoIds;
    return *this;
  }

  Builder& setMetaEvtIds(const uint8_t* metaEvtIds) {
    avConfig.p_meta_evt_ids = metaEvtIds;
    return *this;
  }

  //    Builder& setAvrcControllerName(const char* avrcControllerName) {
  //        strncpy(avConfig.avrc_controller_name, avrcControllerName,
  //        BTA_SERVICE_NAME_LEN); return *this;
  //    }
  //
  //    Builder& setAvrcTargetName(const char* avrcTargetName) {
  //        strncpy(avConfig.avrc_target_name, avrcTargetName,
  //        BTA_SERVICE_NAME_LEN); return *this;
  //    }
  //
  tBTA_AV_CFG build() const { return avConfig; }
};

class BtaAvCfgFactory {
 public:
  static const tBTA_AV_CFG createCustomConfig(bool source_enabled,
                                              bool sink_enabled,
                                              uint16_t profile_version) {
    const uint32_t bta_av_meta_caps_co_ids[] = {AVRC_CO_METADATA,
                                                AVRC_CO_BROADCOM};
    uint16_t avrc_ct_cat = 0;
    uint16_t avrc_tg_cat = 0;
    std::vector<uint8_t> events;
    if (source_enabled) {
      avrc_ct_cat |= BTA_AV_RC_SUPF_CT;
      avrc_tg_cat |= AVRC_SUPF_TG_CAT1;
      events.push_back(AVRC_EVT_PLAY_STATUS_CHANGE);
      events.push_back(AVRC_EVT_TRACK_CHANGE);
      events.push_back(AVRC_EVT_PLAY_POS_CHANGED);
      if (profile_version != AVRC_REV_1_3) {
        avrc_tg_cat |= BTA_AV_RC_SUPF_TG;
        events.push_back(AVRC_EVT_AVAL_PLAYERS_CHANGE);
        events.push_back(AVRC_EVT_ADDR_PLAYER_CHANGE);
        events.push_back(AVRC_EVT_UIDS_CHANGE);
        events.push_back(AVRC_EVT_NOW_PLAYING_CHANGE);
      }
    }
    if (sink_enabled) {
      avrc_ct_cat |= BTA_AVK_RC_SUPF_CT_V15;
      avrc_tg_cat |= BTA_AVK_RC_SUPF_TG;
      if (avrcp_absolute_volume_is_enabled()) {
        events.push_back(AVRC_EVT_VOLUME_CHANGE);
      }
    }
    return tBTA_AV_CFG::Builder()
        .setCompanyId(AVRC_CO_METADATA)
        .setAvrcCtCat(avrc_ct_cat)
        .setAvrcTgCat(avrc_tg_cat)
        .setAudioMqs(6)
        .setAvrcGroup(false)
        .setNumCoIds(2)
        .setNumEvtIds(events.size())
        .setRcPassRsp(BTA_AV_RC_PASS_RSP_CODE)
        .setMetaCoIds(bta_av_meta_caps_co_ids)
        .setMetaEvtIds(&events[0])
        //                .setAvrcControllerName(BTA_AV_RC_CT_NAME)
        .build();
  }
};

#endif /* BTA_AV_INT_H */
