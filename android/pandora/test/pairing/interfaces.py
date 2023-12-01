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
    @property
    def dut(self):
        return self._dut

    @final
    @property
    def ref(self):
        return self._ref

    @abstractmethod
    async def start(self):
        pass

    @abstractmethod
    async def wait_for_completion(self):
        pass

    @abstractmethod
    async def verify_success(self):
        pass

    @abstractmethod
    async def verify_failure(self):
        pass

    @abstractmethod
    async def cleanup(self):
        pass


class IRoleSetting(ABC):
    @abstractmethod
    async def verify_role_setup(self):
        pass

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


class IAclConnectionBuilder(IBase, IRoleSetting):
    def verify_role_setup(self):
        pass

    @final
    @property
    def initiator_connection(self):
        if not hasattr(self, '_init_connection'):
            raise ValueError("wait_for_completion not called yet")

        return self._init_connection

    @final
    @property
    def responder_connection(self):
        if not hasattr(self, '_resp_connection'):
            raise ValueError("wait_for_completion not called yet")

        return self._resp_connection


class IPairingProcessor(ABC):
    def __init__(self, init_event_stream, resp_event_stream):
        self._init_pairing_event_stream = init_event_stream
        self._resp_pairing_event_stream = resp_event_stream

    @abstractmethod
    async def accept_pairing(self):
        pass

    @abstractmethod
    async def reject_pairing(self):
        pass


class IPairingBuilder(IBase, IRoleSetting):
    def verify_role_setup(self):
        pass

    @abstractmethod
    async def accept_pairing(self):
        pass

    @abstractmethod
    async def reject_pairing(self):
        pass


class IServiceAccessor(IBase, IRoleSetting):
    def verify_role_setup(self):
        pass

    def __init__(self, dut, ref, acl_connection_builder):
        self._dut = dut
        self._ref = ref
        self._acl_connection_builder = acl_connection_builder

    @final
    @property
    def acl_connection_builder(self):
        return self._acl_connection_builder
