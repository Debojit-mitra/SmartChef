package com.bunny.ml.smartchef.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.adapters.ChatAdapter;
import com.bunny.ml.smartchef.adapters.SuggestionAdapter;
import com.bunny.ml.smartchef.firebase.ChatRepository;
import com.bunny.ml.smartchef.models.ChatMessage;
import com.bunny.ml.smartchef.utils.Extras;
import com.bunny.ml.smartchef.utils.RecipeSuggestionsManager;
import com.bunny.ml.smartchef.websocket.ChatWebSocket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity implements ChatWebSocket.ChatCallback {
    private static final String TAG = "AIChatActivity";
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private ChatWebSocket chatWebSocket;
    private ChatRepository chatRepository;
    private ListenerRegistration messagesListener;
    private List<ChatMessage> messagesList = new ArrayList<>();
    private StringBuilder currentAiResponse;
    private ChatMessage currentAiMessage;
    private String documentId;
    private String chatId;
    private LottieAnimationView animationView;
    private LinearLayout layout_greet_user;
    private FirebaseAuth firebaseAuth;
    private boolean hasOldMessages = false;
    private boolean isServerConnected = false;

    //pagination
    private DocumentSnapshot lastVisibleMessage;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;

    //suggestions
    private RecyclerView suggestionsRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private LinearLayout layoutSuggestions;
    private RecipeSuggestionsManager suggestionsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aichat);

        // Get initial prompt from intent
        // Get IDs from intent
        documentId = getIntent().getStringExtra("documentId");
        chatId = getIntent().getStringExtra("chatId");
        currentAiResponse = new StringBuilder();

        initializeViews();
        setupChatComponents();
        setupBackPressHandling();
        loadMessages();
        setupPagination();
    }

    private void initializeViews() {
        firebaseAuth = FirebaseAuth.getInstance();
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        animationView = findViewById(R.id.animationView);
        ImageView backBtn = findViewById(R.id.backBtn);
        layout_greet_user = findViewById(R.id.layout_greet_user);
        layoutSuggestions = findViewById(R.id.layout_suggestions);
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);

        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.playAnimation(), 500);

        backBtn.setOnClickListener(v -> handleBack());

        sendButton.setOnClickListener(v -> sendMessage());

        setupSuggestionAdapter();

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

        sendButton.setOnClickListener(v -> {
            if (isServerConnected) {
                sendMessage();
            } else {
                // Attempt to reconnect
                if (chatWebSocket != null) {
                    chatWebSocket.connect();
                }

                // Show toast to inform user
                Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show();
                return;
            }
        });
    }

    private void setupSuggestionAdapter() {
        suggestionAdapter = new SuggestionAdapter();
        int spanCount = Extras.getSpanCount(AIChatActivity.this, 3);
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount, RecyclerView.HORIZONTAL, false);
        suggestionsRecyclerView.setLayoutManager(layoutManager);
        suggestionsRecyclerView.setAdapter(suggestionAdapter);

        suggestionAdapter.setOnSuggestionClickListener(suggestion -> {
            messageInput.setText(suggestion);

            if (isServerConnected) {
                sendMessage();
                layoutSuggestions.setVisibility(View.GONE);
            } else {
                // Show connection toast and attempt to reconnect
                Toast.makeText(this, getString(R.string.connecting_to_server), Toast.LENGTH_SHORT).show();
                if (chatWebSocket != null) {
                    chatWebSocket.connect();
                }
            }
        });

        suggestionsManager = new RecipeSuggestionsManager(this);
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

    private void loadSuggestions() {
        suggestionsManager.getCurrentTimeSuggestions(new RecipeSuggestionsManager.SuggestionsCallback() {
            @Override
            public void onSuggestionsLoaded(List<String> suggestions) {
                runOnUiThread(() -> {
                    suggestionAdapter.setSuggestions(suggestions);
                    layoutSuggestions.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Hide suggestions on error
                    layoutSuggestions.setVisibility(View.GONE);
                });
            }
        });
    }

    private void setupPagination() {
        chatAdapter.setLoadMoreListener(() -> {
            if (!isLoading && hasMoreMessages) {
                loadMoreMessages();
            }
        });

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    // Check if we've scrolled near the top
                    if (firstVisibleItem <= 3 && !isLoading && hasMoreMessages) {
                        loadMoreMessages();
                    }
                }
            }
        });
    }

    private void loadMoreMessages() {
        if (isLoading || !hasMoreMessages) return;

        isLoading = true;
        chatAdapter.setLoading(true);

        chatRepository.getMessagesPage(documentId, lastVisibleMessage)
                .addOnSuccessListener(querySnapshot -> {
                    List<ChatMessage> newMessages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        if (message != null) {
                            message.setDocumentId(documentId);
                            message.setChatId(chatId);
                            newMessages.add(message);
                        }
                    }

                    if (!querySnapshot.isEmpty()) {
                        lastVisibleMessage = querySnapshot.getDocuments()
                                .get(0);  // Changed to get first document since we're using limitToLast
                        hasMoreMessages = querySnapshot.size() >= 20;
                    } else {
                        hasMoreMessages = false;
                    }

                    chatAdapter.setLoading(false);
                    if (!newMessages.isEmpty()) {
                        chatAdapter.addMessages(newMessages, false);
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    chatAdapter.setLoading(false);
                    Toast.makeText(this, "Error loading more messages", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadMessages() {
        if (documentId != null && !isLoading) {
            isLoading = true;
            // Pre-allocate the ArrayList with expected capacity
            List<ChatMessage> newMessages = new ArrayList<>(20); // Since max page size is 20

            chatRepository.getMessagesPage(documentId, null)
                    .addOnSuccessListener(querySnapshot -> {
                        // Process documents directly without intermediate steps
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                message.setDocumentId(documentId);
                                message.setChatId(chatId);
                                newMessages.add(message);
                            }
                        }

                        // Update pagination state
                        if (!querySnapshot.isEmpty()) {
                            lastVisibleMessage = querySnapshot.getDocuments()
                                    .get(querySnapshot.size() - 1);
                            hasMoreMessages = querySnapshot.size() >= 20;
                        } else {
                            hasMoreMessages = false;
                        }

                        // Batch all UI updates together
                        runOnUiThread(() -> {
                            chatAdapter.addMessages(newMessages, true);
                            isLoading = false;

                            if (!newMessages.isEmpty()) {
                                scrollToBottom();
                                if (!hasOldMessages) {
                                    hasOldMessages = true;
                                }
                            } else {
                                layout_greet_user.setVisibility(View.VISIBLE);
                                loadSuggestions();
                                messageInput.requestFocus();
                                Extras.showKeyboard(AIChatActivity.this, messageInput);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        isLoading = false;
                        runOnUiThread(() ->
                                Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
                        );
                    });
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        if (layout_greet_user.getVisibility() == View.VISIBLE) {
            layout_greet_user.setVisibility(View.GONE);
        }

        if (layoutSuggestions.getVisibility() == View.VISIBLE) {
            layoutSuggestions.setVisibility(View.GONE);
        }

        // Add the user message to UI immediately
        ChatMessage userMessage = new ChatMessage(
                messageText,
                firebaseAuth.getUid(),
                false,
                chatId,
                documentId
        );

        chatAdapter.addMessage(userMessage);
        scrollToBottom();

        // Save message to Firebase
        chatRepository.saveMessage(userMessage);
        messageInput.setText("");

        // Attempt to send message, connection will be restored if needed
        chatWebSocket.sendMessage(messageText);
    }


    // WebSocket callback implementations
    @Override
    public void onConnected() {
        isServerConnected = true;
    }

    @Override
    public void onMessageReceived(String content) {
        runOnUiThread(() -> {
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
        currentAiMessage = null;
        currentAiResponse = new StringBuilder();
        isServerConnected = false;
        Log.e(TAG, error);

        runOnUiThread(() -> {
            // Only show error if user was actively trying to send a message
            if (!messageInput.isEnabled()) {
                messageInput.setEnabled(true);
                sendButton.setEnabled(true);
                messageInput.setHint(getString(R.string.type_your_message));
                Toast.makeText(this, "Connection error: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }

    @Override
    public void onDisconnected() {
        isServerConnected = false;
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