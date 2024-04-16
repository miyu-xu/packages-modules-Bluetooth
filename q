[1mdiff --git a/system/gd/hal/hci_hal_android_hidl.cc b/system/gd/hal/hci_hal_android_hidl.cc[m
[1mindex c109d1e7aa..bfb0b4a6f9 100644[m
[1m--- a/system/gd/hal/hci_hal_android_hidl.cc[m
[1m+++ b/system/gd/hal/hci_hal_android_hidl.cc[m
[36m@@ -127,6 +127,7 @@[m [mclass InternalHciCallbacks : public IBluetoothHciCallbacks_1_1 {[m
     link_clocker_->OnHciEvent(received_hci_packet);[m
     btsnoop_logger_->Capture([m
         received_hci_packet, SnoopLogger::Direction::INCOMING, SnoopLogger::PacketType::EVT);[m
[32m+[m[32m    log::warn("hciEventReceived");[m
     {[m
       std::lock_guard<std::mutex> incoming_packet_callback_lock(incoming_packet_callback_mutex_);[m
       if (callback_ != nullptr) {[m
[1mdiff --git a/system/gd/os/handler.cc b/system/gd/os/handler.cc[m
[1mindex 4544636246..e3b1773e82 100644[m
[1m--- a/system/gd/os/handler.cc[m
[1m+++ b/system/gd/os/handler.cc[m
[36m@@ -89,7 +89,9 @@[m [mvoid Handler::handle_next_event() {[m
     closure = std::move(tasks_->front());[m
     tasks_->pop();[m
   }[m
[32m+[m[32m  log::warn("handle_next_event start");[m
   std::move(closure).Run();[m
[32m+[m[32m  log::warn("handle_next_event stop");[m
 }[m
 [m
 }  // namespace os[m
[1mdiff --git a/system/gd/os/linux_generic/reactor.cc b/system/gd/os/linux_generic/reactor.cc[m
[1mindex 90186e0897..dd66f5da3f 100644[m
[1m--- a/system/gd/os/linux_generic/reactor.cc[m
[1m+++ b/system/gd/os/linux_generic/reactor.cc[m
[36m@@ -148,6 +148,8 @@[m [mvoid Reactor::Run() {[m
       idle_promise_ = nullptr;[m
     }[m
 [m
[32m+[m[32m    log::warn("reactor count num: {}", count);[m
[32m+[m
     for (int i = 0; i < count; ++i) {[m
       auto event = events[i];[m
       log::assert_that(event.events != 0u, "assert failed: event.events != 0u");[m
[36m@@ -158,10 +160,12 @@[m [mvoid Reactor::Run() {[m
         eventfd_read(control_fd_, &value);[m
         if ((value & kStopReactor) != 0) {[m
           is_running_ = false;[m
[32m+[m[32m          log::warn("reactor kStopReactor");[m
           return;[m
         } else if ((value & kWaitForIdle) != 0) {[m
           timeout_ms = 30;[m
           waiting_for_idle = true;[m
[32m+[m[32m          log::warn("reactor waiting_for_idle");[m
           continue;[m
         } else {[m
           log::error("Unknown control_fd value {:x}", value);[m
[36m@@ -173,6 +177,7 @@[m [mvoid Reactor::Run() {[m
       executing_reactable_finished_ = nullptr;[m
       // See if this reactable has been removed in the meantime.[m
       if (std::find(invalidation_list_.begin(), invalidation_list_.end(), reactable) != invalidation_list_.end()) {[m
[32m+[m[32m        log::warn("reactor this reactable has been removed in the meantime");[m
         continue;[m
       }[m
 [m
