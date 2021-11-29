/*
 *  Copyright 2021 The Android Open Source Project
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
 */

#include "le_audio_set_configurations.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "audio_set_configurations_generated.h"
#include "audio_set_scenarios_generated.h"
#include "flatbuffers/idl.h"
#include "flatbuffers/util.h"
#include "le_audio_types.h"

namespace le_audio {
namespace set_configurations {

class LeAudioPacRequirementsTest : public ::testing::Test {
 protected:
#ifdef OS_ANDROID
  static constexpr auto configurations_schema_path =
      "/system/etc/bluetooth/le_audio/audio_set_configurations.bfbs";
  static constexpr auto scenarios_schema_path =
      "/system/etc/bluetooth/le_audio/audio_set_scenarios.bfbs";
#else
  static constexpr auto configurations_schema_path =
      "audio_set_configurations.bfbs";
  static constexpr auto scenarios_schema_path = "audio_set_scenarios.bfbs";
#endif
  static constexpr auto configurations_json_content =
      "{"
      "    \"_comments_\": [],"
      "    \"configurations\": ["
      "        {"
      "            \"name\": \"DualDev_OneChanStereoSnk_16_2\","
      "            \"subconfigurations\": ["
      "                {"
      "                    \"device_cnt\": 2,"
      "                    \"ase_cnt\": 2,"
      "                    \"direction\" : \"SINK\","
      "                    \"configuration_strategy\": "
      "                        \"MONO_ONE_CIS_PER_DEVICE\","
      "                    \"codec_id\": {"
      "                        \"coding_format\": 3,"
      "                        \"vendor_company_id\": 0,"
      "                        \"vendor_codec_id\": 0"
      "                    },"
      "                    \"codec_configuration\": ["
      "                       {"
      "                           \"name\": \"sampling_frequency\","
      "                           \"type\": 1,"
      "                           \"compound_value\": {"
      "                               \"value\": ["
      "                                   3"
      "                               ]"
      "                           }"
      "                       },"
      "                       {"
      "                           \"name\": \"two octets\","
      "                           \"type\": 8,"
      "                           \"compound_value\": {"
      "                               \"value\": ["
      "                                   3,"
      "                                   2"
      "                               ],"
      "                               \"value_width\": 1"
      "                           }"
      "                       }"
      "                    ]"
      "                }"
      "            ]"
      "        }"
      "    ]"
      "}";
  static constexpr auto scenarios_json_content =
      "{"
      "    \"_comments_\": [],"
      "    \"scenarios\": ["
      "        {"
      "            \"name\": \"Ringtone\","
      "            \"configurations\": ["
      "                \"DualDev_OneChanStereoSnk_16_2\""
      "            ]"
      "        }"
      "    ]"
      "}";

  void SetUp(void) override {
    // Configurations
    std::string configurations_schema_binary_content;
    bool ok = flatbuffers::LoadFile(configurations_schema_path, true,
                                    &configurations_schema_binary_content);
    ASSERT_TRUE(ok);

    // Load the binary schema
    ok = configurations_parser_.Deserialize(
        (uint8_t*)configurations_schema_binary_content.c_str(),
        configurations_schema_binary_content.length());
    ASSERT_TRUE(ok);

    // Load the content from JSON
    ok = configurations_parser_.Parse(configurations_json_content);
    ASSERT_TRUE(ok);

    // Scenarios
    std::string scenarios_schema_binary_content;
    ok = flatbuffers::LoadFile(scenarios_schema_path, true,
                               &scenarios_schema_binary_content);
    ASSERT_TRUE(ok);

    // Load the binary schema
    ok = scenarios_parser_.Deserialize(
        (uint8_t*)scenarios_schema_binary_content.c_str(),
        scenarios_schema_binary_content.length());
    ASSERT_TRUE(ok);

    // Load the content from JSON
    ok = scenarios_parser_.Parse(scenarios_json_content);
    ASSERT_TRUE(ok);
  }

  void TearDown(void) override { /* not much to do here */
  }

