package io.karte.android.inbox.internal.apis

import io.karte.android.inbox.internal.Config
import org.json.JSONObject

internal class FetchMessagesRequest(
    override val apiKey: String,
    val userId: String,
    val limit: Int? = null,
    val latestMessageId: String? = null,
    override val config: Config
) : BaseApiRequest {
    override val method = HttpMethod.POST
    override val version = "v2native"
    override val path = "inbox/fetchMessages"
    override val header = mapOf(
        "Content-type" to "application/json; charset=utf-8",
        "X-KARTE-Api-key" to apiKey
    )

    override val body = JSONObject().apply {
        put("userId", userId)
        put("appType", "native_app")
        put("os", "android")
        limit?.let { put("limit", it) }
        latestMessageId?.let { put("latestMessageId", it) }
    }
}
