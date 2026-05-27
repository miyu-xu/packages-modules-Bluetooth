#!/usr/bin/env python

import argparse
import dataclasses
import enum
import itertools
import os
import re
import struct
import zipfile
from pathlib import Path
from typing import List, Optional, Set

from btsnoop import Btsnoop
import analyzers.a2dp
import analyzers.asha
import analyzers.avrcp
import analyzers.leaudio
import analyzers.rfcomm
import analyzers.sdp
import analyzers.smp
import analyzers.ssp

class Bugreport:
    BTSNOOP_HCI_PATH = "FS/data/misc/bluetooth/logs/btsnoop_hci.log"
    BTSNOOP_HCI_LAST_PATH = "FS/data/misc/bluetooth/logs/btsnoop_hci.log.last"

    archive: zipfile.ZipFile
    btsnoop_hci: Optional[Btsnoop] = None
    btsnoop_hci_last: Optional[Btsnoop] = None

    def read(self, file_path: str) -> Optional[bytes]:
        try:
            return self.archive.read(file_path)
        except KeyError:
            return None

    def __init__(self, path: Path):
        if path.suffix in ['.log', '.last']:
            with open(path, 'rb') as f:
                self.archive = None
                self.btsnoop_hci = Btsnoop(f.read(), None)

        else:
            self.archive = zipfile.ZipFile(path)
            self.btsnoop_hci = Btsnoop(
                self.read(Bugreport.BTSNOOP_HCI_PATH),
                self.read(Bugreport.BTSNOOP_HCI_LAST_PATH),
            )


def run_a2dp(bugreport: Bugreport, args: argparse.Namespace):
    for acl_connection in bugreport.btsnoop_hci.acl_connections:
        analyzers.a2dp.plot_acl_connection(acl_connection, **vars(args))


def run_asha(bugreport: Bugreport, args: argparse.Namespace):
    for acl_connection in bugreport.btsnoop_hci.le_acl_connections:
        analyzers.asha.plot_acl_connection(acl_connection, **vars(args))


def run_avrcp(bugreport: Bugreport, args: argparse.Namespace):
    for acl_connection in bugreport.btsnoop_hci.acl_connections:
        analyzers.avrcp.plot_acl_connection(acl_connection, **vars(args))


def run_leaudio(bugreport: Bugreport, args: argparse.Namespace):
    analyzers.leaudio.plot_cis_connections(bugreport.btsnoop_hci, **vars(args))


def run_rfcomm(bugreport: Bugreport, args: argparse.Namespace):
    for acl_connection in bugreport.btsnoop_hci.acl_connections:
        analyzers.rfcomm.plot_acl_connection(acl_connection, **vars(args))


def run_sdp(bugreport: Bugreport, args: argparse.Namespace):
    for acl_connection in bugreport.btsnoop_hci.acl_connections:
        analyzers.sdp.plot_acl_connection(acl_connection, **vars(args))


def run_smp(bugreport: Bugreport, args: argparse.Namespace):
    analyzed_handles = set()
    for acl_connection in bugreport.btsnoop_hci.le_acl_connections:
        analyzed_handles.add(acl_connection.connection_handle)
        analyzers.smp.plot_acl_connection(acl_connection, **vars(args))
    # Also scan classic ACL connections for SMP over BR/EDR (CID 0x0007),
    # and for orphaned LE connections that pre-date the log capture.
    for acl_connection in bugreport.btsnoop_hci.acl_connections:
        if acl_connection.connection_handle in analyzed_handles:
            continue
        analyzers.smp.plot_acl_connection(acl_connection, **vars(args))


def run_ssp(bugreport: Bugreport, args: argparse.Namespace):
    analyzers.ssp.plot_btsnoop(bugreport.btsnoop_hci, **vars(args))


def run(args: argparse.Namespace):
    bugreport = Bugreport(args.path)
    args.func(bugreport, args)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers()

    a2dp = subparsers.add_parser('a2dp', description='Extract A2DP profile information')
    a2dp.set_defaults(func=run_a2dp)
    a2dp.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    a2dp.add_argument("--signal-lcid", type=lambda x: int(x,0), help="override the signaling channel LCID")
    a2dp.add_argument("--signal-rcid", type=lambda x: int(x,0), help="override the signaling channel RCID")
    a2dp.add_argument("--stream-cid", type=lambda x: int(x,0), help="override the stream CID")
    a2dp.add_argument("--codec-type", type=str, help="override the codec type")
    a2dp.add_argument("--sampling-frequency", type=int, help="override the sampling frequency")

    asha = subparsers.add_parser('asha', description='Extract ASHA profile information')
    asha.set_defaults(func=run_asha)
    asha.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    asha.add_argument("--psm", type=lambda x: int(x,0), help="override the stream PSM")

    avrcp = subparsers.add_parser('avrcp', description='Extract AVRCP profile information')
    avrcp.set_defaults(func=run_avrcp)
    avrcp.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    avrcp.add_argument("--control-cid", type=lambda x: int(x,0), help="override the AVCTP control channel CID")
    avrcp.add_argument("--browse-cid", type=lambda x: int(x,0), help="override the AVCTP browsing channel CID")

    rfcomm = subparsers.add_parser('rfcomm', description='Extract RFCOMM frame information')
    rfcomm.set_defaults(func=run_rfcomm)
    rfcomm.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    rfcomm.add_argument("--signal-lcid", type=lambda x: int(x,0), help="override the RFCOMM local CID")
    rfcomm.add_argument("--signal-rcid", type=lambda x: int(x,0), help="override the RFCOMM remote CID")

    sdp = subparsers.add_parser('sdp', description='Extract SDP service discovery information')
    sdp.set_defaults(func=run_sdp)
    sdp.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    sdp.add_argument("--signal-lcid", type=lambda x: int(x,0), help="override the SDP local CID")
    sdp.add_argument("--signal-rcid", type=lambda x: int(x,0), help="override the SDP remote CID")

    smp = subparsers.add_parser('smp', description='Extract SMP (Security Manager Protocol) pairing information')
    smp.set_defaults(func=run_smp)
    smp.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")

    ssp = subparsers.add_parser('ssp', description='Extract SSP (Secure Simple Pairing) information')
    ssp.set_defaults(func=run_ssp)
    ssp.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")

    leaudio = subparsers.add_parser('leaudio', description='Extract ASHA profile information')
    leaudio.set_defaults(func=run_leaudio)
    leaudio.add_argument("path", type=Path, help="path to the bugreport or btsnoop file")
    leaudio.add_argument("--plot", action='store_true', help="plot the stream deviation")
    leaudio.add_argument("--extract", action='store_true', help="extract the stream audio")

    args = parser.parse_args()
    run(args)


if __name__ == "__main__":
    main()
