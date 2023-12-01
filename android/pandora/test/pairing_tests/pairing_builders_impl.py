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


import asyncio
import logging

from avatar import BumblePandoraDevice
from pairing_tests.interfaces import IAclConnectionBuilder, IPairingBuilder, IPairingProcessor, IServiceAccessor
from pandora.security_pb2 import LEVEL2
from typing import Type


class DedicatedPairingBuilderRefInitAcl(IPairingBuilder):
    '''
    Setting:

    Ref initiate ACL connection

    Ref or Android initiate pairing
    '''

    def verify_role_setup(self):
        assert_true(isinstance(_acl_connection_builder.initiator, BumblePandoraDevice))

    def __init__(
        self, dut, ref, acl_connection_builder: IAclConnectionBuilder, pair_processor_cls: Type[IPairingProcessor]
    ):
        self._dut = dut
        self._ref = ref
        self._acl_connection_builder = acl_connection_builder
        self._pair_processor_cls = pair_processor_cls

    async def start(self):
        self._init_pairing_event_stream = self.initiator.aio.security.OnPairing()
        self._resp_pairing_event_stream = self.responder.aio.security.OnPairing()
        self._pairing_processor = self._pair_processor_cls(
            self._init_pairing_event_stream, self._resp_pairing_event_stream
        )

        # start the acl connection and wait until the connection is established
        await self._acl_connection_builder.start()
        await self._acl_connection_builder.wait_for_completion()
        await self._acl_connection_builder.verify_success()

        if self.initiator == self._acl_connection_builder.initiator:
            init_conn = self._acl_connection_builder.initiator_connection
            resp_conn = self._acl_connection_builder.responder_connection
        else:
            init_conn = self._acl_connection_builder.responder_connection
            resp_conn = self._acl_connection_builder.initiator_connection

        async def start_pairing():
            return await asyncio.gather(
                self.initiator.aio.security.Secure(connection=init_conn.connection, classic=LEVEL2),
                self.responder.aio.security.WaitSecurity(connection=resp_conn.connection, classic=LEVEL2),
            )

        self._pairing_tsk = asyncio.create_task(start_pairing())

    async def accept_pairing(self):
        await self._pairing_processor.accept_pairing()

    async def reject_pairing(self):
        await self._pairing_processor.reject_pairing()

    async def wait_for_completion(self):
        self.init_sec_res, self.resp_sec_res = await asyncio.wait_for(self._pairing_tsk, timeout=30.0)
        logging.debug(
            f"Pairing result(init/resp): {self.init_sec_res.result_variant()}/{self.resp_sec_res.result_variant()}"
        )
        # TODO: verify pairing results

    async def verify_success(self):
        pass

    async def verify_failure(self):
        pass

    async def cleanup(self):
        pass


class DedicatedPairingBuilderDutInitAcl(IPairingBuilder):
    '''
    Setting:

    Android Initiate an ACL connection to the ref (bumble),
    The implementation of Connect API on Android initiates pairing
    '''

    def verify_role_setup(self):
        assert_true(isinstance(_acl_connection_builder.responder, BumblePandoraDevice))

    def __init__(
        self, dut, ref, acl_connection_builder: IAclConnectionBuilder, pair_processor_cls: Type[IPairingProcessor]
    ):
        self._dut = dut
        self._ref = ref
        self._acl_connection_builder = acl_connection_builder
        self._pair_processor_cls = pair_processor_cls

    async def start(self):
        self._init_pairing_event_stream = self.initiator.aio.security.OnPairing()
        self._resp_pairing_event_stream = self.responder.aio.security.OnPairing()
        self._pairing_processor = self._pair_processor_cls(
            self._init_pairing_event_stream, self._resp_pairing_event_stream
        )

        await self._acl_connection_builder.start()

    async def accept_pairing(self):
        await self._pairing_processor.accept_pairing()

    async def reject_pairing(self):
        await self._pairing_processor.reject_pairing()

    async def wait_for_completion(self):
        await self._acl_connection_builder.wait_for_completion()

    async def verify_success(self):
        await self._acl_connection_builder.verify_success()
        # todo: verify that the link secure returns failure

    async def verify_failure(self):
        await self._acl_connection_builder.verify_failure()
        # todo: verify that the link secure returns failure

    async def cleanup(self):
        pass


