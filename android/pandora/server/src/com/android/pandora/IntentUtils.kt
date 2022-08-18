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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val OBSOLETE_TS = Duration.ofMillis(100)
private val filter = IntentFilter().apply { IntentUtils.values().map { addAction(it.action) } }

enum class IntentUtils(var action: String) {
  STATE_CHANGED(BluetoothAdapter.ACTION_STATE_CHANGED),
  BOND_STATE_CHANGED(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
  CONNECTION_STATE_CHANGED(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED),
  PAIRING_REQUEST(BluetoothDevice.ACTION_PAIRING_REQUEST);

  companion object {
    fun register(ctx: Context) {
      LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, filter)
    }

    private val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent) {
          values().first { it.action == intent.action }.pushIntent(intent)
        }
      }
  }

  private val lock: ReentrantLock = ReentrantLock()

  @Volatile private var list: MutableList<Pair<Long, Intent>> = mutableListOf()

  fun pushIntent(intent: Intent) {
    lock.withLock {
      list += Pair(System.currentTimeMillis(), intent)
      lock.newCondition().signalAll()
    }
  }

  fun popIntent(predicate: (Intent) -> (Boolean)): Intent = popIntent(null, predicate)

  fun popIntent(device: BluetoothDevice): Intent = popIntent(device, null)

  fun popIntent(device: BluetoothDevice?, predicate: ((Intent) -> (Boolean))?): Intent {
    lock.withLock {
      val obsoleteTs: Long = System.currentTimeMillis() - OBSOLETE_TS.toMillis()
      list.removeAll { it.first < obsoleteTs }

      while (true) {
        val matchingIntent =
          list
            .asReversed()
            .filter { device?.let { it1 -> it.second.getBluetoothDeviceExtra() == it1 } ?: true }
            .filter { predicate?.let { it1 -> it1(it.second) } ?: true }
        list.removeAll(matchingIntent)

        if (matchingIntent.isNotEmpty()) {
          return matchingIntent[0].second
        }
        lock.newCondition().await()
      }
    }
  }
}
