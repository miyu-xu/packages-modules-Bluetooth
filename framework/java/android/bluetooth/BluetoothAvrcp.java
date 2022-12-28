/*
 * Copyright (C) 2014 The Android Open Source Project
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

/**
 * This class contains constants for Bluetooth AVRCP profile.
 *
 * {@hide}
 */
public final class BluetoothAvrcp {

    /*
     * State flags for Passthrough commands
    */
    public static final int PASSTHROUGH_STATE_PRESS = 0;
    public static final int PASSTHROUGH_STATE_RELEASE = 1;

    /*
     * Operation IDs for Passthrough commands
    */
    public static final int PASSTHROUGH_ID_SELECT = 0x00;    /* select */
    public static final int PASSTHROUGH_ID_UP = 0x01;    /* up */
    public static final int PASSTHROUGH_ID_DOWN = 0x02;    /* down */
    public static final int PASSTHROUGH_ID_LEFT = 0x03;    /* left */
    public static final int PASSTHROUGH_ID_RIGHT = 0x04;    /* right */
    public static final int PASSTHROUGH_ID_RIGHT_UP = 0x05;    /* right-up */
    public static final int PASSTHROUGH_ID_RIGHT_DOWN = 0x06;    /* right-down */
    public static final int PASSTHROUGH_ID_LEFT_UP = 0x07;    /* left-up */
    public static final int PASSTHROUGH_ID_LEFT_DOWN = 0x08;    /* left-down */
    public static final int PASSTHROUGH_ID_ROOT_MENU = 0x09;    /* root menu */
    public static final int PASSTHROUGH_ID_SETUP_MENU = 0x0A;    /* setup menu */
    public static final int PASSTHROUGH_ID_CONT_MENU = 0x0B;    /* contents menu */
    public static final int PASSTHROUGH_ID_FAV_MENU = 0x0C;    /* favorite menu */
    public static final int PASSTHROUGH_ID_EXIT = 0x0D;    /* exit */
    public static final int PASSTHROUGH_ID_0 = 0x20;    /* 0 */
    public static final int PASSTHROUGH_ID_1 = 0x21;    /* 1 */
    public static final int PASSTHROUGH_ID_2 = 0x22;    /* 2 */
    public static final int PASSTHROUGH_ID_3 = 0x23;    /* 3 */
    public static final int PASSTHROUGH_ID_4 = 0x24;    /* 4 */
    public static final int PASSTHROUGH_ID_5 = 0x25;    /* 5 */
    public static final int PASSTHROUGH_ID_6 = 0x26;    /* 6 */
    public static final int PASSTHROUGH_ID_7 = 0x27;    /* 7 */
    public static final int PASSTHROUGH_ID_8 = 0x28;    /* 8 */
    public static final int PASSTHROUGH_ID_9 = 0x29;    /* 9 */
    public static final int PASSTHROUGH_ID_DOT = 0x2A;    /* dot */
    public static final int PASSTHROUGH_ID_ENTER = 0x2B;    /* enter */
    public static final int PASSTHROUGH_ID_CLEAR = 0x2C;    /* clear */
    public static final int PASSTHROUGH_ID_CHAN_UP = 0x30;    /* channel up */
    public static final int PASSTHROUGH_ID_CHAN_DOWN = 0x31;    /* channel down */
    public static final int PASSTHROUGH_ID_PREV_CHAN = 0x32;    /* previous channel */
    public static final int PASSTHROUGH_ID_SOUND_SEL = 0x33;    /* sound select */
    public static final int PASSTHROUGH_ID_INPUT_SEL = 0x34;    /* input select */
    public static final int PASSTHROUGH_ID_DISP_INFO = 0x35;    /* display information */
    public static final int PASSTHROUGH_ID_HELP = 0x36;    /* help */
    public static final int PASSTHROUGH_ID_PAGE_UP = 0x37;    /* page up */
    public static final int PASSTHROUGH_ID_PAGE_DOWN = 0x38;    /* page down */
    public static final int PASSTHROUGH_ID_POWER = 0x40;    /* power */
    public static final int PASSTHROUGH_ID_VOL_UP = 0x41;    /* volume up */
    public static final int PASSTHROUGH_ID_VOL_DOWN = 0x42;    /* volume down */
    public static final int PASSTHROUGH_ID_MUTE = 0x43;    /* mute */
    public static final int PASSTHROUGH_ID_PLAY = 0x44;    /* play */
    public static final int PASSTHROUGH_ID_STOP = 0x45;    /* stop */
    public static final int PASSTHROUGH_ID_PAUSE = 0x46;    /* pause */
    public static final int PASSTHROUGH_ID_RECORD = 0x47;    /* record */
    public static final int PASSTHROUGH_ID_REWIND = 0x48;    /* rewind */
    public static final int PASSTHROUGH_ID_FAST_FOR = 0x49;    /* fast forward */
    public static final int PASSTHROUGH_ID_EJECT = 0x4A;    /* eject */
    public static final int PASSTHROUGH_ID_FORWARD = 0x4B;    /* forward */
    public static final int PASSTHROUGH_ID_BACKWARD = 0x4C;    /* backward */
    public static final int PASSTHROUGH_ID_ANGLE = 0x50;    /* angle */
    public static final int PASSTHROUGH_ID_SUBPICT = 0x51;    /* subpicture */
    public static final int PASSTHROUGH_ID_F1 = 0x71;    /* F1 */
    public static final int PASSTHROUGH_ID_F2 = 0x72;    /* F2 */
    public static final int PASSTHROUGH_ID_F3 = 0x73;    /* F3 */
    public static final int PASSTHROUGH_ID_F4 = 0x74;    /* F4 */
    public static final int PASSTHROUGH_ID_F5 = 0x75;    /* F5 */
    public static final int PASSTHROUGH_ID_VENDOR = 0x7E;    /* vendor unique */
    public static final int PASSTHROUGH_KEYPRESSED_RELEASE = 0x80;

