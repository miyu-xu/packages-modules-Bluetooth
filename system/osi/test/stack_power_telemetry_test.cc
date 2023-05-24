/*
 */
#include "osi/include/stack_power_telemetry.h"

#include <gtest/gtest.h>

class PowerTelemetryTest : public ::testing::Test {
 protected:
  void SetUp() override {}
  void TearDown() override {}
};

TEST_F(PowerTelemetryTest, nop) {
  int fd = 1;
  power_telemetry::GetInstance().Dumpsys(fd);
}
