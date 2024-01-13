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

#include "discovery/device/data_parser.h"

#include <algorithm>

#include "gtest/gtest.h"
#include "hci/hci_packets.h"

using namespace bluetooth::hci;

namespace {
constexpr uint8_t kOneFlag32Data[] = {
    0x5, static_cast<uint8_t>(GapDataType::FLAGS), 0xde, 0xad, 0xbe, 0xef};
constexpr uint8_t kTwoFlag32Data[] = {
    0x5,
    static_cast<uint8_t>(GapDataType::FLAGS),
    0xde,
    0xad,
    0xbe,
    0xef,
    0x5,
    static_cast<uint8_t>(GapDataType::FLAGS),
    0x11,
    0x22,
    0x33,
    0x44};
constexpr uint8_t kNoUuid16Data[] = {
    0x2, static_cast<uint8_t>(GapDataType::COMPLETE_LIST_16_BIT_UUIDS)};
constexpr uint8_t kPartialUuid16Data[] = {
    0x2, static_cast<uint8_t>(GapDataType::COMPLETE_LIST_16_BIT_UUIDS), 0x12};
constexpr uint8_t kOneUuid16Data[] = {
    0x3, static_cast<uint8_t>(GapDataType::COMPLETE_LIST_16_BIT_UUIDS), 0x12, 0x34};

uint32_t toLeInt(const std::vector<uint8_t>& v) {
  return v[3] | (v[2] << 8) | (v[1] << 16) | (v[0] << 24);
}

}  // namespace

TEST(DataParserTest, simple_flag) {
  std::vector<uint8_t> v(kOneFlag32Data, kOneFlag32Data + sizeof(kOneFlag32Data));
  auto data = std::make_shared<std::vector<uint8_t>>(v);

  auto it = Iterator<kLittleEndian>(std::shared_ptr<std::vector<uint8_t>>(data));
  GapData gap_data;
  it = GapData::Parse(&gap_data, it);

  ASSERT_EQ(it.NumBytesRemaining(), 0U);
  ASSERT_EQ(gap_data.data_type_, GapDataType::FLAGS);
  ASSERT_EQ(0xdeadbeef, toLeInt(gap_data.data_));
}

TEST(DataParserTest, two_flags) {
  std::vector<uint8_t> v(kTwoFlag32Data, kTwoFlag32Data + sizeof(kTwoFlag32Data));
  std::shared_ptr<std::vector<uint8_t>> data = std::make_shared<std::vector<uint8_t>>(v);

  auto it = Iterator<kLittleEndian>(std::shared_ptr<std::vector<uint8_t>>(data));
  GapData gap_data[2];
  it = GapData::Parse(&gap_data[0], it);

  ASSERT_EQ(it.NumBytesRemaining(), 1U /* length */ + 1U /* type */ + 4U /* data */);
  ASSERT_EQ(gap_data[0].data_type_, GapDataType::FLAGS);
  ASSERT_EQ((unsigned)0xdeadbeef, toLeInt(gap_data[0].data_));

  it = GapData::Parse(&gap_data[1], it);

  ASSERT_EQ(it.NumBytesRemaining(), 0U);
  ASSERT_EQ(gap_data[1].data_type_, GapDataType::FLAGS);
  ASSERT_EQ((unsigned)0x11223344, toLeInt(gap_data[1].data_));
}

TEST(DataParserTest, no_uuid16) {
  std::vector<uint8_t> v(kNoUuid16Data, kNoUuid16Data + sizeof(kNoUuid16Data));

  auto it = Iterator<kLittleEndian>(
      std::shared_ptr<std::vector<uint8_t>>(std::make_shared<std::vector<uint8_t>>(v)));
  GapData gap_data;
  it = GapData::Parse(&gap_data, it);

  ASSERT_EQ(it.NumBytesRemaining(), 0U);
  ASSERT_EQ(gap_data.data_type_, GapDataType::COMPLETE_LIST_16_BIT_UUIDS);
  ASSERT_EQ(0U, gap_data.data_.size());
}

