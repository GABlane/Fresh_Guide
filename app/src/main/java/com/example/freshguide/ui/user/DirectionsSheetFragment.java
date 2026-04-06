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
import android.widget.TextView;

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
        ORIGIN,
        DESTINATION
    }

    private final List<OriginEntity> allOrigins = new ArrayList<>();
    private final List<RoomEntity> allRooms = new ArrayList<>();

    private DirectionSearchAdapter searchAdapter;

    private EditText etOrigin;
    private EditText etDestination;
    private ImageButton btnClearOrigin;
    private ImageButton btnClearDestination;
    private TextView tvSearchContext;
    private TextView tvRoomsEmpty;
    private MaterialButton btnStart;

    private int originId = -1;
    private int selectedRoomId = -1;
    private int preselectedRoomId = -1;
    private String preselectedRoomName;
    private ActiveField activeField = ActiveField.ORIGIN;
    private boolean suppressOriginWatcher;
    private boolean suppressDestinationWatcher;

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
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setFitToContents(false);
                behavior.setExpandedOffset(dpToPx(18));
                behavior.setHalfExpandedRatio(0.62f);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(false);
                behavior.setDraggable(true);
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
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

        etOrigin = view.findViewById(R.id.et_origin_search);
        etDestination = view.findViewById(R.id.et_destination_search);
        btnClearOrigin = view.findViewById(R.id.btn_clear_origin);
        btnClearDestination = view.findViewById(R.id.btn_clear_destination);
        tvSearchContext = view.findViewById(R.id.tv_search_context);
        tvRoomsEmpty = view.findViewById(R.id.tv_rooms_empty);
        btnStart = view.findViewById(R.id.btn_start_directions);

        searchAdapter = new DirectionSearchAdapter(this::onSuggestionPicked);
        RecyclerView roomRecycler = view.findViewById(R.id.recycler_rooms);
        roomRecycler.setLayoutManager(new LinearLayoutManager(activity));
        roomRecycler.setAdapter(searchAdapter);

        setupInputs();

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
        etOrigin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeField = ActiveField.ORIGIN;
                updateSuggestionList();
            }
        });
        etDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeField = ActiveField.DESTINATION;
                updateSuggestionList();
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
            suppressOriginWatcher = false;
            originId = -1;
            etOrigin.setText("");
            etOrigin.requestFocus();
            updateStartState();
        });

        btnClearDestination.setOnClickListener(v -> {
            suppressDestinationWatcher = false;
            selectedRoomId = -1;
            etDestination.setText("");
            etDestination.requestFocus();
            updateStartState();
        });
    }

    private void onSuggestionPicked(DirectionSearchAdapter.SuggestionItem item) {
        if (item.isOrigin) {
            originId = item.id;
            setFieldText(etOrigin, item.title, true);
            btnClearOrigin.setVisibility(View.VISIBLE);
            activeField = ActiveField.DESTINATION;
            etDestination.requestFocus();
        } else {
            selectedRoomId = item.id;
            setFieldText(etDestination, item.title, false);
            btnClearDestination.setVisibility(View.VISIBLE);
        }
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
        List<DirectionSearchAdapter.SuggestionItem> suggestions = new ArrayList<>();
        String query = activeField == ActiveField.ORIGIN
                ? textOf(etOrigin)
                : textOf(etDestination);

        if (activeField == ActiveField.ORIGIN) {
            tvSearchContext.setText("Select origin");
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
        } else {
            tvSearchContext.setText("Select destination");
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
        }

        searchAdapter.submitList(suggestions);
        tvRoomsEmpty.setVisibility(suggestions.isEmpty() ? View.VISIBLE : View.GONE);
        tvRoomsEmpty.setText(activeField == ActiveField.ORIGIN
                ? "No origins found"
                : getString(R.string.label_no_rooms));
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
        return code != null ? location + " • " + code : location;
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
        btnStart.setAlpha(enabled ? 1f : 0.6f);
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
