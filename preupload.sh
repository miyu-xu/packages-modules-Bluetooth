#!/bin/bash

# A helper script to sequentially call presubmit hooks that acquire git lock
# to read git tree. Repo hooks by default executes in parallel which will
# cause race condition, so we have to use one repo hook that calls them
# sequentially
#
# Usage: preupload.sh {1} {2} {3}
#    1: ${REPO_ROOT} as defined by the presubmit script
#    2: ${PREUPLOAD_COMMIT} as defined by the presubmit script
#    3: ${PREUPLOAD_FILES} as defined by the presubmit script
#
set -e

${1}/prebuilts/checkstyle/checkstyle.py --sha ${2} \
    --config_xml checkstyle.xml \
    -fw android/app/src/com/android/bluetooth/ \
    android/app/lib/mapapi/com/android/bluetooth/mapapi/ \
    android/app/tests/src/com/android/bluetooth/ \
    framework/ \
    service/
${1}/tools/repohooks/tools/clang-format.py --commit ${2} --style file --extensions c,h,cc,cpp
${1}/frameworks/base/tools/aosp/aosp_sha.sh ${2} ${3}
