/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package pl.codecoup.ehima.leaudio;

import android.bluetooth.BluetoothBroadcastAudioScan;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class BroadcastScanActivity extends AppCompatActivity {
    private static final int BIS_ALL = 0xFFFFFFFF;

    private BluetoothDevice device;
    private Integer receiver_id;
    private BroadcastScanViewModel mViewModel;
    private BroadcastItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broadcast_scan_activity);

        RecyclerView recyclerView = findViewById(R.id.broadcast_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new BroadcastItemsAdapter();
        adapter.setOnItemClickListener(broadcast -> {
            mViewModel.scanForBroadcasts(device, false);

            // TODO: Support selecting the subgroups instead of using all
            if (broadcast.isLocal()) {
                if ((broadcast.getState() != BluetoothLeAudio.BROADCAST_STATE_STOPPED)) {
                    List configs = new ArrayList<>();
                    configs.add(broadcast.config);
                    mViewModel.setLocalBroadcastSource(device, broadcast.local_instance_id, true,
                            configs);

                    Toast.makeText(recyclerView.getContext(), "Set local broadcast",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String message = "wrong broadcasts state";
                    Intent result_intent = new Intent();
                    result_intent.putExtra("MESSAGE", message);
                    setResult(RESULT_CANCELED, result_intent);
                    finish();
                    return;
                }
            } else {
                mViewModel.addBroadcastSource(device, broadcast.scan_result.getBroadcastId(), true,
                        broadcast.scan_result.getSubgroupConfigs());
                Toast.makeText(recyclerView.getContext(), "Add remote broadcast",
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        mViewModel = ViewModelProviders.of(this).get(BroadcastScanViewModel.class);
        mViewModel.getAllBroadcasts().observe(this, audioBroadcasts -> {
            // Update Broadcast list in the adapter
            adapter.setBroadcasts(audioBroadcasts);
        });

        Intent intent = getIntent();
        receiver_id = intent.getIntExtra(BluetoothBroadcastAudioScan.EXTRA_BASS_RECEIVER_ID, 0);
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mViewModel.scanForBroadcasts(device, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mViewModel.getAllBroadcasts().getValue() != null)
            adapter.setBroadcasts(mViewModel.getAllBroadcasts().getValue());

        mViewModel.scanForBroadcasts(device, true);
        mViewModel.getAllLocalBroadcasts();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
}
