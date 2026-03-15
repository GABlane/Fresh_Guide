package com.example.freshguide.ui.user;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.ui.view.CampusMapView;
import com.example.freshguide.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private CampusMapView campusMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        campusMap = view.findViewById(R.id.campus_map);

        NavController nav = Navigation.findNavController(view);

        setupSearch(view, nav);
        setupFloorChips(view);
        setupCampusMap(nav);
        setupFab(view);
        observeSync(view);

        viewModel.sync();
    }

    private void setupSearch(View view, NavController nav) {
        view.findViewById(R.id.layout_search).setOnClickListener(
                v -> nav.navigate(R.id.action_home_to_roomList));
    }

    private void setupFloorChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_floors);
        NavController nav = Navigation.findNavController(view);

        viewModel.getFloorNumbers().observe(getViewLifecycleOwner(), floors -> {
            chipGroup.removeAllViews();

            List<Integer> numbers = new ArrayList<>();
            if (floors != null) {
                for (Integer num : floors.keySet()) {
                    if (num != null && num >= 1) numbers.add(num);
                }
            }
            if (numbers.isEmpty()) {
                for (int i = 1; i <= 5; i++) numbers.add(i);
            }
            Collections.sort(numbers);

            for (int i = 0; i < numbers.size(); i++) {
                int floorNum = numbers.get(i);
                Chip chip = makeFilterChip(floorLabel(floorNum));
                
                // Add click handler for navigation to floor
                chip.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("buildingCode", "MAIN");
                    args.putString("buildingName", "Main Building");
                    args.putInt("selectedFloor", floorNum);
                    nav.navigate(R.id.action_home_to_floorLayout, args);
                });
                
                chipGroup.addView(chip);
            }
        });
    }

    /** Outlined pill-style chip matching the design. */
    private Chip makeFilterChip(String label) {
        Chip chip = new Chip(requireContext(),
                null, com.google.android.material.R.attr.chipStyle);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);

        float density = getResources().getDisplayMetrics().density;
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTextSize(11f);
        chip.setChipMinHeight(28f * density);
        chip.setChipStartPadding(12f * density);
        chip.setChipEndPadding(12f * density);
        chip.setEnsureMinTouchTargetSize(false);

        // Outline: green border, transparent fill when unchecked; filled when checked
        int green = requireContext().getColor(R.color.green_primary);

        chip.setChipStrokeColorResource(R.color.green_primary);
        chip.setChipStrokeWidth(1.2f * density);

        // Background state list: checked = green, unchecked = white
        ColorStateList bg = new ColorStateList(
                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                new int[]{ green, Color.WHITE }
        );
        chip.setChipBackgroundColor(bg);

        // Text color: checked = white, unchecked = green
        ColorStateList tc = new ColorStateList(
                new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                new int[]{ Color.WHITE, green }
        );
        chip.setTextColor(tc);

        return chip;
    }

    private String floorLabel(int number) {
        return number + ordinalSuffix(number) + " Floor";
    }

    private String ordinalSuffix(int number) {
        if (number >= 11 && number <= 13) return "th";
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private void setupCampusMap(NavController nav) {
        campusMap.setOnBuildingClickListener((code, name) -> {
            Bundle args = new Bundle();
            args.putString("buildingCode", code);
            args.putString("buildingName", name);
            if ("MAIN".equalsIgnoreCase(code)) {
                nav.navigate(R.id.action_home_to_floorLayout, args);
            } else {
                nav.navigate(R.id.action_home_to_roomList, args);
            }
        });
    }

    /** Re-centres the map to its default zoom/pan. */
    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_compass);
        fab.setOnClickListener(v ->
                new DirectionsSheetFragment().show(getParentFragmentManager(), "directions_sheet"));
        fab.setOnLongClickListener(v -> {
            campusMap.resetView();
            return true;
        });
    }

    private void observeSync(View view) {
        viewModel.getSyncError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Snackbar.make(view, "Sync failed: " + err, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
