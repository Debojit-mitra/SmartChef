package com.bunny.ml.smartchef.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.activities.RegistrationActivity;
import com.bunny.ml.smartchef.utils.CustomOTPView;
import com.bunny.ml.smartchef.utils.SharedData;
import com.bunny.ml.smartchef.utils.SmsRetrieverHelper;

import java.util.Locale;

public class RegisterFragment2 extends Fragment {
    private TextView resendOtpBtn, enterOtpDescTextview;
    private static CustomOTPView otpView;
    private CountDownTimer countDownTimer;
    private SmsRetrieverHelper smsRetrieverHelper;
    private static final long OTP_TIMEOUT_MILLISECONDS = 60000; // 60 seconds

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register2, container, false);

        initializeViews(view);
        setupOtpView();
        startOtpTimer();

        return view;
    }

    private void initializeViews(View view) {
        TextView reEnterNumberBtn = view.findViewById(R.id.reEnterNumberBtn);
        resendOtpBtn = view.findViewById(R.id.resendOtpBtn);
        enterOtpDescTextview = view.findViewById(R.id.enterOtpDescTextview);
        otpView = view.findViewById(R.id.otpView);
        updateOtpDescription();
        reEnterNumberBtn.setOnClickListener(v ->
                ((goBackListener) requireActivity()).onGoBack());

        resendOtpBtn.setOnClickListener(v -> handleResendOtp());
    }

    private void setupOtpView() {
        if (otpView != null) {
            otpView.setOnOTPCompleteListener(this::handleOTPComplete);

            otpView.post(() -> {
                otpView.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(otpView, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    private void updateOtpDescription() {
        String phoneNumber = SharedData.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            enterOtpDescTextview.setText(String.format("Enter otp sent to your phone number %s", phoneNumber));
        }
    }

    public void showVerificationState(String state) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Verification state: " + state, Toast.LENGTH_SHORT).show();
        }
    }

    public void handleOTPComplete(String otp) {
        Log.d("OTP_DEBUG", "OTP Complete Called: " + otp);
        if (getActivity() instanceof RegistrationActivity) {
            RegistrationActivity activity = (RegistrationActivity) getActivity();
            activity.runOnUiThread(() -> {
                Log.d("OTP_DEBUG", "Handling verification for OTP: " + otp);
                activity.handleOtpVerification(otp);
            });
        }
    }


    public void handleVerificationFailed() {
        if (otpView != null) {
            otpView.setError(true);
            otpView.clearOTP();
            otpView.requestFocus();
        }
    }

    public void setOTPError() {
        if (otpView != null) {
            otpView.setError(true);
            otpView.clearOTP();
        }
    }

    public String getEnteredOTP() {
        return otpView != null ? otpView.getOTP() : null;
    }

    public void clearOTP() {
        if (otpView != null) {
            otpView.clearOTP();
        }
    }

    public void requestOTPViewFocus() {
        if (otpView != null) {
            otpView.requestFocus();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(otpView, InputMethodManager.SHOW_FORCED);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }, 100);
        }
    }

    public void startOtpTimer() {
        if (resendOtpBtn != null) {
            resendOtpBtn.setEnabled(false);
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            countDownTimer = new CountDownTimer(OTP_TIMEOUT_MILLISECONDS, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (resendOtpBtn != null) {
                        int seconds = (int) (millisUntilFinished / 1000);
                        resendOtpBtn.setText(String.format(Locale.getDefault(),
                                "Resend OTP in %02d:%02d", seconds / 60, seconds % 60));
                    }
                }

                @Override
                public void onFinish() {
                    if (resendOtpBtn != null) {
                        resendOtpBtn.setText(getString(R.string.resend));
                        resendOtpBtn.setEnabled(true);
                    }
                }
            }.start();
        }
    }

    private void handleResendOtp() {
        String phoneNumber = SharedData.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            RegistrationActivity activity = (RegistrationActivity) requireActivity();
            activity.showLoadingDialog("Resending OTP");

            // Clean up existing SMS retriever
            if (activity.smsRetrieverHelper != null) {
                activity.smsRetrieverHelper.unregisterReceiver();
            }

            // Initialize and start new SMS retriever before resending OTP
            activity.initializeAndStartSmsRetriever(() -> {
                activity.getAuthManager().startPhoneNumberVerification(phoneNumber);
            });

            startOtpTimer();
        }
    }

    public void setOTP(String otp) {
        if (otpView != null && otp != null && otp.length() == 6) {
            Log.d("OTP_DEBUG", "Setting OTP: " + otp);
            otpView.post(() -> {
                otpView.setOTP(otp);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public interface goBackListener {
        void onGoBack();
    }
}