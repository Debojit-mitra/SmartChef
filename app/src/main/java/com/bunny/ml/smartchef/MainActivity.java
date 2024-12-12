package com.bunny.ml.smartchef;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements ProfileActivity.MainActivityCallback {

    private FirebaseAuth auth;
    private ProfileManager profileManager;
    private CircleImageView profile_image;
    private TextView hello_user_textview;
    private LinearLayout layout_try_model_btn, layout_chat_btn;
    private LottieAnimationView animationView;
    private TextView history_textview;
    private RecyclerView chatHistoryRecyclerView;
    private ChatHistoryAdapter chatHistoryAdapter;
    private ChatRepository chatRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        initializeViews();
        setProfileImage();
        loadChatHistory();

    }

    private void initializeViews() {
        profile_image = findViewById(R.id.profile_image);
        profileManager = ProfileManager.getInstance(MainActivity.this);
        hello_user_textview = findViewById(R.id.hello_user_textview);
        history_textview = findViewById(R.id.history_textview);
        layout_try_model_btn = findViewById(R.id.layout_try_model_btn);
        layout_chat_btn = findViewById(R.id.layout_chat_btn);
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

        layout_try_model_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TryModelActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        layout_chat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.playAnimation(),500);
    }

    private void loadChatHistory() {
        chatRepository.getChatMetadata()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Chat> chats = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Chat chat = document.toObject(Chat.class);
                        if (chat != null) {
                            // Ensure document ID is set from the Firestore document
                            chat.setDocumentId(document.getId());
                            chats.add(chat);
                        }
                    }
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

    private void logoutUser() {
        profileManager.signOut();
        Intent intent = new Intent(MainActivity.this, SignInSignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
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
        logout_btn.setOnClickListener(view -> logoutUser());
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