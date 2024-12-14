package com.bunny.ml.smartchef.utils;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bunny.ml.smartchef.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppUpdater {
    private static final String TAG = "AppUpdater";
    public static final int REQUEST_INSTALL_PACKAGES = 1001;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Debojit-mitra/SmartChef/releases/latest";
    public static final String INSTALL_ACTION = "com.bunny.ml.smartchef.INSTALL_COMPLETE";
    private static final String PREFS_NAME = "AppUpdaterPrefs";
    private static final String PREF_DOWNLOADED_VERSION = "downloadedVersion";
    private static final String PREF_LAST_UPDATE_CHECK = "lastUpdateCheck";
    private final Context context;
    private final OkHttpClient client;
    public MaterialAlertDialogBuilder progressDialogBuilder;
    private androidx.appcompat.app.AlertDialog progressDialog;
    private String pendingNewVersion;
    private String pendingDownloadUrl;

    public interface UpdateCheckCallback {
        void onUpdateAvailable(boolean available);
    }


    public AppUpdater(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void checkForUpdates(boolean showNoUpdateDialog) {
        if (!isReleaseBuild()) {
            Log.d(TAG, "Skipping update check for non-release build");
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(GITHUB_API_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String latestVersion = jsonResponse.getString("tag_name");
                        String currentVersion = getCurrentVersion();
                        String releaseNotes = jsonResponse.getString("body");

                        // Extract hash from release notes or assets
                        String expectedHash = extractHashFromRelease(jsonResponse);

                        if (isUpdateAvailable(currentVersion, latestVersion)) {
                            String downloadUrl = jsonResponse.getJSONArray("assets")
                                    .getJSONObject(0)
                                    .getString("browser_download_url");

                            if (isUpdateAlreadyDownloaded(latestVersion)) {
                                // Verify hash of existing download
                                File updateFile = new File(context.getExternalCacheDir(), "update.apk");
                                if (verifyFileHash(updateFile, expectedHash)) {
                                    notifyUpdateReady(latestVersion, releaseNotes);
                                } else {
                                    // Hash verification failed, re-download
                                    updateFile.delete();
                                    notifyUpdate(latestVersion, downloadUrl, releaseNotes);
                                }
                            } else {
                                notifyUpdate(latestVersion, downloadUrl, releaseNotes);
                            }
                        } else if (showNoUpdateDialog) {
                            Log.d(TAG, "No update available");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
        }).start();
    }

    public void checkForUpdatesInBackground(UpdateCheckCallback callback) {
        if (!isReleaseBuild()) {
            callback.onUpdateAvailable(false);
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(GITHUB_API_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String latestVersion = jsonResponse.getString("tag_name");
                        String currentVersion = getCurrentVersion();

                        boolean updateAvailable = isUpdateAvailable(currentVersion, latestVersion);
                        context.getMainExecutor().execute(() -> callback.onUpdateAvailable(updateAvailable));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
                context.getMainExecutor().execute(() -> callback.onUpdateAvailable(false));
            }
        }).start();
    }

    // Add this method to handle the update check from notification
    public void checkForUpdatesFromNotification() {
        checkForUpdates(true);
    }


    private boolean isUpdateAlreadyDownloaded(String latestVersion) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String downloadedVersion = prefs.getString(PREF_DOWNLOADED_VERSION, "");
        File updateFile = new File(context.getExternalCacheDir(), "update.apk");
        return latestVersion.equals(downloadedVersion) && updateFile.exists();
    }

    private void notifyUpdateReady(String newVersion, String releaseNotes) {
        context.getMainExecutor().execute(() -> showUpdateReadyDialog(newVersion, releaseNotes));
    }

    private void showUpdateReadyDialog(String newVersion, String releaseNotes) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Update Ready")
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                .setMessage("A new version (" + newVersion + ") is ready to install.\n\nRelease Notes:\n" + releaseNotes)
                .setCancelable(false)
                .setPositiveButton("Install", (dialog, which) -> installExistingUpdate())
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void installExistingUpdate() {
        File updateFile = new File(context.getExternalCacheDir(), "update.apk");
        if (updateFile.exists()) {
            installUpdate(updateFile);
        } else {
            Toast.makeText(context, "Update file not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isReleaseBuild() {
        return !com.bunny.ml.smartchef.BuildConfig.DEBUG;
    }

    private String getCurrentVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting current version", e);
            return "";
        }
    }

    private boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        // Remove any leading 'v' if present
        currentVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;
        latestVersion = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;

        // Split versions into components
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        // Compare each component
        for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
            int currentPart = Integer.parseInt(currentParts[i]);
            int latestPart = Integer.parseInt(latestParts[i]);
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        // If all components are equal, check if latest has more components
        return latestParts.length > currentParts.length;
    }

    private void notifyUpdate(String newVersion, String downloadUrl, String releaseNotes) {
        if (context instanceof Activity) {
            context.getMainExecutor().execute(() -> showUpdateDialog(newVersion, downloadUrl, releaseNotes));
        }
    }

    private void showUpdateDialog(String newVersion, String downloadUrl, String releaseNotes) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Update Available")
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                .setMessage("A new version (" + newVersion + ") is available.\n\nRelease Notes:\n" + releaseNotes)
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> handleUpdateClick(newVersion, downloadUrl))
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void handleUpdateClick(String newVersion, String downloadUrl) {
        this.pendingNewVersion = newVersion;
        this.pendingDownloadUrl = downloadUrl;
        if (!context.getPackageManager().canRequestPackageInstalls()) {
            showInstallPermissionDialog(newVersion, downloadUrl);
        } else {
            showDownloadConfirmationDialog(newVersion, downloadUrl);
        }
    }

    private void showInstallPermissionDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Permission Required")
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                .setMessage("To install updates, this app needs permission to install unknown apps. Would you like to grant this permission?")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + context.getPackageName()));
                    if (context instanceof Activity) {
                        ((Activity) context).startActivityForResult(intent, REQUEST_INSTALL_PACKAGES);
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                    storeDownloadInfo(newVersion, downloadUrl);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showInstallPermissionDialog(){
        showInstallPermissionDialog(pendingNewVersion, pendingDownloadUrl);
    }

    private void storeDownloadInfo(String newVersion, String downloadUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("pendingNewVersion", newVersion)
                .putString("pendingDownloadUrl", downloadUrl)
                .apply();
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (context.getPackageManager().canRequestPackageInstalls()) {
                // Permission granted, show download confirmation
                if (pendingNewVersion != null && pendingDownloadUrl != null) {
                    showDownloadConfirmationDialog(pendingNewVersion, pendingDownloadUrl);
                }
            } else {
                // Permission still not granted, show permission dialog again
                showInstallPermissionDialog();
            }
        }
    }


    public void checkAndDownloadPendingUpdate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String pendingNewVersion = prefs.getString("pendingNewVersion", null);
        String pendingDownloadUrl = prefs.getString("pendingDownloadUrl", null);
        if (pendingNewVersion != null && pendingDownloadUrl != null) {
            prefs.edit().remove("pendingNewVersion").remove("pendingDownloadUrl").apply();
            downloadAndInstallUpdate(pendingNewVersion, pendingDownloadUrl);
        }
    }


    private void showDownloadConfirmationDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Download Update")
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                .setMessage("Are you ready to download the update?")
                .setCancelable(false)
                .setPositiveButton("Download", (dialog, which) -> downloadAndInstallUpdate(newVersion, downloadUrl))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void downloadAndInstallUpdate(String newVersion, String downloadUrl) {
        progressDialogBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle("Downloading Update")
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                .setView(R.layout.progress_dialog_layout)
                .setCancelable(false);
        progressDialog = progressDialogBuilder.create();
        progressDialog.show();

        new Thread(() -> {
            try {
                // First, get the expected hash
                Request hashRequest = new Request.Builder().url(GITHUB_API_URL).build();
                String expectedHash = null;
                try (Response response = client.newCall(hashRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject release = new JSONObject(response.body().string());
                        expectedHash = extractHashFromRelease(release);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // Download and verify the file
                Log.d(TAG, "Downloading update from: " + downloadUrl);
                Request downloadRequest = new Request.Builder().url(downloadUrl).build();
                try (Response response = client.newCall(downloadRequest).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Failed to download update: " + response);
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new IOException("Empty response body");
                    }

                    File file = new File(context.getExternalCacheDir(), "update.apk");
                    boolean downloadSuccess = downloadWithProgress(body, file);

                    if (downloadSuccess) {
                        // Verify the hash
                        if (expectedHash == null || verifyFileHash(file, expectedHash)) {
                            Log.d(TAG, "Download completed and verified. Prompting user to install.");
                            if (newVersion != null) {
                                storeDownloadedVersion(newVersion);
                            }
                            promptUserToInstall(file);
                        } else {
                            throw new IOException("Hash verification failed");
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error downloading update", e);
                dismissProgressDialog();
                context.getMainExecutor().execute(() ->
                        Toast.makeText(context, "Update download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean downloadWithProgress(ResponseBody body, File file) throws IOException {
        long totalBytes = body.contentLength();
        try (InputStream in = body.byteStream();
             OutputStream out = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            long downloadedBytes = 0;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                updateProgressDialog(downloadedBytes, totalBytes);
            }
            return true;
        }
    }

    private void storeDownloadedVersion(String version) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_DOWNLOADED_VERSION, version).apply();
    }

    private void updateProgressDialog(long downloadedBytes, long totalBytes) {
        int progress = (int) ((downloadedBytes * 100) / totalBytes);
        context.getMainExecutor().execute(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                ProgressBar progressBar = progressDialog.findViewById(R.id.progressBar);
                TextView progressText = progressDialog.findViewById(R.id.progressText);
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                }
                if (progressText != null) {
                    String progressTxt = progress + "%";
                    progressText.setText(progressTxt);
                }
            }
        });
    }

    private void dismissProgressDialog() {
        context.getMainExecutor().execute(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }

    private void promptUserToInstall(File file) {
        context.getMainExecutor().execute(() -> {
            dismissProgressDialog();
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Update Downloaded")
                    .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_button_background_dark))
                    .setMessage("Would you like to install the update now?")
                    .setCancelable(false)
                    .setPositiveButton("Install", (dialog, which) -> {
                        dialog.dismiss();
                        installUpdate(file);
                    })
                    .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void installUpdate(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (INSTALL_ACTION.equals(intent.getAction())) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                switch (status) {
                    case PackageInstaller.STATUS_SUCCESS:
                        Toast.makeText(context, "Update installed successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case PackageInstaller.STATUS_FAILURE:
                    case PackageInstaller.STATUS_FAILURE_ABORTED:
                    case PackageInstaller.STATUS_FAILURE_BLOCKED:
                    case PackageInstaller.STATUS_FAILURE_CONFLICT:
                    case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                    case PackageInstaller.STATUS_FAILURE_INVALID:
                    case PackageInstaller.STATUS_FAILURE_STORAGE:
                        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                        Toast.makeText(context, "Update failed: " + message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Update failed with message: " + message);
                        break;
                    default:
                        Toast.makeText(context, "Update failed with unknown error", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Update failed with unknown status: " + status);
                        break;
                }
            }
        }
    }

    //hash verification
    private String extractHashFromRelease(JSONObject release) throws Exception {
        String body = release.getString("body");
        // Example format: SHA-256: <hash>
        String[] lines = body.split("\n");
        for (String line : lines) {
            if (line.startsWith("SHA-256:")) {
                return line.substring(8).trim();
            }
        }
        return null;
    }

    private boolean verifyFileHash(File file, String expectedHash) {
        if (expectedHash == null) return true; // Skip verification if hash not provided

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            String calculatedHash = bytesToHex(hashBytes);
            return calculatedHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying file hash", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

}