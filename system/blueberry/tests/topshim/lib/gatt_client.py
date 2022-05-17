#!/usr/bin/env python3
#
#   Copyright 2022 - The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import asyncio
import grpc

from blueberry.facade.topshim import facade_pb2
from blueberry.facade.topshim import facade_pb2_grpc

from google.protobuf import empty_pb2 as empty_proto


class GattClient():
    """
    Wrapper gRPC interface to the GATT Service
    """
    # Timeout for async wait
    DEFAULT_TIMEOUT = 2
    __task_list = []
    __channel = None
    __gatt_stub = None
    __adapter_event_stream = None

    def __init__(self, port=8999):
        self.__channel = grpc.aio.insecure_channel("localhost:%d" % port)
        self.__gatt_stub = facade_pb2_grpc.GattServiceStub(self.__channel)
        #self.__gatt_event_stream = self.__gatt_stub.FetchEvents(facade_pb2.FetchEventsRequest())

    async def terminate(self):
        for task in self.__task_list:
            task.cancel()
            task = None
        self.__task_list.clear()
        await self.__channel.close()

    async def start_advertising_set(self):
        """
        """
        await self.__gatt_stub.StartAdvertisingSet(empty_proto.Empty())

    async def stop_advertising_set(self):
        """
        """
        await self.__gatt_stub.StopAdvertisingSet(empty_proto.Empty())
