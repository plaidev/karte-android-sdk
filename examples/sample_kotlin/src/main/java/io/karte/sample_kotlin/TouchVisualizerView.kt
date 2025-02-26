package io.karte.sample_kotlin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import java.util.concurrent.ConcurrentHashMap

/**
 * タッチイベントを視覚的に表示するカスタムビュー
 * dispatchTouchEventをオーバーライドしてタッチ箇所を円で表示します
 */
class TouchVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // タッチポイントを保持するマップ
    private val touchPoints = ConcurrentHashMap<Int, TouchPoint>()
    
    // タッチポイントの描画用ペイント
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        alpha = 128
        style = Paint.STYLE.FILL
    }

    init {
        // 背景を透明に設定
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * タッチイベントをインターセプトして視覚化
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // 新しいタッチポイントを追加
                val touchPoint = TouchPoint(x, y)
                touchPoints[pointerId] = touchPoint
                touchPoint.startAnimation()
            }
            MotionEvent.ACTION_MOVE -> {
                // 既存のポインターすべての位置を更新
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    touchPoints[id]?.apply {
                        this.x = event.getX(i)
                        this.y = event.getY(i)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // タッチポイントをフェードアウト
                touchPoints[pointerId]?.startFadeOut()
            }
        }
        
        invalidate()
        // 子ビューにイベントを渡す
        return super.dispatchTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // すべてのタッチポイントを描画
        for (touchPoint in touchPoints.values) {
            paint.alpha = touchPoint.alpha
            canvas.drawCircle(touchPoint.x, touchPoint.y, touchPoint.radius, paint)
        }
    }

    /**
     * タッチポイントを表すデータクラス
     */
    private inner class TouchPoint(
        var x: Float,
        var y: Float,
        var radius: Float = 0f,
        var alpha: Int = 0
    ) {
        private var animator: ValueAnimator? = null
        private var fadeOutAnimator: ValueAnimator? = null
        
        /**
         * タッチポイントのアニメーションを開始
         */
        fun startAnimation() {
            // 既存のアニメーションをキャンセル
            animator?.cancel()
            fadeOutAnimator?.cancel()
            
            // 出現アニメーション
            animator = ValueAnimator.ofFloat(0f, 80f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    radius = animation.animatedValue as Float
                    alpha = 128
                    invalidate()
                }
                start()
            }
        }
        
        /**
         * フェードアウトアニメーションを開始
         */
        fun startFadeOut() {
            fadeOutAnimator = ValueAnimator.ofInt(128, 0).apply {
                duration = 500
                addUpdateListener { animation ->
                    alpha = animation.animatedValue as Int
                    invalidate()
                    if (alpha == 0) {
                        // アニメーション終了時にマップから削除
                        touchPoints.values.removeIf { it === this@TouchPoint }
                    }
                }
                start()
            }
        }
    }
}
