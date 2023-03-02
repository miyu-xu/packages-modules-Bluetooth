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


from google.protobuf import empty_pb2
from pandora import host_pb2
from pandora_experimental import hfp_pb2

from pandora_experimental._utils import unwrap, Sender, Stream, StreamStream



class AudioPath(enum.IntEnum):
    AUDIO_PATH_UNKNOWN = 0
    AUDIO_PATH_SPEAKERS = 1
    AUDIO_PATH_HANDSFREE = 2


@dataclass
class EnableSlcRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(EnableSlcRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.EnableSlcRequest(*args, **kwargs))  # type: ignore

@dataclass
class DisableSlcRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(DisableSlcRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.DisableSlcRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetBatteryLevelRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    battery_percentage: int = 0

setattr(SetBatteryLevelRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetBatteryLevelRequest(*args, **kwargs))  # type: ignore

@dataclass
class AnswerCallRequest(Message):
    pass

setattr(AnswerCallRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.AnswerCallRequest(*args, **kwargs))  # type: ignore

@dataclass
class AnswerCallResponse(Message):
    pass

setattr(AnswerCallResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.AnswerCallResponse(*args, **kwargs))  # type: ignore

@dataclass
class DeclineCallRequest(Message):
    pass

setattr(DeclineCallRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.DeclineCallRequest(*args, **kwargs))  # type: ignore

@dataclass
class DeclineCallResponse(Message):
    pass

setattr(DeclineCallResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.DeclineCallResponse(*args, **kwargs))  # type: ignore

@dataclass
class SetAudioPathRequest(Message):
    audio_path: AudioPath = AudioPath.AUDIO_PATH_UNKNOWN

setattr(SetAudioPathRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetAudioPathRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetAudioPathResponse(Message):
    pass

setattr(SetAudioPathResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetAudioPathResponse(*args, **kwargs))  # type: ignore

@dataclass
class SwapActiveCallRequest(Message):
    pass

setattr(SwapActiveCallRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SwapActiveCallRequest(*args, **kwargs))  # type: ignore

@dataclass
class SwapActiveCallResponse(Message):
    pass

setattr(SwapActiveCallResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SwapActiveCallResponse(*args, **kwargs))  # type: ignore

@dataclass
class SetInBandRingtoneRequest(Message):
    enabled: bool = False

setattr(SetInBandRingtoneRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetInBandRingtoneRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetInBandRingtoneResponse(Message):
    pass

setattr(SetInBandRingtoneResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetInBandRingtoneResponse(*args, **kwargs))  # type: ignore

@dataclass
class MakeCallRequest(Message):
    number: str = ''

setattr(MakeCallRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.MakeCallRequest(*args, **kwargs))  # type: ignore

@dataclass
class MakeCallResponse(Message):
    pass

setattr(MakeCallResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.MakeCallResponse(*args, **kwargs))  # type: ignore

@dataclass
class SetVoiceRecognitionRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    enabled: bool = False

setattr(SetVoiceRecognitionRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetVoiceRecognitionRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetVoiceRecognitionResponse(Message):
    pass

setattr(SetVoiceRecognitionResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetVoiceRecognitionResponse(*args, **kwargs))  # type: ignore

@dataclass
class ClearCallHistoryRequest(Message):
    pass

setattr(ClearCallHistoryRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.ClearCallHistoryRequest(*args, **kwargs))  # type: ignore

@dataclass
class ClearCallHistoryResponse(Message):
    pass

setattr(ClearCallHistoryResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.ClearCallHistoryResponse(*args, **kwargs))  # type: ignore

@dataclass
class AnswerCallAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(AnswerCallAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.AnswerCallAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class AnswerCallAsHandsfreeResponse(Message):
    pass

setattr(AnswerCallAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.AnswerCallAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class EndCallAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(EndCallAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.EndCallAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class EndCallAsHandsfreeResponse(Message):
    pass

setattr(EndCallAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.EndCallAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class DeclineCallAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(DeclineCallAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.DeclineCallAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class DeclineCallAsHandsfreeResponse(Message):
    pass

setattr(DeclineCallAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.DeclineCallAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class ConnectToAudioAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(ConnectToAudioAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.ConnectToAudioAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class ConnectToAudioAsHandsfreeResponse(Message):
    pass

setattr(ConnectToAudioAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.ConnectToAudioAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class DisconnectFromAudioAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(DisconnectFromAudioAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.DisconnectFromAudioAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class DisconnectFromAudioAsHandsfreeResponse(Message):
    pass

setattr(DisconnectFromAudioAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.DisconnectFromAudioAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class MakeCallAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    number: str = ''

setattr(MakeCallAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.MakeCallAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class MakeCallAsHandsfreeResponse(Message):
    pass

setattr(MakeCallAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.MakeCallAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class CallTransferAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(CallTransferAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.CallTransferAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class CallTransferAsHandsfreeResponse(Message):
    pass

setattr(CallTransferAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.CallTransferAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class EnableSlcAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(EnableSlcAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.EnableSlcAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class DisableSlcAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()

setattr(DisableSlcAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.DisableSlcAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetVoiceRecognitionAsHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    enabled: bool = False

setattr(SetVoiceRecognitionAsHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class SetVoiceRecognitionAsHandsfreeResponse(Message):
    pass

setattr(SetVoiceRecognitionAsHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse(*args, **kwargs))  # type: ignore

@dataclass
class SendDtmfFromHandsfreeRequest(Message):
    connection: 'host_pb2.Connection' = host_pb2.Connection()
    code: int = 0

setattr(SendDtmfFromHandsfreeRequest, '__new__', lambda _, *args, **kwargs: hfp_pb2.SendDtmfFromHandsfreeRequest(*args, **kwargs))  # type: ignore

@dataclass
class SendDtmfFromHandsfreeResponse(Message):
    pass

setattr(SendDtmfFromHandsfreeResponse, '__new__', lambda _, *args, **kwargs: hfp_pb2.SendDtmfFromHandsfreeResponse(*args, **kwargs))  # type: ignore


class HFP:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel):
        self.channel = channel

    def EnableSlc(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EnableSlc',
            request_serializer=hfp_pb2.EnableSlcRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.EnableSlcRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisableSlc(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisableSlc',
            request_serializer=hfp_pb2.DisableSlcRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.DisableSlcRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetBatteryLevel(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), battery_percentage: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetBatteryLevel',
            request_serializer=hfp_pb2.SetBatteryLevelRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.SetBatteryLevelRequest(connection=connection, battery_percentage=battery_percentage), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def MakeCall(self, number: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> MakeCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/MakeCall',
            request_serializer=hfp_pb2.MakeCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.MakeCallResponse.FromString  # type: ignore
        )(hfp_pb2.MakeCallRequest(number=number), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AnswerCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> AnswerCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/AnswerCall',
            request_serializer=hfp_pb2.AnswerCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.AnswerCallResponse.FromString  # type: ignore
        )(hfp_pb2.AnswerCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DeclineCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DeclineCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DeclineCall',
            request_serializer=hfp_pb2.DeclineCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DeclineCallResponse.FromString  # type: ignore
        )(hfp_pb2.DeclineCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetAudioPath(self, audio_path: AudioPath = AudioPath.AUDIO_PATH_UNKNOWN, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SetAudioPathResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetAudioPath',
            request_serializer=hfp_pb2.SetAudioPathRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetAudioPathResponse.FromString  # type: ignore
        )(hfp_pb2.SetAudioPathRequest(audio_path=audio_path), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SwapActiveCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SwapActiveCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SwapActiveCall',
            request_serializer=hfp_pb2.SwapActiveCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SwapActiveCallResponse.FromString  # type: ignore
        )(hfp_pb2.SwapActiveCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetInBandRingtone(self, enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SetInBandRingtoneResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetInBandRingtone',
            request_serializer=hfp_pb2.SetInBandRingtoneRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetInBandRingtoneResponse.FromString  # type: ignore
        )(hfp_pb2.SetInBandRingtoneRequest(enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetVoiceRecognition(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SetVoiceRecognitionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetVoiceRecognition',
            request_serializer=hfp_pb2.SetVoiceRecognitionRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetVoiceRecognitionResponse.FromString  # type: ignore
        )(hfp_pb2.SetVoiceRecognitionRequest(connection=connection, enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ClearCallHistory(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ClearCallHistoryResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/ClearCallHistory',
            request_serializer=hfp_pb2.ClearCallHistoryRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.ClearCallHistoryResponse.FromString  # type: ignore
        )(hfp_pb2.ClearCallHistoryRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AnswerCallAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> AnswerCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/AnswerCallAsHandsfree',
            request_serializer=hfp_pb2.AnswerCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.AnswerCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.AnswerCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def EndCallAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> EndCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EndCallAsHandsfree',
            request_serializer=hfp_pb2.EndCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.EndCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.EndCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DeclineCallAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DeclineCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DeclineCallAsHandsfree',
            request_serializer=hfp_pb2.DeclineCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DeclineCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.DeclineCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ConnectToAudioAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> ConnectToAudioAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/ConnectToAudioAsHandsfree',
            request_serializer=hfp_pb2.ConnectToAudioAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.ConnectToAudioAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.ConnectToAudioAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisconnectFromAudioAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> DisconnectFromAudioAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisconnectFromAudioAsHandsfree',
            request_serializer=hfp_pb2.DisconnectFromAudioAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DisconnectFromAudioAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.DisconnectFromAudioAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def MakeCallAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), number: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> MakeCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/MakeCallAsHandsfree',
            request_serializer=hfp_pb2.MakeCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.MakeCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.MakeCallAsHandsfreeRequest(connection=connection, number=number), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def CallTransferAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> CallTransferAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/CallTransferAsHandsfree',
            request_serializer=hfp_pb2.CallTransferAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.CallTransferAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.CallTransferAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def EnableSlcAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EnableSlcAsHandsfree',
            request_serializer=hfp_pb2.EnableSlcAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.EnableSlcAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisableSlcAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisableSlcAsHandsfree',
            request_serializer=hfp_pb2.DisableSlcAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.DisableSlcAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetVoiceRecognitionAsHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SetVoiceRecognitionAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetVoiceRecognitionAsHandsfree',
            request_serializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest(connection=connection, enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendDtmfFromHandsfree(self, connection: 'host_pb2.Connection' = host_pb2.Connection(), code: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> SendDtmfFromHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SendDtmfFromHandsfree',
            request_serializer=hfp_pb2.SendDtmfFromHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SendDtmfFromHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.SendDtmfFromHandsfreeRequest(connection=connection, code=code), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class HFPServicer:
    def EnableSlc(self, request: EnableSlcRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisableSlc(self, request: DisableSlcRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetBatteryLevel(self, request: SetBatteryLevelRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def MakeCall(self, request: MakeCallRequest, context: grpc.ServicerContext) -> MakeCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AnswerCall(self, request: AnswerCallRequest, context: grpc.ServicerContext) -> AnswerCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DeclineCall(self, request: DeclineCallRequest, context: grpc.ServicerContext) -> DeclineCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetAudioPath(self, request: SetAudioPathRequest, context: grpc.ServicerContext) -> SetAudioPathResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SwapActiveCall(self, request: SwapActiveCallRequest, context: grpc.ServicerContext) -> SwapActiveCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetInBandRingtone(self, request: SetInBandRingtoneRequest, context: grpc.ServicerContext) -> SetInBandRingtoneResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetVoiceRecognition(self, request: SetVoiceRecognitionRequest, context: grpc.ServicerContext) -> SetVoiceRecognitionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ClearCallHistory(self, request: ClearCallHistoryRequest, context: grpc.ServicerContext) -> ClearCallHistoryResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AnswerCallAsHandsfree(self, request: AnswerCallAsHandsfreeRequest, context: grpc.ServicerContext) -> AnswerCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def EndCallAsHandsfree(self, request: EndCallAsHandsfreeRequest, context: grpc.ServicerContext) -> EndCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DeclineCallAsHandsfree(self, request: DeclineCallAsHandsfreeRequest, context: grpc.ServicerContext) -> DeclineCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ConnectToAudioAsHandsfree(self, request: ConnectToAudioAsHandsfreeRequest, context: grpc.ServicerContext) -> ConnectToAudioAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisconnectFromAudioAsHandsfree(self, request: DisconnectFromAudioAsHandsfreeRequest, context: grpc.ServicerContext) -> DisconnectFromAudioAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def MakeCallAsHandsfree(self, request: MakeCallAsHandsfreeRequest, context: grpc.ServicerContext) -> MakeCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def CallTransferAsHandsfree(self, request: CallTransferAsHandsfreeRequest, context: grpc.ServicerContext) -> CallTransferAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def EnableSlcAsHandsfree(self, request: EnableSlcAsHandsfreeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisableSlcAsHandsfree(self, request: DisableSlcAsHandsfreeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetVoiceRecognitionAsHandsfree(self, request: SetVoiceRecognitionAsHandsfreeRequest, context: grpc.ServicerContext) -> SetVoiceRecognitionAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SendDtmfFromHandsfree(self, request: SendDtmfFromHandsfreeRequest, context: grpc.ServicerContext) -> SendDtmfFromHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")


def add_HFPServicer_to_server(servicer: HFPServicer, server: grpc.Server) -> None:
    rpc_method_handlers = {
        'EnableSlc': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.EnableSlc,
            request_deserializer=hfp_pb2.EnableSlcRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'DisableSlc': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DisableSlc,
            request_deserializer=hfp_pb2.DisableSlcRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SetBatteryLevel': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetBatteryLevel,
            request_deserializer=hfp_pb2.SetBatteryLevelRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'MakeCall': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.MakeCall,
            request_deserializer=hfp_pb2.MakeCallRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.MakeCallResponse.SerializeToString,  # type: ignore
        ),
        'AnswerCall': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AnswerCall,
            request_deserializer=hfp_pb2.AnswerCallRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.AnswerCallResponse.SerializeToString,  # type: ignore
        ),
        'DeclineCall': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DeclineCall,
            request_deserializer=hfp_pb2.DeclineCallRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.DeclineCallResponse.SerializeToString,  # type: ignore
        ),
        'SetAudioPath': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetAudioPath,
            request_deserializer=hfp_pb2.SetAudioPathRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SetAudioPathResponse.SerializeToString,  # type: ignore
        ),
        'SwapActiveCall': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SwapActiveCall,
            request_deserializer=hfp_pb2.SwapActiveCallRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SwapActiveCallResponse.SerializeToString,  # type: ignore
        ),
        'SetInBandRingtone': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetInBandRingtone,
            request_deserializer=hfp_pb2.SetInBandRingtoneRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SetInBandRingtoneResponse.SerializeToString,  # type: ignore
        ),
        'SetVoiceRecognition': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetVoiceRecognition,
            request_deserializer=hfp_pb2.SetVoiceRecognitionRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SetVoiceRecognitionResponse.SerializeToString,  # type: ignore
        ),
        'ClearCallHistory': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ClearCallHistory,
            request_deserializer=hfp_pb2.ClearCallHistoryRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.ClearCallHistoryResponse.SerializeToString,  # type: ignore
        ),
        'AnswerCallAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.AnswerCallAsHandsfree,
            request_deserializer=hfp_pb2.AnswerCallAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.AnswerCallAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'EndCallAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.EndCallAsHandsfree,
            request_deserializer=hfp_pb2.EndCallAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.EndCallAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'DeclineCallAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DeclineCallAsHandsfree,
            request_deserializer=hfp_pb2.DeclineCallAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.DeclineCallAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'ConnectToAudioAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.ConnectToAudioAsHandsfree,
            request_deserializer=hfp_pb2.ConnectToAudioAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.ConnectToAudioAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'DisconnectFromAudioAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DisconnectFromAudioAsHandsfree,
            request_deserializer=hfp_pb2.DisconnectFromAudioAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.DisconnectFromAudioAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'MakeCallAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.MakeCallAsHandsfree,
            request_deserializer=hfp_pb2.MakeCallAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.MakeCallAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'CallTransferAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.CallTransferAsHandsfree,
            request_deserializer=hfp_pb2.CallTransferAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.CallTransferAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'EnableSlcAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.EnableSlcAsHandsfree,
            request_deserializer=hfp_pb2.EnableSlcAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'DisableSlcAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.DisableSlcAsHandsfree,
            request_deserializer=hfp_pb2.DisableSlcAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=empty_pb2.Empty.SerializeToString,  # type: ignore
        ),
        'SetVoiceRecognitionAsHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SetVoiceRecognitionAsHandsfree,
            request_deserializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse.SerializeToString,  # type: ignore
        ),
        'SendDtmfFromHandsfree': grpc.unary_unary_rpc_method_handler(  # type: ignore
            servicer.SendDtmfFromHandsfree,
            request_deserializer=hfp_pb2.SendDtmfFromHandsfreeRequest.FromString,  # type: ignore
            response_serializer=hfp_pb2.SendDtmfFromHandsfreeResponse.SerializeToString,  # type: ignore
        ),
    
    }
    generic_handler = grpc.method_handlers_generic_handler(  # type: ignore
        'pandora.HFP', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))  # type: ignore


