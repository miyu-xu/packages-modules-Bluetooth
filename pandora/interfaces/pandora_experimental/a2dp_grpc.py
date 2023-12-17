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
from google.protobuf import wrappers_pb2
from pandora import a2dp_pb2
from pandora import host_pb2
from typing import Generator
from typing import Iterator
from typing import Optional
import grpc

class A2DP:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def OpenSource(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.OpenSourceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/OpenSource',
            request_serializer=a2dp_pb2.OpenSourceRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.OpenSourceResponse.FromString  # type: ignore
        )(a2dp_pb2.OpenSourceRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def OpenSink(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.OpenSinkResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/OpenSink',
            request_serializer=a2dp_pb2.OpenSinkRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.OpenSinkResponse.FromString  # type: ignore
        )(a2dp_pb2.OpenSinkRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitSource(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.WaitSourceResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/WaitSource',
            request_serializer=a2dp_pb2.WaitSourceRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.WaitSourceResponse.FromString  # type: ignore
        )(a2dp_pb2.WaitSourceRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def WaitSink(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.WaitSinkResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/WaitSink',
            request_serializer=a2dp_pb2.WaitSinkRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.WaitSinkResponse.FromString  # type: ignore
        )(a2dp_pb2.WaitSinkRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def IsSuspended(self, sink: Optional[a2dp_pb2.Sink] = None, source: Optional[a2dp_pb2.Source] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> wrappers_pb2.BoolValue:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/IsSuspended',
            request_serializer=a2dp_pb2.IsSuspendedRequest.SerializeToString,  # type: ignore
            response_deserializer=wrappers_pb2.BoolValue.FromString  # type: ignore
        )(a2dp_pb2.IsSuspendedRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Start(self, sink: Optional[a2dp_pb2.Sink] = None, source: Optional[a2dp_pb2.Source] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.StartResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Start',
            request_serializer=a2dp_pb2.StartRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.StartResponse.FromString  # type: ignore
        )(a2dp_pb2.StartRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Suspend(self, sink: Optional[a2dp_pb2.Sink] = None, source: Optional[a2dp_pb2.Source] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.SuspendResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Suspend',
            request_serializer=a2dp_pb2.SuspendRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.SuspendResponse.FromString  # type: ignore
        )(a2dp_pb2.SuspendRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def Close(self, sink: Optional[a2dp_pb2.Sink] = None, source: Optional[a2dp_pb2.Source] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.CloseResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/Close',
            request_serializer=a2dp_pb2.CloseRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.CloseResponse.FromString  # type: ignore
        )(a2dp_pb2.CloseRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def GetAudioEncoding(self, sink: Optional[a2dp_pb2.Sink] = None, source: Optional[a2dp_pb2.Source] = None, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> a2dp_pb2.GetAudioEncodingResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.A2DP/GetAudioEncoding',
            request_serializer=a2dp_pb2.GetAudioEncodingRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.GetAudioEncodingResponse.FromString  # type: ignore
        )(a2dp_pb2.GetAudioEncodingRequest(sink=sink, source=source), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def PlaybackAudio(self, iterator: Iterator[a2dp_pb2.PlaybackAudioRequest], timeout: Optional[float] = None) -> a2dp_pb2.PlaybackAudioResponse:
        return self.channel.stream_unary(  # type: ignore
            '/pandora.A2DP/PlaybackAudio',
            request_serializer=a2dp_pb2.PlaybackAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.PlaybackAudioResponse.FromString  # type: ignore
        )(iterator)

    def CaptureAudio(self, sink: a2dp_pb2.Sink = a2dp_pb2.Sink(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> Stream[a2dp_pb2.CaptureAudioResponse]:
        return self.channel.unary_stream(  # type: ignore
            '/pandora.A2DP/CaptureAudio',
            request_serializer=a2dp_pb2.CaptureAudioRequest.SerializeToString,  # type: ignore
            response_deserializer=a2dp_pb2.CaptureAudioResponse.FromString  # type: ignore
        )(a2dp_pb2.CaptureAudioRequest(sink=sink), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class A2DPServicer:
    def OpenSource(self, request: a2dp_pb2.OpenSourceRequest, context: grpc.ServicerContext) -> a2dp_pb2.OpenSourceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def OpenSink(self, request: a2dp_pb2.OpenSinkRequest, context: grpc.ServicerContext) -> a2dp_pb2.OpenSinkResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitSource(self, request: a2dp_pb2.WaitSourceRequest, context: grpc.ServicerContext) -> a2dp_pb2.WaitSourceResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def WaitSink(self, request: a2dp_pb2.WaitSinkRequest, context: grpc.ServicerContext) -> a2dp_pb2.WaitSinkResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def IsSuspended(self, request: a2dp_pb2.IsSuspendedRequest, context: grpc.ServicerContext) -> wrappers_pb2.BoolValue:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Start(self, request: a2dp_pb2.StartRequest, context: grpc.ServicerContext) -> a2dp_pb2.StartResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Suspend(self, request: a2dp_pb2.SuspendRequest, context: grpc.ServicerContext) -> a2dp_pb2.SuspendResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def Close(self, request: a2dp_pb2.CloseRequest, context: grpc.ServicerContext) -> a2dp_pb2.CloseResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def GetAudioEncoding(self, request: a2dp_pb2.GetAudioEncodingRequest, context: grpc.ServicerContext) -> a2dp_pb2.GetAudioEncodingResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def PlaybackAudio(self, request: Iterator[a2dp_pb2.PlaybackAudioRequest], context: grpc.ServicerContext) -> a2dp_pb2.PlaybackAudioResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def CaptureAudio(self, request: a2dp_pb2.CaptureAudioRequest, context: grpc.ServicerContext) -> Generator[a2dp_pb2.CaptureAudioResponse, None, None]:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")
        yield a2dp_pb2.CaptureAudioResponse()  # no-op: to make the linter happy


def add_A2DPServicer_to_server(servicer: A2DPServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'OpenSource': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenSource,
            request_deserializer=a2dp_pb2.OpenSourceRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.OpenSourceResponse.SerializeToString,  # type: ignore
        ),
        'OpenSink': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.OpenSink,
            request_deserializer=a2dp_pb2.OpenSinkRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.OpenSinkResponse.SerializeToString,  # type: ignore
        ),
        'WaitSource': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitSource,
            request_deserializer=a2dp_pb2.WaitSourceRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.WaitSourceResponse.SerializeToString,  # type: ignore
        ),
        'WaitSink': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.WaitSink,
            request_deserializer=a2dp_pb2.WaitSinkRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.WaitSinkResponse.SerializeToString,  # type: ignore
        ),
        'IsSuspended': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.IsSuspended,
            request_deserializer=a2dp_pb2.IsSuspendedRequest.FromString,  # type: ignore
            response_serializer=wrappers_pb2.BoolValue.SerializeToString,  # type: ignore
        ),
        'Start': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Start,
            request_deserializer=a2dp_pb2.StartRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.StartResponse.SerializeToString,  # type: ignore
        ),
        'Suspend': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Suspend,
            request_deserializer=a2dp_pb2.SuspendRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.SuspendResponse.SerializeToString,  # type: ignore
        ),
        'Close': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.Close,
            request_deserializer=a2dp_pb2.CloseRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.CloseResponse.SerializeToString,  # type: ignore
        ),
        'GetAudioEncoding': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.GetAudioEncoding,
            request_deserializer=a2dp_pb2.GetAudioEncodingRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.GetAudioEncodingResponse.SerializeToString,  # type: ignore
        ),
        'PlaybackAudio': grpc.stream_unary_rpc_method_handler(  # type: ignore
            servicer.PlaybackAudio,
            request_deserializer=a2dp_pb2.PlaybackAudioRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.PlaybackAudioResponse.SerializeToString,  # type: ignore
        ),
        'CaptureAudio': grpc.unary_stream_rpc_method_handler(  # type: ignore
            servicer.CaptureAudio,
            request_deserializer=a2dp_pb2.CaptureAudioRequest.FromString,  # type: ignore
            response_serializer=a2dp_pb2.CaptureAudioResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.A2DP', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore
