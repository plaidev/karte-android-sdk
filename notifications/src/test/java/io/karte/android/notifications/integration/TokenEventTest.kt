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
import io.karte.android.notifications.internal.TokenRegistrar
import io.karte.android.notifications.manager
import io.karte.android.notifications.registerFCMToken
import io.karte.android.notifications.setPermission
import io.karte.android.test_lib.assertThatNoEventOccured
import io.karte.android.test_lib.integration.TrackerTestCase
import io.karte.android.test_lib.parseBody
import io.karte.android.test_lib.proceedBufferedCall
import org.json.JSONObject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

private fun TrackerTestCase.tokenEvent(): JSONObject {
    return JSONObject(server.takeRequest().parseBody()).getJSONArray("events").getJSONObject(0)
}

@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class TokenEventTest {
    @RunWith(Enclosed::class)
    class FCMトークンの取得に成功した場合 {
        class 通知が許可されている場合 : TrackerTestCase() {
            @Test
            fun plugin_native_app_identifyイベントがサーバに送信されること() {
                enqueueSuccessResponse()
                manager.setPermission(true)
                KarteApp.registerFCMToken("dummy_fcm_token")
                proceedBufferedCall()

                val event = tokenEvent()
                assertNotNull(event, "plugin_native_app_identifyがtrackサーバに送信されること")
                val eventValues = event.getJSONObject("values")
                Truth.assertWithMessage("FCMトークンがfcm_tokenパラメータとしてtrackサーバに送信されること").that(eventValues.getString("fcm_token")).isEqualTo("dummy_fcm_token")
                Truth.assertWithMessage("通知の可否がsubscribeパラメータとしてtrackサーバに送信されること").that(eventValues.getString("subscribe")).isEqualTo("true")
            }
        }

        class 通知が許可されていない場合 : TrackerTestCase() {
            @Test
            fun plugin_native_app_identifyイベントがサーバに送信されること() {
                enqueueSuccessResponse()
                manager.setPermission(false)
                KarteApp.registerFCMToken("dummy_fcm_token")
                proceedBufferedCall()

                val event = tokenEvent()
                assertNotNull(event, "plugin_native_app_identifyがtrackサーバに送信されること")
                val eventValues = event.getJSONObject("values")
                Truth.assertWithMessage("FCMトークンがfcm_tokenパラメータとしてtrackサーバに送信されること").that(eventValues.getString("fcm_token")).isEqualTo("dummy_fcm_token")
                Truth.assertWithMessage("通知の可否がsubscribeパラメータとしてtrackサーバに送信されること").that(eventValues.getString("subscribe")).isEqualTo("false")
            }
        }
    }

    class FCMトークンの取得に失敗した場合 : TrackerTestCase() {

        @Test
        fun FCMトークンがサーバに送信されないこと() {
            TokenRegistrar(application).registerFCMToken(null)
            proceedBufferedCall()
            server.assertThatNoEventOccured()
        }
    }
}
