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

import android.app.Notification
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.karte.android.notifications.internal.IntentAppender
import io.karte.android.notifications.internal.wrapper.RemoteMessageWrapper
import io.karte.android.test_lib.RobolectricTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class IntentAppenderTest : RobolectricTestCase() {

    @Test
    fun notificationにintentを付与すること() {
        val notification = Notification()
        assertThat(notification.contentIntent).isNull()
        assertThat(notification.deleteIntent).isNull()

        IntentAppender.append(
            notification,
            application,
            0,
            RemoteMessageWrapper(mapOf()),
            Intent(Intent.ACTION_VIEW)
        )

        assertThat(notification.contentIntent).isNotNull()
        Shadows.shadowOf(notification.contentIntent).run {
            assertThat(isBroadcast).isTrue()
            assertThat(requestCode).isEqualTo(0)
            savedIntent.run {
                assertThat(action).isEqualTo(Intent.ACTION_VIEW)
                assertThat(getStringExtra("krt_event_name")).isEqualTo("message_click")
            }
        }
        Shadows.shadowOf(notification.deleteIntent).run {
            assertThat(isBroadcast).isTrue()
            assertThat(requestCode).isEqualTo(0)
            savedIntent.run {
                assertThat(getStringExtra("krt_event_name")).isEqualTo("message_ignore")
            }
        }
    }
}
