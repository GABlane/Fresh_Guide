package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.ui.adapter.RouteStepAdapter;
import com.example.freshguide.viewmodel.DirectionsViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class DirectionsFragment extends Fragment {

    private DirectionsViewModel viewModel;
    private RouteStepAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_directions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Checklist 5.7: receive route data via args
        int roomId = requireArguments().getInt("roomId", -1);
        int originId = requireArguments().getInt("originId", -1);

        viewModel = new ViewModelProvider(this).get(DirectionsViewModel.class);

        adapter = new RouteStepAdapter();
        RecyclerView recycler = view.findViewById(R.id.recycler_steps);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        TextView tvRouteName = view.findViewById(R.id.tv_route_name);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE));

        viewModel.getRoute().observe(getViewLifecycleOwner(), route -> {
            if (route == null) return;
            adapter.setSteps(route.steps);
        });

        viewModel.getRouteTitle().observe(getViewLifecycleOwner(), title -> {
            if (title != null && !title.trim().isEmpty()) {
                tvRouteName.setText(title);
            } else {
                tvRouteName.setText("Directions");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        if (roomId != -1 && originId != -1) {
            viewModel.loadRoute(roomId, originId);
        } else if (roomId != -1) {
            resolvePreferredOriginAndLoad(roomId, view);
        }
    }

    private void resolvePreferredOriginAndLoad(int roomId, @NonNull View view) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
            List<OriginEntity> origins = db.originDao().getAllSync();
            List<RouteEntity> routesForRoom = db.routeDao().getRoutesForRoomSync(roomId);

            OriginEntity chosen = null;
            if (routesForRoom != null && !routesForRoom.isEmpty() && origins != null && !origins.isEmpty()) {
                Set<Integer> routeOriginIds = new HashSet<>();
                for (RouteEntity route : routesForRoom) {
                    if (route != null) {
                        routeOriginIds.add(route.originId);
                    }
                }

                for (OriginEntity origin : origins) {
                    if (origin == null || !routeOriginIds.contains(origin.id)) {
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
                        if (origin == null || !routeOriginIds.contains(origin.id)) {
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
                        if (origin != null && routeOriginIds.contains(origin.id)) {
                            chosen = origin;
                            break;
                        }
                    }
                }
            }

            if (chosen == null && origins != null && !origins.isEmpty()) {
                for (OriginEntity origin : origins) {
                    if (origin == null) {
                        continue;
                    }
                    String name = origin.name != null ? origin.name.toLowerCase(Locale.US) : "";
                    String code = origin.code != null ? origin.code.toUpperCase(Locale.US) : "";
                    if (name.contains("gate") || "GATE".equals(code)) {
                        chosen = origin;
                        break;
                    }
                }
            }

            if (chosen == null && origins != null && !origins.isEmpty()) {
                for (OriginEntity origin : origins) {
                    if (origin == null) {
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

            if (chosen == null && origins != null && !origins.isEmpty()) {
                chosen = origins.get(0);
            }

            OriginEntity finalChosen = chosen;
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                if (finalChosen == null) {
                    Snackbar.make(view, "No starting point available", Snackbar.LENGTH_LONG).show();
                    return;
                }
                viewModel.loadRoute(roomId, finalChosen.id);
            });
        });
    }
}
