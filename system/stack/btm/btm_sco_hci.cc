/*
 * Copyright 2021 The Android Open Source Project
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

#include <errno.h>
#include <grp.h>
#include <math.h>
#include <sys/stat.h>
#include <unistd.h>

#include <memory>
#include <vector>

// Define before including log.h
#define LOG_TAG "sco_hci"

#include "btif/include/core_callbacks.h"
#include "btif/include/stack_manager.h"
#include "osi/include/allocator.h"
#include "osi/include/log.h"
#include "stack/btm/btm_sco.h"
#include "udrv/include/uipc.h"

#define SCO_DATA_READ_POLL_MS 10
#define SCO_HOST_DATA_PATH "/var/run/bluetooth/audio/.sco_data"
// TODO(b/198260375): Make SCO data owner group configurable.
#define SCO_HOST_DATA_GROUP "bluetooth-audio"

/* Per Bluetooth Core v5.0 and HFP 1.7 specification. */
#define BTM_WBS_H2_HEADER_0 0x01
#define BTM_WBS_H2_HEADER_LEN 2
#define BTM_WBS_PKT_LEN 60
#define BTM_WBS_MAX_FS 240 /* Frame size: mSBC=120, LC3SWB=240 */

namespace {

std::unique_ptr<tUIPC_STATE> sco_uipc = nullptr;

void sco_data_cb(tUIPC_CH_ID, tUIPC_EVENT event) {
  switch (event) {
    case UIPC_OPEN_EVT:
      /*
       * Read directly from media task from here on (keep callback for
       * connection events.
       */
      UIPC_Ioctl(*sco_uipc, UIPC_CH_ID_AV_AUDIO, UIPC_REG_REMOVE_ACTIVE_READSET,
                 NULL);
      UIPC_Ioctl(*sco_uipc, UIPC_CH_ID_AV_AUDIO, UIPC_SET_READ_POLL_TMO,
                 reinterpret_cast<void*>(SCO_DATA_READ_POLL_MS));
      break;
    default:
      break;
  }
}

}  // namespace

namespace bluetooth {
namespace audio {
namespace sco {

void open() {
  if (sco_uipc != nullptr) {
    LOG_WARN("Re-opening UIPC that is already running");
  }

  sco_uipc = UIPC_Init();
  if (sco_uipc == nullptr) {
    LOG_ERROR("%s failed to init UIPC", __func__);
    return;
  }

  UIPC_Open(*sco_uipc, UIPC_CH_ID_AV_AUDIO, sco_data_cb, SCO_HOST_DATA_PATH);
  struct group* grp = getgrnam(SCO_HOST_DATA_GROUP);
  chmod(SCO_HOST_DATA_PATH, 0770);
  if (grp) {
    int res = chown(SCO_HOST_DATA_PATH, -1, grp->gr_gid);
    if (res == -1) {
      LOG_ERROR("%s failed: %s", __func__, strerror(errno));
    }
  }
}

void cleanup() {
  if (sco_uipc == nullptr) {
    return;
  }
  UIPC_Close(*sco_uipc, UIPC_CH_ID_ALL);
  sco_uipc = nullptr;
}

size_t read(uint8_t* p_buf, uint32_t len) {
  if (sco_uipc == nullptr) {
    LOG_WARN("Read from uninitialized or closed UIPC");
    return 0;
  }
  return UIPC_Read(*sco_uipc, UIPC_CH_ID_AV_AUDIO, p_buf, len);
}

size_t write(const uint8_t* p_buf, uint32_t len) {
  if (sco_uipc == nullptr) {
    LOG_WARN("Write to uninitialized or closed UIPC");
    return 0;
  }
  return UIPC_Send(*sco_uipc, UIPC_CH_ID_AV_AUDIO, 0, p_buf, len) ? len : 0;
}

namespace wbs {

/* Second octet of H2 header is composed by 4 bits fixed 0x8 and 4 bits
 * sequence number 0000, 0011, 1100, 1111. */
static const uint8_t btm_h2_header_frames_count[] = {0x08, 0x38, 0xc8, 0xf8};

/* Supported SCO packet sizes for mSBC/LC3. The frame parsing
 * code ties to limited packet size values. Specifically list them out
 * to check against when setting packet size. The first entry is the default
 * value as a fallback. */
constexpr size_t btm_wbs_supported_pkt_size[] = {BTM_WBS_PKT_LEN, 72, 0};
/* Buffer size should be set to least common multiple of SCO packet size and
 * BTM_WBS_PKT_LEN for optimizing buffer copy. */
constexpr size_t btm_wbs_buffer_size[] = {BTM_WBS_PKT_LEN, 360, 0};

/* Define the structure that contains (S)WBS data */
struct tBTM_WBS_INFO {
  core::CodecInterface* active_codec;

