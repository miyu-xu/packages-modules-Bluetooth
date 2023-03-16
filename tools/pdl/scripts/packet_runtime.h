/*
 * Copyright 2023 The Android Open Source Project
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

#pragma once

#include <cassert>
#include <cstdint>
#include <memory>
#include <vector>

namespace pdl::packet {

class slice {
 public:
  slice() = default;
  slice(slice const&) = default;
  slice(std::shared_ptr<const std::vector<uint8_t>> packet)
      : packet_(packet), offset_(0), size_(packet_->size()) {}

  slice(std::shared_ptr<const std::vector<uint8_t>> packet, size_t offset,
        size_t size)
      : packet_(packet), offset_(offset), size_(size) {}

  slice subrange(size_t offset, size_t size) const {
    assert((offset + size) <= size_);
    return slice(packet_, offset_ + offset, size);
  }

  template <typename T, size_t N = sizeof(T)>
  T read() {
    static_assert(N <= sizeof(T));
    assert(N <= size_);
    T value = 0;
    for (size_t n = 0; n < N; n++) {
      value |= (T)at(n) << (8 * n);
    }
    skip(N);
    return value;
  }

  uint8_t at(size_t offset) const {
    assert(offset <= size_);
    return packet_->at(offset_ + offset);
  }

  void skip(size_t size) {
    assert(size <= size_);
    offset_ += size;
    size_ -= size;
  }

  void clear() { size_ = 0; }

  size_t size() const { return size_; }

 private:
  std::shared_ptr<const std::vector<uint8_t>> packet_;
  size_t offset_{0};
  size_t size_{0};
};

class Builder {
 public:
  virtual void Serialize(std::vector<uint8_t>&) const = 0;

  std::vector<uint8_t> Serialize() const {
    std::vector<uint8_t> output;
    Serialize(output);
    return output;
  }
};

class Parser {
 public:
  virtual bool Parse(slice& input, Parser* output) const = 0;
};

}  // namespace pdl::packet
