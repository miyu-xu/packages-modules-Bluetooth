

#pragma once

#include "os/handler.h"
#include "os/thread.h"

namespace bluetooth {
namespace test {
namespace headless {

class Handler {
 public:
  Handler();
  ~Handler();
  Handler(const Handler& handler) = default;

  void Post(common::OnceClosure closure);

 private:
  os::Thread* thread_{nullptr};
  os::Handler* handler_{nullptr};
};

}  // namespace headless
}  // namespace test
}  // namespace bluetooth
