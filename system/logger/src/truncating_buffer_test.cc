

#include <gtest/gtest.h>

#include "truncating_buffer.h"

class TruncatingBufferTest : public ::testing::Test {};

TEST_F(TruncatingBufferTest, test_truncates) {
  constexpr size_t max_buffer_len = 8;
  logger::truncating_buffer<max_buffer_len> buffer;
  for (char i = 0; i < max_buffer_len; i++) {
    buffer.push_back(i);
    ASSERT_TRUE(buffer.len == (i+1));
  }
  for (char i = max_buffer_len; i < 2*max_buffer_len; i++) {
    buffer.push_back(i);
    ASSERT_TRUE(buffer.len == max_buffer_len);
  }
}
