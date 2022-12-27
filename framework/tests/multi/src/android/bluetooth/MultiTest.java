/*
 * Copyright 2022 The Android Open Source Project
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

package android.bluetooth;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.IBinder;
import android.os.Process;
import android.content.AttributionSource;

@RunWith(AndroidJUnit4.class)
public class MultiTest {
    static {
        System.loadLibrary("android_bluetooth_binder_rpc");
    }

    static native IBinder connectRPC(int port);

    @Test
    public void test() throws Exception {
        IBinder root = connectRPC(4242);
        System.out.println(root);
        AttributionSource attributionSource = new AttributionSource(
         Process.SHELL_UID, "com.android.shell", null, root);
        IBluetoothManager service = IBluetoothManager.Stub.asInterface(root);
        System.out.println(service.getAddress(attributionSource));

        root = connectRPC(4242);
        attributionSource = new AttributionSource(
         Process.SHELL_UID, "com.android.shell", null, root);
        service = IBluetoothManager.Stub.asInterface(root);
        System.out.println(service.getName(attributionSource));
        //BluetoothAdapter adapter = new BluetoothAdapter(service, attributionSource);
    }
}
