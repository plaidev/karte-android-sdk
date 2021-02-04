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

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.integration.DryRunTestCase
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.Notifications
import io.karte.android.notifications.internal.wrapper.KEY_PUSH_NOTIFICATION_FLAG
import org.junit.Test

class DryRunTest : DryRunTestCase() {
    @Test
    fun testNotifications() {
        Notifications.registerFCMToken("test")

        assertDryRun()
    }

    /** MessageHandlerは単体では通常通り動作する */
    @Test
    fun testMessageHandler() {
        val remoteMessage = RemoteMessage.Builder("dummyToken")
            .addData(KEY_PUSH_NOTIFICATION_FLAG, "true")
            .addData("krt_attributes", "{}")
            .build()
        assertThat(MessageHandler.canHandleMessage(remoteMessage)).isTrue()

        val attributes = MessageHandler.extractKarteAttributes(remoteMessage)
        assertThat(attributes).isNotNull()

        assertThat(MessageHandler.handleMessage(application, remoteMessage)).isTrue()

        // copyInfoToIntentは反応しない.
        val intent = Intent()
        @Suppress("DEPRECATION")
        MessageHandler.copyInfoToIntent(remoteMessage.data, intent)
        assertThat(intent.getStringExtra(KEY_PUSH_NOTIFICATION_FLAG)).isNull()
    }
}