TEST(DataParserTest, partial_uuid16) {
  std::vector<uint8_t> v(kPartialUuid16Data, kPartialUuid16Data + sizeof(kPartialUuid16Data));

  auto it = Iterator<kLittleEndian>(
      std::shared_ptr<std::vector<uint8_t>>(std::make_shared<std::vector<uint8_t>>(v)));
  GapData gap_data;
  it = GapData::Parse(&gap_data, it);

  ASSERT_EQ(it.NumBytesRemaining(), 0U);
  ASSERT_EQ(gap_data.data_type_, GapDataType::COMPLETE_LIST_16_BIT_UUIDS);
  ASSERT_EQ(1U, gap_data.data_.size());
}

TEST(DataParserTest, one_uuid16) {
  std::vector<uint8_t> v(kOneUuid16Data, kOneUuid16Data + sizeof(kOneUuid16Data));

  auto it = Iterator<kLittleEndian>(
      std::shared_ptr<std::vector<uint8_t>>(std::make_shared<std::vector<uint8_t>>(v)));
  GapData gap_data;
  it = GapData::Parse(&gap_data, it);

  ASSERT_EQ(it.NumBytesRemaining(), 0U);
  ASSERT_EQ(gap_data.data_type_, GapDataType::COMPLETE_LIST_16_BIT_UUIDS);
  ASSERT_EQ(2U, gap_data.data_.size());
}

TEST(DataParserTest, simple_data_parser) {
  std::vector<uint8_t> v(kTwoFlag32Data, kTwoFlag32Data + sizeof(kTwoFlag32Data));
  bluetooth::discovery::device::DataParser data_parser(v);
  ASSERT_EQ(2U, data_parser.Size());

  std::vector<bluetooth::hci::GapData> flags;
  std::vector<bluetooth::hci::GapData> gap_data = data_parser.GetData();
  for (const auto& data : gap_data) {
    ASSERT_EQ(bluetooth::hci::GapDataType::FLAGS, data.data_type_);
    flags.push_back(data);
  }

  ASSERT_EQ(2U, flags.size());
  uint32_t value[2] = {
      toLeInt(flags[0].data_),
      toLeInt(flags[1].data_),
  };
  ASSERT_EQ((unsigned)0xdeadbeef, value[0]);
  ASSERT_EQ((unsigned)0x11223344, value[1]);
}

TEST(DataParserTest, two_flags_backing_store_cleared) {
  std::vector<uint8_t>* v = new std::vector<uint8_t>(sizeof(kTwoFlag32Data));
  std::copy(kTwoFlag32Data, kTwoFlag32Data + sizeof(kTwoFlag32Data), v->begin());
  bluetooth::discovery::device::DataParser data_parser(*v);
  v->clear();
  ASSERT_EQ(2U, data_parser.Size());

  std::vector<bluetooth::hci::GapData> flags;
  std::vector<bluetooth::hci::GapData> gap_data = data_parser.GetData();
  for (const auto& data : gap_data) {
    ASSERT_EQ(bluetooth::hci::GapDataType::FLAGS, data.data_type_);
    flags.push_back(data);
  }

  ASSERT_EQ(2U, flags.size());
  uint32_t value[2] = {
      toLeInt(flags[0].data_),
      toLeInt(flags[1].data_),
  };
  ASSERT_EQ((unsigned)0xdeadbeef, value[0]);
  ASSERT_EQ((unsigned)0x11223344, value[1]);

  delete v;
}

TEST(DataParserTest, backing_store_freed) {
  uint8_t* data = (uint8_t*)malloc(sizeof(kTwoFlag32Data));
  std::copy(kTwoFlag32Data, kTwoFlag32Data + sizeof(kTwoFlag32Data), data);
  bluetooth::discovery::device::DataParser data_parser(
      std::vector<uint8_t>(data, data + sizeof(kTwoFlag32Data)));
  free(data);
  ASSERT_EQ(2U, data_parser.Size());

  std::vector<bluetooth::hci::GapData> flags;
  std::vector<bluetooth::hci::GapData> gap_data = data_parser.GetData();
  for (const auto& data : gap_data) {
    ASSERT_EQ(bluetooth::hci::GapDataType::FLAGS, data.data_type_);
    flags.push_back(data);
  }

  ASSERT_EQ(2U, flags.size());
  uint32_t value[2] = {
      toLeInt(flags[0].data_),
      toLeInt(flags[1].data_),
  };
  ASSERT_EQ((unsigned)0xdeadbeef, value[0]);
  ASSERT_EQ((unsigned)0x11223344, value[1]);
}
