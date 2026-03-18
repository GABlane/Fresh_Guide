package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.RouteStepDto;

import java.util.List;
import java.util.Locale;

public class RouteStepEditAdapter extends RecyclerView.Adapter<RouteStepEditAdapter.ViewHolder> {

    public interface StepActionListener {
        void onEdit(int position);
        void onDelete(int position);
        void onMoveUp(int position);
        void onMoveDown(int position);
    }

    private final List<RouteStepDto> steps;
    private final StepActionListener listener;

    public RouteStepEditAdapter(@NonNull List<RouteStepDto> steps,
                                @NonNull StepActionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_step_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteStepDto item = steps.get(position);
        holder.bind(item, position, steps.size());

        holder.btnEditStep.setOnClickListener(v -> listener.onEdit(position));
        holder.btnDeleteStep.setOnClickListener(v -> listener.onDelete(position));
        holder.btnMoveUp.setOnClickListener(v -> listener.onMoveUp(position));
        holder.btnMoveDown.setOnClickListener(v -> listener.onMoveDown(position));
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStepNumber;
        private final TextView tvInstruction;
        private final TextView tvDirectionLandmark;
        private final ImageButton btnMoveUp;
        private final ImageButton btnMoveDown;
        private final ImageButton btnEditStep;
        private final ImageButton btnDeleteStep;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.tv_step_number);
            tvInstruction = itemView.findViewById(R.id.tv_instruction);
            tvDirectionLandmark = itemView.findViewById(R.id.tv_direction_landmark);
            btnMoveUp = itemView.findViewById(R.id.btn_move_up);
            btnMoveDown = itemView.findViewById(R.id.btn_move_down);
            btnEditStep = itemView.findViewById(R.id.btn_edit_step);
            btnDeleteStep = itemView.findViewById(R.id.btn_delete_step);
        }

        void bind(RouteStepDto step, int position, int totalItems) {
            tvStepNumber.setText(String.valueOf(position + 1));
            tvInstruction.setText(step.instruction != null && !step.instruction.trim().isEmpty()
                    ? step.instruction.trim()
                    : "Step instruction");

            StringBuilder meta = new StringBuilder();
            if (step.direction != null && !step.direction.trim().isEmpty()) {
                meta.append(capitalize(step.direction.trim()));
            }
            if (step.landmark != null && !step.landmark.trim().isEmpty()) {
                if (meta.length() > 0) {
                    meta.append(" - ");
                }
                meta.append(step.landmark.trim());
            }

            if (meta.length() > 0) {
                tvDirectionLandmark.setText(meta.toString());
                tvDirectionLandmark.setVisibility(View.VISIBLE);
            } else {
                tvDirectionLandmark.setVisibility(View.GONE);
            }

            boolean canMoveUp = position > 0;
            boolean canMoveDown = position < totalItems - 1;

            btnMoveUp.setEnabled(canMoveUp);
            btnMoveUp.setAlpha(canMoveUp ? 1f : 0.35f);
            btnMoveDown.setEnabled(canMoveDown);
            btnMoveDown.setAlpha(canMoveDown ? 1f : 0.35f);
        }

        private String capitalize(String value) {
            if (value.isEmpty()) {
                return value;
            }
            String lower = value.toLowerCase(Locale.US);
            return lower.substring(0, 1).toUpperCase(Locale.US) + lower.substring(1);
        }
    }
}
