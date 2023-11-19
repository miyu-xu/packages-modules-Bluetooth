package android.bluetooth;

import android.os.Parcelable;
import android.os.Parcel;

public class BluetoothDevice implements Parcelable {
    public int describeContents() {
         return 0;
    }

     public void writeToParcel(Parcel out, int flags) {}

     public static final Parcelable.Creator<BluetoothDevice> CREATOR
             = new Parcelable.Creator<BluetoothDevice>() {
         public BluetoothDevice createFromParcel(Parcel in) {
             throw new IllegalStateException();
         }

         public BluetoothDevice[] newArray(int size) {
             throw new IllegalStateException();
         }
     };
}
