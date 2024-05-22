/*
 * Copyright 2024 The Android Open Source Project
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

#include <algorithm>
#include <cstring>
#include <vector>

namespace bluetooth::hal {

class HciPacketReadWrite {
 public:
  void skip(int n) {
    position_ += n;
  }
  size_t tell() {
    return position_;
  }

 protected:
  size_t position_ = 0;
};

class HciPacketReader : public HciPacketReadWrite {
 public:
  HciPacketReader(const std::vector<uint8_t>& vector) : vector_(vector), length_(vector.size()) {}

  size_t length() {
    return length_;
  }

  template <typename T>
  T read(int n = sizeof(T)) {
    T v = 0;
    if (position_ + n <= length_) {
      const uint8_t* p = vector_.data() + position_;
      for (int i = 0; i < n; i++) v |= *(p++) << (i * 8);
    }

    position_ += n;
    return v;
  }

 protected:
  const std::vector<uint8_t>& vector_;
  size_t length_;
};

class HciPacketWriter : public HciPacketReadWrite {
 public:
  HciPacketWriter(size_t size) : vector_(size) {}

  std::vector<uint8_t>& vector() {
    return vector_;
  }

  template <typename T>
  void write(T v, int n = sizeof(T)) {
    if (position_ + n <= vector_.size()) {
      uint8_t* p = vector_.data() + position_;
      for (int i = 0; i < n; i++) *(p++) = (v >> (i * 8)) & 0xff;
    }

    position_ += n;
  }

  void write(const std::vector<uint8_t>& v) {
    if (position_ < vector_.size()) {
      std::memcpy(
          vector_.data() + position_, v.data(), std::min(v.size(), vector_.size() - position_));
    }

    position_ += v.size();
  }

 protected:
  std::vector<uint8_t> vector_;
};

enum class HciCommandOpCode : uint16_t;

static inline constexpr uint16_t HciFormatCommandOpCode(int ogf, int ocf) {
  return (ogf << 10) | ocf;
}

class HciCommandReader : public HciPacketReader {
 public:
  HciCommandReader(const std::vector<uint8_t>& vector)
      : HciPacketReader(vector), opcode(HciCommandOpCode(read<uint16_t>())) {
    length_ = std::min(size_t(read<uint8_t>()) + 3, length_);
  }

  const HciCommandOpCode opcode;
  template <typename T>
  T read(int n = sizeof(T)) {
    return HciPacketReader::read<T>(n);
  }
};

enum class HciEventCode : uint8_t;

class HciEventReader : public HciPacketReader {
 public:
  HciEventReader(const std::vector<uint8_t>& vector)
      : HciPacketReader(vector), code(HciEventCode(read<uint8_t>())) {
    length_ = std::min(size_t(read<uint8_t>()) + 2, length_);
  }

  const HciEventCode code;
  template <typename T>
  T read() {
    return HciPacketReader::read<T>(sizeof(T));
  }
  template <typename T>
  T read(int n) {
    return HciPacketReader::read<T>(n);
  }
};

class HciEventWriter : public HciPacketWriter {
 public:
  HciEventWriter(HciEventCode event_code) : HciPacketWriter(UINT8_MAX + 2) {
    write(uint8_t(event_code));
    skip(1);
  }

  std::vector<uint8_t> flush() {
    vector_[1] = std::min(position_ - 2, size_t(UINT8_MAX));
    vector_.resize(position_);
    return std::move(vector_);
  }

  template <typename T>
  void write(T v) {
    HciPacketWriter::write<T>(v, sizeof(T));
  }
  template <typename T>
  void write(T v, int n) {
    HciPacketWriter::write<T>(v, n);
  }
  template <>
  void write(std::vector<uint8_t> v) {
    HciPacketWriter::write(v);
  }
};

}  // namespace bluetooth::hal
