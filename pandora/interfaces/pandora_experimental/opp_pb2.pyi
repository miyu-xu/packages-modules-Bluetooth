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



from google.protobuf.internal.enum_type_wrapper import EnumTypeWrapper
from google.protobuf.message import Message

class PutStatus(int, EnumTypeWrapper):
  pass

ACCEPTED: PutStatus
DECLINED: PutStatus


class OpenRfcommChannelRequest(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class OpenL2capChannelRequest(Message):
  address: bytes

  def __init__(self, address: bytes = b'') -> None: ...

class AcceptPutOperationResponse(Message):
  status: PutStatus

  def __init__(self, status: PutStatus = ACCEPTED) -> None: ...

