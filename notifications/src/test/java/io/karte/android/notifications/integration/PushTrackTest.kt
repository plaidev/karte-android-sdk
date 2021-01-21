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

import android.app.Activity
import android.app.Notification
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.TrackerRequestDispatcher
import io.karte.android.TrackerTestCase
import io.karte.android.assertThatNoEventOccured
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.MessageReceiver
import io.karte.android.notifications.manager
import io.karte.android.notifications.setPermission
import io.karte.android.proceedBufferedCall
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import java.util.HashMap
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class PushTrackerTestCase : TrackerTestCase() {
    private lateinit var dispatcher: TrackerRequestDispatcher
    private val notification: Notification
        get() = Shadows.shadowOf(manager).allNotifications[0]

    @Before
    fun prepare() {
        dispatcher = TrackerRequestDispatcher()
        server.dispatcher = dispatcher
    }

    fun event(name: String): JSONObject? {
        return dispatcher.trackedEvents().firstOrNull { it.getString("event_name") == name }
    }

    fun handleMessageBySDK(data: HashMap<String, String>) {
        manager.setPermission(true)

        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.setPackage(application.packageName)
        val resolveInfo = ResolveInfo()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = application.packageName
        activityInfo.name = "MainActivity"
        resolveInfo.activityInfo = activityInfo
        @Suppress("DEPRECATION")
        Shadows.shadowOf(application.packageManager)
            .addResolveInfoForIntent(launchIntent, resolveInfo)

        val builder = RemoteMessage.Builder("dummyDestination")
        builder.setData(data)
        val remoteMessage = builder.build()
        MessageHandler.handleMessage(application, remoteMessage)
        proceedBufferedCall()
    }

    private fun sendBroadcast(intent: Intent) {
        // NOTE: このintentをbroadcastして、MessageReceiverのonReceiveが呼ばれることもテストしたいが現状では無理そう。
        // see https://github.com/plaidev/tracker-android/pull/163#issuecomment-470797618
        // ひとまずreceiverを無理やり生成してonReceiveを呼んでテストする。
        val receiver = MessageReceiver()
        receiver.onReceive(application, intent)
        proceedBufferedCall()
    }

    fun clickNotification() {
        sendBroadcast(Shadows.shadowOf(notification.contentIntent).savedIntent)
    }

    fun deleteNotification() {
        sendBroadcast(Shadows.shadowOf(notification.deleteIntent).savedIntent)
    }

    fun openAppByCustomIntent(data: HashMap<String, String>) {
        val intent = Intent(application, Activity::class.java)
        @Suppress("DEPRECATION")
        MessageHandler.copyInfoToIntent(data, intent)
        Robolectric.buildActivity(Activity::class.java, intent).start()
        proceedBufferedCall()
    }

    fun assertShowNotification() {
        assertThat(Shadows.shadowOf(manager).size()).isEqualTo(1)
    }

    fun assertNotShowNotification() {
        assertThat(Shadows.shadowOf(manager).size()).isEqualTo(0)
    }
}

