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

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.ValueCallback
import io.karte.android.KarteApp
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.view.AlertDialogFragment
import io.karte.android.inappmessaging.internal.view.FileChooserFragment
import io.karte.android.utilities.ActivityLifecycleCallback
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

private const val LOG_TAG = "Karte.IAMProcessor"

internal class IAMProcessor(application: Application, private val panelWindowManager: PanelWindowManager) : ActivityLifecycleCallback(), WebViewDelegate {
    private val container = WebViewContainer(application, this)
    private val webView: IAMWebView?
        get() = container.get()
    private var window: IAMWindow? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var isWindowFocusByCross = false
    private var isWindowFocus = false

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    // region public
    val isPresenting: Boolean
        get() = window?.isShowing == true

    fun teardown() {
        dismiss()
        webView?.destroy()
        currentActivity = null
    }

    fun handle(message: MessageModel) {
        setWindowFocus(message)
        webView?.handleResponseData(message.string)
    }

    fun handleChangePv() {
        webView?.handleChangePv()
    }

    fun handleView(values: JSONObject) {
        webView?.handleView(values)
    }

    fun reset(isForce: Boolean) {
        webView?.reset(isForce)
    }

    fun reload(url: String? = null) {
        val targetUrl = url ?: InAppMessaging.self?.generateOverlayURL()
        if (webView?.url == targetUrl || targetUrl == null) {
            webView?.reload()
        } else {
            container.renew(targetUrl)
        }
    }
    // endregion

    private fun setWindowFocus(message: MessageModel) {
        if (!isWindowFocusByCross) isWindowFocusByCross = message.shouldFocusCrossDisplayCampaign()
        // cross-display接客がfocusをとるときはreceiveごとに上書きしない
        isWindowFocus = if (isWindowFocusByCross) {
            true
        } else {
            message.shouldFocus()
        }
    }

    private fun show(activity: Activity) {
        if (window?.activity == activity) {
            window?.show()
            return
        }
        dismiss()

        window = IAMWindow(activity, panelWindowManager)
        window?.show(isWindowFocus, webView)
    }

    private fun dismiss(withDelay: Boolean = false) {
        window?.dismiss(withDelay)
        window = null
    }

    //region WebViewDelegate
    override fun onWebViewVisible() {
        Logger.d(LOG_TAG, "onWebViewVisible")
        currentActivity?.get()?.let {
            show(it)
        }
    }

    override fun onWebViewInvisible() {
        Logger.d(LOG_TAG, "onWebViewInvisible")
        dismiss()
        isWindowFocus = false
        isWindowFocusByCross = false
    }

    override fun onUpdateTouchableRegions(touchableRegions: JSONArray) {
        window?.updateTouchableRegions(touchableRegions)
    }

    override fun shouldOpenUrl(uri: Uri): Boolean {
        Logger.d(LOG_TAG, "shouldOpenURL ${InAppMessaging.delegate}")
        return InAppMessaging.delegate?.shouldOpenURL(uri) ?: true
    }

    override fun onOpenUrl(uri: Uri, withReset: Boolean) {
        if (!withReset) {
            currentActivity?.get().let { ResetPrevent.enablePreventResetFlag(it) }
        }
        KarteApp.openUrl(uri, currentActivity?.get())
    }

    override fun onErrorOccurred() {
        reset(true)
        dismiss()
    }

    override fun onShowAlert(message: String) {
        currentActivity?.get()?.let { AlertDialogFragment.show(it, message) }
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>): Boolean {
        val activity = currentActivity?.get() ?: return false
        return FileChooserFragment.showFileChooser(activity, filePathCallback)
    }
    //endregion

    //region ActivityLifecycle
    override fun onActivityResumed(activity: Activity) {
        Logger.d(LOG_TAG, "onActivityResumed: visible? ${webView?.visible}")
        currentActivity = WeakReference(activity)
        // アプリフォアグラウンド時にはwebViewが参照され、初期化される
        if (webView?.visible == true) {
            show(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // FileChooser等reset防止時以外はresetする.
        val isPreventReset = ResetPrevent.isPreventReset(activity)
        Logger.d(LOG_TAG, "onActivityPaused: should prevent reset? $isPreventReset")
        if (!isPreventReset) {
            reset(false)
            dismiss(true)
        }
        currentActivity = null
    }
    //endregion
}

private class WebViewContainer(private val application: Application, private val delegate: WebViewDelegate) {
    private var _webView: IAMWebView? = null
    fun get(): IAMWebView? {
        if (_webView == null) {
            setupWebView()
        }
        return _webView
    }

    fun renew(url: String) {
        _webView?.destroy()
        _webView = null
        setupWebView(url)
    }

    private fun setupWebView(url: String? = null) {
        try {
            _webView = IAMWebView(application, delegate)
            val targetUrl = url ?: InAppMessaging.self?.generateOverlayURL()
            if (targetUrl != null) {
                _webView?.loadUrl(targetUrl)
            } else {
                Logger.e(LOG_TAG, "Failed to construct overlay url.")
            }
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
    }
}
