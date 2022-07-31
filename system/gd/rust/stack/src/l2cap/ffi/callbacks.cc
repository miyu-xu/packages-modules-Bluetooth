#include "callbacks.h"

std::unique_ptr<tL2CAP_APPL_INFO> prepare_p_cb_info() {
  return std::make_unique<tL2CAP_APPL_INFO>();
}
