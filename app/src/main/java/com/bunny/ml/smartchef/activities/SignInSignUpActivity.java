package com.bunny.ml.smartchef.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bunny.ml.smartchef.MainActivity;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.SplashActivity;
import com.bunny.ml.smartchef.firebase.AuthManager;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.models.UserData;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class SignInSignUpActivity extends AppCompatActivity implements AuthManager.AuthCallback {
    private static final String TAG = "SignInSignUpActivity";
    private MaterialButton signUpButton, signInButton;
    private MaterialButton googleSignInButton;
    private boolean isGoogleSignIn = false;
    private LoadingDialog loadingDialog;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in_sign_up);

        initializeViews();
        initializeFirebaseAuth();
        setupGoogleSignInLauncher();
        setupClickListeners();
    }

    private void initializeViews() {
        signUpButton = findViewById(R.id.signUpButton);
        signInButton = findViewById(R.id.signInButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loadingDialog = new LoadingDialog(this);
    }

    private void initializeFirebaseAuth() {
        authManager = new AuthManager(this, getString(R.string.default_web_client_id));
        authManager.setAuthCallback(this);
    }

    private void setupGoogleSignInLauncher() {
        ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            authManager.handleGoogleSignInResult(result.getData());
                        } else {
                            hideLoadingDialog();
                            Toast.makeText(SignInSignUpActivity.this,
                                    "Google sign in cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Set the launcher in AuthManager
        authManager.setGoogleSignInLauncher(googleSignInLauncher);
    }

    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignInSignUpActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });

        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignInSignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        googleSignInButton.setOnClickListener(v -> {
            isGoogleSignIn = true;
            showLoadingDialog("Signing in with Google");
            authManager.startGoogleSignIn();
        });
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        hideLoadingDialog();
        if (isGoogleSignIn) {
            // Check if the user needs to complete profile
            checkUserProfile(user);
        }
    }

    private void checkUserProfile(FirebaseUser user) {
        authManager.checkUserProfileCompletion(user, new AuthManager.ProfileCheckCallback() {
            @Override
            public void onProfileComplete() {
                // Initialize cache and navigate
                navigateToMain();
            }

            @Override
            public void onProfileIncomplete(boolean hasBasicProfile) {
                hideLoadingDialog();
                // User needs to complete their profile
                navigateToRegistration(hasBasicProfile ? 4 : 3);
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(SignInSignUpActivity.this,
                        "Error checking profile: " + error, Toast.LENGTH_SHORT).show();
                navigateToRegistration(3); // Safe default
            }
        });
    }

    private void navigateToMain() {
        // Initialize cache before navigation
        ProfileManager.getInstance(this).initializeCache(new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserData userData) {
                hideLoadingDialog();
                Intent intent = new Intent(SignInSignUpActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                hideLoadingDialog();
                Log.w(TAG, "Cache initialization failed: " + error);
                // Even if cache fails, proceed to MainActivity
                Intent intent = new Intent(SignInSignUpActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }


    private void navigateToRegistration(int startFragment) {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra("START_FRAGMENT", startFragment);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFailure(String error) {
        hideLoadingDialog();
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCodeSent(com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken token) {
        // Not used for Google Sign In
    }

    @Override
    public void onCodeAutoRetrievalTimeout() {
        // Not used for Google Sign In
    }

    public void showLoadingDialog(String message) {
        if (loadingDialog != null) {
            loadingDialog.show(message);
        }
    }

    public void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
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