package com.example.freshguide.ui.user;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.repository.SavedRoomRepository;
import com.example.freshguide.ui.adapter.RoomImageGalleryAdapter;
import com.example.freshguide.util.RoomImageCacheManager;
import com.example.freshguide.util.RoomImageUrlResolver;
import com.example.freshguide.viewmodel.RoomDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomDetailFragment extends BottomSheetDialogFragment {

    public static final String ARG_ROOM_ID = "roomId";

    private RoomDetailViewModel viewModel;
    private int roomId;
    private boolean isCampusArea;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private String latestImageUrl;
    private RoomImageGalleryAdapter galleryAdapter;
    private View galleryFadeLeft;
    private View galleryFadeRight;
    private View singleImageCard;
    private ImageView singleImageView;
    private TextView singleImagePlaceholder;
    private ImageButton btnBookmark;

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_FreshGuide_MapBottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_detail, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0f);
            }
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(R.drawable.bg_room_detail_sheet);
                sheet.setElevation(dpToPx(22));
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                if (params != null) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    sheet.setLayoutParams(params);
                }
                // Place-details sheet: visible over the map, draggable between collapsed, half, and full.
                behavior.setFitToContents(false);
                behavior.setExpandedOffset(dpToPx(14));
                behavior.setHalfExpandedRatio(0.5f);
                behavior.setPeekHeight(dpToPx(86), true);
                behavior.setSkipCollapsed(false);
                behavior.setHideable(false);
                behavior.setDraggable(true);
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Checklist 5.2: receive data from args
        roomId = requireArguments().getInt(ARG_ROOM_ID, -1);
        isCampusArea = requireArguments().getBoolean("isCampusArea", false);
        viewModel = new ViewModelProvider(this).get(RoomDetailViewModel.class);

        TextView tvName = view.findViewById(R.id.tv_room_name);
        TextView tvSubtitle = view.findViewById(R.id.tv_room_subtitle);
        TextView tvType = view.findViewById(R.id.tv_room_type);
        TextView tvDescription = view.findViewById(R.id.tv_room_description);
        TextView tvFacilities = view.findViewById(R.id.tv_facilities);
        View btnDirections = view.findViewById(R.id.btn_get_directions);
        btnBookmark = view.findViewById(R.id.btn_room_bookmark);
        RecyclerView galleryRecycler = view.findViewById(R.id.recycler_room_gallery);
        galleryFadeLeft = view.findViewById(R.id.gallery_fade_left);
        galleryFadeRight = view.findViewById(R.id.gallery_fade_right);
        singleImageCard = view.findViewById(R.id.single_image_card);
        singleImageView = view.findViewById(R.id.iv_room_image);
        singleImagePlaceholder = view.findViewById(R.id.tv_image_placeholder);
        NavController nav = NavHostFragment.findNavController(this);

        galleryAdapter = new RoomImageGalleryAdapter();
        LinearLayoutManager galleryLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        galleryRecycler.setLayoutManager(galleryLayoutManager);
        galleryRecycler.setAdapter(galleryAdapter);
        galleryRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateGalleryFades(recyclerView);
            }
        });
        galleryRecycler.post(() -> updateGalleryFades(galleryRecycler));

        btnBookmark.setOnClickListener(v ->
                viewModel.toggleSaved(new SavedRoomRepository.ToggleCallback() {
                    @Override
                    public void onComplete(boolean isSaved) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(
                                requireContext(),
                                isSaved ? R.string.saved_location_added : R.string.saved_location_removed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                }));

        viewModel.getRoom().observe(getViewLifecycleOwner(), room -> {
            if (room == null) return;
            tvName.setText(room.name);
            tvSubtitle.setText(buildSubtitle(room.code, room.location));

            if (room.type != null && !room.type.isBlank()) {
                tvType.setVisibility(View.VISIBLE);
                tvType.setText(room.type.toUpperCase(Locale.getDefault()));
            } else {
                tvType.setVisibility(View.GONE);
            }

            String description = room.description != null ? room.description : "";
            if (description.isBlank()) {
                description = "No description available.";
            }
            tvDescription.setText(description);

            String resolvedImageUrl = RoomImageUrlResolver.resolvePath(requireContext(), room.imageUrl);
            loadRoomImages(room.cachedImagePath, resolvedImageUrl, galleryRecycler);
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
            if (err == null) return;

            if (isCampusArea) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                nav.navigate(R.id.homeFragment);
                return;
            }

            Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getIsSaved().observe(getViewLifecycleOwner(), saved -> updateBookmarkState(Boolean.TRUE.equals(saved)));

        // Open the directions modal first (same flow as compass FAB).
        btnDirections.setOnClickListener(v -> {
            if (roomId <= 0) {
                Toast.makeText(requireContext(), "Invalid room", Toast.LENGTH_SHORT).show();
                return;
            }

            DirectionsSheetFragment sheet = new DirectionsSheetFragment();
            Bundle sheetArgs = new Bundle();
            sheetArgs.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_ID, roomId);
            sheetArgs.putString(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_NAME, tvName.getText().toString());
            sheet.setArguments(sheetArgs);
            sheet.show(getParentFragmentManager(), "directions_sheet");
            dismissAllowingStateLoss();
        });

        if (roomId != -1) {
            viewModel.loadRoom(roomId);
        }
    }

    private void updateBookmarkState(boolean isSaved) {
        if (btnBookmark == null) {
            return;
        }
        btnBookmark.setImageDrawable(AppCompatResources.getDrawable(
                requireContext(),
                isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline
        ));
        btnBookmark.setContentDescription(getString(
                isSaved ? R.string.saved_location_remove : R.string.saved_location_add
        ));
    }

    private String buildSubtitle(String code, String location) {
        String c = code != null ? code.trim() : "";
        String l = location != null ? location.trim() : "";
        if (!l.isEmpty() && !c.isEmpty()) {
            return l + " • " + c;
        }
        if (!l.isEmpty()) {
            return l;
        }
        if (!c.isEmpty()) {
            return c;
        }
        return "Location details unavailable";
    }

    private void loadRoomImages(String cachedImagePath, String imageUrl, RecyclerView galleryRecycler) {
        List<RoomImageGalleryAdapter.GalleryItem> placeholders = new ArrayList<>();
        placeholders.add(new RoomImageGalleryAdapter.GalleryItem(null, "No room image"));
        showSingleImage(null);

        if (cachedImagePath != null && !cachedImagePath.trim().isEmpty()) {
            File cachedFile = new File(cachedImagePath);
            if (cachedFile.exists()) {
                Bitmap cachedBitmap = BitmapFactory.decodeFile(cachedFile.getAbsolutePath());
                if (cachedBitmap != null) {
                    showSingleImage(cachedBitmap);
                    return;
                }
            }
        }

        latestImageUrl = imageUrl;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        final android.content.Context appContext = requireContext().getApplicationContext();
        final AppDatabase db = AppDatabase.getInstance(appContext);

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
            String cachedPath = null;
            if (finalBitmap != null && roomId > 0) {
                cachedPath = RoomImageCacheManager.cacheRoomBitmap(appContext, roomId, finalBitmap);
                if (cachedPath != null && !cachedPath.isBlank()) {
                    RoomEntity localRoom = db.roomDao().getByIdSync(roomId);
                    if (localRoom != null) {
                        localRoom.cachedImagePath = cachedPath;
                        if (localRoom.imageUrl == null || localRoom.imageUrl.isBlank()) {
                            localRoom.imageUrl = imageUrl;
                        }
                        db.roomDao().insert(localRoom);
                    }
                }
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (latestImageUrl == null || !latestImageUrl.equals(imageUrl)) return;

                if (finalBitmap != null) {
                    showSingleImage(finalBitmap);
                } else {
                    showSingleImage(null);
                }
            });
        });
    }

    private void showSingleImage(@Nullable Bitmap bitmap) {
        if (singleImageCard == null || singleImageView == null || singleImagePlaceholder == null) {
            return;
        }
        singleImageCard.setVisibility(View.VISIBLE);
        if (bitmap != null) {
            singleImageView.setImageBitmap(bitmap);
            singleImageView.setVisibility(View.VISIBLE);
            singleImagePlaceholder.setVisibility(View.GONE);
        } else {
            singleImageView.setImageDrawable(null);
            singleImageView.setVisibility(View.VISIBLE);
            singleImagePlaceholder.setVisibility(View.VISIBLE);
        }
        if (galleryFadeLeft != null) {
            galleryFadeLeft.setVisibility(View.GONE);
        }
        if (galleryFadeRight != null) {
            galleryFadeRight.setVisibility(View.GONE);
        }
    }

    private void updateGalleryFades(@NonNull RecyclerView galleryRecycler) {
        if (galleryFadeLeft == null || galleryFadeRight == null) {
            return;
        }
        boolean canScrollLeft = galleryRecycler.canScrollHorizontally(-1);
        boolean canScrollRight = galleryRecycler.canScrollHorizontally(1);
        galleryFadeLeft.setVisibility(canScrollLeft ? View.VISIBLE : View.GONE);
        galleryFadeRight.setVisibility(canScrollRight ? View.VISIBLE : View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Shutdown executor to prevent thread leaks
        if (imageExecutor != null && !imageExecutor.isShutdown()) {
            imageExecutor.shutdown();
        }
    }
}
