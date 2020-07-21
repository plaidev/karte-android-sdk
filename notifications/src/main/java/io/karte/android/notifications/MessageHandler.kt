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
package io.karte.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.BitmapUtil
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

private const val LOG_TAG = "Karte.MessageHandler"

internal const val KARTE_PUSH_NOTIFICATION_FLAG = "krt_push_notification"
internal const val KARTE_MASS_PUSH_NOTIFICATION_FLAG = "krt_mass_push_notification"
internal const val EXTRA_EVENT_VALUES = "krt_event_values"
internal const val EXTRA_CAMPAIGN_ID = "krt_campaign_id"
internal const val EXTRA_SHORTEN_ID = "krt_shorten_id"
internal const val EXTRA_COMPONENT_NAME = "krt_component_name"

private const val DATA_KARTE_ATTRIBUTES_KEY = "krt_attributes"
private const val ATTACHMENT_TYPE_IMAGE = "image"

private const val NOTIFICATION_TAG = "krt_notification_tag"

private const val DEFAULT_NOTIFICATION_CHANNEL = "krt_default_channel"

private const val META_DATA_ICON_KEY = "io.karte.android.Tracker.notification_icon"
private const val META_DATA_LARGE_ICON_KEY = "io.karte.android.Tracker.notification_large_icon"
private const val META_DATA_COLOR_KEY = "io.karte.android.Tracker.notification_color"

/**
 * KARTEから送信された通知メッセージを処理する機能を提供するクラスです。
 */
object MessageHandler {

    /**
     * KARTE経由で送信された通知メッセージであるか判定します。
     *
     * @param[message] [RemoteMessage]
     * @return 判定結果を返します。
     *
     * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
     */
    @JvmStatic
    fun canHandleMessage(message: RemoteMessage): Boolean {
        if (message.data == null) return false
        return canHandleMessage(message.data)
    }

    /**
     * KARTE経由で送信された通知メッセージであるか判定します。
     *
     * @param[data] [Map]
     * @return 判定結果を返します。
     *
     * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
     */
    @JvmStatic
    fun canHandleMessage(data: Map<String, String>): Boolean {
        return data[KARTE_PUSH_NOTIFICATION_FLAG]?.toBoolean() ?: false ||
            data[KARTE_MASS_PUSH_NOTIFICATION_FLAG]?.toBoolean() ?: false
    }

    /**
     * [RemoteMessage]からSDKが自動で処理するデータを取り出し、[KarteAttributes]インスタンスを返します。
     *
     * @param message [RemoteMessage]
     * @return [KarteAttributes]インスタンスを返します。
     */
    @JvmStatic
    fun extractKarteAttributes(message: RemoteMessage): KarteAttributes? {
        val data = message.data ?: return null
        return extractKarteAttributes(data)
    }

