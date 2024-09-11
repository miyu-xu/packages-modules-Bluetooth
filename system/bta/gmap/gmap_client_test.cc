/*
 * Copyright (C) 2024 The Android Open Source Project
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

#include <bluetooth/log.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <hardware/bluetooth.h>

#include "bta/le_audio/le_audio_types.h"
#include "bta_gmap_client_api.h"

using ::testing::_;

class GmapClientTest : public ::testing::Test {
public:
  RawAddress addr = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
  GmapClient gmapClient = GmapClient(addr);
};

TEST_F(GmapClientTest, test_parse_role) {
  const uint8_t role = 0b0001;
  gmapClient.parseAndSaveGmapRole(1, &role);

  ASSERT_EQ(gmapClient.getRole(), role);
}

TEST_F(GmapClientTest, test_parse_invalid_role) {
  const uint8_t role = 0b0001;
  ASSERT_FALSE(gmapClient.parseAndSaveGmapRole(2, &role));
}

TEST_F(GmapClientTest, test_parse_ugt_feature) {
  const uint8_t value = 0b0001;
  gmapClient.parseAndSaveUGTFeature(1, &value);

  ASSERT_EQ(gmapClient.getUGTFeature(), value);
}

TEST_F(GmapClientTest, test_parse_invalid_ugt_feature) {
  const uint8_t value = 0b0001;
  ASSERT_FALSE(gmapClient.parseAndSaveUGTFeature(2, &value));
}
