/******************************************************************************
 *
 *  Copyright 2022 The Android Open Source Project
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

#include "storage_helper.h"

#include "client_parser.h"
#include "gd/common/strings.h"
#include "le_audio_types.h"
#include "osi/include/log.h"

using le_audio::types::hdl_pair;

namespace le_audio {
static constexpr uint8_t LEAUDIO_PACS_STORAGE_CURRENT_LAYOUT_MAGIC = 0x00;
static constexpr uint8_t LEAUDIO_ASE_STORAGE_CURRENT_LAYOUT_MAGIC = 0x00;
static constexpr uint8_t LEAUDIO_HANDLES_STORAGE_CURRENT_LAYOUT_MAGIC = 0x00;
static constexpr uint8_t LEAUDIO_CODEC_ID_SZ = 5;

static constexpr size_t LEAUDIO_SOTRAGE_MAGIC_SZ =
    sizeof(uint8_t) /* magic is always uint8_t */;

static constexpr size_t LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ =
    LEAUDIO_SOTRAGE_MAGIC_SZ + sizeof(uint8_t); /* num_of_entries */

static constexpr size_t LEAUDIO_PACS_ENTRY_SZ =
    sizeof(uint16_t) /*handle*/ + sizeof(uint16_t) /*ccc handle*/ +
    LEAUDIO_CODEC_ID_SZ /*codec id*/ +
    sizeof(uint8_t) /*codec capabilities len*/ +
    sizeof(uint8_t) /*metadata len*/;

static constexpr size_t LEAUDIO_ASES_ENTRY_SZ =
    sizeof(uint16_t) /*handle*/ + sizeof(uint16_t) /*ccc handle*/ +
    sizeof(uint8_t) /*direction*/ + sizeof(uint8_t) /*ase id*/;

static constexpr size_t LEAUDIO_STORAGE_HANDLES_ENTRIES_SZ =
    LEAUDIO_SOTRAGE_MAGIC_SZ + sizeof(uint16_t) /*control point handle*/ +
    sizeof(uint16_t) /*ccc handle*/ +
    sizeof(uint16_t) /*sink audio location handle*/ +
    sizeof(uint16_t) /*ccc handle*/ +
    sizeof(uint16_t) /*source audio location handle*/ +
    sizeof(uint16_t) /*ccc handle*/ +
    sizeof(uint16_t) /*supported context type handle*/ +
    sizeof(uint16_t) /*ccc handle*/ +
    sizeof(uint16_t) /*available context type handle*/ +
    sizeof(uint16_t) /*ccc handle*/ + sizeof(uint16_t) /* tmas handle */;

bool serializePacs(const le_audio::types::PublishedAudioCapabilities& pacs,
                   std::vector<uint8_t>& out) {
  auto num_of_pacs = pacs.size();
  if (num_of_pacs == 0 || (num_of_pacs > std::numeric_limits<uint8_t>::max())) {
    LOG_WARN("No pacs available");
    return false;
  }

  /* Calculate the total size */
  auto pac_bin_size = LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ;
  for (auto pac_tuple : pacs) {
    auto& pac_recs = std::get<1>(pac_tuple);
    for (const auto pac : pac_recs) {
      pac_bin_size += LEAUDIO_PACS_ENTRY_SZ;
      pac_bin_size += 1; /* store size of single pac */
      pac_bin_size += pac.metadata.size();
      pac_bin_size += pac.codec_spec_caps.Size();
    }
  }

  out.resize(pac_bin_size);
  auto* ptr = out.data();

  /* header */
  UINT8_TO_STREAM(ptr, LEAUDIO_PACS_STORAGE_CURRENT_LAYOUT_MAGIC);
  UINT8_TO_STREAM(ptr, num_of_pacs);

  /* pacs entries */
  for (auto pac_tuple : pacs) {
    auto& pac_recs = std::get<1>(pac_tuple);

    for (const auto pac : pac_recs) {
      UINT16_TO_STREAM(ptr, std::get<0>(pac_tuple).val_hdl);
      UINT16_TO_STREAM(ptr, std::get<0>(pac_tuple).ccc_hdl);

      /* Pac len */
      UINT8_TO_STREAM(ptr, LEAUDIO_PACS_ENTRY_SZ + pac.codec_spec_caps.Size() +
                               pac.metadata.size());

      /* Codec ID*/
      UINT8_TO_STREAM(ptr, pac.codec_id.coding_format);
      UINT16_TO_STREAM(ptr, pac.codec_id.vendor_company_id);
      UINT16_TO_STREAM(ptr, pac.codec_id.vendor_codec_id);

      /* Codec caps */
      UINT8_TO_STREAM(ptr, pac.codec_spec_caps.Size());
      if (pac.codec_spec_caps.Size() > 0) {
        pac.codec_spec_caps.RawPacket(ptr);
      }

      /* Metadata */
      UINT8_TO_STREAM(ptr, pac.metadata.size());
      if (pac.metadata.size() > 0) {
        ARRAY_TO_STREAM(ptr, pac.metadata.data(), (int)pac.metadata.size());
      }
    }
  }
  return true;
}

