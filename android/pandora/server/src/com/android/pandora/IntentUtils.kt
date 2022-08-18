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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val OBSOLETE_TS = Duration.ofMillis(100)

private const val TAG = "PandoraIntentUtils"

enum class IntentUtils(var action: String) {
  STATE_CHANGED(BluetoothAdapter.ACTION_STATE_CHANGED),
  BOND_STATE_CHANGED(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
  CONNECTION_STATE_CHANGED(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED),
  PAIRING_REQUEST(BluetoothDevice.ACTION_PAIRING_REQUEST);

  companion object {
    val receiverFilter = IntentFilter().apply { values().map { addAction(it.action) } }

    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
          values().first { it.action == intent.action }.pushIntent(intent)
        }
      }
  }

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  @Volatile private var list: MutableList<Pair<Long, Intent>> = mutableListOf()

  fun pushIntent(intent: Intent) {
    lock.withLock {
      list += Pair(System.currentTimeMillis(), intent)
      condition.signalAll()
    }
  }

  fun popIntent(predicate: (Intent) -> (Boolean)): Intent = popIntent(null, predicate)

  fun popIntent(device: BluetoothDevice): Intent = popIntent(device, null)

  fun popIntent(device: BluetoothDevice?, predicate: ((Intent) -> (Boolean))?): Intent {
    Log.d(TAG, "Wait for $action. BluetoothDevice: $device | Predicate: $predicate")
    lock.withLock {
      val obsoleteTs = System.currentTimeMillis() - OBSOLETE_TS.toMillis()
      list.removeAll { it.first < obsoleteTs }

      while (true) {
        val matchingIntent =
          list
            .asReversed()
            .filter { device?.let { it1 -> it.second.getBluetoothDeviceExtra() == it1 } ?: true }
            .filter { predicate?.let { it1 -> it1(it.second) } ?: true }

        if (matchingIntent.isNotEmpty()) {
          list.removeAll(matchingIntent)
          Log.d(TAG, "$action found")
          return matchingIntent[0].second
        }
        list.clear()
        condition.await()
      }
    }
  }
}
