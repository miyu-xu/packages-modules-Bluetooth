/*
 * Copyright (C) 2024 The Android Open Source Project
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

#define LOG_TAG "BluetoothServerJni"

#include <android/log.h>
#include <inttypes.h>
#include <nativehelper/JNIPlatformHelp.h>

#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* Set the default logging level for the process using the tag
 * "log.tag.bluetooth" and/or "persist.log.tag.bluetooth" via the android
 * logging framework.
 */
static void update_process_minimum_log_level(JNIEnv* env, jobject /* obj */) {
    const char* stack_default_log_tag = "bluetooth";
    int default_prio = ANDROID_LOG_INFO;
    if (__android_log_is_loggable(ANDROID_LOG_VERBOSE, stack_default_log_tag, default_prio)) {
        LOG_I("Set Bluetooth server default log level to 'VERBOSE'");
        __android_log_set_minimum_priority(ANDROID_LOG_VERBOSE);
    } else if (__android_log_is_loggable(ANDROID_LOG_DEBUG, stack_default_log_tag, default_prio)) {
        LOG_I("Set Bluetooth server default log level to 'DEBUG'");
        __android_log_set_minimum_priority(ANDROID_LOG_DEBUG);
    } else if (__android_log_is_loggable(ANDROID_LOG_INFO, stack_default_log_tag, default_prio)) {
        LOG_I("Set Bluetooth server default log level to 'INFO'");
        __android_log_set_minimum_priority(ANDROID_LOG_INFO);
    } else if (__android_log_is_loggable(ANDROID_LOG_WARN, stack_default_log_tag, default_prio)) {
        LOG_I("Set Bluetooth server default log level to 'WARN'");
        __android_log_set_minimum_priority(ANDROID_LOG_WARN);
    } else if (__android_log_is_loggable(ANDROID_LOG_ERROR, stack_default_log_tag, default_prio)) {
        LOG_I("Set Bluetooth server default log level to 'ERROR'");
        __android_log_set_minimum_priority(ANDROID_LOG_ERROR);
    }
}

namespace android {

const char* sNativeInterfaceClassName = "com/android/server/bluetooth/NativeInterface";

const static JNINativeMethod sMethods[] = {
        {"updateProcessMinimumLogLevelNative", "()V", (void*)update_process_minimum_log_level},
};

int register_com_android_server_bluetooth_NativeInterface(JNIEnv* e) {
    return jniRegisterNativeMethods(e, sNativeInterfaceClassName, sMethods, NELEM(sMethods));
}

} // end namespace android

/*
 * JNI Initialization
 */
jint JNI_OnLoad(JavaVM* jvm, void* /* reserved */) {
    JNIEnv* e = nullptr;
    int status;

    LOG_I("Bluetooth Server : loading JNI\n");

    // Check JNI version
    if (jvm->GetEnv((void**)&e, JNI_VERSION_1_6)) {
        LOG_E("JNI version mismatch error");
        return JNI_ERR;
    }

    // Register functions with Java Virtual Machine
    if (android::register_com_android_server_bluetooth_NativeInterface(e) == -1) {
        LOG_E("Could not register functions for NativeInterface");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
