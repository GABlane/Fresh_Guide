package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.RouteStepDto;

import java.util.ArrayList;
import java.util.List;

public class RouteStepAdapter extends RecyclerView.Adapter<RouteStepAdapter.ViewHolder> {

    private List<RouteStepDto> steps = new ArrayList<>();

    public RouteStepAdapter() {
        setHasStableIds(true);
    }

    public void setSteps(List<RouteStepDto> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        RouteStepDto step = steps.get(position);
        if (step.id > 0) {
            return step.id;
        }
        return step.orderNum > 0 ? step.orderNum : position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_step, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(steps.get(position), position + 1, position == 0, position == getItemCount() - 1);
    }

    @Override
    public int getItemCount() { return steps.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStep;
        private final TextView tvInstruction;
        private final TextView tvLandmark;
        private final View topConnector;
        private final View bottomConnector;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStep = itemView.findViewById(R.id.tv_step_number);
            tvInstruction = itemView.findViewById(R.id.tv_step_instruction);
            tvLandmark = itemView.findViewById(R.id.tv_step_landmark);
            topConnector = itemView.findViewById(R.id.view_step_connector_top);
            bottomConnector = itemView.findViewById(R.id.view_step_connector_bottom);
        }

        void bind(RouteStepDto step, int number, boolean isFirst, boolean isLast) {
            tvStep.setText(String.valueOf(number));
            tvInstruction.setText(step.instruction != null ? step.instruction.trim() : "");

            String secondaryText = null;
            if (step.landmark != null && !step.landmark.trim().isEmpty()) {
                secondaryText = step.landmark.trim();
            } else if (step.direction != null && !step.direction.trim().isEmpty()) {
                secondaryText = formatDirection(step.direction);
            }

            if (secondaryText != null && !secondaryText.isEmpty()) {
                tvLandmark.setVisibility(View.VISIBLE);
                tvLandmark.setText(secondaryText);
            } else {
                tvLandmark.setVisibility(View.GONE);
            }

            topConnector.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            bottomConnector.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }

        private String formatDirection(String direction) {
            String trimmed = direction.trim();
            if (trimmed.isEmpty()) {
                return "";
            }
            String normalized = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
            return "Move " + normalized;
        }
    }
}
