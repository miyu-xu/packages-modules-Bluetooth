#include <fuzzer/FuzzedDataProvider.h>
#include <gtest/gtest.h>

#include "avrcp_browse_packet.h"
#include "packet_test_helper.h"

namespace bluetooth {

namespace avrcp {

// ConcretePacket for testing purposes
class ConcretePacket : public ::bluetooth::Packet {
 public:
  bool IsValid() const override {
    // Implement the IsValid method logic
    return true;  // Replace with your implementation
  }

  std::string ToString() const override {
    // Implement the ToString method logic
    return "ConcretePacket";  // Replace with your implementation
  }

  std::pair<size_t, size_t> GetPayloadIndecies() const override {
    // Implement the GetPayloadIndecies method logic
    return {0, 0};  // Replace with your implementation
  }
};

using TestBrowsePacket = TestPacketType<BrowsePacket>;

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  if (size < 1) {
    // Not enough data to fuzz
    return 0;
  }

  FuzzedDataProvider stream(data, size);

  // Creating a dummy packet using a shared_ptr to the concrete subclass
  std::shared_ptr<ConcretePacket> dummy_packet(new ConcretePacket);

  auto test_packet = TestBrowsePacket::Make(dummy_packet);

  // Fuzzing the Parse method
  auto fuzzed_packet = TestBrowsePacket::Parse(dummy_packet);

  return 0;
}

}  // namespace avrcp
}  // namespace bluetooth
