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
import android.os.Build
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
import io.karte.android.inappmessaging.internal.IAMProcessor
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.inappmessaging.internal.PanelWindowManager
import io.karte.android.inappmessaging.internal.preview.PreviewParams
import io.karte.android.tracking.Event
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.tracking.queue.TrackEventRejectionFilterRule
import io.karte.android.utilities.ActivityLifecycleCallback
import org.json.JSONException
import org.json.JSONObject

private const val LOG_TAG = "Karte.InAppMessaging"
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
        this.processor = IAMProcessor(app.application, panelWindowManager)
        this.config = app.libraryConfig(InAppMessagingConfig::class.java) ?: InAppMessagingConfig.build()
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.application.unregisterActivityLifecycleCallbacks(this)
        app.unregister(this)

        // teardown
        isSuppressed = false
        delegate = null
        processor.teardown()
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        uiThreadHandler.post {
            val message = MessageModel(trackResponse.json, trackRequest)
            message.filter(app.pvId, ::trackMessageSuppressed)
            if (isSuppressed) {
                message.messages.map {
                    trackMessageSuppressed(it, "The display is suppressed by suppress mode.")
                }
                return@post
            }
            processor.handle(message)
        }
    }

    override fun reset() {
        Logger.d(LOG_TAG, "reset pv_id. ${app.pvId} ${app.originalPvId}")
        // pvIdがある(onResumeより後ろ)場合のみdismissする
        if (app.pvId != app.originalPvId) {
            uiThreadHandler.post {
                processor.handleChangePv()
                processor.reset(false)
            }
        }
    }

    override fun resetAll() {
        uiThreadHandler.post {
            processor.reset(true)
        }
    }
    //endregion

    //region UserModule
    override fun renewVisitorId(current: String, previous: String?) {
        uiThreadHandler.post {
            processor.reset(true)
            processor.reload()
            clearWebViewCookies()
        }
    }
    //endregion

    //region TrackModule
    override val eventRejectionFilterRules: List<TrackEventRejectionFilterRule>
        get() = listOf(ExpiredMessageOpenEventRejectionFilterRule())

    override fun prepare(event: Event): Event {
        if (event.eventName.value == "view") {
            uiThreadHandler.post {
                processor.handleView(event.values)
            }
        }
        return event
    }

    override fun intercept(request: TrackRequest): TrackRequest {
        return request
    }
    //endregion

    //region ActivityLifecycleCallback
    override fun onActivityStarted(activity: Activity) {
        val previewParams = PreviewParams(activity)
        // 接客プレビューの場合はイベントを送信しない。
        if (previewParams.shouldShowPreview()) {
            Logger.i(LOG_TAG, "Enter preview mode. ${previewParams.generateUrl(app)}")
            app.optOutTemporarily()
            processor.reload(previewParams.generateUrl(app))
        }
    }
    //endregion

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
            get() = self?.processor?.isPresenting == true

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
                self?.processor?.reset(true)
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
    @Deprecated("Use InAppMessagingConfig class.")
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
    private lateinit var processor: IAMProcessor
    private lateinit var config: InAppMessagingConfig
    private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val panelWindowManager = PanelWindowManager()
    private val overlayBaseUrl: String
        get() = config.overlayBaseUrl

    private var isSuppressed = false
    private var delegate: InAppMessagingDelegate? = null

    internal fun generateOverlayURL(): String {
        return "$overlayBaseUrl/v0/native/overlay" +
            "?app_key=${app.appKey}" +
            "&_k_vid=${KarteApp.visitorId}" +
            "&_k_app_prof=${app.appInfo?.json}" +
            "&location=${app.config.dataLocation}"
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

    private fun trackMessageSuppressed(message: JSONObject, reason: String) {
        try {
            val action = message.getJSONObject("action")
            val campaignId = action.getString("campaign_id")
            val shortenId = action.getString("shorten_id")
            val campaign = message.getJSONObject("campaign")
            val serviceActionType = campaign.getString("service_action_type")
            if (serviceActionType == "remote_config") {
                return
            }
            val values = mapOf("reason" to reason)
            Tracker.track(MessageEvent(MessageEventType.Suppressed, campaignId, shortenId, values))
        } catch (e: JSONException) {
            Logger.d(LOG_TAG, "Failed to parse json.", e)
        }
    }
}
