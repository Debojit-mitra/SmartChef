package com.bunny.ml.smartchef.firebase;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final long VERIFICATION_TIMEOUT_SECONDS = 60;

    private final FirebaseAuth firebaseAuth;
    private final SignInClient signInClient;
    private final Activity activity;
    private final String webClientId;

    private AuthCallback authCallback;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher;
    private boolean isVerificationInProgress;

    public AuthManager(Activity activity, String webClientId) {
        this.activity = activity;
        this.webClientId = webClientId;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.signInClient = Identity.getSignInClient(activity);
        setupPhoneAuthCallbacks();
    }

    public void setGoogleSignInLauncher(ActivityResultLauncher<IntentSenderRequest> launcher) {
        this.googleSignInLauncher = launcher;
    }

    private void setupPhoneAuthCallbacks() {
        phoneAuthCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                logDebug("onVerificationCompleted");
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                logError("onVerificationFailed", e);
                handleVerificationFailure(e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                logDebug("onCodeSent: " + vId);
                verificationId = vId;
                resendToken = token;
                isVerificationInProgress = true;
                notifyCodeSent(token);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String vId) {
                logDebug("onCodeAutoRetrievalTimeOut");
                if (verificationId != null && verificationId.equals(vId)) {
                    notifyCodeAutoRetrievalTimeout();
                }
            }
        };
    }

    public void startPhoneNumberVerification(String phoneNumber) {
        if (validatePhoneNumber(phoneNumber)) return;

        String formattedPhone = formatPhoneNumber(phoneNumber);
        logDebug("Starting phone verification for: " + formattedPhone);
        clearVerificationState(); // Clear any existing verification state

        PhoneAuthOptions options = buildPhoneAuthOptions(formattedPhone, null);
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void resendVerificationCode(String phoneNumber) {
        if (validatePhoneNumber(phoneNumber)) return;

        String formattedPhone = formatPhoneNumber(phoneNumber);
        logDebug("Resending verification code to: " + formattedPhone);

        PhoneAuthOptions options = buildPhoneAuthOptions(formattedPhone, resendToken);
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthOptions buildPhoneAuthOptions(String phoneNumber, @Nullable PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(phoneAuthCallbacks);

        if (token != null) {
            builder.setForceResendingToken(token);
        }

        return builder.build();
    }

    public void verifyPhoneNumberWithCode(String code) {
        logDebug("Verifying code. VerificationId: " + (verificationId != null ? "present" : "null") +
                ", isVerificationInProgress: " + isVerificationInProgress);

        if (!validateCode(code)) return;

        if (verificationId == null) {
            notifyFailure("Please request OTP first");
            return;
        }

        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        } catch (Exception e) {
            logError("Error creating credential", e);
            notifyFailure("Invalid verification code");
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        logDebug("Signing in with phone auth credential");
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    logDebug("signInWithCredential:success");
                    clearVerificationState();
                    notifySuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    logError("signInWithCredential:failure", e);
                    notifyFailure(e.getMessage());
                });
    }

    public void startGoogleSignIn() {
        if (googleSignInLauncher == null) {
            notifyFailure("Google Sign In not properly initialized");
            return;
        }

        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        signInClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                                result.getPendingIntent().getIntentSender())
                                .build();
                        googleSignInLauncher.launch(intentSenderRequest);
                    } catch (Exception e) {
                        notifyFailure("Could not start Google sign in: " + e.getLocalizedMessage());
                    }
                })
                .addOnFailureListener(e -> notifyFailure("Google sign in failed: " + e.getLocalizedMessage()));
    }

    public void handleGoogleSignInResult(Intent data) {
        try {
            SignInCredential credential = signInClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();
            if (idToken != null) {
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                firebaseAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(activity, task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                notifySuccess(task.getResult().getUser());
                            } else {
                                notifyFailure(task.getException() != null ?
                                        task.getException().getMessage() : "Google sign in failed");
                            }
                        });
            }
        } catch (ApiException e) {
            notifyFailure("Google sign in failed: " + e.getMessage());
        }
    }

    public void checkUserProfileCompletion(FirebaseUser user, ProfileCheckCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean hasBasicProfile = hasBasicProfile(documentSnapshot);
                        boolean hasCompleteProfile = hasBasicProfile &&
                                documentSnapshot.getString("dietPreference") != null;

                        if (hasCompleteProfile) {
                            callback.onProfileComplete();
                        } else {
                            callback.onProfileIncomplete(hasBasicProfile);
                        }
                    } else {
                        callback.onProfileIncomplete(false);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void checkUserRegistrationStatus(FirebaseUser user, RegistrationStatusCallback callback) {
        if (user == null) {
            callback.onNotRegistered();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean hasBasicProfile = hasBasicProfile(documentSnapshot);
                        boolean hasCompleteProfile = hasBasicProfile &&
                                documentSnapshot.getString("dietPreference") != null;

                        if (hasCompleteProfile) {
                            callback.onAlreadyRegistered();
                        } else {
                            callback.onIncompleteRegistration(hasBasicProfile);
                        }
                    } else {
                        callback.onNotRegistered();
                    }
                })
                .addOnFailureListener(e -> {
                    logError("Error checking registration status", e);
                    callback.onError(e.getMessage());
                });
    }


    private boolean hasBasicProfile(com.google.firebase.firestore.DocumentSnapshot doc) {
        return doc.getString("name") != null &&
                doc.getString("dateOfBirth") != null &&
                doc.getString("gender") != null;
    }

    // interface for registration status
    public interface RegistrationStatusCallback {
        void onAlreadyRegistered();
        void onNotRegistered();
        void onIncompleteRegistration(boolean hasBasicProfile);
        void onError(String error);
    }

    // Utility methods
    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            notifyFailure("Invalid phone number");
            return true;
        }
        return false;
    }

    private String formatPhoneNumber(String phoneNumber) {
        return phoneNumber.startsWith("+") ? phoneNumber : "+" + phoneNumber;
    }

    private boolean validateVerificationState() {
        if (!isVerificationInProgress || verificationId == null) {
            notifyFailure("Please request OTP first");
            return false;
        }
        return true;
    }

    private boolean validateCode(String code) {
        if (code == null || code.length() != 6) {
            notifyFailure("Invalid OTP code");
            return false;
        }
        return true;
    }

    // Notification methods
    private void notifySuccess(FirebaseUser user) {
        if (authCallback != null) authCallback.onSuccess(user);
    }

    private void notifyFailure(String error) {
        if (authCallback != null) authCallback.onFailure(error);
    }

    private void notifyCodeSent(PhoneAuthProvider.ForceResendingToken token) {
        if (authCallback != null) authCallback.onCodeSent(token);
    }

    private void notifyCodeAutoRetrievalTimeout() {
        if (authCallback != null) authCallback.onCodeAutoRetrievalTimeout();
    }

    private void handleVerificationFailure(String message) {
        isVerificationInProgress = false;
        verificationId = null;
        notifyFailure(message);
    }

    // Logging methods
    private void logDebug(String message) {
        Log.d(TAG, message);
    }

    private void logError(String message, Exception e) {
        Log.e(TAG, message, e);
    }

    // Public methods
    public void signOut() {
        // Sign out from Firebase Auth
        firebaseAuth.signOut();

        // Sign out from Google Sign In if it was used
        signInClient.signOut().addOnCompleteListener(task -> {
            // Clear any verification state
            clearVerificationState();
        });
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void setAuthCallback(AuthCallback callback) {
        this.authCallback = callback;
    }

    public boolean isVerificationInProgress() {
        return isVerificationInProgress && verificationId != null;
    }

    public void clearVerificationState() {
        isVerificationInProgress = false;
        verificationId = null;
        resendToken = null;
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneAuthCallbacks;

    // Interfaces
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onFailure(String error);

        void onCodeSent(PhoneAuthProvider.ForceResendingToken token);

        void onCodeAutoRetrievalTimeout();
    }

    public interface ProfileCheckCallback {
        void onProfileComplete();

        void onProfileIncomplete(boolean hasBasicProfile);

        void onError(String error);
    }
}