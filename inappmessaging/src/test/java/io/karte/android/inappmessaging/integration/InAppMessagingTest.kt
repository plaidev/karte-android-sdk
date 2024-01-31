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
package io.karte.android.inappmessaging.integration

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.view.KeyEvent
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.InAppMessagingConfig
import io.karte.android.inappmessaging.internal.IAMProcessor
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.IAMWindow
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.assertThat
import io.karte.android.test_lib.createControlGroupMessage
import io.karte.android.test_lib.createMessage
import io.karte.android.test_lib.createMessageOpen
import io.karte.android.test_lib.createMessagesResponse
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.proceedBufferedCall
import io.karte.android.test_lib.proceedUiBufferedCall
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.shadow.CustomShadowWebView
import io.karte.android.test_lib.tearDownKarteApp
import io.karte.android.test_lib.toList
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.forEach
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.util.Base64

private const val iamAppKey = "inappmessaging_appkey_1234567890"
private const val overlayBaseUrl = "https://iam-test.karte.io/v0/native"

private const val popup1ActionId = "action1"
private const val popup2ActionId = "action2"
private const val limitedActionId = "action3"
private val popupMsg1 = createMessage(shortenId = popup1ActionId, pluginType = "webpopup")
private val popupMsg2 = createMessage(shortenId = popup2ActionId, pluginType = "webpopup")
private val cgPopupMsg = createControlGroupMessage()
private val limitedMsg = createMessage(shortenId = limitedActionId, pluginType = "webpopup").apply {
    getJSONObject("campaign").put("native_app_display_limit_mode", true)
}

abstract class InAppMessagingTestCase : RobolectricTestCase() {
    lateinit var server: MockWebServer
    lateinit var dispatcher: IAMRequestDispatcher
    lateinit var activity: ActivityController<Activity>
    lateinit var app: KarteApp

    internal val iamWindow: IAMWindow?
        get() = (shadowOf(activity.get().windowManager) as ShadowWindowManagerImpl).views.filterIsInstance(IAMWindow::class.java).firstOrNull()
    val webView: WebView?
        get() = iamWindow?.getChildAt(0) as? WebView
    val shadowWebView: CustomShadowWebView?
        get() = CustomShadowWebView.current
    private val iamWebView: IAMWebView?
        get() = shadowWebView?.getJavascriptInterface("NativeBridge") as? IAMWebView

    @Before
    fun initTracker() {
        server = MockWebServer()
        dispatcher = IAMRequestDispatcher { request ->
            val body = JSONObject(request.parseBody())
            val eventNames =
                body.getJSONArray("events").toList().map { it.getString("event_name") }

            val messages = JSONArray()
            if (eventNames.any { it == "popup1" }) {
                messages.put(popupMsg1)
            }
            if (eventNames.any { it == "popup2" }) {
                messages.put(popupMsg2)
            }
            if (eventNames.any { it == "cg_popup" }) {
                messages.put(cgPopupMsg)
            }
            if (eventNames.any { it == "limited" }) {
                messages.put(limitedMsg)
            }

            MockResponse().setBody(createMessagesResponse(messages).toString())
        }
        server.dispatcher = dispatcher
        server.start()
        // IAMWebViewをspyしても良いが、バグでviewのサブクラスをspyするとstackoverflowを起こすのでProcessorにしている
        mockkConstructor(IAMProcessor::class)
        every { anyConstructed<IAMProcessor>().handle(any()) } answers {
            val messages = firstArg<MessageModel>()
            println("IAMProcessor.handle spy: ${messages.shouldLoad()}, $iamWebView")
            callOriginal()
            if (shouldVisible(messages)) emitVisibleCallbackFromJs()
        }
        every { anyConstructed<IAMProcessor>().reset(any()) } answers {
            println("IAMProcessor.reset spy: ${firstArg<Boolean>()}")
            callOriginal()
            emitInvisibleCallbackFromJs()
        }

        val configBuilder = Config.Builder().libraryConfigs(
            InAppMessagingConfig.build { overlayBaseUrl = "https://iam-test.karte.io" }
        )
        app = setupKarteApp(server, appKey = iamAppKey, configBuilder = configBuilder)
        activity = Robolectric.buildActivity(
            Activity::class.java,
            Intent(application, Activity::class.java)
        ).create().resume().visible()

        // flush first native_app_install event
        proceedBufferedCall()
        server.takeRequest()
        proceedUiBufferedCall()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
        CustomShadowWebView.teardown()
        unmockkConstructor(IAMProcessor::class)
    }

