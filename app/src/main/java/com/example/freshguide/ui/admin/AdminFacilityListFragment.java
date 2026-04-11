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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.FacilityDto;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AdminFacilityListFragment extends Fragment {

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

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Facilities");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Review the facility tags students see in room details and related guidance.");

        adapter = new GenericListAdapter();
        adapter.setEditEnabled(false);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // No facility creation form — hide FAB
        view.findViewById(R.id.fab_add).setVisibility(View.GONE);

        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                // No facility form — edit not implemented
            }

            @Override
            public void onDelete(int position, int id) {
                AdminDialogUtils.showDestructiveConfirmation(
                        AdminFacilityListFragment.this,
                        "Delete Facility",
                        "Are you sure?",
                        "Delete",
                        () -> viewModel.deleteFacility(id)
                );
            }
        });

        viewModel.getFacilities().observe(getViewLifecycleOwner(), facilityList -> {
            if (facilityList == null) return;
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (FacilityDto f : facilityList) {
                items.add(new GenericListAdapter.Item(f.id, f.name, f.icon != null ? f.icon : ""));
            }
            adapter.setItems(items);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
        });

        viewModel.loadFacilities();
    }
}
