package io.karte.android.inbox.models

import org.json.JSONObject
import java.util.Date

/**
 * KARTE経由で送信したPush通知を表すタイプです。
 *
 * @property[timestamp] 送信された時間を返します。
 * @property[title] Push通知のタイトルを返します。
 * @property[body] Push通知の本文を返します。
 * @property[linkUrl] Push通知に設定された遷移先リンクURLを返します。未設定の場合は空文字を返します。
 * @property[attachmentUrl] Push通知に設定された画像URLを返します。未設定の場合は空文字を返します。
 * @property[campaignId] 接客のキャンペーンIDを返します。
 * @property[messageId] Push通知のユニークなIDを返します。
 */
public data class InboxMessage(
    val timestamp: Date,
    val title: String,
    val body: String,
    val linkUrl: String,
    val attachmentUrl: String,
    val campaignId: String,
    val messageId: String
) {
    internal companion object {
        fun fromJsonObject(json: JSONObject): InboxMessage {
            // Convert Unix time(Seconds) to milliseconds since Date takes constructor parameter as milliseconds
            val unixTimeMillis = json.optLong("timestamp") * 1000L
            return InboxMessage(
                timestamp = Date(unixTimeMillis),
                title = json.optString("title"),
                body = json.optString("body"),
                linkUrl = json.optString("linkUrl"),
                attachmentUrl = json.optString("attachmentUrl"),
                campaignId = json.optString("campaignId"),
                messageId = json.optString("messageId")
            )
        }
    }
}
