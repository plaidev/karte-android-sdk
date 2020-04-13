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
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.core.config.Config
import io.karte.android.eventNameTransform
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tearDownKarteApp
import io.karte.android.tracking.Tracker
import io.karte.android.visualtracking.autoTrackDefinition
import io.karte.android.visualtracking.condition
import io.karte.android.visualtracking.definition
import io.karte.android.visualtracking.injectDirectExecutorServiceToAutoTrackModules
import io.karte.android.visualtracking.internal.VTHook
import io.karte.android.visualtracking.trigger
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric

class VisualTrackingTest : RobolectricTestCase() {
    lateinit var server: MockWebServer
    lateinit var dispatcher: TrackerRequestDispatcher
    val lastModified = 10.toLong()

    @Before
    fun setup() {
        dispatcher = object : TrackerRequestDispatcher() {
            override fun onTrackRequest(request: RecordedRequest): MockResponse {
                if (request.getHeader("X-KARTE-Auto-Track-OS") == "android") {

                    val since = request.getHeader("X-KARTE-Auto-Track-If-Modified-Since")
                    if (since != null && Integer.parseInt(since) <= lastModified) {
                        return MockResponse().setBody(
                            JSONObject().put(
                                "response",
                                JSONObject()
                            ).toString()
                        )
                    }

                    return MockResponse().setBody(
                        JSONObject().put(
                            "response", JSONObject()
                                .put(
                                    "auto_track_definition", autoTrackDefinition(
                                        definition(
                                            "event1",
                                            trigger(
                                                condition("action", "\$eq", "action1"),
                                                fields = JSONObject().put(
                                                    "field1",
                                                    "value1"
                                                ).put("field2", "value2")
                                            )
                                        ),
                                        lastModified = lastModified,
                                        status = "modified"
                                    )
                                )
                        )
                            .toString()
                    )
                }
                return super.onTrackRequest(request)
            }
        }
        server = MockWebServer()
        server.setDispatcher(dispatcher)
        server.start()

        setupKarteApp(server, "appkey", Config.Builder().enabledTrackingAaid(false))

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
        assertThat(req.getHeader("X-KARTE-Auto-Track-If-Modified-Since")).isEqualTo("10")
    }

    @Test
    fun 自動計測定義によりイベントが発火する() {
        Tracker.view("hoge")
        proceedBufferedCall()
        VTHook.hookAction("action1", arrayOf())
        proceedBufferedCall()

        assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
            .contains("event1")
        val values =
            dispatcher.trackedEvents().find { it.getString("event_name") == "event1" }?.getJSONObject(
                "values"
            )!!
        assertThatJson(values).isObject.containsAllEntriesOf(
            mapOf(
                "field1" to "value1",
                "field2" to "value2"
            )
        )
        val request = server.takeRequest()
        assertThatJson(JSONObject(request.body.readUtf8())).node("app_info").isObject
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
