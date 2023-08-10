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

import android.app.Notification
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.NOTIFICATION_TAG
import io.karte.android.notifications.internal.wrapper.KEY_CAMPAIGN_ID
import io.karte.android.notifications.internal.wrapper.KEY_SHORTEN_ID
import io.karte.android.notifications.manager
import io.karte.android.notifications.uniqueId
import io.karte.android.test_lib.RobolectricTestCase
import io.karte.android.test_lib.integration.TrackerTestCase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.net.URL
import kotlin.test.assertNotNull

private const val NOTIFICAITON_ID = 1000

private val RobolectricTestCase.packageName: String
    get() {
        return application.packageName
    }

private fun RobolectricTestCase.createPackageInfo(metaDataMap: Map<String, Int> = emptyMap()):
    PackageInfo {
    val metaData = Bundle()
    metaDataMap.entries.forEach { metaData.putInt(it.key, it.value) }
    val applicationInfo = ApplicationInfo()
    applicationInfo.metaData = metaData

    val packageInfo = PackageInfo()
    packageInfo.packageName = packageName
    packageInfo.applicationInfo = applicationInfo
    return packageInfo
}

private fun RobolectricTestCase.getNotification(id: Int = NOTIFICAITON_ID): Notification {
    val notification = shadowOf(manager).getNotification(NOTIFICATION_TAG, id)
    assertNotNull(notification)
    return notification
}

private fun RobolectricTestCase.assertNotShowNotification() {
    assertThat(shadowOf(manager).size()).isEqualTo(0)
}

private fun createIntent(
    action: String,
    category: String? = null,
    packageName: String? = null,
    link: String? = null
): Intent {
    val launchIntent = Intent(action)
    if (category != null) {
        launchIntent.addCategory(category)
    }
    if (packageName != null) {
        launchIntent.setPackage(packageName)
    }
    if (link != null) {
        launchIntent.data = Uri.parse(link)
    }
    return launchIntent
}

private fun createResolvedInfo(packageName: String, className: String): ResolveInfo {
    val resolveInfo = ResolveInfo()
    val activityInfo = ActivityInfo()
    activityInfo.packageName = packageName
    activityInfo.name = className
    resolveInfo.activityInfo = activityInfo
    return resolveInfo
}

@RunWith(Enclosed::class)
class MessageHandlerTest {

    class handleMessage : RobolectricTestCase() {

        private val sampleTitle = "sample_title"
        private val sampleBody = "sample_body"
        private val sampleUrl = "karte-tracker-test://simplepage"
        private val sampleCampaignId = "campaign1"
        private val sampleShortenId = "shorten1"
        lateinit var mainIntent: Intent
        lateinit var linkIntent: Intent
        private val sampleAttachmentUrl: URL? = this.javaClass.classLoader?.getResource("1kx1k.png")
        private fun createRemoteMessage(
            title: String = sampleTitle,
            body: String = sampleBody,
            url: String? = null,
            sound: String? = null,
            attachmentUrl: URL? = null
        ): RemoteMessage {
            val attr = JSONObject()
            attr.put("title", title)
            attr.put("body", body)
            if (url != null) attr.put("url", url)
            if (sound != null) attr.put("sound", sound)
            if (attachmentUrl != null) {
                attr.put("attachment_type", "image")
                attr.put("attachment_url", attachmentUrl)
            }

            return RemoteMessage.Builder("dummyToken")
                .addData("krt_push_notification", "true")
                .addData("krt_campaign_id", sampleCampaignId)
                .addData("krt_shorten_id", sampleShortenId)
                .addData("krt_attributes", attr.toString())
                .build()
        }

        @Suppress("DEPRECATION")
        @Before
        fun setup() {
            // packageManagerの準備.
            // getLaunchIntentForPackageの返却値を設定
            val mainResolvedInfo =
                createResolvedInfo("io.karte.android.tracker.Integration.mock", "TestActivity")
            mainIntent = createIntent(Intent.ACTION_MAIN, Intent.CATEGORY_LAUNCHER, packageName)

            // packageManager.queryIntentActivitiesの準備. urlが指定された場合のactivity
            linkIntent = createIntent(Intent.ACTION_VIEW, link = sampleUrl)
            val linkResolvedInfo =
                createResolvedInfo("io.karte.android.tracker.Integration.mock", "TestActivity")

            val packageManager = shadowOf(application.packageManager)
            packageManager.addResolveInfoForIntent(mainIntent, mainResolvedInfo)
            packageManager.addResolveInfoForIntent(linkIntent, linkResolvedInfo)

            packageManager.installPackage(createPackageInfo())

            mockkStatic("io.karte.android.notifications.MessageHandlerKt")
            every { uniqueId() } returns NOTIFICAITON_ID
        }

