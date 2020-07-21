package io.karte.sample_kotlin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.karte.android.KarteApp
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.variables.Variables

private const val APP_KEY = "SET_YOUR_APP_KEY"

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        KarteApp.setup(this, APP_KEY)
        Variables.fetch()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(this)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            "my_channel",
            "通知テストチャンネル",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "テストの説明です"
        channel.setShowBadge(true)

        // create or update the notification channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
