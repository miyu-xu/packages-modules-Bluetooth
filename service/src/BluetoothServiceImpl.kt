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
package com.android.server.bluetooth

import android.app.BroadcastOptions
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_BLE_ON
import android.bluetooth.BluetoothAdapter.STATE_BLE_TURNING_OFF
import android.bluetooth.BluetoothAdapter.STATE_BLE_TURNING_ON
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.IBluetooth
import android.bluetooth.IBluetoothCallback
import android.bluetooth.IBluetoothManagerCallback
import android.content.AttributionSource
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerExemptionManager
import android.os.PowerExemptionManager.TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.UserHandle
import android.os.UserManager
import com.android.internal.util.StateMachine
import com.android.internal.util.StateMachineBuilder
import com.android.internal.util.enumStateMachine
import com.android.server.SystemService
import com.android.server.SystemService.TargetUser
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintWriter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MessengerInterface {
  fun registerAdapter(callback: IBluetoothManagerCallback): IBluetooth?

  fun unregisterAdapter(callback: IBluetoothManagerCallback)

  fun enable(packageName: String, quietMode: Boolean, bleToken: IBinder): Boolean

  fun disable(packageName: String, persist: Boolean, bleToken: IBinder): Boolean

  fun factoryReset(packageName: String): Boolean

  fun getAddress(): String

  fun getName(): String

  fun isBleScanAvailable(): Boolean

  fun isHearingAidProfileSupported(): Boolean

  fun isAutoOnSupported(): Boolean

  fun isAutoOnEnabled(): Boolean

  fun setAutoOnEnabled(status: Boolean)
}

private const val TAG = "BluetoothServiceImpl"

class BluetoothServiceImpl(context: Context) : SystemService(context), MessengerInterface {
  private val handlerThread: HandlerThread
  private val looper: Looper
  private val userManager: UserManager
  private val serviceMessenger: ServiceMessenger?
  private val permissionChecker: PermissionChecker?
  private val stateMachine: StateMachine
  private val source: AttributionSource

  init {
    handlerThread = HandlerThread("BluetoothServiceImpl")
    handlerThread.start()
    looper = handlerThread.looper
    userManager = context.getSystemService(UserManager::class.java)
    source = context.getAttributionSource()
    serviceMessenger = null
    permissionChecker = null
    stateMachine = stateMachineBuilder().create().apply { start() }
  }

  override fun onStart() {
    publishBinderService(
      BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE,
      BluetoothServiceBinder(serviceMessenger?.messenger, permissionChecker),
    )
  }

  override fun onBootPhase(phase: Int) {
    Log.e("WILLIAM", "onBootPhase($phase)")
  }

  override fun onUserStarting(user: TargetUser) {
    Log.e("WILLIAM", "onUserStarting($user)")
  }

  override fun onUserSwitching(from: TargetUser?, to: TargetUser) {
    Log.e("WILLIAM", "onUserStarting($from, $to)")
  }

  override fun onUserUnlocking(user: TargetUser) {
    Log.e("WILLIAM", "onUserUnlocking($user)")
  }

  override fun registerAdapter(callback: IBluetoothManagerCallback): IBluetooth? {
    return null
  }

  override fun unregisterAdapter(callback: IBluetoothManagerCallback) {}

  override fun enable(packageName: String, quietMode: Boolean, bleToken: IBinder): Boolean {
    return false
  }

  override fun disable(packageName: String, persist: Boolean, bleToken: IBinder): Boolean {
    return false
  }

  override fun factoryReset(packageName: String): Boolean {
    return false
  }

  override fun getAddress(): String {
    return ""
  }

  override fun getName(): String {
    return ""
  }

  override fun isBleScanAvailable(): Boolean {
    return false
  }

  override fun isHearingAidProfileSupported(): Boolean {
    return false
  }

  override fun isAutoOnSupported(): Boolean {
    return false
  }