bool SerializeSinkPacs(const le_audio::LeAudioDevice* leAudioDevice,
                       std::vector<uint8_t>& out) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());
  return serializePacs(leAudioDevice->snk_pacs_, out);
}

bool SerializeSourcePacs(const le_audio::LeAudioDevice* leAudioDevice,
                         std::vector<uint8_t>& out) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());
  return serializePacs(leAudioDevice->src_pacs_, out);
}

bool deserializePacs(LeAudioDevice* leAudioDevice,
                     types::PublishedAudioCapabilities& pacs_db,
                     const std::vector<uint8_t>& in) {
  if (in.size() <
      LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ + LEAUDIO_PACS_ENTRY_SZ) {
    LOG_WARN("There is not single PACS stored");
    return false;
  }

  auto* ptr = in.data();

  uint8_t magic;
  STREAM_TO_UINT8(magic, ptr);

  if (magic == LEAUDIO_PACS_STORAGE_CURRENT_LAYOUT_MAGIC) {
    uint8_t num_of_pacs;
    STREAM_TO_UINT8(num_of_pacs, ptr);

    if (in.size() < LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ +
                        (num_of_pacs * LEAUDIO_PACS_ENTRY_SZ)) {
      LOG_ERROR("Invalid persistent storage data");
      return false;
    }

    /* pacs entries */
    while (num_of_pacs--) {
      struct hdl_pair hdl_pair;
      uint8_t pac_len;

      STREAM_TO_UINT16(hdl_pair.val_hdl, ptr);
      STREAM_TO_UINT16(hdl_pair.ccc_hdl, ptr);
      STREAM_TO_UINT8(pac_len, ptr);

      pacs_db.push_back(std::make_tuple(
          hdl_pair, std::vector<struct le_audio::types::acs_ac_record>()));

      auto hdl = hdl_pair.val_hdl;
      auto pac_tuple_iter =
          std::find_if(pacs_db.begin(), pacs_db.end(), [&hdl](auto& pac_ent) {
            return std::get<0>(pac_ent).val_hdl == hdl;
          });

      std::vector<struct le_audio::types::acs_ac_record> pac_recs;
      client_parser::pacs::ParsePac(pac_recs, pac_len, ptr);
      ptr += pac_len;

      LOG_DEBUG("Registering  PAC ");
      leAudioDevice->RegisterPACs(&std::get<1>(*pac_tuple_iter), &pac_recs);
    }
  }
  return true;
}

bool DeserializeSinkPacs(le_audio::LeAudioDevice* leAudioDevice,
                         const std::vector<uint8_t>& in) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }
  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());
  return deserializePacs(leAudioDevice, leAudioDevice->snk_pacs_, in);
}

bool DeserializeSourcePacs(le_audio::LeAudioDevice* leAudioDevice,
                           const std::vector<uint8_t>& in) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }
  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());
  return deserializePacs(leAudioDevice, leAudioDevice->src_pacs_, in);
}

bool SerializeAses(const le_audio::LeAudioDevice* leAudioDevice,
                   std::vector<uint8_t>& out) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());

  auto num_of_ases = leAudioDevice->ases_.size();
  if (num_of_ases == 0 || (num_of_ases > std::numeric_limits<uint8_t>::max())) {
    LOG_WARN("No ases available");
    return false;
  }

  /* Calculate the total size */
  auto ases_bin_size = LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ +
                       num_of_ases * LEAUDIO_ASES_ENTRY_SZ;
  out.resize(ases_bin_size);
  auto* ptr = out.data();

  /* header */
  UINT8_TO_STREAM(ptr, LEAUDIO_ASE_STORAGE_CURRENT_LAYOUT_MAGIC);
  UINT8_TO_STREAM(ptr, num_of_ases);

  /* pacs entries */
  for (auto ase : leAudioDevice->ases_) {
    UINT16_TO_STREAM(ptr, ase.hdls.val_hdl);
    UINT16_TO_STREAM(ptr, ase.hdls.ccc_hdl);
    UINT8_TO_STREAM(ptr, ase.direction);
    UINT8_TO_STREAM(ptr, ase.id);
  }

  return true;
}

