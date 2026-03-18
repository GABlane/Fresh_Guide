package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.ui.adapter.DirectionRoomAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class DirectionsSheetFragment extends BottomSheetDialogFragment {

    public static final String ARG_PRESELECTED_ROOM_ID = "preselectedRoomId";
    public static final String ARG_PRESELECTED_ROOM_NAME = "preselectedRoomName";

    private DirectionRoomAdapter roomAdapter;

    private TextView tvOrigin;
    private TextView tvOriginHint;
    private TextView tvDestination;
    private TextView tvRoomsEmpty;
    private MaterialButton btnStart;

    private int originId = -1;
    private int selectedRoomId = -1;
    private int preselectedRoomId = -1;
    private String preselectedRoomName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_directions, container, false);
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

        tvOrigin = view.findViewById(R.id.tv_origin_value);
        tvOriginHint = view.findViewById(R.id.tv_origin_hint);
        tvDestination = view.findViewById(R.id.tv_destination_value);
        tvRoomsEmpty = view.findViewById(R.id.tv_rooms_empty);
        btnStart = view.findViewById(R.id.btn_start_directions);

        roomAdapter = new DirectionRoomAdapter();
        RecyclerView roomRecycler = view.findViewById(R.id.recycler_rooms);
        roomRecycler.setLayoutManager(new LinearLayoutManager(activity));
        roomRecycler.setAdapter(roomAdapter);

        roomAdapter.setOnRoomSelectedListener(room -> {
            selectedRoomId = room.id;
            tvDestination.setText(room.name != null ? room.name : getString(R.string.label_destination));
            tvDestination.setTextColor(requireContext().getColor(R.color.text_primary));
            resolveOriginForSelectedRoom();
            updateStartState();
        });

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

    private void loadOriginsAndRooms() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<OriginEntity> origins = db.originDao().getAllSync();
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();

            OriginEntity chosen = pickPreferredOrigin(origins);

            OriginEntity finalChosen = chosen;
            requireActivity().runOnUiThread(() -> {
                if (finalChosen != null) {
                    originId = finalChosen.id;
                    tvOrigin.setText(finalChosen.name != null ? finalChosen.name : getString(R.string.default_origin));
                    tvOriginHint.setVisibility(View.GONE);
                } else {
                    originId = -1;
                    tvOrigin.setText(R.string.default_origin);
                    tvOriginHint.setText(R.string.hint_origin_missing);
                    tvOriginHint.setVisibility(View.VISIBLE);
                }

                if (rooms == null || rooms.isEmpty()) {
                    roomAdapter.setRooms(null);
                    tvRoomsEmpty.setVisibility(View.VISIBLE);
                } else {
                    roomAdapter.setRooms(rooms);
                    tvRoomsEmpty.setVisibility(View.GONE);
                    applyPreselectedRoom(rooms);
                }
                if (selectedRoomId > 0) {
                    resolveOriginForSelectedRoom();
                }
                updateStartState();
            });
        });
    }

    private OriginEntity pickPreferredOrigin(List<OriginEntity> origins) {
        if (origins == null || origins.isEmpty()) {
            return null;
        }

        for (OriginEntity origin : origins) {
            if (origin == null) {
                continue;
            }
            String name = origin.name != null ? origin.name.toLowerCase(Locale.US) : "";
            String code = origin.code != null ? origin.code.toUpperCase(Locale.US) : "";
            if (name.contains("gate") || "GATE".equals(code)) {
                return origin;
            }
        }

        for (OriginEntity origin : origins) {
            if (origin == null) {
                continue;
            }
            String name = origin.name != null ? origin.name.toLowerCase(Locale.US) : "";
            String code = origin.code != null ? origin.code.toUpperCase(Locale.US) : "";
            if (name.contains("entrance") || "ENT".equals(code)) {
                return origin;
            }
        }

        return origins.get(0);
    }

    private void resolveOriginForSelectedRoom() {
        int roomId = selectedRoomId;
        if (roomId <= 0 || !isAdded()) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
            List<OriginEntity> origins = db.originDao().getAllSync();
            List<RouteEntity> routesForRoom = db.routeDao().getRoutesForRoomSync(roomId);

            OriginEntity chosen = null;
            if (routesForRoom != null && !routesForRoom.isEmpty()) {
                Set<Integer> originIds = new HashSet<>();
                for (RouteEntity route : routesForRoom) {
                    if (route != null) {
                        originIds.add(route.originId);
                    }
                }

                if (origins != null && !origins.isEmpty()) {
                    for (OriginEntity origin : origins) {
                        if (origin == null || !originIds.contains(origin.id)) {
                            continue;
                        }
                        String name = origin.name != null ? origin.name.toLowerCase(Locale.US) : "";
                        String code = origin.code != null ? origin.code.toUpperCase(Locale.US) : "";
                        if (name.contains("gate") || "GATE".equals(code)) {
                            chosen = origin;
                            break;
                        }
                    }

                    if (chosen == null) {
                        for (OriginEntity origin : origins) {
                            if (origin == null || !originIds.contains(origin.id)) {
                                continue;
                            }
                            String name = origin.name != null ? origin.name.toLowerCase(Locale.US) : "";
                            String code = origin.code != null ? origin.code.toUpperCase(Locale.US) : "";
                            if (name.contains("entrance") || "ENT".equals(code)) {
                                chosen = origin;
                                break;
                            }
                        }
                    }

                    if (chosen == null) {
                        for (OriginEntity origin : origins) {
                            if (origin != null && originIds.contains(origin.id)) {
                                chosen = origin;
                                break;
                            }
                        }
                    }
                }
            }

            if (chosen == null) {
                chosen = pickPreferredOrigin(origins);
            }

            OriginEntity finalChosen = chosen;
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || finalChosen == null || selectedRoomId != roomId) {
                    return;
                }
                originId = finalChosen.id;
                tvOrigin.setText(finalChosen.name != null ? finalChosen.name : getString(R.string.default_origin));
                tvOriginHint.setVisibility(View.GONE);
                updateStartState();
            });
        });
    }

    private void applyPreselectedRoom(@NonNull List<RoomEntity> rooms) {
        if (preselectedRoomId <= 0) {
            return;
        }

        for (RoomEntity room : rooms) {
            if (room.id != preselectedRoomId) {
                continue;
            }
            selectedRoomId = room.id;
            roomAdapter.setSelectedRoomId(room.id);
            String name = room.name != null && !room.name.trim().isEmpty()
                    ? room.name
                    : (preselectedRoomName != null && !preselectedRoomName.trim().isEmpty()
                        ? preselectedRoomName
                        : getString(R.string.label_destination));
            tvDestination.setText(name);
            tvDestination.setTextColor(requireContext().getColor(R.color.text_primary));
            break;
        }
    }

    private void updateStartState() {
        boolean enabled = originId != -1 && selectedRoomId != -1;
        btnStart.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 1f : 0.6f);
    }
}
