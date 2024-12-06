# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------

from bumble.colors import color
from bumble.device import Connection
try:
    from packets import avdtp as avdt_packet_module
    from packets.avdtp import *
except ImportError:
    from .packets import avdtp as avdt_packet_module
    from .packets.avdtp import *
from pyee import EventEmitter
from typing import Union

import asyncio
import bumble.l2cap as l2cap
import bumble.avdtp as avdtp
import collections
import logging

# -----------------------------------------------------------------------------
# Logging
# -----------------------------------------------------------------------------
logger = logging.getLogger(__name__)

avdt_packet_module.print = lambda *args, **kwargs: logger.info(" ".join(map(str, args)))


class Any:
    """Helper class that will match all other values.
       Use an element of this class in expected packets to match any value
      returned by the Controller stack."""

    def __eq__(self, other) -> bool:
        return True

    def __format__(self, format_spec: str) -> str:
        return "_"

    def __len__(self) -> int:
        return 1

    def show(self, prefix: str = "") -> str:
        return prefix + "_"


class SignalingChannel(EventEmitter):
    connection: Connection
    signaling_l2cap_channel: Optional[l2cap.ClassicChannel] = None
    rtp_l2cap_channel: Optional[l2cap.ClassicChannel] = None
    l2cap_server: Optional[l2cap.ClassicChannelServer] = None

    def __init__(self, connection: Connection):
        super().__init__()
        self.connection = connection
        self.sig_queue = collections.deque()
        self.sig_queue_event = asyncio.Event()

    async def initiate_signaling_channel(self):
        if self.signaling_l2cap_channel != None:
            logger.error(f'{color("Signaling L2CAP channel already exists", "red")}')
            return False
        self.signaling_l2cap_channel = await self.connection.create_l2cap_channel(spec=l2cap.ClassicChannelSpec(
            psm=avdtp.AVDTP_PSM))
        # Register to receive PDUs from the channel
        self.signaling_l2cap_channel.sink = self.on_pdu
        return True

    async def disconnect_signaling_channel(self):
        if self.signaling_l2cap_channel == None:
            logger.error(f'{color("No connected signaling channel", "red")}')
            return False
        return await self.signaling_l2cap_channel.disconnect()

    async def initiate_rtp_channel(self):
        if self.rtp_l2cap_channel != None:
            logger.error(f'{color("RTP L2CAP channel already exists", "red")}')
            return False
        self.rtp_l2cap_channel = await self.connection.create_l2cap_channel(
            l2cap.ClassicChannelSpec(psm=avdtp.AVDTP_PSM))
        return True

    async def disconnect_rtp_channel(self):
        if self.rtp_l2cap_channel == None:
            logger.error(f'{color("No connected RTP channel", "red")}')
            return False
        return await self.rtp_l2cap_channel.disconnect()

    def accept_signaling_channel(self) -> bool:
        if self.l2cap_server != None:
            logger.error(f'{color("L2CAP server already exists", "red")}')
            return False
        l2cap_server = self.connection.device.l2cap_channel_manager.servers.get(avdtp.AVDTP_PSM)
        if l2cap_server == None:
            self.l2cap_server = self.connection.device.create_l2cap_server(spec=l2cap.ClassicChannelSpec(
                psm=avdtp.AVDTP_PSM))
        else:
            self.l2cap_server = l2cap_server
        self.l2cap_server.on('connection', self.__on_l2cap_connection)
        return True

    def __on_l2cap_connection(self, channel: l2cap.ClassicChannel) -> None:
        logger.info(f"Incoming L2CAP channel: {channel}")

        if self.signaling_l2cap_channel == None:

            def on_channel_open():
                logger.info(f"Signaling opened on channel {self.signaling_l2cap_channel}")
                # Register to receive PDUs from the channel
                self.signaling_l2cap_channel.sink = self.on_pdu
                self.emit('connection')

            def on_channel_close():
                logger.info("Signaling channel closed")
                self.signaling_l2cap_channel = None

            self.signaling_l2cap_channel = channel
            self.signaling_l2cap_channel.on('open', on_channel_open)
            self.signaling_l2cap_channel.on('close', on_channel_close)
        elif self.rtp_l2cap_channel == None:

            def on_channel_open():
                logger.info(f"RTP opened on channel {self.rtp_l2cap_channel}")
                # Register to receive PDUs from the channel
                self.rtp_l2cap_channel.sink = self.on_avdtp_packet

            def on_channel_close():
                logger.info('RTP channel closed')
                self.rtp_l2cap_channel = None

            self.rtp_l2cap_channel = channel
            self.rtp_l2cap_channel.on('open', on_channel_open)
            self.rtp_l2cap_channel.on('close', on_channel_close)

    def on_pdu(self, pdu: bytes):
        self.sig_queue.append(pdu)
        self.sig_queue_event.set()

    def on_avdtp_packet(self, packet):
        rtp_packet = avdtp.MediaPacket.from_bytes(packet)
        logger.debug(f"RTP Packet: {rtp_packet} {rtp_packet.payload[:16].hex()}")
        self.emit('rtp_packet', rtp_packet)

    async def __receive_sig(self) -> bytes:
        while not self.sig_queue:
            await self.sig_queue_event.wait()
            self.sig_queue_event.clear()
        return self.sig_queue.popleft()

    async def expect_sig(self, expected_sig: Union[SignalingPacket, type], timeout: float = 3) -> SignalingPacket:
        packet = await asyncio.wait_for(self.__receive_sig(), timeout=timeout)
        sig = SignalingPacket.parse_all(packet)

        if isinstance(expected_sig, type) and not isinstance(sig, expected_sig):
            print("received unexpected event")
            print(f"expected event: {expected_sig.__class__.__name__}")
            print("received event:")
            sig.show()
            assert False

        if isinstance(expected_sig, SignalingPacket) and sig != expected_sig:
            print("received unexpected event")
            print(f"expected event:")
            expected_sig.show()
            print("received event:")
            sig.show()
            assert False

        return sig

    def send_message(self, packet: SignalingPacket) -> None:
        self.signaling_l2cap_channel.send_pdu(packet.serialize())

    def send_command(self, command: SignalingPacket):
        self.send_message(command)

    def send_media(self, packet: avdtp.MediaPacket) -> None:
        self.rtp_l2cap_channel.send_pdu(bytes(packet))
