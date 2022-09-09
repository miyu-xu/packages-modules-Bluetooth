/*
 * Copyright 2022 The Android Open Source Project
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

#include <base/bind.h>
#include <base/location.h>
#include <base/logging.h>

#include "btif/include/btif_common.h"
#include "btif_config.h"
#include "btif_storage.h"
#include "include/bta_le_audio_api.h"
#include "stack/include/btu.h"  // do_in_main_thread

#define BTIF_STORAGE_LEAUDIO_AUTOCONNECT "LeAudioAutoconnect"
#define BTIF_STORAGE_LEAUDIO_HANDLES_BIN "LeAudioHandlesBin"
#define BTIF_STORAGE_LEAUDIO_SINK_PACS_BIN "SinkPacsBin"
#define BTIF_STORAGE_LEAUDIO_SOURCE_PACS_BIN "SourcePacsBin"
#define BTIF_STORAGE_LEAUDIO_ASES_BIN "AsesBin"
#define BTIF_STORAGE_LEAUDIO_SINK_AUDIOLOCATION "SinkAudioLocation"
#define BTIF_STORAGE_LEAUDIO_SOURCE_AUDIOLOCATION "SourceAudioLocation"
#define BTIF_STORAGE_LEAUDIO_SINK_SUPPORTED_CONTEXT_TYPE \
  "SinkSupportedContextType"
#define BTIF_STORAGE_LEAUDIO_SOURCE_SUPPORTED_CONTEXT_TYPE \
  "SourceSupportedContextType"

using base::Bind;
using bluetooth::Uuid;

namespace le_audio {
namespace storage {
/** Set autoconnect information for LeAudio device */
void SetLeAudioAutoconnect(const RawAddress& addr, bool autoconnect) {
  do_in_jni_thread(FROM_HERE, Bind(
                                  [](const RawAddress& addr, bool autoconnect) {
                                    std::string bdstr = addr.ToString();
                                    VLOG(2)
                                        << "saving le audio device: " << bdstr;
                                    btif_config_set_int(
                                        bdstr, BTIF_STORAGE_LEAUDIO_AUTOCONNECT,
                                        autoconnect);
                                    btif_config_save();
                                  },
                                  addr, autoconnect));
}

/** Store ASEs information */
void UpdateHandlesBin(const RawAddress& addr) {
  std::vector<uint8_t> handles;

  if (LeAudioClient::GetHandlesForStorage(addr, handles)) {
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> handles) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_LEAUDIO_HANDLES_BIN,
                                  handles.data(), handles.size());
              btif_config_save();
            },
            addr, std::move(handles)));
  }
}

/** Store PACs information */
void UpdatePacsBin(const RawAddress& addr) {
  std::vector<uint8_t> sink_pacs;

  if (LeAudioClient::GetSinkPacsForStorage(addr, sink_pacs)) {
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> sink_pacs) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_LEAUDIO_SINK_PACS_BIN,
                                  sink_pacs.data(), sink_pacs.size());
              btif_config_save();
            },
            addr, std::move(sink_pacs)));
  }

  std::vector<uint8_t> source_pacs;
  if (LeAudioClient::GetSourcePacsForStorage(addr, source_pacs)) {
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> source_pacs) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_LEAUDIO_SOURCE_PACS_BIN,
                                  source_pacs.data(), source_pacs.size());
              btif_config_save();
            },
            addr, std::move(source_pacs)));
  }
}

/** Store ASEs information */
void UpdateAseBin(const RawAddress& addr) {
  std::vector<uint8_t> ases;

  if (LeAudioClient::GetAsesForStorage(addr, ases)) {
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> ases) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_LEAUDIO_ASES_BIN,
                                  ases.data(), ases.size());
              btif_config_save();
            },
            addr, std::move(ases)));
  }
}

/** Store Le Audio device audio locations */
void SetAudioLocation(const RawAddress& addr, uint32_t sink_location,
                      uint32_t source_location) {
  do_in_jni_thread(
      FROM_HERE,
      Bind(
          [](const RawAddress& addr, int sink_location, int source_location) {
            std::string bdstr = addr.ToString();
            LOG_DEBUG("saving le audio device: %s", bdstr.c_str());
            btif_config_set_int(bdstr, BTIF_STORAGE_LEAUDIO_SINK_AUDIOLOCATION,
                                sink_location);
            btif_config_set_int(bdstr,
                                BTIF_STORAGE_LEAUDIO_SOURCE_AUDIOLOCATION,
                                source_location);
            btif_config_save();
          },
          addr, sink_location, source_location));
}

