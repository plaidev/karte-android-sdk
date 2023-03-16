package io.karte.android.inbox.internal.apis

import io.karte.android.inbox.internal.Config
import org.json.JSONObject
import java.net.URL

internal interface BaseApiRequest {
    val url: URL
        get() = URL("${config.baseUrl}/$version/$path")

    val hasBody: Boolean
        get() = method == HttpMethod.POST && body != null

    val apiKey: String
    val method: HttpMethod
    val version: String
    val path: String
    val header: Map<String, String>
    val body: JSONObject?
    val config: Config
}
