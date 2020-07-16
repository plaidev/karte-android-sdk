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
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import io.karte.android.core.logger.Logger
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.internal.view.WindowView

private const val LOG_TAG = "Karte.IAMView"
private const val FRAGMENT_TAG = "Karte.FileChooserFragment"

@SuppressLint("ViewConstructor")
@TargetApi(Build.VERSION_CODES.KITKAT)
internal class IAMWindow(
    activity: Activity,
    panelWindowManager: PanelWindowManager,
    private val webView: IAMWebView
) : WindowView(activity, panelWindowManager), Window,
    ParentView {
    override var presenter: IAMPresenter? = null

    override val isShowing: Boolean
        get() = visibility == VISIBLE && isAttachedToWindow

    init {
        if (webView.parent != null) {
            Logger.e(LOG_TAG, "webView already has Parent View!")
            (webView.parent as ViewGroup).removeView(webView)
        }
        this.addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        webView.parentView = this
        webView.setWebChromeClient(object : WebChromeClient() {

            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                AlertDialogFragment.show(activity, message)
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
            ): Boolean {
                val fileChooserListener = { uris: Array<Uri>? ->
                    filePathCallback.onReceiveValue(uris)
                }
                if (activity is FragmentActivity) {
                    val fragment = FileChooserFragment.newInstance()
                    fragment.listener = fileChooserListener

                    val transaction = activity.supportFragmentManager.beginTransaction()
                    transaction.add(fragment, FRAGMENT_TAG)
                    transaction.commit()
                } else {
                    val fragment = FileChooserDeprecatedFragment.newInstance()
                    fragment.listener = fileChooserListener

                    val transaction = activity.fragmentManager.beginTransaction()
                    transaction.add(fragment, FRAGMENT_TAG)
                    transaction.commit()
                }
                return true
            }
        })
    }

    override fun destroy(isForceClose: Boolean) {
        webView.webChromeClient = null
        webView.parentView = null
        postDelayed({
            this.removeView(webView)
            dismiss()
        }, 50)
        webView.reset(isForceClose)
    }

    override fun show() {
        super.show()
        InAppMessaging.delegate?.onWindowPresented()
    }

    override fun dismiss() {
        super.dismiss()
        InAppMessaging.delegate?.onWindowDismissed()
    }

    override fun openUrl(uri: Uri, withReset: Boolean) {
        Logger.d(LOG_TAG, "Opening url: $uri")
        try {
            val intent = if (uri.scheme == "app-settings") {
                val uriString = "package:${context.packageName}"
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
            } else {
                Intent(Intent.ACTION_VIEW).apply { data = uri }
            }
            if (!withReset) {
                (context as? Activity)?.let { InAppMessaging.self?.enablePreventRelayFlag(it) }
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Logger.e(LOG_TAG, "Failed to open url.", e)
        }
    }

    override fun errorOccurred() {
        presenter?.destroy()
    }
}

internal class AlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val builder = AlertDialog.Builder(activity)
        val message = arguments.getString("message")
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok, null)
        return builder.create()
    }

    companion object {

        internal fun show(activity: Activity, message: String) {
            val alertDialogFragment = AlertDialogFragment()
            val bundle = Bundle()
            bundle.putString("message", message)
            alertDialogFragment.arguments = bundle
            alertDialogFragment.show(activity.fragmentManager, "krt_alert_dialog")
        }
    }
}
