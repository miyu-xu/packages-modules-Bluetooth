/*
 * Copyright 2025 The Android Open Source Project
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

#include "fuzzer_validation_test.h"

using namespace testing;

class TestEnvironment : public ::testing::Environment {
  void SetUp() override {
    system("rm -rf ./data/corpus");
    int result = system("unzip -o ./data/rfcomm_corpus.zip -d ./data/corpus");
    if (result != 0) {
      FAIL() << "Failed to unzip the corpus.";
    }
  }

  void TearDown() override { system("rm -rf ./data/corpus"); }
};

// Write own main function to allow for plugging in the test environment, which runs only once.
int main(int argc, char** argv) {
  InitGoogleTest(&argc, argv);
  TestEnvironment* env = new TestEnvironment();
  AddGlobalTestEnvironment(env);
  return RUN_ALL_TESTS();
}

TEST(RfcommFuzzerValidationTest, DoesNotCrashOnCorpus) {
  EXPECT_EXIT(runFuzzerOnCorpusAndExitProcess(std::string("./data/corpus")), ExitedWithCode(0),
              ".*");
}
