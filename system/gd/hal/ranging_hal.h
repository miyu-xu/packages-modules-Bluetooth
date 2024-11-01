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

#pragma once

#include <complex>

#include "hci/hci_packets.h"
#include "module.h"
#include "ras/ras_packets.h"

namespace bluetooth {
namespace hal {
/**
 * See BLUETOOTH CORE SPECIFICATION Version 6.0 | Vol 4, Part E 7.7.65.44 for details.
 */
static constexpr uint8_t kInitiatorMeasuredOffsetBits = 15;
static constexpr uint16_t kUnavailableInitiatorMeasuredOffset = 0xC000;
static constexpr uint8_t kUnavailablePacketRssi = 0x7F;
static constexpr uint8_t kIQSampleBits = 12;

enum RangingHalVersion {
  V_UNKNOWN = 0,
  V_1 = 1,
  V_2 = 2,
};

struct VendorSpecificCharacteristic {
  std::array<uint8_t, 16> characteristicUuid_;
  std::vector<uint8_t> value_;
};

struct ChannelSoundingRawData {
  uint8_t num_antenna_paths_;
  std::vector<uint8_t> step_channel_;
  std::vector<std::vector<std::complex<double>>> tone_pct_initiator_;
  std::vector<std::vector<std::complex<double>>> tone_pct_reflector_;
  std::vector<std::vector<uint8_t>> tone_quality_indicator_initiator_;
  std::vector<std::vector<uint8_t>> tone_quality_indicator_reflector_;
  std::vector<int8_t> packet_quality_initiator;
  std::vector<int8_t> packet_quality_reflector;
  std::vector<int16_t> toa_tod_initiators_;
  std::vector<int16_t> tod_toa_reflectors_;
};

static inline int16_t ConvertToSigned(uint16_t num_unsigned, uint8_t bits) {
  unsigned msb_mask = 1 << (bits - 1);  // setup a mask for most significant bit
  int16_t num_signed = num_unsigned;
  if ((num_signed & msb_mask) != 0) {
    num_signed |= ~(msb_mask - 1);  // extend the MSB
  }
  return num_signed;
}

struct Mode0Data {
  Mode0Data(const hci::LeCsMode0InitatorData& le_cs_mode0_data) {
    packet_rssi_ = le_cs_mode0_data.packet_rssi_;
    packet_quality_ = le_cs_mode0_data.packet_quality_;
    packet_antenna_ = le_cs_mode0_data.packet_antenna_;
    initiator_measured_offset = le_cs_mode0_data.measured_freq_offset_;
  }
  Mode0Data(const hci::LeCsMode0ReflectorData& le_cs_mode0_data) {
    packet_rssi_ = le_cs_mode0_data.packet_rssi_;
    packet_quality_ = static_cast<int8_t>(le_cs_mode0_data.packet_quality_);
    packet_antenna_ = le_cs_mode0_data.packet_antenna_;
  }
  uint8_t packet_quality_ = 0;
  uint8_t packet_rssi_ = kUnavailablePacketRssi;
  uint8_t packet_antenna_ = 0;
  uint16_t initiator_measured_offset = kUnavailableInitiatorMeasuredOffset;
};

struct Mode1Data {
  Mode1Data() {}
  Mode1Data(const hci::LeCsMode1InitatorData& le_cs_mode1_data) {
    packet_quality_ = le_cs_mode1_data.packet_quality_;
    packet_nadm_ = le_cs_mode1_data.packet_nadm_;
    packet_rssi_ = le_cs_mode1_data.packet_rssi_;
    rtt_toa_tod_data_ = le_cs_mode1_data.toa_tod_initiator_;
    packet_antenna_ = le_cs_mode1_data.packet_antenna_;
  }

