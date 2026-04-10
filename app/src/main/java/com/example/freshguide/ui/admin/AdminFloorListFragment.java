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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.FloorDto;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminFloorListFragment extends Fragment {

    private AdminViewModel viewModel;
    private GenericListAdapter adapter;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private List<FloorDto> currentFloors = new ArrayList<>();

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
                .setText("View floor layouts using the same map screen students use.");

        adapter = new GenericListAdapter();
        adapter.setActionsEnabled(false);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        adapter.setOnItemClickListener((position, item) -> {
            if (position < 0 || position >= currentFloors.size()) {
                return;
            }
            openFloorLayout(currentFloors.get(position));
        });

        view.findViewById(R.id.fab_add).setVisibility(View.GONE);

        viewModel.getFloors().observe(getViewLifecycleOwner(), floorList -> {
            if (floorList == null) return;
            currentFloors = floorList;
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

    private void openFloorLayout(FloorDto floor) {
        ioExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
            BuildingEntity building = db.buildingDao().getByIdSync(floor.buildingId);

            String buildingCode = (building != null && building.code != null && !building.code.trim().isEmpty())
                    ? building.code
                    : "MAIN";
            String buildingName = (building != null && building.name != null && !building.name.trim().isEmpty())
                    ? building.name
                    : "Main Building";

            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                Bundle args = new Bundle();
                args.putString("buildingCode", buildingCode);
                args.putString("buildingName", buildingName);
                args.putInt("selectedFloor", floor.number);
                NavHostFragment.findNavController(this).navigate(R.id.floorLayoutFragment, args);
            });
        });
    }
}
