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
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import io.karte.android.core.config.ExperimentalConfig
import io.karte.android.core.config.OperationMode
import io.karte.android.core.library.LibraryConfig
import io.karte.android.modules.crashreporting.CrashReporting
import io.karte.android.modules.crashreporting.CrashReportingConfig
import io.karte.android.test.R
import io.karte.android.test_lib.eventNameTransform
import io.karte.android.test_lib.integration.SetupTestCase
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.proceedBufferedCall
import io.karte.android.test_lib.setupKarteApp
import io.karte.android.tracking.Tracker
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric

@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class SetupTest {
    class 初回起動の場合 : SetupTestCase() {
        @Test
        fun native_app_installイベントがtrackサーバに送信されること() {
            // getPreferenceEdit().remove("app_version_code").commit()
            setupKarteApp(server, appKey = setupAppKey)
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
            setupKarteApp(server, appKey = setupAppKey)
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
            setupKarteApp(server, appKey = setupAppKey)
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
                setupKarteApp(server, appKey = setupAppKey)
            }

            @Test
            fun native_app_openイベントがtrackサーバに送信されること() {
                Robolectric.buildActivity(Activity::class.java).create()
                proceedBufferedCall()
                assertThat(dispatcher.trackedEvents())
                    .comparingElementsUsing(eventNameTransform)
                    .contains("native_app_open")
            }

            @Test
            fun native_app_foregroundイベントがtrackサーバに送信されること() {
                Robolectric.buildActivity(Activity::class.java).create().start()
                proceedBufferedCall()
                assertThat(dispatcher.trackedEvents())
                    .comparingElementsUsing(eventNameTransform)
                    .contains("native_app_foreground")
            }
        }

        class 二回目以降アプリ起動の場合 : SetupTestCase() {

            @Before
            fun createActivity() {
                setupKarteApp(server, appKey = setupAppKey)

                Robolectric.buildActivity(Activity::class.java).create()
                proceedBufferedCall()
            }

            @Test
            fun native_app_openイベントが初回の起動時しかtrackサーバに送信されないこと() {
                Robolectric.buildActivity(Activity::class.java).create()
                Tracker.track("buy")
                proceedBufferedCall()
                assertThat(dispatcher.trackedEvents())
                    .comparingElementsUsing(eventNameTransform)
                    .contains("native_app_open")
                // assertThat(dispatcher.trackedEvents()).comparingElementsUsing(eventNameTransform).containsOnlyOnce("native_app_open")
                assertThat(dispatcher.trackedEvents()
                    .count { it.getString("event_name") == "native_app_open" })
                    .isEqualTo(1)
            }
        }
    }

    class CrashReportingが設定される : SetupTestCase() {
        @Test
        fun デフォルトではUncaughtExceptionHandlerがセットされること() {
            setupKarteApp(server, appKey = setupAppKey)
            assertThat(Thread.getDefaultUncaughtExceptionHandler()).isNotNull()
            assertThat(Thread.getDefaultUncaughtExceptionHandler()).isInstanceOf(
                CrashReporting::class.java
            )
        }

        @Test
        fun 設定をオン時にはUncaughtExceptionHandlerがセットされること() {
            val configBuilder = Config.Builder()
                .libraryConfigs(CrashReportingConfig.build { enabledTracking = true })
            setupKarteApp(server, configBuilder, setupAppKey)
            assertThat(Thread.getDefaultUncaughtExceptionHandler()).isNotNull()
            assertThat(Thread.getDefaultUncaughtExceptionHandler()).isInstanceOf(
                CrashReporting::class.java
            )
        }

        @Test
        fun 設定をオフ時にはUncaughtExceptionHandlerがセットされないこと() {
            val configBuilder = Config.Builder()
                .libraryConfigs(CrashReportingConfig.build { enabledTracking = false })
            setupKarteApp(server, configBuilder, setupAppKey)
            assertThat(Thread.getDefaultUncaughtExceptionHandler()).isNull()
        }
    }

    class AppKeyのValidation : SetupTestCase() {
        @Test
        fun 文字数が不正な時は初期化されない() {
            KarteApp.setup(application, "aa")
            assertThat(KarteApp.self.tracker).isNull()
            KarteApp.setup(application, "b".repeat(100))
            assertThat(KarteApp.self.tracker).isNull()
            KarteApp.setup(application, "c".repeat(32))
            assertThat(KarteApp.self.tracker).isNotNull()
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    class Config指定のテスト(private val pattern: SetupPattern) : SetupTestCase() {
        enum class SetupPattern { FROM_RESOURCE, BY_CONFIG, BY_METHOD }
        companion object {
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
            fun data(): List<Any> {
                return listOf(
                    arrayOf(SetupPattern.FROM_RESOURCE),
                    arrayOf(SetupPattern.BY_CONFIG),
                    arrayOf(SetupPattern.BY_METHOD)
                )
            }
        }

        private val overwriteAppKey = "overwriteappkey_1234567890123456"

        private fun setup(configBuilder: Config.Builder = Config.Builder()) {
            val config = configBuilder.baseUrl(server.url("").toString()).build()
            when (pattern) {
                SetupPattern.FROM_RESOURCE -> KarteApp.setup(application, config)
                SetupPattern.BY_CONFIG -> KarteApp.setup(
                    application,
                    config.apply { appKey = overwriteAppKey })

                SetupPattern.BY_METHOD -> KarteApp.setup(application, overwriteAppKey, config)
            }
        }

        @Test
        fun appKey_指定したAPP_KEYがヘッダとしてtrackサーバに送信されること() {
            setup()
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            val expected = if (pattern == SetupPattern.FROM_RESOURCE) {
                application.getString(R.string.karte_app_key)
            } else {
                overwriteAppKey
            }
            assertThat(dispatcher.trackedRequests().first().getHeader("X-KARTE-App-Key"))
                .isEqualTo(expected)
        }

        @Test
        fun apiKey_リソースまたはconfig経由で設定されること() {
            val expected = if (pattern == SetupPattern.FROM_RESOURCE) {
                setup()
                application.getString(R.string.karte_api_key)
            } else {
                setup(Config.Builder().apply { apiKey = "test_api_key" })
                "test_api_key"
            }
            assertThat(KarteApp.self.config.apiKey).isEqualTo(expected)
        }

        @Test
        fun baseUrl_指定したendpointに対してリクエストが行われること() {
            setup()
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            assertThat(dispatcher.trackedRequests().first().requestUrl)
                .isEqualTo(server.url("/v0/native/track"))
        }

        /** [isLimitAdTrackingEnabled] がfalseの時、trackingを許可している. */
        private fun mockAdvertisingId(isLimitAdTrackingEnabled: Boolean) {
            mockkStatic(AdvertisingIdClient::class)
            val info = AdvertisingIdClient.Info(advertisingId, isLimitAdTrackingEnabled)
            every { AdvertisingIdClient.getAdvertisingIdInfo(any()) }.returns(info)
        }

        private fun unmockAdvertisingId() {
            unmockkStatic(AdvertisingIdClient::class)
        }

        @Test
        fun Aaid_true_広告のトラッキングを許可している場合app_infoにaaidパラメータが含まれること() {
            mockAdvertisingId(false)

            setup(Config.Builder().enabledTrackingAaid(true))
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            val request = server.takeRequest()
            val actual = JSONObject(request.parseBody())
                .getJSONObject("app_info").getJSONObject("system_info")
                .getString("aaid")
            assertThat(actual).isEqualTo(advertisingId)

            unmockAdvertisingId()
        }

        @Test
        fun Aaid_true_広告のトラッキングを許可していない場合app_infoにaaidパラメータが含まれないこと() {
            mockAdvertisingId(true)

            setup(Config.Builder().enabledTrackingAaid(true))
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            val request = server.takeRequest()
            val systemInfo =
                JSONObject(request.parseBody()).getJSONObject("app_info")
                    .getJSONObject("system_info")
            assertThat(systemInfo.has("aaid")).isFalse()

            unmockAdvertisingId()
        }

        @Test
        fun Aaid_false_app_infoにaaidパラメータが含まれないこと() {
            setup(Config.Builder().enabledTrackingAaid(false))
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            val request = server.takeRequest()
            val systemInfo =
                JSONObject(request.parseBody()).getJSONObject("app_info")
                    .getJSONObject("system_info")
            assertThat(systemInfo.has("aaid")).isFalse()
        }

        @Test
        fun IsDryRun_true_Trackerインスタンスの実体がnullであること() {
            setup(Config.Builder().isDryRun(true))
            assertThat(KarteApp.self.tracker).isNull()
        }

        @Test
        fun IsDryRun_false_Trackerインスタンスの実体があること() {
            setup(Config.Builder().isDryRun(false))
            assertThat(KarteApp.self.tracker).isNotNull()
        }

        @Test
        fun OperationMode_DEFAULT_モードに対応するendpointに対してリクエストが行われること() {
            setup(ExperimentalConfig.Builder())
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            assertThat(dispatcher.trackedRequests().first().requestUrl)
                .isEqualTo(server.url("/v0/native/track"))
        }

        @Test
        fun OperationMode_INGEST_モードに対応するendpointに対してリクエストが行われること() {
            setup(ExperimentalConfig.Builder().operationMode(OperationMode.INGEST))
            Robolectric.buildActivity(Activity::class.java).create()
            proceedBufferedCall()
            assertThat(dispatcher.ingestRequests().first().requestUrl)
                .isEqualTo(server.url("/v0/native/ingest"))
        }

        class TestLibraryConfig(val enableTest: Boolean) : LibraryConfig

        @Test
        fun libraryConfigsに任意のConfigを追加() {
            setup(Config.Builder().libraryConfigs(TestLibraryConfig(true)))
            val testConfig = KarteApp.self.libraryConfig(TestLibraryConfig::class.java)
            assertThat(testConfig).isNotNull()
            assertThat(testConfig?.enableTest).isTrue()
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
                setupKarteApp(server, appKey = setupAppKey)
                assertThat(KarteApp.self.appKey).isEqualTo(setupAppKey)
            }
        }

        @RunWith(Enclosed::class)
        class 二回初期化した場合 {
            class appKey指定なしの場合 : SetupTestCase() {
                @Test
                fun デフォルトトラッカーのAPP_KEYが一番目に初期化する際に指定したAPP_KEYと一致すること() {
                    setupKarteApp(server, appKey = setupAppKey)
                    setupKarteApp(server, appKey = setupAppKey)
                    assertThat(KarteApp.self.appKey).isEqualTo(setupAppKey)
                }
            }

            class appKey指定ありの場合 : SetupTestCase() {
                @Test
                fun 取得したトラッカーのAPP_KEYが二番目に初期化する際に指定したAPP_KEYと一致すること() {
                    setupKarteApp(server, appKey = setupAppKey)
                    setupKarteApp(server, appKey = "appkey2")

                    assertThat(KarteApp.self.appKey).isEqualTo(setupAppKey)
                }
            }
        }
    }
}
