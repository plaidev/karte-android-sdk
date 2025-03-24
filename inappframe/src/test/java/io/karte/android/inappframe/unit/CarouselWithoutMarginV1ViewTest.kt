package io.karte.android.inappframe.unit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import io.karte.android.inappframe.components.CarouselWithoutMarginView
import io.karte.android.inappframe.model.CarouselWithoutMarginV1
import io.karte.android.inappframe.model.CarouselWithoutMarginConfig
import io.karte.android.inappframe.model.CarouselWithoutMarginContent
import io.karte.android.inappframe.model.IAFTracker
import io.karte.android.inappframe.model.IAFVersion
import io.karte.android.inappframe.model.Image
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class CarouselWithoutMarginV1ViewTest {

    private lateinit var context: Context
    private lateinit var mockTracker: IAFTracker

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        // Create mock tracker
        mockTracker = mockk(relaxed = true)
        every { mockTracker.trackOpen() } returns Unit
        every { mockTracker.trackClick(any(), any()) } returns Unit
    }

    @Test
    fun clickOnCarousel_shouldStartBrowserIntent() {
        val config = CarouselWithoutMarginConfig(
            paddingTop = 8,
            paddingBottom = 8,
            ratio = 320,
            radius = 8,
            autoplaySpeed = 1.0,
            templateType = "carouselWithoutMargin"
        )
        val data = listOf(
            Image("https://example.com", null, 0)
        )
        val content = CarouselWithoutMarginContent(
            data = data,
            config = config
        )
        val carousel = CarouselWithoutMarginV1("carouselWithoutMargin", IAFVersion.V1, content)

        // Activityのインスタンスを作成
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val carouselView = CarouselWithoutMarginView(activity, carousel, mockTracker)

        // Initialize view for testing
        carouselView.setupViewForTesting()

        // タップイベントをシミュレート
        val downTime = System.currentTimeMillis()
        val eventTime = downTime + 100

        val downEvent = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN,
            100f, 100f, 0
        )
        val upEvent = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_UP,
            100f, 100f, 0
        )

        try {
            carouselView.onInterceptTouchEvent(downEvent)
            carouselView.onInterceptTouchEvent(upEvent)

            // Intentが正しく発行されたか確認
            val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
            assertNotNull("Intent should be started", startedIntent)
            assertEquals(Intent.ACTION_VIEW, startedIntent.action)
            assertEquals(Uri.parse("https://example.com"), startedIntent.data)
        } finally {
            downEvent.recycle()
            upEvent.recycle()
        }
    }
}
