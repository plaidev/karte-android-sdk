//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android.notifications.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.karte.android.core.logger.Logger

private const val DEFAULT_NOTIFICATION_CHANNEL = "krt_default_channel"
private const val LOG_TAG = "Karte.Notification"

internal object ChannelUtil {

    fun getChannel(manager: NotificationManager, channel: String): String {
        return if (manager.channelExists(channel)) {
            channel
        } else {
            manager.createDefaultChannel()
            DEFAULT_NOTIFICATION_CHANNEL
        }
    }

    private fun NotificationManager.createDefaultChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        Logger.i(LOG_TAG, "Creating defaultChannel for KARTE notification.")
        val channel = NotificationChannel(
            DEFAULT_NOTIFICATION_CHANNEL,
            "Default",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        channel.enableVibration(true)
        channel.setShowBadge(true)
        createNotificationChannel(channel)
    }

    private fun NotificationManager.channelExists(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }
        return notificationChannels.any { it.id == channelId }
    }
}
