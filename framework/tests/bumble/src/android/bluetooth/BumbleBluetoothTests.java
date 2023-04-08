package android.bluetooth;

import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;

public class BumbleBluetoothTests extends DeviceTestCase {

    /**
     * The command to launch the instrumentation tests.
     */
    private static final String START_COMMAND = String.format(
        "am instrument -w android.bluetooth/androidx.test.runner.AndroidJUnitRunner");

    /**
     * Start instrumentation test.
     *
     * @throws Exception
     */
    public void testStart() throws Exception {
        ITestDevice device = getDevice();
        // Clear logcat.
        device.executeAdbCommand("logcat", "-c");
        // Start the APK and wait for it to complete.
        device.executeShellCommand(START_COMMAND);
    }
}
