//
//  Copyright 2023 PLAID, Inc.
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

import io.karte.android.test_lib.InternalUtils
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.tearDownKarteApp
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before

abstract class OptOutTestCase : RobolectricTestCase() {
    val body = JSONObject().put("response", JSONObject().put("huga", "hoge"))

    lateinit var server: MockWebServer

    @Before
    fun initTracker() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        val repository = InternalUtils.karteApp.repository()
        repository.remove(InternalUtils.prefOptOutKey)
        server.shutdown()
    }
}
