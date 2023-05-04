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
import avatar
import itertools
import logging

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from bumble.hci import HCI_CENTRAL_ROLE, HCI_PERIPHERAL_ROLE
from bumble.pairing import PairingDelegate
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_in  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_not_in  # type: ignore
from mobly.asserts import fail  # type: ignore
from pandora.host_pb2 import Connection
from pandora.security_pb2 import LEVEL2, PairingEventAnswer, SecureResponse, SecurityLevel, WaitSecurityResponse
from typing import Callable, Coroutine, Optional, Tuple

ALL_ROLES = (HCI_CENTRAL_ROLE, HCI_PERIPHERAL_ROLE)
ALL_IO_CAPABILITIES = (
    None,
    PairingDelegate.DISPLAY_OUTPUT_ONLY,
    PairingDelegate.DISPLAY_OUTPUT_AND_YES_NO_INPUT,
    PairingDelegate.KEYBOARD_INPUT_ONLY,
    PairingDelegate.NO_OUTPUT_NO_INPUT,
    PairingDelegate.DISPLAY_OUTPUT_AND_KEYBOARD_INPUT,
)


class ClassicSspTest(base_test.BaseTestClass):  # type: ignore[misc]
    '''
    This class aim to test SSP (Secure Simple Pairing) on Classic
    Bluetooth devices.
    '''

    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    @avatar.asynchronous
    async def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        # Enable BR/EDR mode and SSP for Bumble devices.
        for device in self.devices:
            if isinstance(device, BumblePandoraDevice):
                device.config.setdefault('classic_enabled', True)
                device.config.setdefault('classic_ssp_enabled', True)
                device.config.setdefault(
                    'server',
                    {
                        'io_capability': 'display_output_and_yes_no_input',
                    },
                )

        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:  # pytype: disable=wrong-arg-types
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    async def _test_success(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        connect_and_pair: Callable[[], Coroutine[None, None, Tuple[SecureResponse, WaitSecurityResponse]]],
    ) -> None:
        '''
        Perform SSP and assert it is successful.
        Prerequisites:
          - DUT and REF not bonded.
        Pairing methods:
          - Numeric Comparison with automatic confirmation (Just Works).
          - Numeric Comparison.
          - Passkey Entry.
          - OOB (TODO: out of the scope of Avatar right now).
        '''
        # Try to override reference device default IO capability.
        if ref_io_capability is not None:
            if isinstance(self.ref, BumblePandoraDevice):
                # Override Bumble reference device default IO capability.
                self.ref.server_config.io_capability = ref_io_capability
            else:
                raise signals.TestSkip('Unable to override IO capability on non Bumble device.')

        # Listen for pairing event on bot DUT and REF.
        dut_pairing_stream = self.dut.aio.security.OnPairing()
        ref_pairing_stream = self.ref.aio.security.OnPairing()

        # Start connection/pairing.
        connect_and_pair_task = asyncio.create_task(connect_and_pair())

        try:
            dut_pairing_event = await asyncio.wait_for(anext(dut_pairing_stream), timeout=15.0)
            self.dut.log.info(f'DUT pairing event: {dut_pairing_event.method_variant()}')

            ref_pairing_event = await asyncio.wait_for(anext(ref_pairing_stream), timeout=3.0)
            self.dut.log.info(f'REF pairing event: {ref_pairing_event.method_variant()}')

            if dut_pairing_event.method_variant() in ('numeric_comparison', 'just_works'):
                assert_in(ref_pairing_event.method_variant(), ('numeric_comparison', 'just_works'))
                confirm = True
                if (
                    dut_pairing_event.method_variant() == 'numeric_comparison'
                    and ref_pairing_event.method_variant() == 'numeric_comparison'
                ):
                    confirm = ref_pairing_event.numeric_comparison == dut_pairing_event.numeric_comparison
                dut_pairing_stream.send_nowait(PairingEventAnswer(event=dut_pairing_event, confirm=confirm))
                ref_pairing_stream.send_nowait(PairingEventAnswer(event=ref_pairing_event, confirm=confirm))
            elif dut_pairing_event.method_variant() == 'passkey_entry_notification':
                assert_equal(ref_pairing_event.method_variant(), 'passkey_entry_request')
                ref_pairing_stream.send_nowait(
                    PairingEventAnswer(event=ref_pairing_event, passkey=dut_pairing_event.passkey_entry_notification)
                )
            elif dut_pairing_event.method_variant() == 'passkey_entry_request':
                assert_equal(ref_pairing_event.method_variant(), 'passkey_entry_notification')
                dut_pairing_stream.send_nowait(
                    PairingEventAnswer(event=dut_pairing_event, passkey=ref_pairing_event.passkey_entry_notification)
                )
            else:
                fail("")
        except (asyncio.CancelledError, asyncio.TimeoutError):
            logging.error('Pairing timed-out.')
        except AssertionError:
            connect_and_pair_task.cancel()
        finally:
            # Assert success.
            (secure, wait_security) = await connect_and_pair_task
            logging.info(f'Initiator pairing: {secure.result_variant()}')
            logging.info(f'Acceptor pairing: {wait_security.result_variant()}')
            assert_equal(secure.result_variant(), 'success')
            assert_equal(wait_security.result_variant(), 'success')

    async def _test_failure(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        connect_and_pair: Callable[[], Coroutine[None, None, Tuple[SecureResponse, WaitSecurityResponse]]],
    ) -> None:
        '''
        Perform SSP and assert it is successful.
        Prerequisites:
          - DUT and REF not bonded.
        Pairing methods:
          - Numeric Comparison with automatic confirmation (Just Works).
          - Numeric Comparison.
          - Passkey Entry.
          - OOB (TODO: out of the scope of Avatar right now).
        '''
        # Try to override reference device default IO capability.
        if ref_io_capability is not None:
            if isinstance(self.ref, BumblePandoraDevice):
                # Override Bumble reference device default IO capability.
                self.ref.server_config.io_capability = ref_io_capability
            else:
                raise signals.TestSkip('Unable to override IO capability on non Bumble device.')

        # Listen for pairing event on bot DUT and REF.
        dut_pairing_stream = self.dut.aio.security.OnPairing()
        ref_pairing_stream = self.ref.aio.security.OnPairing()

        # Start connection/pairing.
        connect_and_pair_task = asyncio.create_task(connect_and_pair())

        try:
            dut_pairing_event = await asyncio.wait_for(anext(dut_pairing_stream), timeout=15.0)
            self.dut.log.info(f'DUT pairing event: {dut_pairing_event.method_variant()}')

            ref_pairing_event = await asyncio.wait_for(anext(ref_pairing_stream), timeout=3.0)
            self.dut.log.info(f'REF pairing event: {ref_pairing_event.method_variant()}')

            if dut_pairing_event.method_variant() in ('numeric_comparison', 'just_works'):
                assert_in(ref_pairing_event.method_variant(), ('numeric_comparison', 'just_works'))
                confirm = False
                dut_pairing_stream.send_nowait(PairingEventAnswer(event=dut_pairing_event, confirm=confirm))
                ref_pairing_stream.send_nowait(PairingEventAnswer(event=ref_pairing_event, confirm=confirm))
            elif dut_pairing_event.method_variant() == 'passkey_entry_notification':
                assert_equal(ref_pairing_event.method_variant(), 'passkey_entry_request')
                assert dut_pairing_event.passkey_entry_notification
                ref_pairing_stream.send_nowait(
                    PairingEventAnswer(
                        event=ref_pairing_event, passkey=dut_pairing_event.passkey_entry_notification + 0x42
                    )
                )
            elif dut_pairing_event.method_variant() == 'passkey_entry_request':
                assert_equal(ref_pairing_event.method_variant(), 'passkey_entry_notification')
                assert ref_pairing_event.passkey_entry_notification
                dut_pairing_stream.send_nowait(
                    PairingEventAnswer(
                        event=dut_pairing_event, passkey=ref_pairing_event.passkey_entry_notification + 0x42
                    )
                )
            else:
                fail("")
        except (asyncio.CancelledError, asyncio.TimeoutError):
            logging.error('Pairing timed-out.')
        except AssertionError:
            connect_and_pair_task.cancel()
        finally:
            # Assert success.
            (secure, wait_security) = await connect_and_pair_task
            logging.info(f'Initiator pairing: {secure.result_variant()}')
            logging.info(f'Acceptor pairing: {wait_security.result_variant()}')
            assert_equal(secure.result_variant(), 'authentication_failure')
            assert_equal(wait_security.result_variant(), 'authentication_failure')

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_success_initiate_connection_initiate_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            dut_ref, ref_dut = await make_classic_connection(self.dut, self.ref)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.dut, dut_ref, self.ref, ref_dut, LEVEL2)

        await self._test_success(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_success_initiate_connection_accept_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        if not isinstance(self.dut, BumblePandoraDevice):
            raise signals.TestSkip('TODO: fix AOSP failures')
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            dut_ref, ref_dut = await make_classic_connection(self.dut, self.ref)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.ref, ref_dut, self.dut, dut_ref, LEVEL2)

        await self._test_success(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_success_accept_connection_initiate_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            ref_dut, dut_ref = await make_classic_connection(self.ref, self.dut)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.dut, dut_ref, self.ref, ref_dut, LEVEL2)

        await self._test_success(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_success_accept_connection_accept_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            ref_dut, dut_ref = await make_classic_connection(self.ref, self.dut)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.ref, ref_dut, self.dut, dut_ref, LEVEL2)

        await self._test_success(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_failure_initiate_connection_initiate_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        raise signals.TestSkip('TODO: fix failure tests')
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            dut_ref, ref_dut = await make_classic_connection(self.dut, self.ref)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.dut, dut_ref, self.ref, ref_dut, LEVEL2)

        await self._test_failure(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_failure_initiate_connection_accept_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        raise signals.TestSkip('TODO: fix failure tests')
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            dut_ref, ref_dut = await make_classic_connection(self.dut, self.ref)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.ref, ref_dut, self.dut, dut_ref, LEVEL2)

        await self._test_failure(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_failure_accept_connection_initiate_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        raise signals.TestSkip('TODO: fix failure tests')
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            ref_dut, dut_ref = await make_classic_connection(self.ref, self.dut)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.dut, dut_ref, self.ref, ref_dut, LEVEL2)

        await self._test_failure(ref_io_capability, connect_and_pair)

    @avatar.parameterized(*itertools.product(ALL_IO_CAPABILITIES, ALL_ROLES))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_failure_accept_connection_accept_pairing(
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_role: Optional[int],
    ) -> None:
        raise signals.TestSkip('TODO: fix failure tests')
        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SecureResponse, WaitSecurityResponse]:
            ref_dut, dut_ref = await make_classic_connection(self.ref, self.dut)
            if ref_role:
                await role_switch(self.ref, ref_dut, ref_role)
            return await authenticate(self.ref, ref_dut, self.dut, dut_ref, LEVEL2)

        await self._test_failure(ref_io_capability, connect_and_pair)


