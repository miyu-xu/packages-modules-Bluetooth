

#pragma once

#include "test/headless/handler.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace test {
namespace headless {

void disconnector(bluetooth::test::headless::Handler* handler,
                  const RawAddress& bd_addr, tBT_TRANSPORT transport);

}
}  // namespace test
}  // namespace bluetooth
