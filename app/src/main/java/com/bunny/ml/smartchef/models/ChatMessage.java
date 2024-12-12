package com.bunny.ml.smartchef.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ChatMessage {
    private String content;
    @ServerTimestamp
    private Date timestamp;
    private String senderId;
    private boolean isAi;
    private String chatId;      // API chat ID
    private String documentId;

    public ChatMessage() {
        // Required empty constructor for Firebase
        this.timestamp = new Date();
    }

    public ChatMessage(String content, String senderId, boolean isAi, String chatId, String documentId) {
        this.content = content;
        this.senderId = senderId;
        this.isAi = isAi;
        this.chatId = chatId;
        this.documentId = documentId;
        this.timestamp = new Date();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public boolean isAi() {
        return isAi;
    }

    public void setAi(boolean ai) {
        isAi = ai;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}