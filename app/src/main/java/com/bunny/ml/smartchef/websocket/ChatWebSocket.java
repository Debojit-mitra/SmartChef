package com.bunny.ml.smartchef.websocket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bunny.ml.smartchef.firebase.ProfileManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatWebSocket {
    private static final String BASE_URL = "wss://smartchefapi.zerotwobunny.fun/ws/chat";
    private WebSocket webSocket;
    private String chatId;
    private final ChatCallback callback;
    private final Handler mainHandler;
    private boolean isConnected = false;
    private static final int RECONNECT_DELAY = 5000; // 5 seconds
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;
    private ProfileManager profileManager;


    public interface ChatCallback {
        void onConnected();

        void onMessageReceived(String content);

        void onError(String error);

        void onDisconnected();

        void onResponseComplete(String chatId, double processingTime);
        void onChatIdReceived(String chatId);
    }

    public ChatWebSocket(ChatCallback callback, Context context, String existingChatId) {
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.chatId = existingChatId;
        profileManager = ProfileManager.getInstance(context);
    }

    public void connect() {
        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Add chatId to URL if it exists
        String url = BASE_URL;
        if (chatId != null && !chatId.isEmpty()) {
            url += "?chat_id=" + chatId;
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("ChatWebSocket", "WebSocket connected successfully");
                mainHandler.post(() -> {
                    isConnected = true;
                    retryCount = 0;
                    callback.onConnected();
                });
            }


            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject message = new JSONObject(text);
                    String type = message.getString("type");

                    switch (type) {
                        case "chat_id":
                            if (chatId == null || chatId.isEmpty()) {
                                chatId = message.getString("chat_id");
                                mainHandler.post(() -> callback.onChatIdReceived(chatId));
                            }
                            break;

                        case "content":
                            mainHandler.post(() -> {
                                try {
                                    callback.onMessageReceived(message.getString("content"));
                                } catch (JSONException e) {
                                    callback.onError(e.getMessage());
                                }
                            });
                            break;

                        case "done":
                            mainHandler.post(() -> {
                                try {
                                    callback.onResponseComplete(
                                            message.getString("chat_id"),
                                            message.getDouble("query_time")
                                    );
                                } catch (JSONException e) {
                                    callback.onError(e.getMessage());
                                }
                            });
                            break;

                        case "error":
                            mainHandler.post(() -> {
                                try {
                                    callback.onError(message.getString("error"));
                                } catch (JSONException e) {
                                    callback.onError(e.getMessage());
                                }
                            });
                            break;
                    }
                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onError("Error processing message: " + e.getMessage()));
                    Log.e("ChatWebSocket", "Error processing message", e);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("ChatWebSocket", "WebSocket connection failed", t);
                mainHandler.post(() -> {
                    isConnected = false;
                    callback.onDisconnected();
                    attemptReconnect();
                });
            }
        });
    }

    private void attemptReconnect() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            mainHandler.postDelayed(this::connect, RECONNECT_DELAY);
        }
    }

    public void sendMessage(String message) {
        if (!isConnected) {
            callback.onError("Not connected to server");
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            if (chatId != null) {
                json.put("chat_id", chatId);
            }
            json.put("users_name", profileManager.getName());
            json.put("users_dob", profileManager.getDateOfBirth());
            json.put("users_gender", profileManager.getGender());
            // Check for null or empty values and set "none" if they are
            profileManager.getCuisinePreferences();
            String cuisinePreferences = !profileManager.getCuisinePreferences().isEmpty()
                    ? profileManager.getCuisinePreferences().toString()
                    : "none";

            String dietPreference = (profileManager.getDietPreference() != null && !profileManager.getDietPreference().isEmpty())
                    ? profileManager.getDietPreference()
                    : "none";

            String conditions = (profileManager.getConditions() != null && !profileManager.getConditions().isEmpty())
                    ? profileManager.getConditions()
                    : "none";

            json.put("cuisinePreferences", cuisinePreferences);
            json.put("dietPreference", dietPreference);
            json.put("conditions", conditions);

            webSocket.send(json.toString());
        } catch (Exception e) {
            callback.onError("Error sending message: " + e.getMessage());
            Log.e("ChatWebSocket", "Error sending message", e);
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User disconnected");
        }
        isConnected = false;
    }
}