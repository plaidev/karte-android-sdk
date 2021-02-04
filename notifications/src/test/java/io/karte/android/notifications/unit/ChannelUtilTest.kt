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
package io.karte.android.notifications.unit

import android.app.NotificationChannel
import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.notifications.internal.ChannelUtil
import io.karte.android.notifications.manager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelUtilTest : RobolectricTestCase() {

    @Test
    fun channelがなければデフォルトを返すこと() {
        val channel = ChannelUtil.getChannel(manager, "test")
        assertThat(channel).isNotEqualTo("test")
        assertThat(channel).isEqualTo("krt_default_channel")
    }

    @Test
    fun channelがあればそれを返すこと() {
        manager.createNotificationChannel(NotificationChannel("test", "test", 0))

        val channel = ChannelUtil.getChannel(manager, "test")
        assertThat(channel).isEqualTo("test")

        manager.deleteNotificationChannel("test")
    }
}
