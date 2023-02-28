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
package io.karte.android.test_lib.integration

import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.test_lib.tearDownKarteApp
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before

abstract class TrackerTestCase : RobolectricTestCase() {

    lateinit var server: MockWebServer

    // val advertisingId = "advertisingId"
    private val body = JSONObject().put("response", JSONObject().put("huga", "hoge"))

    @Before
    fun initTracker() {
        server = MockWebServer()
        server.start()

        setupKarteApp(server)
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    fun enqueueSuccessResponse() {
        server.enqueue(
            MockResponse().setBody(body.toString()).addHeader(
                "Content-Type",
                "text/html; charset=utf-8"
            )
        )
    }

    fun enqueueFailedResponse(status: Int) {
        server.enqueue(
            MockResponse().setBody(body.toString()).setResponseCode(status).addHeader(
                "Content-Type",
                "text/html; charset=utf-8"
            )
        )
    }
}
