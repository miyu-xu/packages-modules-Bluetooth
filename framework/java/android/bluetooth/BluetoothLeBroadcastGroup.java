/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.List;

/**
 * This class represents an LE Audio Broadcast group and the associated information that is needed
 * by Broadcast Audio Scan Service (BASS) residing on a Scan Delegator.
 *
 * <p>For example, the Scan Delegator on an LE Audio Broadcast Sink can use the information
 * contained within an instance of this class to synchronize with an LE Audio Broadcast group in
 * order to listen to audio from Broadcast subgroup using one or more BISes
 *
 * <p>BroadcastAssistant has a BASS client which facilitates scanning and discovery of Broadcast
 * Sources on behalf of say a Broadcast Sink. Upon successful discovery of one or more Broadcast
 * sources, this information needs to be communicated to the BASS Server residing within the Scan
 * Delegator on a Broadcast Sink. This is achieved using the Periodic Advertising Synchronization
 * Transfer (PAST) procedure. This procedure uses information contained within an instance of this
 * class.
 *
 * @hide
 */
public final class BluetoothLeBroadcastGroup implements Parcelable {
    private static final String TAG = "BluetoothLeBroadcastSourceInfo";
    private static final boolean DBG = true;

    // Information needed for adding broadcast source

    // Optional: Identity address type
    private @BluetoothDevice.AddressType int mSourceAddressType;
    // Optional: Must use identity address
    private BluetoothDevice mSourceDevice;
    private int mSourceAdvertisingSid;
    private int mBroadcastId;
    private int mPaSyncInterval;
    private byte[] mBroadcastCode;

    // BASE structure

    // See Section 7 for description. Range: 0x000000 – 0xFFFFFF Units: μs
    //All other values: RFU
    private int mPresentationDelay;
    // Number of subgroups used to group BISes present in the BIG
    //Shall be at least 1, as defined by Rule 1
    // Sub group info numSubGroup = mSubGroups.length
    private List<BluetoothLeBroadcastGroup> mSubGroups;

    private static void log(@NonNull String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
;
