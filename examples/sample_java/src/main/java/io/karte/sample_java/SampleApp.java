package io.karte.sample_java;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import io.karte.android.KarteApp;
import io.karte.android.core.logger.LogLevel;
import io.karte.android.core.logger.Logger;
import io.karte.android.inappmessaging.InAppMessaging;
import io.karte.android.variables.Variables;

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setLevel(LogLevel.DEBUG);
        KarteApp.setup(this);
        InAppMessaging.Config.setEnabledWebViewCache(true);
        Variables.fetch();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createChannel(final Context context) {
        NotificationChannel channel = new NotificationChannel("my_channel", "通知テストチャンネル", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("テストの説明です");
        channel.setShowBadge(true);

        // create or update the notification channel
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}
