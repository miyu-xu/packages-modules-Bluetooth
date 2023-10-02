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

from apps import pandora_server
from bumble import pandora as bumble_server

from bumble_experimental.asha import AshaService
from bumble_experimental.dck import DckService
from bumble_experimental.gatt import GATTService

from pandora_experimental.asha_grpc_aio import add_AshaServicer_to_server
from pandora_experimental.dck_grpc_aio import add_DckServicer_to_server
from pandora_experimental.gatt_grpc_aio import add_GATTServicer_to_server

if __name__ == '__main__':
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_AshaServicer_to_server(AshaService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_DckServicer_to_server(DckService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_GATTServicer_to_server(GATTService(bumble.device), server))
    pandora_server.main()  # pylint: disable=no-value-for-parameter
