package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.ui.adapter.DirectionSearchAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
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

    private final List<OriginEntity> allOrigins = new ArrayList<>();
    private final List<RoomEntity> allRooms = new ArrayList<>();

    private DirectionSearchAdapter originAdapter;
    private DirectionSearchAdapter destinationAdapter;

    private View sheetRoot;
    private View resultsScrim;
    private View originFieldContainer;
    private View destinationFieldContainer;
    private EditText etOrigin;
    private EditText etDestination;
    private ImageButton btnClearOrigin;
    private ImageButton btnClearDestination;
    private LinearLayout originResults;
    private LinearLayout destinationResults;
    private RecyclerView originRecycler;
    private RecyclerView destinationRecycler;
    private View originEmpty;
    private View destinationEmpty;
    private MaterialButton btnStart;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private int originId = -1;
    private int selectedRoomId = -1;
    private int preselectedRoomId = -1;
    private String preselectedRoomName;
    private ActiveField activeField = ActiveField.NONE;
    private boolean suppressOriginWatcher;
    private boolean suppressDestinationWatcher;
    private int resultPanelMaxHeightPx;

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
                bottomSheetBehavior.setHideable(false);
                bottomSheetBehavior.setDraggable(true);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            resultPanelMaxHeightPx = dpToPx(360);
                        } else if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED
                                || newState == BottomSheetBehavior.STATE_COLLAPSED
                                || newState == BottomSheetBehavior.STATE_SETTLING) {
                            resultPanelMaxHeightPx = dpToPx(200);
                        }
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

        sheetRoot = view.findViewById(R.id.sheet_root);
        resultsScrim = view.findViewById(R.id.view_results_scrim);
        originFieldContainer = view.findViewById(R.id.layout_origin_field);
        destinationFieldContainer = view.findViewById(R.id.layout_destination_field);
        etOrigin = view.findViewById(R.id.et_origin_search);
        etDestination = view.findViewById(R.id.et_destination_search);
        btnClearOrigin = view.findViewById(R.id.btn_clear_origin);
        btnClearDestination = view.findViewById(R.id.btn_clear_destination);
        originResults = view.findViewById(R.id.layout_origin_results);
        destinationResults = view.findViewById(R.id.layout_destination_results);
        originEmpty = view.findViewById(R.id.tv_origin_empty);
        destinationEmpty = view.findViewById(R.id.tv_destination_empty);
        btnStart = view.findViewById(R.id.btn_start_directions);

        originAdapter = new DirectionSearchAdapter(this::onSuggestionPicked);
        destinationAdapter = new DirectionSearchAdapter(this::onSuggestionPicked);

        originRecycler = view.findViewById(R.id.recycler_origin_results);
        originRecycler.setLayoutManager(new LinearLayoutManager(activity));
        originRecycler.setAdapter(originAdapter);

        destinationRecycler = view.findViewById(R.id.recycler_destination_results);
        destinationRecycler.setLayoutManager(new LinearLayoutManager(activity));
        destinationRecycler.setAdapter(destinationAdapter);

        resultPanelMaxHeightPx = dpToPx(200);
        setupInputs();
        view.post(this::updateResultPanelHeights);

        btnStart.setOnClickListener(v -> {
            if (originId == -1) {
                Snackbar.make(view, R.string.error_origin_missing, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (selectedRoomId == -1) {
                Snackbar.make(view, R.string.error_destination_missing, Snackbar.LENGTH_LONG).show();
                return;
            }
            navigateToDirections(selectedRoomId, originId);
        });

        loadOriginsAndRooms();
    }

    private void setupInputs() {
        resultsScrim.setOnClickListener(v -> hideResultsAndClearFocus());
        originResults.setOnClickListener(v -> {
        });
        destinationResults.setOnClickListener(v -> {
        });

        originFieldContainer.setOnClickListener(v -> focusField(etOrigin, ActiveField.ORIGIN));
        destinationFieldContainer.setOnClickListener(v -> focusField(etDestination, ActiveField.DESTINATION));

        etOrigin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeField = ActiveField.ORIGIN;
                updateSuggestionList();
            } else if (!etDestination.hasFocus()) {
                hideResults();
            }
        });

        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeField = ActiveField.DESTINATION;
                updateSuggestionList();
            } else if (!etOrigin.hasFocus()) {
                hideResults();
            }
        });

        etOrigin.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressOriginWatcher) {
                    return;
                }
                activeField = ActiveField.ORIGIN;
                originId = -1;
                btnClearOrigin.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                updateSuggestionList();
                updateStartState();
            }
        });

        etDestination.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressDestinationWatcher) {
                    return;
                }
                activeField = ActiveField.DESTINATION;
                selectedRoomId = -1;
                btnClearDestination.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                updateSuggestionList();
                updateStartState();
            }
        });

        btnClearOrigin.setOnClickListener(v -> {
            originId = -1;
            etOrigin.setText("");
            focusField(etOrigin, ActiveField.ORIGIN);
            updateStartState();
        });

        btnClearDestination.setOnClickListener(v -> {
            selectedRoomId = -1;
            etDestination.setText("");
            focusField(etDestination, ActiveField.DESTINATION);
            updateStartState();
        });
    }

    private void onSuggestionPicked(DirectionSearchAdapter.SuggestionItem item) {
        if (item.isOrigin) {
            originId = item.id;
            setFieldText(etOrigin, item.title, true);
            btnClearOrigin.setVisibility(View.VISIBLE);
        } else {
            selectedRoomId = item.id;
            setFieldText(etDestination, item.title, false);
            btnClearDestination.setVisibility(View.VISIBLE);
        }
        hideResultsAndClearFocus();
        updateSuggestionList();
        updateStartState();
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
                if (origins != null) {
                    allOrigins.addAll(origins);
                }

                allRooms.clear();
                if (rooms != null) {
                    allRooms.addAll(rooms);
                }

                applyPreselectedRoom();
                updateSuggestionList();
                hideResults();
                updateStartState();
            });
        });
    }

    private void applyPreselectedRoom() {
        if (preselectedRoomId <= 0) {
            return;
        }
        for (RoomEntity room : allRooms) {
            if (room.id != preselectedRoomId) {
                continue;
            }
            selectedRoomId = room.id;
            String name = room.name != null && !room.name.trim().isEmpty()
                    ? room.name
                    : (preselectedRoomName != null && !preselectedRoomName.trim().isEmpty()
                    ? preselectedRoomName
                    : getString(R.string.label_destination));
            setFieldText(etDestination, name, false);
            btnClearDestination.setVisibility(View.VISIBLE);
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
            if (!matches(query, origin.name, origin.code, origin.description)) {
                continue;
            }
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
            if (!matches(query, room.name, room.code, room.location)) {
                continue;
            }
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
        if (originResults == null || destinationResults == null) {
            return;
        }
        boolean showOrigin = activeField == ActiveField.ORIGIN && etOrigin.hasFocus();
        boolean showDestination = activeField == ActiveField.DESTINATION && etDestination.hasFocus();
        originResults.setVisibility(showOrigin ? View.VISIBLE : View.GONE);
        destinationResults.setVisibility(showDestination ? View.VISIBLE : View.GONE);
        resultsScrim.setVisibility((showOrigin || showDestination) ? View.VISIBLE : View.GONE);
        if (resultsScrim.getVisibility() == View.VISIBLE) {
            resultsScrim.bringToFront();
            if (showOrigin) {
                originResults.bringToFront();
            } else if (showDestination) {
                destinationResults.bringToFront();
            }
        }
    }

    private void hideResults() {
        if (originResults != null) {
            originResults.setVisibility(View.GONE);
        }
        if (destinationResults != null) {
            destinationResults.setVisibility(View.GONE);
        }
        if (resultsScrim != null) {
            resultsScrim.setVisibility(View.GONE);
        }
        activeField = ActiveField.NONE;
    }

    private void hideResultsAndClearFocus() {
        hideResults();
        if (etOrigin != null) {
            etOrigin.clearFocus();
        }
        if (etDestination != null) {
            etDestination.clearFocus();
        }
        if (sheetRoot != null) {
            sheetRoot.requestFocus();
        }
    }

    private void focusField(EditText field, ActiveField fieldType) {
        if (fieldType == ActiveField.ORIGIN && etDestination != null) {
            etDestination.clearFocus();
        } else if (fieldType == ActiveField.DESTINATION && etOrigin != null) {
            etOrigin.clearFocus();
        }
        activeField = fieldType;
        field.requestFocus();
        field.setSelection(field.getText() != null ? field.getText().length() : 0);
        updateSuggestionList();
    }

    private void updateResultPanelHeights() {
        if (sheetRoot == null || btnStart == null) {
            return;
        }
        sheetRoot.post(() -> {
            setHeight(originResults, computeDropdownHeight(etOrigin, originAdapter != null ? originAdapter.getItemCount() : 0));
            setHeight(destinationResults, computeDropdownHeight(etDestination, destinationAdapter != null ? destinationAdapter.getItemCount() : 0));
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
        if (target == null) {
            return;
        }
        ViewGroup.LayoutParams params = target.getLayoutParams();
        if (params == null) {
            return;
        }
        params.height = heightPx;
        target.setLayoutParams(params);
    }

    private boolean matches(String query, String... values) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.US);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.US).contains(normalized)) {
                return true;
            }
        }
        return false;
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

    private void navigateToDirections(int roomId, int selectedOriginId) {
        if (!isAdded()) {
            return;
        }

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        Bundle args = new Bundle();
        args.putInt("roomId", roomId);
        args.putInt("originId", selectedOriginId);
        dismissAllowingStateLoss();
        navController.navigate(R.id.directionsFragment, args);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
