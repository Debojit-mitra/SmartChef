package com.bunny.ml.smartchef.models;

import com.google.firebase.Timestamp;

public class Chat {
    private String documentId;    // Firebase document ID
    private String chatId;
    private String lastMessage;
    private Timestamp lastMessageTime;
    private boolean isAiLastMessage;

    public Chat() {
        // Required empty constructor for Firestore
    }

    public Chat(String documentId, String chatId, String lastMessage, Timestamp lastMessageTime, boolean isAiLastMessage) {
        this.documentId = documentId;
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.isAiLastMessage = isAiLastMessage;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isAiLastMessage() {
        return isAiLastMessage;
    }

    public void setAiLastMessage(boolean aiLastMessage) {
        isAiLastMessage = aiLastMessage;
    }
}
