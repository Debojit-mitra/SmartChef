package com.bunny.ml.smartchef.fragments;

import static com.bunny.ml.smartchef.utils.Extras.hideKeyboard;
import static com.bunny.ml.smartchef.utils.Extras.showKeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.activities.LoginActivity;
import com.bunny.ml.smartchef.activities.RegistrationActivity;
import com.bunny.ml.smartchef.utils.PhoneNumberValidator;
import com.bunny.ml.smartchef.utils.SharedData;
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class RegisterFragment1 extends Fragment implements RegistrationActivity.OnButtonClickListener {
    private TextInputEditText phoneNumberEditText;
    private TextInputLayout phoneNumberTextInput;
    private boolean firstFrag;
    private View alreadyHaveAccLayout;
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private PhoneNumberTextWatcher phoneNumberTextWatcher;
    private TextView loginBtn;
    private ActivityResultLauncher<IntentSenderRequest> phoneNumberHintLauncher;
    private boolean isFirstFocus = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoneNumberHintLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        String phoneNumber = null;
                        try {
                            phoneNumber = Identity.getSignInClient(requireActivity())
                                    .getPhoneNumberFromIntent(result.getData());
                            phoneNumberEditText.setText(phoneNumber);
                        } catch (ApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // After phone hint dialog is dismissed, mark first focus as complete
                    isFirstFocus = false;
                }
        );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register1, container, false);
        initializeViews(view);
        restoreState(savedInstanceState);
        setupTextWatcher();
        return view;
    }

    private void initializeViews(View view) {
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        phoneNumberTextInput = view.findViewById(R.id.phoneNumberTextInput);
        alreadyHaveAccLayout = view.findViewById(R.id.alreadyHaveAccLayout);
        loginBtn = view.findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                requireActivity().startActivity(intent);
                requireActivity().finish();
            }
        });
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String phoneNumber = savedInstanceState.getString(KEY_PHONE_NUMBER, "");
            phoneNumberEditText.setText(phoneNumber);
        }
    }

    private void requestPhoneNumberHint() {
        GetPhoneNumberHintIntentRequest request = GetPhoneNumberHintIntentRequest.builder().build();
        Identity.getSignInClient(requireActivity())
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener(result -> {
                    IntentSenderRequest intentRequest = new IntentSenderRequest.Builder(result.getIntentSender()).build();
                    phoneNumberHintLauncher.launch(intentRequest);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }


    private void setupTextWatcher() {
        phoneNumberTextWatcher = new PhoneNumberTextWatcher(
                phoneNumberTextInput,
                alreadyHaveAccLayout,
                requireContext()
        );
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            String cleanNumber;


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > before) {// Text was added
                    String text = s.toString();
                    if (text.startsWith("+91")) {
                        cleanNumber = text.substring(3);
                    } else if (text.startsWith("0")) {
                        // Remove leading 0
                        cleanNumber = text.substring(1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFirstFocus) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> phoneNumberEditText.setText(cleanNumber), 10);
                }
            }
        });

        phoneNumberEditText.addTextChangedListener(phoneNumberTextWatcher);
        phoneNumberEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                if (isFirstFocus) {
                    // For first focus, show phone hint and prevent keyboard
                    phoneNumberEditText.clearFocus();
                    hideKeyboard(requireActivity(), phoneNumberEditText);
                    requestPhoneNumberHint();
                } else {
                    // For subsequent focuses, show keyboard normally
                    int maxLength = 10;
                    InputFilter[] filters = new InputFilter[1];
                    filters[0] = new InputFilter.LengthFilter(maxLength);
                    phoneNumberEditText.setFilters(filters);
                    showKeyboard(requireActivity(), phoneNumberEditText);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHONE_NUMBER, Objects.requireNonNull(phoneNumberEditText.getText()).toString());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (phoneNumberTextWatcher != null) {
            phoneNumberTextWatcher.reapplyMargins();
        }
    }

    @Override
    public void onButtonClick() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> firstFrag = true, 100);
    }

    @Override
    public void requestFocus() {
        phoneNumberEditText.requestFocus();
        if (!isFirstFocus) {
            phoneNumberEditText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(phoneNumberEditText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        }
    }

    @Override
    public boolean firstFragment() {
        return firstFrag;
    }

    public static class PhoneNumberTextWatcher implements TextWatcher {
        private final TextInputLayout phoneNumberTextInput;
        private final View alreadyHaveAccLayout;
        private final Context context;

        public PhoneNumberTextWatcher(TextInputLayout phoneNumberTextInput,
                                      View alreadyHaveAccLayout,
                                      Context context) {
            this.phoneNumberTextInput = phoneNumberTextInput;
            this.alreadyHaveAccLayout = alreadyHaveAccLayout;
            this.context = context;
        }

        public void reapplyMargins() {
            Editable text = ((TextInputEditText) Objects.requireNonNull(phoneNumberTextInput.getEditText())).getText();
            if (text != null) {
                afterTextChanged(text);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            String phoneNumber = s.toString();
            PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validateIndianPhoneNumber(phoneNumber);
            phoneNumberTextInput.setError(result.getErrorMessage());
            updateLayoutMargin(phoneNumber.isEmpty(), result.isValid());

            if (result.isValid() && !phoneNumber.isEmpty()) {
                SharedData.setPhoneNumber(PhoneNumberValidator.formatWithCountryCode(phoneNumber));
                RegistrationActivity.buttonEnable(context);
            } else {
                RegistrationActivity.buttonDisabled(context);
            }
        }

        private void updateLayoutMargin(boolean isEmpty, boolean isValid) {
            if (alreadyHaveAccLayout == null) return;

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alreadyHaveAccLayout.getLayoutParams();
            int marginDp = isEmpty ? dpToPx(5) : (!isValid ? dpToPx(15) : dpToPx(5));

            if (params.topMargin != marginDp) {
                params.topMargin = marginDp;
                alreadyHaveAccLayout.setLayoutParams(params);
            }
        }

        private int dpToPx(int dp) {
            float density = context.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}