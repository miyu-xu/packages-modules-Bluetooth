#!/usr/bin/env python3

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

"""Build Pandora gRPC Bluetooth test interfaces."""

import os
import pkg_resources
from grpc_tools import protoc

build_directory = os.path.dirname(os.path.realpath(__file__))
package_directory = f'{build_directory}/..'
pandora_experimental_directory = f'{package_directory}/..'
pandora_directory = f'{package_directory}/../../../../../../external/pandora/bt-test-interfaces'
proto_directory = f'{pandora_experimental_directory}/pandora_experimental'


def build():

    os.environ['PATH'] = build_directory + ':' + os.environ['PATH']

    proto_include = pkg_resources.resource_filename('grpc_tools', '_proto')

    files = [
        f'pandora_experimental/{f}' for f in os.listdir(proto_directory) if f.endswith('.proto')]
    protoc.main([
        'grpc_tools.protoc',
        f'-I{pandora_experimental_directory}',
        f'-I{pandora_directory}',
        f'-I{proto_include}',
        f'--plugin=protoc-gen-grpc={pandora_directory}/python/_build/protoc-gen-custom_grpc',
        f'--python_out={package_directory}',
        f'--grpc_out={package_directory}',
    ] + files)


if __name__ == '__main__':
    build()
