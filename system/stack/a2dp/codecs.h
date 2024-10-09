
struct CodecCapability {
    /// [AVDTPv1.3] Media Codec Capability.
    /// Interpretation of these octets is codec specific and standardized
    /// as part of the codec specification.
    uint8_t codec_capability[];

    /// Vendor codec configuration parameters not exposed through the
    /// AVDTP Media Codec Capability. Includes for example the LDAC bit rate
    /// selection. Interpretation of these values is codec specific.
    /// The values will be passed to the Java layer as the codecSpecific{1-4}
    /// values.
    uint32_t vendor_capability[4];

    CodecId GetId() const;
};

class Codec {
public:
  /// Standardized codec identifier.
  /// The codec identifier is 40 bits,
  ///  - Bits 0-7: Audio Codec ID, as defined by [ID 6.5.1]
  ///       0x00: SBC
  ///       0x02: AAC
  ///      0xFF: Vendor
  ///  - Bits 8-23: Company ID,
  ///      set to 0, if octet 0 is not 0xFF.
  ///  - Bits 24-39: Vendor-defined codec ID,
  ///      set to 0, if octet 0 is not 0xFF.
  const tA2DP_CODEC_ID id;

  /// Codec name.
  /// Provided for debugging purposes.
  const std::string name;

  /// Return true if the RTP marker bit should be set in the RTP header
  /// as defined in RFC 6416.
  const bool set_rtp_marker_bit;

  /// Return the list of selectable codec capabilities given the
  /// queried remote capabilities.
  virtual btav_a2dp_codec_config_t GetSelectableCapability(CodecCapability const& remote_codec_configuration) const;

  /// Return the preferred configuration to be used given the queried
  /// remote capabilities.
  virtual CodecCapability GetConfiguration(CodecCapability const& remote_codec_configuration) const;

  /// Return the codec capabilities exposed to remote devices.
  virtual CodecCapability GetCapability() const;

  /// Return the bitrate of the input codec capabilities.
  /// Required for the legacy offload configuration.
  /// @deprecated
  virtual int GetBitRate(CodecCapability const& codec_configuration) const;

  virtual Encoder* GetEncoderInterface() const;
  virtual Encoder* GetDecoderInterface() const;
};

class SbcCodec : Codec {
};

class AacCodec : Codec {
};

class OffloadCodec : Codec {
};

class Codecs {
public:
  SupportedCodecs();
  ~SupportedCodecs();

  Codec* Get(tA2DP_CODEC_ID id) const;

private:
   std::vector<Codec*> codecs_;
};
