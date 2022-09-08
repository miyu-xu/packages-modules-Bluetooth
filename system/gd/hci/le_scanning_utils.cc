#include "le_scanning_utils.h"

std::vector<uint8_t> FilterSignificantData(std::vector<bluetooth::hci::LengthAndData> data) {
  auto out = std::vector<uint8_t>{};
  for (const auto& datum : data) {
    if (!datum.data_.empty()) {
      out.push_back(static_cast<uint8_t>(datum.data_.size()));
      out.insert(datum.data_.end(), datum.data_.begin(), datum.data_.end());
    }
  }
  return out;
}
