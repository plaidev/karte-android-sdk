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
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.KarteApp
import io.karte.android.TrackerTestCase
import io.karte.android.assertThatNoEventOccured
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.MessageReceiver
import io.karte.android.notifications.registerFCMToken
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import java.util.HashMap

private fun TrackerTestCase.proceedNotification(data: HashMap<String, String>) {
    Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)

    val launchIntent = Intent(Intent.ACTION_MAIN)
    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    launchIntent.setPackage(application.packageName)
    val resolveInfo = ResolveInfo()
    val activityInfo = ActivityInfo()
    activityInfo.packageName = application.packageName
    activityInfo.name = "MainActivity"
    resolveInfo.activityInfo = activityInfo
    @Suppress("DEPRECATION")
    Shadows.shadowOf(application.packageManager).addResolveInfoForIntent(launchIntent, resolveInfo)

    val builder = RemoteMessage.Builder("dummyDestination")
    builder.setData(data)
    val remoteMessage = builder.build()
    MessageHandler.handleMessage(application, remoteMessage)
    proceedBufferedCall()
}

private fun TrackerTestCase.proceedMessage(notification: Notification) {
    val pendingIntent = notification.contentIntent
    val intent = Shadows.shadowOf(pendingIntent).savedIntent

    // NOTE: このintentをbroadcastして、MessageReceiverのonReceiveが呼ばれることもテストしたいが現状では無理そう。
    // see https://github.com/plaidev/tracker-android/pull/163#issuecomment-470797618
    // ひとまずreceiverを無理やり生成してonReceiveを呼んでテストする。
    val receiver = MessageReceiver()
    receiver.onReceive(application, intent)
    proceedBufferedCall()
}

