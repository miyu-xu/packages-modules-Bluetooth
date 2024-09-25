#include <base/location.h>

#include "os/handler.h"
#include "os/thread.h"
#include "osi/include/alarm.h"

extern void alarm_set_closure(const base::Location& /* posted_from*/, alarm_t* /* alarm */,
                              uint64_t /* interval_ms */, base::OnceClosure /* user_task */);
