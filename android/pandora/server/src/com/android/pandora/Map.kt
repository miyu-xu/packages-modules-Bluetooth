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

import android.content.Context
import com.google.protobuf.Empty
import android.provider.Telephony.*
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.net.Uri
import io.grpc.stub.StreamObserver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import pandora.MAPGrpc.MAPImplBase
import pandora.MapProto.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class Map(val context: Context) : MAPImplBase() {
  private val TAG = "PandoraMap"

  private val scope: CoroutineScope
  private var telephonyManager = context.getSystemService(TelephonyManager::class.java)
  private val MESSAGE_LEN = 130
  private val MESSAGE_COUNT = 10

  init {
    scope = CoroutineScope(Dispatchers.Default)

    prepareMAPDatabase()
  }

  private fun prepareMAPDatabase() {
    // prepare Inbox
    if (getInboxCount() < MESSAGE_COUNT) {
      sendTextMessage(MESSAGE_COUNT)
    }
  }

  override fun sendSMS(request: Empty, responseObserver: StreamObserver<Empty>) {
    grpcUnary<Empty>(scope, responseObserver) {
      sendTextMessage(1)
      Empty.getDefaultInstance()
    }
  }

  private fun getInboxCount(): Int {
    val cursor = context.getContentResolver().query(Sms.Inbox.CONTENT_URI, null, null, null, null)
    return cursor.getCount()
  }

  private fun sendTextMessage(count: Int) {
    val smsManager = SmsManager.getDefault()
    val defaultSmsSub = SubscriptionManager.getDefaultSmsSubscriptionId()
    telephonyManager = telephonyManager.createForSubscriptionId(defaultSmsSub)
    val avdPhoneNumber = telephonyManager.getLine1Number()

    for (index in 1..count) {
      smsManager.sendTextMessage(avdPhoneNumber, avdPhoneNumber, generateAlphanumericString(MESSAGE_LEN), null, null)
    }
  }

  fun deinit() {
    scope.cancel()
  }
}
