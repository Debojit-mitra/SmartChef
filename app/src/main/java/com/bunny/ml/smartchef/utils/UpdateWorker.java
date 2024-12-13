package com.bunny.ml.smartchef.utils;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bunny.ml.smartchef.MainActivity;
import com.bunny.ml.smartchef.R;

public class UpdateWorker extends Worker {
    private static final String CHANNEL_ID = "update_channel";
    private static final int NOTIFICATION_ID = 1;

    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppUpdater appUpdater = new AppUpdater(getApplicationContext());
        appUpdater.checkForUpdatesInBackground(updateAvailable -> {
            if (updateAvailable) {
                showUpdateNotification();
            }
        });
        return Result.success();
    }

    private void showUpdateNotification() {
        createNotificationChannel();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("show_update", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_update)
                .setContentTitle("Update Available")
                .setContentText("A new version of SmartChef is available")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Update Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for app updates");

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}