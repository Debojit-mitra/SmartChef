package com.bunny.ml.smartchef;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bunny.ml.smartchef.activities.RegistrationActivity;
import com.bunny.ml.smartchef.activities.SignInSignUpActivity;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.models.UserData;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 500;
    private LoadingDialog loadingDialog;
    private boolean isCheckingStatus = false;
    private ProfileManager profileManager;
    private static final String THEME_PREFS = "theme_prefs";
    private static final String CURRENT_THEME = "current_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize these flags before any potential recreation
        loadingDialog = new LoadingDialog(this);
        profileManager = ProfileManager.getInstance(this);
        isCheckingStatus = false;

        // Set theme before super.onCreate()
        int savedTheme = getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
                .getInt(CURRENT_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        // Force the appropriate theme based on the saved setting
        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            setTheme(R.style.Theme_SmartChef_SplashScreen);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.Theme_SmartChef_SplashScreen);
        }


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);


        // Re-initialize dialog in case of recreation
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if activity and dialog are still valid
            if (!isFinishing() && !isCheckingStatus && loadingDialog != null) {
                loadingDialog.show("Loading");
                isCheckingStatus = true;
                checkUserStatus();
            }
        }, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            loadingDialog.dismiss();
            navigateToSignIn();
            return;
        }

        // First check if we have fresh cached data
        if (profileManager.isCacheFresh()) {
            UserData cachedData = profileManager.getCachedUserData();
            if (cachedData != null) {
                loadingDialog.dismiss();
                determineNavigation(cachedData);
                return;
            }
        }

        // If no cache, load from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isFinishing()) {
                        if (documentSnapshot.exists()) {
                            // Initialize cache with the fetched data
                            profileManager.initializeCache(new ProfileManager.ProfileCallback() {
                                @Override
                                public void onSuccess(UserData userData) {
                                    loadingDialog.dismiss();
                                    determineNavigation(userData);
                                }

                                @Override
                                public void onFailure(String error) {
                                    // Even if cache init fails, proceed with navigation
                                    loadingDialog.dismiss();
                                    String name = documentSnapshot.getString("name");
                                    String dateOfBirth = documentSnapshot.getString("dateOfBirth");
                                    String gender = documentSnapshot.getString("gender");

                                    if (isBasicProfileComplete(name, dateOfBirth, gender)) {
                                        navigateToMain();
                                    } else {
                                        navigateToRegistration(3);
                                    }
                                }
                            });
                        } else {
                            loadingDialog.dismiss();
                            navigateToRegistration(3);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        loadingDialog.dismiss();
                        Log.e(TAG, "Error checking user profile", e);
                        navigateToRegistration(3);
                    }
                });
    }

    private void determineNavigation(UserData userData) {
        if (userData.hasBasicProfile()) {
            navigateToMain();
        } else {
            navigateToRegistration(3);
        }
    }

    private boolean isBasicProfileComplete(String name, String dateOfBirth, String gender) {
        return name != null && !name.isEmpty() &&
                dateOfBirth != null && !dateOfBirth.isEmpty() &&
                gender != null && !gender.isEmpty();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInSignUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegistration(int startFragment) {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra("START_FRAGMENT", startFragment);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
}