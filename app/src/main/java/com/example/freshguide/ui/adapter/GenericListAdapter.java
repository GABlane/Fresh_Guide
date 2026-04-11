package com.example.freshguide.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;

/**
 * Generic two-line list adapter for admin list screens (buildings, floors, origins, etc.)
 * Now using DiffUtil for efficient updates instead of notifyDataSetChanged.
 */
public class GenericListAdapter extends ListAdapter<GenericListAdapter.Item, GenericListAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(int position, int id);
        void onDelete(int position, int id);
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Item item);
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

        // Override equals and hashCode for DiffUtil
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return id == item.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.id == newItem.id
                    && (oldItem.title != null ? oldItem.title.equals(newItem.title) : newItem.title == null)
                    && (oldItem.subtitle != null ? oldItem.subtitle.equals(newItem.subtitle) : newItem.subtitle == null);
        }
    };

    private OnActionListener listener;
    private OnItemClickListener itemClickListener;
    private boolean actionsEnabled = true;
    private boolean editEnabled = true;
    private boolean deleteEnabled = true;
    private boolean iconActionsEnabled = false;

    public GenericListAdapter() {
        super(DIFF_CALLBACK);
    }

    // Renamed from setItems to submitList (ListAdapter convention)
    // Old code can still call setItems, we'll redirect to submitList
    public void setItems(java.util.List<Item> items) {
        submitList(items);
    }

    public void setOnActionListener(OnActionListener l) {
        listener = l;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setActionsEnabled(boolean enabled) {
        if (actionsEnabled != enabled) {
            actionsEnabled = enabled;
            notifyDataSetChanged(); // Only notify when actions visibility changes
        }
    }

    public void setEditEnabled(boolean enabled) {
        editEnabled = enabled;
        notifyDataSetChanged();
    }

    public void setDeleteEnabled(boolean enabled) {
        deleteEnabled = enabled;
        notifyDataSetChanged();
    }

    public void setIconActionsEnabled(boolean enabled) {
        if (iconActionsEnabled != enabled) {
            iconActionsEnabled = enabled;
            notifyDataSetChanged();
        }
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
        Item item = getItem(position); // Use getItem() from ListAdapter instead of items.get()
        holder.bind(item);
        holder.itemView.setOnClickListener(itemClickListener == null
                ? null
                : v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    itemClickListener.onItemClick(currentPosition, getItem(currentPosition));
                });

        boolean showEdit = actionsEnabled && editEnabled;
        boolean showDelete = actionsEnabled && deleteEnabled;
        boolean showAnyAction = showEdit || showDelete;

        holder.textActionGroup.setVisibility(!iconActionsEnabled && showAnyAction ? View.VISIBLE : View.GONE);
        holder.iconActionGroup.setVisibility(iconActionsEnabled && showAnyAction ? View.VISIBLE : View.GONE);

        holder.btnEdit.setVisibility(!iconActionsEnabled && showEdit ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(!iconActionsEnabled && showDelete ? View.VISIBLE : View.GONE);
        holder.btnEditIcon.setVisibility(iconActionsEnabled && showEdit ? View.VISIBLE : View.GONE);
        holder.btnDeleteIcon.setVisibility(iconActionsEnabled && showDelete ? View.VISIBLE : View.GONE);

        bindAction(holder.btnEdit, !iconActionsEnabled && showEdit, holder, true);
        bindAction(holder.btnDelete, !iconActionsEnabled && showDelete, holder, false);
        bindAction(holder.btnEditIcon, iconActionsEnabled && showEdit, holder, true);
        bindAction(holder.btnDeleteIcon, iconActionsEnabled && showDelete, holder, false);
    }

    private void bindAction(View actionView, boolean visible, @NonNull ViewHolder holder, boolean editAction) {
        if (!visible) {
            actionView.setOnClickListener(null);
            return;
        }

        actionView.setOnClickListener(v -> {
            if (listener == null) {
                return;
            }
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }
            Item currentItem = getItem(currentPosition);
            if (editAction) {
                listener.onEdit(currentPosition, currentItem.id);
            } else {
                listener.onDelete(currentPosition, currentItem.id);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, btnEdit, btnDelete;
        View textActionGroup, iconActionGroup;
        ImageButton btnEditIcon, btnDeleteIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            textActionGroup = itemView.findViewById(R.id.layout_text_actions);
            iconActionGroup = itemView.findViewById(R.id.layout_icon_actions);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnEditIcon = itemView.findViewById(R.id.btn_edit_icon);
            btnDeleteIcon = itemView.findViewById(R.id.btn_delete_icon);
        }

        void bind(Item item) {
            tvTitle.setText(item.title);
            tvSubtitle.setText(item.subtitle != null ? item.subtitle : "");
        }
    }
}
