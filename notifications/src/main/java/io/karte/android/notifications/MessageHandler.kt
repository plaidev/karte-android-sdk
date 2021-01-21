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
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.ChannelUtil
import io.karte.android.notifications.internal.IntentAppender
import io.karte.android.notifications.internal.NotificationBuilder
import io.karte.android.notifications.internal.track.ReachedTracker
import io.karte.android.notifications.internal.wrapper.EventType
import io.karte.android.notifications.internal.wrapper.IntentWrapper
import io.karte.android.notifications.internal.wrapper.MessageWrapper
import java.util.Date

private const val LOG_TAG = "Karte.MessageHandler"

internal const val NOTIFICATION_TAG = "krt_notification_tag"

internal fun uniqueId(): Int {
    return (Date().time / 1000L % Integer.MAX_VALUE).toInt()
}

/**
 * KARTEから送信された通知メッセージを処理する機能を提供するクラスです。
 */
class MessageHandler private constructor(val context: Context, val data: Map<String, String>) {
    private val wrapper = MessageWrapper(data)
    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val uniqueId = uniqueId()

    @Throws(PackageManager.NameNotFoundException::class)
    private fun makeNotification(): Notification? {
        val attributes = wrapper.attributes ?: return null
        Logger.d(LOG_TAG, "makeNotification(): $context, attributes: $attributes")

        if (manager == null) {
            Logger.w(LOG_TAG, "Stopped to show notification because NotificationManager is null.")
            return null
        }

        val channelId = ChannelUtil.getChannel(manager, attributes.channel)
        return NotificationBuilder.build(context, channelId, attributes)
    }

    private fun showNotification(notification: Notification?, defaultIntent: Intent?): Boolean {
        ReachedTracker.sendIfNeeded(wrapper)
        val target = notification ?: makeNotification()
        if (target == null || manager == null || wrapper.attributes == null) return false
        IntentAppender.append(target, context, uniqueId, wrapper, defaultIntent)
        manager.notify(NOTIFICATION_TAG, uniqueId, target)
        Logger.d(LOG_TAG, "Notified notification: $target")
        return true
    }

    companion object {

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
            return MessageWrapper(data).isKartePush
        }

        /**
         * [RemoteMessage]からSDKが自動で処理するデータを取り出し、[KarteAttributes]インスタンスを返します。
         *
         * @param message [RemoteMessage]
         * @return [KarteAttributes]インスタンスを返します。
         */
        @JvmStatic
        fun extractKarteAttributes(message: RemoteMessage): KarteAttributes? {
            return extractKarteAttributes(message.data)
        }

        /**
         * データメッセージからSDKが自動で処理するデータを取り出し、[KarteAttributes]インスタンスを返します。
         *
         * @param data [Map]
         * @return [KarteAttributes]インスタンスを返します。
         */
        @JvmStatic
        fun extractKarteAttributes(data: Map<String, String>): KarteAttributes? {
            return MessageWrapper(data).attributes
        }

        /**
         * KARTE経由で送信された通知メッセージから、通知を作成・表示します。
         *
         * @param[context] [Context]
         * @param[message] [RemoteMessage]
         * @param[defaultIntent] [Intent]
         *
         * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
         *
         * @return 処理結果を返します。
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
            return handleMessage(context, message.data, defaultIntent)
        }

        /**
         * KARTE経由で送信された通知メッセージ（データメッセージ）から、通知を作成・表示します。
         *
         * @param[context] [Context]
         * @param[data] [Map]
         * @param[defaultIntent] [Intent]
         *
         * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
         *
         * @return 処理結果を返します。
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
            return try {
                Logger.i(
                    LOG_TAG,
                    "handleMessage() context: $context, defaultIntent: $defaultIntent, data: $data"
                )
                MessageHandler(context, data).showNotification(null, defaultIntent)
            } catch (e: Exception) {
                Logger.e(LOG_TAG, "Failed to show notification. $e", e)
                true
            }
        }

        /**
         * KARTE経由で送信された通知メッセージから、通知を作成・表示します。
         *
         * @param[context] [Context]
         * @param[message] [RemoteMessage]
         * @param[notification] [Notification] カスタマイズした通知オブジェクト。表示をカスタマイズする場合に指定します。
         * @param[defaultIntent] [Intent]
         *
         * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
         *
         * @return 処理結果を返します。
         *
         * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
         */
        @JvmStatic
        @JvmOverloads
        fun handleMessage(
            context: Context,
            message: RemoteMessage,
            notification: Notification?,
            defaultIntent: Intent? = null
        ): Boolean {
            return handleMessage(context, message.data, notification, defaultIntent)
        }

        /**
         * KARTE経由で送信された通知メッセージ（データメッセージ）から、通知を作成・表示します。
         *
         * @param[context] [Context]
         * @param[data] [Map]
         * @param[notification] [Notification] カスタマイズした通知オブジェクト。表示をカスタマイズする場合に指定します。
         * @param[defaultIntent] [Intent]
         *
         * ディープリンクが未指定の場合や、ディープリンクに対応するアクティビティが存在しない場合に、開始するアクティビティの情報。
         *
         * @return 処理結果を返します。
         *
         * KARTE経由で送信された通知メッセージの場合は`true`、KARTE以外から送信された通知メッセージの場合は、`false`を返します。
         */
        @JvmStatic
        @JvmOverloads
        fun handleMessage(
            context: Context,
            data: Map<String, String>,
            notification: Notification?,
            defaultIntent: Intent? = null
        ): Boolean {
            if (!canHandleMessage(data)) {
                return false
            }
            return try {
                Logger.i(
                    LOG_TAG,
                    "handleMessage() context: $context, defaultIntent: $defaultIntent, data: $data"
                )
                MessageHandler(context, data).showNotification(notification, defaultIntent)
            } catch (e: Exception) {
                Logger.e(LOG_TAG, "Failed to show notification. $e", e)
                true
            }
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
        @Deprecated(
            "This method has restrictions on click measurement. " +
                "If you want to show custom notification, pass Notification instance to " +
                "'handleMessage(Context,Map<String, String>,Notification?,Intent?): Boolean'.",
            ReplaceWith("MessageHandler.handleMessage(context, data, notification, intent)")
        )
        fun copyInfoToIntent(data: Map<String, String>?, intent: Intent) {
            Logger.d(LOG_TAG, "copyInfoToIntent() data: $data, intent: $intent")
            if (data == null) return
            val context = Notifications.self?.app?.application ?: return
            IntentWrapper.wrapIntent(context, MessageWrapper(data), EventType.MESSAGE_CLICK, intent)
        }
    }
}
