//
//  Copyright 2021 PLAID, Inc.
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

package io.karte.android.notifications.unit.wrapper

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.karte.android.notifications.MessageReceiveActivity
import io.karte.android.notifications.MessageReceiver
import io.karte.android.notifications.internal.wrapper.ACTION_KARTE_IGNORED
import io.karte.android.notifications.internal.wrapper.IntentProcessor
import io.karte.android.notifications.internal.wrapper.RemoteMessageWrapper
import io.karte.android.notifications.internal.wrapper.isTrampolineBlocked
import io.karte.android.test_lib.RobolectricTestCase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class IntentProcessorTest : RobolectricTestCase() {
    private val message = RemoteMessageWrapper(
        mapOf(
            "krt_push_notification" to "true",
            "krt_campaign_id" to "sampleCampaignId",
            "krt_shorten_id" to "sampleShortenId",
            "krt_event_values" to "{\"test\":\"aaa\"}"
        )
    )

    @Test
    fun IntentをClick用に加工すること() {
        val intent = Intent("test_action")

        val pendingIntent =
            IntentProcessor.forClick(application, message, intent).pendingIntent(100)
        val shadow = shadowOf(pendingIntent)
        assertThat(shadow.requestCode).isEqualTo(100)
        assertThat(shadow.isBroadcastIntent).isTrue()
        assertThat(shadow.flags and PendingIntent.FLAG_IMMUTABLE)
            .isEqualTo(PendingIntent.FLAG_IMMUTABLE)
        assertThat(shadow.flags and PendingIntent.FLAG_ONE_SHOT)
            .isEqualTo(PendingIntent.FLAG_ONE_SHOT)
        assertThat(shadow.savedContext).isEqualTo(application)

        val savedIntent = shadow.savedIntent
        assertThat(savedIntent.action).isEqualTo(intent.action)
        assertThat(savedIntent.component)
            .isEqualTo(ComponentName(application, MessageReceiver::class.java))
        assertThat(savedIntent.getStringExtra("krt_push_notification")).isEqualTo("true")
        assertThat(savedIntent.getStringExtra("krt_campaign_id")).isEqualTo("sampleCampaignId")
        assertThat(savedIntent.getStringExtra("krt_shorten_id")).isEqualTo("sampleShortenId")
    }

    @Test
    fun IntentをIgnore用に加工すること() {
        val pendingIntent = IntentProcessor.forIgnore(application, message).pendingIntent(101)

        val shadow = shadowOf(pendingIntent)
        assertThat(shadow.requestCode).isEqualTo(101)
        assertThat(shadow.isBroadcastIntent).isTrue()
        assertThat(shadow.flags and PendingIntent.FLAG_IMMUTABLE)
            .isEqualTo(PendingIntent.FLAG_IMMUTABLE)
        assertThat(shadow.flags and PendingIntent.FLAG_ONE_SHOT)
            .isEqualTo(PendingIntent.FLAG_ONE_SHOT)
        assertThat(shadow.savedContext).isEqualTo(application)

        val savedIntent = shadow.savedIntent
        assertThat(savedIntent.action).isEqualTo(ACTION_KARTE_IGNORED)
        assertThat(savedIntent.component)
            .isEqualTo(ComponentName(application, MessageReceiver::class.java))
        assertThat(savedIntent.getStringExtra("krt_push_notification")).isEqualTo("true")
        assertThat(savedIntent.getStringExtra("krt_campaign_id")).isEqualTo("sampleCampaignId")
        assertThat(savedIntent.getStringExtra("krt_shorten_id")).isEqualTo("sampleShortenId")
    }

    @Test
    fun 通知トランポリン制限時_Click() {
        mockkStatic("io.karte.android.notifications.internal.wrapper.IntentWrapperKt")
        every { isTrampolineBlocked(any()) } returns true

        val intent = Intent("test_action")
        val pendingIntent =
            IntentProcessor.forClick(application, message, intent).pendingIntent(100)
        val shadow = shadowOf(pendingIntent)
        // Activityで受け取る
        assertThat(shadow.isActivityIntent).isTrue()
        assertThat(shadow.savedIntent.component)
            .isEqualTo(ComponentName(application, MessageReceiveActivity::class.java))

        unmockkStatic("io.karte.android.notifications.internal.wrapper.IntentWrapperKt")
    }

    @Test
    fun 通知トランポリン制限時_Ignore() {
        mockkStatic("io.karte.android.notifications.internal.wrapper.IntentWrapperKt")
        every { isTrampolineBlocked(any()) } returns true

        val pendingIntent = IntentProcessor.forIgnore(application, message).pendingIntent(100)
        val shadow = shadowOf(pendingIntent)
        // Receiverのまま
        assertThat(shadow.isBroadcastIntent).isTrue()
        assertThat(shadow.savedIntent.component)
            .isEqualTo(ComponentName(application, MessageReceiver::class.java))

        unmockkStatic("io.karte.android.notifications.internal.wrapper.IntentWrapperKt")
    }
}
