package com.example.freshguide.ui.user;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.ui.adapter.DirectionSearchAdapter;
import com.example.freshguide.ui.adapter.RouteStepAdapter;
import com.example.freshguide.viewmodel.DirectionsViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DirectionsSheetFragment extends BottomSheetDialogFragment {

    public static final String ARG_PRESELECTED_ROOM_ID = "preselectedRoomId";
    public static final String ARG_PRESELECTED_ROOM_NAME = "preselectedRoomName";
    public static final String ARG_PRESELECTED_ORIGIN_ID = "preselectedOriginId";
    public static final String ARG_PRESELECTED_ORIGIN_ROOM_ID = "preselectedOriginRoomId";
    public static final String ARG_AUTO_START_ROUTE = "autoStartRoute";
    public static final String ARG_KEEP_OPEN_ON_START = "keepOpenOnStart";
    public static final String ARG_SHEET_SESSION_ID = "sheetSessionId";
    public static final String RESULT_ROUTE_MAP_OVERLAY = "route_map_overlay_result";
    public static final String RESULT_SHEET_VISIBILITY = "directions_sheet_visibility_result";
    public static final String RESULT_NAVIGATION_FOCUS = "directions_navigation_focus_result";
    public static final String KEY_ROUTE_VISIBLE = "route_visible";
    public static final String KEY_ROUTE_ROOM_ID = "route_room_id";
    public static final String KEY_ROUTE_ORIGIN_ID = "route_origin_id";
    public static final String KEY_ROUTE_ORIGIN_ROOM_ID = "route_origin_room_id";
    public static final String KEY_ROUTE_USE_STAIRS = "route_use_stairs";
    public static final String KEY_ROUTE_USE_ELEVATOR = "route_use_elevator";
    public static final String KEY_SHEET_VISIBLE = "sheet_visible";
    public static final String KEY_SHEET_SESSION_ID = "sheet_session_id";
    public static final String KEY_NAVIGATION_FOCUS_ACTIVE = "navigation_focus_active";

    private enum ActiveField {
        NONE,
        ORIGIN,
        DESTINATION
    }

    private enum SheetDisplayState {
        FULL,
        HALF,
        SMALL
    }

    private enum ContentMode {
        SUMMARY,
        ROUTE
    }

    private final List<OriginEntity> allOrigins = new ArrayList<>();
    private final List<RoomEntity> allRooms = new ArrayList<>();

    private DirectionSearchAdapter originAdapter;
    private DirectionSearchAdapter destinationAdapter;
    private RouteStepAdapter routeAdapter;
    private DirectionsViewModel viewModel;

    private View sheetRoot;
    private View summaryContent;
    private View routeContent;
    private View resultsScrim;
    private View collapsedSummary;
    private TextView titleView;
    private View originLabel;
    private View destinationLabel;
    private View originFieldContainer;
    private View destinationFieldContainer;
    private EditText etOrigin;
    private EditText etDestination;
    private ImageButton btnSwapDirection;
    private ImageButton btnClearOrigin;
    private ImageButton btnClearDestination;
    private LinearLayout originResults;
    private LinearLayout destinationResults;
    private RecyclerView originRecycler;
    private RecyclerView destinationRecycler;
    private RecyclerView routeRecycler;
    private View originEmpty;
    private View destinationEmpty;
    private View routeEmptyState;
    private TextView collapsedLabelText;
    private TextView collapsedOriginText;
    private TextView collapsedDestinationText;
    private TextView collapsedHintText;
    private ProgressBar routeLoading;
    private MaterialButton btnStart;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private int originId = -1;
    private int selectedOriginRoomId = -1;
    private int selectedRoomId = -1;
    private int preselectedRoomId = -1;
    private String preselectedRoomName;
    private int preselectedOriginId = -1;
    private int preselectedOriginRoomId = -1;
    private boolean autoStartRoute;
    private boolean keepOpenOnStart;
    private boolean routeAutoStarted;
    private ActiveField activeField = ActiveField.NONE;
    private SheetDisplayState sheetDisplayState = SheetDisplayState.HALF;
    private ContentMode contentMode = ContentMode.SUMMARY;
    private boolean dropdownVisible;
    private boolean suppressOriginWatcher;
    private boolean suppressDestinationWatcher;
    private boolean reverseCurrentRoute;
    private int activeRouteOriginId = -1;
    private int activeRouteOriginRoomId = -1;
    private int activeRouteRoomId = -1;
    private int sheetSessionId;
    private boolean preserveOverlayOnDismiss;
    private int resultPanelMaxHeightPx;
    private float lastSheetSlideOffset;

    // -----------------------------------------------------------------------
    // FIX: Guard flag that prevents focus-change listeners from re-opening
    // the dropdown while we are programmatically tearing it down.
    // -----------------------------------------------------------------------
    private boolean isClosingDropdown = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_directions, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        publishSheetVisibility(true);
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setDismissWithAnimation(true);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.BOTTOM);
                window.setDimAmount(0.16f);
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                if (params != null) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    sheet.setLayoutParams(params);
                }
                sheet.setBackgroundResource(R.drawable.bg_bottom_sheet_surface);
                applyBottomSheetDepth(sheet, 30);
                bottomSheetBehavior = BottomSheetBehavior.from(sheet);
                bottomSheetBehavior.setFitToContents(false);
                bottomSheetBehavior.setExpandedOffset(0);
                bottomSheetBehavior.setHalfExpandedRatio(0.52f);
                bottomSheetBehavior.setPeekHeight(dpToPx(92), true);
                bottomSheetBehavior.setSkipCollapsed(false);
                bottomSheetBehavior.setHideable(true);
                bottomSheetBehavior.setDraggable(true);
                sheet.post(() -> {
                    if (bottomSheetBehavior == null) return;
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    updateWindowForSheetState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                });
                bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismissAllowingStateLoss();
                            return;
                        }
                        updateWindowForSheetState(newState);
                        handleBottomSheetStateChanged(newState);
                        updateResultPanelHeights();
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        lastSheetSlideOffset = slideOffset;
                        if (bottomSheetBehavior != null) {
                            updateWindowForSheetState(bottomSheetBehavior.getState());
                        }
                        updateResultPanelHeights();
                    }
                });
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            preselectedRoomId = args.getInt(ARG_PRESELECTED_ROOM_ID, -1);
            preselectedRoomName = args.getString(ARG_PRESELECTED_ROOM_NAME);
            preselectedOriginId = args.getInt(ARG_PRESELECTED_ORIGIN_ID, -1);
            preselectedOriginRoomId = args.getInt(ARG_PRESELECTED_ORIGIN_ROOM_ID, -1);
            autoStartRoute = args.getBoolean(ARG_AUTO_START_ROUTE, false);
            keepOpenOnStart = args.getBoolean(ARG_KEEP_OPEN_ON_START, false);
            sheetSessionId = args.getInt(ARG_SHEET_SESSION_ID, 0);
            if (keepOpenOnStart) {
                preserveOverlayOnDismiss = true;
            }
        }

        FragmentActivity activity = requireActivity();
        viewModel = new ViewModelProvider(this).get(DirectionsViewModel.class);

        sheetRoot = view.findViewById(R.id.sheet_root);
        titleView = view.findViewById(R.id.tv_title);
        collapsedSummary = view.findViewById(R.id.layout_collapsed_summary);
        collapsedLabelText = view.findViewById(R.id.tv_collapsed_label);
        collapsedOriginText = view.findViewById(R.id.tv_collapsed_origin);
        collapsedDestinationText = view.findViewById(R.id.tv_collapsed_destination);
        collapsedHintText = view.findViewById(R.id.tv_collapsed_hint);
        summaryContent = view.findViewById(R.id.layout_summary_content);
        routeContent = view.findViewById(R.id.layout_route_content);
        resultsScrim = view.findViewById(R.id.view_results_scrim);
        originLabel = view.findViewById(R.id.tv_origin_label);
        destinationLabel = view.findViewById(R.id.tv_destination_label);
        originFieldContainer = view.findViewById(R.id.layout_origin_field);
        destinationFieldContainer = view.findViewById(R.id.layout_destination_field);
        etOrigin = view.findViewById(R.id.et_origin_search);
        etDestination = view.findViewById(R.id.et_destination_search);
        btnSwapDirection = view.findViewById(R.id.btn_swap_direction);
        btnClearOrigin = view.findViewById(R.id.btn_clear_origin);
        btnClearDestination = view.findViewById(R.id.btn_clear_destination);
        originResults = view.findViewById(R.id.layout_origin_results);
        destinationResults = view.findViewById(R.id.layout_destination_results);
        originEmpty = view.findViewById(R.id.tv_origin_empty);
        destinationEmpty = view.findViewById(R.id.tv_destination_empty);
        routeEmptyState = view.findViewById(R.id.layout_route_empty_state);
        routeLoading = view.findViewById(R.id.progress_route_loading);
        btnStart = view.findViewById(R.id.btn_start_directions);
        originAdapter = new DirectionSearchAdapter(this::onSuggestionPicked);
        destinationAdapter = new DirectionSearchAdapter(this::onSuggestionPicked);
        routeAdapter = new RouteStepAdapter();

        originRecycler = view.findViewById(R.id.recycler_origin_results);
        originRecycler.setLayoutManager(new LinearLayoutManager(activity));
        originRecycler.setAdapter(originAdapter);

        destinationRecycler = view.findViewById(R.id.recycler_destination_results);
        destinationRecycler.setLayoutManager(new LinearLayoutManager(activity));
        destinationRecycler.setAdapter(destinationAdapter);

        routeRecycler = view.findViewById(R.id.recycler_route_steps);
        routeRecycler.setLayoutManager(new LinearLayoutManager(activity));
        routeRecycler.setAdapter(routeAdapter);
        routeRecycler.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_route_steps_in));

        resultPanelMaxHeightPx = dpToPx(200);
        setupInputs();
        observeDirectionsState(view);
        updateCollapsedSummary();
        applySheetChrome(false);
        view.post(this::updateResultPanelHeights);

        btnStart.setOnClickListener(v -> startDirectionsInPlace(view));
        btnSwapDirection.setOnClickListener(v -> swapOriginAndDestination());

        loadOriginsAndRooms();
    }

    private void observeDirectionsState(@NonNull View rootView) {
        viewModel.getRoute().observe(getViewLifecycleOwner(), route -> {
            if (route == null) return;
            renderRoute(route);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || err.trim().isEmpty()) return;

            if (contentMode == ContentMode.ROUTE && isMissingRouteError(err)) {
                showRouteEmptyState(activeRouteOriginRoomId > 0);
                return;
            }

            clearRouteFeedback(false);
            Snackbar.make(rootView, err, Snackbar.LENGTH_LONG).show();
        });
    }

    private void startDirectionsInPlace(@NonNull View rootView) {
        boolean shouldDismissOnStart = autoStartRoute && !keepOpenOnStart;
        String originText = textOf(etOrigin);
        String destinationText = textOf(etDestination);

        int directOriginId = originId != -1 ? originId : resolveOriginId(originText);
        int directOriginRoomId = selectedOriginRoomId != -1 ? selectedOriginRoomId : resolveOriginRoomId(originText);
        int directRoomId = selectedRoomId != -1 ? selectedRoomId : resolveRoomId(destinationText);
        int swappedRoomId = selectedRoomId != -1 ? selectedRoomId : resolveRoomId(originText);
        int swappedOriginId = originId != -1 ? originId : resolveOriginId(destinationText);

        int routeOriginId = -1;
        int routeRoomId = -1;

        if (directOriginId != -1 && directRoomId != -1) {
            routeOriginId = directOriginId;
            routeRoomId = directRoomId;
            reverseCurrentRoute = false;
            activeRouteOriginRoomId = -1;
        } else if (directOriginRoomId != -1 && directRoomId != -1) {
            publishNavigationFocusState(true);
            hideResultsAndClearFocus();
            showRouteLoadingState();
            presentStartedRouteSheet();
            activeRouteOriginId = -1;
            activeRouteOriginRoomId = directOriginRoomId;
            activeRouteRoomId = directRoomId;
            reverseCurrentRoute = false;
            viewModel.loadRoomRoute(directRoomId, directOriginRoomId);
            return;
        } else if (swappedOriginId != -1 && swappedRoomId != -1) {
            routeOriginId = swappedOriginId;
            routeRoomId = swappedRoomId;
            reverseCurrentRoute = true;
            activeRouteOriginRoomId = -1;
        }

        if (routeOriginId == -1) {
            Snackbar.make(rootView, R.string.error_origin_missing, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (routeRoomId == -1) {
            Snackbar.make(rootView, R.string.error_destination_missing, Snackbar.LENGTH_LONG).show();
            return;
        }

        publishNavigationFocusState(true);
        hideResultsAndClearFocus();
        activeRouteOriginId = routeOriginId;
        activeRouteOriginRoomId = -1;
        activeRouteRoomId = routeRoomId;
        if (shouldDismissOnStart) {
            publishOriginRouteOverlay(routeRoomId, routeOriginId);
            dismissAllowingStateLoss();
            return;
        }
        showRouteLoadingState();
        presentStartedRouteSheet();
        viewModel.loadRoute(routeRoomId, routeOriginId);
    }

    private void showRouteLoadingState() {
        contentMode = ContentMode.ROUTE;
        updateCollapsedSummary();
        routeAdapter.setSteps(Collections.emptyList());
        routeContent.setVisibility(View.VISIBLE);
        routeContent.setAlpha(1f);
        routeRecycler.setVisibility(View.GONE);
        routeEmptyState.setVisibility(View.GONE);
        routeLoading.setVisibility(View.VISIBLE);
        routeLoading.setAlpha(0f);
        routeLoading.animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void renderRoute(@NonNull RouteDto route) {
        List<RouteStepDto> steps = route.steps != null ? route.steps : Collections.emptyList();
        List<RouteStepDto> displaySteps = new ArrayList<>(steps);
        if (reverseCurrentRoute) {
            Collections.reverse(displaySteps);
        }

        routeLoading.animate().cancel();
        routeLoading.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> routeLoading.setVisibility(View.GONE))
                .start();

        if (displaySteps.isEmpty()) {
            showRouteEmptyState(false);
            return;
        }

        publishRouteOverlay(activeRouteRoomId, activeRouteOriginId, activeRouteOriginRoomId, route);
        routeEmptyState.setVisibility(View.GONE);
        routeAdapter.setSteps(displaySteps);
        routeRecycler.scrollToPosition(0);
        routeRecycler.setAlpha(0f);
        routeRecycler.setVisibility(View.VISIBLE);
        routeRecycler.post(() -> {
            if (!isAdded()) return;
            routeRecycler.scheduleLayoutAnimation();
            routeRecycler.animate()
                    .alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }

    private void showRouteEmptyState(boolean keepOverlay) {
        routeLoading.animate().cancel();
        routeLoading.setVisibility(View.GONE);
        routeAdapter.setSteps(Collections.emptyList());
        routeContent.setVisibility(View.VISIBLE);
        if (!keepOverlay) {
            clearRouteOverlay();
        }
        routeRecycler.setVisibility(View.GONE);
        routeEmptyState.setAlpha(0f);
        routeEmptyState.setVisibility(View.VISIBLE);
        routeEmptyState.animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private boolean isMissingRouteError(@Nullable String message) {
        String normalized = normalizeValue(message);
        return normalized.contains("route not found")
                || normalized.contains("no route")
                || normalized.contains("no directions")
                || normalized.contains("not available");
    }

    private void updateCollapsedSummary() {
        if (collapsedLabelText != null) {
            collapsedLabelText.setText(R.string.label_destination);
        }
        if (collapsedOriginText != null) {
            String originValue = textOf(etOrigin).trim();
            collapsedOriginText.setText(originValue.isEmpty()
                    ? getString(R.string.directions_small_origin_placeholder)
                    : getString(R.string.directions_small_from, originValue));
        }
        if (collapsedDestinationText != null) {
            collapsedDestinationText.setText(getSummaryValue(
                    textOf(etDestination),
                    getString(R.string.label_select_destination)
            ));
        }
        if (collapsedHintText != null) {
            collapsedHintText.setText(contentMode == ContentMode.ROUTE
                    ? R.string.directions_small_hint_steps
                    : R.string.directions_small_hint_edit);
        }
    }

    @NonNull
    private String getSummaryValue(@Nullable String value, @NonNull String fallback) {
        String trimmed = value != null ? value.trim() : "";
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void applySheetChrome(boolean collapsed) {
        if (collapsedSummary != null) {
            collapsedSummary.setVisibility(collapsed ? View.VISIBLE : View.GONE);
        }
        if (summaryContent != null) {
            summaryContent.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
        if (resultsScrim != null && collapsed) {
            hideResults();
        }
        if (sheetRoot != null && collapsed) {
            clearAllFieldFocus();
        }
        if (titleView != null) {
            titleView.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        }
        applyWindowDim(collapsed ? 0f : 0.16f);
    }

    private void applyWindowDim(float dimAmount) {
        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }
        Window window = ((BottomSheetDialog) getDialog()).getWindow();
        if (window != null) {
            window.setDimAmount(dimAmount);
        }
    }

    private void presentStartedRouteSheet() {
        if (bottomSheetBehavior == null) return;
        sheetRoot.post(() -> {
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void clearRouteFeedback(boolean collapseToSummary) {
        contentMode = ContentMode.SUMMARY;
        reverseCurrentRoute = false;
        preserveOverlayOnDismiss = false;
        routeLoading.animate().cancel();
        routeLoading.setVisibility(View.GONE);
        routeRecycler.animate().cancel();
        routeRecycler.setVisibility(View.GONE);
        routeEmptyState.animate().cancel();
        routeEmptyState.setVisibility(View.GONE);
        routeAdapter.setSteps(Collections.emptyList());
        clearRouteOverlay();
        routeContent.setVisibility(View.GONE);
        activeRouteOriginId = -1;
        activeRouteOriginRoomId = -1;
        activeRouteRoomId = -1;
        updateCollapsedSummary();
        if (collapseToSummary && bottomSheetBehavior != null) {
            sheetRoot.post(() -> {
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            });
        }
    }

    private void setupInputs() {

        if (sheetRoot != null) {
            sheetRoot.setFocusable(true);
            sheetRoot.setFocusableInTouchMode(true);
        }

        resultsScrim.setOnClickListener(v -> hideResultsAndClearFocus());

        sheetRoot.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && shouldDismissResults(event)) {
                hideResultsAndClearFocus();
            }
            return false;
        });


        bindFieldInteractions(ActiveField.ORIGIN);
        bindFieldInteractions(ActiveField.DESTINATION);
    }

    private void swapOriginAndDestination() {
        hideResultsAndClearFocus();
        clearRouteFeedback(false);

        String previousOrigin = textOf(etOrigin);
        String previousDestination = textOf(etDestination);

        setFieldText(etOrigin, previousDestination, true);
        setFieldText(etDestination, previousOrigin, false);

        originId = resolveOriginId(previousDestination);
        selectedOriginRoomId = originId == -1 ? resolveOriginRoomId(previousDestination) : -1;
        selectedRoomId = resolveRoomId(previousOrigin);

        updateClearButtons();
        updateSuggestionList();
        updateStartState();

        if (btnSwapDirection != null) {
            btnSwapDirection.animate()
                    .rotationBy(180f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void onSuggestionPicked(DirectionSearchAdapter.SuggestionItem item) {

        activeField = ActiveField.NONE;
        dropdownVisible = false;


        if (item.isOrigin) {
            applySuggestionSelection(ActiveField.ORIGIN, item.id, item.title, item.roomBased);
        } else {
            applySuggestionSelection(ActiveField.DESTINATION, item.id, item.title, item.roomBased);
        }


        forceHideAllDropdowns();


        clearAllFieldFocus();

        clearRouteFeedback(false);
        updateClearButtons();
        updateStartState();
    }


    private void forceHideAllDropdowns() {
        setDropdownVisibility(originResults, false);
        setDropdownVisibility(destinationResults, false);
        setDropdownVisibility(resultsScrim, false);
    }

    private void clearAllFieldFocus() {
        isClosingDropdown = true;
        try {
            if (etOrigin != null) etOrigin.clearFocus();
            if (etDestination != null) etDestination.clearFocus();
            if (sheetRoot != null) sheetRoot.requestFocus();
        } finally {
            isClosingDropdown = false;
        }
    }

    private void setFieldText(EditText field, String value, boolean originField) {
        if (originField) {
            suppressOriginWatcher = true;
        } else {
            suppressDestinationWatcher = true;
        }
        field.setText(value);
        field.setSelection(value != null ? value.length() : 0);
        if (originField) {
            suppressOriginWatcher = false;
        } else {
            suppressDestinationWatcher = false;
        }
        updateCollapsedSummary();
    }

    private void loadOriginsAndRooms() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<OriginEntity> origins = db.originDao().getAllSync();
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();

            requireActivity().runOnUiThread(() -> {
                allOrigins.clear();
                if (origins != null) allOrigins.addAll(origins);
                allRooms.clear();
                if (rooms != null) allRooms.addAll(rooms);

                applyPreselectedRoom();
                applyPreselectedOrigin();
                updateSuggestionList();
                hideResults();
                updateStartState();
                maybeAutoStartRoute();
            });
        });
    }

    private void applyPreselectedRoom() {
        if (preselectedRoomId <= 0) return;
        for (RoomEntity room : allRooms) {
            if (room.id != preselectedRoomId) continue;
            selectedRoomId = room.id;
            String name = room.name != null && !room.name.trim().isEmpty()
                    ? room.name
                    : (preselectedRoomName != null && !preselectedRoomName.trim().isEmpty()
                    ? preselectedRoomName
                    : getString(R.string.label_destination));
            setFieldText(getFieldInput(ActiveField.DESTINATION), name, false);
            updateClearButtons();
            break;
        }
    }

    private void applyPreselectedOrigin() {
        if (preselectedOriginId > 0) {
            for (OriginEntity origin : allOrigins) {
                if (origin == null || origin.id != preselectedOriginId) {
                    continue;
                }
                originId = origin.id;
                selectedOriginRoomId = -1;
                setFieldText(getFieldInput(ActiveField.ORIGIN), safe(origin.name, "Origin"), true);
                updateClearButtons();
                return;
            }
        }
        if (preselectedOriginRoomId <= 0) {
            return;
        }
        for (RoomEntity room : allRooms) {
            if (room == null || room.id != preselectedOriginRoomId) {
                continue;
            }
            originId = -1;
            selectedOriginRoomId = room.id;
            setFieldText(getFieldInput(ActiveField.ORIGIN), safe(room.name, "Room"), true);
            updateClearButtons();
            return;
        }
    }

    private void maybeAutoStartRoute() {
        if (!autoStartRoute || routeAutoStarted) {
            return;
        }
        if (textOf(etOrigin).trim().isEmpty() || textOf(etDestination).trim().isEmpty()) {
            return;
        }
        routeAutoStarted = true;
        View root = sheetRoot != null ? sheetRoot : getView();
        if (root != null) {
            startDirectionsInPlace(root);
        }
    }

    private void updateSuggestionList() {
        originAdapter.submitList(buildOriginSuggestions(textOf(etOrigin)));
        destinationAdapter.submitList(buildDestinationSuggestions(textOf(etDestination)));
        boolean originHasResults = originAdapter.getItemCount() > 0;
        boolean destinationHasResults = destinationAdapter.getItemCount() > 0;
        originRecycler.setVisibility(originHasResults ? View.VISIBLE : View.GONE);
        destinationRecycler.setVisibility(destinationHasResults ? View.VISIBLE : View.GONE);
        originEmpty.setVisibility(originHasResults ? View.GONE : View.VISIBLE);
        destinationEmpty.setVisibility(destinationHasResults ? View.GONE : View.VISIBLE);
        showActiveResults();
        updateResultPanelHeights();
    }

    private List<DirectionSearchAdapter.SuggestionItem> buildOriginSuggestions(String query) {
        List<DirectionSearchAdapter.SuggestionItem> suggestions = new ArrayList<>();
        for (OriginEntity origin : allOrigins) {
            if (!matches(query, origin.name, origin.code, origin.description)) continue;
            suggestions.add(new DirectionSearchAdapter.SuggestionItem(
                    origin.id,
                    safe(origin.name, "Origin"),
                    safe(origin.description, safe(origin.code, "Campus origin")),
                    R.drawable.ic_directions,
                    true,
                    false
            ));
        }
        for (RoomEntity room : allRooms) {
            if (!matches(query, room.name, room.code, room.location)) continue;
            suggestions.add(new DirectionSearchAdapter.SuggestionItem(
                    room.id,
                    safe(room.name, "Room"),
                    "Start from " + buildRoomSubtitle(room),
                    R.drawable.ic_search_pin,
                    true,
                    true
            ));
        }
        return suggestions;
    }

    private List<DirectionSearchAdapter.SuggestionItem> buildDestinationSuggestions(String query) {
        List<DirectionSearchAdapter.SuggestionItem> suggestions = new ArrayList<>();
        for (RoomEntity room : allRooms) {
            if (!matches(query, room.name, room.code, room.location)) continue;
            suggestions.add(new DirectionSearchAdapter.SuggestionItem(
                    room.id,
                    safe(room.name, "Room"),
                    buildRoomSubtitle(room),
                    R.drawable.ic_search_pin,
                    false,
                    true
            ));
        }
        return suggestions;
    }

    private void showActiveResults() {
        if (originResults == null || destinationResults == null) {
            return;
        }

        if (isClosingDropdown) {
            return;
        }

        boolean showOrigin = activeField == ActiveField.ORIGIN && etOrigin.hasFocus();
        boolean showDestination = activeField == ActiveField.DESTINATION && etDestination.hasFocus();
        dropdownVisible = showOrigin || showDestination;

        setDropdownVisibility(originResults, showOrigin);
        setDropdownVisibility(destinationResults, showDestination);
        setDropdownVisibility(resultsScrim, dropdownVisible);

        if (dropdownVisible) {
            resultsScrim.bringToFront();
            bringPersistentControlsToFront();
            if (showOrigin) {
                originResults.bringToFront();
            } else if (showDestination) {
                destinationResults.bringToFront();
            }
        }
    }

    private void hideResults() {
        dropdownVisible = false;
        activeField = ActiveField.NONE;
        setDropdownVisibility(originResults, false);
        setDropdownVisibility(destinationResults, false);
        setDropdownVisibility(resultsScrim, false);
    }

    private void hideResultsAndClearFocus() {
        hideResults();
        clearAllFieldFocus();
        updateClearButtons();
    }

    private void focusField(ActiveField fieldType) {
        if (fieldType == ActiveField.NONE) {
            hideResultsAndClearFocus();
            return;
        }

        if (isClosingDropdown) return;

        EditText field = getFieldInput(fieldType);
        EditText otherField = getFieldInput(getOtherField(fieldType));
        if (otherField != null) otherField.clearFocus();
        activeField = fieldType;
        if (field != null) {
            field.requestFocus();
            field.setSelection(field.getText() != null ? field.getText().length() : 0);
        }
        updateSuggestionList();
    }

    private void updateClearButtons() {
        if (btnClearOrigin != null) {
            btnClearOrigin.setVisibility(textOf(etOrigin).trim().isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (btnClearDestination != null) {
            btnClearDestination.setVisibility(textOf(etDestination).trim().isEmpty() ? View.GONE : View.VISIBLE);
        }
        updateCollapsedSummary();
    }

    private void bringPersistentControlsToFront() {
        if (originLabel != null) originLabel.bringToFront();
        if (originFieldContainer != null) originFieldContainer.bringToFront();
        if (destinationLabel != null) destinationLabel.bringToFront();
        if (destinationFieldContainer != null) destinationFieldContainer.bringToFront();
        if (btnSwapDirection != null) btnSwapDirection.bringToFront();
        if (btnStart != null) btnStart.bringToFront();
    }

    private boolean shouldDismissResults(@NonNull MotionEvent event) {
        if (activeField == ActiveField.NONE || !dropdownVisible) return false;
        View activeInput = getFieldContainer(activeField);
        View activeResults = getResultsPanel(activeField);
        return !isTouchWithinView(activeInput, event) && !isTouchWithinView(activeResults, event);
    }

    private boolean isTouchWithinView(@Nullable View target, @NonNull MotionEvent event) {
        if (target == null || target.getVisibility() != View.VISIBLE) return false;
        int[] location = new int[2];
        target.getLocationOnScreen(location);
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        return rawX >= location[0]
                && rawX <= location[0] + target.getWidth()
                && rawY >= location[1]
                && rawY <= location[1] + target.getHeight();
    }

    private void updateResultPanelHeights() {
        if (sheetRoot == null || btnStart == null || summaryContent == null
                || summaryContent.getVisibility() != View.VISIBLE) {
            return;
        }
        sheetRoot.post(() -> {
            setHeight(originResults, computeDropdownHeight(etOrigin,
                    originAdapter != null ? originAdapter.getItemCount() : 0));
            setHeight(destinationResults, computeDropdownHeight(etDestination,
                    destinationAdapter != null ? destinationAdapter.getItemCount() : 0));
        });
    }

    private int computeDropdownHeight(@Nullable View anchor, int itemCount) {
        int emptyHeight = dpToPx(104);
        int rowHeight = dpToPx(66);
        int contentHeight = itemCount > 0 ? rowHeight * Math.min(itemCount, 5) : emptyHeight;
        int desiredHeight = itemCount == 1 ? dpToPx(78) : contentHeight;
        return Math.min(computeAvailableDropdownHeight(anchor, emptyHeight), desiredHeight);
    }

    private int computeAvailableDropdownHeight(@Nullable View anchor, int minimumHeight) {
        if (anchor == null || btnStart == null) {
            return resultPanelMaxHeightPx > 0 ? resultPanelMaxHeightPx : minimumHeight;
        }
        int spacing = dpToPx(18);
        int available = btnStart.getTop() - anchor.getBottom() - spacing;
        int bounded = resultPanelMaxHeightPx > 0 ? Math.min(available, resultPanelMaxHeightPx) : available;
        return Math.max(minimumHeight, bounded);
    }

    private void setHeight(@Nullable View target, int heightPx) {
        if (target == null) return;
        ViewGroup.LayoutParams params = target.getLayoutParams();
        if (params == null) return;
        params.height = heightPx;
        target.setLayoutParams(params);
    }

    private void setDropdownVisibility(@Nullable View target, boolean visible) {
        if (target == null) return;
        target.setVisibility(visible ? View.VISIBLE : View.GONE);
        target.setClickable(visible);
        target.setFocusable(visible);
        target.setEnabled(visible);
    }

    private void autoResolveSelection(@NonNull ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) {
            originId = resolveOriginId(textOf(etOrigin));
            selectedOriginRoomId = originId == -1 ? resolveOriginRoomId(textOf(etOrigin)) : -1;
        } else if (fieldType == ActiveField.DESTINATION) {
            selectedRoomId = resolveRoomId(textOf(etDestination));
        }
    }

    private int resolveOriginId(@Nullable String value) {
        String normalized = normalizeValue(value);
        if (normalized.isEmpty()) {
            return -1;
        }
        int fuzzyMatch = -1;
        int fuzzyCount = 0;
        for (OriginEntity origin : allOrigins) {
            if (matchesExact(normalized, origin.name, origin.code, origin.description)) {
                return origin.id;
            }
            if (matches(normalized, origin.name, origin.code, origin.description)) {
                fuzzyCount++;
            }
            if (fuzzyMatch == -1 && matches(normalized, origin.name, origin.code, origin.description)) {
                fuzzyMatch = origin.id;
            }
        }
        return fuzzyCount == 1 ? fuzzyMatch : -1;
    }

    private int resolveOriginRoomId(@Nullable String value) {
        return resolveRoomId(value);
    }

    private int resolveRoomId(@Nullable String value) {
        String normalized = normalizeValue(value);
        if (normalized.isEmpty()) {
            return -1;
        }
        int fuzzyMatch = -1;
        int fuzzyCount = 0;
        for (RoomEntity room : allRooms) {
            if (matchesExact(normalized, room.name, room.code, room.location)) {
                return room.id;
            }
            if (matches(normalized, room.name, room.code, room.location)) {
                fuzzyCount++;
            }
            if (fuzzyMatch == -1 && matches(normalized, room.name, room.code, room.location)) {
                fuzzyMatch = room.id;
            }
        }
        return fuzzyCount == 1 ? fuzzyMatch : -1;
    }

    private boolean matches(String query, String... values) {
        if (query == null || query.trim().isEmpty()) return true;
        String normalized = query.trim().toLowerCase(Locale.US);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.US).contains(normalized)) return true;
        }
        return false;
    }

    private boolean matchesExact(@NonNull String query, String... values) {
        for (String value : values) {
            if (normalizeValue(value).equals(query)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String normalizeValue(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private String buildRoomSubtitle(RoomEntity room) {
        String location = safe(room.location, "Campus room");
        String code = room.code != null && !room.code.trim().isEmpty() ? room.code.trim() : null;
        return code != null ? location + " - " + code : location;
    }

    private String textOf(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private String safe(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private void updateStartState() {
        boolean enabled = !textOf(etOrigin).trim().isEmpty() && !textOf(etDestination).trim().isEmpty();
        btnStart.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 1f : 0.7f);
    }

    private void handleBottomSheetStateChanged(int newState) {
        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            sheetDisplayState = SheetDisplayState.FULL;
            lastSheetSlideOffset = 1f;
            resultPanelMaxHeightPx = dpToPx(360);
            applySheetChrome(false);
            return;
        }
        if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            sheetDisplayState = SheetDisplayState.HALF;
            lastSheetSlideOffset = 0.5f;
            resultPanelMaxHeightPx = dpToPx(200);
            applySheetChrome(false);
            return;
        }
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetDisplayState = SheetDisplayState.SMALL;
            lastSheetSlideOffset = 0f;
            resultPanelMaxHeightPx = dpToPx(120);
            applySheetChrome(true);
        }
    }

    private void updateWindowForSheetState(int state) {
        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }
        Window window = ((BottomSheetDialog) getDialog()).getWindow();
        if (window == null) {
            return;
        }

        boolean compactState = state == BottomSheetBehavior.STATE_COLLAPSED
                || state == BottomSheetBehavior.STATE_HIDDEN
                || ((state == BottomSheetBehavior.STATE_DRAGGING
                || state == BottomSheetBehavior.STATE_SETTLING)
                && shouldKeepCompactWindowDuringTransition());
        window.setDimAmount(compactState ? 0f : 0.16f);
        window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                compactState ? dpToPx(132) : ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private boolean shouldKeepCompactWindowDuringTransition() {
        return sheetDisplayState == SheetDisplayState.SMALL && lastSheetSlideOffset <= 0f;
    }

    private void bindFieldInteractions(ActiveField fieldType) {
        View fieldContainer = getFieldContainer(fieldType);
        EditText field = getFieldInput(fieldType);
        ImageButton clearButton = getClearButton(fieldType);
        if (fieldContainer != null) {
            fieldContainer.setOnClickListener(v -> focusField(fieldType));
        }
        if (field != null) {
            field.setOnFocusChangeListener((v, hasFocus) -> handleFieldFocusChanged(fieldType, hasFocus));
            field.addTextChangedListener(new SimpleWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (isWatcherSuppressed(fieldType)) return;
                    handleFieldTextChanged(fieldType);
                }
            });
        }
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> clearFieldSelection(fieldType));
        }
    }

    private void handleFieldFocusChanged(ActiveField fieldType, boolean hasFocus) {
        if (isClosingDropdown) return;

        if (hasFocus) {
            activeField = fieldType;
            updateSuggestionList();
            return;
        }
        EditText otherField = getFieldInput(getOtherField(fieldType));
        if (otherField == null || !otherField.hasFocus()) {
            hideResults();
            updateClearButtons();
        }
    }

    private void handleFieldTextChanged(ActiveField fieldType) {
        if (isClosingDropdown) return;
        activeField = fieldType;
        clearSelectedId(fieldType);
        autoResolveSelection(fieldType);
        clearRouteFeedback(false);
        updateClearButtons();
        updateSuggestionList();
        updateStartState();
    }

    private void clearFieldSelection(ActiveField fieldType) {
        clearSelectedId(fieldType);
        EditText field = getFieldInput(fieldType);
        if (field != null) field.setText("");
        clearRouteFeedback(false);
        focusField(fieldType);
        updateClearButtons();
        updateStartState();
    }

    private void applySuggestionSelection(ActiveField fieldType, int id, String value, boolean roomBased) {
        assignSelectedId(fieldType, id, roomBased);
        setFieldText(getFieldInput(fieldType), value, fieldType == ActiveField.ORIGIN);
    }

    private void clearSelectedId(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) {
            originId = -1;
            selectedOriginRoomId = -1;
        }
        else if (fieldType == ActiveField.DESTINATION) selectedRoomId = -1;
    }

    private void assignSelectedId(ActiveField fieldType, int id, boolean roomBased) {
        if (fieldType == ActiveField.ORIGIN) {
            if (roomBased) {
                originId = -1;
                selectedOriginRoomId = id;
            } else {
                originId = id;
                selectedOriginRoomId = -1;
            }
        } else if (fieldType == ActiveField.DESTINATION) {
            selectedRoomId = id;
        }
    }

    private boolean isWatcherSuppressed(ActiveField fieldType) {
        return fieldType == ActiveField.ORIGIN ? suppressOriginWatcher : suppressDestinationWatcher;
    }

    @Nullable
    private EditText getFieldInput(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) return etOrigin;
        if (fieldType == ActiveField.DESTINATION) return etDestination;
        return null;
    }

    @Nullable
    private View getFieldContainer(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) return originFieldContainer;
        if (fieldType == ActiveField.DESTINATION) return destinationFieldContainer;
        return null;
    }

    @Nullable
    private ImageButton getClearButton(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) return btnClearOrigin;
        if (fieldType == ActiveField.DESTINATION) return btnClearDestination;
        return null;
    }

    @Nullable
    private View getResultsPanel(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) return originResults;
        if (fieldType == ActiveField.DESTINATION) return destinationResults;
        return null;
    }

    @NonNull
    private ActiveField getOtherField(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) return ActiveField.DESTINATION;
        if (fieldType == ActiveField.DESTINATION) return ActiveField.ORIGIN;
        return ActiveField.NONE;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void applyBottomSheetDepth(@NonNull View sheet, int elevationDp) {
        float elevationPx = dpToPx(elevationDp);
        sheet.setElevation(elevationPx);
        sheet.setTranslationZ(elevationPx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sheet.setOutlineAmbientShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_ambient));
            sheet.setOutlineSpotShadowColor(ContextCompat.getColor(requireContext(), R.color.bottom_sheet_shadow_spot));
        }
    }

    private void publishRouteOverlay(int roomId, int originId, int originRoomId, @NonNull RouteDto route) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_ROUTE_VISIBLE, true);
        result.putInt(KEY_ROUTE_ROOM_ID, roomId);
        result.putInt(KEY_ROUTE_ORIGIN_ID, originId);
        result.putInt(KEY_ROUTE_ORIGIN_ROOM_ID, originRoomId);
        result.putBoolean(KEY_ROUTE_USE_STAIRS, shouldUseStairs(route));
        result.putBoolean(KEY_ROUTE_USE_ELEVATOR, shouldUseElevator(route));
        getParentFragmentManager().setFragmentResult(RESULT_ROUTE_MAP_OVERLAY, result);
    }

    private void publishLocalRoomRouteOverlay(int originRoomId, int destinationRoomId) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_ROUTE_VISIBLE, true);
        result.putInt(KEY_ROUTE_ROOM_ID, destinationRoomId);
        result.putInt(KEY_ROUTE_ORIGIN_ID, -1);
        result.putInt(KEY_ROUTE_ORIGIN_ROOM_ID, originRoomId);
        result.putBoolean(KEY_ROUTE_USE_STAIRS, false);
        result.putBoolean(KEY_ROUTE_USE_ELEVATOR, false);
        getParentFragmentManager().setFragmentResult(RESULT_ROUTE_MAP_OVERLAY, result);
    }

    private void publishOriginRouteOverlay(int destinationRoomId, int originId) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_ROUTE_VISIBLE, true);
        result.putInt(KEY_ROUTE_ROOM_ID, destinationRoomId);
        result.putInt(KEY_ROUTE_ORIGIN_ID, originId);
        result.putInt(KEY_ROUTE_ORIGIN_ROOM_ID, -1);
        result.putBoolean(KEY_ROUTE_USE_STAIRS, false);
        result.putBoolean(KEY_ROUTE_USE_ELEVATOR, false);
        getParentFragmentManager().setFragmentResult(RESULT_ROUTE_MAP_OVERLAY, result);
    }

    private void clearRouteOverlay() {
        Bundle result = new Bundle();
        result.putBoolean(KEY_ROUTE_VISIBLE, false);
        getParentFragmentManager().setFragmentResult(RESULT_ROUTE_MAP_OVERLAY, result);
    }

    private void publishNavigationFocusState(boolean active) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_NAVIGATION_FOCUS_ACTIVE, active);
        getParentFragmentManager().setFragmentResult(RESULT_NAVIGATION_FOCUS, result);
    }

    private boolean shouldUseStairs(@Nullable RouteDto route) {
        String routeText = buildRouteText(route);
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
        appendRouteText(builder, route.description);
        appendRouteText(builder, route.instruction);
        if (route.steps != null) {
            for (RouteStepDto step : route.steps) {
                if (step == null) {
                    continue;
                }
                appendRouteText(builder, step.instruction);
                appendRouteText(builder, step.direction);
                appendRouteText(builder, step.landmark);
            }
        }
        return builder.toString();
    }

    private void appendRouteText(@NonNull StringBuilder builder, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        builder.append(' ').append(value.trim().toLowerCase(Locale.US));
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        clearRouteOverlay();
        publishNavigationFocusState(false);
        publishSheetVisibility(false);
        super.onDismiss(dialog);
    }

    private void publishSheetVisibility(boolean visible) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_SHEET_VISIBLE, visible);
        result.putInt(KEY_SHEET_SESSION_ID, sheetSessionId);
        getParentFragmentManager().setFragmentResult(RESULT_SHEET_VISIBILITY, result);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
