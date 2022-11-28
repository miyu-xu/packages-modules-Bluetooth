#include "device/include/profile_config.h"
#include "btcore/include/module.h"

#include <gtest/gtest.h>

#include "types/raw_address.h"
extern const module_t profile_config_module;

TEST(profileConfigTest, test_profile_feature_support) {
  module_init(&profile_config_module);

  EXPECT_TRUE(profile_config_get_interface()->is_map_0104_enabled());
  EXPECT_TRUE(profile_config_get_interface()->is_pbap_0102_enabled());
  EXPECT_TRUE(profile_config_get_interface()->is_pbap_sim_enabled());

  module_clean_up(&profile_config_module);
}