  size_t packet_size; /* SCO mSBC/LC3 packet size supported by lower layer */
  size_t buf_size; /* The size of the buffer, determined by the packet_size. */

  uint8_t* wbs_decode_buf;  /* Buffer to store mSBC/LC3 packets to decode */
  size_t decode_buf_wo;     /* Write offset of the decode buffer */
  size_t decode_buf_ro;     /* Read offset of the decode buffer */
  bool read_corrupted;      /* If the current WBS packet read is corrupted */

  uint8_t* wbs_encode_buf;  /* Buffer to store the encoded SCO packets */
  size_t encode_buf_wo;     /* Write offset of the encode buffer */
  size_t encode_buf_ro;     /* Read offset of the encode buffer */

  uint8_t num_encoded_wbs_pkts; /* Number of the encoded packets */

  int16_t decoded_pcm_buf[BTM_WBS_MAX_FS]; /* Buffer to store decoded PCM */

  static size_t get_supported_packet_size(size_t pkt_size,
                                          size_t* buffer_size) {
    int i;
    for (i = 0; btm_wbs_supported_pkt_size[i] != 0 &&
                btm_wbs_supported_pkt_size[i] != pkt_size;
         i++)
      ;
    /* In case of unsupported value, error log and fallback to
     * BTM_WBS_PKT_LEN(60). */
    if (btm_wbs_supported_pkt_size[i] == 0) {
      LOG_WARN("Unsupported packet size %lu", (unsigned long)pkt_size);
      i = 0;
    }

    if (buffer_size) {
      *buffer_size = btm_wbs_buffer_size[i];
    }
    return btm_wbs_supported_pkt_size[i];
  }

  bool verify_h2_header_seq_num(const uint8_t num) {
    for (int i = 0; i < 4; i++) {
      if (num == btm_h2_header_frames_count[i]) {
        return true;
      }
    }
    return false;
  }

 public:
  size_t init(size_t pkt_size) {
    decode_buf_wo = 0;
    decode_buf_ro = 0;
    encode_buf_wo = 0;
    encode_buf_ro = 0;

    pkt_size = get_supported_packet_size(pkt_size, &buf_size);
    if (pkt_size == packet_size) return packet_size;
    packet_size = pkt_size;

    if (wbs_decode_buf) osi_free(wbs_decode_buf);
    wbs_decode_buf = (uint8_t*)osi_calloc(buf_size);

    if (wbs_encode_buf) osi_free(wbs_encode_buf);
    wbs_encode_buf = (uint8_t*)osi_calloc(buf_size);

    return packet_size;
  }

  void deinit() {
    if (wbs_decode_buf) osi_free(wbs_decode_buf);
    if (wbs_encode_buf) osi_free(wbs_encode_buf);
    wbs_decode_buf = nullptr;
    wbs_encode_buf = nullptr;
  }

  size_t decodable() { return decode_buf_wo - decode_buf_ro; }

