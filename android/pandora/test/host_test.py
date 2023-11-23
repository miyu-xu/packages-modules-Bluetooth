# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import avatar
import avatar.cases.host_test
import logging

from mobly import test_runner


class HostTest(avatar.cases.host_test.HostTest):  # type: ignore[misc]
    """
    This extend the default Avatar Host test cases.
    """

    def test_reset(self) -> None:
        """
        Test the `host.Reset` Pandora API:
        1. Make a BR/EDR connection.
        2. Reset the DUT.
        3. Ensure the connection has been disconnected.
        """

        # 1. Make a BR/EDR connection.
        connection = self.dut.host.Connect(address=self.ref.address).connection
        assert connection

        # 2. Reset the DUT.
        self.dut.host.Reset()

        # 3. Ensure the connection has been disconnected.
        # Use a small timeout to make sure it's been disconnected from the
        # `Reset` procedure but not the Page timeout.
        self.dut.host.WaitDisconnection(connection=connection, timeout=0.5)

    def test_remote_reset(self) -> None:
        """
        Test the `host.Reset` Pandora API:
        1. Make a BR/EDR connection.
        2. Reset the REF.
        3. Ensure the connection has been disconnected.
        """

        # 1. Make a BR/EDR connection.
        connection = self.dut.host.Connect(address=self.ref.address).connection
        assert connection

        # 2. Reset the REF.
        self.ref.host.Reset()

        # 3. Ensure the connection has been disconnected.
        # Use a small timeout to make sure it's been disconnected from the
        # remote `Reset` procedure but not the Page timeout.
        self.dut.host.WaitDisconnection(connection=connection, timeout=0.5)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
