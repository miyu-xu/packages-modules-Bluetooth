package com.example.bttestapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.bttestapp.ui.theme.BttestappTheme

class MainActivity : ComponentActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val MY_MAC_ADDRESS = "FC:91:5D:64:FE:5F"
    private val TAG = "WENDEE TEST"
    private lateinit var context : Context
    private var pairedDevice : BluetoothDevice? = null

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 30000
    private var scanning = false
    private var isGattConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BttestappTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("bt testing app")
                }
            }
        }

        requestBTPermission(this@MainActivity)
        checkAndEnableBluetooth()
    }

    private fun isPermissionGranted(permissionToCheck: String): Boolean {
        return ActivityCompat.checkSelfPermission(context, permissionToCheck) ==
            PackageManager.PERMISSION_GRANTED
    }

    /* Request all Bluetooth permissions when the activity starts. */
    fun requestBTPermission(_context: Context) {
        context = _context
        val bluetoothManager = ActivityCompat.getSystemService(context, BluetoothManager::class.java) ?: return
        bluetoothAdapter = bluetoothManager.adapter ?: return

        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_ADVERTISE) ||
            !isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            requestBluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            Log.d(TAG, "all granted")
        }
    }

    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            permissions.forEach { p ->
                if (p.value == false) {
                    Log.d(TAG, "$p is not permitted")
                }
            }
        }

    fun checkAndEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            startBluetoothIntentForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            // start scanning
            Log.d(TAG, "checkAndEnableBluetooth")
            scanDevice()
        }
    }

    private val startBluetoothIntentForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode != Activity.RESULT_OK) {
                // Uncomment the following line to force turn on
                // checkAndEnableBluetooth()
            } else {
                // start scanning
                Log.d(TAG, "startBluetoothIntentForResult")
                scanDevice()
            }
        }

    @SuppressLint("MissingPermission")
    private fun scanDevice() {
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        context.registerReceiver(broadcastReceiver, filter)

        val devices = bluetoothAdapter?.bondedDevices
        if (devices != null && devices.size > 0) {
            Log.d(TAG, "devices -> $devices")

            for (device in devices) {
                Log.d(TAG, "bonded -> ${device.address}")
                if (device.address == MY_MAC_ADDRESS) {
                    Log.d(TAG, "before connect!!!!")
                    device.connect()
                    Log.d(TAG, "paired ~~~~~~~~~~~~~")
                    return
                }
            }

            return
        }

    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        // onReceive called at ACTION_BOND_STATE_CHANGED
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.d(TAG, "intent -> $intent ,action -> $action")
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
            val previousState = intent.getParcelableExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice::class.java)
            val deviceAddress = device.address
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            Log.d(TAG, "address -> $deviceAddress")
            Log.d(TAG, "bondState -> ${device.bondState}, previous -> $previousState")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BttestappTheme {
        Greeting("bt testing app")
    }
}
