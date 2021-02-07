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
package io.karte.android.inappmessaging.internal

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.DisplayCutout
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.BuildConfig
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.javascript.Callback
import io.karte.android.inappmessaging.internal.javascript.DOCUMENT_CHANGED
import io.karte.android.inappmessaging.internal.javascript.EVENT
import io.karte.android.inappmessaging.internal.javascript.OPEN_URL
import io.karte.android.inappmessaging.internal.javascript.STATE_CHANGE
import io.karte.android.inappmessaging.internal.javascript.State
import io.karte.android.inappmessaging.internal.javascript.VISIBILITY
import io.karte.android.tracking.MessageEventName
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.asString
import io.karte.android.utilities.toList
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList
import kotlin.math.roundToInt

private const val LOG_TAG = "Karte.IAMWebView"
private const val FILE_SCHEME = "file://"

internal interface ParentView {
    fun updateTouchableRegions(touchableRegions: List<RectF>)
    fun openUrl(uri: Uri, withReset: Boolean = true)
    fun errorOccurred()
    fun show()
    fun dismiss()
}

@SuppressLint("ViewConstructor", "SetJavaScriptEnabled", "AddJavascriptInterface")
internal class IAMWebView
constructor(
    context: Context,
    private val shouldOpenURLListener: ((uri: Uri) -> Boolean)?
) :
    WebView(context.applicationContext), MessageModel.MessageView {
    private val uiThreadHandler: Handler

    override var adapter: MessageModel.MessageAdapter? = null
    internal var parentView: ParentView? = null
    internal var hasMessage: Boolean = false

    @VisibleForTesting
    var state = State.LOADING

    class SafeInsets(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    private var safeInsets: SafeInsets? = null

    init {

        settings.javaScriptEnabled = true
        @Suppress("DEPRECATION")
        settings.savePassword = false
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        setBackgroundColor(Color.TRANSPARENT)

        // 初回表示時にスクロールバーが画面端にちらつく現象の回避
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            @Suppress("DEPRECATION")
            settings.databasePath =
                getContext().filesDir.path + getContext().packageName + "/databases/"
        }
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Web版も合わせて接客側で対応ができるまではダークモードはオフにする
            settings.forceDark = WebSettings.FORCE_DARK_OFF
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
                if (!Callback.isTrackerJsCallback(url)) {
                    // アプリ内メッセージにリンクが設定されていない状態で、リンクをタップすると file:// から始まるURLへのアクセスが発生する
                    // Android N以上だとクラッシュする可能性があるので、 file:// から始まるURLは無視する
                    // https://github.com/plaidev/karte-io/issues/24432
                    if (url.startsWith(FILE_SCHEME)) return true

                    val uri = Uri.parse(url)
                    if (shouldOpenURLListener?.invoke(uri) != false) {
                        parentView?.openUrl(uri)
                    }
                    return true
                }
                return true
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                super.onReceivedSslError(view, handler, error)
                handleError("SslError occurred in WebView. $error", error.url)
            }

            // api23以上でmainpage以外でも呼ばれる
            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                var message = "HttpError occurred in WebView. "
                try {
                    message += errorResponse?.data?.asString() ?: ""
                } catch (e: IOException) {
                    Logger.d(LOG_TAG, "Failed to parse Http error response.", e)
                }

                handleError(message, request.url.toString())
            }

            // api23以上でmainpage以外でも呼ばれる
            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                handleError(
                    "Error occurred in WebView. " + error.description.toString(),
                    request.url.toString()
                )
            }

            // mainpageの失敗時のみ呼ばれる
            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                handleError("Error $errorCode occurred in WebView. $description", failingUrl)
            }
        }

        uiThreadHandler = Handler(Looper.getMainLooper())
        addJavascriptInterface(this, "NativeBridge")
    }

    fun handleChangePv() {
        Logger.d(LOG_TAG, "handleChangePv()")
        if (parentView == null) return
        try {
            if (hasMessage) parentView?.show()
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to show Window.", e)
        }
        loadUrl("javascript:window.tracker.handleChangePv();")
        reset(false)
    }

    fun reset(isForceClose: Boolean) {
        Logger.d(LOG_TAG, "resetTrackerJs()")
        if (isForceClose) {
            adapter = null
            parentView = null
        }
        loadUrl("javascript:window.tracker.resetPageState($isForceClose);")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        @Suppress("DEPRECATION")
        super.onLayout(changed, l, t, r, b)
        // 自身がスクリーンのトップに無ければcutoutが重ならないのでsafeAreaInsetTopを0にする。
        if (!isLocatedAtTopOfScreen()) {
            loadUrl("javascript:window.tracker.setSafeAreaInset(0);")
            return
        }
        val insets = safeInsets ?: return
        val safeAreaInsetTop = insets.top
        loadUrl("javascript:window.tracker.setSafeAreaInset($safeAreaInsetTop);")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.safeInsets = getSafeInsets()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // chat等のcloseイベントをハンドルするために、戻るボタンでhistory backを行う.
        if (event.keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            if (event.action == KeyEvent.ACTION_UP) {
                goBack()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun destroy() {
        Logger.d(LOG_TAG, "destroy")
        super.destroy()
        webChromeClient = null
    }

    override fun notifyChanged() {
        when (state) {
            State.LOADING -> {
            }
            State.READY -> loadQueue()
            State.DESTROYED -> Logger.d(
                LOG_TAG,
                "Ignore response because InAppMessagingView has been destroyed."
            )
        } // wait.
    }

    private fun loadQueue() {
        if (adapter == null)
            return
        while (true) {
            val message = adapter?.dequeue() ?: break
            Logger.d(LOG_TAG, "loadQueue $message")
            loadUrl("javascript:window.tracker.handleResponseData('${message.string}');")
        }
    }

    private fun changeState(newState: State) {
        if (state == newState)
            return
        Logger.d(LOG_TAG, "OverlayView entered state: $newState")
        if (newState == State.READY) {
            loadQueue()
        } else if (newState == State.DESTROYED) {
            // TODO: reload Trackerjs or dismiss.
            //      destroy();
        }
        state = newState
    }

    @JavascriptInterface
    fun onReceivedMessage(name: String, data: String) {
        uiThreadHandler.post { handleCallback(name, data) }
    }

    private fun handleCallback(name: String, data: String) {
        try {
            val callback = Callback.parse(name, data)

            when (callback.callbackName) {
                EVENT -> {
                    val eventName = callback.data.getString("event_name")
                    val values = callback.data.getJSONObject("values")
                    Logger.d(LOG_TAG, "Received event callback: event_name=$eventName")
                    Tracker.track(eventName, values)
                    notifyCampaignOpenOrClose(eventName, values)
                }
                STATE_CHANGE -> {
                    val stateStr = callback.data.getString("state")
                    Logger.d(LOG_TAG, "Received state_change callback: state=$stateStr")
                    changeState(State.of(stateStr))
                }
                OPEN_URL -> {
                    val uri = Uri.parse(callback.data.getString("url"))
                    val target = callback.data.optString("target")
                    Logger.d(LOG_TAG, "Received open_url callback: url=$uri")

                    if (shouldOpenURLListener?.invoke(uri) != false) {
                        parentView?.openUrl(uri, target != "_blank")
                    }
                }
                DOCUMENT_CHANGED -> {
                    val regions = callback.data.getJSONArray("touchable_regions")
                    Logger.d(
                        LOG_TAG,
                        "Received document_changed callback: touchable_regions=$regions"
                    )

                    parentView?.updateTouchableRegions(parseDocumentRect(regions))
                }
                VISIBILITY -> {
                    val visibility = callback.data.getString("state")
                    Logger.d(LOG_TAG, "Received visibility callback: state=$visibility")
                    if ("visible" == visibility) {
                        hasMessage = true
                        parentView?.show()
                    } else {
                        hasMessage = false
                        parentView?.dismiss()
                    }
                }
                else -> Logger.w(
                    LOG_TAG,
                    "Unknown callback " + callback.callbackName + " was passed from WebView"
                )
            } // noop
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to parse callback url.", e)
        }
    }

    private fun handleError(message: String, urlTriedToLoad: String?) {
        Logger.e(LOG_TAG, "$message, url: $urlTriedToLoad")
        // 現在のページのエラー時には空htmlを読み込む
        if (url != null && url == urlTriedToLoad)
            loadData("<html></html>", "text/html", "utf-8")
        if (urlTriedToLoad == null || urlTriedToLoad.contains("/native/overlay") ||
            urlTriedToLoad.contains("native_tracker")
        ) {
            parentView?.errorOccurred()
        }
    }

    private fun notifyCampaignOpenOrClose(eventName: String, values: JSONObject) {
        val message = values.optJSONObject("message")
        val shortenId = message?.optString("shorten_id")
        val campaignId = message?.optString("campaign_id")
        if (shortenId == null || campaignId == null) return

        when (eventName) {
            MessageEventName.MessageOpen.value ->
                InAppMessaging.delegate?.onPresented(campaignId, shortenId)
            MessageEventName.MessageClose.value ->
                InAppMessaging.delegate?.onDismissed(campaignId, shortenId)
        }
    }

    private fun parseDocumentRect(regionsJson: JSONArray): List<RectF> {
        try {
            val density = resources.displayMetrics.density

            return regionsJson.toList().filterIsInstance<JSONObject>().map { rect ->
                RectF(
                    (density * rect.getDouble("left")).toFloat(),
                    (density * rect.getDouble("top")).toFloat(),
                    (density * rect.getDouble("right")).toFloat(),
                    (density * rect.getDouble("bottom")).toFloat()
                )
            }
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to update touchable regions.", e)
        }

        return ArrayList()
    }

    private fun getSafeInsets(): SafeInsets? {
        // Pより前のバージョンではcutoutが取得できないので何もしない
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        val cutout: DisplayCutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.cutout
        } else {
            rootWindowInsets.displayCutout
        } ?: return null

        val scale = Resources.getSystem().displayMetrics.density
        return SafeInsets(
            (cutout.safeInsetLeft / scale).roundToInt(),
            (cutout.safeInsetTop / scale).roundToInt(),
            (cutout.safeInsetRight / scale).roundToInt(),
            (cutout.safeInsetBottom / scale).roundToInt()
        )
    }

    private fun isLocatedAtTopOfScreen(): Boolean {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1] == 0
    }
}
