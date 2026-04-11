package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AdminRouteListFragment extends Fragment {

    private AdminViewModel viewModel;
    private GenericListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        AdminNavigationUtils.bindBackToDashboard(this, view);

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Routes");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Review and update the route guidance students follow in directions.");

        adapter = new GenericListAdapter();
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                Bundle args = new Bundle();
                args.putInt("routeId", id);
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminRouteList_to_adminRouteForm, args);
            }

            @Override
            public void onDelete(int position, int id) {
                AdminDialogUtils.showDestructiveConfirmation(
                        AdminRouteListFragment.this,
                        "Delete Route",
                        "Are you sure?",
                        "Delete",
                        () -> viewModel.deleteRoute(id)
                );
            }
        });

        View fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminRouteList_to_adminRouteForm));

        viewModel.getRoutes().observe(getViewLifecycleOwner(), routeList -> {
            if (routeList == null) return;
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (RouteDto r : routeList) {
                String originName = (r.origin != null && r.origin.name != null && !r.origin.name.trim().isEmpty())
                        ? r.origin.name.trim()
                        : "Origin " + r.originId;
                String roomName = (r.destinationRoom != null && r.destinationRoom.name != null
                        && !r.destinationRoom.name.trim().isEmpty())
                        ? r.destinationRoom.name.trim()
                        : "Room " + r.destinationRoomId;
                String displayName = originName + " → " + roomName;
                String subtitle = (r.description != null && !r.description.trim().isEmpty())
                        ? r.description.trim()
                        : (r.instruction != null && !r.instruction.trim().isEmpty()
                            ? r.instruction.trim()
                            : "No description");
                items.add(new GenericListAdapter.Item(r.id, displayName, subtitle));
            }
            adapter.setItems(items);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
        });

        viewModel.loadRoutes();
    }
}
