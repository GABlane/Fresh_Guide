package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private enum ActiveField {
        NONE,
        ORIGIN,
        DESTINATION
    }

    private enum SheetDisplayState {
        FULL,
        HALF,
        CLOSED
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
    private TextView routeOriginValue;
    private TextView routeDestinationValue;
    private TextView routeEmptyText;
    private ProgressBar routeLoading;
    private MaterialButton btnStart;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private int originId = -1;
    private int selectedRoomId = -1;
    private int preselectedRoomId = -1;
    private String preselectedRoomName;
    private ActiveField activeField = ActiveField.NONE;
    private SheetDisplayState sheetDisplayState = SheetDisplayState.HALF;
    private ContentMode contentMode = ContentMode.SUMMARY;
    private boolean dropdownVisible;
    private boolean suppressOriginWatcher;
    private boolean suppressDestinationWatcher;
    private int resultPanelMaxHeightPx;

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
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(0.12f);
            }
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                ViewGroup.LayoutParams params = sheet.getLayoutParams();
                if (params != null) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    sheet.setLayoutParams(params);
                }
                sheet.setElevation(dpToPx(22));
                bottomSheetBehavior = BottomSheetBehavior.from(sheet);
                bottomSheetBehavior.setFitToContents(false);
                bottomSheetBehavior.setExpandedOffset(0);
                bottomSheetBehavior.setHalfExpandedRatio(0.50f);
                bottomSheetBehavior.setSkipCollapsed(true);
                bottomSheetBehavior.setHideable(true);
                bottomSheetBehavior.setDraggable(true);
                sheet.post(() -> {
                    if (bottomSheetBehavior == null) return;
                    int initialState = contentMode == ContentMode.ROUTE
                            ? BottomSheetBehavior.STATE_EXPANDED
                            : BottomSheetBehavior.STATE_HALF_EXPANDED;
                    bottomSheetBehavior.setState(initialState);
                });
                bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        handleBottomSheetStateChanged(newState);
                        updateResultPanelHeights();
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
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
        }

        FragmentActivity activity = requireActivity();
        viewModel = new ViewModelProvider(this).get(DirectionsViewModel.class);

        sheetRoot = view.findViewById(R.id.sheet_root);
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
        routeOriginValue = view.findViewById(R.id.tv_route_origin_value);
        routeDestinationValue = view.findViewById(R.id.tv_route_destination_value);
        routeEmptyText = view.findViewById(R.id.tv_route_empty);
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
                showRouteEmptyState();
                return;
            }

            if (contentMode == ContentMode.ROUTE) {
                showSummaryContent();
            }
            Snackbar.make(rootView, err, Snackbar.LENGTH_LONG).show();
        });
    }

    private void startDirectionsInPlace(@NonNull View rootView) {
        if (originId == -1) {
            Snackbar.make(rootView, R.string.error_origin_missing, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (selectedRoomId == -1) {
            Snackbar.make(rootView, R.string.error_destination_missing, Snackbar.LENGTH_LONG).show();
            return;
        }

        hideResultsAndClearFocus();
        populateRouteHeader();
        showRouteLoadingState();
        expandRouteSheet();
        viewModel.loadRoute(selectedRoomId, originId);
    }

    private void showRouteLoadingState() {
        contentMode = ContentMode.ROUTE;
        routeAdapter.setSteps(Collections.emptyList());
        routeRecycler.setVisibility(View.GONE);
        routeEmptyState.setVisibility(View.GONE);
        routeLoading.setVisibility(View.VISIBLE);
        routeLoading.setAlpha(0f);
        routeLoading.animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        animateContentSwap(summaryContent, routeContent);
    }

    private void renderRoute(@NonNull RouteDto route) {
        List<RouteStepDto> steps = route.steps != null ? route.steps : Collections.emptyList();

        routeLoading.animate().cancel();
        routeLoading.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> routeLoading.setVisibility(View.GONE))
                .start();

        if (steps.isEmpty()) {
            showRouteEmptyState();
            return;
        }

        routeEmptyState.setVisibility(View.GONE);
        routeAdapter.setSteps(steps);
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

    private void showRouteEmptyState() {
        routeLoading.animate().cancel();
        routeLoading.setVisibility(View.GONE);
        routeAdapter.setSteps(Collections.emptyList());
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

    private void populateRouteHeader() {
        routeOriginValue.setText(safeDisplayText(textOf(etOrigin), R.string.label_origin));
        routeDestinationValue.setText(safeDisplayText(textOf(etDestination), R.string.label_destination));
    }

    private CharSequence safeDisplayText(String value, int fallbackResId) {
        String trimmed = value != null ? value.trim() : "";
        return trimmed.isEmpty() ? getString(fallbackResId) : trimmed;
    }

    private void expandRouteSheet() {
        if (bottomSheetBehavior == null) return;
        sheetRoot.post(() -> {
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    private void showSummaryContent() {
        contentMode = ContentMode.SUMMARY;
        routeLoading.animate().cancel();
        routeLoading.setVisibility(View.GONE);
        routeRecycler.animate().cancel();
        routeRecycler.setVisibility(View.GONE);
        routeEmptyState.animate().cancel();
        routeEmptyState.setVisibility(View.GONE);
        animateContentSwap(routeContent, summaryContent);
        updateStartState();
        if (bottomSheetBehavior != null) {
            sheetRoot.post(() -> {
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            });
        }
    }

    private void animateContentSwap(@Nullable View fromView, @NonNull View toView) {
        int enterOffset = dpToPx(18);
        int exitOffset = dpToPx(12);

        toView.animate().cancel();
        toView.setVisibility(View.VISIBLE);
        toView.setAlpha(0f);
        toView.setTranslationY(enterOffset);

        if (fromView != null && fromView.getVisibility() == View.VISIBLE) {
            fromView.animate().cancel();
            fromView.animate()
                    .alpha(0f)
                    .translationY(-exitOffset)
                    .setDuration(160)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        fromView.setVisibility(View.GONE);
                        fromView.setAlpha(1f);
                        fromView.setTranslationY(0f);
                    })
                    .start();
        }

        toView.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(40)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator())
                .start();
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
        if (contentMode != ContentMode.SUMMARY) {
            return;
        }

        hideResultsAndClearFocus();

        String previousOrigin = textOf(etOrigin);
        String previousDestination = textOf(etDestination);

        setFieldText(etOrigin, previousDestination, true);
        setFieldText(etDestination, previousOrigin, false);

        originId = resolveOriginId(previousDestination);
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
            applySuggestionSelection(ActiveField.ORIGIN, item.id, item.title);
        } else {
            applySuggestionSelection(ActiveField.DESTINATION, item.id, item.title);
        }


        forceHideAllDropdowns();


        clearAllFieldFocus();


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
                updateSuggestionList();
                hideResults();
                updateStartState();
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
                    false
            ));
        }
        return suggestions;
    }

    private void showActiveResults() {
        if (originResults == null || destinationResults == null || contentMode != ContentMode.SUMMARY) {
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

        if (isClosingDropdown || contentMode != ContentMode.SUMMARY) return;

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

    private int resolveOriginId(@Nullable String value) {
        String normalized = normalizeValue(value);
        if (normalized.isEmpty()) {
            return -1;
        }
        for (OriginEntity origin : allOrigins) {
            if (matchesExact(normalized, origin.name, origin.code, origin.description)) {
                return origin.id;
            }
        }
        return -1;
    }

    private int resolveRoomId(@Nullable String value) {
        String normalized = normalizeValue(value);
        if (normalized.isEmpty()) {
            return -1;
        }
        for (RoomEntity room : allRooms) {
            if (matchesExact(normalized, room.name, room.code, room.location)) {
                return room.id;
            }
        }
        return -1;
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
        boolean enabled = originId != -1 && selectedRoomId != -1;
        btnStart.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 1f : 0.7f);
    }

    private void handleBottomSheetStateChanged(int newState) {
        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            sheetDisplayState = SheetDisplayState.FULL;
            resultPanelMaxHeightPx = dpToPx(360);
            return;
        }
        if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            if (contentMode == ContentMode.ROUTE && bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                return;
            }
            sheetDisplayState = SheetDisplayState.HALF;
            resultPanelMaxHeightPx = dpToPx(200);
            return;
        }
        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            sheetDisplayState = SheetDisplayState.CLOSED;
            hideResultsAndClearFocus();
            dismissAllowingStateLoss();
            return;
        }
        if (newState == BottomSheetBehavior.STATE_COLLAPSED && bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
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
        if (isClosingDropdown || contentMode != ContentMode.SUMMARY) return;

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
        if (isClosingDropdown || contentMode != ContentMode.SUMMARY) return;
        activeField = fieldType;
        clearSelectedId(fieldType);
        updateClearButtons();
        updateSuggestionList();
        updateStartState();
    }

    private void clearFieldSelection(ActiveField fieldType) {
        clearSelectedId(fieldType);
        EditText field = getFieldInput(fieldType);
        if (field != null) field.setText("");
        focusField(fieldType);
        updateClearButtons();
        updateStartState();
    }

    private void applySuggestionSelection(ActiveField fieldType, int id, String value) {
        assignSelectedId(fieldType, id);
        setFieldText(getFieldInput(fieldType), value, fieldType == ActiveField.ORIGIN);
    }

    private void clearSelectedId(ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN) originId = -1;
        else if (fieldType == ActiveField.DESTINATION) selectedRoomId = -1;
    }

    private void assignSelectedId(ActiveField fieldType, int id) {
        if (fieldType == ActiveField.ORIGIN) originId = id;
        else if (fieldType == ActiveField.DESTINATION) selectedRoomId = id;
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

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
