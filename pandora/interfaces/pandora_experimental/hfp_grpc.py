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
from pandora import hfp_pb2
from pandora import host_pb2
from pandora.hfp_pb2 import AUDIO_PATH_UNKNOWN
from typing import Optional
import grpc

class HFP:
    channel: grpc.Channel

    def __init__(self, channel: grpc.Channel) -> None:
        self.channel = channel

    def EnableSlc(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EnableSlc',
            request_serializer=hfp_pb2.EnableSlcRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.EnableSlcRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisableSlc(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisableSlc',
            request_serializer=hfp_pb2.DisableSlcRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.DisableSlcRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetBatteryLevel(self, connection: host_pb2.Connection = host_pb2.Connection(), battery_percentage: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetBatteryLevel',
            request_serializer=hfp_pb2.SetBatteryLevelRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.SetBatteryLevelRequest(connection=connection, battery_percentage=battery_percentage), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def MakeCall(self, number: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.MakeCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/MakeCall',
            request_serializer=hfp_pb2.MakeCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.MakeCallResponse.FromString  # type: ignore
        )(hfp_pb2.MakeCallRequest(number=number), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AnswerCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.AnswerCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/AnswerCall',
            request_serializer=hfp_pb2.AnswerCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.AnswerCallResponse.FromString  # type: ignore
        )(hfp_pb2.AnswerCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DeclineCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.DeclineCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DeclineCall',
            request_serializer=hfp_pb2.DeclineCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DeclineCallResponse.FromString  # type: ignore
        )(hfp_pb2.DeclineCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetAudioPath(self, audio_path: hfp_pb2.AudioPath = AUDIO_PATH_UNKNOWN, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SetAudioPathResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetAudioPath',
            request_serializer=hfp_pb2.SetAudioPathRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetAudioPathResponse.FromString  # type: ignore
        )(hfp_pb2.SetAudioPathRequest(audio_path=audio_path), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SwapActiveCall(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SwapActiveCallResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SwapActiveCall',
            request_serializer=hfp_pb2.SwapActiveCallRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SwapActiveCallResponse.FromString  # type: ignore
        )(hfp_pb2.SwapActiveCallRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetInBandRingtone(self, enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SetInBandRingtoneResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetInBandRingtone',
            request_serializer=hfp_pb2.SetInBandRingtoneRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetInBandRingtoneResponse.FromString  # type: ignore
        )(hfp_pb2.SetInBandRingtoneRequest(enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetVoiceRecognition(self, connection: host_pb2.Connection = host_pb2.Connection(), enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SetVoiceRecognitionResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetVoiceRecognition',
            request_serializer=hfp_pb2.SetVoiceRecognitionRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetVoiceRecognitionResponse.FromString  # type: ignore
        )(hfp_pb2.SetVoiceRecognitionRequest(connection=connection, enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ClearCallHistory(self, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.ClearCallHistoryResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/ClearCallHistory',
            request_serializer=hfp_pb2.ClearCallHistoryRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.ClearCallHistoryResponse.FromString  # type: ignore
        )(hfp_pb2.ClearCallHistoryRequest(), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def AnswerCallAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.AnswerCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/AnswerCallAsHandsfree',
            request_serializer=hfp_pb2.AnswerCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.AnswerCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.AnswerCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def EndCallAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.EndCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EndCallAsHandsfree',
            request_serializer=hfp_pb2.EndCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.EndCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.EndCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DeclineCallAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.DeclineCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DeclineCallAsHandsfree',
            request_serializer=hfp_pb2.DeclineCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DeclineCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.DeclineCallAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def ConnectToAudioAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.ConnectToAudioAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/ConnectToAudioAsHandsfree',
            request_serializer=hfp_pb2.ConnectToAudioAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.ConnectToAudioAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.ConnectToAudioAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisconnectFromAudioAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.DisconnectFromAudioAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisconnectFromAudioAsHandsfree',
            request_serializer=hfp_pb2.DisconnectFromAudioAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.DisconnectFromAudioAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.DisconnectFromAudioAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def MakeCallAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), number: str = '', wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.MakeCallAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/MakeCallAsHandsfree',
            request_serializer=hfp_pb2.MakeCallAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.MakeCallAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.MakeCallAsHandsfreeRequest(connection=connection, number=number), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def CallTransferAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.CallTransferAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/CallTransferAsHandsfree',
            request_serializer=hfp_pb2.CallTransferAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.CallTransferAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.CallTransferAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def EnableSlcAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/EnableSlcAsHandsfree',
            request_serializer=hfp_pb2.EnableSlcAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.EnableSlcAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def DisableSlcAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> empty_pb2.Empty:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/DisableSlcAsHandsfree',
            request_serializer=hfp_pb2.DisableSlcAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=empty_pb2.Empty.FromString  # type: ignore
        )(hfp_pb2.DisableSlcAsHandsfreeRequest(connection=connection), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SetVoiceRecognitionAsHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), enabled: bool = False, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SetVoiceRecognitionAsHandsfree',
            request_serializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest(connection=connection, enabled=enabled), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore

    def SendDtmfFromHandsfree(self, connection: host_pb2.Connection = host_pb2.Connection(), code: int = 0, wait_for_ready: Optional[bool] = None, timeout: Optional[float] = None) -> hfp_pb2.SendDtmfFromHandsfreeResponse:
        return self.channel.unary_unary(  # type: ignore
            '/pandora.HFP/SendDtmfFromHandsfree',
            request_serializer=hfp_pb2.SendDtmfFromHandsfreeRequest.SerializeToString,  # type: ignore
            response_deserializer=hfp_pb2.SendDtmfFromHandsfreeResponse.FromString  # type: ignore
        )(hfp_pb2.SendDtmfFromHandsfreeRequest(connection=connection, code=code), wait_for_ready=wait_for_ready, timeout=timeout)  # type: ignore


class HFPServicer:
    def EnableSlc(self, request: hfp_pb2.EnableSlcRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisableSlc(self, request: hfp_pb2.DisableSlcRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetBatteryLevel(self, request: hfp_pb2.SetBatteryLevelRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def MakeCall(self, request: hfp_pb2.MakeCallRequest, context: grpc.ServicerContext) -> hfp_pb2.MakeCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AnswerCall(self, request: hfp_pb2.AnswerCallRequest, context: grpc.ServicerContext) -> hfp_pb2.AnswerCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DeclineCall(self, request: hfp_pb2.DeclineCallRequest, context: grpc.ServicerContext) -> hfp_pb2.DeclineCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetAudioPath(self, request: hfp_pb2.SetAudioPathRequest, context: grpc.ServicerContext) -> hfp_pb2.SetAudioPathResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SwapActiveCall(self, request: hfp_pb2.SwapActiveCallRequest, context: grpc.ServicerContext) -> hfp_pb2.SwapActiveCallResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetInBandRingtone(self, request: hfp_pb2.SetInBandRingtoneRequest, context: grpc.ServicerContext) -> hfp_pb2.SetInBandRingtoneResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetVoiceRecognition(self, request: hfp_pb2.SetVoiceRecognitionRequest, context: grpc.ServicerContext) -> hfp_pb2.SetVoiceRecognitionResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ClearCallHistory(self, request: hfp_pb2.ClearCallHistoryRequest, context: grpc.ServicerContext) -> hfp_pb2.ClearCallHistoryResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def AnswerCallAsHandsfree(self, request: hfp_pb2.AnswerCallAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.AnswerCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def EndCallAsHandsfree(self, request: hfp_pb2.EndCallAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.EndCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DeclineCallAsHandsfree(self, request: hfp_pb2.DeclineCallAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.DeclineCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def ConnectToAudioAsHandsfree(self, request: hfp_pb2.ConnectToAudioAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.ConnectToAudioAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisconnectFromAudioAsHandsfree(self, request: hfp_pb2.DisconnectFromAudioAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.DisconnectFromAudioAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def MakeCallAsHandsfree(self, request: hfp_pb2.MakeCallAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.MakeCallAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def CallTransferAsHandsfree(self, request: hfp_pb2.CallTransferAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.CallTransferAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def EnableSlcAsHandsfree(self, request: hfp_pb2.EnableSlcAsHandsfreeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def DisableSlcAsHandsfree(self, request: hfp_pb2.DisableSlcAsHandsfreeRequest, context: grpc.ServicerContext) -> empty_pb2.Empty:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SetVoiceRecognitionAsHandsfree(self, request: hfp_pb2.SetVoiceRecognitionAsHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.SetVoiceRecognitionAsHandsfreeResponse:
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)  # type: ignore
        context.set_details("Method not implemented!")  # type: ignore
        raise NotImplementedError("Method not implemented!")

    def SendDtmfFromHandsfree(self, request: hfp_pb2.SendDtmfFromHandsfreeRequest, context: grpc.ServicerContext) -> hfp_pb2.SendDtmfFromHandsfreeResponse:
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
