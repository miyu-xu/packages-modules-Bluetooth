package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * PendingIntentScanReceiver is registered statically in the manifest file as a BroadcastReceiver for the
 * android.bluetooth.ACTION_SCAN_RESULT action. Tests can use createNextScanResultFutureStatic() to get
 * a future that completes when scan results are next delivered. Alternatively, PendingIntentScanReceiver
 * supports being a dynamically registered BroadcastReceiver (using context.registerReceiver()), in
 * which case the createNextScanResultFuture() method should be used to get scan results.
 */
public class PendingIntentScanReceiver extends BroadcastReceiver {
    private static final String TAG = "PendingIntentScanReceiver";

    public static final String ACTION_SCAN_RESULT = "android.bluetooth.ACTION_SCAN_RESULT";

    private Optional<CompletableFuture<List<ScanResult>>> mNextScanResultFuture = Optional.empty();
    private static Optional<CompletableFuture<List<ScanResult>>> mNextScanResultFutureStatic = Optional.empty();

    public static Intent newIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction(PendingIntentScanReceiver.ACTION_SCAN_RESULT);
        intent.setClass(context, PendingIntentScanReceiver.class);
        return intent;
    }

    public static PendingIntent newBroadcastPendingIntent(Context context, int requestCode) {
        return PendingIntent.getBroadcast(
                context, requestCode, newIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public CompletableFuture<List<ScanResult>> createNextScanResultFuture() {
        if (mNextScanResultFuture.isPresent()) {
            mNextScanResultFuture.get().cancel(false);
        }
        mNextScanResultFuture = Optional.of(new CompletableFuture<List<ScanResult>>());
        return mNextScanResultFuture.get();
    }

    public static CompletableFuture<List<ScanResult>> createNextScanResultFutureStatic() {
        if (mNextScanResultFutureStatic.isPresent()) {
            mNextScanResultFutureStatic.get().cancel(false);
        }
        mNextScanResultFutureStatic = Optional.of(new CompletableFuture<List<ScanResult>>());
        return mNextScanResultFutureStatic.get();
    }

    public static void resetNextStaticScanResultFuture() {
        mNextScanResultFutureStatic = Optional.empty();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive() intent: " + intent);

        if (intent.getAction() != ACTION_SCAN_RESULT) {
            return;
        }

        int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0);
        if (errorCode != 0) {
            Log.e(TAG, "onReceive() error: " + errorCode);
            return;
        }

        List<ScanResult> scanResults =
                intent.getParcelableExtra(
                        BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                        new ArrayList<ScanResult>().getClass());
        Log.i(TAG, "onReceive scanResults: " + scanResults);

        if (mNextScanResultFuture.isPresent()) {
            Log.i(TAG, "onReceive() completing mNextScanResultFuture");
            mNextScanResultFuture.get().complete(scanResults);
            mNextScanResultFuture = Optional.empty();
        }

        if (mNextScanResultFutureStatic.isPresent()) {
            Log.i(TAG, "onReceive() completing mNextStaticScanResultFuture");
            mNextScanResultFutureStatic.get().complete(scanResults);
            mNextScanResultFutureStatic = Optional.empty();
        }
    }
}
