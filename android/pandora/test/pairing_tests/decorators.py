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
import time

from avatar import asynchronous, PandoraDevices

from mobly.asserts import assert_true

from pairing_tests.interfaces import IAclConnectionBuilder, IPairingProcessor, IServiceAccessor
from pairing_tests.pairing_builders_impl import (
    DedicatedPairingBuilderDutInitAcl,
    DedicatedPairingBuilderRefInitAcl,
    GeneralPairingBuilderRefInitAcl,
    GeneralPairingBuilderRefInitAclSsp,
)


def _add_ref_init_acl_dd_pairing_tests(cls, acl_connection_builder_class, pairing_process_class):
    @asynchronous
    async def test_dedicated_pairing_ref_initiate_acl_ref_init_pairing(self) -> None:
        '''
        Role setting:

        acl:
            ref: initiator
            dut: responder

        pairing:
            ref: initiator
            dut: responder
        '''

        acl_connection_builder = acl_connection_builder_class(self.dut, self.ref)
        acl_connection_builder.initiator = self.ref
        acl_connection_builder.responder = self.dut

        pairing_builder = DedicatedPairingBuilderRefInitAcl(
            self.dut, self.ref, acl_connection_builder, pairing_process_class
        )

        pairing_builder.initiator = self.ref
        pairing_builder.responder = self.dut

        await pairing_builder.start()
        await pairing_builder.accept_pairing()
        await pairing_builder.wait_for_completion()
        await pairing_builder.verify_success()

    setattr(
        cls,
        'test_dedicated_pairing_ref_initiate_acl_ref_init_pairing',
        test_dedicated_pairing_ref_initiate_acl_ref_init_pairing,
    )

    @asynchronous
    async def test_dedicated_pairing_ref_initiate_acl_dut_init_pairing(self) -> None:
        '''
        Role setting:

        acl:
            ref: initiator
            dut: responder

        pairing:
            ref: responder
            dut: initiator
        '''

        acl_connection_builder = acl_connection_builder_class(self.dut, self.ref)
        acl_connection_builder.initiator = self.ref
        acl_connection_builder.responder = self.dut

        pairing_builder = DedicatedPairingBuilderRefInitAcl(
            self.dut, self.ref, acl_connection_builder, pairing_process_class
        )
        pairing_builder.initiator = self.dut
        pairing_builder.responder = self.ref

        await pairing_builder.start()
        await pairing_builder.accept_pairing()
        await pairing_builder.wait_for_completion()
        await pairing_builder.verify_success()

    setattr(
        cls,
        'test_dedicated_pairing_ref_initiate_acl_dut_init_pairing',
        test_dedicated_pairing_ref_initiate_acl_dut_init_pairing,
    )


def _add_dut_init_acl_dd_pairing_tests(cls, acl_connection_builder_class, pairing_process_class):
    @asynchronous
    async def test_dedicated_pairing_dut_initiate_acl_dut_init_pairing(self) -> None:
        '''
        acl:
            ref: responder
            dut: initiator

        pairing:
            ref: responder
            dut: initiator

        Note: we can not change the role of pairing actions in the current avatar
        implementation, as the implementation of Connect (initiating acl connection)
        on Android will initiate pairing.

        Pairing initiated from ref is not supported yet
        '''

        acl_connection_builder = acl_connection_builder_class(self.dut, self.ref)
        acl_connection_builder.initiator = self.dut
        acl_connection_builder.responder = self.ref

        pairing_builder = DedicatedPairingBuilderDutInitAcl(
            self.dut, self.ref, acl_connection_builder, pairing_process_class
        )
        pairing_builder.initiator = self.dut
        pairing_builder.responder = self.ref

        await pairing_builder.start()
        await pairing_builder.accept_pairing()
        await pairing_builder.wait_for_completion()
        await pairing_builder.verify_success()

    # assert not hasattr(cls, 'test_dedicated_pairing_dut_initiate_acl_dut_init_pairing')

    setattr(
        cls,
        'test_dedicated_pairing_dut_initiate_acl_dut_init_pairing',
        test_dedicated_pairing_dut_initiate_acl_dut_init_pairing,
    )


def _add_general_pairing_tests(cls, acl_connection_builder_class, service_accessor_class, pairing_process_class):
    @asynchronous
    async def test_general_pairing(self) -> None:
        '''
        acl:
            ref: initiator
            dut: responder

        Secure Service access:
            ref: initiator
            dut: responder

        pairing:
            ref: responder
            dut: initiator

        Pairing initiated from ref is not supported yet
        '''

        acl_connection_builder = acl_connection_builder_class(self.dut, self.ref)
        acl_connection_builder.initiator = self.ref
        acl_connection_builder.responder = self.dut

        service_accessor = service_accessor_class(self.dut, self.ref, acl_connection_builder)
        service_accessor.initiator = self.ref
        service_accessor.responder = self.dut

        if self.ref_config['classic_enabled'] and not self.ref_config['le_enabled'] and not self.ref_config['classic_ssp_enabled']:
            # bredr legacy
            pairing_builder = GeneralPairingBuilderRefInitAcl(self.dut, self.ref, service_accessor, pairing_process_class)
            pairing_builder.initiator = self.dut
            pairing_builder.responder = self.ref
        else:
            pairing_builder = GeneralPairingBuilderRefInitAclSsp(self.dut, self.ref, service_accessor, pairing_process_class)
            pairing_builder.initiator = self.ref
            pairing_builder.responder = self.dut

        await pairing_builder.start()
        await pairing_builder.accept_pairing()
        await pairing_builder.wait_for_completion()
        await pairing_builder.verify_success()

    test_name = test_general_pairing.__name__ + "_" + service_accessor_class.__name__
    assert not hasattr(cls, test_name)

    setattr(cls, test_name, test_general_pairing)



def add_boilerplate_methods(cls):

    assert_true(hasattr(cls, 'ref_config'), f'ref_config field is not defined in {cls.__name__}')

    # add setup_class method
    @asynchronous
    async def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        # update the config
        # ref_config is a class field defined in each test class
        self.ref.config.update(self.ref_config)

    setattr(cls, setup_class.__name__, setup_class)

    # add teardown_class method
    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    setattr(cls, teardown_class.__name__, teardown_class)

    # add setup_test method
    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    setattr(cls, setup_test.__name__, setup_test)

    # add teardown_test method
    def teardown_test(self):
        time.sleep(5)

    setattr(cls, teardown_test.__name__, teardown_test)

    return cls

def add_common_tests(
    acl_connection_builder_classes: list[IAclConnectionBuilder],
    pairing_process_class: IPairingProcessor,
    service_accessor_classes: list[IServiceAccessor],
    **kwargs
):
    def _add_common_test(cls):
        for acl_connection_builder_class in acl_connection_builder_classes:
            for service_accessor_class in service_accessor_classes:
                _add_ref_init_acl_dd_pairing_tests(cls, acl_connection_builder_class, pairing_process_class)
                _add_dut_init_acl_dd_pairing_tests(cls, acl_connection_builder_class, pairing_process_class)
                _add_general_pairing_tests(
                    cls, acl_connection_builder_class, service_accessor_class, pairing_process_class
                )

        return cls

    return _add_common_test