        @Suppress("DEPRECATION")
        @After
        fun tearDown() {
            val packageManager = shadowOf(application.packageManager)
            packageManager.removePackage(packageName)

            packageManager.removeResolveInfosForIntent(mainIntent, packageName)
            packageManager.removeResolveInfosForIntent(linkIntent, packageName)

            unmockkStatic("io.karte.android.notifications.MessageHandlerKt")
        }

        @Test
        fun karteからの通知である場合_返り値がtrueであること() {
            val ret =
                MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(ret).isTrue()
        }

        @Test
        fun karte以外からの通知である場合_ハンドルしないこと() {
            val ret = MessageHandler.handleMessage(
                application,
                RemoteMessage.Builder("dummyToken").build()
            )
            assertThat(ret).isFalse()
            assertNotShowNotification()
        }

        @Test
        fun タイトルがセットされること() {
            MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(shadowOf(getNotification()).contentTitle).isEqualTo(sampleTitle)
        }

        @Test
        fun 本文がセットされること() {
            MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(shadowOf(getNotification()).bigText).isEqualTo(sampleBody)
        }

        @Test
        fun スモールアイコンが設定されている場合_通知にセットされること() {
            val packageManager = shadowOf(application.packageManager)
            packageManager.installPackage(
                createPackageInfo(
                    mapOf(
                        Pair(
                            "io.karte.android.Tracker.notification_icon",
                            android.R.drawable.btn_minus
                        )
                    )
                )
            )
            MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(getNotification().smallIcon).isNotNull()
            assertThat(getNotification().smallIcon.resId).isEqualTo(android.R.drawable.btn_minus)
        }

        @Test
        fun ラージアイコンが設定されている場合_通知にセットされること() {
            val packageManager = shadowOf(application.packageManager)
            packageManager.installPackage(
                createPackageInfo(
                    mapOf(
                        Pair(
                            "io.karte.android.Tracker.notification_large_icon",
                            android.R.mipmap.sym_def_app_icon
                        )
                    )
                )
            )
            MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(getNotification().getLargeIcon()).isNotNull()
            assertThat(getNotification().getLargeIcon().type).isEqualTo(Icon.TYPE_BITMAP)
        }

        @Test
        fun ラージアイコンが設定されていない場合_通知にセットされないこと() {
            MessageHandler.handleMessage(application, createRemoteMessage())
            assertThat(getNotification().getLargeIcon()).isNull()
        }

        @Test
        fun attchment_urlが設定されている場合_largeIconがセットされること() {
            MessageHandler.handleMessage(
                application,
                createRemoteMessage(attachmentUrl = sampleAttachmentUrl)
            )
            assertThat(getNotification().getLargeIcon()).isNotNull()
        }

        @Test
        fun urlが指定されてない場合_launcherアクティビティがセットされること() {
            val id = 99
            every { uniqueId() } returns id
            MessageHandler.handleMessage(application, createRemoteMessage())

            val contentIntent = shadowOf(getNotification(id).contentIntent)
            assertThat(contentIntent.requestCode).isEqualTo(id)
            assertThat(contentIntent.savedIntent.action).isEqualTo(Intent.ACTION_MAIN)
            assertThat(contentIntent.savedIntent.extras?.getString("krt_component_name"))
                .isEqualTo("${application.packageName}/TestActivity")
        }

        @Test
        fun urlが指定されてない場合_引数に渡されたデフォルトのIntentがセットされること() {
            val id = 98
            every { uniqueId() } returns id
            val uri = Uri.parse(sampleUrl)
            val defIntent = Intent(Intent.ACTION_VIEW, uri)
            MessageHandler.handleMessage(application, createRemoteMessage(), defIntent)

            val contentIntent = shadowOf(getNotification(id).contentIntent)
            assertThat(contentIntent.requestCode).isEqualTo(id)
            assertThat(contentIntent.savedIntent.data).isEqualTo(uri)
            assertThat(contentIntent.savedIntent.action).isEqualTo(Intent.ACTION_VIEW)
        }

