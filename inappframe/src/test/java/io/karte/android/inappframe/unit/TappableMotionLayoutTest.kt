package io.karte.android.inappframe.unit

import android.content.Context
import android.view.MotionEvent
import io.karte.android.inappframe.model.EpochMillis
import io.karte.android.inappframe.components.shared.TappableMotionLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class TappableMotionLayoutTest {

    private lateinit var tappableMotionLayout: TappableMotionLayout
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)

        tappableMotionLayout = spyk(
            object : TappableMotionLayout(context) {
                override fun onClickAction() {
                }
            },
            recordPrivateCalls = true
        )

        // onInterceptTouchEvent() が実際に呼ばれるように設定
        every { tappableMotionLayout.onInterceptTouchEvent(any()) } answers { callOriginal() }
    }

    @Test
    fun `ACTION_UP で条件を満たす場合 onClickAction() が呼ばれる`() {
        val eventDown = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_DOWN
            every { x } returns 100f
            every { y } returns 200f
        }

        val eventUp = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_UP
            // 閾値10px以内
            every { x } returns 105f
            every { y } returns 205f
        }

        mockkObject(EpochMillis)
        every { EpochMillis.now() } returnsMany listOf(1000L, 1200L)

        // タッチイベント発火
        tappableMotionLayout.onInterceptTouchEvent(eventDown)
        tappableMotionLayout.onInterceptTouchEvent(eventUp)

        // protected なメソッド呼び出しを検証 → invokeNoArgs("onClickAction") を使用
        verify(exactly = 1) {
            tappableMotionLayout invokeNoArgs "onClickAction"
        }
    }

    @Test
    fun `ACTION_UP でタップ閾値を超える場合 onClickAction() が呼ばれない`() {
        val eventDown = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_DOWN
            every { x } returns 100f
            every { y } returns 200f
        }

        val eventUp = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_UP
            // 閾値10px超え
            every { x } returns 120f
            every { y } returns 220f
        }

        // 時間は問題なく 200ms で済んでも、移動距離が大きい
        mockkObject(EpochMillis)
        every { EpochMillis.now() } returnsMany listOf(1000L, 1200L)

        tappableMotionLayout.onInterceptTouchEvent(eventDown)
        tappableMotionLayout.onInterceptTouchEvent(eventUp)

        // onClickAction() が呼ばれていないことを検証
        verify(exactly = 0) {
            tappableMotionLayout invokeNoArgs "onClickAction"
        }
    }

    @Test
    fun `ACTION_UP でタップ時間が長すぎる場合 onClickAction() が呼ばれない`() {
        val eventDown = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_DOWN
            every { x } returns 100f
            every { y } returns 200f
        }

        val eventUp = mockk<MotionEvent> {
            every { action } returns MotionEvent.ACTION_UP
            // しきい値10px以内
            every { x } returns 102f
            every { y } returns 202f
        }

        // 1回目=1000L, 2回目=2000L (1000ms経過) → しきい値500ms超え
        mockkObject(EpochMillis)
        every { EpochMillis.now() } returnsMany listOf(1000L, 2000L)

        tappableMotionLayout.onInterceptTouchEvent(eventDown)
        tappableMotionLayout.onInterceptTouchEvent(eventUp)

        // 時間オーバーでタップ判定されない
        verify(exactly = 0) {
            tappableMotionLayout invokeNoArgs "onClickAction"
        }
    }
}
