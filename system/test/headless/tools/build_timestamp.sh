#!/bin/bash

/usr/bin/cat<<EOF
#pragma once

namespace bluetooth::test::headless {
constexpr char kBuildTime[]="$(date -Iseconds)";
}  // namespace bluetooth::test::headless
EOF
