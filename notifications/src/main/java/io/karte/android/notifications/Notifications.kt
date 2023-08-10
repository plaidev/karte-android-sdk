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
import io.karte.android.notifications.internal.TokenRegistrar
import io.karte.android.notifications.internal.command.RegisterPushCommandExecutor
import io.karte.android.notifications.internal.track.ClickTracker
import io.karte.android.utilities.ActivityLifecycleCallback
import java.lang.ref.WeakReference

private const val LOG_TAG = "Karte.Notifications"

/**
 * FCMトークンの登録やクリックイベントの送信を管理するクラスです。
 */
class Notifications : Library, ActivityLifecycleCallback() {
    private lateinit var registrar: TokenRegistrar
    private val clickTracker = ClickTracker
    private val commandExecutor: RegisterPushCommandExecutor = RegisterPushCommandExecutor()

    internal lateinit var app: KarteApp
    private var config: NotificationsConfig? = null
    internal var currentActivity: WeakReference<Activity>? = null

    //region Libraary
    override val name: String = "notifications"
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        self = this
        this.app = app
        config = app.libraryConfig(NotificationsConfig::class.java)
        app.application.registerActivityLifecycleCallbacks(this)
        registrar = TokenRegistrar(app.application)
        app.register(registrar)
        app.register(clickTracker)
        app.register(commandExecutor)
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.application.unregisterActivityLifecycleCallbacks(this)
        app.unregister(registrar)
        app.unregister(clickTracker)
        app.unregister(commandExecutor)
    }
    //endregion

    //region ActivityLifecycleCallback
    override fun onActivityResumed(activity: Activity) {
        Logger.v(LOG_TAG, "onActivityResumed $activity")
        if (enabledFCMTokenResend) registrar.registerFCMToken()
        currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = null
    }
    //endregion

    private val enabledFCMTokenResend: Boolean
        @Suppress("DEPRECATION")
        get() = config?.enabledFCMTokenResend ?: Config.enabledFCMTokenResend

    companion object {
        internal var self: Notifications? = null

        /**
         * FCM（Firebase Cloud Messaging）トークンを登録します。
         *
         * なお初期化が行われていない状態で呼び出した場合は登録処理は行われません。
         *
         * @param[token] FCMトークン
         */
        @JvmStatic
        fun registerFCMToken(token: String) {
            self?.registrar?.registerFCMToken(token)
        }
    }

    /**Notificationsモジュールの設定を保持するクラスです。*/
    @Deprecated("Renamed. Use 'NotificationsConfig'.")
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
