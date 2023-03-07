#!/usr/bin/env bash

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

function _check {
  ec="$?"
  if [[ "${ec}" -eq 0 ]]; then echo -e ": OK"
  else echo -e ": KO\n$1"; return "${ec}"; fi
}

case "$1" in
  'format') shift
    echo -n "-- Install format python remote dependencies"; _check "$(
      2>&1 pip install \
        'black==22.10.0' \
        'isort==5.12.0'
    )" || exit
    echo -n "-- Format"; _check "$(
      2>&1 black -S -l 119 "$@" "${_TEST_FILES[@]}"
      2>&1 isort --profile black -l 119 --ds --lbt 1 --ca "$@" "${_TEST_FILES[@]}"
    )" || exit
  ;;
  'lint') shift
    echo -n "-- Install lint python remote dependencies"; _check "$(
      2>&1 pip install \
        'grpcio==1.51.1' \
        'protobuf==4.21.0' \
        'pyright==1.1.296' \
        'mypy==1.0' \
        'types-protobuf==4.21.0.3'
    )" || exit
    export PYTHONPATH="$(IFS=:; echo "${_PANDORA_PYTHON_PATHS[*]}")"
    echo -n "-- Lint";  _check "$(
      2>&1 pyright \
        -p "${_TEST_ROOT}" \
        "$@" "${_TEST_FILES[@]}"
      2>&1 mypy \
        --pretty --show-column-numbers --strict --no-warn-unused-ignores --ignore-missing-imports \
        "$@" "${_TEST_FILES[@]}"
    )" || exit
  ;;
  'run') shift
    tradefed.sh \
      run commandAndExit template/local_min --template:map test=avatar --log-level INFO \
      "$@"
  ;;
  *)
    echo "$1: invalid command"
    exit 1
  ;;
esac
