# BumbleBluetoothTests

Bumble Bluetooth tests are instrumented Android-specific multi-device tests using a reference
peer device implementing the Pandora APIs.

## Architecture

BumbleBluetoothTests is an Android APK that offers enhanced control over Android compared to Avatar
by interacting directly with the Device Under Test (DUT) via Android APIs. Instead of mocking every
API call, it communicates with actual reference devices using gRPC and limits peer device interactions
to the Pandora APIs.

Here is an overview of the BumbleBluetoothTests architecture:
![BumbleBluetoothTests architecture](asset/java-bumble-test-setup.png)

A simple LE connection test looks like this:

```kotlin
// Setup a Bumble Pandora device for the duration of the test.
// Acting as a Pandora client, it can be interacted with through the Pandora APIs.
@Rule @JvmField val mBumble = PandoraDevice()

@Test
fun testGattConnect() {
    mBumble
        .hostBlocking()
        .advertise(
            AdvertiseRequest.newBuilder()
                .setLegacy(true)
                .setConnectable(true)
                .setOwnAddressType(OwnAddressType.RANDOM)
                .build()
        )

    val gattCallback = mock(BluetoothGattCallback::class.java)

    var bumbleGatt = bumbleDevice.connectGatt(context, false, gattCallback)

    verify(gattCallback, timeout(TIMEOUT))
        .onConnectionStateChange(
            any(),
            eq(BluetoothGatt.GATT_SUCCESS),
            eq(BluetoothProfile.STATE_CONNECTED)
        )

    bumbleGatt.disconnect()

    verify(gattCallback, timeout(TIMEOUT))
        .onConnectionStateChange(
            any(),
            eq(BluetoothGatt.GATT_SUCCESS),
            eq(BluetoothProfile.STATE_DISCONNECTED)
        )
}
```
