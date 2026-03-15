package com.example.freshguide.ui.user;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.viewmodel.RoomDetailViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomDetailFragment extends Fragment {

    public static final String ARG_ROOM_ID = "roomId";

    private RoomDetailViewModel viewModel;
    private int roomId;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private String latestImageUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Checklist 5.2: receive data from args
        roomId = requireArguments().getInt(ARG_ROOM_ID, -1);
        viewModel = new ViewModelProvider(this).get(RoomDetailViewModel.class);

        TextView tvName = view.findViewById(R.id.tv_room_name);
        TextView tvCode = view.findViewById(R.id.tv_room_code);
        TextView tvType = view.findViewById(R.id.tv_room_type);
        TextView tvDescription = view.findViewById(R.id.tv_room_description);
        TextView tvFacilities = view.findViewById(R.id.tv_facilities);
        ImageView ivRoomImage = view.findViewById(R.id.iv_room_image);
        Button btnDirections = view.findViewById(R.id.btn_get_directions);

        viewModel.getRoom().observe(getViewLifecycleOwner(), room -> {
            if (room == null) return;
            tvName.setText(room.name);
            tvCode.setText(room.code);
            tvType.setText(room.type != null ? room.type : "");
            String description = room.description != null ? room.description : "";
            if (room.location != null && !room.location.isEmpty()) {
                description = description.isEmpty()
                        ? "Location: " + room.location
                        : description + "\nLocation: " + room.location;
            }
            tvDescription.setText(description);

            loadRoomImage(room.imageUrl, ivRoomImage);
        });

        viewModel.getFacilities().observe(getViewLifecycleOwner(), facilities -> {
            if (facilities == null || facilities.isEmpty()) {
                tvFacilities.setText("No facilities listed");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (FacilityEntity f : facilities) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(f.name);
            }
            tvFacilities.setText(sb.toString());
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        NavController nav = Navigation.findNavController(view);

        // Checklist 5.3: navigate to origin picker, receive result back
        btnDirections.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("roomId", roomId);
            nav.navigate(R.id.action_roomDetail_to_originPicker, args);
        });

        // Listen for origin selection result (set by OriginPickerFragment)
        getParentFragmentManager().setFragmentResultListener(
                OriginPickerFragment.RESULT_KEY, getViewLifecycleOwner(),
                (requestKey, result) -> {
                    int originId = result.getInt(OriginPickerFragment.KEY_ORIGIN_ID, -1);
                    if (originId != -1) {
                        Bundle dirArgs = new Bundle();
                        dirArgs.putInt("roomId", roomId);
                        dirArgs.putInt("originId", originId);
                        nav.navigate(R.id.action_roomDetail_to_directions, dirArgs);
                    }
                });

        if (roomId != -1) {
            viewModel.loadRoom(roomId);
        }
    }

    private void loadRoomImage(String imageUrl, ImageView imageView) {
        latestImageUrl = imageUrl;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageView.setImageBitmap(null);
            imageView.setVisibility(View.GONE);
            return;
        }

        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(null);

        imageExecutor.execute(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            Bitmap bitmap = null;

            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.connect();

                inputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (Exception ignored) {
                bitmap = null;
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }

            Bitmap finalBitmap = bitmap;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (latestImageUrl == null || !latestImageUrl.equals(imageUrl)) return;

                if (finalBitmap != null) {
                    imageView.setImageBitmap(finalBitmap);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    imageView.setImageBitmap(null);
                    imageView.setVisibility(View.GONE);
                }
            });
        });
    }

}
