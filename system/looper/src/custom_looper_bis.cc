
#include <cstdint>
#include <atomic>
#include <thread>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <functional>
#include <queue>

#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <sys/timerfd.h>
#include <unistd.h>

#include "looper.h"

namespace {

/// Return the current value of CLOCK_MONOTONIC.
/// The timeout for epoll_wait is measured against the
/// CLOCK_MONOTONIC clock.
static uint64_t clock_monotonic_us() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000;
}

struct immediate_task {
    std::function<void()> closure;
};

struct pending_task {
    std::function<void()> closure;
    uint64_t expiry_us;
};

bool operator<(pending_task const& lhs, pending_task const& rhs) {
    return lhs.expiry_us < rhs.expiry_us;
}

struct looper : ILooper {
    std::thread thread{};
    std::atomic_bool terminated{false};

    int epoll_fd{-1};
    int alarm_fd{-1};
    int event_fd{-1};

    looper(int priority);
    virtual ~looper();

    void run(int priority);
    void post(std::function<void()>&& closure) override;
    void post_delayed(std::function<void()>&& closure, int delay_ms);

    std::mutex task_mutex;
    std::queue<immediate_task> immediate_tasks;
    std::priority_queue<pending_task> pending_tasks;
    int timeout_ms{-1};
};

looper::looper(int priority) {
    // *_CLOEXEC will force the file descriptor to be closed
    // when execve() is called.
    epoll_fd = epoll_create1(EPOLL_CLOEXEC);
    alarm_fd = timerfd_create(CLOCK_BOOTTIME_ALARM, TFD_CLOEXEC | TFD_NONBLOCK);
    event_fd = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);

    printf("epoll_fd: %d\n", epoll_fd);
    printf("alarm_fd: %d %s\n", alarm_fd, strerror(errno));
    printf("event_fd: %d\n", event_fd);

    assert(epoll_fd != -1);
    assert(alarm_fd != -1);
    assert(event_fd != -1);

    epoll_event epoll_settings { EPOLLIN, { .ptr = nullptr } };
    int ret;

    ret = epoll_ctl(epoll_fd, EPOLL_CTL_ADD, alarm_fd, &epoll_settings);
    assert(ret == 0);
    ret = epoll_ctl(epoll_fd, EPOLL_CTL_ADD, event_fd, &epoll_settings);
    assert(ret == 0);

    // Start the looper thread.
    thread = std::thread(&looper::run, this, priority);
}

looper::~looper() {
    printf("deleting looper\n");
    terminated = true;

    // Wakeup the event file descriptor to break out of the epoll_wait
    // syscall. It is not actually necessary to append a task, since
    // the terminated flag is checked first.
    uint64_t incr = 1;
    write(event_fd, &incr, sizeof(incr));

    thread.join();
    close(event_fd);
    close(alarm_fd);
    close(epoll_fd);
}

void looper::post(std::function<void()>&& closure) {
    std::lock_guard<std::mutex> task_guard(task_mutex);
    immediate_tasks.emplace(immediate_task(std::move(closure)));

    if (immediate_tasks.size() == 1) {
        uint64_t incr = 1;
        write(event_fd, &incr, sizeof(incr));
    }
}

void looper::post_delayed(std::function<void()>&& closure, int delay_ms) {
    uint64_t ts_now = clock_monotonic_us();

    {
        // TODO check expiry > ts_now
        std::lock_guard<std::mutex> task_guard(task_mutex);
        pending_tasks.emplace(pending_task(std::move(closure), ts_now + delay_ms));
    }

    // TODO update timeout
}

void looper::run(int priority) {
    if (priority != -1) {
        // Optionally configure the thread to use RT scheduling.
        // Bluetooth threads that are used in audio have deadline requirements for
        // glitchless playback. Those threads need to be scheduled as RT tasks to
        // ensure that they can meet the deadline even if there is high system load.
        struct sched_param sched_params = {
            .sched_priority = priority,
        };

        // If pid equals zero, the scheduling policy and parameters of
        // the calling thread will be set.
        int rc = sched_setscheduler(0, SCHED_FIFO, &sched_params);
        assert (rc == 0);
    }

    for (;;) {
        // The call to epoll_wait() will block until either:
        //  •  a file descriptor delivers an event;
        //  •  the call is interrupted by a signal handler; or
        //  •  the timeout expires.
        struct epoll_event events[2];
        int ret = epoll_wait(epoll_fd, events, 2, timeout_ms);

        // The call to epoll_wait was interrupted by a signal.
        // Retry adjusting the timeout based on elapsed time.
        // TODO
        if (ret == -1 && errno == EINTR) {
            printf("epoll_wait interrupted\n");
            continue;
        }

        // epoll_wait returns the number of file descriptors ready for the
        // requested I/O, or zero if no file descriptor became ready during
        // the requested timeout milliseconds.
        assert(ret >= 0);

        // Check whether the looper is being terminated.
        if (terminated) {
            printf("looper terminated\n");
            break;
        }

        // Check event sources by order of priority: immediate tasks, alarms,
        // pending tasks. All file descritors are non-blocking;
        // it is not necessary to check the events before reading.

        // Run immediate tasks first. The idea is to ensure that tasks
        // scheduled immediately following timer events are not interlaced
        // with other timer events.
        uint64_t event_count = 0;
        ret = read(event_fd, &event_count, sizeof(event_count));
        assert(ret == sizeof(event_count));

        for (;;) {
            std::function<void()> f;
            {
                std::lock_guard<std::mutex> tasks_guard(task_mutex);
                if (immediate_tasks.size() == 0) {
                    break;
                }

                std::swap(f, immediate_tasks.front().closure);
                immediate_tasks.pop();
            }
            // Invoke the task closure.
            f();
        }

        // Run timer tasks and set the next timeout.
        // The tasks are checked if a timer was set.
        if (timeout_ms != -1) {
            uint64_t ts_now = clock_monotonic_us();

            for (;;) {
                std::function<void()> f;
                {
                    std::lock_guard<std::mutex> tasks_guard(task_mutex);

                    // If no other task is pending the timeout is reset.
                    if (pending_tasks.empty()) {
                        timeout_ms = -1;
                        break;
                    }

                    // If all other pending tasks are in the future
                    // the timeout is updated to match the next pending
                    // task.
                    if (pending_tasks.top().expiry_us > ts_now) {
                        timeout_ms = pending_tasks.top().expiry_us - ts_now;
                        break;
                    }

                    // Otherwise pick the next pending task for execution.
                    // TODO: the top accessor is const, it is not
                    // possible to swap the closure out...
                    f = pending_tasks.top().closure;
                    pending_tasks.pop();
                }

                // Invoke the task closure.
                f();
            }
        }
    }
}
/*
int main() {
    {
        printf("creating looper\n");
        looper looper(-1);
        sleep(1);
        looper.post([]() {
            printf("coucou\n");
        });
        sleep(1);
        looper.post([&]() {
            printf("coucou\n");
            looper.post([]() {
                printf("    reentrant coucou\n");
            });
        });
        sleep(1);
    }

    printf("looper deleted\n");
    return 0;
}
*/

}

ILooper* new_custom_looper_bis() {
  return new looper(-1);
}
