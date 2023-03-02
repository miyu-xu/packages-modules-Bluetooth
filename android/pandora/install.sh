#!/usr/bin/env bash
pip install \
  -e ../../pandora/server/ \
  -e ../../pandora/interfaces/python/ \
  -e ../../../../../external/pandora/bt-test-interfaces/python/ \
  -e ../../../../../external/pandora/avatar/[dev] \
  -e ../../../../../external/python/bumble/ \
  -e ../../../../../external/python/mobly/ \
  -e ../../../../../external/python/pyee/
