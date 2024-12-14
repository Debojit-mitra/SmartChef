package com.bunny.ml.smartchef.models;

import androidx.annotation.Keep;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Keep
public class UserData {
    private String uid;
    private String name;
    private String phoneNumber;
    private String dateOfBirth;
    private String gender;
    private String dietPreference;
    private List<String> cuisinePreferences;
    private String conditions;
    private String profilePhotoUrl;
    private boolean healthConscious;
    private boolean cookingMotivation;

    // Empty constructor required for Firestore
    public UserData() {
        cuisinePreferences = new ArrayList<>();
    }

    // Builder class
    public static class Builder {
        private final UserData userData;

        public Builder() {
            userData = new UserData();
        }

        public Builder withUid(String uid) {
            userData.uid = uid;
            return this;
        }

        public Builder withName(String name) {
            userData.name = name;
            return this;
        }

        public Builder withPhoneNumber(String phoneNumber) {
            userData.phoneNumber = phoneNumber;
            return this;
        }

        public Builder withDateOfBirth(String dateOfBirth) {
            userData.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withGender(String gender) {
            userData.gender = gender;
            return this;
        }

        public Builder withDietPreference(String dietPreference) {
            userData.dietPreference = dietPreference;
            return this;
        }

        public Builder withCuisinePreferences(List<String> cuisinePreferences) {
            userData.cuisinePreferences = new ArrayList<>(cuisinePreferences);
            return this;
        }

        public Builder withProfilePhotoUrl(String profilePhotoUrl) {
            userData.profilePhotoUrl = profilePhotoUrl;
            return this;
        }

        public Builder withCookingMotivation(boolean cookingMotivation) {
            userData.cookingMotivation = cookingMotivation;
            return this;
        }

        public Builder withHealthConscious(boolean healthConscious) {
            userData.healthConscious = healthConscious;
            return this;
        }

        public Builder withConditions(String conditions) {
            userData.conditions = conditions;
            return this;
        }

        public UserData build() {
            return userData;
        }
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDietPreference() {
        return dietPreference;
    }

    public void setDietPreference(String dietPreference) {
        this.dietPreference = dietPreference;
    }

    public List<String> getCuisinePreferences() {
        return cuisinePreferences;
    }

    public boolean isHealthConscious() {
        return healthConscious;
    }

    public void setHealthConscious(boolean healthConscious) {
        this.healthConscious = healthConscious;
    }

    public void setCuisinePreferences(List<String> cuisinePreferences) {
        this.cuisinePreferences = cuisinePreferences;
    }

    @Exclude
    public void setCuisinePreferencesFromSet(Set<String> preferences) {
        this.cuisinePreferences = new ArrayList<>(preferences);
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public boolean isCookingMotivation() {
        return cookingMotivation;
    }

    public void setCookingMotivation(boolean cookingMotivation) {
        this.cookingMotivation = cookingMotivation;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    // Validation Methods
    @Exclude
    public boolean hasBasicProfile() {
        return name != null && !name.isEmpty() &&
                dateOfBirth != null && !dateOfBirth.isEmpty() &&
                gender != null && !gender.isEmpty();
    }

    @Exclude
    public boolean hasCompleteProfile() {
        return hasBasicProfile() &&
                dietPreference != null && !dietPreference.isEmpty() &&
                cuisinePreferences != null && !cuisinePreferences.isEmpty();
    }

}