bool DeserializeAses(le_audio::LeAudioDevice* leAudioDevice,
                     const std::vector<uint8_t>& in) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());

  if (in.size() <
      LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ + LEAUDIO_ASES_ENTRY_SZ) {
    LOG_WARN("There is not single ASE stored");
    return false;
  }

  auto* ptr = in.data();

  uint8_t magic;
  STREAM_TO_UINT8(magic, ptr);

  if (magic == LEAUDIO_ASE_STORAGE_CURRENT_LAYOUT_MAGIC) {
    uint8_t num_of_ases;
    STREAM_TO_UINT8(num_of_ases, ptr);

    if (in.size() < LEAUDIO_STORAGE_HEADER_WITH_ENTRIES_SZ +
                        (num_of_ases * LEAUDIO_ASES_ENTRY_SZ)) {
      LOG_ERROR("Invalid persistent storage data");
      return false;
    }

    /* sets entries */
    while (num_of_ases--) {
      uint16_t handle;
      uint16_t ccc_handle;
      uint8_t direction;
      uint8_t ase_id;

      STREAM_TO_UINT16(handle, ptr);
      STREAM_TO_UINT16(ccc_handle, ptr);
      STREAM_TO_UINT8(direction, ptr);
      STREAM_TO_UINT8(ase_id, ptr);

      leAudioDevice->ases_.emplace_back(handle, ccc_handle, direction, ase_id);
      LOG_DEBUG(
          " Loading ASE ID: %d, direction %s, handle 0x%04x, ccc_handle 0x%04x",
          ase_id,
          direction == le_audio::types::kLeAudioDirectionSink ? "sink "
                                                              : "source",
          handle, ccc_handle);
    }
  }
  return true;
}

bool SerializeHandles(const LeAudioDevice* leAudioDevice,
                      std::vector<uint8_t>& out) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());

  /* Calculate the total size */
  out.resize(LEAUDIO_STORAGE_HANDLES_ENTRIES_SZ);
  auto* ptr = out.data();

  /* header */
  UINT8_TO_STREAM(ptr, LEAUDIO_HANDLES_STORAGE_CURRENT_LAYOUT_MAGIC);

  if (leAudioDevice->ctp_hdls_.val_hdl == 0 ||
      leAudioDevice->ctp_hdls_.ccc_hdl == 0) {
    LOG_WARN("Invalid control point handles ");
    return false;
  }

  UINT16_TO_STREAM(ptr, leAudioDevice->ctp_hdls_.val_hdl);
  UINT16_TO_STREAM(ptr, leAudioDevice->ctp_hdls_.ccc_hdl);

  UINT16_TO_STREAM(ptr, leAudioDevice->snk_audio_locations_hdls_.val_hdl);
  UINT16_TO_STREAM(ptr, leAudioDevice->snk_audio_locations_hdls_.ccc_hdl);

  UINT16_TO_STREAM(ptr, leAudioDevice->src_audio_locations_hdls_.val_hdl);
  UINT16_TO_STREAM(ptr, leAudioDevice->src_audio_locations_hdls_.ccc_hdl);

  UINT16_TO_STREAM(ptr, leAudioDevice->audio_supp_cont_hdls_.val_hdl);
  UINT16_TO_STREAM(ptr, leAudioDevice->audio_supp_cont_hdls_.ccc_hdl);

  UINT16_TO_STREAM(ptr, leAudioDevice->audio_avail_hdls_.val_hdl);
  UINT16_TO_STREAM(ptr, leAudioDevice->audio_avail_hdls_.ccc_hdl);

  UINT16_TO_STREAM(ptr, leAudioDevice->tmap_role_hdl_);

  return true;
}

bool DeserializeHandles(LeAudioDevice* leAudioDevice,
                        const std::vector<uint8_t>& in) {
  if (leAudioDevice == nullptr) {
    LOG_WARN(" Skipping unknown device");
    return false;
  }

  LOG_DEBUG(" device: %s", leAudioDevice->address_.ToString().c_str());

  if (in.size() != LEAUDIO_STORAGE_HANDLES_ENTRIES_SZ) {
    LOG_WARN("There is not single ASE stored");
    return false;
  }

  auto* ptr = in.data();

  uint8_t magic;
  STREAM_TO_UINT8(magic, ptr);

  if (magic != LEAUDIO_HANDLES_STORAGE_CURRENT_LAYOUT_MAGIC) {
    return false;
  }

  STREAM_TO_UINT16(leAudioDevice->ctp_hdls_.val_hdl, ptr);
  STREAM_TO_UINT16(leAudioDevice->ctp_hdls_.ccc_hdl, ptr);

  STREAM_TO_UINT16(leAudioDevice->snk_audio_locations_hdls_.val_hdl, ptr);
  STREAM_TO_UINT16(leAudioDevice->snk_audio_locations_hdls_.ccc_hdl, ptr);

  STREAM_TO_UINT16(leAudioDevice->src_audio_locations_hdls_.val_hdl, ptr);
  STREAM_TO_UINT16(leAudioDevice->src_audio_locations_hdls_.ccc_hdl, ptr);

  STREAM_TO_UINT16(leAudioDevice->audio_supp_cont_hdls_.val_hdl, ptr);
  STREAM_TO_UINT16(leAudioDevice->audio_supp_cont_hdls_.ccc_hdl, ptr);

  STREAM_TO_UINT16(leAudioDevice->audio_avail_hdls_.val_hdl, ptr);
  STREAM_TO_UINT16(leAudioDevice->audio_avail_hdls_.ccc_hdl, ptr);

  STREAM_TO_UINT16(leAudioDevice->tmap_role_hdl_, ptr);

  leAudioDevice->known_service_handles_ = true;
  return true;
}
}  // namespace le_audio