    /**
     * データメッセージからSDKが自動で処理するデータを取り出し、[KarteAttributes]インスタンスを返します。
     *
     * @param message [Map]
     * @return [KarteAttributes]インスタンスを返します。
     */
    @JvmStatic
    fun extractKarteAttributes(data: Map<String, String>): KarteAttributes? {
        val karteAttributes = data[DATA_KARTE_ATTRIBUTES_KEY] ?: return null
        return try {
            KarteAttributes().load(JSONObject(karteAttributes))
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * KARTE経由で送信された通知メッセージから、通知を作成します。
     *
     * @param[context] [Context]
     * @param[message] [RemoteMessage]
     * @param[defaultIntent] [Intent]
     *
     * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
     *
     * @return 判定結果を返します。
     *
     * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
     */
    @JvmStatic
    @JvmOverloads
    fun handleMessage(
        context: Context,
        message: RemoteMessage,
        defaultIntent: Intent? = null
    ): Boolean {
        if (!canHandleMessage(message)) {
            return false
        }
        return handleMessage(context, message.data, defaultIntent)
    }

    /**
     * KARTE経由で送信された通知メッセージ（データメッセージ）から、通知を作成します。
     *
     * @param[context] [Context]
     * @param[data] [Map]
     * @param[defaultIntent] [Intent]
     *
     * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
     *
     * @return 判定結果を返します。
     *
     * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
     */
    @JvmStatic
    @JvmOverloads
    fun handleMessage(
        context: Context,
        data: Map<String, String>,
        defaultIntent: Intent? = null
    ): Boolean {
        if (!canHandleMessage(data)) {
            return false
        }
        try {
            Logger.i(
                LOG_TAG,
                "handleMessage() context: $context, defaultIntent: $defaultIntent, data: $data"
            )
            val karteAttributes = extractKarteAttributes(data) ?: return false

            showNotification(context, karteAttributes, data, defaultIntent)
            return true
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to show notification. $e", e)
            return true
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun showNotification(
        context: Context,
        attributes: KarteAttributes,
        data: Map<String, String>,
        defaultIntent: Intent?
    ) {
        Logger.d(LOG_TAG, "showNotification(): $context, attributes: $attributes")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager == null) {
            Logger.w(LOG_TAG, "Stopped to show notification because NotificationManager is null.")
            return
        }

        val notificationBuilder: NotificationCompat.Builder
        if (supportLibHasNotificationChannelMethod()) {
            if (channelExists(attributes.channel, notificationManager)) {
                notificationBuilder = NotificationCompat.Builder(context, attributes.channel)
            } else {
                createDefaultChannel(notificationManager)
                notificationBuilder =
                    NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL)
            }
        } else {

            notificationBuilder = NotificationCompat.Builder(context)
        }

        val notification =
            buildNotification(context, attributes, data, defaultIntent, notificationBuilder)
        val id = (Date().time / 1000L % Integer.MAX_VALUE).toInt()
        notificationManager.notify(NOTIFICATION_TAG, id, notification)

        Logger.d(LOG_TAG, "Notified notification: $notification")
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun buildNotification(
        context: Context,
        attributes: KarteAttributes,
        data: Map<String, String>,
        defaultIntent: Intent?,
        builder: NotificationCompat.Builder
    ): Notification {
        val pendingIntent = getPendingIntent(context, attributes, data, defaultIntent)
        builder.setAutoCancel(true)
            .setContentTitle(attributes.title)
            .setContentText(attributes.body)
            .setContentIntent(pendingIntent)

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
            builder.setColor(ContextCompat.getColor(context, bundle.getInt(META_DATA_COLOR_KEY)))
        }

        if (attributes.sound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        val bigPicture: Bitmap? = if (attributes.type == ATTACHMENT_TYPE_IMAGE && attributes.fileUrl != "") {
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

        return builder.build()
    }

    private fun getPendingIntent(
        context: Context,
        attributes: KarteAttributes,
        data: Map<String, String>,
        defaultIntent: Intent?
    ): PendingIntent? {
        var defaultIntent = defaultIntent
        val packageManager = context.packageManager
        if (defaultIntent == null) {
            // CATEGORY_INFO, CATEGORY_LAUNCHERに該当するActivityがない場合はnull
            defaultIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        }

        var intent = defaultIntent
        if (attributes.link.isNotEmpty()) {
            val uri = Uri.parse(attributes.link)
            intent = Notifications.self?.app?.executeCommand(uri)?.filterIsInstance<Intent>()?.firstOrNull()
            if (intent == null) {
                intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) == null) {
                    Logger.w(
                        LOG_TAG,
                        "Cannot resolve specified link. Trying to use default Activity."
                    )
                    intent = defaultIntent
                }
            }
        }

        if (intent == null) {
            Logger.w(LOG_TAG, "No Activity to launch was found.")
            return null
        }
        // MessageReceiverでBroadcastを受けるためにsetClassする。
        // intentに元々ComponentNameがセットされている場合はMessageReceiverで復元できるようにextraに入れておく。
        val componentName = intent.component
        if (componentName != null) {
            intent.putExtra(EXTRA_COMPONENT_NAME, componentName.flattenToString())
        }
        intent.setClass(context, MessageReceiver::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        copyInfoToIntent(data, intent)

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
    }

    private fun supportLibHasNotificationChannelMethod(): Boolean {
        return try {
            NotificationCompat.Builder::class.java!!.getConstructor(
                Context::class.java,
                String::class.java
            )
            true
        } catch (e: NoSuchMethodException) {
            false
        }
    }

    private fun createDefaultChannel(notificationManager: NotificationManager) {
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
        notificationManager.createNotificationChannel(channel)
    }

    private fun channelExists(
        channelId: String,
        notificationManager: NotificationManager
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }
        var exists = false
        for (notificationChannel in notificationManager.notificationChannels) {
            if (notificationChannel.id == channelId) {
                exists = true
                break
            }
        }
        return exists
    }

    /**
     * プッシュ通知の開封イベントの送信に必要なパラメータを[Intent]にコピーします。
     *
     * @param[data] 通知データ
     * @param[intent] [Intent]
     *
     * 通知メッセージをクリックした際に開始するアクティビティの情報
     */
    @JvmStatic
    fun copyInfoToIntent(data: Map<String, String>?, intent: Intent) {
        Logger.d(LOG_TAG, "copyInfoToIntent() data: $data, intent: $intent")

        if (data == null) return

        val isTargetPush = data[KARTE_PUSH_NOTIFICATION_FLAG]
        val isMassPush = data[KARTE_MASS_PUSH_NOTIFICATION_FLAG]
        val eventValues = data[EXTRA_EVENT_VALUES]

        if (isTargetPush == "true") {
            intent.putExtra(KARTE_PUSH_NOTIFICATION_FLAG, isTargetPush)
            intent.putExtra(EXTRA_CAMPAIGN_ID, data[EXTRA_CAMPAIGN_ID])
            intent.putExtra(EXTRA_SHORTEN_ID, data[EXTRA_SHORTEN_ID])
            intent.putExtra(EXTRA_EVENT_VALUES, eventValues)
        } else if (isMassPush == "true") {
            intent.putExtra(KARTE_MASS_PUSH_NOTIFICATION_FLAG, isMassPush)
            intent.putExtra(EXTRA_EVENT_VALUES, eventValues)
        }
    }
}