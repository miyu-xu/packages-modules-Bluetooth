/******************************************************************************
 *
 *  Copyright 2018 The Android Open Source Project
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

#include "gatt/database.h"

#include <base/strings/string_number_conversions.h>
#include <bluetooth/log.h>
#include <gtest/gtest.h>

#include "gatt/database_builder.h"
#include "stack/include/gattdefs.h"
#include "types/bluetooth/uuid.h"

using bluetooth::Uuid;
using namespace bluetooth;

namespace gatt {

namespace {
const Uuid PRIMARY_SERVICE = Uuid::From16Bit(GATT_UUID_PRI_SERVICE);
const Uuid SECONDARY_SERVICE = Uuid::From16Bit(GATT_UUID_SEC_SERVICE);
const Uuid INCLUDE = Uuid::From16Bit(GATT_UUID_INCLUDE_SERVICE);
const Uuid CHARACTERISTIC = Uuid::From16Bit(GATT_UUID_CHAR_DECLARE);
const Uuid CHARACTERISTIC_EXTENDED_PROPERTIES = Uuid::From16Bit(GATT_UUID_CHAR_EXT_PROP);

Uuid SERVICE_1_UUID = Uuid::FromString("1800");
Uuid SERVICE_2_UUID = Uuid::FromString("1801");
Uuid SERVICE_1_CHAR_1_UUID = Uuid::FromString("2a00");
Uuid SERVICE_1_CHAR_1_DESC_1_UUID = Uuid::FromString("2902");
}  // namespace

/* This test makes sure that each possible GATT cache element is properly
 * serialized into StoredAttribute */
TEST(GattDatabaseTest, serialize_deserialize_binary_test) {
  DatabaseBuilder builder;
  builder.AddService(0x0001, 0x000f, SERVICE_1_UUID, true);
  builder.AddService(0x0010, 0x001f, SERVICE_2_UUID, false);
  builder.AddIncludedService(0x0002, SERVICE_2_UUID, 0x0010, 0x001f);
  builder.AddCharacteristic(0x0003, 0x0004, SERVICE_1_CHAR_1_UUID, 0x02);
  builder.AddDescriptor(0x0005, SERVICE_1_CHAR_1_DESC_1_UUID);
  builder.AddDescriptor(0x0006, CHARACTERISTIC_EXTENDED_PROPERTIES);

  // Set value of only «Characteristic Extended Properties» descriptor
  builder.SetValueOfDescriptors({0x0001});

  Database db = builder.Build();
  std::vector<StoredAttribute> serialized = db.Serialize();

  // Primary Service
  EXPECT_EQ(serialized[0].handle, 0x0001);
  EXPECT_EQ(serialized[0].type, PRIMARY_SERVICE);
  EXPECT_EQ(serialized[0].value.service.uuid, SERVICE_1_UUID);
  EXPECT_EQ(serialized[0].value.service.end_handle, 0x000f);

  // Secondary Service
  EXPECT_EQ(serialized[1].handle, 0x0010);
  EXPECT_EQ(serialized[1].type, SECONDARY_SERVICE);
  EXPECT_EQ(serialized[1].value.service.uuid, SERVICE_2_UUID);
  EXPECT_EQ(serialized[1].value.service.end_handle, 0x001f);

  // Included Service
  EXPECT_EQ(serialized[2].handle, 0x0002);
  EXPECT_EQ(serialized[2].type, INCLUDE);
  EXPECT_EQ(serialized[2].value.included_service.handle, 0x0010);
  EXPECT_EQ(serialized[2].value.included_service.end_handle, 0x001f);
  EXPECT_EQ(serialized[2].value.included_service.uuid, SERVICE_2_UUID);

  // Characteristic
  EXPECT_EQ(serialized[3].handle, 0x0003);
  EXPECT_EQ(serialized[3].type, CHARACTERISTIC);
  EXPECT_EQ(serialized[3].value.characteristic.properties, 0x02);
  EXPECT_EQ(serialized[3].value.characteristic.value_handle, 0x0004);
  EXPECT_EQ(serialized[3].value.characteristic.uuid, SERVICE_1_CHAR_1_UUID);

  // Descriptor
  EXPECT_EQ(serialized[4].handle, 0x0005);
  EXPECT_EQ(serialized[4].type, SERVICE_1_CHAR_1_DESC_1_UUID);

  // Characteristic Extended Properties Descriptor
  EXPECT_EQ(serialized[5].handle, 0x0006);
  EXPECT_EQ(serialized[5].type, CHARACTERISTIC_EXTENDED_PROPERTIES);
  EXPECT_EQ(serialized[5].value.characteristic_extended_properties, 0x0001);
}

