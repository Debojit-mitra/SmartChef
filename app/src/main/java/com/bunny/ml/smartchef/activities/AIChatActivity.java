package com.bunny.ml.smartchef.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.adapters.ChatAdapter;
import com.bunny.ml.smartchef.firebase.ChatRepository;
import com.bunny.ml.smartchef.models.ChatMessage;
import com.bunny.ml.smartchef.websocket.ChatWebSocket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity implements ChatWebSocket.ChatCallback {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private ChatWebSocket chatWebSocket;
    private ChatRepository chatRepository;
    private ListenerRegistration messagesListener;
    private String initialPrompt;
    private List<ChatMessage> messagesList = new ArrayList<>();
    private StringBuilder currentAiResponse;
    private ChatMessage currentAiMessage;
    private String documentId;
    private String chatId;
    private LottieAnimationView animationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aichat);

        // Get initial prompt from intent
        // Get IDs from intent
        documentId = getIntent().getStringExtra("documentId");
        chatId = getIntent().getStringExtra("chatId");
        initialPrompt = getIntent().getStringExtra("prompt");
        currentAiResponse = new StringBuilder();

        initializeViews();
        setupChatComponents();
        setupBackPressHandling();
        loadMessages();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        animationView = findViewById(R.id.animationView);
        ImageView backBtn = findViewById(R.id.backBtn);
        //TextView chatTitle = findViewById(R.id.chatTitle);

        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.playAnimation(), 500);
        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.pauseAnimation(), 2000);

        backBtn.setOnClickListener(v -> handleBack());
        //chatTitle.setText(initialPrompt);

        sendButton.setOnClickListener(v -> sendMessage());

        // Add keyboard visibility listener
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToBottom();
            }
        });

        // Optional: Add soft keyboard listener
        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                // Keyboard opened
                scrollToBottom();
            }
        });
    }

    private void setupChatComponents() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        chatRepository = new ChatRepository();
        // Create new document if we don't have one
        if (documentId == null) {
            documentId = chatRepository.createNewDocument();
        }

        chatWebSocket = new ChatWebSocket(this, AIChatActivity.this, chatId);
        chatWebSocket.connect();
    }

    private void loadMessages() {
        if (documentId != null) {
            messagesListener = chatRepository.getMessagesQuery(documentId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }

                        if (value != null) {
                            messagesList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                ChatMessage message = doc.toObject(ChatMessage.class);
                                if (message != null) {
                                    // Ensure both IDs are set correctly
                                    message.setDocumentId(documentId);
                                    message.setChatId(chatId);
                                    messagesList.add(message);
                                }
                            }
                            animationView.setVisibility(View.GONE);
                            chatAdapter.setMessages(new ArrayList<>(messagesList));
                            scrollToBottom();
                        }
                    });
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        if (animationView.getVisibility() == View.VISIBLE){
            animationView.setVisibility(View.GONE);
        }
        ChatMessage userMessage = new ChatMessage(
                messageText,
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                false,
                chatId,      // API chat ID
                documentId   // Firebase document ID
        );

        chatAdapter.addMessage(userMessage);
        scrollToBottom();
        chatWebSocket.sendMessage(messageText);
        chatRepository.saveMessage(userMessage);
        messageInput.setText("");
    }

    // WebSocket callback implementations
    @Override
    public void onConnected() {
        // Optional: Show connection status
    }

    @Override
    public void onMessageReceived(String content) {
        runOnUiThread(() -> {
            chatAdapter.hideTypingIndicator();

            if (currentAiMessage == null) {
                currentAiMessage = new ChatMessage(
                        content,
                        "AI",
                        true,
                        chatId,      // API chat ID
                        documentId
                );
                currentAiResponse = new StringBuilder(content);
                chatAdapter.addMessage(currentAiMessage);
            } else {
                currentAiResponse.append(content);
                currentAiMessage.setContent(currentAiResponse.toString());
                chatAdapter.updateLastMessage(currentAiResponse.toString());
            }
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        });
    }

    @Override
    public void onChatIdReceived(String chatId) {
        this.chatId = chatId;  // Update activity's chatId
    }

    @Override
    public void onResponseComplete(String chatId, double processingTime) {
        if (currentAiMessage != null) {
            // Save the complete message to Firebase
            chatRepository.saveMessage(currentAiMessage);
            // Reset for next stream
            currentAiMessage = null;
            currentAiResponse = new StringBuilder();
        }
    }


    @Override
    public void onError(String error) {
        // Reset streaming state on error
        currentAiMessage = null;
        currentAiResponse = new StringBuilder();
    }

    @Override
    public void onDisconnected() {
        // Handle disconnection - maybe show reconnection UI
    }

    private void setupBackPressHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void handleBack() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatWebSocket != null) {
            chatWebSocket.disconnect();
        }
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> {
            if (chatAdapter.getItemCount() > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }
}