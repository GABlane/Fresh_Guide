package com.example.freshguide.ui.user;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

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
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final long CHIP_ANIM_DURATION_MS = 300L;
    private static final String CODE_MAIN = "MAIN";
    private static final String CODE_REG = "REG";
    private static final String CODE_LIB = "LIB";
    private static final String CODE_COURT = "COURT";
    private static final String CODE_ENT = "ENT";
    private static final String CODE_EXIT = "EXIT";
    private Integer selectedFloor = null;


    private HomeViewModel viewModel;
    private CampusMapView campusMap;
    private HorizontalScrollView floorChipContainer;
    private View leftFade;
    private View rightFade;
    private String selectedBuildingCode;

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
        // campusMap = view.findViewById(R.id.campus_map);

        floorChipContainer = view.findViewById(R.id.floor_chip_container);
        leftFade = view.findViewById(R.id.leftFade);
        rightFade = view.findViewById(R.id.rightFade);

        NavController nav = Navigation.findNavController(view);

        setupSearch(view, nav);
        setupFloorChips(view);
        setupChipFade();
        // setupCampusMap(nav);
        setupFab(view);
        observeSync(view);

        viewModel.sync();
    }

    private void setupSearch(View view, NavController nav) {
        view.findViewById(R.id.layout_search).setOnClickListener(
                v -> nav.navigate(R.id.action_home_to_roomList));
    }

    private void setupFloorChips(View view) {
        Chip chip1 = view.findViewById(R.id.chip_floor_1);
        Chip chip2 = view.findViewById(R.id.chip_floor_2);
        Chip chip3 = view.findViewById(R.id.chip_floor_3);
        Chip chip4 = view.findViewById(R.id.chip_floor_4);
        Chip chip5 = view.findViewById(R.id.chip_floor_5);

        Chip[] chips = {chip1, chip2, chip3, chip4, chip5};

        int green = requireContext().getColor(R.color.green);

        ColorStateList bgColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        green,
                        Color.WHITE
                }
        );

        ColorStateList textColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        Color.WHITE,
                        green
                }
        );

        for (Chip chip : chips) {
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(bgColors);
            chip.setTextColor(textColors);
            chip.setChipStrokeColor(ColorStateList.valueOf(green));
//            chip.setChipStrokeWidth(dpToPx(1.5f));
        }

        chip1.setOnClickListener(v -> handleFloorChipClick(chip1, 1, chips));
        chip2.setOnClickListener(v -> handleFloorChipClick(chip2, 2, chips));
        chip3.setOnClickListener(v -> handleFloorChipClick(chip3, 3, chips));
        chip4.setOnClickListener(v -> handleFloorChipClick(chip4, 4, chips));
        chip5.setOnClickListener(v -> handleFloorChipClick(chip5, 5, chips));

        showOverallMap();
        updateFade();
    }

    private void handleFloorChipClick(Chip clickedChip, int floor, Chip[] allChips) {
        if (selectedFloor != null && selectedFloor == floor) {
            clickedChip.setChecked(false);
            selectedFloor = null;
            showOverallMap();
            return;
        }

        for (Chip chip : allChips) {
            chip.setChecked(false);
        }

        clickedChip.setChecked(true);
        selectedFloor = floor;
        showFloorMap(floor);
    }

    private void showOverallMap() {
        // default campus map here
    }

    private void showFloorMap(int floor) {
        switch (floor) {
            case 1:
                // show 1st floor map
                break;
            case 2:
                // show 2nd floor map
                break;
            case 3:
                // show 3rd floor map
                break;
            case 4:
                // show 4th floor map
                break;
            case 5:
                // show 5th floor map
                break;
        }
    }

    private void openFloor(NavController nav, int floorNum) {
        if (!CODE_MAIN.equalsIgnoreCase(selectedBuildingCode)) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("buildingCode", CODE_MAIN);
        args.putString("buildingName", "Main Building");
        args.putInt("selectedFloor", floorNum);
        nav.navigate(R.id.action_home_to_floorLayout, args);
    }

