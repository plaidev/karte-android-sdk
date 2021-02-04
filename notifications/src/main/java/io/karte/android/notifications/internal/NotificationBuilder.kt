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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.karte.android.notifications.KarteAttributes
import io.karte.android.notifications.R

private const val ATTACHMENT_TYPE_IMAGE = "image"
private const val META_DATA_ICON_KEY = "io.karte.android.Tracker.notification_icon"
private const val META_DATA_LARGE_ICON_KEY = "io.karte.android.Tracker.notification_large_icon"
private const val META_DATA_COLOR_KEY = "io.karte.android.Tracker.notification_color"

internal class NotificationBuilder(
    private val context: Context,
    channelId: String
) {
    private val builder = NotificationCompat.Builder(context, channelId)

    @Throws(PackageManager.NameNotFoundException::class)
    private fun setAttributes(attributes: KarteAttributes): NotificationBuilder {
        builder.setAutoCancel(true)
            .setContentTitle(attributes.title)
            .setContentText(attributes.body)

        val bundle: Bundle = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).metaData ?: Bundle()
        if (bundle.containsKey(META_DATA_ICON_KEY)) {
            builder.setSmallIcon(bundle.getInt(META_DATA_ICON_KEY))
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                // adaptive icon設定時にクラッシュするバグを回避するため、アプリアイコンを利用しない
                // https://stackoverflow.com/questions/47368187/android-oreo-notification-crashes-system-ui
                builder.setSmallIcon(R.drawable.krt__notification_default)
            } else {
                // above lollipop, white square will be shown.
                builder.setSmallIcon(context.applicationInfo.icon)
            }
        }

        if (bundle.containsKey(META_DATA_COLOR_KEY)) {
            builder.color = ContextCompat.getColor(context, bundle.getInt(META_DATA_COLOR_KEY))
        }

        if (attributes.sound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        val bigPicture: Bitmap? =
            if (attributes.type == ATTACHMENT_TYPE_IMAGE && attributes.fileUrl != "") {
                BitmapUtil.getBigPicture(attributes.fileUrl)
            } else {
                null
            }
        if (bigPicture != null) {
            builder.setLargeIcon(bigPicture)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .setBigContentTitle(attributes.title)
                    .setSummaryText(attributes.body)
            )
        } else {
            if (bundle.containsKey(META_DATA_LARGE_ICON_KEY)) {
                builder.setLargeIcon(
                    BitmapFactory.decodeResource(
                        context.resources,
                        bundle.getInt(META_DATA_LARGE_ICON_KEY)
                    )
                )
            }
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(attributes.title)
                    .bigText(attributes.body)
            )
        }
        return this
    }

    fun build(): Notification = builder.build()

    companion object {
        fun build(
            context: Context,
            channelId: String,
            attributes: KarteAttributes
        ): Notification {
            return NotificationBuilder(context, channelId)
                .setAttributes(attributes)
                .build()
        }
    }
}
