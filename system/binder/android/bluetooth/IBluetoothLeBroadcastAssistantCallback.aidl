/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 */

package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastSourceInfo;
//import android.bluetooth.BleBroadcastSourceChannel;
import android.bluetooth.le.ScanResult;

/** @hide */
interface IBluetoothLeBroadcastAssistantCallback {
    void onBluetoothLeBroadcastSourceFound(in ScanResult result);

    void onBluetoothLeBroadcastSourceSelected(
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceLost(
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceAdded(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceUpdated(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);
/*
    void onBleBroadcastPinUpdated(in BluetoothDevice rcvr,
                                  in byte srcId,
                                  in int status);
*/
    void onBluetoothLeBroadcastSourceRemoved(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);
}
