package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PendingIntentScanReceiver extends BroadcastReceiver {
    private static final String TAG = "PendingIntentScanReceiver";

    static final String ACTION = "android.bluetooth.ACTION_FOUND";

    private Optional<CompletableFuture<ScanResult>> mNextScanResultFuture = Optional.empty();

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, PendingIntentScanReceiver.class);
        intent.setAction(PendingIntentScanReceiver.ACTION);
        return intent;
    }

    public static PendingIntent newBroadcastPendingIntent(Context context, int requestCode) {
        return PendingIntent.getBroadcast(
                context, requestCode, newIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public CompletableFuture<ScanResult> getNextScanResult() {
        if (mNextScanResultFuture.isPresent()) {
            mNextScanResultFuture.get().complete(null);
        }
        mNextScanResultFuture = Optional.of(new CompletableFuture<ScanResult>());
        return mNextScanResultFuture.get();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive intent: " + intent);

        int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0);
        if (errorCode != 0) {
            Log.e(TAG, "onReceive error: " + errorCode);
            return;
        }

        ArrayList<ScanResult> scanResults =
                intent.getParcelableExtra(
                        BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                        new ArrayList<ScanResult>().getClass());
        Log.i(TAG, "onReceive scanResults: " + scanResults);

        if (mNextScanResultFuture.isPresent() && !scanResults.isEmpty()) {
            mNextScanResultFuture.get().complete(scanResults.get(0));
            mNextScanResultFuture = Optional.empty();
        }
    }
}