@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class PushTrackTest {
    @RunWith(Enclosed::class)
    class 通常配信 {
        class 必要な全パラメータを含む : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_push_notification", "true")
                put("krt_campaign_id", "dummy_campaign_id")
                put("krt_shorten_id", "dummy_shorten_id")
                put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
                put(
                    "krt_event_values",
                    JSONObject()
                        .put("user_id", "dummy_user_id")
                        .put("task_id", "dummy_task_id")
                        .put("schedule_id", "dummy_schedule_id")
                        .put("source_user_id", "dummy_source_user_id")
                        .put("target", "dummy_target")
                        .put("user_id", "dummy_user_id")
                        .toString()
                )
            }

            private fun assertMessageValues(event: JSONObject) {
                val eventMessage = event.getJSONObject("values").getJSONObject("message")
                assertWithMessage("キャンペーンIDがcampaign_idパラメータとしてtrackサーバに送信されること")
                    .that(eventMessage.getString("campaign_id"))
                    .isEqualTo("dummy_campaign_id")
                assertWithMessage("短縮IDがshorten_idパラメータとしてtrackサーバに送信されること")
                    .that(eventMessage.getString("shorten_id"))
                    .isEqualTo("dummy_shorten_id")

                val eventValues = event.getJSONObject("values")
                assertWithMessage("krt_user_idがuser_idパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("user_id"))
                    .isEqualTo("dummy_user_id")
                assertWithMessage("krt_task_idがtask_idパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("task_id"))
                    .isEqualTo("dummy_task_id")
                assertWithMessage("krt_schedule_idがschedule_idパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("schedule_id"))
                    .isEqualTo("dummy_schedule_id")
                assertWithMessage("krt_source_user_idがsource_user_idパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("source_user_id"))
                    .isEqualTo("dummy_source_user_id")
                assertWithMessage("krt_targetがtargetパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("target"))
                    .isEqualTo("dummy_target")
            }

            @Test
            fun SDKが処理した場合message_clickが送信されること() {
                handleMessageBySDK(data)

                assertShowNotification()
                val reached = event("_message_reached")
                assertNotNull(reached)
                assertMessageValues(reached)

                clickNotification()

                val event = event("message_click")
                assertNotNull(event, "message_clickがtrackサーバに送信されること")
                assertMessageValues(event)
            }

            @Test
            fun SDKが処理し通知が削除された場合message_ignoredが送信されること() {
                handleMessageBySDK(data)

                assertShowNotification()
                val reached = event("_message_reached")
                assertNotNull(reached)
                assertMessageValues(reached)

                deleteNotification()

                val event = event("_message_ignored")
                assertNotNull(event, "message_ignoredがtrackサーバに送信されること")
                assertMessageValues(event)
            }

            @Test
            fun クライアントが処理message_clickが送信されること() {
                openAppByCustomIntent(data)

                val event = event("message_click")
                assertNotNull(event, "message_clickがtrackサーバに送信されること")
                assertMessageValues(event)
            }
        }

        class krt_push_notificationがfalse : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_push_notification", "false")
                put("krt_campaign_id", "dummy_campaign_id")
                put("krt_shorten_id", "dummy_shorten_id")
                put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが処理した場合message_clickが送信されないこと() {
                handleMessageBySDK(data)

                assertNotShowNotification()
                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが独自に処理した場合message_clickが送信されないこと() {
                openAppByCustomIntent(data)

                val event = event("message_click")
                assertWithMessage("message_clickがtrackサーバに送信されないこと")
                    .that(event).isNull()
            }
        }

        class krt_campaign_idを含まない場合 : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_push_notification", "true")
                put("krt_shorten_id", "dummy_shorten_id")
                put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが処理した場合message_clickが送信されないこと() {
                handleMessageBySDK(data)

                assertShowNotification()
                clickNotification()
                deleteNotification()

                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが処理した場合message_clickが送信されないこと() {
                openAppByCustomIntent(data)

                assertWithMessage("message_clickとしてtrackサーバに送信されないこと")
                    .that(event("message_click")).isNull()
            }
        }

        class krt_shorten_idを含まない場合 : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_push_notification", "true")
                put("krt_campaign_id", "dummy_campaign_id")
                put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが通知を処理した場合message_clickが送信されないこと() {
                handleMessageBySDK(data)

                assertShowNotification()
                clickNotification()
                deleteNotification()

                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが処理した場合message_clickが送信されないこと() {
                val data = HashMap<String, String>()
                data.put("krt_push_notification", "true")
                data.put("krt_campaign_id", "dummy_campaign_id")
                data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                data.put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
                openAppByCustomIntent(data)

                assertWithMessage("message_clickとしてtrackサーバに送信されないこと")
                    .that(event("message_click")).isNull()
            }
        }

        class krt_event_valuesを含まない場合 : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_push_notification", "true")
                put("krt_campaign_id", "dummy_campaign_id")
                put("krt_shorten_id", "dummy_shorten_id")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが通知を処理した場合message_clickが送信されないこと() {
                handleMessageBySDK(data)

                assertShowNotification()
                clickNotification()
                deleteNotification()

                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが処理した場合message_clickが送信されないこと() {
                openAppByCustomIntent(data)

                assertWithMessage("message_clickとしてtrackサーバに送信されないこと")
                    .that(event("message_click")).isNull()
            }
        }
    }

    @RunWith(Enclosed::class)
    class masspush {
        class 必要な全パラメータを含む : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_mass_push_notification", "true")
                put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが処理した場合mass_push_clickが送信されること() {
                handleMessageBySDK(data)
                assertNull(event("_message_reached"))

                assertShowNotification()
                clickNotification()

                val event = event("mass_push_click")
                assertNotNull(event, "mass_push_clickがtrackサーバに送信されること")
                val eventValues = event.getJSONObject("values")
                assertWithMessage("mass_push_idがパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("mass_push_id"))
                    .isEqualTo("dummy_mass_push_id")
            }

            @Test
            fun SDKが処理してもmass_pushでは補助イベントが送信されないこと() {
                handleMessageBySDK(data)
                assertNull(event("_message_reached"))

                assertShowNotification()
                deleteNotification()

                assertNull(event("_message_ignored"))
            }

            @Test
            fun クライアントが処理した場合mass_push_clickが送信されること() {
                openAppByCustomIntent(data)

                val event = event("mass_push_click")
                assertNotNull(event, "mass_push_clickがtrackサーバに送信されること")
                val eventValues = event.getJSONObject("values")
                assertWithMessage("mass_push_idがパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("mass_push_id"))
                    .isEqualTo("dummy_mass_push_id")
            }
        }

        class krt_mass_push_notificationがfalseの場合 : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_mass_push_notification", "false")
                put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが処理した場合mass_push_clickが送信されないこと() {
                handleMessageBySDK(data)
                assertNull(event("_message_reached"))

                assertNotShowNotification()
                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが処理した場合mass_push_clickが送信されないこと() {
                openAppByCustomIntent(data)

                assertWithMessage("mass_push_clickとしてtrackサーバに送信されないこと")
                    .that(event("mass_push_click")).isNull()
            }
        }

        class krt_event_valuesを含まない : PushTrackerTestCase() {
            private val data = HashMap<String, String>().apply {
                put("krt_mass_push_notification", "true")
                put(
                    "krt_attributes",
                    "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                )
            }

            @Test
            fun SDKが処理した場合mass_push_clickが送信されないこと() {
                handleMessageBySDK(data)
                assertNull(event("_message_reached"))

                assertShowNotification()
                clickNotification()
                deleteNotification()

                server.assertThatNoEventOccured()
            }

            @Test
            fun クライアントが処理した場合mass_push_clickが送信されないこと() {
                openAppByCustomIntent(data)

                assertWithMessage("mass_push_clickとしてtrackサーバに送信されないこと")
                    .that(event("mass_push_click")).isNull()
            }
        }
    }

    class KARTE経由の通知ではない : PushTrackerTestCase() {
        private val data = HashMap<String, String>().apply {
            put("krt_campaign_id", "dummy_campaign_id")
            put("krt_shorten_id", "dummy_shorten_id")
            put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
            put(
                "krt_attributes",
                "{\"title\":\"notification title\", \"body\":\"notification body\"}"
            )
        }

        @Test
        fun SDKが通知を処理した場合message_clickとmass_push_clickが送信されないこと() {
            handleMessageBySDK(data)
            assertNull(event("_message_reached"))

            assertNotShowNotification()
            server.assertThatNoEventOccured()
        }

        @Test
        fun クライアントが通知を処理した場合message_clickとmass_push_clickがに送信されないこと() {
            openAppByCustomIntent(data)

            assertWithMessage("mass_push_clickとしてtrackサーバに送信されないこと")
                .that(event("mass_push_click")).isNull()
            assertWithMessage("message_clickとしてtrackサーバに送信されないこと")
                .that(event("message_click")).isNull()
        }
    }
}
