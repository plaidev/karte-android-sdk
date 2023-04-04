package io.karte.android.inbox.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.inbox.models.InboxMessage
import org.json.JSONObject
import org.junit.Test
import java.util.Date

public class ModelTest {
    @Test
    public fun testInboxMessageCanBeInitializedFromJSONObject() {
        val json = InboxMessage.makeDummyJSON()
        val message = InboxMessage.fromJsonObject(json)

        // Convert epoch seconds to milliseconds
        val dateFromMillis = Date(1674015317 * 1000L)
        assertThat(message.timestamp).isEqualTo(dateFromMillis)
        assertThat(message.title).isEqualTo("Dummy title")
        assertThat(message.body).isEqualTo("Dummy body")
        assertThat(message.linkUrl).isEqualTo("Dummy link")
        assertThat(message.attachmentUrl).isEqualTo("Dummy attachment url")
        assertThat(message.campaignId).isEqualTo("Dummy campaign id")
        assertThat(message.messageId).isEqualTo("Dummy message id")
        assertThat(message.isRead).isTrue()

        assertThat(message.customPayload["keyStr"]).isEqualTo("Dummy")
        assertThat(message.customPayload["keyInt"]).isEqualTo(10)
        assertThat(message.customPayload["keyDouble"]).isEqualTo(1.11)
        assertThat(message.customPayload["keyArray"]).isEqualTo(listOf(1, 2, 3))
        assertThat(message.customPayload["keyMap"]).isEqualTo(mapOf("prop1" to "hoge", "prop2" to 0))
        assertThat(message.customPayload["keyNull"]).isNull()
    }

    @Test
    public fun testInboxMessageCanBeInitializedFromEmptyJSONObject() {
        val json = JSONObject()
        val message = InboxMessage.fromJsonObject(json)
        assertThat(message.timestamp).isEqualTo(Date(0))
        assertThat(message.title).isEmpty()
        assertThat(message.body).isEmpty()
        assertThat(message.linkUrl).isEmpty()
        assertThat(message.attachmentUrl).isEmpty()
        assertThat(message.campaignId).isEmpty()
        assertThat(message.messageId).isEmpty()
        assertThat(message.isRead).isFalse()
        assertThat(message.customPayload.isEmpty()).isTrue()
    }
}

internal fun InboxMessage.Companion.makeDummyJSON(): JSONObject {
    return JSONObject().apply {
        put("timestamp", 1674015317)
        put("title", "Dummy title")
        put("body", "Dummy body")
        put("linkUrl", "Dummy link")
        put("attachmentUrl", "Dummy attachment url")
        put("campaignId", "Dummy campaign id")
        put("messageId", "Dummy message id")
        put("isRead", true)

        val payload = """
            {
                "keyStr": "Dummy",
                "keyInt": 10,
                "keyDouble": 1.11,
                "keyArray": [1, 2, 3],
                "keyMap": {
                    "prop1": "hoge",
                    "prop2": 0
                },
                "keyNull": null
            }
        """.trimIndent()
        put("customPayload", JSONObject(payload))
    }
}
