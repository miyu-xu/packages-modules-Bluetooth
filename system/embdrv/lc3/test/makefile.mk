#
# Copyright 2021 Google, Inc.
#

TEST_DIR := test

.PHONY: test test-clean

test:
	$(V)cd $(TEST_DIR) && python3 setup.py && python3 run.py

test-clean:
	$(V)cd $(TEST_DIR) && python3 setup.py clean > /tmp/zero

clean-all: test-clean
