package io.karte.android.inbox.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.inbox.internal.Config
import io.karte.android.inbox.internal.ProductionConfig
import io.karte.android.inbox.internal.apis.FetchMessagesRequest
import org.json.JSONObject
import org.junit.Test
import java.net.URL

public class ApiRequestTest {
    private val dummyConfig = object : Config {
        override val baseUrl: String = "dummy"
    }

    @Test
    public fun testFetchMessagesRequestHasProperUrlWithProductionConfig() {
        val req = FetchMessagesRequest("", "", config = ProductionConfig())
        val url = URL("https://api.karte.io/v2native/inbox/fetchMessages")
        assertThat(req.url).isEqualTo(url)
    }

    @Test
    public fun testFetchMessagesRequestHasProperBodyWithUserId() {
        val dummyUserId = "dummy_user_id"
        val req = FetchMessagesRequest("", dummyUserId, config = dummyConfig)
        val body = JSONObject().apply {
            put("userId", dummyUserId)
            put("appType", "native_app")
            put("os", "android")
        }
        assertThat(req.body.toString()).isEqualTo(body.toString())
    }

    @Test
    public fun testFetchMessagesRequestHasProperBodyWithOptionalParams() {
        val dummyLimit = 1
        val dummyMessageId = "Dummy"
        val req = FetchMessagesRequest("", "", dummyLimit, dummyMessageId, dummyConfig)
        val body = JSONObject().apply {
            put("userId", "")
            put("appType", "native_app")
            put("os", "android")
            put("limit", dummyLimit)
            put("latestMessageId", dummyMessageId)
        }
        assertThat(req.body.toString()).isEqualTo(body.toString())
    }

    @Test
    public fun testFetchMessagesRequestHasBody() {
        val req = FetchMessagesRequest("", "", config = dummyConfig)
        assertThat(req.hasBody).isTrue()
    }

    @Test
    public fun testFetchMessagesRequestHasProperApiKeyInHeader() {
        val dummyApiKey = "dummy_api_key"
        val req = FetchMessagesRequest(dummyApiKey, "", config = dummyConfig)
        assertThat(req.header["X-KARTE-Api-key"]).isEqualTo(dummyApiKey)
    }
}
