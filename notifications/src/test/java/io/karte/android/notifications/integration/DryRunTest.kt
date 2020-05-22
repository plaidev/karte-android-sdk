package io.karte.android.notifications.integration

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.integration.DryRunTestCase
import io.karte.android.notifications.KARTE_PUSH_NOTIFICATION_FLAG
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.Notifications
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
            .addData(KARTE_PUSH_NOTIFICATION_FLAG, "true")
            .addData("krt_attributes", "{}")
            .build()
        assertThat(MessageHandler.canHandleMessage(remoteMessage)).isTrue()

        val attributes = MessageHandler.extractKarteAttributes(remoteMessage)
        assertThat(attributes).isNotNull()

        assertThat(MessageHandler.handleMessage(application, remoteMessage)).isTrue()

        val intent = Intent()
        MessageHandler.copyInfoToIntent(remoteMessage.data, intent)
        assertThat(intent.getStringExtra(KARTE_PUSH_NOTIFICATION_FLAG)).isEqualTo("true")
    }
}
