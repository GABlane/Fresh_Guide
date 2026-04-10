package com.example.freshguide.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.ui.RoomSearchResult;

public class RoomAdapter extends ListAdapter<RoomSearchResult, RoomAdapter.ViewHolder> {

    public static final int MODE_SEARCH = 0;
    public static final int MODE_RECENT = 1;
    public static final int MODE_SAVED = 2;

    public interface OnItemClickListener {
        void onItemClick(RoomSearchResult room);
    }

    public interface OnActionClickListener {
        void onActionClick(RoomSearchResult room);
    }

    private OnItemClickListener listener;
    private OnActionClickListener actionClickListener;
    private int displayMode = MODE_SEARCH;

    public RoomAdapter() {
        super(DIFF);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        listener = l;
    }

    public void setOnActionClickListener(OnActionClickListener l) {
        actionClickListener = l;
    }

    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType == MODE_SAVED ? R.layout.item_favorite_room : R.layout.item_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        return displayMode == MODE_SAVED ? MODE_SAVED : MODE_SEARCH;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomSearchResult room = getItem(position);
        holder.bind(room, displayMode);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });
        if (displayMode == MODE_SAVED) {
            holder.leadingIcon.setOnClickListener(v -> {
                if (actionClickListener != null) {
                    actionClickListener.onActionClick(room);
                }
            });
        } else {
            holder.leadingIcon.setOnClickListener(null);
        }
        if (holder.actionButton != null) {
            holder.actionButton.setOnClickListener(v -> {
                if (actionClickListener != null) {
                    actionClickListener.onActionClick(room);
                }
            });
        }
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

        void bind(RoomSearchResult room, int displayMode) {
            tvName.setText(room.getDisplayName());
            tvCode.setText(room.getSubtitle());
            int iconRes;
            int iconTint;
            if (displayMode == MODE_RECENT) {
                iconRes = R.drawable.ic_search_history;
                iconTint = R.color.text_secondary;
            } else if (displayMode == MODE_SAVED) {
                iconRes = R.drawable.ic_star_filled;
                iconTint = R.color.green_primary;
            } else {
                iconRes = R.drawable.ic_search_pin;
                iconTint = R.color.text_secondary;
            }
            leadingIcon.setImageDrawable(AppCompatResources.getDrawable(itemView.getContext(), iconRes));
            ImageViewCompat.setImageTintList(
                    leadingIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), iconTint))
            );
            leadingIcon.setClickable(displayMode == MODE_SAVED);
            leadingIcon.setFocusable(displayMode == MODE_SAVED);
            if (actionButton != null) {
                actionButton.setVisibility(displayMode == MODE_SEARCH ? View.VISIBLE : View.GONE);
            }
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
