

#pragma once

#include <algorithm>
#include <cstdint>
#include <deque>
#include <map>
#include <memory>
#include <string>

#include "include/hardware/bluetooth.h"
#include "test/headless/log.h"
#include "types/bluetooth/uuid.h"

namespace bluetooth {
namespace test {
namespace headless {

struct bt_property_t {
  int Type() const { return type; }

  virtual std::string ToString() const = 0;

 protected:
  bt_property_t(const uint8_t* data, const size_t len) {
    this->len = len;
    this->data = std::make_unique<uint8_t[]>(len);
    std::copy(data, data + len, this->data.get());
  }
  virtual ~bt_property_t() = default;

  std::unique_ptr<uint8_t[]> data;
  size_t len;
  int type;
};

namespace property {

struct Factory {
 public:
  Factory(
      std::function<bt_property_t*(const uint8_t* data, const size_t len)> ctor)
      : ctor_(ctor) {}

  bt_property_t* operator()(const uint8_t* data, const size_t len) const {
    return ctor_(data, len);
  }

 private:
  std::function<bt_property_t*(const uint8_t* data, const size_t len)> ctor_;
};

struct uuid_t : public bt_property_t {
 public:
  uuid_t(const uint8_t* data, const size_t len) : bt_property_t(data, len) {}

  std::deque<bluetooth::Uuid> get_uuids() const {
    std::deque<bluetooth::Uuid> uuids;
    bluetooth::Uuid* p_uuid = reinterpret_cast<bluetooth::Uuid*>(data.get());
    for (size_t i = 0; i < num_uuid(); i++, p_uuid++) {
      bluetooth::Uuid uuid = bluetooth::Uuid::From128BitBE(
          reinterpret_cast<const uint8_t*>(p_uuid));
      uuids.push_back(uuid);
    }
    return uuids;
  }

  static const property::Factory Factory;

  virtual std::string ToString() const override {
    return base::StringPrintf("Number of uuids:%zu", get_uuids().size());
  }

 private:
  size_t num_uuid() const { return len / sizeof(bluetooth::Uuid); }
};

struct name_t : public bt_property_t {
  name_t(const uint8_t* data, const size_t len) : bt_property_t(data, len) {
    type = BT_PROPERTY_BDNAME;
  }

  std::string get_name() const {
    char* s = reinterpret_cast<char*>(data.get());
    return std::string(s);
  }

  static const property::Factory Factory;

  virtual std::string ToString() const override {
    return base::StringPrintf("Name:%s", get_name().c_str());
  }
};

struct class_of_device_t : public bt_property_t {
  class_of_device_t(const uint8_t* data, const size_t len)
      : bt_property_t(data, len) {
    type = BT_PROPERTY_CLASS_OF_DEVICE;
  }

  uint32_t get_class_of_device() const {
    uint32_t* cod = reinterpret_cast<uint32_t*>(data.get());
    return *cod;
  }

  static const property::Factory Factory;

  virtual std::string ToString() const override {
    return base::StringPrintf("cod:0x%04x", get_class_of_device());
  }
};

struct type_of_device_t : public bt_property_t {
  type_of_device_t(const uint8_t* data, const size_t len)
      : bt_property_t(data, len) {
    type = BT_PROPERTY_TYPE_OF_DEVICE;
  }

  uint32_t get_type_of_device() const {
    uint32_t* tod = reinterpret_cast<uint32_t*>(data.get());
    return *tod;
  }

  static const property::Factory Factory;

  virtual std::string ToString() const override {
    return base::StringPrintf("tod:0x%04x", get_type_of_device());
  }
};

}  // namespace property

// extern std::map<::bt_property_type_t, headless::bt_property_t*> map_;

// bt_property_t* create_derived(bt_property_type_t, const ::bt_property_t&
// bt_property) {
//
// }

template <::bt_property_type_t>
struct Map;

extern std::map<::bt_property_type_t,
                std::function<headless::bt_property_t*(const uint8_t* data,
                                                       const size_t len)>>
    my_map;

template <>
struct Map<BT_PROPERTY_BDNAME> {
  using type = headless::property::name_t*;
  //
  //   headless::property::name_t* operator()(const ::bt_property_t*
  //   bt_property) {
  //     return new
  //     headless::property::name_t(static_cast<uint8_t*>(bt_property->val),
  //                                           static_cast<size_t>(bt_property->len));
  //   }
};

// Caller owns the memory
inline bt_property_t* property_factory(const ::bt_property_t& bt_property) {
  ASSERT_LOG(bt_property.len > -1, "Property count is less than zero");
  ASSERT_LOG(bt_property.val != nullptr, "Property data value is null");

  // ASSERT_LOG(bt_property.type is in Map);

  const uint8_t* data = static_cast<uint8_t*>(bt_property.val);
  const size_t size = static_cast<size_t>(bt_property.len);

  //  return
  //  Map(bt_property.type)->second(static_cast<uint8_t*>(bt_property.val),
  //                                       static_cast<size_t>(bt_property.len));
  //
  return my_map[bt_property.type](data, size);
#if 0
  // (nullptr);
//    static_cast<uint8_t*>(bt_property.val),
//    static_cast<size_t>(bt_property.len));

  switch (bt_property.type) {
    case BT_PROPERTY_BDNAME:
      return new property::name_t(static_cast<uint8_t*>(bt_property.val),
                                  static_cast<size_t>(bt_property.len));
      break;

    case BT_PROPERTY_UUIDS:
      return new property::uuid_t(static_cast<uint8_t*>(bt_property.val),
                                  static_cast<size_t>(bt_property.len));
      break;

    case BT_PROPERTY_CLASS_OF_DEVICE:
      return property::class_of_device_t::Factory(data, size);
//      return new property::class_of_device_t(
//          static_cast<uint8_t*>(bt_property.val),
//          static_cast<size_t>(bt_property.len));
      break;

    case BT_PROPERTY_TYPE_OF_DEVICE:
      return property::type_of_device_t::Factory(data, size);
//      return new property::type_of_device_t(
//          static_cast<uint8_t*>(bt_property.val),
//          static_cast<size_t>(bt_property.len));
      break;

    default:
//      LOG_CONSOLE("TODO: Headless property not yet handled : %d",
//                  bt_property.type);
    return Map<BT_PROPERTY_BDNAME>::type(nullptr);
      break;
  }
  return nullptr;
#endif
}

template <typename T>
T* get_property_type(bluetooth::test::headless::bt_property_t* bt_property) {
  return static_cast<T*>(bt_property);
}

}  // namespace headless
}  // namespace test
}  // namespace bluetooth
