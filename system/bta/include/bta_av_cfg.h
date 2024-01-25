/*
 * Copyright 2024 The Android Open Source Project
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

#pragma once
#include <avrc_api.h>

#include <cstdint>

#include "bta/include/bta_av_api.h"
#include "btif/include/btif_av.h"

/* AV configuration structure */
class BtaAvConfig {
 public:
  class Builder;

  BtaAvConfig() {
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
  // TODO: b/321806163 Remove the full constructor as part of the flag cleanup.
  BtaAvConfig(uint32_t company_id, uint16_t avrc_ct_cat, uint16_t avrc_tg_cat,
              uint16_t audio_mqs, bool avrc_group, uint8_t num_co_ids,
              uint8_t num_evt_ids, tBTA_AV_CODE rc_pass_rsp,
              const uint32_t* p_meta_co_ids, const uint8_t* p_meta_evt_ids)
      : company_id(company_id),
        avrc_ct_cat(avrc_ct_cat),
        avrc_tg_cat(avrc_tg_cat),
        audio_mqs(audio_mqs),
        avrc_group(avrc_group),
        num_co_ids(num_co_ids),
        num_evt_ids(num_evt_ids),
        rc_pass_rsp(rc_pass_rsp),
        p_meta_co_ids(p_meta_co_ids),
        p_meta_evt_ids(p_meta_evt_ids) {}

  /**
   * Getter method for the individual attributes.
   */
  uint32_t getCompanyId() const { return company_id; }

  uint16_t getAvrcpControllerCategories() const { return avrc_ct_cat; }

  uint16_t getAvrcpTargetCategories() const { return avrc_tg_cat; }

  uint16_t getAudioMqs() const { return audio_mqs; }

  bool isAvrcGroup() const { return avrc_group; }

  uint8_t getNumCoIds() const { return num_co_ids; }

  uint8_t getNumEvtIds() const { return num_evt_ids; }

  tBTA_AV_CODE getRcPassRsp() const { return rc_pass_rsp; }

  const uint32_t* getPMetaCoIds() const { return p_meta_co_ids; }

  const uint8_t* getPMetaEvtIds() const { return p_meta_evt_ids; }

  bool operator==(const BtaAvConfig& other) const;  // Declaration

 private:
  uint32_t company_id;  /* AVRCP Company ID */
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
  const uint8_t* p_meta_evt_ids; /* the metadata Get Capabilities response
                                for event id */
  friend class Builder;
};

/**
 * Builder class for BTA AV Config.
 */
class BtaAvConfig::Builder {
 public:
  /**
   * Default constructor for the builder to initialize default values.
   */
  Builder() : bta_av_config() {
    // Initialize default values if needed
    bta_av_config.company_id = 0;
    bta_av_config.avrc_ct_cat = 0;
    bta_av_config.avrc_tg_cat = 0;
    bta_av_config.audio_mqs = 0;
    bta_av_config.avrc_group = false;
    bta_av_config.num_co_ids = 0;
    bta_av_config.num_evt_ids = 0;
    bta_av_config.rc_pass_rsp = 0;  // Set appropriate default value
    bta_av_config.p_meta_co_ids = nullptr;
    bta_av_config.p_meta_evt_ids = nullptr;
  }

  /**
   * Setter class for the build and returns the pointer to the builder.
   */
  Builder& setCompanyId(uint32_t companyId) {
    bta_av_config.company_id = companyId;
    return *this;
  }

  Builder& setAvrcCtCat(uint16_t avrcCtCat) {
    bta_av_config.avrc_ct_cat = avrcCtCat;
    return *this;
  }

  Builder& setAvrcTgCat(uint16_t avrcTgCat) {
    bta_av_config.avrc_tg_cat = avrcTgCat;
    return *this;
  }

  Builder& setAudioMqs(uint16_t audioMqs) {
    bta_av_config.audio_mqs = audioMqs;
    return *this;
  }

  Builder& setAvrcGroup(bool avrcGroup) {
    bta_av_config.avrc_group = avrcGroup;
    return *this;
  }

  Builder& setNumCoIds(uint8_t numCoIds) {
    bta_av_config.num_co_ids = numCoIds;
    return *this;
  }

  Builder& setNumEvtIds(uint8_t numEvtIds) {
    bta_av_config.num_evt_ids = numEvtIds;
    return *this;
  }

  Builder& setRcPassRsp(tBTA_AV_CODE rcPassRsp) {
    bta_av_config.rc_pass_rsp = rcPassRsp;
    return *this;
  }

  Builder& setMetaCoIds(const uint32_t* metaCoIds) {
    bta_av_config.p_meta_co_ids = metaCoIds;
    return *this;
  }

  Builder& setMetaEvtIds(const uint8_t* metaEvtIds) {
    bta_av_config.p_meta_evt_ids = metaEvtIds;
    return *this;
  }

  /**
   * Returns the BAT AV Config.
   * @return bta av config.
   */
  BtaAvConfig build() const { return bta_av_config; }

 private:
  BtaAvConfig bta_av_config;
};

/**
 * Factory class to generate the BTA AV config based on the specified
 * parameters.
 */
class BtaAvCfgFactory {
 public:
  /**
   * Creates custom BTA AV config based on the specified parameters.
   * @param source_enabled is the A2DP source profile enabled.
   * @param sink_enabled is the A2DP sink profile enabled.
   * @param profile_version AVRCP profile version.
   * @return the BTA AV config populated with attributes based on the specified
   * parameters.
   */
  static const BtaAvConfig createCustomConfig(const bool source_enabled,
                                              const bool sink_enabled,
                                              const uint16_t profile_version);
};
