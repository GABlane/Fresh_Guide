package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic two-line list adapter for admin list screens (buildings, floors, origins, etc.)
 */
public class GenericListAdapter extends RecyclerView.Adapter<GenericListAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(int position, int id);
        void onDelete(int position, int id);
    }

    public static class Item {
        public int id;
        public String title;
        public String subtitle;

        public Item(int id, String title, String subtitle) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private List<Item> items = new ArrayList<>();
    private OnActionListener listener;
    private boolean actionsEnabled = true;

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnActionListener(OnActionListener l) {
        listener = l;
    }

    public void setActionsEnabled(boolean enabled) {
        actionsEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generic, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item);

        int actionVisibility = actionsEnabled ? View.VISIBLE : View.GONE;
        holder.btnEdit.setVisibility(actionVisibility);
        holder.btnDelete.setVisibility(actionVisibility);

        if (actionsEnabled) {
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(position, item.id);
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(position, item.id);
            });
        } else {
            holder.btnEdit.setOnClickListener(null);
            holder.btnDelete.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Item item) {
            tvTitle.setText(item.title);
            tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");
        }
    }
}
