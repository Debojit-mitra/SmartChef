package com.bunny.ml.smartchef.fragments;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.models.UserData;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterFragment3 extends Fragment {

    private CircleImageView profile_image;
    private TextInputEditText nameEditText;
    private TextInputLayout nameInputLayout;
    private TextInputLayout dobInputLayout;
    private MaterialAutoCompleteTextView dobEditText;
    private RadioGroup radio_group_character_gender;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register3, container, false);

        initializeViews(view);
        setupDatePicker();
        setupImagePicker();
        return view;
    }

    private void initializeViews(View view) {
        profile_image = view.findViewById(R.id.profile_image);
        dobEditText = view.findViewById(R.id.dobEditText);
        nameEditText = view.findViewById(R.id.nameEditText);
        nameInputLayout = view.findViewById(R.id.nameTextInput);
        dobInputLayout = view.findViewById(R.id.dobTextInput);

        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        ContextCompat.getColor(requireActivity(), R.color.mode_inverse),
                        ContextCompat.getColor(requireActivity(), R.color.mode_inverse)
                }
        );

        radio_group_character_gender = view.findViewById(R.id.radio_group_character_gender);
        for (int i = 0; i < radio_group_character_gender.getChildCount(); i++) {
            if (radio_group_character_gender.getChildAt(i) instanceof RadioButton) {
                ((RadioButton) radio_group_character_gender.getChildAt(i)).setButtonTintList(colorStateList);
            }
        }

        profile_image.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && !s.toString().trim().isEmpty()) {
                    nameInputLayout.setError(null);
                }
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        profile_image.setImageURI(uri);
                    }
                }
        );
    }

    // In RegisterFragment3.java

    private void setupDatePicker() {
        // Create date picker
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -13);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date of birth")
                .setSelection(maxDate.getTimeInMillis())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(maxDate.getTimeInMillis())
                        .build())
                .setTheme(R.style.CustomDatePickerStyle)
                .build();

        // Set up the AutoCompleteTextView
        dobEditText.setFocusable(false);
        dobEditText.setClickable(true);
        dobInputLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);

        // Handle clicks on the EditText and its parent layout
        View.OnClickListener showDatePicker = v -> {
            if (!datePicker.isAdded()) {
                datePicker.show(getParentFragmentManager(), "DOB_PICKER");
            }
        };

        dobEditText.setOnClickListener(showDatePicker);
        dobInputLayout.setStartIconOnClickListener(showDatePicker);
        dobInputLayout.setEndIconOnClickListener(v -> dobEditText.setText(""));

        // Handle date selection
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dobEditText.setText(sdf.format(new Date(selection)));
            dobInputLayout.setError(null);
        });
    }

    public boolean validateData() {
        boolean isValid = true;
        String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
        String dob = dobEditText.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Name is required");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // Validate date of birth
        if (TextUtils.isEmpty(dob)) {
            dobInputLayout.setError("Date of birth is required");
            isValid = false;
        } else {
            dobInputLayout.setError(null);
        }

        // Validate gender selection
        if (radio_group_character_gender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Please select your gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    public UserData getUserData() {
        UserData userData = new UserData();

        // Set required fields
        userData.setName(Objects.requireNonNull(nameEditText.getText()).toString().trim());
        userData.setDateOfBirth(dobEditText.getText().toString().trim());

        // Set gender
        int selectedId = radio_group_character_gender.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedGender = radio_group_character_gender.findViewById(selectedId);
            userData.setGender(selectedGender.getText().toString());
        }

        // Set phone number from Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhoneNumber() != null) {
            userData.setPhoneNumber(currentUser.getPhoneNumber());
        }

        return userData;
    }

    public Uri getSelectedImageUri() {
        return selectedImageUri;
    }

}