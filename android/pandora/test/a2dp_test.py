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

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.aio import asynchronous
from bumble.a2dp import (
    A2DP_SBC_CODEC_TYPE,
    SBC_DUAL_CHANNEL_MODE,
    SBC_JOINT_STEREO_CHANNEL_MODE,
    SBC_LOUDNESS_ALLOCATION_METHOD,
    SBC_MONO_CHANNEL_MODE,
    SBC_SNR_ALLOCATION_METHOD,
    SBC_STEREO_CHANNEL_MODE,
    SbcMediaCodecInformation,
    make_audio_sink_service_sdp_records, # type: ignore
)
from bumble.avdtp import AVDTP_AUDIO_MEDIA_TYPE, Listener, MediaCodecCapabilities
from bumble.pairing import PairingConfig, PairingDelegate
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_false  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.a2dp_grpc_aio import A2DP
from pandora.host_pb2 import RANDOM
from pandora.security_pb2 import LE_LEVEL2, LEVEL2
from typing import Optional


class A2dpTest(base_test.BaseTestClass):  # type: ignore[misc]
    devices: Optional[PandoraDevices] = None

    dut: PandoraDevice
    ref: PandoraDevice

    def setup_class(self) -> None:
        self.devices = PandoraDevices(self)
        self.dut, self.ref, *_ = self.devices

        # Enable BR/EDR mode for Bumble devices.
        for device in self.devices:
            if isinstance(device, BumblePandoraDevice):
                device.config.setdefault('classic_enabled', True)

    def teardown_class(self) -> None:
        if self.devices:
            self.devices.stop_all()

    @asynchronous
    async def setup_test(self) -> None:
        await asyncio.gather(self.dut.reset(), self.ref.reset())

    @asynchronous
    async def test_ad2p_autoconnect_when_only_avrcp_connected(self) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip("")
        ref_dut = await self.ref.aio.host.Connect(address=self.dut.address)
        assert ref_dut and ref_dut.connection
        await self.ref.aio.security.Secure(connection=ref_dut.connection, classic=LEVEL2)

        # Retrieve Bumble connection object from Pandora connection token
        connection_handle = int.from_bytes(ref_dut.connection.cookie.value, 'big')
        connection = self.ref.device.lookup_connection(connection_handle)  # type: ignore

        # 1. Open AVRCP L2CAP channel
        avrcp = await self.ref.device.l2cap_channel_manager.connect(connection, psm=0x0017)  # type: ignore
        self.ref.log.info(f"AVRCP: {avrcp}")

        # 2. Wait for AVDTP L2CAP channel
        avdtp_future = asyncio.get_running_loop().create_future()
        self.ref.device.l2cap_channel_manager.register_server(0x0019, avdtp_future.set_result)
        avdtp = await asyncio.wait_for(avdtp_future, timeout=5.0)
        self.ref.log.info(f"AVDTP: {avdtp}")

    @asynchronous
    async def test_yolo_classic(self) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip("")

        # Add SDP records for A2DP sink in the Bumble
        self.ref.device.sdp_service_records = {0x00010001: make_audio_sink_service_sdp_records(0x00010001)}

        # Start advertising on LE using Random address
        adv = self.ref.aio.host.Advertise(legacy=True, own_address_type=RANDOM, connectable=True)

        # Connect on LE to the bumble device
        dut_ref = await self.dut.aio.host.ConnectLE(own_address_type=RANDOM, random=self.ref.random_address)
        assert dut_ref and dut_ref.connection
        adv.cancel()

        # Enabling CTKD on bumble
        bumble_key_distribution = (
            PairingDelegate.KeyDistribution.DISTRIBUTE_ENCRYPTION_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_SIGNING_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_LINK_KEY
        )
        self.ref.server_config.smp_local_initiator_key_distribution = bumble_key_distribution
        self.ref.server_config.smp_local_responder_key_distribution = bumble_key_distribution
        self.ref.server_config.identity_address_type = PairingConfig.AddressType.PUBLIC

        # Trigger SMP pairing from android to bumble
        await self.dut.aio.security.Secure(connection=dut_ref.connection, le=LE_LEVEL2)

        # Waiting for CTKD classic connection
        classic_dut_ref = await self.dut.aio.host.WaitConnection(address=self.ref.address)
        assert classic_dut_ref and classic_dut_ref.connection

        # Create a listener to wait for AVDTP connections and register codec capabilities
        codec_capabilities = MediaCodecCapabilities(
            media_type=AVDTP_AUDIO_MEDIA_TYPE,
            media_codec_type=A2DP_SBC_CODEC_TYPE,
            media_codec_information=SbcMediaCodecInformation.from_lists(  # type: ignore
                sampling_frequencies=[48000, 44100, 32000, 16000],
                channel_modes=[
                    SBC_MONO_CHANNEL_MODE,
                    SBC_DUAL_CHANNEL_MODE,
                    SBC_STEREO_CHANNEL_MODE,
                    SBC_JOINT_STEREO_CHANNEL_MODE,
                ],
                block_lengths=[4, 8, 12, 16],
                subbands=[4, 8],
                allocation_methods=[
                    SBC_LOUDNESS_ALLOCATION_METHOD,
                    SBC_SNR_ALLOCATION_METHOD,
                ],
                minimum_bitpool_value=2,
                maximum_bitpool_value=53,
            ),
        )
        listener = Listener(Listener.create_registrar(self.ref.device)) # type: ignore
        listener.on('connection', lambda server: server.add_sink(codec_capabilities)) # type: ignore

        # Register AVRCP on reference since A2DP needs AVRCP to connect
        self.ref.device.l2cap_channel_manager.register_server(
            0x0017, lambda _: self.ref.log.info("AVRCP registered on REF")
        )

        a2dp = A2DP(channel=self.dut.aio.channel)
        await a2dp.OpenSource(classic_dut_ref.connection)


    @asynchronous
    async def test_yolo_le(self) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip("")

        # Add SDP records for A2DP sink in the Bumble
        self.ref.device.sdp_service_records = {0x00010001: make_audio_sink_service_sdp_records(0x00010001)}

        # Start advertising on LE using Random address
        adv = self.ref.aio.host.Advertise(legacy=True, own_address_type=RANDOM, connectable=True)

        # Connect on LE to the bumble device
        dut_ref = await self.dut.aio.host.ConnectLE(own_address_type=RANDOM, random=self.ref.random_address)
        assert dut_ref and dut_ref.connection
        adv.cancel()

        # Enabling CTKD on bumble
        bumble_key_distribution = (
            PairingDelegate.KeyDistribution.DISTRIBUTE_ENCRYPTION_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_SIGNING_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_LINK_KEY
        )
        self.ref.server_config.smp_local_initiator_key_distribution = bumble_key_distribution
        self.ref.server_config.smp_local_responder_key_distribution = bumble_key_distribution
        self.ref.server_config.identity_address_type = PairingConfig.AddressType.PUBLIC

        # Trigger SMP pairing from android to bumble
        await self.dut.aio.security.Secure(connection=dut_ref.connection, le=LE_LEVEL2)

        # Waiting for CTKD classic connection
        classic_dut_ref = await self.dut.aio.host.WaitConnection(address=self.ref.address)
        assert classic_dut_ref and classic_dut_ref.connection

        # Create a listener to wait for AVDTP connections and register codec capabilities
        codec_capabilities = MediaCodecCapabilities(
            media_type=AVDTP_AUDIO_MEDIA_TYPE,
            media_codec_type=A2DP_SBC_CODEC_TYPE,
            media_codec_information=SbcMediaCodecInformation.from_lists(  # type: ignore
                sampling_frequencies=[48000, 44100, 32000, 16000],
                channel_modes=[
                    SBC_MONO_CHANNEL_MODE,
                    SBC_DUAL_CHANNEL_MODE,
                    SBC_STEREO_CHANNEL_MODE,
                    SBC_JOINT_STEREO_CHANNEL_MODE,
                ],
                block_lengths=[4, 8, 12, 16],
                subbands=[4, 8],
                allocation_methods=[
                    SBC_LOUDNESS_ALLOCATION_METHOD,
                    SBC_SNR_ALLOCATION_METHOD,
                ],
                minimum_bitpool_value=2,
                maximum_bitpool_value=53,
            ),
        )
        listener = Listener(Listener.create_registrar(self.ref.device)) # type: ignore
        listener.on('connection', lambda server: server.add_sink(codec_capabilities)) # type: ignore

        # Register AVRCP on reference since A2DP needs AVRCP to connect
        self.ref.device.l2cap_channel_manager.register_server(
            0x0017, lambda _: self.ref.log.info("AVRCP registered on REF")
        )

        a2dp = A2DP(channel=self.dut.aio.channel)
        await a2dp.OpenSource(dut_ref.connection)

    @asynchronous
    async def test_yolo_2(self) -> None:
        if not isinstance(self.ref, BumblePandoraDevice):
            raise signals.TestSkip("")

        # Add SDP records for A2DP sink in the Bumble
        self.ref.device.sdp_service_records = {0x00010001: make_audio_sink_service_sdp_records(0x00010001)}

        # Make REF identity address public
        # self.ref.server_config.identity_address_type = PairingConfig.AddressType.PUBLIC

        # Start advertising on LE using Random address
        adv = self.ref.aio.host.Advertise(legacy=True, own_address_type=RANDOM, connectable=True)

        bumble_key_distribution = (
            PairingDelegate.KeyDistribution.DISTRIBUTE_ENCRYPTION_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_IDENTITY_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_SIGNING_KEY
            | PairingDelegate.KeyDistribution.DISTRIBUTE_LINK_KEY
        )
        self.ref.server_config.smp_local_initiator_key_distribution = bumble_key_distribution
        self.ref.server_config.smp_local_responder_key_distribution = bumble_key_distribution

        # Connect on classic to the bumble device
        dut_ref = await self.dut.aio.host.Connect(address=self.ref.address)
        assert dut_ref and dut_ref.connection
        await self.dut.aio.security.Secure(connection=dut_ref.connection, classic=LEVEL2)

        ref_dut = await anext(aiter(adv))
        assert ref_dut and ref_dut.connection
        await self.ref.aio.security.Secure(connection=ref_dut.connection, le=LE_LEVEL2)

        # Create a listener to wait for AVDTP connections and register codec capabilities
        codec_capabilities = MediaCodecCapabilities(
            media_type=AVDTP_AUDIO_MEDIA_TYPE,
            media_codec_type=A2DP_SBC_CODEC_TYPE,
            media_codec_information=SbcMediaCodecInformation.from_lists(  # type: ignore
                sampling_frequencies=[48000, 44100, 32000, 16000],
                channel_modes=[
                    SBC_MONO_CHANNEL_MODE,
                    SBC_DUAL_CHANNEL_MODE,
                    SBC_STEREO_CHANNEL_MODE,
                    SBC_JOINT_STEREO_CHANNEL_MODE,
                ],
                block_lengths=[4, 8, 12, 16],
                subbands=[4, 8],
                allocation_methods=[
                    SBC_LOUDNESS_ALLOCATION_METHOD,
                    SBC_SNR_ALLOCATION_METHOD,
                ],
                minimum_bitpool_value=2,
                maximum_bitpool_value=53,
            ),
        )
        listener = Listener(Listener.create_registrar(self.ref.device)) # type: ignore
        listener.on('connection', lambda server: server.add_sink(codec_capabilities)) # type: ignore

        # Register AVRCP on reference since A2DP needs AVRCP to connect
        self.ref.device.l2cap_channel_manager.register_server(
            0x0017, lambda _: self.ref.log.info("AVRCP registered on REF")
        )
        # await asyncio.sleep(30)
        a2dp = A2DP(channel=self.dut.aio.channel)
        await a2dp.OpenSource(dut_ref.connection)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    test_runner.main()  # type: ignore
