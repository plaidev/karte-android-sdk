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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.tearDownKarteApp
import io.karte.android.visualtracking.BasicAction
import io.karte.android.visualtracking.ImageProvider
import io.karte.android.visualtracking.PairingActivity
import io.karte.android.visualtracking.VisualTracking
import io.karte.android.visualtracking.VisualTrackingDelegate
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

    @Test
    fun 手動でビジュアルトラッキングを処理したときにtraceが送信される() {
        Robolectric.buildActivity(PairingActivity::class.java, pairingActivityIntent).create()
        val action = BasicAction("touch", "favorite_tapped", "item_detail_screen_favorite_1")
        VisualTracking.handle(action)
        val req = dispatcher.autoTrackRequests().find { it.path?.contains("/trace") == true }
        val body = req?.body?.readUtf8()
        assertThat(req?.getHeader("X-KARTE-Auto-Track-Account-Id")).isEqualTo("sampleAccountId")
        assertThat(req?.getHeader("X-KARTE-App-Key")).isEqualTo(paringAppKey)

        // Parse multi request is difficult so assert partially.
        assertThat(body).contains("\"os\":\"android\"")
        assertThat(body).doesNotContain("Content-Type: application/octet-stream")
    }

    @Test
    fun 手動でビジュアルトラッキングを処理したときに画像付きのtraceが送信される() {
        Robolectric.buildActivity(PairingActivity::class.java, pairingActivityIntent).create()
        val imageProvider = ImageProvider {
            val bitmap = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = Color.RED
            canvas.drawRect(0f, 0f, 100f, 100f, paint)
            bitmap
        }

        val action = BasicAction(
            "touch",
            "item_detail_screen_favorite_button_1",
            "favorite button tapped",
            imageProvider
        )

        VisualTracking.handle(action)

        val req = dispatcher.autoTrackRequests().find { it.path?.contains("/trace") == true }
        val body = req?.body?.readUtf8()

        assertThat(req?.getHeader("X-KARTE-Auto-Track-Account-Id")).isEqualTo("sampleAccountId")
        assertThat(req?.getHeader("X-KARTE-App-Key")).isEqualTo(paringAppKey)

        // Parse multi request is difficult so assert partially.
        assertThat(body).contains("\"os\":\"android\"")
        assertThat(body).contains("Content-Type: application/octet-stream")
    }

    @Test
    fun ペアリング状態が変更された際に通知される() {
        var paired = false
        VisualTracking.delegate = object : VisualTrackingDelegate() {
            override fun onDevicePairingStatusUpdated(isPaired: Boolean) {
                paired = isPaired
            }
        }
        assertThat(VisualTracking.isPaired).isFalse()
        assertThat(paired).isFalse()

        Robolectric.buildActivity(PairingActivity::class.java, pairingActivityIntent).create()

        assertThat(VisualTracking.isPaired).isTrue()
        assertThat(paired).isTrue()
    }
}
