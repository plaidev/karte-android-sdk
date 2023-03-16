package io.karte.android.inbox.integration

import android.os.Handler
import io.karte.android.inbox.internal.Config
import io.karte.android.inbox.internal.apis.InboxClient
import io.karte.android.inbox.internal.apis.InboxClientImpl
import io.karte.android.inbox.models.InboxMessage

internal class TestInboxClient(baseUrl: String, override val apiKey: String) : InboxClient {
    private var client: InboxClient
    private var config: Config

    init {
        this.config = object : Config {
            override val baseUrl: String = baseUrl
        }
        this.client = InboxClientImpl(apiKey, config)
    }

    override suspend fun fetchMessages(userId: String, limit: Int?, latestMessageId: String?): List<InboxMessage>? {
        return client.fetchMessages(userId, limit, latestMessageId)
    }

    override fun fetchMessagesAsync(
        userId: String,
        limit: Int?,
        latestMessageId: String?,
        handler: Handler?,
        callback: (List<InboxMessage>?) -> Unit
    ) {
        client.fetchMessagesAsync(userId, limit, latestMessageId, handler, callback)
    }
}

internal val dummyRawResponse = """
    {
        "messages": [
            {
                "attachmentUrl": "",
                "body": "body1",
                "campaignId": "dummy_campaignId_1",
                "linkUrl": "",
                "messageId": "dummy_messageId_1",
                "timestamp": 1674015317,
                "title": "title1"
            },
            {
                "attachmentUrl": "",
                "body": "body2",
                "campaignId": "dummy_campaignId_2",
                "linkUrl": "",
                "messageId": "dummy_messageId_2",
                "timestamp": 1672919717,
                "title": "title2"
            }
        ]
    }
""".trimIndent()
