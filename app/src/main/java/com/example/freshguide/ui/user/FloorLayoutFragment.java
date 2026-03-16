//package com.example.freshguide.ui.user;
//
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.util.SparseIntArray;
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
//
//import com.example.freshguide.R;
//import com.example.freshguide.database.AppDatabase;
//import com.example.freshguide.model.entity.BuildingEntity;
//import com.example.freshguide.model.entity.FloorEntity;
//import com.example.freshguide.model.entity.RoomEntity;
//import com.google.android.material.chip.Chip;
//import com.google.android.material.chip.ChipGroup;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//
//public class FloorLayoutFragment extends Fragment {
//
//    private static final int DEFAULT_FLOOR = 1;
//
//    private final Executor ioExecutor = Executors.newSingleThreadExecutor();
//    private final SparseIntArray floorByChipId = new SparseIntArray();
//
//    private int currentFloor = DEFAULT_FLOOR;
//    private String buildingCode = "MAIN";
//    private String buildingName = "Main Building";
//    private LinearLayout roomColumnLeft;
//    private LinearLayout roomColumnRight;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_floor_layout, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        Bundle args = getArguments();
//        if (args != null) {
//            buildingCode = args.getString("buildingCode", buildingCode);
//            buildingName = args.getString("buildingName", buildingName);
//            int selectedFloor = args.getInt("selectedFloor", DEFAULT_FLOOR);
//            currentFloor = selectedFloor;
//        }
//
//        roomColumnLeft = view.findViewById(R.id.room_column_left);
//        roomColumnRight = view.findViewById(R.id.room_column_right);
//
//        setupFloorChips(view);
//    }
//
//    private void setupFloorChips(View view) {
//        ChipGroup chipGroup = view.findViewById(R.id.chip_group_floor);
//        ioExecutor.execute(() -> {
//            List<Integer> floorNumbers = getAvailableFloorNumbers();
//            runOnUiThreadSafely(() -> {
//                if (!isAdded()) return;
//
//                chipGroup.setOnCheckedChangeListener(null);
//                chipGroup.removeAllViews();
//                floorByChipId.clear();
//
//                for (int floorNumber : floorNumbers) {
//                    Chip chip = makeFloorChip(floorLabel(floorNumber));
//                    int chipId = View.generateViewId();
//                    chip.setId(chipId);
//                    chipGroup.addView(chip);
//                    floorByChipId.put(chipId, floorNumber);
//                }
//
//                if (!floorNumbers.contains(currentFloor)) {
//                    currentFloor = floorNumbers.get(0);
//                }
//
//                int selectedChipId = findChipIdForFloor(currentFloor);
//                if (selectedChipId != View.NO_ID) {
//                    chipGroup.check(selectedChipId);
//                }
//
//                chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
//                    if (checkedId == View.NO_ID) return;
//                    int selectedFloor = floorByChipId.get(checkedId, -1);
//                    if (selectedFloor < 1 || selectedFloor == currentFloor) return;
//                    currentFloor = selectedFloor;
//                    loadFloor(currentFloor, view);
//                });
//
//                loadFloor(currentFloor, view);
//            });
//        });
//    }
//
//    private List<Integer> getAvailableFloorNumbers() {
//        AppDatabase db = AppDatabase.getInstance(requireContext());
//        BuildingEntity building = db.buildingDao().getByCodeSync(buildingCode);
//        if (building == null) {
//            return Collections.singletonList(DEFAULT_FLOOR);
//        }
//
//        List<FloorEntity> floors = db.floorDao().getByBuildingSync(building.id);
//        List<Integer> floorNumbers = new ArrayList<>();
//        for (FloorEntity floor : floors) {
//            if (floor.number > 0 && !floorNumbers.contains(floor.number)) {
//                floorNumbers.add(floor.number);
//            }
//        }
//
//        if (floorNumbers.isEmpty()) {
//            floorNumbers.add(DEFAULT_FLOOR);
//        }
//
//        Collections.sort(floorNumbers);
//        return floorNumbers;
//    }
//
//    private int findChipIdForFloor(int floor) {
//        for (int i = 0; i < floorByChipId.size(); i++) {
//            int chipId = floorByChipId.keyAt(i);
//            if (floorByChipId.get(chipId) == floor) {
//                return chipId;
//            }
//        }
//        return View.NO_ID;
//    }
//
//    private Chip makeFloorChip(String label) {
//        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
//        chip.setText(label);
//        styleChip(chip);
//        return chip;
//    }
//
//    private void styleChip(Chip chip) {
//        if (chip == null) return;
//        int green = requireContext().getColor(R.color.green_primary);
//        chip.setCheckable(true);
//        chip.setCheckedIconVisible(false);
//
//        chip.setEnsureMinTouchTargetSize(false);
//        float density = getResources().getDisplayMetrics().density;
//        chip.setTextSize(11f);
//        chip.setChipMinHeight(28f * density);
//        chip.setChipStartPadding(12f * density);
//        chip.setChipEndPadding(12f * density);
//
//        chip.setChipStrokeColorResource(R.color.green_primary);
//        chip.setChipStrokeWidth(1.2f * density);
//
//        ColorStateList bg = new ColorStateList(
//                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
//                new int[]{ green, Color.WHITE }
//        );
//        chip.setChipBackgroundColor(bg);
//
//        ColorStateList tc = new ColorStateList(
//                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
//                new int[]{ Color.WHITE, green }
//        );
//        chip.setTextColor(tc);
//    }
//
//    private void loadFloor(int floorNumber, View view) {
//        TextView hallway = view.findViewById(R.id.tv_hallway);
//        hallway.setText(floorNumber + ordinalSuffix(floorNumber) + " Floor Hallway");
//
//        TextView guardDesk = view.findViewById(R.id.box_guard);
//        if (guardDesk != null) {
//            guardDesk.setVisibility(floorNumber == 1 ? View.VISIBLE : View.GONE);
//        }
//
//        clearRoomColumns();
//
//        ioExecutor.execute(() -> {
//            AppDatabase db = AppDatabase.getInstance(requireContext());
//            BuildingEntity building = db.buildingDao().getByCodeSync(buildingCode);
//            if (building == null) {
//                runOnUiThreadSafely(() -> bindRooms(Collections.emptyList(), view));
//                return;
//            }
//
//            List<FloorEntity> floors = db.floorDao().getByBuildingSync(building.id);
//            FloorEntity target = null;
//            for (FloorEntity f : floors) {
//                if (f.number == floorNumber) {
//                    target = f;
//                    break;
//                }
//            }
//            if (target == null) {
//                runOnUiThreadSafely(() -> bindRooms(Collections.emptyList(), view));
//                return;
//            }
//
//            List<RoomEntity> rooms = db.roomDao().getByFloorSync(target.id);
//            runOnUiThreadSafely(() -> bindRooms(rooms, view));
//        });
//    }
//
//    private void bindRooms(List<RoomEntity> rooms, View view) {
//        clearRoomColumns();
//
//        if (rooms == null || rooms.isEmpty()) {
//            addEmptyRoomCard(roomColumnLeft);
//            return;
//        }
//
//        NavController nav = Navigation.findNavController(view);
//        int leftCount = 0;
//        int rightCount = 0;
//
//        for (int i = 0; i < rooms.size(); i++) {
//            RoomEntity room = rooms.get(i);
//            boolean placeOnLeft = i % 2 == 0;
//            if (placeOnLeft) {
//                addRoomCard(roomColumnLeft, room, false, leftCount++, nav);
//            } else {
//                addRoomCard(roomColumnRight, room, true, rightCount++, nav);
//            }
//        }
//    }
//
//    private void addRoomCard(
//            LinearLayout container,
//            RoomEntity room,
//            boolean markerAtStart,
//            int positionInColumn,
//            NavController nav
//    ) {
//        if (container == null) return;
//
//        FrameLayout card = (FrameLayout) LayoutInflater.from(requireContext())
//                .inflate(R.layout.item_floor_room_card, container, false);
//
//        TextView roomLabel = card.findViewById(R.id.tv_room_dynamic);
//        View marker = card.findViewById(R.id.view_room_marker);
//
//        roomLabel.setText(getRoomDisplayName(room));
//        roomLabel.setTextColor(requireContext().getColor(R.color.text_hint));
//
//        FrameLayout.LayoutParams markerParams = (FrameLayout.LayoutParams) marker.getLayoutParams();
//        if (markerAtStart) {
//            markerParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
//            markerParams.setMarginStart(dpToPx(4));
//            markerParams.setMarginEnd(0);
//        } else {
//            markerParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
//            markerParams.setMarginStart(0);
//            markerParams.setMarginEnd(dpToPx(4));
//        }
//        marker.setLayoutParams(markerParams);
//
//        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
//        cardParams.topMargin = dpToPx(positionInColumn == 0 ? 6 : 8);
//        card.setLayoutParams(cardParams);
//
//        card.setOnClickListener(v -> {
//            Bundle args = new Bundle();
//            args.putInt("roomId", room.id);
//            args.putString("roomName", room.name != null ? room.name : "Room");
//            nav.navigate(R.id.action_floorLayout_to_roomDetail, args);
//        });
//
//        container.addView(card);
//    }
//
//    private void addEmptyRoomCard(LinearLayout container) {
//        if (container == null) return;
//
//        FrameLayout card = (FrameLayout) LayoutInflater.from(requireContext())
//                .inflate(R.layout.item_floor_room_card, container, false);
//        TextView roomLabel = card.findViewById(R.id.tv_room_dynamic);
//        roomLabel.setText("No rooms yet");
//        card.setAlpha(0.6f);
//        card.setOnClickListener(null);
//
//        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
//        cardParams.topMargin = dpToPx(6);
//        card.setLayoutParams(cardParams);
//
//        container.addView(card);
//    }
//
//    private String getRoomDisplayName(RoomEntity room) {
//        if (room == null) return "Room";
//        if (room.name != null && !room.name.trim().isEmpty()) return room.name;
//        if (room.code != null && !room.code.trim().isEmpty()) return room.code;
//        return "Room";
//    }
//
//    private void clearRoomColumns() {
//        if (roomColumnLeft != null) {
//            roomColumnLeft.removeAllViews();
//        }
//        if (roomColumnRight != null) {
//            roomColumnRight.removeAllViews();
//        }
//    }
//
//    private int dpToPx(int dp) {
//        return Math.round(dp * getResources().getDisplayMetrics().density);
//    }
//
//    private void runOnUiThreadSafely(Runnable task) {
//        if (!isAdded()) return;
//        requireActivity().runOnUiThread(() -> {
//            if (isAdded()) {
//                task.run();
//            }
//        });
//    }
//
//    private String floorLabel(int number) {
//        return number + ordinalSuffix(number) + " Floor";
//    }
//
//    private String ordinalSuffix(int number) {
//        if (number >= 11 && number <= 13) return "th";
//        switch (number % 10) {
//            case 1: return "st";
//            case 2: return "nd";
//            case 3: return "rd";
//            default: return "th";
//        }
//    }
//}
