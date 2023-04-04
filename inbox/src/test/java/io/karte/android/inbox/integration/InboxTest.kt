package io.karte.android.inbox.integration

import com.google.common.truth.Truth.assertThat
import io.karte.android.inbox.internal.apis.InboxClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public class InboxTest {
    private lateinit var server: MockWebServer
    private lateinit var client: InboxClient
    private val dummyApiKey = "dummy_api_key"

    @Before
    public fun setup() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("").toString().dropLast(1) // Remove trailing slash
        client = TestInboxClient(baseUrl, dummyApiKey)
    }

    @Test
    public fun testInboxClientShouldReturnValidResponse(): Unit = runBlocking {
        server.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(dummyRawResponse)
        })

        val dummyVisitorId = "dummy_visitor"
        val res = client.fetchMessages(dummyVisitorId, null, null)
        assertThat(res?.count()).isEqualTo(2)

        val m1 = res!![0]
        assertThat(m1.timestamp).isNotNull()
        assertThat(m1.title).isEqualTo("title1")
        assertThat(m1.body).isEqualTo("body1")
        assertThat(m1.linkUrl).isEmpty()
        assertThat(m1.attachmentUrl).isEmpty()
        assertThat(m1.campaignId).isEqualTo("dummy_campaignId_1")
        assertThat(m1.messageId).isEqualTo("dummy_messageId_1")
        assertThat(m1.isRead).isTrue()

        val m2 = res[1]
        assertThat(m2.timestamp).isNotNull()
        assertThat(m2.title).isEqualTo("title2")
        assertThat(m2.body).isEqualTo("body2")
        assertThat(m2.linkUrl).isEmpty()
        assertThat(m2.attachmentUrl).isEmpty()
        assertThat(m2.campaignId).isEqualTo("dummy_campaignId_2")
        assertThat(m2.messageId).isEqualTo("dummy_messageId_2")
        assertThat(m2.isRead).isFalse()
    }

    @Test
    public fun testCustomPayloadShouldBeParsedProperly(): Unit = runBlocking {
        server.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(dummyRawResponse)
        })

        val dummyVisitorId = "dummy_visitor"
        val res = client.fetchMessages(dummyVisitorId, null, null)
        assertThat(res?.count()).isEqualTo(2)

        val m1 = res!![0]
        assertThat(m1.customPayload["keyStr"]).isEqualTo("Dummy")
        assertThat(m1.customPayload["keyInt"]).isEqualTo(10)
        assertThat(m1.customPayload["keyDouble"]).isEqualTo(1.11)
        assertThat(m1.customPayload["keyArray"]).isEqualTo(listOf(1, 2, 3))
        assertThat(m1.customPayload["keyMap"]).isEqualTo(mapOf("prop1" to "hoge", "prop2" to 0))
        assertThat(m1.customPayload["keyNull"]).isNull()

        val m2 = res[1]
        assertThat(m2.customPayload.isEmpty()).isTrue()
    }

    @Test
    public fun testVisitorIdWillBeSetToRequestBody(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val dummyVisitorId = "dummy_visitor"
        client.fetchMessages(dummyVisitorId, null, null)

        val recorded = server.takeRequest()
        val requestedVisitorId = JSONObject(recorded.body.readUtf8()).optString("visitorId")
        assertThat(requestedVisitorId).isEqualTo(dummyVisitorId)
    }

    @Test
    public fun testApiKeyWillBeSetToRequestHeader(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        client.fetchMessages("", null, null)
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("X-KARTE-Api-key")).isEqualTo(dummyApiKey)
    }

    @Test
    public fun testInboxClientShouldReturnNullIfResponseCodeIs400(): Unit = runBlocking {
        server.enqueue(MockResponse().apply {
            val errorMessage = JSONObject().apply { put("error", "Bad request") }
            setResponseCode(400)
            setBody(errorMessage.toString())
        })
        val res = client.fetchMessages("", null, null)
        assertThat(res).isNull()
    }

    @Test
    public fun testInboxClientShouldReturnNullIfResponseCodeIs401(): Unit = runBlocking {
        server.enqueue(MockResponse().apply {
            val errorMessage = JSONObject().apply { put("error", "Can't found project") }
            setResponseCode(401)
            setBody(errorMessage.toString())
        })
        val res = client.fetchMessages("", null, null)
        assertThat(res).isNull()
    }

    @Test
    public fun testInboxClientShouldReturnNullIfResponseCodeIs500(): Unit = runBlocking {
        server.enqueue(MockResponse().apply {
            val errorMessage = JSONObject().apply { put("error", "Server error") }
            setResponseCode(500)
            setBody(errorMessage.toString())
        })
        val res = client.fetchMessages("", null, null)
        assertThat(res).isNull()
    }

    @Test
    public fun testFetchMessagesAsyncShouldRunCallbackOnBackgroundThreadWithoutHandlerParameter() {
        val latch = CountDownLatch(1)
        server.enqueue(MockResponse().setResponseCode(200))

        lateinit var workerThreadName: String
        client.fetchMessagesAsync("", null, null, null) {
            workerThreadName = Thread.currentThread().name
            latch.countDown()
        }
        latch.await(1000L, TimeUnit.MILLISECONDS)
        assertThat(workerThreadName).isNotEqualTo(Thread.currentThread().name)
    }

    @Test
    public fun testOpenMessagesAsyncShouldRunCallbackOnBackgroundThreadWithoutHandlerParameter() {
        val latch = CountDownLatch(1)
        server.enqueue(MockResponse().setResponseCode(200))

        lateinit var workerThreadName: String
        client.openMessagesAsync("", listOf(), null) {
            workerThreadName = Thread.currentThread().name
            latch.countDown()
        }
        latch.await(1000L, TimeUnit.MILLISECONDS)
        assertThat(workerThreadName).isNotEqualTo(Thread.currentThread().name)
    }

    @After
    public fun tearDown() {
        server.shutdown()
    }
}
