/*
 * Copyright 2023 The Android Open Source Project
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

#include <gtest/gtest.h>

#include "osi/include/log.h"
#include "stack/include/sdp_api.h"
#include "stack/include/sdpdefs.h"
#include "stack/sdp/sdpint.h"
#include "test/common/log_msg.h"

namespace {
constexpr char service_name[] = "TestServiceName";
constexpr uint32_t kFirstRecordHandle = 0x10000;
}  // namespace

class SdpDbTest : public ::testing::Test {
 protected:
  void SetUp() override {
    test_common_log_msg = [](uint32_t trace_set_mask, const char* msg) {
      LOG_INFO("%s", msg);
    };
    sdp_cb.trace_level = BT_TRACE_LEVEL_DEBUG;
    // Ensure no records exist in global state
    ASSERT_EQ((uint16_t)0, sdp_cb.server_db.num_records);
  }

  void TearDown() override {
    // Ensure all records have been deleted from global state
    ASSERT_EQ((uint16_t)0, sdp_cb.server_db.num_records);
  }
};

TEST_F(SdpDbTest, SDP_AddAttribute__create_record) {
  uint32_t record_handle = SDP_CreateRecord();

  ASSERT_NE((uint32_t)0, record_handle);
  ASSERT_EQ((uint16_t)1, sdp_cb.server_db.num_records);

  tSDP_RECORD* record = sdp_db_find_record(record_handle);
  ASSERT_TRUE(record != nullptr);

  // The sdp handle is always the first attribute
  ASSERT_EQ((uint16_t)1, record->num_attributes);
  ASSERT_EQ(kFirstRecordHandle, record->record_handle);
  ASSERT_EQ(sizeof(uint32_t), record->free_pad_ptr);

  ASSERT_TRUE(SDP_DeleteRecord(record_handle));
}

TEST_F(SdpDbTest, SDP_AddAttribute__add_service_name) {
  uint32_t record_handle = SDP_CreateRecord();

  ASSERT_NE((uint32_t)0, record_handle);
  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_SERVICE_NAME, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));

  tSDP_RECORD* record = sdp_db_find_record(record_handle);
  ASSERT_TRUE(record != nullptr);

  // The sdp handle is always the first attribute
  ASSERT_EQ((uint16_t)(1 /* record_handle */ + 1 /* service name */),
            record->num_attributes);
  ASSERT_EQ(kFirstRecordHandle, record->record_handle);
  ASSERT_EQ(sizeof(uint32_t) + strlen(service_name) + 1, record->free_pad_ptr);

  const tSDP_ATTRIBUTE* attribute = sdp_db_find_attr_in_rec(
      record, ATTR_ID_SERVICE_NAME, ATTR_ID_SERVICE_NAME);
  ASSERT_TRUE(attribute != nullptr);

  ASSERT_TRUE(SDP_DeleteRecord(record_handle));
}

TEST_F(SdpDbTest, SDP_AddAttribute__three_attributes) {
  uint32_t record_handle = SDP_CreateRecord();

  ASSERT_NE((uint32_t)0, record_handle);

  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_SERVICE_NAME, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));
  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_SERVICE_DESCRIPTION, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));
  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_PROVIDER_NAME, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));

  tSDP_RECORD* record = sdp_db_find_record(record_handle);
  ASSERT_TRUE(record != nullptr);

  // The sdp handle is always the first attribute
  ASSERT_EQ((uint16_t)(1 /* record_handle */ + 3 /* service name */),
            record->num_attributes);
  ASSERT_EQ(kFirstRecordHandle, record->record_handle);
  ASSERT_EQ(sizeof(uint32_t) + (3 * (strlen(service_name) + 1)),
            record->free_pad_ptr);

  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_NAME,
                                      ATTR_ID_SERVICE_NAME) != nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_DESCRIPTION,
                                      ATTR_ID_SERVICE_DESCRIPTION) != nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_PROVIDER_NAME,
                                      ATTR_ID_PROVIDER_NAME) != nullptr);

  ASSERT_TRUE(SDP_DeleteRecord(record_handle));
}

TEST_F(SdpDbTest, SDP_AddAttribute__too_many_attributes) {
  uint32_t record_handle = SDP_CreateRecord();
  ASSERT_NE((uint32_t)0, record_handle);

  uint8_t boolean = 1;
  for (size_t i = 0; i < SDP_MAX_REC_ATTR; i++) {
    ASSERT_TRUE(SDP_AddAttribute(record_handle, (uint16_t)i, BOOLEAN_DESC_TYPE,
                                 boolean, &boolean));
  }

  ASSERT_FALSE(SDP_AddAttribute(record_handle, SDP_MAX_REC_ATTR + 1,
                                BOOLEAN_DESC_TYPE, boolean, &boolean));
  ASSERT_TRUE(SDP_DeleteRecord(record_handle));
}

TEST_F(SdpDbTest, DISABLED_SDP_AddAttribute__three_attributes_replace_middle) {
  uint32_t record_handle = SDP_CreateRecord();

  ASSERT_NE((uint32_t)0, record_handle);

  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_SERVICE_NAME, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));
  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_SERVICE_DESCRIPTION, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));
  ASSERT_TRUE(SDP_AddAttribute(
      record_handle, ATTR_ID_PROVIDER_NAME, TEXT_STR_DESC_TYPE,
      (uint32_t)(strlen(service_name) + 1), (uint8_t*)service_name));

  tSDP_RECORD* record = sdp_db_find_record(record_handle);
  ASSERT_TRUE(record != nullptr);

  // The sdp handle is always the first attribute
  ASSERT_EQ((uint16_t)(1 /* record_handle */ + 3 /* attribute count */),
            record->num_attributes);
  ASSERT_EQ(kFirstRecordHandle, record->record_handle);
  ASSERT_EQ(sizeof(uint32_t) + (3 * (strlen(service_name) + 1)),
            record->free_pad_ptr);

  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_NAME,
                                      ATTR_ID_SERVICE_NAME) != nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_DESCRIPTION,
                                      ATTR_ID_SERVICE_DESCRIPTION) != nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_PROVIDER_NAME,
                                      ATTR_ID_PROVIDER_NAME) != nullptr);

  // Replace a middle attribute with a bad attribute
  ASSERT_FALSE(SDP_AddAttribute(record_handle, ATTR_ID_SERVICE_DESCRIPTION,
                                TEXT_STR_DESC_TYPE, (uint32_t)0,
                                (uint8_t*)nullptr));

  // Ensure database is still intact.
  // NOTE: The failed attribute addition still deletes that attribute even
  // if it was not added.
  ASSERT_EQ((uint16_t)(1 /* record_handle */ + 2 /* attribute count */),
            record->num_attributes);
  ASSERT_EQ(kFirstRecordHandle, record->record_handle);
  ASSERT_EQ(sizeof(uint32_t) + (2 * (strlen(service_name) + 1)),
            record->free_pad_ptr);

  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_NAME,
                                      ATTR_ID_SERVICE_NAME) != nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_SERVICE_DESCRIPTION,
                                      ATTR_ID_SERVICE_DESCRIPTION) == nullptr);
  ASSERT_TRUE(sdp_db_find_attr_in_rec(record, ATTR_ID_PROVIDER_NAME,
                                      ATTR_ID_PROVIDER_NAME) != nullptr);

  ASSERT_TRUE(SDP_DeleteRecord(record_handle));
}
