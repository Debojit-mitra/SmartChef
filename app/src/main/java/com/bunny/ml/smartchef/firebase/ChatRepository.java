package com.bunny.ml.smartchef.firebase;

import com.bunny.ml.smartchef.models.Chat;
import com.bunny.ml.smartchef.models.ChatMessage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChatRepository {
    private final FirebaseFirestore db;
    private final String userId;
    private static final String CHATS_COLLECTION = "chats";
    private static final String MESSAGES_COLLECTION = "messages";
    private static final String CHAT_METADATA_COLLECTION = "chat_metadata";
    private static final int MAX_CHATS = 5;
    private static final int MESSAGES_PER_PAGE = 20;
    private static final int MAX_STARRED_CHATS = 5;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        this.userId = firebaseAuth.getUid();
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
            return;
        }

        // Save the message
        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(message.getDocumentId())
                .collection(MESSAGES_COLLECTION)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // After message is saved, update chat metadata
                    Chat chatMetadata = new Chat(
                            message.getDocumentId(),
                            message.getChatId(),
                            message.getContent(),
                            Timestamp.now(),
                            message.isAi(),
                            false  // New chats start unstarred
                    );

                    db.collection(CHATS_COLLECTION)
                            .document(userId)
                            .collection(CHAT_METADATA_COLLECTION)
                            .document(message.getDocumentId())
                            .set(chatMetadata)
                            .addOnSuccessListener(aVoid -> cleanupOldChatsIfNeeded());
                });
    }


    private void cleanupOldChatsIfNeeded() {
        // Get only unstarred chats ordered by timestamp
        db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .whereEqualTo("starred", false)
                .orderBy("lastMessageTime", Query.Direction.ASCENDING)  // Get oldest first
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Chat> unstarredChats = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setDocumentId(doc.getId());
                            unstarredChats.add(chat);
                        }
                    }

                    if (unstarredChats.size() > MAX_CHATS) {
                        // Calculate how many chats need to be deleted
                        int chatsToDelete = unstarredChats.size() - MAX_CHATS;

                        // Delete the oldest chats
                        for (int i = 0; i < chatsToDelete; i++) {
                            Chat oldestChat = unstarredChats.get(i);
                            if (oldestChat.getDocumentId() != null) {
                                deleteChat(oldestChat.getDocumentId());
                            }
                        }
                    }
                });
    }

    public Task<Void> toggleStarredStatus(String documentId) {
        DocumentReference chatRef = db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .document(documentId);

        // First get the current starred status
        return chatRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }

            DocumentSnapshot chatDoc = task.getResult();
            boolean currentStarred = Boolean.TRUE.equals(chatDoc.getBoolean("starred"));

            if (currentStarred) {
                // If currently starred, simply unstar it
                return chatRef.update("starred", false);
            } else {
                // If not starred, check if we can star it
                return db.collection(CHATS_COLLECTION)
                        .document(userId)
                        .collection(CHAT_METADATA_COLLECTION)
                        .whereEqualTo("starred", true)
                        .get()
                        .continueWithTask(queryTask -> {
                            if (!queryTask.isSuccessful()) {
                                throw Objects.requireNonNull(queryTask.getException());
                            }

                            if (queryTask.getResult().size() >= MAX_STARRED_CHATS) {
                                throw new FirebaseFirestoreException(
                                        "Maximum number of starred chats reached",
                                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                                );
                            }

                            return chatRef.update("starred", true);
                        });
            }
        });
    }


    public Task<QuerySnapshot> getMessagesPage(String documentId, DocumentSnapshot startAfter) {
        Query query = db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(MESSAGES_COLLECTION)
                .document(documentId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(MESSAGES_PER_PAGE);

        if (startAfter != null) {
            query = query.endBefore(startAfter);
        }

        return query.get();
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

    public Task<List<Chat>> getChatMetadata() {
        // Get all starred chats
        Task<QuerySnapshot> starredChatsTask = db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .whereEqualTo("starred", true)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get();

        // Get recent unstarred chats
        Task<QuerySnapshot> recentChatsTask = db.collection(CHATS_COLLECTION)
                .document(userId)
                .collection(CHAT_METADATA_COLLECTION)
                .whereEqualTo("starred", false)  // Changed from whereNotEqualTo
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)  // Removed the starred ordering
                .limit(MAX_CHATS)
                .get();

        return Tasks.whenAllSuccess(starredChatsTask, recentChatsTask)
                .continueWith(task -> {
                    QuerySnapshot starredChats = (QuerySnapshot) task.getResult().get(0);
                    QuerySnapshot recentChats = (QuerySnapshot) task.getResult().get(1);

                    List<Chat> allChats = new ArrayList<>();

                    // Add starred chats
                    for (DocumentSnapshot doc : starredChats.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setDocumentId(doc.getId());
                            allChats.add(chat);
                        }
                    }

                    // Add recent unstarred chats
                    for (DocumentSnapshot doc : recentChats.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setDocumentId(doc.getId());
                            allChats.add(chat);
                        }
                    }

                    Collections.sort(allChats, (a, b) ->
                            b.getLastMessageTime().compareTo(a.getLastMessageTime()));

                    return allChats;
                });
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