# Connection task.
async def make_classic_connection(initiator: PandoraDevice, acceptor: PandoraDevice) -> Tuple[Connection, Connection]:
    '''Connect two device and returns both connection tokens.'''
    (connect, wait_connection) = await asyncio.gather(
        initiator.aio.host.Connect(address=acceptor.address),
        acceptor.aio.host.WaitConnection(address=initiator.address),
    )

    # Assert connection are successful.
    assert_equal(connect.result_variant(), 'connection')
    assert_equal(wait_connection.result_variant(), 'connection')
    assert_is_not_none(connect.connection)
    assert_is_not_none(wait_connection.connection)
    assert connect.connection and wait_connection.connection

    # Returns connections.
    return connect.connection, wait_connection.connection


# Pairing task.
async def authenticate(
    initiator: PandoraDevice,
    initiator_connection: Connection,
    acceptor: PandoraDevice,
    acceptor_connection: Connection,
    security_level: SecurityLevel,
) -> Tuple[SecureResponse, WaitSecurityResponse]:
    '''Pair two device and returns both pairing responses.'''
    return await asyncio.gather(
        initiator.aio.security.Secure(connection=initiator_connection, classic=security_level),
        acceptor.aio.security.WaitSecurity(connection=acceptor_connection, classic=security_level),
    )


# Role switch task.
async def role_switch(
    device: PandoraDevice,
    connection: Connection,
    role: int,
) -> None:
    '''Switch role if supported.'''
    if not isinstance(device, BumblePandoraDevice):
        return

    connection_handle = int.from_bytes(connection.cookie.value, 'big')
    bumble_connection = device.device.lookup_connection(connection_handle)
    assert_is_not_none(bumble_connection)
    assert bumble_connection

    if bumble_connection.role != role:
        device.log.info(f"Role switch to: {'Central' if role == HCI_CENTRAL_ROLE else 'Peripheral'}")
        await bumble_connection.switch_role(role)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
