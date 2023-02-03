#!/bin/bash

destdir="$1"

header_dirs=(
    base
    base/allocator
    base/allocator/partition_allocator
    base/allocator/partition_allocator/partition_alloc_base
    base/allocator/partition_allocator/partition_alloc_base/augmentations
    base/allocator/partition_allocator/starscan
    base/containers
    base/debug
    base/files
    base/functional
    base/hash
    base/i18n
    base/json
    base/memory
    base/message_loop
    base/metrics
    base/numerics
    base/posix
    base/process
    base/profiler
    base/ranges
    base/strings
    base/synchronization
    base/system
    base/task
    base/task/common
    base/task/sequence_manager
    base/task/thread_pool
    base/test
    base/third_party/icu
    base/third_party/nspr
    base/third_party/valgrind
    base/threading
    base/time
    base/timer
    base/trace_event
    base/trace_event/common
    base/types
    build
    components/policy
    components/policy/core/common
    testing/gmock/include/gmock
    testing/gtest/include/gtest
    dbus
    third_party/abseil-cpp/absl/base
    third_party/abseil-cpp/absl/functional
    third_party/abseil-cpp/absl/synchronization
    third_party/abseil-cpp/absl/types
    third_party/abseil-cpp/absl/utility
    third_party/libevent/
    third_party/perfetto/include/perfetto/tracing/
    third_party/perfetto/include/perfetto/protozero/
    third_party/perfetto/protos/perfetto/trace/track_event/
)

# Install header files.
for d in "${header_dirs[@]}" ; do
  mkdir -p "${destdir}/usr/include/libchrome/${d}"
  cp libchrome/"${d}"/*.h "${destdir}/usr/include/libchrome/${d}"
done

# Install all buildflags.h.
for f in $(cd out/Release/gen/libchrome && find -type f -name "*buildflags.h"); do
  mkdir -p "${destdir}/usr/include/libchrome/$(dirname $f)"
  cp out/Release/gen/libchrome/"${f}" "${destdir}/usr/include/libchrome/${f}"
done
