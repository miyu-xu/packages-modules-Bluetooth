/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /usr/local/google/home/licorne/work/aosp/out/host/linux-x86/bin/aidl --lang=java --out=. -I../../system/binder -I ../../../../../frameworks/libs/modules-utils/java -I ../../framework/aidl-export/ -I ../../../../../frameworks/base/core/java/ -I ../binder ../../system/binder/android/bluetooth/IBluetooth.aidl ../../system/binder/android/bluetooth/IBluetoothCallback.aidl ../binder/android/bluetooth/IBluetoothManager.aidl
 */
package android.bluetooth;
/**
 * System private API for talking with the Bluetooth service.
 * 
 * {@hide}
 */
public interface IBluetooth extends android.os.IInterface
{
  /** Default implementation for IBluetooth. */
  public static class Default implements android.bluetooth.IBluetooth
  {
    @Override public void getState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void enable(boolean quietMode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void disable(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getAddress(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLogRedactionEnabled(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getUuids(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setName(java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getIdentityAddress(java.lang.String address, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getName(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getNameLengthForAdvertise(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getIoCapability(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setIoCapability(int capability, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getScanMode(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setScanMode(int mode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getDiscoverableTimeout(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setDiscoverableTimeout(long timeout, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void startDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void cancelDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isDiscovering(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getDiscoveryEndMillis(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getAdapterConnectionState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getProfileConnectionState(int profile, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getBondedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void createBond(android.bluetooth.BluetoothDevice device, int transport, android.bluetooth.OobData p192Data, android.bluetooth.OobData p256Data, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void cancelBondProcess(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void removeBond(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getBondState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isBondingInitiatedLocally(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getSupportedProfiles(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getConnectionState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getConnectionHandle(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRemoteName(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRemoteType(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRemoteAlias(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setRemoteAlias(android.bluetooth.BluetoothDevice device, java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRemoteClass(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRemoteUuids(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void fetchRemoteUuids(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void sdpSearch(android.bluetooth.BluetoothDevice device, android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getBatteryLevel(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getMaxConnectedAudioDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setPin(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] pinCode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setPasskey(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] passkey, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setPairingConfirmation(android.bluetooth.BluetoothDevice device, boolean accept, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setSilenceMode(android.bluetooth.BluetoothDevice device, boolean silence, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getSilenceMode(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getMessageAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setMessageAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getSimAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setSimAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void registerCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void unregisterCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    // For Socket
    @Override public void logL2capcocServerConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, long timeoutMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public android.bluetooth.IBluetoothSocketManager getSocketManager() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void logL2capcocClientConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void factoryReset(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isMultiAdvertisementSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isOffloadedFilteringSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isOffloadedScanBatchingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isActivityAndEnergyReportingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLe2MPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLeCodedPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLeExtendedAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLePeriodicAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLeAudioSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLeAudioBroadcastSourceSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isLeAudioBroadcastAssistantSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isDistanceMeasurementSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getLeMaximumAdvertisingDataLength(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void reportActivityInfo(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    // For Metadata
    @Override public void registerMetadataListener(android.bluetooth.IBluetoothMetadataListener listener, android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void unregisterMetadataListener(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setMetadata(android.bluetooth.BluetoothDevice device, int key, byte[] value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getMetadata(android.bluetooth.BluetoothDevice device, int key, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    /**
     * Requests the controller activity info asynchronously.
     * The implementor is expected to reply with the
     * {@link android.bluetooth.BluetoothActivityEnergyInfo} object placed into the Bundle with the
     * key {@link android.os.BatteryStats#RESULT_RECEIVER_CONTROLLER_KEY}.
     * The result code is ignored.
     */
    @Override public void requestActivityInfo(android.bluetooth.IBluetoothActivityEnergyInfoListener listener, android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
    }
    @Override public void startBrEdr(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void stopBle(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void connectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void disconnectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setActiveDevice(android.bluetooth.BluetoothDevice device, int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getActiveDevices(int profile, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getMostRecentlyConnectedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void removeActiveDevice(int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void registerBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void unregisterBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void canBondWithoutDialog(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getPackageNameOfBondingApplication(android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void generateLocalOobData(int transport, android.bluetooth.IBluetoothOobDataCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void allowLowLatencyAudio(boolean allowed, android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void isRequestAudioPolicyAsSinkSupported(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void requestAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.bluetooth.BluetoothSinkAudioPolicy policies, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getRequestedAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void startRfcommListener(java.lang.String name, android.os.ParcelUuid uuid, android.app.PendingIntent intent, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void stopRfcommListener(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void retrievePendingSocketForServiceRecord(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void setForegroundUserId(int userId, android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
    }
    @Override public void setPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.os.Bundle modeToProfileBundle, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void registerPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void unregisterPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void notifyActiveDeviceChangeApplied(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void registerBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void unregisterBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override public void getOffloadedTransportDiscoveryDataScanSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.bluetooth.IBluetooth
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.bluetooth.IBluetooth interface,
     * generating a proxy if needed.
     */
    public static android.bluetooth.IBluetooth asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.bluetooth.IBluetooth))) {
        return ((android.bluetooth.IBluetooth)iin);
      }
      return new android.bluetooth.IBluetooth.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_getState:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getState(_arg0);
          break;
        }
        case TRANSACTION_enable:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.enable(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_disable:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.disable(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getAddress:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getAddress(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isLogRedactionEnabled:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLogRedactionEnabled(_arg0);
          break;
        }
        case TRANSACTION_getUuids:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getUuids(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setName:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setName(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getIdentityAddress:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getIdentityAddress(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getName:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getName(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getNameLengthForAdvertise:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getNameLengthForAdvertise(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getIoCapability:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getIoCapability(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setIoCapability:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setIoCapability(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getScanMode:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getScanMode(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setScanMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setScanMode(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getDiscoverableTimeout:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getDiscoverableTimeout(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setDiscoverableTimeout:
        {
          long _arg0;
          _arg0 = data.readLong();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setDiscoverableTimeout(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_startDiscovery:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.startDiscovery(_arg0, _arg1);
          break;
        }
        case TRANSACTION_cancelDiscovery:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.cancelDiscovery(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isDiscovering:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isDiscovering(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getDiscoveryEndMillis:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getDiscoveryEndMillis(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getAdapterConnectionState:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getAdapterConnectionState(_arg0);
          break;
        }
        case TRANSACTION_getProfileConnectionState:
        {
          int _arg0;
          _arg0 = data.readInt();
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getProfileConnectionState(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getBondedDevices:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getBondedDevices(_arg0, _arg1);
          break;
        }
        case TRANSACTION_createBond:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.bluetooth.OobData _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.bluetooth.OobData.CREATOR);
          android.bluetooth.OobData _arg3;
          _arg3 = _Parcel.readTypedObject(data, android.bluetooth.OobData.CREATOR);
          android.content.AttributionSource _arg4;
          _arg4 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg5;
          _arg5 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.createBond(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_cancelBondProcess:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.cancelBondProcess(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_removeBond:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.removeBond(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getBondState:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getBondState(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_isBondingInitiatedLocally:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isBondingInitiatedLocally(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getSupportedProfiles:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getSupportedProfiles(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getConnectionState:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getConnectionState(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getConnectionHandle:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getConnectionHandle(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getRemoteName:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRemoteName(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getRemoteType:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRemoteType(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getRemoteAlias:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRemoteAlias(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setRemoteAlias:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setRemoteAlias(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getRemoteClass:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRemoteClass(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getRemoteUuids:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRemoteUuids(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_fetchRemoteUuids:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.fetchRemoteUuids(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_sdpSearch:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.os.ParcelUuid _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.os.ParcelUuid.CREATOR);
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.sdpSearch(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getBatteryLevel:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getBatteryLevel(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getMaxConnectedAudioDevices:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getMaxConnectedAudioDevices(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setPin:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          int _arg2;
          _arg2 = data.readInt();
          byte[] _arg3;
          _arg3 = data.createByteArray();
          android.content.AttributionSource _arg4;
          _arg4 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg5;
          _arg5 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setPin(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_setPasskey:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          int _arg2;
          _arg2 = data.readInt();
          byte[] _arg3;
          _arg3 = data.createByteArray();
          android.content.AttributionSource _arg4;
          _arg4 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg5;
          _arg5 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setPasskey(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
          break;
        }
        case TRANSACTION_setPairingConfirmation:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setPairingConfirmation(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getPhonebookAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getPhonebookAccessPermission(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setSilenceMode:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setSilenceMode(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getSilenceMode:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getSilenceMode(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setPhonebookAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setPhonebookAccessPermission(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getMessageAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getMessageAccessPermission(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setMessageAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setMessageAccessPermission(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getSimAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getSimAccessPermission(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setSimAccessPermission:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setSimAccessPermission(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_registerCallback:
        {
          android.bluetooth.IBluetoothCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.registerCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_unregisterCallback:
        {
          android.bluetooth.IBluetoothCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.unregisterCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_logL2capcocServerConnection:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          boolean _arg2;
          _arg2 = (0!=data.readInt());
          int _arg3;
          _arg3 = data.readInt();
          long _arg4;
          _arg4 = data.readLong();
          long _arg5;
          _arg5 = data.readLong();
          long _arg6;
          _arg6 = data.readLong();
          long _arg7;
          _arg7 = data.readLong();
          com.android.modules.utils.SynchronousResultReceiver _arg8;
          _arg8 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.logL2capcocServerConnection(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8);
          break;
        }
        case TRANSACTION_getSocketManager:
        {
          android.bluetooth.IBluetoothSocketManager _result = this.getSocketManager();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_logL2capcocClientConnection:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          boolean _arg2;
          _arg2 = (0!=data.readInt());
          int _arg3;
          _arg3 = data.readInt();
          long _arg4;
          _arg4 = data.readLong();
          long _arg5;
          _arg5 = data.readLong();
          long _arg6;
          _arg6 = data.readLong();
          com.android.modules.utils.SynchronousResultReceiver _arg7;
          _arg7 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.logL2capcocClientConnection(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
          break;
        }
        case TRANSACTION_factoryReset:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.factoryReset(_arg0, _arg1);
          break;
        }
        case TRANSACTION_isMultiAdvertisementSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isMultiAdvertisementSupported(_arg0);
          break;
        }
        case TRANSACTION_isOffloadedFilteringSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isOffloadedFilteringSupported(_arg0);
          break;
        }
        case TRANSACTION_isOffloadedScanBatchingSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isOffloadedScanBatchingSupported(_arg0);
          break;
        }
        case TRANSACTION_isActivityAndEnergyReportingSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isActivityAndEnergyReportingSupported(_arg0);
          break;
        }
        case TRANSACTION_isLe2MPhySupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLe2MPhySupported(_arg0);
          break;
        }
        case TRANSACTION_isLeCodedPhySupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLeCodedPhySupported(_arg0);
          break;
        }
        case TRANSACTION_isLeExtendedAdvertisingSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLeExtendedAdvertisingSupported(_arg0);
          break;
        }
        case TRANSACTION_isLePeriodicAdvertisingSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLePeriodicAdvertisingSupported(_arg0);
          break;
        }
        case TRANSACTION_isLeAudioSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLeAudioSupported(_arg0);
          break;
        }
        case TRANSACTION_isLeAudioBroadcastSourceSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLeAudioBroadcastSourceSupported(_arg0);
          break;
        }
        case TRANSACTION_isLeAudioBroadcastAssistantSupported:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isLeAudioBroadcastAssistantSupported(_arg0);
          break;
        }
        case TRANSACTION_isDistanceMeasurementSupported:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isDistanceMeasurementSupported(_arg0, _arg1);
          break;
        }
        case TRANSACTION_getLeMaximumAdvertisingDataLength:
        {
          com.android.modules.utils.SynchronousResultReceiver _arg0;
          _arg0 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getLeMaximumAdvertisingDataLength(_arg0);
          break;
        }
        case TRANSACTION_reportActivityInfo:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.reportActivityInfo(_arg0, _arg1);
          break;
        }
        case TRANSACTION_registerMetadataListener:
        {
          android.bluetooth.IBluetoothMetadataListener _arg0;
          _arg0 = android.bluetooth.IBluetoothMetadataListener.Stub.asInterface(data.readStrongBinder());
          android.bluetooth.BluetoothDevice _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.registerMetadataListener(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_unregisterMetadataListener:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.unregisterMetadataListener(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setMetadata:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          byte[] _arg2;
          _arg2 = data.createByteArray();
          android.content.AttributionSource _arg3;
          _arg3 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg4;
          _arg4 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setMetadata(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_getMetadata:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getMetadata(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_requestActivityInfo:
        {
          android.bluetooth.IBluetoothActivityEnergyInfoListener _arg0;
          _arg0 = android.bluetooth.IBluetoothActivityEnergyInfoListener.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          this.requestActivityInfo(_arg0, _arg1);
          break;
        }
        case TRANSACTION_startBrEdr:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.startBrEdr(_arg0, _arg1);
          break;
        }
        case TRANSACTION_stopBle:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.stopBle(_arg0, _arg1);
          break;
        }
        case TRANSACTION_connectAllEnabledProfiles:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.connectAllEnabledProfiles(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_disconnectAllEnabledProfiles:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.disconnectAllEnabledProfiles(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setActiveDevice:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          int _arg1;
          _arg1 = data.readInt();
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setActiveDevice(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getActiveDevices:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getActiveDevices(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getMostRecentlyConnectedDevices:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getMostRecentlyConnectedDevices(_arg0, _arg1);
          break;
        }
        case TRANSACTION_removeActiveDevice:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.removeActiveDevice(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_registerBluetoothConnectionCallback:
        {
          android.bluetooth.IBluetoothConnectionCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothConnectionCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.registerBluetoothConnectionCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_unregisterBluetoothConnectionCallback:
        {
          android.bluetooth.IBluetoothConnectionCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothConnectionCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.unregisterBluetoothConnectionCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_canBondWithoutDialog:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.canBondWithoutDialog(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getPackageNameOfBondingApplication:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getPackageNameOfBondingApplication(_arg0, _arg1);
          break;
        }
        case TRANSACTION_generateLocalOobData:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.bluetooth.IBluetoothOobDataCallback _arg1;
          _arg1 = android.bluetooth.IBluetoothOobDataCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.generateLocalOobData(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_allowLowLatencyAudio:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          android.bluetooth.BluetoothDevice _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.allowLowLatencyAudio(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_isRequestAudioPolicyAsSinkSupported:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.isRequestAudioPolicyAsSinkSupported(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_requestAudioPolicyAsSink:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.bluetooth.BluetoothSinkAudioPolicy _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothSinkAudioPolicy.CREATOR);
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.requestAudioPolicyAsSink(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getRequestedAudioPolicyAsSink:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getRequestedAudioPolicyAsSink(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_startRfcommListener:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          android.os.ParcelUuid _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.os.ParcelUuid.CREATOR);
          android.app.PendingIntent _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.app.PendingIntent.CREATOR);
          android.content.AttributionSource _arg3;
          _arg3 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg4;
          _arg4 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.startRfcommListener(_arg0, _arg1, _arg2, _arg3, _arg4);
          break;
        }
        case TRANSACTION_stopRfcommListener:
        {
          android.os.ParcelUuid _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.os.ParcelUuid.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.stopRfcommListener(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_retrievePendingSocketForServiceRecord:
        {
          android.os.ParcelUuid _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.os.ParcelUuid.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.retrievePendingSocketForServiceRecord(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_setForegroundUserId:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          this.setForegroundUserId(_arg0, _arg1);
          break;
        }
        case TRANSACTION_setPreferredAudioProfiles:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.os.Bundle _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.os.Bundle.CREATOR);
          android.content.AttributionSource _arg2;
          _arg2 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg3;
          _arg3 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.setPreferredAudioProfiles(_arg0, _arg1, _arg2, _arg3);
          break;
        }
        case TRANSACTION_getPreferredAudioProfiles:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getPreferredAudioProfiles(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_registerPreferredAudioProfilesChangedCallback:
        {
          android.bluetooth.IBluetoothPreferredAudioProfilesCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothPreferredAudioProfilesCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.registerPreferredAudioProfilesChangedCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_unregisterPreferredAudioProfilesChangedCallback:
        {
          android.bluetooth.IBluetoothPreferredAudioProfilesCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothPreferredAudioProfilesCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.unregisterPreferredAudioProfilesChangedCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_notifyActiveDeviceChangeApplied:
        {
          android.bluetooth.BluetoothDevice _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.bluetooth.BluetoothDevice.CREATOR);
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.notifyActiveDeviceChangeApplied(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_registerBluetoothQualityReportReadyCallback:
        {
          android.bluetooth.IBluetoothQualityReportReadyCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothQualityReportReadyCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.registerBluetoothQualityReportReadyCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_unregisterBluetoothQualityReportReadyCallback:
        {
          android.bluetooth.IBluetoothQualityReportReadyCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothQualityReportReadyCallback.Stub.asInterface(data.readStrongBinder());
          android.content.AttributionSource _arg1;
          _arg1 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg2;
          _arg2 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.unregisterBluetoothQualityReportReadyCallback(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_getOffloadedTransportDiscoveryDataScanSupported:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          com.android.modules.utils.SynchronousResultReceiver _arg1;
          _arg1 = _Parcel.readTypedObject(data, com.android.modules.utils.SynchronousResultReceiver.CREATOR);
          this.getOffloadedTransportDiscoveryDataScanSupported(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.bluetooth.IBluetooth
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void getState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getState, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void enable(boolean quietMode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((quietMode)?(1):(0)));
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void disable(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_disable, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAddress(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAddress, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLogRedactionEnabled(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLogRedactionEnabled, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getUuids(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getUuids, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setName(java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(name);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setName, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getIdentityAddress(java.lang.String address, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(address);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getIdentityAddress, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getName(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getName, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getNameLengthForAdvertise(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getNameLengthForAdvertise, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getIoCapability(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getIoCapability, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setIoCapability(int capability, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(capability);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setIoCapability, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getScanMode(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getScanMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setScanMode(int mode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(mode);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setScanMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getDiscoverableTimeout(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDiscoverableTimeout, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setDiscoverableTimeout(long timeout, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeLong(timeout);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setDiscoverableTimeout, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startDiscovery, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelDiscovery, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isDiscovering(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isDiscovering, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getDiscoveryEndMillis(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDiscoveryEndMillis, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getAdapterConnectionState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAdapterConnectionState, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getProfileConnectionState(int profile, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(profile);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getProfileConnectionState, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBondedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBondedDevices, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void createBond(android.bluetooth.BluetoothDevice device, int transport, android.bluetooth.OobData p192Data, android.bluetooth.OobData p256Data, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(transport);
          _Parcel.writeTypedObject(_data, p192Data, 0);
          _Parcel.writeTypedObject(_data, p256Data, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_createBond, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void cancelBondProcess(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_cancelBondProcess, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void removeBond(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeBond, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBondState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBondState, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isBondingInitiatedLocally(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBondingInitiatedLocally, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSupportedProfiles(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSupportedProfiles, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getConnectionState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getConnectionState, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getConnectionHandle(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(transport);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getConnectionHandle, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRemoteName(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRemoteName, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRemoteType(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRemoteType, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRemoteAlias(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRemoteAlias, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setRemoteAlias(android.bluetooth.BluetoothDevice device, java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeString(name);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRemoteAlias, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRemoteClass(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRemoteClass, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getRemoteUuids(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRemoteUuids, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void fetchRemoteUuids(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(transport);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_fetchRemoteUuids, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void sdpSearch(android.bluetooth.BluetoothDevice device, android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, uuid, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sdpSearch, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getBatteryLevel(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBatteryLevel, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMaxConnectedAudioDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxConnectedAudioDevices, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPin(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] pinCode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(((accept)?(1):(0)));
          _data.writeInt(len);
          _data.writeByteArray(pinCode);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPin, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPasskey(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] passkey, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(((accept)?(1):(0)));
          _data.writeInt(len);
          _data.writeByteArray(passkey);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPasskey, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPairingConfirmation(android.bluetooth.BluetoothDevice device, boolean accept, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(((accept)?(1):(0)));
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPairingConfirmation, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPhonebookAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSilenceMode(android.bluetooth.BluetoothDevice device, boolean silence, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(((silence)?(1):(0)));
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSilenceMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSilenceMode(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSilenceMode, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(value);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPhonebookAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMessageAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMessageAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setMessageAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(value);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMessageAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getSimAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSimAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setSimAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(value);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSimAccessPermission, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registerCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      // For Socket
      @Override public void logL2capcocServerConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, long timeoutMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(port);
          _data.writeInt(((isSecured)?(1):(0)));
          _data.writeInt(result);
          _data.writeLong(socketCreationTimeMillis);
          _data.writeLong(socketCreationLatencyMillis);
          _data.writeLong(socketConnectionTimeMillis);
          _data.writeLong(timeoutMillis);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_logL2capcocServerConnection, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public android.bluetooth.IBluetoothSocketManager getSocketManager() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.bluetooth.IBluetoothSocketManager _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSocketManager, _data, _reply, 0);
          _reply.readException();
          _result = android.bluetooth.IBluetoothSocketManager.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void logL2capcocClientConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(port);
          _data.writeInt(((isSecured)?(1):(0)));
          _data.writeInt(result);
          _data.writeLong(socketCreationTimeMillis);
          _data.writeLong(socketCreationLatencyMillis);
          _data.writeLong(socketConnectionTimeMillis);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_logL2capcocClientConnection, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void factoryReset(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_factoryReset, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isMultiAdvertisementSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isMultiAdvertisementSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isOffloadedFilteringSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isOffloadedFilteringSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isOffloadedScanBatchingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isOffloadedScanBatchingSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isActivityAndEnergyReportingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isActivityAndEnergyReportingSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLe2MPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLe2MPhySupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLeCodedPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLeCodedPhySupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLeExtendedAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLeExtendedAdvertisingSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLePeriodicAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLePeriodicAdvertisingSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLeAudioSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLeAudioSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLeAudioBroadcastSourceSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLeAudioBroadcastSourceSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isLeAudioBroadcastAssistantSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isLeAudioBroadcastAssistantSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isDistanceMeasurementSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isDistanceMeasurementSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getLeMaximumAdvertisingDataLength(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLeMaximumAdvertisingDataLength, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void reportActivityInfo(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reportActivityInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      // For Metadata
      @Override public void registerMetadataListener(android.bluetooth.IBluetoothMetadataListener listener, android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerMetadataListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterMetadataListener(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterMetadataListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setMetadata(android.bluetooth.BluetoothDevice device, int key, byte[] value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(key);
          _data.writeByteArray(value);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMetadata, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMetadata(android.bluetooth.BluetoothDevice device, int key, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(key);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMetadata, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /**
       * Requests the controller activity info asynchronously.
       * The implementor is expected to reply with the
       * {@link android.bluetooth.BluetoothActivityEnergyInfo} object placed into the Bundle with the
       * key {@link android.os.BatteryStats#RESULT_RECEIVER_CONTROLLER_KEY}.
       * The result code is ignored.
       */
      @Override public void requestActivityInfo(android.bluetooth.IBluetoothActivityEnergyInfoListener listener, android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(listener);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestActivityInfo, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void startBrEdr(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startBrEdr, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopBle(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopBle, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void connectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_connectAllEnabledProfiles, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void disconnectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_disconnectAllEnabledProfiles, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setActiveDevice(android.bluetooth.BluetoothDevice device, int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _data.writeInt(profiles);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setActiveDevice, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getActiveDevices(int profile, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(profile);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getActiveDevices, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getMostRecentlyConnectedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMostRecentlyConnectedDevices, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void removeActiveDevice(int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(profiles);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_removeActiveDevice, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registerBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerBluetoothConnectionCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterBluetoothConnectionCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void canBondWithoutDialog(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_canBondWithoutDialog, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPackageNameOfBondingApplication(android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPackageNameOfBondingApplication, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void generateLocalOobData(int transport, android.bluetooth.IBluetoothOobDataCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(transport);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_generateLocalOobData, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void allowLowLatencyAudio(boolean allowed, android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((allowed)?(1):(0)));
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_allowLowLatencyAudio, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void isRequestAudioPolicyAsSinkSupported(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isRequestAudioPolicyAsSinkSupported, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void requestAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.bluetooth.BluetoothSinkAudioPolicy policies, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, policies, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestAudioPolicyAsSink, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void getRequestedAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRequestedAudioPolicyAsSink, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void startRfcommListener(java.lang.String name, android.os.ParcelUuid uuid, android.app.PendingIntent intent, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(name);
          _Parcel.writeTypedObject(_data, uuid, 0);
          _Parcel.writeTypedObject(_data, intent, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_startRfcommListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void stopRfcommListener(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, uuid, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_stopRfcommListener, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void retrievePendingSocketForServiceRecord(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, uuid, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_retrievePendingSocketForServiceRecord, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setForegroundUserId(int userId, android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(userId);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setForegroundUserId, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void setPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.os.Bundle modeToProfileBundle, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, modeToProfileBundle, 0);
          _Parcel.writeTypedObject(_data, source, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setPreferredAudioProfiles, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, source, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getPreferredAudioProfiles, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registerPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerPreferredAudioProfilesChangedCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterPreferredAudioProfilesChangedCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void notifyActiveDeviceChangeApplied(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, device, 0);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_notifyActiveDeviceChangeApplied, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void registerBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerBluetoothQualityReportReadyCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void unregisterBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterBluetoothQualityReportReadyCallback, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void getOffloadedTransportDiscoveryDataScanSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _Parcel.writeTypedObject(_data, receiver, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOffloadedTransportDiscoveryDataScanSupported, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    public static final java.lang.String DESCRIPTOR = "android.bluetooth.IBluetooth";
    static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_enable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_disable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_isLogRedactionEnabled = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getUuids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getIdentityAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getNameLengthForAdvertise = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getIoCapability = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_setIoCapability = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getScanMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_setScanMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_getDiscoverableTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_setDiscoverableTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_startDiscovery = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_cancelDiscovery = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_isDiscovering = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_getDiscoveryEndMillis = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getAdapterConnectionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getProfileConnectionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_getBondedDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_createBond = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_cancelBondProcess = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_removeBond = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_getBondState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_isBondingInitiatedLocally = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_getSupportedProfiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_getConnectionState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_getConnectionHandle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_getRemoteName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_getRemoteType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_getRemoteAlias = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_setRemoteAlias = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_getRemoteClass = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_getRemoteUuids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_fetchRemoteUuids = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_sdpSearch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
    static final int TRANSACTION_getBatteryLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
    static final int TRANSACTION_getMaxConnectedAudioDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
    static final int TRANSACTION_setPin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
    static final int TRANSACTION_setPasskey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
    static final int TRANSACTION_setPairingConfirmation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
    static final int TRANSACTION_getPhonebookAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
    static final int TRANSACTION_setSilenceMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
    static final int TRANSACTION_getSilenceMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
    static final int TRANSACTION_setPhonebookAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
    static final int TRANSACTION_getMessageAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
    static final int TRANSACTION_setMessageAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
    static final int TRANSACTION_getSimAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
    static final int TRANSACTION_setSimAccessPermission = (android.os.IBinder.FIRST_CALL_TRANSACTION + 51);
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 52);
    static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 53);
    static final int TRANSACTION_logL2capcocServerConnection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 54);
    static final int TRANSACTION_getSocketManager = (android.os.IBinder.FIRST_CALL_TRANSACTION + 55);
    static final int TRANSACTION_logL2capcocClientConnection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 56);
    static final int TRANSACTION_factoryReset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 57);
    static final int TRANSACTION_isMultiAdvertisementSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 58);
    static final int TRANSACTION_isOffloadedFilteringSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 59);
    static final int TRANSACTION_isOffloadedScanBatchingSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 60);
    static final int TRANSACTION_isActivityAndEnergyReportingSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 61);
    static final int TRANSACTION_isLe2MPhySupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 62);
    static final int TRANSACTION_isLeCodedPhySupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 63);
    static final int TRANSACTION_isLeExtendedAdvertisingSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 64);
    static final int TRANSACTION_isLePeriodicAdvertisingSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 65);
    static final int TRANSACTION_isLeAudioSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 66);
    static final int TRANSACTION_isLeAudioBroadcastSourceSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 67);
    static final int TRANSACTION_isLeAudioBroadcastAssistantSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 68);
    static final int TRANSACTION_isDistanceMeasurementSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 69);
    static final int TRANSACTION_getLeMaximumAdvertisingDataLength = (android.os.IBinder.FIRST_CALL_TRANSACTION + 70);
    static final int TRANSACTION_reportActivityInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 71);
    static final int TRANSACTION_registerMetadataListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 72);
    static final int TRANSACTION_unregisterMetadataListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 73);
    static final int TRANSACTION_setMetadata = (android.os.IBinder.FIRST_CALL_TRANSACTION + 74);
    static final int TRANSACTION_getMetadata = (android.os.IBinder.FIRST_CALL_TRANSACTION + 75);
    static final int TRANSACTION_requestActivityInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 76);
    static final int TRANSACTION_startBrEdr = (android.os.IBinder.FIRST_CALL_TRANSACTION + 77);
    static final int TRANSACTION_stopBle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 78);
    static final int TRANSACTION_connectAllEnabledProfiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 79);
    static final int TRANSACTION_disconnectAllEnabledProfiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 80);
    static final int TRANSACTION_setActiveDevice = (android.os.IBinder.FIRST_CALL_TRANSACTION + 81);
    static final int TRANSACTION_getActiveDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 82);
    static final int TRANSACTION_getMostRecentlyConnectedDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 83);
    static final int TRANSACTION_removeActiveDevice = (android.os.IBinder.FIRST_CALL_TRANSACTION + 84);
    static final int TRANSACTION_registerBluetoothConnectionCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 85);
    static final int TRANSACTION_unregisterBluetoothConnectionCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 86);
    static final int TRANSACTION_canBondWithoutDialog = (android.os.IBinder.FIRST_CALL_TRANSACTION + 87);
    static final int TRANSACTION_getPackageNameOfBondingApplication = (android.os.IBinder.FIRST_CALL_TRANSACTION + 88);
    static final int TRANSACTION_generateLocalOobData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 89);
    static final int TRANSACTION_allowLowLatencyAudio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 90);
    static final int TRANSACTION_isRequestAudioPolicyAsSinkSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 91);
    static final int TRANSACTION_requestAudioPolicyAsSink = (android.os.IBinder.FIRST_CALL_TRANSACTION + 92);
    static final int TRANSACTION_getRequestedAudioPolicyAsSink = (android.os.IBinder.FIRST_CALL_TRANSACTION + 93);
    static final int TRANSACTION_startRfcommListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 94);
    static final int TRANSACTION_stopRfcommListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 95);
    static final int TRANSACTION_retrievePendingSocketForServiceRecord = (android.os.IBinder.FIRST_CALL_TRANSACTION + 96);
    static final int TRANSACTION_setForegroundUserId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 97);
    static final int TRANSACTION_setPreferredAudioProfiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 98);
    static final int TRANSACTION_getPreferredAudioProfiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 99);
    static final int TRANSACTION_registerPreferredAudioProfilesChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 100);
    static final int TRANSACTION_unregisterPreferredAudioProfilesChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 101);
    static final int TRANSACTION_notifyActiveDeviceChangeApplied = (android.os.IBinder.FIRST_CALL_TRANSACTION + 102);
    static final int TRANSACTION_registerBluetoothQualityReportReadyCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 103);
    static final int TRANSACTION_unregisterBluetoothQualityReportReadyCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 104);
    static final int TRANSACTION_getOffloadedTransportDiscoveryDataScanSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 105);
  }
  @android.annotation.RequiresNoPermission
  public void getState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT},anyOf={android.Manifest.permission.INTERACT_ACROSS_USERS,android.Manifest.permission.MANAGE_USERS})
  public void enable(boolean quietMode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void disable(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.LOCAL_MAC_ADDRESS})
  public void getAddress(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLogRedactionEnabled(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getUuids(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void setName(java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getIdentityAddress(java.lang.String address, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getName(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
  public void getNameLengthForAdvertise(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getIoCapability(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setIoCapability(int capability, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void getScanMode(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void setScanMode(int mode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void getDiscoverableTimeout(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void setDiscoverableTimeout(long timeout, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void startDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void cancelDiscovery(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
  public void isDiscovering(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getDiscoveryEndMillis(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void getAdapterConnectionState(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void getProfileConnectionState(int profile, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getBondedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void createBond(android.bluetooth.BluetoothDevice device, int transport, android.bluetooth.OobData p192Data, android.bluetooth.OobData p256Data, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void cancelBondProcess(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void removeBond(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getBondState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void isBondingInitiatedLocally(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getSupportedProfiles(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getConnectionState(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getConnectionHandle(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRemoteName(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRemoteType(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRemoteAlias(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT})
  public void setRemoteAlias(android.bluetooth.BluetoothDevice device, java.lang.String name, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRemoteClass(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRemoteUuids(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void fetchRemoteUuids(android.bluetooth.BluetoothDevice device, int transport, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void sdpSearch(android.bluetooth.BluetoothDevice device, android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getBatteryLevel(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getMaxConnectedAudioDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void setPin(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] pinCode, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void setPasskey(android.bluetooth.BluetoothDevice device, boolean accept, int len, byte[] passkey, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setPairingConfirmation(android.bluetooth.BluetoothDevice device, boolean accept, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setSilenceMode(android.bluetooth.BluetoothDevice device, boolean silence, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getSilenceMode(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setPhonebookAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getMessageAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setMessageAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getSimAccessPermission(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setSimAccessPermission(android.bluetooth.BluetoothDevice device, int value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void registerCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void unregisterCallback(android.bluetooth.IBluetoothCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  // For Socket
  @android.annotation.RequiresNoPermission
  public void logL2capcocServerConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, long timeoutMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public android.bluetooth.IBluetoothSocketManager getSocketManager() throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void logL2capcocClientConnection(android.bluetooth.BluetoothDevice device, int port, boolean isSecured, int result, long socketCreationTimeMillis, long socketCreationLatencyMillis, long socketConnectionTimeMillis, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void factoryReset(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isMultiAdvertisementSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isOffloadedFilteringSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isOffloadedScanBatchingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isActivityAndEnergyReportingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLe2MPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLeCodedPhySupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLeExtendedAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLePeriodicAdvertisingSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLeAudioSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLeAudioBroadcastSourceSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void isLeAudioBroadcastAssistantSupported(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void isDistanceMeasurementSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void getLeMaximumAdvertisingDataLength(com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void reportActivityInfo(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  // For Metadata
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void registerMetadataListener(android.bluetooth.IBluetoothMetadataListener listener, android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void unregisterMetadataListener(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setMetadata(android.bluetooth.BluetoothDevice device, int key, byte[] value, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getMetadata(android.bluetooth.BluetoothDevice device, int key, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  /**
   * Requests the controller activity info asynchronously.
   * The implementor is expected to reply with the
   * {@link android.bluetooth.BluetoothActivityEnergyInfo} object placed into the Bundle with the
   * key {@link android.os.BatteryStats#RESULT_RECEIVER_CONTROLLER_KEY}.
   * The result code is ignored.
   */
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void requestActivityInfo(android.bluetooth.IBluetoothActivityEnergyInfoListener listener, android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void startBrEdr(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void stopBle(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED,android.Manifest.permission.MODIFY_PHONE_STATE})
  public void connectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void disconnectAllEnabledProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED,android.Manifest.permission.MODIFY_PHONE_STATE})
  public void setActiveDevice(android.bluetooth.BluetoothDevice device, int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getActiveDevices(int profile, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getMostRecentlyConnectedDevices(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED,android.Manifest.permission.MODIFY_PHONE_STATE})
  public void removeActiveDevice(int profiles, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void registerBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void unregisterBluetoothConnectionCallback(android.bluetooth.IBluetoothConnectionCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void canBondWithoutDialog(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getPackageNameOfBondingApplication(android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void generateLocalOobData(int transport, android.bluetooth.IBluetoothOobDataCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void allowLowLatencyAudio(boolean allowed, android.bluetooth.BluetoothDevice device, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void isRequestAudioPolicyAsSinkSupported(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void requestAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.bluetooth.BluetoothSinkAudioPolicy policies, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public void getRequestedAudioPolicyAsSink(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void startRfcommListener(java.lang.String name, android.os.ParcelUuid uuid, android.app.PendingIntent intent, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void stopRfcommListener(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void retrievePendingSocketForServiceRecord(android.os.ParcelUuid uuid, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setForegroundUserId(int userId, android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void setPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.os.Bundle modeToProfileBundle, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getPreferredAudioProfiles(android.bluetooth.BluetoothDevice device, android.content.AttributionSource source, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void registerPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void unregisterPreferredAudioProfilesChangedCallback(android.bluetooth.IBluetoothPreferredAudioProfilesCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void notifyActiveDeviceChangeApplied(android.bluetooth.BluetoothDevice device, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void registerBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void unregisterBluetoothQualityReportReadyCallback(android.bluetooth.IBluetoothQualityReportReadyCallback callback, android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_SCAN,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public void getOffloadedTransportDiscoveryDataScanSupported(android.content.AttributionSource attributionSource, com.android.modules.utils.SynchronousResultReceiver receiver) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}
