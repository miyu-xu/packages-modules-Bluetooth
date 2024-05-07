package android.bluetooth

import android.bluetooth.DckTestRule.LeScanResult
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.test.core.app.ApplicationProvider
import com.android.compatibility.common.util.AdoptShellPermissionsRule
import com.google.common.collect.Sets
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/** DCK Scan With UUID Tests */
@RunWith(Parameterized::class)
class DckScanWithUuidTest(
    isBluetoothToggled: Boolean,
    isGattConnected: Boolean,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Gives shell permissions during the test.
    @Rule(order = 0) @JvmField val shellPermissionRule = AdoptShellPermissionsRule()

    // Setup a Bumble Pandora device for the duration of the test.
    // Acting as a Pandora client, it can be interacted with through the Pandora APIs.
    @Rule(order = 1) @JvmField val bumble = PandoraDevice()

    // Test rule for common DCK test setup and teardown procedures, along with utility APIs.
    @Rule(order = 2)
    @JvmField
    val dck =
        DckTestRule(
            context,
            bumble,
            isBluetoothToggled = isBluetoothToggled,
            isRemoteAdvertisingWithUuid = true,
            isGattConnected = isGattConnected
        )

    @Test
    fun scanForUuid_remoteFound() {
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(CCC_DK_UUID)).build()
        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .build()

        val result: LeScanResult = runBlocking {
            withTimeout(TIMEOUT_MS) { dck.scanWithCallback(scanFilter, scanSettings).first() }
        }

        assertThat(result).isInstanceOf(LeScanResult.Success::class.java)
        assertThat((result as LeScanResult.Success).callbackType)
            .isEqualTo(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        assertThat((result as LeScanResult.Success).scanResult.device.address)
            .isEqualTo(Utils.BUMBLE_RANDOM_ADDRESS)
    }

    companion object {
        private const val TIMEOUT_MS = 3000L
        private val CCC_DK_UUID = UUID.fromString("0000FFF5-0000-1000-8000-00805f9b34fb")

        @Parameters(
            name =
                "{index}: isRemoteAdvertisingWithUuid = true, " +
                    "isBluetoothToggled = {0}, isGattConnected = {1}"
        )
        @JvmStatic
        fun parameters(): Iterable<Array<Any>> {
            val booleanVariations = setOf(true, false)

            return Sets.cartesianProduct(
                    listOf(
                        /* isBluetoothToggled */ booleanVariations,
                        /* isGattConnected */ booleanVariations
                    )
                )
                .map { it.toTypedArray() }
        }
    }
}
