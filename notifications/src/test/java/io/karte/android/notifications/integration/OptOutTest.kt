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
package io.karte.android.notifications.integration

import com.google.common.truth.Truth
import io.karte.android.KarteApp
import io.karte.android.integration.OptOutTestCase
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class OptOutTest {
    class OptOut実行 : OptOutTestCase() {
        @Before
        fun setup() {
            setupKarteApp(server)
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            KarteApp.optOut()
            proceedBufferedCall()
        }

        @Test
        fun plugin_native_app_identifyが送信されること() {
            val request = server.takeRequest()
            val event =
                JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
            val eventValues = event.getJSONObject("values")
            Truth.assertThat(event.getString("event_name")).isEqualTo("plugin_native_app_identify")
            Truth.assertThat(eventValues.getBoolean("subscribe")).isEqualTo(false)
        }
    }
}
