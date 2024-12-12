package com.bunny.ml.smartchef.fragments;

import static com.bunny.ml.smartchef.utils.Extras.hideKeyboard;
import static com.bunny.ml.smartchef.utils.Extras.showKeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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

public class LoginFragment1 extends Fragment {
    private TextInputEditText phoneNumberEditText;
    private TextInputLayout phoneNumberTextInput;
    private TextView registerBtn;
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
        View view = inflater.inflate(R.layout.fragment_login1, container, false);

        initializeViews(view);
        setupTextWatcher();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        phoneNumberTextInput = view.findViewById(R.id.phoneNumberTextInput);
        registerBtn = view.findViewById(R.id.registerBtn);
    }

    private void setupTextWatcher() {
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            String cleanNumber;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > before) {  // Text was added
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
                String phoneNumber = s.toString();
                PhoneNumberValidator.ValidationResult result =
                        PhoneNumberValidator.validateIndianPhoneNumber(phoneNumber);

                phoneNumberTextInput.setError(result.getErrorMessage());

                if (result.isValid() && !phoneNumber.isEmpty()) {
                    SharedData.setPhoneNumber(PhoneNumberValidator.formatWithCountryCode(phoneNumber));
                    LoginActivity.buttonEnable(requireContext());
                } else {
                    LoginActivity.buttonDisabled(requireContext());
                }
                if (isFirstFocus) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> phoneNumberEditText.setText(cleanNumber), 10);
                }
            }
        });
        phoneNumberEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                if (isFirstFocus) {
                    // For first focus, show phone hint and prevent keyboard
                    phoneNumberEditText.clearFocus();
                    hideKeyboard(requireActivity(), phoneNumberEditText);
                    requestPhoneNumberHint();
                } else {
                    //to set length to 10 after first focus
                    int maxLength = 10;
                    InputFilter[] filters = new InputFilter[1];
                    filters[0] = new InputFilter.LengthFilter(maxLength);
                    phoneNumberEditText.setFilters(filters);
                    // For subsequent focuses, show keyboard normally
                    showKeyboard(requireActivity(), phoneNumberEditText);
                }
            }
        });
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), RegistrationActivity.class);
            requireActivity().startActivity(intent);
            requireActivity().finish();
        });
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
}