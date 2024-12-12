package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.ViewHolder> {
    private final List<Map.Entry<String, Double>> nutritionItems;

    public NutritionAdapter(Map<String, Double> nutritionalInfo) {
        this.nutritionItems = new ArrayList<>(nutritionalInfo.entrySet());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutrition, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> item = nutritionItems.get(position);
        holder.nameText.setText(formatNutritionName(item.getKey()));
        holder.valueText.setText(String.format(Locale.US, "%.1f", item.getValue()));
    }

    private String formatNutritionName(String name) {
        return name.replace("_", " ")
                .replace("content", "")
                .trim()
                .substring(0, 1).toUpperCase() +
                name.substring(1).replace("_", " ")
                        .replace("content", "")
                        .trim();
    }

    @Override
    public int getItemCount() {
        return nutritionItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView valueText;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nutrition_name);
            valueText = view.findViewById(R.id.nutrition_value);
        }
    }
}
