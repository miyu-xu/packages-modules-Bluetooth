#include <cstdint>
#include <unistd.h>
#include <vector>
#include <iostream>
#include <time.h>
#include <fstream>
#include <future>
#include <fmt/format.h>
#include "looper.h"

ILooper* new_custom_looper();
ILooper* new_custom_looper_bis();
ILooper* new_libchrome_looper();
ILooper* new_gd_looper();

/// Return the current value of CLOCK_MONOTONIC.
/// The timeout for epoll_wait is measured against the
/// CLOCK_MONOTONIC clock.
static uint64_t clock_monotonic_us() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000;
}

static uint64_t clock_monotonic_ns() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000 + ts.tv_nsec;
}

static void write_results(char const *test_name, char const *looper_name,
                          int repeat, int results[]) {
  std::ofstream out;
  out.open(fmt::format("{}-{}.txt", test_name, looper_name));
  for (int i = 0; i < repeat; i++) {
    out << results[i] << std::endl;
  }
  out.close();
}

static void test_delayed(char const* name, int repeat, ILooper* looper) {
  std::cout << "starting delayed test for " << name << std::endl;
  int* results = new int[repeat];

  for (int i = 0; i < repeat; i++) {
      std::promise<void> promise;
      auto future = promise.get_future();

      uint64_t then = clock_monotonic_ns();
      looper->post([=, &promise] () {
          uint64_t now = clock_monotonic_ns();
          results[i] = now - then;
          promise.set_value();
      });

      // wait for previous task to complete before scheduling
      // the next one.
      future.wait();
  }

  write_results("delayed", name, repeat, results);
  delete[] results;
}

static void test_burst(char const* name, int repeat, ILooper* looper) {
  std::cout << "starting burst test for " << name << std::endl;
  int* results = new int[repeat];
  std::promise<void> promise;
  auto future = promise.get_future();

  for (int i = 0; i < repeat; i++) {
      uint64_t then = clock_monotonic_ns();
      looper->post([=, &promise] () {
          uint64_t now = clock_monotonic_ns();
          results[i] = now - then;

          if (i == (repeat - 1)) {
              promise.set_value();
          }
      });
  }

  future.wait();
  write_results("burst", name, repeat, results);
  delete[] results;
}

struct test_state {
  ILooper* looper;
  uint64_t then;
  int i;
  int repeat;
  int *results;
  std::promise<void> promise;
};

static void test(test_state* state) {
  uint64_t now = clock_monotonic_ns();
  state->results[state->i] = now - state->then;
  state->then = now;
  state->i++;

  if (state->i >= state->repeat) {
    state->promise.set_value();
    return;
  }

  state->looper->post(std::bind(test, state));
}

static void test_recursive(char const* name, int repeat, ILooper* looper) {
  std::cout << "starting recursive test for " << name << std::endl;

  test_state state = {
    .looper = looper,
    .then = clock_monotonic_ns(),
    .i = 0,
    .repeat = repeat,
    .results = new int[repeat],
  };

  looper->post(std::bind(test, &state));

  auto future = state.promise.get_future();
  future.wait();
  write_results("recursive", name, repeat, state.results);
  delete[] state.results;
}

int main() {
   ILooper *custom_looper = new_custom_looper();
   ILooper *custom_looper_bis = new_custom_looper_bis();
   ILooper *gd_looper = new_gd_looper();
   ILooper *libchrome_looper = new_libchrome_looper();

   if (1) {
     test_delayed("custom", 1000000, custom_looper);
     test_delayed("custom_bis", 1000000, custom_looper_bis);
     test_delayed("gd", 1000000, gd_looper);
     test_delayed("cros", 1000000, libchrome_looper);
   }
   if (1) {
     test_burst("custom", 1000000, custom_looper);
     test_burst("custom_bis", 1000000, custom_looper_bis);
     test_burst("gd", 1000000, gd_looper);
     test_burst("cros", 1000000, libchrome_looper);
   }
   if (1) {
     test_recursive("custom", 1000000, custom_looper);
     test_recursive("custom_bis", 1000000, custom_looper_bis);
     test_recursive("gd", 1000000, gd_looper);
     test_recursive("cros", 1000000, libchrome_looper);
   }
   return 0;
}
