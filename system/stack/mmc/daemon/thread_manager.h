/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MMC_DAEMON_THREAD_MANAGER_H_
#define MMC_DAEMON_THREAD_MANAGER_H_

#include <base/functional/bind.h>
#include <base/functional/callback_helpers.h>
#include <base/logging.h>

#include <future>
#include <memory>
#include <string>
#include <vector>

#include "common/message_loop_thread.h"
#include "mmc/daemon/constants.h"
#include "mmc/socket_wrapper/socket_wrapper_interface.h"

namespace mmc {

class ThreadManager {
 public:
  ThreadManager() = default;
  ~ThreadManager() = default;

  // Adds a thread to the thread pool and makes it listen on the socket fd.
  bool StartWorkerThread(std::unique_ptr<SocketWrapperInterface> socket_wrapper,
                         std::unique_ptr<MmcInterface> codec_server) {
    std::promise<void> task_ended;
    thread_pool_.push_back(
        std::make_unique<Thread(kWorkerThreadName, task_ended.get_future())>);
    if (!thread_pool_.back()->StartTask(
            base::BindOnce(&StartSocketListener, std::move(socket_wrapper),
                           std::move(codec_server)))) {
      LOG(ERROR) << "Failed to run task";
      return false;
    }
    return true;
  }

  // Removes idle threads from the thread pool.
  void RemoveIdleThread() {
    for (auto thread = thread_pool_.begin(); thread != thread_pool_.end();) {
      if (thread->isIdle(kThreadCheckTimeout)) {
        // The task is over, close the thread and remove it from the thread
        // pool.
        thread->ShutDown();
        thread = thread_pool_.erase(thread);
      } else {
        thread++;
      }
    }
  }

 private:
  // Wrapper of MessageLoopThread
  class Thread {
   public:
    explicit Thread(const std::string& thread_name,
                    std::promise<void>& task_ended) {
      thread_ =
          std::make_unique<bluetooth::common::MessageLoopThread>(thread_name);
      task_ended_ =
          std::make_unique<std::future<void>>(task_ended.get_future());
    }

    // Start up thread and assign task to it.
    bool StartTask(base::OnceClosure task) {
      thread_->StartUp();
      if (!thread_->IsRunning()) {
        LOG(ERROR) << "Failed to start thread";
        return false;
      }

      // Real-time scheduling increases thread priority.
      // Without it, the thread still works.
      if (!thread_->EnableRealTimeScheduling()) {
        LOG(WARNING) << "Failed to enable real time scheduling";
      }

      if (!thread_->DoInThread(FROM_HERE, task)) {
        LOG(ERROR) << "Failed to run task";
        return false;
      }
      return true;
    }

    bool isIdle(int timeout) {
      return task_ended_->wait_for(std::chrono::milliseconds(timeout)) ==
             std::future_status::ready;
    }

    void ShutDown() { thread_->ShutDown(); }

   private:
    std::unique_ptr<bluetooth::common::MessageLoopThread> thread_;
    std::unique_ptr<std::future<void>> task_ended_;
  }

  std::vector<std::unique_ptr<Thread>>
      thread_pool_;
};
}  // namespace mmc

#endif  // MMC_DAEMON_THREAD_MANAGER_H_