  override fun isAutoOnEnabled(): Boolean {
    return false
  }

  override fun setAutoOnEnabled(status: Boolean) {}

  // TODO: Can we add data to these enums ?
  enum class Events {
    // INIT_USER,
    // CLEAN_USER,
    START_BLUETOOTH,
    BIND_TIMEOUT,
    SERVICE_CONNECTED,
    SERVICE_DISCONNECTED,
    STATE_CHANGED,
    ENABLE_BLE
  }

  private val TIMEOUT_BIND: Duration = 3.seconds

  private fun stateMachineBuilder(): StateMachineBuilder<Events> {
    // TODO on which looper !
    return enumStateMachine<Events>(TAG, initial = "None") {
      var binder: IBinder? = null
      state("None") {
        transition(event = Events.ENABLE_BLE, target="enableBle") {}


      }
      state("enableBle") {
      }
      state("Binding") {
        onEntry {
          sendMessageDelayed(
            obtainMessage(Events.BIND_TIMEOUT.ordinal),
            TIMEOUT_BIND.inWholeMilliseconds,
          )
          if (
            !doBind(
              Intent(IBluetooth::class.java.name),
              Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT,
              UserHandle.CURRENT,
            )
          ) {
            // TODO manage error
          }
        }
        transition(event = Events.SERVICE_CONNECTED, target = "Bound") {
          binder = it.obj as IBinder
        }
        // onExit { removeMessages(Events.BIND_TIMEOUT.ordinal) } // TODO removeMessages is
        // protected
      }
      state("Bound", initial = "turningBleOn") {
        lateinit var adapter: AdapterBinder
        var prevState = STATE_OFF
        onEntry {
          adapter = AdapterBinder(binder!!)
          adapter.registerCallback(bluetoothCallback, source)
        }

        transition(event = Events.STATE_CHANGED) { broadcastStateChange(it.arg1, it.arg2) }

        state("turningBleOn") {
          onEntry { adapter.enable(false, source) }
          transition(
            event = Events.STATE_CHANGED,
            cond = { it.arg2 == STATE_BLE_ON },
            target = "BleOn",
          ) {}
          onExit { prevState = STATE_BLE_TURNING_ON }
        }
        state("BleOn") {
          onEntry {
            when (prevState) {
              STATE_BLE_TURNING_ON -> {}
            // STATE_TURNING_OFF -> {
            // sendBrEdrDownCallback()
            // }
            }

            if (prevState == STATE_BLE_TURNING_ON) {}
          }
        }

        state("BrEdrUp") {
          onEntry {
            broadcastToAdapters("sendBluetoothOnCallback", IBluetoothManagerCallback::onBluetoothOn)
          }
          onExit {
            broadcastToAdapters(
              "sendBluetoothOffCallback",
              IBluetoothManagerCallback::onBluetoothOff,
            )
          }
        }
        onExit { adapter.unregisterCallback(bluetoothCallback, source) }
      }
    }
  }

  private fun broadcastStateChange(prevState: Int, newState: Int) {
    broadcastIntentStateChange(BluetoothAdapter.ACTION_BLE_STATE_CHANGED, prevState, newState)

    // BLE state are shown as STATE_OFF for BrEdr users
    val prevBrEdrState = if (isBleState(prevState)) STATE_OFF else prevState
    val newBrEdrState = if (isBleState(newState)) STATE_OFF else newState

    if (prevBrEdrState != newBrEdrState) { // Only broadcast when there is a BrEdr state change.
      broadcastIntentStateChange(
        BluetoothAdapter.ACTION_STATE_CHANGED,
        prevBrEdrState,
        newBrEdrState,
      )
    }
  }

