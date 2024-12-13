package com.bunny.ml.smartchef.utils;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

public class PermissionManager {
    private static final String PERMISSION_PREFS  = "permission_prefs";
    private static final String AUTO_UPDATE_ENABLED = "auto_update_enabled";
    private static final String NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked";
    private static final String NOTIFICATION_PERMISSION_DENIED = "notification_permission_denied";

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void setAutoUpdateEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS , Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AUTO_UPDATE_ENABLED, enabled).apply();
    }

    public static boolean isAutoUpdateEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS , Context.MODE_PRIVATE);
        return prefs.getBoolean(AUTO_UPDATE_ENABLED, true);
    }

    public static boolean wasNotificationPermissionAsked(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(NOTIFICATION_PERMISSION_ASKED, false);
    }

    public static void setNotificationPermissionAsked(Context context, boolean asked) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(NOTIFICATION_PERMISSION_ASKED, asked).apply();
    }

    public static boolean wasNotificationPermissionDenied(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(NOTIFICATION_PERMISSION_DENIED, false);
    }

    public static void setNotificationPermissionDenied(Context context, boolean denied) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(NOTIFICATION_PERMISSION_DENIED, denied).apply();
    }

    public static void resetNotificationPermissionState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(NOTIFICATION_PERMISSION_ASKED, false)
                .putBoolean(NOTIFICATION_PERMISSION_DENIED, false)
                .apply();
    }

    public static boolean shouldAskNotificationPermission(Context context) {
        // If permission is already granted, no need to ask
        if (hasNotificationPermission(context)) {
            return false;
        }

        // If user has explicitly denied and we've recorded that, don't ask
        if (wasNotificationPermissionDenied(context)) {
            return false;
        }

        // If we haven't asked before, or if permission was previously granted but now revoked
        // (indicating user removed it from settings), we should ask
        return !wasNotificationPermissionAsked(context) ||
                (wasNotificationPermissionAsked(context) && wasPermissionPreviouslyGranted(context));
    }

    private static boolean wasPermissionPreviouslyGranted(Context context) {
        return wasNotificationPermissionAsked(context) && !wasNotificationPermissionDenied(context);
    }
}

