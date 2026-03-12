package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.ui.adapter.DirectionRoomAdapter;
import com.example.freshguide.ui.adapter.RouteStepAdapter;
import com.example.freshguide.viewmodel.DirectionsViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;

public class DirectionsSheetFragment extends BottomSheetDialogFragment {

    private DirectionsViewModel viewModel;
    private DirectionRoomAdapter roomAdapter;
    private RouteStepAdapter stepAdapter;

    private TextView tvOrigin;
    private TextView tvOriginHint;
    private TextView tvDestination;
    private TextView tvRoomsEmpty;
    private TextView tvStepsEmpty;
    private MaterialButton btnStart;
    private ProgressBar progressBar;

    private int originId = -1;
    private int selectedRoomId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_directions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = requireActivity();
        viewModel = new ViewModelProvider(this).get(DirectionsViewModel.class);

        tvOrigin = view.findViewById(R.id.tv_origin_value);
        tvOriginHint = view.findViewById(R.id.tv_origin_hint);
        tvDestination = view.findViewById(R.id.tv_destination_value);
        tvRoomsEmpty = view.findViewById(R.id.tv_rooms_empty);
        tvStepsEmpty = view.findViewById(R.id.tv_steps_empty);
        btnStart = view.findViewById(R.id.btn_start_directions);
        progressBar = view.findViewById(R.id.progress_bar);

        roomAdapter = new DirectionRoomAdapter();
        RecyclerView roomRecycler = view.findViewById(R.id.recycler_rooms);
        roomRecycler.setLayoutManager(new LinearLayoutManager(activity));
        roomRecycler.setAdapter(roomAdapter);

        stepAdapter = new RouteStepAdapter();
        RecyclerView stepsRecycler = view.findViewById(R.id.recycler_route_steps);
        stepsRecycler.setLayoutManager(new LinearLayoutManager(activity));
        stepsRecycler.setAdapter(stepAdapter);

        roomAdapter.setOnRoomSelectedListener(room -> {
            selectedRoomId = room.id;
            tvDestination.setText(room.name != null ? room.name : getString(R.string.label_destination));
            tvDestination.setTextColor(requireContext().getColor(R.color.text_primary));
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
            tvStepsEmpty.setVisibility(View.GONE);
            viewModel.loadRoute(selectedRoomId, originId);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getRoute().observe(getViewLifecycleOwner(), route -> {
            if (route == null || route.steps == null || route.steps.isEmpty()) {
                stepAdapter.setSteps(null);
                tvStepsEmpty.setVisibility(View.VISIBLE);
                return;
            }
            tvStepsEmpty.setVisibility(View.GONE);
            stepAdapter.setSteps(route.steps);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                tvStepsEmpty.setVisibility(View.VISIBLE);
                Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
            }
        });

        loadOriginsAndRooms();
    }

    private void loadOriginsAndRooms() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<OriginEntity> origins = db.originDao().getAllSync();
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();

            OriginEntity chosen = null;
            if (origins != null) {
                for (OriginEntity o : origins) {
                    if (o.name != null && o.name.toLowerCase().contains("entrance")) {
                        chosen = o;
                        break;
                    }
                    if (o.code != null && o.code.equalsIgnoreCase("ENT")) {
                        chosen = o;
                    }
                }
                if (chosen == null && !origins.isEmpty()) chosen = origins.get(0);
            }

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
                }
                updateStartState();
            });
        });
    }

    private void updateStartState() {
        boolean enabled = originId != -1 && selectedRoomId != -1;
        btnStart.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 1f : 0.6f);
    }
}
