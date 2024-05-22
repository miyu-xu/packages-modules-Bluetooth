/*
 * Copyright 2024 The Android Open Source Project
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

#include <bluetooth/log.h>

#include <future>
#include <list>
#include <mutex>
#include <optional>

#include "hal/hci_audio.h"
#include "hal/hci_backend.h"
#include "hal/hci_hal.h"
#include "hal/hci_packet.h"
#include "hal/link_clocker.h"
#include "hal/snoop_logger.h"

namespace bluetooth::hal {

enum class HciCommandOpCode : uint16_t {
  kUndefined = 0x0000,
  kReset = HciFormatCommandOpCode(0x03, 0x003),
  kReadBufferSize = HciFormatCommandOpCode(0x04, 0x005),
  kGetVendorCapabilities = HciFormatCommandOpCode(0x3f, 0x153),
  kA2dpHardwareOffload = HciFormatCommandOpCode(0x3f, 0x15d),
};

enum class HciEventCode : uint8_t {
  kUndefined = 0x00,
  kConnectionComplete = 0x03,
  kDisconnectionComplete = 0x05,
  kCommandComplete = 0x0e,
  kNumberOfCompletedPackets = 0x13,
};

class HciHalImpl;

class HciCallbacksImpl : public HciBackendCallbacks {
  class : public HciHalCallbacks {
   public:
    void hciEventReceived(std::vector<uint8_t>) override {
      log::warn("Dropping HCI Event, since callback is not set");
    }
    void aclDataReceived(std::vector<uint8_t>) override {
      log::warn("Dropping ACL Data, since callback is not set");
    }
    void scoDataReceived(std::vector<uint8_t>) override {
      log::warn("Dropping SCO Data, since callback is not set");
    }
    void isoDataReceived(std::vector<uint8_t>) override {
      log::warn("Dropping ISO Data, since callback is not set");
    }
  } kNullCallbacks;

 public:
  HciCallbacksImpl(HciHalImpl* hal, SnoopLogger* btsnoop_logger, LinkClocker* link_clocker)
      : hal_(hal), btsnoop_logger_(btsnoop_logger), link_clocker_(link_clocker) {}

  void SetCallback(HciHalCallbacks*);
  void ResetCallback();

  std::promise<void>* const init_promise = &init_promise_;
  void initializationComplete() override {
    init_promise_.set_value();
  }

  void hciEventReceived(const std::vector<uint8_t>&) override;
  void aclDataReceived(const std::vector<uint8_t>&) override;
  void scoDataReceived(const std::vector<uint8_t>&) override;
  void isoDataReceived(const std::vector<uint8_t>&) override;

  void InjectHciEvent(std::vector<uint8_t>);

 private:
  std::mutex mutex_;
  std::promise<void> init_promise_;
  HciHalCallbacks* callback_ = &kNullCallbacks;
  HciHalImpl* hal_;
  SnoopLogger* btsnoop_logger_;
  LinkClocker* link_clocker_;
};

class HciHalImpl : public HciHal {
 public:
  static HciHalImpl* instance;

  HciHalImpl() {
    log::assert_that(instance == nullptr, "Multiple instance not allowed");
    instance = this;
  }

  ~HciHalImpl() {
    instance = nullptr;
  }

  void registerIncomingPacketCallback(HciHalCallbacks* callback) override {
    callbacks_->SetCallback(callback);
  }

  void unregisterIncomingPacketCallback() override {
    callbacks_->ResetCallback();
  }

  void setupA2dpOutput(
      std::optional<A2dpLinkId> link_id,
      std::shared_ptr<A2dpNotification> notif,
      A2dpBuffers buffers,
      int* max_packet_size) {
    std::lock_guard<std::mutex> lock(mutex_);

    max_packets_[PacketOrigin::kA2dp] = buffers.max_packets;
    a2dp_packets_reserve_ = buffers.packets_reserve;
    a2dp_[A2dpStreamDirection::kOutput].emplace(A2dp{link_id, notif});

    if (max_packet_size) *max_packet_size = max_controller_packet_size_;
  }

  void releaseA2dpOutput(std::optional<A2dpLinkId> link_id) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto& a2dp = a2dp_[A2dpStreamDirection::kOutput];

    if (!a2dp || (link_id && link_id != a2dp->link_id)) {
      log::error("Releasing unknown A2DP Link");
      return;
    }

    DisableA2dpOutput();
    a2dp.reset();
  }

  void setupA2dpInput(
      std::optional<A2dpLinkId> link_id,
      std::shared_ptr<A2dpNotification> notif,
      std::function<void(std::vector<uint8_t>)> recv_cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    a2dp_recv_cb_ = recv_cb;
    a2dp_[A2dpStreamDirection::kInput].emplace(A2dp{link_id, notif});
  }

  void releaseA2dpInput(std::optional<A2dpLinkId> link_id) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto& a2dp = a2dp_[A2dpStreamDirection::kInput];

    if (!a2dp || (link_id && link_id != a2dp->link_id)) {
      log::error("Releasing unknown A2DP Link");
      return;
    }

    a2dp.reset();
  }

  void sendHciCommand(std::vector<uint8_t>) override;
  void sendAclData(std::vector<uint8_t>) override;
  void sendScoData(std::vector<uint8_t>) override;
  void sendIsoData(std::vector<uint8_t>) override;

  void sendA2dpPacket(std::vector<uint8_t> packet);

  bool CatchA2dpPacket(const std::vector<uint8_t>&);
  std::vector<uint8_t> FixupHciEvent(const std::vector<uint8_t>&);

 protected:
  std::string ToString() const override {
    return std::string("HciHal");
  }

  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;

 private:
  LinkClocker* link_clocker_ = nullptr;
  SnoopLogger* btsnoop_logger_ = nullptr;
  std::shared_ptr<HciBackend> backend_;
  std::shared_ptr<HciCallbacksImpl> callbacks_;

  enum PacketOrigin { kStack = 0, kA2dp, kNumValues };

  static std::string to_string(PacketOrigin origin) {
    switch (origin) {
      case PacketOrigin::kStack:
        return "Stack";
      case PacketOrigin::kA2dp:
        return "A2DP";
      default:
        return std::to_string(static_cast<int>(origin));
    }
  }

  struct AclCommit {
    uint16_t conn_handle;
    PacketOrigin origin;
  };

  struct A2dp {
    std::optional<A2dpLinkId> link_id;
    std::shared_ptr<A2dpNotification> notif;
    bool started = false;
  };

  size_t a2dp_packets_reserve_;
  std::array<std::optional<A2dp>, 2> a2dp_;
  std::function<void(std::vector<uint8_t>)> a2dp_recv_cb_;

  std::mutex mutex_;

  std::vector<uint16_t> connections_;
  std::array<int, PacketOrigin::kNumValues> max_commits_;
  std::list<AclCommit> committed_;

  size_t max_controller_packets_ = 0;
  size_t max_controller_packet_size_ = 0;
  std::array<size_t, PacketOrigin::kNumValues> max_packets_;
  std::list<std::vector<uint8_t>> queues_[PacketOrigin::kNumValues];

  void ReturnCommandComplete(HciCommandOpCode, std::vector<uint8_t>);
  std::optional<std::vector<uint8_t>> ProcessA2dpStartStopCommand(HciCommandReader&);
  bool OnStartA2dp(A2dpStreamDirection, A2dpLinkId);
  bool OnStopA2dp(A2dpStreamDirection, A2dpLinkId);

  void OnResetComplete(HciEventReader&);
  void OnReadBufferSizeComplete(HciEventReader&);
  void OnConnectionComplete(HciEventReader&);
  void OnDisconnectionComplete(HciEventReader&);
  std::vector<uint8_t> ManageNumberOfCompletedPackets(HciEventReader&);

  void EnableA2dpOutput(void);
  void DisableA2dpOutput(void);
  void ScheduleTransfer(PacketOrigin, std::vector<uint8_t>);
  void ScheduleTransfer(void);
};

void HciCallbacksImpl::SetCallback(HciHalCallbacks* callback) {
  log::assert_that(callback_ == &kNullCallbacks, "callbacks already set");
  log::assert_that(callback != nullptr, "callback != nullptr");
  std::lock_guard<std::mutex> lock(mutex_);
  callback_ = callback;
}

void HciCallbacksImpl::ResetCallback() {
  std::lock_guard<std::mutex> lock(mutex_);
  log::info("callbacks have been reset!");
  callback_ = &kNullCallbacks;
}

void HciCallbacksImpl::hciEventReceived(const std::vector<uint8_t>& incoming_packet) {
  auto packet = hal_->FixupHciEvent(incoming_packet);
  if (!packet.size()) return;

  link_clocker_->OnHciEvent(packet);
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::INCOMING, SnoopLogger::PacketType::EVT);
  {
    std::lock_guard<std::mutex> lock(mutex_);
    callback_->hciEventReceived(packet);
  }
}

void HciCallbacksImpl::aclDataReceived(const std::vector<uint8_t>& packet) {
  if (hal_->CatchA2dpPacket(packet)) return;

  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::INCOMING, SnoopLogger::PacketType::ACL);
  {
    std::lock_guard<std::mutex> lock(mutex_);
    callback_->aclDataReceived(packet);
  }
}

void HciCallbacksImpl::scoDataReceived(const std::vector<uint8_t>& packet) {
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::INCOMING, SnoopLogger::PacketType::SCO);
  {
    std::lock_guard<std::mutex> lock(mutex_);
    callback_->scoDataReceived(packet);
  }
}

void HciCallbacksImpl::isoDataReceived(const std::vector<uint8_t>& packet) {
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::INCOMING, SnoopLogger::PacketType::ISO);
  {
    std::lock_guard<std::mutex> lock(mutex_);
    callback_->isoDataReceived(packet);
  }
}

void HciCallbacksImpl::InjectHciEvent(std::vector<uint8_t> packet) {
  std::lock_guard<std::mutex> lock(mutex_);
  callback_->hciEventReceived(std::move(packet));
}

HciHalImpl* HciHalImpl::instance = nullptr;

void HciHalImpl::ListDependencies(ModuleList* list) const {
  list->add<LinkClocker>();
  list->add<SnoopLogger>();
}

void HciHalImpl::Start() {
  log::assert_that(
      backend_ == nullptr, "Start can't be called more than once before Stop is called.");

  link_clocker_ = GetDependency<LinkClocker>();
  btsnoop_logger_ = GetDependency<SnoopLogger>();

  backend_ = HciBackend::CreateAidl();
  if (!backend_) backend_ = HciBackend::CreateHidl(GetHandler());

  log::assert_that(backend_ != nullptr, "No backend available");

  callbacks_ = std::make_shared<HciCallbacksImpl>(this, btsnoop_logger_, link_clocker_);

  backend_->initialize(callbacks_);
  callbacks_->init_promise->get_future().wait();
}

void HciHalImpl::Stop() {
  backend_.reset();
  callbacks_.reset();
  btsnoop_logger_ = nullptr;
  link_clocker_ = nullptr;
}

void HciHalImpl::sendHciCommand(std::vector<uint8_t> packet) {
  auto c = HciCommandReader(packet);
  if (c.opcode == HciCommandOpCode::kA2dpHardwareOffload) {
    auto ret = ProcessA2dpStartStopCommand(c);
    if (ret) {
      ReturnCommandComplete(c.opcode, std::move(*ret));
      return;
    }
  }

  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::OUTGOING, SnoopLogger::PacketType::CMD);
  backend_->sendHciCommand(packet);
}

void HciHalImpl::sendAclData(std::vector<uint8_t> packet) {
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::OUTGOING, SnoopLogger::PacketType::ACL);
  {
    std::lock_guard<std::mutex> lock(mutex_);
    ScheduleTransfer(PacketOrigin::kStack, std::move(packet));
  }
}

void HciHalImpl::sendScoData(std::vector<uint8_t> packet) {
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::OUTGOING, SnoopLogger::PacketType::SCO);
  backend_->sendScoData(packet);
}

void HciHalImpl::sendIsoData(std::vector<uint8_t> packet) {
  btsnoop_logger_->Capture(packet, SnoopLogger::Direction::OUTGOING, SnoopLogger::PacketType::ISO);
  backend_->sendIsoData(packet);
}

void HciHalImpl::sendA2dpPacket(std::vector<uint8_t> packet) {
  std::lock_guard<std::mutex> lock(mutex_);
  if (packet.size() > max_controller_packet_size_) {
    log::error("Cannot send A2DP packet: packet too big!");
    return;
  }
  ScheduleTransfer(PacketOrigin::kA2dp, std::move(packet));
}

void HciHalImpl::ReturnCommandComplete(
    HciCommandOpCode opcode, std::vector<uint8_t> return_parameters) {
  // HCI Command Complete [Core 4.E.7.7.14]
  // | 1 Num_HCI_Command_Packets
  // | 2 Command_Opcode
  // | N Return_Parameters

  auto e = HciEventWriter(HciEventCode::kCommandComplete);
  e.write(uint8_t(1));
  e.write(uint16_t(opcode));
  e.write(std::move(return_parameters));
  callbacks_->InjectHciEvent(e.flush());
}

std::optional<std::vector<uint8_t>> HciHalImpl::ProcessA2dpStartStopCommand(HciCommandReader& c) {
  enum class SubOcf : uint8_t {
    kStart = 0x03,
    kStop = 0x04,
  };

  // HCI A2DP Hardware Offload Command [Android HCI Requirements]
  // | 1 Sub OCF (Start or Stop A2DP Offload)
  // | N Command

  auto sub_ocf = SubOcf(c.read<uint8_t>());
  if (sub_ocf != SubOcf::kStart && sub_ocf != SubOcf::kStop) return std::nullopt;

  // Start/Stop A2DP Offload [Android HCI Requirements]
  // | 2 Connection Handle (12 bits)
  // | 2 L2CAP Channel ID
  // | 1 Data Path Direction (0: Output  1: Input)
  // | Start A2DP Offload
  // | | 1 Peer MTU
  // | | 1 CP Enable SCMS_T
  // | | 1 CP Header SCMS_T
  // | | 1 Vendor Specific Parameters Length (N = 0..128)
  // | | N Vendor Specific Parameters

  auto conn_handle = uint16_t(c.read<uint16_t>() & 0xfff);
  auto l2cap_cid = c.read<uint16_t>();
  auto link_id = A2dpLinkId({conn_handle, l2cap_cid});
  auto dir = A2dpStreamDirection(c.read<uint8_t>());
  if (sub_ocf == SubOcf::kStart) {
    c.skip(3);
    c.skip(c.read<uint8_t>());
  }

  uint8_t status = c.tell() != c.length() || int(dir) >= 2;
  auto rp = HciPacketWriter(2);
  rp.write(status);
  rp.write(uint8_t(sub_ocf));

  if (status) {
    log::error("Start/Stop A2DP Offload command parsing failed");
    return std::move(rp.vector());
  }

  if (sub_ocf == SubOcf::kStart ? !OnStartA2dp(dir, link_id) : !OnStopA2dp(dir, link_id))
    return std::nullopt;

  return std::move(rp.vector());
}

bool HciHalImpl::OnStartA2dp(A2dpStreamDirection dir, A2dpLinkId link_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto& a2dp = a2dp_[dir];

  if (!a2dp || (a2dp->link_id && a2dp->link_id != link_id)) return false;

  a2dp->link_id = link_id;
  a2dp->notif->startA2dp(dir, link_id);
  if (dir == A2dpStreamDirection::kOutput) EnableA2dpOutput();

  a2dp->started = true;

  return true;
}

bool HciHalImpl::OnStopA2dp(A2dpStreamDirection dir, A2dpLinkId link_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto& a2dp = a2dp_[dir];

  if (!a2dp || !a2dp->started || a2dp->link_id != link_id) return false;

  if (dir == A2dpStreamDirection::kOutput) DisableA2dpOutput();
  a2dp->notif->stopA2dp(dir, link_id);

  a2dp->started = false;

  return true;
}

bool HciHalImpl::CatchA2dpPacket(const std::vector<uint8_t>& packet) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto& a2dp = a2dp_[A2dpStreamDirection::kInput];

  if (!a2dp || !a2dp->link_id || !a2dp->started) return false;

  auto p = HciPacketReader(packet);

  auto conn_handle = uint16_t(p.read<uint16_t>() & 0xfff);
  auto data_total_length = p.read<uint16_t>();
  if (data_total_length < 4) return false;

  [[maybe_unused]] auto pdu_length = p.read<uint16_t>();
  auto cid = p.read<uint16_t>();

  if (a2dp_[A2dpStreamDirection::kInput]->link_id != A2dpLinkId{conn_handle, cid}) return false;

  a2dp_recv_cb_(packet);
  return true;
}

std::vector<uint8_t> HciHalImpl::FixupHciEvent(const std::vector<uint8_t>& packet) {
  auto e = HciEventReader(packet);

  switch (e.code) {
    case HciEventCode::kConnectionComplete:
      OnConnectionComplete(e);
      break;

    case HciEventCode::kDisconnectionComplete:
      OnDisconnectionComplete(e);
      break;

    case HciEventCode::kCommandComplete:
      e.skip(1);
      switch (HciCommandOpCode(e.read<uint16_t>())) {
        case HciCommandOpCode::kReset:
          OnResetComplete(e);
          break;

        case HciCommandOpCode::kReadBufferSize:
          OnReadBufferSizeComplete(e);
          break;

        case HciCommandOpCode::kGetVendorCapabilities:
          break;

        default:
          break;
      }
      break;

    default:
      break;
  }

  if (e.code == HciEventCode::kNumberOfCompletedPackets)
    return ManageNumberOfCompletedPackets(e);
  else
    return packet;
}

void HciHalImpl::OnResetComplete(HciEventReader& e) {
  // HCI Reset Return Parameters [Core 4.E.7.3.2]
  // | 1 Status

  auto status = e.read<uint8_t>();

  if (e.tell() != e.length()) log::error("Event parsing failed");
  if (status != 0) log::warn("Reset holds bad status: {:02x}", status);
  if (e.tell() != e.length() || status != 0) return;

  {
    std::lock_guard<std::mutex> lock(mutex_);
    connections_.clear();
    committed_.clear();
    max_commits_.fill(0);
  }
}

void HciHalImpl::OnReadBufferSizeComplete(HciEventReader& e) {
  // HCI Read Buffer Size Return Parameters [Core 4.E.7.4.5]
  // | 1 Status
  // | 2 ACL_Data_Packet_Length
  // | 1 Synchronous_Data_Packet_Length
  // | 2 Total_Num_ACL_Data_Packets
  // | 2 Total_Num_Synchronous_Data_Packets

  auto status = e.read<uint8_t>();
  auto acl_packet_length = e.read<uint16_t>();
  [[maybe_unused]] auto sco_packet_length = e.read<uint8_t>();
  auto num_acl_packets = e.read<uint16_t>();
  [[maybe_unused]] auto num_sco_packets = e.read<uint16_t>();

  if (e.tell() != e.length()) log::error("Event parsing failed");
  if (status != 0) log::warn("Buffer size info holds bad status: {:02x}", status);
  if (e.tell() != e.length() || status != 0) return;

  {
    std::lock_guard<std::mutex> lock(mutex_);
    max_controller_packets_ = num_acl_packets;
    max_controller_packet_size_ = acl_packet_length;
    max_commits_[PacketOrigin::kStack] = max_controller_packets_;
    max_packets_[PacketOrigin::kStack] = max_controller_packets_;
  }
}

void HciHalImpl::OnConnectionComplete(HciEventReader& e) {
  enum class LinkType : uint8_t {
    kSco = 0x00,
    kAcl = 0x01,
  };

  // HCI Connection Complete [Core 4.E.7.7.3]
  // | 1 Status
  // | 2 Connection_Handle (12 bits)
  // | 6 BD_ADDR
  // | 1 Link_Type
  // | 1 Encryption_Enabled

  auto status = e.read<uint8_t>();
  auto conn_handle = uint16_t(e.read<uint16_t>() & 0xfff);
  e.skip(6);
  auto link_type = LinkType(e.read<uint8_t>());
  e.skip(1);

  if (e.tell() != e.length()) log::error("Event parsing failed");
  if (e.tell() != e.length() || status != 0) return;

  {
    std::lock_guard<std::mutex> lock(mutex_);
    if (link_type == LinkType::kAcl) connections_.push_back(conn_handle);
  }
}

void HciHalImpl::OnDisconnectionComplete(HciEventReader& e) {
  // HCI Disconnection Complete [Core 4.E.7.7.5]
  // | 1 Status
  // | 2 Connection_Handle (12 bits)
  // | 1 Reason

  auto status = e.read<uint8_t>();
  auto conn_handle = uint16_t(e.read<uint16_t>() & 0xfff);
  e.skip(1);

  if (e.tell() != e.length()) log::error("Event parsing failed");
  if (e.tell() != e.length() || status != 0) return;

  {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it_connections = std::find(connections_.begin(), connections_.end(), conn_handle);
    if (it_connections == connections_.end()) {
      log::error("Disconnection occurred on unknown connection {:04x}", conn_handle);
      return;
    }
    connections_.erase(it_connections);

    auto it_committed =
        std::find_if(committed_.begin(), committed_.end(), [conn_handle](AclCommit& c) {
          return c.conn_handle == conn_handle;
        });
    while (it_committed != committed_.end()) it_committed = committed_.erase(it_committed);
    ScheduleTransfer();
  }
}

std::vector<uint8_t> HciHalImpl::ManageNumberOfCompletedPackets(HciEventReader& e) {
  struct ConnCompleted {
    uint16_t conn_handle;
    uint16_t num_completed;
  };

  // HCI Number Of Completed Packets [Core 4.E.7.7.19]
  // | 1 Num_Handles
  // | 2 Connection_Handle[i] (12 bits)
  // | 2 Num_Completed_Packets[i]

  std::vector<ConnCompleted> nocp(e.read<uint8_t>());
  for (auto& pair : nocp) pair.conn_handle = uint16_t(e.read<uint16_t>() & 0xfff);
  for (auto& pair : nocp) pair.num_completed = e.read<uint16_t>();

  if (e.tell() != e.length()) log::error("Event parsing failed");

  {
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto it_nocp = nocp.begin(); it_nocp != nocp.end();) {
      auto conn_handle = it_nocp->conn_handle;
      auto n_stack = 0;

      auto it = std::find_if(committed_.begin(), committed_.end(), [conn_handle](AclCommit& c) {
        return c.conn_handle == conn_handle;
      });
      for (auto n = it_nocp->num_completed; it != committed_.end() && n > 0; --n) {
        n_stack += (it->origin == PacketOrigin::kStack);
        it = committed_.erase(it);
      }

      it_nocp->num_completed = n_stack;
      it_nocp = n_stack <= 0 ? nocp.erase(it_nocp) : it_nocp + 1;
    }
    ScheduleTransfer();
  }

  if (nocp.size() == 0) return std::vector<uint8_t>();

  auto out = HciEventWriter(HciEventCode::kNumberOfCompletedPackets);
  out.write(uint8_t(nocp.size()));
  for (auto& pair : nocp) out.write(pair.conn_handle);
  for (auto& pair : nocp) out.write(pair.num_completed);
  return out.flush();
}

void HciHalImpl::EnableA2dpOutput() {
  // Reserve packet on the controller queue, no more than half the capacity,
  // and allow audio path to commit half the capacity
  size_t reserve = std::min(a2dp_packets_reserve_, max_controller_packets_ / 2);
  max_commits_[PacketOrigin::kStack] = (max_controller_packets_ - reserve);
  max_commits_[PacketOrigin::kA2dp] = (max_controller_packets_ + 1) / 2;
}

void HciHalImpl::DisableA2dpOutput() {
  max_commits_[PacketOrigin::kStack] = max_controller_packets_;
  max_commits_[PacketOrigin::kA2dp] = 0;
  queues_[PacketOrigin::kA2dp].clear();
}

void HciHalImpl::ScheduleTransfer(PacketOrigin origin, std::vector<uint8_t> packet) {
  queues_[origin].emplace_back(packet);
  ScheduleTransfer();
}

void HciHalImpl::ScheduleTransfer(void) {
  size_t num_committed[PacketOrigin::kNumValues] = {0};
  for (const auto& c : committed_) ++num_committed[c.origin];

  for (auto origin : {PacketOrigin::kA2dp, PacketOrigin::kStack}) {
    auto& q = queues_[origin];

    // Check buffer count on the path

    while (num_committed[origin] + q.size() > max_packets_[origin]) {
      log::assert_that(origin != PacketOrigin::kStack, "Stack queue should never be full");
      log::warn("{} maximum buffer count reached, dropping packet.", to_string(origin));
      q.pop_back();
    }

    // Loop on packets allowed to transmit

    int n = std::min(
        max_commits_[origin] - num_committed[origin],
        max_controller_packets_ - int(committed_.size()));

    while (!q.empty() && n > 0) {
      auto packet = std::move(q.front());
      uint16_t conn_handle = HciPacketReader(packet).read<uint16_t>() & 0xfff;
      q.pop_front();

      // Check if the connection exists,
      // otherwise drop packet (disconnection occurred ?)

      auto it_find = std::find(connections_.begin(), connections_.end(), conn_handle);
      if (it_find == std::end(connections_)) continue;

      // Let's commit the packet

      backend_->sendAclData(std::move(packet));
      committed_.push_back({conn_handle, origin});
      --n;
    }
  }
}

void setupA2dpOutput(
    std::optional<A2dpLinkId> link_id,
    std::shared_ptr<A2dpNotification> notif,
    A2dpBuffers buffers,
    int* max_packet_size) {
  auto instance = HciHalImpl::instance;
  log::assert_that(instance != nullptr, "HAL instance not available");
  log::assert_that(notif != nullptr, "Notification must be handled");

  instance->setupA2dpOutput(link_id, notif, buffers, max_packet_size);
}

void sendA2dpPacket(std::vector<uint8_t> packet) {
  auto instance = HciHalImpl::instance;
  log::assert_that(instance != nullptr, "HAL instance not available");

  instance->sendA2dpPacket(std::move(packet));
}

void releaseA2dpOutput(std::optional<A2dpLinkId> link_id) {
  auto instance = HciHalImpl::instance;
  log::assert_that(instance != nullptr, "HAL instance not available");

  instance->releaseA2dpOutput(link_id);
}

void setupA2dpInput(
    std::optional<A2dpLinkId> link_id,
    std::shared_ptr<A2dpNotification> notif,
    std::function<void(std::vector<uint8_t>)> recv_cb) {
  auto instance = HciHalImpl::instance;
  log::assert_that(instance != nullptr, "HAL instance not available");
  log::assert_that(notif != nullptr, "Notification must be handled");

  instance->setupA2dpInput(link_id, notif, recv_cb);
}

void releaseA2dpInput(std::optional<A2dpLinkId> link_id) {
  auto instance = HciHalImpl::instance;
  log::assert_that(instance != nullptr, "HAL instance not available");

  instance->releaseA2dpInput(link_id);
}

const ModuleFactory HciHal::Factory = ModuleFactory([]() { return new HciHalImpl(); });

}  // namespace bluetooth::hal
