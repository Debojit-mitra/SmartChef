package com.bunny.ml.smartchef.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bunny.ml.smartchef.MainActivity;
import com.bunny.ml.smartchef.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CookingMotivationManager {
    private static final String TAG = "CookingMotivationManager";
    private static final String CHANNEL_ID = "cooking_motivation_channel";
    private static final int NOTIFICATION_ID = 200;
    private static final String MOTIVATION_API_URL = "https://api.npoint.io/e519da2a9cf6d1e43bf2";
    private static final String WORK_NAME = "daily_motivation";

    private final Context context;
    private final OkHttpClient client;
    private final WorkManager workManager;

    public CookingMotivationManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient();
        this.workManager = WorkManager.getInstance(context);
        createNotificationChannel();
    }

    public void scheduleDailyMotivation() {
        // First check if work is already scheduled
        workManager.getWorkInfosForUniqueWork(WORK_NAME)
                .addListener(() -> {
                    // Create and schedule new work only if no existing work is found
                    PeriodicWorkRequest motivationWorkRequest =
                            new PeriodicWorkRequest.Builder(MotivationWorker.class, 24, TimeUnit.HOURS)
                                    .build();

                    workManager.enqueueUniquePeriodicWork(
                            WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if any
                            motivationWorkRequest
                    );
                }, ContextCompat.getMainExecutor(context));
    }

    public void cancelDailyMotivation() {
        workManager.cancelUniqueWork(WORK_NAME);
    }

    public boolean isMotivationScheduled() {
        try {
            boolean[] isScheduled = {false};
            workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
                    .forEach(workInfo -> {
                        if (workInfo != null && !workInfo.getState().isFinished()) {
                            isScheduled[0] = true;
                        }
                    });
            return isScheduled[0];
        } catch (Exception e) {
            Log.e(TAG, "Error checking motivation schedule: " + e.getMessage());
            return false;
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Cooking Motivation",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Daily cooking motivation notifications");

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public static class MotivationWorker extends Worker {
        public MotivationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            CookingMotivationManager manager = new CookingMotivationManager(getApplicationContext());
            manager.fetchAndShowMotivation();
            return Result.success();
        }
    }

    private void fetchAndShowMotivation() {
        Request request = new Request.Builder()
                .url(MOTIVATION_API_URL)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected response " + response);

            String responseData = "";
            if (response.body() != null) {
                responseData = response.body().string();
            }
            List<String> motivations = parseMotivations(responseData);

            if (!motivations.isEmpty()) {
                String randomMotivation = getRandomMotivation(motivations);
                showMotivationNotification(randomMotivation);
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error fetching motivation: " + e.getMessage());
            // Use fallback motivation if API fails
            showMotivationNotification("Time to create something delicious!");
        }
    }

    private List<String> parseMotivations(String jsonData) throws JSONException {
        List<String> motivations = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray motivationsArray = jsonObject.getJSONArray("motivations");

        for (int i = 0; i < motivationsArray.length(); i++) {
            motivations.add(motivationsArray.getString(i));
        }

        return motivations;
    }

    private String getRandomMotivation(List<String> motivations) {
        Random random = new Random();
        return motivations.get(random.nextInt(motivations.size()));
    }

    private void showMotivationNotification(String motivation) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chef_2)
                .setContentTitle(context.getString(R.string.cooking_motivation_for_today))
                .setContentText(motivation)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}