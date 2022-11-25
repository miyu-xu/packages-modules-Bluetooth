

#include "os/handler.h"

#include <chrono>

#include "os/thread.h"
#include "test/headless/handler.h"
#include "test/headless/log.h"

namespace bluetooth {
namespace test {

headless::Handler::Handler() {
  thread_ = new os::Thread("headless_thread", os::Thread::Priority::NORMAL);
  handler_ = new os::Handler(thread_);
}

headless::Handler::~Handler() {
  handler_->Clear();
  handler_->WaitUntilStopped(std::chrono::milliseconds(2000));
  delete handler_;
  delete thread_;
}

void headless::Handler::Post(common::OnceClosure closure) {
  ASSERT_LOG(handler_ != nullptr, "Handler is not valid");
  handler_->Post(std::move(closure));
}

}  // namespace test
}  // namespace bluetooth
