package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.models.Recipe;

import java.util.List;
import java.util.Locale;

public class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {
    private final List<Recipe.InstructionStep> instructions;

    public InstructionAdapter(List<Recipe.InstructionStep> instructions) {
        this.instructions = instructions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instruction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe.InstructionStep step = instructions.get(position);
        holder.stepNumber.setText(String.format(Locale.US, "Step %d", step.getStepNumber()));
        holder.instructionText.setText(step.getText());
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stepNumber;
        TextView instructionText;

        ViewHolder(View view) {
            super(view);
            stepNumber = view.findViewById(R.id.step_number);
            instructionText = view.findViewById(R.id.instruction_text);
        }
    }
}
