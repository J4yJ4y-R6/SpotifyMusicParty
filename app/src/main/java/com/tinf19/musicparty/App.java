package com.tinf19.musicparty;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        Log.d(App.class.getName(), "App created a notification channel");
        NotificationChannel serviceChannel = new NotificationChannel(
                Constants.CHANNEL_ID,
                getString(R.string.service_channelName),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }
}
