#!/bin/bash

# Usage:
# acloud-pull-logs.sh [path to expect script]

export DEFAULT_EXPECT_SCRIPT=packages/modules/Bluetooth/android/pandora/server/configs/acloud-pull-all.exp
export EXPECT_SCRIPT="${1:-$DEFAULT_EXPECT_SCRIPT}"

# Setup.
# Resort to using OS's ps because toybox's ps implementation doesn't correcly handle `ps aux`.
export PATH=/usr/bin:$PATH

# Find atest-output directory.
# Start with atest-output dir matching pattern /tmp/tf-workfolder*.
export ATEST_OUTPUT_DIR=$(ls -td /tmp/stage-android-build-api/*/*/stub/inv_* | head -n 1)
if [[ "${ATEST_OUTPUT_DIR}" == "" ]];
then
  # Fall back on atest-output dir matching pattern /tmp/atest_result/*_*_*.
  export ATEST_OUTPUT_DIR=$(ls -td /tmp/atest_result/*_*_* | head -n 1)
fi
if [[ "${ATEST_OUTPUT_DIR}" == "" ]];
then
  echo "Could not determine the output directory for atest."
  exit 1
fi
echo "Contents of atest-output dir (${ATEST_OUTPUT_DIR}):"
ls -l "${ATEST_OUTPUT_DIR}"

# Make an acloud-pull directory in the atest's output directory.
export ATEST_OUTPUT_ACLOUD_DIR="${ATEST_OUTPUT_DIR}/acloud_pull"
mkdir -p ${ATEST_OUTPUT_ACLOUD_DIR}
chmod +w ${ATEST_OUTPUT_ACLOUD_DIR}
# Create a log for expect (to run acloud pull).
export EXPECT_LOG="${ATEST_OUTPUT_ACLOUD_DIR}/expect.log"
touch "${EXPECT_LOG}"
ls -l "${EXPECT_LOG}"
echo "chmod +666 ${EXPECT_LOG}"
chmod +666 "${EXPECT_LOG}"
ls -l "${EXPECT_LOG}"

# Run acloud pull on Cuttlefish.
# expect packages/modules/Bluetooth/android/pandora/server/configs/acloud-pull-all.exp "${EXPECT_LOG}"
echo "Expect script: ${EXPECT_SCRIPT}"
echo "Expect log: ${EXPECT_LOG}"
which expect
expect ${EXPECT_SCRIPT} "${EXPECT_LOG}"
echo "cat ${EXPECT_LOG}"
cat "${EXPECT_LOG}"

# Location where acloud-pulled files land on the host. Sample output to extract:
# acloud-pull output dir: /tmp/ins-f8a61b52-9920406-aosp-cf-x86-64-phone-userdebug
export ACLOUD_PULL_DIR="$(grep "acloud-pull output dir:" "${EXPECT_LOG}" | sed "s/acloud-pull output dir: \(.*\)/\1/g" | tr -d ' \n\r')"

# Move files from acloud-pull dir to test-output dir.
echo "Will move files from directory [${ACLOUD_PULL_DIR}] to directory [${ATEST_OUTPUT_ACLOUD_DIR}]."
mv ${ACLOUD_PULL_DIR}/* ${ATEST_OUTPUT_ACLOUD_DIR}
rmdir ${ACLOUD_PULL_DIR}

