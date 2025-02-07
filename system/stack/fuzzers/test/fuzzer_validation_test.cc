/*
 * Copyright yyyy The Android Open Source Project
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

#include "fuzzer_validation_test.h"

#include <bluetooth/log.h>

#include <filesystem>
#include <fstream>

using namespace std;
using namespace bluetooth;

namespace fs = std::filesystem;

// Allow referencing of fuzzer entrance function as-is.
extern "C" int LLVMFuzzerTestOneInput(const uint8_t *Data, size_t Size);

void runFuzzerOnCorpusAndExitProcess(const string &corpus_path) {
  for (const auto &corpus_entry : fs::directory_iterator(corpus_path)) {
    const fs::path &path = corpus_entry.path();
    log::info("Running rfcomm-fuzzer with %s", path.string());
    ifstream corpus(path, ios::in | ios::binary | ios::ate);
    log::assert_that(corpus.is_open(), "%s does not exist!", path.string());
    streampos size = corpus.tellg();
    char *data = new char[size];
    corpus.seekg(0, ios::beg);
    corpus.read(data, size);
    corpus.close();
    LLVMFuzzerTestOneInput(reinterpret_cast<const uint8_t *>(data), size);
  }
  exit(0);
}
