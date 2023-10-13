# Writing a BumbleBluetoothTests: A Comprehensive Guide

This guide seeks to demystify the process using `testDiscoverDkGattService` as an example.
By the end, you should have a blueprint for constructing similar tests for your Bluetooth
functionalities.

The BumbleBluetoothTests source code can be found in the Android codebase at this
[location][bumble-bluetooth-tests-code].

Pandora APIs are implemented both on Android in a [PandoraServer][pandora-server-code] app and on
[BumblePandoraServer][bumble-github-pandora-server]. The communication between the virtual Android
DUT and the virtual Bumble Reference device is made through [Rootcanal][rootcanal-code], a virtual
Bluetooth Controller.


## Prerequisites

Before diving in, ensure you are acquainted with:
- [Android Junit4][android-junit4] for unit testing
- [Mockito](https://site.mockito.org/) for mocking dependencies
- [Java gRPC documentation][grpc-java-doc]
- [Pandora stable APIs][pandora-stable-apis]
- [Pandora experimental APIs][pandora-experimental-apis]

You must have a running Cuttlefish instance. If not, you can run the following commands from the
root of your Android repository:

```shell
cd $ANDROID_BUILD_TOP
source build/envsetup.sh
lunch aosp_cf_x86_64_phone-userdebug
acloud create --local-image --local-instance
```

Note: For Googlers, from an internal Android repository, use the `cf_x86_64_phone-userdebug` target
instead. You can also use a CF remote instance by removing `--local-instance`.

## Run existing tests

You can run all the exisiting BumbleBluetoothTests by doing so:
```shell
atest BumbleBluetoothTests
```

If you wish to run a specific test file :
```shell
atest BumbleBluetoothTests:<package_name>.<test_file_name>
atest BumbleBluetoothTests:android.bluetooth.DckTest
```

And to run a specific test from a test file:
```shell
atest BumbleBluetoothTests:<package_name>.<test_file_name>#<test_name>
atest BumbleBluetoothTests:android.bluetooth.DckTest#testDiscoverDkGattService
```

## Crafting the Test: Step by Step

### 0. Create the test file

You can either choose to build your new test in Kotlin or in Java. It is totally up to you.
However for this example we will build one in Kotlin.

Let say we are creating a Dck test.

First, create your file under `p/m/Blueooth/frameworks/bumble/src/android/bluetooth` like so:
```shell
cd p/m/Bluetooth/frameworks/bumble/src/android/bluetooth
touch DckTest.kt # We usualy name our test file <test_suite_name>Test.kt/.java
```

Then add the minimum requirements:
```kotlin
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Rule
import org.junit.Test

@RunWith(AndroidJUnit4::class)
public class DckTest {
  private val TAG = "DckTest"

  @Rule @JvmField val mPermissionRule = AdoptShellPermissionsRule()

  @Rule @JvmField val mBumble = PandoraDevice()

  @Test
  fun testDiscoverDkGattService() {
    // Test implementation
  }
}
```

### 1. Register with Service via gRPC

Here, we're dealing with Bumble's DCK (Digital Car Key) service. First, we need Bumble to register
the Dck Gatt service.

```kotlin
// - `dckBlocking()` is likely a stub accessing the DCK service over gRPC in a synchronous manner.
// - `withDeadline(Deadline.after(TIMEOUT, TimeUnit.MILLISECONDS))` sets a timeout for the call.
// - `register(Empty.getDefaultInstance())` communicates our registration to the server.
mBumble
    .dckBlocking()
    .withDeadline(Deadline.after(TIMEOUT, TimeUnit.MILLISECONDS))
    .register(Empty.getDefaultInstance())
```

### 2. Advertise Bluetooth Capabilities

If our device wants to be discoverable and indicate its capabilities, it would "advertise" these
capabilities. Here, it's done via another gRPC call.

```kotlin
mBumble
    .hostBlocking()
    .advertise(
        AdvertiseRequest.newBuilder()
            .setLegacy(true)
            .setConnectable(true)
            .setOwnAddressType(OwnAddressType.RANDOM)
            .build()
    )
```

### 3. Fetch a Known Remote Bluetooth Device

```kotlin
val bumbleDevice =
    bluetoothAdapter.getRemoteLeDevice(
        Utils.BUMBLE_RANDOM_ADDRESS,
        BluetoothDevice.ADDRESS_TYPE_RANDOM
    )
```

### 4. Create Mock Callback for GATT Events

Interactions over Bluetooth often involve callback mechanisms. Here, we're mocking the callback
with Mockito to verify later that expected events occurred.
```kotlin
val gattCallback = mock(BluetoothGattCallback::class.java)
```
### 5. Initiate and Verify Connection

To bond with Bumble, we initiate a connection and then verify that the connection is successful.
```kotlin
var bumbleGatt = bumbleDevice.connectGatt(context, false, gattCallback)
verify(gattCallback, timeout(TIMEOUT))
    .onConnectionStateChange(
        any(),
        eq(BluetoothGatt.GATT_SUCCESS),
        eq(BluetoothProfile.STATE_CONNECTED)
    )
```
### 6. Discover and Verify GATT Services

After connecting, we seek to find out the services offered by Bumble and affirm their successful
discovery.

```kotlin
bumbleGatt.discoverServices()
verify(gattCallback, timeout(TIMEOUT))
    .onServicesDiscovered(any(), eq(BluetoothGatt.GATT_SUCCESS))

```

### 7. Confirm Service Availability

Ensure that the specific service (in this example, CCC_DK_UUID) is present on the remote device.

```kotlin
assertThat(bumbleGatt.getService(CCC_DK_UUID)).isNotNull()
```
### 8. Disconnect and Confirm
Finally, after our operations, we disconnect and ensure it is done gracefully.

```kotlin
bumbleGatt.disconnect()
verify(gattCallback, timeout(TIMEOUT))
    .onConnectionStateChange(
        any(),
        eq(BluetoothGatt.GATT_SUCCESS),
        eq(BluetoothProfile.STATE_DISCONNECTED)
    )
```

## Conclusion

This specific test is entirely commented and available directly within BumbleBluetoothTests.

## Conclusion

This tutorial provided a step-by-step guide on testing some Bluetooth functionalities on top of the
Android Bluetooth frameworks, leveraging both gRPC and Bluetooth GATT interactions. For the detailed
implementation and the full code, refer to our [source code][bumble-bluetooth-tests-code].



[android-junit4]: https://developer.android.com/reference/androidx/test/runner/AndroidJUnit4
[bumble-bluetooth-tests-code]: https://cs.android.com/android/platform/superproject/+/master:packages/modules/Bluetooth/framework/tests/bumble/
[bumble-github-pandora-server]: https://github.com/google/bumble/tree/main/bumble/pandora
[grpc-java-doc]: https://grpc.io/docs/languages/java/
[pandora-experimental-apis]: https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Bluetooth/pandora/interfaces/pandora_experimental/
[pandora-server-code]: https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Bluetooth/android/pandora/server/
[pandora-stable-apis]: https://cs.android.com/android/platform/superproject/main/+/main:external/pandora/bt-test-interfaces/
[rootcanal-code]: https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Bluetooth/tools/rootcanal




