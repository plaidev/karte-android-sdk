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

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.TrackerTestCase
import io.karte.android.assertThatNoEventOccured
import io.karte.android.eventNameTransform
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.toList
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.toValues
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import java.util.Date
import kotlin.math.min

private fun createJsonValues(): JSONObject {
    val jsonItem = JSONObject().put("item_name", "t-shirt")
    val arrayParam = JSONArray().put(1).put("hoge")
    return JSONObject()
        .put("num", 10)
        .put("str", "hoge")
        .put("date", Date())
        .put("json", jsonItem)
        .put("arr", arrayParam)
}

/**
 * Created by mario on 2018/01/29.
 */
@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class TrackerIntegrationTest {
    @RunWith(Enclosed::class)
    class trackイベント {
        class 引数にJSONObjectを指定する場合 : TrackerTestCase() {
            @Test
            fun trackイベントがサーバに送信されること() {
                val jsonValues = createJsonValues()
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Tracker.track("buy", jsonValues.toValues())
                proceedBufferedCall()
                val request = server.takeRequest()
                val event =
                    JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                val eventValues = event.getJSONObject("values")
                assertWithMessage("event_nameがtrackサーバに送信されること").that(event.getString("event_name"))
                    .isEqualTo("buy")
                assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num"))
                    .isEqualTo(10)
                assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                    .isEqualTo("hoge")
                assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                    .isEqualTo((jsonValues.get("date") as Date).time / 1000)
                assertWithMessage("JSONがtrackサーバに送信されること")
                    .that(eventValues.getJSONObject("json").toString())
                    .isEqualTo(jsonValues.getJSONObject("json").toString())
                assertWithMessage("JSONArrayがtrackサーバに送信されること")
                    .that(eventValues.getJSONArray("arr").toString())
                    .isEqualTo(jsonValues.getJSONArray("arr").toString())
            }
        }
    }

    @RunWith(Enclosed::class)
    class identifyイベント {
        class user_idを設定した場合 : TrackerTestCase() {
            @Test
            fun identifyイベントがサーバに送信されること() {
                val jsonValues = createJsonValues()
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Tracker.identify("test_user", jsonValues.toValues())
                proceedBufferedCall()

                val request = server.takeRequest()
                val event = JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                val eventValues = event.getJSONObject("values")
                assertWithMessage("event_nameがtrackサーバに送信されること").that(event.getString("event_name"))
                    .isEqualTo("identify")
                assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num"))
                    .isEqualTo(10)
                assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                    .isEqualTo("hoge")
                assertWithMessage("user_idがtrackサーバに送信されること").that(eventValues.getString("user_id"))
                    .isEqualTo("test_user")
                assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                    .isEqualTo((jsonValues.get("date") as Date).time / 1000)
                assertWithMessage("JSONがtrackサーバに送信されること")
                    .that(eventValues.getJSONObject("json").toString())
                    .isEqualTo(jsonValues.getJSONObject("json").toString())
                assertWithMessage("JSONArrayがtrackサーバに送信されること")
                    .that(eventValues.getJSONArray("arr").toString())
                    .isEqualTo(jsonValues.getJSONArray("arr").toString())
            }
        }

        class user_idに空文字を設定した場合 : TrackerTestCase() {
            @Test
            fun identifyイベントがサーバに送信されないこと() {
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Tracker.identify("", JSONObject())
                proceedBufferedCall()
                server.assertThatNoEventOccured()
            }
        }
    }

    class attributeイベント : TrackerTestCase() {
        @Test
        fun attributeイベントがサーバに送信されること() {
            val jsonValues = createJsonValues()
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            Tracker.attribute(jsonValues.toValues())
            proceedBufferedCall()

            val request = server.takeRequest()
            val event = JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
            val eventValues = event.getJSONObject("values")
            assertWithMessage("event_nameがtrackサーバに送信されること").that(event.getString("event_name"))
                .isEqualTo("attribute")
            assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num")).isEqualTo(10)
            assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                .isEqualTo("hoge")
            assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                .isEqualTo((jsonValues.get("date") as Date).time / 1000)
            assertWithMessage("JSONがtrackサーバに送信されること")
                .that(eventValues.getJSONObject("json").toString())
                .isEqualTo(jsonValues.getJSONObject("json").toString())
            assertWithMessage("JSONArrayがtrackサーバに送信されること")
                .that(eventValues.getJSONArray("arr").toString())
                .isEqualTo(jsonValues.getJSONArray("arr").toString())
        }
    }

    @RunWith(Enclosed::class)
    class viewイベント {
        class view_namに空文字を設定した場合 : TrackerTestCase() {
            @Test
            fun viewイベントがサーバに送信されないこと() {
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Tracker.view("", JSONObject())
                proceedBufferedCall()
                server.assertThatNoEventOccured()
            }
        }

        @RunWith(Enclosed::class)
        class valuesを設定しない場合 {
            class titleを設定しない場合 : TrackerTestCase() {
                @Test
                fun viewイベントがサーバに送信されること() {
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    Tracker.view("test_view")
                    proceedBufferedCall()

                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertWithMessage("event_nameがviewとしてtrackサーバに送信されること")
                        .that(event.getString("event_name"))
                        .isEqualTo("view")
                    assertWithMessage("view名がview_nameパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("view_name"))
                        .isEqualTo("test_view")
                    assertWithMessage("タイトルがtitleパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("title"))
                        .isEqualTo("test_view")
                }
            }

            class titleを設定する場合 : TrackerTestCase() {
                @Test
                fun viewイベントがサーバに送信されること() {
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    Tracker.view("test_view", "fuga")
                    proceedBufferedCall()

                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertWithMessage("event_nameがviewとしてtrackサーバに送信されること")
                        .that(event.getString("event_name"))
                        .isEqualTo("view")
                    assertWithMessage("view名がview_nameパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("view_name"))
                        .isEqualTo("test_view")
                    assertWithMessage("タイトルがtitleパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("title"))
                        .isEqualTo("fuga")
                }
            }
        }

        @RunWith(Enclosed::class)
        class valuesを設定する場合 {
            class 引数としてtitleを設定する場合 : TrackerTestCase() {
                @Test
                fun タイトルがtitleパラメータとしてtrackサーバに送信されること() {
                    val jsonValues = createJsonValues()
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    Tracker.view("test_view", "fuga", jsonValues.toValues())
                    proceedBufferedCall()

                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertWithMessage("event_nameがviewとしてtrackサーバに送信されること")
                        .that(event.getString("event_name"))
                        .isEqualTo("view")
                    assertWithMessage("view名がview_nameパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("view_name"))
                        .isEqualTo("test_view")
                    assertWithMessage("タイトルがtitleパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("title"))
                        .isEqualTo("fuga")
                    assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num"))
                        .isEqualTo(10)
                    assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                        .isEqualTo("hoge")
                    assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                        .isEqualTo((jsonValues.get("date") as Date).time / 1000)
                    assertWithMessage("JSONがtrackサーバに送信されること")
                        .that(eventValues.getJSONObject("json").toString())
                        .isEqualTo(jsonValues.getJSONObject("json").toString())
                    assertWithMessage("JSONArrayがtrackサーバに送信されること")
                        .that(eventValues.getJSONArray("arr").toString())
                        .isEqualTo(jsonValues.getJSONArray("arr").toString())
                }
            }

            class valuesにtitleを設定する場合 : TrackerTestCase() {
                @Test
                fun タイトルがtitleパラメータとしてtrackサーバに送信されること() {
                    val jsonValues = createJsonValues()
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    jsonValues.put("title", "fuga")
                    Tracker.view("test_view", jsonValues.toValues())
                    proceedBufferedCall()

                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertWithMessage("event_nameがviewとしてtrackサーバに送信されること")
                        .that(event.getString("event_name"))
                        .isEqualTo("view")
                    assertWithMessage("view名がview_nameパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("view_name"))
                        .isEqualTo("test_view")
                    assertWithMessage("タイトルがtitleパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("title"))
                        .isEqualTo("fuga")
                    assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num"))
                        .isEqualTo(10)
                    assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                        .isEqualTo("hoge")
                    assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                        .isEqualTo((jsonValues.get("date") as Date).time / 1000)
                    assertWithMessage("JSONがtrackサーバに送信されること")
                        .that(eventValues.getJSONObject("json").toString())
                        .isEqualTo(jsonValues.getJSONObject("json").toString())
                    assertWithMessage("JSONArrayがtrackサーバに送信されること")
                        .that(eventValues.getJSONArray("arr").toString())
                        .isEqualTo(jsonValues.getJSONArray("arr").toString())
                }
            }

            class titleを設定しない場合 : TrackerTestCase() {
                @Test
                fun view_nameと同じ値がtitleパラメータとしてサーバに送信されること() {
                    val jsonValues = createJsonValues()
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    Tracker.view("test_view", jsonValues.toValues())
                    proceedBufferedCall()

                    val request = server.takeRequest()
                    val event =
                        JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                    val eventValues = event.getJSONObject("values")
                    assertWithMessage("event_nameがviewとしてtrackサーバに送信されること")
                        .that(event.getString("event_name"))
                        .isEqualTo("view")
                    assertWithMessage("view名がview_nameパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("view_name"))
                        .isEqualTo("test_view")
                    assertWithMessage("view_nameと同じ値がtitleパラメータとしてtrackサーバに送信されること")
                        .that(eventValues.getString("title"))
                        .isEqualTo("test_view")
                    assertWithMessage("数値がtrackサーバに送信されること").that(eventValues.getInt("num"))
                        .isEqualTo(10)
                    assertWithMessage("文字列がtrackサーバに送信されること").that(eventValues.getString("str"))
                        .isEqualTo("hoge")
                    assertWithMessage("日付がtrackサーバに送信されること").that(eventValues.getInt("date"))
                        .isEqualTo((jsonValues.get("date") as Date).time / 1000)
                    assertWithMessage("JSONがtrackサーバに送信されること")
                        .that(eventValues.getJSONObject("json").toString())
                        .isEqualTo(jsonValues.getJSONObject("json").toString())
                    assertWithMessage("JSONArrayがtrackサーバに送信されること")
                        .that(eventValues.getJSONArray("arr").toString())
                        .isEqualTo(jsonValues.getJSONArray("arr").toString())
                }
            }
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    class 再送(private val retryCount: Int) : TrackerTestCase() {
        private val maxRetryCount = 3

        companion object {

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters
            fun data(): List<Any> {
                val d = mutableListOf<Any>()
                repeat(10) {
                    d.add(arrayOf(it + 1))
                }
                return d
            }
        }

        private fun enqueueSuccessResponse() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
        }

        private fun enqueueFailedRetryableResponse() {
            server.enqueue(
                MockResponse().setBody(body.toString()).setResponseCode(500).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
        }

        private fun enqueueFailedUnretryableResponse() {
            server.enqueue(
                MockResponse().setBody(body.toString()).setResponseCode(400).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
        }

        @Test
        fun リトライ上限まで再送されること() {
            repeat(retryCount) {
                enqueueFailedRetryableResponse()
            }
            enqueueSuccessResponse()
            Tracker.view("test")
            proceedBufferedCall()

            // 成功まで or 上限まで の小さい回数と一致
            val expectedRetryCount = min(retryCount + 1, maxRetryCount)
            println("requestCount: $retryCount, ${server.requestCount} $expectedRetryCount")
            assertThat(server.requestCount).isEqualTo(expectedRetryCount)
        }

        @Test
        fun ステータスコードが400番台の場合は再送されないこと() {
            enqueueFailedUnretryableResponse()
            Tracker.view("test")
            proceedBufferedCall()

            println("requestCount: $retryCount, ${server.requestCount}")
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    class 再送の制限() : TrackerTestCase() {
        private val maxRetryCount = 3

        private fun enqueueSuccessResponse() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
        }

        private fun enqueueFailedResponse() {
            server.enqueue(
                MockResponse().setBody(body.toString()).setResponseCode(500).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
        }

        @Test
        fun リトライ上限を超えたらしばらく再送しないこと() {
            repeat(10) {
                enqueueFailedResponse()
            }
            enqueueSuccessResponse()
            Tracker.view("test")
            proceedBufferedCall()
            Tracker.view("test")
            proceedBufferedCall()

            println("requestCount: ${server.requestCount} $maxRetryCount")
            assertThat(server.requestCount).isEqualTo(maxRetryCount + 1)
        }
    }

    @Ignore("仕様の見直し中のため、テストしない")
    class 画面遷移時のeventまとめ制御 : TrackerTestCase() {
        private lateinit var firstActivityController: ActivityController<Activity>
        @Before
        fun setup() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            firstActivityController =
                Robolectric.buildActivity(Activity::class.java).create().start().resume()
            // flush native_app_install and native_app_open
            proceedBufferedCall()
            proceedBufferedCall()
            server.takeRequest()
        }

        @Test
        fun eventsがviewイベントにより表示画面毎にまとめられて全て送られること() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )

            Tracker.track("frame1_a")
            Tracker.track("frame1_b")

            Tracker.view("view")

            Tracker.track("frame2_a")
            Tracker.track("frame2_b")

            proceedBufferedCall()

            val request1 = server.takeRequest()
            val events1 = JSONObject(request1.body.readUtf8()).getJSONArray("events").toList()
            assertThat(events1).comparingElementsUsing(eventNameTransform).contains("frame1_a")
            assertThat(events1).comparingElementsUsing(eventNameTransform).contains("frame1_b")
            assertThat(events1).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame2_a")
            assertThat(events1).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame2_b")

            val request2 = server.takeRequest()
            val events2 = JSONObject(request2.body.readUtf8()).getJSONArray("events").toList()
            assertThat(events2).comparingElementsUsing(eventNameTransform).contains("frame2_a")
            assertThat(events2).comparingElementsUsing(eventNameTransform).contains("frame2_b")
            assertThat(events2).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame1_a")
            assertThat(events2).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame1_b")
        }

        @Test
        fun eventsがonPauseにより表示画面毎にまとめられて全て送られること() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )

            Tracker.view("view1")
            Tracker.track("frame1_a")
            Tracker.track("frame1_b")

            firstActivityController.pause().stop()

            Tracker.track("frame2_a")
            Tracker.track("frame2_b")

            proceedBufferedCall()

            val request1 = server.takeRequest()
            val events1 = JSONObject(request1.body.readUtf8()).getJSONArray("events").toList()
            assertThat(events1).comparingElementsUsing(eventNameTransform).contains("frame1_a")
            assertThat(events1).comparingElementsUsing(eventNameTransform).contains("frame1_b")
            assertThat(events1).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame2_a")
            assertThat(events1).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame2_b")

            val request2 = server.takeRequest()
            val events2 = JSONObject(request2.body.readUtf8()).getJSONArray("events").toList()
            assertThat(events2).comparingElementsUsing(eventNameTransform).contains("frame2_a")
            assertThat(events2).comparingElementsUsing(eventNameTransform).contains("frame2_b")
            assertThat(events2).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame1_a")
            assertThat(events2).comparingElementsUsing(eventNameTransform)
                .doesNotContain("frame1_b")
        }
    }

    class AppInfo : TrackerTestCase() {

        @Test
        fun app_infoがサーバに送信されること() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            Tracker.view("view1")
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()

            val request = server.takeRequest()
            val bodyAppInfo = JSONObject(request.parseBody()).getJSONObject("app_info")
            val bodySystemInfo = bodyAppInfo.getJSONObject("system_info")
            assertWithMessage("バージョン番号がversion_nameパラメータとしてtrackサーバに送信されること")
                .that(bodyAppInfo.getString("version_name"))
                .isEqualTo("1.0.0")
            assertWithMessage("ビルド番号がversion_codeパラメータとしてtrackサーバに送信されること")
                .that(bodyAppInfo.getString("version_code"))
                .isEqualTo("1")
            assertWithMessage("SDKバージョンがkarte_sdk_versionパラメータとしてtrackサーバに送信されること")
                .that(bodyAppInfo.getString("karte_sdk_version"))
                .isEqualTo(BuildConfig.LIB_VERSION)
            assertWithMessage("パッケージ名がpachage_nameパラメータとしてtrackサーバに送信されること")
                .that(bodyAppInfo.getString("package_name"))
                .isEqualTo(application.packageName)
            assertWithMessage("OS名がosパラメータとしてtrackサーバに送信されること").that(bodySystemInfo.getString("os"))
                .isEqualTo("Android")
            assertWithMessage("OSバージョンがos_versionパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("os_version"))
                .isEqualTo("8.0.0")
            assertWithMessage("デバイス名がdeviceパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("device"))
                .isEqualTo("mario device")
            assertWithMessage("端末ブランド名がbrandパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("brand"))
                .isEqualTo("google")
            assertWithMessage("端末モデル名がmodelパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("model"))
                .isEqualTo("Nexus")
            assertWithMessage("プロダクト名がproductパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("product"))
                .isEqualTo("KARTE for APP")
            assertWithMessage("言語がlanguageパラメータとしてtrackサーバに送信されること")
                .that(bodySystemInfo.getString("language"))
                .isEqualTo("en-US")

            val screen = bodySystemInfo.getJSONObject("screen")
            assertWithMessage("画面の幅がscreen.widthパラメータとしてtrackサーバに送信されること")
                .that(screen.getString("width"))
                .isEqualTo("320")
            assertWithMessage("画面の高さがscreen.heightパラメータとしてtrackサーバに送信されること")
                .that(screen.getString("height"))
                .isEqualTo("470")
        }
    }

    class RenewVisitorId : TrackerTestCase() {
        @Test
        fun renewVisitorId_成功した場合() {
            for (i in 1..3)
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
            val oldVisitorId = KarteApp.visitorId
            KarteApp.renewVisitorId()
            proceedBufferedCall()

            assertThat(KarteApp.visitorId).isNotNull()
            assertThat(KarteApp.visitorId).isNotEqualTo(oldVisitorId)

            coreで発火されるeventのみを確認(oldVisitorId)
        }

        private fun coreで発火されるeventのみを確認(oldVisitorId: String) {
            // renew event for old user
            with(server.takeRequest()) {
                val body = JSONObject(this.parseBody())
                val visitorId = body.getJSONObject("keys").get("visitor_id")
                assertThat(visitorId).isEqualTo(oldVisitorId)
                val event = body.getJSONArray("events").getJSONObject(0)
                assertThat(event.getString("event_name")).isEqualTo("native_app_renew_visitor_id")
                assertThat(event.getJSONObject("values").get("new_visitor_id")).isEqualTo(
                    KarteApp.visitorId
                )
            }

            // renew event for new user
            with(server.takeRequest()) {
                val body = org.json.JSONObject(this.parseBody())
                val visitorId = body.getJSONObject("keys").get("visitor_id")
                assertThat(visitorId).isEqualTo(KarteApp.visitorId)
                val event = body.getJSONArray("events").getJSONObject(0)
                assertThat(event.getString("event_name")).isEqualTo("native_app_renew_visitor_id")
                assertThat(event.getJSONObject("values").get("old_visitor_id")).isEqualTo(
                    oldVisitorId
                )
            }
            assertThat(server.requestCount).isEqualTo(2)
        }
    }

    class VisitorId : TrackerTestCase() {
        @Test
        fun visitor_idがサーバに送信されること() {
            val jsonValues = createJsonValues()
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            Tracker.track("buy", jsonValues.toValues())
            proceedBufferedCall()

            val request = server.takeRequest()
            val event = JSONObject(request.parseBody()).getJSONObject("keys")
            assertWithMessage("ビジターIDがvisitor_idパラメータとしてtrackサーバに送信されること")
                .that(event.getString("visitor_id"))
                .isNotNull()
        }
    }
}
