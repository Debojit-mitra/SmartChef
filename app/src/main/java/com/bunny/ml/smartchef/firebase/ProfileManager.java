package com.bunny.ml.smartchef.firebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.models.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileManager {
    private static final String TAG = "ProfileManager";
    private static final String USERS_COLLECTION = "users";
    private static final long CACHE_FRESH_DURATION = TimeUnit.MINUTES.toMillis(5);

    private static ProfileManager instance;
    private final DatabaseManager databaseManager;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;
    private final Context context;

    private UserData cachedUserData;
    private boolean isRefreshing = false;
    private boolean isCacheInitialized = false;
    private long lastCacheUpdateTime = 0;


    // Interfaces
    public interface BaseCallback {
        void onFailure(String error);
    }

    public interface ProfileCallback extends BaseCallback {
        void onSuccess(UserData userData);
    }

    public interface ProfileCompletionCallback extends BaseCallback {
        void onComplete(boolean isComplete, boolean hasBasicProfile);

        void onError(String error); // This can be removed since we extend BaseCallback
    }

    public interface PreferencesCallback extends BaseCallback {
        void onSuccess(List<String> preferences);
    }

    // Constructor and Instance Management
    private ProfileManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseManager = DatabaseManager.getInstance(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized ProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager(context);
        }
        return instance;
    }

    // Authentication Check
    private boolean checkAuthentication(BaseCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onFailure("No authenticated user found");
            }
            return true;
        }
        return false;
    }

    public void initializeCache(@Nullable final ProfileCallback callback) {
        if (isRefreshing || isCacheInitialized) {
            if (callback != null) {
                callback.onSuccess(cachedUserData);
            }
            return;
        }

        loadUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserData userData) {
                updateCache(userData);
                if (callback != null) {
                    callback.onSuccess(userData);
                }
            }

            @Override
            public void onFailure(String error) {
                if (callback != null) {
                    callback.onFailure(error);
                }
            }
        });
    }

    // New method to check cache status
    public boolean isCacheFresh() {
        return System.currentTimeMillis() - lastCacheUpdateTime < CACHE_FRESH_DURATION;
    }


    private void updateCache(UserData userData) {
        cachedUserData = userData;
        isCacheInitialized = true;
        lastCacheUpdateTime = System.currentTimeMillis();
    }

    // Data Access Methods - Synchronous
    @Nullable
    public String getName() {
        return cachedUserData != null ? cachedUserData.getName() : null;
    }

    @Nullable
    public String getPhoneNumber() {
        return cachedUserData != null ? cachedUserData.getPhoneNumber() : null;
    }

    @Nullable
    public String getDateOfBirth() {
        return cachedUserData != null ? cachedUserData.getDateOfBirth() : null;
    }

    @Nullable
    public String getGender() {
        return cachedUserData != null ? cachedUserData.getGender() : null;
    }

    @Nullable
    public String getDietPreference() {
        return cachedUserData != null ? cachedUserData.getDietPreference() : null;
    }

    @Nullable
    public String getConditions() {
        return cachedUserData != null ? cachedUserData.getConditions() : null;
    }

    @NonNull
    public List<String> getCuisinePreferences() {
        return cachedUserData != null && cachedUserData.getCuisinePreferences() != null ?
                cachedUserData.getCuisinePreferences() : new ArrayList<>();
    }

    public boolean getCookingMotivation() {
        return cachedUserData != null && cachedUserData.isCookingMotivation();
    }

    // Profile Image Methods
    public void loadProfileImage(@NonNull CircleImageView imageView) {
        if (checkAuthentication(null)) {
            imageView.setImageResource(R.drawable.ic_account);
            return;
        }

        String imageUrl = cachedUserData != null ? cachedUserData.getProfilePhotoUrl() : null;
        if (imageUrl != null) {
            loadImageWithGlide(imageView, imageUrl);
            return;
        }

        loadProfileFromFirebase(imageView);
    }

    public void loadProfileImage(CircleImageView imageView, String imageUrl) {
        loadImageWithGlide(imageView, imageUrl);
    }

    private void loadProfileFromFirebase(CircleImageView imageView) {
        String uid = getCurrentUserId();
        databaseManager.getUserData(uid, new DatabaseManager.DocumentCallback<UserData>() {
            @Override
            public void onSuccess(UserData result) {
                if (result != null && result.getProfilePhotoUrl() != null) {
                    loadImageWithGlide(imageView, result.getProfilePhotoUrl());
                } else {
                    imageView.setImageResource(R.drawable.ic_account);
                }
            }

            @Override
            public void onFailure(String error) {
                imageView.setImageResource(R.drawable.ic_account);
                Log.e(TAG, "Error loading profile image: " + error);
            }
        });
    }

    private void loadImageWithGlide(CircleImageView imageView, String imageUrl) {
        Glide.with(context)
                .load(imageUrl).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_account)
                .error(R.drawable.ic_account)
                .into(imageView);
    }

    // Profile Data Methods
    public void loadUserProfile(ProfileCallback callback) {
        if (checkAuthentication(callback)) return;

        if (cachedUserData != null && isCacheFresh() && !isRefreshing) {
            callback.onSuccess(cachedUserData);
            return;
        }

        String uid = getCurrentUserId();
        databaseManager.getUserData(uid, new DatabaseManager.DocumentCallback<UserData>() {
            @Override
            public void onSuccess(UserData result) {
                updateCache(result);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading user profile: " + error);
                callback.onFailure(error);
            }
        });
    }

    public void updateProfile(UserData userData, @Nullable Uri profileImageUri, ProfileCallback callback) {
        if (checkAuthentication(callback)) return;

        if (profileImageUri == null && cachedUserData != null) {
            userData.setProfilePhotoUrl(cachedUserData.getProfilePhotoUrl());
        }

        userData.setUid(getCurrentUserId());
        databaseManager.createOrUpdateUser(userData, profileImageUri, new DatabaseManager.DatabaseCallback() {
            @Override
            public void onSuccess() {
                updateCache(userData);
                callback.onSuccess(userData);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error updating profile: " + error);
                callback.onFailure(error);
            }
        });
    }

    public void refreshUserProfile(@Nullable ProfileCallback callback) {
        if (checkAuthentication(callback)) return;

        if (isRefreshing) {
            if (callback != null) callback.onFailure("Refresh already in progress");
            return;
        }

        isRefreshing = true;
        String uid = getCurrentUserId();
        databaseManager.getUserData(uid, new DatabaseManager.DocumentCallback<UserData>() {
            @Override
            public void onSuccess(UserData result) {
                isRefreshing = false;
                cachedUserData = result;
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onFailure(String error) {
                isRefreshing = false;
                if (callback != null) callback.onFailure(error);
            }
        });
    }

    public void checkProfileCompletion(ProfileCompletionCallback callback) {
        if (checkAuthentication(callback::onError)) return;

        String uid = getCurrentUserId();
        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserData userData = documentSnapshot.toObject(UserData.class);
                    if (userData != null) {
                        callback.onComplete(userData.hasCompleteProfile(),
                                userData.hasBasicProfile());
                    } else {
                        callback.onComplete(false, false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking profile completion: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    private String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return user.getUid();
    }

    // Cache Management
    public void clearCache() {
        cachedUserData = null;
        isRefreshing = false;
        isCacheInitialized = false;
        lastCacheUpdateTime = 0;
    }

    public boolean isProfileCached() {
        return cachedUserData != null;
    }

    @Nullable
    public UserData getCachedUserData() {
        return cachedUserData;
    }

    // Authentication
    public void signOut() {
        clearCache();
        firebaseAuth.signOut();
    }
}