        @Test
        fun message_click計測用の値がセットされること() {
            val id = 97
            every { uniqueId() } returns id
            MessageHandler.handleMessage(application, createRemoteMessage())

            val contentIntent = shadowOf(getNotification(id).contentIntent)
            assertThat(contentIntent.requestCode).isEqualTo(id)
            assertThat(contentIntent.savedIntent.extras?.getString(KEY_CAMPAIGN_ID))
                .isEqualTo(sampleCampaignId)
            assertThat(contentIntent.savedIntent.extras?.getString(KEY_SHORTEN_ID))
                .isEqualTo(sampleShortenId)
        }

        // TODO: channel_id周りのテスト
        // TODO: bigImage周りのテスト

        @Test
        fun 複数回通知された時にrequestCodeやidが異なること() {
            val firstId = 96
            every { uniqueId() } returns firstId
            MessageHandler.handleMessage(application, createRemoteMessage())
            val secondId = 95
            every { uniqueId() } returns secondId
            MessageHandler.handleMessage(application, createRemoteMessage())

            val firstContentIntent = shadowOf(getNotification(firstId).contentIntent)
            assertThat(firstContentIntent.requestCode).isEqualTo(firstId)
            val secondContentIntent = shadowOf(getNotification(secondId).contentIntent)
            assertThat(secondContentIntent.requestCode).isEqualTo(secondId)
        }
    }

    @RunWith(Enclosed::class)
    class canHandleMessage {
        class krt_push_notificationがtrueの場合 : RobolectricTestCase() {
            @Test
            fun trueを返すこと() {
                val remoteMessage =
                    RemoteMessage.Builder("dummyToken").addData("krt_push_notification", "true")
                        .build()
                val actual = MessageHandler.canHandleMessage(remoteMessage)
                assertThat(actual).isTrue()
            }
        }

        class krt_mass_push_notificationがtrueの場合 : RobolectricTestCase() {
            @Test
            fun trueを返すこと() {
                val remoteMessage = RemoteMessage.Builder("dummyToken")
                    .addData("krt_mass_push_notification", "true").build()
                val actual = MessageHandler.canHandleMessage(remoteMessage)
                assertThat(actual).isTrue()
            }
        }

        class krt_push_notificationとkrt_mass_push_notificationの両方を含まない場合 : RobolectricTestCase() {
            @Test
            fun falseを返すこと() {
                val remoteMessage =
                    RemoteMessage.Builder("dummyToken").addData("hoge", "huga").build()
                val actual = MessageHandler.canHandleMessage(remoteMessage)
                assertThat(actual).isFalse()
            }
        }

        class krt_push_notificationがfalseでkrt_mass_push_notificationを含まない場合 : TrackerTestCase() {
            @Test
            fun falseを返すこと() {
                val remoteMessage =
                    RemoteMessage.Builder("dummyToken").addData("krt_push_notification", "false")
                        .build()
                val actual = MessageHandler.canHandleMessage(remoteMessage)
                assertThat(actual).isFalse()
            }
        }

        class krt_mass_push_notificationがfalseでkrt_push_notificationを含まない場合 : TrackerTestCase() {
            @Test
            fun falseを返すこと() {
                val remoteMessage = RemoteMessage.Builder("dummyToken")
                    .addData("krt_mass_push_notification", "false").build()
                val actual = MessageHandler.canHandleMessage(remoteMessage)
                assertThat(actual).isFalse()
            }
        }
    }

    @RunWith(Enclosed::class)
    class extractKarteAttributes {
        class JSONにkrt_attributesが含まれている場合 : RobolectricTestCase() {
            @Test
            fun KarteAttributesを返すこと() {
                val json = JSONObject("{\"url\"=\"https://www.example.com\"}")
                val remoteMessage =
                    RemoteMessage.Builder("dummyToken").addData("krt_attributes", json.toString())
                        .build()
                val actual = MessageHandler.extractKarteAttributes(remoteMessage)
                assertThat(actual).isNotNull()
                assertThat(actual?.link.toString()).isEqualTo("https://www.example.com")
            }
        }

        class JSONにkrt_attributesが含まれていない場合 : RobolectricTestCase() {
            @Test
            fun nullを返すこと() {
                val remoteMessage =
                    RemoteMessage.Builder("dummyToken").addData("hoge", "fuga").build()
                val actual = MessageHandler.extractKarteAttributes(remoteMessage)
                assertThat(actual).isNull()
            }
        }
    }
}
