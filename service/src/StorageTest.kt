/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.server.bluetooth.Storage
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class StorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resolver = context.getContentResolver()
    private val looper = Looper.getMainLooper()
    lateinit var storage: Storage

    @Before
    public fun setup() {
        storage = Storage(context, "StorageTest")
    }

    @Test
    fun getValues_onEmptyStorage_returnUninitializedValue() {
        assertThat(storage.getName()).isNull()
        assertThat(storage.getAddress()).isNull()
        assertThat(storage.nameAndAddressMigrated()).isFalse()
    }

    @Test
    fun updateName() {
        val name = "whaou nice name"
        storage.updateNameFromJava(name)
        assertThat(storage.getName()).isEqualTo(name)
    }

    @Test
    fun updateAddress() {
        val address = "FF:EE:DD:CC:BB:AA"
        storage.updateAddressFromJava(address)
        assertThat(storage.getAddress()).isEqualTo(address)
    }

    @Test
    fun updateMigration() {
        storage.updatenameAndAddressMigratedFromJava()
        assertThat(storage.nameAndAddressMigrated()).isTrue()
    }

    @Test
    fun performMigration_withValidNameAndAddress_nameAndAddressAreSet() {
        val name = "performMigration_withValidNameAndAddress_nameAndAddressAreSet"
        val address = "FF:EE:DD:CC:BB:42"
        Settings.Secure.putString(resolver, "bluetooth_name", name)
        Settings.Secure.putString(resolver, "bluetooth_address", address)
        shadowOf(looper).idle()
        storage.migrateNameAndAddressIfNeeded(resolver)
        assertThat(storage.getName()).isEqualTo(name)
        assertThat(storage.getAddress()).isEqualTo(address)
    }

    @Test
    fun performMigration_whenAlreadyMigrated_noUpdate() {
        val original_name = "performMigration_whenAlreadyMigrated_noUpdate"
        val original_address = "FF:EE:DD:CC:BB:43"
        Settings.Secure.putString(resolver, "bluetooth_name", original_name)
        Settings.Secure.putString(resolver, "bluetooth_address", original_address)
        shadowOf(looper).idle()
        storage.migrateNameAndAddressIfNeeded(resolver)

        val name = "new name"
        val address = "00:EE:DD:CC:BB:43"
        Settings.Secure.putString(resolver, "bluetooth_name", name)
        Settings.Secure.putString(resolver, "bluetooth_address", address)
        shadowOf(looper).idle()
        storage.migrateNameAndAddressIfNeeded(resolver)

        assertThat(storage.getName()).isEqualTo(original_name)
        assertThat(storage.getAddress()).isEqualTo(original_address)
    }

    @Test
    fun performMigration_withoutNameAndAddress_nameAndAddressAreUnset() {
        storage.migrateNameAndAddressIfNeeded(resolver)
        assertThat(storage.getName()).isNull()
        assertThat(storage.getAddress()).isNull()
    }

    @Test
    fun performMigration_withInvalidNameAndAddress_nameAndAddressAreSet() {
        val name = ""
        val address = "FF:EE:Not a valid Address:CC:BB:42"
        Settings.Secure.putString(resolver, "bluetooth_name", name)
        Settings.Secure.putString(resolver, "bluetooth_address", address)
        shadowOf(looper).idle()
        storage.migrateNameAndAddressIfNeeded(resolver)
        assertThat(storage.getName()).isNull()
        assertThat(storage.getAddress()).isNull()
    }
}
