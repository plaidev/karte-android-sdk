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
import android.net.Uri
import android.net.http.SslError
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.assertThat
import io.karte.android.createControlGroupMessage
import io.karte.android.createMessage
import io.karte.android.createMessageOpen
import io.karte.android.createMessageResponse
import io.karte.android.createMessagesResponse
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.shadow.CustomShadowWebView
import io.karte.android.tearDownKarteApp
import io.karte.android.toList
import io.karte.android.tracking.Tracker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.util.Base64

private const val appKey = "sampleappkey"
private const val overlayBaseUrl = "https://cf-native.karte.io/v0/native"

private val popupMsg1 = createMessage(shortenId = "action1", pluginType = "webpopup")
private val popupMsg2 = createMessage(shortenId = "action2", pluginType = "webpopup")
private val cgPopupMsg = createControlGroupMessage()

@RunWith(ParameterizedRobolectricTestRunner::class)
abstract class InAppMessagingTestCase(private val webViewCache: Boolean = false) :
    RobolectricTestCase() {
    lateinit var server: MockWebServer
    lateinit var dispatcher: TrackerRequestDispatcher
    lateinit var activity: ActivityController<Activity>
    lateinit var app: KarteApp

    val view: ViewGroup?
        get() = (shadowOf(activity.get().windowManager) as ShadowWindowManagerImpl).views.getOrNull(
            0
        ) as? ViewGroup
    val webView: WebView?
        get() = view?.getChildAt(0) as? WebView
    val shadowWebView: CustomShadowWebView?
        get() = CustomShadowWebView.current

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun data(): Iterable<Array<Any?>> {
            // webViewCacheの有効・無効を両方テスト.
            return listOf(
                arrayOf<Any?>(true),
                arrayOf<Any?>(false)
            )
        }
    }

    @Before
    fun initTracker() {
        server = MockWebServer()
        dispatcher = object : TrackerRequestDispatcher() {
            override fun onTrackRequest(request: RecordedRequest): MockResponse {
                val body = JSONObject(request.body.clone().readUtf8())
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

                return MockResponse().setBody(createMessagesResponse(messages).toString())
            }
        }
        server.setDispatcher(dispatcher)
        server.start()

        app = setupKarteApp(server, appKey)
        InAppMessaging.Config.enabledWebViewCache = webViewCache
        activity = Robolectric.buildActivity(
            Activity::class.java,
            Intent(application, Activity::class.java)
        ).create().resume()

        //flush first native_app_install event
        proceedBufferedCall()
        server.takeRequest()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
        CustomShadowWebView.teardown()
    }

    fun trackPopUp1() {
        Tracker.track("popup1")
        proceedBufferedCall()
        emitVisibledCallbackFromTrackerJs()
    }

    fun trackCgPopUp() {
        Tracker.track("cg_popup")
        proceedBufferedCall()
    }

    protected fun emitCbFromTrackerJs(callbackName: String, data: JSONObject) {
        val mock = mockk<WebView>()
        every { mock.context } returns application
        val bridge = shadowWebView?.getJavascriptInterface("NativeBridge") as? IAMWebView
        bridge?.onReceivedMessage(callbackName, data.toString())
        proceedBufferedCall()
    }

    protected fun emitInitializedCallbackFromTrackerJs() {
        emitCbFromTrackerJs("state_changed", JSONObject().put("state", "initialized"))
    }

    protected fun emitVisibledCallbackFromTrackerJs() {
        emitCbFromTrackerJs("visibility", JSONObject().put("state", "visible"))
    }

    protected fun emitInvisibledCallbackFromTrackerJs() {
        emitCbFromTrackerJs("visibility", JSONObject().put("state", "invisible"))
    }

    protected fun responsesPassedToJs(): List<JSONObject> {
        val handleResponseDataHead = "javascript:window.tracker.handleResponseData('"
        val handleResponseDataTail = "');"
        val filtered = shadowWebView!!.loadedUrls.filter { it.startsWith(handleResponseDataHead) }
        val encodedStr = filtered.map {
            it.substring(
                handleResponseDataHead.length,
                it.length - handleResponseDataTail.length
            )
        }
        return encodedStr.map { JSONObject(String(Base64.getDecoder().decode(it))) }
    }
}

