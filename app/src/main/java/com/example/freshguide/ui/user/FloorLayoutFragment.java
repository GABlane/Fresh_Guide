package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.RoomEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloorLayoutFragment extends Fragment {

    private static final String ARG_BUILDING_CODE = "buildingCode";
    private static final String ARG_SELECTED_FLOOR = "selectedFloor";

    private static final String CODE_MAIN = "MAIN";
    private static final String CODE_COURT = "COURT";
    private static final String CODE_REG = "REG";
    private static final String CODE_LIB = "LIB";
    private static final String CODE_ENT = "ENT";
    private static final String CODE_EXIT = "EXIT";

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

    private static final int[] FLOOR2_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_left_4,
            R.id.room_left_5,
            R.id.room_left_6,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_right_4,
            R.id.room_right_5,
            R.id.room_right_6
    };

    private static final int[] FLOOR3_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_left_4,
            R.id.room_left_5,
            R.id.room_left_6,
            R.id.room_left_7,
            R.id.room_left_8,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_right_4,
            R.id.room_right_5,
            R.id.room_right_6,
            R.id.room_right_7
    };

    private static final int[] FLOOR4_ROOM_BOX_IDS = {
            R.id.room_left_1,
            R.id.room_left_2,
            R.id.room_left_3,
            R.id.room_left_4,
            R.id.room_left_5,
            R.id.room_left_6,
            R.id.room_right_1,
            R.id.room_right_2,
            R.id.room_right_3,
            R.id.room_right_4,
            R.id.room_right_5,
            R.id.room_right_6,
            R.id.room_right_7
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
    private static final Map<String, Integer> FLOOR2_ROOM_SLOTS = createFloor2RoomSlots();
    private static final Map<String, Integer> FLOOR3_ROOM_SLOTS = createFloor3RoomSlots();
    private static final Map<String, Integer> FLOOR4_ROOM_SLOTS = createFloor4RoomSlots();

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private String buildingCode = CODE_MAIN;
    private int selectedFloor = 1;
    private int floorLayoutResId;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            buildingCode = args.getString(ARG_BUILDING_CODE, CODE_MAIN);
            selectedFloor = args.getInt(ARG_SELECTED_FLOOR, 1);
        }

        floorLayoutResId = resolveFloorLayoutRes(buildingCode, selectedFloor);
        if (floorLayoutResId == 0) {
            return new FrameLayout(inflater.getContext());
        }
        return inflater.inflate(floorLayoutResId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        if (floorLayoutResId == 0) {
            Toast.makeText(requireContext(),
                    "Floor layout not available for this building/floor.",
                    Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        bindRoomsToStaticLayout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private void bindRoomsToStaticLayout() {
        clearRoomBoxes();

        ioExecutor.execute(() -> {
            List<RoomEntity> rooms = AppDatabase.getInstance(requireContext().getApplicationContext())
                    .roomDao()
                    .getRoomsByBuildingAndFloorSync(buildingCode, selectedFloor);

            List<RoomEntity> floorRooms = new ArrayList<>();
            if (rooms != null) {
                for (RoomEntity room : rooms) {
                    if (room == null || isCampusAreaCode(room.code)) {
                        continue;
                    }
                    floorRooms.add(room);
                }
            }
            floorRooms.sort(Comparator.comparingInt(r -> r.id));

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> applyRoomsToStaticLayout(floorRooms));
        });
    }

    private void applyRoomsToStaticLayout(@NonNull List<RoomEntity> floorRooms) {
        if (!isAdded() || rootView == null) {
            return;
        }

        if (selectedFloor == 1) {
            applyFloorOneRoomsByCode(floorRooms);
            return;
        }

        if (selectedFloor == 2) {
            applyFloorTwoRoomsByCode(floorRooms);
            return;
        }

        if (selectedFloor == 3) {
            applyFloorThreeRoomsByCode(floorRooms);
            return;
        }

        if (selectedFloor == 4) {
            applyFloorFourRoomsByCode(floorRooms);
            return;
        }

        if (selectedFloor == 5) {
            applyFloorFiveStaticLabels();
            return;
        }

        int max = Math.min(floorRooms.size(), DEFAULT_ROOM_BOX_IDS.length);
        for (int i = 0; i < max; i++) {
            setRoomLabel(DEFAULT_ROOM_BOX_IDS[i], getRoomDisplayName(floorRooms.get(i)));
        }
    }

    private void applyFloorOneRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            String code = normalizeCode(room.code);
            if (code == null || !FLOOR1_ROOM_SLOTS.containsKey(code)) {
                continue;
            }
            roomByCode.putIfAbsent(code, room);
        }

        for (Map.Entry<String, Integer> slot : FLOOR1_ROOM_SLOTS.entrySet()) {
            RoomEntity room = roomByCode.get(slot.getKey());
            if (room != null) {
                setRoomLabel(slot.getValue(), getRoomDisplayName(room));
            }
        }
    }

    private void applyFloorTwoRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            String code = normalizeCode(room.code);
            if (code == null || !FLOOR2_ROOM_SLOTS.containsKey(code)) {
                continue;
            }
            roomByCode.putIfAbsent(code, room);
        }

        for (Map.Entry<String, Integer> slot : FLOOR2_ROOM_SLOTS.entrySet()) {
            RoomEntity room = roomByCode.get(slot.getKey());
            if (room != null) {
                setRoomLabel(slot.getValue(), getRoomDisplayName(room));
            }
        }
    }

    private void applyFloorThreeRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            String code = normalizeCode(room.code);
            if (code != null && FLOOR3_ROOM_SLOTS.containsKey(code)) {
                roomByCode.putIfAbsent(code, room);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR3_ROOM_SLOTS.entrySet()) {
            RoomEntity room = roomByCode.get(slot.getKey());
            if (room != null) {
                setRoomLabel(slot.getValue(), getRoomDisplayName(room));
            }
        }
    }

    private void applyFloorFourRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            String code = normalizeCode(room.code);
            if (code != null && FLOOR4_ROOM_SLOTS.containsKey(code)) {
                roomByCode.putIfAbsent(code, room);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR4_ROOM_SLOTS.entrySet()) {
            RoomEntity room = roomByCode.get(slot.getKey());
            if (room != null) {
                setRoomLabel(slot.getValue(), getRoomDisplayName(room));
            }
        }
    }

    private void applyFloorFiveStaticLabels() {
        setRoomLabel(R.id.room_left_1, "V-48");
        setRoomLabel(R.id.room_left_2, "V-46");
        setRoomLabel(R.id.room_left_3, "Industrial\nArts\nLaboratory");
        setRoomLabel(R.id.room_right_1, "Speech\nLaboratory");
        setRoomLabel(R.id.room_right_2, "Craft /\nSewing\nLaboratory");
        setRoomLabel(R.id.room_right_3, "Office");
        setRoomLabel(R.id.room_auditorium, "Auditorium");
    }

    private void clearRoomBoxes() {
        for (int roomBoxId : getActiveRoomBoxIds(selectedFloor)) {
            View roomBox = rootView != null ? rootView.findViewById(roomBoxId) : null;
            if (roomBox == null) {
                continue;
            }
            TextView roomLabel = roomBox.findViewById(R.id.room_label);
            if (roomLabel != null) {
                roomLabel.setText("Room");
            }
            disableRoomBoxInteraction(roomBox);
        }
    }

    private void setRoomLabel(int roomViewId, @NonNull String label) {
        if (rootView == null) {
            return;
        }
        View roomBox = rootView.findViewById(roomViewId);
        if (roomBox == null) {
            return;
        }
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(label);
        }
        disableRoomBoxInteraction(roomBox);
    }

    private void disableRoomBoxInteraction(@NonNull View roomBox) {
        roomBox.setOnClickListener(null);
        roomBox.setClickable(false);
        roomBox.setFocusable(false);
        roomBox.setOnTouchListener(null);
    }

    private int resolveFloorLayoutRes(@Nullable String buildingCode, int floorNumber) {
        if (buildingCode == null || !CODE_MAIN.equalsIgnoreCase(buildingCode.trim())) {
            return 0;
        }
        switch (floorNumber) {
            case 1:
                return R.layout.map_floor_1;
            case 2:
                return R.layout.map_floor_2;
            case 3:
                return R.layout.map_floor_3;
            case 4:
                return R.layout.map_floor_4;
            case 5:
                return R.layout.map_floor_5;
            default:
                return 0;
        }
    }

    private int[] getActiveRoomBoxIds(int floorNumber) {
        if (floorNumber == 1) {
            return FLOOR1_ROOM_BOX_IDS;
        }
        if (floorNumber == 2) {
            return FLOOR2_ROOM_BOX_IDS;
        }
        if (floorNumber == 3) {
            return FLOOR3_ROOM_BOX_IDS;
        }
        if (floorNumber == 4) {
            return FLOOR4_ROOM_BOX_IDS;
        }
        if (floorNumber == 5) {
            return FLOOR5_ROOM_BOX_IDS;
        }
        return DEFAULT_ROOM_BOX_IDS;
    }

    private boolean isCampusAreaCode(@Nullable String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase();
        return CODE_COURT.equals(normalized)
                || CODE_REG.equals(normalized)
                || CODE_LIB.equals(normalized)
                || CODE_ENT.equals(normalized)
                || CODE_EXIT.equals(normalized);
    }

    @Nullable
    private String normalizeCode(@Nullable String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String normalized = rawCode.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    @NonNull
    private String getRoomDisplayName(@NonNull RoomEntity room) {
        if (room.name != null && !room.name.trim().isEmpty()) {
            return room.name;
        }
        if (room.code != null && !room.code.trim().isEmpty()) {
            return room.code;
        }
        return "Room";
    }

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

    private static Map<String, Integer> createFloor2RoomSlots() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        // Left column
        map.put("MAIN-2-LR211", R.id.room_left_1);
        map.put("MAIN-2-LR209", R.id.room_left_2);
        map.put("MAIN-2-LR207", R.id.room_left_3);
        map.put("MAIN-2-LR205", R.id.room_left_4);
        map.put("MAIN-2-LR203", R.id.room_left_5);
        map.put("MAIN-2-LR201", R.id.room_left_6);

        // Right column
        map.put("MAIN-2-LR212", R.id.room_right_1);
        map.put("MAIN-2-LR210", R.id.room_right_2);
        map.put("MAIN-2-LR208", R.id.room_right_3);
        map.put("MAIN-2-LR206", R.id.room_right_4);
        map.put("MAIN-2-LR204", R.id.room_right_5);
        map.put("MAIN-2-LR202", R.id.room_right_6);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Integer> createFloor3RoomSlots() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        // Left column
        map.put("MAIN-3-CBA-COORD", R.id.room_left_1);
        map.put("MAIN-3-CBA-DEAN", R.id.room_left_2);
        map.put("MAIN-3-LR310", R.id.room_left_3);
        map.put("MAIN-3-LR308", R.id.room_left_4);
        map.put("MAIN-3-CLAS-COORD", R.id.room_left_5);
        map.put("MAIN-3-SOUND-LAB", R.id.room_left_6);
        map.put("MAIN-3-LR304", R.id.room_left_7);
        map.put("MAIN-3-LR302", R.id.room_left_8);

        // Right column (Student Affairs is static, not in mapping)
        map.put("MAIN-3-MIS-DATA", R.id.room_right_1);
        map.put("MAIN-3-CS-DEPT", R.id.room_right_2);
        map.put("MAIN-3-MULTIMEDIA", R.id.room_right_3);
        map.put("MAIN-3-LABTECH", R.id.room_right_4);
        map.put("MAIN-3-COMPLAB1", R.id.room_right_5);
        map.put("MAIN-3-COMPLAB2", R.id.room_right_6);
        map.put("MAIN-3-COMPLAB3", R.id.room_right_7);

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Integer> createFloor4RoomSlots() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        // Left column
        map.put("MAIN-4-LR411", R.id.room_left_1);
        map.put("MAIN-4-EARLY-CHILD", R.id.room_left_2);
        map.put("MAIN-4-LR408", R.id.room_left_3);
        map.put("MAIN-4-PHYSICS-LAB", R.id.room_left_4);
        map.put("MAIN-4-LR404", R.id.room_left_5);
        map.put("MAIN-4-LR402", R.id.room_left_6);

        // Right column (Student Lounge is static, not in mapping)
        map.put("MAIN-4-COE-COUNCIL", R.id.room_right_1);
        map.put("MAIN-4-EDTECH-LAB", R.id.room_right_2);
        map.put("MAIN-4-BIO-LAB", R.id.room_right_3);
        map.put("MAIN-4-CHEM-LAB", R.id.room_right_4);
        map.put("MAIN-4-LAW-OFFICE", R.id.room_right_5);
        map.put("MAIN-4-LR401", R.id.room_right_6);
        map.put("MAIN-4-COE", R.id.room_right_7);

        return Collections.unmodifiableMap(map);
    }
}
