
#include <log/log.h>
#include "bluetooth/logger.h"

namespace logger {

void vlog(Level level, char const *tag, char const *file_name, int line,
          std::string_view fmt, std::format_args vargs) {
    // Check if log is enabled.
    if (!__android_log_is_loggable(level, tag,
            __android_log_is_loggable(level, "bluetooth", ANDROID_LOG_INFO))) {
      return;
    }

    // Format to stack buffer.
    truncating_buffer<logger::kBufferSize> buffer;
    auto result = std::vformat_to(std::back_insert_iterator(buffer), fmt, vargs);
    buffer.buffer[buffer.len] = '\0';

    // Send message to liblog.
    struct __android_log_message message = {
        .struct_size = sizeof(__android_log_message),
        .buffer_id = LOG_ID_MAIN,
        .priority = level,
        .tag = LOG_TAG,
        .file = file_name,
        .line = line,
        .message = buffer.buffer,
    };
    __android_log_write_log_message(&message);
}

}  // logger
