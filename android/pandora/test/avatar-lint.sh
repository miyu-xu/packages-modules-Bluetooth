#!/usr/bin/env bash

function _check {
  ec="$?"
  if [[ "${ec}" -eq 0 ]]; then echo -e ": OK"
  else echo -e ": KO\n$1"; return "${ec}"; fi
}

echo -n "-- Install lint python remote dependencies"; _check "$(
  2>&1 pip install \
    'grpcio==1.51.1' \
    'protobuf==4.21.0' \
    'pyright==1.1.296' \
    'mypy==1.0' \
    'types-protobuf==4.21.0.3'
)" || exit

_BT_ROOT="${ANDROID_BUILD_TOP}/packages/modules/Bluetooth"
_ROOT="${_BT_ROOT}/android/pandora/test"

_PANDORA_PYTHON_PATHS=(
  "${_BT_ROOT}/pandora/server/"
  "${ANDROID_BUILD_TOP}/external/pandora/avatar/"
  "${ANDROID_BUILD_TOP}/external/python/bumble/"
  "${ANDROID_BUILD_TOP}/external/python/mobly/"
  "${ANDROID_BUILD_TOP}/external/python/pyee/"
  "${ANDROID_BUILD_TOP}/out/soong/.intermediates/external/pandora/bt-test-interfaces/python/pandora-python-gen-src/gen/",
  "${ANDROID_BUILD_TOP}/out/soong/.intermediates/packages/modules/Bluetooth/pandora/interfaces/python/pandora_experimental-python-gen-src/gen/"
)

argv=("${@:2}" "${_ROOT}/main.py" "${_ROOT}/"*_test.py)
echo -n "-- Lint"; PYTHONPATH="${PYTHONPATH}:$(IFS=:; echo "${_PANDORA_PYTHON_PATHS[*]}")" _check "$(
  2>&1 mypy --pretty --show-column-numbers --strict --no-warn-unused-ignores --ignore-missing-imports "${argv[@]}"
  2>&1 pyright -p "${_ROOT}" "${argv[@]}"
)" || exit