    fun track(eventName: String) {
        Tracker.track(eventName)
        proceedBufferedCall()
        proceedUiBufferedCall()
    }

    fun view(viewName: String) {
        Tracker.view(viewName)
        proceedBufferedCall()
        proceedUiBufferedCall()
    }

    fun trackPopUp1() {
        track("popup1")
    }

    fun trackCgPopUp() {
        track("cg_popup")
    }

    private fun shouldVisible(messages: MessageModel): Boolean {
        return messages.messages.any {
            val type = it.optJSONObject("action")?.optString("type")
            type != null && type != "control"
        }
    }

    protected fun emitCbFromJs(callbackName: String, data: JSONObject) {
        val mock = mockk<WebView>()
        every { mock.context } returns application
        iamWebView?.onReceivedMessage(callbackName, data.toString())
        proceedUiBufferedCall()
    }

    protected fun emitInitializedCallbackFromJs() {
        emitCbFromJs("state_changed", JSONObject().put("state", "initialized"))
    }

    protected fun emitVisibleCallbackFromJs() {
        emitCbFromJs("visibility", JSONObject().put("state", "visible"))
    }

    protected fun emitInvisibleCallbackFromJs() {
        emitCbFromJs("visibility", JSONObject().put("state", "invisible"))
    }

    private fun responsesPassedToJs(): List<JSONObject> {
        val handleResponseDataHead = "javascript:window.tracker.handleResponseData('"
        val handleResponseDataTail = "');"
        val filtered = shadowWebView?.loadedUrls?.filter { it.startsWith(handleResponseDataHead) }
        val encodedStr = filtered?.map {
            it.substring(
                handleResponseDataHead.length,
                it.length - handleResponseDataTail.length
            )
        }
        return encodedStr?.map { JSONObject(String(Base64.getDecoder().decode(it))) } ?: listOf()
    }

    protected fun assertNoResponseDataPassed() {
        assertThat(responsesPassedToJs()).isEmpty()
    }

    protected fun assertActionInResponseData(actionId: String) {
        val responseData = responsesPassedToJs()
        val actionIds = mutableListOf<String>()
        responseData.forEach {
            it.getJSONArray("messages").forEach {
                if (it is JSONObject) {
                    actionIds.add(it.getJSONObject("action").getString("shorten_id"))
                }
            }
        }
        assertThat(actionIds).contains(actionId)
    }

    protected fun assertUrlIsCorrect(uri: Uri) {
        assertThat(uri.scheme).isEqualTo("https")
        assertThat(uri.host).isEqualTo("iam-test.karte.io")
        assertThat(uri.path).isEqualTo("/v0/native/overlay")
        assertThat(uri.queryParameterNames).isEqualTo(setOf("app_key", "_k_vid", "_k_app_prof", "location"))
        assertThat(uri.getQueryParameter("app_key")).isEqualTo(iamAppKey)
        assertThat(uri.getQueryParameter("_k_vid")).isEqualTo(KarteApp.visitorId)
        assertThat(uri.getQueryParameter("_k_app_prof")).isEqualTo(app.appInfo?.json.toString())
        assertThat(uri.getQueryParameter("location")).isEqualTo(app.config.dataLocation)
    }
}

@RunWith(Enclosed::class)
class InAppMessagingTest {
    class IAMWebViewの初期化 : InAppMessagingTestCase() {
        @Test
        fun 最初のloadで正しいurlの読み込みを行うこと() {
            val shadow = shadowWebView ?: throw AssertionError()
            val uri = Uri.parse(shadow.loadedUrls.first())
            assertUrlIsCorrect(uri)
        }
    }

    class IAMWebViewの初期化失敗 : RobolectricTestCase() {
        @Before
        fun setup() {
            mockkConstructor(IAMWebView::class)
            every {
                anyConstructed<IAMWebView>().loadUrl(any())
            } throws PackageManager.NameNotFoundException()
            setupKarteApp(appKey = iamAppKey)
        }

