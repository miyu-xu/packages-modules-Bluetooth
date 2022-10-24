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

import asyncio
import logging

from bumble.core import BT_BR_EDR_TRANSPORT
from bumble.smp import PairingConfig, PairingDelegate

from google.protobuf.empty_pb2 import Empty
from pandora_experimental.host_pb2 import Connection
from pandora_experimental.security_pb2 import PairingEvent, PairingEventAnswer
from pandora_experimental.security_grpc import SecurityServicer


class SecurityService(SecurityServicer):

    def __init__(self, manager):
        self.manager = manager
        self.pairing_events = asyncio.Queue()
        self.pairing_answers = asyncio.Queue()

    @property
    def device(self):
        return self.manager.device

    async def Pair(self, request, context):
        logging.info('Pair')
        connection_handle = int.from_bytes(request.connection.cookie, 'big')
        connection = self.device.lookup_connection(connection_handle)
        asyncio.create_task(self.device.authenticate(connection))
        logging.info("Authenticated")
        return Empty()

    async def OnPairing(self, request_iterator, context):
        logging.info("OnPairing")

        async def receive_answer():
            async for answer in request_iterator:
                answer_type = answer.WhichOneof("answer")
                if answer_type == "confirm":
                    await self.pairing_answers.put(answer.confirm)
                elif answer_type == "passkey":
                    await self.pairing_answers.put(answer.passkey)
                elif answer_type == "pin":
                    # unimplemented
                    pass

        asyncio.create_task(receive_answer())

        while True:
            yield await self.pairing_events.get()

    def set_pairing_config(self, io_cap, mitm, sc, bonding, initiator_key_dist=[], responder_key_dist=[]):
        logging.info(f'SetPairingConfig: io_cap={io_cap}, bonding={bonding}, mitm={mitm}, sc={sc}')
        local_i_key_dist = sum(set(initiator_key_dist))
        local_r_key_dist = sum(set(responder_key_dist))
        delegate = Delegate(
            io_capability=io_cap, servicer=self, local_i_key_dist=local_i_key_dist, local_r_key_dist=local_r_key_dist)
        self.manager.pairing_config_factory = lambda _: PairingConfig(
            sc=sc, bonding=bonding, mitm=mitm, delegate=delegate)
        self.device.pairing_config_factory = self.manager.pairing_config_factory


class Delegate(PairingDelegate):

    def __init__(self, io_capability, servicer, local_i_key_dist, local_r_key_dist):
        logging.info("Delegate init")
        super().__init__(io_capability, local_i_key_dist, local_r_key_dist)
        self.servicer = servicer

    async def get_number(self):
        logging.info("get_number")
        await self.servicer.pairing_events.put(PairingEvent(passkey_entry_request=Empty()))
        return await self.servicer.pairing_answers.get()

    async def compare_numbers(self, number, digits=6):
        logging.info("compare_number")
        await self.servicer.pairing_events.put(PairingEvent(numeric_comparison=number))
        # Confirmation number are passed to the test body and compare there, so we simply return answer here
        return await self.servicer.pairing_answers.get()
