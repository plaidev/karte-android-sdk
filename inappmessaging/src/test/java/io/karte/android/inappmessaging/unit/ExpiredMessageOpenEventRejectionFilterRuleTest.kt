package io.karte.android.inappmessaging.unit

import io.karte.android.inappmessaging.internal.ExpiredMessageOpenEventRejectionFilterRule
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.queue.TrackEventRejectionFilterRule
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExpiredMessageOpenEventRejectionFilterRuleTest {
    private lateinit var now: Date
    private lateinit var rule: TrackEventRejectionFilterRule
    private fun runTest(responseTimestamp: Date): Boolean {
        val event = MessageEvent(MessageEventType.Open, "cid", "sid", mapOf(
            "message" to mapOf(
                "response_timestamp" to ExpiredMessageOpenEventRejectionFilterRule.dateFormatter.format(responseTimestamp)
            )
        ))
        return rule.reject(event)
    }

    @Before
    fun setup() {
        now = Date()
        rule = ExpiredMessageOpenEventRejectionFilterRule(180) { now }
    }

    @Test
    fun checkExpiredEvent() {
        Assert.assertTrue(runTest(Date(now.time - 181 * 1000)))
    }

    @Test
    fun checkNotExpiredEvent() {
        Assert.assertFalse(runTest(Date(now.time - 180 * 1000)))
    }
}
