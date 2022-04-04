# RootCanal

RootCanal is a virtual Bluetooth Controller.
Its goals include, but are not limited to: Bluetooth Testing and Emulation.

## Usage

RootCanal can be used in multiple ways:
- With the Cuttlefish Virtual Device.
- As a Host standalone binary.
- As a Bluetooth HAL.
- As a library.

### Cuttlefish Virtual Device

RootCanal is enabled by default when you use Cuttlefish, refer
to the Cuttlefish documentation

### Host standalone binary

```bash
m root-canal # Build RootCanal
out/host/linux-x86/bin/root-canal # Run RootCanal
```

Note: You can also find a prebuilt version inside [cvd-host_package.tar.gz from Android CI][cvd-host_package]

[cvd-host_package]: https://ci.android.com/builds/latest/branches/aosp-master/targets/aosp_cf_x86_64_phone-userdebug/view/cvd-host_package.tar.gz

RootCanal when run as a host tool, expose by default 4 ports:
- 6401: Test channel port
- 6402: HCI port
- 6403: BR_EDR Phy port
- 6404: LE Phy port

### Bluetooth HAL

A HAL using RootCanal is available as `android.hardware.bluetooth@1.1-service.sim`

## Channels

### HCI Channel

The HCI channel uses the standard Bluetooth UART transport protocol (also known as H4) over TCP.
You can refer to Vol 4, Part A, 2 of the Bluetooth Core Specification for more information.
Each connection on the HCI channel creates a new virtual controller.

### Test Channel

The test channel uses a simple custom protocol to send test commands to RootCanal.
You can connect to it using [scripts/test_channel.py](scripts/test_channel.py).

### Phy Channels

The physical channel uses a custom protocol described in [packets/link_layer_packets.pdl](packets/link_layer_packets.pdl).
This channel is internal to RootCanal and should not be used.
