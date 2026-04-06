package com.example.freshguide.ui.adapter;

import android.graphics.Bitmap;
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

public class RoomImageGalleryAdapter extends RecyclerView.Adapter<RoomImageGalleryAdapter.ViewHolder> {

    public static class GalleryItem {
        public final Bitmap bitmap;
        public final String label;

        public GalleryItem(Bitmap bitmap, String label) {
            this.bitmap = bitmap;
            this.label = label;
        }
    }

    private final List<GalleryItem> items = new ArrayList<>();

    public void submitList(List<GalleryItem> galleryItems) {
        items.clear();
        if (galleryItems != null) {
            items.addAll(galleryItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView placeholder;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_gallery_image);
            placeholder = itemView.findViewById(R.id.tv_gallery_placeholder);
        }

        void bind(GalleryItem item) {
            if (item.bitmap != null) {
                image.setImageBitmap(item.bitmap);
                image.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
            } else {
                image.setImageDrawable(null);
                image.setVisibility(View.INVISIBLE);
                placeholder.setText(item.label != null ? item.label : "No image");
                placeholder.setVisibility(View.VISIBLE);
            }
        }
    }
}
