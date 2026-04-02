package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;

import java.util.List;

public class TimeWheelAdapter extends RecyclerView.Adapter<TimeWheelAdapter.ViewHolder> {

    private final List<String> items;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public TimeWheelAdapter(List<String> items) {
        this.items = items;
    }

    public void setSelectedPosition(int newPosition) {
        if (newPosition == selectedPosition) return;

        int oldPosition = selectedPosition;
        selectedPosition = newPosition;

        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_wheel_text, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String text = items.get(position);
        boolean selected = position == selectedPosition;

        holder.textView.setText(text);
        holder.textView.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                selected ? R.color.time_picker_text_primary : R.color.time_picker_text_secondary
        ));

        // same size for all, only color/alpha changes
        holder.textView.setTextSize(22f);
        holder.textView.setAlpha(selected ? 1f : 0.45f);
        holder.textView.setScaleX(1f);
        holder.textView.setScaleY(1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_wheel_text);
        }
    }
}