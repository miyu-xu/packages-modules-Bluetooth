/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 */
package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BleBroadcastSourceInfo;
import android.bluetooth.IBleBroadcastAudioScanAssistCallback;
import android.bluetooth.le.ScanResult;
import android.content.AttributionSource;

/**
 * APIs for Bluetooth Bluetooth Scan offloader service
 *
 * @hide
 */
interface IBluetoothSyncHelper {
    // Public API
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean connect(in BluetoothDevice device,in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean disconnect(in BluetoothDevice device,in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);

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

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void registerAppCallback(in BluetoothDevice device,
                             in IBleBroadcastAudioScanAssistCallback cb, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void unregisterAppCallback(in BluetoothDevice device,
                               in IBleBroadcastAudioScanAssistCallback cb, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    boolean searchforLeAudioBroadcasters (in BluetoothDevice device, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    boolean stopSearchforLeAudioBroadcasters(in BluetoothDevice device, in AttributionSource attributionSource);

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean addBroadcastSource(in BluetoothDevice device,
                               in BleBroadcastSourceInfo srcInfo,
                               in boolean groupOp,
                               in AttributionSource attributionSource
                            );
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean selectBroadcastSource(in BluetoothDevice device,
                                  in ScanResult scanRes,
                                  in boolean groupOp,
                                  in AttributionSource attributionSource
                                  );

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean updateBroadcastSource(in BluetoothDevice device,
                                  in BleBroadcastSourceInfo srcInfo,
                                  in boolean groupOp,
                                  in AttributionSource attributionSource
                                  );

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setBroadcastCode (in BluetoothDevice device,
                              in BleBroadcastSourceInfo srcInfo,
                              in boolean groupOp,
                              in AttributionSource attributionSource
                              );

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean removeBroadcastSource (in BluetoothDevice device,
                                   in byte SourceId,
                                   in boolean groupOp,
                                   in AttributionSource attributionSource
                                   );

    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BleBroadcastSourceInfo> getAllBroadcastSourceInformation(
                                             in BluetoothDevice device,
                                             in AttributionSource attributionSource);
}
