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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;

import java.io.IOException;
import java.lang.annotation.Retention;

@Retention(SOURCE)
@IntDef({
    BluetoothSocketException.BLUETOOTH_OFF_FAILURE,
    BluetoothSocketException.SOCKET_MANAGER_FAILURE,
    BluetoothSocketException.SOCKET_CLOSED,
    BluetoothSocketException.SOCKET_CONNECTION_FAILURE,
    BluetoothSocketException.NULL_DEVICE,
    BluetoothSocketException.L2CAP_ACL_FAILURE,
    BluetoothSocketException.L2CAP_CLIENT_SECURITY_FAILURE,
    BluetoothSocketException.L2CAP_INSUFFICIENT_AUTHENTICATION,
    BluetoothSocketException.L2CAP_INSUFFICIENT_AUTHORIZATION,
    BluetoothSocketException.L2CAP_INSUFFICIENT_ENCRYPT_KEY_SIZE,
    BluetoothSocketException.L2CAP_INSUFFICIENT_ENCRYPTION,
    BluetoothSocketException.L2CAP_INVALID_SOURCE_CID,
    BluetoothSocketException.L2CAP_SOURCE_CID_ALREADY_ALLOCATED,
    BluetoothSocketException.L2CAP_UNACCEPTABLE_PARAMETERS,
    BluetoothSocketException.L2CAP_INVALID_PARAMETERS,
    BluetoothSocketException.L2CAP_NO_RESOURCES,
    BluetoothSocketException.L2CAP_NO_PSM_AVAILABLE,
    BluetoothSocketException.L2CAP_TIMEOUT})
public @interface BluetoothSocketExceptionCode  {}

/**
 * Thrown when an error occurs during a Bluetooth Socket related exception.
 *
 * <p> This is currently only intended to be thrown for a failure during
 * {@link BluetoothSocket#connect()} operation.
 */
public class BluetoothSocketException extends IOException {

    /**
     * Error code during connect when socket connection fails for unknown reason
     * during L2CAP connection.
     */
    public static final int L2CAP_UNKNOWN = 1;

    /**
     * Error code during connect when there is an ACL connection failure.
     */
    public static final int L2CAP_ACL_FAILURE = 2;

    /**
     * Error code during connect when security clearance fails on the client during
     * L2CAP connection.
     */
    public static final int L2CAP_CLIENT_SECURITY_FAILURE = 3;

    /**
     * Error code during connect when authentication fails on the peer device
     * during L2CAP connection.
     */
    public static final int L2CAP_INSUFFICIENT_AUTHENTICATION = 4;

    /**
     * Error code during connect when authorization fails on the peer device during
     * L2CAP connection.
     */
    public static final int L2CAP_INSUFFICIENT_AUTHORIZATION = 5;

    /**
     * Error code during connect indicating insufficient encryption key size on the
     * peer device during L2CAP connection.
     */
    public static final int L2CAP_INSUFFICIENT_ENCRYPT_KEY_SIZE = 6;

    /**
     * Error code during connect for insufficient encryption from the peer device
     * during L2CAP connection.
     */
    public static final int L2CAP_INSUFFICIENT_ENCRYPTION = 7;

    /**
     * Error code during connect for invalid Channel ID from the peer device during
     * L2CAP connection.
     */
    public static final int L2CAP_INVALID_SOURCE_CID = 8;

    /**
     * Error code during connect for already allocated Channel ID from the peer
     * device during L2CAP connection.
     */
    public static final int L2CAP_SOURCE_CID_ALREADY_ALLOCATED = 9;

    /**
     * Error code during connect for unacceptable Parameters from the peer device
     * during L2CAP connection.
     */
    public static final int L2CAP_UNACCEPTABLE_PARAMETERS = 10;

    /**
     * Error code during connect for invalid parameters from the peer device during
     * L2CAP connection.
     */
    public static final int L2CAP_INVALID_PARAMETERS = 11;

    /**
     * Error code during connect when no resources are available for L2CAP
     * connection.
     */
    public static final int L2CAP_NO_RESOURCES = 12;

    /**
     * Error code during connect when no PSM is available for L2CAP connection.
     */
    public static final int L2CAP_NO_PSM_AVAILABLE = 13;

    /**
     * Error code during connect when L2CAP connection timeout.
     */
    public static final int L2CAP_TIMEOUT = 14;

    /**
     * Error code during connect when Bluetooth is off and socket connection is
     * triggered.
     */
    public static final int BLUETOOTH_OFF_FAILURE = 15;

    /**
     * Error code during connect when socket manager is not available.
     */
    public static final int SOCKET_MANAGER_FAILURE = 16;

    /**
     * Error code during connect when socket is closed.
     */
    public static final int SOCKET_CLOSED = 17;

    /**
     * Error code during connect for generic socket connection failures.
     */
    public static final int SOCKET_CONNECTION_FAILURE = 18;

