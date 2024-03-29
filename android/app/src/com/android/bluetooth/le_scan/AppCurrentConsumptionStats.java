/*
 * Copyright (C) 2012 The Android Open Source Project
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
//SS_BLE_FEATURE_P43 START
package com.samsung.ble;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.view.Display;
import android.hardware.display.DisplayManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.HashSet;

import android.os.SystemClock;
import android.util.Log;
import android.content.Context;

import android.content.BroadcastReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.bluetooth.Utils;
import com.android.bluetooth.R;
import com.android.bluetooth.gatt.ScanManager;
import com.android.bluetooth.mapclient.obex.Message;
import com.android.bluetooth.gatt.GattServiceConfig;


public class AppCurrentConsumptionStats {

    static final String TAG = GattServiceConfig.TAG_PREFIX + "AppCurrentConsumptionStats";

    private boolean requireAlarmCancel;

    static boolean sIsScreenOn = false;

    static final int MSG_ADD_SCR_OFF_RESULT = 0;
    static final int MSG_SCREEN_ON = 1;
    static final int MSG_UPDATE_EXCEPT_LIST = 2;

    public static final String CURRENT_CONSUMPTION_NONE = "None";
    public static final String CURRENT_CONSUMPTION_LOW = "Low";
    public static final String CURRENT_CONSUMPTION_MID = "Mid";
    public static final String CURRENT_CONSUMPTION_HIGH = "High";

    public static final int SCAN_1_HOUR = 1;
    public static final int SCAN_4_HOUR = 4;
    public static final int SCAN_8_HOUR = 8;

    public static final int LOW = 0;
    public static final int MID = 1;
    public static final int HIGH = 2;

    public static final int[][] degree_threshold = {
            /*                      LOW    MID     HIGH*/
            /*for >=1 to <4hr*/    {480,   640,    800},
            /*for >=4 to <8hr */   {320,   480,    640},
            /*for >=8 hr*/         {160,   320,    480}
    };


    private static Context mContext;

    // Intent Action of alarm, that is used to check the severity every hour on the hour
    private static final String ALARM_TRIGGER_ACTION = "com.samsung.android.bluetooth.action.SCAN_RESULT_UPDATE";
    static final int NUM_LAST_HOURS_KEPT = 8;

    private AlarmManager mAlarm;
    private PendingIntent mAlarmPendingIntent;
    private Map<String, ScanResultStats> mHashMap;
    private ConsumptionHandler mHandler;


    notificationCompat notificationCompat;
    
    // notification flag
    boolean isNotiRegistered;
    boolean isNotiSent;

    Set <String> mExceptList = new HashSet<>();


    public AppCurrentConsumptionStats(Context context) {
        HandlerThread thread = new HandlerThread("AppCurrentConsumption");
        thread.start();
        mHandler = ConsumptionHandler(thread.getLooper());
        mHashMap = new HashMap<>();
        mContext = context;

        IntentFilter filterWithPermission = new IntentFilter();
        filterWithPermission.addAction(ALARM_TRIGGER_ACTION);
        mContext.registerReceiver(mReceiverWithPermission, filterWithPermission,
                Utils.PERMISSION_BLUETOOTH_PRIVILEGED, null, Context.RECEIVER_NOT_EXPORTED);
        
    }

    public void cleanup(){
        Log.i(TAG, "unregister receiver");
        if (mReceiverWithPermission != null) {
            mContext.unregisterReceiver(mReceiverWithPermission); // SS_BLE_FEATURE_P58
        }

        cancelAlarm();
        
        if (mHandler != null) {
            // Shut down the thread
            ConsumptionHandler handler = mHandler;
            mHandler = null;
            handler.removeCallbacksAndMessages(null);
            Looper looper = handler.getLooper();
            if (looper != null) {
                looper.quitSafely();
            }
        }
    }

    public String getCurrentScanStats() {
        updateSeverity();

        String str = null;
        JSONArray jsonArr= new JSONArray();
        for (ScanResultStats entry : mHashMap.values()) {
            jsonArr.put(entry.toString());
        }

        str = jsonArr.toString();
        Log.d(TAG, "JSON:" + str);
        return str;
    }

    public void addScrOffResult(String appName) {
        // Filter out System or OEM services
        if(mExceptList.contains(appName)) {
            Log.d(TAG, appName + "is in the except list");
            return;
        }
        final ConsumptionHandler handler = mHandler;
        if (handler == null) {
            Log.d(TAG, "addScrOffResult(): mHandler is null.");
            return;
        }
        Message message = new Message();
        message.what = MSG_ADD_SCR_OFF_RESULT;
        message.obj = appName;
        handler.sendMessage(message);
    }

    public void sendScreenOnMsg() {
        final ConsumptionHandler handler = mHandler;
        if (handler == null) {
            Log.d(TAG, "sendScreenOnMsg(): mHandler is null.");
            return;
        }
        Message message = new Message();
        message.what = MSG_SCREEN_ON;
        handler.sendMessage(message);
    }

    public void updateCurrentConsumptionExceptList(ArrayList<String> consumptionExceptList) {
        ConsumptionHandler handler = mHandler;
        if (handler == null) {
            Log.d(TAG, "updateAppScanBlacklist(): mHandler is null.");
            return;
        }
        Message message = new Message();
        message.what = MSG_UPDATE_EXCEPT_LIST;
        message.obj = consumptionExceptList;
        handler.sendMessage(message);
    }

    public setScreenState(boolean isScreenOn) {
        sIsScreenOn = isScreenOn;
    }

    private class ConsumptionHandler extends Handler {
        
        ConsumptionHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_ADD_SCR_OFF_RESULT:
                    handleAddScrOffResult((String)msg.obj);
                    break;
                case MSG_SCREEN_ON:
                    handleScreenOn();
                    break;
                case MSG_UPDATE_EXCEPT_LIST:
                    handleUpdateExceptList((ArrayList<String>) msg.obj);
                    break;
                default:
                    // Shouldn't happen.
                    Log.e(TAG, "received an unkown message : " + msg.what);
            }
        }

        private void handleScreenOn() {
            Log.d(TAG, "handleScreenOn()");
            if (updateSeverity()) {
                //If there are high severity apps and a notification flag is ture, make notification
                if (isNotiRegistered) {
                    Log.d(TAG, "high severity app present, send notification.");
                    // code to make notification
                }
            } else if (isNotiSent){
                // If there is no high serverity app and notifiation is already made, remove notification
                isNotiRegistered = false;
                Log.d(TAG, "no high severity app present, remove notification.");
                // code to remove notification
                isNotiSent = false;
            }
        }

        private void handleAddScrOffResult(String appName) {
            // Set Alarm to check serverity every hour on the hour 
            setAlarm();
    
            ScanResultStats scanAppRecord = getRecordFromPkgName(appName);
            if (scanAppRecord == null) {
                String pkg = appName;
                scanAppRecord = new ScanResultStats(pkg);
                mHashMap.put(pkg, scanAppRecord);
                Log.d(TAG, "new record(" + pkg + ") created with th as:" + scanAppRecord.getThreshold());
            }
            scanAppRecord.addScrOffResult();
    
            if (scanAppRecord.getScrOffResultCount() >= scanAppRecord.getThreshold()) {
                if(scanAppRecord.getSeverity() != CURRENT_CONSUMPTION_HIGH) {
                    Log.d(TAG, "SR count:" +scanAppRecord.getScrOffResultCount() + "Threshold" +scanAppRecord.getThreshold());
                    Log.d(TAG, "Level of current consumption reached high for!!" +scanAppRecord.getpkgName());
                    scanAppRecord.setSeverity(CURRENT_CONSUMPTION_HIGH);
                }
                
                if(!isNotiRegistered)
                    //If the severity is high and notificationCompat object is not maded, make notificationCompat object and notify.
                    makeNotiAndNotify(appName);
            }
        }

        private void handleUpdateExceptList(ArrayList<String> consumptionExceptList) {
            Log.d(TAG, "updateAppScanBlacklist") ;
            if(consumptionExceptList.size() > 0) {
                mExceptList.clear();
                for(String app : consumptionExceptList) {
                    mExceptList.add(app);
                    Log.d(TAG, "updateAppScanBlacklist :: blacklist app: " + app) ;
                }
            }
        }

        private ScanResultStats getRecordFromPkgName(String pkg) {
            return mHashMap.get(pkg);
        }
    }

    private int getCurrentHour(ScanResultStats sRecord){
        int hoursScanned = sRecord.getArraySize();
        int index = 0;
        while (index < sRecord.getArraySize()
                && sRecord.mHourlyScanResultCntArray.get(index) == 0) {
            hoursScanned--;
            index++;
        }

        int currentHour = Math.min(hoursScanned +1 , NUM_LAST_HOURS_KEPT);
        Log.d(TAG, "getCurrentHour:" + currentHour);
        return currentHour;
    }

    private int getSumScanResultCnt(ScanResultStats sRecord){
        int sum = 0;
        int index = 0;
        while(index < sRecord.getArraySize()){
            sum += sRecord.mHourlyScanResultCntArray.get(index);
            index++;
        }
        Log.d(TAG, "getSumScanResultCnt:" + sum);
        return sum;
    }

    private int getCurrentThreshold(ScanResultStats sRecord){
        int sumScanResCnt = getSumScanResultCnt(sRecord);
        int currentHour = getCurrentHour(sRecord);

        int currentHourTh;
        if (currentHour <= SCAN_4_HOUR)
            currentHourTh = (degree_threshold[1][HIGH] * currentHour) - sumScanResCnt;
        else
            currentHourTh = (degree_threshold[2][HIGH] * currentHour) - sumScanResCnt;

        Log.d(TAG, "getCurrentThreshold:: threshold: " + currentHourTh);

        return currentHourTh;
    }

    private Calendar nextHourofDay(){
        Calendar now = Calendar.getInstance();
        int nxthour = now.get(Calendar.HOUR_OF_DAY) + 1;
        now.set(Calendar.HOUR_OF_DAY, nxthour);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);

        Log.d(TAG, "First alarm will trigger at" + nxthour);
        return now;
    }

    private void setAlarm() {
        if (mAlarmPendingIntent == null) {
            Intent pendingIntent = new Intent(ALARM_TRIGGER_ACTION)
                    .setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                    .setPackage(Utils.PACKAGE_NAME);

            mAlarmPendingIntent =
                    PendingIntent.getBroadcast(mContext, 0, pendingIntent, PendingIntent.FLAG_IMMUTABLE);

            mAlarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            long firstTime = nextHourofDay().getTimeInMillis();
            mAlarm.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, AlarmManager.INTERVAL_HOUR,
                    mAlarmPendingIntent);
        }
    }

    private void cancelAlarm(){
        Log.i(TAG,"Alarm cancelled");
        if (mAlarmPendingIntent != null) {
            mAlarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            mAlarm.cancel(mAlarmPendingIntent);
            mAlarmPendingIntent = null;
        }
    }

    private boolean updateSeverity() {
        boolean isHighSevAppPresent = false;
        String mSeverity = CURRENT_CONSUMPTION_NONE;

        Iterator<Map.Entry<String, ScanResultStats>> mapIt = mHashMap.entrySet().iterator();
        while (mapIt.hasNext()) {
            Map.Entry<String, ScanResultStats> entry = mapIt.next();

            ScanResultStats sRecord = entry.getValue();
            Log.d(TAG, "updateSeverity::For scanner:" + sRecord.getpkgName());

            Log.d(TAG, "result cnt in middle of hour:" + sRecord.getScrOffResultCount());
            int currScanResCnt = getSumScanResultCnt(sRecord) + sRecord.getScrOffResultCount();
            if (sRecord.getArraySize() == NUM_LAST_HOURS_KEPT)
                currScanResCnt =  currScanResCnt - sRecord.mHourlyScanResultCntArray.get(0);
            int currentHour = getCurrentHour(sRecord);

            int average = currScanResCnt/currentHour;
            Log.d(TAG, "updateSeverity:: average: " +average);

            int index;
            if (currentHour > SCAN_4_HOUR)      index = 2;
            else if (currentHour > SCAN_1_HOUR) index = 1;
            else index = 0;

            Log.d(TAG, "updateSeverity:: index: " +index);

            if (average >= degree_threshold[index][HIGH]) { mSeverity = CURRENT_CONSUMPTION_HIGH; isHighSevAppPresent = true;} 
            else if (average >= degree_threshold[index][MID]) mSeverity = CURRENT_CONSUMPTION_MID;
            else if (average >= degree_threshold[index][LOW]) mSeverity = CURRENT_CONSUMPTION_LOW;

            sRecord.setSeverity(mSeverity);
            Log.d(TAG, "updateSeverity:: set: " +sRecord.getSeverity());
        }
        return isHighSevAppPresent;
    }

    //Reciever to handle the hour alarm.
    BroadcastReceiver mReceiverWithPermission = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceiveWithPermission : " + action);
            if (action.equals(ALARM_TRIGGER_ACTION)) {

                Log.d(TAG, "ALARM TRIGGERED!!");

                boolean requireAlarmCancel = true;
                Iterator<Map.Entry<String, ScanResultStats>> mapIt = mHashMap.entrySet().iterator();
                while (mapIt.hasNext()) {
                    Map.Entry<String, ScanResultStats> entry = mapIt.next();

                    ScanResultStats sResultRecord = entry.getValue();
                    String appName = sResultRecord.getpkgName();
                    Log.d(TAG, "For scanner:" + appName);

                    if (sResultRecord.getArraySize() >= NUM_LAST_HOURS_KEPT) {
                        sResultRecord.removeFirstRecord();
                    }
                    sResultRecord.addNewRecord();
                    Log.d(TAG, "result cnt in end of hour:" + sResultRecord.getScrOffResultCount());
                    sResultRecord.resetCount();
                    sResultRecord.setThreshold(getCurrentThreshold(sResultRecord));
                    sResultRecord.printAppRecords();

                    if (sResultRecord.getThreshold() <= 0) {
                        Log.d(TAG, "Level of current consumption reached high for!!" + appName);
                        sResultRecord.setSeverity(CURRENT_CONSUMPTION_HIGH);
                        if(!isNotiRegistered)
                            makeNotiAndNotify(appName);
                    }

                    if (getSumScanResultCnt(sResultRecord)==0) {
                        Log.d(TAG, "Remove Scan Record for pkg:" + appName);
                        mapIt.remove();
                    }

                    if (getSumScanResultCnt(sResultRecord)>0) {
                        requireAlarmCancel = false;
                    }
                }

                if (requireAlarmCancel) {
                    cancelAlarm();
                }
            }
        }
    };



    private class ScanResultStats {
        public String pkgName;
        private int scrOffResultCount;
        private String severity;
        private List<Integer> mHourlyScanResultCntArray;
        private int hThreshold;

        ScanResultStats(String pkg) {
            this.pkgName = pkg;
            this.scrOffResultCount = 0;
            this.severity = AppCurrentConsumptionStats.CURRENT_CONSUMPTION_NONE;
            this.hThreshold = degree_threshold[0][2];
            mHourlyScanResultCntArray = new ArrayList<>(AppCurrentConsumptionStats.NUM_LAST_HOURS_KEPT);
        }

        public String getpkgName(){
            return pkgName;
        }

        public int getArraySize(){
            return mHourlyScanResultCntArray.size();
        }

        public void removeFirstRecord(){
            mHourlyScanResultCntArray.remove(0);
        }

        public void addNewRecord(){
            mHourlyScanResultCntArray.add(scrOffResultCount);
        }

        public void resetCount(){
            scrOffResultCount = 0;
        }

        public int getScrOffResultCount() {
            return scrOffResultCount;
        }

        public void addScrOffResult() {
            scrOffResultCount++;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public void setThreshold(int th){
            hThreshold = th;
        }

        public int getThreshold(){
            return hThreshold;
        }

        public void printAppRecords(){
            Log.d(TAG, "app record:" + mHourlyScanResultCntArray);
        }

        @Override
        public String toString() {
            return this.pkgName+":"+this.severity;
        }
    }

    private boolean isScreenOn() {
        return sIsScreenOn;
    }
    
    private void notifyCurrentConsumption(){
        Log.d(TAG, "notifiy ScrOn:" + isScreenOn());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //codes to notify using already maded notificationCompat object.
            }
        });
        t.start();
        isNotiRegistered = false;
        isNotiSent = true;
    }

    // When the device is screen off, just make notificationCompat object. and notify it when screen on
    private void makeNotiAndNotify(String appName) {
        if(mExceptList.contains(appName)) {
            Log.d(TAG, "makeNotiAndNotify: except for " + appName);
            return;
        } else {
            Log.d(TAG, "makeNotiAndNotify: notification for " + appName);
        }

        //codes to make notification object.


        isNotiRegistered = true;
        if (isScreenOn())
            notifyCurrentConsumption();
        else
            Log.d(TAG, "notification registered but not notify");
    }
}