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
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.core.config.Config
import io.karte.android.eventNameTransform
import io.karte.android.parseBody
import io.karte.android.modules.crashreporting.CrashReporting
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tracking.Tracker
import io.mockk.every
import io.mockk.mockkStatic
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Robolectric

abstract class SetupTestCase : RobolectricTestCase() {
    var appKey = "sampleappkey"
    val advertisingId = "advertisingId"

    lateinit var server: MockWebServer
    lateinit var dispatcher: TrackerRequestDispatcher

    @Before
    fun init() {
        server = MockWebServer()
        dispatcher = TrackerRequestDispatcher().also { server.setDispatcher(it) }
        server.start()
    }

    @After
    fun tearDown() {
        KarteApp.self.teardown()
        server.shutdown()
    }

    protected fun getPreferenceEdit(): SharedPreferences.Editor {
        return application.getSharedPreferences(
            "io.karte.android.tracker.Data_$appKey",
            Context.MODE_PRIVATE
        ).edit()
    }
}

@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class SetupTest {
    @RunWith(Enclosed::class)
    class KarteTracker {
        class 初回起動の場合 : SetupTestCase() {
            @Test
            fun native_app_installイベントがtrackサーバに送信されること() {
                // getPreferenceEdit().remove("app_version_code").commit()
                setupKarteApp(server, appKey)
                Robolectric.buildActivity(Activity::class.java).create()
                proceedBufferedCall()

                assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                    .contains("native_app_install")
            }
        }

        class アップデートの場合 : SetupTestCase() {
            @Test
            fun native_app_updateイベントがtrackサーバに送信されること() {
                getPreferenceEdit().putInt("app_version_code", 0).commit()
                setupKarteApp(server, appKey)
                Robolectric.buildActivity(Activity::class.java).create()
                proceedBufferedCall()
                assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                    .contains("native_app_update")
            }
        }

        class 初回起動でもアップデートでもない場合 : SetupTestCase() {
            @Test
            fun イベントがtrackサーバに送信されないこと() {
                getPreferenceEdit().putInt("app_version_code", 1).commit()
                setupKarteApp(server, appKey)
                Robolectric.buildActivity(Activity::class.java).create()

                proceedBufferedCall()
                assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                    .containsExactly("native_app_open")
            }
        }

        @RunWith(Enclosed::class)
        class アプリ起動 {
            class 初回アプリ起動の場合 : SetupTestCase() {
                @Before
                fun beforeInit() {
                    setupKarteApp(server, appKey)
                }

                @Test
                fun native_app_openイベントがtrackサーバに送信されること() {
                    Robolectric.buildActivity(Activity::class.java).create()
                    proceedBufferedCall()
                    assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                        .contains("native_app_open")
                }

                @Test
                fun native_app_foregroundイベントがtrackサーバに送信されること() {
                    Robolectric.buildActivity(Activity::class.java).create().start()
                    proceedBufferedCall()
                    assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                        .contains("native_app_foreground")
                }
            }

            class 二回目以降アプリ起動の場合 : SetupTestCase() {

                @Before
                fun createActivity() {
                    setupKarteApp(server, appKey)

                    Robolectric.buildActivity(Activity::class.java).create()
                    proceedBufferedCall()
                }

                @Test
                fun native_app_openイベントが初回の起動時しかtrackサーバに送信されないこと() {
                    Robolectric.buildActivity(Activity::class.java).create()
                    Tracker.track("buy")
                    proceedBufferedCall()
                    assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform)
                        .contains("native_app_open")
                    // assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform).containsOnlyOnce("native_app_open")
                    assertThat(dispatcher.trackedEvents().count { it.getString("event_name") == "native_app_open" }).isEqualTo(
                        1
                    )
                }
            }
        }

        @RunWith(Enclosed::class)
        class ConfigTest {
            @RunWith(Enclosed::class)
            class configあり {
                class configありで初期化した場合 : SetupTestCase() {
                    @Test
                    fun 初期化時に指定したAPP_KEYがヘッダとしてtrackサーバに送信されること() {
                        setupKarteApp(server, appKey)
                        Robolectric.buildActivity(Activity::class.java).create()
                        proceedBufferedCall()
                        assertThat(dispatcher.trackedRequests().first().getHeader("X-KARTE-App-Key")).isEqualTo(
                            appKey
                        )
                    }
                }

                class endpointが指定されている場合 : SetupTestCase() {
                    @Test
                    fun configで指定したendpointに対してリクエストが行われること() {
                        setupKarteApp(server, appKey)
                        Robolectric.buildActivity(Activity::class.java).create()
                        proceedBufferedCall()
                        assertThat(dispatcher.trackedRequests().first().requestUrl).isEqualTo(
                            server.url(
                                "/native/track"
                            )
                        )
                    }
                }

                class enabledTrackingCrashErrorが有効な場合 : SetupTestCase() {
                    @Test
                    fun UncaughtExceptionHandlerがセットされてること() {
                        setupKarteApp(server, appKey)
                        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isNotNull()
                        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isInstanceOf(
                            CrashReporting::class.java
                        )
                    }
                }

                class enableTrackingAaidが有効の場合 : SetupTestCase() {

                    @Test
                    fun 広告のトラッキングを許可している場合app_infoにaaidパラメータが含まれること() {
                        mockkStatic(AdvertisingIdClient::class)
                        val info = AdvertisingIdClient.Info(advertisingId, false)
                        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) }.returns(info)

                        setupKarteApp(server, appKey, Config.Builder().enabledTrackingAaid(true))
                        Robolectric.buildActivity(Activity::class.java).create()
                        proceedBufferedCall()
                        val request = server.takeRequest()
                        val actual = JSONObject(request.parseBody()).getJSONObject("app_info")
                            .getJSONObject("system_info").getString("aaid")
                        assertThat(actual).isEqualTo(advertisingId)
                    }

                    @Test
                    fun 広告のトラッキングを許可していない場合app_infoにaaidパラメータが含まれないこと() {
                        mockkStatic(AdvertisingIdClient::class)
                        val info = AdvertisingIdClient.Info(advertisingId, true)
                        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) }.returns(info)

                        setupKarteApp(server, appKey, Config.Builder().enabledTrackingAaid(true))
                        Robolectric.buildActivity(Activity::class.java).create()
                        proceedBufferedCall()
                        val request = server.takeRequest()
                        val systemInfo =
                            JSONObject(request.parseBody()).getJSONObject("app_info")
                                .getJSONObject("system_info")
                        assertThat(systemInfo.has("aaid")).isFalse()
                    }
                }

                class enableTrackingAaidが無効の場合 : SetupTestCase() {

                    @Before
                    fun initAdvertisingIdClient() {
                        mockkStatic(AdvertisingIdClient::class)
                        val info = AdvertisingIdClient.Info(advertisingId, true)
                        every { AdvertisingIdClient.getAdvertisingIdInfo(any()) }.returns(info)
                    }

                    @Test
                    fun app_infoにaaidパラメータが含まれないこと() {
                        setupKarteApp(server, appKey, Config.Builder().enabledTrackingAaid(false))
                        Robolectric.buildActivity(Activity::class.java).create()
                        proceedBufferedCall()
                        val request = server.takeRequest()
                        val systemInfo =
                            JSONObject(request.parseBody()).getJSONObject("app_info")
                                .getJSONObject("system_info")
                        assertThat(systemInfo.has("aaid")).isFalse()
                    }
                }

                class isDryRunが有効の場合 : SetupTestCase() {

                    @Test
                    fun Trackerインスタンスの実体がNullTrackerであること() {
                        setupKarteApp(server, appKey, Config.Builder().isDryRun(true))
                        assertThat(KarteApp.self.tracker).isNull()
                    }
                }

                class isDryRunが無効の場合 : SetupTestCase() {

                    @Test
                    fun Trackerインスタンスの実体がTrackerImplであること() {
                        setupKarteApp(server, appKey, Config.Builder().isDryRun(false))
                        assertThat(KarteApp.self.tracker).isNotNull()
                    }
                }
            }
        }

        @RunWith(Enclosed::class)
        class getTrackerInstance {
            class 初期化してない場合 : SetupTestCase() {
                @Test
                fun NullTrackerが呼ばれる() {
                    assertThat(KarteApp.self.tracker).isNull()
                }
            }

            class 初期化した場合 : SetupTestCase() {
                @Test
                fun 初期化時に指定したAPP_KEYがヘッダとしてtrackサーバに送信されること() {
                    setupKarteApp(server, appKey)
                    assertThat(KarteApp.self.appKey).isEqualTo(appKey)
                }
            }

            @RunWith(Enclosed::class)
            class 二回初期化した場合 {
                class appKey指定なしの場合 : SetupTestCase() {
                    @Test
                    fun デフォルトトラッカーのAPP_KEYが一番目に初期化する際に指定したAPP_KEYと一致すること() {
                        setupKarteApp(server, appKey)
                        setupKarteApp(server, appKey)
                        assertThat(KarteApp.self.appKey).isEqualTo(appKey)
                    }
                }

                class appKey指定ありの場合 : SetupTestCase() {
                    @Test
                    fun 取得したトラッカーのAPP_KEYが二番目に初期化する際に指定したAPP_KEYと一致すること() {
                        setupKarteApp(server, appKey)
                        setupKarteApp(server, "appkey2")

                        assertThat(KarteApp.self.appKey).isEqualTo(appKey)
                    }
                }
            }
        }
    }
}