    /**
     * Error code during connect when null device attempts to do socket connection.
     */
    public static final int NULL_DEVICE = 19;

    /* Corresponding messages for respective error codes. */
    private static final String UNKNOWN_MSG = "Connection failed for unknown reason";
    private static final String L2CAP_ACL_FAILURE_MSG = "ACL connection failed";
    private static final String L2CAP_CLIENT_SECURITY_FAILURE_MSG =
            "Client security clearance failed";
    private static final String L2CAP_INSUFFICIENT_AUTHENTICATION_MSG =
            "Insufficient authentication";
    private static final String L2CAP_INSUFFICIENT_AUTHORIZATION_MSG = "Insufficient authorization";
    private static final String L2CAP_INSUFFICIENT_ENCRYPT_KEY_SIZE_MSG =
            "Insufficient encryption key size";
    private static final String L2CAP_INSUFFICIENT_ENCRYPTION_MSG = "Insufficient encryption";
    private static final String L2CAP_INVALID_SOURCE_CID_MSG = "Invalid source CID";
    private static final String L2CAP_SOURCE_CID_ALREADY_ALLOCATED_MSG =
            "Source CID already allocated";
    private static final String L2CAP_UNACCEPTABLE_PARAMETERS_MSG = "Unacceptable Parameters";
    private static final String L2CAP_INVALID_PARAMETERS_MSG = "Invalid Parameters";
    private static final String L2CAP_NO_RESOURCES_MSG = "No resources Available";
    private static final String L2CAP_NO_PSM_AVAILABLE_MSG = "No PSM available";
    private static final String L2CAP_TIMEOUT_MSG = "Connection Timeout";
    private static final String BLUETOOTH_OFF_FAILURE_MSG = "Bluetooth is off";
    private static final String SOCKET_MANAGER_FAILURE_MSG = "bt get socket manager failed";
    private static final String SOCKET_CLOSED_MSG = "socket closed";
    private static final String SOCKET_CONNECTION_FAILURE_MSG = "bt socket connect failed";
    private static final String NULL_DEVICE_MSG = "Connect is called on null device";

    @BluetoothSocketExceptionCode private final int mCode;

    public BluetoothSocketException(@BluetoothSocketExceptionCode int code) {
        super(getMessage(code));
        this.mCode = code;
    }

    private static String getMessage(@BluetoothSocketExceptionCode int code) {
        switch(code) {
            case BLUETOOTH_OFF_FAILURE:
                return BLUETOOTH_OFF_FAILURE_MSG;
            case SOCKET_MANAGER_FAILURE:
                return SOCKET_MANAGER_FAILURE_MSG;
            case SOCKET_CLOSED:
                return SOCKET_CLOSED_MSG;
            case SOCKET_CONNECTION_FAILURE:
                return SOCKET_CONNECTION_FAILURE_MSG;
            case NULL_DEVICE:
                return NULL_DEVICE_MSG;
            case L2CAP_ACL_FAILURE:
                return L2CAP_ACL_FAILURE_MSG;
            case L2CAP_CLIENT_SECURITY_FAILURE:
                return L2CAP_CLIENT_SECURITY_FAILURE_MSG;
            case L2CAP_INSUFFICIENT_AUTHENTICATION:
                return L2CAP_INSUFFICIENT_AUTHENTICATION_MSG;
            case L2CAP_INSUFFICIENT_AUTHORIZATION:
                return L2CAP_INSUFFICIENT_AUTHORIZATION_MSG;
            case L2CAP_INSUFFICIENT_ENCRYPT_KEY_SIZE:
                return L2CAP_INSUFFICIENT_ENCRYPT_KEY_SIZE_MSG;
            case L2CAP_INSUFFICIENT_ENCRYPTION:
                return L2CAP_INSUFFICIENT_ENCRYPTION_MSG;
            case L2CAP_INVALID_SOURCE_CID:
                return L2CAP_INVALID_SOURCE_CID_MSG;
            case L2CAP_SOURCE_CID_ALREADY_ALLOCATED:
                return L2CAP_SOURCE_CID_ALREADY_ALLOCATED_MSG;
            case L2CAP_UNACCEPTABLE_PARAMETERS:
                return L2CAP_UNACCEPTABLE_PARAMETERS_MSG;
            case L2CAP_INVALID_PARAMETERS:
                return L2CAP_INVALID_PARAMETERS_MSG;
            case L2CAP_NO_RESOURCES:
                return L2CAP_NO_RESOURCES_MSG;
            case L2CAP_NO_PSM_AVAILABLE:
                return L2CAP_NO_PSM_AVAILABLE_MSG;
            case L2CAP_TIMEOUT:
                return L2CAP_TIMEOUT_MSG;
            case L2CAP_UNKNOWN:
            default:
                return UNKNOWN_MSG;
        }
    }
}
