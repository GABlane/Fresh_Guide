package com.example.freshguide.ui.user;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.os.Bundle;
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
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.repository.SavedRoomRepository;
import com.example.freshguide.ui.adapter.RoomImageGalleryAdapter;
import com.example.freshguide.ui.view.RoutePathOverlayView;
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
    public static final String KEY_DIRECTIONS_LAUNCH_REQUEST = "directions_launch_request";
    private static final String CODE_MAIN = "MAIN";
    private static final String CODE_REG = "REG";
    private static final String CODE_LIB = "LIB";
    private static final String CODE_COURT = "COURT";
    private static final String CODE_ENT = "ENT";
    private static final String CODE_EXIT = "EXIT";
    private static final int ROUTE_ANCHOR_AUTO = 0;
    private static final int ROUTE_ANCHOR_STAIRS_TOP = 1;
    private static final int ROUTE_ANCHOR_STAIRS_BOTTOM = 2;
    private static final int ROUTE_ANCHOR_ELEVATOR = 3;
    private static final int OVERALL_ANCHOR_ENTRANCE = 1;
    private static final int OVERALL_ANCHOR_EXIT = 2;
    private static final int OVERALL_ANCHOR_LIBRARY = 3;
    private static final int OVERALL_ANCHOR_REGISTRAR = 4;
    private static final int OVERALL_ANCHOR_COURT = 5;
    private static final int OVERALL_ANCHOR_MAIN = 6;
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
    private View homeLogoView;
    private View homeSearchView;
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
    private View routeFloorControls;
    private MaterialButton btnRoutePrevFloor;
    private MaterialButton btnRouteNextFloor;
    private MaterialButton btnExitDirectionMode;
    private boolean hasDirectionsPrompted;
    private FloatingActionButton fabCompass;
    private FloatingActionButton fabEmergencyExit;
    private int activeRoomId = -1;
    private boolean activeRoomIsCampusArea;
    private RoomEntity activeRoom;
    private String latestImageUrl;
    private Runnable pendingAfterRoomSheetHidden;
    private final Map<Integer, Integer> currentRoomBoxIds = new HashMap<>();
    private RoutePathOverlayView floorRouteOverlay;
    private RoutePathOverlayView overallRouteOverlay;
    @Nullable
    private RouteOverlayState activeRouteOverlay;
    private int activeRouteDestinationRoomId = -1;
    private int activeRouteOriginId = -1;
    private int activeRouteOriginRoomId = -1;
    @Nullable
    private Animator activeRouteAnchorAnimator;
    @Nullable
    private View activeRouteAnchorView;
    private boolean isDirectionsFocusMode;
    private boolean isDirectionsSheetVisible;
    private int activeDirectionsSheetSessionId;

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

        // Right column
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

        // Right column
        map.put("MAIN-4-COE-COUNCIL", R.id.room_right_1);
        map.put("MAIN-4-EDTECH-LAB", R.id.room_right_2);
        map.put("MAIN-4-BIO-LAB", R.id.room_right_3);
        map.put("MAIN-4-CHEM-LAB", R.id.room_right_4);
        map.put("MAIN-4-LAW-OFFICE", R.id.room_right_5);
        map.put("MAIN-4-LR401", R.id.room_right_6);
        map.put("MAIN-4-COE", R.id.room_right_7);
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
        fabEmergencyExit = view.findViewById(R.id.fab_emergency_exit);
        homeLogoView = view.findViewById(R.id.iv_home_logo);
        homeSearchView = view.findViewById(R.id.layout_search);
        routeFloorControls = view.findViewById(R.id.layout_route_floor_controls);
        btnRoutePrevFloor = view.findViewById(R.id.btn_route_prev_floor);
        btnRouteNextFloor = view.findViewById(R.id.btn_route_next_floor);
        btnExitDirectionMode = view.findViewById(R.id.btn_exit_direction_mode);

        NavController nav = Navigation.findNavController(view);

        applyBuildingPinOverlays(view);
        setupRoomDetailSheet(view);
        setupSearch(view, nav);
        setupFloorChips(view);
        setupOverallMapClicks(view, nav);
        setupChipFade();
        setupFab();
        setupExitDirectionModeButton();
        setupRouteFloorControls();
        observeSync(view);
        observeMapFocusRequests();
        observeDirectionsLaunchRequests();
        observeDirectionsRouteOverlay();
        observeDirectionsSheetVisibility();
        observeDirectionsFocusMode();

        viewModel.sync();
    }

    private void setupSearch(View view, NavController nav) {
        if (homeSearchView == null) {
            return;
        }
        homeSearchView.setOnClickListener(v -> {
            if (shouldBlockBrowsingForDirections()) {
                return;
            }

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
        updateFloorChipAvailability();
        updateFade();
    }

    private void handleFloorChipClick(Chip clickedChip, int floor, Chip[] allChips) {
        if (isDirectionsFocusMode && selectedFloor != null && selectedFloor == floor) {
            return;
        }

        if (activeRouteOverlay != null) {
            if (!isFloorWithinActiveRoute(floor)) {
                return;
            }
            if (activeRouteOverlay.campusTransitionSequence) {
                activeRouteOverlay.showingCampusLeg = false;
            }
            activeRouteOverlay.currentFloorNumber = floor;
            setFloorSelection(floor);
            return;
        }

        if (selectedFloor != null && selectedFloor == floor) {
            setFloorSelection(null);
            return;
        }

        setFloorSelection(floor);
    }

    private void showOverallMap() {
        clearRoomHighlight();
        clearActiveRouteAnchorCue();

        if (overallMapContainer != null) {
            overallMapContainer.setVisibility(View.VISIBLE);
        }

        if (floorMapContainer != null) {
            floorMapContainer.setVisibility(View.GONE);
            floorMapContainer.removeAllViews();
            floorRouteOverlay = null;
        }

        renderActiveRouteOverlay();
    }

    private void showFloorMap(int floor) {
        clearActiveRouteAnchorCue();
        if (overallMapContainer != null) {
            overallMapContainer.setVisibility(View.GONE);
        }
        clearOverallRouteOverlay();

        if (floorMapContainer != null) {
            floorMapContainer.setVisibility(View.VISIBLE);
            floorMapContainer.removeAllViews();
            floorRouteOverlay = null;

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
                ensureFloorRouteOverlayAttached();
                bindFloorData(floor);
            }
        }
    }

    private void setupOverallMapClicks(View view, NavController nav) {
        setClick(view, R.id.img_main_building, () -> openMainBuildingFloor());
        setClick(view, R.id.img_pin_main, () -> openMainBuildingFloor());
        setClick(view, R.id.txt_main_building, () -> openMainBuildingFloor());

        setClick(view, R.id.img_library, () -> navigateToCampusAreaRoom(nav, CODE_LIB, "LIBRARY"));
        setClick(view, R.id.img_pin_library, () -> navigateToCampusAreaRoom(nav, CODE_LIB, "LIBRARY"));
        setClick(view, R.id.txt_library, () -> navigateToCampusAreaRoom(nav, CODE_LIB, "LIBRARY"));

        setClick(view, R.id.img_registrar, () -> navigateToCampusAreaRoom(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.img_registrar2, () -> navigateToCampusAreaRoom(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.img_pin_registrar, () -> navigateToCampusAreaRoom(nav, CODE_REG, "REGISTRAR"));
        setClick(view, R.id.txt_registrar, () -> navigateToCampusAreaRoom(nav, CODE_REG, "REGISTRAR"));

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
            target.setOnClickListener(v -> {
                if (shouldBlockBrowsingForDirections()) {
                    return;
                }
                action.run();
            });
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
        if (isFloorTwoSelected()) {
            if (floorRooms.isEmpty()) {
                Log.w(TAG, "No Floor 2 rooms found for static slot mapping");
            }
            applyFloorTwoRoomsByCode(floorRooms);
            return;
        }
        if (isFloorThreeSelected()) {
            if (floorRooms.isEmpty()) {
                Log.w(TAG, "No Floor 3 rooms found for static slot mapping");
            }
            applyFloorThreeRoomsByCode(floorRooms);
            return;
        }
        if (isFloorFourSelected()) {
            if (floorRooms.isEmpty()) {
                Log.w(TAG, "No Floor 4 rooms found for static slot mapping");
            }
            applyFloorFourRoomsByCode(floorRooms);
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

        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void applyFloorFiveStaticLayout(@NonNull List<RoomEntity> floorRooms) {
        List<RoomEntity> remainingRooms = new ArrayList<>();
        for (RoomEntity room : floorRooms) {
            if (room != null) {
                remainingRooms.add(room);
            }
        }

        Map<Integer, RoomEntity> slotRooms = new LinkedHashMap<>();
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_left_1, "V48", "V-48");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_left_2, "V46", "V-46");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_left_3, "INDUSTRIAL", "ARTS");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_right_1, "SPEECH");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_right_2, "CRAFT", "SEWING");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_right_3, "OFFICE");
        assignFloorFiveSlot(slotRooms, remainingRooms, R.id.room_auditorium, "AUDITORIUM", "AUDIT");

        int[] fallbackSlots = {
                R.id.room_left_1,
                R.id.room_left_2,
                R.id.room_left_3,
                R.id.room_right_1,
                R.id.room_right_2,
                R.id.room_right_3
        };
        for (int slotId : fallbackSlots) {
            if (!slotRooms.containsKey(slotId) && !remainingRooms.isEmpty()) {
                slotRooms.put(slotId, remainingRooms.remove(0));
            }
        }
        if (!slotRooms.containsKey(R.id.room_auditorium) && !remainingRooms.isEmpty()) {
            slotRooms.put(R.id.room_auditorium, remainingRooms.remove(0));
        }

        bindFloorFiveSlot(R.id.room_left_1, "V-48", slotRooms.get(R.id.room_left_1));
        bindFloorFiveSlot(R.id.room_left_2, "V-46", slotRooms.get(R.id.room_left_2));
        bindFloorFiveSlot(R.id.room_left_3, "Industrial\nArts\nLaboratory", slotRooms.get(R.id.room_left_3));
        bindFloorFiveSlot(R.id.room_right_1, "Speech\nLaboratory", slotRooms.get(R.id.room_right_1));
        bindFloorFiveSlot(R.id.room_right_2, "Craft /\nSewing\nLaboratory", slotRooms.get(R.id.room_right_2));
        bindFloorFiveSlot(R.id.room_right_3, "Office", slotRooms.get(R.id.room_right_3));
        bindFloorFiveSlot(R.id.room_auditorium, "Auditorium", slotRooms.get(R.id.room_auditorium));
        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void assignFloorFiveSlot(@NonNull Map<Integer, RoomEntity> slotRooms,
                                     @NonNull List<RoomEntity> remainingRooms,
                                     int slotId,
                                     @NonNull String... keywords) {
        for (int i = 0; i < remainingRooms.size(); i++) {
            RoomEntity room = remainingRooms.get(i);
            if (!matchesFloorFiveRoom(room, keywords)) {
                continue;
            }
            slotRooms.put(slotId, room);
            remainingRooms.remove(i);
            return;
        }
    }

    private boolean matchesFloorFiveRoom(@Nullable RoomEntity room, @NonNull String... keywords) {
        if (room == null) {
            return false;
        }
        String haystack = normalizeSlotValue(room.name) + " "
                + normalizeSlotValue(room.code) + " "
                + normalizeSlotValue(room.type);
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeSlotValue(keyword);
            if (!normalizedKeyword.isEmpty() && haystack.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String normalizeSlotValue(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private void bindFloorFiveSlot(int roomViewId, @NonNull String fallbackLabel, @Nullable RoomEntity room) {
        if (floorMapContainer == null) {
            return;
        }
        View roomBox = floorMapContainer.findViewById(roomViewId);
        if (roomBox == null) {
            return;
        }

        if (room == null) {
            bindStaticRoomLabel(roomViewId, fallbackLabel);
            return;
        }

        bindRoomToBox(roomBox, room);
        if (pendingFocusedRoomId != null && pendingFocusedRoomId == room.id) {
            openRoomOnMap(room, roomBox, true);
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
        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void applyFloorTwoRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            if (room == null) {
                continue;
            }

            String code = normalizeCode(room.code);
            if (code == null) {
                continue;
            }

            if (!FLOOR2_ROOM_SLOTS.containsKey(code)) {
                Log.w(TAG, "Floor 2 room code is not mapped to a static slot: " + code);
                continue;
            }

            RoomEntity existing = roomByCode.putIfAbsent(code, room);
            if (existing != null) {
                Log.w(TAG, "Duplicate Floor 2 room code found, keeping first: " + code);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR2_ROOM_SLOTS.entrySet()) {
            View roomBox = floorMapContainer.findViewById(slot.getValue());
            if (roomBox == null) {
                Log.w(TAG, "Missing Floor 2 room view for slot code=" + slot.getKey());
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
        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void applyFloorThreeRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            if (room == null) {
                continue;
            }

            String code = normalizeCode(room.code);
            if (code == null) {
                continue;
            }

            if (!FLOOR3_ROOM_SLOTS.containsKey(code)) {
                Log.w(TAG, "Floor 3 room code is not mapped to a static slot: " + code);
                continue;
            }

            RoomEntity existing = roomByCode.putIfAbsent(code, room);
            if (existing != null) {
                Log.w(TAG, "Duplicate Floor 3 room code found, keeping first: " + code);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR3_ROOM_SLOTS.entrySet()) {
            View roomBox = floorMapContainer.findViewById(slot.getValue());
            if (roomBox == null) {
                Log.w(TAG, "Missing Floor 3 room view for slot code=" + slot.getKey());
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
        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void applyFloorFourRoomsByCode(@NonNull List<RoomEntity> floorRooms) {
        Map<String, RoomEntity> roomByCode = new HashMap<>();
        for (RoomEntity room : floorRooms) {
            if (room == null) {
                continue;
            }

            String code = normalizeCode(room.code);
            if (code == null) {
                continue;
            }

            if (!FLOOR4_ROOM_SLOTS.containsKey(code)) {
                Log.w(TAG, "Floor 4 room code is not mapped to a static slot: " + code);
                continue;
            }

            RoomEntity existing = roomByCode.putIfAbsent(code, room);
            if (existing != null) {
                Log.w(TAG, "Duplicate Floor 4 room code found, keeping first: " + code);
            }
        }

        for (Map.Entry<String, Integer> slot : FLOOR4_ROOM_SLOTS.entrySet()) {
            View roomBox = floorMapContainer.findViewById(slot.getValue());
            if (roomBox == null) {
                Log.w(TAG, "Missing Floor 4 room view for slot code=" + slot.getKey());
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
        floorMapContainer.post(this::renderActiveRouteOverlay);
    }

    private void bindRoomToBox(@NonNull View roomBox, @NonNull RoomEntity room) {
        TextView roomLabel = roomBox.findViewById(R.id.room_label);
        if (roomLabel != null) {
            roomLabel.setText(getRoomDisplayName(room));
        }
        currentRoomBoxIds.put(room.id, roomBox.getId());

        roomBox.setClickable(true);
        roomBox.setFocusable(false);
        roomBox.setOnTouchListener(null);
        roomBox.setOnClickListener(v -> {
            if (shouldBlockBrowsingForDirections()) {
                return;
            }
            Log.d(TAG, "Room clicked id=" + room.id + " code=" + room.code + " name=" + room.name);
            openRoomOnMap(room, roomBox, true);
        });
    }

    private void clearFloorRoomViews() {
        if (floorMapContainer == null) return;
        currentRoomBoxIds.clear();

        for (int roomBoxId : getActiveRoomBoxIds()) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);

            if (roomBox != null) {
                resetRoomBox(roomBox);
            }
        }
        highlightedRoomId = null;
        clearFloorRouteOverlay();
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
        return CODE_COURT.equals(c)
                || CODE_REG.equals(c)
                || CODE_LIB.equals(c)
                || CODE_ENT.equals(c)
                || CODE_EXIT.equals(c);
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

    private void observeDirectionsLaunchRequests() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.getBackStackEntry(navController.getGraph().getId())
                .getSavedStateHandle()
                .<Bundle>getLiveData(KEY_DIRECTIONS_LAUNCH_REQUEST)
                .observe(getViewLifecycleOwner(), request -> {
                    if (request == null || !isAdded()) {
                        return;
                    }

                    navController.getBackStackEntry(navController.getGraph().getId())
                            .getSavedStateHandle()
                            .remove(KEY_DIRECTIONS_LAUNCH_REQUEST);

                    int roomId = request.getInt(KEY_PENDING_ROOM_ID, -1);
                    String roomName = request.getString(KEY_PENDING_ROOM_NAME, "");
                    String buildingCode = request.getString(KEY_PENDING_BUILDING_CODE, "");
                    int floorNumber = request.getInt(KEY_PENDING_FLOOR_NUMBER, -1);

                    dismissDirectionsSheetIfPresent();
                    if (CODE_MAIN.equalsIgnoreCase(buildingCode) && floorNumber > 0) {
                        setFloorSelection(floorNumber);
                    } else {
                        setFloorSelection(null);
                    }
                    showDirectionsSheet(roomId, roomName);
                });
    }

    private void observeDirectionsRouteOverlay() {
        getParentFragmentManager().setFragmentResultListener(
                DirectionsSheetFragment.RESULT_ROUTE_MAP_OVERLAY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (!result.getBoolean(DirectionsSheetFragment.KEY_ROUTE_VISIBLE, false)) {
                        activeRouteDestinationRoomId = -1;
                        activeRouteOriginId = -1;
                        activeRouteOriginRoomId = -1;
                        clearActiveRouteOverlay();
                        return;
                    }

                    int destinationRoomId = result.getInt(DirectionsSheetFragment.KEY_ROUTE_ROOM_ID, -1);
                    if (destinationRoomId <= 0 || ioExecutor == null || ioExecutor.isShutdown()) {
                        clearActiveRouteOverlay();
                        return;
                    }

                    boolean useStairs = result.getBoolean(DirectionsSheetFragment.KEY_ROUTE_USE_STAIRS, false);
                    boolean useElevator = result.getBoolean(DirectionsSheetFragment.KEY_ROUTE_USE_ELEVATOR, false);
                    int originRoomId = result.getInt(DirectionsSheetFragment.KEY_ROUTE_ORIGIN_ROOM_ID, -1);
                    int originId = result.getInt(DirectionsSheetFragment.KEY_ROUTE_ORIGIN_ID, -1);
                    activeRouteDestinationRoomId = destinationRoomId;
                    activeRouteOriginId = originId;
                    activeRouteOriginRoomId = originRoomId;
                    ioExecutor.execute(() -> resolveAndApplyRouteOverlay(destinationRoomId, originRoomId, originId, useStairs, useElevator));
                });
    }

    private void observeDirectionsSheetVisibility() {
        getParentFragmentManager().setFragmentResultListener(
                DirectionsSheetFragment.RESULT_SHEET_VISIBILITY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (!isAdded()) {
                        return;
                    }
                    int sessionId = result.getInt(DirectionsSheetFragment.KEY_SHEET_SESSION_ID, 0);
                    if (sessionId != 0 && sessionId != activeDirectionsSheetSessionId) {
                        return;
                    }
                    boolean visible = result.getBoolean(DirectionsSheetFragment.KEY_SHEET_VISIBLE, false);
                    applyDirectionsSheetVisibility(visible);
                });
    }

    private void observeDirectionsFocusMode() {
        getParentFragmentManager().setFragmentResultListener(
                DirectionsSheetFragment.RESULT_NAVIGATION_FOCUS,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (!isAdded()) {
                        return;
                    }
                    setDirectionsFocusMode(result.getBoolean(
                            DirectionsSheetFragment.KEY_NAVIGATION_FOCUS_ACTIVE,
                            false
                    ));
                    updateFloorChipAvailability();
                });
    }

    private void setFloorSelection(@Nullable Integer floorNumber) {
        selectedFloor = floorNumber;
        if (floorChips != null) {
            for (int i = 0; i < floorChips.length; i++) {
                floorChips[i].setChecked(floorNumber != null && (i + 1) == floorNumber);
            }
        }
        updateFloorChipAvailability();
        if (floorNumber == null) {
            showOverallMap();
            updateRouteFloorControls();
            return;
        }
        showFloorMap(floorNumber);
        updateRouteFloorControls();
    }

    private void openRoomOnMap(@NonNull RoomEntity room, @NonNull View roomBox, boolean animateCentering) {
        dismissDirectionsSheetIfPresent();
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

        applyBottomSheetDepth(roomDetailSheet, 30);
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
            hideRoomDetailSheet();
            showDirectionsSheet(activeRoomId, tvRoomName.getText().toString());
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

    private void dismissDirectionsSheetIfPresent() {
        androidx.fragment.app.Fragment fragment = getParentFragmentManager().findFragmentByTag("directions_sheet");
        if (fragment instanceof DirectionsSheetFragment) {
            ((DirectionsSheetFragment) fragment).dismissAllowingStateLoss();
        }
    }

    private void showDirectionsSheet(int roomId, @Nullable String roomName) {
        int sheetSessionId = beginDirectionsSheetSession();
        dismissDirectionsSheetIfPresent();
        hasDirectionsPrompted = true;
        DirectionsSheetFragment sheet = new DirectionsSheetFragment();
        Bundle args = new Bundle();
        args.putInt(DirectionsSheetFragment.ARG_SHEET_SESSION_ID, sheetSessionId);
        if (roomId > 0) {
            args.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_ID, roomId);
            args.putString(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_NAME, roomName);
        }
        sheet.setArguments(args);
        sheet.show(getParentFragmentManager(), "directions_sheet");
    }

    private void showDirectionsSheetForActiveRoute() {
        int sheetSessionId = beginDirectionsSheetSession();
        dismissDirectionsSheetIfPresent();
        hasDirectionsPrompted = true;
        DirectionsSheetFragment sheet = new DirectionsSheetFragment();
        Bundle args = new Bundle();
        args.putInt(DirectionsSheetFragment.ARG_SHEET_SESSION_ID, sheetSessionId);
        args.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ROOM_ID, activeRouteDestinationRoomId);
        args.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ORIGIN_ID, activeRouteOriginId);
        args.putInt(DirectionsSheetFragment.ARG_PRESELECTED_ORIGIN_ROOM_ID, activeRouteOriginRoomId);
        args.putBoolean(DirectionsSheetFragment.ARG_AUTO_START_ROUTE, true);
        args.putBoolean(DirectionsSheetFragment.ARG_KEEP_OPEN_ON_START, true);
        sheet.setArguments(args);
        sheet.show(getParentFragmentManager(), "directions_sheet");
    }

    private void startEmergencyExitRoute() {
        dismissDirectionsSheetIfPresent();
        hasDirectionsPrompted = true;
        viewModel.findRoomIdByCode(CODE_EXIT, exitRoomId -> {
            if (!isAdded()) {
                return;
            }
            if (exitRoomId == null || exitRoomId <= 0) {
                Toast.makeText(requireContext(), "Emergency exit is not available yet", Toast.LENGTH_SHORT).show();
                return;
            }
            showDirectionsSheet(exitRoomId, "EXIT");
        });
    }

    private boolean isRoomDetailSheetShowing() {
        return roomDetailSheetBehavior != null
                && roomDetailSheet != null
                && roomDetailSheet.getVisibility() == View.VISIBLE
                && roomDetailSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
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
        if (fabCompass == null && fabEmergencyExit == null) {
            return;
        }

        if (fabCompass != null) {
            fabCompass.setOnClickListener(v -> {
                if (!isDirectionsSheetVisible) {
                    showDirectionsSheet(-1, null);
                }
            });

            fabCompass.setOnLongClickListener(v -> {
                setFloorSelection(null);
                return true;
            });
        }

        if (fabEmergencyExit != null) {
            fabEmergencyExit.setOnClickListener(v -> startEmergencyExitRoute());
        }

        setDirectionsFabVisible(true);
    }

    private void setupExitDirectionModeButton() {
        if (btnExitDirectionMode == null) {
            return;
        }
        btnExitDirectionMode.setOnClickListener(v -> {
            dismissDirectionsSheetIfPresent();
            clearActiveRouteOverlay();
        });
        updateExitDirectionModeButtonVisibility();
    }

    private void updateExitDirectionModeButtonVisibility() {
        if (btnExitDirectionMode == null) {
            return;
        }
        boolean shouldShow = hasDirectionsPrompted
                && !isRoomDetailSheetShowing()
                && !isDirectionsSheetVisible
                && activeRouteOverlay != null;
        btnExitDirectionMode.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void setupRouteFloorControls() {
        if (btnRoutePrevFloor != null) {
            btnRoutePrevFloor.setOnClickListener(v -> advanceRouteToPreviousFloor());
        }
        if (btnRouteNextFloor != null) {
            btnRouteNextFloor.setOnClickListener(v -> advanceRouteToNextFloor());
        }
        updateRouteFloorControls();
    }

    private void setDirectionsFabVisible(boolean visible) {
        if (fabCompass == null && fabEmergencyExit == null) {
            return;
        }
        if (fabCompass != null) {
            fabCompass.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (fabEmergencyExit != null) {
            fabEmergencyExit.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        updateExitDirectionModeButtonVisibility();
    }

    private int beginDirectionsSheetSession() {
        activeDirectionsSheetSessionId++;
        applyDirectionsSheetVisibility(true);
        return activeDirectionsSheetSessionId;
    }

    private void applyDirectionsSheetVisibility(boolean visible) {
        isDirectionsSheetVisible = visible;
        if (!isAdded()) {
            return;
        }
        boolean showBaseChrome = !visible && !isRoomDetailSheetShowing();
        setBottomNavVisible(showBaseChrome);
        setDirectionsFabVisible(showBaseChrome);
        updateExitDirectionModeButtonVisibility();
        updateRouteFloorControls();
    }

    private void setDirectionsFocusMode(boolean focused) {
        isDirectionsFocusMode = focused;
        setHeaderViewVisibility(homeLogoView, !focused);
        setHeaderViewVisibility(homeSearchView, !focused);
    }

    private void setHeaderViewVisibility(@Nullable View target, boolean visible) {
        if (target == null) {
            return;
        }

        target.animate().cancel();
        if (visible) {
            target.setEnabled(true);
            target.setClickable(target.hasOnClickListeners());
            if (target.getVisibility() != View.VISIBLE) {
                target.setAlpha(0f);
                target.setTranslationY(-dpToPx(10));
                target.setVisibility(View.VISIBLE);
            }
            target.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180L)
                    .start();
            return;
        }

        target.setEnabled(false);
        target.setClickable(false);
        if (target.getVisibility() != View.VISIBLE) {
            return;
        }
        target.animate()
                .alpha(0f)
                .translationY(-dpToPx(10))
                .setDuration(140L)
                .withEndAction(() -> {
                    target.setVisibility(View.GONE);
                    target.setAlpha(1f);
                    target.setTranslationY(0f);
                })
                .start();
    }

    private boolean shouldBlockBrowsingForDirections() {
        return isDirectionsFocusMode;
    }

    private void applyBottomSheetDepth(@Nullable View sheet, int elevationDp) {
        if (sheet == null) {
            return;
        }

        float elevationPx = dpToPx(elevationDp);
        sheet.setElevation(elevationPx);
        sheet.setTranslationZ(elevationPx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sheet.setOutlineAmbientShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_ambient));
            sheet.setOutlineSpotShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_spot));
        }
    }

    private void updateFloorChipAvailability() {
        if (floorChips == null) {
            return;
        }

        for (int i = 0; i < floorChips.length; i++) {
            Chip chip = floorChips[i];
            int floorNumber = i + 1;
            boolean enabled = activeRouteOverlay == null || isFloorWithinActiveRoute(floorNumber);
            chip.setEnabled(enabled);
            chip.setClickable(enabled);
            chip.setAlpha(enabled ? 1f : 0.42f);
        }
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

    private void resolveAndApplyRouteOverlay(int destinationRoomId,
                                             int originRoomId,
                                             int originId,
                                             boolean useStairs,
                                             boolean useElevator) {
        AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        RoomEntity destinationRoom = db.roomDao().getByIdSync(destinationRoomId);
        if (destinationRoom == null) {
            runOnUiThreadSafely(this::clearActiveRouteOverlay);
            return;
        }

        FloorEntity floor = db.floorDao().getByIdSync(destinationRoom.floorId);
        BuildingEntity building = floor != null ? db.buildingDao().getByIdSync(floor.buildingId) : null;
        if (floor == null || building == null || building.code == null) {
            runOnUiThreadSafely(this::clearActiveRouteOverlay);
            return;
        }

        String destinationBuildingCode = normalizeCode(building.code);
        int destinationOverallType = resolveOverallDestinationType(destinationRoom, destinationBuildingCode);
        boolean destinationIsMainInterior = CODE_MAIN.equals(destinationBuildingCode)
                && !isCampusAreaCode(destinationRoom.code);

        int resolvedOriginRoomId = -1;
        int originFloorNumber = destinationIsMainInterior ? floor.number : 1;
        boolean preferGroundStart = false;
        int overallOriginType = OVERALL_ANCHOR_ENTRANCE;
        boolean originIsMainInteriorRoom = false;
        if (originRoomId > 0) {
            RoomEntity originRoom = db.roomDao().getByIdSync(originRoomId);
            if (originRoom != null) {
                FloorEntity originFloor = db.floorDao().getByIdSync(originRoom.floorId);
                BuildingEntity originBuilding = originFloor != null
                        ? db.buildingDao().getByIdSync(originFloor.buildingId)
                        : null;
                String originBuildingCode = originBuilding != null ? normalizeCode(originBuilding.code) : null;
                overallOriginType = resolveOverallOriginType(originRoom, originBuildingCode);
                if (originFloor != null
                        && CODE_MAIN.equals(originBuildingCode)
                        && !isCampusAreaCode(originRoom.code)) {
                    resolvedOriginRoomId = originRoom.id;
                    originFloorNumber = originFloor.number;
                    originIsMainInteriorRoom = true;
                }
            }
        } else if (originId > 0) {
            OriginEntity origin = db.originDao().getByIdSync(originId);
            preferGroundStart = isGroundEntryOrigin(origin);
            overallOriginType = resolveOverallOriginType(origin);
            int inferredOriginFloor = inferOriginFloorNumber(origin);
            if (inferredOriginFloor > 0) {
                originFloorNumber = inferredOriginFloor;
            } else {
                // Most admin-defined origins such as gate/entrance start from the ground floor.
                originFloorNumber = 1;
            }
        }

        if (overallOriginType == OVERALL_ANCHOR_EXIT && destinationOverallType == OVERALL_ANCHOR_COURT) {
            runOnUiThreadSafely(() -> {
                clearActiveRouteOverlay();
                Toast.makeText(requireContext(), "Route from Main Exit to Court is not allowed", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        boolean campusTransitionSequence = originIsMainInteriorRoom
                && destinationOverallType != OVERALL_ANCHOR_MAIN;
        boolean routeViaMainBeforeCourt = overallOriginType == OVERALL_ANCHOR_ENTRANCE
                && (destinationOverallType == OVERALL_ANCHOR_COURT
                || destinationOverallType == OVERALL_ANCHOR_REGISTRAR
                || destinationOverallType == OVERALL_ANCHOR_LIBRARY);
        boolean forceAuditoriumBottomStair = destinationIsMainInterior
                && floor.number == 5
                && isAuditoriumRoom(destinationRoom);

        RouteOverlayState state = new RouteOverlayState(
                destinationRoom.id,
                originFloorNumber,
                destinationIsMainInterior ? floor.number : 1,
                resolvedOriginRoomId,
                useElevator ? ROUTE_ANCHOR_ELEVATOR : ROUTE_ANCHOR_AUTO,
                useStairs,
                useElevator,
                preferGroundStart,
                overallOriginType,
                destinationOverallType,
                !destinationIsMainInterior && !campusTransitionSequence,
                routeViaMainBeforeCourt,
                campusTransitionSequence,
                forceAuditoriumBottomStair
        );
        runOnUiThreadSafely(() -> {
            activeRouteOverlay = state;
            updateFloorChipAvailability();
            if (state.overallOnly) {
                setFloorSelection(null);
            } else if (selectedFloor == null || selectedFloor != state.currentFloorNumber) {
                setFloorSelection(state.currentFloorNumber);
            } else {
                renderActiveRouteOverlay();
            }
            updateRouteFloorControls();
            updateExitDirectionModeButtonVisibility();
        });
    }

    private void renderActiveRouteOverlay() {
        if (!isAdded() || activeRouteOverlay == null) {
            return;
        }

        if (selectedFloor == null) {
            clearFloorRouteOverlay();
            if (activeRouteOverlay.campusTransitionSequence && !activeRouteOverlay.showingCampusLeg) {
                activeRouteOverlay.showingCampusLeg = true;
            }
            renderOverallRouteOverlay(activeRouteOverlay);
            return;
        }

        if (activeRouteOverlay.overallOnly) {
            clearFloorRouteOverlay();
            clearOverallRouteOverlay();
            return;
        }

        if (activeRouteOverlay.campusTransitionSequence && selectedFloor != null && selectedFloor > 1) {
            clearOverallRouteOverlay();
            if (activeRouteOverlay.isMultiFloor()) {
                renderMultiFloorRouteOverlay(activeRouteOverlay);
            } else {
                clearFloorRouteOverlay();
            }
            return;
        }

        if (selectedFloor != activeRouteOverlay.currentFloorNumber) {
            clearFloorRouteOverlay();
            clearOverallRouteOverlay();
            return;
        }

        clearOverallRouteOverlay();

        if (highlightedRoomId != null) {
            clearRoomHighlight();
        }

        ensureFloorRouteOverlayAttached();
        if (floorRouteOverlay == null) {
            return;
        }

        if (activeRouteOverlay.campusTransitionSequence && !activeRouteOverlay.showingCampusLeg) {
            renderCampusTransitionFloorRoute(activeRouteOverlay);
            return;
        }

        if (activeRouteOverlay.isMultiFloor()) {
            renderMultiFloorRouteOverlay(activeRouteOverlay);
            return;
        }

        Integer roomBoxId = currentRoomBoxIds.get(activeRouteOverlay.destinationRoomId);
        if (roomBoxId == null) {
            floorRouteOverlay.clearRoute();
            return;
        }

        View destinationView = floorMapContainer.findViewById(roomBoxId);
        if (destinationView == null) {
            floorRouteOverlay.clearRoute();
            return;
        }

        PointF endPoint = createRoomAnchor(destinationView);
        PointF startPoint = resolveRouteStartAnchor(destinationView, endPoint, activeRouteOverlay);
        clearRouteAnchorInteractions();
        floorRouteOverlay.setRoute(startPoint, endPoint, resolveHallwayX(), null);
        centerRoomInFloorMap(destinationView);
    }

    private void renderCampusTransitionFloorRoute(@NonNull RouteOverlayState state) {
        if (state.originFloorNumber > 1 && floorMapContainer != null) {
            View stairsTop = floorMapContainer.findViewById(R.id.stairs_top);
            if (stairsTop != null) {
                PointF startPoint = createAnchorAtCenter(stairsTop);
                PointF endPoint = new PointF(resolveHallwayX(), startPoint.y);
                clearRouteAnchorInteractions();
                floorRouteOverlay.setRouteWithWaypoints(startPoint, endPoint, null, endPoint);
                centerRoomInFloorMap(stairsTop);
                return;
            }
        }

        PointF startPoint = createCampusTransitionFloorStartPoint(state);
        PointF endPoint = createFloorHallwayExitPoint();
        clearRouteAnchorInteractions();
        floorRouteOverlay.setRoute(startPoint, endPoint, resolveHallwayX(), endPoint);
        centerCampusTransitionFocus(state);
    }

    @NonNull
    private PointF createFloorHallwayExitPoint() {
        return new PointF(resolveHallwayX(), dpToPx(120));
    }

    @NonNull
    private PointF createCampusTransitionFloorStartPoint(@NonNull RouteOverlayState state) {
        if (state.originFloorNumber == 1 && state.originRoomId > 0) {
            View originView = findRoomViewByRoomId(state.originRoomId);
            if (originView != null) {
                return createRoomAnchor(originView);
            }
        }
        if (floorMapContainer != null) {
            View stairsTop = floorMapContainer.findViewById(R.id.stairs_top);
            if (stairsTop != null) {
                return createAnchorAtCenter(stairsTop);
            }
        }
        return new PointF(dpToPx(72), dpToPx(120));
    }

    private void centerCampusTransitionFocus(@NonNull RouteOverlayState state) {
        if (state.originFloorNumber == 1 && state.originRoomId > 0) {
            View originView = findRoomViewByRoomId(state.originRoomId);
            if (originView != null) {
                centerRoomInFloorMap(originView);
                return;
            }
        }
        if (floorMapContainer != null) {
            View stairsTop = floorMapContainer.findViewById(R.id.stairs_top);
            if (stairsTop != null) {
                centerRoomInFloorMap(stairsTop);
            }
        }
    }

    private void renderMultiFloorRouteOverlay(@NonNull RouteOverlayState state) {
        if (state.anchorType == ROUTE_ANCHOR_AUTO) {
            state.anchorType = resolveAutoAnchorType(state);
        }

        View routeAnchorView = findRouteAnchorView(state);
        if (routeAnchorView == null) {
            floorRouteOverlay.clearRoute();
            return;
        }

        clearRouteAnchorInteractions();
        configureRouteAnchorInteraction(routeAnchorView, state);

        PointF startPoint;
        PointF endPoint;
        View focalView = routeAnchorView;

        if (state.currentFloorNumber == state.originFloorNumber && state.originRoomId > 0) {
            View originView = findRoomViewByRoomId(state.originRoomId);
            if (originView == null) {
                floorRouteOverlay.clearRoute();
                return;
            }
            startPoint = createRoomAnchor(originView);
            endPoint = createRouteAnchor(routeAnchorView, state);
            focalView = originView;
        } else if (state.currentFloorNumber == state.originFloorNumber) {
            startPoint = createOriginFloorStartPoint(routeAnchorView, state);
            endPoint = createRouteAnchor(routeAnchorView, state);
        } else if (state.currentFloorNumber == state.destinationFloorNumber) {
            View destinationView = findRoomViewByRoomId(state.destinationRoomId);
            if (destinationView == null) {
                floorRouteOverlay.clearRoute();
                return;
            }
            startPoint = createRouteAnchor(routeAnchorView, state);
            endPoint = createRoomAnchor(destinationView);
            focalView = destinationView;
        } else {
            endPoint = createRouteAnchor(routeAnchorView, state);
            startPoint = createIntermediateFloorStartPoint(routeAnchorView);
        }

        PointF interactiveAnchor = state.currentFloorNumber != state.destinationFloorNumber ? endPoint : null;
        floorRouteOverlay.setRoute(startPoint, endPoint, resolveHallwayX(), interactiveAnchor);
        centerRoomInFloorMap(focalView);
    }

    @NonNull
    private PointF resolveRouteStartAnchor(@NonNull View destinationView,
                                           @NonNull PointF endPoint,
                                           @NonNull RouteOverlayState state) {
        if (state.originRoomId > 0) {
            Integer originRoomBoxId = currentRoomBoxIds.get(state.originRoomId);
            if (originRoomBoxId != null) {
                View originView = floorMapContainer.findViewById(originRoomBoxId);
                if (originView != null) {
                    return createRoomAnchor(originView);
                }
            }
        }

        if (state.preferGroundStart && !state.isMultiFloor() && state.currentFloorNumber == 1) {
            ConstraintLayout mapContent = findCurrentMapContent();
            if (mapContent != null && mapContent.getHeight() > 0) {
                return new PointF(resolveHallwayX(), mapContent.getHeight() - dpToPx(64));
            }
        }

        if (state.useElevator) {
            View elevator = floorMapContainer.findViewById(R.id.elevator);
            if (elevator != null) {
                return createAnchorAtTopCenter(elevator);
            }
        }

        if (state.useStairs || state.currentFloorNumber > 1) {
            View topStairs = floorMapContainer.findViewById(R.id.stairs_top);
            View bottomStairs = floorMapContainer.findViewById(R.id.stairs_bottom);
            if (state.forceAuditoriumBottomStair
                    && state.currentFloorNumber == state.destinationFloorNumber) {
                if (bottomStairs != null) {
                    return createAnchorAtCenter(bottomStairs);
                }
                if (topStairs != null) {
                    return createAnchorAtCenter(topStairs);
                }
            }
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

    @Nullable
    private View findRoomViewByRoomId(int roomId) {
        Integer roomBoxId = currentRoomBoxIds.get(roomId);
        return roomBoxId != null && floorMapContainer != null
                ? floorMapContainer.findViewById(roomBoxId)
                : null;
    }

    @Nullable
    private View findRouteAnchorView(@NonNull RouteOverlayState state) {
        if (floorMapContainer == null) {
            return null;
        }
        int anchorViewId = resolveAnchorViewId(state.anchorType);
        return anchorViewId != View.NO_ID ? floorMapContainer.findViewById(anchorViewId) : null;
    }

    private int resolveAutoAnchorType(@NonNull RouteOverlayState state) {
        if (state.useElevator) {
            return ROUTE_ANCHOR_ELEVATOR;
        }
        if (state.forceAuditoriumBottomStair) {
            return ROUTE_ANCHOR_STAIRS_BOTTOM;
        }
        View referenceView = null;
        if (state.currentFloorNumber == state.originFloorNumber && state.originRoomId > 0) {
            referenceView = findRoomViewByRoomId(state.originRoomId);
        } else if (state.currentFloorNumber == state.destinationFloorNumber) {
            referenceView = findRoomViewByRoomId(state.destinationRoomId);
        }
        if (referenceView == null) {
            return ROUTE_ANCHOR_STAIRS_TOP;
        }

        View topStairs = floorMapContainer.findViewById(R.id.stairs_top);
        View bottomStairs = floorMapContainer.findViewById(R.id.stairs_bottom);
        View chosen = pickNearestAnchor(referenceView, topStairs, bottomStairs);
        if (chosen == null) {
            return ROUTE_ANCHOR_STAIRS_TOP;
        }
        return chosen.getId() == R.id.stairs_bottom ? ROUTE_ANCHOR_STAIRS_BOTTOM : ROUTE_ANCHOR_STAIRS_TOP;
    }

    private int resolveAnchorViewId(int anchorType) {
        switch (anchorType) {
            case ROUTE_ANCHOR_STAIRS_TOP:
                return R.id.stairs_top;
            case ROUTE_ANCHOR_STAIRS_BOTTOM:
                return R.id.stairs_bottom;
            case ROUTE_ANCHOR_ELEVATOR:
                return R.id.elevator;
            default:
                return View.NO_ID;
        }
    }

    @NonNull
    private PointF createRouteAnchor(@NonNull View anchorView, @NonNull RouteOverlayState state) {
        return state.anchorType == ROUTE_ANCHOR_ELEVATOR
                ? createAnchorAtTopCenter(anchorView)
                : createAnchorAtCenter(anchorView);
    }

    @NonNull
    private PointF createIntermediateFloorStartPoint(@NonNull View anchorView) {
        return new PointF(resolveHallwayX(), anchorView.getY() + (anchorView.getHeight() / 2f));
    }

    @NonNull
    private PointF createOriginFloorStartPoint(@NonNull View anchorView, @NonNull RouteOverlayState state) {
        ConstraintLayout mapContent = findCurrentMapContent();
        if (mapContent == null || mapContent.getHeight() <= 0) {
            return createIntermediateFloorStartPoint(anchorView);
        }

        float hallwayX = resolveHallwayX();
        float topInset = dpToPx(72);
        float bottomInset = dpToPx(56);
        boolean movingUp = state.destinationFloorNumber > state.originFloorNumber;
        float y = movingUp
                ? mapContent.getHeight() - bottomInset
                : topInset;
        return new PointF(hallwayX, Math.max(topInset, Math.min(y, mapContent.getHeight() - bottomInset)));
    }

    private int inferOriginFloorNumber(@Nullable OriginEntity origin) {
        if (origin == null) {
            return -1;
        }

        int explicitFloor = findFloorNumber(origin.name, origin.code, origin.description);
        if (explicitFloor > 0) {
            return explicitFloor;
        }

        String combined = normalizeText(origin.name) + " "
                + normalizeText(origin.code) + " "
                + normalizeText(origin.description);
        if (combined.contains("gate")
                || combined.contains("entrance")
                || combined.contains("lobby")
                || combined.contains("ground")) {
            return 1;
        }
        return -1;
    }

    private boolean isGroundEntryOrigin(@Nullable OriginEntity origin) {
        if (origin == null) {
            return false;
        }
        String combined = normalizeText(origin.name) + " "
                + normalizeText(origin.code) + " "
                + normalizeText(origin.description);
        return combined.contains("gate")
                || combined.contains("entrance")
                || combined.contains("exit");
    }

    private int findFloorNumber(@Nullable String... values) {
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
    private String normalizeText(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean isAuditoriumRoom(@Nullable RoomEntity room) {
        if (room == null) {
            return false;
        }
        String normalizedName = normalizeSlotValue(room.name);
        String normalizedCode = normalizeSlotValue(room.code);
        return normalizedName.contains("AUDITORIUM")
                || normalizedCode.contains("AUDIT")
                || normalizedCode.contains("AUDITORIUM");
    }

    private void configureRouteAnchorInteraction(@NonNull View anchorView, @NonNull RouteOverlayState state) {
        boolean shouldAdvance = state.isMultiFloor() && state.currentFloorNumber != state.destinationFloorNumber;
        anchorView.setClickable(shouldAdvance);
        anchorView.setFocusable(shouldAdvance);
        anchorView.setOnClickListener(shouldAdvance ? v -> advanceRouteToNextFloor() : null);
        if (shouldAdvance) {
            startRouteAnchorCue(anchorView);
        }
    }

    private void clearRouteAnchorInteractions() {
        clearActiveRouteAnchorCue();
        if (floorMapContainer == null) {
            return;
        }
        int[] anchorIds = {R.id.stairs_top, R.id.stairs_bottom, R.id.elevator};
        for (int anchorId : anchorIds) {
            View anchorView = floorMapContainer.findViewById(anchorId);
            if (anchorView == null) {
                continue;
            }
            anchorView.setOnClickListener(null);
            anchorView.setClickable(false);
            anchorView.setFocusable(false);
            anchorView.setScaleX(1f);
            anchorView.setScaleY(1f);
            anchorView.setAlpha(1f);
            anchorView.setTranslationZ(0f);
        }
    }

    private void startRouteAnchorCue(@NonNull View anchorView) {
        clearActiveRouteAnchorCue();
        activeRouteAnchorView = anchorView;
        anchorView.setTranslationZ(dpToPx(16));
        anchorView.setAlpha(1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                anchorView,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.84f, 1f)
        );
        animator.setDuration(900L);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        activeRouteAnchorAnimator = animator;
        animator.start();
    }

    private void clearActiveRouteAnchorCue() {
        if (activeRouteAnchorAnimator != null) {
            activeRouteAnchorAnimator.cancel();
            activeRouteAnchorAnimator = null;
        }
        if (activeRouteAnchorView != null) {
            activeRouteAnchorView.setScaleX(1f);
            activeRouteAnchorView.setScaleY(1f);
            activeRouteAnchorView.setAlpha(1f);
            activeRouteAnchorView.setTranslationZ(0f);
            activeRouteAnchorView = null;
        }
    }

    private void advanceRouteToNextFloor() {
        if (activeRouteOverlay == null) {
            return;
        }
        if (activeRouteOverlay.campusTransitionSequence) {
            if (activeRouteOverlay.currentFloorNumber > 1) {
                activeRouteOverlay.currentFloorNumber -= 1;
                setFloorSelection(activeRouteOverlay.currentFloorNumber);
            } else if (!activeRouteOverlay.showingCampusLeg) {
                activeRouteOverlay.showingCampusLeg = true;
                setFloorSelection(null);
            }
            updateRouteFloorControls();
            return;
        }
        if (!activeRouteOverlay.isMultiFloor()) {
            return;
        }
        if (activeRouteOverlay.currentFloorNumber == activeRouteOverlay.destinationFloorNumber) {
            updateRouteFloorControls();
            return;
        }

        int direction = activeRouteOverlay.destinationFloorNumber > activeRouteOverlay.currentFloorNumber ? 1 : -1;
        activeRouteOverlay.currentFloorNumber += direction;
        setFloorSelection(activeRouteOverlay.currentFloorNumber);
        updateRouteFloorControls();
    }

    private void advanceRouteToPreviousFloor() {
        if (activeRouteOverlay == null) {
            return;
        }
        if (activeRouteOverlay.campusTransitionSequence) {
            if (activeRouteOverlay.showingCampusLeg) {
                activeRouteOverlay.showingCampusLeg = false;
                activeRouteOverlay.currentFloorNumber = 1;
                setFloorSelection(1);
            } else if (activeRouteOverlay.currentFloorNumber < activeRouteOverlay.originFloorNumber) {
                activeRouteOverlay.currentFloorNumber += 1;
                setFloorSelection(activeRouteOverlay.currentFloorNumber);
            }
            updateRouteFloorControls();
            return;
        }
        if (!activeRouteOverlay.isMultiFloor()) {
            return;
        }
        if (activeRouteOverlay.currentFloorNumber == activeRouteOverlay.originFloorNumber) {
            updateRouteFloorControls();
            return;
        }

        int direction = activeRouteOverlay.destinationFloorNumber > activeRouteOverlay.originFloorNumber ? -1 : 1;
        activeRouteOverlay.currentFloorNumber += direction;
        setFloorSelection(activeRouteOverlay.currentFloorNumber);
        updateRouteFloorControls();
    }

    private void updateRouteFloorControls() {
        if (routeFloorControls == null || btnRoutePrevFloor == null || btnRouteNextFloor == null) {
            return;
        }

        RouteOverlayState state = activeRouteOverlay;
        boolean visible = state != null
                && isDirectionsSheetVisible
                && (
                (state.campusTransitionSequence)
                        || (state.isMultiFloor() && selectedFloor != null)
        );
        routeFloorControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        routeFloorControls.bringToFront();

        if (state.campusTransitionSequence) {
            boolean canGoPrev = state.showingCampusLeg
                    || state.currentFloorNumber != state.originFloorNumber;
            boolean canGoNext = !state.showingCampusLeg;
            setRouteFloorButtonState(btnRoutePrevFloor, canGoPrev, "Previous");
            setRouteFloorButtonState(btnRouteNextFloor, canGoNext, "Next");
            return;
        }

        boolean canGoPrev = state.currentFloorNumber != state.originFloorNumber;
        boolean canGoNext = state.currentFloorNumber != state.destinationFloorNumber;

        setRouteFloorButtonState(btnRoutePrevFloor, canGoPrev, "Previous");
        setRouteFloorButtonState(btnRouteNextFloor, canGoNext, "Next");
    }

    private void setRouteFloorButtonState(@NonNull MaterialButton button,
                                          boolean visible,
                                          @NonNull String label) {
        button.setText(label);
        button.setEnabled(visible);
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isFloorWithinActiveRoute(int floorNumber) {
        if (activeRouteOverlay == null) {
            return true;
        }
        int minFloor = Math.min(activeRouteOverlay.originFloorNumber, activeRouteOverlay.destinationFloorNumber);
        int maxFloor = Math.max(activeRouteOverlay.originFloorNumber, activeRouteOverlay.destinationFloorNumber);
        return floorNumber >= minFloor && floorNumber <= maxFloor;
    }

    private float resolveHallwayX() {
        ConstraintLayout mapContent = findCurrentMapContent();
        if (mapContent == null || mapContent.getWidth() <= 0) {
            return dpToPx(164);
        }
        return mapContent.getWidth() / 2f;
    }

    private void ensureFloorRouteOverlayAttached() {
        if (floorMapContainer == null || floorMapContainer.getChildCount() == 0) {
            return;
        }

        ConstraintLayout mapContent = findCurrentMapContent();
        if (mapContent == null) {
            floorRouteOverlay = null;
            return;
        }

        if (floorRouteOverlay != null && floorRouteOverlay.getParent() == mapContent) {
            configureRouteOverlayTouchPassthrough(floorRouteOverlay);
            floorRouteOverlay.bringToFront();
            return;
        }

        floorRouteOverlay = new RoutePathOverlayView(requireContext());
        configureRouteOverlayTouchPassthrough(floorRouteOverlay);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        mapContent.addView(floorRouteOverlay, params);
        floorRouteOverlay.bringToFront();
    }

    private void configureRouteOverlayTouchPassthrough(@NonNull RoutePathOverlayView overlay) {
        overlay.setClickable(false);
        overlay.setFocusable(false);
        overlay.setLongClickable(false);
        overlay.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }

    @Nullable
    private ConstraintLayout findCurrentMapContent() {
        if (floorMapContainer == null || floorMapContainer.getChildCount() == 0) {
            return null;
        }
        View root = floorMapContainer.getChildAt(0);
        ScrollView scrollView = findFirstScrollView(root);
        return scrollView != null ? findConstraintContent(scrollView) : null;
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

    @Nullable
    private View findMirroredRoom(@NonNull View destinationView) {
        ConstraintLayout mapContent = findCurrentMapContent();
        if (mapContent == null) {
            return null;
        }

        float destinationCenterX = destinationView.getX() + (destinationView.getWidth() / 2f);
        float destinationCenterY = destinationView.getY() + (destinationView.getHeight() / 2f);
        float hallwayX = mapContent.getWidth() / 2f;
        boolean destinationOnLeft = destinationCenterX < hallwayX;

        View bestMatch = null;
        float bestDistance = Float.MAX_VALUE;
        for (int roomBoxId : getActiveRoomBoxIds()) {
            View roomBox = floorMapContainer.findViewById(roomBoxId);
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

    @NonNull
    private PointF createRoomAnchor(@NonNull View roomView) {
        if (isAuditoriumRoomView(roomView)) {
            return createAuditoriumEntranceAnchor(roomView);
        }
        return new PointF(
                roomView.getX() + (roomView.getWidth() / 2f),
                roomView.getY() + dpToPx(6)
        );
    }

    private boolean isAuditoriumRoomView(@NonNull View roomView) {
        return roomView.getId() == R.id.room_auditorium;
    }

    @NonNull
    private PointF createAuditoriumEntranceAnchor(@NonNull View roomView) {
        // Floor 5 auditorium entrance is along the lower-left edge.
        float anchorX = roomView.getX() + (roomView.getWidth() * 0.42f);
        float anchorY = roomView.getY() + roomView.getHeight() - dpToPx(6);
        return new PointF(anchorX, anchorY);
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

    private void clearFloorRouteOverlay() {
        if (floorRouteOverlay != null) {
            floorRouteOverlay.clearRoute();
        }
        clearRouteAnchorInteractions();
    }

    private void clearOverallRouteOverlay() {
        if (overallRouteOverlay != null) {
            overallRouteOverlay.clearRoute();
        }
    }

    private void renderOverallRouteOverlay(@NonNull RouteOverlayState state) {
        if (!(overallMapContainer instanceof ConstraintLayout)) {
            return;
        }
        ConstraintLayout overallMap = (ConstraintLayout) overallMapContainer;
        if (overallMap.getWidth() <= 0 || overallMap.getHeight() <= 0) {
            overallMap.post(() -> {
                if (isAdded() && activeRouteOverlay == state && selectedFloor == null) {
                    renderOverallRouteOverlay(state);
                }
            });
            return;
        }

        ensureOverallRouteOverlayAttached(overallMap);
        if (overallRouteOverlay == null) {
            return;
        }

        View destinationView = resolveOverallAnchorView(overallMap, state.overallDestinationType, true);
        View originView = resolveOverallAnchorView(overallMap, state.overallOriginType, false);
        if (destinationView == null || originView == null) {
            overallRouteOverlay.clearRoute();
            return;
        }

        PointF startPoint = createOverallAnchorPoint(originView, state.overallOriginType, false);
        PointF endPoint = createOverallAnchorPoint(destinationView, state.overallDestinationType, true);
        if (state.campusTransitionSequence && state.showingCampusLeg) {
            startPoint = createCampusOverallStartPoint(overallMap);
        }
        if (state.routeViaMainBeforeCourt) {
            if (state.overallDestinationType == OVERALL_ANCHOR_REGISTRAR) {
                endPoint = createEntranceToRegistrarEndPoint(destinationView, overallMap);
            } else if (state.overallDestinationType == OVERALL_ANCHOR_LIBRARY) {
                endPoint = createEntranceToLibraryEndPoint(destinationView, overallMap);
            } else {
                endPoint = createEntranceToCourtEndPoint(destinationView, overallMap);
            }
        }
        if (Math.abs(startPoint.x - endPoint.x) < dpToPx(8) && Math.abs(startPoint.y - endPoint.y) < dpToPx(8)) {
            View fallback = overallMap.findViewById(R.id.img_entrance);
            if (fallback != null) {
                startPoint = createAnchorAtCenter(fallback);
            }
        }

        if (state.routeViaMainBeforeCourt) {
            List<PointF> waypoints;
            if (state.overallDestinationType == OVERALL_ANCHOR_LIBRARY) {
                waypoints = buildEntranceToLibraryWaypoints(startPoint, endPoint, overallMap);
            } else {
                waypoints = buildEntranceToCourtWaypoints(startPoint, endPoint, overallMap);
            }
            overallRouteOverlay.setRouteWithWaypoints(startPoint, endPoint, waypoints, null);
            return;
        }
        if (state.campusTransitionSequence
                && state.showingCampusLeg
                && state.overallDestinationType == OVERALL_ANCHOR_EXIT) {
            List<PointF> waypoints = buildMainToExitWaypoints(startPoint, endPoint, overallMap);
            overallRouteOverlay.setRouteWithWaypoints(startPoint, endPoint, waypoints, null);
            return;
        }

        float bendX = resolveOverallRouteBendX(state, overallMap);
        overallRouteOverlay.setRoute(startPoint, endPoint, bendX, null);
    }

    @NonNull
    private PointF createCampusOverallStartPoint(@NonNull ConstraintLayout overallMap) {
        View mainBuilding = overallMap.findViewById(R.id.img_main_building);
        if (mainBuilding != null) {
            return new PointF(
                    mainBuilding.getX() + (mainBuilding.getWidth() * 0.45f),
                    mainBuilding.getY() + dpToPx(26)
            );
        }
        return new PointF(overallMap.getWidth() * 0.42f, dpToPx(140));
    }

    @NonNull
    private List<PointF> buildEntranceToCourtWaypoints(@NonNull PointF startPoint,
                                                       @NonNull PointF endPoint,
                                                       @NonNull ConstraintLayout overallMap) {
        List<PointF> waypoints = new ArrayList<>();

        View mainBuilding = overallMap.findViewById(R.id.img_main_building);
        float hallwayX = startPoint.x;
        float hallwayEntryY = startPoint.y;
        float hallwayExitY = endPoint.y;

        if (mainBuilding != null) {
            // Keep the vertical leg inside the main-building hallway before crossing to court.
            hallwayX = mainBuilding.getX() + (mainBuilding.getWidth() * 0.45f);
            // Drop this waypoint lower so the visible vertical segment is longer.
            hallwayEntryY = mainBuilding.getY() + mainBuilding.getHeight() + dpToPx(18);
        }

        float minY = dpToPx(40);
        float maxY = Math.max(minY, overallMap.getHeight() - dpToPx(40));
        hallwayEntryY = Math.max(minY, Math.min(hallwayEntryY, maxY));
        hallwayExitY = Math.max(minY, Math.min(hallwayExitY, maxY));

        addWaypointIfFar(waypoints, hallwayX, startPoint.y);
        addWaypointIfFar(waypoints, hallwayX, hallwayEntryY);
        addWaypointIfFar(waypoints, hallwayX, hallwayExitY);
        addWaypointIfFar(waypoints, endPoint.x, hallwayExitY);
        return waypoints;
    }

    @NonNull
    private List<PointF> buildMainToExitWaypoints(@NonNull PointF startPoint,
                                                  @NonNull PointF endPoint,
                                                  @NonNull ConstraintLayout overallMap) {
        List<PointF> waypoints = new ArrayList<>();
        float rightLaneX = overallMap.getWidth() - dpToPx(48);
        View court = overallMap.findViewById(R.id.img_court);
        if (court != null) {
            rightLaneX = court.getX() + (court.getWidth() * 0.62f);
        }

        float downY = endPoint.y;
        View exit = overallMap.findViewById(R.id.img_exit);
        if (exit != null) {
            downY = exit.getY() + (exit.getHeight() * 0.68f);
        }

        addWaypointIfFar(waypoints, rightLaneX, startPoint.y);
        addWaypointIfFar(waypoints, rightLaneX, downY);
        addWaypointIfFar(waypoints, endPoint.x, downY);
        return waypoints;
    }

    @NonNull
    private PointF createEntranceToCourtEndPoint(@NonNull View courtView,
                                                 @NonNull ConstraintLayout overallMap) {
        float x = courtView.getX() + dpToPx(8);
        float y = courtView.getY() + dpToPx(18);
        View topLane = overallMap.findViewById(R.id.img_car_1);
        if (topLane != null) {
            y = topLane.getY() + (topLane.getHeight() / 2f);
        } else {
            View mainBuilding = overallMap.findViewById(R.id.img_main_building);
            if (mainBuilding != null) {
                y = mainBuilding.getY() + dpToPx(26);
            }
        }
        float minY = courtView.getY() + dpToPx(8);
        float maxY = courtView.getY() + courtView.getHeight() - dpToPx(8);
        y = Math.max(minY, Math.min(y, maxY));
        return new PointF(x, y);
    }

    @NonNull
    private PointF createEntranceToRegistrarEndPoint(@NonNull View registrarView,
                                                     @NonNull ConstraintLayout overallMap) {
        float x = registrarView.getX() + (registrarView.getWidth() * 0.46f);
        float y = registrarView.getY() + registrarView.getHeight() - dpToPx(6);

        View topLane = overallMap.findViewById(R.id.img_car_1);
        if (topLane != null) {
            // Keep the handoff to registrar near the upper driveway lane.
            float laneY = topLane.getY() + (topLane.getHeight() / 2f) + dpToPx(8);
            y = Math.min(y, laneY);
        }

        float minY = registrarView.getY() + dpToPx(8);
        float maxY = registrarView.getY() + registrarView.getHeight() - dpToPx(4);
        y = Math.max(minY, Math.min(y, maxY));
        return new PointF(x, y);
    }

    @NonNull
    private PointF createEntranceToLibraryEndPoint(@NonNull View libraryView,
                                                   @NonNull ConstraintLayout overallMap) {
        float x = libraryView.getX() + (libraryView.getWidth() * 0.70f);
        float y = libraryView.getY() + (libraryView.getHeight() * 0.58f);

        float minY = libraryView.getY() + dpToPx(6);
        float maxY = libraryView.getY() + libraryView.getHeight() - dpToPx(6);
        y = Math.max(minY, Math.min(y, maxY));
        return new PointF(x, y);
    }

    @NonNull
    private List<PointF> buildEntranceToLibraryWaypoints(@NonNull PointF startPoint,
                                                          @NonNull PointF endPoint,
                                                          @NonNull ConstraintLayout overallMap) {
        List<PointF> waypoints = new ArrayList<>();

        View mainBuilding = overallMap.findViewById(R.id.img_main_building);
        float hallwayX = startPoint.x;
        if (mainBuilding != null) {
            // Keep the vertical leg closer to the right-side corridor of main building.
            hallwayX = mainBuilding.getX() + (mainBuilding.getWidth() * 0.62f);
        }

        float crossY = endPoint.y;
        View topLane = overallMap.findViewById(R.id.img_car_1);
        if (topLane != null) {
            crossY = topLane.getY() + (topLane.getHeight() / 2f) + dpToPx(2);
        } else if (mainBuilding != null) {
            crossY = mainBuilding.getY() + dpToPx(18);
        }

        addWaypointIfFar(waypoints, hallwayX, startPoint.y);
        addWaypointIfFar(waypoints, hallwayX, crossY);
        addWaypointIfFar(waypoints, endPoint.x, crossY);
        return waypoints;
    }

    @NonNull
    private PointF createOverallAnchorPoint(@NonNull View anchorView, int anchorType, boolean destination) {
        if (!destination && anchorType == OVERALL_ANCHOR_ENTRANCE) {
            return new PointF(
                    anchorView.getX() + (anchorView.getWidth() / 2f),
                    anchorView.getY() + dpToPx(8)
            );
        }
        if (destination && anchorType == OVERALL_ANCHOR_EXIT) {
            return new PointF(
                    anchorView.getX() + dpToPx(10),
                    anchorView.getY() + (anchorView.getHeight() * 0.72f)
            );
        }
        if (destination && anchorType == OVERALL_ANCHOR_COURT) {
            return new PointF(
                    anchorView.getX() + dpToPx(8),
                    anchorView.getY() + dpToPx(18)
            );
        }
        return createAnchorAtCenter(anchorView);
    }

    private void addWaypointIfFar(@NonNull List<PointF> waypoints, float x, float y) {
        if (waypoints.isEmpty()) {
            waypoints.add(new PointF(x, y));
            return;
        }
        PointF last = waypoints.get(waypoints.size() - 1);
        if (Math.abs(last.x - x) >= dpToPx(2) || Math.abs(last.y - y) >= dpToPx(2)) {
            waypoints.add(new PointF(x, y));
        }
    }

    private float resolveOverallRouteBendX(@NonNull RouteOverlayState state,
                                           @NonNull ConstraintLayout overallMap) {
        if (state.overallOriginType == OVERALL_ANCHOR_ENTRANCE
                && state.overallDestinationType == OVERALL_ANCHOR_COURT) {
            View mainBuilding = overallMap.findViewById(R.id.img_main_building);
            if (mainBuilding != null) {
                return mainBuilding.getX() + mainBuilding.getWidth() - dpToPx(8);
            }
        }
        return overallMap.getWidth() / 2f;
    }

    private void ensureOverallRouteOverlayAttached(@NonNull ConstraintLayout overallMap) {
        if (overallRouteOverlay != null && overallRouteOverlay.getParent() == overallMap) {
            configureRouteOverlayTouchPassthrough(overallRouteOverlay);
            overallRouteOverlay.bringToFront();
            return;
        }

        overallRouteOverlay = new RoutePathOverlayView(requireContext());
        configureRouteOverlayTouchPassthrough(overallRouteOverlay);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        overallMap.addView(overallRouteOverlay, params);
        overallRouteOverlay.bringToFront();
    }

    @Nullable
    private View resolveOverallAnchorView(@NonNull ConstraintLayout overallMap,
                                          int anchorType,
                                          boolean destination) {
        int viewId;
        switch (anchorType) {
            case OVERALL_ANCHOR_EXIT:
                viewId = R.id.img_exit;
                break;
            case OVERALL_ANCHOR_LIBRARY:
                viewId = R.id.img_library;
                break;
            case OVERALL_ANCHOR_REGISTRAR:
                viewId = R.id.img_registrar;
                break;
            case OVERALL_ANCHOR_COURT:
                viewId = R.id.img_court;
                break;
            case OVERALL_ANCHOR_MAIN:
                viewId = destination ? R.id.img_pin_main : R.id.img_main_building;
                break;
            case OVERALL_ANCHOR_ENTRANCE:
            default:
                viewId = R.id.img_entrance;
                break;
        }
        View resolved = overallMap.findViewById(viewId);
        if (resolved != null) {
            return resolved;
        }
        View fallbackMain = overallMap.findViewById(R.id.img_main_building);
        if (fallbackMain != null) {
            return fallbackMain;
        }
        return overallMap.findViewById(R.id.img_entrance);
    }

    private int resolveOverallOriginType(@Nullable OriginEntity origin) {
        if (origin == null) {
            return OVERALL_ANCHOR_ENTRANCE;
        }

        String combined = normalizeText(origin.name) + " "
                + normalizeText(origin.code) + " "
                + normalizeText(origin.description);
        return resolveOverallAnchorTypeFromText(combined);
    }

    private int resolveOverallOriginType(@NonNull RoomEntity originRoom, @Nullable String originBuildingCode) {
        int fromRoom = resolveOverallAnchorTypeFromRoom(originRoom, originBuildingCode);
        if (fromRoom != OVERALL_ANCHOR_MAIN) {
            return fromRoom;
        }
        return originBuildingCode == null ? OVERALL_ANCHOR_ENTRANCE : OVERALL_ANCHOR_MAIN;
    }

    private int resolveOverallDestinationType(@NonNull RoomEntity destinationRoom,
                                              @Nullable String destinationBuildingCode) {
        int fromRoom = resolveOverallAnchorTypeFromRoom(destinationRoom, destinationBuildingCode);
        if (fromRoom != OVERALL_ANCHOR_MAIN) {
            return fromRoom;
        }
        if (CODE_LIB.equals(destinationBuildingCode)) {
            return OVERALL_ANCHOR_LIBRARY;
        }
        if (CODE_REG.equals(destinationBuildingCode)) {
            return OVERALL_ANCHOR_REGISTRAR;
        }
        return OVERALL_ANCHOR_MAIN;
    }

    private int resolveOverallAnchorTypeFromRoom(@NonNull RoomEntity room, @Nullable String buildingCode) {
        String code = normalizeCode(room.code);
        if (CODE_ENT.equals(code)) {
            return OVERALL_ANCHOR_ENTRANCE;
        }
        if (CODE_EXIT.equals(code)) {
            return OVERALL_ANCHOR_EXIT;
        }
        if (CODE_COURT.equals(code)) {
            return OVERALL_ANCHOR_COURT;
        }
        if (CODE_LIB.equals(buildingCode) || (code != null && code.contains("LIB"))) {
            return OVERALL_ANCHOR_LIBRARY;
        }
        if (CODE_REG.equals(buildingCode) || (code != null && code.startsWith("REG"))) {
            return OVERALL_ANCHOR_REGISTRAR;
        }

        String roomText = normalizeText(room.name) + " "
                + normalizeText(room.code) + " "
                + normalizeText(room.description);
        int fromText = resolveOverallAnchorTypeFromText(roomText);
        return fromText == OVERALL_ANCHOR_ENTRANCE ? OVERALL_ANCHOR_MAIN : fromText;
    }

    private int resolveOverallAnchorTypeFromText(@Nullable String rawText) {
        String combined = normalizeText(rawText);
        if (combined.contains("exit")) {
            return OVERALL_ANCHOR_EXIT;
        }
        if (combined.contains("library") || combined.contains("lib")) {
            return OVERALL_ANCHOR_LIBRARY;
        }
        if (combined.contains("registrar") || combined.contains("reg")) {
            return OVERALL_ANCHOR_REGISTRAR;
        }
        if (combined.contains("court")) {
            return OVERALL_ANCHOR_COURT;
        }
        if (combined.contains("gate") || combined.contains("entrance")) {
            return OVERALL_ANCHOR_ENTRANCE;
        }
        return OVERALL_ANCHOR_MAIN;
    }

    private void clearActiveRouteOverlay() {
        RouteOverlayState state = activeRouteOverlay;
        activeRouteOverlay = null;
        activeRouteDestinationRoomId = -1;
        activeRouteOriginId = -1;
        activeRouteOriginRoomId = -1;
        clearFloorRouteOverlay();
        updateFloorChipAvailability();
        if (state != null && highlightedRoomId != null && highlightedRoomId == state.destinationRoomId) {
            clearRoomHighlight();
        }
    }

    private boolean isFloorOneSelected() {
        return selectedFloor != null && selectedFloor == 1;
    }

    private boolean isFloorTwoSelected() {
        return selectedFloor != null && selectedFloor == 2;
    }

    private boolean isFloorThreeSelected() {
        return selectedFloor != null && selectedFloor == 3;
    }

    private boolean isFloorFourSelected() {
        return selectedFloor != null && selectedFloor == 4;
    }

    private boolean isFloorFiveSelected() {
        return selectedFloor != null && selectedFloor == 5;
    }

    private int[] getActiveRoomBoxIds() {
        if (isFloorOneSelected()) {
            return FLOOR1_ROOM_BOX_IDS;
        }
        if (isFloorTwoSelected()) {
            return FLOOR2_ROOM_BOX_IDS;
        }
        if (isFloorThreeSelected()) {
            return FLOOR3_ROOM_BOX_IDS;
        }
        if (isFloorFourSelected()) {
            return FLOOR4_ROOM_BOX_IDS;
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
        clearActiveRouteOverlay();
        setDirectionsFocusMode(false);
        setBottomNavVisible(true);
        setDirectionsFabVisible(true);
        // Shutdown executor to prevent thread leaks
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
        }
    }

    private static final class RouteOverlayState {
        final int destinationRoomId;
        int currentFloorNumber;
        final int destinationFloorNumber;
        final int originFloorNumber;
        final int originRoomId;
        int anchorType;
        final boolean useStairs;
        final boolean useElevator;
        final boolean preferGroundStart;
        final int overallOriginType;
        final int overallDestinationType;
        final boolean overallOnly;
        final boolean routeViaMainBeforeCourt;
        final boolean campusTransitionSequence;
        final boolean forceAuditoriumBottomStair;
        boolean showingCampusLeg;

        RouteOverlayState(int destinationRoomId,
                          int originFloorNumber,
                          int destinationFloorNumber,
                          int originRoomId,
                          int anchorType,
                          boolean useStairs,
                          boolean useElevator,
                          boolean preferGroundStart,
                          int overallOriginType,
                          int overallDestinationType,
                          boolean overallOnly,
                          boolean routeViaMainBeforeCourt,
                          boolean campusTransitionSequence,
                          boolean forceAuditoriumBottomStair) {
            this.destinationRoomId = destinationRoomId;
            this.currentFloorNumber = originFloorNumber;
            this.originFloorNumber = originFloorNumber;
            this.destinationFloorNumber = destinationFloorNumber;
            this.originRoomId = originRoomId;
            this.anchorType = anchorType;
            this.useStairs = useStairs;
            this.useElevator = useElevator;
            this.preferGroundStart = preferGroundStart;
            this.overallOriginType = overallOriginType;
            this.overallDestinationType = overallDestinationType;
            this.overallOnly = overallOnly;
            this.routeViaMainBeforeCourt = routeViaMainBeforeCourt;
            this.campusTransitionSequence = campusTransitionSequence;
            this.forceAuditoriumBottomStair = forceAuditoriumBottomStair;
            this.showingCampusLeg = false;
        }

        boolean isMultiFloor() {
            return !overallOnly && originFloorNumber != destinationFloorNumber;
        }
    }
}
