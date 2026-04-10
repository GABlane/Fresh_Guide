package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;

import java.util.ArrayList;
import java.util.List;

public class DirectionSearchAdapter extends RecyclerView.Adapter<DirectionSearchAdapter.ViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClicked(SuggestionItem item);
    }

    public static class SuggestionItem {
        public final int id;
        public final String title;
        public final String subtitle;
        public final int iconResId;
        public final boolean isOrigin;
        public final boolean roomBased;

        public SuggestionItem(int id, String title, String subtitle, int iconResId, boolean isOrigin, boolean roomBased) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.iconResId = iconResId;
            this.isOrigin = isOrigin;
            this.roomBased = roomBased;
        }
    }

    private final List<SuggestionItem> items = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public DirectionSearchAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SuggestionItem> suggestions) {
        items.clear();
        if (suggestions != null) {
            items.addAll(suggestions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_direction_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SuggestionItem item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView subtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_room_pin);
            title = itemView.findViewById(R.id.tv_room_title);
            subtitle = itemView.findViewById(R.id.tv_room_subtitle);
        }

        void bind(SuggestionItem item) {
            icon.setImageResource(item.iconResId);
            title.setText(item.title);
            subtitle.setText(item.subtitle);
        }
    }
}
