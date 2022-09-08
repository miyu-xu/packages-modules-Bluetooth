#include <vector>

#include "hci/hci_packets.h"

std::vector<uint8_t> FilterSignificantData(std::vector<bluetooth::hci::LengthAndData> data);