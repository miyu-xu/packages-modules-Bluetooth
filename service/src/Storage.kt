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

package com.android.server.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private const val TAG = "Storage"

private val KEY_NAME = stringPreferencesKey("name")
private val KEY_ADDRESS = stringPreferencesKey("address")
private val KEY_NAME_AND_ADDRESS_MIGRATED = booleanPreferencesKey("name_and_address_migrated ")

class Storage(private val context: Context, fileName: String) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = fileName)

    fun getName() = runBlocking { context.dataStore.data.map { it[KEY_NAME] }.firstOrNull() }

    fun getAddress() = runBlocking { context.dataStore.data.map { it[KEY_ADDRESS] }.firstOrNull() }

    suspend fun nameAndAddressMigrated() =
        context.dataStore.data.map { it[KEY_NAME_AND_ADDRESS_MIGRATED] }.firstOrNull() ?: false

    fun updateNameFromJava(name: String) = runBlocking { updateName(name) }

    fun updateAddressFromJava(address: String) = runBlocking { updateAddress(address) }

    fun updatenameAndAddressMigratedFromJava() = runBlocking { updatenameAndAddressMigrated() }

    suspend fun updateName(name: String) {
        context.dataStore.edit { it[KEY_NAME] = name }
    }

    suspend fun updateAddress(address: String) {
        context.dataStore.edit { it[KEY_ADDRESS] = address }
    }

    suspend fun updatenameAndAddressMigrated() {
        context.dataStore.edit { it[KEY_NAME_AND_ADDRESS_MIGRATED] = true }
    }

    fun migrateNameAndAddressIfNeeded(contentResolver: ContentResolver) {
        runBlocking {
            if (nameAndAddressMigrated()) {
                Log.d(TAG, "migrateNameAndAddressIfNeeded: skipped")
                return@runBlocking
            }
            Log.d(TAG, "migrateNameAndAddressIfNeeded: running now")
            val name = Settings.Secure.getString(contentResolver, Settings.Secure.BLUETOOTH_NAME)
            if (name != null && !name.isEmpty()) {
                updateName(name)
            }
            val address =
                Settings.Secure.getString(contentResolver, Settings.Secure.BLUETOOTH_ADDRESS)
            if (BluetoothAdapter.checkBluetoothAddress(address)) {
                updateAddress(address)
            }
            updatenameAndAddressMigrated()
        }
    }
}
