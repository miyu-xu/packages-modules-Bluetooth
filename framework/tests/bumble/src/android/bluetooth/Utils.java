package android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

import org.junit.Assert;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import pandora.HostGrpc;

public final class Utils {
    private static final String TAG = "Utils";

    public static String addressStringFromByteString(ByteString bs) {
        StringBuilder refAddrBuilder = new StringBuilder();
        for (int i = 0; i < bs.size(); i++) {
            if (i != 0) {
              refAddrBuilder.append(':');
            }
            refAddrBuilder.append(String.format("%02X", bs.byteAt(i)));
        }
        return refAddrBuilder.toString();
    }

    public static ManagedChannel factoryResetAndCreateNewChannel() throws InterruptedException {
        // FactoryReset is killing the server and restarting all channels created before the server
        // restarted that cannot be reused
        ManagedChannel channel = OkHttpChannelBuilder
                .forAddress("localhost", 7999)
                .usePlaintext()
                .build();

        HostGrpc.HostBlockingStub stub = HostGrpc.newBlockingStub(channel);
        stub.factoryReset(Empty.getDefaultInstance());

        // terminate the channel
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);

        // return new channel for future use
        return OkHttpChannelBuilder.forAddress("localhost", 7999).usePlaintext().build();
    }

    /**
     * Wait and verify that an item has been received.
     *
     * @param timeoutMs the time (in milliseconds) to wait for the item
     * @param queue the queue for the item
     * @return the received intent
     */
    public static <T> T waitForItem(int timeoutMs, BlockingQueue<T> queue) {
        try {
            return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Cannot obtain an item from the queue: " + e.getMessage());
        }
        return null;
    }

    /** Device based broadcast receiver */
    public static class DeviceBasedBroadcastReceiver extends BroadcastReceiver {
        private final HashMap<BluetoothDevice, LinkedBlockingQueue<Intent>> mDeviceQueueMap =
                new HashMap<>();
        private final LinkedBlockingQueue<Intent> mDefaultQueue = new LinkedBlockingQueue<>();

        /**
         * Add a device into the tracker
         *
         * @param device to be added
         */
        public void addDevice(BluetoothDevice device) {
            mDeviceQueueMap.put(device, new LinkedBlockingQueue<>());
        }

        /**
         * Get the blocking queue for the device
         *
         * @param device device must be added before
         * @return null if device wasn't added earlier, the blocking queue if device was added
         */
        public LinkedBlockingQueue<Intent> getQueue(BluetoothDevice device) {
            return mDeviceQueueMap.get(device);
        }

        /**
         * Get the default queue when no EXTRA_DEVICE is included
         *
         * @return the default queue
         */
        public LinkedBlockingQueue<Intent> getDefaultQueue() {
            return mDefaultQueue;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                BluetoothDevice device =
                        intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device == null) {
                    mDefaultQueue.put(intent);
                } else if (mDeviceQueueMap.containsKey(device)) {
                    LinkedBlockingQueue<Intent> queue = mDeviceQueueMap.get(device);
                    queue.put(intent);
                }
            } catch (InterruptedException e) {
                Assert.fail("Cannot add Intent to the queue: " + e.getMessage());
            }
        }
    }
}
