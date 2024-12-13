package com.bunny.ml.smartchef.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RecipeSuggestionsManager {
    private static final String TAG = "RecipeSuggestionsManager";
    private static final String PREFS_NAME = "RecipeSuggestionPrefs";
    private static final String SUGGESTIONS_KEY = "suggestions";
    private static final String LAST_FETCH_TIME_KEY = "last_fetch_time";
    private static final String API_URL = "https://api.npoint.io/c4460c966af910c7586d";
    private static final long CACHE_DURATION = TimeUnit.HOURS.toMillis(24);

    private final Context context;
    private final SharedPreferences preferences;
    private final OkHttpClient client;
    private final Gson gson;

    public RecipeSuggestionsManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public interface SuggestionsCallback {
        void onSuggestionsLoaded(List<String> suggestions);

        void onError(String error);
    }

    public void getCurrentTimeSuggestions(SuggestionsCallback callback) {
        LocalTime currentTime = LocalTime.now();
        String timeCategory = getTimeCategory(currentTime);
        getSuggestions(timeCategory, callback);
    }

    private String getTimeCategory(LocalTime time) {
        int hour = time.getHour();

        if (hour >= 4 && hour < 12) {
            return "morning";
        } else if (hour >= 12 && hour < 17) {
            return "afternoon";
        } else if (hour >= 17 && hour < 21) {
            return "evening";
        } else {
            return "late_night";
        }
    }

    private void getSuggestions(String timeCategory, SuggestionsCallback callback) {
        // Check if cache needs refresh
        if (shouldRefreshCache()) {
            // Fetch fresh data
            fetchSuggestionsFromApi(new SuggestionsCallback() {
                @Override
                public void onSuggestionsLoaded(List<String> suggestions) {
                    List<String> timeCategorySuggestions = filterSuggestionsByTime(suggestions, timeCategory);
                    callback.onSuggestionsLoaded(timeCategorySuggestions);
                }

                @Override
                public void onError(String error) {
                    // If API fetch fails, try to use cached data
                    List<String> cachedSuggestions = getCachedSuggestions(timeCategory);
                    if (cachedSuggestions != null && !cachedSuggestions.isEmpty()) {
                        callback.onSuggestionsLoaded(cachedSuggestions);
                    } else {
                        callback.onError(error);
                    }
                }
            });
        } else {
            // Use cached data
            List<String> cachedSuggestions = getCachedSuggestions(timeCategory);
            if (cachedSuggestions != null && !cachedSuggestions.isEmpty()) {
                callback.onSuggestionsLoaded(cachedSuggestions);
            } else {
                // Cache is empty or invalid, fetch fresh data
                fetchSuggestionsFromApi(callback);
            }
        }
    }

    private boolean shouldRefreshCache() {
        long lastFetchTime = preferences.getLong(LAST_FETCH_TIME_KEY, 0);
        return System.currentTimeMillis() - lastFetchTime >= CACHE_DURATION;
    }

    private void fetchSuggestionsFromApi(SuggestionsCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_URL)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected response " + response);

                    String jsonData = "";
                    if (response.body() != null) {
                        jsonData = response.body().string();
                    }
                    JsonObject suggestionsJson = gson.fromJson(jsonData, JsonObject.class);

                    // Cache the entire response
                    preferences.edit()
                            .putString(SUGGESTIONS_KEY, jsonData)
                            .putLong(LAST_FETCH_TIME_KEY, System.currentTimeMillis())
                            .apply();

                    // Return suggestions for the requested time category
                    callback.onSuggestionsLoaded(new ArrayList<>(suggestionsJson.getAsJsonObject("suggestions").keySet()));
                }
            } catch (Exception e) {
                callback.onError("Failed to fetch suggestions: " + e.getMessage());
            }
        }).start();
    }

    private List<String> getCachedSuggestions(String timeCategory) {
        String cachedJson = preferences.getString(SUGGESTIONS_KEY, null);
        if (cachedJson == null) return null;

        try {
            JsonObject suggestionsJson = gson.fromJson(cachedJson, JsonObject.class);
            JsonObject suggestions = suggestionsJson.getAsJsonObject("suggestions");
            List<String> combinedSuggestions = new ArrayList<>();

            // Add time-specific suggestions
            if (suggestions.has(timeCategory)) {
                combinedSuggestions.addAll(gson.fromJson(suggestions.getAsJsonArray(timeCategory),
                        new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()));
            }

            // Add general suggestions
            if (suggestions.has("general")) {
                combinedSuggestions.addAll(gson.fromJson(suggestions.getAsJsonArray("general"),
                        new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()));
            }

            return combinedSuggestions;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing cached suggestions", e);
        }
        return null;
    }

    private List<String> filterSuggestionsByTime(List<String> suggestions, String timeCategory) {
        String cachedJson = preferences.getString(SUGGESTIONS_KEY, null);
        if (cachedJson == null) return suggestions;

        try {
            JsonObject suggestionsJson = gson.fromJson(cachedJson, JsonObject.class);
            JsonObject allSuggestions = suggestionsJson.getAsJsonObject("suggestions");
            List<String> combinedSuggestions = new ArrayList<>();

            // Add time-specific suggestions
            if (allSuggestions.has(timeCategory)) {
                combinedSuggestions.addAll(gson.fromJson(allSuggestions.getAsJsonArray(timeCategory),
                        new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()));
            }

            // Add general suggestions
            if (allSuggestions.has("general")) {
                combinedSuggestions.addAll(gson.fromJson(allSuggestions.getAsJsonArray("general"),
                        new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()));
            }

            return combinedSuggestions;
        } catch (Exception e) {
            Log.e(TAG, "Error filtering suggestions", e);
        }
        return suggestions;
    }
}
