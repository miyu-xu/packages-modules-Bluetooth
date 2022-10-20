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

import os
import sys
import logging
import argparse
import subprocess

from re import sub
from pathlib import Path
from genericpath import exists
from multiprocessing import Process

ANDROID_BUILD_TOP = os.getenv("ANDROID_BUILD_TOP")
TARGET_PRODUCT = os.getenv("TARGET_PRODUCT")
TARGET_BUILD_VARIANT = os.getenv("TARGET_BUILD_VARIANT")
ANDROID_PRODUCT_OUT = os.getenv("ANDROID_PRODUCT_OUT")
PANDORA_CF_APK = Path(
    f'{ANDROID_BUILD_TOP}/out/target/product/vsoc_x86_64/testcases/PandoraServer/x86_64/PandoraServer.apk'
)


def build_pandora_server():
  target = TARGET_PRODUCT if TARGET_BUILD_VARIANT == "release" else f'{TARGET_PRODUCT}-{TARGET_BUILD_VARIANT}'
  logging.debug(f'build_pandora_server: {target}')
  pandora_server_cmd = f'source build/envsetup.sh && lunch {target} && make PandoraServer'
  subprocess.run(pandora_server_cmd,
                 cwd=ANDROID_BUILD_TOP,
                 shell=True,
                 executable='/bin/bash',
                 check=True)

def install_pandora_server(serial):
  logging.debug('Install PandoraServer.apk')
  pandora_apk_path = Path(
      f'{ANDROID_PRODUCT_OUT}/testcases/PandoraServer/x86_64/PandoraServer.apk')
  if not pandora_apk_path.exists():
    logging.error(
        f"PandoraServer apk is not build or the path is wrong: {pandora_apk_path}"
    )
    sys.exit(1)
  install_apk_cmd = ['adb', 'install', '-r', '-g', str(pandora_apk_path)]
  if args.serial != "":
    install_apk_cmd.append(f'-s {serial}')
  subprocess.run(install_apk_cmd, check=True)

def instrument_pandora_server():
  logging.debug('instrument_pandora_server')
  instrument_cmd = 'adb shell am instrument --no-hidden-api-checks -w com.android.pandora/.Main'
  instrument_process = Process(
      target=lambda: subprocess.run(instrument_cmd, shell=True, check=True))
  instrument_process.start()
  return instrument_process

def run_test(args):
  logging.debug(f'run_test config: {args.config} test: {args.test}')
  test_cmd = ['python3', args.test, '-c', args.config]
  p = subprocess.Popen(test_cmd)
  p.wait(timeout=args.timeout)
  p.terminate()

def run(args):
  if not PANDORA_CF_APK.exists() or args.build:
    build_pandora_server()
  install_pandora_server(args.serial)
  instrument_process = instrument_pandora_server()
  run_test(args)
  instrument_process.terminate()


def init_parser():
  '''
  Initialize the parser
  '''
  parser = argparse.ArgumentParser()
  parser.add_argument("-c",
                      "--config",
                      type=str,
                      help="Test config file path",
                      required=True)
  parser.add_argument("-t",
                      "--test",
                      type=str,
                      help="Test script path",
                      required=True)
  parser.add_argument("-b",
                      "--build",
                      help="Build the PandoraServer.apk",
                      action="store_true")
  parser.add_argument("-s",
                      "--serial",
                      type=str,
                      default="",
                      help="Use device with given serial")
  parser.add_argument("-m", "--timeout", type=int, default=1800000, help="Mobly test timeout")
  return parser.parse_args()


if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  args = init_parser()
  run(args)
