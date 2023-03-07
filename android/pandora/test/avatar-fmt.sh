#!/usr/bin/env bash

function _check {
  ec="$?"
  if [[ "${ec}" -eq 0 ]]; then echo -e ": OK"
  else echo -e ": KO\n$1"; return "${ec}"; fi
}

echo -n "-- Install format python remote dependencies"; _check "$(
  2>&1 pip install \
    'black==22.10.0' \
    'isort==5.12.0'
)" || exit

_BT_ROOT="${ANDROID_BUILD_TOP}/packages/modules/Bluetooth"
_ROOT="${_BT_ROOT}/android/pandora/test"

argv=("${@:2}" "${_ROOT}/main.py" "${_ROOT}/"*_test.py)
echo -n "-- Format"; _check "$(
  2>&1 black -S -l 119 "${argv[@]}"
  2>&1 isort --profile black -l 119 --ds --lbt 1 --ca "${argv[@]}"
)" || exit
