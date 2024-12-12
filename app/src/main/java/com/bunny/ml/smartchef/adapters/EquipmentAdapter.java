package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {
    private final List<String> equipment;

    public EquipmentAdapter(List<String> equipment) {
        this.equipment = equipment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.equipmentNumber.setText(String.valueOf(position + 1));
        holder.equipmentText.setText(equipment.get(position));
    }

    @Override
    public int getItemCount() {
        return equipment.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView equipmentNumber;
        TextView equipmentText;

        ViewHolder(View view) {
            super(view);
            equipmentNumber = view.findViewById(R.id.equipment_number);
            equipmentText = view.findViewById(R.id.equipment_text);
        }
    }
}
