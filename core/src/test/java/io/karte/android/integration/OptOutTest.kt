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
package io.karte.android.integration

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.assertThatNoEventOccured
import io.karte.android.core.config.Config
import io.karte.android.core.optout.PREF_KEY_OPT_OUT
import io.karte.android.core.usersync.UserSync
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tracking.Tracker
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.net.URLEncoder

abstract class OptOutTestCase : RobolectricTestCase() {
    val appKey = "sampleappkey"
    val body = JSONObject().put("response", JSONObject().put("huga", "hoge"))

    lateinit var server: MockWebServer

    @Before
    fun initTracker() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        KarteApp.self.teardown()
        val repository = KarteApp.self.repository()
        repository.remove(PREF_KEY_OPT_OUT)
        server.shutdown()
    }
}

@RunWith(Enclosed::class)
class OptOutTest {
    @RunWith(Enclosed::class)
    class OptOut {
        class enableTrackerOptOutが有効の場合 : OptOutTestCase() {
            @Before
            fun setup() {
                setupKarteApp(server, appKey, Config.Builder().isOptOut(true))
            }

            @Test
            fun NullTrackerで初期化されること() {
                // assertThat(KarteApp.self.tracker).isNull()
                assertThat(KarteApp.isOptOut).isTrue()
            }

            @Test
            fun trackが発生しないこと() {
                Tracker.track("test")
                server.assertThatNoEventOccured()
            }
        }

        @RunWith(Enclosed::class)
        class OptOutConfigTest {
            class OptOut実行 : OptOutTestCase() {
                @Before
                fun setup() {
                    setupKarteApp(server, appKey)
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    KarteApp.optOut()
                }

                @Test
                fun NullTrackerで初期化されること() {
                    // assertThat(KarteApp.self.tracker).isNull()
                    assertThat(KarteApp.isOptOut).isTrue()
                }

                @Test
                fun trackを呼び出せないこと() {
                    Tracker.track("test", mapOf("huga" to "hoge"))
                    server.assertThatNoEventOccured()
                }

                @Test
                fun UserSyncQueryParameterに_karte_tracker_deactivateが追加されること() {
                    val param = "{\"_karte_tracker_deactivate\":true}"
                    val expected = URLEncoder.encode(
                        Base64.encodeToString(
                            param.toByteArray(),
                            Base64.NO_WRAP
                        ), "utf8"
                    )
                    val syncParam =
                        UserSync.appendUserSyncQueryParameter("https://plaid.co.jp")
                    assertThat(syncParam).contains(expected)
                }
            }

            class OptIn実行 : OptOutTestCase() {
                @Before
                fun setup() {
                    setupKarteApp(server, appKey, Config.Builder().isOptOut(true))
                    KarteApp.optIn()
                }

                @Test
                fun TrackerImplで初期化されること() {
                    assertThat(KarteApp.self.tracker).isNotNull()
                    assertThat(KarteApp.isOptOut).isFalse()
                }

                @Test
                fun trackを呼び出せること() {
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    Tracker.track("test", mapOf("huga" to "hoge"))
                    proceedBufferedCall()
                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertThat(event.getString("event_name")).isEqualTo("test")
                    assertThat(eventValues.getString("huga")).isEqualTo("hoge")
                }

                @Test
                fun UserSyncQueryParameterに_karte_tracker_deactivateが追加されないこと() {
                    val param = "{\"_karte_tracker_deactivate\":true}"
                    val expected = URLEncoder.encode(
                        Base64.encodeToString(
                            param.toByteArray(),
                            Base64.NO_WRAP
                        ), "utf8"
                    )
                    val syncParam =
                        UserSync.appendUserSyncQueryParameter("https://plaid.co.jp")
                    assertThat(syncParam).doesNotContain(expected)
                }
            }
        }
    }
}
