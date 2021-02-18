/*
 * Copyright 2018 The Android Open Source Project
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

#include <memory>
#include <list>

#include "packet.h"
#include "avrcp_common.h"
namespace bluetooth {

// A helper templated class to access the protected members of Packet to make
// testing easier
template <class PacketType>
class TestPacketType : public PacketType {
 public:
  using PacketType::PacketType;

  static std::shared_ptr<TestPacketType<PacketType>> Make() {
    return std::shared_ptr<TestPacketType<PacketType>>(
        new TestPacketType<PacketType>());
  }

  static std::shared_ptr<TestPacketType<PacketType>> Make(
      std::shared_ptr<Packet> packet) {
    return std::shared_ptr<TestPacketType<PacketType>>(
        new TestPacketType<PacketType>(packet));
  }

  static std::shared_ptr<TestPacketType<PacketType>> Make(
      std::vector<uint8_t> payload) {
    size_t end = payload.size();
    return Make(std::move(payload), 0, end);
  }

  static std::shared_ptr<TestPacketType<PacketType>> Make(
      std::vector<uint8_t> payload, size_t start, size_t end) {
    auto pkt = std::shared_ptr<TestPacketType<PacketType>>(
        new TestPacketType<PacketType>());
    pkt->packet_start_index_ = start;
    pkt->packet_end_index_ = end;
    pkt->data_ = std::make_shared<std::vector<uint8_t>>(std::move(payload));
    return pkt;
  }

  const std::vector<uint8_t>& GetData() { return *PacketType::data_; }

  std::shared_ptr<std::vector<uint8_t>> GetDataPointer() {
    return PacketType::data_;
  }
};

namespace avrcp {

inline
std::string to_string(const Attribute& a) {
  switch (a) {
  case Attribute::TITLE: return "TITLE";
  case Attribute::ARTIST_NAME: return "ARTIST_NAME";
  case Attribute::ALBUM_NAME: return "ALBUM_NAME";
  case Attribute::TRACK_NUMBER: return "TRACK_NUMBER";
  case Attribute::TOTAL_NUMBER_OF_TRACKS: return "TOTAL_NUMBER_OF_TRACKS";
  case Attribute::GENRE: return "GENRE";
  case Attribute::PLAYING_TIME: return "PLAYING_TIME";
  case Attribute::DEFAULT_COVER_ART: return "DEFAULT_COVER_ART" ;
  default: return "UNKNOWN ATTRIBUTE";
  };
}

inline
std::string to_string(const AttributeEntry& entry) {
  std::stringstream ss;
  ss << to_string(entry.attribute()) << ": " << entry.value() ;
  return ss.str();
}

template<class Container>
std::string to_string(const Container& entries) {
  std::stringstream ss;
  for (const auto& el: entries) {
    ss << to_string(el) << std::endl;
  }
  return ss.str();
}

#pragma GCC diagnostic ignored "-Wformat-security"
class Report {
  std::stringstream maker;

public:

  template<typename ... Args>
  void write( const char* format, Args... args) {
    int size = std::snprintf(nullptr, 0, format, args...) + 1;
    if (size > 0) {
      std::unique_ptr<char[]> buf(new char[size]);
      std::snprintf(buf.get(), size, format, args...);
      maker << buf.get();
    }
  }

  std::string get() const {
    return maker.str();
  }

  void clear() {
    maker.str("");
  }
};

inline
bool operator==(AttributeEntry a, AttributeEntry b) {
  return (a.attribute() == b.attribute())
    && (a.value() == b.value());
}

inline
bool operator!=(AttributeEntry a, AttributeEntry b) {
  return !(a == b);
}

template <class AttributesResponseBuilder>
class AttributesResponseBuilderTestUser {
public:
  using Builder = AttributesResponseBuilder;
  using Maker = std::function<typename Builder::Builder(size_t)>;

private:
  Maker maker;
  typename Builder::Builder builder;
  size_t mtu;
  size_t current_size = 0;
  size_t entry_counter = 0;
  std::set<AttributeEntry> control_set;
  std::list<AttributeEntry> order_control;
  std::list<AttributeEntry> sended_order;
  Report report;
  bool test_result = true;
  bool order_test_result = true;

  void reset() {
    for (const auto& en : builder->entries_) {
      sended_order.push_back(en);
    }
    this->current_size = 0, this->entry_counter = 0;
    this->control_set.clear();
    this->builder->clear();
  }

  size_t expected_size() {
    return Builder::kHeaderSize() + this->current_size;
  }

public:

  std::string getReport() const {
    return this->report.get();
  }

  AttributesResponseBuilderTestUser(size_t m_size, Maker maker):
    maker(maker),
    builder (maker(m_size)),
    mtu (m_size)
    {
      this->report.write("AttributesResponseBuilderTestUser: start test for mtu \"%zu\"\n",
                         this->mtu);
    }

  void startTest(size_t m_size) {
    this->builder = maker(m_size);
    this->mtu = m_size;
    this->reset();
    this->report.clear();
    this->order_control.clear();
    this->sended_order.clear();
    this->report.write("AttributesResponseBuilderTestUser: starts test for mtu \"%zu\"\n",
                       this->mtu);
    this->order_test_result = true;
    this->test_result = true;
  }

  bool testResult ()const {
    return this->test_result;
  }

  bool testOrder () {
    return this->order_test_result;
  }

  void finishTest () {
    this->reset();
    if (order_control.size() != sended_order.size()) {
      this->report.write("testOrder FAIL: the count of entries which should send (%zu)\
                        is not equal to sended entries%zu)) \n input:\n%s\n sended:\n%s\n",
                         order_control.size(), sended_order.size(),
                         to_string(order_control).c_str(),
                         to_string(sended_order).c_str());
      this->order_test_result = false;
      return;
    }
    auto e = this->order_control.begin();
    auto s = this->sended_order.begin();
    for(; e != this->order_control.end(); ++e, ++s) {
      if (*e != *s) {
        this->report.write("testOrder FAIL: order of entries was changed\n");
        this->order_test_result = false;
        break;
      }
    }
    this->report.write("AttributesResponseBuilderTestUser: ends test for mtu \"%zu\"\n",
                       this->mtu);
  }

  void AddAttributeEntry(AttributeEntry entry) {
    auto f = this->builder->AddAttributeEntry(entry);
    if (f != 0) {
      this->current_size += f;
      ++this->entry_counter;
    }
    if (f == entry.size()) {
      this->wholeEntry(f, std::move(entry));
    }
    else {
      this->fractionEntry(f, std::move(entry));
    }
  }

private:

  void wholeEntry(size_t f, AttributeEntry&& entry) {
    this->control_set.insert(entry);
    this->order_control.push_back(entry);
    if(this->builder->size() != this->expected_size()) {
      this->report.write("AttributesResponseBuilderTestUser FAIL for \"%s\":"
                         "is not allowed to add.\n", to_string(entry).c_str());
      this->test_result = false;
    }
  }

  void fractionEntry(size_t f, AttributeEntry&& entry) {
    auto l_value = entry.value().size() - (entry.size() - f);
    if (f != 0) {
      auto pushed_entry = AttributeEntry(entry.attribute(),
                                         std::string(entry.value(), 0, l_value));
      this->control_set.insert(pushed_entry);
      this->order_control.push_back(pushed_entry);
    }

    if (expected_size() != builder->size()) {
      this->test_result = false;
      report.write("AttributesResponseBuilderTestUser FAIL for \"%s\":"
                   "is not allowed to add.\n", to_string(entry).c_str());
    }

    if (this->builder->size() != this->expected_size()
        || this->builder->entries_.size() != this->entry_counter) {
      report.write("AttributesResponseBuilderTestUser FAIL: for \"%s\""
                   "unexpected the size of the packet\n", to_string(entry).c_str());
      test_result = false;
    }
    for (auto dat = builder->entries_.begin(), ex = control_set.begin();
         ex != control_set.end(); ++dat, ++ex) {
      if(*dat != *ex) {
        report.write("AttributesResponseBuilderTestUser FAIL: for \"%s\"\
            unexpected order of entries\n", to_string(entry).c_str());
        test_result = false;
      }
    }
    auto tail = (f == 0) ? entry :
      AttributeEntry(entry.attribute(), std::string(entry.value(), l_value));
    if(builder->entries_.size() != 0) {
      this->reset();
      this->AddAttributeEntry(tail);
    }
    if(builder->entries_.size() == 0) {
      report.write("AttributesResponseBuilderTestUser FAIL: MTU %zu too small\n",
                   this->mtu);
      test_result = false;
      this->order_control.push_back(entry);
      this->reset();
    }
  }
};

template<class AttributesBuilder>
class FructionEntryBuildTestHelper {
public:
  using Builder = AttributesBuilder;
  using Helper = AttributesResponseBuilderTestUser<Builder>;
  using Maker = typename Helper::Maker;
  using Result = std::tuple<bool, bool, std::string>;
  enum {
    FRUCTION_TEST_RESULT,
    ORDERING_TEST_RESULT,
    REPORT,
  };

  FructionEntryBuildTestHelper (size_t mtu, Maker m):
    helper(mtu, m)
  {}

  template<class TestCollection>
  Result runTest (const TestCollection& test_data, size_t mtu) {
    this->helper.startTest(mtu);

    for (auto& i: test_data) {
      this->helper.AddAttributeEntry(i);
    }
    this->helper.finishTest();
    return std::make_tuple(this->helper.testResult(),
                           this->helper.testOrder(),
                           this->helper.getReport());
  }

  template<class TestCollection>
  Result runTestWithFailePrintf (const TestCollection& test_data, size_t mtu) {
    return this->runTestWithUnexpectedPrintf(test_data, mtu);
  }

  template<class TestCollection>
  Result runTestWithUnexpectedPrintf (const TestCollection& test_data, size_t mtu,
                                      bool expected_fruction = true,
                                      bool expected_ordering = true) {
    auto result = this->runTest(test_data, mtu);
    std::string report;
    bool fruction_pass;
    bool orderig_pass;
    std::tie(fruction_pass, orderig_pass, report) = result;
    if (fruction_pass != expected_fruction || orderig_pass != expected_ordering) {
      printf("%s", report.c_str());
    }
    return result;
  }

private:
  Helper helper;
};
}  // namespace avrcp
}  // namespace bluetooth