  private fun broadcastIntentStateChange(action: String, prevState: Int, newState: Int) {
    Log.d(
      TAG,
      "broadcastIntentStateChange:" +
        (" action=" + action.substring(action.lastIndexOf('.') + 1)) +
        (" prevState=" + BluetoothAdapter.nameForState(prevState)) +
        (" newState=" + BluetoothAdapter.nameForState(newState)),
    )

    val intent =
      Intent(action)
        .putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, prevState)
        .putExtra(BluetoothAdapter.EXTRA_STATE, newState)
        .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT)

    context.sendBroadcastAsUser(
      intent,
      UserHandle.ALL,
      null,
      BroadcastOptions.makeBasic()
        .apply {
          setTemporaryAppAllowlist(
            10.seconds.inWholeMilliseconds,
            TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED,
            PowerExemptionManager.REASON_BLUETOOTH_BROADCAST,
            "Broadcasting bluetooth state change: $prevState -> $newState",
          )
        }
        .toBundle(),
    )
  }

  fun interface RemoteExceptionConsumer<T> {
    @Throws(RemoteException::class) fun accept(t: T)
  }

  val mCallbacks = RemoteCallbackList<IBluetoothManagerCallback>()

  private fun broadcastToAdapters(
    logAction: String,
    action: RemoteExceptionConsumer<IBluetoothManagerCallback>,
  ) {
    val itemCount = mCallbacks.beginBroadcast()
    try {
      Log.d(TAG, "Broadcasting $logAction() to $itemCount receivers.")
      for (i in 0..itemCount) { // TODO check boundary of iteration
        try {
          action.accept(mCallbacks.getBroadcastItem(i))
        } catch (e: RemoteException) {
          Log.e(TAG, "RemoteException while calling $logAction()#$i", e)
        }
      }
    } finally {
      mCallbacks.finishBroadcast()
    }
  }

  private fun isBleState(state: Int): Boolean {
    return intArrayOf(STATE_BLE_ON, STATE_BLE_TURNING_ON, STATE_BLE_TURNING_OFF).any { it == state }
  }

  private fun doBind(intent: Intent, flags: Int, user: UserHandle): Boolean {
    val comp = resolveSystemService(intent)
    intent.setComponent(comp)
    if (comp == null || !context.bindServiceAsUser(intent, serviceConnection, flags, user)) {
      Log.e(TAG, "Fail to bind to: " + intent)
      return false
    }
    return true
  }

  private fun resolveSystemService(intent: Intent): ComponentName? {
    Log.e("resolveSystemService", "Not implemented $intent")
    return null
    // TODO
  }

  private val bluetoothCallback =
    object : IBluetoothCallback.Stub() {
      override fun onBluetoothStateChange(prevState: Int, newState: Int) {
        stateMachine.obtainMessage(Events.STATE_CHANGED.ordinal, prevState, newState).sendToTarget()
      }
    }

  private val serviceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
        val name = componentName.getClassName()
        Log.d(TAG, "ServiceConnection.onServiceConnected(" + name + ", " + service + ")")
        if (!name.equals("com.android.bluetooth.btservice.AdapterService")) {
          Log.e(TAG, "Unknown service connected: " + name)
          return
        }
        stateMachine.obtainMessage(Events.SERVICE_CONNECTED.ordinal, service).sendToTarget()
      }

      override fun onServiceDisconnected(componentName: ComponentName) {
        // Called if we unexpectedly disconnect.
        val name = componentName.getClassName()
        Log.d(TAG, "ServiceConnection.onServiceDisconnected(" + name + ")")
        if (!name.equals("com.android.bluetooth.btservice.AdapterService")) {
          Log.e(TAG, "Unknown service disconnected: " + name)
          return
        }
        stateMachine.obtainMessage(Events.SERVICE_DISCONNECTED.ordinal).sendToTarget()
      }
    }

  fun dump(fd: FileDescriptor) {
    PrintWriter(FileOutputStream(fd)).apply {
      println(" I am now printing the stateMachine")
      val output = ByteArrayOutputStream()
      stateMachineBuilder().toScxml(output, "UTF-8")
      println(output.toString("UTF-8"))
      flush()
    }
  }
}