  Mode1Data(const hci::LeCsMode1InitatorDataWithPacketPct& le_cs_mode1_data) {
    packet_quality_ = le_cs_mode1_data.packet_quality_;
    packet_nadm_ = le_cs_mode1_data.packet_nadm_;
    packet_rssi_ = le_cs_mode1_data.packet_rssi_;
    rtt_toa_tod_data_ = le_cs_mode1_data.toa_tod_initiator_;
    packet_antenna_ = le_cs_mode1_data.packet_antenna_;
    i_packet_pct1_.emplace(le_cs_mode1_data.packet_pct1_.i_sample_);
    q_packet_pct1_.emplace(le_cs_mode1_data.packet_pct1_.q_sample_);
    i_packet_pct2_.emplace(le_cs_mode1_data.packet_pct2_.i_sample_);
    q_packet_pct2_.emplace(le_cs_mode1_data.packet_pct2_.q_sample_);
  }

  Mode1Data(const hci::LeCsMode1ReflectorData& le_cs_mode1_data) {
    packet_quality_ = le_cs_mode1_data.packet_quality_;
    packet_nadm_ = le_cs_mode1_data.packet_nadm_;
    packet_rssi_ = le_cs_mode1_data.packet_rssi_;
    rtt_toa_tod_data_ = le_cs_mode1_data.tod_toa_reflector_;
    packet_antenna_ = le_cs_mode1_data.packet_antenna_;
  }

  Mode1Data(const hci::LeCsMode1ReflectorDataWithPacketPct& le_cs_mode1_data) {
    packet_quality_ = le_cs_mode1_data.packet_quality_;
    packet_nadm_ = le_cs_mode1_data.packet_nadm_;
    packet_rssi_ = le_cs_mode1_data.packet_rssi_;
    rtt_toa_tod_data_ = le_cs_mode1_data.tod_toa_reflector_;
    packet_antenna_ = le_cs_mode1_data.packet_antenna_;
    i_packet_pct1_.emplace(le_cs_mode1_data.packet_pct1_.i_sample_);
    q_packet_pct1_.emplace(le_cs_mode1_data.packet_pct1_.q_sample_);
    i_packet_pct2_.emplace(le_cs_mode1_data.packet_pct2_.i_sample_);
    q_packet_pct2_.emplace(le_cs_mode1_data.packet_pct2_.q_sample_);
  }

  uint8_t packet_quality_ = 0;
  hci::CsPacketNadm packet_nadm_ = hci::CsPacketNadm::UNKNOWN_NADM;
  uint8_t packet_rssi_ = kUnavailablePacketRssi;
  uint16_t rtt_toa_tod_data_ = 0;
  uint8_t packet_antenna_ = 0;
  std::optional<uint16_t> i_packet_pct1_;
  std::optional<uint16_t> q_packet_pct1_;
  std::optional<uint16_t> i_packet_pct2_;
  std::optional<uint16_t> q_packet_pct2_;
};

struct Mode2Data {
  Mode2Data() {}
  Mode2Data(const hci::LeCsMode2Data& le_cs_mode2_data) {
    antenna_permutation_index_ = le_cs_mode2_data.antenna_permutation_index_;
    std::copy(le_cs_mode2_data.tone_data_.begin(), le_cs_mode2_data.tone_data_.end(),
              std::back_inserter(tone_data_with_qualities_));
  }

  uint8_t antenna_permutation_index_ = 0;
  std::vector<hci::LeCsToneDataWithQuality> tone_data_with_qualities_;
};

struct Mode3Data {
  Mode3Data(const hci::LeCsMode3InitatorData& le_cs_mode3_data) {
    mode1_data_.packet_quality_ = le_cs_mode3_data.packet_quality_;
    mode1_data_.packet_nadm_ = le_cs_mode3_data.packet_nadm_;
    mode1_data_.packet_rssi_ = le_cs_mode3_data.packet_rssi_;
    mode1_data_.rtt_toa_tod_data_ = le_cs_mode3_data.toa_tod_initiator_;
    mode1_data_.packet_antenna_ = le_cs_mode3_data.packet_antenna_;

    mode2_data_.antenna_permutation_index_ = le_cs_mode3_data.antenna_permutation_index_;
    std::copy(le_cs_mode3_data.tone_data_.begin(), le_cs_mode3_data.tone_data_.end(),
              std::back_inserter(mode2_data_.tone_data_with_qualities_));
  }