/* This test makes sure that Service represented in StoredAttribute have proper
 * binary format. */
TEST(GattCacheTest, stored_attribute_to_binary_service_test) {
  StoredAttribute attr;

  /* make sure padding at end of union is cleared */
  memset(&attr, 0, sizeof(attr));

  attr = {
          .handle = 0x0001,
          .type = PRIMARY_SERVICE,
          .value = {.service = {.uuid = Uuid::FromString("1800"), .end_handle = 0x001c}},
  };

  constexpr size_t len = sizeof(StoredAttribute);
  // clang-format off
  uint8_t binary_form[len] = {
      /*handle */ 0x01, 0x00,
      /* type*/ 0x00, 0x00, 0x28, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* service uuid */ 0x00, 0x00, 0x18, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* end handle */ 0x1C, 0x00,
      /* padding at end of union*/ 0x00, 0x00};
  // clang-format on

  // useful for debugging:
  // log::error("{}", base::HexEncode(&attr, len));

  // Do not compare last 2 bytes which are padding as
  // x86 can use non-zero padding causing the test to fail
  EXPECT_EQ(memcmp(binary_form, &attr, len - 2), 0);
}

/* This test makes sure that Service represented in StoredAttribute have proper
 * binary format. */
TEST(GattCacheTest, stored_attribute_to_binary_included_service_test) {
  StoredAttribute attr;

  /* make sure padding at end of union is cleared */
  memset(&attr, 0, sizeof(attr));

  attr = {
          .handle = 0x0001,
          .type = INCLUDE,
          .value = {.included_service =
                            {
                                    .handle = 0x0010,
                                    .end_handle = 0x001f,
                                    .uuid = Uuid::FromString("1801"),
                            }},
  };

  constexpr size_t len = sizeof(StoredAttribute);
  // clang-format off
  uint8_t binary_form[len] = {
      /*handle */ 0x01, 0x00,
      /* type*/ 0x00, 0x00, 0x28, 0x02, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* handle */ 0x10, 0x00,
      /* end handle */ 0x1f, 0x00,
      /* service uuid */ 0x00, 0x00, 0x18, 0x01, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
  // clang-format on

  // useful for debugging:
  // log::error("{}", base::HexEncode(&attr, len));
  EXPECT_EQ(memcmp(binary_form, &attr, len), 0);
}

/* This test makes sure that «Characteristic Extended Properties» descriptor
 * represented in StoredAttribute have proper binary format. */
TEST(GattCacheTest, stored_attribute_to_binary_characteristic_test) {
  StoredAttribute attr;

  /* make sure padding at end of union is cleared */
  memset(&attr, 0, sizeof(attr));

  attr = {
          .handle = 0x0002,
          .type = CHARACTERISTIC,
          .value = {.characteristic = {.properties = 0x02,
                                       .value_handle = 0x0003,
                                       .uuid = Uuid::FromString("2a00")}},
  };

  constexpr size_t len = sizeof(StoredAttribute);
  // clang-format off
  uint8_t binary_form[len] = {
      /*handle */ 0x02, 0x00,
      /* type */ 0x00, 0x00, 0x28, 0x03, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* properties */ 0x02,
      /* after properties there is one byte padding. This might cause troube
         on other platforms, investigate if it's ever a problem */ 0x00,
      /* value handle */ 0x03, 0x00,
      /* uuid */ 0x00, 0x00, 0x2a, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
  // clang-format on

  // useful for debugging:
  // log::error("{}", base::HexEncode(&attr, len));
  EXPECT_EQ(memcmp(binary_form, &attr, len), 0);
}

/* This test makes sure that Descriptor represented in StoredAttribute have
 * proper binary format. */
TEST(GattCacheTest, stored_attribute_to_binary_descriptor_test) {
  StoredAttribute attr;

  /* make sure padding at end of union is cleared */
  memset(&attr, 0, sizeof(attr));

  attr = {.handle = 0x0003,
          .type = Uuid::FromString("2902"),
          .value = {.characteristic_extended_properties = 0x00}};

  constexpr size_t len = sizeof(StoredAttribute);
  // clang-format off
  uint8_t binary_form[len] = {
      /*handle */ 0x03, 0x00,
      /* type */ 0x00, 0x00, 0x29, 0x02, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* clear padding    */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                             0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
  // clang-format on

  // useful for debugging:
  // log::error("{}", base::HexEncode(&attr, len));
  EXPECT_EQ(memcmp(binary_form, &attr, len), 0);
}

// Example from Bluetooth SPEC V5.2, Vol 3, Part G, APPENDIX B
TEST(GattDatabaseTest, hash_test) {
  DatabaseBuilder builder;
  builder.AddService(0x0001, 0x0005, Uuid::From16Bit(0x1800), true);
  builder.AddService(0x0006, 0x000D, Uuid::From16Bit(0x1801), true);
  builder.AddService(0x000E, 0x0013, Uuid::From16Bit(0x1808), true);
  builder.AddService(0x0014, 0xFFFF, Uuid::From16Bit(0x180F), false);

  builder.AddCharacteristic(0x0002, 0x0003, Uuid::From16Bit(0x2A00), 0x0A);
  builder.AddCharacteristic(0x0004, 0x0005, Uuid::From16Bit(0x2A01), 0x02);

  builder.AddCharacteristic(0x0007, 0x0008, Uuid::From16Bit(0x2A05), 0x20);
  builder.AddDescriptor(0x0009, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x000A, 0x000B, Uuid::From16Bit(0x2B29), 0x0A);
  builder.AddCharacteristic(0x000C, 0x000D, Uuid::From16Bit(0x2B2A), 0x02);

  builder.AddIncludedService(0x000F, Uuid::From16Bit(0x180F), 0x0014, 0x0016);
  builder.AddCharacteristic(0x0010, 0x0011, Uuid::From16Bit(0x2A18), 0xA2);
  builder.AddDescriptor(0x0012, Uuid::From16Bit(0x2902));
  builder.AddDescriptor(0x0013, Uuid::From16Bit(0x2900));

  builder.AddCharacteristic(0x0015, 0x0016, Uuid::From16Bit(0x2A19), 0x02);

  // set characteristic extended properties descriptor values
  std::vector<uint16_t> descriptorValues = {0x0000};
  builder.SetValueOfDescriptors(descriptorValues);

  Database db = builder.Build();

  // Big endian example from Bluetooth SPEC V5.2, Vol 3, Part G, APPENDIX B
  Octet16 expected_hash{0xF1, 0xCA, 0x2D, 0x48, 0xEC, 0xF5, 0x8B, 0xAC,
                        0x8A, 0x88, 0x30, 0xBB, 0xB9, 0xFB, 0xA9, 0x90};

  Octet16 hash = db.Hash();
  // Convert output hash from little endian to big endian
  std::reverse(hash.begin(), hash.end());

  EXPECT_EQ(hash, expected_hash);
}

/* This test makes sure that Descriptor represented in StoredAttribute have
 * proper binary format. */
TEST(GattCacheTest, stored_attribute_to_binary_characteristic_extended_properties_test) {
  StoredAttribute attr;

  /* make sure padding at end of union is cleared */
  memset(&attr, 0, sizeof(attr));

  attr = {.handle = 0x0003,
          .type = Uuid::FromString("2900"),
          .value = {.characteristic_extended_properties = 0x0001}};

  constexpr size_t len = sizeof(StoredAttribute);
  // clang-format off
  std::vector<uint8_t> binary_form {
      /*handle */ 0x03, 0x00,
      /* type */ 0x00, 0x00, 0x29, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* characteristic extended properties */ 0x01, 0x00,
      /* clear padding    */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                             0x00, 0x00, 0x00, 0x00};
  // clang-format on

  // useful for debugging:
  // log::error("{}", base::HexEncode(&attr, len));
  EXPECT_EQ(memcmp(binary_form.data(), &attr, len), 0);

  // Don't use memcmp, for better error messages.
  std::vector<uint8_t> copied(len, 0);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);

  EXPECT_EQ(binary_form, copied);
}

/* This test makes sure that Descriptor represented in StoredAttribute have
 * proper binary format. */
TEST(GattCacheTest, stored_attribute_serialized_to_binary_characteristic_extended_properties_test) {
  StoredAttribute attr;

  attr = {.handle = 0x0003,
          .type = Uuid::FromString("2900"),
          .value = {.characteristic_extended_properties = 0x0001}};

  constexpr size_t len = StoredAttribute::kSizeOnDisk;
  std::vector<uint8_t> serialized;
  StoredAttribute::SerializeStoredAttribute(attr, serialized);

  // clang-format off
  std::vector<uint8_t> binary_form {
      /*handle */ 0x03, 0x00,
      /* type */ 0x00, 0x00, 0x29, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB,
      /* characteristic extended properties */ 0x01, 0x00,
      /* clear padding    */ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                             0x00, 0x00, 0x00, 0x00};
  // clang-format on

  EXPECT_EQ(binary_form.size(), len);
  EXPECT_EQ(binary_form.size(), serialized.size());
  EXPECT_EQ(binary_form, serialized);
}

/* This test makes sure that Descriptor represented in StoredAttribute have
 * proper binary format. */
TEST(GattCacheTest, stored_attributes_serialized_to_binary_test) {
  // Allocate enough space so that no matter the layout, we don't overflow.
  uint8_t attr_bytes[StoredAttribute::kSizeOnDisk * 2];
  // This is the attribute we fill from a binary representation
  StoredAttribute attr;

  /*
  // Characteristic extended property
  attr = {.handle = 0x0003,
          .type = Uuid::FromString("2900"),
          .value.characteristic_extended_properties = 0x1234};
  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */

  memcpy(attr_bytes,
         "\x03\x00"                                                          // handle
         "\x00\x00\x29\x00\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // Uuid
         "\x34\x12"                                                          // extended property
         "\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
         "\x00",
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  std::vector<uint8_t> serialized;
  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  std::vector<uint8_t> copied(StoredAttribute::kSizeOnDisk, 0);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);

  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);
  /*
  // Primary Service
  attr = {
      .handle = 0x0203,
      .type = Uuid::FromString("2800"),
      .value.service =
          {
              .uuid = Uuid::FromString("4203"),
              .end_handle = 0x1203,
          },
  };
  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */
  memcpy(attr_bytes,
         "\x03\x02"                                                          // handle
         "\x00\x00\x28\x00\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // Type
         "\x00\x00\x42\x03\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // Uuid
         "\x03\x12"                                                          // end_handle
         "\x00\x00",
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);
  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);

  /*
  // Secondary Service
  attr = {
      .handle = 0x0304,
      .type = Uuid::FromString("2801"),
      .value.service =
          {
              .uuid = Uuid::FromString("4303"),
              .end_handle = 0x1203,
          },
  };

  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */
  memcpy(attr_bytes,
         "\x04\x03"                                                          // handle
         "\x00\x00\x28\x01\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // type
         "\x00\x00\x43\x03\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // UUID
         "\x03\x12"                                                          // end_handle
         "\x00\x000",
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);
  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);

  /*
  // Included Service
  attr = {
      .handle = 0x0103,
      .type = Uuid::FromString("2802"),
      .value.included_service =
          {
              .handle = 0x0134,
              .end_handle = 0x0138,
              .uuid = Uuid::FromString("3456"),
          },
  };
  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */

  memcpy(attr_bytes,
         "\x03\x01"                                                           // handle
         "\x00\x00\x28\x02\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"   // type
         "\x34\x01"                                                           // handle
         "\x38\x01"                                                           // end_handle
         "\x00\x00\x34\x56\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB",  // Uuid
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);
  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);

  /*
  // characteristic definition
  attr = {
      .handle = 0x0103,
      .type = Uuid::FromString("2803"),
      .value.characteristic = {.properties = 4,
                               .value_handle = 0x302,
                               .uuid = Uuid::FromString("3456")},
  };
  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */
  memcpy(attr_bytes,
         "\x03\x01"                                                           // handle
         "\x00\x00\x28\x03\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"   // type
         "\x04"                                                               // properties
         "\x00"                                                               // padding
         "\x02\x03"                                                           // value_handle
         "\x00\x00\x34\x56\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB",  // uuid
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);
  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);

  /*
  // Unknown Uuid
  attr = {
      .handle = 0x0103,
      .type = Uuid::FromString("4444"),
      .value.characteristic = {},
  };
  log::error("{}", base::HexEncode(&attr, StoredAttribute::kSizeOnDisk));
  */
  memcpy(attr_bytes,
         "\x03\x01"                                                          // handle
         "\x00\x00\x44\x44\x00\x00\x10\x00\x80\x00\x00\x80\x5F\x9B\x34\xFB"  // type
         "\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00"
         "\x00\x00",
         StoredAttribute::kSizeOnDisk);
  attr = *(StoredAttribute*)attr_bytes;

  StoredAttribute::SerializeStoredAttribute(attr, serialized);
  memcpy(copied.data(), &attr, StoredAttribute::kSizeOnDisk);

  EXPECT_EQ(serialized, copied);
  serialized.clear();
  copied = std::vector<uint8_t>(StoredAttribute::kSizeOnDisk, 0);
}