  void mark_pkt_decoded() {
    if (decode_buf_ro + BTM_WBS_PKT_LEN > decode_buf_wo) {
      LOG_ERROR("Trying to mark read offset beyond write offset.");
      return;
    }

    decode_buf_ro += BTM_WBS_PKT_LEN;
    if (decode_buf_ro == decode_buf_wo) {
      decode_buf_ro = 0;
      decode_buf_wo = 0;
    }
  }

  size_t write(const uint8_t* input, size_t len) {
    if (len > buf_size - decode_buf_wo) {
      return 0;
    }

    std::copy(input, input + len, wbs_decode_buf + decode_buf_wo);
    decode_buf_wo += len;
    return len;
  }

  const uint8_t* find_wbs_pkt_head() {
    if (read_corrupted) {
      LOG_DEBUG("Skip corrupted WBS packets");
      read_corrupted = false;
      return nullptr;
    }

    size_t rp = 0;
    while (rp < BTM_WBS_PKT_LEN &&
           decode_buf_wo - (decode_buf_ro + rp) >= BTM_WBS_PKT_LEN) {
      if ((wbs_decode_buf[decode_buf_ro + rp] != BTM_WBS_H2_HEADER_0) ||
          (!verify_h2_header_seq_num(wbs_decode_buf[decode_buf_ro + rp + 1]))) {
        rp++;
        continue;
      }

      if (rp != 0) {
        LOG_WARN("Skipped %lu bytes of WBS data ahead of a valid WBS frame",
                 (unsigned long)rp);
        decode_buf_ro += rp;
      }
      return &wbs_decode_buf[decode_buf_ro];
    }

    return nullptr;
  }

  /* Fill in the WBS header and update the buffer's write offset to guard the
   * buffer space to be written. Return a pointer to the start of WBS packet's
   * body for the caller to fill the encoded mSBC/LC3 data if there is enough
   * space in the buffer to fill in a new packet, otherwise return a nullptr. */
  uint8_t* fill_wbs_pkt_template() {
    uint8_t* wp = &wbs_encode_buf[encode_buf_wo];
    if (buf_size - encode_buf_wo < BTM_WBS_PKT_LEN) {
      LOG_DEBUG("Packet queue can't accommodate more packets.");
      return nullptr;
    }

    wp[0] = BTM_WBS_H2_HEADER_0;
    wp[1] = btm_h2_header_frames_count[num_encoded_wbs_pkts % 4];
    encode_buf_wo += BTM_WBS_PKT_LEN;

    num_encoded_wbs_pkts++;
    return wp + BTM_WBS_H2_HEADER_LEN;
  }

  size_t mark_pkt_dequeued() {
    LOG_DEBUG(
        "Try to mark an encoded packet dequeued: ro:%lu wo:%lu pkt_size:%lu",
        (unsigned long)encode_buf_ro, (unsigned long)encode_buf_wo,
        (unsigned long)packet_size);

    if (encode_buf_wo - encode_buf_ro < packet_size) return 0;

    encode_buf_ro += packet_size;
    if (encode_buf_ro == encode_buf_wo) {
      encode_buf_ro = 0;
      encode_buf_wo = 0;
    }

    return packet_size;
  }

