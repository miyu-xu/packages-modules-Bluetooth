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

#include <cstdint>
#include <string>

namespace bluetooth {
namespace avrc {

/**
 * Struct containing all the required data to add the AVRC SDP records.
 */
struct AddSdpRecordRequest {
  /**
   * Service uuid for the SDP record.
   */
  uint16_t service_uuid;

  /**
   * Service name for the record.
   */
  std::string service_name;

  /**
   * Provider name for the record.
   */
  std::string provider_name;

  /**
   * Categories of features that are supported.
   * Each bit represents the feature that is supported.
   */
  uint16_t categories;

  /**
   * Is browse supported by the service.
   */
  bool browse_supported;

  /**
   * Profile version for the service.
   */
  uint16_t profile_version;

  /**
   * Cover art psm for the service.
   */
  uint16_t cover_art_psm;

  /***
   *
   * Sets the category bit to the existing categories.
   * @param category category bit that needs to be added.
   */
  void AddToExistingCategories(uint16_t category) { categories |= category; }

  /**
   * Remove the category bit from the existing set of categories.
   * @param category category bit that needs to be removed.
   */
  void RemoveCategory(uint16_t category) { categories &= ~category; }
};

/**
 * Abstract class to add, remove AVRC SDP records.
 */
class AvrcSdpRecordHelper {
 public:
  /**
   * Default constructor.
   */
  AvrcSdpRecordHelper() = default;

  /**
   * Default virtual destructor.
   */
  virtual ~AvrcSdpRecordHelper() = default;

  /**
   * Adds the records if none exists. If records already exists, then it only
   * updates the categories that can be supported.
   * @param add_record_request record request that needs
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  virtual uint16_t AddRecord(const AddSdpRecordRequest& add_record_request,
                             const bool add_sys_uid = true);

  /**
   * Removes the SDP records.
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  uint16_t RemoveRecord();

  /**
   * Abstract method for child class to implement.
   * @param cover_art_psm cover art protocol service multiplexor.
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  virtual uint16_t EnableCovertArt(uint16_t cover_art_psm) = 0;

  /**
   * Abstract method for child class to implement.
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  virtual uint16_t DisableCovertArt() = 0;

 protected:
  /**
   * Record handle for the SDP records.
   */
  uint32_t sdp_record_handle_ = -1;

  /**
   * Cached SDP record request.
   */
  AddSdpRecordRequest cached_add_sdp_record_request_;

  /**
   * Update the SDP record with the new set of categories.
   * @param new_categories new categories bits that needs to be added.
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  uint16_t UpdateRecord(const uint16_t& new_categories);
};

/**
 * Helper class to add Control AVRC SDP records.
 */
class ControlAvrcSdpRecordHelper : public AvrcSdpRecordHelper {
 public:
  /**
   * Default constructor.
   */
  ControlAvrcSdpRecordHelper() = default;

  /**
   * Invokes the super method #AddRecord if no new record is present.
   * Otherwise, invokes the super method #UpdateRecord and updates the profile
   * version based on certain conditions.
   * @param add_record_request
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  uint16_t AddRecord(const AddSdpRecordRequest& add_record_request,
                     const bool add_sys_uid = true) override;

  /**
   * Unsupported method for control SDP records.
   * @param cover_art_psm no-op.
   * @return AVRC_FAIL as it's unsupported.
   */
  uint16_t EnableCovertArt(uint16_t cover_art_psm) override;

  /**
   * Unsupported method for control SDP records.
   * @return AVRC_FAIL as it's unsupported.
   */
  uint16_t DisableCovertArt() override;
};

/**
 * Helper class to add Target AVRC SDP records.
 */
class TargetAvrcSdpRecordHelper : public AvrcSdpRecordHelper {
 public:
  /**
   * Default constructor.
   */
  TargetAvrcSdpRecordHelper() = default;

  /**
   * Enables cover art support. It removes the existing SDP records, updates the
   * cached SDP record request with cover art attributes (categories & cover art
   * psm), creates new AVRC SDP records.
   * @param cover_art_psm cover art protocol service multiplexor.
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  uint16_t EnableCovertArt(uint16_t cover_art_psm) override;

  /**
   * Disables cover art support. It removes the existing SDP records, removes
   * the cached SDP record request with cover art attributes (categories & cover
   * art psm), creates new AVRC SDP records w/o cover art support.
   * @param cover_art_psm cover art protocol service multiplexor
   * @return AVRC_SUCCESS if successful.
   *         AVRC_FAIL otherwise
   */
  uint16_t DisableCovertArt() override;
};
}  // namespace avrc
}  // namespace bluetooth
