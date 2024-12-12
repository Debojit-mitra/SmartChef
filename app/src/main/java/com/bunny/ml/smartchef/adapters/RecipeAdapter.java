package com.bunny.ml.smartchef.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.activities.RecipeViewActivity;
import com.bunny.ml.smartchef.models.Recipe;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipes;
    private final Context context;

    public RecipeAdapter(Context context) {
        this.context = context;
        this.recipes = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.recipeName.setText(recipe.getName());
        holder.totalTime.setText(recipe.getTotalTime());

        String servings = recipe.getRecipeServings() == -1 ? "N/A" : recipe.getRecipeServings() + " servings";
        holder.servings.setText(servings);

        String rating = recipe.getAggregatedRating() == -1 ? "N/A" : String.format(Locale.US, "%.1f (%d)",
                recipe.getAggregatedRating(), recipe.getRatingCount());
        holder.rating.setText(rating);

        List<String> ingredients = recipe.getIngredientParts();
        String ingredientText = ingredients.size() > 3
                ? String.join(", ", ingredients.subList(0, 3)) + "..."
                : String.join(", ", ingredients);
        holder.ingredients.setText(ingredientText);

        Glide.with(context)
                .load(recipe.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        holder.thumbnailProgress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        holder.thumbnailProgress.setVisibility(View.GONE);
                        return false;
                    }
                })
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(holder.recipeImage);

        holder.similarityScore.setText(String.format(Locale.US, "%.0f%% Match",
                recipe.getSimilarityScore() * 100));

        // Set click listener for the entire item
        holder.recipeCard.setOnClickListener(v -> openRecipeDetails(recipe));
    }

    private void openRecipeDetails(Recipe recipe) {
        Intent intent = new Intent(context, RecipeViewActivity.class);
        intent.putExtra("recipe", recipe);
        context.startActivity(intent);
        // Add transition animation
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).overridePendingTransition(
                    R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void setRecipes(List<Recipe> newRecipes) {
        // Use DiffUtil to calculate the difference and dispatch specific change events
        RecipeDiffCallback diffCallback = new RecipeDiffCallback(this.recipes, newRecipes);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.recipes = new ArrayList<>(newRecipes);
        diffResult.dispatchUpdatesTo(this);
    }

    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
        notifyItemInserted(recipes.size() - 1);
    }

    public void addRecipes(List<Recipe> newRecipes) {
        int startPosition = recipes.size();
        recipes.addAll(newRecipes);
        notifyItemRangeInserted(startPosition, newRecipes.size());
    }

    public void updateRecipe(int position, Recipe recipe) {
        if (position >= 0 && position < recipes.size()) {
            recipes.set(position, recipe);
            notifyItemChanged(position);
        }
    }

    public void removeRecipe(int position) {
        if (position >= 0 && position < recipes.size()) {
            recipes.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearRecipes() {
        int size = recipes.size();
        recipes.clear();
        notifyItemRangeRemoved(0, size);
    }

    private static class RecipeDiffCallback extends DiffUtil.Callback {
        private final List<Recipe> oldRecipes;
        private final List<Recipe> newRecipes;

        RecipeDiffCallback(List<Recipe> oldRecipes, List<Recipe> newRecipes) {
            this.oldRecipes = oldRecipes;
            this.newRecipes = newRecipes;
        }

        @Override
        public int getOldListSize() {
            return oldRecipes.size();
        }

        @Override
        public int getNewListSize() {
            return newRecipes.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldRecipes.get(oldItemPosition);
            Recipe newRecipe = newRecipes.get(newItemPosition);
            return oldRecipe.getRecipeId().equals(newRecipe.getRecipeId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldRecipes.get(oldItemPosition);
            Recipe newRecipe = newRecipes.get(newItemPosition);

            // Compare all relevant fields
            return oldRecipe.getName().equals(newRecipe.getName()) &&
                    oldRecipe.getTotalTime().equals(newRecipe.getTotalTime()) &&
                    oldRecipe.getRecipeServings() == newRecipe.getRecipeServings() &&
                    oldRecipe.getAggregatedRating() == newRecipe.getAggregatedRating() &&
                    oldRecipe.getRatingCount() == newRecipe.getRatingCount() &&
                    oldRecipe.getIngredientParts().equals(newRecipe.getIngredientParts()) &&
                    oldRecipe.getImageUrl().equals(newRecipe.getImageUrl()) &&
                    oldRecipe.getSimilarityScore() == newRecipe.getSimilarityScore();
        }
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView recipeCard;
        ImageView recipeImage;
        TextView recipeName, totalTime, rating, ingredients, servings, similarityScore;
        CircularProgressIndicator thumbnailProgress;

        RecipeViewHolder(View itemView) {
            super(itemView);
            recipeCard = (MaterialCardView) itemView;
            recipeImage = itemView.findViewById(R.id.recipe_image);
            recipeName = itemView.findViewById(R.id.recipe_name);
            totalTime = itemView.findViewById(R.id.total_time);
            rating = itemView.findViewById(R.id.rating);
            ingredients = itemView.findViewById(R.id.ingredients);
            servings = itemView.findViewById(R.id.servings);
            similarityScore = itemView.findViewById(R.id.similarity_score);
            thumbnailProgress = itemView.findViewById(R.id.thumbnailProgress);
        }
    }
}