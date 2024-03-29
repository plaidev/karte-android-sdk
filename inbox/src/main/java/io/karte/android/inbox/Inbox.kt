package io.karte.android.inbox

import android.os.Handler
import io.karte.android.KarteApp
import io.karte.android.core.library.Library
import io.karte.android.inbox.internal.apis.InboxClient
import io.karte.android.inbox.internal.apis.InboxClientFactory
import io.karte.android.inbox.models.InboxMessage

/**
 * KARTEから送信したPush通知の履歴を取得するクラスです。
 */
public class Inbox : Library {
    public companion object {
        internal var apiKey: String = ""
        private val client: InboxClient by InboxClientFactory()

        /**
         * Push通知の送信履歴を取得するSuspend関数です。エラー発生時はnullを返します。
         *
         * @param limit 最大取得件数を指定します。デフォルトは最新50件を取得します。
         * @param latestMessageId この値で指定されたmessageIdより前の履歴を取得します。指定したmessageIdを持つ履歴は戻り値に含まれません。
         *
         * @return 引数で指定した条件に合致するPush通知送信履歴のリストを返します。エラー発生時はnullを返します。
         */
        public suspend fun fetchMessages(limit: Int? = null, latestMessageId: String? = null): List<InboxMessage>? {
            val visitorId = KarteApp.visitorId
            return client.fetchMessages(visitorId, limit, latestMessageId)
        }

        /**
         * Push通知の送信履歴を取得する非同期関数です。通信処理はバックグラウンドで実行され、handlerで指定したスレッドでコールバックが実行されます。
         *
         * @param limit 最大取得件数を指定します。デフォルトは最新50件を取得します。
         * @param latestMessageId この値で指定されたmessageIdより前の履歴を取得します。指定したmessageIdを持つ履歴は戻り値に含まれません。
         * @param handler callbackが実行されるスレッドを指定します。指定しない場合は通信処理を行ったスレッドと同じスレッドでcallbackが実行されます。
         * @param callback 通信処理の実行結果を受け取って任意の処理を実行するコールバック関数を指定します。エラー発生時はnullが渡されます。
         */
        public fun fetchMessagesAsync(
            limit: Int? = null,
            latestMessageId: String? = null,
            handler: Handler? = null,
            callback: (response: List<InboxMessage>?) -> Unit
        ) {
            val visitorId = KarteApp.visitorId
            client.fetchMessagesAsync(visitorId, limit, latestMessageId, handler, callback)
        }

        /**
         * Push通知を指定して既読状態にします。
         *
         * @param messageIds 既読状態にする対象のメッセージIDの配列。
         *
         * @return リクエスト成功時はtrue, エラー発生時はfalseを返します。
         */
        public suspend fun openMessages(messageIds: List<String>): Boolean {
            val visitorId = KarteApp.visitorId
            return client.openMessages(visitorId, messageIds)
        }

        /**
         * Push通知を指定して既読状態にする非同期関数です。通信処理はバックグラウンドで実行され、handlerで指定したスレッドでコールバックが実行されます。
         *
         * @param messageIds 既読状態にする対象のメッセージIDの配列。
         * @param handler callbackが実行されるスレッドを指定します。指定しない場合は通信処理を行ったスレッドと同じスレッドでcallbackが実行されます。
         * @param callback 通信処理の実行結果を受け取って任意の処理を実行するコールバック関数を指定します。エラー発生時はfalseが渡されます。
         */
        public fun openMessagesAsync(messageIds: List<String>, handler: Handler?, callback: (Boolean) -> Unit) {
            val visitorId = KarteApp.visitorId
            client.openMessagesAsync(visitorId, messageIds, handler, callback)
        }
    }

    override val name: String = "inbox"
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        apiKey = app.config.apiKey
    }

    override fun unconfigure(app: KarteApp) {}
}
