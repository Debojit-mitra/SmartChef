package com.bunny.ml.smartchef.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.bunny.ml.smartchef.models.UserData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

public class DatabaseManager {
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    private static DatabaseManager instance;
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int COMPRESSION_QUALITY = 85;
    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_IMAGES_PATH = "profile_images";

    private DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    public void createOrUpdateUser(UserData userData, @Nullable Uri profileImageUri, DatabaseCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No authenticated user found");
            return;
        }
        String userId = user.getUid();

        // If no profile image provided or URI is null, just save user data
        if (profileImageUri == null) {
            saveUserData(userId, userData, callback);
            return;
        }

        // Only attempt image upload if URI is provided
        uploadProfileImage(userId, profileImageUri)
                .addOnSuccessListener(uri -> {
                    userData.setProfilePhotoUrl(uri.toString());
                    saveUserData(userId, userData, callback);
                })
                .addOnFailureListener(e -> {
                    // Continue with user data save even if image upload fails
                    saveUserData(userId, userData, callback);
                });
    }

    private byte[] compressImage(Uri imageUri) throws FileNotFoundException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);

        int scaleFactor = Math.max(1, Math.max(
                options.outWidth / MAX_IMAGE_DIMENSION,
                options.outHeight / MAX_IMAGE_DIMENSION
        ));

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, COMPRESSION_QUALITY, baos);
            bitmap.recycle();
        }

        return baos.toByteArray();
    }


    private Task<Uri> uploadProfileImage(String userId, Uri imageUri) {
        String imageName = UUID.randomUUID().toString() + ".webp";
        StorageReference imageRef = storage.getReference()
                .child(PROFILE_IMAGES_PATH)
                .child(userId)
                .child(imageName);

        try {
            byte[] compressedImage = compressImage(imageUri);
            // Set correct content type for WebP
            return imageRef.putBytes(compressedImage)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.updateMetadata(new StorageMetadata.Builder()
                                .setContentType("image/webp")
                                .build());
                    })
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return imageRef.getDownloadUrl();
                    });
        } catch (FileNotFoundException e) {
            return Tasks.forException(e);
        }
    }

    private void saveUserData(String userId, UserData userData, DatabaseCallback callback) {
        userData.setUid(userId);
        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUserData(String userId, DocumentCallback<UserData> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    UserData userData = document.toObject(UserData.class);
                    callback.onSuccess(userData);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateUserField(String userId, String field, Object value, DatabaseCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(field, value)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteProfileImage(String imageUrl, DatabaseCallback callback) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            storage.getReferenceFromUrl(imageUrl)
                    .delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }
    }

    public interface DatabaseCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public interface DocumentCallback<T> {
        void onSuccess(T result);

        void onFailure(String error);
    }
}
