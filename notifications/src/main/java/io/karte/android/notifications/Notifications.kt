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
package io.karte.android.notifications

import android.app.Activity
import io.karte.android.KarteApp
import io.karte.android.core.library.Library
import io.karte.android.core.logger.Logger
import io.karte.android.notifications.internal.MessageClickTracker
import io.karte.android.notifications.internal.TokenRegistrar
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.Values
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.utilities.getLowerClassName

private const val LOG_TAG = "Karte.Notifications"

/**
 * FCMトークンの登録やクリックイベントの送信を管理するクラスです。
 */
class Notifications : Library, ActivityLifecycleCallback() {
    private lateinit var registrar: TokenRegistrar
    private val clickTracker = MessageClickTracker
    private lateinit var app: KarteApp

    //region Libraary
    override val name: String = getLowerClassName()
    override val version: String = BuildConfig.VERSION_NAME
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        self = this
        this.app = app
        app.application.registerActivityLifecycleCallbacks(this)
        registrar = TokenRegistrar(app.application)
        app.register(registrar)
        app.register(clickTracker)
    }

    override fun unconfigure(app: KarteApp) {
        app.application.unregisterActivityLifecycleCallbacks(this)
        app.unregister(registrar)
        app.unregister(clickTracker)
    }
    //endregion

    //region AcitivityLifecycleCallback
    override fun onActivityResumed(activity: Activity) {
        Logger.v(LOG_TAG, "onActivityResumed $activity")
        if (Config.enabledFCMTokenResend) registrar.registerFCMToken()
    }
    //endregion

    companion object {
        internal lateinit var self: Notifications

        /**
         * FCM（Firebase Cloud Messaging）トークンを登録します。
         *
         * なお初期化が行われていない状態で呼び出した場合は登録処理は行われません。
         *
         * @param[token] FCMトークン
         */
        @JvmStatic
        fun registerFCMToken(token: String) {
            self.registrar.registerFCMToken(token)
        }
    }

    /**Notificationsモジュールの設定を保持するクラスです。*/
    object Config {
        /**
         * FCMTokenの自動送信の有無の取得・設定を行います。
         *
         * `true` の場合はFCMTokenの自動送信が有効となり、`false` の場合は無効となります。デフォルトは `true` です。
         */
        @JvmStatic
        var enabledFCMTokenResend = true
    }
}

internal class MassPushClickEvent(values: Values) :
    Event(PushEventName.MassPushClick, values)

internal class PluginNativeAppIdentifyEvent(subscribe: Boolean, token: String? = null) :
    Event(
        PushEventName.PluginNativeAppIdentify,
        values = mutableMapOf<String, Any>("subscribe" to subscribe).apply {
            if (token != null) this["fcm_token"] = token
        })

private enum class PushEventName(override val value: String) : EventName {
    MassPushClick("mass_push_click"),
    PluginNativeAppIdentify("plugin_native_app_identify"),
}

/**
 * FCM（Firebase Cloud Messaging）トークンを登録します。
 *
 * なお初期化が行われていない状態で呼び出した場合は登録処理は行われません。
 *
 * @param[token] FCMトークン
 */
fun KarteApp.Companion.registerFCMToken(token: String) {
    Notifications.registerFCMToken(token)
}
