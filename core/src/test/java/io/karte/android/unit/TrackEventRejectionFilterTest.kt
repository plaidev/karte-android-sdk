package io.karte.android.unit

import io.karte.android.tracking.CustomEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.queue.TrackEventRejectionFilter
import io.karte.android.tracking.queue.TrackEventRejectionFilterRule
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackEventRejectionFilterTest {
    private lateinit var filter: TrackEventRejectionFilter

    @Before
    fun setUp() {
        filter = TrackEventRejectionFilter().apply {
            add(TestRejectionRule("m1", CustomEventName("e1"), "v1"))
        }
    }

    @Test
    fun testFilter() {
        val e1 = Event(CustomEventName("e1"), mapOf("f1" to "v1"), libraryName = "m1")
        Assert.assertFalse(filter.reject(e1))

        val e2 = Event(CustomEventName("e1"), mapOf("f1" to "v2"), libraryName = "m1")
        Assert.assertTrue(filter.reject(e2))

        val e3 = Event(CustomEventName("e1"), mapOf("f1" to "v1"), libraryName = "m2")
        Assert.assertFalse(filter.reject(e3))

        val e4 = Event(CustomEventName("e2"), mapOf("f1" to "v1"), libraryName = "m1")
        Assert.assertFalse(filter.reject(e4))
    }

    class TestRejectionRule(
        override var libraryName: String,
        override var eventName: EventName,
        var value: String
    ) : TrackEventRejectionFilterRule {
        override fun reject(event: Event): Boolean {
            return event.values.optString("f1") != value
        }
    }
}