@RunWith(Enclosed::class)
class InAppMessagingTest {
    class IAMWebViewの初期化(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {
        @Test
        fun 最初のloadで正しいurlの読み込みを行うこと() {
            trackPopUp1()

            val shadow = shadowWebView ?: throw AssertionError()
            val uri = Uri.parse(shadow.loadedUrls.first())

            assertThat(uri.scheme).isEqualTo("https")
            assertThat(uri.host).isEqualTo("cf-native.karte.io")
            assertThat(uri.path).isEqualTo("/v0/native/overlay")
            assertThat(uri.queryParameterNames).isEqualTo(setOf("app_key", "_k_vid", "_k_app_prof"))
            assertThat(uri.getQueryParameter("app_key")).isEqualTo(appKey)
            assertThat(uri.getQueryParameter("_k_vid")).isEqualTo(KarteApp.visitorId)
            assertThat(uri.getQueryParameter("_k_app_prof")).isEqualTo(app.appInfo?.json.toString())
        }
    }

    class レスポンスハンドリング(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

        @Test
        fun webpopup接客を含むレスポンスが来た場合はoverlayが追加されること() {
            trackPopUp1()
            emitInitializedCallbackFromTrackerJs()
            emitVisibledCallbackFromTrackerJs()

            assertThat(webView).isNotNull()
            assertThat(view?.visibility).isEqualTo(View.VISIBLE)
            assertThat(shadowWebView?.lastLoadedUrl).startsWith("javascript:window.tracker.handleResponseData")
            assertThat(shadowWebView?.loadedUrls?.first()).startsWith(overlayBaseUrl)
            val uri = Uri.parse(shadowWebView?.loadedUrls?.first())
            assertThat(uri.queryParameterNames).isEqualTo(
                setOf(
                    "app_key",
                    "_k_vid",
                    "_k_app_prof"
                )
            )
            assertThat(uri.getQueryParameter("app_key")).isEqualTo(appKey)
            assertThat(uri.getQueryParameter("_k_vid")).isEqualTo(KarteApp.visitorId)
            assertThat(JSONObject(uri.getQueryParameter("_k_app_prof"))).isEqualTo(app.appInfo?.json)
        }

        @Test
        fun 未実施のwebpopup接客が来た場合はoverlayが追加されないこと() {
            trackCgPopUp()

            assertThat(view).isNull()
            assertThat(webView).isNull()
            assertThat(shadowWebView?.lastLoadedUrl).startsWith(overlayBaseUrl)
        }

        @Test
        fun webpopup接客を含まないレスポンスが来た場合はInAppMessagingViewが追加されないこと() {
            Tracker.track("hoge")
            proceedBufferedCall()

            assertThat(view).isNull()
        }

        @Test
        fun trackerJs読み込み中の場合はresponseが渡らないこと() {
            //overlay表示
            trackPopUp1()
            //2度目のpopup response
            trackPopUp1()

            assertThat(shadowWebView).isNotNull()
            assertThat(responsesPassedToJs()).isEmpty()
        }

        @Test
        fun trackerJs読み込み完了時にバッファされていたresponseが渡ること() {
            trackPopUp1()

            Tracker.track("popup2")
            proceedBufferedCall()

            assertThat(shadowWebView).isNotNull()
            emitInitializedCallbackFromTrackerJs()

            val passedResponses = responsesPassedToJs()
            passedResponses.forEach { it.remove("request_body") }

            assertThat(passedResponses).hasSize(2)
            assertThat(passedResponses[0]).isEqualTo(
                createMessageResponse(popupMsg1).getJSONObject(
                    "response"
                )
            )
            assertThat(passedResponses[1]).isEqualTo(
                createMessageResponse(popupMsg2).getJSONObject(
                    "response"
                )
            )

            assertThat(view?.visibility).isEqualTo(View.VISIBLE)
        }

        @Test
        fun trackerJs読み込み完了後に来たresponseがtrackerJsに渡ること() {
            trackPopUp1()

            assertThat(shadowWebView).isNotNull()
            emitInitializedCallbackFromTrackerJs()

            Tracker.track("popup2")
            proceedBufferedCall()

            val passedResponses = responsesPassedToJs()
            passedResponses.forEach { it.remove("request_body") }
            assertThat(passedResponses).hasSize(2)
            assertThat(passedResponses[1]).isEqualTo(
                createMessageResponse(popupMsg2).getJSONObject(
                    "response"
                )
            )
        }
    }

    class ページのリセット処理(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

        @Before
        fun setupOverlayAndTrackerJs() {
            Tracker.view("page1")
            trackPopUp1()

            emitInitializedCallbackFromTrackerJs()
        }

        @Test
        fun viewによりページが切り替わった時にInAppMessagingViewが破棄されること() {
            Tracker.view("page2")
            assertThat(view).isNull()
        }

        @Test
        fun ページが切り替わらなければresetPageStateが実行されないこと() {
            Tracker.track("hogehoge")
            assertThat(shadowWebView?.loadedUrls).isNotEmpty()
            assertThat(shadowWebView?.loadedUrls).doesNotContain("javascript:window.tracker.resetPageState();")
        }

        @Test
        fun onPauseによりページが切り替わった時に破棄処理が呼ばれること() {
            val currentShadowWebView = shadowWebView

            activity.pause()
            if (InAppMessaging.Config.enabledWebViewCache) {
                assertThat(currentShadowWebView?.loadedUrls).contains("javascript:window.tracker.resetPageState();")
            } else {
                assertThat(currentShadowWebView?.wasDestroyCalled()).isTrue()
            }
            assertThat(view).isNull()
        }
    }

    class バックボタンハンドリング(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

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

            val iamView = view
            assertThat(iamView).isNotNull()

            val consume = iamView?.dispatchKeyEvent(backButtonEvent())
            assertThat(consume).isTrue()
            // activityが終了しているはず.
            assertThat(activity.get().isFinishing).isTrue()
        }

        @Test
        fun スタックを積んだ時にバックボタンを押すとIAMViewが処理すること() {
            trackPopUp1()

            val iamView = view
            assertThat(iamView).isNotNull()

            webView?.loadUrl("test")
            assertThat(webView?.canGoBack()).isTrue()

            val consume = iamView?.dispatchKeyEvent(backButtonEvent())
            assertThat(consume).isTrue()
            // activityは終了しないはず.
            assertThat(activity.get().isFinishing).isFalse()
        }
    }