    public interface PlayerSettingsCallback {
        /**
         * Request for players to set attributes values.
         */
        public void onSetPlayerSettingsAttributes(Map<Integer, Integer> attributesValues,
                BluetoothDevice device) {};
        /**
         * Players should return a list of values mapped to the given attributes.
         */
        public void onRequestPlayerAttributesValues(List<Integer> attributes,
                BluetoothDevice device) {};
    }

    public final class AvrcpPlayerSettings implements Parcelable {
        public static final String TAG = "AvrcpPlayerSettings";

        /**
         * Repeat setting.
         */
        public static final int SETTING_REPEAT = 0;

        /**
         * Shuffle setting.
         */
        public static final int SETTING_SHUFFLE = 1;

        /** @hide */
        @IntDef({SETTING_REPEAT, SETTING_SHUFFLE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface PlayerSetting {}

        /**
         * OFF state.
         *
         * Denotes a general OFF state. Applies to all settings.
         */
        public static final int STATE_OFF = 0;

        /**
         * Single track repeat.
         *
         * Applies only to {@link SETTING_REPEAT}.
         */
        public static final int STATE_SINGLE_TRACK = 1;

        /**
         * All track repeat/shuffle.
         *
         * Applies to {@link #SETTING_REPEAT}, {@link #SETTING_SHUFFLE}.
         */
        public static final int STATE_ALL_TRACK = 2;

        /**
         * Group repeat/shuffle.
         *
         * Applies to {@link #SETTING_REPEAT}, {@link #SETTING_SHUFFLE}.
         */
        public static final int STATE_GROUP = 3;

        /** @hide */
        @IntDef({STATE_OFF, STATE_SINGLE_TRACK, STATE_ALL_TRACK, STATE_GROUP})
        @Retention(RetentionPolicy.SOURCE)
        public @interface PlayerSettingValue {}


        private Map<Integer, Integer> mSettingsValue = new HashMap<Integer, Integer>();
        private Map<Integer, String> mSettingsText = new HashMap<Integer, String>();

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
            for (int j : mSettingsValue.keySet()) {
                out.writeInt(j);
                out.writeInt(mSettingsValue.get(j));
            }
            out.writeInt(mSettingsText.size());
            for (int k : mSettingsText.keySet()) {
                out.writeInt(k);
                out.writeString(mSettingsText.get(k));
            }
        }

        public static final @NonNull Creator<AvrcpPlayerSettings> CREATOR = new Creator<>() {
            public AvrcpPlayerSettings createFromParcel(Parcel in) {
                return new AvrcpPlayerSettings(in);
            }

            public AvrcpPlayerSettings[] newArray(int size) {
                return new AvrcpPlayerSettings[size];
            }
        };

        private AvrcpPlayerSettings(Parcel in) {
            int numSettingsValues = in.readInt();
            for (int i = 0; i < numSettingsValues; i++) {
                mSettingsValue.put(in.readInt(), in.readInt());
            }
            int numSettingsText = in.readInt();
            for (int j = 0; j < numSettingsText; j++) {
                mSettingsValue.put(in.readInt(), in.readString());
            }
        }

        private AvrcpPlayerSettings(@NonNull Map<Integer, Integer> settingsValue,
                Map<Integer, String> settingsText) {
            mSettingsValue = settingsValue;
            mSettingsText = settingsText;
        }

        /**
         * @return true if the settings are not empty
         */
        public boolean isValid() {
            return !mSettingsValue.isEmpty();
        }

        /**
         * Ensures that the setting are valid.
         *
         * @return true if the settings are valid, false otherwise
         */
        public boolean isValidPlayerSetting(@PlayerSetting int setting) {
            if (setting > SETTING_SHUFFLE || setting < SETTING_REPEAT) {
                return false;
            }
            return true;
        }

        /**
         * Ensures that the setting and value given are valid.
         *
         * @return true if the settings are valid, false otherwise
         */
        public boolean isValidPlayerSettingValue(@PlayerSetting int setting,
                @PlayerSettingValue int value) {
            switch (setting) {
                case SETTING_SHUFFLE:
                    if (value == STATE_OFF || value == STATE_ALL_TRACK || value == STATE_GROUP) {
                        return true;
                    }
                    return false;
                case SETTING_REPEAT:
                    if (value == STATE_OFF || value == STATE_ALL_TRACK || value == STATE_GROUP
                            || value == STATE_SINGLE_TRACK) {
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }

        /**
         * Returns the value of the given setting.
         *
         * @param setting the setting to get the value for
         * @return the value of the setting or STATE_OFF if the setting is not found
         */
        public int getPlayerSettingValue(@PlayerSetting int setting) {
            if (isValidPlayerSetting(setting) && mSettingsValue.contains(setting)) {
                return mSettingsValue.get(setting);
            }
            return STATE_OFF;
        }

        /**
         * Returns the text describing the given setting.
         *
         * @param setting the setting to get the text for
         * @return the text of the setting or null if the setting is not found
         */
        public @Nullable String getPlayerSettingText(@PlayerSetting int setting) {
            if (isValidPlayerSetting(setting) && mSettingsText.contains(setting)) {
                return mSettingsText.get(setting);
            }
            return null;
        }

        public static final class Builder {
            private Map<Integer, Integer> mSettingsValue = new HashMap();
            private Map<Integer, String> mSettingsText = new HashMap();

            /**
             * Adds a new setting value pair.
             *
             * @param setting the setting to add
             * @param value the value for this setting
             * @return the same Builder instance
             */
            public @NonNull Builder addPlayerSettingValue(@PlayerSetting int setting,
                    @PlayerSettingValue int value) {
                if (isValidPlayerSettingValue(setting, value)) {
                    mSettingsValue.add(setting, value);
                }
                return this;
            }

            /**
             * Adds a new setting value pair.
             *
             * @param setting the setting to add
             * @param value the value for this setting
             * @param text a string representing this setting
             * @return the same Builder instance
             */
            public @NonNull Builder addPlayerSettingText(@PlayerSetting int setting,
                    @PlayerSettingValue int value, @NonNull String text) {
                if (isValidPlayerSettingValue(setting, value)) {
                    mSettingsValue.add(setting, value);
                    mSettingsText.add(setting, text);
                }
                return this;
            }

            /**
             * Build {@link AvrcpPlayerSettings}.
             * @return new AvrcpPlayerSettings built
             */
            public @NonNull AvrcpPlayerSettings build() {
                return new AvrcpPlayerSettings(mSettingsValue, mSettingsText);
            }
        }

    }
}
