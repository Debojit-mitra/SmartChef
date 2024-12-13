package com.bunny.ml.smartchef.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bunny.ml.smartchef.MainActivity;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.firebase.DatabaseManager;
import com.bunny.ml.smartchef.firebase.FirebaseManager;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.models.UserData;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;
import java.util.Set;

public class RegisterFragment4 extends Fragment implements FirebaseManager.FirebaseCallback {
    private Dialog progressDialog;
    private MaterialAutoCompleteTextView dietPrefEditText;
    private TextInputLayout dietPrefTextInput;
    private TextInputLayout cuisinePrefTextInput;
    private TextInputEditText conditionsEditText;
    private ChipGroup cuisineChipGroup;
    private Chip cuisineInputChip;
    private final Set<String> selectedCuisines = new HashSet<>();
    private String[] cuisineTypes;
    private ListPopupWindow listPopupWindow;
    private MaterialSwitch cookingMotivationSwitch, healthConsciousSwitch;
    private static final int MAX_CUISINE_SELECTIONS = 5;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register4, container, false);
        initializeViews(view);
        setupDietTypeSelection();
        setupCuisineSelection();
        return view;
    }

    private void initializeViews(View view) {
        dietPrefEditText = view.findViewById(R.id.dietPrefEditText);
        dietPrefTextInput = view.findViewById(R.id.dietPrefTextInput);
        conditionsEditText = view.findViewById(R.id.conditionsEditText);
        cuisinePrefTextInput = view.findViewById(R.id.cuisinePrefTextInput);
        cuisineChipGroup = view.findViewById(R.id.cuisineChipGroup);
        cuisineInputChip = view.findViewById(R.id.cuisineInputChip);
        healthConsciousSwitch = view.findViewById(R.id.healthConsciousSwitch);
        cookingMotivationSwitch = view.findViewById(R.id.cookingMotivationSwitch);
        loadingDialog = new LoadingDialog(requireContext());

    }

    private void setupDietTypeSelection() {
        String[] dietTypes = {
                "Vegetarian", "Vegan", "Flexitarian", "Pescatarian", "Keto",
                "Paleo", "Mediterranean", "Gluten-Free", "Low-Carb"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireActivity(),
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
        cuisineTypes = new String[]{
                "Indian", "Chinese", "Korean", "Japanese", "Italian",
                "Mexican", "Thai", "Mediterranean", "French", "American",
                "Vietnamese", "Greek", "Spanish", "Middle Eastern"
        };

        listPopupWindow = new ListPopupWindow(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.simple_dropdown_item,
                cuisineTypes
        );

        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setAnchorView(cuisineInputChip);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCuisine = cuisineTypes[position];
            if (selectedCuisines.size() >= MAX_CUISINE_SELECTIONS) {
                showToast();
            } else if (!selectedCuisines.contains(selectedCuisine)) {
                selectedCuisines.add(selectedCuisine);
                addChip(selectedCuisine);
            }
            listPopupWindow.dismiss();
        });

        cuisineInputChip.setCheckedIconVisible(false);
        cuisineInputChip.setOnClickListener(v -> {
            if (selectedCuisines.size() >= MAX_CUISINE_SELECTIONS) {
                showToast();
            } else if (!listPopupWindow.isShowing()) {
                listPopupWindow.show();
            }
        });
    }

    private void addChip(String cuisine) {
        Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.SelectedCuisineChipStyle));
        chip.setText(cuisine);
        chip.setCloseIconVisible(true);

        ColorStateList colorStateList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.mode_darker));
        chip.setChipBackgroundColor(colorStateList);

        chip.setOnCloseIconClickListener(v -> {
            cuisineChipGroup.removeView(chip);
            selectedCuisines.remove(cuisine);
        });

        cuisineChipGroup.addView(chip, 1);
    }

    private void showToast() {
        Toast.makeText(requireContext(), "Maximum 5 cuisines allowed", Toast.LENGTH_SHORT).show();
    }

    private boolean validateDietSelection() {
        if (dietPrefEditText.getText().toString().trim().isEmpty()) {
            dietPrefTextInput.setError("Please select a diet type");
            return false;
        }
        dietPrefTextInput.setError(null);
        return true;
    }

    private void validateCuisineSelections() {
        if (selectedCuisines.isEmpty()) {
            cuisinePrefTextInput.setError("Please select at least one cuisine");
        } else {
            cuisinePrefTextInput.setError(null);
        }
    }

    public Set<String> getSelectedCuisines() {
        return new HashSet<>(selectedCuisines);
    }

    public void setSelectedCuisines(Set<String> cuisines) {
        selectedCuisines.clear();
        // Remove all chips except the input chip
        for (int i = cuisineChipGroup.getChildCount() - 2; i >= 0; i--) {
            cuisineChipGroup.removeViewAt(i);
        }

        for (String cuisine : cuisines) {
            selectedCuisines.add(cuisine);
            addChip(cuisine);
        }
    }

    public void clearCuisineSelections() {
        selectedCuisines.clear();
        while (cuisineChipGroup.getChildCount() > 1) {
            cuisineChipGroup.removeViewAt(1);
        }
    }

    public void completeRegistration() {
        if (!validateSelections()) return;

        loadingDialog.show();

        RegisterFragment3 fragment3 = (RegisterFragment3) getParentFragmentManager()
                .findFragmentByTag("FRAG3");

        if (fragment3 == null) {
            onFailure("Could not access user data");
            return;
        }

        UserData userData = fragment3.getUserData();
        Uri imageUri = fragment3.getSelectedImageUri();

        // Add Fragment 4 data
        userData.setDietPreference(dietPrefEditText.getText().toString());
        userData.setCuisinePreferencesFromSet(selectedCuisines);
        userData.setConditions(conditionsEditText.getText() != null ? conditionsEditText.getText().toString() : "");
        userData.setCookingMotivation(cookingMotivationSwitch.isChecked());
        userData.setHealthConscious(healthConsciousSwitch.isChecked());

        // Use DatabaseManager instead of FirebaseManager
        DatabaseManager.getInstance(requireContext()).createOrUpdateUser(
                userData,
                imageUri,
                new DatabaseManager.DatabaseCallback() {
                    @Override
                    public void onSuccess() {
                        RegisterFragment4.this.onSuccess();
                    }

                    @Override
                    public void onFailure(String error) {
                        RegisterFragment4.this.onFailure(error);
                    }
                }
        );
    }

    private boolean validateSelections() {
        // Optional validations as per requirements
        return true;
    }


    @Override
    public void onSuccess() {
        // Get the current UserData instance
        RegisterFragment3 fragment3 = (RegisterFragment3) getParentFragmentManager()
                .findFragmentByTag("FRAG3");

        if (fragment3 != null) {
            UserData userData = fragment3.getUserData();
            // Add Fragment 4 data to ensure complete profile data
            userData.setDietPreference(dietPrefEditText.getText().toString());
            userData.setCuisinePreferencesFromSet(selectedCuisines);
            userData.setConditions(conditionsEditText.getText() != null ? conditionsEditText.getText().toString() : "");
            userData.setCookingMotivation(cookingMotivationSwitch.isChecked());
            userData.setHealthConscious(healthConsciousSwitch.isChecked());

            // Initialize the profile cache before navigation
            ProfileManager.getInstance(requireContext()).initializeCache(new ProfileManager.ProfileCallback() {
                @Override
                public void onSuccess(UserData profileData) {
                    requireActivity().runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        // Navigate to MainActivity
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        // Even if cache initialization fails, proceed to MainActivity
                        // The cache will be initialized later
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    });
                }
            });
        } else {
            loadingDialog.dismiss();
            onFailure("Could not access user data");
        }
    }

    @Override
    public void onFailure(String error) {
        loadingDialog.dismiss();
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }
}