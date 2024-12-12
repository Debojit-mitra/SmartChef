package com.bunny.ml.smartchef.activities;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.adapters.EquipmentAdapter;
import com.bunny.ml.smartchef.adapters.IngredientAdapter;
import com.bunny.ml.smartchef.adapters.InstructionAdapter;
import com.bunny.ml.smartchef.adapters.NutritionAdapter;
import com.bunny.ml.smartchef.models.Recipe;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class RecipeViewActivity extends AppCompatActivity {

    private Recipe recipe;
    private CircularProgressIndicator imageProgress;

    // Preference constants
    private static final String PREFS_NAME = "SmartChefPrefs";
    private static final String PREF_MODEL_TYPE = "model_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_view);

        // Get recipe from intent
        recipe = getIntent().getParcelableExtra("recipe");
        if (recipe == null) {
            finish();
            return;
        }


        setupViews();
        setupRecyclerViews();
        setNavigationColor();
        setupBackPressHandling();
    }

    private void setupViews() {

        TextView appbar_title = findViewById(R.id.appbar_title);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean savedModelType = preferences.getBoolean(PREF_MODEL_TYPE, false);
        String appbarTitle;
        if (savedModelType) {
            appbarTitle = "Bert Model";
            appbar_title.setText(appbarTitle);
        } else {
            appbarTitle = "TF-IDF Model";
            appbar_title.setText(appbarTitle);
        }

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> handleBack());

        // Setup header information
        TextView recipeName = findViewById(R.id.recipe_name);
        recipeName.setText(recipe.getName());

        // Setup Time Info
        View timeInfo = findViewById(R.id.time_info);
        setupInfoCard(timeInfo, R.drawable.ic_round_time,
                recipe.getTotalTime(),
                getString(R.string.total_time));

        // Setup Rating Info
        View ratingInfo = findViewById(R.id.rating_info);
        String ratingValue;
        if (recipe.getAggregatedRating() == -1) {
            ratingValue = "N/A";
        } else {
            ratingValue = String.format(Locale.US, "%.1f (%d)",
                    recipe.getAggregatedRating(),
                    recipe.getRatingCount());
        }
        setupInfoCard(ratingInfo, R.drawable.ic_round_star,
                ratingValue,
                getString(R.string.rating));

        // Setup Servings Info
        View servingsInfo = findViewById(R.id.servings_info);
        String servingsValue = recipe.getRecipeServings() == -1 ?
                "N/A" :
                String.format(Locale.US, "%d", recipe.getRecipeServings());
        setupInfoCard(servingsInfo, R.drawable.ic_serving,
                servingsValue,
                getString(R.string.servings));

        // Setup recipe image
        ImageView recipeImage = findViewById(R.id.recipe_image);
        imageProgress = findViewById(R.id.image_progress);

        Glide.with(this)
                .load(recipe.getImageUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        imageProgress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   @NonNull Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        imageProgress.setVisibility(View.GONE);
                        return false;
                    }
                })
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(recipeImage);
    }

    private void setupInfoCard(View infoView, @DrawableRes int iconRes, String value, String label) {
        ImageView icon = infoView.findViewById(R.id.info_icon);
        TextView valueText = infoView.findViewById(R.id.info_value);
        TextView labelText = infoView.findViewById(R.id.info_label);

        icon.setImageResource(iconRes);
        valueText.setText(value);
        labelText.setText(label);
    }

    private void setupRecyclerViews() {
        // Setup ingredients recycler view
        RecyclerView ingredientsRecycler = findViewById(R.id.ingredients_recycler);
        ingredientsRecycler.setLayoutManager(new LinearLayoutManager(this));
        IngredientAdapter ingredientAdapter = new IngredientAdapter(
                recipe.getIngredientParts(),
                recipe.getIngredientQuantities()
        );
        ingredientsRecycler.setAdapter(ingredientAdapter);

        // Setup nutrition recycler view
        RecyclerView nutritionRecycler = findViewById(R.id.nutrition_recycler);
        nutritionRecycler.setLayoutManager(new LinearLayoutManager(this));
        NutritionAdapter nutritionAdapter = new NutritionAdapter(recipe.getNutritionalInfo());
        nutritionRecycler.setAdapter(nutritionAdapter);

        // Setup equipment recycler view
        MaterialCardView equipmentCard = findViewById(R.id.card_equipment_needed);
        System.out.println("HAHAHAH:   "+recipe.getEquipmentNeeded());
        if (recipe.getEquipmentNeeded() == null || recipe.getEquipmentNeeded().isEmpty()) {
            equipmentCard.setVisibility(View.GONE);
        } else {
            RecyclerView equipmentRecycler = findViewById(R.id.equipment_recycler);
            equipmentRecycler.setLayoutManager(new LinearLayoutManager(this));
            EquipmentAdapter equipmentAdapter = new EquipmentAdapter(recipe.getEquipmentNeeded());
            equipmentRecycler.setAdapter(equipmentAdapter);
        }


        // Setup instructions recycler view
        RecyclerView instructionsRecycler = findViewById(R.id.instructions_recycler);
        instructionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        InstructionAdapter instructionAdapter = new InstructionAdapter(recipe.getRecipeInstructions());
        instructionsRecycler.setAdapter(instructionAdapter);
    }

    private void setNavigationColor() {
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            insetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
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
}