/*
 * Copyright 2023 The Android Open Source Project
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
package com.android.server.bluetooth.airplane

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val TAG = "BluetoothNotificationManager"
private const val NOTIFICATION_TAG = "com.android.bluetooth"
private const val APM_NOTIFICATION_CHANNEL = "apm_notification_channel"
private const val APM_NOTIFICATION_GROUP = "apm_notification_group"
private const val HELP_PAGE_URL = "https://support.google.com/pixelphone/answer/12639358"

private lateinit var notificationManager: NotificationManager

fun recreateNotificationChannel(userContext: Context) {
    for (notification in notificationManager.activeNotifications) {
        if (NOTIFICATION_TAG == notification.tag) {
            notificationManager.cancel(NOTIFICATION_TAG, notification.id)
        }
    }
    createNotificationChannels(userContext)
}

/** Create notification channels for the current user */
fun createNotificationChannels(userContext: Context) {
    notificationManager = userContext.getSystemService(NotificationManager::class.java)!!

    val channelsList =
        arrayListOf(
            NotificationChannel(
                APM_NOTIFICATION_CHANNEL,
                APM_NOTIFICATION_GROUP,
                NotificationManager.IMPORTANCE_HIGH
            )
        )

    notificationManager.createNotificationChannels(channelsList)
}

/** Build and send the APM notification to the current user */
internal fun sendApmNotification(userContext: Context, title: String, message: String) {
    val openLinkIntent =
        Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse(HELP_PAGE_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val tapPendingIntent =
        PendingIntent.getActivity(
            userContext,
            PendingIntent.FLAG_UPDATE_CURRENT,
            openLinkIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

    val notification =
        Notification.Builder(userContext, APM_NOTIFICATION_CHANNEL)
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(tapPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
    notificationManager.notify(
        NOTIFICATION_TAG,
        com.android.server.bluetooth.BluetoothNotificationManager.NOTE_BT_APM_NOTIFICATION,
        notification
    )
}
