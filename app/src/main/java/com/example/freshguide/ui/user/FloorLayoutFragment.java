package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class FloorLayoutFragment extends Fragment {

    private static final int DEFAULT_FLOOR = 1;

    private final Map<Integer, SlotView> slots = new HashMap<>();
    private int currentFloor = DEFAULT_FLOOR;
    private String buildingCode = "MAIN";
    private String buildingName = "Main Building";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_floor_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            buildingCode = args.getString("buildingCode", buildingCode);
            buildingName = args.getString("buildingName", buildingName);
        }

        bindSlotViews(view);
        setupFloorChips(view);
        loadFloor(currentFloor, view);
    }

    private void bindSlotViews(View view) {
        slots.put(1, new SlotView(view.findViewById(R.id.room_1), view.findViewById(R.id.tv_room_1)));
        slots.put(2, new SlotView(view.findViewById(R.id.room_2), view.findViewById(R.id.tv_room_2)));
        slots.put(3, new SlotView(view.findViewById(R.id.room_3), view.findViewById(R.id.tv_room_3)));
        slots.put(4, new SlotView(view.findViewById(R.id.room_4), view.findViewById(R.id.tv_room_4)));
        slots.put(5, new SlotView(view.findViewById(R.id.room_5), view.findViewById(R.id.tv_room_5)));
        slots.put(6, new SlotView(view.findViewById(R.id.room_6), view.findViewById(R.id.tv_room_6)));
        slots.put(7, new SlotView(view.findViewById(R.id.room_7), view.findViewById(R.id.tv_room_7)));
        slots.put(8, new SlotView(view.findViewById(R.id.room_8), view.findViewById(R.id.tv_room_8)));
        slots.put(9, new SlotView(view.findViewById(R.id.room_9), view.findViewById(R.id.tv_room_9)));
        slots.put(10, new SlotView(view.findViewById(R.id.room_10), view.findViewById(R.id.tv_room_10)));
    }

    private void setupFloorChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_floor);
        Chip chip1 = view.findViewById(R.id.chip_floor_1);
        Chip chip2 = view.findViewById(R.id.chip_floor_2);
        Chip chip3 = view.findViewById(R.id.chip_floor_3);
        Chip chip4 = view.findViewById(R.id.chip_floor_4);

        styleChip(chip1);
        styleChip(chip2);
        styleChip(chip3);
        styleChip(chip4);

        chip1.setChecked(true);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_floor_1) currentFloor = 1;
            else if (checkedId == R.id.chip_floor_2) currentFloor = 2;
            else if (checkedId == R.id.chip_floor_3) currentFloor = 3;
            else if (checkedId == R.id.chip_floor_4) currentFloor = 4;
            loadFloor(currentFloor, view);
        });
    }

    private void styleChip(Chip chip) {
        if (chip == null) return;
        int green = requireContext().getColor(R.color.green_primary);
        int white = requireContext().getColor(android.R.color.white);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);

        chip.setEnsureMinTouchTargetSize(false);
        float density = getResources().getDisplayMetrics().density;
        chip.setTextSize(11f);
        chip.setChipMinHeight(28f * density);
        chip.setChipStartPadding(12f * density);
        chip.setChipEndPadding(12f * density);

        chip.setChipStrokeColorResource(R.color.green_primary);
        chip.setChipStrokeWidth(1.2f * density);

        android.content.res.ColorStateList bg = new android.content.res.ColorStateList(
                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                new int[]{ green, white }
        );
        chip.setChipBackgroundColor(bg);

        android.content.res.ColorStateList tc = new android.content.res.ColorStateList(
                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                new int[]{ android.graphics.Color.WHITE, green }
        );
        chip.setTextColor(tc);
    }

    private void loadFloor(int floorNumber, View view) {
        TextView hallway = view.findViewById(R.id.tv_hallway);
        hallway.setText(floorNumber + ordinalSuffix(floorNumber) + " Floor Hallway");

        TextView guardDesk = view.findViewById(R.id.box_guard);
        if (guardDesk != null) {
            guardDesk.setVisibility(floorNumber == 1 ? View.VISIBLE : View.GONE);
        }

        for (SlotView slot : slots.values()) {
            slot.clear();
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            BuildingEntity building = db.buildingDao().getByCodeSync(buildingCode);
            if (building == null) return;
            List<FloorEntity> floors = db.floorDao().getByBuildingSync(building.id);
            FloorEntity target = null;
            for (FloorEntity f : floors) {
                if (f.number == floorNumber) {
                    target = f;
                    break;
                }
            }
            if (target == null) return;
            List<RoomEntity> rooms = db.roomDao().getByFloorSync(target.id);

            requireActivity().runOnUiThread(() -> bindRooms(rooms, view));
        });
    }

    private void bindRooms(List<RoomEntity> rooms, View view) {
        if (rooms == null) return;
        NavController nav = Navigation.findNavController(view);

        for (RoomEntity room : rooms) {
            int slotNumber = parseSlot(room.code);
            SlotView slot = slots.get(slotNumber);
            if (slot == null) continue;
            slot.bind(room, v -> {
                Bundle args = new Bundle();
                args.putInt("roomId", room.id);
                args.putString("roomName", room.name);
                nav.navigate(R.id.action_floorLayout_to_roomDetail, args);
            });
        }
    }

    private int parseSlot(String code) {
        if (code == null || !code.contains("-")) return -1;
        String[] parts = code.split("-");
        if (parts.length != 2) return -1;
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String ordinalSuffix(int number) {
        if (number >= 11 && number <= 13) return "th";
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private static class SlotView {
        private final View card;
        private final TextView label;
        private RoomEntity room;

        SlotView(View card, TextView label) {
            this.card = card;
            this.label = label;
        }

        void clear() {
            room = null;
            label.setText("Room");
            card.setAlpha(0.6f);
            card.setOnClickListener(null);
        }

        void bind(RoomEntity room, View.OnClickListener listener) {
            this.room = room;
            label.setText(room.name != null ? room.name : "Room");
            card.setAlpha(1f);
            card.setOnClickListener(listener);
        }
    }
}
