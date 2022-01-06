/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 */
package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastSourceInfo;
import android.bluetooth.IBluetoothLeBroadcastAssistantCallback;
import android.bluetooth.le.ScanResult;
//import android.content.AttributionSource;

/**
 * APIs for Bluetooth LE Audio Broadcast Assistant service
 *
 * @hide
 */
interface IBluetoothLeBroadcastAssistant {
    // Public API
/*
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean connect(in BluetoothDevice device,in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean disconnect(in BluetoothDevice device,in AttributionSource attributionSource);
*/
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getConnectedDevices(/*in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionState(in BluetoothDevice device/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy/*, in AttributionSource attributionSource*/);

/*
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean startScanOffload (in BluetoothDevice device,
                              in boolean groupOp,
                              in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean stopScanOffload (in BluetoothDevice device,
                             in boolean groupOp,
                             in AttributionSource attributionSource);
*/
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int registerCallback(in BluetoothDevice sink,
                             in IBluetoothLeBroadcastAssistantCallback cb/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    /*void*/ int unregisterCallback(in BluetoothDevice device,
                               in IBluetoothLeBroadcastAssistantCallback cb/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    /*boolean*/ int searchforBroadcastSources(in BluetoothDevice sink/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    /*boolean*/ int stopSearchforBroadcastSources(in BluetoothDevice sink/*, in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    /*boolean*/ int addBroadcastSource(in BluetoothDevice sink,
                               in BluetoothLeBroadcastSourceInfo source,
                               in boolean groupOp/*,
                               in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    /*boolean*/ int selectBroadcastSource(in BluetoothDevice device,
                                  in ScanResult scanRes,
                                  in boolean groupOp/*,
                                  in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    /*boolean*/ int updateBroadcastSource(in BluetoothDevice sink,
                                  in BluetoothLeBroadcastSourceInfo source,
                                  in boolean groupOp/*,
                                  in AttributionSource attributionSource*/);

/*
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setBroadcastCode (in BluetoothDevice device,
                              in BleBroadcastSourceInfo srcInfo,
                              in boolean groupOp,
                              in AttributionSource attributionSource
                              );
*/

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    /*boolean*/ int removeBroadcastSource (in BluetoothDevice device,
                                   in int sourceId,
                                   in boolean groupOp/*,
                                   in AttributionSource attributionSource*/);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothLeBroadcastSourceInfo> getAllBroadcastSources(
                                             in BluetoothDevice sink/*,
                                             in AttributionSource attributionSource*/);
}
