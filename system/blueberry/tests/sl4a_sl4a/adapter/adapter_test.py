#!/usr/bin/env python3
#
#   Copyright 2022 - The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

from blueberry.tests.sl4a_sl4a.lib import sl4a_sl4a_base_test

import time


class AdapterTest(sl4a_sl4a_base_test.Sl4aSl4aBaseTestClass):

    def test_enable_disable(self):
        for _ in range(20):
            self.dut.sl4a.bluetoothToggleState(False)
            time.sleep(5)
            self.dut.sl4a.bluetoothToggleState(True)
            time.sleep(5)