  Mode3Data(const hci::LeCsMode3InitatorDataWithPacketPct& le_cs_mode3_data) {
    mode1_data_.packet_quality_ = le_cs_mode3_data.packet_quality_;
    mode1_data_.packet_nadm_ = le_cs_mode3_data.packet_nadm_;
    mode1_data_.packet_rssi_ = le_cs_mode3_data.packet_rssi_;
    mode1_data_.rtt_toa_tod_data_ = le_cs_mode3_data.toa_tod_initiator_;
    mode1_data_.packet_antenna_ = le_cs_mode3_data.packet_antenna_;
    mode1_data_.i_packet_pct1_ = le_cs_mode3_data.packet_pct1_.i_sample_;
    mode1_data_.q_packet_pct1_ = le_cs_mode3_data.packet_pct1_.q_sample_;
    mode1_data_.i_packet_pct2_ = le_cs_mode3_data.packet_pct2_.i_sample_;
    mode1_data_.q_packet_pct2_ = le_cs_mode3_data.packet_pct2_.q_sample_;

    mode2_data_.antenna_permutation_index_ = le_cs_mode3_data.antenna_permutation_index_;
    std::copy(le_cs_mode3_data.tone_data_.begin(), le_cs_mode3_data.tone_data_.end(),
              std::back_inserter(mode2_data_.tone_data_with_qualities_));
  }

  Mode3Data(const hci::LeCsMode3ReflectorData& le_cs_mode3_data) {
    mode1_data_.packet_quality_ = le_cs_mode3_data.packet_quality_;
    mode1_data_.packet_nadm_ = le_cs_mode3_data.packet_nadm_;
    mode1_data_.packet_rssi_ = le_cs_mode3_data.packet_rssi_;
    mode1_data_.rtt_toa_tod_data_ = le_cs_mode3_data.tod_toa_reflector_;
    mode1_data_.packet_antenna_ = le_cs_mode3_data.packet_antenna_;

    mode2_data_.antenna_permutation_index_ = le_cs_mode3_data.antenna_permutation_index_;
    std::copy(le_cs_mode3_data.tone_data_.begin(), le_cs_mode3_data.tone_data_.end(),
              std::back_inserter(mode2_data_.tone_data_with_qualities_));
  }

  Mode3Data(const hci::LeCsMode3ReflectorDataWithPacketPct& le_cs_mode3_data) {
    mode1_data_.packet_quality_ = le_cs_mode3_data.packet_quality_;
    mode1_data_.packet_nadm_ = le_cs_mode3_data.packet_nadm_;
    mode1_data_.packet_rssi_ = le_cs_mode3_data.packet_rssi_;
    mode1_data_.rtt_toa_tod_data_ = le_cs_mode3_data.tod_toa_reflector_;
    mode1_data_.packet_antenna_ = le_cs_mode3_data.packet_antenna_;
    mode1_data_.i_packet_pct1_ = le_cs_mode3_data.packet_pct1_.i_sample_;
    mode1_data_.q_packet_pct1_ = le_cs_mode3_data.packet_pct1_.q_sample_;
    mode1_data_.i_packet_pct2_ = le_cs_mode3_data.packet_pct2_.i_sample_;
    mode1_data_.q_packet_pct2_ = le_cs_mode3_data.packet_pct2_.q_sample_;

    mode2_data_.antenna_permutation_index_ = le_cs_mode3_data.antenna_permutation_index_;
    std::copy(le_cs_mode3_data.tone_data_.begin(), le_cs_mode3_data.tone_data_.end(),
              std::back_inserter(mode2_data_.tone_data_with_qualities_));
  }

