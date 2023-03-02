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

"""Generated python gRPC interfaces."""

__version__ = "0.0.1"

import enum
import grpc
import sys

from dataclasses import dataclass, field
from typing import Dict, Generator, Optional, List, Union, Iterator, AsyncGenerator, AsyncIterator, Awaitable, TypeVar

if sys.version_info >= (3, 8):
    from typing import Literal, TypedDict

from google.protobuf.message import Message


from pandora_experimental import hid_pb2
from pandora_experimental.hid_grpc import HidReportType
from pandora_experimental.hid_grpc import SendHostReportRequest, SendHostReportResponse

from pandora_experimental._utils import unwrap, AioSender as Sender, AioStream as Stream, AioStreamStream as StreamStream

class HID:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel):
        self.channel = channel

    def SendHostReport(self, address: bytes = b'', report_type: HidReportType = HidReportType.HID_REPORT_TYPE_UNSPECIFIED, report: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[SendHostReportResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HID/SendHostReport',
            request_serializer=hid_pb2.SendHostReportRequest.SerializeToString,  # type: ignore
            response_deserializer=hid_pb2.SendHostReportResponse.FromString  # type: ignore
        )(hid_pb2.SendHostReportRequest(address=address, report_type=report_type, report=report), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class HIDServicer:
    async def SendHostReport(self, request: SendHostReportRequest, context: grpc.ServicerContext) -> SendHostReportResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_HIDServicer_to_server(servicer: HIDServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'SendHostReport': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendHostReport,
            request_deserializer=hid_pb2.SendHostReportRequest.FromString,  # type: ignore
            response_serializer=hid_pb2.SendHostReportResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.HID', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
