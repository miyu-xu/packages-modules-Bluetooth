// This header is currently needed for hci_packets.h
// FIXME: Change hci_packets.h to not depend on os/log.h
//        and remove this.

#ifndef LOG_TAG
#define LOG_TAG nullptr
#endif
#include "include/log.h"
