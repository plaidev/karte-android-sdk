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
package io.karte.android.inappmessaging

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.webkit.CookieManager
import android.widget.PopupWindow
import io.karte.android.KarteApp
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.UserModule
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.internal.IAMPresenter
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.IAMWindow
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.inappmessaging.internal.preview.PreviewParams
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.utilities.getLowerClassName
import org.json.JSONException
import java.lang.ref.WeakReference

private const val LOG_TAG = "Karte.InAppMessaging"
private const val PREVENT_RELAY_TO_PRESENTER_KEY = "krt_prevent_relay_to_presenter"
private const val COOKIE_DOMAIN = "karte.io"

/**
 * アプリ内メッセージの管理を行うクラスです。
 */
class InAppMessaging : Library, ActionModule, UserModule, ActivityLifecycleCallback() {

    //region Library
    override val name: String = getLowerClassName()
    override val version: String = BuildConfig.VERSION_NAME
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        self = this
        app.application.registerActivityLifecycleCallbacks(this)
        this.app = app
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        app.application.unregisterActivityLifecycleCallbacks(this)
        app.unregister(this)

        // teardown
        currentActiveActivity = null
        presenter?.destroy()
        isSuppressed = false
        delegate = null
        cachedWebView?.destroy()
        cachedWebView = null
        Config.enabledWebViewCache = false
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        uiThreadHandler.post {
            if (isSuppressed) return@post
            currentActiveActivity?.get()?.let { activity ->
                try {
                    val message = MessageModel(trackResponse.json, trackRequest)
                    message.filter(app.pvId)
                    if (message.shouldLoad()) {
                        Logger.d(
                            LOG_TAG,
                            "Try to add overlay to activity if not yet added. $activity"
                        )
                        setIAMWindow(message.shouldFocus())
                        presenter?.addMessage(message)
                    }
                } catch (e: JSONException) {
                    Logger.d(LOG_TAG, "Failed to parse json. ", e)
                }
            }
        }
    }

    override fun reset() {
        Logger.d(LOG_TAG, "reset pv_id. ${app.pvId} ${app.originalPvId}")
        // pvIdがある(onResumeより後ろ)場合のみdismissする
        if (app.pvId != app.originalPvId) {
            dismiss()
        }
    }

    override fun resetAll() {
        dismiss()
    }
    //endregion

    //region UserModule
    override fun renewVisitorId(current: String, previous: String?) {
        uiThreadHandler.post {
            presenter?.destroy()
            cachedWebView?.loadUrl(generateOverlayURL())
            clearWebViewCookies()
        }
    }
    //endregion

    //region ActivityLifecycleCallback
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActiveActivity = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        val previewParams = PreviewParams(activity)
        // 接客プレビューの場合はイベントを送信しない。
        if (previewParams.shouldShowPreview()) {
            app.optOutTemporarily()
            showPreview(previewParams)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActiveActivity = WeakReference(activity)
        // foregroundになった時に初めてキャッシュする.
        if (Config.enabledWebViewCache)
            getWebView()
    }

    override fun onActivityPaused(activity: Activity) {
        // FileChooser等のrelay以外は非表示にする.
        var isPreventRelayToPresenter = false
        activity.intent?.let { intent ->
            isPreventRelayToPresenter =
                intent.getBooleanExtra(PREVENT_RELAY_TO_PRESENTER_KEY, false)
            intent.removeExtra(PREVENT_RELAY_TO_PRESENTER_KEY)
        }
        Logger.d(LOG_TAG, "onActivityPaused prevent_relay flag: $isPreventRelayToPresenter")
        if (!isPreventRelayToPresenter) presenter?.destroy()
        currentActiveActivity = null
    }
    //endregion

    internal fun enablePreventRelayFlag(activity: Activity?) {
        activity?.intent?.putExtra(PREVENT_RELAY_TO_PRESENTER_KEY, true)
    }

    companion object {
        internal var self: InAppMessaging? = null
        /**
         * アプリ内メッセージの表示有無を返します。
         *
         * アプリ内メッセージが表示中の場合は `true` を返し、表示されていない場合は `false` を返します。
         */
        @JvmStatic
        val isPresenting: Boolean
            get() = self?.presenter?.isVisible == true
        /**
         * アプリ内メッセージで発生するイベント等を委譲するためのデリゲートインスタンスを取得・設定します。
         */
        @JvmStatic
        var delegate: InAppMessagingDelegate?
            get() = self?.delegate
            set(value) {
                self?.delegate = value
            }

        /**
         * 現在表示中の全てのアプリ内メッセージを非表示にします。
         */
        @JvmStatic
        fun dismiss() {
            self?.uiThreadHandler?.post {
                self?.presenter?.destroy()
            }
        }

        /**
         * アプリ内メッセージの表示を抑制します。
         *
         * なお既に表示されているアプリ内メッセージは、メソッドの呼び出しと同時に非表示となります。
         */
        @JvmStatic
        fun suppress() {
            self?.isSuppressed = true
            dismiss()
        }

        /**
         * アプリ内メッセージの表示抑制状態を解除します。
         */
        @JvmStatic
        fun unsuppress() {
            self?.isSuppressed = false
        }

        /**
         * アプリ内で保持している[PopupWindow]を渡します。
         * SDKはアプリ内メッセージ表示中に、渡された[PopupWindow]の状態に応じてタップの透過等を行ないます。
         *
         * @param[popupWindow] [PopupWindow]
         */
        @JvmStatic
        fun registerPopupWindow(popupWindow: PopupWindow) {
            self?.panelWindowManager?.registerPopupWindow(popupWindow)
        }

        /**
         * アプリ内で保持している[TYPE_APPLICATION_PANEL][android.view.WindowManager.LayoutParams#TYPE_APPLICATION_PANEL]タイプの[Window]を渡します。
         * SDKはアプリ内メッセージ表示中に、渡された[Window]の状態に応じてタップの透過等を行ないます。
         *
         * @param[window] [Window]
         */
        @JvmStatic
        fun registerWindow(window: Window) {
            self?.panelWindowManager?.registerWindow(window)
        }
    }

    /**InAppMessagingモジュールの設定を保持するクラスです。*/
    object Config {
        /**
         * 接客用WebViewのキャッシュの有無の取得・設定を行います。
         *
         * `true` の場合はキャッシュが有効となり、`false` の場合は無効となります。デフォルトは `true` です。
         */
        @JvmStatic
        var enabledWebViewCache = true
    }

    private lateinit var app: KarteApp
    private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val panelWindowManager = PanelWindowManager()
    private val overlayBaseUrl = "https://cf-native.karte.io/v0/native"
    private var currentActiveActivity: WeakReference<Activity>? = null
    private var presenter: IAMPresenter? = null
    private var isSuppressed = false
    private var delegate: InAppMessagingDelegate? = null

    private var cachedWebView: IAMWebView? = null

    /*
      * WebViewを作成. キャッシュ有効時には初回はキャッシュを作成し、以後使い回す。
      */
    private fun getWebView(): IAMWebView {
        cachedWebView?.let { return it }

        val newWebView = IAMWebView(app.application, Config.enabledWebViewCache) { uri: Uri ->
            Boolean
            Logger.d(LOG_TAG, " shouldOpenURL $delegate")
            delegate?.shouldOpenURL(uri) ?: true
        }.apply { loadUrl(generateOverlayURL()) }
        if (Config.enabledWebViewCache) {
            cachedWebView = newWebView
        }
        return newWebView
    }

    private fun setIAMWindow(focusable: Boolean) {
        try {
            // IAMWindow内でWebViewを初期化する際に稀に例外が発生することがあるので対策する
            //
            // NOTE: https://stackoverflow.com/questions/29575313/namenotfoundexception-webview
            // 4系,5.0系のosのバグで、頻度は低い。
            // System WebViewをアップデートした直後に発生する場合があるが再現も難しい。
            if (presenter == null) {
                Logger.d(LOG_TAG, "Adding IAMWindow to Activity. $currentActiveActivity")
                val webView = getWebView()
                currentActiveActivity?.get()?.let { activity ->
                    val window = IAMWindow(activity, panelWindowManager, webView)
                    window.setFocus(focusable)
                    presenter = IAMPresenter(window, webView) { presenter = null }
                }
            }
        } catch (t: Throwable) {
            Logger.e(LOG_TAG, "Failed to construct IAMWindow", t)
        }
    }

    private fun generateOverlayURL(): String {
        return "${overlayBaseUrl}/overlay?app_key=${app.appKey}&_k_vid=${KarteApp.visitorId}&_k_app_prof=${app.appInfo?.json}"
    }

    private fun clearWebViewCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cookieManager = CookieManager.getInstance()
            val allCookies = cookieManager.getCookie(COOKIE_DOMAIN) ?: return
            allCookies
                .split("; ")
                .filter { !it.isNullOrBlank() }
                .forEach {
                    val equalCharIndex = it.indexOf("=")
                    if (equalCharIndex >= 0) {
                        val cookieString = it.substring(0,equalCharIndex) + "=; Domain=" + COOKIE_DOMAIN
                        cookieManager.setCookie(COOKIE_DOMAIN, cookieString)
                    }
                }
            cookieManager.flush()
        }
    }

    private fun showPreview(params: PreviewParams) {
        currentActiveActivity?.get()?.window?.decorView?.post {
            currentActiveActivity?.get()?.let { activity ->
                val webView =
                    IAMWebView(app.application, Config.enabledWebViewCache) { uri: Uri ->
                        Boolean
                        delegate?.shouldOpenURL(uri) ?: true
                    }.apply { params.generateUrl(app)?.let { loadUrl(it) } }
                presenter = IAMPresenter(
                    IAMWindow(activity, panelWindowManager, webView),
                    webView
                ) { presenter = null }
            }
        }
    }
}