  flatbuffers::Parser configurations_parser_;
  flatbuffers::Parser scenarios_parser_;
};

TEST_F(LeAudioPacRequirementsTest, testScenarios) {
  // Get the root container
  auto scenarios_root = bluetooth::le_audio::GetAudioSetScenarios(
      scenarios_parser_.builder_.GetBufferPointer());
  ASSERT_NE(nullptr, scenarios_root);

  // Get all scenarios
  auto scenarios = scenarios_root->scenarios();
  EXPECT_NE(0u, scenarios->size());

  // Get the ringtone scenario
  auto scenario_ringtone = scenarios->LookupByKey("Ringtone");
  ASSERT_NE(nullptr, scenario_ringtone);
  ASSERT_NE(0u, scenario_ringtone->configurations()->size());

  // Get the config referenced by the scenarios 1st entry
  auto configurations_root = bluetooth::le_audio::GetAudioSetConfigurations(
      configurations_parser_.builder_.GetBufferPointer());
  auto all_configs = configurations_root->configurations();
  ASSERT_NE(nullptr, all_configs);
  auto ringtone_config0_name = *scenario_ringtone->configurations()->begin();
  auto ringtone_config0 =
      all_configs->LookupByKey(ringtone_config0_name->c_str());
  EXPECT_NE(nullptr, ringtone_config0);
}

TEST_F(LeAudioPacRequirementsTest, testConfiguration) {
  // Get the root container
  auto root = bluetooth::le_audio::GetAudioSetConfigurations(
      configurations_parser_.builder_.GetBufferPointer());
  ASSERT_NE(nullptr, root);

  // Get the config referenced by the scenarios 1st entry
  auto all_configs = root->configurations();
  ASSERT_NE(nullptr, all_configs);
  auto config0 = *all_configs->begin();
  EXPECT_NE(nullptr, config0);

  // Check the subconfigurations
  ASSERT_NE(nullptr, config0->subconfigurations());
  ASSERT_NE(0u, config0->subconfigurations()->size());

  // Verify the 1st subconfig content
  ASSERT_NE(config0->subconfigurations()->end(),
            config0->subconfigurations()->begin());
  auto sink_config = *config0->subconfigurations()->begin();
  ASSERT_NE(nullptr, sink_config);

  ASSERT_EQ(2, sink_config->device_cnt());
  ASSERT_EQ(2, sink_config->ase_cnt());
  ASSERT_EQ(static_cast<uint8_t>(
                le_audio::set_configurations::LeAudioConfigurationStrategy::
                    MONO_ONE_CIS_PER_DEVICE),
            static_cast<uint8_t>(sink_config->configuration_strategy()));

  // Verify codec ID
  auto codec_id = sink_config->codec_id();
  ASSERT_NE(nullptr, codec_id);
  ASSERT_EQ(3, codec_id->coding_format());
  ASSERT_EQ(0, codec_id->vendor_company_id());
  ASSERT_EQ(0, codec_id->vendor_codec_id());

  // Verify codec parameters
  auto codec_config_params = sink_config->codec_configuration();
  ASSERT_NE(nullptr, codec_config_params);
  ASSERT_NE(0u, codec_config_params->size());

  auto param_it = codec_config_params->begin();

  auto codec_config_param0 = *param_it++;
  ASSERT_STREQ("sampling_frequency", codec_config_param0->name()->c_str());
  ASSERT_EQ(1, codec_config_param0->type());

  // Verify single value
  auto codec_config_param0_value =
      codec_config_param0->compound_value()->value();
  ASSERT_NE(nullptr, codec_config_param0_value);
  ASSERT_EQ(1u, codec_config_param0_value->size());
  ASSERT_EQ(3u, codec_config_param0_value->Get(0));

  // Verify multiple values
  auto codec_config_param1 = *param_it++;
  ASSERT_STREQ("two octets", codec_config_param1->name()->c_str());
  ASSERT_EQ(8, codec_config_param1->type());

  // Expect two single octet values
  ASSERT_NE(nullptr, codec_config_param1->compound_value());
  auto codec_config_param1_value_width =
      codec_config_param1->compound_value()->value_width();
  ASSERT_EQ(1, codec_config_param1_value_width);

  auto codec_config_param1_value =
      codec_config_param1->compound_value()->value();
  ASSERT_NE(nullptr, codec_config_param1_value);
  ASSERT_EQ(2u, codec_config_param1_value->size());

  ASSERT_EQ(3, codec_config_param1_value->Get(0));
  ASSERT_EQ(2, codec_config_param1_value->Get(1));
}

}  // namespace set_configurations
}  // namespace le_audio
