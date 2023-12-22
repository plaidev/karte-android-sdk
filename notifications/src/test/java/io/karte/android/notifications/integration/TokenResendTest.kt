//
//  Copyright 2021 PLAID, Inc.
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

package io.karte.android.notifications.integration

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.karte.android.core.config.Config
import io.karte.android.notifications.Notifications
import io.karte.android.notifications.NotificationsConfig
import io.karte.android.notifications.proceedBufferedThreads
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.TrackerRequestDispatcher
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.tearDownKarteApp
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric

abstract class NotificationSetupTestCase : RobolectricTestCase() {
    lateinit var server: MockWebServer
    val dispatcher = TrackerRequestDispatcher()

    @Before
    fun initTracker() {
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    fun pluginIdentifyEvents(): List<JSONObject> {
        return dispatcher.trackedEvents()
            .filter { it.getString("event_name") == "plugin_native_app_identify" }
    }

    fun pluginIdentifyEvent(): JSONObject? {
        return pluginIdentifyEvents().lastOrNull()
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
class TokenResendTest(private val pattern: MockPattern) : NotificationSetupTestCase() {
    enum class MockPattern { MESSAGING, NONE }
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): List<Any> {
            return listOf(
                arrayOf(MockPattern.MESSAGING),
                arrayOf(MockPattern.NONE)
            )
        }
    }

    private fun mockToken(token: String) {
        when (pattern) {
            MockPattern.MESSAGING -> mockFirebaseMessaging(token)
            MockPattern.NONE -> {
            }
        }
    }

    private fun unmockToken() {
        when (pattern) {
            MockPattern.MESSAGING -> unmockFirebaseMessaging()
            MockPattern.NONE -> {
            }
        }
    }

    @Before
    fun setup() {
        mockToken("test")
    }

    @After
    fun teardown() {
        unmockToken()
    }

    @Test
    fun デフォルト_onResumeでplugin_native_app_identifyイベントがtrackサーバに送信されること() {
        setupKarteApp(server)
        Robolectric.buildActivity(Activity::class.java).create().resume()
        proceedBufferedThreads()

        if (pattern == MockPattern.NONE) {
            // mockしなければ送信されない
            assertThat(pluginIdentifyEvents()).hasSize(0)
            return
        }

        assertThat(pluginIdentifyEvents()).hasSize(1)
        assertThat(pluginIdentifyEvent()?.getJSONObject("values")?.getString("fcm_token"))
            .isEqualTo("test")

        // 2回目はtokenが変更されなければ呼ばれない
        Robolectric.buildActivity(Activity::class.java).create().resume()
        proceedBufferedThreads()
        assertThat(pluginIdentifyEvents()).hasSize(1)

        // tokenが更新されれば再送信する
        mockToken("test2")
        Robolectric.buildActivity(Activity::class.java).create().resume()
        proceedBufferedThreads()
        assertThat(pluginIdentifyEvents()).hasSize(2)
        assertThat(pluginIdentifyEvent()?.getJSONObject("values")?.getString("fcm_token"))
            .isEqualTo("test2")
    }

    @Test
    fun DeprecatedConfig_onResumeでplugin_native_app_identifyイベントがtrackサーバに送信されないこと() {
        @Suppress("DEPRECATION")
        Notifications.Config.enabledFCMTokenResend = false
        setupKarteApp(server)
        Robolectric.buildActivity(Activity::class.java).create().resume()
        proceedBufferedThreads()

        assertThat(pluginIdentifyEvents()).hasSize(0)

        @Suppress("DEPRECATION")
        Notifications.Config.enabledFCMTokenResend = true
    }

    @Test
    fun LibraryConfig_onResumeでplugin_native_app_identifyイベントがtrackサーバに送信されないこと() {
        val notificationsConfig = NotificationsConfig.build { enabledFCMTokenResend = false }
        val configBuilder = Config.Builder().libraryConfigs(notificationsConfig)
        setupKarteApp(server, configBuilder)
        Robolectric.buildActivity(Activity::class.java).create().resume()
        proceedBufferedThreads()

        assertThat(pluginIdentifyEvents()).hasSize(0)
    }
}
