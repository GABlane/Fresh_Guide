package com.example.freshguide.ui.user;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.repository.SavedRoomRepository;
import com.example.freshguide.ui.adapter.RoomImageGalleryAdapter;
import com.example.freshguide.util.RoomImageCacheManager;
import com.example.freshguide.util.RoomImageUrlResolver;
import com.example.freshguide.viewmodel.HomeViewModel;
import com.example.freshguide.viewmodel.RoomDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String KEY_PENDING_ROOM_ID = "roomId";
    private static final String KEY_PENDING_FLOOR_NUMBER = "floorNumber";
    private static final String KEY_PENDING_ROOM_NAME = "roomName";
    private static final String KEY_PENDING_BUILDING_CODE = "buildingCode";
    private static final String CODE_MAIN = "MAIN";
    private static final String CODE_REG = "REG";
    private static final String CODE_LIB = "LIB";
    private static final String CODE_COURT = "COURT";
    private static final String CODE_ENT = "ENT";
    private static final String CODE_EXIT = "EXIT";
    private static final String TAG = "HomeFragment";
    private static final int[] DEFAULT_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_left_4,
            R.id.room_left_5,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_right_4,
            R.id.room_right_5
    };
    private static final int[] FLOOR1_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_left_4,
            R.id.room_left_5,
            R.id.room_left_6,
            R.id.room_left_7,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_right_4,
            R.id.room_right_5,
            R.id.room_right_6,
            R.id.room_right_7,
            R.id.room_right_8
    };
    private static final int[] FLOOR5_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_auditorium
    };
    private static final Map<String, Integer> FLOOR1_ROOM_SLOTS = createFloor1RoomSlots();

    private ExecutorService ioExecutor;

    private Integer selectedFloor = null;

    private HomeViewModel viewModel;
    private RoomDetailViewModel roomDetailViewModel;

    private Chip[] floorChips;
    private HorizontalScrollView floorChipContainer;
    private FrameLayout floorMapContainer;
    private View leftFade;
    private View rightFade;
    private View overallMapContainer;
    private Integer pendingFocusedRoomId;
    private Integer highlightedRoomId;
    private String pendingFocusedRoomName;
    private View roomDetailSheet;
    private BottomSheetBehavior<View> roomDetailSheetBehavior;
    private View roomSummaryLayout;
    private TextView tvRoomName;
    private TextView tvRoomSubtitle;
    private TextView tvRoomType;
    private TextView tvRoomDescription;
    private TextView tvFacilities;
    private MaterialButton btnGoToMap;
    private MaterialButton btnDirections;
    private ImageButton btnBookmark;
    private RoomImageGalleryAdapter galleryAdapter;
    private View galleryFadeLeft;
    private View galleryFadeRight;
    private View singleImageCard;
    private ImageView singleImageView;
    private TextView singleImagePlaceholder;
    private FloatingActionButton fabCompass;
    private int activeRoomId = -1;
    private boolean activeRoomIsCampusArea;
    private RoomEntity activeRoom;
    private String latestImageUrl;
    private Runnable pendingAfterRoomSheetHidden;

    private static Map<String, Integer> createFloor1RoomSlots() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("KITCHEN_LAB", R.id.room_left_1);
        map.put("BARTENDER_LAB", R.id.room_left_2);
        map.put("108", R.id.room_left_3);
        map.put("105", R.id.room_left_4);
        map.put("104", R.id.room_left_5);
        map.put("IT_CENTER", R.id.room_left_6);
        map.put("GUIDANCE", R.id.room_left_7);
        map.put("PWD_CR", R.id.room_right_1);
        map.put("109", R.id.room_right_2);
        map.put("FACULTY", R.id.room_right_3);
        map.put("HR_OFFICE", R.id.room_right_4);
        map.put("FINANCE", R.id.room_right_5);
        map.put("PHOTO_LAB", R.id.room_right_6);
        map.put("CRIMINOLOGY", R.id.room_right_7);
        map.put("101", R.id.room_right_8);
        return Collections.unmodifiableMap(map);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize executor for this view lifecycle
        ioExecutor = Executors.newSingleThreadExecutor();

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        roomDetailViewModel = new ViewModelProvider(this).get(RoomDetailViewModel.class);

        floorChipContainer = view.findViewById(R.id.floor_chip_container);
        floorMapContainer = view.findViewById(R.id.floor_map_container);
        leftFade = view.findViewById(R.id.leftFade);
        rightFade = view.findViewById(R.id.rightFade);
        overallMapContainer = view.findViewById(R.id.overall_map_container);
        roomDetailSheet = view.findViewById(R.id.room_detail_sheet);
        fabCompass = view.findViewById(R.id.fab_compass);

        NavController nav = Navigation.findNavController(view);

        applyBuildingPinOverlays(view);
        setupRoomDetailSheet(view);
        setupSearch(view, nav);
        setupFloorChips(view);
        setupOverallMapClicks(view, nav);
        setupChipFade();
        setupFab();
        observeSync(view);
        observeMapFocusRequests();

        viewModel.sync();
    }

    private void setupSearch(View view, NavController nav) {
        View searchBar = view.findViewById(R.id.layout_search);
        searchBar.setOnClickListener(v -> {
            NavOptions options = new NavOptions.Builder()
                    .setEnterAnim(R.anim.search_screen_enter)
                    .setExitAnim(R.anim.home_screen_exit)
                    .setPopEnterAnim(R.anim.home_screen_reenter)
                    .setPopExitAnim(R.anim.search_screen_exit)
                    .build();

            v.animate()
                    .cancel();
            v.animate()
                    .scaleX(0.996f)
                    .scaleY(0.996f)
                    .alpha(0.985f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        v.setAlpha(1f);
                        closeRoomDetailSheetIfOpen(() -> nav.navigate(R.id.action_home_to_roomList, null, options));
                    })
                    .start();
        });
    }

    private void setupFloorChips(View view) {
        Chip chip1 = view.findViewById(R.id.chip_floor_1);
        Chip chip2 = view.findViewById(R.id.chip_floor_2);
        Chip chip3 = view.findViewById(R.id.chip_floor_3);
        Chip chip4 = view.findViewById(R.id.chip_floor_4);
        Chip chip5 = view.findViewById(R.id.chip_floor_5);

        floorChips = new Chip[]{chip1, chip2, chip3, chip4, chip5};

        int selectedBg = ContextCompat.getColor(requireContext(), R.color.floor_chip_selected_bg);
        int uncheckedBg = ContextCompat.getColor(requireContext(), R.color.floor_chip_unselected_bg);
        int selectedText = ContextCompat.getColor(requireContext(), R.color.floor_chip_selected_text);
        int uncheckedText = ContextCompat.getColor(requireContext(), R.color.floor_chip_unselected_text);
        int stroke = ContextCompat.getColor(requireContext(), R.color.floor_chip_stroke);

        ColorStateList bgColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        selectedBg,
                        uncheckedBg
                }
        );

        ColorStateList textColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        selectedText,
                        uncheckedText
                }
        );

        for (Chip chip : floorChips) {
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setSaveEnabled(false);
            chip.setChipBackgroundColor(bgColors);
            chip.setTextColor(textColors);
            chip.setChipStrokeColor(ColorStateList.valueOf(stroke));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 0.5f);
        }

        chip1.setOnClickListener(v -> handleFloorChipClick(chip1, 1, floorChips));
        chip2.setOnClickListener(v -> handleFloorChipClick(chip2, 2, floorChips));
        chip3.setOnClickListener(v -> handleFloorChipClick(chip3, 3, floorChips));
        chip4.setOnClickListener(v -> handleFloorChipClick(chip4, 4, floorChips));
        chip5.setOnClickListener(v -> handleFloorChipClick(chip5, 5, floorChips));

        setFloorSelection(selectedFloor);
        updateFade();
    }

    private void handleFloorChipClick(Chip clickedChip, int floor, Chip[] allChips) {
        if (selectedFloor != null && selectedFloor == floor) {
            setFloorSelection(null);
            return;
        }

        setFloorSelection(floor);
    }

    private void showOverallMap() {
        clearRoomHighlight();

        if (overallMapContainer != null) {
            overallMapContainer.setVisibility(View.VISIBLE);
        }

        if (floorMapContainer != null) {
            floorMapContainer.setVisibility(View.GONE);
            floorMapContainer.removeAllViews();
        }
    }

    private void showFloorMap(int floor) {
        if (overallMapContainer != null) {
            overallMapContainer.setVisibility(View.GONE);
        }

        if (floorMapContainer != null) {
            floorMapContainer.setVisibility(View.VISIBLE);
            floorMapContainer.removeAllViews();

            int layoutResId = 0;

            switch (floor) {
                case 1:
                    layoutResId = R.layout.map_floor_1;
                    break;
                case 2:
                    layoutResId = R.layout.map_floor_2;
                    break;
                case 3:
                    layoutResId = R.layout.map_floor_3;
                    break;
                case 4:
                    layoutResId = R.layout.map_floor_4;
                    break;
                case 5:
                    layoutResId = R.layout.map_floor_5;
                    break;
            }

            if (layoutResId != 0) {
                LayoutInflater.from(requireContext()).inflate(layoutResId, floorMapContainer, true);
                floorMapContainer.setAlpha(0f);
                floorMapContainer.setTranslationY(10f);
                floorMapContainer.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220L)
                        .start();
                bindFloorData(floor);
            }
        }
    }

    private void setupOverallMapClicks(View view, NavController nav) {
        setClick(view, R.id.img_main_building, () -> openMainBuildingFloor());
        setClick(view, R.id.img_pin_main, () -> openMainBuildingFloor());
        setClick(view, R.id.txt_main_building, () -> openMainBuildingFloor());

        setClick(view, R.id.img_library, () -> navigateToBuildingRoomList(nav, CODE_LIB, "LIBRARY"));
        setClick(view, R.id.img_pin_library, () -> navigateToBuildingRoomList(nav, CODE_LIB, "LIBRARY"));
        setClick(view, R.id.txt_library, () -> navigateToBuildingRoomList(nav, CODE_LIB, "LIBRARY"));

        setClick(view, R.id.img_registrar, () -> navigateToBuildingRoomList(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.img_registrar2, () -> navigateToBuildingRoomList(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.img_pin_registrar, () -> navigateToBuildingRoomList(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.txt_registrar, () -> navigateToBuildingRoomList(nav, CODE_REG, "REGISTRAR"));

        setClick(view, R.id.img_court, () -> navigateToCampusAreaRoom(nav, CODE_COURT, "COURT"));
        setClick(view, R.id.txt_court, () -> navigateToCampusAreaRoom(nav, CODE_COURT, "COURT"));
        setClick(view, R.id.img_entrance, () -> navigateToCampusAreaRoom(nav, CODE_ENT, "ENTRANCE"));
        setClick(view, R.id.img_exit, () -> navigateToCampusAreaRoom(nav, CODE_EXIT, "EXIT"));
    }

    private void applyBuildingPinOverlays(View root) {
        setOverlayPin(root, R.id.img_pin_main, resolvePinDrawable("pin_main", "main_pin", "main_building_pin"));
        setOverlayPin(root, R.id.img_pin_library, resolvePinDrawable("pin_lib", "pin_library", "library_pin"));
        setOverlayPin(root, R.id.img_pin_registrar, resolvePinDrawable("pin_reg", "pin_registrar", "registrar_pin"));
    }

    private void setOverlayPin(View root, int viewId, int drawableResId) {
        View target = root.findViewById(viewId);
        if (target instanceof ImageView) {
            ((ImageView) target).setImageResource(drawableResId);
        }
    }

    private int resolvePinDrawable(String... drawableNames) {
        if (!isAdded()) {
            return R.drawable.pin_placeholder;
        }

        for (String name : drawableNames) {
            int id = requireContext().getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
            if (id != 0) {
                return id;
            }
        }
        return R.drawable.pin_placeholder;
    }

    private void setClick(View root, int viewId, Runnable action) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(v -> action.run());
        }
    }

    private void openMainBuildingFloor() {
        setFloorSelection(1);
    }

    private void navigateToBuildingRoomList(NavController nav, String buildingCode, String buildingName) {
        Bundle args = new Bundle();
        args.putString("buildingCode", buildingCode);
        args.putString("buildingName", buildingName);
        nav.navigate(R.id.action_home_to_roomList, args);
    }

    private void bindFloorData(int floorNumber) {
        ioExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());

                // Optimized: Single JOIN query instead of 3 sequential queries
                List<RoomEntity> rooms = db.roomDao().getRoomsByBuildingAndFloorSync(CODE_MAIN, floorNumber);

                if (rooms == null || rooms.isEmpty()) {
                    Log.w(TAG, "No rooms found for building=" + CODE_MAIN + ", floor=" + floorNumber);
                    runOnUiThreadSafely(this::clearFloorRoomViews);
                    return;
                }

                // Sort by ID for consistent ordering
                rooms.sort(Comparator.comparingInt(r -> r.id));
                Log.d(TAG, "Binding floor=" + floorNumber + " with rooms=" + rooms.size());

                runOnUiThreadSafely(() -> applyRoomsToCurrentFloorLayout(rooms));

            } catch (Exception e) {
                Log.e(TAG, "Failed loading floor data", e);
                runOnUiThreadSafely(() -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load floor data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void applyRoomsToCurrentFloorLayout(List<RoomEntity> rooms) {
        if (floorMapContainer == null) return;

        clearFloorRoomViews();

        List<RoomEntity> floorRooms = new ArrayList<>();
        if (rooms != null) {
            for (RoomEntity room : rooms) {
                if (room == null) continue;
                if (isCampusAreaCode(room.code)) continue;
                floorRooms.add(room);
            }
        }

        if (isFloorOneSelected()) {
            if (floorRooms.isEmpty()) {
                Log.w(TAG, "No Floor 1 rooms found for static slot mapping");
            }
            applyFloorOneRoomsByCode(floorRooms);
            return;
        }
        if (isFloorFiveSelected()) {
            applyFloorFiveStaticLayout(floorRooms);
            return;
        }

        if (floorRooms.isEmpty()) {
            Log.w(TAG, "Only campus-area rooms found for current floor; nothing to bind");
            return;
        }

        RoomEntity pendingRoomToFocus = null;
        View pendingRoomBoxToFocus = null;
        int max = Math.min(floorRooms.size(), DEFAULT_ROOM_BOX_IDS.length);
        for (int i = 0; i < max; i++) {
            RoomEntity room = floorRooms.get(i);

            View roomBox = floorMapContainer.findViewById(DEFAULT_ROOM_BOX_IDS[i]);

            if (roomBox != null) {
                bindRoomToBox(roomBox, room);

                if (pendingFocusedRoomId != null && pendingFocusedRoomId == room.id) {
                    pendingRoomToFocus = room;
                    pendingRoomBoxToFocus = roomBox;
                }
            } else {
                Log.w(TAG, "Room view not found for index=" + i);
            }
        }

        if (pendingRoomToFocus != null && pendingRoomBoxToFocus != null) {
            RoomEntity roomToFocus = pendingRoomToFocus;
            View roomBoxToFocus = pendingRoomBoxToFocus;
            floorMapContainer.post(() -> {
                if (!isAdded() || floorMapContainer == null) {
                    return;
                }
                openRoomOnMap(roomToFocus, roomBoxToFocus, true);
            });
        }
    }

    private void applyFloorFiveStaticLayout(@NonNull List<RoomEntity> floorRooms) {
        bindStaticRoomLabel(R.id.room_left_1, "V-48");
        bindStaticRoomLabel(R.id.room_left_2, "V-46");
        bindStaticRoomLabel(R.id.room_left_3, "Industrial\nArts\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_1, "Speech\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_2, "Craft /\nSewing\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_3, "Office");
        bindStaticRoomLabel(R.id.room_auditorium, "Auditorium");

        View auditoriumBox = floorMapContainer.findViewById(R.id.room_auditorium);
        if (auditoriumBox == null) {
            return;
        }

        RoomEntity auditorium = findFloorFiveAuditorium(floorRooms);
        if (auditorium == null) {
            disableRoomBoxInteraction(auditoriumBox);
            return;
        }

        bindRoomToBox(auditoriumBox, auditorium);
        TextView auditoriumLabel = auditoriumBox.findViewById(R.id.room_label);
        if (auditoriumLabel != null) {
            auditoriumLabel.setText("Auditorium");
        }
        if (pendingFocusedRoomId != null && pendingFocusedRoomId == auditorium.id) {
            openRoomOnMap(auditorium, auditoriumBox, true);
        }
    }

    private void bindStaticRoomLabel(int roomViewId, @NonNull String label) {
        if (floorMapContainer == null) {
            return;
        }

        View roomBox = floorMapContainer.findViewById(roomViewId);
        if (roomBox == null) {
            return;
        }

        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(label);
        }

        disableRoomBoxInteraction(roomBox);
        applyRoomHighlightState(roomBox, false);
    }

    private RoomEntity findFloorFiveAuditorium(@NonNull List<RoomEntity> floorRooms) {
        RoomEntity nameMatch = null;
        for (RoomEntity room : floorRooms) {
            if (room == null) {
                continue;
            }

            String code = normalizeCode(room.code);
            if ("MAIN-5-AUDIT".equals(code)) {
                return room;
            }

            if (room.name != null && room.name.toUpperCase().contains("AUDITORIUM")) {
                nameMatch = room;
            }
        }
        return nameMatch;
    }

    private void applyFloorOneRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            if (room == null) {
                continue;
            }

            String code = normalizeCode(room.code);
            if (code == null) {
                continue;
            }

            if (!FLOOR1_ROOM_SLOTS.containsKey(code)) {
                Log.w(TAG, "Floor 1 room code is not mapped to a static slot: " + code);
                continue;
            }

            RoomEntity existing = roomByCode.putIfAbsent(code, room);
            if (existing != null) {
                Log.w(TAG, "Duplicate Floor 1 room code found, keeping first: " + code);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR1_ROOM_SLOTS.entrySet()) {
            View roomBox = floorMapContainer.findViewById(slot.getValue());
            if (roomBox == null) {
                Log.w(TAG, "Missing Floor 1 room view for slot code=" + slot.getKey());
                continue;
            }

            RoomEntity room = roomByCode.get(slot.getKey());
            if (room == null) {
                resetRoomBox(roomBox);
                continue;
            }

            bindRoomToBox(roomBox, room);
            if (pendingFocusedRoomId != null && pendingFocusedRoomId == room.id) {
                openRoomOnMap(room, roomBox, true);
            }
        }
    }

    private void bindRoomToBox(@NonNull View roomBox, @NonNull RoomEntity room) {
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(getRoomDisplayName(room));
        }

        roomBox.setClickable(true);
        roomBox.setFocusable(false);
        roomBox.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });
        roomBox.setOnClickListener(v -> {
            Log.d(TAG, "Room clicked id=" + room.id + " code=" + room.code + " name=" + room.name);
            openRoomOnMap(room, roomBox, true);
        });
    }

    private void clearFloorRoomViews() {
        if (floorMapContainer == null) return;

        for (int roomBoxId : getActiveRoomBoxIds()) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);

            if (roomBox != null) {
                resetRoomBox(roomBox);
            }
        }
        highlightedRoomId = null;
    }

    private void resetRoomBox(@NonNull View roomBox) {
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText("Room");
        }
        disableRoomBoxInteraction(roomBox);
        roomBox.setScaleX(1f);
        roomBox.setScaleY(1f);
        roomBox.setTranslationZ(0f);
        applyRoomHighlightState(roomBox, false);
    }

    private void disableRoomBoxInteraction(@NonNull View roomBox) {
        roomBox.setOnClickListener(null);
        roomBox.setClickable(false);
        roomBox.setFocusable(false);
        roomBox.setOnTouchListener(null);
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String normalized = rawCode.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String getRoomDisplayName(RoomEntity room) {
        if (room == null) return "Room";
        if (room.name != null && !room.name.trim().isEmpty()) return room.name;
        if (room.code != null && !room.code.trim().isEmpty()) return room.code;
        return "Room";
    }

    private boolean isCampusAreaCode(String code) {
        if (code == null) return false;
        String c = code.trim().toUpperCase();
        return CODE_COURT.equals(c) || CODE_ENT.equals(c) || CODE_EXIT.equals(c);
    }

    private void setupChipFade() {
        if (floorChipContainer == null) return;

        floorChipContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        updateFade();
                    }
                }
        );

        floorChipContainer.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                updateFade();
            }
        });
    }

    private void updateFade() {
        if (floorChipContainer == null || leftFade == null || rightFade == null) return;

        View child = floorChipContainer.getChildAt(0);
        if (child == null) return;

        int maxScroll = child.getWidth() - floorChipContainer.getWidth();
        int scrollX = floorChipContainer.getScrollX();

        if (maxScroll <= 0) {
            leftFade.setVisibility(View.GONE);
            rightFade.setVisibility(View.GONE);
            return;
        }

        leftFade.setVisibility(scrollX > 0 ? View.VISIBLE : View.GONE);
        rightFade.setVisibility(scrollX < maxScroll ? View.VISIBLE : View.GONE);
    }

    private void navigateToCampusAreaRoom(NavController nav, String areaCode, String areaName) {
        viewModel.findRoomIdByCode(areaCode, roomId -> {
            if (!isAdded()) {
                return;
            }

            if (roomId == null || roomId <= 0) {
                Toast.makeText(requireContext(), "Campus area not available yet", Toast.LENGTH_SHORT).show();
                return;
            }

            showRoomDetailSheet(roomId, areaName != null ? areaName : areaCode, true);
        });
    }

    private void observeSync(View view) {
        viewModel.getSyncError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Snackbar.make(view, "Sync failed: " + err, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void observeMapFocusRequests() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.getBackStackEntry(navController.getGraph().getId())
                .getSavedStateHandle()
                .<Bundle>getLiveData(RoomListFragment.KEY_MAP_FOCUS_REQUEST)
                .observe(getViewLifecycleOwner(), request -> {
                    if (request == null || !isAdded()) {
                        return;
                    }

                    navController.getBackStackEntry(navController.getGraph().getId())
                            .getSavedStateHandle()
                            .remove(RoomListFragment.KEY_MAP_FOCUS_REQUEST);

                    String buildingCode = request.getString(KEY_PENDING_BUILDING_CODE, "");
                    int floorNumber = request.getInt(KEY_PENDING_FLOOR_NUMBER, -1);
                    int roomId = request.getInt(KEY_PENDING_ROOM_ID, -1);
                    if (!CODE_MAIN.equalsIgnoreCase(buildingCode) || floorNumber <= 0 || roomId <= 0) {
                        return;
                    }

                    pendingFocusedRoomId = roomId;
                    pendingFocusedRoomName = request.getString(KEY_PENDING_ROOM_NAME, "Room");
                    setFloorSelection(floorNumber);
                });
    }

    private void setFloorSelection(@Nullable Integer floorNumber) {
        selectedFloor = floorNumber;
        if (floorChips != null) {
            for (int i = 0; i < floorChips.length; i++) {
                floorChips[i].setChecked(floorNumber != null && (i + 1) == floorNumber);
            }
        }
        if (floorNumber == null) {
            showOverallMap();
            return;
        }
        showFloorMap(floorNumber);
    }

    private void openRoomOnMap(@NonNull RoomEntity room, @NonNull View roomBox, boolean animateCentering) {
        clearRoomHighlight();
        highlightedRoomId = room.id;
        applyRoomHighlightState(roomBox, true);
        roomBox.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(140L)
                .withEndAction(() -> roomBox.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .start())
                .start();
        animateRoomPin(roomBox);
        if (animateCentering) {
            centerRoomInFloorMap(roomBox);
        }

        pendingFocusedRoomId = null;
        pendingFocusedRoomName = null;
        openRoomDetailSheet(room.id, getRoomDisplayName(room));
    }

    private void openRoomDetailSheet(int roomId, String roomName) {
        showRoomDetailSheet(roomId, roomName, false);
    }

    private void closeRoomDetailSheetIfOpen(@NonNull Runnable afterClose) {
        if (roomDetailSheetBehavior != null
                && roomDetailSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN
                && roomDetailSheet != null
                && roomDetailSheet.getVisibility() == View.VISIBLE) {
            pendingAfterRoomSheetHidden = afterClose;
            hideRoomDetailSheet();
            return;
        }
        afterClose.run();
    }

    private void setupRoomDetailSheet(@NonNull View root) {
        if (roomDetailSheet == null) {
            return;
        }

        tvRoomName = root.findViewById(R.id.tv_room_name);
        tvRoomSubtitle = root.findViewById(R.id.tv_room_subtitle);
        tvRoomType = root.findViewById(R.id.tv_room_type);
        roomSummaryLayout = root.findViewById(R.id.layout_room_summary);
        tvRoomDescription = root.findViewById(R.id.tv_room_description);
        tvFacilities = root.findViewById(R.id.tv_facilities);
        btnGoToMap = root.findViewById(R.id.btn_go_to_map);
        btnDirections = root.findViewById(R.id.btn_get_directions);
        btnBookmark = root.findViewById(R.id.btn_room_bookmark);
        RecyclerView galleryRecycler = root.findViewById(R.id.recycler_room_gallery);
        galleryFadeLeft = root.findViewById(R.id.gallery_fade_left);
        galleryFadeRight = root.findViewById(R.id.gallery_fade_right);
        singleImageCard = root.findViewById(R.id.single_image_card);
        singleImageView = root.findViewById(R.id.iv_room_image);
        singleImagePlaceholder = root.findViewById(R.id.tv_image_placeholder);

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
        configureActionButtons(false);

        roomDetailSheetBehavior = BottomSheetBehavior.from(roomDetailSheet);
        roomDetailSheetBehavior.setFitToContents(false);
        roomDetailSheetBehavior.setExpandedOffset(dpToPx(14));
        roomDetailSheetBehavior.setHalfExpandedRatio(0.5f);
        roomDetailSheetBehavior.setPeekHeight(dpToPx(132), true);
        roomDetailSheetBehavior.setSkipCollapsed(false);
        roomDetailSheetBehavior.setHideable(true);
        roomDetailSheetBehavior.setDraggable(true);
        roomDetailSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        roomDetailSheet.post(this::updateRoomDetailPeekHeight);
        roomDetailSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheet.setVisibility(View.GONE);
                    setBottomNavVisible(true);
                    setDirectionsFabVisible(true);
                    Runnable afterClose = pendingAfterRoomSheetHidden;
                    pendingAfterRoomSheetHidden = null;
                    if (afterClose != null && isAdded()) {
                        bottomSheet.post(afterClose);
                    }
                    return;
                }

                if (bottomSheet.getVisibility() != View.VISIBLE) {
                    bottomSheet.setVisibility(View.VISIBLE);
                }
                setBottomNavVisible(false);
                setDirectionsFabVisible(false);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // No-op.
            }
        });

        btnBookmark.setOnClickListener(v ->
                roomDetailViewModel.toggleSaved(new SavedRoomRepository.ToggleCallback() {
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

        btnDirections.setOnClickListener(v -> {
            if (activeRoomId <= 0) {
                Toast.makeText(requireContext(), "Invalid room", Toast.LENGTH_SHORT).show();
                return;
            }

            DirectionsSheetFragment sheet = new DirectionsSheetFragment();
            Bundle sheetArgs = new Bundle();
            sheetArgs.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_ID, activeRoomId);
            sheetArgs.putString(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_NAME, tvRoomName.getText().toString());
            sheet.setArguments(sheetArgs);
            hideRoomDetailSheet();
            sheet.show(getParentFragmentManager(), "directions_sheet");
        });

        roomDetailViewModel.getRoom().observe(getViewLifecycleOwner(), room -> {
            if (room == null) {
                return;
            }

            activeRoom = room;
            tvRoomName.setText(room.name);
            tvRoomSubtitle.setText(buildSubtitle(room.code, room.location));

            if (room.type != null && !room.type.isBlank()) {
                tvRoomType.setVisibility(View.VISIBLE);
                tvRoomType.setText(room.type.toUpperCase(java.util.Locale.getDefault()));
            } else {
                tvRoomType.setVisibility(View.GONE);
            }

            String description = room.description != null ? room.description : "";
            if (description.isBlank()) {
                description = "No description available.";
            }
            tvRoomDescription.setText(description);

            String resolvedImageUrl = RoomImageUrlResolver.resolvePath(requireContext(), room.imageUrl);
            loadRoomImages(room.cachedImagePath, resolvedImageUrl, galleryRecycler);
        });

        roomDetailViewModel.getFacilities().observe(getViewLifecycleOwner(), facilities -> {
            if (facilities == null || facilities.isEmpty()) {
                tvFacilities.setText("No facilities listed");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (FacilityEntity facility : facilities) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(facility.name);
            }
            tvFacilities.setText(sb.toString());
        });

        roomDetailViewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || !isAdded()) {
                return;
            }

            if (activeRoomIsCampusArea) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                return;
            }

            View rootView = getView();
            if (rootView != null) {
                Snackbar.make(rootView, err, Snackbar.LENGTH_LONG).show();
            }
        });

        roomDetailViewModel.getIsSaved().observe(getViewLifecycleOwner(),
                saved -> updateBookmarkState(Boolean.TRUE.equals(saved)));
    }

    private void configureActionButtons(boolean shouldShowGoTo) {
        if (btnGoToMap == null || btnDirections == null) {
            return;
        }

        LinearLayout.LayoutParams goToParams = (LinearLayout.LayoutParams) btnGoToMap.getLayoutParams();
        LinearLayout.LayoutParams directionsParams = (LinearLayout.LayoutParams) btnDirections.getLayoutParams();

        if (shouldShowGoTo) {
            btnGoToMap.setVisibility(View.VISIBLE);
            goToParams.width = 0;
            goToParams.weight = 1f;
            goToParams.setMarginEnd(dpToPx(6));
            btnGoToMap.setLayoutParams(goToParams);

            directionsParams.width = 0;
            directionsParams.weight = 1f;
            directionsParams.setMarginStart(dpToPx(6));
            btnDirections.setLayoutParams(directionsParams);
            btnDirections.setText(R.string.room_detail_directions);
            return;
        }

        btnGoToMap.setVisibility(View.GONE);
        goToParams.width = 0;
        goToParams.weight = 0f;
        goToParams.setMarginEnd(0);
        btnGoToMap.setLayoutParams(goToParams);

        directionsParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        directionsParams.weight = 0f;
        directionsParams.setMarginStart(0);
        btnDirections.setLayoutParams(directionsParams);
        btnDirections.setText("DIRECTIONS");
    }

    private void showRoomDetailSheet(int roomId, @Nullable String roomName, boolean campusArea) {
        if (roomDetailSheet == null || roomDetailSheetBehavior == null || roomId <= 0) {
            return;
        }

        activeRoomId = roomId;
        activeRoomIsCampusArea = campusArea;
        activeRoom = null;
        latestImageUrl = null;
        roomDetailSheet.setVisibility(View.VISIBLE);
        setBottomNavVisible(false);
        setDirectionsFabVisible(false);
        roomDetailSheet.post(this::updateRoomDetailPeekHeight);
        roomDetailSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        roomDetailViewModel.loadRoom(roomId);
    }

    private void hideRoomDetailSheet() {
        if (roomDetailSheetBehavior == null) {
            return;
        }
        roomDetailSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void setBottomNavVisible(boolean visible) {
        if (!isAdded()) {
            return;
        }
        View navBar = requireActivity().findViewById(R.id.nav_bar_container);
        View navHost = requireActivity().findViewById(R.id.nav_host_fragment);
        if (navBar != null) {
            navBar.setVisibility(visible ? View.VISIBLE : View.GONE);
            navBar.post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (navHost != null) {
                    int bottomPadding = (visible && navBar.getVisibility() == View.VISIBLE)
                            ? navBar.getHeight()
                            : 0;
                    navHost.setPadding(
                            navHost.getPaddingLeft(),
                            navHost.getPaddingTop(),
                            navHost.getPaddingRight(),
                            bottomPadding
                    );
                }
            });
        }
    }

    private void setupFab() {
        if (fabCompass == null) {
            return;
        }

        fabCompass.setOnClickListener(v ->
                new DirectionsSheetFragment().show(getParentFragmentManager(), "directions_sheet"));

        fabCompass.setOnLongClickListener(v -> {
            setFloorSelection(null);
            return true;
        });

        setDirectionsFabVisible(true);
    }

    private void setDirectionsFabVisible(boolean visible) {
        if (fabCompass == null) {
            return;
        }
        fabCompass.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateBookmarkState(boolean isSaved) {
        if (btnBookmark == null || !isAdded()) {
            return;
        }
        btnBookmark.setImageDrawable(AppCompatResources.getDrawable(
                requireContext(),
                isSaved ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
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
        final int roomId = activeRoomId;

        ioExecutor.execute(() -> {
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
            if (finalBitmap != null && roomId > 0) {
                String cachedPath = RoomImageCacheManager.cacheRoomBitmap(appContext, roomId, finalBitmap);
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

            runOnUiThreadSafely(() -> {
                if (latestImageUrl == null || !latestImageUrl.equals(imageUrl) || roomId != activeRoomId) {
                    return;
                }
                showSingleImage(finalBitmap);
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

    private void updateRoomDetailPeekHeight() {
        if (roomDetailSheetBehavior == null || roomDetailSheet == null || roomSummaryLayout == null) {
            return;
        }

        int fallbackPeekHeight = dpToPx(132);
        int summaryBottom = roomSummaryLayout.getBottom();
        if (summaryBottom <= 0) {
            roomDetailSheetBehavior.setPeekHeight(fallbackPeekHeight, true);
            return;
        }

        int desiredPeekHeight = summaryBottom + dpToPx(18);
        roomDetailSheetBehavior.setPeekHeight(Math.max(desiredPeekHeight, fallbackPeekHeight), true);
    }

    private void centerRoomInFloorMap(@NonNull View roomBox) {
        View parent = (View) roomBox.getParent();
        while (parent != null && !(parent instanceof ScrollView) && parent.getParent() instanceof View) {
            parent = (View) parent.getParent();
        }

        if (parent instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) parent;
            scrollView.post(() -> {
                int targetY = roomBox.getTop() - (scrollView.getHeight() / 2) + (roomBox.getHeight() / 2);
                scrollView.smoothScrollTo(0, Math.max(targetY, 0));
            });
        }
    }

    private void clearRoomHighlight() {
        if (floorMapContainer == null) {
            highlightedRoomId = null;
            return;
        }

        for (int roomBoxId : getActiveRoomBoxIds()) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);
            if (roomBox != null) {
                applyRoomHighlightState(roomBox, false);
            }
        }
        highlightedRoomId = null;
    }

    private boolean isFloorOneSelected() {
        return selectedFloor != null && selectedFloor == 1;
    }

    private boolean isFloorFiveSelected() {
        return selectedFloor != null && selectedFloor == 5;
    }

    private int[] getActiveRoomBoxIds() {
        if (isFloorOneSelected()) {
            return FLOOR1_ROOM_BOX_IDS;
        }
        if (isFloorFiveSelected()) {
            return FLOOR5_ROOM_BOX_IDS;
        }
        return DEFAULT_ROOM_BOX_IDS;
    }

    private void applyRoomHighlightState(@NonNull View roomBox, boolean highlighted) {
        View pin = roomBox.findViewById(R.id.room_pin);
        roomBox.setTranslationZ(highlighted ? 10f : 0f);

        TextView label = roomBox.findViewById(R.id.room_label);
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(requireContext(),
                    highlighted ? R.color.green_primary : R.color.floor_room_label));
            label.setTypeface(Typeface.DEFAULT, highlighted ? Typeface.BOLD : Typeface.NORMAL);
        }

        if (pin != null) {
            pin.setVisibility(highlighted ? View.VISIBLE : View.GONE);
        }
    }

    private void animateRoomPin(@NonNull View roomBox) {
        View pin = roomBox.findViewById(R.id.room_pin);
        if (pin == null) {
            return;
        }
        pin.setScaleX(0.65f);
        pin.setScaleY(0.65f);
        pin.setAlpha(0f);
        pin.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .start();
    }

    private void runOnUiThreadSafely(Runnable task) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                task.run();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setBottomNavVisible(true);
        setDirectionsFabVisible(true);
        // Shutdown executor to prevent thread leaks
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
    }
}
