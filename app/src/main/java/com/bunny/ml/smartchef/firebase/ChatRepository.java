package com.bunny.ml.smartchef.firebase;

import com.bunny.ml.smartchef.models.Chat;
import com.bunny.ml.smartchef.models.ChatMessage;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class ChatRepository {
    private final FirebaseFirestore db;
    private final String userId;
    private static final String CHATS_COLLECTION = "chats";
    private static final String MESSAGES_COLLECTION = "messages";
    private static final String CHAT_METADATA_COLLECTION = "chat_metadata";
    private static final int MAX_CHATS = 5;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public String createNewDocument() {
        return db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document()
                .getId();
    }

    public void saveMessage(ChatMessage message) {
        if (message.getDocumentId() == null) {
            return; // Don't save messages without a document ID
        }

        // Save the message
        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(message.getDocumentId())
                .collection(MESSAGES_COLLECTION)
                .add(message);

        // Update chat metadata
        Chat chatMetadata = new Chat(
                message.getDocumentId(),    // Use document ID for Firebase
                message.getChatId(),        // Store server chat_id
                message.getContent(),
                Timestamp.now(),
                message.isAi()
        );

        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .document(message.getDocumentId())
                .set(chatMetadata)
                .addOnSuccessListener(aVoid -> cleanupOldChatsIfNeeded());
    }

    private void cleanupOldChatsIfNeeded() {
        getChatMetadata()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() > MAX_CHATS) {
                        // Get the oldest chat
                        DocumentSnapshot oldestChat = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);

                        // Delete the oldest chat using document ID
                        String oldestDocumentId = oldestChat.getString("documentId");
                        if (oldestDocumentId != null) {
                            deleteChat(oldestDocumentId);
                        }
                    }
                });
    }

    public void deleteChat(String documentId) {
        // Delete chat metadata
        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .document(documentId)
                .delete();

        // Delete all messages in the chat
        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(documentId)
                .collection(MESSAGES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().delete();
                    }
                });
    }

    public Query getMessagesQuery(String documentId) {
        return db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(documentId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public Task<QuerySnapshot> getChatMetadata() {
        return db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .limit(MAX_CHATS)
                .get();
    }

    // Optional: Add method to get server chatId by documentId if needed
    public Task<DocumentSnapshot> getServerChatId(String documentId) {
        return db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .document(documentId)
                .get();
    }

    public void updateMessageChatId(ChatMessage message) {
        if (message.getDocumentId() == null) return;

        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(message.getDocumentId())
                .collection(MESSAGES_COLLECTION)
                .whereEqualTo("timestamp", message.getTimestamp())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("chatId", message.getChatId());
                    }
                });
    }
}