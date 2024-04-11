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
package com.android.server.bluetooth.test

// import com.android.bedstead.permissions.annotations.EnsureDoesNotHavePermission
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.os.UserManager
import android.permission.PermissionManager
import androidx.test.core.app.ApplicationProvider
import com.android.bedstead.harrier.BedsteadJUnit4
import com.android.bedstead.harrier.DeviceState
import com.android.bedstead.harrier.annotations.EnsureDoesNotHavePermission
import com.android.bedstead.harrier.annotations.EnsureHasPermission
import com.android.server.bluetooth.Log
import com.android.server.bluetooth.PermissionChecker
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(BedsteadJUnit4::class)
class PermissionCheckerTest {
    companion object {
        @JvmField @ClassRule @Rule val deviceState = DeviceState()
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val userManager = context.getSystemService(UserManager::class.java)!!
    private val permissionManager = context.getSystemService(PermissionManager::class.java)!!
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)!!

    @JvmField @Rule val testName = TestName()

    lateinit var checker: PermissionChecker

    @Before
    fun setUp() {
        Log.i("PermissionCheckerTest", "\t--> setup of " + testName.getMethodName())
        checker =
            PermissionChecker(
                context,
                userManager,
                context.packageManager,
                permissionManager,
                appOpsManager,
                context.attributionSource
            )
    }

    @Test
    fun enableBackground_whenAllowed_DontThrow() {
        checker.enableAllowed(Process.SYSTEM_UID, context.attributionSource, false)
    }

    @Test
    fun enableForeground_whenAllowed_DontThrow() {
        checker.enableAllowed(Process.SYSTEM_UID, context.attributionSource, true)
    }

    @Test
    @EnsureDoesNotHavePermission(BLUETOOTH_CONNECT)
    fun foo() {
        checker.getAddressAllowed(Process.myUid(), context.attributionSource)
    }

    @Test
    @EnsureHasPermission(BLUETOOTH_CONNECT)
    fun fooWith() {
        checker.getAddressAllowed(Process.myUid(), context.attributionSource)
    }
}
