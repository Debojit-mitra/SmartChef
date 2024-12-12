package com.bunny.ml.smartchef.activities;

import android.content.Context;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.firebase.AuthManager;
import com.bunny.ml.smartchef.fragments.RegisterFragment1;
import com.bunny.ml.smartchef.fragments.RegisterFragment2;
import com.bunny.ml.smartchef.fragments.RegisterFragment3;
import com.bunny.ml.smartchef.fragments.RegisterFragment4;
import com.bunny.ml.smartchef.utils.Extras;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.bunny.ml.smartchef.utils.SharedData;
import com.bunny.ml.smartchef.utils.SmsRetrieverHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class RegistrationActivity extends AppCompatActivity implements RegisterFragment2.goBackListener, AuthManager.AuthCallback {
    private static MaterialButton sendOtpBtn;
    private ImageView backBtn;
    private MaterialButton googleSignInButton;
    private RegisterFragment1 registerFragment1;
    private RegisterFragment2 registerFragment2;
    private RegisterFragment3 registerFragment3;
    private RegisterFragment4 registerFragment4;
    private TextView orContinueTextview;
    private AuthManager authManager;
    private boolean isGoogleSignIn = false;
    private LoadingDialog loadingDialog;
    public SmsRetrieverHelper smsRetrieverHelper;
    private ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher;

    private static final String KEY_CURRENT_FRAGMENT = "current_fragment";
    private static final int FRAG_1 = 1;
    private static final int FRAG_2 = 2;
    private static final int FRAG_3 = 3;
    private static final int FRAG_4 = 4;
    private int currentFragment = FRAG_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        initializeViews();
        initializeLoadingDialog();
        setupGoogleSignInLauncher();
        initializeFirebaseAuth();
        setupClickListeners();
        initializeFragments(savedInstanceState);
        // Get starting fragment from intent, default to FRAG_1 if not specified
        int startFragment = getIntent().getIntExtra("START_FRAGMENT", FRAG_1);
        if (savedInstanceState == null) {

            setupInitialFragment(startFragment);
        } else {
            // Restore saved state but maintain the intended start fragment
            currentFragment = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, startFragment);
            setupInitialFragment(currentFragment);
        }
        setupBackPressHandling();
    }

    private void initializeViews() {
        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        backBtn = findViewById(R.id.backBtn);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        orContinueTextview = findViewById(R.id.orContinueTextview);
        smsRetrieverHelper = new SmsRetrieverHelper(this);
    }

    private void initializeFragments(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentFragment = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, FRAG_1);
            registerFragment1 = (RegisterFragment1) getSupportFragmentManager().findFragmentByTag("FRAG1");
            registerFragment2 = (RegisterFragment2) getSupportFragmentManager().findFragmentByTag("FRAG2");
            registerFragment3 = (RegisterFragment3) getSupportFragmentManager().findFragmentByTag("FRAG3");
            registerFragment4 = (RegisterFragment4) getSupportFragmentManager().findFragmentByTag("FRAG4");
        }

        if (registerFragment1 == null) registerFragment1 = new RegisterFragment1();
        if (registerFragment2 == null) registerFragment2 = new RegisterFragment2();
        if (registerFragment3 == null) registerFragment3 = new RegisterFragment3();
        if (registerFragment4 == null) registerFragment4 = new RegisterFragment4();
    }

    private void setupInitialFragment(int startFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Hide the Google sign-in and verification options for registration completion
        if (startFragment > FRAG_2) {
            Extras extras = new Extras();
            extras.hideWithFadeAnimation(orContinueTextview);
            extras.hideWithFadeAnimation(googleSignInButton);
        }

        // Set the current fragment
        currentFragment = startFragment;

        // Show the appropriate fragment and set button text
        Fragment fragmentToShow;
        String fragmentTag;

        switch (startFragment) {
            case FRAG_3:
                fragmentToShow = registerFragment3;
                fragmentTag = "FRAG3";
                sendOtpBtn.setVisibility(View.VISIBLE);
                enableButton();
                sendOtpBtn.setText(getString(R.string.continue_));
                break;

            case FRAG_4:
                fragmentToShow = registerFragment4;
                fragmentTag = "FRAG4";
                sendOtpBtn.setVisibility(View.VISIBLE);
                enableButton();
                sendOtpBtn.setText(getString(R.string.complete_));
                break;

            case FRAG_2:
                fragmentToShow = registerFragment2;
                fragmentTag = "FRAG2";
                sendOtpBtn.setVisibility(View.VISIBLE);
                disableButton();
                sendOtpBtn.setText(getString(R.string.verify_otp));
                break;

            default:
                fragmentToShow = registerFragment1;
                fragmentTag = "FRAG1";
                sendOtpBtn.setVisibility(View.VISIBLE);
                disableButton();
                sendOtpBtn.setText(getString(R.string.send_otp));
                break;
        }

        // Handle the visibility of other UI elements based on fragment
        if (startFragment <= FRAG_2) {
            orContinueTextview.setVisibility(View.VISIBLE);
            googleSignInButton.setVisibility(View.VISIBLE);
        } else {
            orContinueTextview.setVisibility(View.GONE);
            googleSignInButton.setVisibility(View.GONE);
        }

        transaction.replace(R.id.main_registration_frame, fragmentToShow, fragmentTag);
        transaction.commit();
    }

    private void initializeLoadingDialog() {
        loadingDialog = new LoadingDialog(this);
    }

    private void initializeFirebaseAuth() {
        authManager = new AuthManager(this, getString(R.string.default_web_client_id));
        authManager.setAuthCallback(this);
        authManager.setGoogleSignInLauncher(googleSignInLauncher);
    }

    private void enableButton() {
        sendOtpBtn.setEnabled(true);
        sendOtpBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.mode_inverse));
    }

    private void disableButton() {
        sendOtpBtn.setEnabled(false);
        sendOtpBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.mode_inverse_extra));
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
            isGoogleSignIn = false;
            Toast.makeText(this, "Google sign in cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGoogleSignIn() {
        isGoogleSignIn = true;
        showLoadingDialog("Signing in with Google");
        authManager.startGoogleSignIn();
    }


    public AuthManager getAuthManager() {
        return authManager;
    }

    private void setupClickListeners() {
        sendOtpBtn.setOnClickListener(view -> handleSendOtpClick());
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
        backBtn.setOnClickListener(v -> handleBack());
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
        if (currentFragment >= FRAG_3) {
            if (currentFragment == FRAG_4) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

                transaction.hide(registerFragment4);
                transaction.show(registerFragment3);
                transaction.commit();

                currentFragment = FRAG_3;
                updateButtonForFragment(FRAG_3);
            } else {
                Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (currentFragment > FRAG_1) {
            onGoBack();
            return;
        }

        finish();
    }

    private void handleSendOtpClick() {
        if (currentFragment == FRAG_1) {
            String phoneNumber = SharedData.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                showLoadingDialog("Sending OTP");

                // First clean up any existing SMS retriever
                if (smsRetrieverHelper != null) {
                    smsRetrieverHelper.unregisterReceiver();
                }

                // Initialize and start SMS retriever with a delay to ensure it's ready
                initializeAndStartSmsRetriever(() -> {
                    // Only start phone verification after SMS retriever is ready
                    authManager.startPhoneNumberVerification(phoneNumber);
                });
            }
        } else if (currentFragment == FRAG_2) {
            if (authManager.isVerificationInProgress()) {
                Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show();
                onGoBack();
                return;
            }
            String otp = registerFragment2.getEnteredOTP();
            if (otp != null && otp.length() == 6) {
                showLoadingDialog("Verifying OTP");
                authManager.verifyPhoneNumberWithCode(otp);
            }
        } else if (currentFragment == FRAG_3) {
            if (registerFragment3 != null && registerFragment3.validateData()) {
                showNextFragment();
            } else {

                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            }
        } else if (currentFragment == FRAG_4) {
            if (registerFragment4 != null) {
                registerFragment4.completeRegistration();
            }
        }
    }

    public void initializeAndStartSmsRetriever(Runnable onReady) {
        // Create new SMS retriever instance
        smsRetrieverHelper = new SmsRetrieverHelper(this);

        // Start SMS retriever with callback
        smsRetrieverHelper.startSmsRetriever(new SmsRetrieverHelper.SmsRetrieverCallback() {
            @Override
            public void onSuccess(String otp) {
                runOnUiThread(() -> {
                    if (registerFragment2 != null && registerFragment2.isVisible()) {
                        registerFragment2.setOTP(otp);
                        // Wait a brief moment before triggering verification
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            registerFragment2.handleOTPComplete(otp);
                        }, 500);
                    }
                });
            }

            @Override
            public void onFailure() {
                Log.d("SMS_RETRIEVER", "Failed to retrieve SMS");
            }
        });

        // Give SMS retriever a moment to initialize before proceeding
        new Handler(Looper.getMainLooper()).postDelayed(onReady, 1000);
    }

    private void showNextFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);

        Fragment currentFrag = getCurrentFragment();
        Fragment nextFragment;
        String nextFragmentTag;

        switch (currentFragment) {
            case FRAG_1:
                nextFragment = registerFragment2;
                nextFragmentTag = "FRAG2";
                currentFragment = FRAG_2;
                updateButtonForFragment(FRAG_2);
                Extras extras = new Extras();
                extras.hideWithFadeAnimation(orContinueTextview);
                extras.hideWithFadeAnimation(googleSignInButton);
                break;
            case FRAG_2:
                nextFragment = registerFragment3;
                nextFragmentTag = "FRAG3";
                currentFragment = FRAG_3;
                updateButtonForFragment(FRAG_3);
                break;
            case FRAG_3:
                nextFragment = registerFragment4;
                nextFragmentTag = "FRAG4";
                currentFragment = FRAG_4;
                updateButtonForFragment(FRAG_4);
                break;
            default:
                return;
        }

        transaction.hide(currentFrag);

        if (getSupportFragmentManager().findFragmentByTag(nextFragmentTag) == null) {
            transaction.add(R.id.main_registration_frame, nextFragment, nextFragmentTag);
        } else {
            transaction.show(nextFragment);
        }

        transaction.commit();
    }

    private Fragment getCurrentFragment() {
        switch (currentFragment) {
            case FRAG_2:
                return registerFragment2;
            case FRAG_3:
                return registerFragment3;
            case FRAG_4:
                return registerFragment4;
            default:
                return registerFragment1;
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_FRAGMENT, currentFragment);
    }

    private void updateButtonForFragment(int fragment) {
        switch (fragment) {
            case FRAG_1:
                sendOtpBtn.setText(getString(R.string.enter_otp));
                break;
            case FRAG_2:
                sendOtpBtn.setText(getString(R.string.verify_otp));
                break;
            case FRAG_3:
                sendOtpBtn.setText(getString(R.string.continue_));
                sendOtpBtn.setEnabled(true);
                break;
            case FRAG_4:
                sendOtpBtn.setText(getString(R.string.complete_));
                sendOtpBtn.setEnabled(true);
                break;
        }
    }

    public static void buttonEnable(Context context) {
        sendOtpBtn.setEnabled(true);
        sendOtpBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.mode_inverse));
    }

    public static void buttonDisabled(Context context) {
        sendOtpBtn.setEnabled(false);
        sendOtpBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.mode_inverse_extra));
    }

    @Override
    public void onGoBack() {
        // If we're completing registration, don't allow going back
        if (getIntent().getIntExtra("START_FRAGMENT", FRAG_1) > FRAG_2) {
            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
            return;
        }

        Extras extras = new Extras();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

        Fragment currentFrag = getCurrentFragment();
        if (currentFrag != null) {
            transaction.hide(currentFrag);
        }

        transaction.show(registerFragment1);
        transaction.commitAllowingStateLoss();

        currentFragment = FRAG_1;

        sendOtpBtn.setVisibility(View.GONE);
        extras.showWithFadeAnimation(sendOtpBtn);
        extras.showWithFadeAnimation(orContinueTextview);
        extras.showWithFadeAnimation(googleSignInButton);
        sendOtpBtn.setText(getString(R.string.enter_otp));

        if (registerFragment1 != null) {
            ((OnButtonClickListener) registerFragment1).requestFocus();
        }
    }

    public interface OnButtonClickListener {
        void onButtonClick();

        void requestFocus();

        boolean firstFragment();
    }

    public void handleOtpVerification(String otp) {
        // Remove the duplicate verification check here since it's handled in AuthManager
        if (otp != null && otp.length() == 6) {
            showLoadingDialog("Verifying OTP");
            authManager.verifyPhoneNumberWithCode(otp);
        } else {
            if (registerFragment2 != null) {
                registerFragment2.handleVerificationFailed();
            }
            Toast.makeText(this, "Please enter a valid OTP.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        hideLoadingDialog();

        // Check registration status
        authManager.checkUserRegistrationStatus(user, new AuthManager.RegistrationStatusCallback() {
            @Override
            public void onAlreadyRegistered() {
                // Sign out and redirect to login
                authManager.signOut();
                Toast.makeText(RegistrationActivity.this,
                        "You are already registered. Please login instead.",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onNotRegistered() {
                if (isGoogleSignIn) {
                    // New user via Google Sign In, start registration from Fragment 3
                    currentFragment = FRAG_3;
                    showNextFragment();
                    isGoogleSignIn = false;
                } else if (currentFragment == FRAG_2) {
                    if (registerFragment2 != null) {
                        registerFragment2.clearOTP();
                    }
                    showNextFragment();
                }
            }

            @Override
            public void onIncompleteRegistration(boolean hasBasicProfile) {
                // Navigate to appropriate fragment based on profile completion
                currentFragment = hasBasicProfile ? FRAG_4 : FRAG_3;
                showNextFragment();
                isGoogleSignIn = false;
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegistrationActivity.this,
                        "Error checking registration: " + error,
                        Toast.LENGTH_SHORT).show();
                // Sign out and redirect to login as a precaution
                authManager.signOut();
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onFailure(String error) {
        runOnUiThread(() -> {
            hideLoadingDialog();
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            if (currentFragment == FRAG_2 && registerFragment2 != null) {
                registerFragment2.handleVerificationFailed();
            }
        });
    }

    @Override
    public void onCodeSent(PhoneAuthProvider.ForceResendingToken token) {
        runOnUiThread(() -> {
            hideLoadingDialog();
            showNextFragment();
            if (registerFragment2 != null) {
                registerFragment2.clearOTP();
                registerFragment2.requestOTPViewFocus();
                registerFragment2.startOtpTimer();
            }
        });
    }

    @Override
    public void onCodeAutoRetrievalTimeout() {
        hideLoadingDialog();
        // Handle OTP timeout - maybe show a message to the user
        Toast.makeText(this, "OTP code timeout. Please request new code.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsRetrieverHelper != null) {
            smsRetrieverHelper.unregisterReceiver();
        }
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
}