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
import com.example.freshguide.model.dto.BuildingDto;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AdminBuildingListFragment extends Fragment {

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

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Buildings");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Reference structures mirrored on the home map and search experience.");

        adapter = new GenericListAdapter();
        adapter.setActionsEnabled(false);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        View fab = view.findViewById(R.id.fab_add);
        fab.setVisibility(View.GONE);

        Snackbar.make(view, "Building records are view-only to protect home visuals.", Snackbar.LENGTH_LONG).show();

        viewModel.getBuildings().observe(getViewLifecycleOwner(), buildings -> {
            if (buildings == null) return;
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (BuildingDto b : buildings) {
                items.add(new GenericListAdapter.Item(b.id, b.name, b.code));
            }
            adapter.setItems(items);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
        });

        viewModel.loadBuildings();
    }
}
