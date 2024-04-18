#include "os/thread.h"
#include "os/handler.h"

#include "looper.h"

namespace {

struct looper: public ILooper {
    bluetooth::os::Thread *thread{};
    bluetooth::os::Handler *handler{};

    looper();
    ~looper();

    void post(std::function<void()> && closure) override;
};

looper::looper() {
    thread = new bluetooth::os::Thread("test", bluetooth::os::Thread::Priority::NORMAL);
    handler = new bluetooth::os::Handler(thread);
}

looper::~looper() {
    handler->Clear();
    handler->WaitUntilStopped(std::chrono::milliseconds(1000));
    thread->Stop();
    delete handler;
     delete thread;
}

void looper::post(std::function<void()> &&closure) {
  handler->Post(
      base::BindOnce(
        [](std::function<void()> f) { f(); }, std::move(closure)));
}

}

ILooper *new_gd_looper() {
  return new looper();
}
