package io.karte.android.inbox.internal.apis

import io.karte.android.inbox.models.InboxMessage
import org.json.JSONObject

internal sealed class ResponseParser<T> {
    internal abstract fun parse(rawResponse: JSONObject): T

    object FetchMessagesParser : ResponseParser<FetchMessagesResponse>() {
        override fun parse(rawResponse: JSONObject): FetchMessagesResponse {
            val messages = mutableListOf<InboxMessage>()
            rawResponse.optJSONArray("messages")?.let {
                for (i in 0 until it.length()) {
                    it.optJSONObject(i)?.let { m ->
                        val message = InboxMessage.fromJsonObject(m)
                        messages.add(message)
                    }
                }
            }
            return messages
        }
    }

    object OpenMessagesParser : ResponseParser<SuccessResponse>() {
        override fun parse(rawResponse: JSONObject): SuccessResponse {
            val isSuccess = rawResponse.optBoolean("success")
            return SuccessResponse(isSuccess)
        }
    }
}

private typealias FetchMessagesResponse = List<InboxMessage>?

internal class SuccessResponse(val success: Boolean)
