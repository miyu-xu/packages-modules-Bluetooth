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
from concurrent import futures
from contextlib import suppress
from mobly import base_test, signals
from mobly.asserts import assert_equal, assert_in, assert_is_not_none, fail
from pandora.host_pb2 import PUBLIC, RANDOM, Connection, DataTypes, OwnAddressType
from pandora.security_pb2 import LE_LEVEL3, PairingEventAnswer, SecureResponse, WaitSecurityResponse
from typing import Callable, Coroutine, Literal, Optional, Tuple, Union

ALL_IO_CAPABILITIES = (
    PairingDelegate.DISPLAY_OUTPUT_ONLY,
    PairingDelegate.DISPLAY_OUTPUT_AND_YES_NO_INPUT,
    PairingDelegate.KEYBOARD_INPUT_ONLY,
    PairingDelegate.NO_OUTPUT_NO_INPUT,
    PairingDelegate.DISPLAY_OUTPUT_AND_KEYBOARD_INPUT,
)

KEY_DISTRIBUTION_IRK = PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY
KEY_DISTRIBUTION_IRK_CSRK = (PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY |
                             PairingDelegate.KeyDistribution.DISTRIBUTE_SIGNING_KEY)
KEY_DISTRIBUTION_IRK_CSRK_LK = (PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY |
                                PairingDelegate.KeyDistribution.DISTRIBUTE_SIGNING_KEY |
                                PairingDelegate.KeyDistribution.DISTRIBUTE_LINK_KEY)
ALL_KEY_DISTRIBUTIONS = (
    (KEY_DISTRIBUTION_IRK, KEY_DISTRIBUTION_IRK),
    (KEY_DISTRIBUTION_IRK_CSRK, KEY_DISTRIBUTION_IRK_CSRK),
    # TODO: Bumble does not support Public Identity Address distribution over a connection using random address
    # and distribute LinkKey requires Public Identity Address.
    # (KEY_DISTRIBUTION_IRK_CSRK_LK, KEY_DISTRIBUTION_IRK_CSRK_LK),
)
ALL_ADDRESS_TYPES = (RANDOM, PUBLIC)
ALL_DIRECTIONS = ("refdut", "dutref")
ALL_ROLES = (HCI_CENTRAL_ROLE, HCI_PERIPHERAL_ROLE)

SECURITY_RESPONSE = Union[SecureResponse, WaitSecurityResponse]


class LeSecurityTest(base_test.BaseTestClass):  # type: ignore[misc]

    devices: Optional[PandoraDevices] = None

    # pandora devices.
    dut: PandoraDevice
    ref: PandoraDevice

    @avatar.asynchronous
    async def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        await asyncio.gather(self.dut.reset(), self.ref.reset())

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @avatar.asynchronous
    async def setup_test(self) -> None:  # pytype: disable=wrong-arg-types
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    @avatar.parameterized(*itertools.product(
        ALL_IO_CAPABILITIES,
        ALL_KEY_DISTRIBUTIONS,
        ALL_ADDRESS_TYPES,
        ALL_ADDRESS_TYPES,
        ALL_ROLES,
        ALL_DIRECTIONS,
    ))  # type: ignore[misc]
    @avatar.asynchronous
    async def test_le_pairing_success(  # pytype: disable=wrong-arg-types
        self,
        ref_io_capability: Optional[PairingDelegate.IoCapability],
        ref_key_distribution_pair: Tuple[PairingDelegate.KeyDistribution, PairingDelegate.KeyDistribution],
        dut_address_type: OwnAddressType,
        ref_address_type: OwnAddressType,
        ref_connection_role: int,
        authentication_direction: Literal["dutref", "refdut"],
    ) -> None:
        if ref_connection_role == HCI_CENTRAL_ROLE and dut_address_type == PUBLIC:
            raise signals.TestSkip('Android does not support scan with public address')

        ref_initiator_key_distribution = ref_key_distribution_pair[0]
        ref_responder_key_distribution = ref_key_distribution_pair[1]
        set_pairing_parameters(self.ref, ref_io_capability, ref_initiator_key_distribution,
                               ref_responder_key_distribution)

        # Connection/pairing task.
        async def connect_and_pair() -> Tuple[SECURITY_RESPONSE, SECURITY_RESPONSE]:
            if ref_connection_role == HCI_CENTRAL_ROLE:
                ref_dut, dut_ref = await make_le_connection(self.ref, ref_address_type, self.dut, dut_address_type)
            else:
                dut_ref, ref_dut = await make_le_connection(self.dut, dut_address_type, self.ref, ref_address_type)

            if authentication_direction == "dutref":
                return await asyncio.gather(
                    self.ref.aio.security.Secure(connection=ref_dut, le=LE_LEVEL3),
                    self.dut.aio.security.WaitSecurity(connection=dut_ref, le=LE_LEVEL3),
                )
            else:
                return await asyncio.gather(
                    self.ref.aio.security.WaitSecurity(connection=ref_dut, le=LE_LEVEL3),
                    self.dut.aio.security.Secure(connection=dut_ref, le=LE_LEVEL3),
                )

        # Handle pairing.
        initiator_pairing, acceptor_pairing = await handle_pairing(
            self.dut,
            self.ref,
            connect_and_pair,
        )

        # Assert success.
        assert_equal(initiator_pairing.result_variant(), 'success')
        assert_equal(acceptor_pairing.result_variant(), 'success')


