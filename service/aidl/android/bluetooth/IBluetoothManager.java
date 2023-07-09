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
public interface IBluetoothManager extends android.os.IInterface
{
  /** Default implementation for IBluetoothManager. */
  public static class Default implements android.bluetooth.IBluetoothManager
  {
    @Override public android.bluetooth.IBluetooth registerAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void unregisterAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void registerStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void unregisterStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException
    {
    }
    @Override public boolean enable(android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean enableNoAutoConnect(android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean disable(android.content.AttributionSource attributionSource, boolean persist) throws android.os.RemoteException
    {
      return false;
    }
    @Override public int getState() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public android.bluetooth.IBluetoothGatt getBluetoothGatt() throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean bindBluetoothProfileService(int profile, java.lang.String serviceName, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException
    {
      return false;
    }
    @Override public void unbindBluetoothProfileService(int profile, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String getAddress(android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String getName(android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
      return null;
    }
    @Override public boolean onFactoryReset(android.content.AttributionSource attributionSource) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isBleScanAlwaysAvailable() throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean enableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean disableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isBleAppPresent() throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean isHearingAidProfileSupported() throws android.os.RemoteException
    {
      return false;
    }
    @Override public int setBtHciSnoopLogMode(int mode) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getBtHciSnoopLogMode() throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.bluetooth.IBluetoothManager
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.bluetooth.IBluetoothManager interface,
     * generating a proxy if needed.
     */
    public static android.bluetooth.IBluetoothManager asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.bluetooth.IBluetoothManager))) {
        return ((android.bluetooth.IBluetoothManager)iin);
      }
      return new android.bluetooth.IBluetoothManager.Stub.Proxy(obj);
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
        case TRANSACTION_registerAdapter:
        {
          android.bluetooth.IBluetoothManagerCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothManagerCallback.Stub.asInterface(data.readStrongBinder());
          android.bluetooth.IBluetooth _result = this.registerAdapter(_arg0);
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_unregisterAdapter:
        {
          android.bluetooth.IBluetoothManagerCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothManagerCallback.Stub.asInterface(data.readStrongBinder());
          this.unregisterAdapter(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerStateChangeCallback:
        {
          android.bluetooth.IBluetoothStateChangeCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothStateChangeCallback.Stub.asInterface(data.readStrongBinder());
          this.registerStateChangeCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterStateChangeCallback:
        {
          android.bluetooth.IBluetoothStateChangeCallback _arg0;
          _arg0 = android.bluetooth.IBluetoothStateChangeCallback.Stub.asInterface(data.readStrongBinder());
          this.unregisterStateChangeCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_enable:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          boolean _result = this.enable(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_enableNoAutoConnect:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          boolean _result = this.enableNoAutoConnect(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_disable:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          boolean _result = this.disable(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_getState:
        {
          int _result = this.getState();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getBluetoothGatt:
        {
          android.bluetooth.IBluetoothGatt _result = this.getBluetoothGatt();
          reply.writeNoException();
          reply.writeStrongInterface(_result);
          break;
        }
        case TRANSACTION_bindBluetoothProfileService:
        {
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String _arg1;
          _arg1 = data.readString();
          android.bluetooth.IBluetoothProfileServiceConnection _arg2;
          _arg2 = android.bluetooth.IBluetoothProfileServiceConnection.Stub.asInterface(data.readStrongBinder());
          boolean _result = this.bindBluetoothProfileService(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_unbindBluetoothProfileService:
        {
          int _arg0;
          _arg0 = data.readInt();
          android.bluetooth.IBluetoothProfileServiceConnection _arg1;
          _arg1 = android.bluetooth.IBluetoothProfileServiceConnection.Stub.asInterface(data.readStrongBinder());
          this.unbindBluetoothProfileService(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getAddress:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          java.lang.String _result = this.getAddress(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_getName:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          java.lang.String _result = this.getName(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_onFactoryReset:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          boolean _result = this.onFactoryReset(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_isBleScanAlwaysAvailable:
        {
          boolean _result = this.isBleScanAlwaysAvailable();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_enableBle:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          android.os.IBinder _arg1;
          _arg1 = data.readStrongBinder();
          boolean _result = this.enableBle(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_disableBle:
        {
          android.content.AttributionSource _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.content.AttributionSource.CREATOR);
          android.os.IBinder _arg1;
          _arg1 = data.readStrongBinder();
          boolean _result = this.disableBle(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_isBleAppPresent:
        {
          boolean _result = this.isBleAppPresent();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_isHearingAidProfileSupported:
        {
          boolean _result = this.isHearingAidProfileSupported();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_setBtHciSnoopLogMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _result = this.setBtHciSnoopLogMode(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getBtHciSnoopLogMode:
        {
          int _result = this.getBtHciSnoopLogMode();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.bluetooth.IBluetoothManager
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
      @Override public android.bluetooth.IBluetooth registerAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.bluetooth.IBluetooth _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerAdapter, _data, _reply, 0);
          _reply.readException();
          _result = android.bluetooth.IBluetooth.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void unregisterAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterAdapter, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void registerStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerStateChangeCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterStateChangeCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean enable(android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enable, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean enableNoAutoConnect(android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableNoAutoConnect, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean disable(android.content.AttributionSource attributionSource, boolean persist) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _data.writeInt(((persist)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_disable, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getState() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public android.bluetooth.IBluetoothGatt getBluetoothGatt() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        android.bluetooth.IBluetoothGatt _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBluetoothGatt, _data, _reply, 0);
          _reply.readException();
          _result = android.bluetooth.IBluetoothGatt.Stub.asInterface(_reply.readStrongBinder());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean bindBluetoothProfileService(int profile, java.lang.String serviceName, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(profile);
          _data.writeString(serviceName);
          _data.writeStrongInterface(proxy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_bindBluetoothProfileService, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void unbindBluetoothProfileService(int profile, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(profile);
          _data.writeStrongInterface(proxy);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unbindBluetoothProfileService, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String getAddress(android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getAddress, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getName(android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getName, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean onFactoryReset(android.content.AttributionSource attributionSource) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onFactoryReset, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isBleScanAlwaysAvailable() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBleScanAlwaysAvailable, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean enableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _data.writeStrongBinder(b);
          boolean _status = mRemote.transact(Stub.TRANSACTION_enableBle, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean disableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, attributionSource, 0);
          _data.writeStrongBinder(b);
          boolean _status = mRemote.transact(Stub.TRANSACTION_disableBle, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isBleAppPresent() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBleAppPresent, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean isHearingAidProfileSupported() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHearingAidProfileSupported, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setBtHciSnoopLogMode(int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setBtHciSnoopLogMode, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getBtHciSnoopLogMode() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBtHciSnoopLogMode, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    public static final java.lang.String DESCRIPTOR = "android.bluetooth.IBluetoothManager";
    static final int TRANSACTION_registerAdapter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterAdapter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerStateChangeCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_unregisterStateChangeCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_enable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_enableNoAutoConnect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_disable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_getBluetoothGatt = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_bindBluetoothProfileService = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_unbindBluetoothProfileService = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onFactoryReset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_isBleScanAlwaysAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_enableBle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_disableBle = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_isBleAppPresent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_isHearingAidProfileSupported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_setBtHciSnoopLogMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_getBtHciSnoopLogMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
  }
  @android.annotation.RequiresNoPermission
  public android.bluetooth.IBluetooth registerAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void unregisterAdapter(android.bluetooth.IBluetoothManagerCallback callback) throws android.os.RemoteException;
  public void registerStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException;
  public void unregisterStateChangeCallback(android.bluetooth.IBluetoothStateChangeCallback callback) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public boolean enable(android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public boolean enableNoAutoConnect(android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public boolean disable(android.content.AttributionSource attributionSource, boolean persist) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public int getState() throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public android.bluetooth.IBluetoothGatt getBluetoothGatt() throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public boolean bindBluetoothProfileService(int profile, java.lang.String serviceName, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public void unbindBluetoothProfileService(int profile, android.bluetooth.IBluetoothProfileServiceConnection proxy) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.LOCAL_MAC_ADDRESS})
  public java.lang.String getAddress(android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public java.lang.String getName(android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})
  public boolean onFactoryReset(android.content.AttributionSource attributionSource) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public boolean isBleScanAlwaysAvailable() throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public boolean enableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
  public boolean disableBle(android.content.AttributionSource attributionSource, android.os.IBinder b) throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public boolean isBleAppPresent() throws android.os.RemoteException;
  @android.annotation.RequiresNoPermission
  public boolean isHearingAidProfileSupported() throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
  public int setBtHciSnoopLogMode(int mode) throws android.os.RemoteException;
  @android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
  public int getBtHciSnoopLogMode() throws android.os.RemoteException;
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
