package com.example.freshguide.ui.user;

import android.content.res.ColorStateList;
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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executor;
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
    private static final int[] ROOM_BOX_IDS = {
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

    private final Executor ioExecutor = Executors.newSingleThreadExecutor();

    private Integer selectedFloor = null;

    private HomeViewModel viewModel;

    private Chip[] floorChips;
    private HorizontalScrollView floorChipContainer;
    private FrameLayout floorMapContainer;
    private View leftFade;
    private View rightFade;
    private View overallMapContainer;
    private Integer pendingFocusedRoomId;
    private Integer highlightedRoomId;
    private String pendingFocusedRoomName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        floorChipContainer = view.findViewById(R.id.floor_chip_container);
        floorMapContainer = view.findViewById(R.id.floor_map_container);
        leftFade = view.findViewById(R.id.leftFade);
        rightFade = view.findViewById(R.id.rightFade);
        overallMapContainer = view.findViewById(R.id.overall_map_container);

        NavController nav = Navigation.findNavController(view);

        applyBuildingPinOverlays(view);
        setupSearch(view, nav);
        setupFloorChips(view);
        setupOverallMapClicks(view, nav);
        setupChipFade();
        setupFab(view);
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
                        nav.navigate(R.id.action_home_to_roomList, null, options);
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

        showOverallMap();
        updateFade();
    }

    private void handleFloorChipClick(Chip clickedChip, int floor, Chip[] allChips) {
        if (selectedFloor != null && selectedFloor == floor) {
            clickedChip.setChecked(false);
            selectedFloor = null;
            showOverallMap();
            return;
        }

        for (Chip chip : allChips) {
            chip.setChecked(false);
        }

        clickedChip.setChecked(true);
        selectedFloor = floor;
        showFloorMap(floor);
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
        selectedFloor = 1;
        if (floorChips != null) {
            for (int i = 0; i < floorChips.length; i++) {
                floorChips[i].setChecked(i == 0);
            }
        }
        showFloorMap(1);
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
                BuildingEntity building = db.buildingDao().getByCodeSync(CODE_MAIN);

                if (building == null) {
                    Log.w(TAG, "Building with code MAIN not found");
                    runOnUiThreadSafely(() -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Main building not found in database", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                List<FloorEntity> floors = db.floorDao().getByBuildingSync(building.id);
                FloorEntity targetFloor = null;

                for (FloorEntity floor : floors) {
                    if (floor.number == floorNumber) {
                        targetFloor = floor;
                        break;
                    }
                }

                if (targetFloor == null) {
                    Log.w(TAG, "No floor entity found for floorNumber=" + floorNumber);
                    runOnUiThreadSafely(this::clearFloorRoomViews);
                    return;
                }

                List<RoomEntity> rooms = db.roomDao().getByFloorSync(targetFloor.id);
                if (rooms != null) {
                    rooms.sort(Comparator.comparingInt(r -> r.id));
                }
                Log.d(TAG, "Binding floor=" + floorNumber + " with rooms=" + (rooms == null ? 0 : rooms.size()));

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

        if (rooms == null || rooms.isEmpty()) {
            Log.w(TAG, "No rooms to bind for current floor layout");
            return;
        }

        List<RoomEntity> floorRooms = new ArrayList<>();
        for (RoomEntity room : rooms) {
            if (room == null) continue;
            if (isCampusAreaCode(room.code)) continue;
            floorRooms.add(room);
        }

        if (floorRooms.isEmpty()) {
            Log.w(TAG, "Only campus-area rooms found for current floor; nothing to bind");
            return;
        }

        View root = getView();
        if (root == null) return;

        final NavController navController = Navigation.findNavController(root);

        int max = Math.min(floorRooms.size(), ROOM_BOX_IDS.length);
        for (int i = 0; i < max; i++) {
            RoomEntity room = floorRooms.get(i);

            View roomBox = floorMapContainer.findViewById(ROOM_BOX_IDS[i]);

            if (roomBox != null) {
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

                if (pendingFocusedRoomId != null && pendingFocusedRoomId == room.id) {
                    openRoomOnMap(room, roomBox, true);
                }
            } else {
                Log.w(TAG, "Room view not found for index=" + i);
            }
        }
    }

    private void clearFloorRoomViews() {
        if (floorMapContainer == null) return;

        for (int roomBoxId : ROOM_BOX_IDS) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);

            if (roomBox != null) {
                TextView roomLabel = roomBox.findViewById(R.id.room_label);
                if (roomLabel != null) {
                    roomLabel.setText("Room");
                }
                roomBox.setOnClickListener(null);
                roomBox.setClickable(false);
                roomBox.setFocusable(false);
                roomBox.setOnTouchListener(null);
                roomBox.setScaleX(1f);
                roomBox.setScaleY(1f);
                roomBox.setTranslationZ(0f);
                applyRoomHighlightState(roomBox, false);
            }
        }
        highlightedRoomId = null;
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

            Bundle args = new Bundle();
            args.putInt("roomId", roomId);
            args.putString("roomName", areaName != null ? areaName : areaCode);
            args.putBoolean("isCampusArea", true);
            nav.navigate(R.id.action_home_to_roomDetail, args);
        });
    }

    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_compass);

        fab.setOnClickListener(v ->
                new DirectionsSheetFragment().show(getParentFragmentManager(), "directions_sheet"));

        fab.setOnLongClickListener(v -> {
            if (selectedFloor != null) {
                selectedFloor = null;
            }
            if (floorChips != null) {
                for (Chip chip : floorChips) {
                    chip.setChecked(false);
                }
            }
            showOverallMap();
            return true;
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
        navController.getCurrentBackStackEntry()
                .getSavedStateHandle()
                .<Bundle>getLiveData(RoomListFragment.KEY_MAP_FOCUS_REQUEST)
                .observe(getViewLifecycleOwner(), request -> {
                    if (request == null || !isAdded()) {
                        return;
                    }

                    navController.getCurrentBackStackEntry()
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
                    selectedFloor = floorNumber;
                    updateSelectedFloorChip(floorNumber);
                    showFloorMap(floorNumber);
                });
    }

    private void updateSelectedFloorChip(int floorNumber) {
        if (floorChips == null) {
            return;
        }
        for (int i = 0; i < floorChips.length; i++) {
            floorChips[i].setChecked((i + 1) == floorNumber);
        }
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
        Bundle args = new Bundle();
        args.putInt("roomId", roomId);
        args.putString("roomName", roomName);
        try {
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_roomDetail, args);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open room detail", e);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Unable to open room details", Toast.LENGTH_SHORT).show();
            }
        }
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

        for (int roomBoxId : ROOM_BOX_IDS) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);
            if (roomBox != null) {
                applyRoomHighlightState(roomBox, false);
            }
        }
        highlightedRoomId = null;
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
}
