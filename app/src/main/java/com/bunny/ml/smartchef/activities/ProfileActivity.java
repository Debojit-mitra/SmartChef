package com.bunny.ml.smartchef.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.models.UserData;
import com.bunny.ml.smartchef.utils.CookingMotivationManager;
import com.bunny.ml.smartchef.utils.CustomAlertDialog;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.bunny.ml.smartchef.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private CircleImageView profileImage;
    private TextInputEditText nameEditText, conditionsEditText;
    private MaterialAutoCompleteTextView dobEditText;
    private MaterialAutoCompleteTextView dietPrefEditText;
    private TextView account_using_textview, logout_btn;
    private TextInputLayout nameInputLayout;
    private TextInputLayout dobInputLayout;
    private TextInputLayout dietPrefTextInput;
    private TextInputLayout cuisinePrefTextInput;
    private RadioGroup genderRadioGroup;
    private ChipGroup cuisineChipGroup;
    private MaterialSwitch cookingMotivationSwitch, healthConsciousSwitch;
    private LoadingDialog loadingDialog;
    private ProfileManager profileManager;
    private Uri selectedImageUri;
    private final Set<String> selectedCuisines = new HashSet<>();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ListPopupWindow listPopupWindow;
    private Chip cuisineInputChip;
    private static final int MAX_CUISINE_SELECTIONS = 5;
    private MaterialButton updateProfileBtn;
    private UserData originalUserData;
    private Uri originalImageUri;
    private boolean hasChanges = false;
    private CookingMotivationManager cookingMotivationManager;

    private static MainActivityCallback mainActivityCallback;

    public interface MainActivityCallback {
        void refreshProfileImage(String profileImageURL);
    }

    public static void setCallback(MainActivityCallback callback) {
        mainActivityCallback = callback;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        cookingMotivationManager = new CookingMotivationManager(this);

        initializeViews();
        initializeLoadingDialog();
        setupImagePicker();
        setupDatePicker();
        setupDietTypeSelection();
        setupCuisineSelection();
        setupBackPressHandling();
        setupChangeListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        ImageView backBtn = findViewById(R.id.backBtn);
        profileImage = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.nameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        dietPrefEditText = findViewById(R.id.dietPrefEditText);
        nameInputLayout = findViewById(R.id.nameTextInput);
        dobInputLayout = findViewById(R.id.dobTextInput);
        dietPrefTextInput = findViewById(R.id.dietPrefTextInput);
        conditionsEditText = findViewById(R.id.conditionsEditText);
        cuisinePrefTextInput = findViewById(R.id.cuisinePrefTextInput);
        genderRadioGroup = findViewById(R.id.radio_group_character_gender);
        cuisineChipGroup = findViewById(R.id.cuisineChipGroup);
        cuisineInputChip = findViewById(R.id.cuisineInputChip);
        cookingMotivationSwitch = findViewById(R.id.cookingMotivationSwitch);
        healthConsciousSwitch = findViewById(R.id.healthConsciousSwitch);
        account_using_textview = findViewById(R.id.account_using_textview);
        logout_btn = findViewById(R.id.logout_btn);
        updateProfileBtn = findViewById(R.id.updateProfileBtn);
        updateProfileBtn.setEnabled(false);

        // Setup click listeners
        backBtn.setOnClickListener(view -> handleBack());
        logout_btn.setOnClickListener(view -> logoutUser());
        profileImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        updateProfileBtn.setOnClickListener(v -> updateProfile());

        // Setup radio group colors
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        ContextCompat.getColor(this, R.color.mode_inverse),
                        ContextCompat.getColor(this, R.color.mode_inverse)
                }
        );

        for (int i = 0; i < genderRadioGroup.getChildCount(); i++) {
            if (genderRadioGroup.getChildAt(i) instanceof RadioButton) {
                ((RadioButton) genderRadioGroup.getChildAt(i)).setButtonTintList(colorStateList);
            }
        }
    }

    private void initializeLoadingDialog() {
        loadingDialog = new LoadingDialog(this);
    }

    private void setupChangeListeners() {
        // Name change listener
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Date of birth change listener
        dobEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Diet preference change listener
        dietPrefEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        conditionsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Gender change listener
        genderRadioGroup.setOnCheckedChangeListener((group, checkedId) -> checkForChanges());

        // Cooking motivation change listener
        cookingMotivationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!PermissionManager.hasNotificationPermission(this)) {
                        showNotificationPermissionDialog();
                        return;
                    }
                }
                // Initialize manager if needed
                if (cookingMotivationManager == null) {
                    cookingMotivationManager = new CookingMotivationManager(this);
                }
                // Schedule motivation notifications
                cookingMotivationManager.scheduleDailyMotivation();
            } else {
                if (cookingMotivationManager == null) {
                    cookingMotivationManager = new CookingMotivationManager(this);
                }
                // Cancel motivation notifications
                cookingMotivationManager.cancelDailyMotivation();
            }
            new Handler(Looper.getMainLooper()).postDelayed(this::checkForChanges, 500);
        });

        healthConsciousSwitch.setOnCheckedChangeListener((compoundButton, b) -> checkForChanges());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        profileImage.setImageURI(uri);
                        checkForChanges();
                    }
                }
        );
    }

    private void setupDatePicker() {
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

        dobEditText.setFocusable(false);
        dobEditText.setClickable(true);
        dobInputLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);

        View.OnClickListener showDatePicker = v -> {
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), "DOB_PICKER");
            }
        };

        dobEditText.setOnClickListener(showDatePicker);
        dobInputLayout.setStartIconOnClickListener(showDatePicker);
        dobInputLayout.setEndIconOnClickListener(v -> dobEditText.setText(""));

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dobEditText.setText(sdf.format(new Date(selection)));
            dobInputLayout.setError(null);
        });
    }

    private void setupDietTypeSelection() {
        String[] dietTypes = {
                "Vegetarian", "Vegan", "Flexitarian", "Pescatarian", "Keto",
                "Paleo", "Mediterranean", "Gluten-Free", "Low-Carb"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ProfileActivity.this,
                R.layout.simple_dropdown_item,
                dietTypes
        );

        dietPrefEditText.setAdapter(adapter);
        dietPrefEditText.setOnClickListener(v -> dietPrefEditText.showDropDown());
        dietPrefEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dietPrefEditText.showDropDown();
            }
        });
        dietPrefTextInput.setEndIconOnClickListener(v -> {
            dietPrefEditText.setText("");
            dietPrefEditText.clearFocus();
        });
    }

    private void setupCuisineSelection() {
        String[] cuisineTypes = {
                "Indian", "Chinese", "Korean", "Japanese", "Italian",
                "Mexican", "Thai", "Mediterranean", "French", "American",
                "Vietnamese", "Greek", "Spanish", "Middle Eastern"
        };

        listPopupWindow = new ListPopupWindow(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.simple_dropdown_item,
                cuisineTypes
        );

        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setAnchorView(cuisineInputChip);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCuisine = cuisineTypes[position];
            if (selectedCuisines.size() >= MAX_CUISINE_SELECTIONS) {
                showMaxCuisineToast();
            } else if (!selectedCuisines.contains(selectedCuisine)) {
                selectedCuisines.add(selectedCuisine);
                addCuisineChip(selectedCuisine);
            }
            listPopupWindow.dismiss();
        });

        cuisineInputChip.setCheckedIconVisible(false);
        cuisineInputChip.setOnClickListener(v -> {
            if (selectedCuisines.size() >= MAX_CUISINE_SELECTIONS) {
                showMaxCuisineToast();
            } else if (!listPopupWindow.isShowing()) {
                listPopupWindow.show();
            }
        });
    }

    private void loadUserProfile() {
        loadingDialog.show("Loading profile...");
        profileManager = ProfileManager.getInstance(this);

        if (profileManager.isCacheFresh()) {
            // Use cached data if it's fresh
            UserData userData = profileManager.getCachedUserData();
            if (userData != null) {
                populateUserData(userData);
                loadingDialog.dismiss();
                return;
            }
        }

        // Load from server if cache isn't fresh or available
        profileManager.loadUserProfile(new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserData userData) {
                runOnUiThread(() -> {
                    populateUserData(userData);
                    loadingDialog.dismiss();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "Error loading profile: " + error,
                            Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                });
            }
        });
    }


    private void populateUserData(UserData userData) {
        if (userData == null) return;

        // Store original data
        originalUserData = userData;
        originalImageUri = null;

        profileManager.loadProfileImage(profileImage, userData.getProfilePhotoUrl());
        nameEditText.setText(userData.getName());
        dobEditText.setText(userData.getDateOfBirth());
        dietPrefEditText.setText(userData.getDietPreference());
        conditionsEditText.setText(userData.getConditions());
        setupDietTypeSelection();

        if (userData.getGender() != null) {
            switch (userData.getGender().toLowerCase()) {
                case "male":
                    ((RadioButton) findViewById(R.id.radio_male)).setChecked(true);
                    break;
                case "female":
                    ((RadioButton) findViewById(R.id.radio_female)).setChecked(true);
                    break;
                case "others":
                    ((RadioButton) findViewById(R.id.radio_others)).setChecked(true);
                    break;
            }
        }

        if (userData.getCuisinePreferences() != null) {
            selectedCuisines.clear();
            cuisineChipGroup.removeAllViews();
            cuisineChipGroup.addView(cuisineInputChip, 0);

            for (String cuisine : userData.getCuisinePreferences()) {
                selectedCuisines.add(cuisine);
                addCuisineChip(cuisine);
            }
        }

        cookingMotivationSwitch.setChecked(userData.isCookingMotivation());
        healthConsciousSwitch.setChecked(userData.isHealthConscious());

        String yourAccountSetup = getString(R.string.your_account_is_using);
        if (userData.getPhoneNumber() != null && !userData.getPhoneNumber().isEmpty()) {
            yourAccountSetup = yourAccountSetup + " " + userData.getPhoneNumber();
            account_using_textview.setText(yourAccountSetup);
        } else {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                yourAccountSetup = yourAccountSetup + " " + firebaseUser.getEmail();
                account_using_textview.setText(yourAccountSetup);
            }
        }

        // Reset changes flag after populating
        hasChanges = false;
        updateProfileBtn.setEnabled(false);
        updateProfileBtn.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.mode_inverse_extra));
    }

    private void addCuisineChip(String cuisine) {
        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.SelectedCuisineChipStyle));
        chip.setText(cuisine);
        chip.setCloseIconVisible(true);

        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.mode_darker));
        chip.setChipBackgroundColor(colorStateList);

        chip.setOnCloseIconClickListener(v -> {
            cuisineChipGroup.removeView(chip);
            selectedCuisines.remove(cuisine);
            checkForChanges();
        });

        cuisineChipGroup.addView(chip, 1);
        checkForChanges();
    }

    private void updateProfile() {
        if (!validateData()) return;

        loadingDialog.show("Updating profile");
        String phoneNumber = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getPhoneNumber();
        String conditions = conditionsEditText.getText() != null ? conditionsEditText.getText().toString().trim() : "";


        UserData userData = new UserData.Builder()
                .withName(Objects.requireNonNull(nameEditText.getText()).toString().trim())
                .withPhoneNumber(phoneNumber)
                .withDateOfBirth(dobEditText.getText().toString().trim())
                .withGender(getSelectedGender())
                .withDietPreference(dietPrefEditText.getText().toString().trim())
                .withConditions(conditions)
                .withCuisinePreferences(new ArrayList<>(selectedCuisines))
                .withHealthConscious(healthConsciousSwitch.isChecked())
                .withCookingMotivation(cookingMotivationSwitch.isChecked())
                .build();

        ProfileManager profileManager = ProfileManager.getInstance(this);
        profileManager.updateProfile(userData, selectedImageUri, new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserData updatedData) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Profile manager already updates cache in updateProfile
                    // Just need to refresh the Main Activity's UI
                    refreshProfileImageInMain(updatedData.getProfilePhotoUrl());

                    loadingDialog.dismiss();
                    handleBack();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "Error updating profile: " + error, Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                });
            }
        });
    }

    private boolean validateData() {
        boolean isValid = true;

        if (TextUtils.isEmpty(nameEditText.getText())) {
            nameInputLayout.setError("Name is required");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(dobEditText.getText())) {
            dobInputLayout.setError("Date of birth is required");
            isValid = false;
        } else {
            dobInputLayout.setError(null);
        }

        if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private String getSelectedGender() {
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedGender = findViewById(selectedId);
            return selectedGender.getText().toString();
        }
        return null;
    }

    private void showMaxCuisineToast() {
        Toast.makeText(this, "Maximum " + MAX_CUISINE_SELECTIONS + " cuisines allowed",
                Toast.LENGTH_SHORT).show();
    }

    private void refreshProfileImageInMain(String profilePhotoUrl) {
        if (mainActivityCallback != null) {
            mainActivityCallback.refreshProfileImage(profilePhotoUrl);
        }
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void checkForChanges() {
        if (originalUserData == null) return;

        boolean hasNameChange = !Objects.equals(
                originalUserData.getName(),
                nameEditText.getText() != null ? nameEditText.getText().toString().trim() : ""
        );

        boolean hasDobChange = !Objects.equals(
                originalUserData.getDateOfBirth(),
                dobEditText.getText() != null ? dobEditText.getText().toString().trim() : ""
        );

        boolean hasDietChange = !Objects.equals(
                originalUserData.getDietPreference() != null ? originalUserData.getDietPreference() : "",
                dietPrefEditText.getText() != null ? dietPrefEditText.getText().toString().trim() : ""
        );

        boolean hasConditionsChange = !Objects.equals(
                originalUserData.getConditions() != null ? originalUserData.getConditions() : "",
                conditionsEditText.getText() != null ? conditionsEditText.getText().toString().trim() : ""
        );

        boolean hasGenderChange = !Objects.equals(
                originalUserData.getGender(),
                getSelectedGender()
        );

        boolean hasCuisineChange = !Objects.equals(
                new HashSet<>(originalUserData.getCuisinePreferences() != null ?
                        originalUserData.getCuisinePreferences() : new ArrayList<>()),
                selectedCuisines.isEmpty() ? new HashSet<>() : selectedCuisines
        );

        boolean hasHealthConsciousChange = originalUserData.isHealthConscious() != healthConsciousSwitch.isChecked();

        boolean hasMotivationChange = originalUserData.isCookingMotivation() != cookingMotivationSwitch.isChecked();

        boolean hasImageChange = selectedImageUri != null && !Objects.equals(selectedImageUri, originalImageUri);

        hasChanges = hasNameChange || hasDobChange || hasDietChange || hasHealthConsciousChange || hasConditionsChange || hasGenderChange ||
                hasCuisineChange || hasMotivationChange || hasImageChange;

        updateProfileBtn.setEnabled(hasChanges);
        if (hasChanges) {
            updateProfileBtn.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.mode_inverse));
        } else {
            updateProfileBtn.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.mode_inverse_extra));
        }
    }

    private void logoutUser() {
        CustomAlertDialog customAlertDialog = new CustomAlertDialog(ProfileActivity.this);
        customAlertDialog
                .setDialogTitle("Logout")
                .setMessage("Your are about to be logged out!")
                .setTitleAlignment(View.TEXT_ALIGNMENT_TEXT_START)
                .setPositiveButton("Yes", () -> {
                    profileManager.signOut(); // This already clears the cache
                    Intent intent = new Intent(ProfileActivity.this, SignInSignUpActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                })
                .setNegativeButton("Cancel", customAlertDialog::dismiss)
                .show();
    }

    private void showNotificationPermissionDialog() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> new CustomAlertDialog(ProfileActivity.this)
                .setDialogTitle("Notification Permission")
                .setMessage("SmartChef needs notification permission for two reasons:\n\n1. For sending cooking motivations.\n2. To keep you updated with the latest app versions.\n\nWould you like to enable notifications?")
                .setPositiveButton("Enable", () -> {
                    PermissionManager.setNotificationPermissionAsked(this, true);
                    requestNotificationPermission();
                })
                .setNegativeButton("No Thanks", () -> {
                    cookingMotivationSwitch.setChecked(false);
                    cookingMotivationManager.cancelDailyMotivation();
                    PermissionManager.setNotificationPermissionAsked(this, true);
                    PermissionManager.setNotificationPermissionDenied(this, true);
                    PermissionManager.setAutoUpdateEnabled(ProfileActivity.this, false);
                    // Cancel any scheduled update checks
                    androidx.work.WorkManager.getInstance(ProfileActivity.this)
                            .cancelUniqueWork("update_check");
                })
                .show(), 500);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                cookingMotivationManager.scheduleDailyMotivation();
                PermissionManager.setNotificationPermissionDenied(this, false);
            } else {
                // Permission denied
                cookingMotivationSwitch.setChecked(false);
                cookingMotivationManager.cancelDailyMotivation();
                PermissionManager.setNotificationPermissionDenied(this, true);
                PermissionManager.setAutoUpdateEnabled(this, false);
            }
        }
    }

    private void handleBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listPopupWindow != null && listPopupWindow.isShowing()) {
            listPopupWindow.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
        if (listPopupWindow != null) {
            listPopupWindow.dismiss();
            listPopupWindow = null;
        }
    }
}