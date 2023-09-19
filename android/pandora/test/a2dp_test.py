# Copyright 2024 Google LLC
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
from typing import Optional

from avatar import BumblePandoraDevice, PandoraDevice, PandoraDevices
from avatar.aio import asynchronous
from avatar.pandora_server import AndroidPandoraServer
from bumble import l2cap
from bumble.a2dp import make_audio_sink_service_sdp_records
from bumble.a2dp import (
    SBC_DUAL_CHANNEL_MODE,
    SBC_JOINT_STEREO_CHANNEL_MODE,
    SBC_LOUDNESS_ALLOCATION_METHOD,
    SBC_MONO_CHANNEL_MODE,
    SBC_SNR_ALLOCATION_METHOD,
    SBC_STEREO_CHANNEL_MODE,
    SbcMediaCodecInformation,
)
from bumble.avctp import AVCTP_PSM
from bumble.avdtp import (
    A2DP_SBC_CODEC_TYPE,
    AVDTP_AUDIO_MEDIA_TYPE,
    AVDTP_IDLE_STATE,
    AVDTP_STREAMING_STATE,
    AVDTP_TSEP_SNK,
    Listener,
    MediaCodecCapabilities,
    MediaPacket,
    MediaPacketPump,
    Protocol,
)
from mobly import base_test, signals, test_runner
from mobly.asserts import assert_equal  # type: ignore
from mobly.asserts import assert_false  # type: ignore
from mobly.asserts import assert_is_not_none  # type: ignore
from mobly.asserts import assert_true  # type: ignore
from pandora.security_pb2 import LEVEL2

AVRCP_CONNECT_A2DP_DELAYED = 'persist.device_config.aconfig_flags.bluetooth.com.android.bluetooth.flags.avrcp_connect_a2dp_delayed'


# -----------------------------------------------------------------------------
def sink_codec_capabilities():
  return MediaCodecCapabilities(
      media_type=AVDTP_AUDIO_MEDIA_TYPE,
      media_codec_type=A2DP_SBC_CODEC_TYPE,
      media_codec_information=SbcMediaCodecInformation.from_lists(
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
        device.config.setdefault('classic_ssp_enabled', True)

    # Enable AVRCP connect A2DP delayed feature
    for server in self.devices._servers:
      if isinstance(server, AndroidPandoraServer):
        self.dut_adb = server.device.adb
        self.dut_adb.shell(['setprop', AVRCP_CONNECT_A2DP_DELAYED, 'true'])  # type: ignore
        break

  def teardown_class(self) -> None:
    if self.devices:
      self.devices.stop_all()

  @asynchronous
  async def setup_test(self) -> None:
    await asyncio.gather(self.dut.reset(), self.ref.reset())

  @asynchronous
  async def test_ad2p_autoconnect_when_only_avrcp_connected(self) -> None:
    if not isinstance(self.ref, BumblePandoraDevice):
      raise signals.TestSkip('')

    ref_dut_res, dut_ref_res = await asyncio.gather(
        self.ref.aio.host.Connect(address=self.dut.address),
        self.dut.aio.host.WaitConnection(address=self.ref.address),
    )
    assert_is_not_none(ref_dut_res.connection)
    assert_is_not_none(dut_ref_res.connection)
    ref_dut, dut_ref = ref_dut_res.connection, dut_ref_res.connection
    assert ref_dut and dut_ref

    await asyncio.gather(
        self.ref.aio.security.Secure(connection=ref_dut, classic=LEVEL2),
        self.dut.aio.security.WaitSecurity(connection=dut_ref, classic=LEVEL2),
    )

    # Retrieve Bumble connection object from Pandora connection token
    connection_handle = int.from_bytes(ref_dut.cookie.value, 'big')
    connection = self.ref.device.lookup_connection(connection_handle)  # type: ignore

    # Register SDP service
    self.ref.device.sdp_server.service_records.update(
        {0x00010001: make_audio_sink_service_sdp_records(0x00010001)}
    )

    # 1. Open AVRCP L2CAP channel
    avrcp = await self.ref.device.l2cap_channel_manager.connect(connection, psm=AVCTP_PSM)  # type: ignore
    self.ref.log.info(f'AVRCP: {avrcp}')

    # 2. Wait for AVDTP L2CAP channel
    avdtp_future = asyncio.get_running_loop().create_future()

    def on_avdtp_connection(server):
      nonlocal avdtp_future
      sink = server.add_sink(sink_codec_capabilities())
      self.ref.log.info(f'Sink: {sink}')
      avdtp_future.set_result(None)

    # Create a listener to wait for AVDTP connections
    listener = Listener.for_device(self.ref.device)
    listener.on('connection', on_avdtp_connection)
    await asyncio.wait_for(avdtp_future, timeout=10.0)


if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  test_runner.main()  # type: ignore