  const uint8_t* sco_pkt_read_ptr() {
    if (encode_buf_wo - encode_buf_ro < packet_size) {
      LOG_DEBUG("Insufficient data as a SCO packet to read.");
      return nullptr;
    }

    return &wbs_encode_buf[encode_buf_ro];
  }
};

static tBTM_WBS_INFO* wbs_info = nullptr;

size_t init(esco_coding_format_t coding_format, size_t pkt_size) {
  if (wbs_info) {
    LOG_WARN("Re-initiating WBS buffer that is active or not cleaned");
    wbs_info->deinit();
    osi_free(wbs_info);
  }

  wbs_info = (tBTM_WBS_INFO*)osi_calloc(sizeof(*wbs_info));

  switch (coding_format) {
    case ESCO_CODING_FORMAT_MSBC:
      wbs_info->active_codec = GetInterfaceToProfiles()->msbcCodec;
      break;
    case ESCO_CODING_FORMAT_LC3:
      wbs_info->active_codec = GetInterfaceToProfiles()->lc3Codec;
      break;
    default:
      LOG_ERROR("%s: Unknown coding format %d, trying to use mSBC", __func__,
                coding_format);
      wbs_info->active_codec = GetInterfaceToProfiles()->msbcCodec;
  }

  wbs_info->active_codec->initialize();

  return wbs_info->init(pkt_size);
}

bool cleanup(int* num_decoded_frames, double* packet_loss_ratio) {
  bool ret = false;

  if (wbs_info && wbs_info->active_codec &&
      wbs_info->active_codec->cleanup(num_decoded_frames, packet_loss_ratio)) {
    ret = true;
  }

  if (wbs_info) {
    wbs_info->deinit();
    osi_free(wbs_info);
    wbs_info = nullptr;
  }

  return ret;
}

size_t enqueue_packet(const uint8_t* data, size_t pkt_size, bool corrupted) {
  if (wbs_info == nullptr) {
    LOG_WARN("WBS buffer uninitialized or cleaned");
    return 0;
  }

  if (pkt_size != wbs_info->packet_size) {
    LOG_WARN(
        "Ignoring the coming packet with size %lu that is inconsistent with "
        "the HAL reported packet size %lu",
        (unsigned long)pkt_size, (unsigned long)wbs_info->packet_size);
    return 0;
  }

  if (data == nullptr) {
    LOG_WARN("Invalid data to enqueue");
    return 0;
  }

  wbs_info->read_corrupted |= corrupted;
  if (wbs_info->write(data, pkt_size) != pkt_size) {
    LOG_DEBUG("Fail to write packet with size %lu to buffer",
              (unsigned long)pkt_size);
    return 0;
  }

  return pkt_size;
}

size_t decode(const uint8_t** out_data) {
  const uint8_t* frame_head = nullptr;

  if (wbs_info == nullptr) {
    LOG_WARN("WBS buffer uninitialized or cleaned");
    return 0;
  }

  if (out_data == nullptr) {
    LOG_WARN("%s Invalid output pointer", __func__);
    return 0;
  }

  if (wbs_info->decodable() < BTM_WBS_PKT_LEN) {
    LOG_DEBUG("No complete WBS packet to decode");
    return 0;
  }

  frame_head = wbs_info->find_wbs_pkt_head();

  uint32_t out_len = wbs_info->active_codec->decodePacket(
      frame_head, wbs_info->decoded_pcm_buf);

  *out_data = (const uint8_t*)wbs_info->decoded_pcm_buf;

  wbs_info->mark_pkt_decoded();

  return out_len;
}

size_t encode(int16_t* data) {
  uint8_t* pkt_body = nullptr;
  if (wbs_info == nullptr) {
    LOG_WARN("WBS buffer uninitialized or cleaned");
    return 0;
  }

  if (data == nullptr) {
    LOG_WARN("Invalid data to encode");
    return 0;
  }

  pkt_body = wbs_info->fill_wbs_pkt_template();
  if (pkt_body == nullptr) {
    LOG_DEBUG("Failed to fill the template to fill the WBS packet");
    return 0;
  }

  return wbs_info->active_codec->encodePacket(data, pkt_body);
}

size_t dequeue_packet(const uint8_t** output) {
  if (wbs_info == nullptr) {
    LOG_WARN("WBS buffer uninitialized or cleaned");
    return 0;
  }

  if (output == nullptr) {
    LOG_WARN("%s Invalid output pointer", __func__);
    return 0;
  }

  *output = wbs_info->sco_pkt_read_ptr();
  if (*output == nullptr) {
    LOG_DEBUG("Insufficient data to dequeue.");
    return 0;
  }

  return wbs_info->mark_pkt_dequeued();
}

}  // namespace wbs

}  // namespace sco
}  // namespace audio
}  // namespace bluetooth
