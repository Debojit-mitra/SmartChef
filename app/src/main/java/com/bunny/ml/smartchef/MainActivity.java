package com.bunny.ml.smartchef;

import static com.bunny.ml.smartchef.activities.SettingsActivity.UPDATE_CHECK_INTERVAL;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;

import com.airbnb.lottie.LottieAnimationView;
import com.bunny.ml.smartchef.activities.AIChatActivity;
import com.bunny.ml.smartchef.activities.ProfileActivity;
import com.bunny.ml.smartchef.activities.SettingsActivity;
import com.bunny.ml.smartchef.activities.SignInSignUpActivity;
import com.bunny.ml.smartchef.activities.TryModelActivity;
import com.bunny.ml.smartchef.adapters.ChatHistoryAdapter;
import com.bunny.ml.smartchef.firebase.ChatRepository;
import com.bunny.ml.smartchef.firebase.ProfileManager;
import com.bunny.ml.smartchef.models.Chat;
import com.bunny.ml.smartchef.utils.AppUpdater;
import com.bunny.ml.smartchef.utils.CookingMotivationManager;
import com.bunny.ml.smartchef.utils.CustomAlertDialog;
import com.bunny.ml.smartchef.utils.PermissionManager;
import com.bunny.ml.smartchef.utils.SwipeCallback;
import com.bunny.ml.smartchef.utils.UpdateWorker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements ProfileActivity.MainActivityCallback {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private ProfileManager profileManager;
    private CircleImageView profile_image;
    private LottieAnimationView animationView;
    private TextView history_textview;
    private RecyclerView chatHistoryRecyclerView;
    private ChatHistoryAdapter chatHistoryAdapter;
    private ChatRepository chatRepository;
    private AppUpdater appUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize views and base functionality
        initializeViews();
        setProfileImage();
        loadChatHistory();

        // Check notification permission and handle update system
        checkNotificationPermission();

        // Initialize appUpdater after permission check
        appUpdater = new AppUpdater(MainActivity.this);

        // Check if opened from notification
        if (getIntent().getBooleanExtra("show_update", false)) {
            appUpdater.checkForUpdatesFromNotification();
        } else {
            // Always check for updates when app opens
            appUpdater.checkForUpdates(false);
        }

        // Only check for updates if permission is granted and auto-update is enabled
        if (PermissionManager.isAutoUpdateEnabled(this) &&
                PermissionManager.hasNotificationPermission(this)) {
            // Schedule periodic updates
            scheduleUpdateChecks();

        }
    }

    private void initializeViews() {
        profile_image = findViewById(R.id.profile_image);
        profileManager = ProfileManager.getInstance(MainActivity.this);
        TextView hello_user_textview = findViewById(R.id.hello_user_textview);
        history_textview = findViewById(R.id.history_textview);
        LinearLayout layout_try_model_btn = findViewById(R.id.layout_try_model_btn);
        LinearLayout layout_chat_btn = findViewById(R.id.layout_chat_btn);
        animationView = findViewById(R.id.animationView);

        profile_image.setOnClickListener(view12 -> setUpBottomSheet());
        String helloUser = getString(R.string.hello_user) + " " + profileManager.getName();
        hello_user_textview.setText(helloUser);

        chatHistoryRecyclerView = findViewById(R.id.chatHistoryRecyclerView);
        chatHistoryAdapter = new ChatHistoryAdapter(this);
        chatRepository = new ChatRepository();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatHistoryRecyclerView.setLayoutManager(layoutManager);
        chatHistoryRecyclerView.setAdapter(chatHistoryAdapter);

        setupSwipeDelete();

        layout_try_model_btn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TryModelActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        layout_chat_btn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.playAnimation(), 500);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionManager.hasNotificationPermission(this) &&
                    PermissionManager.shouldAskNotificationPermission(this)) {
                showNotificationPermissionDialog();
            }
        }
    }

    private void showNotificationPermissionDialog() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> new CustomAlertDialog(MainActivity.this)
                .setDialogTitle("Notification Permission")
                .setMessage("SmartChef needs notification permission for two reasons:\n\n1. For sending cooking motivations.\n2. To keep you updated with the latest app versions.\n\nWould you like to enable notifications?")
                .setPositiveButton("Enable", () -> {
                    PermissionManager.setNotificationPermissionAsked(this, true);
                    requestNotificationPermission();
                })
                .setNegativeButton("No Thanks", () -> {
                    turnOffMotivation();
                    PermissionManager.setNotificationPermissionAsked(this, true);
                    PermissionManager.setNotificationPermissionDenied(this, true);
                    PermissionManager.setAutoUpdateEnabled(MainActivity.this, false);
                    // Cancel any scheduled update checks
                    androidx.work.WorkManager.getInstance(MainActivity.this)
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
                if (profileManager.getCookingMotivation()){
                    CookingMotivationManager cookingMotivationManager = new CookingMotivationManager(this);
                    cookingMotivationManager.scheduleDailyMotivation();
                }
                PermissionManager.setNotificationPermissionDenied(this, false);
                PermissionManager.setAutoUpdateEnabled(this, true);
                if (appUpdater != null) {
                    appUpdater.checkForUpdates(false);
                }
            } else {
                // Permission denied
                turnOffMotivation();
                PermissionManager.setNotificationPermissionDenied(this, true);
                PermissionManager.setAutoUpdateEnabled(this, false);
            }
        }
    }

    private void turnOffMotivation() {
        if (profileManager.getCookingMotivation()){
            profileManager.setCookingMotivation(false, new ProfileManager.BaseCallback() {
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, error);
                }
            });
        }
    }

    private void scheduleUpdateChecks() {
        androidx.work.PeriodicWorkRequest updateWorkRequest =
                new androidx.work.PeriodicWorkRequest.Builder(
                        UpdateWorker.class,
                        UPDATE_CHECK_INTERVAL,
                        java.util.concurrent.TimeUnit.HOURS
                )
                        .build();

        androidx.work.WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "update_check",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        updateWorkRequest
                );
    }

    private void loadChatHistory() {
        chatRepository.getChatMetadata()
                .addOnSuccessListener(chats -> {
                    // Update history text view based on chat availability
                    if (chats.isEmpty()) {
                        history_textview.setText(getString(R.string.no_history));
                        chatHistoryRecyclerView.setVisibility(View.GONE);
                    } else {
                        history_textview.setText(getString(R.string.history));
                        chatHistoryRecyclerView.setVisibility(View.VISIBLE);
                        chatHistoryAdapter.setChatList(chats);
                    }
                })
                .addOnFailureListener(e -> {
                    history_textview.setText(getString(R.string.no_history));
                    chatHistoryRecyclerView.setVisibility(View.GONE);
                });
    }

    public void setProfileImage() {
        profileManager.loadProfileImage(profile_image);
    }

    @Override
    public void refreshProfileImage(String profileImageUrl) {
        profileManager.loadProfileImage(profile_image, profileImageUrl);
    }

    private void logoutUser(BottomSheetDialog bottomSheetDialog) {
        bottomSheetDialog.cancel();
        CustomAlertDialog customAlertDialog = new CustomAlertDialog(MainActivity.this);
        customAlertDialog
                .setDialogTitle("Logout")
                .setMessage("Your are about to be logged out!")
                .setTitleAlignment(View.TEXT_ALIGNMENT_TEXT_START)
                .setPositiveButton("Yes", () -> {
                    profileManager.signOut();
                    Intent intent = new Intent(MainActivity.this, SignInSignUpActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                })
                .setNegativeButton("Cancel", customAlertDialog::dismiss)
                .show();
    }

    private void setUpBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.CustomBottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_profile_menu, bottomSheetDialog.findViewById(R.id.relative_main));
        bottomSheetDialog.setContentView(bottomSheetView);
        LinearLayout layout_settings, layout_profile;
        CircleImageView profile_image = bottomSheetView.findViewById(R.id.profile_image);
        profileManager.loadProfileImage(profile_image);
        layout_settings = bottomSheetView.findViewById(R.id.layout_settings);
        layout_profile = bottomSheetView.findViewById(R.id.layout_profile);
        TextView logout_btn = bottomSheetView.findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(view -> logoutUser(bottomSheetDialog));
        layout_settings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            bottomSheetDialog.cancel();
        });
        layout_profile.setOnClickListener(view -> {
            ProfileActivity.setCallback(MainActivity.this);
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            bottomSheetDialog.cancel();
        });
        bottomSheetDialog.show();
    }

    private void setupSwipeDelete() {
        // Create configuration for left swipe delete
        SwipeCallback.SwipeConfig deleteConfig = new SwipeCallback.SwipeConfig(
                this,
                getColor(R.color.delete), // Red color for delete
                R.drawable.ic_round_delete,
                0, // No alternate icon for delete
                24 // Icon size in dp
        );

        // Create configuration for right swipe star/unstar
        SwipeCallback.SwipeConfig starConfig = new SwipeCallback.SwipeConfig(
                this,
                getColor(R.color.star), // Gold color for star
                R.drawable.ic_round_star_2, // Star icon
                R.drawable.ic_unstar, // Unstar icon
                24 // Icon size in dp
        );

        // Create and attach the swipe callback
        SwipeCallback swipeCallback = new SwipeCallback(
                this,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                deleteConfig,
                starConfig
        ) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                final Chat currentChat = chatHistoryAdapter.getChatAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Delete chat
                    CustomAlertDialog dialog = new CustomAlertDialog(MainActivity.this);
                    dialog.setIcon(Objects.requireNonNull(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_round_delete)))
                            .setIconTint(ContextCompat.getColor(MainActivity.this, R.color.mode_inverse))
                            .setMessage("Are you sure you want to delete this chat?")
                            .setPositiveButton("Yes", () -> {
                                chatRepository.deleteChat(currentChat.getDocumentId());
                                chatHistoryAdapter.removeChat(position);
                                loadChatHistory();
                            })
                            .setNegativeButton("No", () -> {
                                dialog.dismiss();
                                chatHistoryAdapter.notifyItemChanged(position);
                            })
                            .show();

                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (currentChat.isStarred()) {
                        // Show confirmation dialog for unstarring
                        CustomAlertDialog dialog = new CustomAlertDialog(MainActivity.this);
                        dialog.setIcon(Objects.requireNonNull(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_unstar)))
                                .setIconTint(ContextCompat.getColor(MainActivity.this, R.color.star))
                                .setMessage("Are you sure you want to unstar this chat?")
                                .setPositiveButton("Yes", () -> {
                                    performStarToggle(currentChat, position);
                                })
                                .setNegativeButton("No", () -> {
                                    dialog.dismiss();
                                    chatHistoryAdapter.notifyItemChanged(position);
                                })
                                .show();
                    } else {
                        // Directly star the chat without confirmation
                        performStarToggle(currentChat, position);
                    }
                }
            }

            private void performStarToggle(Chat chat, int position) {
                chatRepository.toggleStarredStatus(chat.getDocumentId())
                        .addOnSuccessListener(aVoid -> {
                            // Update UI only on success
                            chat.setStarred(!chat.isStarred());
                            chatHistoryAdapter.notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> {
                            // Reset the swipe state
                            chatHistoryAdapter.notifyItemChanged(position);

                            if (e instanceof FirebaseFirestoreException &&
                                    ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                                Toast.makeText(MainActivity.this,
                                        "You can only star up to 3 chats",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Failed to update starred status",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            protected boolean shouldUseAlternateIcon(RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Chat chat = chatHistoryAdapter.getChatAt(position);
                    return chat.isStarred(); // Use alternate icon (outline) if already starred
                }
                return false;
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(chatHistoryRecyclerView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppUpdater.REQUEST_INSTALL_PACKAGES) {
            appUpdater.onActivityResult(requestCode, resultCode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProfileActivity.setCallback(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatHistory(); // Refresh chat history when returning to MainActivity
    }
}