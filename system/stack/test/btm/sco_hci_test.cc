/*
 *
 *  Copyright 2022 The Android Open Source Project
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

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <map>
#include <memory>

#include "stack/btm/btm_sco.h"
#include "udrv/include/uipc.h"

extern std::map<std::string, int> mock_function_count_map;
std::unique_ptr<tUIPC_STATE> mock_uipc_init_ret;
uint32_t mock_uipc_read_ret;
bool mock_uipc_send_ret;

namespace {

using testing::Test;

class ScoHciTest : public Test {
 public:
 protected:
  void SetUp() override {
    mock_function_count_map.clear();
    mock_uipc_init_ret = nullptr;
    mock_uipc_read_ret = 0;
    mock_uipc_send_ret = true;
  }
  void TearDown() override {}
};

class ScoHciWithOpenCleanTest : public ScoHciTest {
 public:
 protected:
  void SetUp() override {
    mock_uipc_init_ret = std::make_unique<tUIPC_STATE>();
    bluetooth::audio::sco::open();
  }
  void TearDown() override { bluetooth::audio::sco::cleanup(); }
};

TEST_F(ScoHciTest, ScoOverHciOpenFail) {
  bluetooth::audio::sco::open();
  ASSERT_EQ(mock_function_count_map["UIPC_Init"], 1);
  ASSERT_EQ(mock_function_count_map["UIPC_Open"], 0);
}

TEST_F(ScoHciTest, ScoOverHciOpenClean) {
  mock_uipc_init_ret = std::make_unique<tUIPC_STATE>();
  bluetooth::audio::sco::open();
  ASSERT_EQ(mock_function_count_map["UIPC_Init"], 1);
  ASSERT_EQ(mock_function_count_map["UIPC_Open"], 1);

  mock_uipc_init_ret = std::make_unique<tUIPC_STATE>();
  // Double open will override uipc
  bluetooth::audio::sco::open();
  ASSERT_EQ(mock_function_count_map["UIPC_Init"], 2);
  ASSERT_EQ(mock_function_count_map["UIPC_Open"], 2);

  bluetooth::audio::sco::cleanup();
  ASSERT_EQ(mock_function_count_map["UIPC_Close"], 1);

  // Double clean shouldn't fail
  bluetooth::audio::sco::cleanup();
  ASSERT_EQ(mock_function_count_map["UIPC_Close"], 1);
}

TEST_F(ScoHciTest, ScoOverHciReadNoOpen) {
  uint8_t buf[100];
  ASSERT_EQ(bluetooth::audio::sco::read(buf, sizeof(buf)), size_t(0));
  ASSERT_EQ(mock_function_count_map["UIPC_Read"], 0);
}

TEST_F(ScoHciWithOpenCleanTest, ScoOverHciRead) {
  uint8_t buf[100];
  mock_uipc_read_ret = sizeof(buf);
  ASSERT_EQ(bluetooth::audio::sco::read(buf, sizeof(buf)), mock_uipc_read_ret);
  ASSERT_EQ(mock_function_count_map["UIPC_Read"], 1);
}

TEST_F(ScoHciTest, ScoOverHciWriteNoOpen) {
  uint8_t buf[100];
  bluetooth::audio::sco::write(buf, sizeof(buf));
  ASSERT_EQ(mock_function_count_map["UIPC_Send"], 0);
}

TEST_F(ScoHciWithOpenCleanTest, ScoOverHciWrite) {
  uint8_t buf[100];
  ASSERT_EQ(bluetooth::audio::sco::write(buf, sizeof(buf)), sizeof(buf));
  ASSERT_EQ(mock_function_count_map["UIPC_Send"], 1);

  mock_uipc_send_ret = false;

  ASSERT_EQ(bluetooth::audio::sco::write(buf, sizeof(buf)), size_t(0));
  ASSERT_EQ(mock_function_count_map["UIPC_Send"], 2);
}

}  // namespace
