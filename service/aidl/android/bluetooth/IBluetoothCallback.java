/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /usr/local/google/home/licorne/work/aosp/out/host/linux-x86/bin/aidl --lang=java --out=. -I../../system/binder -I ../../../../../frameworks/libs/modules-utils/java -I ../../framework/aidl-export/ -I ../../../../../frameworks/base/core/java/ -I ../binder ../../system/binder/android/bluetooth/IBluetooth.aidl ../../system/binder/android/bluetooth/IBluetoothCallback.aidl ../binder/android/bluetooth/IBluetoothManager.aidl
 */
package android.bluetooth;
/**
 * System private API for Bluetooth service callbacks.
 * 
 * {@hide}
 */
public interface IBluetoothCallback extends android.os.IInterface
{
  /** Default implementation for IBluetoothCallback. */
  public static class Default implements android.bluetooth.IBluetoothCallback
  {
    //void onRfcommChannelFound(int channel);
    @Override public void onBluetoothStateChange(int prevState, int newState) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements android.bluetooth.IBluetoothCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an android.bluetooth.IBluetoothCallback interface,
     * generating a proxy if needed.
     */
    public static android.bluetooth.IBluetoothCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof android.bluetooth.IBluetoothCallback))) {
        return ((android.bluetooth.IBluetoothCallback)iin);
      }
      return new android.bluetooth.IBluetoothCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onBluetoothStateChange:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.onBluetoothStateChange(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements android.bluetooth.IBluetoothCallback
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
      //void onRfcommChannelFound(int channel);
      @Override public void onBluetoothStateChange(int prevState, int newState) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(prevState);
          _data.writeInt(newState);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onBluetoothStateChange, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    public static final java.lang.String DESCRIPTOR = "android.bluetooth.IBluetoothCallback";
    static final int TRANSACTION_onBluetoothStateChange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  //void onRfcommChannelFound(int channel);
  public void onBluetoothStateChange(int prevState, int newState) throws android.os.RemoteException;
}
