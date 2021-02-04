package io.karte.sample_kotlin

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.karte.android.KarteApp
import io.karte.android.notifications.MessageHandler
import io.karte.android.notifications.registerFCMToken

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val handled = MessageHandler.handleMessage(this, remoteMessage)
        if (!handled) {
            val data = remoteMessage.data
            val title = data["subject"]
            val body = data["text"]
            sendNotification(title, body, data.get("android_channel_id"))
        }
    }

    private fun sendNotification(
        title: String?,
        body: String?,
        channel: String?
    ) {

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, "my_channel")
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        channel?.let { notificationBuilder.setChannelId(it) }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                ?: return

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        KarteApp.registerFCMToken(token)
    }
}
