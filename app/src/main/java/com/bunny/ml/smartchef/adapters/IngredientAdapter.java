package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {
    private final List<String> ingredients;
    private final List<String> quantities;

    public IngredientAdapter(List<String> ingredients, List<String> quantities) {
        this.ingredients = ingredients;
        this.quantities = quantities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ingredientText.setText(ingredients.get(position));
        holder.quantityText.setText(quantities.get(position));
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ingredientText;
        TextView quantityText;

        ViewHolder(View view) {
            super(view);
            ingredientText = view.findViewById(R.id.ingredient_text);
            quantityText = view.findViewById(R.id.quantity_text);
        }
    }
}

