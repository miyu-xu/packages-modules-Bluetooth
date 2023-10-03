# Copyright 2023 Google LLC
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

import importlib
import os
import pathlib

from inspect import getmembers, isclass
from mobly import base_test

def _scan_test_sources():
    '''
    all py sources ending with tests.py (e.g., XXXXtests.py)
    are considered as candidates to be scanned
    '''
    cur_dir = os.path.dirname(__file__)
    path = pathlib.Path(cur_dir)
    ret = []
    for f in path.glob('**/*.py'):
        r = f.relative_to(path)
        if r.name.endswith('tests.py'):
            ret.append(str(r))
    return ret


_cur_mod_name = os.path.basename(os.path.dirname(__file__))


def _source_to_modpath(source_path):
    return _cur_mod_name + '.' + source_path.replace('.py', '').replace('/', '.')


def _scan_test_classes_from_mod(m):
    '''
    all subclasses of base_test.BaseTestClass
    are considered as test classes
    '''
    ret = []
    for name, member in getmembers(m):
        if isclass(member) and issubclass(member, base_test.BaseTestClass):
            ret.append(member)
    return ret


def get_test_class_list():
    '''
    scan the sources tree, look for test files
    and scan for test classes in each test file automatically
    '''
    ret = []
    test_sources = _scan_test_sources()
    for ts in test_sources:
        modpath = _source_to_modpath(ts)
        mod = importlib.import_module(modpath)
        ret += _scan_test_classes_from_mod(mod)

    return ret
