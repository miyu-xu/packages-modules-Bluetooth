#include "bta/include/bta_av_cfg.h"

#include <av/bta_av_int.h>
#include <gtest/gtest.h>

class BtaAvCfgTest : public ::testing::Test {};

TEST_F(BtaAvCfgTest, BtaAvCfg_onlySourceEnabledV14_configIsValid) {
  // Test when source is enabled and sink is disabled
  const BtaAvConfig& bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, false, AVRC_REV_1_4, true);
  EXPECT_EQ(bta_av_config.getCompanyId(),
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT2,
            AVRC_SUPF_CT_CAT2);
  // Target Categories
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT1,
            AVRC_SUPF_TG_CAT1);
  EXPECT_EQ(
      bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_MULTI_PLAYER,
      AVRC_SUPF_TG_MULTI_PLAYER);
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_BROWSE,
            AVRC_SUPF_TG_BROWSE);

  EXPECT_EQ(bta_av_config.getAudioMqs(), 6);
  EXPECT_FALSE(bta_av_config.isAvrcGroup());
  EXPECT_EQ(bta_av_config.getRcPassRsp(), AVRC_RSP_NOT_IMPL);

  EXPECT_EQ(bta_av_config.getNumCoIds(), 2);
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[0],
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[1],
            static_cast<uint32_t>(AVRC_CO_BROADCOM));

  EXPECT_EQ(bta_av_config.getNumEvtIds(), 7);
  const uint8_t* metaEvtIds = bta_av_config.getPMetaEvtIds();
  EXPECT_EQ(metaEvtIds[0], AVRC_EVT_PLAY_STATUS_CHANGE);
  EXPECT_EQ(metaEvtIds[1], AVRC_EVT_TRACK_CHANGE);
  EXPECT_EQ(metaEvtIds[2], AVRC_EVT_PLAY_POS_CHANGED);
  EXPECT_EQ(metaEvtIds[3], AVRC_EVT_AVAL_PLAYERS_CHANGE);
  EXPECT_EQ(metaEvtIds[4], AVRC_EVT_ADDR_PLAYER_CHANGE);
  EXPECT_EQ(metaEvtIds[5], AVRC_EVT_UIDS_CHANGE);
  EXPECT_EQ(metaEvtIds[6], AVRC_EVT_NOW_PLAYING_CHANGE);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_onlySourceEnabledV13_configIsValid) {
  // Test when source is enabled and sink is disabled
  const BtaAvConfig& bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, false, AVRC_REV_1_3, true);
  EXPECT_EQ(bta_av_config.getCompanyId(),
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT2,
            AVRC_SUPF_CT_CAT2);
  // Target Categories
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT1,
            AVRC_SUPF_TG_CAT1);

  EXPECT_EQ(bta_av_config.getAudioMqs(), 6);
  EXPECT_FALSE(bta_av_config.isAvrcGroup());
  EXPECT_EQ(bta_av_config.getRcPassRsp(), AVRC_RSP_NOT_IMPL);

  EXPECT_EQ(bta_av_config.getNumCoIds(), 2);
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[0],
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[1],
            static_cast<uint32_t>(AVRC_CO_BROADCOM));

  EXPECT_EQ(bta_av_config.getNumEvtIds(), 3);
  const uint8_t* metaEvtIds = bta_av_config.getPMetaEvtIds();
  EXPECT_EQ(metaEvtIds[0], AVRC_EVT_PLAY_STATUS_CHANGE);
  EXPECT_EQ(metaEvtIds[1], AVRC_EVT_TRACK_CHANGE);
  EXPECT_EQ(metaEvtIds[2], AVRC_EVT_PLAY_POS_CHANGED);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_onlySinkEnabledV14_configIsValid) {
  // Test when source is enabled and sink is disabled
  const BtaAvConfig& bta_av_config =
      BtaAvCfgFactory::createCustomConfig(false, true, AVRC_REV_1_4, true);
  EXPECT_EQ(bta_av_config.getCompanyId(),
            static_cast<uint32_t>(AVRC_CO_METADATA));
  // Controller categories
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT1,
            AVRC_SUPF_CT_CAT1);
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_TG_BROWSE,
            AVRC_SUPF_TG_BROWSE);
  // Target categories
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT2,
            AVRC_SUPF_TG_CAT2);

  EXPECT_EQ(bta_av_config.getAudioMqs(), 6);
  EXPECT_FALSE(bta_av_config.isAvrcGroup());
  EXPECT_EQ(bta_av_config.getRcPassRsp(), AVRC_RSP_NOT_IMPL);

  EXPECT_EQ(bta_av_config.getNumCoIds(), 2);
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[0],
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[1],
            static_cast<uint32_t>(AVRC_CO_BROADCOM));

  EXPECT_EQ(bta_av_config.getNumEvtIds(), 1);
  const uint8_t* metaEvtIds = bta_av_config.getPMetaEvtIds();
  EXPECT_EQ(metaEvtIds[0], AVRC_EVT_VOLUME_CHANGE);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_SourceSinkEnabledV14_configIsValid) {
  // Test when source is enabled and sink is disabled
  const BtaAvConfig& bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, true, AVRC_REV_1_4, true);
  EXPECT_EQ(bta_av_config.getCompanyId(),
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT2,
            AVRC_SUPF_CT_CAT2);
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT1,
            AVRC_SUPF_CT_CAT1);
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_TG_BROWSE,
            AVRC_SUPF_TG_BROWSE);
  // Target Categories
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT1,
            AVRC_SUPF_TG_CAT1);
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT2,
            AVRC_SUPF_TG_CAT2);
  EXPECT_EQ(
      bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_MULTI_PLAYER,
      AVRC_SUPF_TG_MULTI_PLAYER);
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_BROWSE,
            AVRC_SUPF_TG_BROWSE);

  EXPECT_EQ(bta_av_config.getAudioMqs(), 6);
  EXPECT_FALSE(bta_av_config.isAvrcGroup());
  EXPECT_EQ(bta_av_config.getRcPassRsp(), AVRC_RSP_NOT_IMPL);

  EXPECT_EQ(bta_av_config.getNumCoIds(), 2);
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[0],
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[1],
            static_cast<uint32_t>(AVRC_CO_BROADCOM));
  EXPECT_EQ(bta_av_config.getNumEvtIds(), 8);
  const uint8_t* metaEvtIds = bta_av_config.getPMetaEvtIds();
  EXPECT_EQ(metaEvtIds[0], AVRC_EVT_PLAY_STATUS_CHANGE);
  EXPECT_EQ(metaEvtIds[1], AVRC_EVT_TRACK_CHANGE);
  EXPECT_EQ(metaEvtIds[2], AVRC_EVT_PLAY_POS_CHANGED);
  EXPECT_EQ(metaEvtIds[3], AVRC_EVT_AVAL_PLAYERS_CHANGE);
  EXPECT_EQ(metaEvtIds[4], AVRC_EVT_ADDR_PLAYER_CHANGE);
  EXPECT_EQ(metaEvtIds[5], AVRC_EVT_UIDS_CHANGE);
  EXPECT_EQ(metaEvtIds[6], AVRC_EVT_NOW_PLAYING_CHANGE);
  EXPECT_EQ(metaEvtIds[7], AVRC_EVT_VOLUME_CHANGE);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_SourceSinkEnabledV13_configIsValid) {
  // Test when source is enabled and sink is disabled
  const BtaAvConfig& bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, true, AVRC_REV_1_3, true);
  EXPECT_EQ(bta_av_config.getCompanyId(),
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT2,
            AVRC_SUPF_CT_CAT2);
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_CT_CAT1,
            AVRC_SUPF_CT_CAT1);
  EXPECT_EQ(bta_av_config.getAvrcpControllerCategories() & AVRC_SUPF_TG_BROWSE,
            AVRC_SUPF_TG_BROWSE);
  // Target Categories
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT1,
            AVRC_SUPF_TG_CAT1);
  EXPECT_EQ(bta_av_config.getAvrcpTargetCategories() & AVRC_SUPF_TG_CAT2,
            AVRC_SUPF_TG_CAT2);

  EXPECT_EQ(bta_av_config.getAudioMqs(), 6);
  EXPECT_FALSE(bta_av_config.isAvrcGroup());
  EXPECT_EQ(bta_av_config.getRcPassRsp(), AVRC_RSP_NOT_IMPL);

  EXPECT_EQ(bta_av_config.getNumCoIds(), 2);
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[0],
            static_cast<uint32_t>(AVRC_CO_METADATA));
  EXPECT_EQ(bta_av_config.getPMetaCoIds()[1],
            static_cast<uint32_t>(AVRC_CO_BROADCOM));

  EXPECT_EQ(bta_av_config.getNumEvtIds(), 4);
  const uint8_t* metaEvtIds = bta_av_config.getPMetaEvtIds();
  EXPECT_EQ(metaEvtIds[0], AVRC_EVT_PLAY_STATUS_CHANGE);
  EXPECT_EQ(metaEvtIds[1], AVRC_EVT_TRACK_CHANGE);
  EXPECT_EQ(metaEvtIds[2], AVRC_EVT_PLAY_POS_CHANGED);
  EXPECT_EQ(metaEvtIds[3], AVRC_EVT_VOLUME_CHANGE);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_getOldSinkOnlyConfigCfg_configIsEqual) {
  BtaAvConfig sink_only_bta_av_config =
      BtaAvCfgFactory::createCustomConfig(false, true, AVRC_REV_1_4, true);
  const BtaAvConfig avkCfg = get_bta_avk_cfg();
  EXPECT_EQ(sink_only_bta_av_config, avkCfg);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_getOldSourceOnlyConfigv14_configIsEqual) {
  BtaAvConfig source_only_bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, false, AVRC_REV_1_4, true);
  EXPECT_EQ(source_only_bta_av_config, bta_av_cfg);
}

TEST_F(BtaAvCfgTest, BtaAvCfg_getOldSourceOnlyConfigV13_configIsEqual) {
  BtaAvConfig source_only_bta_av_config =
      BtaAvCfgFactory::createCustomConfig(true, false, AVRC_REV_1_3, true);
  EXPECT_EQ(source_only_bta_av_config, bta_av_cfg_compatibility);
}
