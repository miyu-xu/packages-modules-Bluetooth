# BumbleBluetoothTests

Bumble Bluetooth tests are instrumented Android-specific multi-device tests using a reference
peer device implementing the Pandora APIs.

## Main Architecture

BumbleBluetoothTests was created to provide a way to write tests with an enhanced control over
Android in comparison with Avatar and rather than mocking every API call, it retains interactions
with actual reference devices.

In essence, BumbleBluetoothTests is an Android APK. It communicates directly with the Android
Device Under Test (DUT) via the Android APIs. While this approach offers increased control over Android
compared to Avatar, it does come with a limitation: the interaction with the peer devices is done only
through the Pandora APIs.

Here is an overview of the Bumble architecture:
![BumbleBluetoothTests architecture](asset/java-bumble-test-setup.png)
