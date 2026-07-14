//
//  Copyright 2023 PLAID, Inc.
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
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.Display
import android.view.DisplayCutout
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.BuildConfig
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.utilities.asString
import java.io.IOException
import kotlin.math.roundToInt

private const val LOG_TAG = "Karte.BaseWebView"
private const val FILE_SCHEME = "file://"
private const val KARTE_CALLBACK_SCHEME = "karte-tracker-callback://"

@SuppressLint("SetJavaScriptEnabled")
internal abstract class BaseWebView(context: Context) : WebView(context.applicationContext) {

    class SafeInsets(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private var safeInsets: SafeInsets? = null

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // NOTE: API 35 から非推奨、かつ Chromium での WebSQL サポート終了後は処理が無効となる
        @Suppress("DEPRECATION")
        settings.databaseEnabled = true

        this.setBackgroundColor(Color.TRANSPARENT)

        // 初回表示時にスクロールバーが画面端にちらつく現象の回避
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }

        webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in API level 24")
            override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
                // 接客内callback用
                if (url.startsWith(KARTE_CALLBACK_SCHEME)) return true
                // アプリ内メッセージにリンクが設定されていない状態で、リンクをタップすると file:// から始まるURLへのアクセスが発生する
                // Android N以上だとクラッシュする可能性があるので、 file:// から始まるURLは無視する
                // https://github.com/plaidev/karte-io/issues/24432
                if (url.startsWith(FILE_SCHEME)) return true

                val uri = Uri.parse(url)
                openUrl(uri)
                return true
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                super.onReceivedSslError(view, handler, error)
                handleError(
                    "SslError occurred in WebView. $error",
                    error.url,
                    isNetworkError = false
                )
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

                handleError(
                    message,
                    request.url.toString(),
                    isNetworkError = false
                )
            }

            // api23以上でmainpage以外でも呼ばれる
            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // super はメインフレームエラー時に deprecated の onReceivedError を内部で呼ぶため、
                // 二重呼び出しを防ぐため super は呼ばない

                // ネットワーク系のエラーで発火するため、isNetworkError を true に設定
                handleError(
                    "Error occurred in WebView. " + error.description.toString(),
                    request.url.toString(),
                    isNetworkError = true
                )
            }

            // mainpageの失敗時のみ呼ばれる
            // NOTE: API 23 未満との互換性のために override が必要 (API 23 以上は上記 onReceivedError が呼ばれる)
            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                @Suppress("DEPRECATION")
                super.onReceivedError(view, errorCode, description, failingUrl)
                // ネットワーク系のエラーで発火するため、isNetworkError を true に設定
                handleError(
                    "Error $errorCode occurred in WebView. $description",
                    failingUrl,
                    isNetworkError = true
                )
            }
        }

        webChromeClient = object : WebChromeClient() {

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                showAlert(message)
                result.cancel()
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Logger.d(LOG_TAG, "Console message:" + consoleMessage.message())
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean = showFileChooser(filePathCallback)
        }
    }

    @Deprecated("Deprecated in API level3")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        @Suppress("DEPRECATION")
        super.onLayout(changed, l, t, r, b)
        // 自身がスクリーンのトップに無ければcutoutが重ならないのでsafeAreaInsetTopを0にする。
        if (!isLocatedAtTopOfScreen()) {
            setSafeAreaInset(0)
            return
        }
        val insets = safeInsets ?: return
        val safeAreaInsetTop = insets.top
        setSafeAreaInset(safeAreaInsetTop)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.safeInsets = getSafeInsets()
        if (InAppMessaging.isEdgeToEdgeEnabled) {
            injectSafeAreaInsetCSS()
        }
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

    private fun handleError(message: String, urlTriedToLoad: String, isNetworkError: Boolean) {
        Logger.e(LOG_TAG, "$message, url: $urlTriedToLoad")
        when {
            // オーバーレイ URL の読み込みエラー
            urlTriedToLoad.contains("/native/overlay") -> {
                loadData("<html></html>", "text/html", "utf-8")
                overlayLoadFailed()
                errorOccurred()
            }

            // サブリソースのネットワークエラー (オフライン時にメインフレームがキャッシュから読み込まれた場合の検知)
            // オーバーレイ URL 以外が WebView に読み込まれることは想定しないが、予期せぬ URL への誤反応を防ぐための防衛対応
            isNetworkError && url?.contains("/native/overlay") == true -> {
                overlayLoadFailed()
            }

            // 上記に該当しないサブリソースのエラーはスルー
        }
    }

    private fun getSafeInsets(): SafeInsets? {
        // Pより前のバージョンではcutoutが取得できないので何もしない
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        val cutout: DisplayCutout? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)?.cutout
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            // NOTE: API 30 以上では DisplayManagerCompat で取得するため、defaultDisplay は API 29 以下で使用
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.cutout
        } else {
            rootWindowInsets.displayCutout
        }
        if (cutout == null) {
            return null
        }

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

    private fun injectSafeAreaInsetCSS() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insetsCompat ->
            // Compat API でマスクを組み合わせ
            val mask = WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()

            // Compat Insets を取得
            val raw = insetsCompat.getInsets(mask)

            // dp 換算
            val density = view.resources.displayMetrics.density
            val safeInsets = SafeInsets(
                (raw.left / density).roundToInt(),
                (raw.top / density).roundToInt(),
                (raw.right / density).roundToInt(),
                (raw.bottom / density).roundToInt()
            )

            // CSS 注入などの後続処理
            val safeAreaCSS = """
      document.documentElement.style.setProperty("--krt-safe-area-inset-left",   "${safeInsets.left}px");
      document.documentElement.style.setProperty("--krt-safe-area-inset-top",    "${safeInsets.top}px");
      document.documentElement.style.setProperty("--krt-safe-area-inset-right",  "${safeInsets.right}px");
      document.documentElement.style.setProperty("--krt-safe-area-inset-bottom","${safeInsets.bottom}px");
    """
            evaluateJavascript(safeAreaCSS, null)

            // 必要に応じて Compat インセットを返す
            insetsCompat
        }
    }

    abstract fun setSafeAreaInset(top: Int)
    abstract fun overlayLoadFailed()
    abstract fun errorOccurred()
    abstract fun openUrl(uri: Uri)
    abstract fun showAlert(message: String)
    abstract fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean
}
