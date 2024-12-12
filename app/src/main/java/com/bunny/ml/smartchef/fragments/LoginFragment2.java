package com.bunny.ml.smartchef.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.activities.LoginActivity;
import com.bunny.ml.smartchef.utils.CustomOTPView;
import com.bunny.ml.smartchef.utils.SharedData;

import java.util.Locale;

public class LoginFragment2 extends Fragment {
    private TextView resendOtpBtn;
    private TextView enterOtpDescTextview;
    private CustomOTPView otpView;
    private CountDownTimer countDownTimer;
    private static final long OTP_TIMEOUT_MILLISECONDS = 60000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login2, container, false);

        initializeViews(view);
        setupOtpView();
        setupClickListeners();
        updateOtpDescription();

        return view;
    }

    private void initializeViews(View view) {
        resendOtpBtn = view.findViewById(R.id.resendOtpBtn);
        enterOtpDescTextview = view.findViewById(R.id.enterOtpDescTextview);
        otpView = view.findViewById(R.id.otpView);
        TextView reEnterNumberBtn = view.findViewById(R.id.reEnterNumberBtn);

        reEnterNumberBtn.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupOtpView() {
        otpView.setOnOTPCompleteListener(this::handleOTPComplete);
        requestOTPViewFocus();
    }

    private void setupClickListeners() {
        resendOtpBtn.setOnClickListener(v -> handleResendOtp());
    }

    private void updateOtpDescription() {
        String phoneNumber = SharedData.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            enterOtpDescTextview.setText(String.format("Enter OTP sent to your phone number %s", phoneNumber));
        }
    }

    private void handleOTPComplete(String otp) {
        if (getActivity() instanceof LoginActivity) {
            LoginActivity activity = (LoginActivity) getActivity();
            activity.handleLoginClick();
        }
    }

    public void handleVerificationFailed() {
        if (otpView != null) {
            otpView.setError(true);
            otpView.clearOTP();
            otpView.requestFocus();
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
                // Check if Fragment is still attached before accessing context
                if (isAdded() && getContext() != null) {
                    InputMethodManager imm = (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(otpView, InputMethodManager.SHOW_FORCED);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            }, 100);
        }
    }

    private void handleResendOtp() {
        String phoneNumber = SharedData.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty() && getActivity() instanceof LoginActivity) {
            LoginActivity activity = (LoginActivity) getActivity();
            activity.showLoadingDialog("Resending OTP...");
            activity.getAuthManager().startPhoneNumberVerification(phoneNumber);
            startOtpTimer();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}