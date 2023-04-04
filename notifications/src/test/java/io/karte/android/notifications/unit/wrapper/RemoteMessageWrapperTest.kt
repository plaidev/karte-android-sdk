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

import com.google.common.truth.Truth.assertThat
import io.karte.android.notifications.internal.wrapper.RemoteMessageWrapper
import io.karte.android.test_lib.RobolectricTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteMessageWrapperTest : RobolectricTestCase() {

    @Test
    fun karteの通知でないこと() {
        val data = mapOf<String, String>()
        val wrapper = RemoteMessageWrapper(data)
        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isFalse()
    }

    @Test
    fun ターゲットpushのパラメータをパースすること() {
        val data = mapOf(
            "krt_push_notification" to "true",
            "krt_campaign_id" to "sampleCampaignId",
            "krt_shorten_id" to "sampleShortenId",
            "krt_event_values" to "{\"test\":\"aaa\"}",
            "krt_attributes" to "{\"title\":\"title\",\"body\":\"body\"}"
        )
        val wrapper = RemoteMessageWrapper(data)
        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isTrue()
        assertThat(wrapper.isTargetPush).isTrue()
        assertThat(wrapper.isMassPush).isFalse()
        assertThat(wrapper.campaignId).isEqualTo("sampleCampaignId")
        assertThat(wrapper.shortenId).isEqualTo("sampleShortenId")
        assertThat(wrapper.eventValues).containsEntry("test", "aaa")
        assertThat(wrapper.attributes).isNotNull()
        assertThat(wrapper.attributes?.title).isEqualTo("title")
        assertThat(wrapper.attributes?.body).isEqualTo("body")
    }

    @Test
    fun mass_pushのパラメータをパースすること() {
        val data = mapOf("krt_mass_push_notification" to "true")
        val wrapper = RemoteMessageWrapper(data)
        assertThat(wrapper).isNotNull()
        assertThat(wrapper.isValid).isTrue()
        assertThat(wrapper.isTargetPush).isFalse()
        assertThat(wrapper.isMassPush).isTrue()
    }
}
