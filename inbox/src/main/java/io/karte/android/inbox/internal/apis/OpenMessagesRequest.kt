package io.karte.android.inbox.internal.apis

import io.karte.android.inbox.internal.Config
import org.json.JSONArray
import org.json.JSONObject

internal class OpenMessagesRequest(
    override val apiKey: String,
    val visitorId: String,
    val messageIds: List<String>,
    override val config: Config
) : BaseApiRequest {
    override val method = HttpMethod.POST
    override val version = "v2native"
    override val path = "inbox/openMessages"
    override val header = mapOf(
        "Content-type" to "application/json; charset=utf-8",
        "X-KARTE-Api-key" to apiKey
    )

    override val body = JSONObject().apply {
        put("visitorId", visitorId)
        put("appType", "native_app")
        put("os", "android")
        put("messageIds", JSONArray(messageIds))
    }
}
