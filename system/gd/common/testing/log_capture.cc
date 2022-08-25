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

#include "common/testing/log_capture.h"

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

#include <sstream>

#include "os/log.h"

namespace {
constexpr size_t kTempFilenameSize = 64;
constexpr size_t kBufferSize = 4096;
constexpr int kStandardErrorFd = STDERR_FILENO;
}  // namespace

namespace bluetooth {
namespace testing {

LogCapture::LogCapture() {
  int fd = create_backing_store();
  if (fd == -1) {
    LOG_ERROR("Unable to create backing storage: %s", strerror(errno));
    return;
  }
  if (!set_non_blocking(fd)) {
    LOG_ERROR("Unable to set socket non-blocking: %s", strerror(errno));
    return;
  }
  fd_ = fd;
  original_stderr_fd = dup(kStandardErrorFd);
  dup2(fd_, kStandardErrorFd);
}

LogCapture::~LogCapture() {
  Rewind()->Flush();
  clean_up();
}

LogCapture* LogCapture::Rewind() {
  if (fd_ != -1) {
    lseek(fd_, SEEK_SET, 0);
  }
  return this;
}

bool LogCapture::Find(std::string to_find) {
  std::string str = this->Read();
  return str.find(to_find) != std::string::npos;
}

void LogCapture::Flush() {
  if (fd_ != -1 && original_stderr_fd != -1) {
    ssize_t sz{-1};
    do {
      char buf[kBufferSize];
      sz = read(fd_, buf, sizeof(buf));
      if (sz > 0) {
        write(original_stderr_fd, buf, sz);
      }
    } while (sz == kBufferSize);
  }
}

std::string LogCapture::Read() {
  if (fd_ == -1) {
    return std::string();
  }
  std::ostringstream oss;
  ssize_t sz{-1};
  do {
    char buf[kBufferSize];
    sz = read(fd_, buf, sizeof(buf));
    if (sz > 0) {
      oss << buf;
    }
  } while (sz == kBufferSize);
  return oss.str();
}

int LogCapture::create_backing_store() {
  char tmp_filename[kTempFilenameSize] = "/tmp/bt_gtest_log_capture-XXXXXX";
  int fd = mkstemp(tmp_filename);
  if (fd != -1) {
    unlink(tmp_filename);
  }
  return fd;
}

bool LogCapture::set_non_blocking(int fd) {
  int flags = fcntl(fd, F_GETFL, 0);
  if (flags == -1) {
    LOG_ERROR("Unable to get file descriptor flags:%s", strerror(errno));
    return false;
  }
  if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) == -1) {
    LOG_ERROR("Unable to set file descriptor flags:%s", strerror(errno));
    return false;
  }
  return true;
}

void LogCapture::clean_up() {
  if (original_stderr_fd != -1) {
    dup2(original_stderr_fd, 2);
  }
  if (fd_ != -1) {
    close(fd_);
  }
}

}  // namespace testing
}  // namespace bluetooth
