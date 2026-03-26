package com.example.freshguide.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleEntryAdapter extends RecyclerView.Adapter<ScheduleEntryAdapter.ViewHolder> {

    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleEntryEntity entry);
    }

    private final List<ScheduleEntryEntity> items = new ArrayList<>();
    private final Map<Integer, String> roomNameMap = new HashMap<>();
    private OnScheduleClickListener listener;

    public void setOnScheduleClickListener(OnScheduleClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ScheduleEntryEntity> schedules) {
        items.clear();
        if (schedules != null) {
            items.addAll(schedules);
        }
        notifyDataSetChanged();
    }

    public void setRoomNameMap(Map<Integer, String> roomNameMap) {
        this.roomNameMap.clear();
        if (roomNameMap != null) {
            this.roomNameMap.putAll(roomNameMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleEntryEntity item = items.get(position);
        holder.bind(item, roomNameMap);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScheduleClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView tvStart;
        private final TextView tvEnd;
        private final TextView tvCode;
        private final TextView tvTitle;
        private final TextView tvProfessor;
        private final TextView tvLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_schedule_item);
            tvStart = itemView.findViewById(R.id.tv_schedule_start);
            tvEnd = itemView.findViewById(R.id.tv_schedule_end);
            tvCode = itemView.findViewById(R.id.tv_schedule_code);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvProfessor = itemView.findViewById(R.id.tv_schedule_professor);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
        }

        void bind(ScheduleEntryEntity entry, Map<Integer, String> roomNameMap) {
            tvStart.setText(formatMinutes(entry.startMinutes));
            tvEnd.setText(formatMinutes(entry.endMinutes));
            tvCode.setText(entry.courseCode != null && !entry.courseCode.isBlank() ? entry.courseCode : "No code");
            tvTitle.setText(entry.title != null && !entry.title.isBlank() ? entry.title : "Untitled Class");
            tvProfessor.setText(entry.instructor != null && !entry.instructor.isBlank()
                    ? "Prof. " + entry.instructor
                    : "Professor not set");

            if (entry.isOnline == 1) {
                String platform = entry.onlinePlatform != null && !entry.onlinePlatform.isBlank()
                        ? entry.onlinePlatform
                        : "Online";
                tvLocation.setText("Online • " + platform);
            } else if (entry.roomId != null && roomNameMap.containsKey(entry.roomId)) {
                tvLocation.setText(roomNameMap.get(entry.roomId));
            } else {
                tvLocation.setText("Room not set");
            }

            int backgroundColor = parseColorOrDefault(entry.colorHex, "#F4F4F4");
            card.setCardBackgroundColor(backgroundColor);

            // Set text colors based on background brightness for readability
            int textColor = getContrastingTextColor(backgroundColor);
            int secondaryTextColor = getSecondaryTextColor(backgroundColor);
            tvStart.setTextColor(textColor);
            tvEnd.setTextColor(secondaryTextColor);
            tvCode.setTextColor(secondaryTextColor);
            tvTitle.setTextColor(textColor);
            tvProfessor.setTextColor(secondaryTextColor);
            tvLocation.setTextColor(secondaryTextColor);
        }

        /**
         * Calculate contrasting text color based on background brightness.
         * Uses relative luminance formula from WCAG guidelines.
         * @param backgroundColor The background color to contrast against
         * @return Dark text for light backgrounds, light text for dark backgrounds
         */
        private int getContrastingTextColor(int backgroundColor) {
            // Extract RGB components
            int red = Color.red(backgroundColor);
            int green = Color.green(backgroundColor);
            int blue = Color.blue(backgroundColor);

            // Calculate relative luminance using WCAG formula
            double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

            // If background is light (luminance > 0.5), use dark text; otherwise use light text
            return luminance > 0.5 ? Color.parseColor("#1C1B1F") : Color.parseColor("#E6E6E6");
        }

        /**
         * Get secondary text color (slightly less prominent) based on background brightness.
         * @param backgroundColor The background color to contrast against
         * @return Secondary text color with appropriate contrast
         */
        private int getSecondaryTextColor(int backgroundColor) {
            // Extract RGB components
            int red = Color.red(backgroundColor);
            int green = Color.green(backgroundColor);
            int blue = Color.blue(backgroundColor);

            // Calculate relative luminance using WCAG formula
            double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

            // If background is light, use medium gray; if dark, use light gray
            return luminance > 0.5 ? Color.parseColor("#5E5E5E") : Color.parseColor("#B8B8B8");
        }

        private int parseColorOrDefault(String input, String fallback) {
            try {
                return Color.parseColor(input);
            } catch (Exception e) {
                return Color.parseColor(fallback);
            }
        }

        private String formatMinutes(int minutes) {
            int hour24 = Math.max(0, Math.min(23, minutes / 60));
            int minute = Math.max(0, Math.min(59, minutes % 60));
            int hour12 = hour24 % 12;
            if (hour12 == 0) hour12 = 12;
            String suffix = hour24 >= 12 ? "PM" : "AM";
            return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, suffix);
        }
    }
}