def set_pairing_parameters(
    device: PandoraDevice,
    io_capability: Optional[PairingDelegate.IoCapability],
    initiator_key_distribution: Optional[PairingDelegate.KeyDistribution],
    responder_key_distribution: Optional[PairingDelegate.KeyDistribution],
) -> None:
    if io_capability is None:
        return
    if isinstance(device, BumblePandoraDevice):
        # Override Bumble reference device default IO capability.
        device.server_config.io_capability = io_capability
        device.server_config.smp_local_initiator_key_distribution = initiator_key_distribution
        device.server_config.smp_local_responder_key_distribution = responder_key_distribution
    else:
        raise signals.TestSkip('Unable to override IO capability on non Bumble device.')


async def make_le_connection(
    central: PandoraDevice,
    central_address_type: OwnAddressType,
    peripheral: PandoraDevice,
    peripheral_address_type: OwnAddressType,
) -> Tuple[Connection, Connection]:
    advertisement = central.aio.host.Advertise(
        legacy=True,
        connectable=True,
        own_address_type=central_address_type,
        data=DataTypes(manufacturer_specific_data=b'pause cafe'),
    )

    scan = peripheral.aio.host.Scan(own_address_type=peripheral_address_type)
    cen = await anext((x async for x in scan if b'pause cafe' in x.data.manufacturer_specific_data))
    scan.cancel()

    (per_cen_res, cen_per_res) = await asyncio.gather(
        peripheral.aio.host.ConnectLE(own_address_type=peripheral_address_type, **cen.address_asdict()),
        anext(aiter(advertisement)),
    )

    advertisement.cancel()
    cen_per, per_cen = cen_per_res.connection, per_cen_res.connection
    assert_is_not_none(cen_per)
    assert_is_not_none(per_cen)
    assert cen_per and per_cen
    return (cen_per, per_cen)


# Handle pairing events task.
async def handle_pairing(
    dut: PandoraDevice,
    ref: PandoraDevice,
    connect_and_pair: Callable[[], Coroutine[None, None, Tuple[SECURITY_RESPONSE, SECURITY_RESPONSE]]],
    confirm: Callable[[bool], bool] = lambda x: x,
    passkey: Callable[[int], int] = lambda x: x,
) -> Tuple[SECURITY_RESPONSE, SECURITY_RESPONSE]:

    # Listen for pairing event on bot DUT and REF.
    dut_pairing, ref_pairing = dut.aio.security.OnPairing(), ref.aio.security.OnPairing()

    # Start connection/pairing.
    connect_and_pair_task = asyncio.create_task(connect_and_pair())

    try:
        dut_ev = await asyncio.wait_for(anext(dut_pairing), timeout=25.0)
        dut.log.info(f'DUT pairing event: {dut_ev.method_variant()}')

        ref_ev = await asyncio.wait_for(anext(ref_pairing), timeout=3.0)
        ref.log.info(f'REF pairing event: {ref_ev.method_variant()}')

        if dut_ev.method_variant() in ('numeric_comparison', 'just_works'):
            assert_in(ref_ev.method_variant(), ('numeric_comparison', 'just_works'))
            confirm_res = True
            if dut_ev.method_variant() == 'numeric_comparison' and ref_ev.method_variant() == 'numeric_comparison':
                confirm_res = ref_ev.numeric_comparison == dut_ev.numeric_comparison
            confirm_res = confirm(confirm_res)
            dut_pairing.send_nowait(PairingEventAnswer(event=dut_ev, confirm=confirm_res))
            ref_pairing.send_nowait(PairingEventAnswer(event=ref_ev, confirm=confirm_res))

        elif dut_ev.method_variant() == 'passkey_entry_notification':
            assert_equal(ref_ev.method_variant(), 'passkey_entry_request')
            assert_is_not_none(dut_ev.passkey_entry_notification)
            assert dut_ev.passkey_entry_notification is not None
            passkey_res = passkey(dut_ev.passkey_entry_notification)
            ref_pairing.send_nowait(PairingEventAnswer(event=ref_ev, passkey=passkey_res))

        elif dut_ev.method_variant() == 'passkey_entry_request':
            assert_equal(ref_ev.method_variant(), 'passkey_entry_notification')
            assert_is_not_none(ref_ev.passkey_entry_notification)
            assert ref_ev.passkey_entry_notification is not None
            passkey_res = passkey(ref_ev.passkey_entry_notification)
            dut_pairing.send_nowait(PairingEventAnswer(event=dut_ev, passkey=passkey_res))

        else:
            fail("")

    except (asyncio.CancelledError, asyncio.TimeoutError):
        logging.exception('Pairing timed-out.')

    finally:

        try:
            sec_result = await asyncio.wait_for(connect_and_pair_task, 15.0)
            logging.info(f'Pairing result: {sec_result[0].result_variant()}/{sec_result[1].result_variant()}')
            return sec_result

        finally:
            dut_pairing.cancel()
            ref_pairing.cancel()
