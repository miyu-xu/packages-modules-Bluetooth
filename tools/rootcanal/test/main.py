from importlib import resources
from pathlib import Path
import tempfile

# Python is not able to load the module lib_rootcanal_python3.so
# when the test target is configured with embedded_launcher: true.
# This code loads the file to a temporary directory and adds the
# path to the sys lookup.
with tempfile.TemporaryDirectory() as cache:
    with (Path('lib_rootcanal_python3.so').open('rb') as fin,
          Path(cache, 'lib_rootcanal_python3.so').open('wb') as fout):
        fout.write(fin.read())
    sys.path.append(cache)
    import lib_rootcanal_python3

import unittest
import test.LL.DDI.SCN.BV_13_C

if __name__ == "__main__":
    unittest.TextTestRunner(verbosity=3).run(unittest.defaultTestLoader.loadTestsFromModule(test.LL.DDI.SCN.BV_13_C))
