#!/usr/bin/env bash
avatar-fmt
avatar-lint
echo "-- Run"
tradefed.sh run commandAndExit template/local_min --template:map test=avatar "$@"
