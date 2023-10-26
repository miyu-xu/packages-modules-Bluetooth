import site

site.main()

import argparse
import logging
import os
import sys

from argparse import Namespace
from mobly import suite_runner
from typing import List, Tuple

_BUMBLE_BTSNOOP_FMT = 'bumble_btsnoop_{pid}_{instance}.log'

# Import test cases modules.
import asha_test
import avatar.cases.host_test
import avatar.cases.le_host_test
import avatar.cases.le_security_test
import avatar.cases.security_test
import gatt_test
import hfpclient_test
import sdp_test
import smp_test
import pairing_tests.br_edr.legacy.tests as bredr_legacy_tests
import pairing_tests.br_edr.ssp.display_output_and_yes_no_input.tests as bredr_ssp_displayyesno_tests
import pairing_tests.br_edr.ssp.display_output_only.tests as bredr_ssp_displayonly_tests
import pairing_tests.br_edr.ssp.keyboard_input_only.tests as bredr_ssp_keyboardonly_tests
import pairing_tests.br_edr.ssp.no_output_no_input.tests as bredr_ssp_no_output_no_inpu_tests

_TEST_CLASSES_LIST = [
    avatar.cases.host_test.HostTest,
    avatar.cases.le_host_test.LeHostTest,
    avatar.cases.security_test.SecurityTest,
    avatar.cases.le_security_test.LeSecurityTest,
    sdp_test.SdpTest,
    smp_test.SmpTest,
    gatt_test.GattTest,
    asha_test.AshaTest,
    hfpclient_test.HfpClientTest,
    bredr_legacy_tests.BREDRLegacyTestClass,
    bredr_ssp_displayyesno_tests.BREDRDisplayYesNoTestClass,
    bredr_ssp_displayonly_tests.BREDRDisplayOnlyTestClass,
    bredr_ssp_keyboardonly_tests.BREDRKeyboardOnlyTestClass,
    bredr_ssp_no_output_no_inpu_tests.BREDRNoOutputNoInputTestClass,
]

def _parse_cli_args() -> Tuple[Namespace, List[str]]:
    parser = argparse.ArgumentParser(description='Avatar test runner.')
    parser.add_argument('-o', '--log_path', type=str, metavar='<PATH>', help='Path to the test configuration file.')
    return parser.parse_known_args()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # This is a hack for `tradefed` because of `b/166468397`.
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1 :]

    # Enable bumble snoop logger.
    ns, argv = _parse_cli_args()
    if ns.log_path:
        os.environ.setdefault('BUMBLE_SNOOPER', f'btsnoop:file:{ns.log_path}/{_BUMBLE_BTSNOOP_FMT}')

    # Run the test suite.
    suite_runner.run_suite(_TEST_CLASSES_LIST, argv)  # type: ignore
