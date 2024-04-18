import argparse
import avatar
import logging
import os
import sys

from argparse import Namespace
from avatar import PandoraDevices
from avatar.pandora_server import BumblePandoraServer
from mobly import base_instrumentation_test, test_runner
from mobly.controllers import android_device
from typing import List, Optional, Tuple

from bumble import pandora as bumble_server
from bumble_experimental.asha import AshaService
from bumble_experimental.dck import DckService
from bumble_experimental.gatt import GATTService

from pandora_experimental.asha_grpc_aio import add_AshaServicer_to_server
from pandora_experimental.dck_grpc_aio import add_DckServicer_to_server
from pandora_experimental.gatt_grpc_aio import add_GATTServicer_to_server


_BUMBLE_BTSNOOP_FMT = 'bumble_btsnoop_{pid}_{instance}.log'


class InstrumentationTest(base_instrumentation_test.BaseInstrumentationTestClass):
    dut: android_device.AndroidDevice

    def setup_class(self):
        self.dut = self.register_controller(android_device)[0]

        devices = self.register_controller(BumblePandoraServer.MOBLY_CONTROLLER_MODULE)
        self.servers = []
        for device in devices:
            server = BumblePandoraServer(device)
            self.servers.append(server)
            client = server.start()
            self.client = client
            print(client.config)
            port = client.grpc_target.rsplit(':', 1)[1]
            self.dut.adb.reverse([f'tcp:7999', f'tcp:{port}'])

    @avatar.asynchronous
    async def setup_test(self) -> None:
        #await self.client.reset()
        pass

    def test_instrumentation(self):
        self.run_instrumentation_test(self.dut, 'android.bluetooth', options={
            'log': 'true'
        }, runner='androidx.test.runner.AndroidJUnitRunner')

def _parse_cli_args() -> Tuple[Namespace, List[str]]:
    parser = argparse.ArgumentParser(description='BumbleBluetoothTests runner.')
    parser.add_argument('-o', '--log_path', type=str, metavar='<PATH>', help='Path to the test configuration file.')
    return parser.parse_known_args()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # This is a hack for `tradefed` because of `b/166468397`.
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]

    # Enable bumble snoop logger.
    ns, argv = _parse_cli_args()
    if ns.log_path:
        os.environ.setdefault('BUMBLE_SNOOPER', f'btsnoop:file:{ns.log_path}/{_BUMBLE_BTSNOOP_FMT}')

    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_AshaServicer_to_server(AshaService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_DckServicer_to_server(DckService(bumble.device), server))
    bumble_server.register_servicer_hook(
        lambda bumble, _, server: add_GATTServicer_to_server(GATTService(bumble.device), server))

    # Run the test suite.
    test_runner.main()