        @After
        fun teardown() {
            unmockkConstructor(IAMWebView::class)
            tearDownKarteApp()
        }

        @Test
        fun クラッシュしないこと() {
            Robolectric.buildActivity(
                Activity::class.java,
                Intent(application, Activity::class.java)
            ).create().resume()
        }
    }

    class レスポンスハンドリング : InAppMessagingTestCase() {

        @Test
        fun webpopup接客を含むレスポンスが来た場合はoverlayが追加されること() {
            trackPopUp1()
            emitInitializedCallbackFromJs()

            assertThat(webView).isNotNull()
            assertThat(iamWindow?.visibility).isEqualTo(View.VISIBLE)
            assertThat(shadowWebView?.lastLoadedUrl)
                .startsWith("javascript:window.tracker.handleResponseData")
            assertThat(shadowWebView?.loadedUrls?.first()).startsWith(overlayBaseUrl)
            val uri = Uri.parse(shadowWebView?.loadedUrls?.first())
            assertUrlIsCorrect(uri)
        }

        @Test
        fun 未実施のwebpopup接客が来た場合はoverlayが追加されないこと() {
            trackCgPopUp()

            assertThat(iamWindow).isNull()
            assertThat(webView).isNull()
            assertThat(shadowWebView?.lastLoadedUrl).startsWith(overlayBaseUrl)
        }

        @Test
        fun webpopup接客を含まないレスポンスが来た場合はInAppMessagingViewが追加されないこと() {
            track("hoge")

            assertThat(iamWindow).isNull()
        }

        @Test
        fun overlay読み込み中の場合はresponseが渡らないこと() {
            // overlay表示
            trackPopUp1()
            // 2度目のpopup response
            trackPopUp1()

            assertThat(shadowWebView).isNotNull()
            assertNoResponseDataPassed()
        }

        @Test
        fun overlay読み込み完了時にバッファされていたresponseが渡ること() {
            trackPopUp1()
            track("popup2")

            assertThat(shadowWebView).isNotNull()
            assertNoResponseDataPassed()

            emitInitializedCallbackFromJs()
            assertActionInResponseData(popup1ActionId)
            assertActionInResponseData(popup2ActionId)

            assertThat(iamWindow?.visibility).isEqualTo(View.VISIBLE)
        }

        @Test
        fun overlay読み込み完了後に来たresponseがjsに渡ること() {
            trackPopUp1()

            assertThat(shadowWebView).isNotNull()
            assertNoResponseDataPassed()

            emitInitializedCallbackFromJs()
            track("popup2")

            assertActionInResponseData(popup2ActionId)
        }
    }

    class ページのリセット処理 : InAppMessagingTestCase() {

        @Before
        fun setupOverlayAndJs() {
            view("page1")
            trackPopUp1()

            emitInitializedCallbackFromJs()
        }

        @Test
        fun viewによりページが切り替わった時にresetとwindow破棄が呼ばれること() {
            view("page2")
            assertThat(shadowWebView?.loadedUrls)
                .contains("javascript:window.tracker.resetPageState(false);")
            assertThat(iamWindow).isNull()
        }

        @Test
        fun ページが切り替わらなければresetPageStateが実行されないこと() {
            track("hogehoge")
            assertThat(shadowWebView?.loadedUrls).isNotEmpty()
            assertThat(shadowWebView?.loadedUrls)
                .doesNotContain("javascript:window.tracker.resetPageState(")
        }

        @Test
        fun onPauseによりページが切り替わった時にresetとwindow破棄が呼ばれること() {
            val currentShadowWebView = shadowWebView

            activity.pause()
            proceedBufferedCall()
            proceedUiBufferedCall()
            assertThat(currentShadowWebView?.loadedUrls)
                .contains("javascript:window.tracker.resetPageState(false);")
            assertThat(iamWindow).isNull()
        }
    }

    class バックボタンハンドリング : InAppMessagingTestCase() {

        /* Activityまで伝播するKeyEventを作る */
        private fun backButtonEvent(): KeyEvent {
            val keyEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
            val state = KeyEvent.DispatcherState()
            state.startTracking(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK), null)
            state.handleUpEvent(keyEvent)
            return keyEvent
        }

