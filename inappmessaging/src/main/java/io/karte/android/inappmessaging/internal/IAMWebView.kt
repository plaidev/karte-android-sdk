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

package io.karte.android.inappmessaging.internal

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.javascript.JsMessage
import io.karte.android.inappmessaging.internal.javascript.State
import io.karte.android.inappmessaging.internal.view.BaseWebView
import io.karte.android.tracking.CustomEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.MessageEventName
import io.karte.android.tracking.Tracker
import org.json.JSONArray
import org.json.JSONObject

private const val LOG_TAG = "Karte.IAMWebView"

@SuppressLint("ViewConstructor", "AddJavascriptInterface")
internal class IAMWebView(context: Context, private val delegate: WebViewDelegate) : BaseWebView(context) {
    var visible: Boolean = false
    private val uiThreadHandler: Handler = Handler(Looper.getMainLooper())
    internal var state: State = State.LOADING
        private set
    private val queue: MutableList<Data> = mutableListOf()
    private val isReady
        get() = state == State.READY

    init {
        addJavascriptInterface(this, "NativeBridge")
    }

    override fun reload() {
        super.reload()
        state = State.LOADING
    }

    fun reset(isForce: Boolean = false) {
        if (!isReady) {
            Logger.d(LOG_TAG, "overlay not ready, canceled: resetPageState($isForce)")
            return
        }
        Logger.d(LOG_TAG, "resetPageState($isForce)")
        loadUrl("javascript:window.tracker.resetPageState($isForce);")
    }

    fun handleChangePv() {
        if (!isReady) {
            Logger.d(LOG_TAG, "overlay not ready, canceled: handleChangePv()")
            return
        }
        Logger.d(LOG_TAG, "handleChangePv()")
        loadUrl("javascript:window.tracker.handleChangePv();")
    }

    fun handleView(values: JSONObject) {
        if (isReady) {
            val viewName = values.optString("view_name")
            val title = values.optString("title")
            Logger.d(LOG_TAG, "handleView($viewName, $title)")
            loadUrl("javascript:window.tracker.handleView('$viewName', '$title');")
        } else {
            Logger.d(LOG_TAG, "handleView(), queueing")
            uiThreadHandler.post {
                queue.add(Data.ViewData(values))
            }
        }
    }

    fun handleResponseData(data: String) {
        if (isReady) {
            Logger.d(LOG_TAG, "handleResponseData()")
            loadUrl("javascript:window.tracker.handleResponseData('$data');")
        } else {
            Logger.d(LOG_TAG, "handleResponseData(), queuing")
            uiThreadHandler.post {
                queue.add(Data.ResponseData(data))
            }
        }
    }

    private fun changeState(newState: State) {
        if (state == newState)
            return
        state = newState
        if (isReady) {
            Logger.d(LOG_TAG, "Js state: $newState")
            queue.forEach {
                when (it) {
                    is Data.ResponseData -> handleResponseData(it.string)
                    is Data.ViewData -> handleView(it.values)
                }
            }
            queue.clear()
        } else {
            Logger.w(LOG_TAG, "Js state: $newState")
            // TODO: reload overlay or dismiss, destroy();
        }
    }

    @JavascriptInterface
    fun onReceivedMessage(name: String, data: String) {
        Logger.d(LOG_TAG, "onReceivedMessage")
        uiThreadHandler.post { handleJsMessage(name, data) }
    }

    private fun handleJsMessage(name: String, data: String) {
        Logger.d(LOG_TAG, "handleJsMessage")
        try {
            when (val message = JsMessage.parse(name, data)) {
                is JsMessage.Event -> {
                    Logger.d(LOG_TAG, "Received event callback: event_name=${message.eventName}")
                    Tracker.track(Event(CustomEventName(message.eventName), message.values, libraryName = InAppMessaging.name))
                    notifyCampaignOpenOrClose(message.eventName, message.values)
                }
                is JsMessage.StateChanged -> {
                    Logger.d(LOG_TAG, "Received state_change callback: state=${message.state}")
                    changeState(State.of(message.state))
                }
                is JsMessage.OpenUrl -> {
                    Logger.d(LOG_TAG, "Received open_url callback: url=${message.uri}")
                    tryOpenUrl(message.uri, message.withReset)
                }
                is JsMessage.DocumentChanged -> {
                    Logger.d(
                        LOG_TAG,
                        "Received document_changed callback: touchable_regions=${message.regions}"
                    )
                    delegate.onUpdateTouchableRegions(message.regions)
                }
                is JsMessage.Visibility -> {
                    Logger.d(LOG_TAG, "Received visibility callback: visible=${message.visible}")
                    if (message.visible) {
                        visible = true
                        delegate.onWebViewVisible()
                    } else {
                        visible = false
                        delegate.onWebViewInvisible()
                    }
                }
                else -> Logger.w(
                    LOG_TAG,
                    "Unknown callback $name was passed from WebView"
                )
            } // noop
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to parse callback url.", e)
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

    private fun tryOpenUrl(uri: Uri, withReset: Boolean = true) {
        if (delegate.shouldOpenUrl(uri)) {
            delegate.onOpenUrl(uri, withReset)
        }
    }

    override fun setSafeAreaInset(top: Int) {
        if (!isReady) {
            Logger.d(LOG_TAG, "overlay not ready, canceled: setSafeAreaInset($top)")
            return
        }
        Logger.d(LOG_TAG, "setSafeAreaInset($top)")
        loadUrl("javascript:window.tracker.setSafeAreaInset($top);")
    }

    override fun errorOccurred() {
        delegate.onErrorOccurred()
    }

    override fun openUrl(uri: Uri) {
        tryOpenUrl(uri)
    }

    override fun showAlert(message: String) {
        delegate.onShowAlert(message)
    }

    override fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean {
        return delegate.onShowFileChooser(filePathCallback)
    }
}

private sealed class Data {
    class ResponseData(val string: String) : Data()
    class ViewData(val values: JSONObject) : Data()
}

internal interface WebViewDelegate {
    fun onWebViewVisible()
    fun onWebViewInvisible()
    fun onUpdateTouchableRegions(touchableRegions: JSONArray)
    fun shouldOpenUrl(uri: Uri): Boolean
    fun onOpenUrl(uri: Uri, withReset: Boolean = true)
    fun onErrorOccurred()
    fun onShowAlert(message: String)
    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean
}
