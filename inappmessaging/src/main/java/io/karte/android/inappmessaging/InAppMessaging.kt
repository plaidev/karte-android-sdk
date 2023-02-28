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
import android.content.pm.PackageManager
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
import io.karte.android.core.library.TrackModule
import io.karte.android.core.library.UserModule
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.internal.ExpiredMessageOpenEventRejectionFilterRule
import io.karte.android.inappmessaging.internal.IAMPresenter
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.IAMWindow
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.inappmessaging.internal.preview.PreviewParams
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.tracking.queue.TrackEventRejectionFilterRule
import io.karte.android.utilities.ActivityLifecycleCallback
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

private const val LOG_TAG = "Karte.InAppMessaging"
private const val PREVENT_RELAY_TO_PRESENTER_KEY = "krt_prevent_relay_to_presenter"
private const val COOKIE_DOMAIN = "karte.io"

/**
 * アプリ内メッセージの管理を行うクラスです。
 */
class InAppMessaging : Library, ActionModule, UserModule, TrackModule, ActivityLifecycleCallback() {

    //region Library
    override val name: String = InAppMessaging.name
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        self = this
        app.application.registerActivityLifecycleCallbacks(this)
        this.app = app
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.application.unregisterActivityLifecycleCallbacks(this)
        app.unregister(this)