        @Test
        fun 通常時にバックボタンを押してもIAMViewが処理しないこと() {
            trackPopUp1()

            val iamView = iamWindow
            assertThat(iamView).isNotNull()

            val consume = iamView?.dispatchKeyEvent(backButtonEvent())
            assertThat(consume).isTrue()
            // activityが終了しているはず.
            assertThat(activity.get().isFinishing).isTrue()
        }

        @Test
        fun スタックを積んだ時にバックボタンを押すとIAMViewが処理すること() {
            trackPopUp1()

            val iamView = iamWindow
            assertThat(iamView).isNotNull()

            webView?.loadUrl("test")
            assertThat(webView?.canGoBack()).isTrue()

            val consume = iamView?.dispatchKeyEvent(backButtonEvent())
            assertThat(consume).isTrue()
            // activityは終了しないはず.
            assertThat(activity.get().isFinishing).isFalse()
        }
    }

    class overlayからのコールバック : InAppMessagingTestCase() {

        @Before
        fun setupOverlayAndJs() {
            trackPopUp1()

            emitInitializedCallbackFromJs()
        }

        @Test
        fun イベントが来た場合はトラッキングされること() {
            emitCbFromJs(
                "event",
                JSONObject().put("event_name", "from_overlay").put(
                    "values",
                    JSONObject().put("foo", "bar")
                )
            )
            // trackerのthreadも動かす必要がある
            proceedBufferedCall()

            assertThat(dispatcher.trackedEvents().filter {
                it.getString("event_name") == "from_overlay" &&
                    it.getJSONObject("values").getString("foo") == "bar"
            }).isNotEmpty()
        }

        @Test
        fun open_urlが来た場合はIntentが投げられること() {
            emitCbFromJs("open_url", JSONObject().put("url", "test://hoge"))

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            val nextActivity = shadowOf(application).nextStartedActivity
            assertThat(nextActivity).isNotNull()
            assertThat(nextActivity.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(nextActivity.dataString).isEqualTo("test://hoge")
        }

        @Test
        fun visibility_visibleによりoverlayが表示されること() {
            emitInitializedCallbackFromJs()
            emitVisibleCallbackFromJs()
            assertThat(iamWindow).isNotNull()
        }

        @Test
        fun visibility_invisibleによりoverlayが非表示になること() {
            emitInvisibleCallbackFromJs()
            assertThat(iamWindow).isNull()
        }
    }

    class overlayからのコールバック_CG : InAppMessagingTestCase() {

        @Before
        fun setupOverlayAndJs() {
            trackCgPopUp()

            emitInitializedCallbackFromJs()
        }

        @Test
        fun CGではoverlayが表示されないこと() {
            emitCbFromJs("event", createMessageOpen(shortenId = "__sample_shorten"))

            assertThat(iamWindow).isNull()
        }
    }

    class 読み込み失敗時の制御 : InAppMessagingTestCase() {

        @MockK
        private lateinit var mockReq: WebResourceRequest

        @MockK
        private lateinit var mockError: WebResourceError

        @MockK
        private lateinit var mockResourceResponse: WebResourceResponse

        @MockK
        private lateinit var mockSslHandler: SslErrorHandler

        @MockK
        private lateinit var mockSslError: SslError

        @Before
        fun mockReqs() {
            MockKAnnotations.init(this, relaxed = true)
            every { mockReq.url } returns Uri.parse("${app.config.baseUrl}/overlay")
            every { mockError.description } returns "mocked error"
            every { mockSslError.url } returns "${app.config.baseUrl}/overlay"
            every { mockResourceResponse.data } returns "mocked error".byteInputStream()

            trackPopUp1()

            shadowWebView?.resetLoadedUrls()
        }

        // TODO: Serverが500を返すケースとかの正しい再現。
        // WebViewはなぜかMockWebServerにrequestしないのでdispatcherじゃ再現できない。
        // 読み込みした後に手動でonReceivedErrorを呼び出す。
        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedError() {
            shadowWebView?.webViewClient?.onReceivedError(webView, mockReq, mockError)
            proceedBufferedCall()
            proceedUiBufferedCall()
            assertThat(iamWindow).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNoResponseDataPassed()
        }

        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedHttpError() {
            shadowWebView?.webViewClient?.onReceivedHttpError(
                webView,
                mockReq,
                mockResourceResponse
            )
            proceedBufferedCall()
            proceedUiBufferedCall()
            assertThat(iamWindow).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNoResponseDataPassed()
        }

        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedSslError() {
            shadowWebView?.webViewClient?.onReceivedSslError(webView, mockSslHandler, mockSslError)
            proceedBufferedCall()
            proceedUiBufferedCall()
            assertThat(iamWindow).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNoResponseDataPassed()
        }

        @Test
        fun overlayがエラー状態になった場合は何もしないがその後レスポンスは渡らない() {
            emitInitializedCallbackFromJs()
            assertActionInResponseData(popup1ActionId)
            shadowWebView?.resetLoadedUrls()

            // エラー後も表示状態のまま
            emitCbFromJs(
                "state_changed",
                JSONObject().put("state", "error").put("message", "error message")
            )
            assertThat(iamWindow).isNotNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNoResponseDataPassed()
        }
    }

    class ビジターID更新の制御 : InAppMessagingTestCase() {

        @Before
        fun setupOverlayAndJs() {
            trackCgPopUp()

            emitInitializedCallbackFromJs()
        }

        @Test
        fun renewVisitorIdが呼ばれた場合はWindowとWebViewが破棄され新しいvis_idでoverlayが読まれる() {
            val currentShadowWebView = shadowWebView
            KarteApp.renewVisitorId()
            proceedUiBufferedCall()

            assertThat(iamWindow).isNull()
            assertThat(currentShadowWebView?.lastLoadedUrl)
                .startsWith("javascript:window.tracker.resetPageState(")

            val newShadowWebView = shadowWebView
            assertThat(newShadowWebView?.lastLoadedUrl).startsWith(overlayBaseUrl)
            val uri = Uri.parse(newShadowWebView?.lastLoadedUrl)
            assertUrlIsCorrect(uri)
        }
    }

    class MessageSuppressedの発火 : InAppMessagingTestCase() {

        private fun assertNotSuppressed() {
            assertThat(
                dispatcher.trackedEvents()
                    .filter { it.getString("event_name") == "_message_suppressed" })
                .isEmpty()
        }

        private fun assertSuppressed(reasonMatch: String) {
            assertThat(dispatcher.trackedEvents().filter {
                it.getString("event_name") == "_message_suppressed" &&
                    it.getJSONObject("values").getString("reason").contains(reasonMatch)
            }).hasSize(1)
        }

        @Test
        fun Activity_not_found時はsuppressしない() {
            // Activityがあればsuppressされない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNotSuppressed()
            dispatcher.clearHistory()

            // 変更: ActiveなActivityがなくてもsuppressしない
            activity.pause()
            trackPopUp1()
            proceedBufferedCall()
            assertThat(iamWindow).isNull()
            assertNotSuppressed()
        }

        @Test
        fun suppress_mode() {
            // デフォルト値ではsuppressされない
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNotSuppressed()
            dispatcher.clearHistory()

            // suppressメソッドを呼ぶとsuppressされる
            InAppMessaging.suppress()
            proceedUiBufferedCall()
            trackPopUp1()
            proceedBufferedCall()
            assertThat(iamWindow).isNull()
            assertSuppressed("suppress mode")
            dispatcher.clearHistory()

            // unsuppressメソッドを呼ぶとsuppressされない
            InAppMessaging.unsuppress()
            trackPopUp1()
            assertThat(iamWindow).isNotNull()
            assertNotSuppressed()
        }

        @Test
        fun native_app_display_limit_mode() {
            // native_app_display_limit_modeがonな接客は同一画面(view)ならsuppressされない
            track("limited")
            assertThat(iamWindow).isNotNull()
            assertNotSuppressed()
            dispatcher.clearHistory()

            // 異なるviewが発火されたあとにレスポンスが来ると、suppressされる
            // 順番にthreadを実行するとsuppressされずにopenされるのでまとめる。
            Tracker.view("a_page")
            Tracker.track("limited")
            Tracker.view("another")
            proceedBufferedCall()
            proceedUiBufferedCall()
            proceedBufferedCall() // message_suppressed用
            assertSuppressed("native_app_display_limit_mode")
        }
    }
}
