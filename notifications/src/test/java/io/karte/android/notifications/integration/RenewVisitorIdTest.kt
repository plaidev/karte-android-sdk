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

import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.notifications.proceedBufferedThreads
import io.karte.android.test_lib.TrackerRequestDispatcher
import io.karte.android.test_lib.integration.TrackerTestCase
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.toList
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class RenewVisitorIdTest {

    class RenewVisitorId : TrackerTestCase() {

        private lateinit var dispatcher: TrackerRequestDispatcher
        private val mockedToken = "firebase mocked token"

        @Before
        fun setup() {
            dispatcher = TrackerRequestDispatcher()
            server.dispatcher = dispatcher
            mockFirebaseInstanceId(mockedToken)
        }

        @After
        fun teardown() {
            unmockFirebaseInstanceId()
        }

        @Test
        fun renewVisitorId_成功した場合() {
            val oldVisitorId = KarteApp.visitorId
            KarteApp.renewVisitorId()
            val newVisitorId = KarteApp.visitorId
            proceedBufferedThreads()

            val requestEvents = dispatcher.trackedRequests().groupBy({
                JSONObject(it.parseBody()).getJSONObject("keys").getString("visitor_id")
            }) { JSONObject(it.parseBody()).getJSONArray("events").toList() }
                .mapValues { it.value.flatten() }

            // unsubscribe event for old user
            requestEvents[oldVisitorId].run {
                assertThat(this).isNotNull()
                assertThat(this).hasSize(2)
                val eventValueMap =
                    this?.associate { it.getString("event_name") to it.getJSONObject("values") }
                assertThat(eventValueMap).containsKey("plugin_native_app_identify")
                val unsubscribeEvent = eventValueMap?.get("plugin_native_app_identify")
                assertThat(unsubscribeEvent?.getBoolean("subscribe")).isFalse()
            }

            // subscribe event for new user
            requestEvents[newVisitorId].run {
                assertThat(this).isNotNull()
                assertThat(this).hasSize(2)
                val eventValueMap =
                    this?.associate { it.getString("event_name") to it.getJSONObject("values") }
                assertThat(eventValueMap).containsKey("plugin_native_app_identify")
                val subscribeEvent = eventValueMap?.get("plugin_native_app_identify")
                assertThat(subscribeEvent?.getBoolean("subscribe")).isTrue()
                assertThat(subscribeEvent?.getString("fcm_token")).isEqualTo(mockedToken)
            }
        }
    }
}
