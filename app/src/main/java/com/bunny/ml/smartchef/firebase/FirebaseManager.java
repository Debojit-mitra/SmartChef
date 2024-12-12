
package com.bunny.ml.smartchef.firebase;

import android.net.Uri;
import android.util.Log;

import com.bunny.ml.smartchef.models.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Objects;

public class FirebaseManager {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private static FirebaseManager instance;
    private static final String TAG = "FirebaseManager";

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void uploadUserData(UserData userData, Uri profileImageUri, FirebaseCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // If there's no profile image, just save the user data
        if (profileImageUri == null) {
            saveUserToFirestore(uid, userData, callback);
            return;
        }

        // If there is a profile image, upload it first
        StorageReference profileRef = storage.getReference()
                .child("profile_images")
                .child(uid + ".jpg");

        profileRef.putFile(profileImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return profileRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    userData.setProfilePhotoUrl(uri.toString());
                    saveUserToFirestore(uid, userData, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Profile image upload failed", e);
                    // Even if image upload fails, still save user data
                    saveUserToFirestore(uid, userData, callback);
                });
    }

    private void saveUserToFirestore(String uid, UserData userData, FirebaseCallback callback) {
        try {
            // Ensure cuisinePreferences is a List before saving
            if (userData.getCuisinePreferences() == null) {
                userData.setCuisinePreferences(new ArrayList<>());
            }

            userData.setUid(uid);
            db.collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User data saved successfully");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving user data", e);
                        callback.onFailure(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing user data", e);
            callback.onFailure("Error preparing user data: " + e.getMessage());
        }
    }

    public interface FirebaseCallback {
        void onSuccess();

        void onFailure(String error);
    }
}
