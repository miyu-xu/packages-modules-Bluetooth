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


from google.protobuf import empty_pb2
from pandora import mediaplayer_pb2
from pandora.mediaplayer_pb2 import NONE
from typing import Awaitable
from typing import Optional
import grpc
import grpc.aio

class MediaPlayer:
    channel: grpc.aio.Channel

    def __init__(self, channel: grpc.aio.Channel) -> None:
        self.channel = channel

    def Play(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Play',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Stop(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Stop',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Pause(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Pause',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Rewind(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Rewind',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def FastForward(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/FastForward',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Forward(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Forward',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Backward(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/Backward',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetLargeMetadata(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/SetLargeMetadata',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def UpdateQueue(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/UpdateQueue',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def GetShuffleMode(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[mediaplayer_pb2.GetShuffleModeResponse]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/GetShuffleMode',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=mediaplayer_pb2.GetShuffleModeResponse.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetShuffleMode(self, mode: mediaplayer_pb2.ShuffleMode = NONE, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/SetShuffleMode',
            request_serializer=mediaplayer_pb2.SetShuffleModeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(mediaplayer_pb2.SetShuffleModeRequest(mode=mode), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StartTestPlayback(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/StartTestPlayback',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def StopTestPlayback(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Awaitable[empty_pb2.Empty]:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.MediaPlayer/StopTestPlayback',
            request_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(empty_pb2.Empty(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class MediaPlayerServicer:
    async def Play(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Stop(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Pause(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Rewind(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def FastForward(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Forward(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def Backward(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def SetLargeMetadata(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def UpdateQueue(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def GetShuffleMode(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> mediaplayer_pb2.GetShuffleModeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def SetShuffleMode(self, request: mediaplayer_pb2.SetShuffleModeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def StartTestPlayback(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    async def StopTestPlayback(self, request: empty_pb2.Empty, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_MediaPlayerServicer_to_server(servicer: MediaPlayerServicer, server: grpc.aio.Server) -> None:
    rpc_method_handlers = {
        'Play': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Play,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Stop': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Stop,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Pause': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Pause,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Rewind': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Rewind,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'FastForward': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.FastForward,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Forward': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Forward,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'Backward': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Backward,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SetLargeMetadata': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetLargeMetadata,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'UpdateQueue': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.UpdateQueue,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'GetShuffleMode': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.GetShuffleMode,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=mediaplayer_pb2.GetShuffleModeResponse.SerializeToString,  # type: ignore
        ),
        'SetShuffleMode': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetShuffleMode,
            request_deserializer=mediaplayer_pb2.SetShuffleModeRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'StartTestPlayback': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.StartTestPlayback,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'StopTestPlayback': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.StopTestPlayback,
            request_deserializer=empty_pb2.Empty.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.MediaPlayer', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
