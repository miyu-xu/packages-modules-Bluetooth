/******************************************************************************
 *
 *  Copyright 2023 The Android Open Source Project
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

/******************************************************************************
 *
 *  This is the interface file for hid host UHID queue related structure and
 *  functions.
 *
 ******************************************************************************/

#pragma once

#include <mutex>

#include <linux/uhid.h>

typedef struct {
  void* p_event_queue;
  std::mutex* p_mutex;
} tBTA_HH_UHID_EVT_QUEUE;

typedef struct {
  bool success;
  struct uhid_event event;
} tBTA_HH_UHID_DEQUEUE_RESULT;

/*******************************************************************************
 *
 * Function         bta_hh_uhid_evt_queue_init
 *
 * Description      This function initializes a tBTA_HH_UHID_EVT_QUEUE
 *
 * Parameters       queue: The queue to be inited and it shall not be nullptr.
 *                  thread_safe: true if the queue needs to be used in multiple
 *                               threaded cases.
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_hh_uhid_evt_queue_init(
    tBTA_HH_UHID_EVT_QUEUE* queue, bool thread_safe = true);

/*******************************************************************************
 *
 * Function         bta_hh_uhid_evt_queue_destroy
 *
 * Description      This function destroys a tBTA_HH_UHID_EVT_QUEUE. The
 *                  destroed queue can be destroyed multiple times.
 *
 * Parameters       queue: The queue to be destroyed and it shall not be used
 *                         after destroy.
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_hh_uhid_evt_queue_destroy(tBTA_HH_UHID_EVT_QUEUE* queue);

/*******************************************************************************
 *
 * Function         bta_hh_uhid_evt_queue_cleanup
 *
 * Description      This function cleans up a tBTA_HH_UHID_EVT_QUEUE that all
 *                  events in the queue are removed.
 *
 * Parameters       queue: The queue to be cleaned up and it shall be inited.
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_hh_uhid_evt_queue_cleanup(tBTA_HH_UHID_EVT_QUEUE* queue);

/*******************************************************************************
 *
 * Function         bta_hh_uhid_evt_queue_dequeue
 *
 * Description      This function dequeues an event from the queue.
 *
 * Parameters       queue: The queue to be dequeued.
 *
 * Returns          result as tBTA_HH_UHID_DEQUEUE_RESULT; result.success is
 *                  True if dequeue is successful or False if the queue is
 *                  empty. result.event is the dequeued event if successful.
 *
 ******************************************************************************/
tBTA_HH_UHID_DEQUEUE_RESULT bta_hh_uhid_evt_queue_dequeue(
    tBTA_HH_UHID_EVT_QUEUE* queue);

/*******************************************************************************
 *
 * Function         bta_hh_uhid_evt_queue_enqueue
 *
 * Description      This function enqueue an event into the queue.
 *
 * Parameters       queue: The queue to be enqueued.
 *                  event: The event to be put into the queue.
 *
 * Returns          True if enqueue is successful or false if the queue is full.
 *
 ******************************************************************************/
bool bta_hh_uhid_evt_queue_enqueue(
    tBTA_HH_UHID_EVT_QUEUE* queue, const struct uhid_event& event);

#endif  // BTA_HH_UHID_H
