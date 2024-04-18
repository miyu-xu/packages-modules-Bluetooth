#include <thread>
#include <base/message_loop/message_loop.h>
#include <base/location.h>
#include <base/bind.h>
#include "looper.h"

namespace {

struct looper : public ILooper {
  looper();
  ~looper();

  void run();
  void post(std::function<void()> && closure) override;

  base::RunLoop *run_loop{};
  base::MessageLoop *message_loop{};
  std::thread thread{};
};

looper::looper() {
  thread = std::thread(&looper::run, this);
}

looper::~looper() {
  run_loop->QuitWhenIdle();
  thread.join();
}

void looper::run() {
  message_loop = new base::MessageLoop();
  run_loop = new base::RunLoop();
  run_loop->Run();
  delete message_loop;
  delete run_loop;
}

void looper::post(std::function<void()> &&closure) {
  message_loop->task_runner()->PostTask(
      base::Location(), base::BindOnce(
        [](std::function<void()> f) { f(); }, std::move(closure)));
}

}

ILooper* new_libchrome_looper() {
  return new looper();
}

