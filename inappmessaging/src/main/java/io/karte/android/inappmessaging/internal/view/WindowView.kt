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
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.RESULT_HIDDEN
import android.widget.FrameLayout
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.BuildConfig
import io.karte.android.inappmessaging.R
import io.karte.android.inappmessaging.internal.PanelWindowManager
import java.lang.ref.WeakReference
import java.util.ArrayList

private const val LOG_TAG = "Karte.IAMView"
private const val WINDOW_FLAGS_FOCUSED = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
private const val WINDOW_FLAGS_UNFOCUSED = (
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

@SuppressLint("ViewConstructor")
@TargetApi(Build.VERSION_CODES.KITKAT)
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

    private var contentViewVisibleRect = Rect()
    private var iamViewVisibleRect = Rect()

    private val locationOnScreen = IntArray(2)

    private val contentView: View
        get() = (appWindow.peekDecorView() as ViewGroup).getChildAt(0)
    private var focusFlag: Int = WINDOW_FLAGS_UNFOCUSED

    private val appSoftInputModeIsNothing: Boolean
        get() = appWindow.attributes.softInputMode and
            WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST ==
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

    /** StatusBarがContentViewに被っているか。Split Screen時に上画面であること. */
    private val isStatusBarOverlaid: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        (contentView.rootWindowInsets?.systemWindowInsetTop ?: -1) > 0
    } else {
        // API 24未満はSplitScreen未対応のため、常にstatus barがある
        true
    }

    init {
        id = R.id.karte_overlay_view
        appWindow.peekDecorView().viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    open fun show() {
        if (isAttachedToWindow || isAttaching) return
        isAttaching = true

        val decorView = appWindow.peekDecorView()
            ?: throw IllegalStateException("Decor view has not yet created.")
        val contentView = contentView
        val params = WindowManager.LayoutParams(
            contentView.width,
            contentView.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
            focusFlag,
            PixelFormat.TRANSLUCENT
        )

        if (appSoftInputModeIsNothing) {
            // keyboard表示中に接客が配信された場合、接客のz-ordferがkeyboardより上になる。この時appWindowのsoftInputModeがSOFT_INPUT_ADJUST_NOTHINGだとkeyboardの高さを知る方法がない
            // そのためこのケースでは、hideSoftInputFromWindow後にaddViewすることでz-orderをkeyboardより下にし、再度showSoftInputする。（hideSoftInputFromWindowの結果によって元々keyboardが表示されていたかどうかの判定ができる）
            hideAndShowKeyboard()
        }
        params.systemUiVisibility = decorView.windowSystemUiVisibility
        val location = IntArray(2)
        contentView.getLocationOnScreen(location)

        params.gravity = Gravity.START or Gravity.TOP
        params.x = location[0]
        params.y = location[1]
        windowManager.addView(this, params)

        logWindowSize("initialized")
    }

    open fun dismiss() {
        if (!isAttachedToWindow) return

        if (webViewDrawingBitmap != null) {
            webViewDrawingBitmap!!.recycle()
        }
        windowManager.removeView(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttaching = false
    }

    fun updateTouchableRegions(touchableRegions: List<RectF>) {
        this.knownTouchableRegions = touchableRegions
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // onPauseの後に呼ばれた時は何もしない(webviewがnullかどうかでチェック)。
        if (visibility == View.VISIBLE && childCount > 0) {
            setupBitmapAndCanvas()
        }
        requestLayout()
    }

    private fun setupBitmapAndCanvas() {
        if (width <= 0 || height <= 0) {
            // e.g. Arrows f-01 Fullscreen keyboard make these params as 0
            return
        }
        if (webViewDrawingBitmap != null && !webViewDrawingBitmap!!.isRecycled) {
            if (webViewDrawingBitmap!!.width == width && webViewDrawingBitmap!!.height == height) {
                // 既にOverlayViewと同じサイズで利用可能なbitmapがあれば何もしなくて良い。
                return
            } else {
                // bitmapはあるがサイズが正しくない場合はrecycleし、新しいサイズのbitmapを作り直す。
                webViewDrawingBitmap!!.recycle()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // https://stackoverflow.com/questions/9247369/alpha-8-bitmaps-and-getpixel
            webViewDrawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webViewDrawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } else {
            Logger.e(
                LOG_TAG,
                "Tried to create bitmap but " + Build.VERSION.SDK_INT + " is not supported."
            )
            return
        }
        canvas = Canvas(webViewDrawingBitmap!!)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            lastActionDownIsInClientApp = touchIsInClientApp(ev)
            setFocus(!lastActionDownIsInClientApp)
            getLocationOnScreen(locationOnScreen)
        }

        if (lastActionDownIsInClientApp) {
            val copiedEvent = MotionEvent.obtain(ev)
            copiedEvent.offsetLocation(locationOnScreen[0].toFloat(), locationOnScreen[1].toFloat())
            val dispatchedToPanel = panelWindowManager.dispatchTouch(copiedEvent)
            if (!dispatchedToPanel) {
                appWindow.injectInputEvent(MotionEvent.obtain(ev))
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
            appWindow.peekDecorView().dispatchKeyEvent(KeyEvent(event))
            // Check ACTION_UP because when changing focus during event sequence, event not handled properly.
            if (event.action == ACTION_UP) setFocus(false)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    internal fun setFocus(focus: Boolean) {
        focusFlag = if (focus) WINDOW_FLAGS_FOCUSED else WINDOW_FLAGS_UNFOCUSED
        if (!isAttachedToWindow) return

        val p = layoutParams as WindowManager.LayoutParams
        p.flags = focusFlag
        windowManager.updateViewLayout(this, p)
    }

    private fun touchIsInClientApp(ev: MotionEvent): Boolean {
        if (childCount == 0 || webViewDrawingBitmap == null || canvas == null ||
            webViewDrawingBitmap!!.isRecycled
        ) {
            return true
        } else {
            val touchedX = ev.x
            val touchedY = ev.y
            if (touchedX <= 0 || touchedY <= 0 || touchedX >= webViewDrawingBitmap!!.width ||
                touchedY >= webViewDrawingBitmap!!.height
            ) {
                return true
            } else {
                for (knownTouchableRegion in knownTouchableRegions) {
                    if (knownTouchableRegion.contains(touchedX, touchedY)) return false
                }
                // There might be dirty bitmap cache (e.g. when dragging chat icon) so clear with transparent color.	      webViewDrawingBitmap.eraseColor(Color.TRANSPARENT);
                webViewDrawingBitmap!!.eraseColor(Color.TRANSPARENT)
                draw(canvas)

                // If transparent, pass to app. Otherwise, pass to WebView.
                return webViewDrawingBitmap!!.getPixel(touchedX.toInt(), touchedY.toInt()) == 0
            }
        }
    }

    private fun hideAndShowKeyboard() {
        val inputMethodManager =
            appWindow.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                ?: return

        val view = appWindow.currentFocus
        if (view != null) {
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
                if (rect == iamViewVisibleRect) return
                iamViewVisibleRect = rect
            } else {
                val rect = Rect()
                contentView.getWindowVisibleDisplayFrame(rect)
                if (rect == contentViewVisibleRect) return
                contentViewVisibleRect = rect
            }

            logWindowSize("requestLayout at onGlobalLayout")

            requestLayout()
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to layout.", e)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            val contentView = contentView
            updateRectAndLocation()
            val width = contentView.width
            val height = contentView.height
            Logger.d(LOG_TAG, "onMeasure window:($width,$height)")
            syncPadding()
            setMeasuredDimension(width, height)

            val childTop: Int
            val childBottom: Int
            if (appSoftInputModeIsNothing) {
                // appWindowのsoft_input_modeがadjust_nothingの場合、contentViewVisibleRectからkeyboardの高さが引かれない
                // この場合はiamViewをcontentView(padding抜き)と同じ高さにし、webViewからkeyboardの高さを引く
                // iamViewVisibleRect.topはiamViewのyに関わらず0を返すことがあるため、bottomからyを引く
                childTop = if (isStatusBarOverlaid) {
                    top + paddingTop
                } else {
                    iamViewVisibleRect.top
                }
                childBottom = iamViewVisibleRect.bottom
            } else {
                childTop = if (isStatusBarOverlaid) {
                    contentView.top + contentView.paddingTop
                } else {
                    contentViewVisibleRect.top
                }
                childBottom = contentViewVisibleRect.bottom
            }

            Logger.d(
                LOG_TAG,
                "onMeasure child: top:$childTop, bottom:$childBottom," +
                    " height:${childBottom - childTop}"
            )
            for (i in 0 until childCount)
                getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(
                        width - paddingLeft - paddingRight,
                        MeasureSpec.EXACTLY
                    ),
                    MeasureSpec.makeMeasureSpec(childBottom - childTop, MeasureSpec.EXACTLY)
                )
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to measure", e)
        }
    }

    private fun syncPadding() {
        setPadding(
            contentView.paddingLeft,
            contentView.paddingTop,
            contentView.paddingRight,
            contentView.paddingBottom
        )
    }

    private fun updateRectAndLocation() {
        getWindowVisibleDisplayFrame(iamViewVisibleRect)
        getLocationOnScreen(locationOnScreen)
        contentView.getWindowVisibleDisplayFrame(contentViewVisibleRect)
    }

    private fun logWindowSize(message: String) {
        if (!BuildConfig.DEBUG) return

        Logger.v(
            LOG_TAG, "$message\n" +
                "ContentView: ${contentView.log()}\n" +
                "IAMWindow  : ${log()}\n" +
                "WebView    : ${if (childCount > 0) getChildAt(0).log() else ""}\n" +
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
