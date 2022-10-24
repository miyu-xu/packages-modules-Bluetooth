/*
 * Copyright 2022 The Android Open Source Project
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

#include <fcntl.h>
#include <gtest/gtest.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <fstream>
#include <iostream>

#include "aptXbtenc.h"

#define BYTES_PER_CODEWORD 16

class LibAptxEncTest : public ::testing::Test {
 private:
  void* aptxbtenc = nullptr;

 protected:
  void SetUp() override {
    aptxbtenc = malloc(SizeofAptxbtenc());
    ASSERT_NE(aptxbtenc, nullptr);
    ASSERT_EQ(aptxbtenc_init(aptxbtenc, 0), 0);
  }

  void TearDown() override { free(aptxbtenc); }

  void codeword_cmp(const uint16_t pcm[8], const uint32_t codeword) {
    uint32_t pcmL[4];
    uint32_t pcmR[4];
    for (size_t i = 0; i < 4; i++) {
      pcmL[i] = pcm[2 * i];
      pcmR[i] = pcm[(2 * i) + 1];
    }
    uint32_t encoded_sample;
    aptxbtenc_encodestereo(aptxbtenc, &pcmL, &pcmR, &encoded_sample);
    ASSERT_EQ(encoded_sample, codeword);
  }
};

TEST_F(LibAptxEncTest, encoder_size) { ASSERT_EQ(SizeofAptxbtenc(), 5008); }

static int open_non_blocking(const char* filename) {
  int fd = open(filename, O_RDONLY);
  if (fd == -1) {
    printf("Failed to open %s: %s", filename, strerror(errno));
    return -1;
  }
  int flags = fcntl(fd, F_GETFL, 0);
  if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) == -1) {
    printf("Failed to apply non-blocking on %s: %s", filename, strerror(errno));
    return -1;
  }
  return fd;
}

TEST_F(LibAptxEncTest, encode_fake_data) {
  const char input[] =
      "012345678901234567890123456789012345678901234567890123456789012345678901"
      "23456789";
  const uint32_t aptx_codeword[] = {1270827967, 134154239, 670640127,
                                    1280265295, 2485752873};

  ASSERT_EQ((sizeof(input) - 1) % BYTES_PER_CODEWORD, 0);
  ASSERT_EQ((sizeof(input) - 1) / BYTES_PER_CODEWORD,
            sizeof(aptx_codeword) / sizeof(uint32_t));

  size_t idx = 0;

  uint16_t pcm[8];
  while (idx * BYTES_PER_CODEWORD < sizeof(input) - 1) {
    memcpy(pcm, input + idx * BYTES_PER_CODEWORD, BYTES_PER_CODEWORD);
    codeword_cmp(pcm, aptx_codeword[idx]);
    ++idx;
  }
}

TEST_F(LibAptxEncTest, encode_sample_wav_data) {
  int pcm_fd = open_non_blocking("test/sample.wav");
  ASSERT_NE(pcm_fd, -1);
  int aptx_fd = open_non_blocking("test/sample.aptx");
  ASSERT_NE(aptx_fd, -1);

  for (;;) {
    char buf[BYTES_PER_CODEWORD];
    const size_t read_length = read(pcm_fd, buf, BYTES_PER_CODEWORD);
    // sucess no more data to read
    if (read_length == 0) {
      break;
    }
    ASSERT_EQ(read_length, BYTES_PER_CODEWORD);
    uint32_t aptx_codeword;
    ASSERT_EQ(read(aptx_fd, &aptx_codeword, sizeof(uint32_t)),
              sizeof(uint32_t));
    codeword_cmp((uint16_t*)buf, aptx_codeword);
  }

  close(pcm_fd);
  close(aptx_fd);
}