class GeneralPairingBuilderRefInitAcl(IPairingBuilder):
    def verify_role_setup(self):
        assert_true(isinstance(self._service_accessor.initiator, BumblePandoraDevice))
        assert_true(isinstance(self.responder, BumblePandoraDevice))

    def __init__(self, dut, ref, service_accessor: IServiceAccessor, pair_processor_cls: Type[IPairingProcessor]):
        self._dut = dut
        self._ref = ref
        self._service_accessor = service_accessor
        self._pair_processor_cls = pair_processor_cls

    async def start(self):
        self._init_pairing_event_stream = self.initiator.aio.security.OnPairing()
        self._resp_pairing_event_stream = self.responder.aio.security.OnPairing()

        self._pairing_processor = self._pair_processor_cls(
            self._init_pairing_event_stream, self._resp_pairing_event_stream
        )

        await self._service_accessor.start()

    async def accept_pairing(self):
        await self._pairing_processor.accept_pairing()

    async def reject_pairing(self):
        await self._pairing_processor.reject_pairing()

    async def wait_for_completion(self):
        logging.debug(">>>> wait_for_completion")
        await self._service_accessor.wait_for_completion()

    async def verify_success(self):
        await self._service_accessor.verify_success()
        # todo: verify that the link secure returns failure

    async def verify_failure(self):
        await self._service_accessor.verify_failure()
        # todo: verify that the link secure returns failure

    async def cleanup(self):
        pass

class GeneralPairingBuilderRefInitAclSsp(GeneralPairingBuilderRefInitAcl):
    def verify_role_setup(self):
        assert_true(isinstance(self._service_accessor.initiator, BumblePandoraDevice))
        # in ssp, the ref (initiator of service access) should initiate pairing
        assert_true(isinstance(self.initiator, BumblePandoraDevice))

    async def start(self):
        self._init_pairing_event_stream = self.initiator.aio.security.OnPairing()
        self._resp_pairing_event_stream = self.responder.aio.security.OnPairing()

        self._pairing_processor = self._pair_processor_cls(
            self._init_pairing_event_stream, self._resp_pairing_event_stream
        )

        await self._service_accessor.start()

        await asyncio.sleep(1)

        if self.initiator == self._service_accessor.acl_connection_builder.initiator:
            init_conn = self._service_accessor.acl_connection_builder.initiator_connection
            resp_conn = self._service_accessor.acl_connection_builder.responder_connection
        else:
            init_conn = self._service_accessor.acl_connection_builder.responder_connection
            resp_conn = self._service_accessor.acl_connection_builder.initiator_connection

        async def start_pairing():
            return await asyncio.gather(
                self.initiator.aio.security.Secure(connection=init_conn.connection, classic=LEVEL2),
                self.responder.aio.security.WaitSecurity(connection=resp_conn.connection, classic=LEVEL2),
            )

        self._pairing_tsk = asyncio.create_task(start_pairing())

    async def accept_pairing(self):
        await self._pairing_processor.accept_pairing()

    async def reject_pairing(self):
        await self._pairing_processor.reject_pairing()

    async def wait_for_completion(self):
        logging.debug(">>>> wait_for_completion")

        self.init_sec_res, self.resp_sec_res = await asyncio.wait_for(self._pairing_tsk, timeout=30.0)
        logging.debug(
            f"Pairing result(init/resp): {self.init_sec_res.result_variant()}/{self.resp_sec_res.result_variant()}"
        )
        # TODO: verify pairing results


        await self._service_accessor.wait_for_completion()

    async def verify_success(self):
        await self._service_accessor.verify_success()
        # TODO: verify that the link secure returns failure

    async def verify_failure(self):
        await self._service_accessor.verify_failure()
        # TODO: verify that the link secure returns failure

    async def cleanup(self):
        pass
