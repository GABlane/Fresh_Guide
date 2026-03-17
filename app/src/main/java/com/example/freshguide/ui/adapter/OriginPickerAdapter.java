package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.OriginEntity;

import java.util.ArrayList;
import java.util.List;

public class OriginPickerAdapter extends RecyclerView.Adapter<OriginPickerAdapter.ViewHolder> {

    public interface OnOriginClickListener {
        void onOriginClick(OriginEntity origin);
    }

    private final List<OriginEntity> items = new ArrayList<>();
    private OnOriginClickListener listener;

    public void setItems(List<OriginEntity> origins) {
        items.clear();
        if (origins != null) {
            items.addAll(origins);
        }
        notifyDataSetChanged();
    }

    public void setOnOriginClickListener(OnOriginClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_origin_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OriginEntity origin = items.get(position);
        holder.bind(origin);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOriginClick(origin);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvCode;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_origin_name);
            tvCode = itemView.findViewById(R.id.tv_origin_code);
        }

        void bind(OriginEntity origin) {
            tvName.setText(origin.name != null ? origin.name : "Origin");
            tvCode.setText(origin.code != null ? origin.code : "");
        }
    }
}
