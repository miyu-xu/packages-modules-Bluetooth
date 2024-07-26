/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.bluetooth;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.view.KeyEvent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.protobuf.Empty;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class AvrcpTest {
    private static final String TAG = "AvrcpTest";

    private static final Duration CONNECT_INTENT_TIMEOUT = Duration.ofSeconds(10);

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothAdapter mAdapter =
            mContext.getSystemService(BluetoothManager.class).getAdapter();

    private final Host mHost;

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    private BluetoothDevice mBumbleBluetoothDevice;

    @Mock private BroadcastReceiver mReceiver;

    @Before
    public void setup() throws Exception {
        mHost = new Host(mContext);
        mBumbleBluetoothDevice = mBumble.getRemoteDevice();
        mReceiver = mock(BroadcastReceiver.class);

        if (!AvrcpMediaPlayer.isInitialized()) {
            mContext.startService(new Intent(mContext, AvrcpMediaPlayer.class));
        }
        // Connect Bumble BR/EDR, verify Bumble is connected
        mHost.createBondAndVerify(mBumbleBluetoothDevice);
        mContext.registerReceiver(
                mReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        verifyIntentReceived(
                hasAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleBluetoothDevice),
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTING));
        verifyIntentReceived(
                hasAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED),
                hasExtra(BluetoothDevice.EXTRA_DEVICE, mBumbleBluetoothDevice),
                hasExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED));
        mContext.unregisterReceiver(mReceiver);
    }

    @After
    public void tearDown() throws Exception {
        BluetoothDevice device = mBumble.getRemoteDevice();
        if (mAdapter.getBondedDevices().contains(device)) {
            mHost.removeBondAndVerify(device);
        }
        mHost.close();
    }

    @Test
    public void verifyNoNowPlayingListUpdateWhenTrackChanges() throws Exception {

        // Setup player, create playlist, play
        assertThat(AvrcpMediaPlayer.isInitialized()).isTrue();

        AvrcpMediaPlayer mediaplayer = AvrcpMediaPlayer.getInstance();

        ArrayList<MediaMetadata> queue = new ArrayList<>();
        queue.add(AvrcpMediaPlayer.createDefaultMediaMetadata());
        queue.add(AvrcpMediaPlayer.createDefaultMediaMetadata());
        queue.add(AvrcpMediaPlayer.createDefaultMediaMetadata());

        AtomicBoolean nextKeyEventreceived = new AtomicBoolean();
        mediaplayer.checkEventReceived(
                new AvrcpMediaPlayer.EventsReceivedCallback() {
                    public void onEventreceived(long event) {
                        if (KeyEvent.KEYCODE_MEDIA_NEXT == event) {
                            nextKeyEventreceived.set(true);
                        }
                    }
                });
        mediaplayer.createQueue(queue);
        mediaplayer.play();

        // Monitor now playing content changed
        Thread playingContentMonitor =
                new Thread(
                        new Runnable() {
                            public void run() {
                                // Will not return if no event has been received (except Interim).
                                mBumble.avrcpBlocking()
                                        .monitorNowPlayingContent(Empty.getDefaultInstance());
                            }
                        });
        playingContentMonitor.start();

        // TODO(b/356060177): Wait for interim
        Thread.sleep(1000);
        // Send Next key event
        mBumble.avrcpBlocking().sendKeyEventNext(Empty.getDefaultInstance());

        // Wait a bit before checking as we are expected *not* to receive the event
        Thread.sleep(2000);
        // Verify "next" key event has been sent
        assertThat(nextKeyEventreceived.get()).isTrue();
        // Verify no now playing content changed has been sent
        assertThat(playingContentMonitor.isAlive()).isTrue();
    }

    @SafeVarargs
    private void verifyIntentReceived(Matcher<Intent>... matchers) {
        verify(mReceiver, timeout(CONNECT_INTENT_TIMEOUT.toMillis()))
                .onReceive(any(Context.class), MockitoHamcrest.argThat(AllOf.allOf(matchers)));
    }
}