  Mode1Data mode1_data_;
  Mode2Data mode2_data_;
};

struct StepSpecificData {
  uint8_t step_channel_;
  uint8_t mode_type_;
  // ModeSpecificData mode_specific_data_;
  std::variant<Mode0Data, Mode1Data, Mode2Data, Mode3Data> mode_specific_data_;
};

struct SubeventResult {
  /**
   * Starting ACL connection event counter for the results reported in the event
   */
  int start_acl_conn_event_counter_;
  /**
   * Frequency compensation value in units of 0.01 ppm (15-bit signed integer)
   * Unit: 0.01 ppm
   * 0xC000 - Frequency compensation value is not available, or the role is not initiator
   */
  uint16_t frequency_compensation_ = 0xC000;
  /**
   * Reference power level
   * Range: -127 to 20
   * Unit: dBm
   */
  uint8_t reference_power_level_;
  /**
   * 0x00 Ignored because phase measurement does not occur during the CS step
   * 0x01 to 0x04 Number of antenna paths used during the phase measurement stage of the CS step
   */
  uint8_t num_antenna_paths_;
  /**
   * Indicates the abort reason
   */
  hci::SubeventAbortReason subevent_abort_reason_;
  /**
   * The measured data for all steps
   */
  std::vector<StepSpecificData> step_data_;
  /**
   * Timestamp when all subevent data are received by the host; Not defined by the spec.
   * Using epoch time in nanos (e.g., 1697673127175).
   */
  long timestamp_nanos_;
};

struct CsProcedureData {
  CsProcedureData(uint16_t procedure_counter, uint8_t num_antenna_paths, uint8_t configuration_id,
                  uint8_t selected_tx_power)
      : counter(procedure_counter), num_antenna_paths(num_antenna_paths) {
    local_status = hci::CsProcedureDoneStatus::PARTIAL_RESULTS;
    remote_status = hci::CsProcedureDoneStatus::PARTIAL_RESULTS;
    // In ascending order of antenna position with tone extension data at the end
    uint16_t num_tone_data = num_antenna_paths + 1;
    for (uint8_t i = 0; i < num_tone_data; i++) {
      std::vector<std::complex<double>> empty_complex_vector;
      tone_pct_initiator.push_back(empty_complex_vector);
      tone_pct_reflector.push_back(empty_complex_vector);
      std::vector<uint8_t> empty_vector;
      tone_quality_indicator_initiator.push_back(empty_vector);
      tone_quality_indicator_reflector.push_back(empty_vector);
    }
    // RAS data
    segmentation_header_.first_segment_ = 1;
    segmentation_header_.last_segment_ = 0;
    segmentation_header_.rolling_segment_counter_ = 0;
    ranging_header_.ranging_counter_ = counter;
    ranging_header_.configuration_id_ = configuration_id;
    ranging_header_.selected_tx_power_ = selected_tx_power;
    ranging_header_.antenna_paths_mask_ = 0;
    for (uint8_t i = 0; i < num_antenna_paths; i++) {
      ranging_header_.antenna_paths_mask_ |= (1 << i);
    }
    ranging_header_.pct_format_ = ras::PctFormat::IQ;
    local_selected_tx_power_ = selected_tx_power;
  }
  // Procedure counter
  uint16_t counter;
  // Number of antenna paths (1 to 4) reported in the procedure
  uint8_t num_antenna_paths;
  // Frequency Compensation indicates fractional frequency offset (FFO) value of initiator, in
  // 0.01ppm
  std::vector<uint16_t> frequency_compensation;
  // The channel indices of every step in a CS procedure (in time order)
  std::vector<uint8_t> step_channel;
  // Measured Frequency Offset from mode 0, relative to the remote device, in 0.01ppm
  std::vector<uint16_t> measured_freq_offset;
  // Initiator's PCT (complex value) measured from mode-2 or mode-3 steps in a CS procedure (in
  // time order)
  std::vector<std::vector<std::complex<double>>> tone_pct_initiator;
  // Reflector's PCT (complex value) measured from mode-2 or mode-3 steps in a CS procedure (in
  // time order)
  std::vector<std::vector<std::complex<double>>> tone_pct_reflector;
  std::vector<std::vector<uint8_t>> tone_quality_indicator_initiator;
  std::vector<std::vector<uint8_t>> tone_quality_indicator_reflector;
  std::vector<int8_t> packet_quality_initiator;
  std::vector<int8_t> packet_quality_reflector;
  std::vector<int16_t> toa_tod_initiators;
  std::vector<int16_t> tod_toa_reflectors;
  std::vector<int8_t> rssi_initiator;
  std::vector<int8_t> rssi_reflector;
  bool contains_sounding_sequence_local_;
  bool contains_sounding_sequence_remote_;
  hci::CsProcedureDoneStatus local_status;
  hci::CsProcedureDoneStatus remote_status;
  // If any subevent is received with a Subevent_Done_Status of 0x0 (All results complete for the
  // CS subevent)
  bool contains_complete_subevent_ = false;
  // RAS data
  ras::SegmentationHeader segmentation_header_;
  ras::RangingHeader ranging_header_;
  std::vector<uint8_t> ras_raw_data_;  // raw data for multi_subevents;
  uint16_t ras_raw_data_index_ = 0;
  ras::RasSubeventHeader ras_subevent_header_;
  std::vector<uint8_t> ras_subevent_data_;
  uint8_t ras_subevent_counter_ = 0;
  // for HAL v2
  std::vector<std::shared_ptr<hal::SubeventResult>> local_subevent_data_;
  std::vector<std::shared_ptr<hal::SubeventResult>> remote_subevent_data_;
  hci::ProcedureAbortReason local_procedure_abort_reason_;
  hci::ProcedureAbortReason remote_procedure_abort_reason_;
  uint8_t local_selected_tx_power_;
  uint8_t remote_selected_tx_power_;
  // TODO(b/378942784): assign the sequence
  int procedure_sequence_;
};

struct RangingResult {
  double result_meters_;
  // A normalized value from 0 (low confidence) to 100 (high confidence) representing the confidence
  // of estimated distance. The value is -1 when unavailable.
  int8_t confidence_level_;
};

class RangingHalCallback {
public:
  virtual ~RangingHalCallback() = default;
  virtual void OnOpened(uint16_t connection_handle,
                        const std::vector<VendorSpecificCharacteristic>& vendor_specific_reply) = 0;
  virtual void OnOpenFailed(uint16_t connection_handle) = 0;
  virtual void OnHandleVendorSpecificReplyComplete(uint16_t connection_handle, bool success) = 0;
  virtual void OnResult(uint16_t connection_handle, const RangingResult& ranging_result) = 0;
};

class RangingHal : public ::bluetooth::Module {
public:
  static const ModuleFactory Factory;

