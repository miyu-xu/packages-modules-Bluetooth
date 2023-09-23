/******************************************************************************
 *
 *  Copyright 1999-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <base/location.h>

#include "common/message_loop_thread.h"
#include "include/hardware/bluetooth.h"
#include "stack/include/btu.h"

using bluetooth::common::MessageLoopThread;

bluetooth::common::MessageLoopThread* get_main_thread();
bt_status_t do_in_main_thread(const base::Location& from_here,
                              base::OnceClosure task);
bt_status_t do_in_main_thread_delayed(const base::Location& from_here,
                                      base::OnceClosure task,
                                      const base::TimeDelta& delay);
void post_on_bt_main(BtMainClosure closure);
void main_thread_start_up();
void main_thread_shut_down();
