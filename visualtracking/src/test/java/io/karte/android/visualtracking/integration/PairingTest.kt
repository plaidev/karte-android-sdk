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
package io.karte.android.visualtracking.integration

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.parseBody
import io.karte.android.setupKarteApp
import io.karte.android.tearDownKarteApp
import io.karte.android.visualtracking.PairingActivity
import io.karte.android.visualtracking.injectDirectExecutorServiceToAutoTrackModules
import io.karte.android.visualtracking.integration.mock.TestActivity
import io.karte.android.visualtracking.internal.VTHook
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows

class PairingTest : RobolectricTestCase() {
    private val paringAppKey = "paring_appkey_123456789012345678"
    private lateinit var server: MockWebServer
    private lateinit var dispatcher: VTRequestDispatcher
    private val pairingActivityIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("example://_krtp/sampleAccountId"))

    @Before
    fun setup() {
        dispatcher = VTRequestDispatcher()
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start()

        setupKarteApp(server, appKey = paringAppKey)

        injectDirectExecutorServiceToAutoTrackModules()

        val testActivityComponentName = ComponentName(application, TestActivity::class.java)
        val packageManager = Shadows.shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(testActivityComponentName)

        val intentFilter = IntentFilter(Intent.ACTION_MAIN)
        intentFilter.addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.addIntentFilterForActivity(testActivityComponentName, intentFilter)
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    @Test
    fun AutoTrackPairingActivityの起動時にペアリング開始リクエストが送信される() {
        Robolectric.buildActivity(PairingActivity::class.java, pairingActivityIntent).create()

        val req =
            dispatcher.autoTrackRequests().find { it.path?.contains("/pairing-start") == true }!!
        assertThat(req.getHeader("X-KARTE-Auto-Track-Account-Id")).isEqualTo("sampleAccountId")
        assertThat(req.getHeader("X-KARTE-App-Key")).isEqualTo(paringAppKey)

        val body = JSONObject(req.parseBody())
        assertThatJson(body).node("os").isString.isEqualTo("android")
        assertThatJson(body).node("visitor_id").isString.isEqualTo(KarteApp.visitorId)
        assertThatJson(body).node("app_info").isNotNull
    }

    @Test
    fun ペアリング中の操作がtraceとして送信される() {
        Robolectric.buildActivity(PairingActivity::class.java, pairingActivityIntent).create()
        VTHook.hookAction("onClick", arrayOf())

        val req = dispatcher.autoTrackRequests().find { it.path?.contains("/trace") == true }
        val body = req?.body?.readUtf8()
        assertThat(req?.getHeader("X-KARTE-Auto-Track-Account-Id")).isEqualTo("sampleAccountId")
        assertThat(req?.getHeader("X-KARTE-App-Key")).isEqualTo(paringAppKey)

        // Parse multi request is difficult so assert partially.
        assertThat(body).contains("\"os\":\"android\"")
    }
}
