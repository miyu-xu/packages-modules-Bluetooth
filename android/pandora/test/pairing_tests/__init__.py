from inspect import (
    getmembers,
    isclass,
)

import importlib
import os
import pathlib

from mobly import base_test

def _scan_test_sources():
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
    return  _cur_mod_name + '.' + source_path.replace('.py', '').replace('/', '.')

def _scan_test_classes_from_mod(m):
    ret = []
    for name, member in getmembers(m):
        if isclass(member) and issubclass(member, base_test.BaseTestClass):
            ret.append(member)
    return ret

def get_test_class_list():
    ret = []
    test_sources = _scan_test_sources()
    for ts in test_sources:
        modpath = _source_to_modpath(ts)
        mod = importlib.import_module(modpath)
        ret += _scan_test_classes_from_mod(mod)
    
    return ret

