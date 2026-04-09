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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.viewmodel.AdminViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        TextView tvRooms = view.findViewById(R.id.tv_room_count);
        TextView tvBuildings = view.findViewById(R.id.tv_building_count);
        TextView tvFloors = view.findViewById(R.id.tv_floor_count);
        TextView tvRoutes = view.findViewById(R.id.tv_route_count);
        TextView tvSyncVersion = view.findViewById(R.id.tv_admin_sync_version);
        TextView tvAdminName = view.findViewById(R.id.tv_admin_dashboard_name);
        TextView tvDashboardDate = view.findViewById(R.id.tv_admin_dashboard_date);

        tvDashboardDate.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(new Date()));

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String adminName = sessionManager.getUserName();
        tvAdminName.setText(adminName != null && !adminName.trim().isEmpty() ? adminName : "Admin");
        int syncVersion = sessionManager.getSyncVersion();
        tvSyncVersion.setText(syncVersion >= 0
                ? "Current data version: " + syncVersion
                : "Current data version: Not synced");

        viewModel.getRoomCount().observe(getViewLifecycleOwner(), c -> tvRooms.setText(String.valueOf(c)));
        viewModel.getBuildingCount().observe(getViewLifecycleOwner(), c -> tvBuildings.setText(String.valueOf(c)));
        viewModel.getFloorCount().observe(getViewLifecycleOwner(), c -> tvFloors.setText(String.valueOf(c)));
        viewModel.getRouteCount().observe(getViewLifecycleOwner(), c -> tvRoutes.setText(String.valueOf(c)));
        viewModel.loadDashboardCounts();

        NavController nav = Navigation.findNavController(view);

        // Overview cards click listeners
        view.findViewById(R.id.card_rooms_overview).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminRoomList));
        view.findViewById(R.id.card_buildings_overview).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminBuildingList));
        view.findViewById(R.id.card_floors_overview).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminFloorList));
        view.findViewById(R.id.card_routes_overview).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminRouteList));

        view.findViewById(R.id.btn_manage_buildings).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminBuildingList));
        view.findViewById(R.id.btn_manage_floors).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminFloorList));
        view.findViewById(R.id.btn_manage_rooms).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminRoomList));
        view.findViewById(R.id.btn_manage_campus_areas).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminCampusAreaList));
        view.findViewById(R.id.btn_manage_facilities).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminFacilityList));
        view.findViewById(R.id.btn_manage_origins).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminOriginList));
        view.findViewById(R.id.btn_manage_routes).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminRouteList));
        view.findViewById(R.id.btn_publish).setOnClickListener(v ->
                nav.navigate(R.id.action_adminDashboard_to_adminPublish));
    }
}
