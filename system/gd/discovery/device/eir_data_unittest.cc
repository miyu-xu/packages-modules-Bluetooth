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

#include "discovery/device/eir_data.h"

#include "discovery/device/eir_test_data_packets.h"
#include "gtest/gtest.h"
#include "hci/hci_packets.h"
#include "os/log.h"

using namespace bluetooth;
using bluetooth::discovery::device::EirData;

namespace {
constexpr uint8_t kPartialUuid16Data[] = {
    0x2, static_cast<uint8_t>(hci::GapDataType::COMPLETE_LIST_16_BIT_UUIDS), 0x34};
constexpr uint8_t kOneUuid16Data[] = {
    0x3, static_cast<uint8_t>(hci::GapDataType::COMPLETE_LIST_16_BIT_UUIDS), 0x34, 0x12};
}  // namespace

namespace debug {
void LogUuids16(const std::vector<uint16_t>& uuids16) {
  for (const auto& uuid : uuids16) {
    LOG_INFO("  uuid:0x%x", uuid);
  }
}

void LogUuids128(const std::vector<hci::Uuid>& uuids128) {
  for (const auto& uuid : uuids128) {
    LOG_INFO("  uuid:%s", uuid.ToString().c_str());
  }
}
}  // namespace debug

TEST(EirDataTest, partial_uuid16) {
  const EirData eir_data(
      std::vector<uint8_t>(kPartialUuid16Data, kPartialUuid16Data + sizeof(kPartialUuid16Data)));

  std::vector<uint16_t> uuids;
  ASSERT_FALSE(eir_data.GetUuids16(uuids));
}

TEST(EirDataTest, one_uuid16) {
  const EirData eir_data(
      std::vector<uint8_t>(kOneUuid16Data, kOneUuid16Data + sizeof(kOneUuid16Data)));

  std::vector<uint16_t> uuids;
  ASSERT_TRUE(eir_data.GetUuids16(uuids));
  ASSERT_EQ(1U, uuids.size());
  ASSERT_EQ(0x1234, uuids[0]);
}

TEST(EirDataTest, test_select_packets__pkt34639) {
  ASSERT_EQ(1U, selected_packets.count("pkt34639"));
  const auto& pkt = selected_packets["pkt34639"];
  const EirData eir_data(std::vector<uint8_t>(&pkt[kEirOffset], &pkt[kEirOffset] + kEirSize));

  std::vector<uint16_t> uuids16;
  ASSERT_TRUE(eir_data.GetUuids16(uuids16));
  ASSERT_EQ(14U, uuids16.size());
  ASSERT_EQ(0x112e, uuids16[0]);
  ASSERT_EQ(0x180a, uuids16[13]);

  std::vector<uint32_t> uuids32;
  ASSERT_FALSE(eir_data.GetUuids32(uuids32));
  ASSERT_EQ(0U, uuids32.size());

  std::vector<hci::Uuid> uuids128;
  ASSERT_TRUE(eir_data.GetUuids128(uuids128));

  ASSERT_EQ(hci::Uuid::FromString("00000000-deca-fade-deca-deafdecacaff"), uuids128[0]);

  std::vector<int8_t> tx_power_level;
  ASSERT_TRUE(eir_data.GetTxPowerLevel(tx_power_level));
  ASSERT_EQ(4, tx_power_level[0]);

  std::vector<std::array<uint8_t, 240>> names;
  ASSERT_TRUE(eir_data.GetCompleteNames(names));
  ASSERT_STREQ("Audi_MMI_9962", std::string(names[0].begin(), names[0].end()).data());
}
