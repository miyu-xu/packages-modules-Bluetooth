

#include "test/headless/property.h"

#include <map>
#include <unordered_map>

#include "include/hardware/bluetooth.h"

using namespace bluetooth::test;

namespace bluetooth::test::headless {

std::map<::bt_property_type_t, std::function<headless::bt_property_t*(
                                   const uint8_t* data, const size_t len)>>
    my_map = {
        {BT_PROPERTY_BDNAME,
         [](const uint8_t* data, const size_t len) -> headless::bt_property_t* {
           return new headless::property::name_t(data, len);
         }},
        {BT_PROPERTY_UUIDS,
         [](const uint8_t* data, const size_t len) -> headless::bt_property_t* {
           return new headless::property::uuid_t(data, len);
         }},
        {BT_PROPERTY_CLASS_OF_DEVICE,
         [](const uint8_t* data, const size_t len) -> headless::bt_property_t* {
           return new headless::property::class_of_device_t(data, len);
         }},
        {BT_PROPERTY_TYPE_OF_DEVICE,
         [](const uint8_t* data, const size_t len) -> headless::bt_property_t* {
           return new headless::property::type_of_device_t(data, len);
         }},
};
}

// void init() {
//   my_map.insert(std::pair(BT_PROPERTY_BDNAME, nullptr));
//// {BT_PROPERTY_BDNAME, nullptr},
//}

const headless::property::Factory headless::property::uuid_t::Factory =
    headless::property::Factory(
        [](const uint8_t* data, const size_t len) -> bt_property_t* {
          return new headless::property::uuid_t(data, len);
        });

const headless::property::Factory headless::property::name_t::Factory =
    headless::property::Factory(
        [](const uint8_t* data, const size_t len) -> bt_property_t* {
          return new headless::property::name_t(data, len);
        });

const headless::property::Factory
    headless::property::class_of_device_t::Factory =
        headless::property::Factory(
            [](const uint8_t* data, const size_t len) -> bt_property_t* {
              return new headless::property::class_of_device_t(data, len);
            });

const headless::property::Factory
    headless::property::type_of_device_t::Factory = headless::property::Factory(
        [](const uint8_t* data, const size_t len) -> bt_property_t* {
          return new headless::property::type_of_device_t(data, len);
        });

// std::map<::bt_property_type_t, headless::bt_property_t::name_t::Factory*>
// map_;

#if 0
template<bt_property_type_t> struct Map;

template<> struct Map<BT_PROPERTY_BDNAME> {
  using type = headless::property::name_t;

  headless::property::name_t* operator()(const bt_property_t* bt_property) {
    return new headless::property::name_t(static_cast<uint8_t*>(bt_property->val),
                                          static_cast<size_t>(bt_property->len));
  }
};
#endif

// void init() {
//   map_.insert(BT_PROPERTY_BDNAME, headless::property::name_t::Factory);
// }

//        return new property::name_t(static_cast<uint8_t*>(bt_property.val),
//                                                                        static_cast<size_t>(bt_property.len));
//
