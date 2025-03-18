package io.karte.android.inappframe.unit

import android.os.Handler
import androidx.constraintlayout.motion.widget.MotionLayout
import io.karte.android.inappframe.components.shared.AutoScrollableTransitionListener
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.every
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import org.junit.Before
import org.junit.Test

class AutoScrollableTransitionListenerTest {

    @MockK
    lateinit var handler: Handler

    @MockK
    lateinit var autoScrollRunnable: Runnable

    @MockK
    lateinit var motionLayout: MotionLayout

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // 毎回テスト実行前にモックの呼び出し履歴をクリア
        clearAllMocks()

        // `postDelayed(...)` や `removeCallbacks(...)` が呼ばれた際の戻り値などを設定
        every { handler.postDelayed(any(), any()) } returns true
        every { handler.removeCallbacks(any<Runnable>()) } returns Unit
    }

    @Test
    fun `onTransitionCompleted - userInteracted=true ならpostDelayedされない`() {
        // テスト対象インスタンス
        val listener = AutoScrollableTransitionListener(
            handler = handler,
            autoScrollRunnable = autoScrollRunnable,
            autoPlaySpeed = 2.0,
            userInteracted = true
        )

        // テスト対象メソッドを直接呼び出す
        listener.onTransitionCompleted(motionLayout, currentId = 0)

        // userInteracted=true の場合は postDelayed が呼ばれないことを確認
        verify(exactly = 0) { handler.postDelayed(any(), any()) }

        confirmVerified(handler)
    }

    @Test
    fun `onTransitionCompleted - userInteracted=false ならpostDelayedされる`() {
        // テスト対象インスタンス
        val listener = AutoScrollableTransitionListener(
            handler = handler,
            autoScrollRunnable = autoScrollRunnable,
            autoPlaySpeed = 2.0,
            userInteracted = false
        )

        listener.onTransitionCompleted(motionLayout, currentId = 0)

        // postDelayed が呼ばれたことを検証
        // autoPlaySpeed=2.0 なので 2.0*1000で　postDelayedの第2引数は 2000L になる
        verify(exactly = 1) { handler.postDelayed(autoScrollRunnable, eq(2000L)) }

        confirmVerified(handler)
    }

    @Test
    fun `onTransitionStarted で removeCallbacks が呼ばれる`() {
        val listener = AutoScrollableTransitionListener(
            handler = handler,
            autoScrollRunnable = autoScrollRunnable,
            autoPlaySpeed = 2.0,
            userInteracted = false
        )

        listener.onTransitionStarted(motionLayout, startId = 0, endId = 1)

        // removeCallbacks が呼ばれたことを検証
        verify(exactly = 1) { handler.removeCallbacks(autoScrollRunnable) }

        confirmVerified(handler)
    }

    @Test
    fun `onTransitionChange と onTransitionTrigger は何もしない`() {
        val listener = AutoScrollableTransitionListener(
            handler = handler,
            autoScrollRunnable = autoScrollRunnable,
            autoPlaySpeed = 2.0,
            userInteracted = false
        )

        // 呼び出しても何もしない(例外が出ない)ことを確認する程度
        listener.onTransitionChange(motionLayout, 0, 1, 0.5f)
        listener.onTransitionTrigger(motionLayout, 0, true, 0.5f)

        // 特にハンドラの呼び出しが発生しない
        verify(exactly = 0) { handler.postDelayed(any(), any()) }
        verify(exactly = 0) { handler.removeCallbacks(any<Runnable>()) }

        confirmVerified(handler)
    }
}
