package com.example.freshguide.ui.view;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutePreviewMapView extends LinearLayout {

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView titleView;
    private TextView subtitleView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private View emptyLayout;
    private FrameLayout mapHost;

    @Nullable
    private ScrollView currentScrollView;
    @Nullable
    private ConstraintLayout currentMapContent;
    @Nullable
    private RoutePathOverlayView currentOverlay;
    private int currentFloorNumber = -1;

    public RoutePreviewMapView(Context context) {
        this(context, null);
    }

    public RoutePreviewMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_route_preview_map, this, true);
        titleView = findViewById(R.id.tv_route_preview_title);
        subtitleView = findViewById(R.id.tv_route_preview_subtitle);
        emptyView = findViewById(R.id.tv_route_preview_empty);
        progressBar = findViewById(R.id.progress_route_preview);
        emptyLayout = findViewById(R.id.layout_route_preview_empty);
        mapHost = findViewById(R.id.layout_route_preview_map_host);
        showEmpty("Route preview appears here");
    }

    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
        mapHost.removeAllViews();
        titleView.setText("Route Preview");
        subtitleView.setText("Preparing the floor map...");
    }

    public void clearPreview() {
        progressBar.setVisibility(View.GONE);
        showEmpty("Route preview appears here");
    }

    public void renderRoute(@Nullable RouteDto route, int destinationRoomId, int originId, boolean reversed) {
        if (route == null || destinationRoomId <= 0) {
            clearPreview();
            return;
        }

        showLoading();
        Context appContext = getContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            RoomEntity destinationRoom = db.roomDao().getByIdSync(destinationRoomId);
            if (destinationRoom == null) {
                post(() -> showEmpty("Route preview is not available for this destination."));
                return;
            }

            FloorEntity destinationFloor = db.floorDao().getByIdSync(destinationRoom.floorId);
            BuildingEntity destinationBuilding = destinationFloor != null
                    ? db.buildingDao().getByIdSync(destinationFloor.buildingId)
                    : null;
            OriginEntity origin = db.originDao().getByIdSync(originId);

            if (destinationFloor == null || destinationBuilding == null
                    || destinationBuilding.code == null
                    || !CODE_MAIN.equalsIgnoreCase(destinationBuilding.code.trim())) {
                post(() -> showEmpty("Route preview is currently available for the main building only."));
                return;
            }

            List<RoomEntity> floorRooms = db.roomDao()
                    .getRoomsByBuildingAndFloorSync(destinationBuilding.code, destinationFloor.number);
            if (floorRooms == null) {
                floorRooms = Collections.emptyList();
            } else {
                floorRooms = new ArrayList<>(floorRooms);
                floorRooms.sort(Comparator.comparingInt(room -> room.id));
            }

            PreviewModel previewModel = new PreviewModel(
                    destinationFloor.number,
                    destinationRoom,
                    origin,
                    floorRooms,
                    route,
                    reversed
            );
            post(() -> applyPreviewModel(previewModel));
        });
    }

    private void applyPreviewModel(@NonNull PreviewModel previewModel) {
        progressBar.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        titleView.setText(buildFloorTitle(previewModel.floorNumber));
        subtitleView.setText(buildSubtitle(previewModel));

        if (currentFloorNumber != previewModel.floorNumber || mapHost.getChildCount() == 0) {
            inflateFloorLayout(previewModel.floorNumber);
        }
        if (currentMapContent == null) {
            showEmpty("Route preview is not available for this floor.");
            return;
        }

        bindFloorRooms(previewModel);
    }

    private void inflateFloorLayout(int floorNumber) {
        currentFloorNumber = floorNumber;
        mapHost.removeAllViews();

        int layoutRes = getLayoutForFloor(floorNumber);
        if (layoutRes == 0) {
            return;
        }

        View root = LayoutInflater.from(getContext()).inflate(layoutRes, mapHost, false);
        mapHost.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (root instanceof LinearLayout) {
            LinearLayout linear = (LinearLayout) root;
            if (linear.getChildCount() > 0) {
                linear.getChildAt(0).setVisibility(View.GONE);
            }
        }

        currentScrollView = findFirstScrollView(root);
        currentMapContent = currentScrollView != null ? findConstraintContent(currentScrollView) : null;

        if (currentScrollView != null) {
            currentScrollView.setOnTouchListener((v, event) -> true);
            currentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        if (currentMapContent != null) {
            currentMapContent.setClipChildren(false);
            currentMapContent.setClipToPadding(false);
            currentOverlay = new RoutePathOverlayView(getContext());
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            currentMapContent.addView(currentOverlay, params);
        }
    }

    private void bindFloorRooms(@NonNull PreviewModel previewModel) {
        if (currentMapContent == null) {
            return;
        }

        clearFloorRoomViews(previewModel.floorNumber);
        List<RoomEntity> usableRooms = new ArrayList<>();
        for (RoomEntity room : previewModel.floorRooms) {
            if (room == null || isCampusAreaCode(room.code)) {
                continue;
            }
            usableRooms.add(room);
        }

        View destinationView = null;
        if (previewModel.floorNumber == 1) {
            destinationView = bindFloorOneRooms(usableRooms, previewModel.destinationRoom.id);
        } else if (previewModel.floorNumber == 5) {
            destinationView = bindFloorFiveRooms(usableRooms, previewModel.destinationRoom.id);
        } else {
            destinationView = bindSequentialRooms(usableRooms, previewModel.destinationRoom.id);
        }

        if (destinationView == null) {
            showEmpty("Route preview is not available for this room yet.");
            return;
        }

        View finalDestinationView = destinationView;
        currentMapContent.post(() -> drawPreviewRoute(previewModel, finalDestinationView));
    }

    @Nullable
    private View bindSequentialRooms(@NonNull List<RoomEntity> rooms, int destinationRoomId) {
        View destinationView = null;
        int max = Math.min(rooms.size(), DEFAULT_ROOM_BOX_IDS.length);
        for (int i = 0; i < max; i++) {
            View roomBox = currentMapContent.findViewById(DEFAULT_ROOM_BOX_IDS[i]);
            if (roomBox == null) {
                continue;
            }
            RoomEntity room = rooms.get(i);
            bindRoomLabel(roomBox, room, room.id == destinationRoomId);
            if (room.id == destinationRoomId) {
                destinationView = roomBox;
            }
        }
        return destinationView;
    }

    @Nullable
    private View bindFloorOneRooms(@NonNull List<RoomEntity> rooms, int destinationRoomId) {
        View destinationView = null;
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : rooms) {
            String code = normalizeCode(room.code);
            if (code != null && FLOOR1_ROOM_SLOTS.containsKey(code) && !roomByCode.containsKey(code)) {
                roomByCode.put(code, room);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR1_ROOM_SLOTS.entrySet()) {
            View roomBox = currentMapContent.findViewById(slot.getValue());
            if (roomBox == null) {
                continue;
            }
            RoomEntity room = roomByCode.get(slot.getKey());
            if (room == null) {
                continue;
            }
            bindRoomLabel(roomBox, room, room.id == destinationRoomId);
            if (room.id == destinationRoomId) {
                destinationView = roomBox;
            }
        }
        return destinationView;
    }

    @Nullable
    private View bindFloorFiveRooms(@NonNull List<RoomEntity> rooms, int destinationRoomId) {
        bindStaticRoomLabel(R.id.room_left_1, "V-48");
        bindStaticRoomLabel(R.id.room_left_2, "V-46");
        bindStaticRoomLabel(R.id.room_left_3, "Industrial\nArts\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_1, "Speech\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_2, "Craft /\nSewing\nLaboratory");
        bindStaticRoomLabel(R.id.room_right_3, "Office");
        bindStaticRoomLabel(R.id.room_auditorium, "Auditorium");

        RoomEntity auditorium = findFloorFiveAuditorium(rooms);
        if (auditorium == null) {
            return null;
        }
        View auditoriumBox = currentMapContent.findViewById(R.id.room_auditorium);
        if (auditoriumBox == null) {
            return null;
        }
        bindRoomLabel(auditoriumBox, auditorium, auditorium.id == destinationRoomId);
        TextView roomLabel = auditoriumBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText("Auditorium");
        }
        return auditorium.id == destinationRoomId ? auditoriumBox : null;
    }

    private void bindStaticRoomLabel(int roomViewId, @NonNull String label) {
        if (currentMapContent == null) {
            return;
        }
        View roomBox = currentMapContent.findViewById(roomViewId);
        if (roomBox == null) {
            return;
        }
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(label);
        }
    }

    private void bindRoomLabel(@NonNull View roomBox, @NonNull RoomEntity room, boolean highlighted) {
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(getRoomDisplayName(room));
            roomLabel.setTextColor(ContextCompat.getColor(getContext(),
                    highlighted ? R.color.green_primary : R.color.floor_room_label));
            roomLabel.setTypeface(Typeface.DEFAULT, highlighted ? Typeface.BOLD : Typeface.NORMAL);
        }
        View pin = roomBox.findViewById(R.id.room_pin);
        if (pin != null) {
            pin.setVisibility(View.GONE);
        }
        roomBox.setClickable(false);
        roomBox.setFocusable(false);
        roomBox.setOnClickListener(null);
        roomBox.setOnTouchListener((v, event) -> true);
    }

    private void drawPreviewRoute(@NonNull PreviewModel previewModel, @NonNull View destinationView) {
        if (currentMapContent == null || currentOverlay == null) {
            return;
        }

        PointF endPoint = createRoomAnchor(destinationView);
        PointF startPoint = resolveStartAnchor(previewModel, destinationView, endPoint);
        float hallwayX = currentMapContent.getWidth() / 2f;

        currentOverlay.setRoute(startPoint, endPoint, hallwayX);
        centerPreview(destinationView, startPoint, endPoint);
    }

    @NonNull
    private PointF resolveStartAnchor(@NonNull PreviewModel previewModel,
                                      @NonNull View destinationView,
                                      @NonNull PointF endPoint) {
        if (shouldUseElevator(previewModel.route)) {
            View elevator = currentMapContent.findViewById(R.id.elevator);
            if (elevator != null) {
                return createAnchorAtTopCenter(elevator);
            }
        }

        if (shouldUseStairs(previewModel)) {
            View topStairs = currentMapContent.findViewById(R.id.stairs_top);
            View bottomStairs = currentMapContent.findViewById(R.id.stairs_bottom);
            View chosen = pickNearestAnchor(destinationView, topStairs, bottomStairs);
            if (chosen != null) {
                return createAnchorAtCenter(chosen);
            }
        }

        View mirroredRoom = findMirroredRoom(destinationView);
        if (mirroredRoom != null) {
            return createRoomAnchor(mirroredRoom);
        }

        return new PointF(dpToPx(42), endPoint.y);
    }

    private boolean shouldUseStairs(@NonNull PreviewModel previewModel) {
        if (previewModel.floorNumber > 1) {
            return true;
        }
        String routeText = buildRouteText(previewModel);
        return routeText.contains("stairs")
                || routeText.contains("stair")
                || routeText.contains("2nd floor")
                || routeText.contains("3rd floor")
                || routeText.contains("4th floor")
                || routeText.contains("5th floor");
    }

    private boolean shouldUseElevator(@Nullable RouteDto route) {
        String routeText = buildRouteText(route);
        return routeText.contains("elevator") || routeText.contains("lift");
    }

    @NonNull
    private String buildRouteText(@Nullable RouteDto route) {
        if (route == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendText(builder, route.description);
        appendText(builder, route.instruction);
        if (route.steps != null) {
            for (RouteStepDto step : route.steps) {
                if (step == null) {
                    continue;
                }
                appendText(builder, step.instruction);
                appendText(builder, step.direction);
                appendText(builder, step.landmark);
            }
        }
        return builder.toString();
    }

    @NonNull
    private String buildRouteText(@NonNull PreviewModel previewModel) {
        StringBuilder builder = new StringBuilder(buildRouteText(previewModel.route));
        if (previewModel.origin != null) {
            appendText(builder, previewModel.origin.name);
            appendText(builder, previewModel.origin.code);
            appendText(builder, previewModel.origin.description);
        }
        return builder.toString();
    }

    private void appendText(@NonNull StringBuilder builder, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        builder.append(' ').append(value.trim().toLowerCase(Locale.US));
    }

    @Nullable
    private View findMirroredRoom(@NonNull View destinationView) {
        if (currentMapContent == null) {
            return null;
        }
        int[] roomBoxIds = getActiveRoomBoxIds(currentFloorNumber);
        float destinationCenterX = destinationView.getX() + (destinationView.getWidth() / 2f);
        float destinationCenterY = destinationView.getY() + (destinationView.getHeight() / 2f);
        float hallwayX = currentMapContent.getWidth() / 2f;
        boolean destinationOnLeft = destinationCenterX < hallwayX;

        View bestMatch = null;
        float bestDistance = Float.MAX_VALUE;
        for (int roomBoxId : roomBoxIds) {
            View roomBox = currentMapContent.findViewById(roomBoxId);
            if (roomBox == null || roomBox == destinationView) {
                continue;
            }
            float centerX = roomBox.getX() + (roomBox.getWidth() / 2f);
            float centerY = roomBox.getY() + (roomBox.getHeight() / 2f);
            boolean onLeft = centerX < hallwayX;
            if (onLeft == destinationOnLeft) {
                continue;
            }
            float distance = Math.abs(centerY - destinationCenterY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = roomBox;
            }
        }
        return bestMatch;
    }

    @Nullable
    private View pickNearestAnchor(@NonNull View destinationView,
                                   @Nullable View firstAnchor,
                                   @Nullable View secondAnchor) {
        if (firstAnchor == null) return secondAnchor;
        if (secondAnchor == null) return firstAnchor;

        float destinationCenterY = destinationView.getY() + (destinationView.getHeight() / 2f);
        float firstCenterY = firstAnchor.getY() + (firstAnchor.getHeight() / 2f);
        float secondCenterY = secondAnchor.getY() + (secondAnchor.getHeight() / 2f);
        return Math.abs(firstCenterY - destinationCenterY) <= Math.abs(secondCenterY - destinationCenterY)
                ? firstAnchor
                : secondAnchor;
    }

    @NonNull
    private PointF createRoomAnchor(@NonNull View roomView) {
        return new PointF(
                roomView.getX() + (roomView.getWidth() / 2f),
                roomView.getY() + dpToPx(6)
        );
    }

    @NonNull
    private PointF createAnchorAtCenter(@NonNull View anchorView) {
        return new PointF(
                anchorView.getX() + (anchorView.getWidth() / 2f),
                anchorView.getY() + (anchorView.getHeight() / 2f)
        );
    }

    @NonNull
    private PointF createAnchorAtTopCenter(@NonNull View anchorView) {
        return new PointF(
                anchorView.getX() + (anchorView.getWidth() / 2f),
                anchorView.getY() + dpToPx(10)
        );
    }

    private void centerPreview(@NonNull View destinationView, @NonNull PointF startPoint, @NonNull PointF endPoint) {
        if (currentScrollView == null) {
            return;
        }
        float routeCenterY = (startPoint.y + endPoint.y) / 2f;
        int targetY = Math.max(0, Math.round(routeCenterY - (currentScrollView.getHeight() / 2f)));
        currentScrollView.scrollTo(0, targetY);
    }

    private void clearFloorRoomViews(int floorNumber) {
        if (currentMapContent == null) {
            return;
        }

        for (int roomBoxId : getActiveRoomBoxIds(floorNumber)) {
            View roomBox = currentMapContent.findViewById(roomBoxId);
            if (roomBox == null) {
                continue;
            }
            TextView roomLabel = roomBox.findViewById(R.id.room_label);
            if (roomLabel != null) {
                roomLabel.setText("Room");
                roomLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.floor_room_label));
                roomLabel.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            }
            View pin = roomBox.findViewById(R.id.room_pin);
            if (pin != null) {
                pin.setVisibility(View.GONE);
            }
        }

        if (currentOverlay != null) {
            currentOverlay.clearRoute();
        }
    }

    @NonNull
    private String buildFloorTitle(int floorNumber) {
        return ordinal(floorNumber) + " Floor Route Preview";
    }

    @NonNull
    private String buildSubtitle(@NonNull PreviewModel model) {
        if (shouldUseElevator(model.route)) {
            return "Showing the elevator-side path to your destination.";
        }
        if (shouldUseStairs(model)) {
            return "Showing the arrival floor path from the nearest stairs.";
        }
        return "Showing a simple hallway preview toward your destination.";
    }

    private void showEmpty(@NonNull String message) {
        progressBar.setVisibility(View.GONE);
        mapHost.removeAllViews();
        emptyLayout.setVisibility(View.VISIBLE);
        emptyView.setText(message);
        titleView.setText("Route Preview");
        subtitleView.setText("A map line will appear here when it is available.");
    }

    private int getLayoutForFloor(int floorNumber) {
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

    @Nullable
    private ScrollView findFirstScrollView(@NonNull View root) {
        if (root instanceof ScrollView) {
            return (ScrollView) root;
        }
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            ScrollView match = findFirstScrollView(group.getChildAt(i));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    @Nullable
    private ConstraintLayout findConstraintContent(@NonNull View root) {
        if (root instanceof ConstraintLayout) {
            return (ConstraintLayout) root;
        }
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            ConstraintLayout match = findConstraintContent(group.getChildAt(i));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private boolean isCampusAreaCode(@Nullable String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.US);
        return CODE_COURT.equals(normalized)
                || CODE_REG.equals(normalized)
                || CODE_LIB.equals(normalized)
                || CODE_ENT.equals(normalized)
                || CODE_EXIT.equals(normalized);
    }

    @Nullable
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
            if (room.name != null && room.name.toUpperCase(Locale.US).contains("AUDITORIUM")) {
                nameMatch = room;
            }
        }
        return nameMatch;
    }

    @Nullable
    private String normalizeCode(@Nullable String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String normalized = rawCode.trim().toUpperCase(Locale.US);
        return normalized.isEmpty() ? null : normalized;
    }

    @NonNull
    private String getRoomDisplayName(@NonNull RoomEntity room) {
        if (room.name != null && !room.name.trim().isEmpty()) {
            return room.name.trim();
        }
        if (room.code != null && !room.code.trim().isEmpty()) {
            return room.code.trim();
        }
        return "Room";
    }

    private int[] getActiveRoomBoxIds(int floorNumber) {
        if (floorNumber == 1) {
            return FLOOR1_ROOM_BOX_IDS;
        }
        if (floorNumber == 5) {
            return FLOOR5_ROOM_BOX_IDS;
        }
        return DEFAULT_ROOM_BOX_IDS;
    }

    @NonNull
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

    @NonNull
    private String ordinal(int value) {
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

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        executor.shutdownNow();
    }

    private static final class PreviewModel {
        final int floorNumber;
        final RoomEntity destinationRoom;
        @Nullable final OriginEntity origin;
        final List<RoomEntity> floorRooms;
        final RouteDto route;
        final boolean reversed;

        private PreviewModel(int floorNumber,
                             @NonNull RoomEntity destinationRoom,
                             @Nullable OriginEntity origin,
                             @NonNull List<RoomEntity> floorRooms,
                             @NonNull RouteDto route,
                             boolean reversed) {
            this.floorNumber = floorNumber;
            this.destinationRoom = destinationRoom;
            this.origin = origin;
            this.floorRooms = floorRooms;
            this.route = route;
            this.reversed = reversed;
        }
    }
}
