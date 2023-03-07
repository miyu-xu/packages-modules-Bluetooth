#!/usr/bin/env bash
_USAGE="avatar.sh: [-h,--help] {format,lint,run,help,} ..."

_BT_ROOT="${ANDROID_BUILD_TOP}/packages/modules/Bluetooth"
_TEST_ROOT="${_BT_ROOT}/android/pandora/test"
_TEST_FILES=("${_TEST_ROOT}/main.py" "${_TEST_ROOT}/"*_test.py)

_PANDORA_PYTHON_PATHS=(
  "${_BT_ROOT}/pandora/server/"
  "${ANDROID_BUILD_TOP}/external/pandora/avatar/"
  "${ANDROID_BUILD_TOP}/external/python/bumble/"
  "${ANDROID_BUILD_TOP}/external/python/mobly/"
  "${ANDROID_BUILD_TOP}/external/python/pyee/"
  "${ANDROID_BUILD_TOP}/out/soong/.intermediates/external/pandora/bt-test-interfaces/python/pandora-python-gen-src/gen/"
  "${ANDROID_BUILD_TOP}/out/soong/.intermediates/packages/modules/Bluetooth/pandora/interfaces/python/pandora_experimental-python-gen-src/gen/"
)

case "$1" in
  'format') shift
    pip install \
      'black==22.10.0' \
      'isort==5.12.0'
    black -S -l 119 "$@" "${_TEST_FILES[@]}"
    isort --profile black -l 119 --ds --lbt 1 --ca "$@" "${_TEST_FILES[@]}"
  ;;
  'lint') shift
    pip install \
      'grpcio==1.51.1' \
      'protobuf==4.21.0' \
      'pyright==1.1.296' \
      'mypy==1.0' \
      'types-protobuf==4.21.0.3'
    export PYTHONPATH="$(IFS=:; echo "${_PANDORA_PYTHON_PATHS[*]}"):${PYTHONPATH}"
    pyright \
      -p "${_TEST_ROOT}" \
      "$@" "${_TEST_FILES[@]}"
    mypy \
      --pretty --show-column-numbers --strict --no-warn-unused-ignores --ignore-missing-imports \
      "$@" "${_TEST_FILES[@]}"
  ;;
  'run') shift
    tradefed.sh \
      run commandAndExit template/local_min --template:map test=avatar --log-level INFO \
      "$@"
  ;;
  'help'|'--help'|'-h') shift
    echo "${_USAGE}"
    exit 0
  ;;
  '')
    echo "no command provided (try help)"
    echo "${_USAGE}"
    exit 1
  ;;
  *)
    echo "$1: invalid command (try help)"
    echo "${_USAGE}"
    exit 1
  ;;
esac