// Example from Bluetooth SPEC V5.2, Vol 3, Part G, APPENDIX B
TEST(GattDatabaseTest, serialized_hash_test) {
  DatabaseBuilder builder;
  builder.AddService(0x0001, 0x0005, Uuid::From16Bit(0x1800), true);
  builder.AddService(0x0006, 0x000D, Uuid::From16Bit(0x1801), true);
  builder.AddService(0x000E, 0x0013, Uuid::From16Bit(0x1808), true);
  builder.AddService(0x0014, 0xFFFF, Uuid::From16Bit(0x180F), false);

  builder.AddCharacteristic(0x0002, 0x0003, Uuid::From16Bit(0x2A00), 0x0A);
  builder.AddCharacteristic(0x0004, 0x0005, Uuid::From16Bit(0x2A01), 0x02);

  builder.AddCharacteristic(0x0007, 0x0008, Uuid::From16Bit(0x2A05), 0x20);
  builder.AddDescriptor(0x0009, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x000A, 0x000B, Uuid::From16Bit(0x2B29), 0x0A);
  builder.AddCharacteristic(0x000C, 0x000D, Uuid::From16Bit(0x2B2A), 0x02);

  builder.AddIncludedService(0x000F, Uuid::From16Bit(0x180F), 0x0014, 0x0016);
  builder.AddCharacteristic(0x0010, 0x0011, Uuid::From16Bit(0x2A18), 0xA2);
  builder.AddDescriptor(0x0012, Uuid::From16Bit(0x2902));
  builder.AddDescriptor(0x0013, Uuid::From16Bit(0x2900));

  builder.AddCharacteristic(0x0015, 0x0016, Uuid::From16Bit(0x2A19), 0x02);

  // set characteristic extended properties descriptor values
  std::vector<uint16_t> descriptorValues = {0x0000};
  builder.SetValueOfDescriptors(descriptorValues);

  Database db = builder.Build();

  auto serialized = db.Serialize();
  std::vector<uint8_t> bytes;
  for (auto attr : serialized) {
    StoredAttribute::SerializeStoredAttribute(attr, bytes);
  }
  std::vector<StoredAttribute> attr_from_disk(serialized.size());
  std::copy(bytes.cbegin(), bytes.cend(), (uint8_t*)attr_from_disk.data());
  bool is_successful = false;
  Database db_from_disk = gatt::Database::Deserialize(attr_from_disk, &is_successful);
  ASSERT_TRUE(is_successful);
  is_successful = false;
  Database db_from_serialized = gatt::Database::Deserialize(serialized, &is_successful);
  ASSERT_TRUE(is_successful);

  EXPECT_EQ(db_from_disk.Hash(), db_from_serialized.Hash());
}