//    private Chip makeFilterChip(String label) {
//        Chip chip = new Chip(requireContext(),
//                null, com.google.android.material.R.attr.chipStyle);
//
//        chip.setText(label);
//        chip.setCheckable(true);
//        chip.setCheckedIconVisible(false);
//
//        float density = getResources().getDisplayMetrics().density;
//
//        chip.setEnsureMinTouchTargetSize(false);
//        chip.setTextSize(11f);
//        chip.setChipMinHeight(48f * density);
//        chip.setChipStartPadding(18f * density);
//        chip.setChipEndPadding(18f * density);
//
//        int green = requireContext().getColor(R.color.green_primary);
//
//        chip.setChipStrokeColorResource(R.color.green_primary);
//        chip.setChipStrokeWidth(1.2f * density);
//        chip.setChipCornerRadius(24f * density);
//
//        ColorStateList bg = new ColorStateList(
//                new int[][]{
//                        new int[]{android.R.attr.state_checked},
//                        new int[]{}
//                },
//                new int[]{
//                        green,
//                        Color.WHITE
//                }
//        );
//        chip.setChipBackgroundColor(bg);
//
//        ColorStateList tc = new ColorStateList(
//                new int[][]{
//                        new int[]{android.R.attr.state_checked},
//                        new int[]{}
//                },
//                new int[]{
//                        Color.WHITE,
//                        green
//                }
//        );
//        chip.setTextColor(tc);
//
//        return chip;
//    }

    private void setupChipFade() {
        if (floorChipContainer == null) return;

        floorChipContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        updateFade();
                    }
                }
        );

        floorChipContainer.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                updateFade();
            }
        });
    }

    private void updateFade() {
        if (floorChipContainer == null || leftFade == null || rightFade == null) return;

        View child = floorChipContainer.getChildAt(0);
        if (child == null) return;

        int maxScroll = child.getWidth() - floorChipContainer.getWidth();
        int scrollX = floorChipContainer.getScrollX();

        if (maxScroll <= 0) {
            leftFade.setVisibility(View.GONE);
            rightFade.setVisibility(View.GONE);
            return;
        }

        leftFade.setVisibility(scrollX > 0 ? View.VISIBLE : View.GONE);
        rightFade.setVisibility(scrollX < maxScroll ? View.VISIBLE : View.GONE);
    }

//    private String floorLabel(int number) {
//        return number + ordinalSuffix(number) + " Floor";
//    }

//    private String ordinalSuffix(int number) {
//        if (number >= 11 && number <= 13) return "th";
//        switch (number % 10) {
//            case 1: return "st";
//            case 2: return "nd";
//            case 3: return "rd";
//            default: return "th";
//        }
//    }

    private void setupCampusMap(NavController nav) {
        campusMap.setOnBuildingClickListener((code, name) -> {
            selectedBuildingCode = code != null ? code.toUpperCase(Locale.ROOT) : null;

            if (CODE_MAIN.equals(selectedBuildingCode)) {
                showFloorChipsAnimated();
                return;
            }

            hideFloorChipsAnimated();

            Bundle args = new Bundle();
            args.putString("buildingCode", code);
            args.putString("buildingName", name);

            if (CODE_LIB.equals(selectedBuildingCode) || CODE_REG.equals(selectedBuildingCode)) {
                nav.navigate(R.id.action_home_to_roomList, args);
                return;
            }

            if (CODE_COURT.equals(selectedBuildingCode)
                    || CODE_ENT.equals(selectedBuildingCode)
                    || CODE_EXIT.equals(selectedBuildingCode)) {
                navigateToCampusAreaRoom(nav, selectedBuildingCode, name);
            }
        });
    }

    private void navigateToCampusAreaRoom(NavController nav, String areaCode, String areaName) {
        viewModel.findRoomIdByCode(areaCode, roomId -> {
            if (!isAdded()) {
                return;
            }

            if (roomId == null || roomId <= 0) {
                Toast.makeText(requireContext(), "Campus area not available yet", Toast.LENGTH_SHORT).show();
                nav.navigate(R.id.homeFragment);
                return;
            }

            Bundle args = new Bundle();
            args.putInt("roomId", roomId);
            args.putString("roomName", areaName != null ? areaName : areaCode);
            args.putBoolean("isCampusArea", true);
            nav.navigate(R.id.action_home_to_roomDetail, args);
        });
    }

    private void showFloorChipsAnimated() {
        if (floorChipContainer == null) return;
        if (floorChipContainer.getVisibility() == View.VISIBLE) return;

        floorChipContainer.setVisibility(View.VISIBLE);
        floorChipContainer.setAlpha(0f);
        floorChipContainer.setTranslationY(-dpToPx(14));
        floorChipContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(CHIP_ANIM_DURATION_MS)
                .start();

        updateFade();
    }

    private void hideFloorChipsAnimated() {
        if (floorChipContainer == null) return;
        if (floorChipContainer.getVisibility() != View.VISIBLE) return;

        floorChipContainer.animate()
                .alpha(0f)
                .translationY(-dpToPx(14))
                .setDuration(CHIP_ANIM_DURATION_MS)
                .withEndAction(() -> {
                    floorChipContainer.setVisibility(View.GONE);
                    floorChipContainer.setAlpha(1f);
                    floorChipContainer.setTranslationY(0f);

                    if (leftFade != null) leftFade.setVisibility(View.GONE);
                    if (rightFade != null) rightFade.setVisibility(View.GONE);
                })
                .start();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_compass);

        fab.setOnClickListener(v ->
                new DirectionsSheetFragment().show(getParentFragmentManager(), "directions_sheet"));

        fab.setOnLongClickListener(v -> {
            if (campusMap != null) {
                campusMap.resetView();
            }
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