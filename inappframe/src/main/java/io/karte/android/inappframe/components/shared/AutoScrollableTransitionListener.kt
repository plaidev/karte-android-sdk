package io.karte.android.inappframe.components.shared

import androidx.constraintlayout.motion.widget.MotionLayout

internal open class AutoScrollableTransitionListener(
    private val handler: android.os.Handler,
    private val autoScrollRunnable: java.lang.Runnable,
    private val autoPlaySpeed: Double?,
    private val userInteracted: Boolean
) : MotionLayout.TransitionListener, AutoSpeedConfigurable {
    override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
        if (userInteracted) {
            return
        }
        // ユーザが操作していなければ次の自動スクロールをセット
        getTransitionStopMillis(autoPlaySpeed)?.run {
            handler.postDelayed(autoScrollRunnable, this)
        }
    }

    override fun onTransitionChange(
        motionLayout: MotionLayout,
        startId: Int,
        endId: Int,
        progress: Float
    ) {}

    override fun onTransitionStarted(
        motionLayout: MotionLayout,
        startId: Int,
        endId: Int
    ) {
        handler.removeCallbacks(autoScrollRunnable)
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {}
}
