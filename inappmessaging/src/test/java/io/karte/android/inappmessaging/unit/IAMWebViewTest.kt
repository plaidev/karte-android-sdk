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
package io.karte.android.inappmessaging.unit

import android.net.Uri
import android.net.http.SslError
import android.view.KeyEvent
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import com.google.common.truth.Truth.assertThat
import io.karte.android.inappmessaging.internal.IAMWebView
import io.karte.android.inappmessaging.internal.MessageModel
import io.karte.android.inappmessaging.internal.WebViewDelegate
import io.karte.android.inappmessaging.internal.javascript.State
import io.karte.android.test_lib.application
import io.karte.android.test_lib.proceedUiBufferedCall
import io.karte.android.test_lib.shadow.CustomShadowWebView
import io.karte.android.test_lib.shadow.customShadowOf
import io.karte.android.tracking.Event
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.client.TrackRequest
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowWebView

@Suppress("NonAsciiCharacters")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], shadows = [CustomShadowWebView::class])
class IAMWebViewTest {
    private fun makeStateReady() {
        webViewReceivedMessage(
            "state_changed",
            JSONObject().put("state", "initialized")
        )
    }

    private fun webViewReceivedMessage(name: String, data: JSONObject) {
        webView.onReceivedMessage(name, data.toString())
        proceedUiBufferedCall()
    }

    private fun assertUrlLoaded(expectUrl: String) {
        val loadedUrls = customShadowOf(webView).loadedUrls
        assertThat(loadedUrls.size).isEqualTo(1)
        assertThat(loadedUrls.last()).isEqualTo(expectUrl)
    }

    private val dummyUrl = "https://dummy_url/test"
    private val overlayUrl = "https://api.karte.io/v0/native/overlay"
    private val emptyData = "<html></html>"
    private val dummyMessage = MessageModel(JSONObject(), TrackRequest("", "", "", "", listOf()))

    private lateinit var webView: IAMWebView
    private lateinit var shadowWebView: ShadowWebView
    private lateinit var customShadowWebView: CustomShadowWebView

    @MockK
    private lateinit var delegate: WebViewDelegate

