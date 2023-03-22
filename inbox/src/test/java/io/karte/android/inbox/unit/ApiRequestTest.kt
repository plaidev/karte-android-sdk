package io.karte.android.inbox.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.inbox.internal.Config
import io.karte.android.inbox.internal.ProductionConfig
import io.karte.android.inbox.internal.apis.FetchMessagesRequest
import io.karte.android.inbox.internal.apis.OpenMessagesRequest
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
        val dummyVisitorId = "dummy_visitor_id"
        val req = FetchMessagesRequest("", dummyVisitorId, config = dummyConfig)
        val body = JSONObject().apply {
            put("visitorId", dummyVisitorId)
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
            put("visitorId", "")
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

    @Test
    public fun testOpenMessagesRequestHasProperUrlWithProductionConfig() {
        val req = OpenMessagesRequest("", "", listOf(), config = ProductionConfig())
        val url = URL("https://api.karte.io/v2native/inbox/openMessages")
        assertThat(req.url).isEqualTo(url)
    }

    @Test
    public fun testOpenMessagesRequestHasProperBodyWithParameters() {
        val dummyVisitorId = "dummy_visitor_id"
        val ids = listOf("aaa", "bbb", "ccc")
        val req = OpenMessagesRequest("", dummyVisitorId, ids, config = dummyConfig)
        val body = JSONObject().apply {
            put("visitorId", dummyVisitorId)
            put("appType", "native_app")
            put("os", "android")
            put("messageIds", ids)
        }
        assertThat(req.body.toString()).isEqualTo(body.toString())
    }
}
