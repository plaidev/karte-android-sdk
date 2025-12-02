//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android.inappmessaging.internal.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.RESULT_HIDDEN
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.BuildConfig
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.R
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.utilities.toList
import org.json.JSONArray
import java.lang.ref.WeakReference

private const val LOG_TAG = "Karte.WindowView"

@Suppress("DEPRECATION")
private const val WINDOW_FLAGS_FOCUSED = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR

@Suppress("DEPRECATION")
private const val WINDOW_FLAGS_UNFOCUSED = (
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

@SuppressLint("ViewConstructor")
internal open class WindowView(
    activity: Activity,
    private val panelWindowManager: PanelWindowManager
) :
    FrameLayout(activity), ViewTreeObserver.OnGlobalLayoutListener {

    private val appWindow: Window = activity.window
    private val windowManager: WindowManager = activity.windowManager
    private var isAttaching: Boolean = false

    private var webViewDrawingBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var lastActionDownIsInClientApp: Boolean = false
    private var knownTouchableRegions: List<RectF> = ArrayList()

    private var decorViewVisibleRect = Rect()
    private var iamViewVisibleRect = Rect()

    private val decorView: View?
        get() = (appWindow.peekDecorView() as? ViewGroup)

    private var focusFlag: Int = WINDOW_FLAGS_UNFOCUSED

    private val appSoftInputModeIsNothing: Boolean
        get() = appWindow.attributes.softInputMode and
            WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST ==
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

    private val drawingArea: View?
        get() = if (InAppMessaging.isEdgeToEdgeEnabled) {
            decorView
        } else {
            contentView
        }

    private var contentViewVisibleRect = Rect()
    private val locationOnScreen = IntArray(2)
    private val contentViewLocationOnScreen = IntArray(2)
    private val contentView: View
        get() = (appWindow.peekDecorView() as ViewGroup).getChildAt(0)

    /** StatusBarがContentViewに被っているか。Split Screen時に上画面であること. */
    private val isStatusBarOverlaid: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        (contentView.rootWindowInsets?.getInsets(WindowInsets.Type.systemBars())?.top ?: -1) > 0
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        @Suppress("DEPRECATION")
        (contentView.rootWindowInsets?.systemWindowInsetTop ?: -1) > 0
    } else {
        // API 24未満はSplitScreen未対応のため、常にstatus barがある
        true
    }

    private val isActivityNotRenewedOnRotate: Boolean = runCatching {
        (activity.packageManager.getActivityInfo(
            activity.componentName,
            PackageManager.GET_META_DATA
        ).configChanges and ActivityInfo.CONFIG_ORIENTATION) == ActivityInfo.CONFIG_ORIENTATION
    }.getOrDefault(false)

    init {
        id = R.id.karte_overlay_view
        decorView?.viewTreeObserver?.addOnGlobalLayoutListener(this)
    }

    open fun show() {
        decorView?.post {
            if (isAttachedToWindow || isAttaching) return@post
            try {
                isAttaching = true
                showInternal()
            } catch (e: Exception) {
                Logger.w(LOG_TAG, "Failed to attach window: ${e.message}", e)
                isAttaching = false
                try {
                    windowManager.removeView(this)
                } catch (e: Exception) {
                    Logger.w(LOG_TAG, "Failed to detach from window: ${e.message}", e)
                }
            }
        }
    }

    private fun showInternal() {
        val drawingArea = drawingArea
            ?: throw IllegalStateException("View has not yet been created.")
        val w = drawingArea.width
        val h = drawingArea.height

        val params = WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
            focusFlag,
            PixelFormat.TRANSLUCENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // NOTE: WindowManager.addViewしたViewはデフォルトでSystemBarsのInsetが考慮されるため明示的に無効化するために0を設定する
            // https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setFitInsetsTypes(int)
            params.fitInsetsTypes = 0
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (appSoftInputModeIsNothing) {
            // keyboard表示中に接客が配信された場合、接客のz-ordferがkeyboardより上になる。この時appWindowのsoftInputModeがSOFT_INPUT_ADJUST_NOTHINGだとkeyboardの高さを知る方法がない
            // そのためこのケースでは、hideSoftInputFromWindow後にaddViewすることでz-orderをkeyboardより下にし、再度showSoftInputする。（hideSoftInputFromWindowの結果によって元々keyboardが表示されていたかどうかの判定ができる）
            hideAndShowKeyboard()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.windowInsetsController?.setSystemBarsAppearance(
                drawingArea.windowInsetsController?.systemBarsAppearance ?: 0,
                drawingArea.windowInsetsController?.systemBarsAppearance ?: 0
            )
        } else {
            @Suppress("DEPRECATION")
            params.systemUiVisibility = drawingArea.windowSystemUiVisibility
        }

        val location = IntArray(2)
        drawingArea.getLocationOnScreen(location)

        params.gravity = Gravity.START or Gravity.TOP
        params.x = location[0]
        params.y = location[1]
        windowManager.addView(this, params)

        logWindowSize("initialized")
    }

    protected open fun dismiss() {
        if (!isAttachedToWindow) return

        webViewDrawingBitmap?.recycle()
        windowManager.removeView(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttaching = false
    }

    fun updateTouchableRegions(touchableRegions: JSONArray) {
        this.knownTouchableRegions = parseJsonToRect(touchableRegions)
    }

    private fun parseJsonToRect(regionsJson: JSONArray): List<RectF> {
        try {
            val density = resources.displayMetrics.density

            return regionsJson.toList().filterIsInstance<Map<String, Double>>().map { rect ->
                RectF(
                    (density * rect.getValue("left")).toFloat(),
                    (density * rect.getValue("top")).toFloat(),
                    (density * rect.getValue("right")).toFloat(),
                    (density * rect.getValue("bottom")).toFloat()
                )
            }
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to update touchable regions.", e)
        }

        return emptyList()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // onPauseの後に呼ばれた時は何もしない(webviewがnullかどうかでチェック)。
        if (visibility == View.VISIBLE && childCount > 0) {
            setupBitmapAndCanvas()
        }

        if (InAppMessaging.isEdgeToEdgeEnabled) {
            if (childCount > 0 && getChildAt(0) is IAMWebView) {
                getChildAt(0).layout(left, top, right, drawingHeight)
            }
        } else {
            requestLayout()
        }
    }

    private fun setupBitmapAndCanvas() {
        if (width <= 0 || height <= 0) {
            // e.g. Arrows f-01 Fullscreen keyboard make these params as 0
            return
        }
        webViewDrawingBitmap?.let {
            if (!it.isRecycled) {
                if (it.width == width && it.height == height) {
                    // 既にOverlayViewと同じサイズで利用可能なbitmapがあれば何もしなくて良い。
                    return
                } else {
                    // bitmapはあるがサイズが正しくない場合はrecycleし、新しいサイズのbitmapを作り直す。
                    it.recycle()
                }
            }
        }
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // https://stackoverflow.com/questions/9247369/alpha-8-bitmaps-and-getpixel
                Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
            } else {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            canvas = Canvas(bitmap)
            webViewDrawingBitmap = bitmap
        } catch (e: OutOfMemoryError) {
            Logger.e(LOG_TAG, "OutOfMemoryError occurred: ${e.message}", e)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            lastActionDownIsInClientApp = touchIsInClientApp(ev)
            setFocus(!lastActionDownIsInClientApp)
            if (!InAppMessaging.isEdgeToEdgeEnabled) {
                getLocationOnScreen(locationOnScreen)
            }
        }

        if (lastActionDownIsInClientApp) {
            val dispatchedToPanel = if (InAppMessaging.isEdgeToEdgeEnabled) {
                panelWindowManager.dispatchTouch(ev)
            } else {
                val copiedEvent = MotionEvent.obtain(ev)
                copiedEvent.offsetLocation(locationOnScreen[0].toFloat(), locationOnScreen[1].toFloat())
                panelWindowManager.dispatchTouch(copiedEvent)
            }

            if (!dispatchedToPanel) {
                val eventOnAppWindow = MotionEvent.obtain(ev)
                if (!InAppMessaging.isEdgeToEdgeEnabled) {
                    // contentViewとWindowViewは同じ座標にある想定だが、フルスクリーンモードによっては座標が変わる場合があるため、ズレを補正しておく
                    val contentViewLocation = IntArray(2)
                    contentView.getLocationOnScreen(contentViewLocation)
                    val offsetX = (locationOnScreen[0] - contentViewLocation[0]).toFloat()
                    val offsetY = (locationOnScreen[1] - contentViewLocation[1]).toFloat()
                    eventOnAppWindow.offsetLocation(offsetX, offsetY)
                }

                appWindow.injectInputEvent(eventOnAppWindow)
            }
            return false
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            // childrenがconsumeするか確認する
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.dispatchKeyEvent(event))
                    return true
            }
            decorView?.dispatchKeyEvent(KeyEvent(event))
            // Check ACTION_UP because when changing focus during event sequence, event not handled properly.
            if (event.action == ACTION_UP) setFocus(false)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    protected fun setFocus(focus: Boolean) {
        focusFlag = if (focus) WINDOW_FLAGS_FOCUSED else WINDOW_FLAGS_UNFOCUSED
        if (!isAttachedToWindow) return

        val p = layoutParams as WindowManager.LayoutParams
        p.flags = focusFlag
        windowManager.updateViewLayout(this, p)
    }

    private fun touchIsInClientApp(ev: MotionEvent): Boolean {
        if (childCount == 0 || canvas == null ||
            webViewDrawingBitmap == null || webViewDrawingBitmap?.isRecycled == true
        ) {
            return true
        }

        val touchedX = ev.x
        val touchedY = ev.y
        val width = webViewDrawingBitmap?.width ?: 0
        val height = webViewDrawingBitmap?.height ?: 0
        if (touchedX <= 0 || touchedY <= 0 || touchedX >= width || touchedY >= height) return true

        knownTouchableRegions.forEach {
            if (it.contains(touchedX, touchedY)) return false
        }
        // There might be dirty bitmap cache (e.g. when dragging chat icon) so clear with transparent color.
        webViewDrawingBitmap?.eraseColor(Color.TRANSPARENT)
        canvas?.let { draw(it) }

        // If transparent, pass to app. Otherwise, pass to WebView.
        val pixel = webViewDrawingBitmap?.getPixel(touchedX.toInt(), touchedY.toInt()) ?: 0
        return pixel == 0
    }

    private fun hideAndShowKeyboard() {
        val inputMethodManager =
            appWindow.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                ?: return

        val view = appWindow.currentFocus
        if (view != null) {
            // TODO: Fix Deprecation
            @Suppress("DEPRECATION")
            inputMethodManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS,
                ResultReceiverToReshow(null, view)
            )
        }
    }

    // hideSoftInputFromWindowに渡したResultReceiverは強参照で長く保持されるため、static classとWeakReferenceを使う.
    private class ResultReceiverToReshow internal constructor(handler: Handler?, view: View) :
        ResultReceiver(handler) {
        private val viewRef: WeakReference<View> = WeakReference(view)

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            val view = viewRef.get()
            if (resultCode == RESULT_HIDDEN && view != null) {
                val inputMethodManager =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as? InputMethodManager
                inputMethodManager?.showSoftInput(view, 0)
            }
        }
    }

    /**
     * windowのsizeとWebViewの位置・サイズを設定する
     * - WebViewをcontentViewと同じ表示領域にする
     * - Keyboardの高さをWindowから引く(キーボード表示中に接客配信されるとIAMViewのz-orderがキーボードより上になり操作不能になる問題の回避）
     */
    override fun onGlobalLayout() {
        try {
            Logger.d(LOG_TAG, "onGlobalLayout")
            if (appSoftInputModeIsNothing) {
                val rect = Rect()
                getWindowVisibleDisplayFrame(rect)
                if (rect == iamViewVisibleRect) {
                    // 画面回転時にActivityが再生成されない場合はrequestLayoutを呼ぶ
                    // この処理が無いとUnityで回転時にレイアウトが崩れることがある
                    if (isActivityNotRenewedOnRotate) {
                        requestLayout()
                    }
                    return
                }
                iamViewVisibleRect = rect
            } else {
                val rect = Rect()
                drawingArea?.getWindowVisibleDisplayFrame(rect)
                if (InAppMessaging.isEdgeToEdgeEnabled) {
                    if (rect == decorViewVisibleRect) {
                        // 画面回転時にActivityが再生成されない場合はrequestLayoutを呼ぶ
                        // この処理が無いとUnityで回転時にレイアウトが崩れることがある
                        if (isActivityNotRenewedOnRotate) {
                            requestLayout()
                        }
                        return
                    }
                    decorViewVisibleRect = rect
                } else {
                    if (rect == contentViewVisibleRect) {
                        // 画面回転時にActivityが再生成されない場合はrequestLayoutを呼ぶ
                        // この処理が無いとUnityで回転時にレイアウトが崩れることがある
                        if (isActivityNotRenewedOnRotate) {
                            requestLayout()
                        }
                        return
                    }
                    contentViewVisibleRect = rect
                }
            }

            logWindowSize("requestLayout at onGlobalLayout")

            requestLayout()
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to layout.", e)
        }
    }

    private val drawingHeight: Int
        get() {
            // NOTE: Edge to edge有効化時にcontentView.heightとcontentViewVisibleRect.bottomの値が異なる(VisibleRectにNavigationBarの高さが含まれない)ため
            // diffを取って描画すべき領域の高さを決定する
            // diffがNavigationBarの高さを超える場合はキーボード等が表示されていると判定しvisibleRect.bottomを下限とする
            // それ以外はcontentView.height=画面の高さを下限とする
            // IAM2.23.0のEdge to edge対応で描画領域が変更となるが後方互換性のためにフラグで以前の処理に分岐させる
            if (InAppMessaging.isEdgeToEdgeEnabled) {
                val height = decorView?.height ?: 0
                val diff = height - decorViewVisibleRect.bottom
                return if (diff > navbarHeight) decorViewVisibleRect.bottom else height
            } else {
                val diff = contentView.height - contentViewVisibleRect.bottom
                return if (diff > navbarHeight) contentViewVisibleRect.bottom else contentView.height
            }
        }

    private val navbarHeight: Int
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                // バージョンに依存せずにNavigationBarの高さを正確に取得する方法がないため画面下10%をNavigationBarの高さとして扱う
                // ジェスチャーナビゲーションの場合はおそらく2.5%程度だが、ここでやりたいことはKeyboard等が表示されてるかの確認なので、
                // スリーボタンナビゲーションなども考慮して画面下10%としている
                val height = drawingArea?.height ?: 0
                (height * 0.1).toInt()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            val drawingArea = drawingArea ?: throw IllegalStateException("View has not yet been created.")
            updateRectAndLocation()
            val width = drawingArea.width
            val height = drawingArea.height
            Logger.d(LOG_TAG, "onMeasure window:($width,$height)")
            syncPadding()
            setMeasuredDimension(width, height)

            val measuringHeight = calcMeasuringHeight()
            for (i in 0 until childCount)
                getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(
                        width - paddingLeft - paddingRight,
                        MeasureSpec.EXACTLY
                    ),
                    MeasureSpec.makeMeasureSpec(measuringHeight, MeasureSpec.EXACTLY)
                )
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to measure", e)
        }
    }

    private fun calcMeasuringHeight(): Int {
        return if (InAppMessaging.isEdgeToEdgeEnabled) {
            drawingHeight
        } else {
            val childTop: Int
            val childBottom: Int
            if (appSoftInputModeIsNothing) {
                // appWindowのsoft_input_modeがadjust_nothingの場合、contentViewVisibleRectからkeyboardの高さが引かれない
                // この場合はiamViewをcontentView(padding抜き)と同じ高さにし、webViewからkeyboardの高さを引く
                // iamViewVisibleRect.topはiamViewのyに関わらず0を返すことがあるため、bottomからyを引く
                childTop = if (isStatusBarOverlaid) {
                    top + paddingTop
                } else {
                    locationOnScreen[1]
                }
                childBottom = drawingHeight
            } else {
                childTop = if (isStatusBarOverlaid) {
                    contentView.top + contentView.paddingTop
                } else {
                    contentViewVisibleRect.top
                }
                childBottom = drawingHeight
            }

            // フルスクリーンでcutout modeがSHORT_EDGESの場合にIAMWindowとcontentViewの位置に差ができて、その分だけ接客が画面下に見切れてしまうため差の分だけheightを低くする。
            contentView.getLocationOnScreen(contentViewLocationOnScreen)
            val gapY = locationOnScreen[1] - contentViewLocationOnScreen[1]
            childBottom - childTop - gapY
        }
    }

    private fun syncPadding() {
        setPadding(
            drawingArea?.paddingLeft ?: 0,
            drawingArea?.paddingTop ?: 0,
            drawingArea?.paddingRight ?: 0,
            drawingArea?.paddingBottom ?: 0
        )
    }

    private fun updateRectAndLocation() {
        getWindowVisibleDisplayFrame(iamViewVisibleRect)
        getLocationOnScreen(locationOnScreen)
        if (InAppMessaging.isEdgeToEdgeEnabled) {
            decorView?.getWindowVisibleDisplayFrame(decorViewVisibleRect)
        } else {
            contentView.getWindowVisibleDisplayFrame(contentViewVisibleRect)
        }
    }

    private fun logWindowSize(message: String) {
        if (!BuildConfig.DEBUG) return

        Logger.v(
            LOG_TAG, "$message\n" +
                "ContentArea: ${drawingArea?.log()}\n" +
                "IAMWindow  : ${log()}\n" +
                "WebView    : ${if (isNotEmpty()) getChildAt(0).log() else ""}\n" +
                "params=$layoutParams"
        )
    }
}

private fun IntArray.log(): String = "(${joinToString(",")})"
private fun View.logPadding(): String = "($paddingLeft,$paddingTop,$paddingRight,$paddingBottom)"
private fun View.logSize(): String = "($width,$height)"
private fun View.log(): String {
    val rect = Rect()
    val location = IntArray(2)
    getWindowVisibleDisplayFrame(rect)
    getLocationOnScreen(location)
    val insets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        rootWindowInsets
    } else null
    return "$rect, location:${location.log()}, padding:${logPadding()}, size:${logSize()}\n" +
        "\t insets:$insets"
}
