package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.RoomEntity;

import java.util.ArrayList;
import java.util.List;

public class DirectionRoomAdapter extends RecyclerView.Adapter<DirectionRoomAdapter.ViewHolder> {

    public interface OnRoomSelectedListener {
        void onRoomSelected(RoomEntity room);
    }

    private final List<RoomEntity> rooms = new ArrayList<>();
    private int selectedRoomId = -1;
    private OnRoomSelectedListener listener;

    public void setRooms(List<RoomEntity> items) {
        rooms.clear();
        if (items != null) rooms.addAll(items);
        notifyDataSetChanged();
    }

    public void setSelectedRoomId(int roomId) {
        selectedRoomId = roomId;
        notifyDataSetChanged();
    }

    public void setOnRoomSelectedListener(OnRoomSelectedListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_direction_room, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomEntity room = rooms.get(position);
        boolean selected = room.id == selectedRoomId;
        holder.bind(room, selected);
        holder.itemView.setOnClickListener(v -> {
            selectedRoomId = room.id;
            notifyDataSetChanged();
            if (listener != null) listener.onRoomSelected(room);
        });
    }

    @Override
    public int getItemCount() { return rooms.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final TextView selected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_room_title);
            subtitle = itemView.findViewById(R.id.tv_room_subtitle);
            selected = itemView.findViewById(R.id.tv_selected);
        }

        void bind(RoomEntity room, boolean isSelected) {
            title.setText(room.name != null ? room.name : "Room");
            subtitle.setText(room.code != null ? room.code : "");
            itemView.setBackgroundResource(isSelected ?
                    R.drawable.bg_room_item_selected : R.drawable.bg_room_item);
            selected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
    }
}
