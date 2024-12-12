package com.bunny.ml.smartchef.activities;

import static com.bunny.ml.smartchef.utils.SharedData.BASE_URL;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.adapters.RecipeAdapter;
import com.bunny.ml.smartchef.models.Recipe;
import com.bunny.ml.smartchef.utils.LoadingDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TryModelActivity extends AppCompatActivity {

    private static final String TAG = "TryModelActivity";
    private static final int MIN_TOP_N = 5;
    private static final int MAX_TOP_N = 15;
    private static final int STEP_SIZE = 5;
    private static final int REQUEST_TIMEOUT_MS = 60000;
    private static final int ERROR_TIMEOUT = 408;
    private static final int ERROR_GATEWAY_TIMEOUT = 504;

    private RecyclerView resultRecyclerView;
    private LinearLayout layout_center_view;
    private RecipeAdapter recipeAdapter;
    private LoadingDialog loadingDialog;
    private TextInputEditText searchEditText;
    private MaterialSwitch modelSwitch;
    private MaterialSwitch searchTypeSwitch;
    private TextView topNValueText;
    private LottieAnimationView animationView;
    private SharedPreferences preferences;
    private int currentTopN = MIN_TOP_N;
    private boolean somethingChanged = false;

    // Preference constants
    private static final String PREFS_NAME = "SmartChefPrefs";
    private static final String PREF_MODEL_TYPE = "model_type";
    private static final String PREF_SEARCH_TYPE = "search_type";

    // Track current search query to prevent duplicate searches
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_try_model);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        setupRecyclerView();
        setNavigationColor();
        setupBackPressHandling();
        loadSavedPreferences();
        setupSwitchListeners();
        setupTopNSelector();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        ImageView backBtn = findViewById(R.id.backBtn);
        modelSwitch = findViewById(R.id.model_switch);
        searchTypeSwitch = findViewById(R.id.search_type_switch);
        topNValueText = findViewById(R.id.top_n_value);
        layout_center_view = findViewById(R.id.layout_center_view);
        animationView = findViewById(R.id.animationView);

        backBtn.setOnClickListener(v -> handleBack());

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String query = Objects.requireNonNull(v.getText()).toString().trim();
                if (!query.isEmpty() && !query.equals(currentQuery) || !query.isEmpty() && somethingChanged) {
                    currentQuery = query;
                    performSearch(query);
                    somethingChanged = false;
                }
                return true;
            }
            return false;
        });

        recipeAdapter = new RecipeAdapter(this);
        loadingDialog = new LoadingDialog(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> animationView.playAnimation(),500);
    }

    private void loadSavedPreferences() {
        // Load saved preferences with defaults
        boolean savedModelType = preferences.getBoolean(PREF_MODEL_TYPE, false); // false = tfidf, true = bert
        boolean savedSearchType = preferences.getBoolean(PREF_SEARCH_TYPE, false); // false = recipe name, true = ingredients

        // Set switches to saved states
        modelSwitch.setChecked(savedModelType);
        searchTypeSwitch.setChecked(savedSearchType);

        // Update search hint based on saved search type
        updateSearchHint(savedSearchType);
    }

    private void setupTopNSelector() {
        updateTopNValue();

        findViewById(R.id.decrease_btn).setOnClickListener(v -> {
            if (currentTopN > MIN_TOP_N) {
                currentTopN -= STEP_SIZE;
                updateTopNValue();
                somethingChanged = true;
            }
        });

        findViewById(R.id.increase_btn).setOnClickListener(v -> {
            if (currentTopN < MAX_TOP_N) {
                currentTopN += STEP_SIZE;
                updateTopNValue();
                somethingChanged = true;
            }
        });
    }

    private void updateTopNValue() {
        topNValueText.setText(String.valueOf(currentTopN));
    }

    private void setupRecyclerView() {
        resultRecyclerView = findViewById(R.id.result_recyclerview);
        resultRecyclerView.setAdapter(recipeAdapter);
        resultRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSwitchListeners() {
        modelSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save model type preference
            preferences.edit().putBoolean(PREF_MODEL_TYPE, isChecked).apply();
            performSearchIfQueryExists();
        });

        searchTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save search type preference
            preferences.edit().putBoolean(PREF_SEARCH_TYPE, isChecked).apply();
            updateSearchHint(isChecked);
            somethingChanged = true;
        });
    }


    private void performSearchIfQueryExists() {
        String query = Objects.requireNonNull(searchEditText.getText()).toString().trim();
        if (!query.isEmpty()) {
            currentQuery = query;
            performSearch(query);
        }
    }

    private void updateSearchHint(boolean searchByIngredients) {
        searchEditText.setHint(searchByIngredients ?
                getString(R.string.enter_ingredients) :
                getString(R.string.enter_recipe_name));
    }

    private void performSearch(String query) {
        searchEditText.clearFocus();
        loadingDialog.show("Searching recipes");
        layout_center_view.setVisibility(View.GONE);
        resultRecyclerView.setVisibility(View.VISIBLE);
        String modelType = modelSwitch.isChecked() ? "bert" : "tfidf";
        boolean searchByIngredients = searchTypeSwitch.isChecked();

        // Clear existing results before new search
        recipeAdapter.clearRecipes();

        if (searchByIngredients) {
            performIngredientSearch(query, modelType);
        } else {
            performNameSearch(query, modelType);
        }
    }

    private void performNameSearch(String query, String modelType) {
        String url = BASE_URL + "recipe/" + Uri.encode(query) +
                "?model_type=" + modelType + "&top_n=" + currentTopN;

        StringRequest stringRequest = createStringRequest(url);
        addToRequestQueue(stringRequest);
    }

    private void performIngredientSearch(String query, String modelType) {
        String url = BASE_URL + "recommend/ingredients";
        List<String> ingredients = Arrays.asList(query.split(",\\s*"));

        loadingDialog.show("This might take up to 30 seconds...");

        JSONObject jsonBody = createIngredientSearchBody(ingredients, modelType);
        if (jsonBody != null) {
            JsonObjectRequest jsonRequest = createJsonRequest(url, jsonBody);
            addToRequestQueue(jsonRequest);
        }
    }

    private StringRequest createStringRequest(String url) {
        return new StringRequest(Request.Method.GET, url,
                this::handleResponse,
                this::handleError) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
    }

    private JSONObject createIngredientSearchBody(List<String> ingredients, String modelType) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("ingredients", new JSONArray(ingredients));
            jsonBody.put("model_type", modelType);
            jsonBody.put("top_n", currentTopN);
            return jsonBody;
        } catch (JSONException e) {
            logError("Error creating request body", e);
            loadingDialog.dismiss();
            return null;
        }
    }

    private JsonObjectRequest createJsonRequest(String url, JSONObject jsonBody) {
        return new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> handleResponse(response.toString()),
                this::handleError) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
    }

    private void addToRequestQueue(Request<?> request) {
        request.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        Volley.newRequestQueue(this).add(request);
    }

    private void handleResponse(String response) {
        try {
            List<Recipe> recipes = parseRecipes(response);
            updateUI(recipes);
        } catch (JSONException e) {
            logError("Error parsing response", e);
            showError("Error parsing response");
        }
    }

    private List<Recipe> parseRecipes(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray recommendations = jsonResponse.getJSONArray("recommendations");
        List<Recipe> recipes = new ArrayList<>();

        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject recipeJson = recommendations.getJSONObject(i);
            recipes.add(parseRecipe(recipeJson));
        }

        return recipes;
    }

    private Recipe parseRecipe(JSONObject recipeJson) throws JSONException {
        // Parse ingredient parts
        List<String> ingredientParts = parseIngredientParts(recipeJson.getJSONArray("ingredient_parts"));

        // Parse ingredient quantities
        List<String> ingredientQuantities = parseIngredientParts(recipeJson.getJSONArray("ingredient_quantities"));

        // Parse nutritional information
        JSONObject nutritionalInfoJson = recipeJson.getJSONObject("nutritional_info");
        Map<String, Double> nutritionalInfo = new HashMap<>();
        nutritionalInfo.put("calories", nutritionalInfoJson.optDouble("calories", 0.0));
        nutritionalInfo.put("fat_content", nutritionalInfoJson.optDouble("fat_content", 0.0));
        nutritionalInfo.put("saturated_fat_content", nutritionalInfoJson.optDouble("saturated_fat_content", 0.0));
        nutritionalInfo.put("cholesterol_content", nutritionalInfoJson.optDouble("cholesterol_content", 0.0));
        nutritionalInfo.put("sodium_content", nutritionalInfoJson.optDouble("sodium_content", 0.0));
        nutritionalInfo.put("carbohydrate_content", nutritionalInfoJson.optDouble("carbohydrate_content", 0.0));
        nutritionalInfo.put("fiber_content", nutritionalInfoJson.optDouble("fiber_content", 0.0));
        nutritionalInfo.put("sugar_content", nutritionalInfoJson.optDouble("sugar_content", 0.0));
        nutritionalInfo.put("protein_content", nutritionalInfoJson.optDouble("protein_content", 0.0));

        // Parse recipe instructions
        JSONArray instructionsJsonArray = recipeJson.getJSONArray("recipe_instructions");
        List<Recipe.InstructionStep> recipeInstructions = new ArrayList<>();
        for (int i = 0; i < instructionsJsonArray.length(); i++) {
            JSONObject stepJson = instructionsJsonArray.getJSONObject(i);

            int stepNumber = stepJson.getInt("step_number");
            String text = stepJson.getString("text");
            List<String> actions = parseStringArray(stepJson.getJSONArray("actions"));
            List<String> ingredientsMentioned = parseStringArray(stepJson.getJSONArray("ingredients_mentioned"));

            // Parse timing
            JSONObject timingJson = stepJson.optJSONObject("timing");
            Map<String, Integer> timing = new HashMap<>();
            if (timingJson != null) {
                Iterator<String> keys = timingJson.keys(); // Use keys() instead of keySet()
                while (keys.hasNext()) {
                    String key = keys.next();
                    timing.put(key, timingJson.optInt(key, 0));
                }
            }

            boolean requiresAttention = stepJson.optBoolean("requires_attention", false);

            Recipe.InstructionStep instructionStep = new Recipe.InstructionStep(
                    stepNumber, text, actions, ingredientsMentioned, timing, requiresAttention
            );
            recipeInstructions.add(instructionStep);
        }


        // Parse equipment needed
        List<String> equipmentNeeded = parseStringArray(recipeJson.getJSONArray("equipment_needed"));

        // Return the populated Recipe object
        return new Recipe(
                recipeJson.getString("recipe_id"),
                recipeJson.getString("name"),
                recipeJson.optString("cook_time"),
                recipeJson.optString("prep_time"),
                recipeJson.getString("image_url"),
                ingredientParts,
                ingredientQuantities,
                nutritionalInfo,
                recipeInstructions,
                equipmentNeeded,
                recipeJson.getDouble("aggregated_rating"),
                recipeJson.getInt("rating_count"),
                recipeJson.getInt("recipe_servings"),
                recipeJson.getDouble("similarity_score")
        );
    }

    // Utility method to parse a JSONArray of strings into a List<String>
    private List<String> parseStringArray(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list;
    }


    private List<String> parseIngredientParts(JSONArray ingredientPartsJson) throws JSONException {
        List<String> ingredientParts = new ArrayList<>();
        for (int j = 0; j < ingredientPartsJson.length(); j++) {
            if (!ingredientPartsJson.isNull(j)) {
                ingredientParts.add(ingredientPartsJson.getString(j));
            }
        }
        return ingredientParts;
    }

    private void updateUI(List<Recipe> recipes) {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            if (recipes.isEmpty()) {
                showToast("No recipes found");
            } else {
                // Use the more efficient DiffUtil-based update
                recipeAdapter.setRecipes(recipes);
            }
        });
    }

    private void handleError(VolleyError error) {
        String errorMessage = getErrorMessage(error);
        showError(errorMessage);
        clearResults();
    }

    private String getErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == ERROR_TIMEOUT || statusCode == ERROR_GATEWAY_TIMEOUT) {
                return "Request timed out. Please try again.";
            }
            return "Error fetching recipes (Status: " + statusCode + ")";
        }
        if (error.getMessage() != null && error.getMessage().contains("timeout")) {
            return "Request timed out. Please try again.";
        }
        return "Network error. Please check your connection.";
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            showToast(message);
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void clearResults() {
        recipeAdapter.clearRecipes();
    }

    private void logError(String message, Exception e) {
        Log.e(TAG, message + ": " + e.toString());
    }

    private void setNavigationColor() {
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            insetsController.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            );
        }
        getWindow().setNavigationBarColor(getColor(R.color.mode));
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
    protected void onPause() {
        super.onPause();
        // Save current states when activity is paused
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_MODEL_TYPE, modelSwitch.isChecked());
        editor.putBoolean(PREF_SEARCH_TYPE, searchTypeSwitch.isChecked());
        editor.apply();
    }
}