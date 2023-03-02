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




from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream







class PBAP:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    


class PBAPServicer:
    pass


def add_PBAPServicer_to_server(servicer: PBAPServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.PBAP', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore


