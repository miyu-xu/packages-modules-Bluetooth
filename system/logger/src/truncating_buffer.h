/*
 * Copyright 2023 The Android Open Source Project
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

#include <cstddef>

namespace logger {

/// Truncating write buffer.
///
/// This buffer can be used with `std::back_insert_iterator` to create
/// an output iterator. All write actions beyond the maximum length of
/// the buffer are silently ignored.
template <int buffer_size>
struct truncating_buffer {
    using value_type = char;

    void push_back(char c) {
        if (len < buffer_size - 1) {
            buffer[len++] = c;
        }
    }

    char buffer[buffer_size];
    size_t len{0};
};

}  // namespace logger
