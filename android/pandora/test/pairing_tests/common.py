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

from avatar import (
    PandoraDevice,
)

from abc import ABC, abstractmethod

class IAclConnectionHarness(ABC):

    @property
    @abstractmethod
    def initiator(self) -> PandoraDevice:
        pass

    @property
    @abstractmethod
    def responder(self) -> PandoraDevice:
        pass

    @abstractmethod
    async def start_connection(self) -> None:
        pass

    @abstractmethod
    async def verify_connection_success(self) -> None:
        pass

    @abstractmethod
    async def verify_connection_failure(self) -> None:
        pass

class AclConnectionHarnessBase(IAclConnectionHarness):
    def __init__(self, initiator: PandoraDevice, responder: PandoraDevice):
        self._initiator = initiator
        self._responder = responder

    @property
    def initiator(self) -> PandoraDevice:
        return self._initiator

    @property
    def responder(self) -> PandoraDevice:
        return self._responder

class IServiceAccessHarness(ABC):

    @property
    @abstractmethod
    def AclConnectionHarness(self) -> IAclConnectionHarness:
        pass

    @property
    @abstractmethod
    def initiator(self) -> PandoraDevice:
        pass

    @property
    @abstractmethod
    def responder(self) -> PandoraDevice:
        pass

    @abstractmethod
    async def start_service_access(self):
        pass

    @abstractmethod
    async def verify_access_success(self):
        pass

    @abstractmethod
    async def verify_access_failure(self):
        pass

class IPairingHarness(ABC):

    @property
    @abstractmethod
    def AclConnectionHarness(self) -> IAclConnectionHarness:
        pass

    @property
    @abstractmethod
    def initiator(self) -> PandoraDevice:
        pass

    @property
    @abstractmethod
    def responder(self) -> PandoraDevice:
        pass

    @abstractmethod
    async def prepare_pairing(self):
        pass

    @abstractmethod
    async def start_pairing(self):
        pass

    @abstractmethod
    async def accept_pairing(self):
        pass

    @abstractmethod
    async def reject_pairing(self):
        pass


    @abstractmethod
    async def verify_pairing_success(self):
        pass

    @abstractmethod
    async def post_pairing_success(self):
        pass

    @abstractmethod
    async def verify_pairing_failure(self):
        pass

    @abstractmethod
    async def post_pairing_failure(self):
        pass

class PairingHarnessBase(IPairingHarness):

    async def prepare_pairing(self):
        init_pairing_event_stream = self.initiator.aio.security.OnPairing()
        setattr(self.initiator, 'pairing_event_stream', init_pairing_event_stream)
        resp_pairing_event_stream = self.responder.aio.security.OnPairing()
        setattr(self.responder, 'pairing_event_stream', resp_pairing_event_stream)

class IDedicatedPairingHarness(PairingHarnessBase):

    @abstractmethod
    def __init__(self, acl_connection_harness: IAclConnectionHarness):
        pass

    @abstractmethod
    @property
    def acl_connection_harness(self) -> IAclConnectionHarness:
        pass

class DedicatedPairingHarnessBase(IDedicatedPairingHarness):

    def __init__(self, acl_connection_harness: IAclConnectionHarness):
        self._acl_connection_harness = acl_connection_harness

    @property
    def acl_connection_harness(self) -> IAclConnectionHarness:
        return self._acl_connection_harness

class IGeneralPairingHarness(PairingHarnessBase):

    @abstractmethod
    def __init__(self, service_access_harness: IServiceAccessHarness):
        pass

    @abstractmethod
    @property
    def service_access_harness(self) -> IServiceAccessHarness:
        pass

class GeneralPairingHarnessBase(IGeneralPairingHarness):

    def __init__(self, service_access_harness: IServiceAccessHarness):
        self._service_access_harness = service_access_harness

    @property
    def service_access_harness(self) -> IServiceAccessHarness:
        return self._service_access_harness

class IPairingTestHarness(ABC):

    @abstractmethod
    def __init__(self, pairing_harness: IPairingHarness):
        pass

    @abstractmethod
    @property
    def pairing_harness(self) -> IPairingHarness:
        pass

    @abstractmethod
    async def do_accept_test(self):
        pass

    @abstractmethod
    async def do_reject_test(self):
        pass

    @abstractmethod
    async def post_pairing_success(self):
        pass

    @abstractmethod
    async def post_pairing_failure(self):
        pass


class PairingTestHarnessBase(IPairingTestHarness):

    def __init__(self, pairing_harness: IPairingHarness):
        self._pairing_harness = pairing_harness

    @property
    def pairing_harness(self) -> IPairingHarness:
        return self._pairing_harness

    async def do_accept_test(self):
        pairing_harness = self.pairing_harness()
        await pairing_harness.prepare_pairing()
        await pairing_harness.start_pairing()
        await pairing_harness.accept_pairing()
        await pairing_harness.verify_pairing_success()
        await pairing_harness.post_pairing_success()

    async def do_reject_test(self):
        pairing_harness = self.pairing_harness()
        await pairing_harness.prepare_pairing()
        await pairing_harness.start_pairing()
        await pairing_harness.reject_pairing()
        await pairing_harness.verify_pairing_failure()
        await pairing_harness.post_pairing_failure()

    async def post_pairing_success(self):
        pass

    async def post_pairing_failure(self):
        pass

class ClassicDDPairingHarness_Ref_Init_ACL(DedicatedPairingHarnessBase):
    pass

class ClassicDDPairingHarness_Android_Init_ACL(DedicatedPairingHarnessBase):
    pass

class ClassicGeneralPairingHarness(GeneralPairingHarnessBase):
    pass
