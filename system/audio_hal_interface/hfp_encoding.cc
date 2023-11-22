#include "hfp_encoding.h"

#include <cstdint>

#include "aidl/hfp_encoding_aidl.h"
#include "hal_version_manager.h"

namespace bluetooth {
namespace audio {
namespace hfp {

// Check if new bluetooth_audio is enabled
bool is_hal_enabled() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    return aidl::hfp::is_hal_enabled();
  }
  return false;
}

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop) {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    return aidl::hfp::init(message_loop);
  }
  return false;
}

// Clean up BluetoothAudio HAL
void cleanup() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    aidl::hfp::cleanup();
  }
}

// Set up the codec into BluetoothAudio HAL
bool setup_codec() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    return aidl::hfp::setup_codec();
  }
  return false;
}

// Send command to the BluetoothAudio HAL: StartSession, EndSession,
// StreamStarted, StreamSuspended
void start_session() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    aidl::hfp::start_session();
  }
}
void end_session() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    aidl::hfp::end_session();
  }
}

void ack_stream_started(const tHFP_CTRL_ACK& status) {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    aidl::hfp::ack_stream_started(status);
  }
}
void ack_stream_suspended(const tHFP_CTRL_ACK& status) {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    aidl::hfp::ack_stream_suspended(status);
  }
}

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len) {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::AIDL) {
    return aidl::hfp::read(p_buf, len);
  }
  return 0;
}

}  // namespace hfp
}  // namespace audio
}  // namespace bluetooth
