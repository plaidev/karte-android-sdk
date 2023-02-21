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

import android.content.Context
import android.content.SharedPreferences
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.TrackerRequestDispatcher
import io.karte.android.test_lib.tearDownKarteApp
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

abstract class SetupTestCase : RobolectricTestCase() {
    var setupAppKey = "setup_appkey_1234567890123456789"
    val advertisingId = "advertisingId"

    lateinit var server: MockWebServer
    lateinit var dispatcher: TrackerRequestDispatcher

    @Before
    fun init() {
        server = MockWebServer()
        dispatcher = TrackerRequestDispatcher().also { server.dispatcher = it }
        server.start()
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    protected fun getPreferenceEdit(): SharedPreferences.Editor {
        return application.getSharedPreferences(
            "io.karte.android.tracker.Data_$setupAppKey",
            Context.MODE_PRIVATE
        ).edit()
    }
}
