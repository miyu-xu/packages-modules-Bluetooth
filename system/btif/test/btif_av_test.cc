
#include "bta/include/bta_ag_api.h"
#include "btif/include/stack_manager.h"
#include "osi/include/compat.h"

// Stubbed
#if __GLIBC__
pid_t gettid(void) throw() { return (pid_t)0; }
#endif  // __GLIBC___

static const stack_manager_t interface = {nullptr, nullptr, nullptr, nullptr,
                                          nullptr};

const stack_manager_t* stack_manager_get_interface() { return &interface; }

// Stubbed
const tBTA_AG_RES_DATA tBTA_AG_RES_DATA::kEmpty = {};
