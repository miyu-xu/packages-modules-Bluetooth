/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.bluetooth.leaudio;

import android.bluetooth.BluetoothDevice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VoiceAssistantActivity extends AppCompatActivity {
    private static final String TAG = "VoiceAssistantActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_voice_assistant);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent: " + intent);
        if (Intent.ACTION_VOICE_COMMAND.equals(intent.getAction())) {
            BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
            int profile = intent.getIntExtra(EXTRA_PROFILE, -1);

            if (device != null) {
                Log.d(TAG, "Device: " + device);
            } else {
                Log.d(TAG, "Device extra not found");
            }
            if (profile != -1) {
                Log.d(TAG, "Profile: " + profile);
            } else {
                Log.d(TAG, "Profile extra not found");
            }
            startRecording(device, profile);
        }
    }

    private void startRecording(BluetoothDevice device, int profile) {
        if (isRecording) {
            Log.d(TAG, "Already recording");
            return;
        }
        Log.d(TAG, "startRecording");
        int bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized");
            return;
        }

        audioRecord.startRecording();
        isRecording = true;
        //TODO: process audio data
    }

    private void stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "Not recording");
            return;
        }
        Log.d(TAG, "stopRecording");
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            isRecording = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleIntent(getIntent());
            } else {
                Log.e(TAG, "Audio permission not granted");
                finish();
            }
        }
    }
}