/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.bluetooth;

import static org.mockito.Mockito.*;

import android.provider.DeviceConfig;
import android.provider.DeviceConfig.Properties;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class BluetoothDeviceConfigChangeTrackerTest {
    @Test
    public void testNoProperties() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH).build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH).build());

        Assert.assertFalse(shouldRestart);
    }

    @Test
    public void testNewFlag() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .setString("INIT_b", "true")
                                .build());

        Assert.assertTrue(shouldRestart);
    }

    @Test
    public void testChangedFlag() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder("another_namespace")
                                .setString("INIT_a", "false")
                                .build());

        Assert.assertTrue(shouldRestart);
    }

    @Test
    public void testUnchangedInitFlag() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .build());

        Assert.assertFalse(shouldRestart);
    }

    @Test
    public void testRepeatedChangeInitFlag() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH).build());

        changeTracker.shouldRestartWhenPropertiesUpdated(
                new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                        .setString("INIT_a", "true")
                        .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .build());

        Assert.assertFalse(shouldRestart);
    }

    @Test
    public void testWrongNamespace() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH).build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder("another_namespace")
                                .setString("INIT_a", "true")
                                .build());

        Assert.assertFalse(shouldRestart);
    }

    @Test
    public void testSkipProperty() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_a", "true")
                                .setString("INIT_b", "false")
                                .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("INIT_b", "false")
                                .build());

        Assert.assertFalse(shouldRestart);
    }

    @Test
    public void testNonInitFlag() {
        BluetoothDeviceConfigChangeTracker changeTracker =
                new BluetoothDeviceConfigChangeTracker(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("a", "true")
                                .build());

        boolean shouldRestart =
                changeTracker.shouldRestartWhenPropertiesUpdated(
                        new Properties.Builder(DeviceConfig.NAMESPACE_BLUETOOTH)
                                .setString("a", "false")
                                .build());

        Assert.assertFalse(shouldRestart);
    }
}
