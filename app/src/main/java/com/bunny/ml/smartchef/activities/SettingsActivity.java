package com.bunny.ml.smartchef.activities;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.utils.CustomAlertDialog;
import com.bunny.ml.smartchef.utils.PermissionManager;
import com.bunny.ml.smartchef.utils.UpdateWorker;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private static final String THEME_PREFS = "theme_prefs";
    private static final String CURRENT_THEME = "current_theme";
    private static final String UPDATE_PREFS = "update_prefs";
    private static final String AUTO_UPDATE_ENABLED = "auto_update_enabled";
    public static final int UPDATE_CHECK_INTERVAL = 24;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private ImageButton themeAutoBtn;
    private ImageButton themeLightBtn;
    private ImageButton themeDarkBtn;
    private SharedPreferences sharedPreferencesTheme, updatePrefs;
    private TextView textview_developer;
    private MaterialSwitch autoUpdateSwitch;
    private TextView textview_no_notification_permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize SharedPreferences before calling super.onCreate()
        sharedPreferencesTheme = getSharedPreferences(THEME_PREFS, MODE_PRIVATE);
        updatePrefs = getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE);

        // Apply saved theme
        int savedTheme = sharedPreferencesTheme.getInt(CURRENT_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupThemeButtons();
        updateThemeButtonsUI();
        setupBackPressHandling();
        setupAutoUpdateSwitch();
    }

    private void initializeViews() {
        ImageView backBtn = findViewById(R.id.backBtn);
        themeAutoBtn = findViewById(R.id.theme_auto_btn);
        themeLightBtn = findViewById(R.id.theme_light_btn);
        themeDarkBtn = findViewById(R.id.theme_dark_btn);
        textview_developer = findViewById(R.id.textview_developer);
        autoUpdateSwitch = findViewById(R.id.autoUpdateSwitch);
        textview_no_notification_permission = findViewById(R.id.textview_no_notification_permission);
        // Setup click listeners
        backBtn.setOnClickListener(view -> handleBack());
        textview_developer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDeveloper();
            }
        });
    }

    private void setupAutoUpdateSwitch() {
        // Set initial state
        autoUpdateSwitch.setChecked(PermissionManager.isAutoUpdateEnabled(this));

        // Add explanation text below switch if permission is denied
        if (!PermissionManager.hasNotificationPermission(this)) {
            textview_no_notification_permission.setVisibility(View.VISIBLE);
        }

        autoUpdateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !PermissionManager.hasNotificationPermission(this)) {
                // Revert the switch state
                buttonView.setChecked(false);
                showNotificationPermissionDialog();
            } else {
                updateAutoUpdatePreference(isChecked);
            }
        });
    }

    private void showNotificationPermissionDialog() {
        new CustomAlertDialog(this)
                .setDialogTitle("Notification Permission Required")
                .setMessage("To enable auto-updates, SmartChef needs permission to send notifications. Would you like to grant this permission?")
                .setPositiveButton("Grant", this::requestNotificationPermission)
                .setNegativeButton("No Thanks", () -> {
                    autoUpdateSwitch.setChecked(false);
                    PermissionManager.setAutoUpdateEnabled(this, false);
                })
                .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    private void updateAutoUpdatePreference(boolean enabled) {
        PermissionManager.setAutoUpdateEnabled(this, enabled);
        if (enabled) {
            scheduleUpdateChecks();
        } else {
            cancelUpdateChecks();
        }
    }

    private void scheduleUpdateChecks() {
        androidx.work.PeriodicWorkRequest updateWorkRequest =
                new androidx.work.PeriodicWorkRequest.Builder(
                        UpdateWorker.class,
                        24,
                        java.util.concurrent.TimeUnit.HOURS
                )
                        .build();

        androidx.work.WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "update_check",
                        androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                        updateWorkRequest
                );
    }

    private void cancelUpdateChecks() {
        androidx.work.WorkManager.getInstance(this)
                .cancelUniqueWork("update_check");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable auto-update
                autoUpdateSwitch.setChecked(true);
                updateAutoUpdatePreference(true);
                // Remove explanation text if it exists
                if (textview_no_notification_permission.getVisibility() == View.VISIBLE) {
                    textview_no_notification_permission.setVisibility(View.GONE);
                }
            } else {
                // Permission denied
                autoUpdateSwitch.setChecked(false);
                updateAutoUpdatePreference(false);
                Toast.makeText(this, "Permission denied. Auto-updates will remain disabled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAboutDeveloper() {
        new CustomAlertDialog(SettingsActivity.this)
                .setDialogTitle("About the developer")
                .setMessage(getString(R.string.about_developer))
                .setMessageJustification(Layout.JUSTIFICATION_MODE_INTER_WORD)
                .setNegativeButton("OK", null)
                .show();
    }

    private void setupThemeButtons() {
        themeAutoBtn.setOnClickListener(v -> applyThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        themeLightBtn.setOnClickListener(v -> applyThemeMode(AppCompatDelegate.MODE_NIGHT_NO));
        themeDarkBtn.setOnClickListener(v -> applyThemeMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    private void applyThemeMode(int themeMode) {
        // Save theme preference
        SharedPreferences.Editor editor = sharedPreferencesTheme.edit();
        editor.putInt(CURRENT_THEME, themeMode);
        editor.apply();

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Update UI
        updateThemeButtonsUI();

        // Recreate the activity to apply theme changes
        recreate();
    }

    private void updateThemeButtonsUI() {
        int currentTheme = sharedPreferencesTheme.getInt(CURRENT_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Reset all buttons to default alpha
        themeAutoBtn.setAlpha(0.5f);
        themeLightBtn.setAlpha(0.5f);
        themeDarkBtn.setAlpha(0.5f);

        // Highlight selected theme button
        switch (currentTheme) {
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                themeAutoBtn.setAlpha(1.0f);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                themeLightBtn.setAlpha(1.0f);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                themeDarkBtn.setAlpha(1.0f);
                break;
        }
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void handleBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateThemeButtonsUI();
    }
}