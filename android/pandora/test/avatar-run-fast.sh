#!/usr/bin/env bash
pip install grpcio==1.51.1 cryptography==35 appdirs==1.4.4
adb install -r -g "${ANDROID_TARGET_OUT_TESTCASES}/PandoraServer/x86_64/PandoraServer.apk"
trap 'adb forward --remove tcp:6211' SIGINT
adb forward tcp:6211 tcp:6211
{
  trap 'kill 0' SIGINT
  adb shell nc -L -p 6211 nc 192.168.97.1 7300 &
} >/dev/null 2>&1
pid=$!
ec=0; (
  "${ANDROID_HOST_OUT_TESTCASES}/avatar/x86_64/avatar" "$@"
) || ec="$?"
kill "${pid}"
adb forward --remove tcp:6211
exit "${ec}"
