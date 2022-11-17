/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.pandora

import android.bluetooth.BluetoothManager
import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.*
import android.provider.ContactsContract.CommonDataKinds.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.PBAPGrpc.PBAPImplBase
import pandora.PbapProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Pbap(val context: Context) : PBAPImplBase() {
  private val TAG = "PandoraPbap"

  private val scope: CoroutineScope
  private val allowedDigits = ('0'..'9')

  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)!!
  private val bluetoothAdapter = bluetoothManager.adapter

  init {
    // Init the CoroutineScope
    scope = CoroutineScope(Dispatchers.Default)
    preparePBAPDatabase()
  }

  private fun preparePBAPDatabase() {
    prepareContactList()
  }

  private fun prepareContactList() {
    var cursor =
      context
        .getContentResolver()
        .query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

    if (cursor.getCount() > 0) return // return if contacts are present

    for (item in 1..CONTACT_LIST_SIZE) {
      addContact(item)
    }
  }

  private fun addContact(contactIndex: Int) {
    val operations = arrayListOf<ContentProviderOperation>()

    val displayName = String.format(DEFAULT_DISPLAY_NAME, contactIndex)
    val phoneNumber = generatePhoneNumber(PHONE_NUM_LENGTH)
    val emailID = String.format(DEFAULT_EMAIL_ID, contactIndex)

    val rawContactInsertIndex = operations.size
    operations.add(
      ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
        .withValue(RawContacts.ACCOUNT_TYPE, null)
        .withValue(RawContacts.ACCOUNT_NAME, null)
        .build()
    )

    operations.add(
      ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, displayName)
        .build()
    )

    operations.add(
      ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        .withValue(Phone.NUMBER, phoneNumber)
        .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
        .build()
    )

    operations.add(
      ContentProviderOperation.newInsert(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
        .withValue(Email.DATA, emailID)
        .withValue(Email.TYPE, Email.TYPE_MOBILE)
        .build()
    )

    context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations)
  }

  private fun generatePhoneNumber(length: Int): String {
    return buildString { repeat(length) { append(allowedDigits.random()) } }
  }

  fun deinit() {
    // Deinit the CoroutineScope
    scope.cancel()
  }

  companion object {
    const val DEFAULT_DISPLAY_NAME = "Contact Name %d"
    const val DEFAULT_EMAIL_ID = "dummy_%d@dummy.com"
    const val CONTACT_LIST_SIZE = 10
    const val PHONE_NUM_LENGTH = 10
  }
}
