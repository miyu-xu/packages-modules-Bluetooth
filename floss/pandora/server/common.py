import os, sys

client_dir = '/usr/local/autotest'
sys.path.insert(0, client_dir)

import setup_modules

sys.path.pop(0)
setup_modules.setup(base_path=client_dir, root_module_name="autotest_lib.client")
