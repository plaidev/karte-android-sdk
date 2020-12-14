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

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.core.config.Config
import io.karte.android.eventNameTransform
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tearDownKarteApp
import io.karte.android.tracking.Tracker
import io.karte.android.visualtracking.VisualTracking
import io.karte.android.visualtracking.condition
import io.karte.android.visualtracking.definition
import io.karte.android.visualtracking.injectDirectExecutorServiceToAutoTrackModules
import io.karte.android.visualtracking.internal.VTHook
import io.karte.android.visualtracking.trigger
import io.mockk.every
import io.mockk.mockk
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import java.util.concurrent.ScheduledExecutorService

private const val LAST_MODIFIED = 10.toLong()
private val definition = definition(
    "event1",
    trigger(
        condition("action", "\$eq", "action1"),
        fields = JSONObject().put("field1", "value1").put("field2", "value2")
    )
)

class VisualTrackingTest : RobolectricTestCase() {
    private lateinit var server: MockWebServer
    private lateinit var dispatcher: VTRequestDispatcher

    @Before
    fun setup() {
        dispatcher = VTRequestDispatcher(definition, lastModified = LAST_MODIFIED)
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start()

        setupKarteApp(server, Config.Builder().enabledTrackingAaid(false))

        injectDirectExecutorServiceToAutoTrackModules()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    @Test
    fun autoTrackのheaderが最初のtrackのrequestにセットされる() {
        Robolectric.buildActivity(Activity::class.java, Intent(application, Activity::class.java))
            .create().resume()
        proceedBufferedCall()
        val req = dispatcher.trackedRequests().first()
        assertThat(req.getHeader("X-KARTE-Auto-Track-OS")).isEqualTo("android")
        assertThat(req.getHeader("X-KARTE-Auto-Track-If-Modified-Since")).isNull()
    }

    @Test
    fun 保持している定義の最終更新日がtrackのheaderにセットされる() {
        Tracker.view("hoge")
        Tracker.view("fuga")
        proceedBufferedCall()
        val req = dispatcher.trackedRequests().last()
        assertThat(req.getHeader("X-KARTE-Auto-Track-If-Modified-Since"))
            .isEqualTo(LAST_MODIFIED.toString())
    }

    @Test
    fun trackで取得した自動計測定義によりイベントが発火する() {
        Tracker.view("hoge")
        proceedBufferedCall()
        VTHook.hookAction("action1", arrayOf())
        proceedBufferedCall()

        assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
            .contains("event1")
        val values = dispatcher.trackedEvents()
            .find { it.getString("event_name") == "event1" }
            ?.getJSONObject("values")!!
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "field1" to "value1",
                "field2" to "value2"
            )
        )
        val request = dispatcher.trackedRequests().last()
        assertThatJson(JSONObject(request.parseBody())).node("app_info").isObject
    }

    @Test
    fun APIで取得した自動計測定義によりイベントが発火する() {
        val scheduledExecutorService = mockk<ScheduledExecutorService>(relaxed = true)
        every { scheduledExecutorService.schedule(any(), any(), any()) } answers {
            (firstArg() as Runnable).run()
            callOriginal()
        }
        VisualTracking.self?.getDefinitions(scheduledExecutorService)
        VTHook.hookAction("action1", arrayOf())
        proceedBufferedCall()

        assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
            .contains("event1")
        val values = dispatcher.trackedEvents()
            .find { it.getString("event_name") == "event1" }
            ?.getJSONObject("values")!!
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "field1" to "value1",
                "field2" to "value2"
            )
        )
        val request = dispatcher.trackedRequests().last()
        assertThatJson(JSONObject(request.parseBody())).node("app_info").isObject
    }

    @Test
    fun 条件に合致しないイベントは発火しない() {
        Tracker.view("hoge")
        proceedBufferedCall()
        VTHook.hookAction("action2", arrayOf())
        proceedBufferedCall()

        assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
            .doesNotContain("event1")
    }
}
