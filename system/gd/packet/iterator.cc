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

#include "packet/iterator.h"

#undef NDEBUG
#include <cassert>

namespace bluetooth {
namespace packet {

template <bool little_endian>
Iterator<little_endian>::Iterator(std::shared_ptr<const std::vector<uint8_t>> data, size_t begin,
                                  size_t end, size_t index) {
  data_ = std::move(data);
  begin_ = begin;
  end_ = end;
  index_ = index;
}

template <bool little_endian>
Iterator<little_endian>::Iterator(std::shared_ptr<const std::vector<uint8_t>> data) {
  data_ = std::move(data);
  begin_ = 0;
  end_ = data_->size();
  index_ = 0;
}

template <bool little_endian>
Iterator<little_endian> Iterator<little_endian>::operator+(int offset) const {
  auto itr(*this);

  return itr += offset;
}

template <bool little_endian>
Iterator<little_endian>& Iterator<little_endian>::operator+=(int offset) {
  index_ += offset;
  return *this;
}

template <bool little_endian>
Iterator<little_endian>& Iterator<little_endian>::operator++() {
  index_++;
  return *this;
}

template <bool little_endian>
Iterator<little_endian> Iterator<little_endian>::operator-(int offset) const {
  auto itr(*this);

  return itr -= offset;
}

template <bool little_endian>
int Iterator<little_endian>::operator-(const Iterator<little_endian>& itr) const {
  return index_ - itr.index_;
}

template <bool little_endian>
Iterator<little_endian>& Iterator<little_endian>::operator-=(int offset) {
  index_ -= offset;

  return *this;
}

template <bool little_endian>
Iterator<little_endian>& Iterator<little_endian>::operator--() {
  if (index_ != 0) {
    index_--;
  }
  return *this;
}

template <bool little_endian>
Iterator<little_endian>& Iterator<little_endian>::operator=(const Iterator<little_endian>& itr) {
  if (this == &itr) {
    return *this;
  }
  this->data_ = itr.data_;
  this->begin_ = itr.begin_;
  this->end_ = itr.end_;
  this->index_ = itr.index_;
  return *this;
}

template <bool little_endian>
bool Iterator<little_endian>::operator==(const Iterator<little_endian>& itr) const {
  return index_ == itr.index_;
}

template <bool little_endian>
bool Iterator<little_endian>::operator!=(const Iterator<little_endian>& itr) const {
  return !(*this == itr);
}

template <bool little_endian>
bool Iterator<little_endian>::operator<(const Iterator<little_endian>& itr) const {
  return index_ < itr.index_;
}

template <bool little_endian>
bool Iterator<little_endian>::operator>(const Iterator<little_endian>& itr) const {
  return index_ > itr.index_;
}

template <bool little_endian>
bool Iterator<little_endian>::operator<=(const Iterator<little_endian>& itr) const {
  return index_ <= itr.index_;
}

template <bool little_endian>
bool Iterator<little_endian>::operator>=(const Iterator<little_endian>& itr) const {
  return index_ >= itr.index_;
}

template <bool little_endian>
uint8_t Iterator<little_endian>::operator*() const {
  assert(index_ >= begin_ && index_ < end_);
  return data_->at(index_);
}

template <bool little_endian>
size_t Iterator<little_endian>::NumBytesRemaining() const {
  if (end_ > index_ && index_ >= begin_) {
    return end_ - index_;
  }
  return 0;
}

template <bool little_endian>
Iterator<little_endian> Iterator<little_endian>::Subrange(size_t index, size_t length) const {
  return Iterator<little_endian>(data_, index_ + index, index_ + index + length, index_ + index);
}

// Explicit instantiations for both types of Iterators.
template class Iterator<true>;
template class Iterator<false>;
}  // namespace packet
}  // namespace bluetooth
