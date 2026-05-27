# Description

This tool provides functionalities to automatically extract and parse
BT Snoop logs from a Bluetooth bugreport.

# Usage

Select a specific analyzer using the corresponding subcommand.
Supported analyzers are listed below.

## A2DP

```
usage: bugreport.py a2dp [-h] [options] path

Extract A2DP profile information

positional arguments:
  path                  path to the bugreport file

options:
  -h, --help            show this help message and exit
  --signal-lcid SIGNAL_LCID
                        override the signaling channel LCID
  --signal-rcid SIGNAL_RCID
                        override the signaling channel RCID
  --stream-cid STREAM_CID
                        override the stream CID
  --codec-type CODEC_TYPE
                        override the codec type
  --sampling-frequency SAMPLING_FREQUENCY
                        override the sampling frequency
 ```

The A2DP analyzer will parse AVDTP signaling exchanges for each connection,
and automatically extract, plot and decode the audio stream when it is available
(i.e. not offloaded).

As a requirement, `ffmpeg` needs to be installed on the host machine.
`ldac` is not natively supported by ffmpeg, but [libldacdec](https://github.com/hegdi/libldacdec.git)
may be used to decode the extracted stream:

```
git clone https://github.com/hegdi/libldacdec.git
make -C libldacdec

cd libldacdec && ./ldacdec stream_LDAC_*.bt
```

## AVRCP

```
usage: bugreport.py avrcp [-h] [options] path

Extract AVRCP profile information

positional arguments:
  path                  path to the bugreport file

options:
  -h, --help            show this help message and exit
  --control-cid CONTROL_CID
                        override the AVCTP control channel CID
  --browse-cid BROWSE_CID
                        override the AVCTP browsing channel CID
```

The AVRCP analyzer parses AVCTP signaling exchanges for each connection and
decodes the AV/C frames carried over them, including PASS_THROUGH operations
(play, pause, volume, etc.) and vendor-dependent AVRCP PDUs (metadata,
notifications, absolute volume).

## SDP

```
usage: bugreport.py sdp [-h] [options] path

Extract SDP service discovery information

positional arguments:
  path                  path to the bugreport file

options:
  -h, --help            show this help message and exit
  --signal-lcid SIGNAL_LCID
                        override the SDP local CID
  --signal-rcid SIGNAL_RCID
                        override the SDP remote CID
```

The SDP analyzer tracks L2CAP signaling for the SDP PSM (0x0001), decodes
SDP requests/responses (Service Search, Service Attribute, Service Search
Attribute, Error), and resolves well-known UUIDs and attribute IDs to
human-readable names. It also lists the services discovered on each
connection.

## SMP

```
usage: bugreport.py smp [-h] path

Extract SMP (Security Manager Protocol) pairing information

positional arguments:
  path        path to the bugreport file

options:
  -h, --help  show this help message and exit
```

The SMP analyzer decodes Security Manager Protocol packets exchanged on
L2CAP fixed CID 0x0006 (LE) and 0x0007 (BR/EDR). It tracks the full
pairing exchange — Pairing Request/Response, Confirm/Random, Public Key
and DHKey Check (LE Secure Connections), Pairing Failed, and the key
distribution phase (LTK, EDIV+Rand, IRK, Identity Address, CSRK) — and
infers the negotiated pairing type (LE Legacy vs. LE Secure Connections)
and association model (Just Works, Numeric Comparison, Passkey Entry, OOB)
from the IO capabilities and authentication requirements.

## SSP

```
usage: bugreport.py ssp [-h] path

Extract SSP (Secure Simple Pairing) information

positional arguments:
  path        path to the bugreport file

options:
  -h, --help  show this help message and exit
```

The SSP analyzer walks HCI events from the BTSnoop log and decodes the
Secure Simple Pairing exchange: IO Capability Request/Response, User
Confirmation/Passkey Request, Passkey Notification, Keypress Notification,
Simple Pairing Complete, and Link Key Request/Notification. Unlike A2DP /
AVRCP / SDP, SSP runs at the HCI layer and is not bound to a single L2CAP
channel, so the analyzer reports across the entire BTSnoop log rather than
per ACL connection.
