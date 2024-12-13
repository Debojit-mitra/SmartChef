package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;

import java.util.ArrayList;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {
    private final List<String> suggestions = new ArrayList<>();
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    public void setSuggestions(List<String> newSuggestions) {
        List<String> selectedSuggestions = selectRandomSuggestions(newSuggestions);
        // Create DiffUtil callback
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return suggestions.size();
            }

            @Override
            public int getNewListSize() {
                return selectedSuggestions.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return suggestions.get(oldItemPosition).equals(selectedSuggestions.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return suggestions.get(oldItemPosition).equals(selectedSuggestions.get(newItemPosition));
            }
        });

        // Clear and add all in one atomic operation
        suggestions.clear();
        suggestions.addAll(selectedSuggestions);

        // Dispatch updates
        diffResult.dispatchUpdatesTo(this);
    }

    private List<String> selectRandomSuggestions(List<String> allSuggestions) {
        if (allSuggestions.size() <= 3) {
            return new ArrayList<>(allSuggestions);
        }

        List<String> randomSuggestions = new ArrayList<>();
        List<String> tempList = new ArrayList<>(allSuggestions);

        // Select 3 random suggestions
        for (int i = 0; i < 3 && !tempList.isEmpty(); i++) {
            int randomIndex = (int) (Math.random() * tempList.size());
            randomSuggestions.add(tempList.remove(randomIndex));
        }

        return randomSuggestions;
    }

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.suggestion_item, parent, false);
        return new SuggestionViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        holder.bind(suggestions.get(position));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView suggestionText;
        private final OnSuggestionClickListener listener;

        SuggestionViewHolder(@NonNull View itemView, OnSuggestionClickListener listener) {
            super(itemView);
            this.listener = listener;
            suggestionText = itemView.findViewById(R.id.suggestionText);
        }

        void bind(String suggestion) {
            suggestionText.setText(suggestion);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSuggestionClick(suggestion);
                }
            });
        }
    }
}