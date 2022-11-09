/******************************************************************************
 *
 *  Copyright (C) 2022 Google, Inc.
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

#include "device/include/device_iot_config.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <sys/mman.h>

#include "btcore/include/module.h"
#include "btif/include/btif_common.h"
#include "device/src/device_iot_config_int.h"
#include "test/mock/mock_osi_alarm.h"
#include "test/mock/mock_osi_allocator.h"
#include "test/mock/mock_osi_config.h"
#include "test/mock/mock_osi_future.h"
#include "test/mock/mock_osi_properties.h"

using namespace testing;

const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};

std::map<std::string, int> mock_function_count_map;
extern module_t device_iot_config_module;

bt_status_t btif_transfer_context(tBTIF_CBACK* p_cback, uint16_t event,
                                  char* p_params, int param_len,
                                  tBTIF_COPY_CBACK* p_copy_cback) {
  mock_function_count_map[__func__]++;
  return BT_STATUS_SUCCESS;
}

struct alarm_t {
  alarm_t(const char* name){};
  int any_value;
};

struct future_t {
  future_t(void* value){};
  void* value;
};

struct alarm_t placeholder_alarm("");
struct future_t placeholder_future(NULL);
std::string true_val = "true";

class DeviceIotConfigModuleTest : public testing::Test {
 protected:
  void SetUp() override {
    test::mock::osi_alarm::alarm_new.body = [&](const char* name) -> alarm_t* {
      return &placeholder_alarm;
    };

    test::mock::osi_alarm::alarm_set.body =
        [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
            void* data) { return; };

    test::mock::osi_alarm::alarm_free.body = [](alarm_t* alarm) {};

    test::mock::osi_alarm::alarm_is_scheduled.body =
        [&](const alarm_t* alarm) -> bool { return false; };

    test::mock::osi_future::future_new_immediate.body =
        [&](void* value) -> future_t* { return &placeholder_future; };

    test::mock::osi_properties::osi_property_get.body =
        [](const char* key, char* value, const char* default_value) -> int {
      strncpy(value, true_val.c_str(), true_val.size());
      value[true_val.size()] = '\0';
      return 0;
    };

    test::mock::osi_config::config_new_empty.body =
        [&]() -> std::unique_ptr<config_t> {
      return std::make_unique<config_t>();
    };

    test::mock::osi_config::config_new.body =
        [&](const char* filename) -> std::unique_ptr<config_t> {
      return std::make_unique<config_t>();
    };

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, int def_value) { return def_value; };

    test::mock::osi_config::config_set_int.body =
        [&](config_t* config, const std::string& section,
            const std::string& key, int value) { return; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            const std::string* def_value) { return def_value; };

    test::mock::osi_config::config_set_string.body =
        [&](config_t* config, const std::string& section,
            const std::string& key, const std::string& value) { return; };

    test::mock::osi_allocator::osi_free.body = [&](void* ptr) {};

    bluetooth::common::InitFlags::Load(test_flags);

    mock_function_count_map.clear();
  }

  void TearDown() override {
    test::mock::osi_alarm::alarm_new = {};
    test::mock::osi_alarm::alarm_set = {};
    test::mock::osi_alarm::alarm_free = {};
    test::mock::osi_alarm::alarm_is_scheduled = {};
    test::mock::osi_future::future_new_immediate = {};
    test::mock::osi_properties::osi_property_get = {};
    test::mock::osi_config::config_new_empty = {};
    test::mock::osi_config::config_new = {};
    test::mock::osi_config::config_get_int = {};
    test::mock::osi_config::config_set_int = {};
    test::mock::osi_config::config_get_string = {};
    test::mock::osi_config::config_set_string = {};
    test::mock::osi_allocator::osi_free = {};
  }
};

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_iot_logging_not_enabled) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value;
  config_t* config_new_empty_return_value;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "false";
    errno = 0;
    int file_fd = -1;
    int backup_fd = -1;

    file_fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
                   S_IRUSR | S_IWUSR);
    EXPECT_TRUE(file_fd > 0);
    EXPECT_EQ(errno, 0);

    backup_fd = open(IOT_CONFIG_BACKUP_PATH,
                     O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, S_IRUSR | S_IWUSR);
    EXPECT_TRUE(backup_fd > 0);
    EXPECT_EQ(errno, 0);

    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), 0);
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), 0);

    device_iot_config_module_init();

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    EXPECT_EQ(mock_function_count_map["config_new"], 0);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "false";
    errno = 0;
    device_iot_config_module_init();
    EXPECT_EQ(errno, ENOENT);

    EXPECT_EQ(mock_function_count_map["config_new"], 0);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_is_factory_reset) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "true";
    config_new_return_value = NULL;
    config_new_empty_return_value = NULL;

    errno = 0;
    int file_fd = -1;
    int backup_fd = -1;

    file_fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
                   S_IRUSR | S_IWUSR);
    EXPECT_TRUE(file_fd > 0);
    EXPECT_EQ(errno, 0);

    backup_fd = open(IOT_CONFIG_BACKUP_PATH,
                     O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, S_IRUSR | S_IWUSR);
    EXPECT_TRUE(backup_fd > 0);
    EXPECT_EQ(errno, 0);

    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), 0);
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), 0);

    device_iot_config_module_init();

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    EXPECT_EQ(mock_function_count_map["config_new"], 2);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_no_config) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = NULL;
    config_new_empty_return_value = NULL;

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 2);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest, test_device_iot_config_module_init_original) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = DEVICE_IOT_INFO_CURRENT_VERSION;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest, test_device_iot_config_module_init_backup) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    if (strcmp(filename, IOT_CONFIG_BACKUP_PATH) == 0) {
      return std::unique_ptr<config_t>(config_new_return_value);
    }
    return std::unique_ptr<config_t>(nullptr);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = DEVICE_IOT_INFO_CURRENT_VERSION;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 2);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest, test_device_iot_config_module_init_new_file) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = NULL;
    config_new_empty_return_value = new config_t();
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 2);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_version_invalid) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = -1;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(
    DeviceIotConfigModuleTest,
    test_device_iot_config_module_init_version_new_config_new_empty_success) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "true";
    config_new_return_value = new config_t();
    config_new_empty_return_value = new config_t();
    int config_get_int_return_value = 2;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    int file_fd = -1;
    int backup_fd = -1;

    errno = 0;
    file_fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
                   S_IRUSR | S_IWUSR);
    EXPECT_TRUE(file_fd > 0);
    EXPECT_EQ(errno, 0);

    errno = 0;
    backup_fd = open(IOT_CONFIG_BACKUP_PATH,
                     O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, S_IRUSR | S_IWUSR);
    EXPECT_TRUE(backup_fd > 0);
    EXPECT_EQ(errno, 0);

    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), 0);
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), 0);

    device_iot_config_module_init();

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_version_new_config_new_empty_fail) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = 2;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    int file_fd = -1;
    int backup_fd = -1;

    errno = 0;
    file_fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
                   S_IRUSR | S_IWUSR);
    EXPECT_TRUE(file_fd > 0);
    EXPECT_EQ(errno, 0);

    errno = 0;
    backup_fd = open(IOT_CONFIG_BACKUP_PATH,
                     O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, S_IRUSR | S_IWUSR);
    EXPECT_TRUE(backup_fd > 0);
    EXPECT_EQ(errno, 0);

    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), 0);
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), 0);

    device_iot_config_module_init();

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_original_timestamp_null) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = DEVICE_IOT_INFO_CURRENT_VERSION;

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            const std::string* def_value) { return nullptr; };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_init_alarm_new_fail) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;
  config_t* config_new_return_value = NULL;
  config_t* config_new_empty_return_value = NULL;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(config_new_return_value);
  };

  test::mock::osi_config::config_new_empty.body = [&](void) {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  {
    mock_function_count_map.clear();

    enable_logging_property_get_value = "true";
    factory_reset_property_get_value = "false";
    config_new_return_value = new config_t();
    config_new_empty_return_value = NULL;
    int config_get_int_return_value = DEVICE_IOT_INFO_CURRENT_VERSION;
    std::string config_get_string_return_value(TIME_STRING_FORMAT);

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            int def_value) { return config_get_int_return_value; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, const std::string* def_value) {
          return &config_get_string_return_value;
        };

    test::mock::osi_alarm::alarm_new.body = [&](const char* name) {
      return nullptr;
    };

    device_iot_config_module_init();

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_new"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_start_up_logging_disabled) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "false";

  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    device_iot_config_module_start_up();

    EXPECT_EQ(mock_function_count_map["config_new"], 0);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_start_up_logging_enabled) {
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "true";

  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    device_iot_config_module_start_up();

    EXPECT_EQ(mock_function_count_map["config_new"], 0);
    EXPECT_EQ(mock_function_count_map["config_new_empty"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 0);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_shutdown_logging_disabled) {
  bool return_value;
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_alarm::alarm_is_scheduled.body =
      [&](const alarm_t* alarm) -> bool { return return_value; };

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "false";
  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    return_value = false;

    device_iot_config_module_shut_down();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_save"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
  test::mock::osi_alarm::alarm_is_scheduled.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_shutdown_logging_enabled) {
  bool return_value;
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_alarm::alarm_is_scheduled.body =
      [&](const alarm_t* alarm) -> bool { return return_value; };

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "true";
  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    return_value = false;

    device_iot_config_module_shut_down();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    device_iot_config_module_shut_down();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
  test::mock::osi_alarm::alarm_is_scheduled.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_clean_up_logging_disabled) {
  bool return_value;
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_alarm::alarm_is_scheduled.body =
      [&](const alarm_t* alarm) -> bool { return return_value; };

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "false";
  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    return_value = false;
    device_iot_config_module_clean_up();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_save"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
  test::mock::osi_alarm::alarm_is_scheduled.body = {};
}

TEST_F(DeviceIotConfigModuleTest,
       test_device_iot_config_module_clean_up_logging_enabled) {
  bool return_value;
  std::string enable_logging_property_get_value;
  std::string factory_reset_property_get_value;

  test::mock::osi_alarm::alarm_is_scheduled.body =
      [&](const alarm_t* alarm) -> bool { return return_value; };

  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    if (strcmp(key, PROPERTY_ENABLE_LOGGING) == 0) {
      strcpy(value, enable_logging_property_get_value.c_str());

    } else if (strcmp(key, PROPERTY_FACTORY_RESET) == 0) {
      strcpy(value, factory_reset_property_get_value.c_str());
    }
    return 0;
  };

  enable_logging_property_get_value = "true";
  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    return_value = false;
    device_iot_config_module_clean_up();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_save"], 0);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  device_iot_config_module_init();

  {
    mock_function_count_map.clear();

    return_value = true;
    device_iot_config_module_clean_up();

    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 2);
    EXPECT_EQ(mock_function_count_map["alarm_free"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
    EXPECT_EQ(mock_function_count_map["future_new_immediate"], 1);
  }

  test::mock::osi_properties::osi_property_get.body = {};
  test::mock::osi_config::config_new.body = {};
  test::mock::osi_config::config_new_empty.body = {};
  test::mock::osi_alarm::alarm_is_scheduled.body = {};
}

class DeviceIotConfigTest : public testing::Test {
 protected:
  void SetUp() override {
    bluetooth::common::InitFlags::Load(test_flags);

    test::mock::osi_alarm::alarm_new.body = [&](const char* name) -> alarm_t* {
      return &placeholder_alarm;
    };

    test::mock::osi_alarm::alarm_set.body =
        [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
            void* data) { return; };

    test::mock::osi_alarm::alarm_free.body = [](alarm_t* alarm) {};

    test::mock::osi_alarm::alarm_is_scheduled.body =
        [&](const alarm_t* alarm) -> bool { return false; };

    test::mock::osi_future::future_new_immediate.body =
        [&](void* value) -> future_t* { return &placeholder_future; };

    test::mock::osi_properties::osi_property_get.body =
        [](const char* key, char* value, const char* default_value) -> int {
      strncpy(value, true_val.c_str(), true_val.size());
      value[true_val.size()] = '\0';
      return 0;
    };

    test::mock::osi_config::config_new_empty.body =
        [&]() -> std::unique_ptr<config_t> {
      return std::make_unique<config_t>();
    };

    test::mock::osi_config::config_new.body =
        [&](const char* filename) -> std::unique_ptr<config_t> {
      return std::make_unique<config_t>();
    };

    test::mock::osi_config::config_get_int.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key, int def_value) { return def_value; };

    test::mock::osi_config::config_set_int.body =
        [&](config_t* config, const std::string& section,
            const std::string& key, int value) { return; };

    test::mock::osi_config::config_get_string.body =
        [&](const config_t& config, const std::string& section,
            const std::string& key,
            const std::string* def_value) { return def_value; };

    test::mock::osi_config::config_set_string.body =
        [&](config_t* config, const std::string& section,
            const std::string& key, const std::string& value) { return; };

    test::mock::osi_allocator::osi_free.body = [&](void* ptr) {};

    device_iot_config_module_init();
    device_iot_config_module_start_up();

    mock_function_count_map.clear();
  }

  void TearDown() override {
    test::mock::osi_alarm::alarm_new = {};
    test::mock::osi_alarm::alarm_set = {};
    test::mock::osi_alarm::alarm_free = {};
    test::mock::osi_alarm::alarm_is_scheduled = {};
    test::mock::osi_future::future_new_immediate = {};
    test::mock::osi_properties::osi_property_get = {};
    test::mock::osi_config::config_new_empty = {};
    test::mock::osi_config::config_new = {};
    test::mock::osi_config::config_get_int = {};
    test::mock::osi_config::config_set_int = {};
    test::mock::osi_config::config_get_string = {};
    test::mock::osi_config::config_set_string = {};
    test::mock::osi_allocator::osi_free = {};
  }
};

TEST_F(DeviceIotConfigTest, test_device_iot_config_open) {
  std::string actual_section, expected_filename = "temp.conf";
  config_t* return_value = NULL;

  test::mock::osi_config::config_new.body = [&](const char* filename) {
    return std::unique_ptr<config_t>(return_value);
  };

  {
    mock_function_count_map.clear();

    return_value = new config_t();

    auto ret = device_iot_config_open(expected_filename.c_str());
    EXPECT_EQ(ret.get(), return_value);

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = NULL;

    auto ret = device_iot_config_open(expected_filename.c_str());
    EXPECT_EQ(ret.get(), return_value);

    EXPECT_EQ(mock_function_count_map["config_new"], 1);
  }

  test::mock::osi_config::config_new.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_sections_sort_by_entry_key) {
  {
    config_t conf;
    device_iot_config_sections_sort_by_entry_key(&conf, NULL);
  }

  {
    config_t conf;
    conf.sections = {
        section_t{.entries =
                      {
                          entry_t{
                              .key = "a",
                          },
                          entry_t{
                              .key = "b",
                          },
                          entry_t{
                              .key = "c",
                          },
                          entry_t{
                              .key = "d",
                          },
                      }},

        section_t{.entries =
                      {
                          entry_t{
                              .key = "d",
                          },
                          entry_t{
                              .key = "c",
                          },
                          entry_t{
                              .key = "b",
                          },
                          entry_t{
                              .key = "a",
                          },
                      }},

    };
    device_iot_config_sections_sort_by_entry_key(
        &conf, [](const entry_t& first, const entry_t& second) {
          return first.key.compare(second.key) >= 0;
        });

    auto& sec1 = conf.sections.front();
    auto& sec2 = conf.sections.back();

    for (auto i = 0; i < 4; ++i) {
      EXPECT_EQ(sec1.entries.front().key, sec2.entries.front().key);
      sec1.entries.pop_front();
      sec2.entries.pop_front();
    }
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_has_section) {
  std::string actual_section, expected_section = "abc";
  bool return_value = false;

  test::mock::osi_config::config_has_section.body =
      [&](const config_t& config, const std::string& section) {
        actual_section = section;
        return return_value;
      };

  {
    mock_function_count_map.clear();

    EXPECT_EQ(device_iot_config_has_section(expected_section.c_str()),
              return_value);
    EXPECT_EQ(actual_section, expected_section);

    EXPECT_EQ(mock_function_count_map["config_has_section"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    EXPECT_EQ(device_iot_config_has_section(expected_section.c_str()),
              return_value);

    EXPECT_EQ(mock_function_count_map["config_has_section"], 1);
  }

  test::mock::osi_config::config_has_section.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_exist) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  bool return_value = false;

  test::mock::osi_config::config_has_key.body = [&](const config_t& config,
                                                    const std::string& section,
                                                    const std::string& key) {
    actual_section = section;
    actual_key = key;
    return return_value;
  };

  {
    mock_function_count_map.clear();

    EXPECT_EQ(
        device_iot_config_exist(expected_section.c_str(), expected_key.c_str()),
        return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    EXPECT_EQ(
        device_iot_config_exist(expected_section.c_str(), expected_key.c_str()),
        return_value);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
  }

  test::mock::osi_config::config_has_key.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_has_key_value) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  std::string expected_value_str = "xyz", actual_value_str;
  const std::string* actual_def_value = NULL;
  const std::string* return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        actual_def_value = def_value;
        return return_value;
      };

  {
    mock_function_count_map.clear();

    EXPECT_FALSE(device_iot_config_has_key_value(expected_section.c_str(),
                                                 expected_key.c_str(),
                                                 expected_value_str.c_str()));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    actual_value_str = "xyy";
    return_value = &actual_value_str;
    EXPECT_FALSE(device_iot_config_has_key_value(expected_section.c_str(),
                                                 expected_key.c_str(),
                                                 expected_value_str.c_str()));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    actual_value_str = "xy";
    return_value = &actual_value_str;
    EXPECT_FALSE(device_iot_config_has_key_value(expected_section.c_str(),
                                                 expected_key.c_str(),
                                                 expected_value_str.c_str()));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    actual_value_str = "xyyy";
    return_value = &actual_value_str;
    EXPECT_FALSE(device_iot_config_has_key_value(expected_section.c_str(),
                                                 expected_key.c_str(),
                                                 expected_value_str.c_str()));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    actual_value_str = "xyz";
    return_value = &actual_value_str;
    EXPECT_TRUE(device_iot_config_has_key_value(expected_section.c_str(),
                                                expected_key.c_str(),
                                                expected_value_str.c_str()));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_int) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  bool return_value = false;
  int int_value = 0, new_value = 0xff;

  test::mock::osi_config::config_has_key.body = [&](const config_t& config,
                                                    const std::string& section,
                                                    const std::string& key) {
    actual_section = section;
    actual_key = key;
    return return_value;
  };

  test::mock::osi_config::config_get_int.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, int def_value) { return new_value; };

  {
    mock_function_count_map.clear();

    EXPECT_EQ(device_iot_config_get_int(expected_section.c_str(),
                                        expected_key.c_str(), &int_value),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 0);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    EXPECT_EQ(device_iot_config_get_int(expected_section.c_str(),
                                        expected_key.c_str(), &int_value),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, new_value);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
  }

  test::mock::osi_config::config_has_key.body = {};
  test::mock::osi_config::config_get_int.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_get_int) {
  const RawAddress peer_addr{};
  std::string actual_section, actual_key,
      expected_section = "00:00:00:00:00:00", expected_key = "def";
  bool return_value = false;
  int int_value = 0, new_value = 0xff;

  test::mock::osi_config::config_has_key.body = [&](const config_t& config,
                                                    const std::string& section,
                                                    const std::string& key) {
    actual_section = section;
    actual_key = key;
    return return_value;
  };

  test::mock::osi_config::config_get_int.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, int def_value) { return new_value; };

  {
    mock_function_count_map.clear();

    EXPECT_EQ(device_iot_config_addr_get_int(peer_addr, expected_key.c_str(),
                                             &int_value),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 0);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    EXPECT_EQ(device_iot_config_addr_get_int(peer_addr, expected_key.c_str(),
                                             &int_value),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, new_value);

    EXPECT_EQ(mock_function_count_map["config_has_key"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
  }

  test::mock::osi_config::config_has_key.body = {};
  test::mock::osi_config::config_get_int.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_set_int) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  std::string string_return_value = "123456789";
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  int int_value = 123456789;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return &string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    EXPECT_TRUE(device_iot_config_set_int(expected_section.c_str(),
                                          expected_key.c_str(), int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "123";

    EXPECT_TRUE(device_iot_config_set_int(expected_section.c_str(),
                                          expected_key.c_str(), int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, old_string_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_set_int) {
  const RawAddress peer_addr{};
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value = "123456789";
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  int int_value = 123456789;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return &string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    EXPECT_TRUE(device_iot_config_addr_set_int(peer_addr, expected_key.c_str(),
                                               int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "123";

    EXPECT_TRUE(device_iot_config_addr_set_int(peer_addr, expected_key.c_str(),
                                               int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, old_string_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_int_add_one) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  int int_value = 0, get_default_value, set_value;

  test::mock::osi_config::config_get_int.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, int def_value) {
        actual_section = section;
        actual_key = key;
        get_default_value = def_value;
        return int_value;
      };

  test::mock::osi_config::config_set_int.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          int val) { set_value = val; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    int_value = -1;

    EXPECT_TRUE(device_iot_config_int_add_one(expected_section.c_str(),
                                              expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = 0;

    EXPECT_TRUE(device_iot_config_int_add_one(expected_section.c_str(),
                                              expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = 1;

    EXPECT_TRUE(device_iot_config_int_add_one(expected_section.c_str(),
                                              expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = INT_MAX;

    EXPECT_TRUE(device_iot_config_int_add_one(expected_section.c_str(),
                                              expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);
    EXPECT_EQ(set_value, INT_MIN);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = INT_MIN;

    EXPECT_TRUE(device_iot_config_int_add_one(expected_section.c_str(),
                                              expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_int.body = {};
  test::mock::osi_config::config_set_int.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_int_add_one) {
  const RawAddress peer_addr{};
  std::string actual_section, actual_key,
      expected_section = "00:00:00:00:00:00", expected_key = "def";
  int int_value = 0, get_default_value, set_value;

  test::mock::osi_config::config_get_int.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, int def_value) {
        actual_section = section;
        actual_key = key;
        get_default_value = def_value;
        return int_value;
      };

  test::mock::osi_config::config_set_int.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          int val) { set_value = val; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    int_value = -1;

    EXPECT_TRUE(
        device_iot_config_addr_int_add_one(peer_addr, expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = 0;

    EXPECT_TRUE(
        device_iot_config_addr_int_add_one(peer_addr, expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = 1;

    EXPECT_TRUE(
        device_iot_config_addr_int_add_one(peer_addr, expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = INT_MAX;

    EXPECT_TRUE(
        device_iot_config_addr_int_add_one(peer_addr, expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, int_value + 1);
    EXPECT_EQ(set_value, INT_MIN);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    int_value = INT_MIN;

    EXPECT_TRUE(
        device_iot_config_addr_int_add_one(peer_addr, expected_key.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(get_default_value, 0);
    EXPECT_EQ(set_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_int"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_int"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }
  test::mock::osi_config::config_get_int.body = {};
  test::mock::osi_config::config_set_int.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_hex) {
  std::string actual_section, actual_key,
      expected_section = "00:00:00:00:00:00", expected_key = "def";
  int int_value = 0;
  std::string string_value;
  std::string* get_string_return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  {
    mock_function_count_map.clear();

    EXPECT_FALSE(device_iot_config_get_hex(expected_section.c_str(),
                                           expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "g";
    get_string_return_value = &string_value;
    EXPECT_FALSE(device_iot_config_get_hex(expected_section.c_str(),
                                           expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "f";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 15);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "1";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 1);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-e";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -14);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-f";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -15);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0x7fffffff";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, INT_MAX);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-0x80000000";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, INT_MIN);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0xffffffff";
    get_string_return_value = &string_value;
    EXPECT_TRUE(device_iot_config_get_hex(expected_section.c_str(),
                                          expected_key.c_str(), &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -1);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_get_hex) {
  const RawAddress peer_addr{};
  std::string actual_section, actual_key,
      expected_section = "00:00:00:00:00:00", expected_key = "def";
  int int_value = 0;
  std::string string_value;
  std::string* get_string_return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  {
    mock_function_count_map.clear();

    EXPECT_FALSE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                                &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "g";
    get_string_return_value = &string_value;

    EXPECT_FALSE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                                &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "f";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 15);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "1";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, 1);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-e";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -14);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-f";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -15);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0x7fffffff";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, INT_MAX);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "-0x80000000";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, INT_MIN);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    string_value = "0xffffffff";
    get_string_return_value = &string_value;

    EXPECT_TRUE(device_iot_config_addr_get_hex(peer_addr, expected_key.c_str(),
                                               &int_value));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(int_value, -1);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_set_hex) {
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  int int_value, byte_num;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    string_return_value = "01";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "0001";
    int_value = 1;
    byte_num = 2;
    get_string_return_value = &string_return_value;
    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "000001";
    int_value = 1;
    byte_num = 3;
    get_string_return_value = &string_return_value;
    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "00000001";
    int_value = 1;
    byte_num = 4;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "";
    int_value = 1;
    byte_num = 0;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "";
    int_value = 1;
    byte_num = 5;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "ff";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;
    std::string expected_string_value = "01";

    EXPECT_TRUE(device_iot_config_set_hex(
        expected_section.c_str(), expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, expected_string_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_set_hex) {
  const RawAddress peer_addr{};
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  int int_value = 123456789;
  int byte_num = 1;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    string_return_value = "01";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "0001";
    int_value = 1;
    byte_num = 2;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "000001";
    int_value = 1;
    byte_num = 3;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "00000001";
    int_value = 1;
    byte_num = 4;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "";
    int_value = 1;
    byte_num = 0;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "";
    int_value = 1;
    byte_num = 5;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "ff";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;
    std::string expected_string_value = "01";

    EXPECT_TRUE(device_iot_config_addr_set_hex(peer_addr, expected_key.c_str(),
                                               int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, expected_string_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_set_hex_if_greater) {
  const RawAddress peer_addr{};
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  int int_value = 123456789;
  int byte_num = 1;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    string_return_value = "00";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex_if_greater(
        peer_addr, expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 2);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "01";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex_if_greater(
        peer_addr, expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "02";
    int_value = 1;
    byte_num = 1;
    get_string_return_value = &string_return_value;

    EXPECT_TRUE(device_iot_config_addr_set_hex_if_greater(
        peer_addr, expected_key.c_str(), int_value, byte_num));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_str) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  std::string actual_value_str;
  const std::string* actual_def_value = NULL;
  const std::string* return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        actual_def_value = def_value;
        return return_value;
      };

  {
    mock_function_count_map.clear();

    int initial_size_bytes = 30;
    int size_bytes = initial_size_bytes;
    char get_value_str[size_bytes];
    EXPECT_FALSE(device_iot_config_get_str(expected_section.c_str(),
                                           expected_key.c_str(), get_value_str,
                                           &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, initial_size_bytes);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    int initial_size_bytes = 30;
    int size_bytes = initial_size_bytes;
    char get_value_str[size_bytes];

    actual_value_str = "abc";
    return_value = &actual_value_str;
    EXPECT_TRUE(device_iot_config_get_str(expected_section.c_str(),
                                          expected_key.c_str(), get_value_str,
                                          &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, (int)actual_value_str.length() + 1);
    EXPECT_TRUE(strncmp(get_value_str, actual_value_str.c_str(), size_bytes) ==
                0);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_set_str) {
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string input_value;
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  std::string str_value;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    string_return_value = "01";
    get_string_return_value = &string_return_value;

    input_value = "01";
    EXPECT_TRUE(device_iot_config_set_str(
        expected_section.c_str(), expected_key.c_str(), input_value.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "02";
    get_string_return_value = &string_return_value;

    input_value = "01";
    EXPECT_TRUE(device_iot_config_set_str(
        expected_section.c_str(), expected_key.c_str(), input_value.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, input_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_set_str) {
  const RawAddress peer_addr{};
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string input_value;
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  std::string str_value;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  {
    mock_function_count_map.clear();

    string_return_value = "01";
    get_string_return_value = &string_return_value;
    input_value = "01";

    EXPECT_TRUE(device_iot_config_addr_set_str(peer_addr, expected_key.c_str(),
                                               input_value.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
  }

  {
    mock_function_count_map.clear();

    string_return_value = "02";
    get_string_return_value = &string_return_value;
    input_value = "01";

    EXPECT_TRUE(device_iot_config_addr_set_str(peer_addr, expected_key.c_str(),
                                               input_value.c_str()));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(new_string_value, input_value);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_bin) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  std::string actual_value_str;
  const std::string* actual_def_value = NULL;
  const std::string* return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        actual_def_value = def_value;
        return return_value;
      };

  {
    mock_function_count_map.clear();

    size_t initial_size_bytes = 3;
    size_t size_bytes = initial_size_bytes;
    uint8_t value[size_bytes];

    EXPECT_FALSE(device_iot_config_get_bin(
        expected_section.c_str(), expected_key.c_str(), value, &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, initial_size_bytes);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    size_t initial_size_bytes = 3;
    size_t size_bytes = initial_size_bytes;
    uint8_t value[size_bytes];
    actual_value_str = "abc";
    return_value = &actual_value_str;

    EXPECT_FALSE(device_iot_config_get_bin(
        expected_section.c_str(), expected_key.c_str(), value, &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, initial_size_bytes);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    size_t initial_size_bytes = 3;
    size_t size_bytes = initial_size_bytes;
    uint8_t value[size_bytes];
    actual_value_str = "aabbccdd";
    return_value = &actual_value_str;

    EXPECT_FALSE(device_iot_config_get_bin(
        expected_section.c_str(), expected_key.c_str(), value, &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, initial_size_bytes);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    size_t initial_size_bytes = 3;
    size_t size_bytes = initial_size_bytes;
    uint8_t value[size_bytes];
    actual_value_str = "abcdefgh";
    return_value = &actual_value_str;

    EXPECT_FALSE(device_iot_config_get_bin(
        expected_section.c_str(), expected_key.c_str(), value, &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, initial_size_bytes);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();

    size_t initial_size_bytes = 3;
    size_t size_bytes = initial_size_bytes;
    uint8_t value[size_bytes];
    actual_value_str = "abcdef";
    return_value = &actual_value_str;

    EXPECT_TRUE(device_iot_config_get_bin(
        expected_section.c_str(), expected_key.c_str(), value, &size_bytes));
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(size_bytes, actual_value_str.length() / 2);

    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_bin_length) {
  std::string actual_section, actual_key, expected_section = "abc",
                                          expected_key = "def";
  std::string actual_value_str;
  const std::string* actual_def_value = NULL;
  const std::string* return_value = NULL;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        actual_def_value = def_value;
        return return_value;
      };

  {
    mock_function_count_map.clear();
    EXPECT_EQ(device_iot_config_get_bin_length(expected_section.c_str(),
                                               expected_key.c_str()),
              0u);
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();
    actual_value_str = "abc";
    return_value = &actual_value_str;

    EXPECT_EQ(device_iot_config_get_bin_length(expected_section.c_str(),
                                               expected_key.c_str()),
              0u);
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();
    actual_value_str = "aabbccdd";
    return_value = &actual_value_str;

    EXPECT_EQ(device_iot_config_get_bin_length(expected_section.c_str(),
                                               expected_key.c_str()),
              4u);
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();
    /* does not check if characters are correct*/
    actual_value_str = "abcdefgh";
    return_value = &actual_value_str;

    EXPECT_EQ(device_iot_config_get_bin_length(expected_section.c_str(),
                                               expected_key.c_str()),
              4u);
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  {
    mock_function_count_map.clear();
    actual_value_str = "abcdef";
    return_value = &actual_value_str;

    EXPECT_EQ(device_iot_config_get_bin_length(expected_section.c_str(),
                                               expected_key.c_str()),
              3u);
    EXPECT_TRUE(actual_def_value == NULL);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
  }

  test::mock::osi_config::config_get_string.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_set_bin) {
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  std::string str_value;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  test::mock::osi_allocator::osi_calloc.body = [&](size_t size) {
    return new char[size];
  };

  {
    mock_function_count_map.clear();
    string_return_value = "010203";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_TRUE(device_iot_config_set_bin(
        expected_section.c_str(), expected_key.c_str(), input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    string_return_value = "\0";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = 0;

    EXPECT_TRUE(device_iot_config_set_bin(
        expected_section.c_str(), expected_key.c_str(), input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    string_return_value = "010101";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_TRUE(device_iot_config_set_bin(
        expected_section.c_str(), expected_key.c_str(), input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    test::mock::osi_allocator::osi_calloc.body = [&](size_t size) {
      return nullptr;
    };

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_FALSE(device_iot_config_set_bin(
        expected_section.c_str(), expected_key.c_str(), input_value, length));

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 0);
  }

  test::mock::osi_allocator::osi_calloc.body = {};
  test::mock::osi_allocator::osi_free.body = {};
  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_addr_set_bin) {
  const RawAddress peer_addr{};
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  std::string string_return_value;
  std::string old_string_value = string_return_value;
  std::string new_string_value;
  std::string* get_string_return_value = NULL;
  std::string str_value;

  test::mock::osi_config::config_get_string.body =
      [&](const config_t& config, const std::string& section,
          const std::string& key, const std::string* def_value) {
        actual_section = section;
        actual_key = key;
        return get_string_return_value;
      };

  test::mock::osi_config::config_set_string.body =
      [&](config_t* config, const std::string& section, const std::string& key,
          const std::string& value) { new_string_value = value; };

  test::mock::osi_alarm::alarm_set.body =
      [&](alarm_t* alarm, uint64_t interval_ms, alarm_callback_t cb,
          void* data) {};

  test::mock::osi_allocator::osi_calloc.body = [&](size_t size) {
    return new char[size];
  };

  {
    mock_function_count_map.clear();
    string_return_value = "010203";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_TRUE(device_iot_config_addr_set_bin(peer_addr, expected_key.c_str(),
                                               input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    string_return_value = "\0";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = 0;

    EXPECT_TRUE(device_iot_config_addr_set_bin(peer_addr, expected_key.c_str(),
                                               input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    string_return_value = "010101";
    get_string_return_value = &string_return_value;

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_TRUE(device_iot_config_addr_set_bin(peer_addr, expected_key.c_str(),
                                               input_value, length));
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
    EXPECT_EQ(mock_function_count_map["osi_free"], 1);
  }

  {
    mock_function_count_map.clear();
    test::mock::osi_allocator::osi_calloc.body = [&](size_t size) {
      return nullptr;
    };

    uint8_t input_value[] = {0x01, 0x02, 0x03};
    size_t length = sizeof(input_value);

    EXPECT_FALSE(device_iot_config_addr_set_bin(peer_addr, expected_key.c_str(),
                                                input_value, length));

    EXPECT_EQ(mock_function_count_map["osi_calloc"], 1);
    EXPECT_EQ(mock_function_count_map["config_get_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_set"], 0);
    EXPECT_EQ(mock_function_count_map["osi_free"], 0);
  }

  test::mock::osi_allocator::osi_calloc.body = {};
  test::mock::osi_allocator::osi_free.body = {};
  test::mock::osi_config::config_get_string.body = {};
  test::mock::osi_config::config_set_string.body = {};
  test::mock::osi_alarm::alarm_set.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_remove) {
  std::string actual_key, expected_key = "def";
  std::string actual_section, expected_section = "00:00:00:00:00:00";
  bool return_value;

  test::mock::osi_config::config_remove_key.body =
      [&](config_t* config, const std::string& section,
          const std::string& key) {
        actual_section = section;
        actual_key = key;
        return return_value;
      };

  {
    mock_function_count_map.clear();

    return_value = false;

    EXPECT_EQ(device_iot_config_remove(expected_section.c_str(),
                                       expected_key.c_str()),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_remove_key"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    EXPECT_EQ(device_iot_config_remove(expected_section.c_str(),
                                       expected_key.c_str()),
              return_value);
    EXPECT_EQ(actual_section, expected_section);
    EXPECT_EQ(actual_key, expected_key);

    EXPECT_EQ(mock_function_count_map["config_remove_key"], 1);
  }

  test::mock::osi_config::config_remove_key.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_save) {
  {
    mock_function_count_map.clear();

    device_iot_config_save();

    EXPECT_EQ(mock_function_count_map["alarm_set"], 1);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_flush) {
  bool return_value;

  test::mock::osi_alarm::alarm_is_scheduled.body =
      [&](const alarm_t* alarm) -> bool { return return_value; };

  {
    mock_function_count_map.clear();

    return_value = false;

    device_iot_config_flush();

    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }

  {
    mock_function_count_map.clear();

    return_value = true;

    device_iot_config_flush();

    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_is_scheduled"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }

  test::mock::osi_alarm::alarm_is_scheduled.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_clear) {
  config_t* config_new_empty_return_value;
  bool config_save_return_value;

  test::mock::osi_alarm::alarm_cancel.body = [&](alarm_t* alarm) {};

  test::mock::osi_config::config_new_empty.body = [&]() {
    return std::unique_ptr<config_t>(config_new_empty_return_value);
  };

  test::mock::osi_config::config_save.body =
      [&](const config_t& config, const std::string& filename) -> bool {
    return config_save_return_value;
  };

  {
    mock_function_count_map.clear();

    config_new_empty_return_value = new config_t();
    config_save_return_value = false;

    EXPECT_FALSE(device_iot_config_clear());

    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }

  {
    mock_function_count_map.clear();

    config_new_empty_return_value = new config_t();
    config_save_return_value = true;

    EXPECT_TRUE(device_iot_config_clear());

    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }

  {
    mock_function_count_map.clear();

    config_new_empty_return_value = NULL;

    EXPECT_FALSE(device_iot_config_clear());

    EXPECT_EQ(mock_function_count_map["config_new_empty"], 1);
    EXPECT_EQ(mock_function_count_map["alarm_cancel"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 0);
  }

  test::mock::osi_alarm::alarm_cancel.body = {};
  test::mock::osi_config::config_new_empty.body = {};
  test::mock::osi_config::config_save.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_timer_save_cb) {
  {
    mock_function_count_map.clear();

    device_iot_config_timer_save_cb(NULL);

    EXPECT_EQ(mock_function_count_map["btif_transfer_context"], 1);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_set_modified_time) {
  {
    mock_function_count_map.clear();

    device_iot_config_set_modified_time();

    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_get_device_num) {
  {
    config_t config;
    auto num = device_iot_config_get_device_num(&config);
    EXPECT_EQ(num, 0);
  }

  {
    section_t section1 = {.name = "00:01:02:03:04:05"};
    section_t section2 = {.name = "01:01:01:01:01:01"};
    section_t section3 = {.name = "00:00:00:00:00:00"};
    section_t section4 = {.name = ""};
    config_t config;
    config.sections.push_back(section1);
    config.sections.push_back(section2);
    config.sections.push_back(section3);
    config.sections.push_back(section4);
    auto num = device_iot_config_get_device_num(&config);
    EXPECT_EQ(num, 3);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_restrict_device_num) {
  section_t section = {.name = "00:01:02:03:04:05"};

  {
    config_t config;

    EXPECT_EQ(device_iot_config_get_device_num(&config), 0);
    device_iot_config_restrict_device_num(&config);
    EXPECT_EQ(device_iot_config_get_device_num(&config), 0);
  }

  {
    int section_count = DEVICES_MAX_NUM_IN_IOT_INFO_FILE;
    int expected_count = section_count;
    config_t config;
    for (int i = 0; i < section_count; ++i) {
      config.sections.push_back(section);
    }

    EXPECT_EQ(device_iot_config_get_device_num(&config), section_count);
    device_iot_config_restrict_device_num(&config);
    EXPECT_EQ(device_iot_config_get_device_num(&config), expected_count);
  }

  {
    int section_count = DEVICES_MAX_NUM_IN_IOT_INFO_FILE + 1;
    int expected_count = DEVICES_MAX_NUM_IN_IOT_INFO_FILE - DEVICES_NUM_MARGIN;
    config_t config;
    for (int i = 0; i < section_count; ++i) {
      config.sections.push_back(section);
    }

    EXPECT_EQ(device_iot_config_get_device_num(&config), section_count);
    device_iot_config_restrict_device_num(&config);
    EXPECT_EQ(device_iot_config_get_device_num(&config), expected_count);
  }

  {
    int section_count = 2 * DEVICES_MAX_NUM_IN_IOT_INFO_FILE;
    int expected_count = DEVICES_MAX_NUM_IN_IOT_INFO_FILE - DEVICES_NUM_MARGIN;
    config_t config;
    for (int i = 0; i < section_count; ++i) {
      config.sections.push_back(section);
    }

    EXPECT_EQ(device_iot_config_get_device_num(&config), section_count);
    device_iot_config_restrict_device_num(&config);
    EXPECT_EQ(device_iot_config_get_device_num(&config), expected_count);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_compare_key) {
  {
    entry_t first =
                {
                    .key = "NotProfile/a",
                },
            second = {
                .key = "NotProfile/b",
            };

    EXPECT_TRUE(device_iot_config_compare_key(first, second));
  }

  {
    entry_t first =
                {
                    .key = "Profile/a",
                },
            second = {
                .key = "Profile/b",
            };

    EXPECT_TRUE(device_iot_config_compare_key(first, second));
  }

  {
    entry_t first =
                {
                    .key = "Profile/b",
                },
            second = {
                .key = "Profile/a",
            };

    EXPECT_FALSE(device_iot_config_compare_key(first, second));
  }

  {
    entry_t first =
                {
                    .key = "Profile/b",
                },
            second = {
                .key = "NotProfile/a",
            };

    EXPECT_FALSE(device_iot_config_compare_key(first, second));
  }

  {
    entry_t first =
                {
                    .key = "NotProfile/b",
                },
            second = {
                .key = "Profile/a",
            };

    EXPECT_TRUE(device_iot_config_compare_key(first, second));
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_write) {
  test::mock::osi_config::config_save.body =
      [&](const config_t& config, const std::string& filename) -> bool {
    return true;
  };

  {
    mock_function_count_map.clear();

    int event = IOT_CONFIG_FLUSH_EVT;
    device_iot_config_write(event, NULL);

    EXPECT_EQ(mock_function_count_map["config_set_string"], 0);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }

  {
    mock_function_count_map.clear();

    int event = IOT_CONFIG_SAVE_TIMER_FIRED_EVT;
    device_iot_config_write(event, NULL);

    EXPECT_EQ(mock_function_count_map["config_set_string"], 1);
    EXPECT_EQ(mock_function_count_map["config_save"], 1);
  }
  test::mock::osi_config::config_save.body = {};
}

TEST_F(DeviceIotConfigTest, test_device_debug_iot_config_dump) {
  {
    errno = 0;
    int fd = -1;
    const int BUF_SIZE = 100;
    char buf[BUF_SIZE] = {0};

    fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
              S_IRUSR | S_IWUSR);
    EXPECT_TRUE(fd > 0);
    EXPECT_EQ(errno, 0);

    lseek(fd, 0, SEEK_SET);
    auto bytes_read = read(fd, buf, BUF_SIZE);
    EXPECT_EQ(bytes_read, 0);
    EXPECT_EQ(errno, 0);
    lseek(fd, 0, SEEK_SET);

    device_debug_iot_config_dump(fd);

    lseek(fd, 0, SEEK_SET);
    bytes_read = read(fd, buf, BUF_SIZE);
    EXPECT_TRUE(bytes_read > 0);
    EXPECT_EQ(errno, 0);
    lseek(fd, 0, SEEK_SET);

    close(fd);
  }
}

TEST_F(DeviceIotConfigTest, test_device_iot_config_is_factory_reset) {
  int return_value;
  std::string value_set;
  test::mock::osi_properties::osi_property_get.body =
      [&](const char* key, char* value, const char* default_value) -> int {
    strcpy(value, value_set.c_str());
    return return_value;
  };

  {
    value_set = "false";
    EXPECT_FALSE(device_iot_config_is_factory_reset());
  }

  {
    value_set = "placeholder";
    EXPECT_FALSE(device_iot_config_is_factory_reset());
  }

  {
    value_set = "";
    EXPECT_FALSE(device_iot_config_is_factory_reset());
  }

  {
    value_set = "true";
    EXPECT_TRUE(device_iot_config_is_factory_reset());
  }
}

TEST_F(DeviceIotConfigTest, test_device_debug_iot_config_delete_files) {
  {
    errno = 0;
    int file_fd = -1;
    int backup_fd = -1;

    file_fd = open(IOT_CONFIG_FILE_PATH, O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC,
                   S_IRUSR | S_IWUSR);
    EXPECT_TRUE(file_fd > 0);
    EXPECT_EQ(errno, 0);

    backup_fd = open(IOT_CONFIG_BACKUP_PATH,
                     O_CREAT | O_RDWR | O_TRUNC | O_CLOEXEC, S_IRUSR | S_IWUSR);
    EXPECT_TRUE(backup_fd > 0);
    EXPECT_EQ(errno, 0);

    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), 0);
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), 0);

    device_iot_config_delete_files();

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_FILE_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);

    errno = 0;
    EXPECT_EQ(access(IOT_CONFIG_BACKUP_PATH, F_OK), -1);
    EXPECT_EQ(errno, ENOENT);
  }
}