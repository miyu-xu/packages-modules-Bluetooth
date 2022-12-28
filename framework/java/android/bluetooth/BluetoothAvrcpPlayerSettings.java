/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used to identify settings associated with the player.
 * See {@link BluetoothA2dp#registerPlayerSettingsCallback} and
 * {@link BluetoothA2dp#updatePlayerSettings}.
 */
public final class BluetoothAvrcpPlayerSettings implements Parcelable {
    public static final String TAG = "BluetoothAvrcpPlayerSettings";

    /**
     * Repeat setting, as defined in Bluetooth.
     */
    public static final int SETTING_REPEAT = 2;

    /**
     * Shuffle setting, as defined in Bluetooth.
     */
    public static final int SETTING_SHUFFLE = 3;

    /** @hide */
    @IntDef({SETTING_REPEAT, SETTING_SHUFFLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSetting {}

    /**
     * Repeat OFF state, as defined in Bluetooth.
     */
    public static final int STATE_REPEAT_OFF = 1;

    /**
     * Single track repeat, as defined in Bluetooth.
     */
    public static final int STATE_REPEAT_SINGLE_TRACK = 2;

    /**
     * All track repeat, as defined in Bluetooth.
     */
    public static final int STATE_REPEAT_ALL_TRACK = 3;

    /**
     * Group repeat, as defined in Bluetooth.
     */
    public static final int STATE_REPEAT_GROUP = 4;

    /** @hide */
    @IntDef({
            STATE_REPEAT_OFF,
            STATE_REPEAT_SINGLE_TRACK,
            STATE_REPEAT_ALL_TRACK,
            STATE_REPEAT_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSettingRepeatValue {}


    /**
     * Shuffle OFF state, as defined in Bluetooth.
     */
    public static final int STATE_SHUFFLE_OFF = 1;

    /**
     * All track shuffle, as defined in Bluetooth.
     */
    public static final int STATE_SHUFFLE_ALL_TRACK = 2;

    /**
     * Group shuffle, as defined in Bluetooth.
     */
    public static final int STATE_SHUFFLE_GROUP = 3;

    /** @hide */
    @IntDef({STATE_SHUFFLE_OFF, STATE_SHUFFLE_ALL_TRACK, STATE_SHUFFLE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerSettingShuffleValue {}


    private Map<Integer, Integer> mSettingsValue = new HashMap<Integer, Integer>();
    private Map<Integer, String> mSettingsText = new HashMap<Integer, String>();
    private Map<Integer, String> mValuesText = new HashMap<Integer, String>();

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flattens the object to a parcel
     *
     * @param out The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     *
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mSettingsValue.size());
        for (Map.Entry<Integer, String> entryValue : mSettingsValue.entrySet()) {
            out.writeInt(entryValue.getKey());
            out.writeString(entryValue.getValue());
        }
        out.writeInt(mSettingsText.size());
        for (Map.Entry<Integer, String> entrySettingText : mSettingsText.entrySet()) {
            out.writeInt(entrySettingText.getKey());
            out.writeString(entrySettingText.getValue());
        }
        out.writeInt(mValuesText.size());
        for (Map.Entry<Integer, String> entryValueText : mValuesText.entrySet()) {
            out.writeInt(entryValueText.getKey());
            out.writeString(entryValueText.getValue());
        }
    }

    public static final @NonNull Creator<BluetoothAvrcpPlayerSettings> CREATOR =
            new Creator<>() {
        public BluetoothAvrcpPlayerSettings createFromParcel(Parcel in) {
            return new BluetoothAvrcpPlayerSettings(in);
        }

        public BluetoothAvrcpPlayerSettings[] newArray(int size) {
            return new BluetoothAvrcpPlayerSettings[size];
        }
    };

    private BluetoothAvrcpPlayerSettings(Parcel in) {
        int numSettingsValues = in.readInt();
        for (int i = 0; i < numSettingsValues; i++) {
            mSettingsValue.put(in.readInt(), in.readInt());
        }
        int numSettingsText = in.readInt();
        for (int j = 0; j < numSettingsText; j++) {
            mSettingsText.put(in.readInt(), in.readString());
        }
        int numValuesText = in.readInt();
        for (int j = 0; j < numValuesText; j++) {
            mValuesText.put(in.readInt(), in.readString());
        }
    }

    private BluetoothAvrcpPlayerSettings(@NonNull Map<Integer, Integer> settingsValue,
            Map<Integer, String> settingsText, Map<Integer, String> valuesText) {
        mSettingsValue = settingsValue;
        mSettingsText = settingsText;
        mValuesText = valuesText;
    }

    /**
     * @return true if the settings are not empty
     */
    public boolean isValid() {
        return !mSettingsValue.isEmpty();
    }

    /**
     * Ensures that the setting is valid.
     *
     * @return true if the setting is valid, false otherwise
     */
    public static boolean isValidPlayerSetting(@PlayerSetting int setting) {
        if (setting > SETTING_SHUFFLE || setting < SETTING_REPEAT) {
            return false;
        }
        return true;
    }

    /**
     * Ensures that the setting and value given are valid.
     *
     * @return true if the setting is valid, false otherwise
     */
    public static boolean isValidPlayerSettingValue(@PlayerSetting int setting, int value) {
        switch (setting) {
            case SETTING_SHUFFLE:
                if (value == STATE_SHUFFLE_OFF
                        || value == STATE_SHUFFLE_ALL_TRACK
                        || value == STATE_SHUFFLE_GROUP) {
                    return true;
                }
                return false;
            case SETTING_REPEAT:
                if (value == STATE_REPEAT_OFF
                        || value == STATE_REPEAT_ALL_TRACK
                        || value == STATE_REPEAT_GROUP
                        || value == STATE_REPEAT_SINGLE_TRACK) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Ensures that the value given exists.
     *
     * @return true if the value exists, false otherwise
     */
    public static boolean isValidPlayerValue(int value) {
        switch (value) {
            case STATE_SHUFFLE_OFF:
            case STATE_SHUFFLE_ALL_TRACK:
            case STATE_SHUFFLE_GROUP:
            case STATE_REPEAT_OFF:
            case STATE_REPEAT_ALL_TRACK:
            case STATE_REPEAT_GROUP:
            case STATE_REPEAT_SINGLE_TRACK:
                return true;
            default:
                return false;
        }
    }

    /**
     * Retrieves the list of possible values for a setting.
     *
     * @param setting the setting values should be retrieved for
     * @return a list of values corresponding to the provided setting
     */
    public static List<Integer> getSettingPossibleValues(@PlayerSetting int setting) {
        switch (setting) {
            case SETTING_SHUFFLE:
                return Arrays.asList(STATE_SHUFFLE_OFF,
                        STATE_SHUFFLE_ALL_TRACK,
                        STATE_SHUFFLE_GROUP);
            case SETTING_REPEAT:
                return Arrays.asList(STATE_REPEAT_OFF,
                        STATE_REPEAT_ALL_TRACK,
                        STATE_REPEAT_GROUP,
                        STATE_REPEAT_SINGLE_TRACK);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Checks if the setting exists for this player.
     *
     * @param setting the setting to check
     * @return the true if the setting exists or false if the setting is not found
     */
    public boolean isPlayerSettingSet(@PlayerSetting int setting) {
        return mSettingsValue.containsKey(setting);
    }

    /**
     * Returns the value of the given setting.
     *
     * @param setting the setting to get the value for
     * @return the value of the setting or STATE_OFF if the setting is not found
     */
    public int getPlayerSettingValue(@PlayerSetting int setting) {
        return mSettingsValue.getOrDefault(setting, STATE_OFF);
    }

    /**
     * Returns the text describing the given setting.
     *
     * @param setting the setting to get the text for
     * @return the text of the setting or null if the setting is not found
     */
    public @Nullable String getPlayerSettingText(@PlayerSetting int setting) {
        return mSettingsText.get(setting);
    }

    /**
     * Returns the text describing the given value.
     *
     * @param value the value to get the text for
     * @return the text of the value or null if the value is not found
     */
    public @Nullable String getPlayerValueText(int value) {
        return mValuesText.get(value);
    }

    /**
     * Public builder for BluetoothAvrcpPlayerSettings.
     */
    public static final class Builder {
        private Map<Integer, Integer> mSettingsValue = new HashMap();
        private Map<Integer, String> mSettingsText = new HashMap();
        private Map<Integer, String> mValuesText = new HashMap();

        public Builder(BluetoothAvrcpPlayerSettings original) {
            mSettingsValue.putAll(original.mSettingsValue);
            mSettingsText.putAll(original.mSettingsText);
            mValuesText.putAll(original.mValuesText);
            return this;
        }

        /**
         * Adds a new setting value pair.
         *
         * Players should add all their available settings when registering as only available
         * settings will receive updates.
         *
         * @param setting the setting to add
         * @param value the value for this setting
         * @return the same Builder instance
         * @throws IllegalArgumentException if the setting or value is not supported.
         */
        public @NonNull Builder addPlayerSettingValue(@PlayerSetting int setting, int value) {
            if (!isValidPlayerSetting(setting)) {
                throw new IllegalArgumentException("Setting not supported: " + setting);
            }
            if (!isValidPlayerSettingValue(setting, value)) {
                throw new IllegalArgumentException("Value: " + value
                        + " not supported for setting: " + setting);
            }
            mSettingsValue.put(setting, value);
            return this;
        }

        /**
         * Adds a new setting text name pair.
         *
         * @param setting the setting to add
         * @param text a string representing this setting
         * @return the same Builder instance
         * @throws IllegalArgumentException if the setting is not supported.
         */
        public @NonNull Builder addPlayerSettingText(@PlayerSetting int setting,
                @NonNull String text) {
            if (!isValidPlayerSetting(setting)) {
                throw new IllegalArgumentException("Setting not supported: " + setting);
            }
            mSettingsText.put(setting, text);
            return this;
        }

        /**
         * Adds a new value text name pair.
         *
         * @param value the value for this setting
         * @param text a string representing this value
         * @return the same Builder instance
         * @throws IllegalArgumentException if the value is not supported.
         */
        public @NonNull Builder addPlayerValueText(int value, @NonNull String text) {
            if (!isValidPlayerValue(value)) {
                throw new IllegalArgumentException("Value not supported: " + value);
            }
            mValuesText.put(value, text);
            return this;
        }

        /**
         * Build {@link BluetoothAvrcpPlayerSettings}.
         * @return new BluetoothAvrcpPlayerSettings built
         * @throws IllegalStateException if the settings are not set.
         */
        public @NonNull BluetoothAvrcpPlayerSettings build() {
            if (!isValid()) {
                throw new IllegalStateException("Settings cannot be empty");
            }
            return new BluetoothAvrcpPlayerSettings(mSettingsValue, mSettingsText, mValuesText);
        }
    }
}
