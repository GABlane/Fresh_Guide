package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.FloorDto;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AdminFloorListFragment extends Fragment {

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

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Floors");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Manage the floor metadata used across filters, maps, and room placement.");

        adapter = new GenericListAdapter();
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                Bundle args = new Bundle();
                args.putInt("floorId", id);
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminFloorList_to_adminFloorForm, args);
            }

            @Override
            public void onDelete(int position, int id) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Floor")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (d, w) -> viewModel.deleteFloor(id))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminFloorList_to_adminFloorForm));

        viewModel.getFloors().observe(getViewLifecycleOwner(), floorList -> {
            if (floorList == null) return;
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (FloorDto f : floorList) {
                items.add(new GenericListAdapter.Item(f.id, f.name, "Floor " + f.number));
            }
            adapter.setItems(items);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
        });

        viewModel.loadFloors();
    }
}
