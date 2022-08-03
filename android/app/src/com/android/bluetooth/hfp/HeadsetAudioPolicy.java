package com.android.bluetooth.hfp;

public class HeadsetAudioPolicy {
    int mCallPickUpPolicy;
    int mConnectDuringCallPolicy;
    int mCallUiPolicy;

    HeadsetAudioPolicy(int callPickUpPolicy, int connectDuringCallPolicy, int callUiPolicy) {
      mCallPickUpPolicy = callPickUpPolicy;
      mConnectDuringCallPolicy = connectDuringCallPolicy;
      mCallUiPolicy = callUiPolicy;
    }
};