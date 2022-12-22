# Copyright 2022 Google LLC
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
"""Utils functions.

Facilitates the use of common functions across different profiles.
"""

import threading
from pandora_experimental.security_grpc import Security


class Utils:
    """
    auto_confirm_pairing_requests : Function for
    Accepting/Confirming Pairing Request <SSP>.

    """

    def _auto_confirm_pairing_requests(self, channel, times=None):

        def task():
            self.security = Security(channel)
            cnt = 0
            pairing_events = self.security.OnPairing()
            for event in pairing_events:
                if event.WhichOneof('method') in {"just_works", "numeric_comparison"}:
                    if times is None or cnt < times:
                        cnt += 1
                        pairing_events.send(event=event, confirm=True)

        threading.Thread(target=task).start()
