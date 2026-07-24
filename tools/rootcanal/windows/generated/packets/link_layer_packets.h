// Generated from tools/rootcanal/packets/link_layer_packets.pdl for the standalone Windows build.
// /!\ Do not edit by hand

#pragma once

#include <cstdint>
#include <string>
#include <optional>
#include <utility>
#include <vector>

#include <packet_runtime.h>

#include <hci/address.h>
using namespace bluetooth::hci;

#ifndef _ASSERT_VALID
#ifdef ASSERT
#define _ASSERT_VALID ASSERT
#else
#include <cassert>
#define _ASSERT_VALID assert
#endif  // ASSERT
#endif  // !_ASSERT_VALID

namespace model::packets {
class LinkLayerPacketView;
class AclView;
class ScoView;
class LeConnectedIsochronousPduView;
class LeBroadcastIsochronousPduView;
class DisconnectView;
class InquiryView;
class BasicInquiryResponseView;
class InquiryResponseView;
class InquiryResponseWithRssiView;
class ExtendedInquiryResponseView;
class LeLegacyAdvertisingPduView;
class LeExtendedAdvertisingPduView;
class LePeriodicAdvertisingPduView;
class LeConnectView;
class LeConnectCompleteView;
class LeScanView;
class LeScanResponseView;
class PageView;
class PageResponseView;
class PageRejectView;
class ReadClockOffsetView;
class ReadClockOffsetResponseView;
class ReadRemoteSupportedFeaturesView;
class ReadRemoteSupportedFeaturesResponseView;
class ReadRemoteLmpFeaturesView;
class ReadRemoteLmpFeaturesResponseView;
class ReadRemoteExtendedFeaturesView;
class ReadRemoteExtendedFeaturesResponseView;
class ReadRemoteVersionInformationView;
class ReadRemoteVersionInformationResponseView;
class RemoteNameRequestView;
class RemoteNameRequestResponseView;
class LeEncryptConnectionView;
class LeEncryptConnectionResponseView;
class LeReadRemoteFeaturesView;
class LeReadRemoteFeaturesResponseView;
class LeConnectionParameterRequestView;
class LeConnectionParameterUpdateView;
class ScoConnectionRequestView;
class ScoConnectionResponseView;
class ScoDisconnectView;
class LmpView;
class LlcpView;
class PingRequestView;
class PingResponseView;
class RoleSwitchRequestView;
class RoleSwitchResponseView;
class LlPhyReqView;
class LlPhyRspView;
class LlPhyUpdateIndView;

enum class PacketType : uint8_t {
    UNKNOWN = 0x0,
    ACL = 0x1,
    SCO = 0x2,
    LE_CONNECTED_ISOCHRONOUS_PDU = 0x3,
    LE_BROADCAST_ISOCHRONOUS_PDU = 0x4,
    DISCONNECT = 0x5,
    INQUIRY = 0x6,
    INQUIRY_RESPONSE = 0x7,
    LE_LEGACY_ADVERTISING_PDU = 0xb,
    LE_EXTENDED_ADVERTISING_PDU = 0x37,
    LE_PERIODIC_ADVERTISING_PDU = 0x40,
    LE_CONNECT = 0xc,
    LE_CONNECT_COMPLETE = 0xd,
    LE_SCAN = 0xe,
    LE_SCAN_RESPONSE = 0xf,
    PAGE = 0x10,
    PAGE_RESPONSE = 0x11,
    PAGE_REJECT = 0x12,
    READ_CLOCK_OFFSET = 0x13,
    READ_CLOCK_OFFSET_RESPONSE = 0x14,
    READ_REMOTE_SUPPORTED_FEATURES = 0x15,
    READ_REMOTE_SUPPORTED_FEATURES_RESPONSE = 0x16,
    READ_REMOTE_LMP_FEATURES = 0x17,
    READ_REMOTE_LMP_FEATURES_RESPONSE = 0x18,
    READ_REMOTE_EXTENDED_FEATURES = 0x19,
    READ_REMOTE_EXTENDED_FEATURES_RESPONSE = 0x1a,
    READ_REMOTE_VERSION_INFORMATION = 0x1b,
    READ_REMOTE_VERSION_INFORMATION_RESPONSE = 0x1c,
    REMOTE_NAME_REQUEST = 0x1d,
    REMOTE_NAME_REQUEST_RESPONSE = 0x1e,
    LE_ENCRYPT_CONNECTION = 0x20,
    LE_ENCRYPT_CONNECTION_RESPONSE = 0x21,
    LE_READ_REMOTE_FEATURES = 0x2c,
    LE_READ_REMOTE_FEATURES_RESPONSE = 0x2d,
    LE_CONNECTION_PARAMETER_REQUEST = 0x2e,
    LE_CONNECTION_PARAMETER_UPDATE = 0x2f,
    SCO_CONNECTION_REQUEST = 0x30,
    SCO_CONNECTION_RESPONSE = 0x31,
    SCO_DISCONNECT = 0x32,
    LMP = 0x34,
    LLCP = 0x41,
    PING_REQUEST = 0x35,
    PING_RESPONSE = 0x36,
    ROLE_SWITCH_REQUEST = 0x38,
    ROLE_SWITCH_RESPONSE = 0x39,
    LL_PHY_REQ = 0x50,
    LL_PHY_RSP = 0x51,
    LL_PHY_UPDATE_IND = 0x52,
};

inline std::string PacketTypeText(PacketType tag) {
    switch (tag) {
        case PacketType::UNKNOWN: return "UNKNOWN";
        case PacketType::ACL: return "ACL";
        case PacketType::SCO: return "SCO";
        case PacketType::LE_CONNECTED_ISOCHRONOUS_PDU: return "LE_CONNECTED_ISOCHRONOUS_PDU";
        case PacketType::LE_BROADCAST_ISOCHRONOUS_PDU: return "LE_BROADCAST_ISOCHRONOUS_PDU";
        case PacketType::DISCONNECT: return "DISCONNECT";
        case PacketType::INQUIRY: return "INQUIRY";
        case PacketType::INQUIRY_RESPONSE: return "INQUIRY_RESPONSE";
        case PacketType::LE_LEGACY_ADVERTISING_PDU: return "LE_LEGACY_ADVERTISING_PDU";
        case PacketType::LE_EXTENDED_ADVERTISING_PDU: return "LE_EXTENDED_ADVERTISING_PDU";
        case PacketType::LE_PERIODIC_ADVERTISING_PDU: return "LE_PERIODIC_ADVERTISING_PDU";
        case PacketType::LE_CONNECT: return "LE_CONNECT";
        case PacketType::LE_CONNECT_COMPLETE: return "LE_CONNECT_COMPLETE";
        case PacketType::LE_SCAN: return "LE_SCAN";
        case PacketType::LE_SCAN_RESPONSE: return "LE_SCAN_RESPONSE";
        case PacketType::PAGE: return "PAGE";
        case PacketType::PAGE_RESPONSE: return "PAGE_RESPONSE";
        case PacketType::PAGE_REJECT: return "PAGE_REJECT";
        case PacketType::READ_CLOCK_OFFSET: return "READ_CLOCK_OFFSET";
        case PacketType::READ_CLOCK_OFFSET_RESPONSE: return "READ_CLOCK_OFFSET_RESPONSE";
        case PacketType::READ_REMOTE_SUPPORTED_FEATURES: return "READ_REMOTE_SUPPORTED_FEATURES";
        case PacketType::READ_REMOTE_SUPPORTED_FEATURES_RESPONSE: return "READ_REMOTE_SUPPORTED_FEATURES_RESPONSE";
        case PacketType::READ_REMOTE_LMP_FEATURES: return "READ_REMOTE_LMP_FEATURES";
        case PacketType::READ_REMOTE_LMP_FEATURES_RESPONSE: return "READ_REMOTE_LMP_FEATURES_RESPONSE";
        case PacketType::READ_REMOTE_EXTENDED_FEATURES: return "READ_REMOTE_EXTENDED_FEATURES";
        case PacketType::READ_REMOTE_EXTENDED_FEATURES_RESPONSE: return "READ_REMOTE_EXTENDED_FEATURES_RESPONSE";
        case PacketType::READ_REMOTE_VERSION_INFORMATION: return "READ_REMOTE_VERSION_INFORMATION";
        case PacketType::READ_REMOTE_VERSION_INFORMATION_RESPONSE: return "READ_REMOTE_VERSION_INFORMATION_RESPONSE";
        case PacketType::REMOTE_NAME_REQUEST: return "REMOTE_NAME_REQUEST";
        case PacketType::REMOTE_NAME_REQUEST_RESPONSE: return "REMOTE_NAME_REQUEST_RESPONSE";
        case PacketType::LE_ENCRYPT_CONNECTION: return "LE_ENCRYPT_CONNECTION";
        case PacketType::LE_ENCRYPT_CONNECTION_RESPONSE: return "LE_ENCRYPT_CONNECTION_RESPONSE";
        case PacketType::LE_READ_REMOTE_FEATURES: return "LE_READ_REMOTE_FEATURES";
        case PacketType::LE_READ_REMOTE_FEATURES_RESPONSE: return "LE_READ_REMOTE_FEATURES_RESPONSE";
        case PacketType::LE_CONNECTION_PARAMETER_REQUEST: return "LE_CONNECTION_PARAMETER_REQUEST";
        case PacketType::LE_CONNECTION_PARAMETER_UPDATE: return "LE_CONNECTION_PARAMETER_UPDATE";
        case PacketType::SCO_CONNECTION_REQUEST: return "SCO_CONNECTION_REQUEST";
        case PacketType::SCO_CONNECTION_RESPONSE: return "SCO_CONNECTION_RESPONSE";
        case PacketType::SCO_DISCONNECT: return "SCO_DISCONNECT";
        case PacketType::LMP: return "LMP";
        case PacketType::LLCP: return "LLCP";
        case PacketType::PING_REQUEST: return "PING_REQUEST";
        case PacketType::PING_RESPONSE: return "PING_RESPONSE";
        case PacketType::ROLE_SWITCH_REQUEST: return "ROLE_SWITCH_REQUEST";
        case PacketType::ROLE_SWITCH_RESPONSE: return "ROLE_SWITCH_RESPONSE";
        case PacketType::LL_PHY_REQ: return "LL_PHY_REQ";
        case PacketType::LL_PHY_RSP: return "LL_PHY_RSP";
        case PacketType::LL_PHY_UPDATE_IND: return "LL_PHY_UPDATE_IND";
        default:
            return std::string("Unknown PacketType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

class LinkLayerPacketView {
public:
    static LinkLayerPacketView Create(pdl::packet::slice const& parent) {
        return LinkLayerPacketView(parent);
    }

    PacketType GetType() const {
        _ASSERT_VALID(valid_);
        return type_;
    }
    
    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::vector<uint8_t> GetPayload() const {
        _ASSERT_VALID(valid_);
        return payload_.bytes();
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LinkLayerPacketView(pdl::packet::slice const& parent)
          : bytes_(parent) {
        valid_ = Parse(parent);
    }

    bool Parse(pdl::packet::slice const& parent) {
        // Parse packet field values.
        pdl::packet::slice span = parent;
        if (span.size() < 1) {
            return false;
        }
        type_ = PacketType(span.read_le<uint8_t, 1>());
        if (!Address::Parse(span, &source_address_)) {
            return false;
        }
        if (!Address::Parse(span, &destination_address_)) {
            return false;
        }
        payload_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    PacketType type_{PacketType::UNKNOWN};
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice payload_;

    friend class AclView;
    friend class ScoView;
    friend class LeConnectedIsochronousPduView;
    friend class LeBroadcastIsochronousPduView;
    friend class DisconnectView;
    friend class InquiryView;
    friend class BasicInquiryResponseView;
    friend class LeLegacyAdvertisingPduView;
    friend class LeExtendedAdvertisingPduView;
    friend class LePeriodicAdvertisingPduView;
    friend class LeConnectView;
    friend class LeConnectCompleteView;
    friend class LeScanView;
    friend class LeScanResponseView;
    friend class PageView;
    friend class PageResponseView;
    friend class PageRejectView;
    friend class ReadClockOffsetView;
    friend class ReadClockOffsetResponseView;
    friend class ReadRemoteSupportedFeaturesView;
    friend class ReadRemoteSupportedFeaturesResponseView;
    friend class ReadRemoteLmpFeaturesView;
    friend class ReadRemoteLmpFeaturesResponseView;
    friend class ReadRemoteExtendedFeaturesView;
    friend class ReadRemoteExtendedFeaturesResponseView;
    friend class ReadRemoteVersionInformationView;
    friend class ReadRemoteVersionInformationResponseView;
    friend class RemoteNameRequestView;
    friend class RemoteNameRequestResponseView;
    friend class LeEncryptConnectionView;
    friend class LeEncryptConnectionResponseView;
    friend class LeReadRemoteFeaturesView;
    friend class LeReadRemoteFeaturesResponseView;
    friend class LeConnectionParameterRequestView;
    friend class LeConnectionParameterUpdateView;
    friend class ScoConnectionRequestView;
    friend class ScoConnectionResponseView;
    friend class ScoDisconnectView;
    friend class LmpView;
    friend class LlcpView;
    friend class PingRequestView;
    friend class PingResponseView;
    friend class RoleSwitchRequestView;
    friend class RoleSwitchResponseView;
    friend class LlPhyReqView;
    friend class LlPhyRspView;
    friend class LlPhyUpdateIndView;
};

class LinkLayerPacketBuilder : public pdl::packet::Builder {
public:
    ~LinkLayerPacketBuilder() override = default;
    LinkLayerPacketBuilder() = default;
    LinkLayerPacketBuilder(LinkLayerPacketBuilder const&) = default;
    LinkLayerPacketBuilder(LinkLayerPacketBuilder&&) = default;
    LinkLayerPacketBuilder& operator=(LinkLayerPacketBuilder const&) = default;
        LinkLayerPacketBuilder(PacketType type, Address source_address, Address destination_address, std::vector<uint8_t> payload)
        : type_(type), source_address_(std::move(source_address)), destination_address_(std::move(destination_address)), payload_(std::move(payload)) {
    
}
    static std::unique_ptr<LinkLayerPacketBuilder> Create(PacketType type, Address source_address, Address destination_address, std::vector<uint8_t> payload) {
    return std::make_unique<LinkLayerPacketBuilder>(type, std::move(source_address), std::move(destination_address), std::move(payload));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(type_) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), payload_.begin(), payload_.end());
    }

    size_t GetSize() const override {
        return payload_.size() + 13;
    }

    
    PacketType type_{PacketType::UNKNOWN};
    Address source_address_;
    Address destination_address_;
    std::vector<uint8_t> payload_;
};

class AclView {
public:
    static AclView Create(LinkLayerPacketView const& parent) {
        return AclView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPacketBoundaryFlag() const {
        _ASSERT_VALID(valid_);
        return packet_boundary_flag_;
    }
    
    uint8_t GetBroadcastFlag() const {
        _ASSERT_VALID(valid_);
        return broadcast_flag_;
    }
    
    std::vector<uint8_t> GetData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::ACL;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit AclView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::ACL) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        packet_boundary_flag_ = span.read_le<uint8_t, 1>();
        broadcast_flag_ = span.read_le<uint8_t, 1>();
        data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t packet_boundary_flag_{0};
    uint8_t broadcast_flag_{0};
    pdl::packet::slice data_;

    
};

class AclBuilder : public LinkLayerPacketBuilder {
public:
    ~AclBuilder() override = default;
    AclBuilder() = default;
    AclBuilder(AclBuilder const&) = default;
    AclBuilder(AclBuilder&&) = default;
    AclBuilder& operator=(AclBuilder const&) = default;
        AclBuilder(Address source_address, Address destination_address, uint8_t packet_boundary_flag, uint8_t broadcast_flag, std::vector<uint8_t> data)
        : LinkLayerPacketBuilder(PacketType::ACL, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), packet_boundary_flag_(packet_boundary_flag), broadcast_flag_(broadcast_flag), data_(std::move(data)) {
    
}
    static std::unique_ptr<AclBuilder> Create(Address source_address, Address destination_address, uint8_t packet_boundary_flag, uint8_t broadcast_flag, std::vector<uint8_t> data) {
    return std::make_unique<AclBuilder>(std::move(source_address), std::move(destination_address), packet_boundary_flag, broadcast_flag, std::move(data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::ACL) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(packet_boundary_flag_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(broadcast_flag_ & 0xff) << 0));
        output.insert(output.end(), data_.begin(), data_.end());
    }

    size_t GetSize() const override {
        return GetDataSize() + 15;
    }

    size_t GetDataSize() const {
        return data_.size() * 1;
    }
    
    uint8_t packet_boundary_flag_{0};
    uint8_t broadcast_flag_{0};
    std::vector<uint8_t> data_;
};

class ScoView {
public:
    static ScoView Create(LinkLayerPacketView const& parent) {
        return ScoView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::vector<uint8_t> GetPayload() const {
        _ASSERT_VALID(valid_);
        return payload_.bytes();
    }
    
    PacketType GetType() const {
        return PacketType::SCO;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ScoView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::SCO) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        payload_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice payload_;

    
};

class ScoBuilder : public LinkLayerPacketBuilder {
public:
    ~ScoBuilder() override = default;
    ScoBuilder() = default;
    ScoBuilder(ScoBuilder const&) = default;
    ScoBuilder(ScoBuilder&&) = default;
    ScoBuilder& operator=(ScoBuilder const&) = default;
        ScoBuilder(Address source_address, Address destination_address, std::vector<uint8_t> payload)
        : LinkLayerPacketBuilder(PacketType::SCO, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    payload_ = std::move(payload);
}
    static std::unique_ptr<ScoBuilder> Create(Address source_address, Address destination_address, std::vector<uint8_t> payload) {
    return std::make_unique<ScoBuilder>(std::move(source_address), std::move(destination_address), std::move(payload));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::SCO) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), payload_.begin(), payload_.end());
    }

    size_t GetSize() const override {
        return payload_.size() + 13;
    }

    
    
};

class LeConnectedIsochronousPduView {
public:
    static LeConnectedIsochronousPduView Create(LinkLayerPacketView const& parent) {
        return LeConnectedIsochronousPduView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetCigId() const {
        _ASSERT_VALID(valid_);
        return cig_id_;
    }
    
    uint8_t GetCisId() const {
        _ASSERT_VALID(valid_);
        return cis_id_;
    }
    
    uint16_t GetSequenceNumber() const {
        _ASSERT_VALID(valid_);
        return sequence_number_;
    }
    
    std::vector<uint8_t> GetData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_CONNECTED_ISOCHRONOUS_PDU;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeConnectedIsochronousPduView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_CONNECTED_ISOCHRONOUS_PDU) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 4) {
            return false;
        }
        cig_id_ = span.read_le<uint8_t, 1>();
        cis_id_ = span.read_le<uint8_t, 1>();
        sequence_number_ = span.read_le<uint16_t, 2>();
        data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t cig_id_{0};
    uint8_t cis_id_{0};
    uint16_t sequence_number_{0};
    pdl::packet::slice data_;

    
};

class LeConnectedIsochronousPduBuilder : public LinkLayerPacketBuilder {
public:
    ~LeConnectedIsochronousPduBuilder() override = default;
    LeConnectedIsochronousPduBuilder() = default;
    LeConnectedIsochronousPduBuilder(LeConnectedIsochronousPduBuilder const&) = default;
    LeConnectedIsochronousPduBuilder(LeConnectedIsochronousPduBuilder&&) = default;
    LeConnectedIsochronousPduBuilder& operator=(LeConnectedIsochronousPduBuilder const&) = default;
        LeConnectedIsochronousPduBuilder(Address source_address, Address destination_address, uint8_t cig_id, uint8_t cis_id, uint16_t sequence_number, std::vector<uint8_t> data)
        : LinkLayerPacketBuilder(PacketType::LE_CONNECTED_ISOCHRONOUS_PDU, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), cig_id_(cig_id), cis_id_(cis_id), sequence_number_(sequence_number), data_(std::move(data)) {
    
}
    static std::unique_ptr<LeConnectedIsochronousPduBuilder> Create(Address source_address, Address destination_address, uint8_t cig_id, uint8_t cis_id, uint16_t sequence_number, std::vector<uint8_t> data) {
    return std::make_unique<LeConnectedIsochronousPduBuilder>(std::move(source_address), std::move(destination_address), cig_id, cis_id, sequence_number, std::move(data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_CONNECTED_ISOCHRONOUS_PDU) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(cig_id_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(cis_id_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(sequence_number_ & 0xffff) << 0));
        output.insert(output.end(), data_.begin(), data_.end());
    }

    size_t GetSize() const override {
        return GetDataSize() + 17;
    }

    size_t GetDataSize() const {
        return data_.size() * 1;
    }
    
    uint8_t cig_id_{0};
    uint8_t cis_id_{0};
    uint16_t sequence_number_{0};
    std::vector<uint8_t> data_;
};

class LeBroadcastIsochronousPduView {
public:
    static LeBroadcastIsochronousPduView Create(LinkLayerPacketView const& parent) {
        return LeBroadcastIsochronousPduView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_BROADCAST_ISOCHRONOUS_PDU;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeBroadcastIsochronousPduView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_BROADCAST_ISOCHRONOUS_PDU) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class LeBroadcastIsochronousPduBuilder : public LinkLayerPacketBuilder {
public:
    ~LeBroadcastIsochronousPduBuilder() override = default;
    LeBroadcastIsochronousPduBuilder() = default;
    LeBroadcastIsochronousPduBuilder(LeBroadcastIsochronousPduBuilder const&) = default;
    LeBroadcastIsochronousPduBuilder(LeBroadcastIsochronousPduBuilder&&) = default;
    LeBroadcastIsochronousPduBuilder& operator=(LeBroadcastIsochronousPduBuilder const&) = default;
        LeBroadcastIsochronousPduBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::LE_BROADCAST_ISOCHRONOUS_PDU, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<LeBroadcastIsochronousPduBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<LeBroadcastIsochronousPduBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_BROADCAST_ISOCHRONOUS_PDU) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class DisconnectView {
public:
    static DisconnectView Create(LinkLayerPacketView const& parent) {
        return DisconnectView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetReason() const {
        _ASSERT_VALID(valid_);
        return reason_;
    }
    
    PacketType GetType() const {
        return PacketType::DISCONNECT;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit DisconnectView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::DISCONNECT) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        reason_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t reason_{0};

    
};

class DisconnectBuilder : public LinkLayerPacketBuilder {
public:
    ~DisconnectBuilder() override = default;
    DisconnectBuilder() = default;
    DisconnectBuilder(DisconnectBuilder const&) = default;
    DisconnectBuilder(DisconnectBuilder&&) = default;
    DisconnectBuilder& operator=(DisconnectBuilder const&) = default;
        DisconnectBuilder(Address source_address, Address destination_address, uint8_t reason)
        : LinkLayerPacketBuilder(PacketType::DISCONNECT, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), reason_(reason) {
    
}
    static std::unique_ptr<DisconnectBuilder> Create(Address source_address, Address destination_address, uint8_t reason) {
    return std::make_unique<DisconnectBuilder>(std::move(source_address), std::move(destination_address), reason);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::DISCONNECT) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(reason_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t reason_{0};
};

enum class InquiryState : uint8_t {
    STANDBY = 0x0,
    INQUIRY = 0x1,
};

inline std::string InquiryStateText(InquiryState tag) {
    switch (tag) {
        case InquiryState::STANDBY: return "STANDBY";
        case InquiryState::INQUIRY: return "INQUIRY";
        default:
            return std::string("Unknown InquiryState: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

enum class InquiryType : uint8_t {
    STANDARD = 0x0,
    RSSI = 0x1,
    EXTENDED = 0x2,
};

inline std::string InquiryTypeText(InquiryType tag) {
    switch (tag) {
        case InquiryType::STANDARD: return "STANDARD";
        case InquiryType::RSSI: return "RSSI";
        case InquiryType::EXTENDED: return "EXTENDED";
        default:
            return std::string("Unknown InquiryType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

class InquiryView {
public:
    static InquiryView Create(LinkLayerPacketView const& parent) {
        return InquiryView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    InquiryType GetInquiryType() const {
        _ASSERT_VALID(valid_);
        return inquiry_type_;
    }
    
    uint8_t GetLap() const {
        _ASSERT_VALID(valid_);
        return lap_;
    }
    
    PacketType GetType() const {
        return PacketType::INQUIRY;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit InquiryView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::INQUIRY) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        inquiry_type_ = InquiryType(span.read_le<uint8_t, 1>());
        lap_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    InquiryType inquiry_type_{InquiryType::STANDARD};
    uint8_t lap_{0};

    
};

class InquiryBuilder : public LinkLayerPacketBuilder {
public:
    ~InquiryBuilder() override = default;
    InquiryBuilder() = default;
    InquiryBuilder(InquiryBuilder const&) = default;
    InquiryBuilder(InquiryBuilder&&) = default;
    InquiryBuilder& operator=(InquiryBuilder const&) = default;
        InquiryBuilder(Address source_address, Address destination_address, InquiryType inquiry_type, uint8_t lap)
        : LinkLayerPacketBuilder(PacketType::INQUIRY, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), inquiry_type_(inquiry_type), lap_(lap) {
    
}
    static std::unique_ptr<InquiryBuilder> Create(Address source_address, Address destination_address, InquiryType inquiry_type, uint8_t lap) {
    return std::make_unique<InquiryBuilder>(std::move(source_address), std::move(destination_address), inquiry_type, lap);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::INQUIRY) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(inquiry_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(lap_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 15;
    }

    
    InquiryType inquiry_type_{InquiryType::STANDARD};
    uint8_t lap_{0};
};

class BasicInquiryResponseView {
public:
    static BasicInquiryResponseView Create(LinkLayerPacketView const& parent) {
        return BasicInquiryResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    InquiryType GetInquiryType() const {
        _ASSERT_VALID(valid_);
        return inquiry_type_;
    }
    
    uint8_t GetPageScanRepetitionMode() const {
        _ASSERT_VALID(valid_);
        return page_scan_repetition_mode_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    uint16_t GetClockOffset() const {
        _ASSERT_VALID(valid_);
        return clock_offset_;
    }
    
    std::vector<uint8_t> GetPayload() const {
        _ASSERT_VALID(valid_);
        return payload_.bytes();
    }
    
    PacketType GetType() const {
        return PacketType::INQUIRY_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit BasicInquiryResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::INQUIRY_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 7) {
            return false;
        }
        inquiry_type_ = InquiryType(span.read_le<uint8_t, 1>());
        page_scan_repetition_mode_ = span.read_le<uint8_t, 1>();
        class_of_device_ = span.read_le<uint32_t, 3>();
        uint16_t chunk0 = span.read_le<uint16_t, 2>();
        clock_offset_ = (chunk0 >> 0) & 0x7fff;
        payload_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    InquiryType inquiry_type_{InquiryType::STANDARD};
    uint8_t page_scan_repetition_mode_{0};
    uint32_t class_of_device_{0};
    uint16_t clock_offset_{0};
    pdl::packet::slice payload_;

    friend class InquiryResponseView;
    friend class InquiryResponseWithRssiView;
    friend class ExtendedInquiryResponseView;
};

class BasicInquiryResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~BasicInquiryResponseBuilder() override = default;
    BasicInquiryResponseBuilder() = default;
    BasicInquiryResponseBuilder(BasicInquiryResponseBuilder const&) = default;
    BasicInquiryResponseBuilder(BasicInquiryResponseBuilder&&) = default;
    BasicInquiryResponseBuilder& operator=(BasicInquiryResponseBuilder const&) = default;
        BasicInquiryResponseBuilder(Address source_address, Address destination_address, InquiryType inquiry_type, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, std::vector<uint8_t> payload)
        : LinkLayerPacketBuilder(PacketType::INQUIRY_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), inquiry_type_(inquiry_type), page_scan_repetition_mode_(page_scan_repetition_mode), class_of_device_(class_of_device), clock_offset_(clock_offset) {
    payload_ = std::move(payload);
}
    static std::unique_ptr<BasicInquiryResponseBuilder> Create(Address source_address, Address destination_address, InquiryType inquiry_type, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, std::vector<uint8_t> payload) {
    return std::make_unique<BasicInquiryResponseBuilder>(std::move(source_address), std::move(destination_address), inquiry_type, page_scan_repetition_mode, class_of_device, clock_offset, std::move(payload));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::INQUIRY_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(inquiry_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_scan_repetition_mode_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(clock_offset_ & 0x7fff) << 0));
        output.insert(output.end(), payload_.begin(), payload_.end());
    }

    size_t GetSize() const override {
        return payload_.size() + 20;
    }

    
    InquiryType inquiry_type_{InquiryType::STANDARD};
    uint8_t page_scan_repetition_mode_{0};
    uint32_t class_of_device_{0};
    uint16_t clock_offset_{0};
};

class InquiryResponseView {
public:
    static InquiryResponseView Create(BasicInquiryResponseView const& parent) {
        return InquiryResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPageScanRepetitionMode() const {
        _ASSERT_VALID(valid_);
        return page_scan_repetition_mode_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    uint16_t GetClockOffset() const {
        _ASSERT_VALID(valid_);
        return clock_offset_;
    }
    
    PacketType GetType() const {
        return PacketType::INQUIRY_RESPONSE;
    }
    
    InquiryType GetInquiryType() const {
        return InquiryType::STANDARD;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit InquiryResponseView(BasicInquiryResponseView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(BasicInquiryResponseView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        page_scan_repetition_mode_ = parent.page_scan_repetition_mode_;
        class_of_device_ = parent.class_of_device_;
        clock_offset_ = parent.clock_offset_;
        
        if (parent.inquiry_type_ != InquiryType::STANDARD) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t page_scan_repetition_mode_{0};
    uint32_t class_of_device_{0};
    uint16_t clock_offset_{0};

    
};

class InquiryResponseBuilder : public BasicInquiryResponseBuilder {
public:
    ~InquiryResponseBuilder() override = default;
    InquiryResponseBuilder() = default;
    InquiryResponseBuilder(InquiryResponseBuilder const&) = default;
    InquiryResponseBuilder(InquiryResponseBuilder&&) = default;
    InquiryResponseBuilder& operator=(InquiryResponseBuilder const&) = default;
        InquiryResponseBuilder(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset)
        : BasicInquiryResponseBuilder(std::move(source_address), std::move(destination_address), InquiryType::STANDARD, page_scan_repetition_mode, class_of_device, clock_offset, std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<InquiryResponseBuilder> Create(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset) {
    return std::make_unique<InquiryResponseBuilder>(std::move(source_address), std::move(destination_address), page_scan_repetition_mode, class_of_device, clock_offset);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::INQUIRY_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(InquiryType::STANDARD) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_scan_repetition_mode_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(clock_offset_ & 0x7fff) << 0));
    }

    size_t GetSize() const override {
        return 20;
    }

    
    
};

class InquiryResponseWithRssiView {
public:
    static InquiryResponseWithRssiView Create(BasicInquiryResponseView const& parent) {
        return InquiryResponseWithRssiView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPageScanRepetitionMode() const {
        _ASSERT_VALID(valid_);
        return page_scan_repetition_mode_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    uint16_t GetClockOffset() const {
        _ASSERT_VALID(valid_);
        return clock_offset_;
    }
    
    uint8_t GetRssi() const {
        _ASSERT_VALID(valid_);
        return rssi_;
    }
    
    PacketType GetType() const {
        return PacketType::INQUIRY_RESPONSE;
    }
    
    InquiryType GetInquiryType() const {
        return InquiryType::RSSI;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit InquiryResponseWithRssiView(BasicInquiryResponseView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(BasicInquiryResponseView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        page_scan_repetition_mode_ = parent.page_scan_repetition_mode_;
        class_of_device_ = parent.class_of_device_;
        clock_offset_ = parent.clock_offset_;
        
        if (parent.inquiry_type_ != InquiryType::RSSI) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        rssi_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t page_scan_repetition_mode_{0};
    uint32_t class_of_device_{0};
    uint16_t clock_offset_{0};
    uint8_t rssi_{0};

    
};

class InquiryResponseWithRssiBuilder : public BasicInquiryResponseBuilder {
public:
    ~InquiryResponseWithRssiBuilder() override = default;
    InquiryResponseWithRssiBuilder() = default;
    InquiryResponseWithRssiBuilder(InquiryResponseWithRssiBuilder const&) = default;
    InquiryResponseWithRssiBuilder(InquiryResponseWithRssiBuilder&&) = default;
    InquiryResponseWithRssiBuilder& operator=(InquiryResponseWithRssiBuilder const&) = default;
        InquiryResponseWithRssiBuilder(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, uint8_t rssi)
        : BasicInquiryResponseBuilder(std::move(source_address), std::move(destination_address), InquiryType::RSSI, page_scan_repetition_mode, class_of_device, clock_offset, std::vector<uint8_t>{}), rssi_(rssi) {
    
}
    static std::unique_ptr<InquiryResponseWithRssiBuilder> Create(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, uint8_t rssi) {
    return std::make_unique<InquiryResponseWithRssiBuilder>(std::move(source_address), std::move(destination_address), page_scan_repetition_mode, class_of_device, clock_offset, rssi);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::INQUIRY_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(InquiryType::RSSI) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_scan_repetition_mode_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(clock_offset_ & 0x7fff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(rssi_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    uint8_t rssi_{0};
};

class ExtendedInquiryResponseView {
public:
    static ExtendedInquiryResponseView Create(BasicInquiryResponseView const& parent) {
        return ExtendedInquiryResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPageScanRepetitionMode() const {
        _ASSERT_VALID(valid_);
        return page_scan_repetition_mode_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    uint16_t GetClockOffset() const {
        _ASSERT_VALID(valid_);
        return clock_offset_;
    }
    
    uint8_t GetRssi() const {
        _ASSERT_VALID(valid_);
        return rssi_;
    }
    
    std::array<uint8_t, 240> GetExtendedInquiryResponse() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = extended_inquiry_response_;
        std::array<uint8_t, 240> elements;
        for (int n = 0; n < 240; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::INQUIRY_RESPONSE;
    }
    
    InquiryType GetInquiryType() const {
        return InquiryType::EXTENDED;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ExtendedInquiryResponseView(BasicInquiryResponseView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(BasicInquiryResponseView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        page_scan_repetition_mode_ = parent.page_scan_repetition_mode_;
        class_of_device_ = parent.class_of_device_;
        clock_offset_ = parent.clock_offset_;
        
        if (parent.inquiry_type_ != InquiryType::EXTENDED) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        rssi_ = span.read_le<uint8_t, 1>();
        if (span.size() < 240 * 1) {
            return false;
        }
        extended_inquiry_response_ = span.subrange(0, 240 * 1);
        span.skip(240 * 1);
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t page_scan_repetition_mode_{0};
    uint32_t class_of_device_{0};
    uint16_t clock_offset_{0};
    uint8_t rssi_{0};
    pdl::packet::slice extended_inquiry_response_;

    
};

class ExtendedInquiryResponseBuilder : public BasicInquiryResponseBuilder {
public:
    ~ExtendedInquiryResponseBuilder() override = default;
    ExtendedInquiryResponseBuilder() = default;
    ExtendedInquiryResponseBuilder(ExtendedInquiryResponseBuilder const&) = default;
    ExtendedInquiryResponseBuilder(ExtendedInquiryResponseBuilder&&) = default;
    ExtendedInquiryResponseBuilder& operator=(ExtendedInquiryResponseBuilder const&) = default;
        ExtendedInquiryResponseBuilder(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, uint8_t rssi, std::array<uint8_t, 240> extended_inquiry_response)
        : BasicInquiryResponseBuilder(std::move(source_address), std::move(destination_address), InquiryType::EXTENDED, page_scan_repetition_mode, class_of_device, clock_offset, std::vector<uint8_t>{}), rssi_(rssi), extended_inquiry_response_(std::move(extended_inquiry_response)) {
    
}
    static std::unique_ptr<ExtendedInquiryResponseBuilder> Create(Address source_address, Address destination_address, uint8_t page_scan_repetition_mode, uint32_t class_of_device, uint16_t clock_offset, uint8_t rssi, std::array<uint8_t, 240> extended_inquiry_response) {
    return std::make_unique<ExtendedInquiryResponseBuilder>(std::move(source_address), std::move(destination_address), page_scan_repetition_mode, class_of_device, clock_offset, rssi, std::move(extended_inquiry_response));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::INQUIRY_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(InquiryType::EXTENDED) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_scan_repetition_mode_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(clock_offset_ & 0x7fff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(rssi_ & 0xff) << 0));
        output.insert(output.end(), extended_inquiry_response_.begin(), extended_inquiry_response_.end());
    }

    size_t GetSize() const override {
        return 261;
    }

    size_t GetExtendedInquiryResponseSize() const {
        return 240;
    }
    
    uint8_t rssi_{0};
    std::array<uint8_t, 240> extended_inquiry_response_;
};

enum class AddressType : uint8_t {
    PUBLIC = 0x0,
    RANDOM = 0x1,
    PUBLIC_IDENTITY = 0x2,
    RANDOM_IDENTITY = 0x3,
};

inline std::string AddressTypeText(AddressType tag) {
    switch (tag) {
        case AddressType::PUBLIC: return "PUBLIC";
        case AddressType::RANDOM: return "RANDOM";
        case AddressType::PUBLIC_IDENTITY: return "PUBLIC_IDENTITY";
        case AddressType::RANDOM_IDENTITY: return "RANDOM_IDENTITY";
        default:
            return std::string("Unknown AddressType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

enum class LegacyAdvertisingType : uint8_t {
    ADV_IND = 0x0,
    ADV_DIRECT_IND = 0x1,
    ADV_SCAN_IND = 0x2,
    ADV_NONCONN_IND = 0x3,
};

inline std::string LegacyAdvertisingTypeText(LegacyAdvertisingType tag) {
    switch (tag) {
        case LegacyAdvertisingType::ADV_IND: return "ADV_IND";
        case LegacyAdvertisingType::ADV_DIRECT_IND: return "ADV_DIRECT_IND";
        case LegacyAdvertisingType::ADV_SCAN_IND: return "ADV_SCAN_IND";
        case LegacyAdvertisingType::ADV_NONCONN_IND: return "ADV_NONCONN_IND";
        default:
            return std::string("Unknown LegacyAdvertisingType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

class LeLegacyAdvertisingPduView {
public:
    static LeLegacyAdvertisingPduView Create(LinkLayerPacketView const& parent) {
        return LeLegacyAdvertisingPduView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    AddressType GetTargetAddressType() const {
        _ASSERT_VALID(valid_);
        return target_address_type_;
    }
    
    LegacyAdvertisingType GetAdvertisingType() const {
        _ASSERT_VALID(valid_);
        return advertising_type_;
    }
    
    std::vector<uint8_t> GetAdvertisingData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = advertising_data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_LEGACY_ADVERTISING_PDU;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeLegacyAdvertisingPduView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_LEGACY_ADVERTISING_PDU) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 3) {
            return false;
        }
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        target_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        advertising_type_ = LegacyAdvertisingType(span.read_le<uint8_t, 1>());
        advertising_data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType advertising_address_type_{AddressType::PUBLIC};
    AddressType target_address_type_{AddressType::PUBLIC};
    LegacyAdvertisingType advertising_type_{LegacyAdvertisingType::ADV_IND};
    pdl::packet::slice advertising_data_;

    
};

class LeLegacyAdvertisingPduBuilder : public LinkLayerPacketBuilder {
public:
    ~LeLegacyAdvertisingPduBuilder() override = default;
    LeLegacyAdvertisingPduBuilder() = default;
    LeLegacyAdvertisingPduBuilder(LeLegacyAdvertisingPduBuilder const&) = default;
    LeLegacyAdvertisingPduBuilder(LeLegacyAdvertisingPduBuilder&&) = default;
    LeLegacyAdvertisingPduBuilder& operator=(LeLegacyAdvertisingPduBuilder const&) = default;
        LeLegacyAdvertisingPduBuilder(Address source_address, Address destination_address, AddressType advertising_address_type, AddressType target_address_type, LegacyAdvertisingType advertising_type, std::vector<uint8_t> advertising_data)
        : LinkLayerPacketBuilder(PacketType::LE_LEGACY_ADVERTISING_PDU, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), advertising_address_type_(advertising_address_type), target_address_type_(target_address_type), advertising_type_(advertising_type), advertising_data_(std::move(advertising_data)) {
    
}
    static std::unique_ptr<LeLegacyAdvertisingPduBuilder> Create(Address source_address, Address destination_address, AddressType advertising_address_type, AddressType target_address_type, LegacyAdvertisingType advertising_type, std::vector<uint8_t> advertising_data) {
    return std::make_unique<LeLegacyAdvertisingPduBuilder>(std::move(source_address), std::move(destination_address), advertising_address_type, target_address_type, advertising_type, std::move(advertising_data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_LEGACY_ADVERTISING_PDU) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(target_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_type_) << 0));
        output.insert(output.end(), advertising_data_.begin(), advertising_data_.end());
    }

    size_t GetSize() const override {
        return GetAdvertisingDataSize() + 16;
    }

    size_t GetAdvertisingDataSize() const {
        return advertising_data_.size() * 1;
    }
    
    AddressType advertising_address_type_{AddressType::PUBLIC};
    AddressType target_address_type_{AddressType::PUBLIC};
    LegacyAdvertisingType advertising_type_{LegacyAdvertisingType::ADV_IND};
    std::vector<uint8_t> advertising_data_;
};

enum class PhyType : uint8_t {
    NO_PACKETS = 0x0,
    LE_1M = 0x1,
    LE_2M = 0x2,
    LE_CODED_S8 = 0x3,
    LE_CODED_S2 = 0x4,
};

inline std::string PhyTypeText(PhyType tag) {
    switch (tag) {
        case PhyType::NO_PACKETS: return "NO_PACKETS";
        case PhyType::LE_1M: return "LE_1M";
        case PhyType::LE_2M: return "LE_2M";
        case PhyType::LE_CODED_S8: return "LE_CODED_S8";
        case PhyType::LE_CODED_S2: return "LE_CODED_S2";
        default:
            return std::string("Unknown PhyType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

class LeExtendedAdvertisingPduView {
public:
    static LeExtendedAdvertisingPduView Create(LinkLayerPacketView const& parent) {
        return LeExtendedAdvertisingPduView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    AddressType GetTargetAddressType() const {
        _ASSERT_VALID(valid_);
        return target_address_type_;
    }
    
    uint8_t GetConnectable() const {
        _ASSERT_VALID(valid_);
        return connectable_;
    }
    
    uint8_t GetScannable() const {
        _ASSERT_VALID(valid_);
        return scannable_;
    }
    
    uint8_t GetDirected() const {
        _ASSERT_VALID(valid_);
        return directed_;
    }
    
    uint8_t GetSid() const {
        _ASSERT_VALID(valid_);
        return sid_;
    }
    
    uint8_t GetTxPower() const {
        _ASSERT_VALID(valid_);
        return tx_power_;
    }
    
    PhyType GetPrimaryPhy() const {
        _ASSERT_VALID(valid_);
        return primary_phy_;
    }
    
    PhyType GetSecondaryPhy() const {
        _ASSERT_VALID(valid_);
        return secondary_phy_;
    }
    
    uint16_t GetPeriodicAdvertisingInterval() const {
        _ASSERT_VALID(valid_);
        return periodic_advertising_interval_;
    }
    
    std::vector<uint8_t> GetAdvertisingData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = advertising_data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_EXTENDED_ADVERTISING_PDU;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeExtendedAdvertisingPduView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_EXTENDED_ADVERTISING_PDU) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 9) {
            return false;
        }
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        target_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        uint8_t chunk0 = span.read_le<uint8_t, 1>();
        connectable_ = (chunk0 >> 0) & 0x1;
        scannable_ = (chunk0 >> 1) & 0x1;
        directed_ = (chunk0 >> 2) & 0x1;
        sid_ = span.read_le<uint8_t, 1>();
        tx_power_ = span.read_le<uint8_t, 1>();
        primary_phy_ = PhyType(span.read_le<uint8_t, 1>());
        secondary_phy_ = PhyType(span.read_le<uint8_t, 1>());
        periodic_advertising_interval_ = span.read_le<uint16_t, 2>();
        advertising_data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType advertising_address_type_{AddressType::PUBLIC};
    AddressType target_address_type_{AddressType::PUBLIC};
    uint8_t connectable_{0};
    uint8_t scannable_{0};
    uint8_t directed_{0};
    uint8_t sid_{0};
    uint8_t tx_power_{0};
    PhyType primary_phy_{PhyType::NO_PACKETS};
    PhyType secondary_phy_{PhyType::NO_PACKETS};
    uint16_t periodic_advertising_interval_{0};
    pdl::packet::slice advertising_data_;

    
};

class LeExtendedAdvertisingPduBuilder : public LinkLayerPacketBuilder {
public:
    ~LeExtendedAdvertisingPduBuilder() override = default;
    LeExtendedAdvertisingPduBuilder() = default;
    LeExtendedAdvertisingPduBuilder(LeExtendedAdvertisingPduBuilder const&) = default;
    LeExtendedAdvertisingPduBuilder(LeExtendedAdvertisingPduBuilder&&) = default;
    LeExtendedAdvertisingPduBuilder& operator=(LeExtendedAdvertisingPduBuilder const&) = default;
        LeExtendedAdvertisingPduBuilder(Address source_address, Address destination_address, AddressType advertising_address_type, AddressType target_address_type, uint8_t connectable, uint8_t scannable, uint8_t directed, uint8_t sid, uint8_t tx_power, PhyType primary_phy, PhyType secondary_phy, uint16_t periodic_advertising_interval, std::vector<uint8_t> advertising_data)
        : LinkLayerPacketBuilder(PacketType::LE_EXTENDED_ADVERTISING_PDU, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), advertising_address_type_(advertising_address_type), target_address_type_(target_address_type), connectable_(connectable), scannable_(scannable), directed_(directed), sid_(sid), tx_power_(tx_power), primary_phy_(primary_phy), secondary_phy_(secondary_phy), periodic_advertising_interval_(periodic_advertising_interval), advertising_data_(std::move(advertising_data)) {
    
}
    static std::unique_ptr<LeExtendedAdvertisingPduBuilder> Create(Address source_address, Address destination_address, AddressType advertising_address_type, AddressType target_address_type, uint8_t connectable, uint8_t scannable, uint8_t directed, uint8_t sid, uint8_t tx_power, PhyType primary_phy, PhyType secondary_phy, uint16_t periodic_advertising_interval, std::vector<uint8_t> advertising_data) {
    return std::make_unique<LeExtendedAdvertisingPduBuilder>(std::move(source_address), std::move(destination_address), advertising_address_type, target_address_type, connectable, scannable, directed, sid, tx_power, primary_phy, secondary_phy, periodic_advertising_interval, std::move(advertising_data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_EXTENDED_ADVERTISING_PDU) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(target_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(connectable_ & 0x1) << 0) | (static_cast<uint8_t>(scannable_ & 0x1) << 1) | (static_cast<uint8_t>(directed_ & 0x1) << 2));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(sid_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(tx_power_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(primary_phy_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(secondary_phy_) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(periodic_advertising_interval_ & 0xffff) << 0));
        output.insert(output.end(), advertising_data_.begin(), advertising_data_.end());
    }

    size_t GetSize() const override {
        return GetAdvertisingDataSize() + 22;
    }

    size_t GetAdvertisingDataSize() const {
        return advertising_data_.size() * 1;
    }
    
    AddressType advertising_address_type_{AddressType::PUBLIC};
    AddressType target_address_type_{AddressType::PUBLIC};
    uint8_t connectable_{0};
    uint8_t scannable_{0};
    uint8_t directed_{0};
    uint8_t sid_{0};
    uint8_t tx_power_{0};
    PhyType primary_phy_{PhyType::NO_PACKETS};
    PhyType secondary_phy_{PhyType::NO_PACKETS};
    uint16_t periodic_advertising_interval_{0};
    std::vector<uint8_t> advertising_data_;
};

class LePeriodicAdvertisingPduView {
public:
    static LePeriodicAdvertisingPduView Create(LinkLayerPacketView const& parent) {
        return LePeriodicAdvertisingPduView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    uint8_t GetSid() const {
        _ASSERT_VALID(valid_);
        return sid_;
    }
    
    uint8_t GetTxPower() const {
        _ASSERT_VALID(valid_);
        return tx_power_;
    }
    
    uint16_t GetAdvertisingInterval() const {
        _ASSERT_VALID(valid_);
        return advertising_interval_;
    }
    
    std::vector<uint8_t> GetAdvertisingData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = advertising_data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_PERIODIC_ADVERTISING_PDU;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LePeriodicAdvertisingPduView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_PERIODIC_ADVERTISING_PDU) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 5) {
            return false;
        }
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        sid_ = span.read_le<uint8_t, 1>();
        tx_power_ = span.read_le<uint8_t, 1>();
        advertising_interval_ = span.read_le<uint16_t, 2>();
        advertising_data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint8_t sid_{0};
    uint8_t tx_power_{0};
    uint16_t advertising_interval_{0};
    pdl::packet::slice advertising_data_;

    
};

class LePeriodicAdvertisingPduBuilder : public LinkLayerPacketBuilder {
public:
    ~LePeriodicAdvertisingPduBuilder() override = default;
    LePeriodicAdvertisingPduBuilder() = default;
    LePeriodicAdvertisingPduBuilder(LePeriodicAdvertisingPduBuilder const&) = default;
    LePeriodicAdvertisingPduBuilder(LePeriodicAdvertisingPduBuilder&&) = default;
    LePeriodicAdvertisingPduBuilder& operator=(LePeriodicAdvertisingPduBuilder const&) = default;
        LePeriodicAdvertisingPduBuilder(Address source_address, Address destination_address, AddressType advertising_address_type, uint8_t sid, uint8_t tx_power, uint16_t advertising_interval, std::vector<uint8_t> advertising_data)
        : LinkLayerPacketBuilder(PacketType::LE_PERIODIC_ADVERTISING_PDU, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), advertising_address_type_(advertising_address_type), sid_(sid), tx_power_(tx_power), advertising_interval_(advertising_interval), advertising_data_(std::move(advertising_data)) {
    
}
    static std::unique_ptr<LePeriodicAdvertisingPduBuilder> Create(Address source_address, Address destination_address, AddressType advertising_address_type, uint8_t sid, uint8_t tx_power, uint16_t advertising_interval, std::vector<uint8_t> advertising_data) {
    return std::make_unique<LePeriodicAdvertisingPduBuilder>(std::move(source_address), std::move(destination_address), advertising_address_type, sid, tx_power, advertising_interval, std::move(advertising_data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_PERIODIC_ADVERTISING_PDU) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(sid_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(tx_power_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(advertising_interval_ & 0xffff) << 0));
        output.insert(output.end(), advertising_data_.begin(), advertising_data_.end());
    }

    size_t GetSize() const override {
        return GetAdvertisingDataSize() + 18;
    }

    size_t GetAdvertisingDataSize() const {
        return advertising_data_.size() * 1;
    }
    
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint8_t sid_{0};
    uint8_t tx_power_{0};
    uint16_t advertising_interval_{0};
    std::vector<uint8_t> advertising_data_;
};

class LeConnectView {
public:
    static LeConnectView Create(LinkLayerPacketView const& parent) {
        return LeConnectView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetInitiatingAddressType() const {
        _ASSERT_VALID(valid_);
        return initiating_address_type_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    uint16_t GetConnInterval() const {
        _ASSERT_VALID(valid_);
        return conn_interval_;
    }
    
    uint16_t GetConnPeripheralLatency() const {
        _ASSERT_VALID(valid_);
        return conn_peripheral_latency_;
    }
    
    uint16_t GetConnSupervisionTimeout() const {
        _ASSERT_VALID(valid_);
        return conn_supervision_timeout_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_CONNECT;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeConnectView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_CONNECT) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8) {
            return false;
        }
        initiating_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        conn_interval_ = span.read_le<uint16_t, 2>();
        conn_peripheral_latency_ = span.read_le<uint16_t, 2>();
        conn_supervision_timeout_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType initiating_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint16_t conn_interval_{0};
    uint16_t conn_peripheral_latency_{0};
    uint16_t conn_supervision_timeout_{0};

    
};

class LeConnectBuilder : public LinkLayerPacketBuilder {
public:
    ~LeConnectBuilder() override = default;
    LeConnectBuilder() = default;
    LeConnectBuilder(LeConnectBuilder const&) = default;
    LeConnectBuilder(LeConnectBuilder&&) = default;
    LeConnectBuilder& operator=(LeConnectBuilder const&) = default;
        LeConnectBuilder(Address source_address, Address destination_address, AddressType initiating_address_type, AddressType advertising_address_type, uint16_t conn_interval, uint16_t conn_peripheral_latency, uint16_t conn_supervision_timeout)
        : LinkLayerPacketBuilder(PacketType::LE_CONNECT, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), initiating_address_type_(initiating_address_type), advertising_address_type_(advertising_address_type), conn_interval_(conn_interval), conn_peripheral_latency_(conn_peripheral_latency), conn_supervision_timeout_(conn_supervision_timeout) {
    
}
    static std::unique_ptr<LeConnectBuilder> Create(Address source_address, Address destination_address, AddressType initiating_address_type, AddressType advertising_address_type, uint16_t conn_interval, uint16_t conn_peripheral_latency, uint16_t conn_supervision_timeout) {
    return std::make_unique<LeConnectBuilder>(std::move(source_address), std::move(destination_address), initiating_address_type, advertising_address_type, conn_interval, conn_peripheral_latency, conn_supervision_timeout);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_CONNECT) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(initiating_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_interval_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_peripheral_latency_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_supervision_timeout_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    AddressType initiating_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint16_t conn_interval_{0};
    uint16_t conn_peripheral_latency_{0};
    uint16_t conn_supervision_timeout_{0};
};

class LeConnectCompleteView {
public:
    static LeConnectCompleteView Create(LinkLayerPacketView const& parent) {
        return LeConnectCompleteView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetInitiatingAddressType() const {
        _ASSERT_VALID(valid_);
        return initiating_address_type_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    uint16_t GetConnInterval() const {
        _ASSERT_VALID(valid_);
        return conn_interval_;
    }
    
    uint16_t GetConnPeripheralLatency() const {
        _ASSERT_VALID(valid_);
        return conn_peripheral_latency_;
    }
    
    uint16_t GetConnSupervisionTimeout() const {
        _ASSERT_VALID(valid_);
        return conn_supervision_timeout_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_CONNECT_COMPLETE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeConnectCompleteView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_CONNECT_COMPLETE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8) {
            return false;
        }
        initiating_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        conn_interval_ = span.read_le<uint16_t, 2>();
        conn_peripheral_latency_ = span.read_le<uint16_t, 2>();
        conn_supervision_timeout_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType initiating_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint16_t conn_interval_{0};
    uint16_t conn_peripheral_latency_{0};
    uint16_t conn_supervision_timeout_{0};

    
};

class LeConnectCompleteBuilder : public LinkLayerPacketBuilder {
public:
    ~LeConnectCompleteBuilder() override = default;
    LeConnectCompleteBuilder() = default;
    LeConnectCompleteBuilder(LeConnectCompleteBuilder const&) = default;
    LeConnectCompleteBuilder(LeConnectCompleteBuilder&&) = default;
    LeConnectCompleteBuilder& operator=(LeConnectCompleteBuilder const&) = default;
        LeConnectCompleteBuilder(Address source_address, Address destination_address, AddressType initiating_address_type, AddressType advertising_address_type, uint16_t conn_interval, uint16_t conn_peripheral_latency, uint16_t conn_supervision_timeout)
        : LinkLayerPacketBuilder(PacketType::LE_CONNECT_COMPLETE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), initiating_address_type_(initiating_address_type), advertising_address_type_(advertising_address_type), conn_interval_(conn_interval), conn_peripheral_latency_(conn_peripheral_latency), conn_supervision_timeout_(conn_supervision_timeout) {
    
}
    static std::unique_ptr<LeConnectCompleteBuilder> Create(Address source_address, Address destination_address, AddressType initiating_address_type, AddressType advertising_address_type, uint16_t conn_interval, uint16_t conn_peripheral_latency, uint16_t conn_supervision_timeout) {
    return std::make_unique<LeConnectCompleteBuilder>(std::move(source_address), std::move(destination_address), initiating_address_type, advertising_address_type, conn_interval, conn_peripheral_latency, conn_supervision_timeout);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_CONNECT_COMPLETE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(initiating_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_interval_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_peripheral_latency_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(conn_supervision_timeout_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    AddressType initiating_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};
    uint16_t conn_interval_{0};
    uint16_t conn_peripheral_latency_{0};
    uint16_t conn_supervision_timeout_{0};
};

class LeScanView {
public:
    static LeScanView Create(LinkLayerPacketView const& parent) {
        return LeScanView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetScanningAddressType() const {
        _ASSERT_VALID(valid_);
        return scanning_address_type_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_SCAN;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeScanView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_SCAN) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        scanning_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType scanning_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};

    
};

class LeScanBuilder : public LinkLayerPacketBuilder {
public:
    ~LeScanBuilder() override = default;
    LeScanBuilder() = default;
    LeScanBuilder(LeScanBuilder const&) = default;
    LeScanBuilder(LeScanBuilder&&) = default;
    LeScanBuilder& operator=(LeScanBuilder const&) = default;
        LeScanBuilder(Address source_address, Address destination_address, AddressType scanning_address_type, AddressType advertising_address_type)
        : LinkLayerPacketBuilder(PacketType::LE_SCAN, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), scanning_address_type_(scanning_address_type), advertising_address_type_(advertising_address_type) {
    
}
    static std::unique_ptr<LeScanBuilder> Create(Address source_address, Address destination_address, AddressType scanning_address_type, AddressType advertising_address_type) {
    return std::make_unique<LeScanBuilder>(std::move(source_address), std::move(destination_address), scanning_address_type, advertising_address_type);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_SCAN) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(scanning_address_type_) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
    }

    size_t GetSize() const override {
        return 15;
    }

    
    AddressType scanning_address_type_{AddressType::PUBLIC};
    AddressType advertising_address_type_{AddressType::PUBLIC};
};

class LeScanResponseView {
public:
    static LeScanResponseView Create(LinkLayerPacketView const& parent) {
        return LeScanResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    AddressType GetAdvertisingAddressType() const {
        _ASSERT_VALID(valid_);
        return advertising_address_type_;
    }
    
    std::vector<uint8_t> GetScanResponseData() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = scan_response_data_;
        std::vector<uint8_t> elements;
        while (span.size() >= 1) {
            elements.push_back(span.read_le<uint8_t, 1>());
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_SCAN_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeScanResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_SCAN_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        advertising_address_type_ = AddressType(span.read_le<uint8_t, 1>());
        scan_response_data_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    AddressType advertising_address_type_{AddressType::PUBLIC};
    pdl::packet::slice scan_response_data_;

    
};

class LeScanResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~LeScanResponseBuilder() override = default;
    LeScanResponseBuilder() = default;
    LeScanResponseBuilder(LeScanResponseBuilder const&) = default;
    LeScanResponseBuilder(LeScanResponseBuilder&&) = default;
    LeScanResponseBuilder& operator=(LeScanResponseBuilder const&) = default;
        LeScanResponseBuilder(Address source_address, Address destination_address, AddressType advertising_address_type, std::vector<uint8_t> scan_response_data)
        : LinkLayerPacketBuilder(PacketType::LE_SCAN_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), advertising_address_type_(advertising_address_type), scan_response_data_(std::move(scan_response_data)) {
    
}
    static std::unique_ptr<LeScanResponseBuilder> Create(Address source_address, Address destination_address, AddressType advertising_address_type, std::vector<uint8_t> scan_response_data) {
    return std::make_unique<LeScanResponseBuilder>(std::move(source_address), std::move(destination_address), advertising_address_type, std::move(scan_response_data));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_SCAN_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(advertising_address_type_) << 0));
        output.insert(output.end(), scan_response_data_.begin(), scan_response_data_.end());
    }

    size_t GetSize() const override {
        return GetScanResponseDataSize() + 14;
    }

    size_t GetScanResponseDataSize() const {
        return scan_response_data_.size() * 1;
    }
    
    AddressType advertising_address_type_{AddressType::PUBLIC};
    std::vector<uint8_t> scan_response_data_;
};

class PageView {
public:
    static PageView Create(LinkLayerPacketView const& parent) {
        return PageView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    uint8_t GetAllowRoleSwitch() const {
        _ASSERT_VALID(valid_);
        return allow_role_switch_;
    }
    
    PacketType GetType() const {
        return PacketType::PAGE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit PageView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::PAGE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 4) {
            return false;
        }
        class_of_device_ = span.read_le<uint32_t, 3>();
        allow_role_switch_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint32_t class_of_device_{0};
    uint8_t allow_role_switch_{0};

    
};

class PageBuilder : public LinkLayerPacketBuilder {
public:
    ~PageBuilder() override = default;
    PageBuilder() = default;
    PageBuilder(PageBuilder const&) = default;
    PageBuilder(PageBuilder&&) = default;
    PageBuilder& operator=(PageBuilder const&) = default;
        PageBuilder(Address source_address, Address destination_address, uint32_t class_of_device, uint8_t allow_role_switch)
        : LinkLayerPacketBuilder(PacketType::PAGE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), class_of_device_(class_of_device), allow_role_switch_(allow_role_switch) {
    
}
    static std::unique_ptr<PageBuilder> Create(Address source_address, Address destination_address, uint32_t class_of_device, uint8_t allow_role_switch) {
    return std::make_unique<PageBuilder>(std::move(source_address), std::move(destination_address), class_of_device, allow_role_switch);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::PAGE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(allow_role_switch_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 17;
    }

    
    uint32_t class_of_device_{0};
    uint8_t allow_role_switch_{0};
};

class PageResponseView {
public:
    static PageResponseView Create(LinkLayerPacketView const& parent) {
        return PageResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetTryRoleSwitch() const {
        _ASSERT_VALID(valid_);
        return try_role_switch_;
    }
    
    PacketType GetType() const {
        return PacketType::PAGE_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit PageResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::PAGE_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        try_role_switch_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t try_role_switch_{0};

    
};

class PageResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~PageResponseBuilder() override = default;
    PageResponseBuilder() = default;
    PageResponseBuilder(PageResponseBuilder const&) = default;
    PageResponseBuilder(PageResponseBuilder&&) = default;
    PageResponseBuilder& operator=(PageResponseBuilder const&) = default;
        PageResponseBuilder(Address source_address, Address destination_address, uint8_t try_role_switch)
        : LinkLayerPacketBuilder(PacketType::PAGE_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), try_role_switch_(try_role_switch) {
    
}
    static std::unique_ptr<PageResponseBuilder> Create(Address source_address, Address destination_address, uint8_t try_role_switch) {
    return std::make_unique<PageResponseBuilder>(std::move(source_address), std::move(destination_address), try_role_switch);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::PAGE_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(try_role_switch_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t try_role_switch_{0};
};

class PageRejectView {
public:
    static PageRejectView Create(LinkLayerPacketView const& parent) {
        return PageRejectView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetReason() const {
        _ASSERT_VALID(valid_);
        return reason_;
    }
    
    PacketType GetType() const {
        return PacketType::PAGE_REJECT;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit PageRejectView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::PAGE_REJECT) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        reason_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t reason_{0};

    
};

class PageRejectBuilder : public LinkLayerPacketBuilder {
public:
    ~PageRejectBuilder() override = default;
    PageRejectBuilder() = default;
    PageRejectBuilder(PageRejectBuilder const&) = default;
    PageRejectBuilder(PageRejectBuilder&&) = default;
    PageRejectBuilder& operator=(PageRejectBuilder const&) = default;
        PageRejectBuilder(Address source_address, Address destination_address, uint8_t reason)
        : LinkLayerPacketBuilder(PacketType::PAGE_REJECT, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), reason_(reason) {
    
}
    static std::unique_ptr<PageRejectBuilder> Create(Address source_address, Address destination_address, uint8_t reason) {
    return std::make_unique<PageRejectBuilder>(std::move(source_address), std::move(destination_address), reason);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::PAGE_REJECT) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(reason_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t reason_{0};
};

class ReadClockOffsetView {
public:
    static ReadClockOffsetView Create(LinkLayerPacketView const& parent) {
        return ReadClockOffsetView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_CLOCK_OFFSET;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadClockOffsetView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_CLOCK_OFFSET) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class ReadClockOffsetBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadClockOffsetBuilder() override = default;
    ReadClockOffsetBuilder() = default;
    ReadClockOffsetBuilder(ReadClockOffsetBuilder const&) = default;
    ReadClockOffsetBuilder(ReadClockOffsetBuilder&&) = default;
    ReadClockOffsetBuilder& operator=(ReadClockOffsetBuilder const&) = default;
        ReadClockOffsetBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::READ_CLOCK_OFFSET, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<ReadClockOffsetBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<ReadClockOffsetBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_CLOCK_OFFSET) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class ReadClockOffsetResponseView {
public:
    static ReadClockOffsetResponseView Create(LinkLayerPacketView const& parent) {
        return ReadClockOffsetResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint16_t GetOffset() const {
        _ASSERT_VALID(valid_);
        return offset_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_CLOCK_OFFSET_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadClockOffsetResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_CLOCK_OFFSET_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        offset_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint16_t offset_{0};

    
};

class ReadClockOffsetResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadClockOffsetResponseBuilder() override = default;
    ReadClockOffsetResponseBuilder() = default;
    ReadClockOffsetResponseBuilder(ReadClockOffsetResponseBuilder const&) = default;
    ReadClockOffsetResponseBuilder(ReadClockOffsetResponseBuilder&&) = default;
    ReadClockOffsetResponseBuilder& operator=(ReadClockOffsetResponseBuilder const&) = default;
        ReadClockOffsetResponseBuilder(Address source_address, Address destination_address, uint16_t offset)
        : LinkLayerPacketBuilder(PacketType::READ_CLOCK_OFFSET_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), offset_(offset) {
    
}
    static std::unique_ptr<ReadClockOffsetResponseBuilder> Create(Address source_address, Address destination_address, uint16_t offset) {
    return std::make_unique<ReadClockOffsetResponseBuilder>(std::move(source_address), std::move(destination_address), offset);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_CLOCK_OFFSET_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(offset_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 15;
    }

    
    uint16_t offset_{0};
};

class ReadRemoteSupportedFeaturesView {
public:
    static ReadRemoteSupportedFeaturesView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteSupportedFeaturesView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_SUPPORTED_FEATURES;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteSupportedFeaturesView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_SUPPORTED_FEATURES) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class ReadRemoteSupportedFeaturesBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteSupportedFeaturesBuilder() override = default;
    ReadRemoteSupportedFeaturesBuilder() = default;
    ReadRemoteSupportedFeaturesBuilder(ReadRemoteSupportedFeaturesBuilder const&) = default;
    ReadRemoteSupportedFeaturesBuilder(ReadRemoteSupportedFeaturesBuilder&&) = default;
    ReadRemoteSupportedFeaturesBuilder& operator=(ReadRemoteSupportedFeaturesBuilder const&) = default;
        ReadRemoteSupportedFeaturesBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_SUPPORTED_FEATURES, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<ReadRemoteSupportedFeaturesBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<ReadRemoteSupportedFeaturesBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_SUPPORTED_FEATURES) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class ReadRemoteSupportedFeaturesResponseView {
public:
    static ReadRemoteSupportedFeaturesResponseView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteSupportedFeaturesResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint64_t GetFeatures() const {
        _ASSERT_VALID(valid_);
        return features_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_SUPPORTED_FEATURES_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteSupportedFeaturesResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_SUPPORTED_FEATURES_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8) {
            return false;
        }
        features_ = span.read_le<uint64_t, 8>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint64_t features_{0};

    
};

class ReadRemoteSupportedFeaturesResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteSupportedFeaturesResponseBuilder() override = default;
    ReadRemoteSupportedFeaturesResponseBuilder() = default;
    ReadRemoteSupportedFeaturesResponseBuilder(ReadRemoteSupportedFeaturesResponseBuilder const&) = default;
    ReadRemoteSupportedFeaturesResponseBuilder(ReadRemoteSupportedFeaturesResponseBuilder&&) = default;
    ReadRemoteSupportedFeaturesResponseBuilder& operator=(ReadRemoteSupportedFeaturesResponseBuilder const&) = default;
        ReadRemoteSupportedFeaturesResponseBuilder(Address source_address, Address destination_address, uint64_t features)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_SUPPORTED_FEATURES_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), features_(features) {
    
}
    static std::unique_ptr<ReadRemoteSupportedFeaturesResponseBuilder> Create(Address source_address, Address destination_address, uint64_t features) {
    return std::make_unique<ReadRemoteSupportedFeaturesResponseBuilder>(std::move(source_address), std::move(destination_address), features);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_SUPPORTED_FEATURES_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint64_t, 8>(output, (static_cast<uint64_t>(features_ & 0xffffffffffffffff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    uint64_t features_{0};
};

class ReadRemoteLmpFeaturesView {
public:
    static ReadRemoteLmpFeaturesView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteLmpFeaturesView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_LMP_FEATURES;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteLmpFeaturesView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_LMP_FEATURES) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class ReadRemoteLmpFeaturesBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteLmpFeaturesBuilder() override = default;
    ReadRemoteLmpFeaturesBuilder() = default;
    ReadRemoteLmpFeaturesBuilder(ReadRemoteLmpFeaturesBuilder const&) = default;
    ReadRemoteLmpFeaturesBuilder(ReadRemoteLmpFeaturesBuilder&&) = default;
    ReadRemoteLmpFeaturesBuilder& operator=(ReadRemoteLmpFeaturesBuilder const&) = default;
        ReadRemoteLmpFeaturesBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_LMP_FEATURES, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<ReadRemoteLmpFeaturesBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<ReadRemoteLmpFeaturesBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_LMP_FEATURES) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class ReadRemoteLmpFeaturesResponseView {
public:
    static ReadRemoteLmpFeaturesResponseView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteLmpFeaturesResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint64_t GetFeatures() const {
        _ASSERT_VALID(valid_);
        return features_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_LMP_FEATURES_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteLmpFeaturesResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_LMP_FEATURES_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8) {
            return false;
        }
        features_ = span.read_le<uint64_t, 8>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint64_t features_{0};

    
};

class ReadRemoteLmpFeaturesResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteLmpFeaturesResponseBuilder() override = default;
    ReadRemoteLmpFeaturesResponseBuilder() = default;
    ReadRemoteLmpFeaturesResponseBuilder(ReadRemoteLmpFeaturesResponseBuilder const&) = default;
    ReadRemoteLmpFeaturesResponseBuilder(ReadRemoteLmpFeaturesResponseBuilder&&) = default;
    ReadRemoteLmpFeaturesResponseBuilder& operator=(ReadRemoteLmpFeaturesResponseBuilder const&) = default;
        ReadRemoteLmpFeaturesResponseBuilder(Address source_address, Address destination_address, uint64_t features)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_LMP_FEATURES_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), features_(features) {
    
}
    static std::unique_ptr<ReadRemoteLmpFeaturesResponseBuilder> Create(Address source_address, Address destination_address, uint64_t features) {
    return std::make_unique<ReadRemoteLmpFeaturesResponseBuilder>(std::move(source_address), std::move(destination_address), features);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_LMP_FEATURES_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint64_t, 8>(output, (static_cast<uint64_t>(features_ & 0xffffffffffffffff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    uint64_t features_{0};
};

class ReadRemoteExtendedFeaturesView {
public:
    static ReadRemoteExtendedFeaturesView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteExtendedFeaturesView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPageNumber() const {
        _ASSERT_VALID(valid_);
        return page_number_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_EXTENDED_FEATURES;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteExtendedFeaturesView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_EXTENDED_FEATURES) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        page_number_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t page_number_{0};

    
};

class ReadRemoteExtendedFeaturesBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteExtendedFeaturesBuilder() override = default;
    ReadRemoteExtendedFeaturesBuilder() = default;
    ReadRemoteExtendedFeaturesBuilder(ReadRemoteExtendedFeaturesBuilder const&) = default;
    ReadRemoteExtendedFeaturesBuilder(ReadRemoteExtendedFeaturesBuilder&&) = default;
    ReadRemoteExtendedFeaturesBuilder& operator=(ReadRemoteExtendedFeaturesBuilder const&) = default;
        ReadRemoteExtendedFeaturesBuilder(Address source_address, Address destination_address, uint8_t page_number)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_EXTENDED_FEATURES, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), page_number_(page_number) {
    
}
    static std::unique_ptr<ReadRemoteExtendedFeaturesBuilder> Create(Address source_address, Address destination_address, uint8_t page_number) {
    return std::make_unique<ReadRemoteExtendedFeaturesBuilder>(std::move(source_address), std::move(destination_address), page_number);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_EXTENDED_FEATURES) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_number_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t page_number_{0};
};

class ReadRemoteExtendedFeaturesResponseView {
public:
    static ReadRemoteExtendedFeaturesResponseView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteExtendedFeaturesResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetStatus() const {
        _ASSERT_VALID(valid_);
        return status_;
    }
    
    uint8_t GetPageNumber() const {
        _ASSERT_VALID(valid_);
        return page_number_;
    }
    
    uint8_t GetMaxPageNumber() const {
        _ASSERT_VALID(valid_);
        return max_page_number_;
    }
    
    uint64_t GetFeatures() const {
        _ASSERT_VALID(valid_);
        return features_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_EXTENDED_FEATURES_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteExtendedFeaturesResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_EXTENDED_FEATURES_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 11) {
            return false;
        }
        status_ = span.read_le<uint8_t, 1>();
        page_number_ = span.read_le<uint8_t, 1>();
        max_page_number_ = span.read_le<uint8_t, 1>();
        features_ = span.read_le<uint64_t, 8>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t status_{0};
    uint8_t page_number_{0};
    uint8_t max_page_number_{0};
    uint64_t features_{0};

    
};

class ReadRemoteExtendedFeaturesResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteExtendedFeaturesResponseBuilder() override = default;
    ReadRemoteExtendedFeaturesResponseBuilder() = default;
    ReadRemoteExtendedFeaturesResponseBuilder(ReadRemoteExtendedFeaturesResponseBuilder const&) = default;
    ReadRemoteExtendedFeaturesResponseBuilder(ReadRemoteExtendedFeaturesResponseBuilder&&) = default;
    ReadRemoteExtendedFeaturesResponseBuilder& operator=(ReadRemoteExtendedFeaturesResponseBuilder const&) = default;
        ReadRemoteExtendedFeaturesResponseBuilder(Address source_address, Address destination_address, uint8_t status, uint8_t page_number, uint8_t max_page_number, uint64_t features)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_EXTENDED_FEATURES_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), status_(status), page_number_(page_number), max_page_number_(max_page_number), features_(features) {
    
}
    static std::unique_ptr<ReadRemoteExtendedFeaturesResponseBuilder> Create(Address source_address, Address destination_address, uint8_t status, uint8_t page_number, uint8_t max_page_number, uint64_t features) {
    return std::make_unique<ReadRemoteExtendedFeaturesResponseBuilder>(std::move(source_address), std::move(destination_address), status, page_number, max_page_number, features);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_EXTENDED_FEATURES_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(status_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(page_number_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(max_page_number_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint64_t, 8>(output, (static_cast<uint64_t>(features_ & 0xffffffffffffffff) << 0));
    }

    size_t GetSize() const override {
        return 24;
    }

    
    uint8_t status_{0};
    uint8_t page_number_{0};
    uint8_t max_page_number_{0};
    uint64_t features_{0};
};

class ReadRemoteVersionInformationView {
public:
    static ReadRemoteVersionInformationView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteVersionInformationView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_VERSION_INFORMATION;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteVersionInformationView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_VERSION_INFORMATION) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class ReadRemoteVersionInformationBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteVersionInformationBuilder() override = default;
    ReadRemoteVersionInformationBuilder() = default;
    ReadRemoteVersionInformationBuilder(ReadRemoteVersionInformationBuilder const&) = default;
    ReadRemoteVersionInformationBuilder(ReadRemoteVersionInformationBuilder&&) = default;
    ReadRemoteVersionInformationBuilder& operator=(ReadRemoteVersionInformationBuilder const&) = default;
        ReadRemoteVersionInformationBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_VERSION_INFORMATION, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<ReadRemoteVersionInformationBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<ReadRemoteVersionInformationBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_VERSION_INFORMATION) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class ReadRemoteVersionInformationResponseView {
public:
    static ReadRemoteVersionInformationResponseView Create(LinkLayerPacketView const& parent) {
        return ReadRemoteVersionInformationResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetLmpVersion() const {
        _ASSERT_VALID(valid_);
        return lmp_version_;
    }
    
    uint8_t GetLmpSubversion() const {
        _ASSERT_VALID(valid_);
        return lmp_subversion_;
    }
    
    uint16_t GetManufacturerName() const {
        _ASSERT_VALID(valid_);
        return manufacturer_name_;
    }
    
    PacketType GetType() const {
        return PacketType::READ_REMOTE_VERSION_INFORMATION_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ReadRemoteVersionInformationResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::READ_REMOTE_VERSION_INFORMATION_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 4) {
            return false;
        }
        lmp_version_ = span.read_le<uint8_t, 1>();
        lmp_subversion_ = span.read_le<uint8_t, 1>();
        manufacturer_name_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t lmp_version_{0};
    uint8_t lmp_subversion_{0};
    uint16_t manufacturer_name_{0};

    
};

class ReadRemoteVersionInformationResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ReadRemoteVersionInformationResponseBuilder() override = default;
    ReadRemoteVersionInformationResponseBuilder() = default;
    ReadRemoteVersionInformationResponseBuilder(ReadRemoteVersionInformationResponseBuilder const&) = default;
    ReadRemoteVersionInformationResponseBuilder(ReadRemoteVersionInformationResponseBuilder&&) = default;
    ReadRemoteVersionInformationResponseBuilder& operator=(ReadRemoteVersionInformationResponseBuilder const&) = default;
        ReadRemoteVersionInformationResponseBuilder(Address source_address, Address destination_address, uint8_t lmp_version, uint8_t lmp_subversion, uint16_t manufacturer_name)
        : LinkLayerPacketBuilder(PacketType::READ_REMOTE_VERSION_INFORMATION_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), lmp_version_(lmp_version), lmp_subversion_(lmp_subversion), manufacturer_name_(manufacturer_name) {
    
}
    static std::unique_ptr<ReadRemoteVersionInformationResponseBuilder> Create(Address source_address, Address destination_address, uint8_t lmp_version, uint8_t lmp_subversion, uint16_t manufacturer_name) {
    return std::make_unique<ReadRemoteVersionInformationResponseBuilder>(std::move(source_address), std::move(destination_address), lmp_version, lmp_subversion, manufacturer_name);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::READ_REMOTE_VERSION_INFORMATION_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(lmp_version_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(lmp_subversion_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(manufacturer_name_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 17;
    }

    
    uint8_t lmp_version_{0};
    uint8_t lmp_subversion_{0};
    uint16_t manufacturer_name_{0};
};

class RemoteNameRequestView {
public:
    static RemoteNameRequestView Create(LinkLayerPacketView const& parent) {
        return RemoteNameRequestView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::REMOTE_NAME_REQUEST;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit RemoteNameRequestView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::REMOTE_NAME_REQUEST) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class RemoteNameRequestBuilder : public LinkLayerPacketBuilder {
public:
    ~RemoteNameRequestBuilder() override = default;
    RemoteNameRequestBuilder() = default;
    RemoteNameRequestBuilder(RemoteNameRequestBuilder const&) = default;
    RemoteNameRequestBuilder(RemoteNameRequestBuilder&&) = default;
    RemoteNameRequestBuilder& operator=(RemoteNameRequestBuilder const&) = default;
        RemoteNameRequestBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::REMOTE_NAME_REQUEST, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<RemoteNameRequestBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<RemoteNameRequestBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::REMOTE_NAME_REQUEST) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class RemoteNameRequestResponseView {
public:
    static RemoteNameRequestResponseView Create(LinkLayerPacketView const& parent) {
        return RemoteNameRequestResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::array<uint8_t, 248> GetName() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = name_;
        std::array<uint8_t, 248> elements;
        for (int n = 0; n < 248; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::REMOTE_NAME_REQUEST_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit RemoteNameRequestResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::REMOTE_NAME_REQUEST_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 248 * 1) {
            return false;
        }
        name_ = span.subrange(0, 248 * 1);
        span.skip(248 * 1);
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice name_;

    
};

class RemoteNameRequestResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~RemoteNameRequestResponseBuilder() override = default;
    RemoteNameRequestResponseBuilder() = default;
    RemoteNameRequestResponseBuilder(RemoteNameRequestResponseBuilder const&) = default;
    RemoteNameRequestResponseBuilder(RemoteNameRequestResponseBuilder&&) = default;
    RemoteNameRequestResponseBuilder& operator=(RemoteNameRequestResponseBuilder const&) = default;
        RemoteNameRequestResponseBuilder(Address source_address, Address destination_address, std::array<uint8_t, 248> name)
        : LinkLayerPacketBuilder(PacketType::REMOTE_NAME_REQUEST_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), name_(std::move(name)) {
    
}
    static std::unique_ptr<RemoteNameRequestResponseBuilder> Create(Address source_address, Address destination_address, std::array<uint8_t, 248> name) {
    return std::make_unique<RemoteNameRequestResponseBuilder>(std::move(source_address), std::move(destination_address), std::move(name));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::REMOTE_NAME_REQUEST_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), name_.begin(), name_.end());
    }

    size_t GetSize() const override {
        return 261;
    }

    size_t GetNameSize() const {
        return 248;
    }
    
    std::array<uint8_t, 248> name_;
};

class LeEncryptConnectionView {
public:
    static LeEncryptConnectionView Create(LinkLayerPacketView const& parent) {
        return LeEncryptConnectionView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::array<uint8_t, 8> GetRand() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = rand_;
        std::array<uint8_t, 8> elements;
        for (int n = 0; n < 8; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    uint16_t GetEdiv() const {
        _ASSERT_VALID(valid_);
        return ediv_;
    }
    
    std::array<uint8_t, 16> GetLtk() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = ltk_;
        std::array<uint8_t, 16> elements;
        for (int n = 0; n < 16; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_ENCRYPT_CONNECTION;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeEncryptConnectionView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_ENCRYPT_CONNECTION) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8 * 1) {
            return false;
        }
        rand_ = span.subrange(0, 8 * 1);
        span.skip(8 * 1);
        if (span.size() < 2) {
            return false;
        }
        ediv_ = span.read_le<uint16_t, 2>();
        if (span.size() < 16 * 1) {
            return false;
        }
        ltk_ = span.subrange(0, 16 * 1);
        span.skip(16 * 1);
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice rand_;
    uint16_t ediv_{0};
    pdl::packet::slice ltk_;

    
};

class LeEncryptConnectionBuilder : public LinkLayerPacketBuilder {
public:
    ~LeEncryptConnectionBuilder() override = default;
    LeEncryptConnectionBuilder() = default;
    LeEncryptConnectionBuilder(LeEncryptConnectionBuilder const&) = default;
    LeEncryptConnectionBuilder(LeEncryptConnectionBuilder&&) = default;
    LeEncryptConnectionBuilder& operator=(LeEncryptConnectionBuilder const&) = default;
        LeEncryptConnectionBuilder(Address source_address, Address destination_address, std::array<uint8_t, 8> rand, uint16_t ediv, std::array<uint8_t, 16> ltk)
        : LinkLayerPacketBuilder(PacketType::LE_ENCRYPT_CONNECTION, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), rand_(std::move(rand)), ediv_(ediv), ltk_(std::move(ltk)) {
    
}
    static std::unique_ptr<LeEncryptConnectionBuilder> Create(Address source_address, Address destination_address, std::array<uint8_t, 8> rand, uint16_t ediv, std::array<uint8_t, 16> ltk) {
    return std::make_unique<LeEncryptConnectionBuilder>(std::move(source_address), std::move(destination_address), std::move(rand), ediv, std::move(ltk));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_ENCRYPT_CONNECTION) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), rand_.begin(), rand_.end());
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(ediv_ & 0xffff) << 0));
        output.insert(output.end(), ltk_.begin(), ltk_.end());
    }

    size_t GetSize() const override {
        return 39;
    }

    size_t GetRandSize() const {
        return 8;
    }
    
    size_t GetLtkSize() const {
        return 16;
    }
    
    std::array<uint8_t, 8> rand_;
    uint16_t ediv_{0};
    std::array<uint8_t, 16> ltk_;
};

class LeEncryptConnectionResponseView {
public:
    static LeEncryptConnectionResponseView Create(LinkLayerPacketView const& parent) {
        return LeEncryptConnectionResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::array<uint8_t, 8> GetRand() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = rand_;
        std::array<uint8_t, 8> elements;
        for (int n = 0; n < 8; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    uint16_t GetEdiv() const {
        _ASSERT_VALID(valid_);
        return ediv_;
    }
    
    std::array<uint8_t, 16> GetLtk() const {
        _ASSERT_VALID(valid_);
        pdl::packet::slice span = ltk_;
        std::array<uint8_t, 16> elements;
        for (int n = 0; n < 16; n++) {
            elements[n] = span.read_le<uint8_t, 1>();
        }
        return elements;
    }
    
    PacketType GetType() const {
        return PacketType::LE_ENCRYPT_CONNECTION_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeEncryptConnectionResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_ENCRYPT_CONNECTION_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8 * 1) {
            return false;
        }
        rand_ = span.subrange(0, 8 * 1);
        span.skip(8 * 1);
        if (span.size() < 2) {
            return false;
        }
        ediv_ = span.read_le<uint16_t, 2>();
        if (span.size() < 16 * 1) {
            return false;
        }
        ltk_ = span.subrange(0, 16 * 1);
        span.skip(16 * 1);
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice rand_;
    uint16_t ediv_{0};
    pdl::packet::slice ltk_;

    
};

class LeEncryptConnectionResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~LeEncryptConnectionResponseBuilder() override = default;
    LeEncryptConnectionResponseBuilder() = default;
    LeEncryptConnectionResponseBuilder(LeEncryptConnectionResponseBuilder const&) = default;
    LeEncryptConnectionResponseBuilder(LeEncryptConnectionResponseBuilder&&) = default;
    LeEncryptConnectionResponseBuilder& operator=(LeEncryptConnectionResponseBuilder const&) = default;
        LeEncryptConnectionResponseBuilder(Address source_address, Address destination_address, std::array<uint8_t, 8> rand, uint16_t ediv, std::array<uint8_t, 16> ltk)
        : LinkLayerPacketBuilder(PacketType::LE_ENCRYPT_CONNECTION_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), rand_(std::move(rand)), ediv_(ediv), ltk_(std::move(ltk)) {
    
}
    static std::unique_ptr<LeEncryptConnectionResponseBuilder> Create(Address source_address, Address destination_address, std::array<uint8_t, 8> rand, uint16_t ediv, std::array<uint8_t, 16> ltk) {
    return std::make_unique<LeEncryptConnectionResponseBuilder>(std::move(source_address), std::move(destination_address), std::move(rand), ediv, std::move(ltk));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_ENCRYPT_CONNECTION_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), rand_.begin(), rand_.end());
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(ediv_ & 0xffff) << 0));
        output.insert(output.end(), ltk_.begin(), ltk_.end());
    }

    size_t GetSize() const override {
        return 39;
    }

    size_t GetRandSize() const {
        return 8;
    }
    
    size_t GetLtkSize() const {
        return 16;
    }
    
    std::array<uint8_t, 8> rand_;
    uint16_t ediv_{0};
    std::array<uint8_t, 16> ltk_;
};

enum class PasskeyNotificationType : uint8_t {
    ENTRY_STARTED = 0x0,
    DIGIT_ENTERED = 0x1,
    DIGIT_ERASED = 0x2,
    CLEARED = 0x3,
    ENTRY_COMPLETED = 0x4,
};

inline std::string PasskeyNotificationTypeText(PasskeyNotificationType tag) {
    switch (tag) {
        case PasskeyNotificationType::ENTRY_STARTED: return "ENTRY_STARTED";
        case PasskeyNotificationType::DIGIT_ENTERED: return "DIGIT_ENTERED";
        case PasskeyNotificationType::DIGIT_ERASED: return "DIGIT_ERASED";
        case PasskeyNotificationType::CLEARED: return "CLEARED";
        case PasskeyNotificationType::ENTRY_COMPLETED: return "ENTRY_COMPLETED";
        default:
            return std::string("Unknown PasskeyNotificationType: " +
                   std::to_string(static_cast<uint64_t>(tag)));
    }
}

class LeReadRemoteFeaturesView {
public:
    static LeReadRemoteFeaturesView Create(LinkLayerPacketView const& parent) {
        return LeReadRemoteFeaturesView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_READ_REMOTE_FEATURES;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeReadRemoteFeaturesView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_READ_REMOTE_FEATURES) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class LeReadRemoteFeaturesBuilder : public LinkLayerPacketBuilder {
public:
    ~LeReadRemoteFeaturesBuilder() override = default;
    LeReadRemoteFeaturesBuilder() = default;
    LeReadRemoteFeaturesBuilder(LeReadRemoteFeaturesBuilder const&) = default;
    LeReadRemoteFeaturesBuilder(LeReadRemoteFeaturesBuilder&&) = default;
    LeReadRemoteFeaturesBuilder& operator=(LeReadRemoteFeaturesBuilder const&) = default;
        LeReadRemoteFeaturesBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::LE_READ_REMOTE_FEATURES, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<LeReadRemoteFeaturesBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<LeReadRemoteFeaturesBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_READ_REMOTE_FEATURES) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class LeReadRemoteFeaturesResponseView {
public:
    static LeReadRemoteFeaturesResponseView Create(LinkLayerPacketView const& parent) {
        return LeReadRemoteFeaturesResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint64_t GetFeatures() const {
        _ASSERT_VALID(valid_);
        return features_;
    }
    
    uint8_t GetStatus() const {
        _ASSERT_VALID(valid_);
        return status_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_READ_REMOTE_FEATURES_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeReadRemoteFeaturesResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_READ_REMOTE_FEATURES_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 9) {
            return false;
        }
        features_ = span.read_le<uint64_t, 8>();
        status_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint64_t features_{0};
    uint8_t status_{0};

    
};

class LeReadRemoteFeaturesResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~LeReadRemoteFeaturesResponseBuilder() override = default;
    LeReadRemoteFeaturesResponseBuilder() = default;
    LeReadRemoteFeaturesResponseBuilder(LeReadRemoteFeaturesResponseBuilder const&) = default;
    LeReadRemoteFeaturesResponseBuilder(LeReadRemoteFeaturesResponseBuilder&&) = default;
    LeReadRemoteFeaturesResponseBuilder& operator=(LeReadRemoteFeaturesResponseBuilder const&) = default;
        LeReadRemoteFeaturesResponseBuilder(Address source_address, Address destination_address, uint64_t features, uint8_t status)
        : LinkLayerPacketBuilder(PacketType::LE_READ_REMOTE_FEATURES_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), features_(features), status_(status) {
    
}
    static std::unique_ptr<LeReadRemoteFeaturesResponseBuilder> Create(Address source_address, Address destination_address, uint64_t features, uint8_t status) {
    return std::make_unique<LeReadRemoteFeaturesResponseBuilder>(std::move(source_address), std::move(destination_address), features, status);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_READ_REMOTE_FEATURES_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint64_t, 8>(output, (static_cast<uint64_t>(features_ & 0xffffffffffffffff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(status_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 22;
    }

    
    uint64_t features_{0};
    uint8_t status_{0};
};

class LeConnectionParameterRequestView {
public:
    static LeConnectionParameterRequestView Create(LinkLayerPacketView const& parent) {
        return LeConnectionParameterRequestView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint16_t GetIntervalMin() const {
        _ASSERT_VALID(valid_);
        return interval_min_;
    }
    
    uint16_t GetIntervalMax() const {
        _ASSERT_VALID(valid_);
        return interval_max_;
    }
    
    uint16_t GetLatency() const {
        _ASSERT_VALID(valid_);
        return latency_;
    }
    
    uint16_t GetTimeout() const {
        _ASSERT_VALID(valid_);
        return timeout_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_CONNECTION_PARAMETER_REQUEST;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeConnectionParameterRequestView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_CONNECTION_PARAMETER_REQUEST) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 8) {
            return false;
        }
        interval_min_ = span.read_le<uint16_t, 2>();
        interval_max_ = span.read_le<uint16_t, 2>();
        latency_ = span.read_le<uint16_t, 2>();
        timeout_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint16_t interval_min_{0};
    uint16_t interval_max_{0};
    uint16_t latency_{0};
    uint16_t timeout_{0};

    
};

class LeConnectionParameterRequestBuilder : public LinkLayerPacketBuilder {
public:
    ~LeConnectionParameterRequestBuilder() override = default;
    LeConnectionParameterRequestBuilder() = default;
    LeConnectionParameterRequestBuilder(LeConnectionParameterRequestBuilder const&) = default;
    LeConnectionParameterRequestBuilder(LeConnectionParameterRequestBuilder&&) = default;
    LeConnectionParameterRequestBuilder& operator=(LeConnectionParameterRequestBuilder const&) = default;
        LeConnectionParameterRequestBuilder(Address source_address, Address destination_address, uint16_t interval_min, uint16_t interval_max, uint16_t latency, uint16_t timeout)
        : LinkLayerPacketBuilder(PacketType::LE_CONNECTION_PARAMETER_REQUEST, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), interval_min_(interval_min), interval_max_(interval_max), latency_(latency), timeout_(timeout) {
    
}
    static std::unique_ptr<LeConnectionParameterRequestBuilder> Create(Address source_address, Address destination_address, uint16_t interval_min, uint16_t interval_max, uint16_t latency, uint16_t timeout) {
    return std::make_unique<LeConnectionParameterRequestBuilder>(std::move(source_address), std::move(destination_address), interval_min, interval_max, latency, timeout);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_CONNECTION_PARAMETER_REQUEST) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(interval_min_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(interval_max_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(latency_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(timeout_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 21;
    }

    
    uint16_t interval_min_{0};
    uint16_t interval_max_{0};
    uint16_t latency_{0};
    uint16_t timeout_{0};
};

class LeConnectionParameterUpdateView {
public:
    static LeConnectionParameterUpdateView Create(LinkLayerPacketView const& parent) {
        return LeConnectionParameterUpdateView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetStatus() const {
        _ASSERT_VALID(valid_);
        return status_;
    }
    
    uint16_t GetInterval() const {
        _ASSERT_VALID(valid_);
        return interval_;
    }
    
    uint16_t GetLatency() const {
        _ASSERT_VALID(valid_);
        return latency_;
    }
    
    uint16_t GetTimeout() const {
        _ASSERT_VALID(valid_);
        return timeout_;
    }
    
    PacketType GetType() const {
        return PacketType::LE_CONNECTION_PARAMETER_UPDATE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LeConnectionParameterUpdateView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LE_CONNECTION_PARAMETER_UPDATE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 7) {
            return false;
        }
        status_ = span.read_le<uint8_t, 1>();
        interval_ = span.read_le<uint16_t, 2>();
        latency_ = span.read_le<uint16_t, 2>();
        timeout_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t status_{0};
    uint16_t interval_{0};
    uint16_t latency_{0};
    uint16_t timeout_{0};

    
};

class LeConnectionParameterUpdateBuilder : public LinkLayerPacketBuilder {
public:
    ~LeConnectionParameterUpdateBuilder() override = default;
    LeConnectionParameterUpdateBuilder() = default;
    LeConnectionParameterUpdateBuilder(LeConnectionParameterUpdateBuilder const&) = default;
    LeConnectionParameterUpdateBuilder(LeConnectionParameterUpdateBuilder&&) = default;
    LeConnectionParameterUpdateBuilder& operator=(LeConnectionParameterUpdateBuilder const&) = default;
        LeConnectionParameterUpdateBuilder(Address source_address, Address destination_address, uint8_t status, uint16_t interval, uint16_t latency, uint16_t timeout)
        : LinkLayerPacketBuilder(PacketType::LE_CONNECTION_PARAMETER_UPDATE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), status_(status), interval_(interval), latency_(latency), timeout_(timeout) {
    
}
    static std::unique_ptr<LeConnectionParameterUpdateBuilder> Create(Address source_address, Address destination_address, uint8_t status, uint16_t interval, uint16_t latency, uint16_t timeout) {
    return std::make_unique<LeConnectionParameterUpdateBuilder>(std::move(source_address), std::move(destination_address), status, interval, latency, timeout);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LE_CONNECTION_PARAMETER_UPDATE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(status_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(interval_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(latency_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(timeout_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 20;
    }

    
    uint8_t status_{0};
    uint16_t interval_{0};
    uint16_t latency_{0};
    uint16_t timeout_{0};
};

class ScoConnectionRequestView {
public:
    static ScoConnectionRequestView Create(LinkLayerPacketView const& parent) {
        return ScoConnectionRequestView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint32_t GetTransmitBandwidth() const {
        _ASSERT_VALID(valid_);
        return transmit_bandwidth_;
    }
    
    uint32_t GetReceiveBandwidth() const {
        _ASSERT_VALID(valid_);
        return receive_bandwidth_;
    }
    
    uint16_t GetMaxLatency() const {
        _ASSERT_VALID(valid_);
        return max_latency_;
    }
    
    uint16_t GetVoiceSetting() const {
        _ASSERT_VALID(valid_);
        return voice_setting_;
    }
    
    uint8_t GetRetransmissionEffort() const {
        _ASSERT_VALID(valid_);
        return retransmission_effort_;
    }
    
    uint16_t GetPacketType() const {
        _ASSERT_VALID(valid_);
        return packet_type_;
    }
    
    uint32_t GetClassOfDevice() const {
        _ASSERT_VALID(valid_);
        return class_of_device_;
    }
    
    PacketType GetType() const {
        return PacketType::SCO_CONNECTION_REQUEST;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ScoConnectionRequestView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::SCO_CONNECTION_REQUEST) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 18) {
            return false;
        }
        transmit_bandwidth_ = span.read_le<uint32_t, 4>();
        receive_bandwidth_ = span.read_le<uint32_t, 4>();
        max_latency_ = span.read_le<uint16_t, 2>();
        uint16_t chunk0 = span.read_le<uint16_t, 2>();
        voice_setting_ = (chunk0 >> 0) & 0x3ff;
        retransmission_effort_ = span.read_le<uint8_t, 1>();
        packet_type_ = span.read_le<uint16_t, 2>();
        class_of_device_ = span.read_le<uint32_t, 3>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint32_t transmit_bandwidth_{0};
    uint32_t receive_bandwidth_{0};
    uint16_t max_latency_{0};
    uint16_t voice_setting_{0};
    uint8_t retransmission_effort_{0};
    uint16_t packet_type_{0};
    uint32_t class_of_device_{0};

    
};

class ScoConnectionRequestBuilder : public LinkLayerPacketBuilder {
public:
    ~ScoConnectionRequestBuilder() override = default;
    ScoConnectionRequestBuilder() = default;
    ScoConnectionRequestBuilder(ScoConnectionRequestBuilder const&) = default;
    ScoConnectionRequestBuilder(ScoConnectionRequestBuilder&&) = default;
    ScoConnectionRequestBuilder& operator=(ScoConnectionRequestBuilder const&) = default;
        ScoConnectionRequestBuilder(Address source_address, Address destination_address, uint32_t transmit_bandwidth, uint32_t receive_bandwidth, uint16_t max_latency, uint16_t voice_setting, uint8_t retransmission_effort, uint16_t packet_type, uint32_t class_of_device)
        : LinkLayerPacketBuilder(PacketType::SCO_CONNECTION_REQUEST, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), transmit_bandwidth_(transmit_bandwidth), receive_bandwidth_(receive_bandwidth), max_latency_(max_latency), voice_setting_(voice_setting), retransmission_effort_(retransmission_effort), packet_type_(packet_type), class_of_device_(class_of_device) {
    
}
    static std::unique_ptr<ScoConnectionRequestBuilder> Create(Address source_address, Address destination_address, uint32_t transmit_bandwidth, uint32_t receive_bandwidth, uint16_t max_latency, uint16_t voice_setting, uint8_t retransmission_effort, uint16_t packet_type, uint32_t class_of_device) {
    return std::make_unique<ScoConnectionRequestBuilder>(std::move(source_address), std::move(destination_address), transmit_bandwidth, receive_bandwidth, max_latency, voice_setting, retransmission_effort, packet_type, class_of_device);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::SCO_CONNECTION_REQUEST) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint32_t, 4>(output, (static_cast<uint32_t>(transmit_bandwidth_ & 0xffffffff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 4>(output, (static_cast<uint32_t>(receive_bandwidth_ & 0xffffffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(max_latency_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(voice_setting_ & 0x3ff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(retransmission_effort_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(packet_type_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint32_t, 3>(output, (static_cast<uint32_t>(class_of_device_ & 0xffffff) << 0));
    }

    size_t GetSize() const override {
        return 31;
    }

    
    uint32_t transmit_bandwidth_{0};
    uint32_t receive_bandwidth_{0};
    uint16_t max_latency_{0};
    uint16_t voice_setting_{0};
    uint8_t retransmission_effort_{0};
    uint16_t packet_type_{0};
    uint32_t class_of_device_{0};
};

class ScoConnectionResponseView {
public:
    static ScoConnectionResponseView Create(LinkLayerPacketView const& parent) {
        return ScoConnectionResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetStatus() const {
        _ASSERT_VALID(valid_);
        return status_;
    }
    
    uint8_t GetTransmissionInterval() const {
        _ASSERT_VALID(valid_);
        return transmission_interval_;
    }
    
    uint8_t GetRetransmissionWindow() const {
        _ASSERT_VALID(valid_);
        return retransmission_window_;
    }
    
    uint16_t GetRxPacketLength() const {
        _ASSERT_VALID(valid_);
        return rx_packet_length_;
    }
    
    uint16_t GetTxPacketLength() const {
        _ASSERT_VALID(valid_);
        return tx_packet_length_;
    }
    
    uint8_t GetAirMode() const {
        _ASSERT_VALID(valid_);
        return air_mode_;
    }
    
    uint8_t GetExtended() const {
        _ASSERT_VALID(valid_);
        return extended_;
    }
    
    PacketType GetType() const {
        return PacketType::SCO_CONNECTION_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ScoConnectionResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::SCO_CONNECTION_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 9) {
            return false;
        }
        status_ = span.read_le<uint8_t, 1>();
        transmission_interval_ = span.read_le<uint8_t, 1>();
        retransmission_window_ = span.read_le<uint8_t, 1>();
        rx_packet_length_ = span.read_le<uint16_t, 2>();
        tx_packet_length_ = span.read_le<uint16_t, 2>();
        air_mode_ = span.read_le<uint8_t, 1>();
        uint8_t chunk0 = span.read_le<uint8_t, 1>();
        extended_ = (chunk0 >> 0) & 0x1;
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t status_{0};
    uint8_t transmission_interval_{0};
    uint8_t retransmission_window_{0};
    uint16_t rx_packet_length_{0};
    uint16_t tx_packet_length_{0};
    uint8_t air_mode_{0};
    uint8_t extended_{0};

    
};

class ScoConnectionResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~ScoConnectionResponseBuilder() override = default;
    ScoConnectionResponseBuilder() = default;
    ScoConnectionResponseBuilder(ScoConnectionResponseBuilder const&) = default;
    ScoConnectionResponseBuilder(ScoConnectionResponseBuilder&&) = default;
    ScoConnectionResponseBuilder& operator=(ScoConnectionResponseBuilder const&) = default;
        ScoConnectionResponseBuilder(Address source_address, Address destination_address, uint8_t status, uint8_t transmission_interval, uint8_t retransmission_window, uint16_t rx_packet_length, uint16_t tx_packet_length, uint8_t air_mode, uint8_t extended)
        : LinkLayerPacketBuilder(PacketType::SCO_CONNECTION_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), status_(status), transmission_interval_(transmission_interval), retransmission_window_(retransmission_window), rx_packet_length_(rx_packet_length), tx_packet_length_(tx_packet_length), air_mode_(air_mode), extended_(extended) {
    
}
    static std::unique_ptr<ScoConnectionResponseBuilder> Create(Address source_address, Address destination_address, uint8_t status, uint8_t transmission_interval, uint8_t retransmission_window, uint16_t rx_packet_length, uint16_t tx_packet_length, uint8_t air_mode, uint8_t extended) {
    return std::make_unique<ScoConnectionResponseBuilder>(std::move(source_address), std::move(destination_address), status, transmission_interval, retransmission_window, rx_packet_length, tx_packet_length, air_mode, extended);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::SCO_CONNECTION_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(status_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(transmission_interval_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(retransmission_window_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(rx_packet_length_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(tx_packet_length_ & 0xffff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(air_mode_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(extended_ & 0x1) << 0));
    }

    size_t GetSize() const override {
        return 22;
    }

    
    uint8_t status_{0};
    uint8_t transmission_interval_{0};
    uint8_t retransmission_window_{0};
    uint16_t rx_packet_length_{0};
    uint16_t tx_packet_length_{0};
    uint8_t air_mode_{0};
    uint8_t extended_{0};
};

class ScoDisconnectView {
public:
    static ScoDisconnectView Create(LinkLayerPacketView const& parent) {
        return ScoDisconnectView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetReason() const {
        _ASSERT_VALID(valid_);
        return reason_;
    }
    
    PacketType GetType() const {
        return PacketType::SCO_DISCONNECT;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit ScoDisconnectView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::SCO_DISCONNECT) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        reason_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t reason_{0};

    
};

class ScoDisconnectBuilder : public LinkLayerPacketBuilder {
public:
    ~ScoDisconnectBuilder() override = default;
    ScoDisconnectBuilder() = default;
    ScoDisconnectBuilder(ScoDisconnectBuilder const&) = default;
    ScoDisconnectBuilder(ScoDisconnectBuilder&&) = default;
    ScoDisconnectBuilder& operator=(ScoDisconnectBuilder const&) = default;
        ScoDisconnectBuilder(Address source_address, Address destination_address, uint8_t reason)
        : LinkLayerPacketBuilder(PacketType::SCO_DISCONNECT, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), reason_(reason) {
    
}
    static std::unique_ptr<ScoDisconnectBuilder> Create(Address source_address, Address destination_address, uint8_t reason) {
    return std::make_unique<ScoDisconnectBuilder>(std::move(source_address), std::move(destination_address), reason);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::SCO_DISCONNECT) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(reason_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t reason_{0};
};

class LmpView {
public:
    static LmpView Create(LinkLayerPacketView const& parent) {
        return LmpView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::vector<uint8_t> GetPayload() const {
        _ASSERT_VALID(valid_);
        return payload_.bytes();
    }
    
    PacketType GetType() const {
        return PacketType::LMP;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LmpView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LMP) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        payload_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice payload_;

    
};

class LmpBuilder : public LinkLayerPacketBuilder {
public:
    ~LmpBuilder() override = default;
    LmpBuilder() = default;
    LmpBuilder(LmpBuilder const&) = default;
    LmpBuilder(LmpBuilder&&) = default;
    LmpBuilder& operator=(LmpBuilder const&) = default;
        LmpBuilder(Address source_address, Address destination_address, std::vector<uint8_t> payload)
        : LinkLayerPacketBuilder(PacketType::LMP, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    payload_ = std::move(payload);
}
    static std::unique_ptr<LmpBuilder> Create(Address source_address, Address destination_address, std::vector<uint8_t> payload) {
    return std::make_unique<LmpBuilder>(std::move(source_address), std::move(destination_address), std::move(payload));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LMP) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), payload_.begin(), payload_.end());
    }

    size_t GetSize() const override {
        return payload_.size() + 13;
    }

    
    
};

class LlcpView {
public:
    static LlcpView Create(LinkLayerPacketView const& parent) {
        return LlcpView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    std::vector<uint8_t> GetPayload() const {
        _ASSERT_VALID(valid_);
        return payload_.bytes();
    }
    
    PacketType GetType() const {
        return PacketType::LLCP;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LlcpView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LLCP) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        payload_ = span;
        span.clear();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    pdl::packet::slice payload_;

    
};

class LlcpBuilder : public LinkLayerPacketBuilder {
public:
    ~LlcpBuilder() override = default;
    LlcpBuilder() = default;
    LlcpBuilder(LlcpBuilder const&) = default;
    LlcpBuilder(LlcpBuilder&&) = default;
    LlcpBuilder& operator=(LlcpBuilder const&) = default;
        LlcpBuilder(Address source_address, Address destination_address, std::vector<uint8_t> payload)
        : LinkLayerPacketBuilder(PacketType::LLCP, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    payload_ = std::move(payload);
}
    static std::unique_ptr<LlcpBuilder> Create(Address source_address, Address destination_address, std::vector<uint8_t> payload) {
    return std::make_unique<LlcpBuilder>(std::move(source_address), std::move(destination_address), std::move(payload));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LLCP) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        output.insert(output.end(), payload_.begin(), payload_.end());
    }

    size_t GetSize() const override {
        return payload_.size() + 13;
    }

    
    
};

class PingRequestView {
public:
    static PingRequestView Create(LinkLayerPacketView const& parent) {
        return PingRequestView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::PING_REQUEST;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit PingRequestView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::PING_REQUEST) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class PingRequestBuilder : public LinkLayerPacketBuilder {
public:
    ~PingRequestBuilder() override = default;
    PingRequestBuilder() = default;
    PingRequestBuilder(PingRequestBuilder const&) = default;
    PingRequestBuilder(PingRequestBuilder&&) = default;
    PingRequestBuilder& operator=(PingRequestBuilder const&) = default;
        PingRequestBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::PING_REQUEST, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<PingRequestBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<PingRequestBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::PING_REQUEST) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class PingResponseView {
public:
    static PingResponseView Create(LinkLayerPacketView const& parent) {
        return PingResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::PING_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit PingResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::PING_RESPONSE) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class PingResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~PingResponseBuilder() override = default;
    PingResponseBuilder() = default;
    PingResponseBuilder(PingResponseBuilder const&) = default;
    PingResponseBuilder(PingResponseBuilder&&) = default;
    PingResponseBuilder& operator=(PingResponseBuilder const&) = default;
        PingResponseBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::PING_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<PingResponseBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<PingResponseBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::PING_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class RoleSwitchRequestView {
public:
    static RoleSwitchRequestView Create(LinkLayerPacketView const& parent) {
        return RoleSwitchRequestView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    PacketType GetType() const {
        return PacketType::ROLE_SWITCH_REQUEST;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit RoleSwitchRequestView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::ROLE_SWITCH_REQUEST) {
            return false;
        }
        
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;

    
};

class RoleSwitchRequestBuilder : public LinkLayerPacketBuilder {
public:
    ~RoleSwitchRequestBuilder() override = default;
    RoleSwitchRequestBuilder() = default;
    RoleSwitchRequestBuilder(RoleSwitchRequestBuilder const&) = default;
    RoleSwitchRequestBuilder(RoleSwitchRequestBuilder&&) = default;
    RoleSwitchRequestBuilder& operator=(RoleSwitchRequestBuilder const&) = default;
        RoleSwitchRequestBuilder(Address source_address, Address destination_address)
        : LinkLayerPacketBuilder(PacketType::ROLE_SWITCH_REQUEST, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}) {
    
}
    static std::unique_ptr<RoleSwitchRequestBuilder> Create(Address source_address, Address destination_address) {
    return std::make_unique<RoleSwitchRequestBuilder>(std::move(source_address), std::move(destination_address));
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::ROLE_SWITCH_REQUEST) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
    }

    size_t GetSize() const override {
        return 13;
    }

    
    
};

class RoleSwitchResponseView {
public:
    static RoleSwitchResponseView Create(LinkLayerPacketView const& parent) {
        return RoleSwitchResponseView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetStatus() const {
        _ASSERT_VALID(valid_);
        return status_;
    }
    
    PacketType GetType() const {
        return PacketType::ROLE_SWITCH_RESPONSE;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit RoleSwitchResponseView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::ROLE_SWITCH_RESPONSE) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 1) {
            return false;
        }
        status_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t status_{0};

    
};

class RoleSwitchResponseBuilder : public LinkLayerPacketBuilder {
public:
    ~RoleSwitchResponseBuilder() override = default;
    RoleSwitchResponseBuilder() = default;
    RoleSwitchResponseBuilder(RoleSwitchResponseBuilder const&) = default;
    RoleSwitchResponseBuilder(RoleSwitchResponseBuilder&&) = default;
    RoleSwitchResponseBuilder& operator=(RoleSwitchResponseBuilder const&) = default;
        RoleSwitchResponseBuilder(Address source_address, Address destination_address, uint8_t status)
        : LinkLayerPacketBuilder(PacketType::ROLE_SWITCH_RESPONSE, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), status_(status) {
    
}
    static std::unique_ptr<RoleSwitchResponseBuilder> Create(Address source_address, Address destination_address, uint8_t status) {
    return std::make_unique<RoleSwitchResponseBuilder>(std::move(source_address), std::move(destination_address), status);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::ROLE_SWITCH_RESPONSE) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(status_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 14;
    }

    
    uint8_t status_{0};
};

class LlPhyReqView {
public:
    static LlPhyReqView Create(LinkLayerPacketView const& parent) {
        return LlPhyReqView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetTxPhys() const {
        _ASSERT_VALID(valid_);
        return tx_phys_;
    }
    
    uint8_t GetRxPhys() const {
        _ASSERT_VALID(valid_);
        return rx_phys_;
    }
    
    PacketType GetType() const {
        return PacketType::LL_PHY_REQ;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LlPhyReqView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LL_PHY_REQ) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        tx_phys_ = span.read_le<uint8_t, 1>();
        rx_phys_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t tx_phys_{0};
    uint8_t rx_phys_{0};

    
};

class LlPhyReqBuilder : public LinkLayerPacketBuilder {
public:
    ~LlPhyReqBuilder() override = default;
    LlPhyReqBuilder() = default;
    LlPhyReqBuilder(LlPhyReqBuilder const&) = default;
    LlPhyReqBuilder(LlPhyReqBuilder&&) = default;
    LlPhyReqBuilder& operator=(LlPhyReqBuilder const&) = default;
        LlPhyReqBuilder(Address source_address, Address destination_address, uint8_t tx_phys, uint8_t rx_phys)
        : LinkLayerPacketBuilder(PacketType::LL_PHY_REQ, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), tx_phys_(tx_phys), rx_phys_(rx_phys) {
    
}
    static std::unique_ptr<LlPhyReqBuilder> Create(Address source_address, Address destination_address, uint8_t tx_phys, uint8_t rx_phys) {
    return std::make_unique<LlPhyReqBuilder>(std::move(source_address), std::move(destination_address), tx_phys, rx_phys);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LL_PHY_REQ) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(tx_phys_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(rx_phys_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 15;
    }

    
    uint8_t tx_phys_{0};
    uint8_t rx_phys_{0};
};

class LlPhyRspView {
public:
    static LlPhyRspView Create(LinkLayerPacketView const& parent) {
        return LlPhyRspView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetTxPhys() const {
        _ASSERT_VALID(valid_);
        return tx_phys_;
    }
    
    uint8_t GetRxPhys() const {
        _ASSERT_VALID(valid_);
        return rx_phys_;
    }
    
    PacketType GetType() const {
        return PacketType::LL_PHY_RSP;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LlPhyRspView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LL_PHY_RSP) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 2) {
            return false;
        }
        tx_phys_ = span.read_le<uint8_t, 1>();
        rx_phys_ = span.read_le<uint8_t, 1>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t tx_phys_{0};
    uint8_t rx_phys_{0};

    
};

class LlPhyRspBuilder : public LinkLayerPacketBuilder {
public:
    ~LlPhyRspBuilder() override = default;
    LlPhyRspBuilder() = default;
    LlPhyRspBuilder(LlPhyRspBuilder const&) = default;
    LlPhyRspBuilder(LlPhyRspBuilder&&) = default;
    LlPhyRspBuilder& operator=(LlPhyRspBuilder const&) = default;
        LlPhyRspBuilder(Address source_address, Address destination_address, uint8_t tx_phys, uint8_t rx_phys)
        : LinkLayerPacketBuilder(PacketType::LL_PHY_RSP, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), tx_phys_(tx_phys), rx_phys_(rx_phys) {
    
}
    static std::unique_ptr<LlPhyRspBuilder> Create(Address source_address, Address destination_address, uint8_t tx_phys, uint8_t rx_phys) {
    return std::make_unique<LlPhyRspBuilder>(std::move(source_address), std::move(destination_address), tx_phys, rx_phys);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LL_PHY_RSP) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(tx_phys_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(rx_phys_ & 0xff) << 0));
    }

    size_t GetSize() const override {
        return 15;
    }

    
    uint8_t tx_phys_{0};
    uint8_t rx_phys_{0};
};

class LlPhyUpdateIndView {
public:
    static LlPhyUpdateIndView Create(LinkLayerPacketView const& parent) {
        return LlPhyUpdateIndView(parent);
    }

    Address const& GetSourceAddress() const {
        _ASSERT_VALID(valid_);
        return source_address_;
    }
    
    Address const& GetDestinationAddress() const {
        _ASSERT_VALID(valid_);
        return destination_address_;
    }
    
    uint8_t GetPhyCToP() const {
        _ASSERT_VALID(valid_);
        return phy_c_to_p_;
    }
    
    uint8_t GetPhyPToC() const {
        _ASSERT_VALID(valid_);
        return phy_p_to_c_;
    }
    
    uint16_t GetInstant() const {
        _ASSERT_VALID(valid_);
        return instant_;
    }
    
    PacketType GetType() const {
        return PacketType::LL_PHY_UPDATE_IND;
    }
    
    
    std::string ToString() const {
        return "";
    }
    

    bool IsValid() const {
        return valid_;
    }

    pdl::packet::slice bytes() const {
        return bytes_;
    }

protected:
    explicit LlPhyUpdateIndView(LinkLayerPacketView const& parent)
          : bytes_(parent.bytes_) {
        valid_ = Parse(parent);
    }

    bool Parse(LinkLayerPacketView const& parent) {
        // Check validity of parent packet.
        if (!parent.IsValid()) {
            return false;
        }
        
        // Copy parent field values.
        source_address_ = parent.source_address_;
        destination_address_ = parent.destination_address_;
        
        if (parent.type_ != PacketType::LL_PHY_UPDATE_IND) {
            return false;
        }
        
        // Parse packet field values.
        pdl::packet::slice span = parent.payload_;
        if (span.size() < 4) {
            return false;
        }
        phy_c_to_p_ = span.read_le<uint8_t, 1>();
        phy_p_to_c_ = span.read_le<uint8_t, 1>();
        instant_ = span.read_le<uint16_t, 2>();
        return true;
    }

    bool valid_{false};
    pdl::packet::slice bytes_;
    Address source_address_;
    Address destination_address_;
    uint8_t phy_c_to_p_{0};
    uint8_t phy_p_to_c_{0};
    uint16_t instant_{0};

    
};

class LlPhyUpdateIndBuilder : public LinkLayerPacketBuilder {
public:
    ~LlPhyUpdateIndBuilder() override = default;
    LlPhyUpdateIndBuilder() = default;
    LlPhyUpdateIndBuilder(LlPhyUpdateIndBuilder const&) = default;
    LlPhyUpdateIndBuilder(LlPhyUpdateIndBuilder&&) = default;
    LlPhyUpdateIndBuilder& operator=(LlPhyUpdateIndBuilder const&) = default;
        LlPhyUpdateIndBuilder(Address source_address, Address destination_address, uint8_t phy_c_to_p, uint8_t phy_p_to_c, uint16_t instant)
        : LinkLayerPacketBuilder(PacketType::LL_PHY_UPDATE_IND, std::move(source_address), std::move(destination_address), std::vector<uint8_t>{}), phy_c_to_p_(phy_c_to_p), phy_p_to_c_(phy_p_to_c), instant_(instant) {
    
}
    static std::unique_ptr<LlPhyUpdateIndBuilder> Create(Address source_address, Address destination_address, uint8_t phy_c_to_p, uint8_t phy_p_to_c, uint16_t instant) {
    return std::make_unique<LlPhyUpdateIndBuilder>(std::move(source_address), std::move(destination_address), phy_c_to_p, phy_p_to_c, instant);
}

    void Serialize(std::vector<uint8_t>& output) const override {
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(PacketType::LL_PHY_UPDATE_IND) << 0));
        source_address_.Serialize(output);
        destination_address_.Serialize(output);
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(phy_c_to_p_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint8_t, 1>(output, (static_cast<uint8_t>(phy_p_to_c_ & 0xff) << 0));
        pdl::packet::Builder::write_le<uint16_t, 2>(output, (static_cast<uint16_t>(instant_ & 0xffff) << 0));
    }

    size_t GetSize() const override {
        return 17;
    }

    
    uint8_t phy_c_to_p_{0};
    uint8_t phy_p_to_c_{0};
    uint16_t instant_{0};
};
}  // model::packets