// Example from b/392197849
TEST(GattDatabaseTest, serialized_hash_test_including_128_bit_uuid) {
  DatabaseBuilder builder;
  // 1.
  builder.AddService(0x0001, 0x000b, Uuid::From16Bit(0x1800), true);
  // 2.
  builder.AddService(0x0020, 0x0029, Uuid::From16Bit(0x1801), true);
  // 3.
  builder.AddService(0x0f00, 0x0f03, Uuid::From16Bit(0x180f), true);
  // 4.
  builder.AddService(0x0f06, 0x0f09, Uuid::From16Bit(0x180f), true);
  // 5.
  builder.AddService(0x4400, 0x4408, Uuid::From16Bit(0x1844), true);
  // 6.
  builder.AddService(0x4600, 0x460c, Uuid::From16Bit(0x1846), true);
  // 7.
  builder.AddService(0x4d00, 0x4d03, Uuid::From16Bit(0x184d), true);
  // 8.
  builder.AddService(0x4e00, 0x4e0f, Uuid::From16Bit(0x184e), true);
  // 9.
  builder.AddService(0x4f00, 0x4f05, Uuid::From16Bit(0x184f), true);
  // 10.
  builder.AddService(0x5000, 0x5012, Uuid::From16Bit(0x1850), true);
  // 11.
  builder.AddService(0x5300, 0x5301, Uuid::From16Bit(0x1853), true);
  // 12.
  builder.AddService(0x5500, 0x5502, Uuid::From16Bit(0x1855), true);
  // 13.
  builder.AddService(0x8000, 0x8005, Uuid::From16Bit(0xfe03), true);
  // 14.
  builder.AddService(0x8100, 0x8112, Uuid::From16Bit(0xfe2c), true);
  // 15.
  builder.AddService(
          0xa000, 0xa000,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66,
                                               0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66}}),
          true);
  builder.AddService(0xa001, 0xa002, Uuid::From16Bit(0x1c), true);
  builder.AddService(
          0xa002, 0xa003,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77,
                                               0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77}}),
          true);
  // 16.
  builder.AddService(
          0xa100, 0xa101,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80,
                                               0x00, 0x00, 0x90, 0x78, 0x56, 0x34, 0x12}}),
          true);

  // 1.
  builder.AddCharacteristic(0x0002, 0x0003, Uuid::From16Bit(0x2a00), 0x0A);
  builder.AddCharacteristic(0x0004, 0x0005, Uuid::From16Bit(0x2a01), 0x0a);
  builder.AddCharacteristic(0x0006, 0x0007, Uuid::From16Bit(0x2aa6), 0x02);
  builder.AddCharacteristic(0x0008, 0x0009, Uuid::From16Bit(0x2bf5), 0x02);
  builder.AddCharacteristic(0x000a, 0x000b, Uuid::From16Bit(0x2a04), 0x02);

  // 2.
  builder.AddCharacteristic(0x0021, 0x0022, Uuid::From16Bit(0x2a05), 0x20);
  builder.AddDescriptor(0x0023, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x0024, 0x0025, Uuid::From16Bit(0x2b29), 0x0a);
  builder.AddCharacteristic(0x0026, 0x0027, Uuid::From16Bit(0x2b3a), 0x0002);
  builder.AddCharacteristic(0x0028, 0x0029, Uuid::From16Bit(0x2b2a), 0x0002);

  // 3.
  builder.AddDescriptor(0x0f03, Uuid::From16Bit(0x2902));

  // 4.
  builder.AddCharacteristic(0x07, 0x08, Uuid::From16Bit(0x2a19), 0x12);
  builder.AddDescriptor(0x0f09, Uuid::From16Bit(0x2902));

  // 5.
  builder.AddCharacteristic(0x4401, 0x4402, Uuid::From16Bit(0x2b7d), 0x12);
  builder.AddDescriptor(0x4403, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4404, 0x4405, Uuid::From16Bit(0x2b7e), 0x0008);
  builder.AddCharacteristic(0x4406, 0x4407, Uuid::From16Bit(0x2b7f), 0x12);
  builder.AddDescriptor(0x4408, Uuid::From16Bit(0x2902));

  // 6.
  builder.AddCharacteristic(0x4601, 0x4602, Uuid::From16Bit(0x2b84), 0x12);
  builder.AddDescriptor(0x4603, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4604, 0x4605, Uuid::From16Bit(0x2b85), 0x12);
  builder.AddDescriptor(0x4606, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4607, 0x4608, Uuid::From16Bit(0x2b86), 0x1a);
  builder.AddDescriptor(0x4609, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x460a, 0x460b, Uuid::From16Bit(0x2b87), 0x02);
  builder.AddDescriptor(0x460c, Uuid::From16Bit(0x2902));

  // 7.
  builder.AddCharacteristic(0x4d01, 0x4d02, Uuid::From16Bit(0x2bc3), 0x1a);
  builder.AddDescriptor(0x4d03, Uuid::From16Bit(0x2902));

  // 8.
  builder.AddCharacteristic(0x4e01, 0x4e02, Uuid::From16Bit(0x2bc6), 0x1c);
  builder.AddDescriptor(0x4e03, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4e04, 0x4e05, Uuid::From16Bit(0x2c4), 0x12);
  builder.AddDescriptor(0x4e06, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4e07, 0x4e08, Uuid::From16Bit(0x2bc4), 0x12);
  builder.AddDescriptor(0x4e09, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4e0a, 0x4e0b, Uuid::From16Bit(0x2bc5), 0x12);
  builder.AddDescriptor(0x4e0c, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x4e0d, 0x4e0e, Uuid::From16Bit(0x2bc5), 0x12);
  builder.AddDescriptor(0x4e0f, Uuid::From16Bit(0x2902));

  // 9.
  builder.AddCharacteristic(0x4f01, 0x4f02, Uuid::From16Bit(0x2bc7), 0x0c);
  builder.AddCharacteristic(0x4f03, 0x4f04, Uuid::From16Bit(0x2bc8), 0x12);
  builder.AddDescriptor(0x4e05, Uuid::From16Bit(0x2902));

  // 10.
  builder.AddCharacteristic(0x5001, 0x5002, Uuid::From16Bit(0x2bcd), 0x12);
  builder.AddDescriptor(0x5003, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x5004, 0x5005, Uuid::From16Bit(0x2bce), 0x12);
  builder.AddDescriptor(0x5006, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x5007, 0x5008, Uuid::From16Bit(0x2bca), 0x1a);
  builder.AddDescriptor(0x5009, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x500a, 0x500b, Uuid::From16Bit(0x2bcc), 0x1a);
  builder.AddDescriptor(0x500c, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x500d, 0x500e, Uuid::From16Bit(0x2bc9), 0x12);
  builder.AddDescriptor(0x500f, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(0x5010, 0x5011, Uuid::From16Bit(0x2bcb), 0x12);
  builder.AddDescriptor(0x5012, Uuid::From16Bit(0x2902));

  // 11.

  // 12.
  builder.AddCharacteristic(0x5501, 0x5502, Uuid::From16Bit(0x2b51), 0x02);

  // 13.
  builder.AddCharacteristic(
          0x0801, 0x0802,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xf0, 0x4e, 0xb1, 0x77, 0x30, 0x05, 0x43, 0xa7, 0xac,
                                               0x61, 0xa3, 0x90, 0xdd, 0xf8, 0x30, 0x76}}),
          0x08);
  builder.AddCharacteristic(
          0x0803, 0x0804,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x2b, 0xee, 0xa0, 0x5b, 0x18, 0x79, 0x4b, 0xb4, 0x8a,
                                               0x2f, 0x72, 0x64, 0x1f, 0x82, 0x42, 0x0b}}),
          0x10);
  builder.AddDescriptor(0x8005, Uuid::From16Bit(0x2902));

  // 14.
  builder.AddCharacteristic(
          0x8101, 0x8102,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x33, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x02);
  builder.AddCharacteristic(
          0x8103, 0x8104,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x34, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x18);
  builder.AddDescriptor(0x8105, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(
          0x8106, 0x8107,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x35, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x18);
  builder.AddDescriptor(0x8108, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(
          0x8109, 0x810a,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x36, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x08);
  builder.AddCharacteristic(
          0x810b, 0x810c,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x37, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x18);
  builder.AddDescriptor(0x810d, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(
          0x810e, 0x810f,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x3a, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x1a);
  builder.AddDescriptor(0x8110, Uuid::From16Bit(0x2902));
  builder.AddCharacteristic(
          0x8111, 0x8112,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0xfe, 0x2c, 0x12, 0x39, 0x83, 0x66, 0x48, 0x14, 0x8e,
                                               0xb0, 0x01, 0xde, 0x32, 0x10, 0x0b, 0xea}}),
          0x02);

  // 15.
  builder.AddDescriptor(0xa003, Uuid::From16Bit(0x2902));

  // 16.
  builder.AddCharacteristic(
          0xa101, 0xa102,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x03, 0x00, 0x03, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80,
                                               0x00, 0x00, 0x92, 0x78, 0x56, 0x34, 0x12}}),
          0x0c);
  builder.AddDescriptor(0xa103, Uuid::From16Bit(0x2901));
  builder.AddCharacteristic(
          0xa104, 0xa105,
          Uuid::From128BitBE(Uuid::UUID128Bit{{0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80,
                                               0x00, 0x00, 0x91, 0x78, 0x56, 0x34, 0x12}}),
          0x10);
  builder.AddDescriptor(0xa106, Uuid::From16Bit(0x2902));
  builder.AddDescriptor(0xa107, Uuid::From16Bit(0x2901));

  // set characteristic extended properties descriptor values
  std::vector<uint16_t> descriptorValues = {0x0000};
  builder.SetValueOfDescriptors(descriptorValues);

  Database db = builder.Build();

  auto serialized = db.Serialize();
  std::vector<uint8_t> bytes;
  for (auto attr : serialized) {
    StoredAttribute::SerializeStoredAttribute(attr, bytes);
  }
  std::vector<StoredAttribute> attr_from_disk(serialized.size());
  std::copy(bytes.cbegin(), bytes.cend(), (uint8_t*)attr_from_disk.data());
  bool is_successful = false;
  Database db_from_disk = gatt::Database::Deserialize(attr_from_disk, &is_successful);
  ASSERT_TRUE(is_successful);
  is_successful = false;
  Database db_from_serialized = gatt::Database::Deserialize(serialized, &is_successful);
  ASSERT_TRUE(is_successful);

  EXPECT_EQ(db_from_disk.Hash(), db_from_serialized.Hash());
  Octet16 expected_hash{0xdc, 0xde, 0x9c, 0x26, 0x85, 0x63, 0xd1, 0x11, 0x53, 0xf8, 0x09, 0xae,
                        0x0a, 0x59, 0x88, 0x0e};
  EXPECT_EQ(db_from_disk.Hash(), expected_hash);
}
}  // namespace gatt
