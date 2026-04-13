package com.example.freshguide.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DefaultRouteGenerator {

    private static final String CODE_MAIN = "MAIN";
    private static final String CODE_REG = "REG";
    private static final String CODE_LIB = "LIB";
    private static final String CODE_COURT = "COURT";
    private static final String CODE_ENT = "ENT";
    private static final String CODE_EXIT = "EXIT";

    private static final Map<Integer, Map<String, SlotInfo>> MAIN_BUILDING_SLOTS = createMainBuildingSlots();

    private DefaultRouteGenerator() {
    }

    @NonNull
    public static RouteDto createFromOrigin(@Nullable OriginEntity origin,
                                            @NonNull RoomEntity destinationRoom,
                                            @Nullable FloorEntity destinationFloor,
                                            @Nullable BuildingEntity destinationBuilding) {
        RouteDto route = createBaseRoute(destinationRoom);
        route.originId = origin != null ? origin.id : 0;

        String originLabel = getOriginLabel(origin);
        String destinationLabel = getRoomLabel(destinationRoom);
        String destinationBuildingLabel = getBuildingLabel(destinationBuilding);
        int destinationFloorNumber = getFloorNumber(destinationFloor);
        String destinationBuildingCode = normalizeCode(destinationBuilding != null ? destinationBuilding.code : null);
        int originFloorNumber = inferOriginFloor(origin);

        route.description = "Directions from " + originLabel + " to " + destinationLabel;
        route.instruction = "Follow the default walking route to " + destinationLabel + ".";

        List<RouteStepDto> steps = new ArrayList<>();
        int order = 1;

        if (isCampusAreaRoom(destinationRoom)) {
            steps.add(step(order++, "From " + originLabel + ", proceed toward the " + getCampusAreaLabel(destinationRoom) + ".", "straight", getCampusAreaLabel(destinationRoom)));
            route.steps = steps;
            return route;
        }

        if (CODE_MAIN.equals(destinationBuildingCode)) {
            boolean startingOutsideMain = isCampusStyleOrigin(origin);
            if (startingOutsideMain) {
                steps.add(step(order++, "Head to the Main Building entrance.", "straight", "Main Building"));
            }

            if (destinationFloorNumber > 1 && destinationFloorNumber != originFloorNumber) {
                String travelDirection = destinationFloorNumber > originFloorNumber ? "up" : "down";
                steps.add(step(order++, "Take the stairs to the " + ordinal(destinationFloorNumber) + " floor.", travelDirection, "Stairs"));
            }

            order = appendMainBuildingArrivalSteps(steps, order, destinationRoom, destinationFloorNumber);
        } else {
            steps.add(step(order++, "Proceed across campus toward the " + destinationBuildingLabel + ".", "straight", destinationBuildingLabel));
            if (destinationFloorNumber > 1) {
                steps.add(step(order++, "Go to the " + ordinal(destinationFloorNumber) + " floor of the " + destinationBuildingLabel + ".", "up", destinationBuildingLabel));
            }
            steps.add(step(order++, "Look for " + destinationLabel + " inside the " + destinationBuildingLabel + ".", null, destinationLabel));
        }

        route.steps = steps;
        return route;
    }

    @NonNull
    public static RouteDto createFromRoom(@NonNull RoomEntity originRoom,
                                          @Nullable FloorEntity originFloor,
                                          @Nullable BuildingEntity originBuilding,
                                          @NonNull RoomEntity destinationRoom,
                                          @Nullable FloorEntity destinationFloor,
                                          @Nullable BuildingEntity destinationBuilding) {
        RouteDto route = createBaseRoute(destinationRoom);
        route.originId = 0;

        String originLabel = getRoomLabel(originRoom);
        String destinationLabel = getRoomLabel(destinationRoom);
        String originBuildingLabel = getBuildingLabel(originBuilding);
        String destinationBuildingLabel = getBuildingLabel(destinationBuilding);
        String originBuildingCode = normalizeCode(originBuilding != null ? originBuilding.code : null);
        String destinationBuildingCode = normalizeCode(destinationBuilding != null ? destinationBuilding.code : null);
        int originFloorNumber = getFloorNumber(originFloor);
        int destinationFloorNumber = getFloorNumber(destinationFloor);

        route.description = "Directions from " + originLabel + " to " + destinationLabel;
        route.instruction = "Follow the default walking route to " + destinationLabel + ".";

        List<RouteStepDto> steps = new ArrayList<>();
        int order = 1;

        if (originRoom.id == destinationRoom.id) {
            steps.add(step(order, "You are already at " + destinationLabel + ".", null, destinationLabel));
            route.steps = steps;
            return route;
        }

        if (isCampusAreaRoom(destinationRoom)) {
            steps.add(step(order++, "Leave " + originLabel + " and head toward the " + getCampusAreaLabel(destinationRoom) + ".", "straight", originLabel));
            route.steps = steps;
            return route;
        }

        boolean sameBuilding = !originBuildingCode.isEmpty() && originBuildingCode.equals(destinationBuildingCode);
        boolean sameFloor = sameBuilding && originFloorNumber > 0 && originFloorNumber == destinationFloorNumber;

        if (sameFloor) {
            steps.add(step(order++, "Leave " + originLabel + " and continue along the hallway.", "straight", originLabel));
            if (CODE_MAIN.equals(destinationBuildingCode)) {
                order = appendMainBuildingArrivalSteps(steps, order, destinationRoom, destinationFloorNumber);
            } else {
                steps.add(step(order++, "Look for " + destinationLabel + " on the same floor of the " + destinationBuildingLabel + ".", null, destinationLabel));
            }
            route.steps = steps;
            return route;
        }

        if (sameBuilding) {
            steps.add(step(order++, "Leave " + originLabel + " and head to the stairs.", "straight", originLabel));
            String travelDirection = destinationFloorNumber >= originFloorNumber ? "up" : "down";
            steps.add(step(order++, "Go " + travelDirection + " to the " + ordinal(destinationFloorNumber) + " floor.", travelDirection, "Stairs"));
            if (CODE_MAIN.equals(destinationBuildingCode)) {
                order = appendMainBuildingArrivalSteps(steps, order, destinationRoom, destinationFloorNumber);
            } else {
                steps.add(step(order++, "Look for " + destinationLabel + " on the " + ordinal(destinationFloorNumber) + " floor of the " + destinationBuildingLabel + ".", null, destinationLabel));
            }
            route.steps = steps;
            return route;
        }

        steps.add(step(order++, "Leave " + originLabel + " and exit the " + originBuildingLabel + ".", "straight", originLabel));
        steps.add(step(order++, "Proceed across campus toward the " + destinationBuildingLabel + ".", "straight", destinationBuildingLabel));
        if (CODE_MAIN.equals(destinationBuildingCode) && destinationFloorNumber > 1) {
            steps.add(step(order++, "Take the stairs to the " + ordinal(destinationFloorNumber) + " floor.", "up", "Stairs"));
            order = appendMainBuildingArrivalSteps(steps, order, destinationRoom, destinationFloorNumber);
        } else if (destinationFloorNumber > 1) {
            steps.add(step(order++, "Go to the " + ordinal(destinationFloorNumber) + " floor of the " + destinationBuildingLabel + ".", "up", destinationBuildingLabel));
            steps.add(step(order++, "Look for " + destinationLabel + ".", null, destinationLabel));
        } else {
            steps.add(step(order++, "Look for " + destinationLabel + " inside the " + destinationBuildingLabel + ".", null, destinationLabel));
        }

        route.steps = steps;
        return route;
    }

    private static int appendMainBuildingArrivalSteps(@NonNull List<RouteStepDto> steps,
                                                      int startOrder,
                                                      @NonNull RoomEntity destinationRoom,
                                                      int floorNumber) {
        String destinationLabel = getRoomLabel(destinationRoom);
        SlotInfo slotInfo = findSlotInfo(destinationRoom, floorNumber);
        if (slotInfo != null) {
            steps.add(step(startOrder++, "Continue along the " + ordinal(floorNumber) + " floor hallway and keep to the " + slotInfo.sideLabel + " side.", "straight", ordinal(floorNumber) + " floor hallway"));
            steps.add(step(startOrder++, destinationLabel + " is on your " + slotInfo.sideLabel + ".", slotInfo.direction, destinationLabel));
            return startOrder;
        }

        steps.add(step(startOrder++, "Continue along the " + ordinal(floorNumber) + " floor hallway.", "straight", ordinal(floorNumber) + " floor hallway"));
        steps.add(step(startOrder++, "Look for " + destinationLabel + ".", null, destinationLabel));
        return startOrder;
    }

    @NonNull
    private static RouteDto createBaseRoute(@NonNull RoomEntity destinationRoom) {
        RouteDto route = new RouteDto();
        route.id = 0;
        route.destinationRoomId = destinationRoom.id;
        route.steps = new ArrayList<>();
        return route;
    }

    @NonNull
    private static RouteStepDto step(int order,
                                     @NonNull String instruction,
                                     @Nullable String direction,
                                     @Nullable String landmark) {
        RouteStepDto step = new RouteStepDto();
        step.orderNum = order;
        step.instruction = instruction;
        step.direction = normalizeDirection(direction);
        step.landmark = isBlank(landmark) ? null : landmark.trim();
        return step;
    }

    @Nullable
    private static SlotInfo findSlotInfo(@NonNull RoomEntity room, int floorNumber) {
        Map<String, SlotInfo> floorSlots = MAIN_BUILDING_SLOTS.get(floorNumber);
        if (floorSlots == null || floorSlots.isEmpty()) {
            return null;
        }
        String code = normalizeCode(room.code);
        if (floorSlots.containsKey(code)) {
            return floorSlots.get(code);
        }
        return floorSlots.get(normalizeCode(room.name));
    }

    private static boolean isCampusAreaRoom(@Nullable RoomEntity room) {
        String code = normalizeCode(room != null ? room.code : null);
        return CODE_COURT.equals(code) || CODE_ENT.equals(code) || CODE_EXIT.equals(code);
    }

    @NonNull
    private static String getCampusAreaLabel(@NonNull RoomEntity room) {
        String code = normalizeCode(room.code);
        if (CODE_ENT.equals(code)) {
            return "Entrance";
        }
        if (CODE_EXIT.equals(code)) {
            return "Exit";
        }
        if (CODE_COURT.equals(code)) {
            return "Court";
        }
        return getRoomLabel(room);
    }

    private static boolean isCampusStyleOrigin(@Nullable OriginEntity origin) {
        String combined = combinedOriginText(origin);
        return combined.contains("entrance")
                || combined.contains("gate")
                || combined.contains("lobby")
                || combined.contains("court")
                || combined.contains("exit");
    }

    private static int inferOriginFloor(@Nullable OriginEntity origin) {
        if (origin == null) {
            return 1;
        }

        int explicitFloor = findFloorNumber(origin.name, origin.code, origin.description);
        if (explicitFloor > 0) {
            return explicitFloor;
        }

        String combined = combinedOriginText(origin);
        if (combined.contains("entrance")
                || combined.contains("gate")
                || combined.contains("lobby")
                || combined.contains("ground")) {
            return 1;
        }
        return 1;
    }

    @NonNull
    private static String combinedOriginText(@Nullable OriginEntity origin) {
        if (origin == null) {
            return "";
        }
        return normalizeText(origin.name) + " "
                + normalizeText(origin.code) + " "
                + normalizeText(origin.description);
    }

    private static int findFloorNumber(@Nullable String... values) {
        if (values == null) {
            return -1;
        }
        for (int floorNumber = 1; floorNumber <= 5; floorNumber++) {
            String ordinal = floorNumber == 1 ? "1st"
                    : floorNumber == 2 ? "2nd"
                    : floorNumber == 3 ? "3rd"
                    : floorNumber + "th";
            String word = floorNumber == 1 ? "first"
                    : floorNumber == 2 ? "second"
                    : floorNumber == 3 ? "third"
                    : floorNumber == 4 ? "fourth"
                    : "fifth";
            String digit = String.valueOf(floorNumber);

            for (String value : values) {
                String normalized = normalizeText(value);
                if (normalized.isEmpty()) {
                    continue;
                }
                if (normalized.contains(ordinal + " floor")
                        || normalized.contains("floor " + digit)
                        || normalized.contains(word + " floor")
                        || normalized.contains("level " + digit)
                        || normalized.contains("floor-" + digit)
                        || normalized.contains("floor_" + digit)) {
                    return floorNumber;
                }
            }
        }
        return -1;
    }

    @NonNull
    private static String getOriginLabel(@Nullable OriginEntity origin) {
        if (origin == null) {
            return "your starting point";
        }
        if (!isBlank(origin.name)) {
            return origin.name.trim();
        }
        if (!isBlank(origin.code)) {
            return origin.code.trim();
        }
        return "your starting point";
    }

    @NonNull
    private static String getRoomLabel(@Nullable RoomEntity room) {
        if (room == null) {
            return "your destination";
        }
        if (!isBlank(room.name)) {
            return room.name.trim();
        }
        if (!isBlank(room.code)) {
            return room.code.trim();
        }
        return "your destination";
    }

    @NonNull
    private static String getBuildingLabel(@Nullable BuildingEntity building) {
        String code = normalizeCode(building != null ? building.code : null);
        if (CODE_MAIN.equals(code)) {
            return "Main Building";
        }
        if (CODE_LIB.equals(code)) {
            return "Library";
        }
        if (CODE_REG.equals(code)) {
            return "Registrar";
        }
        if (building != null && !isBlank(building.name)) {
            return building.name.trim();
        }
        if (building != null && !isBlank(building.code)) {
            return building.code.trim();
        }
        return "destination building";
    }

    private static int getFloorNumber(@Nullable FloorEntity floor) {
        return floor != null && floor.number > 0 ? floor.number : 1;
    }

    @Nullable
    private static String normalizeDirection(@Nullable String direction) {
        if (isBlank(direction)) {
            return null;
        }
        String normalized = direction.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "straight":
            case "left":
            case "right":
            case "up":
            case "down":
                return normalized;
            default:
                return null;
        }
    }

    @NonNull
    private static String normalizeCode(@Nullable String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }

    @NonNull
    private static String normalizeText(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private static String ordinal(int value) {
        if (value % 100 >= 11 && value % 100 <= 13) {
            return value + "th";
        }
        switch (value % 10) {
            case 1:
                return value + "st";
            case 2:
                return value + "nd";
            case 3:
                return value + "rd";
            default:
                return value + "th";
        }
    }

    @NonNull
    private static Map<Integer, Map<String, SlotInfo>> createMainBuildingSlots() {
        LinkedHashMap<Integer, Map<String, SlotInfo>> floors = new LinkedHashMap<>();
        floors.put(1, createFloorSlots(new String[][]{
                {"KITCHEN_LAB", "left", "left"},
                {"BARTENDER_LAB", "left", "left"},
                {"108", "left", "left"},
                {"105", "left", "left"},
                {"104", "left", "left"},
                {"IT_CENTER", "left", "left"},
                {"GUIDANCE", "left", "left"},
                {"PWD_CR", "right", "right"},
                {"109", "right", "right"},
                {"FACULTY", "right", "right"},
                {"HR_OFFICE", "right", "right"},
                {"FINANCE", "right", "right"},
                {"PHOTO_LAB", "right", "right"},
                {"CRIMINOLOGY", "right", "right"},
                {"101", "right", "right"}
        }));
        floors.put(2, createFloorSlots(new String[][]{
                {"MAIN-2-LR211", "left", "left"},
                {"MAIN-2-LR209", "left", "left"},
                {"MAIN-2-LR207", "left", "left"},
                {"MAIN-2-LR205", "left", "left"},
                {"MAIN-2-LR203", "left", "left"},
                {"MAIN-2-LR201", "left", "left"},
                {"MAIN-2-LR212", "right", "right"},
                {"MAIN-2-LR210", "right", "right"},
                {"MAIN-2-LR208", "right", "right"},
                {"MAIN-2-LR206", "right", "right"},
                {"MAIN-2-LR204", "right", "right"},
                {"MAIN-2-LR202", "right", "right"}
        }));
        floors.put(3, createFloorSlots(new String[][]{
                {"MAIN-3-CBA-COORD", "left", "left"},
                {"MAIN-3-CBA-DEAN", "left", "left"},
                {"MAIN-3-LR310", "left", "left"},
                {"MAIN-3-LR308", "left", "left"},
                {"MAIN-3-CLAS-COORD", "left", "left"},
                {"MAIN-3-SOUND-LAB", "left", "left"},
                {"MAIN-3-LR304", "left", "left"},
                {"MAIN-3-LR302", "left", "left"},
                {"MAIN-3-MIS-DATA", "right", "right"},
                {"MAIN-3-CS-DEPT", "right", "right"},
                {"MAIN-3-MULTIMEDIA", "right", "right"},
                {"MAIN-3-LABTECH", "right", "right"},
                {"MAIN-3-COMPLAB1", "right", "right"},
                {"MAIN-3-COMPLAB2", "right", "right"},
                {"MAIN-3-COMPLAB3", "right", "right"}
        }));
        floors.put(4, createFloorSlots(new String[][]{
                {"MAIN-4-LR411", "left", "left"},
                {"MAIN-4-EARLY-CHILD", "left", "left"},
                {"MAIN-4-LR408", "left", "left"},
                {"MAIN-4-PHYSICS-LAB", "left", "left"},
                {"MAIN-4-LR404", "left", "left"},
                {"MAIN-4-LR402", "left", "left"},
                {"MAIN-4-COE-COUNCIL", "right", "right"},
                {"MAIN-4-EDTECH-LAB", "right", "right"},
                {"MAIN-4-BIO-LAB", "right", "right"},
                {"MAIN-4-CHEM-LAB", "right", "right"},
                {"MAIN-4-LAW-OFFICE", "right", "right"},
                {"MAIN-4-LR401", "right", "right"},
                {"MAIN-4-COE", "right", "right"}
        }));
        return Collections.unmodifiableMap(floors);
    }

    @NonNull
    private static Map<String, SlotInfo> createFloorSlots(@NonNull String[][] definitions) {
        LinkedHashMap<String, SlotInfo> slots = new LinkedHashMap<>();
        for (String[] definition : definitions) {
            if (definition.length < 3) {
                continue;
            }
            slots.put(definition[0], new SlotInfo(definition[1], definition[2]));
        }
        return Collections.unmodifiableMap(slots);
    }

    private static final class SlotInfo {
        private final String sideLabel;
        private final String direction;

        private SlotInfo(@NonNull String sideLabel, @NonNull String direction) {
            this.sideLabel = sideLabel;
            this.direction = direction;
        }
    }
}
