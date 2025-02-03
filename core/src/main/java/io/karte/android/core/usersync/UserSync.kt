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
package io.karte.android.core.usersync

import android.net.Uri
import android.util.Base64
import android.webkit.WebView
import io.karte.android.KarteApp
import io.karte.android.core.logger.Logger
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

private const val LOG_TAG = "Karte.UserSync"
private const val QUERY_KEY_USER_SYNC = "_k_ntvsync_b"

private const val PARAM_OPT_OUT = "_karte_tracker_deactivate"
private const val PARAM_VISITOR_ID = "visitor_id"
private const val PARAM_APP_INFO = "app_info"
private const val PARAM_TIMESTAMP = "ts"

/**
 * WebView 連携するためのクラスです。
 *
 * Webページを開くWebViewに連携用のスクリプトを設定することで、WebとAppのユーザーの紐付けが行われます。
 *
 * なお連携を行うためにはWebページに、KARTEのタグが埋め込まれている必要があります。
 */
object UserSync {
    /**
     * 指定されたURL文字列にWebView連携用のクエリパラメータを付与します。
     *
     * @param[url] 連携するページのURL文字列
     * @return 連携用のクエリパラメータを付与したURL文字列を返します。
     * 指定されたURL文字列の形式が正しくない場合、またはSDKの初期化が行われていない場合は、引数に指定したURL文字列を返します。
     */
    @JvmStatic
    @Deprecated("User sync function using query parameters is deprecated. It will be removed in the future.", ReplaceWith("setUserSyncScript(webView)"))
    fun appendUserSyncQueryParameter(url: String): String {
        @Suppress("DEPRECATION")
        return appendUserSyncQueryParameter(Uri.parse(url))
    }

    /**
     * 指定されたUriにWebView連携用のクエリパラメータを付与します。
     *
     * @param[uri] 連携するページのUriインスタンス
     * @return 連携用のクエリパラメータを付与したURL文字列を返します。
     * SDKの初期化が行われていない場合は、引数に指定したUriを文字列で返します。
     */
    @JvmStatic
    @Deprecated("User sync function using query parameters is deprecated. It will be removed in the future.", ReplaceWith("setUserSyncScript(webView)"))
    fun appendUserSyncQueryParameter(uri: Uri): String {
        val param = buildUserSyncParameter() ?: return uri.toString()

        val base64EncodedParam = Base64.encodeToString(param.toByteArray(), Base64.NO_WRAP)

        val builder = uri.buildUpon()
        return builder
            .appendQueryParameter(QUERY_KEY_USER_SYNC, base64EncodedParam)
            .build()
            .toString()
    }

    /**
     * WebView 連携用のスクリプト(javascript)を返却します。
     *
     * ユーザースクリプトとしてWebViewに設定することで、WebView内のタグと連携されます。
     *
     * なおSDKの初期化が行われていない場合はnullを返却します。
     * @param[webView] [WebView]
     */
    @JvmStatic
    fun getUserSyncScript(): String? {
        val syncParam = buildUserSyncParameter() ?: return null
        return String.format("window.__karte_ntvsync = %s;", syncParam)
    }

    /**
     * WebViewに連携用のスクリプトを設定します。
     *
     * スクリプトはユーザースクリプトとして設定されます。
     *
     * なおSDKの初期化が行われていない場合は設定されません。
     * @param[webView] [WebView]
     */
    @JvmStatic
    fun setUserSyncScript(
        webView: WebView
    ) {
        val syncScript = getUserSyncScript() ?: return
        webView.evaluateJavascript(syncScript) { }
    }

    private fun buildUserSyncParameter(): String? {
        try {
            if (!KarteApp.self.isInitialized) {
                Logger.w(LOG_TAG, "KarteApp.setup is not called.")
                return null
            }
            if (KarteApp.isOptOut) {
                return JSONObject().put(PARAM_OPT_OUT, true).toString()
            }
            val param = JSONObject()
                .put(PARAM_VISITOR_ID, KarteApp.visitorId)
                .put(PARAM_APP_INFO, KarteApp.self.appInfo?.json)
                .put(PARAM_TIMESTAMP, Date().time / 1000)
            return param.toString()
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "failed to create user sync param", e)
            return null
        }
    }
}