        // teardown
        currentActiveActivity = null
        presenter?.destroy()
        isSuppressed = false
        delegate = null
        cachedWebView?.destroy()
        cachedWebView = null
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        uiThreadHandler.post {
            try {
                val message = MessageModel(trackResponse.json, trackRequest)
                message.filter(app.pvId, ::trackMessageSuppressed)
                if (!message.shouldLoad()) return@post
                if (isSuppressed) {
                    message.messages.map {
                        trackMessageSuppressed(it, "The display is suppressed by suppress mode.")
                    }
                    return@post
                }
                val activity = currentActiveActivity?.get()
                if (activity == null) {
                    message.messages.map {
                        trackMessageSuppressed(
                            it,
                            "The display is suppressed because Activity is not found."
                        )
                    }
                    return@post
                }

                Logger.d(LOG_TAG, "Try to add overlay to activity if not yet added. $activity")
                if (!windowFocusable) windowFocusable = message.shouldFocusCrossDisplayCampaign()
                setIAMWindow(message.shouldFocus())
                presenter?.addMessage(message)
            } catch (e: JSONException) {
                Logger.d(LOG_TAG, "Failed to parse json. ", e)
            }
        }
    }

    override fun reset() {
        Logger.d(LOG_TAG, "reset pv_id. ${app.pvId} ${app.originalPvId}")
        // pvIdがある(onResumeより後ろ)場合のみdismissする
        if (app.pvId != app.originalPvId) {
            uiThreadHandler.post {
                getWebView()?.also {
                    if (it.hasMessage) {
                        if (currentActiveActivity != null) {
                            setIAMWindow(windowFocusable)
                        }
                        it.handleChangePv()
                        it.reset(false)
                    } else {
                        Logger.d(LOG_TAG, "Dismiss by reset pv_id")
                        windowFocusable = false
                        dismiss()
                    }
                }
            }
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
            getWebView(generateOverlayURL())
            clearWebViewCookies()
        }
    }
    //endregion

    //region TrackModule
    override val eventRejectionFilterRules: List<TrackEventRejectionFilterRule>
        get() = listOf(ExpiredMessageOpenEventRejectionFilterRule())

    override fun intercept(request: TrackRequest): TrackRequest {
        return request
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
            Logger.i(LOG_TAG, "Enter preview mode. ${previewParams.generateUrl(app)}")
            app.optOutTemporarily()
            showPreview(previewParams)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActiveActivity = WeakReference(activity)
        // foregroundになった時に初めてキャッシュする.
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
        if (!isPreventRelayToPresenter) presenter?.destroy(false)
        currentActiveActivity = null
    }
    //endregion

    internal fun enablePreventRelayFlag(activity: Activity?) {
        activity?.intent?.putExtra(PREVENT_RELAY_TO_PRESENTER_KEY, true)
    }

    companion object {
        internal var self: InAppMessaging? = null
        internal var name: String = "inappmessaging"

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
        @Deprecated("This param is always true")
        var enabledWebViewCache = true
    }

    internal lateinit var app: KarteApp

    private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val panelWindowManager = PanelWindowManager()
    private val overlayBaseUrl = "https://cf-native.karte.io/v0/native"
    private var currentActiveActivity: WeakReference<Activity>? = null
    private var presenter: IAMPresenter? = null
    private var isSuppressed = false
    private var delegate: InAppMessagingDelegate? = null

    private var cachedWebView: IAMWebView? = null
    private var windowFocusable: Boolean = false

    /*
      * WebViewを作成. キャッシュ有効時には初回はキャッシュを作成し、以後使い回す。
      */
    private fun getWebView(url: String? = null): IAMWebView? {
        cachedWebView?.let {
            // urlが指定されている場合、読み込み済みのものと異なればcacheを使用しない
            if (url == null || url == it.url)
                return it
        }
        Logger.d(LOG_TAG, "WebView recreate")
        cachedWebView?.destroy()
        cachedWebView = null
        try {
            cachedWebView = IAMWebView(app.application) { uri: Uri ->
                Boolean
                Logger.d(LOG_TAG, " shouldOpenURL $delegate")
                delegate?.shouldOpenURL(uri) ?: true
            }.apply { loadUrl(url ?: generateOverlayURL()) }
        } catch (e: PackageManager.NameNotFoundException) {
            // WebViewアップデート中に初期化すると例外発生する可能性がある
            // NOTE: https://stackoverflow.com/questions/29575313/namenotfoundexception-webview
            // 4系,5.0系に多いが、その他でも発生しうる。
            Logger.e(LOG_TAG, "Failed to construct IAMWebView, because WebView is updating.", e)
        } catch (t: Throwable) {
            // 7系等入っているWebViewによってWebKit側のExceptionになってしまうのでThrowableでキャッチする
            // https://stackoverflow.com/questions/46278681/android-webkit-webviewfactorymissingwebviewpackageexception-from-android-7-0
            Logger.e(LOG_TAG, "Failed to construct IAMWebView", t)
        }

        return cachedWebView
    }

    private fun setIAMWindow(focusable: Boolean) {
        if (presenter != null) return

        val activity = currentActiveActivity?.get() ?: return
        val webView = getWebView() ?: return
        Logger.d(LOG_TAG, "Setting IAMWindow to activity. $currentActiveActivity")
        presenter = IAMPresenter(
            IAMWindow(activity, panelWindowManager, webView).apply { setFocus(focusable) },
            webView
        ) { presenter = null }
    }

    private fun generateOverlayURL(): String {
        return "$overlayBaseUrl/overlay?app_key=${app.appKey}&_k_vid=${KarteApp.visitorId}" +
            "&_k_app_prof=${app.appInfo?.json}"
    }

    private fun clearWebViewCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cookieManager = CookieManager.getInstance()
            val allCookies = cookieManager.getCookie(COOKIE_DOMAIN) ?: return
            allCookies
                .split("; ")
                .filter { !it.isBlank() && it.contains("=") }
                .forEach {
                    val cookieString = it.substringBefore("=") + "=; Domain=" + COOKIE_DOMAIN
                    cookieManager.setCookie(COOKIE_DOMAIN, cookieString)
                }
            cookieManager.flush()
        }
    }

    private fun showPreview(params: PreviewParams) {
        currentActiveActivity?.get()?.window?.decorView?.post {
            val activity = currentActiveActivity?.get() ?: return@post
            val url = params.generateUrl(app) ?: return@post
            val webView = getWebView(url) ?: return@post
            // dismissされないため. 本来はTracker.jsサイドで処理する？
            webView.hasMessage = true
            presenter = IAMPresenter(
                IAMWindow(activity, panelWindowManager, webView),
                webView
            ) { presenter = null }
        }
    }

    private fun trackMessageSuppressed(message: JSONObject, reason: String) {
        val action = message.getJSONObject("action")
        val campaignId = action.getString("campaign_id")
        val shortenId = action.getString("shorten_id")
        val values = mapOf("reason" to reason)
        Tracker.track(MessageEvent(MessageEventType.Suppressed, campaignId, shortenId, values))
    }
}
