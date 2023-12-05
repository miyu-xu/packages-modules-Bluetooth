/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.bluetooth;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.SystemProperties;
import android.util.Log;

import com.android.bluetooth.flags.FeatureFlags;
import com.android.bluetooth.flags.FeatureFlagsImpl;

import java.util.Objects;

/**
 * A Logger for use by BT stack.
 *
 * This logger wraps the Android Logging Library, and provides logging functions
 * that can use and enforce the log level for the tag provided. This logger can also leverage
 * the stack wide "default log level" that's provided by the special "bluetooth" logging
 * tag.
 *
 * This logger also provides some utilities to get the values of logging tags,
 * particularly for dump purposes so it's easy to understand the configured level of
 * logging in a bug report.
 *
 * Expected usage:
 *  private static final String TAG = BluetoothThing.class.getSimpleName();
 *  private final BluetoothLogger mLogger = BluetoothLogger(TAG);
 *
 *  public void foo() {
 *      mLogger.a("something happened that literally shouldn't ever happen!");
 *      mLogger.e("something happened that's bad!");
 *      mLogger.w("something happened that's recoverable, but you should know!");
 *      mLogger.i("something happened!");
 *      mLogger.d("something happened that's useful in debugging!");
 *      mLogger.v("something happened that's useful in debugging, but spammy!");
 *  }
 *
 *  public void dump() {
 *      pw.print("Component Log Level: " + BluetoothLogger.logLevelToString(
 *                   BluetoothLogger.getTagLogLevel(TAG)));
 *      pw.print("Stack Default Log Level: " + BluetoothLogger.logLevelToString(
 *                   BluetoothLogger.getStackDefaultLogLevel()));
 *  }
 */
public class BluetoothLogger {
    private static final String BLUETOOTH_STACK_TAG = /* [persist.]log.tag. */ "bluetooth";
    private static final int DEFAULT_DEFAULT_LOG_LEVEL = Log.INFO;

    private static final String TAG_PREFIX = "log.tag.";
    private static final String PERSISTED_TAG_PREFIX = "persist.log.tag.";

    private static final String ANDROID_DEFAULT_LOG_TAG = "log.tag";
    private static final String PERSISTED_ANDROID_DEFAULT_LOG_TAG = "persist.log.tag";

    private static final String LOG_VERBOSE_STR = "VERBOSE";
    private static final String LOG_DEBUG_STR = "DEBUG";
    private static final String LOG_INFO_STR = "INFO";
    private static final String LOG_WARN_STR = "WARN";
    private static final String LOG_ERROR_STR = "ERROR";
    private static final String LOG_ASSERT_STR = "ASSERT";

    private static final int TAG_UNSET = -1;

    public static final int RESULT_UNLOGGABLE = 0;

    private static final FeatureFlags sFeatureFlags = new FeatureFlagsImpl();

    private final String mTag;

    // DEBUG and VERBOSE loggability is cached at logger creation time and enforced at log time
    // by default
    private final boolean mDbg;
    private final boolean mVdbg;

    public BluetoothLogger(String tag) {
        mTag = Objects.requireNonNull(tag);
        mDbg = isLoggable(mTag, Log.DEBUG);
        mVdbg = isLoggable(mTag, Log.VERBOSE);
    }

    /**
     * Determine if your tag should be able to log at a given level based on the tag's
     * log level and the Bluetooth stack's default log level.
     */
    private boolean isLoggable(String tag, int level) {
        if (sFeatureFlags.stackDefaultLogLevelFeature()) {
            return Log.isLoggable(tag, level) || Log.isLoggable(BLUETOOTH_STACK_TAG, level);
        }
        return Log.isLoggable(tag, level);
    }

