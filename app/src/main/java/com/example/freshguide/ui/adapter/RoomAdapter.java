package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.ui.RoomSearchResult;

public class RoomAdapter extends ListAdapter<RoomSearchResult, RoomAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RoomSearchResult room);
    }

    public interface OnActionClickListener {
        void onActionClick(RoomSearchResult room);
    }

    private OnItemClickListener listener;
    private OnActionClickListener actionClickListener;
    private boolean recentMode;

    public RoomAdapter() {
        super(DIFF);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        listener = l;
    }

    public void setOnActionClickListener(OnActionClickListener l) {
        actionClickListener = l;
    }

    public void setRecentMode(boolean recentMode) {
        this.recentMode = recentMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomSearchResult room = getItem(position);
        holder.bind(room, recentMode);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });
        holder.actionButton.setOnClickListener(v -> {
            if (actionClickListener != null) {
                actionClickListener.onActionClick(room);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView leadingIcon;
        private final TextView tvName;
        private final TextView tvCode;
        private final ImageButton actionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            leadingIcon = itemView.findViewById(R.id.iv_leading_icon);
            tvName = itemView.findViewById(R.id.tv_room_name);
            tvCode = itemView.findViewById(R.id.tv_room_code);
            actionButton = itemView.findViewById(R.id.btn_item_action);
        }

        void bind(RoomSearchResult room, boolean recentMode) {
            tvName.setText(room.getDisplayName());
            tvCode.setText(room.getSubtitle());
            leadingIcon.setImageDrawable(AppCompatResources.getDrawable(
                    itemView.getContext(),
                    recentMode ? R.drawable.ic_search_history : R.drawable.ic_search_pin
            ));
            actionButton.setVisibility(recentMode ? View.GONE : View.VISIBLE);
        }
    }

    private static final DiffUtil.ItemCallback<RoomSearchResult> DIFF = new DiffUtil.ItemCallback<RoomSearchResult>() {
        @Override
        public boolean areItemsTheSame(@NonNull RoomSearchResult a, @NonNull RoomSearchResult b) {
            return a.roomId == b.roomId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull RoomSearchResult a, @NonNull RoomSearchResult b) {
            return a.roomId == b.roomId
                    && safeEquals(a.roomName, b.roomName)
                    && safeEquals(a.roomCode, b.roomCode)
                    && safeEquals(a.buildingName, b.buildingName)
                    && a.floorNumber == b.floorNumber;
        }
    };

    private static boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
