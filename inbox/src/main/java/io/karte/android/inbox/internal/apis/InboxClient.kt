package io.karte.android.inbox.internal.apis

import android.os.Handler
import io.karte.android.inbox.internal.Config
import io.karte.android.inbox.internal.ProductionConfig
import io.karte.android.inbox.models.InboxMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

internal interface InboxClient {
    val apiKey: String

    suspend fun fetchMessages(userId: String, limit: Int?, latestMessageId: String?): List<InboxMessage>?
    fun fetchMessagesAsync(
        userId: String,
        limit: Int?,
        latestMessageId: String?,
        handler: Handler?,
        callback: (List<InboxMessage>?) -> Unit
    )
}

internal class InboxClientImpl(override val apiKey: String, private val config: Config = ProductionConfig()) : InboxClient {
    private val executorService = Executors.newCachedThreadPool()

    override suspend fun fetchMessages(userId: String, limit: Int?, latestMessageId: String?): List<InboxMessage>? {
        return withContext(Dispatchers.IO) {
            val req = FetchMessagesRequest(apiKey, userId, limit, latestMessageId, config)
            val raw = Call(req).execute()
            raw?.let {
                ResponseParser.FetchMessagesParser.parse(it)
            }
        }
    }

    override fun fetchMessagesAsync(
        userId: String,
        limit: Int?,
        latestMessageId: String?,
        handler: Handler?,
        callback: (List<InboxMessage>?) -> Unit
    ) {
        executorService.execute {
            val req = FetchMessagesRequest(apiKey, userId, limit, latestMessageId, config)
            val raw = Call(req).execute()
            val parsed = raw?.let {
                ResponseParser.FetchMessagesParser.parse(it)
            }

            if (handler != null) {
                handler.post {
                    callback(parsed)
                }
            } else {
                callback(parsed)
            }
        }
    }
}
