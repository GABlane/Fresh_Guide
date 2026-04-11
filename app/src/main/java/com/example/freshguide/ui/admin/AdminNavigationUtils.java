package com.example.freshguide.ui.admin;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.freshguide.R;

final class AdminNavigationUtils {

    private AdminNavigationUtils() {
    }

    static void bindBackToDashboard(@NonNull Fragment fragment, @NonNull View root) {
        View backButton = root.findViewById(R.id.btn_admin_back);
        if (backButton == null) {
            return;
        }
        backButton.setOnClickListener(v -> {
            if (!fragment.isAdded()) {
                return;
            }
            NavController navController = NavHostFragment.findNavController(fragment);
            if (!navController.navigateUp()) {
                navController.navigate(R.id.adminDashboardFragment);
            }
        });
    }

    static void showSearch(@NonNull View root, boolean show) {
        View searchContainer = root.findViewById(R.id.layout_admin_search);
        if (searchContainer != null) {
            searchContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
