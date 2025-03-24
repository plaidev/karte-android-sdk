package io.karte.android.inappframe.components.shared

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import io.karte.android.inappframe.model.EpochMillis

internal abstract class TappableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {

    // タップ判定用のフィールド（共通）
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var touchStartTime: Long = 0L

    private val clickThreshold = 10f // ピクセル単位
    private val clickTimeout = 500L // ミリ秒

    // --- 抽象メソッド ---
    // サブクラスごとにタップ時のURLや処理が異なる場合は、abstractで定義し、各サブクラスで実装
    protected abstract fun onClickAction()

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.x
                touchStartY = ev.y
                touchStartTime = EpochMillis.now()
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = Math.abs(ev.x - touchStartX)
                val deltaY = Math.abs(ev.y - touchStartY)
                val deltaTime = EpochMillis.now() - touchStartTime

                // タップと判定する閾値を超えていない場合
                if (deltaX < clickThreshold && deltaY < clickThreshold && deltaTime <= clickTimeout) {
                    onClickAction() // ← サブクラスに処理を委譲
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
