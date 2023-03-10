/******************************************************************************
 *
 *  Copyright 2015 Broadcom Corporation
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

#include "device/include/esco_parameters.h"

#include "base/logging.h"
#include "check.h"
#include "osi/include/properties.h"

static constexpr uint16_t kDefaultCodedDataSize = 16;
static constexpr uint8_t kDefaultTransportUnitSize = 0;
static constexpr esco_data_path_t kDefaultDataPath = ESCO_DATA_PATH_PCM;

// ESCO parameters defined in Core Specification Vol 4 Part E, Chapter 7.1.45
static const char kPropertyEscoOffloadInputCodedDataSize[] =
    "bluetooth.core.esco.offload.input_coded_data_size";
static const char kPropertyEscoOffloadOutputCodedDataSize[] =
    "bluetooth.core.esco.offload.output_coded_data_size";
static const char kPropertyEscoOffloadInputTransportUnitSize[] =
    "bluetooth.core.esco.offload.input_transport_unit_size";
static const char kPropertyEscoOffloadOutputTransportUnitSize[] =
    "bluetooth.core.esco.offload.output_transport_unit_size";
static const char kPropertyEscoOffloadInputDataPath[] =
    "bluetooth.core.esco.offload.input_data_path";
static const char kPropertyEscoOffloadOutputDataPath[] =
    "bluetooth.core.esco.offload.output_data_path";

static const enh_esco_params_t default_esco_parameters[ESCO_NUM_CODECS] = {
    // CVSD D1
    {
        .transmit_bandwidth = TXRX_64KBITS_RATE,
        .receive_bandwidth = TXRX_64KBITS_RATE,
        .transmit_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                   .company_id = 0x0000,
                                   .vendor_specific_codec_id = 0x0000},
        .receive_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                  .company_id = 0x0000,
                                  .vendor_specific_codec_id = 0x0000},
        .transmit_codec_frame_size = 60,
        .receive_codec_frame_size = 60,
        .input_bandwidth = INPUT_OUTPUT_64K_RATE,
        .output_bandwidth = INPUT_OUTPUT_64K_RATE,
        .input_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                .company_id = 0x0000,
                                .vendor_specific_codec_id = 0x0000},
        .output_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                 .company_id = 0x0000,
                                 .vendor_specific_codec_id = 0x0000},
        .input_coded_data_size = kDefaultCodedDataSize,
        .output_coded_data_size = kDefaultCodedDataSize,
        .input_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .output_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .input_pcm_payload_msb_position = 0,
        .output_pcm_payload_msb_position = 0,
        .input_data_path = kDefaultDataPath,
        .output_data_path = kDefaultDataPath,
        .input_transport_unit_size = kDefaultTransportUnitSize,
        .output_transport_unit_size = kDefaultTransportUnitSize,
        .max_latency_ms = 0xFFFF,  // Don't care
        .packet_types = (ESCO_PKT_TYPES_MASK_HV1 | ESCO_PKT_TYPES_MASK_HV2 |
                         ESCO_PKT_TYPES_MASK_HV3),
        .retransmission_effort = ESCO_RETRANSMISSION_OFF,
    },
    // CVSD S3
    {
        .transmit_bandwidth = TXRX_64KBITS_RATE,
        .receive_bandwidth = TXRX_64KBITS_RATE,
        .transmit_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                   .company_id = 0x0000,
                                   .vendor_specific_codec_id = 0x0000},
        .receive_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                  .company_id = 0x0000,
                                  .vendor_specific_codec_id = 0x0000},
        .transmit_codec_frame_size = 60,
        .receive_codec_frame_size = 60,
        .input_bandwidth = INPUT_OUTPUT_64K_RATE,
        .output_bandwidth = INPUT_OUTPUT_64K_RATE,
        .input_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                .company_id = 0x0000,
                                .vendor_specific_codec_id = 0x0000},
        .output_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                 .company_id = 0x0000,
                                 .vendor_specific_codec_id = 0x0000},
        .input_coded_data_size = kDefaultCodedDataSize,
        .output_coded_data_size = kDefaultCodedDataSize,
        .input_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .output_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .input_pcm_payload_msb_position = 0,
        .output_pcm_payload_msb_position = 0,
        .input_data_path = kDefaultDataPath,
        .output_data_path = kDefaultDataPath,
        .input_transport_unit_size = kDefaultTransportUnitSize,
        .output_transport_unit_size = kDefaultTransportUnitSize,
        .max_latency_ms = 10,
        .packet_types =
            (ESCO_PKT_TYPES_MASK_HV1 | ESCO_PKT_TYPES_MASK_HV2 |
             ESCO_PKT_TYPES_MASK_HV3 | ESCO_PKT_TYPES_MASK_EV3 |
             ESCO_PKT_TYPES_MASK_EV4 | ESCO_PKT_TYPES_MASK_EV5 |
             ESCO_PKT_TYPES_MASK_NO_3_EV3 | ESCO_PKT_TYPES_MASK_NO_2_EV5 |
             ESCO_PKT_TYPES_MASK_NO_3_EV5),
        .retransmission_effort = ESCO_RETRANSMISSION_POWER,
    },
    // CVSD S4
    {
        .transmit_bandwidth = TXRX_64KBITS_RATE,
        .receive_bandwidth = TXRX_64KBITS_RATE,
        .transmit_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                   .company_id = 0x0000,
                                   .vendor_specific_codec_id = 0x0000},
        .receive_coding_format = {.coding_format = ESCO_CODING_FORMAT_CVSD,
                                  .company_id = 0x0000,
                                  .vendor_specific_codec_id = 0x0000},
        .transmit_codec_frame_size = 60,
        .receive_codec_frame_size = 60,
        .input_bandwidth = INPUT_OUTPUT_64K_RATE,
        .output_bandwidth = INPUT_OUTPUT_64K_RATE,
        .input_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                .company_id = 0x0000,
                                .vendor_specific_codec_id = 0x0000},
        .output_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                 .company_id = 0x0000,
                                 .vendor_specific_codec_id = 0x0000},
        .input_coded_data_size = kDefaultCodedDataSize,
        .output_coded_data_size = kDefaultCodedDataSize,
        .input_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .output_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .input_pcm_payload_msb_position = 0,
        .output_pcm_payload_msb_position = 0,
        .input_data_path = kDefaultDataPath,
        .output_data_path = kDefaultDataPath,
        .input_transport_unit_size = kDefaultTransportUnitSize,
        .output_transport_unit_size = kDefaultTransportUnitSize,
        .max_latency_ms = 12,
        .packet_types =
            (ESCO_PKT_TYPES_MASK_HV1 | ESCO_PKT_TYPES_MASK_HV2 |
             ESCO_PKT_TYPES_MASK_HV3 | ESCO_PKT_TYPES_MASK_EV3 |
             ESCO_PKT_TYPES_MASK_EV4 | ESCO_PKT_TYPES_MASK_EV5 |
             ESCO_PKT_TYPES_MASK_NO_3_EV3 | ESCO_PKT_TYPES_MASK_NO_2_EV5 |
             ESCO_PKT_TYPES_MASK_NO_3_EV5),
        .retransmission_effort = ESCO_RETRANSMISSION_QUALITY,
    },
    // mSBC T1
    {
        .transmit_bandwidth = TXRX_64KBITS_RATE,
        .receive_bandwidth = TXRX_64KBITS_RATE,
        .transmit_coding_format = {.coding_format = ESCO_CODING_FORMAT_MSBC,
                                   .company_id = 0x0000,
                                   .vendor_specific_codec_id = 0x0000},
        .receive_coding_format = {.coding_format = ESCO_CODING_FORMAT_MSBC,
                                  .company_id = 0x0000,
                                  .vendor_specific_codec_id = 0x0000},
        .transmit_codec_frame_size = 60,
        .receive_codec_frame_size = 60,
        .input_bandwidth = INPUT_OUTPUT_128K_RATE,
        .output_bandwidth = INPUT_OUTPUT_128K_RATE,
        .input_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                .company_id = 0x0000,
                                .vendor_specific_codec_id = 0x0000},
        .output_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                 .company_id = 0x0000,
                                 .vendor_specific_codec_id = 0x0000},
        .input_coded_data_size = kDefaultCodedDataSize,
        .output_coded_data_size = kDefaultCodedDataSize,
        .input_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .output_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .input_pcm_payload_msb_position = 0,
        .output_pcm_payload_msb_position = 0,
        .input_data_path = kDefaultDataPath,
        .output_data_path = kDefaultDataPath,
        .input_transport_unit_size = kDefaultTransportUnitSize,
        .output_transport_unit_size = kDefaultTransportUnitSize,
        .max_latency_ms = 8,
        .packet_types =
            (ESCO_PKT_TYPES_MASK_EV3 | ESCO_PKT_TYPES_MASK_NO_3_EV3 |
             ESCO_PKT_TYPES_MASK_NO_2_EV5 | ESCO_PKT_TYPES_MASK_NO_3_EV5 |
             ESCO_PKT_TYPES_MASK_NO_2_EV3),
        .retransmission_effort = ESCO_RETRANSMISSION_QUALITY,
    },
    // mSBC T2
    {
        .transmit_bandwidth = TXRX_64KBITS_RATE,
        .receive_bandwidth = TXRX_64KBITS_RATE,
        .transmit_coding_format = {.coding_format = ESCO_CODING_FORMAT_MSBC,
                                   .company_id = 0x0000,
                                   .vendor_specific_codec_id = 0x0000},
        .receive_coding_format = {.coding_format = ESCO_CODING_FORMAT_MSBC,
                                  .company_id = 0x0000,
                                  .vendor_specific_codec_id = 0x0000},
        .transmit_codec_frame_size = 60,
        .receive_codec_frame_size = 60,
        .input_bandwidth = INPUT_OUTPUT_128K_RATE,
        .output_bandwidth = INPUT_OUTPUT_128K_RATE,
        .input_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                .company_id = 0x0000,
                                .vendor_specific_codec_id = 0x0000},
        .output_coding_format = {.coding_format = ESCO_CODING_FORMAT_LINEAR,
                                 .company_id = 0x0000,
                                 .vendor_specific_codec_id = 0x0000},
        .input_coded_data_size = kDefaultCodedDataSize,
        .output_coded_data_size = kDefaultCodedDataSize,
        .input_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .output_pcm_data_format = ESCO_PCM_DATA_FORMAT_2_COMP,
        .input_pcm_payload_msb_position = 0,
        .output_pcm_payload_msb_position = 0,
        .input_data_path = kDefaultDataPath,
        .output_data_path = kDefaultDataPath,
        .input_transport_unit_size = kDefaultTransportUnitSize,
        .output_transport_unit_size = kDefaultTransportUnitSize,
        .max_latency_ms = 13,
        .packet_types =
            (ESCO_PKT_TYPES_MASK_EV3 | ESCO_PKT_TYPES_MASK_NO_3_EV3 |
             ESCO_PKT_TYPES_MASK_NO_2_EV5 | ESCO_PKT_TYPES_MASK_NO_3_EV5),
        .retransmission_effort = ESCO_RETRANSMISSION_QUALITY,
    },
};

enh_esco_params_t esco_parameters_for_codec(esco_codec_t codec, bool offload) {
  CHECK(codec >= 0) << "codec index " << (int)codec << "< 0";
  CHECK(codec < ESCO_NUM_CODECS)
      << "codec index " << (int)codec << " > " << ESCO_NUM_CODECS;

  enh_esco_params_t param = default_esco_parameters[codec];

  if (offload) {
    // Set vendor-specific parameters
    param.input_transport_unit_size = osi_property_get_int32(
        kPropertyEscoOffloadInputTransportUnitSize, kDefaultTransportUnitSize);
    param.output_transport_unit_size = osi_property_get_int32(
        kPropertyEscoOffloadOutputTransportUnitSize, kDefaultTransportUnitSize);
    param.input_coded_data_size = osi_property_get_int32(
        kPropertyEscoOffloadInputCodedDataSize, kDefaultCodedDataSize);
    param.output_coded_data_size = osi_property_get_int32(
        kPropertyEscoOffloadOutputCodedDataSize, kDefaultCodedDataSize);
    param.input_data_path = osi_property_get_int32(
        kPropertyEscoOffloadInputDataPath, kDefaultDataPath);
    param.output_data_path = osi_property_get_int32(
        kPropertyEscoOffloadOutputDataPath, kDefaultDataPath);
    return param;
  }

  param.input_data_path = param.output_data_path = ESCO_DATA_PATH_HCI;

  if (codec >= ESCO_CODEC_MSBC_T1) {
    param.transmit_coding_format.coding_format = ESCO_CODING_FORMAT_TRANSPNT;
    param.receive_coding_format.coding_format = ESCO_CODING_FORMAT_TRANSPNT;
    param.input_coding_format.coding_format = ESCO_CODING_FORMAT_TRANSPNT;
    param.output_coding_format.coding_format = ESCO_CODING_FORMAT_TRANSPNT;
    param.input_bandwidth = TXRX_64KBITS_RATE;
    param.output_bandwidth = TXRX_64KBITS_RATE;
  }

  return param;
}
