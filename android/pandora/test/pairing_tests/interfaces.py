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

from abc import ABC, abstractmethod
from typing import final

class IBase(ABC):

    def __init__(self, dut, ref):
        self._dut = dut
        self._ref = ref

    @final
    def dut(self):
        return self._dut

    @final
    def ref(self):
        return self._ref

    @abstractmethod
    async def start(self):
        pass

    @abstractmethod
    async def verify_success(self):
        pass

    @abstractmethod
    async def verify_failure(self):
        pass

class RoleSetting:

    @final
    @property
    def initiator(self):
        if not hasattr(self, '_initiator'):
            raise ValueError('initiator not set')
        return self._initiator

    @final
    @initiator.setter
    def initiator(self, init_device):
        self._initiator = init_device

    @final
    @property
    def responder(self):
        if not hasattr(self, '_responder'):
            raise ValueError('responder not set')
        return self._responder

    @final
    @responder.setter
    def responder(self, resp_device):
        self._responder = resp_device

class IAclConnectionBuilder(IBase, RoleSetting):

    @abstractmethod
    async def initiator_connection(self):
        pass

    @abstractmethod
    async def responder_connection(self):
        pass

class IPairing(IBase, RoleSetting):

    @abstractmethod
    async def accept_pairing(self):
        pass

    @abstractmethod
    async def reject_pairing(self):
        pass

class IServiceAccess(IBase, RoleSetting):
    pass
