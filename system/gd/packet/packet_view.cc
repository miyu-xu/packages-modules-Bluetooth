/*
 * Copyright 2019 The Android Open Source Project
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

#include "packet/packet_view.h"

#undef NDEBUG
#include <algorithm>
#include <cassert>

namespace bluetooth {
namespace packet {

template <bool little_endian>
PacketView<little_endian>::PacketView(std::shared_ptr<const std::vector<uint8_t>> packet)
    : data_(std::move(packet)), begin_(0), end_(data_->size()) {}

template <bool little_endian>
PacketView<little_endian>::PacketView(std::shared_ptr<const std::vector<uint8_t>> packet,
                                      size_t begin, size_t end)
    : data_(std::move(packet)), begin_(begin), end_(end) {}

template <bool little_endian>
Iterator<little_endian> PacketView<little_endian>::begin() const {
  return Iterator<little_endian>(data_, begin_, end_, begin_);
}

template <bool little_endian>
Iterator<little_endian> PacketView<little_endian>::end() const {
  return Iterator<little_endian>(data_, begin_, end_, end_);
}

template <bool little_endian>
uint8_t PacketView<little_endian>::operator[](size_t index) const {
  return at(begin_ + index);
}

template <bool little_endian>
uint8_t PacketView<little_endian>::at(size_t index) const {
  return data_->at(begin_ + index);
}

template <bool little_endian>
size_t PacketView<little_endian>::size() const {
  return end_ - begin_;
}

template <bool little_endian>
PacketView<true> PacketView<little_endian>::GetLittleEndianSubview(size_t begin, size_t end) const {
  return PacketView<true>(data_, begin_ + begin, begin_ + end);
}

template <bool little_endian>
PacketView<false> PacketView<little_endian>::GetBigEndianSubview(size_t begin, size_t end) const {
  return PacketView<false>(data_, begin_ + begin, begin_ + end);
}

// Explicit instantiations for both types of PacketViews.
template class PacketView<true>;
template class PacketView<false>;
}  // namespace packet
}  // namespace bluetooth
