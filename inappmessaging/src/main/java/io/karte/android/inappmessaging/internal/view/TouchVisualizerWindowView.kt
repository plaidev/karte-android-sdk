package io.karte.android.inappmessaging.internal.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.internal.PanelWindowManager
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * タッチイベントを視覚的に表示するWindowView
 * dispatchTouchEventをオーバーライドしてタッチ箇所を円で表示します
 */
@SuppressLint("ViewConstructor")
internal open class TouchVisualizerWindowView internal constructor(
    activity: Activity,
    panelWindowManager: PanelWindowManager
) : WindowView(activity, panelWindowManager) {

    private val LOG_TAG = "Karte.TouchVisualizer"

    // タッチポイントを保持するマップ（スレッドセーフ）
    private val touchPoints = Collections.synchronizedMap(HashMap<Int, TouchPoint>())
    
    // 描画用のタッチポイントリスト（スレッドセーフ）
    private val drawingPoints = CopyOnWriteArrayList<TouchPoint>()
    
    // UIスレッドでの処理用ハンドラ
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // タッチポイントの描画用ペイント
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        alpha = 128
        style = Paint.Style.FILL
    }

    /**
     * タッチイベントをインターセプトして視覚化
     * 元のdispatchTouchEventを呼び出す前にタッチイベントを処理
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // タッチイベントの視覚化処理
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        val x = ev.x
        val y = ev.y

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // 新しいタッチポイントを追加（UIスレッドで実行）
                mainHandler.post {
                    val touchPoint = TouchPoint(x, y)
                    synchronized(touchPoints) {
                        touchPoints[pointerId] = touchPoint
                        // 描画用リストにも追加
                        drawingPoints.add(touchPoint)
                    }
                    touchPoint.startAnimation()
                    // 再描画を要求（ハンドラ操作の完了後）
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 既存のポインターすべての位置を更新（UIスレッドで実行）
                mainHandler.post {
                    synchronized(touchPoints) {
                        for (i in 0 until ev.pointerCount) {
                            val id = ev.getPointerId(i)
                            touchPoints[id]?.apply {
                                this.x = ev.getX(i)
                                this.y = ev.getY(i)
                            }
                        }
                    }
                    // 再描画を要求
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // タッチポイントをフェードアウト（UIスレッドで実行）
                mainHandler.post {
                    synchronized(touchPoints) {
                        touchPoints[pointerId]?.startFadeOut()
                    }
                    // 再描画を要求
                    invalidate()
                }
            }
        }
        
        // 元のdispatchTouchEventを呼び出す
        return super.dispatchTouchEvent(ev)
    }

    /**
     * ビューの描画
     * 先に親クラスのdrawを呼び出し、その後にタッチポイントを描画
     */
    override fun draw(canvas: Canvas) {
        // 先に親クラスのdrawを呼び出す
        super.draw(canvas)
        
        // スレッドセーフなリストからすべてのタッチポイントを描画
        for (touchPoint in drawingPoints) {
            paint.alpha = touchPoint.alpha
            canvas.drawCircle(touchPoint.x, touchPoint.y, touchPoint.radius, paint)
        }

        // デバッグログ
        Logger.d(LOG_TAG, "Drawing touch points: ${drawingPoints.size}")
    }

    /**
     * タッチポイントを表すデータクラス
     */
    private inner class TouchPoint(
        @Volatile var x: Float,
        @Volatile var y: Float,
        @Volatile var radius: Float = 0f,
        @Volatile var alpha: Int = 0
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
                    // UIスレッドで再描画を要求
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
                    // UIスレッドで再描画を要求
                    invalidate()
                    
                    if (alpha == 0) {
                        // アニメーション終了時にマップから削除（UIスレッドで実行）
                        mainHandler.post {
                            synchronized(touchPoints) {
                                // IDでの削除
                                val idToRemove = touchPoints.entries.find { it.value === this@TouchPoint }?.key
                                idToRemove?.let { touchPoints.remove(it) }
                                
                                // 描画用リストからも削除
                                drawingPoints.remove(this@TouchPoint)
                            }
                        }
                    }
                }
                start()
            }
        }
    }
}
