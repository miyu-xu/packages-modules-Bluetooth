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

#include <cstring>
#include <string>

namespace bluetooth {
namespace testing {

class LogCapture {
 public:
  LogCapture();
  ~LogCapture();

  LogCapture* Rewind();
  bool Find(std::string to_find);
  std::string Read();
  void Flush();
  size_t Size() const;
  void Reset();

 private:
  int create_backing_store();
  bool set_non_blocking(int fd);
  void clean_up();

  int fd_{-1};
  int original_stderr_fd{-1};
};

}  // namespace testing
}  // namespace bluetooth