private val TrackerTestCase.notificationManager: NotificationManager
    get() {
        return application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

private fun TrackerTestCase.clickNotification(data: HashMap<String, String>) {
    val intent = Intent(application, Activity::class.java)
    MessageHandler.copyInfoToIntent(data, intent)
    Robolectric.buildActivity(Activity::class.java, intent).start()
    proceedBufferedCall()
}

@Suppress("NonAsciiCharacters")
@RunWith(Enclosed::class)
class PushTrackTest {
    @RunWith(Enclosed::class)
    class FCMトークンの取得に成功した場合 {
        class 通知が許可されている場合 : TrackerTestCase() {
            @Test
            fun plugin_native_app_identifyイベントがサーバに送信されること() {
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)
                KarteApp.registerFCMToken("dummy_fcm_token")
                proceedBufferedCall()

                val request = server.takeRequest()
                val event =
                    JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                val eventValues = event.getJSONObject("values")
                assertWithMessage("event_nameがplugin_native_app_identifyとしてtrackサーバに送信されること")
                    .that(event.getString("event_name"))
                    .isEqualTo("plugin_native_app_identify")
                assertWithMessage("FCMトークンがfcm_tokenパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("fcm_token"))
                    .isEqualTo("dummy_fcm_token")
                assertWithMessage("通知の可否がsubscribeパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("subscribe"))
                    .isEqualTo("true")
            }
        }

        class 通知が許可されていない場合 : TrackerTestCase() {
            @Test
            fun plugin_native_app_identifyイベントがサーバに送信されること() {
                server.enqueue(
                    MockResponse().setBody(body.toString()).addHeader(
                        "Content-Type",
                        "text/html; charset=utf-8"
                    )
                )
                Shadows.shadowOf(notificationManager).setNotificationsEnabled(false)
                KarteApp.registerFCMToken("dummy_fcm_token")
                proceedBufferedCall()

                val request = server.takeRequest()
                val event =
                    JSONObject(request.parseBody()).getJSONArray("events").getJSONObject(0)
                val eventValues = event.getJSONObject("values")
                assertWithMessage("event_nameがplugin_native_app_identifyとしてtrackサーバに送信されること")
                    .that(event.getString("event_name"))
                    .isEqualTo("plugin_native_app_identify")
                assertWithMessage("FCMトークンがfcm_tokenパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("fcm_token"))
                    .isEqualTo("dummy_fcm_token")
                assertWithMessage("通知の可否がsubscribeパラメータとしてtrackサーバに送信されること")
                    .that(eventValues.getString("subscribe"))
                    .isEqualTo("false")
            }
        }
    }

    class FCMトークンの取得に失敗した場合 : TrackerTestCase() {
        @Test
        fun FCMトークンがサーバに送信されないこと() {
            server.enqueue(
                MockResponse().setBody(body.toString()).addHeader(
                    "Content-Type",
                    "text/html; charset=utf-8"
                )
            )
            // TODO
            // Tracker.trackFCMToken(null)
            proceedBufferedCall()
            server.assertThatNoEventOccured()
        }
    }

    @RunWith(Enclosed::class)
    class 通知開封 {
        @RunWith(Enclosed::class)
        class 通常配信 {
            @RunWith(Enclosed::class)
            class 必要な全パラメータを含む {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されること() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        val request = server.takeRequest()
                        val event = JSONObject(request.parseBody()).getJSONArray("events")
                            .getJSONObject(0)
                        val eventMessage = event.getJSONObject("values").getJSONObject("message")
                        assertWithMessage("event_nameがtrackサーバに送信されること")
                            .that(event.getString("event_name"))
                            .isEqualTo("message_click")
                        assertWithMessage("キャンペーンIDがcampaign_idパラメータとしてtrackサーバに送信されること")
                            .that(eventMessage.getString("campaign_id"))
                            .isEqualTo("dummy_campaign_id")
                        assertWithMessage("短縮IDがshorten_idパラメータとしてtrackサーバに送信されること")
                            .that(eventMessage.getString("shorten_id"))
                            .isEqualTo("dummy_shorten_id")
                    }
                }

                class クライアントが処理 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されること() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val event = JSONObject(request.parseBody()).getJSONArray("events")
                            .getJSONObject(1)
                        val eventMessage = event.getJSONObject("values").getJSONObject("message")
                        assertWithMessage("event_nameがtrackサーバに送信されること")
                            .that(event.getString("event_name"))
                            .isEqualTo("message_click")
                        assertWithMessage("キャンペーンIDがcampaign_idパラメータとしてtrackサーバに送信されること")
                            .that(eventMessage.getString("campaign_id"))
                            .isEqualTo("dummy_campaign_id")
                        assertWithMessage("短縮IDがshorten_idパラメータとしてtrackサーバに送信されること")
                            .that(eventMessage.getString("shorten_id"))
                            .isEqualTo("dummy_shorten_id")
                    }
                }

                class ターゲット配信の処理 : TrackerTestCase() {
                    @Test
                    fun message_clickに必要なパラメータが追加されていること() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put(
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
                        clickNotification(data)

                        val request = server.takeRequest()
                        val event = JSONObject(request.parseBody()).getJSONArray("events")
                            .getJSONObject(1)
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
                        assertWithMessage(
                            "krt_source_user_idがsource_user_idパラメータとしてtrackサーバに送信されること"
                        )
                            .that(eventValues.getString("source_user_id"))
                            .isEqualTo("dummy_source_user_id")
                        assertWithMessage("krt_targetがtargetパラメータとしてtrackサーバに送信されること")
                            .that(eventValues.getString("target"))
                            .isEqualTo("dummy_target")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_push_notificationがfalse {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "false")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)

                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(0)
                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが独自に処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "false")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmessage_clickとしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("message_click")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_campaign_idを含まない場合 {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )

                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(1)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )

                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmessage_clickとしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("message_click")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_shorten_idを含まない場合 {
                class SDKが通知を処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(1)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_event_values", "{\"task_id\": \"dummy_task_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmessage_clickとしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("message_click")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_event_valuesを含まない場合 {
                class SDKが通知を処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(1)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun message_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_push_notification", "true")
                        data.put("krt_campaign_id", "dummy_campaign_id")
                        data.put("krt_shorten_id", "dummy_shorten_id")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmessage_clickとしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("message_click")
                    }
                }
            }
        }

        @RunWith(Enclosed::class)
        class masspush {
            @RunWith(Enclosed::class)
            class 必要な全パラメータを含む {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されること() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "true")
                        data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(1)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        val request = server.takeRequest()
                        val event = JSONObject(request.parseBody()).getJSONArray("events")
                            .getJSONObject(0)
                        val eventValues = event.getJSONObject("values")

                        assertWithMessage("event_nameがtrackサーバに送信されること")
                            .that(event.getString("event_name"))
                            .isEqualTo("mass_push_click")
                        assertWithMessage("mass_push_idがパラメータとしてtrackサーバに送信されること")
                            .that(eventValues.getString("mass_push_id"))
                            .isEqualTo("dummy_mass_push_id")
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されること() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "true")
                        data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val event = JSONObject(request.parseBody()).getJSONArray("events")
                            .getJSONObject(1)
                        val eventValues = event.getJSONObject("values")
                        assertWithMessage("event_nameがtrackサーバに送信されること")
                            .that(event.getString("event_name"))
                            .isEqualTo("mass_push_click")
                        assertWithMessage("mass_push_idがパラメータとしてtrackサーバに送信されること")
                            .that(eventValues.getString("mass_push_id"))
                            .isEqualTo("dummy_mass_push_id")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_mass_push_notificationがfalseの場合 {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "false")
                        data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(0)
                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "false")
                        data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmass_push_clickしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("mass_push_click")
                    }
                }
            }

            @RunWith(Enclosed::class)
            class krt_event_valuesを含まない {
                class SDKが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "true")

                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )

                        proceedNotification(data)
                        assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(1)
                        val notification =
                            Shadows.shadowOf(notificationManager).allNotifications.get(0)
                        proceedMessage(notification)

                        server.assertThatNoEventOccured()
                    }
                }

                class クライアントが処理した場合 : TrackerTestCase() {
                    @Test
                    fun mass_push_clickが送信されないこと() {
                        server.enqueue(
                            MockResponse().setBody(body.toString()).addHeader(
                                "Content-Type",
                                "text/html; charset=utf-8"
                            )
                        )
                        val data = HashMap<String, String>()
                        data.put("krt_mass_push_notification", "true")

                        data.put(
                            "krt_attributes",
                            "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                        )
                        clickNotification(data)

                        val request = server.takeRequest()
                        val events = JSONObject(request.parseBody()).getJSONArray("events")
                        val event = events.getJSONObject(0)
                        assertThat(events.length()).isEqualTo(1)
                        assertWithMessage("event_nameがmass_push_clickしてtrackサーバに送信されないこと")
                            .that(event.getString("event_name"))
                            .isNotEqualTo("mass_push_click")
                    }
                }
            }
        }

        @RunWith(Enclosed::class)
        class KARTE経由の通知ではない {
            class SDKが通知を処理した場合 : TrackerTestCase() {
                @Test
                fun message_clickとmass_push_clickが送信されないこと() {
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    val data = HashMap<String, String>()
                    data.put("krt_campaign_id", "dummy_campaign_id")
                    data.put("krt_shorten_id", "dummy_shorten_id")
                    data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                    data.put(
                        "krt_attributes",
                        "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                    )

                    proceedNotification(data)
                    assertThat(Shadows.shadowOf(notificationManager).size()).isEqualTo(0)
                    server.assertThatNoEventOccured()
                }
            }

            class クライアントが通知を処理した場合 : TrackerTestCase() {
                @Test
                fun message_clickとmass_push_clickがに送信されないこと() {
                    server.enqueue(
                        MockResponse().setBody(body.toString()).addHeader(
                            "Content-Type",
                            "text/html; charset=utf-8"
                        )
                    )
                    val data = HashMap<String, String>()
                    data.put("krt_campaign_id", "dummy_campaign_id")
                    data.put("krt_shorten_id", "dummy_shorten_id")
                    data.put("krt_event_values", "{\"mass_push_id\": \"dummy_mass_push_id\"}")
                    data.put(
                        "krt_attributes",
                        "{\"title\":\"notification title\", \"body\":\"notification body\"}"
                    )
                    clickNotification(data)

                    val request = server.takeRequest()
                    val events = JSONObject(request.parseBody()).getJSONArray("events")
                    val event = events.getJSONObject(0)
                    assertThat(events.length()).isEqualTo(1)
                    assertWithMessage("event_nameがmass_push_clickしてtrackサーバに送信されないこと")
                        .that(event.getString("event_name"))
                        .isNotEqualTo("mass_push_click")
                    assertWithMessage("event_nameがmessage_clickとしてtrackサーバに送信されないこと")
                        .that(event.getString("event_name"))
                        .isNotEqualTo("message_click")
                }
            }
        }
    }
}
