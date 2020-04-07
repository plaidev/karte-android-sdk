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
package io.karte.android

import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before

abstract class TrackerTestCase : RobolectricTestCase() {

    lateinit var server: MockWebServer

    val appKey = "sampleappkey"
    // val advertisingId = "advertisingId"
    val body = JSONObject().put("response", JSONObject().put("huga", "hoge"))

    @Before
    fun initTracker() {
        server = MockWebServer()
        server.start()

        setupKarteApp(server, appKey)
    }

    @After
    fun tearDown() {
        KarteApp.self.teardown()
        server.shutdown()
    }
}
