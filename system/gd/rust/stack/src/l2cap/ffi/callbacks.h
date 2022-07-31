#pragma once

#include <memory>

#include "rust/cxx.h"
#include "src/l2cap/bridge.rs.h"
#include "stack/include/l2c_api.h"

std::unique_ptr<tL2CAP_APPL_INFO> prepare_p_cb_info();
