import asha_test
import example
import gatt_test
import grpc.aio
import logging
import sys

from avatar import bumble_server
from avatar.bumble_device import BumbleDevice
from bumble_experimental.gatt import GATTService
from mobly import suite_runner
from pandora_experimental.gatt_grpc_aio import add_GATTServicer_to_server

_TEST_CLASSES_LIST = [example.ExampleTest, asha_test.ASHATest, gatt_test.GattTest]


def _bumble_servicer_hook(bumble: BumbleDevice, server: grpc.aio.Server) -> None:
    add_GATTServicer_to_server(GATTService(bumble.device), server)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # This is a hack for `tradefed` because of `b/166468397`.
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]

    # Register experimental bumble servicers hook.
    bumble_server.register_servicer_hook(_bumble_servicer_hook)

    # Run the test suite.
    suite_runner.run_suite(_TEST_CLASSES_LIST)  # type: ignore
