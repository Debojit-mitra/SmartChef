package com.bunny.ml.smartchef.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bunny.ml.smartchef.MainActivity;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.firebase.AuthManager;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.fragments.LoginFragment1;
import com.bunny.ml.smartchef.fragments.LoginFragment2;
import com.bunny.ml.smartchef.models.UserData;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.bunny.ml.smartchef.utils.SharedData;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class LoginActivity extends AppCompatActivity implements AuthManager.AuthCallback {
    private static final String TAG = "LoginActivity";
    private static MaterialButton loginButton;
    private ImageView backBtn;
    private MaterialButton googleSignInButton;
    private LoginFragment1 loginFragment1;
    private LoginFragment2 loginFragment2;
    private TextView orContinueTextview;
    private AuthManager authManager;
    private LoadingDialog loadingDialog;
    private ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher;

    private static final String KEY_CURRENT_FRAGMENT = "current_fragment";
    private static final int FRAG_1 = 1;
    private static final int FRAG_2 = 2;
    private int currentFragment = FRAG_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initializeViews();
        initializeLoadingDialog();
        setupGoogleSignInLauncher();
        initializeFirebaseAuth();
        setupClickListeners();
        initializeFragments(savedInstanceState);

        if (savedInstanceState == null) {
            setupInitialFragment(FRAG_1);
        } else {
            currentFragment = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, FRAG_1);
            setupInitialFragment(currentFragment);
        }
        setupBackPressHandling();
    }

    private void initializeViews() {
        loginButton = findViewById(R.id.sendOtpBtn);
        backBtn = findViewById(R.id.backBtn);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        orContinueTextview = findViewById(R.id.orContinueTextview);
    }

    private void initializeFragments(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            loginFragment1 = (LoginFragment1) getSupportFragmentManager().findFragmentByTag("FRAG1");
            loginFragment2 = (LoginFragment2) getSupportFragmentManager().findFragmentByTag("FRAG2");
        }

        if (loginFragment1 == null) loginFragment1 = new LoginFragment1();
        if (loginFragment2 == null) loginFragment2 = new LoginFragment2();
    }

    private void setupInitialFragment(int startFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        currentFragment = startFragment;
        Fragment fragmentToShow = startFragment == FRAG_2 ? loginFragment2 : loginFragment1;
        String fragmentTag = startFragment == FRAG_2 ? "FRAG2" : "FRAG1";

        loginButton.setVisibility(View.VISIBLE);
        loginButton.setText(startFragment == FRAG_2 ?
                getString(R.string.verify_otp) : getString(R.string.login));
        disableButton();

        transaction.replace(R.id.main_login_frame, fragmentToShow, fragmentTag);
        transaction.commit();
    }

    private void initializeLoadingDialog() {
        loadingDialog = new LoadingDialog(this);
    }

    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                this::handleGoogleSignInResult
        );
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                authManager.handleGoogleSignInResult(data);
            }
        } else {
            hideLoadingDialog();
            Toast.makeText(this, "Google sign in cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFirebaseAuth() {
        authManager = new AuthManager(this, getString(R.string.default_web_client_id));
        authManager.setAuthCallback(this);
        authManager.setGoogleSignInLauncher(googleSignInLauncher);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleLoginClick());
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
        backBtn.setOnClickListener(v -> handleBack());
    }

    public void handleLoginClick() {
        if (currentFragment == FRAG_1) {
            String phoneNumber = SharedData.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                showLoadingDialog("Sending OTP");
                authManager.startPhoneNumberVerification(phoneNumber);
            }
        } else if (currentFragment == FRAG_2) {
            String otp = ((LoginFragment2) loginFragment2).getEnteredOTP();
            if (otp != null && otp.length() == 6) {
                showLoadingDialog("Verifying OTP");
                authManager.verifyPhoneNumberWithCode(otp);
            }
        }
    }

    private void handleGoogleSignIn() {
        showLoadingDialog("Signing in with Google");
        authManager.startGoogleSignIn();
    }

    private void handleBack() {
        if (currentFragment > FRAG_1) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

            transaction.replace(R.id.main_login_frame, loginFragment1, "FRAG1");
            transaction.commit();

            currentFragment = FRAG_1;
            loginButton.setText(getString(R.string.login));
            disableButton();
            return;
        }
        finish();
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
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

    private void enableButton() {
        loginButton.setEnabled(true);
        loginButton.setBackgroundColor(getColor(R.color.mode_inverse));
    }

    private void disableButton() {
        loginButton.setEnabled(false);
        loginButton.setBackgroundColor(getColor(R.color.mode_inverse_extra));
    }

    private void navigateToMain() {
        ProfileManager.getInstance(this).initializeCache(new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserData userData) {
                // Navigate to MainActivity after cache is initialized
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                // Even if cache fails, proceed to MainActivity
                // The cache will be initialized later
                Log.w(TAG, "Cache initialization failed: " + error);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    public static void buttonEnable(android.content.Context context) {
        loginButton.setEnabled(true);
        loginButton.setBackgroundColor(context.getColor(R.color.mode_inverse));
    }

    public static void buttonDisabled(android.content.Context context) {
        loginButton.setEnabled(false);
        loginButton.setBackgroundColor(context.getColor(R.color.mode_inverse_extra));
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        authManager.checkUserRegistrationStatus(user, new AuthManager.RegistrationStatusCallback() {
            @Override
            public void onAlreadyRegistered() {
                hideLoadingDialog();
                navigateToMain(); // Modified to include cache initialization
            }

            @Override
            public void onNotRegistered() {
                hideLoadingDialog();
                // Sign out the user since they're not registered
                authManager.signOut();
                Toast.makeText(LoginActivity.this,
                        "Account not found. Please register first.",
                        Toast.LENGTH_LONG).show();

                // Navigate to registration
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onIncompleteRegistration(boolean hasBasicProfile) {
                hideLoadingDialog();
                // Navigate to registration completion
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                intent.putExtra("START_FRAGMENT", hasBasicProfile ? 4 : 3);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                hideLoadingDialog();
                Toast.makeText(LoginActivity.this,
                        "Error checking registration: " + error,
                        Toast.LENGTH_SHORT).show();
                authManager.signOut();
            }
        });
    }

    @Override
    public void onFailure(String error) {
        hideLoadingDialog();
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        Log.e("LoginActivity", error);
        if (currentFragment == FRAG_2) {
            ((LoginFragment2) loginFragment2).handleVerificationFailed();
        }
    }

    @Override
    public void onCodeSent(PhoneAuthProvider.ForceResendingToken token) {
        hideLoadingDialog();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);

        transaction.replace(R.id.main_login_frame, loginFragment2, "FRAG2");
        transaction.commit();

        currentFragment = FRAG_2;
        loginButton.setText(getString(R.string.verify_otp));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (loginFragment2 != null) {
                loginFragment2.clearOTP();
                loginFragment2.requestOTPViewFocus();
                loginFragment2.startOtpTimer();
            }
        }, 100);
    }

    @Override
    public void onCodeAutoRetrievalTimeout() {
        hideLoadingDialog();
        Toast.makeText(this, "OTP code timeout. Please request new code.", Toast.LENGTH_LONG).show();
    }

    public AuthManager getAuthManager() {
        return authManager;
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