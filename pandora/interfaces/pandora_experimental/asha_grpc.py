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



from ._utils import Stream
from google.protobuf import empty_pb2
from pandora import asha_pb2
from pandora import host_pb2
from typing import Generator
from typing import Iterator
from typing import List
from typing import Optional
import grpc

class Asha:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def Register(self, capability: int = 0, hisyncid: List[int] = [], wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.asha.Asha/Register',
            request_serializer=asha_pb2.RegisterRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(asha_pb2.RegisterRequest(capability=capability, hisyncid=hisyncid), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def CaptureAudio(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[asha_pb2.CaptureAudioResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.asha.Asha/CaptureAudio',
            request_serializer=asha_pb2.CaptureAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=asha_pb2.CaptureAudioResponse.FromString  # type: ignore
        )(asha_pb2.CaptureAudioRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Start(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> asha_pb2.StartResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.asha.Asha/Start',
            request_serializer=asha_pb2.StartRequest.SerializeToString,  # type: ignore
            response_deserializer=asha_pb2.StartResponse.FromString  # type: ignore
        )(asha_pb2.StartRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def PlaybackAudio(self, iterator: Iterator[asha_pb2.PlaybackAudioRequest], timeout: Optional[float] = None) -> asha_pb2.PlaybackAudioResponse:
        return self.channel.stream_unary(  # type: ignore
            '/pandora.asha.Asha/PlaybackAudio',
            request_serializer=asha_pb2.PlaybackAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=asha_pb2.PlaybackAudioResponse.FromString  # type: ignore
        )(iterator)

    def Stop(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> asha_pb2.StopResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.asha.Asha/Stop',
            request_serializer=asha_pb2.StopRequest.SerializeToString,  # type: ignore
            response_deserializer=asha_pb2.StopResponse.FromString  # type: ignore
        )(asha_pb2.StopRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitPeripheral(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> asha_pb2.WaitPeripheralResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.asha.Asha/WaitPeripheral',
            request_serializer=asha_pb2.WaitPeripheralRequest.SerializeToString,  # type: ignore
            response_deserializer=asha_pb2.WaitPeripheralResponse.FromString  # type: ignore
        )(asha_pb2.WaitPeripheralRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class AshaServicer:
    def Register(self, request: asha_pb2.RegisterRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def CaptureAudio(self, request: asha_pb2.CaptureAudioRequest, context: grpc.ServicerContext) -> Generator[asha_pb2.CaptureAudioResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield asha_pb2.CaptureAudioResponse()  # no-op: to make the linter happy

    def Start(self, request: asha_pb2.StartRequest, context: grpc.ServicerContext) -> asha_pb2.StartResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def PlaybackAudio(self, request: Iterator[asha_pb2.PlaybackAudioRequest], context: grpc.ServicerContext) -> asha_pb2.PlaybackAudioResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Stop(self, request: asha_pb2.StopRequest, context: grpc.ServicerContext) -> asha_pb2.StopResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitPeripheral(self, request: asha_pb2.WaitPeripheralRequest, context: grpc.ServicerContext) -> asha_pb2.WaitPeripheralResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_AshaServicer_to_server(servicer: AshaServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'Register': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Register,
            request_deserializer=asha_pb2.RegisterRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'CaptureAudio': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.CaptureAudio,
            request_deserializer=asha_pb2.CaptureAudioRequest.FromString,  # type: ignore
            response_serializer=asha_pb2.CaptureAudioResponse.SerializeToString,  # type: ignore
        ),
        'Start': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Start,
            request_deserializer=asha_pb2.StartRequest.FromString,  # type: ignore
            response_serializer=asha_pb2.StartResponse.SerializeToString,  # type: ignore
        ),
        'PlaybackAudio': grpc.stream_unary_rpc_method_handler(  # type: ignore
            servicer.PlaybackAudio,
            request_deserializer=asha_pb2.PlaybackAudioRequest.FromString,  # type: ignore
            response_serializer=asha_pb2.PlaybackAudioResponse.SerializeToString,  # type: ignore
        ),
        'Stop': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Stop,
            request_deserializer=asha_pb2.StopRequest.FromString,  # type: ignore
            response_serializer=asha_pb2.StopResponse.SerializeToString,  # type: ignore
        ),
        'WaitPeripheral': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitPeripheral,
            request_deserializer=asha_pb2.WaitPeripheralRequest.FromString,  # type: ignore
            response_serializer=asha_pb2.WaitPeripheralResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.asha.Asha', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
