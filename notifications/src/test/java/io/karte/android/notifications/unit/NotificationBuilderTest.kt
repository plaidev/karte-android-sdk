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

import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.notifications.KarteAttributes
import io.karte.android.notifications.internal.NotificationBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class NotificationBuilderTest : RobolectricTestCase() {

    @Test
    fun 通知が作成されること() {
        val notification = NotificationBuilder.build(
            application,
            "test",
            KarteAttributes(title = "title", body = "body")
        )
        notification.run {
            assertThat(contentIntent).isNull()
            assertThat(deleteIntent).isNull()
            assertThat(channelId).isEqualTo("test")
        }
        Shadows.shadowOf(notification).run {
            assertThat(contentTitle).isEqualTo("title")
            assertThat(contentText).isEqualTo("body")
        }
    }
}