    /**
     * Log a message at the VERBOSE level
     */
    public int v(@NonNull String msg) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.VERBOSE)) {
                return Log.v(mTag, msg);
            }
        } else if (mVdbg) {
            return Log.v(mTag, msg);
        }
        return RESULT_UNLOGGABLE;
    }

    /**
     * Log a message at the VERBOSE level
     */
    public int v(@Nullable String msg, @Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.VERBOSE)) {
                return Log.v(mTag, msg, tr);
            }
        } else if (mVdbg) {
            return Log.v(mTag, msg, tr);
        }
        return RESULT_UNLOGGABLE;
    }

    /**
     * Log a message at the DEBUG level
     */
    public int d(@NonNull String msg) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.DEBUG)) {
                return Log.d(mTag, msg);
            }
        } else if (mDbg) {
            return Log.d(mTag, msg);
        }
        return RESULT_UNLOGGABLE;

    }

    /**
     * Log a message at the DEBUG level
     */
    public int d(@Nullable String msg, @Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.DEBUG)) {
                return Log.d(mTag, msg, tr);
            }
        } else if (mDbg) {
            return Log.d(mTag, msg, tr);
        }
        return RESULT_UNLOGGABLE;

    }

    /**
     * Log a message at the INFO level
     */
    public int i(@NonNull String msg) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.INFO)) {
                return Log.i(mTag, msg);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.i(mTag, msg);
    }

    /**
     * Log a message at the INFO level
     */
    public int i(@Nullable String msg, @Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.INFO)) {
                return Log.i(mTag, msg, tr);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.i(mTag, msg, tr);
    }

    /**
     * Log a message at the WARN level
     */
    public int w(@NonNull String msg) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.WARN)) {
                return Log.w(mTag, msg);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.w(mTag, msg);
    }

    /**
     * Log a message at the WARN level
     */
    public int w(@Nullable String msg, @Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.WARN)) {
                return Log.w(mTag, msg, tr);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.w(mTag, msg, tr);
    }

    /**
     * Log a message at the WARN level
     */
    public int w(@Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.WARN)) {
                return Log.w(mTag, tr);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.w(mTag, tr);
    }

    /**
     * Log a message at the ERROR level
     */
    public int e(@NonNull String msg) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.ERROR)) {
                return Log.e(mTag, msg);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.e(mTag, msg);
    }

    /**
     * Log a message at the ERROR level
     */
    public int e(@Nullable String msg, @Nullable Throwable tr) {
        if (sFeatureFlags.runtimeEvaluatedLogLevelsFeature()) {
            if (isLoggable(mTag, Log.ERROR)) {
                return Log.e(mTag, msg, tr);
            }
            return RESULT_UNLOGGABLE;
        }
        return Log.e(mTag, msg, tr);
    }

    /**
     * Get a stack trace for where you are in code
     */
    public static String getStackTraceString(@Nullable Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    /**
     * Get the log level for your logger and it's assigned tag
     */
    public int getLogLevel() {
        return getTagLogLevel(mTag);
    }

    /**
     * Get the log level for your logger and it's assigned tag
     */
    public String getLogLevelString() {
        return getTagLogLevelString(mTag);
    }

    /**
     * Get the log level for a specific tag
     *
     * If no level is set, or the level set is invalid, INFO is returned
     */
    public static String getTagLogLevelString(String tag) {
        return logLevelToString(getTagLogLevel(tag));
    }

    /**
     * Get the log level for a specific tag
     *
     * If no level is set, or the level set is invalid, INFO is returned
     */
    public static int getTagLogLevel(String tag) {
        int noPrefixLevel = getTagLogLevelInternal(TAG_PREFIX + tag);
        if (noPrefixLevel != TAG_UNSET) return noPrefixLevel;
        int persistedLevel = getTagLogLevelInternal(PERSISTED_TAG_PREFIX + tag);
        if (persistedLevel != TAG_UNSET) return persistedLevel;
        int androidDefaultLevel = getTagLogLevelInternal(ANDROID_DEFAULT_LOG_TAG);
        if (androidDefaultLevel != TAG_UNSET) return androidDefaultLevel;
        int persistedAndroidDefaultLevel =
                getTagLogLevelInternal(PERSISTED_ANDROID_DEFAULT_LOG_TAG);
        if (persistedAndroidDefaultLevel != TAG_UNSET) return persistedAndroidDefaultLevel;
        return Log.INFO;
    }

    /**
     * Get the log level for a specific tag
     *
     * If no level is set, or the level set is invalid, TAG_UNSET is returned
     */
    private static int getTagLogLevelInternal(String fullyQualifiedTag) {
        String level = SystemProperties.get(fullyQualifiedTag);
        if (level == null || level.equals("")) {
            return TAG_UNSET;
        } else if (LOG_VERBOSE_STR.equals(level)) {
            return Log.VERBOSE;
        } else if (LOG_DEBUG_STR.equals(level)) {
            return Log.DEBUG;
        } else if (LOG_INFO_STR.equals(level)) {
            return Log.INFO;
        } else if (LOG_WARN_STR.equals(level)) {
            return Log.WARN;
        } else if (LOG_ERROR_STR.equals(level)) {
            return Log.ERROR;
        } else if (LOG_ASSERT_STR.equals(level)) {
            return Log.ASSERT;
        }
        return DEFAULT_DEFAULT_LOG_LEVEL;
    }

    /**
     * Get the stack's default log level
     *
     * If no level is set, or the level set is invalid, INFO is returned
     */
    public static int getStackDefaultLogLevel() {
        return getTagLogLevel(BLUETOOTH_STACK_TAG);
    }

    /**
     * Get a human readable version of a log level for dumpsys purposes
     */
    public static String logLevelToString(int level) {
        switch (level) {
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.DEBUG:
                return "DEBUG";
            case Log.INFO:
                return "INFO";
            case Log.WARN:
                return "WARN";
            case Log.ERROR:
                return "ERROR";
            case Log.ASSERT:
                return "ASSERT";
            default:
                return "Unknown (" + level + ")";
        }
    }
}
