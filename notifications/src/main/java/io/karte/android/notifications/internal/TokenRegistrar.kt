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
package io.karte.android.notifications.internal

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.iid.FirebaseInstanceId
import io.karte.android.core.library.NotificationModule
import io.karte.android.core.library.UserModule
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.Tracker

private const val LOG_TAG = "Karte.Notifications.TokenRegistrar"

internal class TokenRegistrar(private val context: Context) : UserModule, NotificationModule {

    //region Module
    override val name: String = "TokenRegistrar"
    //endregion

    //region UserModule
    override fun renewVisitorId(current: String, previous: String?) {
        Logger.d(LOG_TAG, "renewVisitorId $current -> $previous")
        unregisterFCMToken(previous)
        registerFCMToken()
    }
    //endregion

    //region NotificationModule
    override fun unsubscribe() {
        unregisterFCMToken()
    }
    //endregion

    private var token: String? = null
    private var subscribe = false

    fun registerFCMToken(token: String? = null) {
        if (token == null) {
            getToken { _token ->
                registerFCMTokenInternal(_token)
            }
        } else {
            registerFCMTokenInternal(token)
        }
    }

    private fun registerFCMTokenInternal(token: String?) {
        Logger.d(LOG_TAG, "registerFCMTokenInternal $token")
        if (token == null || !isChanged(token)) return

        val subscribe = isNotificationAvailable
        Tracker.track(PluginNativeAppIdentifyEvent(subscribe, token))

        this.subscribe = subscribe
        this.token = token
    }

    private fun unregisterFCMToken(visitorId: String? = null) {
        Tracker.track(PluginNativeAppIdentifyEvent(false), visitorId)

        this.subscribe = false
        this.token = null
    }

    private val isNotificationAvailable: Boolean
        get() {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    private fun getToken(completion: ((token: String) -> Unit)? = null) {
        try {
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Logger.d(LOG_TAG, "Failed to find FCM Token\n ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                // Get new Instance ID token
                task.result?.token?.let {

                    Logger.d(
                        LOG_TAG,
                        "FCM Token found by using FirebaseInstanceId.getInstanceId. $it"
                    )
                    completion?.invoke(it)
                }
            }
//        val token = FirebaseInstanceId.getInstance().getToken()
//        Logger.v(LOG_TAG, "FCM token found by using FirebaseInstanceId.getToken. $token")
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to find FCM Token\n ${e.message}")
        }
    }

    private fun isChanged(token: String): Boolean {
        if (this.token != token) return true
        if (this.subscribe != isNotificationAvailable) return true
        return false
    }
}
