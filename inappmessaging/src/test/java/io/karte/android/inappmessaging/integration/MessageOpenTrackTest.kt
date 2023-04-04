package io.karte.android.inappmessaging.integration

import com.google.common.truth.Truth
import io.karte.android.test_lib.integration.TrackerTestCase
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.proceedBufferedCall
import io.karte.android.test_lib.toList
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import org.json.JSONObject
import org.junit.Test
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MessageOpenTrackTest : TrackerTestCase() {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
        this.timeZone = java.util.TimeZone.getTimeZone("GMT")
    }

    @Test
    fun originがinappmessagingで有効期限切れのmessage_openは送信されないこと() {
        enqueueSuccessResponse()
        Tracker.track(MessageEvent(MessageEventType.Open, "cid", "sid", mapOf(
            "message" to mapOf(
                "response_timestamp" to dateFormatter.format(Date(0))
            )
        ), "inappmessaging"))
        proceedBufferedCall()

        val request = server.takeRequest()
        val events = JSONObject(request.parseBody()).getJSONArray("events").toList()
        Truth.assertWithMessage("有効期限切れのmessage_openは送信されないこと")
            .that(events.size)
            .isEqualTo(0)
    }

    @Test
    fun originがinappmessagingで有効期限切れではないmessage_openは送信されること() {
        enqueueSuccessResponse()
        Tracker.track(MessageEvent(MessageEventType.Open, "cid", "sid", mapOf(
            "message" to mapOf(
                "response_timestamp" to dateFormatter.format(Date())
            )
        ), "inappmessaging"))
        proceedBufferedCall()

        val request = server.takeRequest()
        val events = JSONObject(request.parseBody()).getJSONArray("events").toList()
        Truth.assertWithMessage("有効期限切れではないmessage_openは送信されること")
            .that(events.size)
            .isEqualTo(1)
    }

    @Test
    fun originがinappmessaging以外のmessage_openは送信されること1() {
        enqueueSuccessResponse()
        Tracker.track(MessageEvent(MessageEventType.Open, "cid", "sid", mapOf(
            "message" to mapOf(
                "response_timestamp" to dateFormatter.format(Date(0))
            )
        )))
        proceedBufferedCall()

        val request = server.takeRequest()
        val events = JSONObject(request.parseBody()).getJSONArray("events").toList()
        Truth.assertWithMessage("message_openは送信されること")
            .that(events.size)
            .isEqualTo(1)
    }

    @Test
    fun originがinappmessaging以外のmessage_openは送信されること2() {
        enqueueSuccessResponse()
        Tracker.track(MessageEvent(MessageEventType.Open, "cid", "sid", mapOf(
            "message" to mapOf(
                "response_timestamp" to dateFormatter.format(Date())
            )
        )))
        proceedBufferedCall()

        val request = server.takeRequest()
        val events = JSONObject(request.parseBody()).getJSONArray("events").toList()
        Truth.assertWithMessage("message_openは送信されること")
            .that(events.size)
            .isEqualTo(1)
    }
}
