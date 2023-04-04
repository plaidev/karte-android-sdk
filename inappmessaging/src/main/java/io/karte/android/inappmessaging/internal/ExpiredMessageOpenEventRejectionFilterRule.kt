package io.karte.android.inappmessaging.internal

import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.MessageEventName
import io.karte.android.tracking.queue.TrackEventRejectionFilterRule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class ExpiredMessageOpenEventRejectionFilterRule(
    private val interval: Int = 180,
    private val dateResolver: () -> Date = { Date() }
) : TrackEventRejectionFilterRule {
    companion object {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
            this.timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    override var libraryName: String = InAppMessaging.name
    override var eventName: EventName = MessageEventName.MessageOpen

    override fun reject(event: Event): Boolean {
        val responseTimestamp = event.values.optJSONObject("message")?.optString("response_timestamp") ?: ""
        if (responseTimestamp.isEmpty()) {
            return false
        }

        runCatching {
            dateFormatter.parse(responseTimestamp) ?: return true
        }.fold(
            onSuccess = {
                // response_timestamp has elapsed 3 minutes
                return dateResolver().time / 1000 - interval - it.time / 1000 > 0
            },
            onFailure = {
                return false
            }
        )
    }
}