/** Store Le Audio device context types */
void SetSupportedContextTypes(const RawAddress& addr,
                              uint16_t sink_supported_context_type,
                              uint16_t source_supported_context_type) {
  do_in_jni_thread(
      FROM_HERE,
      Bind(
          [](const RawAddress& addr, int sink_supported_context_type,
             int source_supported_context_type) {
            std::string bdstr = addr.ToString();
            LOG_DEBUG("saving le audio device: %s", bdstr.c_str());
            btif_config_set_int(
                bdstr, BTIF_STORAGE_LEAUDIO_SINK_SUPPORTED_CONTEXT_TYPE,
                sink_supported_context_type);
            btif_config_set_int(
                bdstr, BTIF_STORAGE_LEAUDIO_SOURCE_SUPPORTED_CONTEXT_TYPE,
                source_supported_context_type);
            btif_config_save();
          },
          addr, sink_supported_context_type, source_supported_context_type));
}

/** Loads information about bonded Le Audio devices */
void AddBondedDevices() {
  for (const auto& bd_addr : btif_config_get_paired_devices()) {
    auto name = bd_addr.ToString();

    int size = STORAGE_UUID_STRING_SIZE * BT_MAX_NUM_UUIDS;
    char uuid_str[size];
    bool isLeAudioDevice = false;
    if (btif_config_get_str(name, BTIF_STORAGE_PATH_REMOTE_SERVICE, uuid_str,
                            &size)) {
      Uuid p_uuid[BT_MAX_NUM_UUIDS];
      size_t num_uuids =
          btif_split_uuids_string(uuid_str, p_uuid, BT_MAX_NUM_UUIDS);
      for (size_t i = 0; i < num_uuids; i++) {
        if (p_uuid[i] == Uuid::FromString("184E")) {
          isLeAudioDevice = true;
          break;
        }
      }
    }
    if (!isLeAudioDevice) {
      continue;
    }

    BTIF_TRACE_DEBUG("Remote device:%s", name.c_str());

    int value;
    bool autoconnect = false;
    if (btif_config_get_int(name, BTIF_STORAGE_LEAUDIO_AUTOCONNECT, &value))
      autoconnect = !!value;

    int sink_audio_location = 0;
    if (btif_config_get_int(name, BTIF_STORAGE_LEAUDIO_SINK_AUDIOLOCATION,
                            &value))
      sink_audio_location = value;

    int source_audio_location = 0;
    if (btif_config_get_int(name, BTIF_STORAGE_LEAUDIO_SOURCE_AUDIOLOCATION,
                            &value))
      source_audio_location = value;

    int sink_supported_context_type = 0;
    if (btif_config_get_int(
            name, BTIF_STORAGE_LEAUDIO_SINK_SUPPORTED_CONTEXT_TYPE, &value))
      sink_supported_context_type = value;

    int source_supported_context_type = 0;
    if (btif_config_get_int(
            name, BTIF_STORAGE_LEAUDIO_SOURCE_SUPPORTED_CONTEXT_TYPE, &value))
      source_supported_context_type = value;

    size_t buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_LEAUDIO_HANDLES_BIN);
    std::vector<uint8_t> handles(buffer_size);
    if (buffer_size > 0) {
      btif_config_get_bin(name, BTIF_STORAGE_LEAUDIO_HANDLES_BIN,
                          handles.data(), &buffer_size);
    }

    buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_LEAUDIO_SINK_PACS_BIN);
    std::vector<uint8_t> sink_pacs(buffer_size);
    if (buffer_size > 0) {
      btif_config_get_bin(name, BTIF_STORAGE_LEAUDIO_SINK_PACS_BIN,
                          sink_pacs.data(), &buffer_size);
    }

    buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_LEAUDIO_SOURCE_PACS_BIN);
    std::vector<uint8_t> source_pacs(buffer_size);
    if (buffer_size > 0) {
      btif_config_get_bin(name, BTIF_STORAGE_LEAUDIO_SOURCE_PACS_BIN,
                          source_pacs.data(), &buffer_size);
    }

    buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_LEAUDIO_ASES_BIN);
    std::vector<uint8_t> ases(buffer_size);
    if (buffer_size > 0) {
      btif_config_get_bin(name, BTIF_STORAGE_LEAUDIO_ASES_BIN, ases.data(),
                          &buffer_size);
    }

    do_in_main_thread(
        FROM_HERE,
        Bind(&LeAudioClient::AddFromStorage, bd_addr, autoconnect,
             sink_audio_location, source_audio_location,
             sink_supported_context_type, source_supported_context_type,
             std::move(handles), std::move(sink_pacs), std::move(source_pacs),
             std::move(ases)));
  }
}

/** Remove the Le Audio device from storage */
void RemoveDevice(const RawAddress& address) {
  std::string addrstr = address.ToString();
  btif_config_set_int(addrstr, BTIF_STORAGE_LEAUDIO_AUTOCONNECT, false);
}
}  // namespace storage
}  // namespace le_audio