    @Before
    fun init() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockKarteApp()
        webView = IAMWebView(application(), delegate)
        shadowWebView = Shadows.shadowOf(webView)
        customShadowWebView = customShadowOf(webView)
        every { delegate.shouldOpenUrl(any()) } returns true
    }

    @After
    fun tearDown() {
        customShadowWebView.resetLoadedUrls()
        unmockKarteApp()
    }

    @Test
    fun JsMessageでtrackが呼ばれること() {
        mockkStatic(Tracker::class)
        val eventSlot = slot<Event>()
        every { Tracker.track(capture(eventSlot)) } just Runs

        val values = JSONObject().put("samplekey", "samplevalue")

        webViewReceivedMessage(
            "event",
            JSONObject().put("event_name", "some_event").put("values", values)
        )
        verify(exactly = 1) { Tracker.track(any<Event>()) }
        assertThat(eventSlot.captured.eventName.value).isEqualTo("some_event")
        assertThat(eventSlot.captured.values.toString()).isEqualTo(values.toString())
    }

    @Test
    fun overlayの読み込み後にはすぐに読み込むこと() {
        // Ready前のイベントはすぐに読み込まない
        webView.handleResponseData(dummyMessage.string)
        assertThat(customShadowWebView.loadedUrls).isEmpty()
        makeStateReady()
        assertUrlLoaded("javascript:window.tracker.handleResponseData('e30=');")
        customShadowWebView.resetLoadedUrls()

        webView.handleResponseData(dummyMessage.string)
        webView.handleResponseData(dummyMessage.string)
        val loadedUrls = customShadowWebView.loadedUrls
        assertThat(loadedUrls.size).isEqualTo(2)
        loadedUrls.forEach { uri ->
            assertThat(uri).isEqualTo("javascript:window.tracker.handleResponseData('e30=');")
        }
    }

    @Test
    fun StateChange_initializedでqueueから読み取るされること() {
        webView.handleResponseData(dummyMessage.string)
        webView.handleResponseData(dummyMessage.string)
        assertThat(customShadowWebView.loadedUrls).isEmpty()

        makeStateReady()
        Assert.assertEquals(webView.state, State.READY)
        val loadedUrls = customShadowWebView.loadedUrls
        assertThat(loadedUrls.size).isEqualTo(2)
        loadedUrls.forEach { uri ->
            assertThat(uri).isEqualTo("javascript:window.tracker.handleResponseData('e30=');")
        }
    }

    @Test
    fun StateChange_errorでstateがDESTROYEDに変更されること() {
        webViewReceivedMessage(
            "state_changed", JSONObject()
                .put("state", "error")
                .put("message", "samplemessage")
        )

        Assert.assertEquals(webView.state, State.DESTROYED)
    }

    @Test
    fun startActivityOnOpenUrlCallback() {
        webViewReceivedMessage(
            "open_url",
            JSONObject().put("url", "http://sampleurl")
        )

        verify(exactly = 1) { delegate.onOpenUrl(Uri.parse("http://sampleurl")) }
    }

    @Test
    fun startActivityOnOpenUrlCallbackWithQueryParameters() {
        webViewReceivedMessage(
            "open_url",
            JSONObject().put("url", "http://sampleurl?hoge=fuga&hogehoge=fugafuga")
        )

        verify(exactly = 1) {
            delegate.onOpenUrl(Uri.parse("http://sampleurl?hoge=fuga&hogehoge=fugafuga"))
        }
    }

    @Test
    fun DocumentChangedでonUpdateTouchableRegionsが呼ばれること() {
        webViewReceivedMessage(
            "document_changed", JSONObject()
                .put(
                    "touchable_regions", JSONArray().put(
                        JSONObject()
                            .put("top", 10.0)
                            .put("bottom", 10.0)
                            .put("left", 10.0)
                            .put("right", 10.0)
                    )
                )
        )

        verify(exactly = 1) { delegate.onUpdateTouchableRegions(ofType()) }
    }

    @Test
    fun Visibility_visibleでonWebViewVisibleが呼ばれること() {
        webViewReceivedMessage("visibility", JSONObject().put("state", "visible"))
        verify(exactly = 1) { delegate.onWebViewVisible() }
    }

    @Test
    fun Visibility_invisibleでonWebViewInvisibleが呼ばれること() {
        webViewReceivedMessage("visibility", JSONObject().put("state", "invisible"))
        verify(exactly = 1) { delegate.onWebViewInvisible() }
    }

    @Test
    fun backボタンの処理を行うこと() {
        val backKey = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)

        webView.loadUrl(dummyUrl)
        assertThat(webView.canGoBack()).isFalse()
        assertThat(webView.dispatchKeyEvent(backKey)).isFalse()

        webView.loadUrl(dummyUrl)
        assertThat(webView.canGoBack()).isTrue()
        assertThat(webView.dispatchKeyEvent(backKey)).isTrue()
        assertThat(webView.canGoBack()).isFalse()
    }

    @Test
    fun onReceivedSslErrorでoverlayのロードに失敗するとdelegateがhandleする() {
        val error = mockk<SslError>()
        val handler = mockk<SslErrorHandler>(relaxUnitFun = true)

        // urlが異なる時はempty_dataをloadしない
        every { error.url } returns dummyUrl
        shadowWebView.webViewClient.onReceivedSslError(webView, handler, error)
        assertThat(shadowWebView.lastLoadData).isNull()
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlが同じ時はempty_dataをload
        webView.loadUrl(dummyUrl)
        every { error.url } returns dummyUrl
        shadowWebView.webViewClient.onReceivedSslError(webView, handler, error)
        assertThat(shadowWebView.lastLoadData.data).isEqualTo(emptyData)
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlがoverlayの時はdelegateに伝える
        every { error.url } returns overlayUrl
        shadowWebView.webViewClient.onReceivedSslError(webView, handler, error)
        verify(exactly = 1) { delegate.onErrorOccurred() }
    }

    @Test
    fun onReceivedHttpErrorでoverlayのロードに失敗するとdelegateがhandleする() {
        val webResourceRequest = mockk<WebResourceRequest>()

        // urlが異なる時はempty_dataをloadしない
        every { webResourceRequest.url } returns Uri.parse(dummyUrl)
        shadowWebView.webViewClient.onReceivedHttpError(webView, webResourceRequest, null)
        assertThat(shadowWebView.lastLoadData).isNull()
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlが同じ時はempty_dataをload
        webView.loadUrl(dummyUrl)
        every { webResourceRequest.url } returns Uri.parse(dummyUrl)
        shadowWebView.webViewClient.onReceivedHttpError(webView, webResourceRequest, null)
        assertThat(shadowWebView.lastLoadData.data).isEqualTo(emptyData)
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlがoverlayの時はdelegateに伝える
        every { webResourceRequest.url } returns Uri.parse(overlayUrl)
        shadowWebView.webViewClient.onReceivedHttpError(webView, webResourceRequest, null)
        verify(exactly = 1) { delegate.onErrorOccurred() }
    }

    @Test
    fun onReceivedErrorでoverlayのロードに失敗するとdelegateがhandleする() {
        val description = "dumy error reason"
        val request = mockk<WebResourceRequest>(relaxed = true)
        val error = mockk<WebResourceError>()
        every { error.description } returns description
        every { request.url } returns Uri.parse(dummyUrl)
        // urlが異なる時はempty_dataをloadしない
        shadowWebView.webViewClient.onReceivedError(webView, request, error)
        assertThat(shadowWebView.lastLoadData).isNull()
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlが同じ時はempty_dataをload
        webView.loadUrl(dummyUrl)
        shadowWebView.webViewClient.onReceivedError(webView, request, error)
        assertThat(shadowWebView.lastLoadData.data).isEqualTo(emptyData)
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlがoverlayの時はdelegateに伝える
        every { request.url } returns Uri.parse(overlayUrl)
        shadowWebView.webViewClient.onReceivedError(webView, request, error)
        verify(exactly = 1) { delegate.onErrorOccurred() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun 古いonReceivedErrorでoverlayのロードに失敗するとdelegateがhandleする() {
        val errorCode = 0
        val description = "dummy description"
        // urlが異なる時はempty_dataをloadしない
        shadowWebView.webViewClient.onReceivedError(webView, errorCode, description, dummyUrl)
        assertThat(shadowWebView.lastLoadData).isNull()
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlが同じ時はempty_dataをload
        webView.loadUrl(dummyUrl)
        shadowWebView.webViewClient.onReceivedError(webView, errorCode, description, dummyUrl)
        assertThat(shadowWebView.lastLoadData.data).isEqualTo(emptyData)
        verify(inverse = true) { delegate.onErrorOccurred() }

        // urlがoverlayの時はdelegateに伝える
        shadowWebView.webViewClient.onReceivedError(webView, errorCode, description, overlayUrl)
        verify(exactly = 1) { delegate.onErrorOccurred() }
    }

    @Test
    fun pvId更新時にhandleChangePvとresetされること() {
        makeStateReady()
        webView.handleChangePv()
        assertUrlLoaded("javascript:window.tracker.handleChangePv();")
    }

    @Test
    fun 非表示時にresetされること() {
        makeStateReady()
        webView.reset(true)
        assertUrlLoaded("javascript:window.tracker.resetPageState(true);")
    }
}
