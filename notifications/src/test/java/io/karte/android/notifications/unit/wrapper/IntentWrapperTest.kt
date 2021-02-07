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
package io.karte.android.notifications.unit.wrapper

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.notifications.MessageReceiver
import io.karte.android.notifications.internal.wrapper.EventType
import io.karte.android.notifications.internal.wrapper.IntentWrapper
import io.karte.android.notifications.internal.wrapper.MessageWrapper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentWrapperTest : RobolectricTestCase() {

    @Test
    fun inValidなIntentであること() {
        val intent = Intent()
        val wrapper = IntentWrapper(intent)
        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isFalse()
    }

    @Test
    fun ターゲットpushのパラメータをパースすること() {
        val intent = Intent().apply {
            putExtra("krt_push_notification", "true")
            putExtra("krt_campaign_id", "sampleCampaignId")
            putExtra("krt_shorten_id", "sampleShortenId")
            putExtra("krt_event_values", "{\"test\":\"aaa\"}")
        }
        val wrapper = IntentWrapper(intent)
        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isTrue()
        assertThat(wrapper.isTargetPush).isTrue()
        assertThat(wrapper.isMassPush).isFalse()
        assertThat(wrapper.campaignId).isEqualTo("sampleCampaignId")
        assertThat(wrapper.shortenId).isEqualTo("sampleShortenId")
        assertThat(wrapper.eventType).isNull()
        assertThat(wrapper.eventValues).containsEntry("test", "aaa")
    }

    @Test
    fun Intentをwrapすること() {
        val message = MessageWrapper(
            mapOf(
                "krt_push_notification" to "true",
                "krt_campaign_id" to "sampleCampaignId",
                "krt_shorten_id" to "sampleShortenId",
                "krt_event_values" to "{\"test\":\"aaa\"}"
            )
        )
        val intent = IntentWrapper
            .wrapIntent(application, message, EventType.MESSAGE_CLICK, Intent("test_action"))
        val wrapper = IntentWrapper(intent)

        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isTrue()
        assertThat(wrapper.isTargetPush).isTrue()
        assertThat(wrapper.isMassPush).isFalse()
        assertThat(wrapper.campaignId).isEqualTo("sampleCampaignId")
        assertThat(wrapper.shortenId).isEqualTo("sampleShortenId")
        assertThat(wrapper.eventType).isEqualTo(EventType.MESSAGE_CLICK)
        assertThat(wrapper.eventValues).containsEntry("test", "aaa")
    }

    @Test
    fun wrapしたIntentを編集すること() {
        val message = MessageWrapper(
            mapOf(
                "krt_push_notification" to "true",
                "krt_campaign_id" to "sampleCampaignId",
                "krt_shorten_id" to "sampleShortenId",
                "krt_event_values" to "{\"test\":\"aaa\"}"
            )
        )
        val orgIntent = Intent(application, Activity::class.java)
        val intent = IntentWrapper
            .wrapIntent(application, message, EventType.MESSAGE_CLICK, orgIntent)
        val wrapper = IntentWrapper(intent)

        assertThat(wrapper.intent.component)
            .isEqualTo(ComponentName(application, MessageReceiver::class.java))
        wrapper.popComponentName()
        assertThat(wrapper.intent.component)
            .isEqualTo(ComponentName(application, Activity::class.java))

        assertThat(wrapper.intent.hasExtra("krt_event_values")).isTrue()
        wrapper.clean()
        assertThat(wrapper.intent.hasExtra("krt_event_values")).isFalse()
    }
}
