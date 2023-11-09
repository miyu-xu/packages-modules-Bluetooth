/******************************************************************************
 *
 *  Copyright Google Corporation
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

#include <base/logging.h>

#include "bta_hh_uhid.h"
#include "osi/include/allocator.h"

typedef std::queue<struct uhid_event> uhid_event_queue_t;

static bool uhid_event_queue_dequeue(
    uhid_event_queue_t& queue, struct uhid_event& event) {
  if (queue.empty()) {
    return false;
  }
  event = queue.front();
  queue.pop();
  return true;
}

static bool uhid_event_queue_enqueue(
    uhid_event_queue_t& queue, const struct uhid_event& event) {
  static constexpr size_t MAX_EVENT_COUNT = 20;
  if (queue.size() >= MAX_EVENT_COUNT) {
    return false;
  }
  queue.push(event);
  return true;
}

void bta_hh_uhid_evt_queue_init(tBTA_HH_UHID_EVT_QUEUE* queue, bool thread_safe) {
  CHECK(queue) << "uhid queue shall not be nullptr.";
  queue->p_event_queue = new (std::nothrow) uhid_event_queue_t();

  CHECK(queue->p_event_queue) << "uhid event queue cannot be allocated.";

  queue->p_mutex = NULL;
  if (thread_safe) {
    queue->p_mutex = new (std::nothrow) std::mutex();
    CHECK(queue->p_mutex) << "uhid event queue mutex cannot be allocated.";
  }
}

void bta_hh_uhid_evt_queue_destroy(tBTA_HH_UHID_EVT_QUEUE* queue) {
  CHECK(queue) << "uhid queue shall not be nullptr.";
  CHECK(queue->p_event_queue) << "uhid queue shall be initialized.";

  delete queue->p_event_queue;
  queue->p_event_queue = NULL;

  if (queue->p_mutex) {
    delete queue->p_mutex;
  }
  queue->p_mutex = NULL;
}

void bta_hh_uhid_evt_queue_cleanup(tBTA_HH_UHID_EVT_QUEUE* queue) {
  CHECK(queue) << "uhid queue shall not be nullptr.";
  CHECK(queue->p_event_queue) << "uhid queue shall be initialized.";

  if (queue->p_mutex) {
    std::lock_guard<std::mutex> guard(*queue->p_mutex);
    *queue->p_event_queue = uhid_event_queue_t();
    return;
  }
  *queue->p_event_queue = uhid_event_queue_t();
}

bool bta_hh_uhid_evt_queue_dequeue(
    tBTA_HH_UHID_EVT_QUEUE* queue, struct uhid_event& event) {
  CHECK(queue) << "uhid queue shall not be nullptr.";
  CHECK(queue->p_event_queue) << "uhid queue shall be initialized.";

  if (queue->p_mutex) {
    std::lock_guard<std::mutex> guard(*queue->p_mutex);
    return uhid_event_queue_dequeue(*queue->p_event_queue, event);
  }
  return uhid_event_queue_dequeue(*queue->p_event_queue, event);
}

bool bta_hh_uhid_evt_queue_enqueue(
    tBTA_HH_UHID_EVT_QUEUE* queue, const struct uhid_event& event) {
  CHECK(queue) << "uhid queue shall not be nullptr.";
  CHECK(queue->p_event_queue) << "uhid queue shall be initialized.";

  if (queue->p_mutex) {
    std::lock_guard<std::mutex> guard(*queue->p_mutex);
    return uhid_event_queue_enqueue(*queue->p_event_queue, event);
  }
  return uhid_event_queue_enqueue(*queue->p_event_queue, event);
}
