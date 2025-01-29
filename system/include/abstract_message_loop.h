//
//  Copyright 2021 Google, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at:
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#pragma once

#if BASE_VER >= 822064
#include <base/task/current_thread.h>
#else
#include <base/message_loop/message_loop_current.h>
#endif
#include <base/message_loop/message_pump.h>
#include <base/task/single_thread_task_executor.h>
#include <base/test/task_environment.h>
#include <base/threading/thread.h>
#if BASE_VER >= 1024993
#include <base/task/single_thread_task_runner.h>
#else
#include <base/threading/thread_task_runner_handle.h>
#endif

namespace btbase {

class AbstractMessageLoop : public base::SingleThreadTaskExecutor {
public:
  static scoped_refptr<base::SingleThreadTaskRunner> current_task_runner() {
#if BASE_VER >= 1024993
    return base::SingleThreadTaskRunner::GetCurrentDefault();
#else
    return base::ThreadTaskRunnerHandle::Get();
#endif
  }
};

class AbstractTestMessageLoop : public base::test::TaskEnvironment {
public:
  static scoped_refptr<base::SingleThreadTaskRunner> current_task_runner() {
#if BASE_VER >= 1024993
    return base::SingleThreadTaskRunner::GetCurrentDefault();
#else
    return base::ThreadTaskRunnerHandle::Get();
#endif
  }
};

// Initialize the test task environment
#define DEFINE_TEST_TASK_ENV(var) \
  base::AbstractTestMessageLoop var { base::test::TaskEnvironment::ThreadingMode::MAIN_THREAD_ONLY }

inline void set_message_loop_type_IO(base::Thread::Options& options) {
  options.message_pump_type = base::MessagePumpType::IO;
}

}  // namespace btbase
