/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.bluetooth.gatt;

import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GattNativeInterface {
    private static final String TAG = GattNativeInterface.class.getSimpleName();

    static {
        classInitNative();
    }

    private static GattNativeInterface sInterface;
    private static final Object INSTANCE_LOCK = new Object();

    private GattService mGattService;

    private GattNativeInterface() {}

    GattService getGattService() {
        return mGattService;
    }

    /**
     * This class is a singleton because native library should only be loaded once
     *
     * @return default instance
     */
    public static GattNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInterface == null) {
                sInterface = new GattNativeInterface();
            }
        }
        return sInterface;
    }


    /* Callbacks */

    void onScanResult(int eventType, int addressType, String address, int primaryPhy,
            int secondaryPhy, int advertisingSid, int txPower, int rssi, int periodicAdvInt,
            byte[] advData, String originalAddress) {
        getGattService().onScanResult(eventType, addressType, address, primaryPhy, secondaryPhy,
                advertisingSid, txPower, rssi, periodicAdvInt, advData, originalAddress);
    }

    void onScannerRegistered(int status, int scannerId, long uuidLsb, long uuidMsb)
            throws RemoteException {
        getGattService().onScannerRegistered(status, scannerId, uuidLsb, uuidMsb);
    }

    void onClientRegistered(int status, int clientIf, long uuidLsb, long uuidMsb)
            throws RemoteException {
        getGattService().onClientRegistered(status, clientIf, uuidLsb, uuidMsb);
    }

    void onConnected(int clientIf, int connId, int status, String address) throws RemoteException {
        getGattService().onConnected(clientIf, connId, status, address);
    }

    void onDisconnected(int clientIf, int connId, int status, String address)
            throws RemoteException {
        getGattService().onDisconnected(clientIf, connId, status, address);
    }

    void onClientPhyUpdate(int connId, int txPhy, int rxPhy, int status) throws RemoteException {
        getGattService().onClientPhyUpdate(connId, txPhy, rxPhy, status);
    }

    void onClientPhyRead(int clientIf, String address, int txPhy, int rxPhy, int status)
            throws RemoteException {
        getGattService().onClientPhyRead(clientIf, address, txPhy, rxPhy, status);
    }

    void onClientConnUpdate(int connId, int interval, int latency, int timeout, int status)
            throws RemoteException {
        getGattService().onClientConnUpdate(connId, interval, latency, timeout, status);
    }

    void onServiceChanged(int connId) throws RemoteException {
        getGattService().onServiceChanged(connId);
    }

    void onServerPhyUpdate(int connId, int txPhy, int rxPhy, int status) throws RemoteException {
        getGattService().onServerPhyUpdate(connId, txPhy, rxPhy, status);
    }

    void onServerPhyRead(int serverIf, String address, int txPhy, int rxPhy, int status)
            throws RemoteException {
        getGattService().onServerPhyRead(serverIf, address, txPhy, rxPhy, status);
    }

    void onServerConnUpdate(int connId, int interval, int latency, int timeout, int status)
            throws RemoteException {
        getGattService().onServerConnUpdate(connId, interval, latency, timeout, status);
    }

    void onSearchCompleted(int connId, int status) throws RemoteException {
        getGattService().onSearchCompleted(connId, status);
    }

    GattDbElement getSampleGattDbElement() {
        return getGattService().getSampleGattDbElement();
    }

    void onGetGattDb(int connId, ArrayList<GattDbElement> db) throws RemoteException {
        getGattService().onGetGattDb(connId, db);
    }

    void onRegisterForNotifications(int connId, int status, int registered, int handle) {
        getGattService().onRegisterForNotifications(connId, status, registered, handle);
    }

    void onNotify(int connId, String address, int handle, boolean isNotify, byte[] data)
            throws RemoteException {
        getGattService().onNotify(connId, address, handle, isNotify, data);
    }

    void onReadCharacteristic(int connId, int status, int handle, byte[] data)
            throws RemoteException {
        getGattService().onReadCharacteristic(connId, status, handle, data);
    }

    void onWriteCharacteristic(int connId, int status, int handle, byte[] data)
            throws RemoteException {
        getGattService().onWriteCharacteristic(connId, status, handle, data);
    }

    void onExecuteCompleted(int connId, int status) throws RemoteException {
        getGattService().onExecuteCompleted(connId, status);
    }

    void onReadDescriptor(int connId, int status, int handle, byte[] data) throws RemoteException {
        getGattService().onReadDescriptor(connId, status, handle, data);
    }

    void onWriteDescriptor(int connId, int status, int handle, byte[] data) throws RemoteException {
        getGattService().onWriteDescriptor(connId, status, handle, data);
    }

    void onReadRemoteRssi(int clientIf, String address, int rssi, int status)
            throws RemoteException {
        getGattService().onReadRemoteRssi(clientIf, address, rssi, status);
    }

    void onScanFilterEnableDisabled(int action, int status, int clientIf) {
        getGattService().onScanFilterEnableDisabled(action, status, clientIf);
    }

    void onScanFilterParamsConfigured(int action, int status, int clientIf, int availableSpace) {
        getGattService().onScanFilterParamsConfigured(action, status, clientIf, availableSpace);
    }

    void onScanFilterConfig(int action, int status, int clientIf, int filterType,
            int availableSpace) {
        getGattService().onScanFilterConfig(action, status, clientIf, filterType, availableSpace);
    }

    void onBatchScanStorageConfigured(int status, int clientIf) {
        getGattService().onBatchScanStorageConfigured(status, clientIf);
    }

    void onBatchScanStartStopped(int startStopAction, int status, int clientIf) {
        getGattService().onBatchScanStartStopped(startStopAction, status, clientIf);
    }

    void onBatchScanReports(int status, int scannerId, int reportType, int numRecords,
            byte[] recordData) throws RemoteException {
        getGattService().onBatchScanReports(status, scannerId, reportType, numRecords, recordData);
    }

    void onBatchScanThresholdCrossed(int clientIf) {
        getGattService().onBatchScanThresholdCrossed(clientIf);
    }

    AdvtFilterOnFoundOnLostInfo createOnTrackAdvFoundLostObject(int clientIf, int advPktLen,
            byte[] advPkt, int scanRspLen, byte[] scanRsp, int filtIndex, int advState,
            int advInfoPresent, String address, int addrType, int txPower, int rssiValue,
            int timeStamp) {
        return getGattService().createOnTrackAdvFoundLostObject(clientIf, advPktLen, advPkt,
                scanRspLen, scanRsp, filtIndex, advState, advInfoPresent, address, addrType,
                txPower, rssiValue, timeStamp);
    }

    void onTrackAdvFoundLost(AdvtFilterOnFoundOnLostInfo trackingInfo) throws RemoteException {
        getGattService().onTrackAdvFoundLost(trackingInfo);
    }

    void onScanParamSetupCompleted(int status, int scannerId) throws RemoteException {
        getGattService().onScanParamSetupCompleted(status, scannerId);
    }

    void onConfigureMTU(int connId, int status, int mtu) throws RemoteException {
        getGattService().onConfigureMTU(connId, status, mtu);
    }

    void onClientCongestion(int connId, boolean congested) throws RemoteException {
        getGattService().onClientCongestion(connId, congested);
    }

    /* Server callbacks */

    void onServerRegistered(int status, int serverIf, long uuidLsb, long uuidMsb)
            throws RemoteException {
        getGattService().onServerRegistered(status, serverIf, uuidLsb, uuidMsb);
    }

    void onServiceAdded(int status, int serverIf, List<GattDbElement> service)
            throws RemoteException {
        getGattService().onServiceAdded(status, serverIf, service);
    }

    void onServiceStopped(int status, int serverIf, int srvcHandle) throws RemoteException {
        getGattService().onServiceStopped(status, serverIf, srvcHandle);
    }

    void onServiceDeleted(int status, int serverIf, int srvcHandle) {
        getGattService().onServiceDeleted(status, serverIf, srvcHandle);
    }

    void onClientConnected(String address, boolean connected, int connId, int serverIf)
            throws RemoteException {
        getGattService().onClientConnected(address, connected, connId, serverIf);
    }

    void onServerReadCharacteristic(String address, int connId, int transId, int handle, int offset,
            boolean isLong) throws RemoteException {
        getGattService().onServerReadCharacteristic(address, connId, transId, handle, offset,
                isLong);
    }

    void onServerReadDescriptor(String address, int connId, int transId, int handle, int offset,
            boolean isLong) throws RemoteException {
        getGattService().onServerReadDescriptor(address, connId, transId, handle, offset, isLong);
    }

    void onServerWriteCharacteristic(String address, int connId, int transId, int handle,
            int offset, int length, boolean needRsp, boolean isPrep, byte[] data)
            throws RemoteException {
        getGattService().onServerWriteCharacteristic(address, connId, transId, handle, offset,
                length, needRsp, isPrep, data);
    }

    void onServerWriteDescriptor(String address, int connId, int transId, int handle, int offset,
            int length, boolean needRsp, boolean isPrep, byte[] data) throws RemoteException {
        getGattService().onServerWriteDescriptor(address, connId, transId, handle, offset, length,
                needRsp, isPrep, data);
    }

    void onExecuteWrite(String address, int connId, int transId, int execWrite)
            throws RemoteException {
        getGattService().onExecuteWrite(address, connId, transId, execWrite);
    }

    void onResponseSendCompleted(int status, int attrHandle) {
        getGattService().onResponseSendCompleted(status, attrHandle);
    }

    void onNotificationSent(int connId, int status) throws RemoteException {
        getGattService().onNotificationSent(connId, status);
    }

    void onServerCongestion(int connId, boolean congested) throws RemoteException {
        getGattService().onServerCongestion(connId, congested);
    }

    void onMtuChanged(int connId, int mtu) throws RemoteException {
        getGattService().onMtuChanged(connId, mtu);
    }

    /* Native methods */

    private static native void classInitNative();

    /**
     * Initialize the native interface and native components
     */
    public void init(GattService gattService) {
        mGattService = gattService;
        initializeNative();
    }

    private native void initializeNative();

    /**
     * Cleanup the native interface and native components
     */
    public void cleanup() {
        cleanupNative();
        mGattService = null;
    }

    private native void cleanupNative();

    /**
     * Get the type of Bluetooth device
     *
     * @param address address of the Bluetooth device
     * @return type of Bluetooth device 0 for BR/EDR, 1 for BLE, 2 for DUAL mode (To be confirmed)
     */
    public int gattClientGetDeviceType(String address) {
        return gattClientGetDeviceTypeNative(address);
    }

    private native int gattClientGetDeviceTypeNative(String address);

    /**
     *
     * @param appUuidLsb
     * @param appUuidMsb
     * @param eattSupport
     */
    public void gattClientRegisterApp(long appUuidLsb, long appUuidMsb, boolean eattSupport) {
        gattClientRegisterAppNative(appUuidLsb, appUuidMsb, eattSupport);
    }

    private native void gattClientRegisterAppNative(long appUuidLsb, long appUuidMsb,
            boolean eattSupport);

    /**
     *
     * @param clientIf
     */
    public void gattClientUnregisterApp(int clientIf) {
        gattClientUnregisterAppNative(clientIf);
    }

    private native void gattClientUnregisterAppNative(int clientIf);

    /**
     *
     * @param clientIf
     * @param address
     * @param isDirect
     * @param transport
     * @param opportunistic
     * @param initiatingPhys
     */
    public void gattClientConnect(int clientIf, String address, boolean isDirect, int transport,
            boolean opportunistic, int initiatingPhys) {
        gattClientConnectNative(clientIf, address, isDirect, transport, opportunistic,
                initiatingPhys);
    }

    private native void gattClientConnectNative(int clientIf, String address, boolean isDirect,
            int transport, boolean opportunistic, int initiatingPhys);

    /**
     *
     * @param clientIf
     * @param address
     * @param connId
     */
    public void gattClientDisconnect(int clientIf, String address, int connId) {
        gattClientDisconnectNative(clientIf, address, connId);
    }

    private native void gattClientDisconnectNative(int clientIf, String address, int connId);

    /**
     *
     * @param clientIf
     * @param address
     * @param txPhy
     * @param rxPhy
     * @param phyOptions
     */
    public void gattClientSetPreferredPhy(int clientIf, String address, int txPhy,
            int rxPhy, int phyOptions) {
        gattClientSetPreferredPhyNative(clientIf, address, txPhy, rxPhy, phyOptions);
    }

    private native void gattClientSetPreferredPhyNative(int clientIf, String address, int txPhy,
            int rxPhy, int phyOptions);

    /**
     *
     * @param clientIf
     * @param address
     */
    public void gattClientReadPhy(int clientIf, String address) {
        gattClientReadPhyNative(clientIf, address);
    }

    private native void gattClientReadPhyNative(int clientIf, String address);

    /**
     *
     * @param clientIf
     * @param address
     */
    public void gattClientRefresh(int clientIf, String address) {
        gattClientRefreshNative(clientIf, address);
    }

    private native void gattClientRefreshNative(int clientIf, String address);

    /**
     *
     * @param connId
     * @param searchAll
     * @param serviceUuidLsb
     * @param serviceUuidMsb
     */
    public void gattClientSearchService(int connId, boolean searchAll, long serviceUuidLsb,
            long serviceUuidMsb) {
        gattClientSearchServiceNative(connId, searchAll, serviceUuidLsb, serviceUuidMsb);
    }

    private native void gattClientSearchServiceNative(int connId, boolean searchAll,
            long serviceUuidLsb, long serviceUuidMsb);

    /**
     *
     * @param connId
     * @param serviceUuidLsb
     * @param serviceUuidMsb
     */
    public void gattClientDiscoverServiceByUuid(int connId, long serviceUuidLsb,
            long serviceUuidMsb) {
        gattClientDiscoverServiceByUuidNative(connId, serviceUuidLsb, serviceUuidMsb);
    }

    private native void gattClientDiscoverServiceByUuidNative(int connId, long serviceUuidLsb,
            long serviceUuidMsb);

    /**
     *
     * @param connId
     */
    public void gattClientGetGattDb(int connId) {
        gattClientGetGattDb(connId);
    }

    private native void gattClientGetGattDbNative(int connId);

    /**
     *
     * @param connId
     * @param handle
     * @param authReq
     */
    public void gattClientReadCharacteristic(int connId, int handle, int authReq) {
        gattClientReadCharacteristicNative(connId, handle, authReq);
    }

    private native void gattClientReadCharacteristicNative(int connId, int handle, int authReq);

    /**
     *
     * @param connId
     * @param uuidMsb
     * @param uuidLsb
     * @param sHandle
     * @param eHandle
     * @param authReq
     */
    public void gattClientReadUsingCharacteristicUuid(int connId, long uuidMsb,
            long uuidLsb, int sHandle, int eHandle, int authReq) {
        gattClientReadUsingCharacteristicUuidNative(connId, uuidMsb, uuidLsb, sHandle, eHandle,
                authReq);
    }

    private native void gattClientReadUsingCharacteristicUuidNative(int connId, long uuidMsb,
            long uuidLsb, int sHandle, int eHandle, int authReq);

    /**
     *
     * @param connId
     * @param handle
     * @param authReq
     */
    public void gattClientReadDescriptor(int connId, int handle, int authReq) {
        gattClientReadDescriptorNative(connId, handle, authReq);
    }

    private native void gattClientReadDescriptorNative(int connId, int handle, int authReq);

    /**
     *
     * @param connId
     * @param handle
     * @param writeType
     * @param authReq
     * @param value
     */
    public void gattClientWriteCharacteristic(int connId, int handle, int writeType,
            int authReq, byte[] value) {
        gattClientWriteCharacteristicNative(connId, handle, writeType, authReq, value);
    }

    private native void gattClientWriteCharacteristicNative(int connId, int handle, int writeType,
            int authReq, byte[] value);

    /**
     *
     * @param connId
     * @param handle
     * @param authReq
     * @param value
     */
    public void gattClientWriteDescriptor(int connId, int handle, int authReq,
            byte[] value) {
        gattClientWriteDescriptorNative(connId, handle, authReq, value);
    }

    private native void gattClientWriteDescriptorNative(int connId, int handle, int authReq,
            byte[] value);

    /**
     *
     * @param connId
     * @param execute
     */
    public void gattClientExecuteWrite(int connId, boolean execute) {
        gattClientExecuteWriteNative(connId, execute);
    }

    private native void gattClientExecuteWriteNative(int connId, boolean execute);

    /**
     *
     * @param clientIf
     * @param address
     * @param handle
     * @param enable
     */
    public void gattClientRegisterForNotifications(int clientIf, String address,
            int handle, boolean enable) {
        gattClientRegisterForNotificationsNative(clientIf, address, handle, enable);
    }

    private native void gattClientRegisterForNotificationsNative(int clientIf, String address,
            int handle, boolean enable);

    /**
     *
     * @param clientIf
     * @param address
     */
    public void gattClientReadRemoteRssi(int clientIf, String address) {
        gattClientReadRemoteRssiNative(clientIf, address);
    }

    private native void gattClientReadRemoteRssiNative(int clientIf, String address);

    /**
     *
     * @param connId
     * @param mtu
     */
    public void gattClientConfigureMTU(int connId, int mtu) {
        gattClientConfigureMTUNative(connId, mtu);
    }

    private native void gattClientConfigureMTUNative(int connId, int mtu);

    /**
     *
     * @param clientIf
     * @param address
     * @param minInterval
     * @param maxInterval
     * @param latency
     * @param timeout
     * @param minConnectionEventLen
     * @param maxConnectionEventLen
     */
    public void gattConnectionParameterUpdate(int clientIf, String address,
            int minInterval, int maxInterval, int latency, int timeout, int minConnectionEventLen,
            int maxConnectionEventLen) {
        gattConnectionParameterUpdateNative(clientIf, address, minInterval, maxInterval, latency,
                timeout, minConnectionEventLen, maxConnectionEventLen);
    }

    private native void gattConnectionParameterUpdateNative(int clientIf, String address,
            int minInterval, int maxInterval, int latency, int timeout, int minConnectionEventLen,
            int maxConnectionEventLen);

    /**
     *
     * @param appUuidLsb
     * @param appUuidMsb
     * @param eattSupport
     */
    public void gattServerRegisterApp(long appUuidLsb, long appUuidMsb, boolean eattSupport) {
        gattServerRegisterAppNative(appUuidLsb, appUuidMsb, eattSupport);
    }

    private native void gattServerRegisterAppNative(long appUuidLsb, long appUuidMsb,
            boolean eattSupport);

    /**
     *
     * @param serverIf
     */
    public void gattServerUnregisterApp(int serverIf) {
        gattServerUnregisterAppNative(serverIf);
    }

    private native void gattServerUnregisterAppNative(int serverIf);

    /**
     *
     * @param serverIf
     * @param address
     * @param isDirect
     * @param transport
     */
    public void gattServerConnect(int serverIf, String address, boolean isDirect,
            int transport) {
        gattServerConnectNative(serverIf, address, isDirect, transport);
    }

    private native void gattServerConnectNative(int serverIf, String address, boolean isDirect,
            int transport);

    /**
     *
     * @param serverIf
     * @param address
     * @param connId
     */
    public void gattServerDisconnect(int serverIf, String address, int connId) {
        gattServerDisconnectNative(serverIf, address, connId);
    }

    private native void gattServerDisconnectNative(int serverIf, String address, int connId);

    /**
     *
     * @param clientIf
     * @param address
     * @param txPhy
     * @param rxPhy
     * @param phyOptions
     */
    public void gattServerSetPreferredPhy(int clientIf, String address, int txPhy,
            int rxPhy, int phyOptions) {
        gattServerSetPreferredPhyNative(clientIf, address, txPhy, rxPhy, phyOptions);
    }

    private native void gattServerSetPreferredPhyNative(int clientIf, String address, int txPhy,
            int rxPhy, int phyOptions);

    /**
     *
     * @param clientIf
     * @param address
     */
    public void gattServerReadPhy(int clientIf, String address) {
        gattServerReadPhyNative(clientIf, address);
    }

    private native void gattServerReadPhyNative(int clientIf, String address);

    /**
     *
     * @param serverIf
     * @param service
     */
    public void gattServerAddService(int serverIf, List<GattDbElement> service) {
        gattServerAddServiceNative(serverIf, service);
    }

    private native void gattServerAddServiceNative(int serverIf, List<GattDbElement> service);

    /**
     *
     * @param serverIf
     * @param svcHandle
     */
    public void gattServerStopService(int serverIf, int svcHandle) {
        gattServerStopServiceNative(serverIf, svcHandle);
    }

    private native void gattServerStopServiceNative(int serverIf, int svcHandle);

    /**
     *
     * @param serverIf
     * @param svcHandle
     */
    public void gattServerDeleteService(int serverIf, int svcHandle) {
        gattServerDeleteServiceNative(serverIf, svcHandle);
    }

    private native void gattServerDeleteServiceNative(int serverIf, int svcHandle);

    /**
     *
     * @param serverIf
     * @param attrHandle
     * @param connId
     * @param val
     */
    public void gattServerSendIndication(int serverIf, int attrHandle, int connId,
            byte[] val) {
        gattServerSendIndicationNative(serverIf, attrHandle, connId, val);
    }

    private native void gattServerSendIndicationNative(int serverIf, int attrHandle, int connId,
            byte[] val);

    /**
     *
     * @param serverIf
     * @param attrHandle
     * @param connId
     * @param val
     */
    public void gattServerSendNotification(int serverIf, int attrHandle, int connId,
            byte[] val) {
        gattServerSendNotificationNative(serverIf, attrHandle, connId, val);
    }

    private native void gattServerSendNotificationNative(int serverIf, int attrHandle, int connId,
            byte[] val);

    /**
     *
     * @param serverIf
     * @param connId
     * @param transId
     * @param status
     * @param handle
     * @param offset
     * @param val
     * @param authReq
     */
    public void gattServerSendResponse(int serverIf, int connId, int transId,
            int status, int handle, int offset, byte[] val, int authReq) {
        gattServerSendResponseNative(serverIf, connId, transId, status, handle, offset, val,
                authReq);
    }

    private native void gattServerSendResponseNative(int serverIf, int connId, int transId,
            int status, int handle, int offset, byte[] val, int authReq);

    /**
     *
     * @param command
     * @param uuid1Lsb
     * @param uuid1Msb
     * @param bda1
     * @param p1
     * @param p2
     * @param p3
     * @param p4
     * @param p5
     */
    public void gattTest(int command, long uuid1Lsb, long uuid1Msb, String bda1,
            int p1, int p2, int p3, int p4, int p5) {
        gattTestNative(command, uuid1Lsb, uuid1Msb, bda1, p1, p2, p3, p4, p5);
    }

    private native void gattTestNative(int command, long uuid1Lsb, long uuid1Msb, String bda1,
            int p1, int p2, int p3, int p4, int p5);

}