    class trackerJsからのコールバック(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

        @Before
        fun setupOverlayAndTrackerJs() {
            trackPopUp1()

            emitInitializedCallbackFromTrackerJs()
        }

        @Test
        fun イベントが来た場合はトラッキングされること() {
            emitCbFromTrackerJs(
                "event",
                JSONObject().put("event_name", "from_tracker_js").put(
                    "values",
                    JSONObject().put("foo", "bar")
                )
            )

            assertThat(dispatcher.trackedEvents().filter {
                it.getString("event_name") == "from_tracker_js" &&
                    it.getJSONObject("values").getString("foo") == "bar"
            }).isNotEmpty()
        }

        @Test
        fun open_urlが来た場合はIntentが投げられること() {
            emitCbFromTrackerJs("open_url", JSONObject().put("url", "test://hoge"))

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            val nextActivity = shadowOf(application).nextStartedActivity
            assertThat(nextActivity).isNotNull()
            assertThat(nextActivity.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(nextActivity.dataString).isEqualTo("test://hoge")
        }

        @Test
        fun visibility_invisibleによりoverlayが表示されること() {
            emitInitializedCallbackFromTrackerJs()
            emitVisibledCallbackFromTrackerJs()
            assertThat(view).isNotNull()
        }

        @Test
        fun visibility_invisibleによりoverlayが非表示になること() {
            emitInvisibledCallbackFromTrackerJs()
            assertThat(view).isNull()
        }
    }

    class trackerJsからのコールバック_CG(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

        @Before
        fun setupOverlayAndTrackerJs() {
            trackCgPopUp()

            emitInitializedCallbackFromTrackerJs()
        }

        @Test
        fun CGではoverlayが表示されないこと() {
            emitCbFromTrackerJs("event", createMessageOpen(shortenId = "__sample_shorten"))

            assertThat(view).isNull()
        }
    }

    class 読み込み失敗時の制御(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

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

        //TODO: Serverが500を返すケースとかの正しい再現。
        // WebViewはなぜかMockWebServerにrequestしないのでdispatcherじゃ再現できない。
        // 読み込みした後に手動でonReceivedErrorを呼び出す。
        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedError() {
            shadowWebView?.webViewClient?.onReceivedError(webView, mockReq, mockError)
            assertThat(view).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(view).isNotNull()
            assertThat(responsesPassedToJs()).isEmpty()
        }

        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedHttpError() {
            shadowWebView?.webViewClient?.onReceivedHttpError(
                webView,
                mockReq,
                mockResourceResponse
            )
            assertThat(view).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(view).isNotNull()
            assertThat(responsesPassedToJs()).isEmpty()
        }

        @Test
        fun overlayの読み込みに失敗する場合は非表示になりその後レスポンスは渡らない_onReceivedSslError() {
            shadowWebView?.webViewClient?.onReceivedSslError(webView, mockSslHandler, mockSslError)
            assertThat(view).isNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(view).isNotNull()
            assertThat(responsesPassedToJs()).isEmpty()
        }

        @Test
        fun trackerJsがエラー状態になった場合は何もしないがその後レスポンスは渡らない() {
            emitInitializedCallbackFromTrackerJs()
            assertThat(responsesPassedToJs()).hasSize(1)
            shadowWebView?.resetLoadedUrls()

            // エラー後も表示状態のまま
            emitCbFromTrackerJs(
                "state_changed",
                JSONObject().put("state", "error").put("message", "error message")
            )
            assertThat(view).isNotNull()

            // 以降のイベントはviewは追加されるが何もロードしない
            trackPopUp1()
            assertThat(view).isNotNull()
            assertThat(responsesPassedToJs()).isEmpty()
        }
    }

    class ビジターID更新の制御(webViewCache: Boolean) : InAppMessagingTestCase(webViewCache) {

        @Before
        fun setupOverlayAndTrackerJs() {
            trackCgPopUp()

            emitInitializedCallbackFromTrackerJs()
        }

        @Test
        fun trackerのrenewVisitorIdが呼ばれた場合はInAppMessagingViewが破棄され新しいVidでTrackerJsが読まれる() {
            val currentShadowWebView = shadowWebView
            KarteApp.renewVisitorId()
            proceedBufferedCall()

            assertThat(view).isNull()
            if (InAppMessaging.Config.enabledWebViewCache) {
                assertThat(currentShadowWebView?.lastLoadedUrl).startsWith(overlayBaseUrl)
                val uri = Uri.parse(currentShadowWebView?.lastLoadedUrl)
                assertThat(uri.queryParameterNames).isEqualTo(
                    setOf(
                        "app_key",
                        "_k_vid",
                        "_k_app_prof"
                    )
                )
                assertThat(uri.getQueryParameter("app_key")).isEqualTo(appKey)
                assertThat(uri.getQueryParameter("_k_vid")).isEqualTo(KarteApp.visitorId)
                assertThat(JSONObject(uri.getQueryParameter("_k_app_prof"))).isEqualTo(app.appInfo?.json)
            } else {
                assertThat(currentShadowWebView?.wasDestroyCalled()).isTrue()
            }
        }
    }
}