  virtual ~RangingHal() = default;
  virtual bool IsBound() = 0;
  virtual RangingHalVersion GetRangingHalVersion() = 0;
  virtual void RegisterCallback(RangingHalCallback* callback) = 0;
  virtual std::vector<VendorSpecificCharacteristic> GetVendorSpecificCharacteristics() = 0;
  virtual void OpenSession(
          uint16_t connection_handle, uint16_t att_handle,
          const std::vector<hal::VendorSpecificCharacteristic>& vendor_specific_data) = 0;
  virtual void HandleVendorSpecificReply(
          uint16_t connection_handle,
          const std::vector<hal::VendorSpecificCharacteristic>& vendor_specific_reply) = 0;
  virtual void WriteRawData(uint16_t connection_handle, const ChannelSoundingRawData& raw_data) = 0;
  virtual void UpdateChannelSoundingConfig(
          uint16_t connection_handle,
          const hci::LeCsConfigCompleteView& leCsConfigCompleteView) = 0;
  virtual void UpdateProcedureEnableConfig(
          uint16_t connection_handle,
          const hci::LeCsProcedureEnableCompleteView& leCsProcedureEnableCompleteView) = 0;
  virtual void WriteProcedureData(uint16_t connection_handle, hci::CsRole local_cs_role,
                                  const CsProcedureData& procedure_data) = 0;
};

}  // namespace hal
}  // namespace bluetooth
