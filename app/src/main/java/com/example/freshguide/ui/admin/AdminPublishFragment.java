package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.freshguide.R;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

public class AdminPublishFragment extends Fragment {

    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_publish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        AdminNavigationUtils.bindBackToDashboard(this, view);

        Button btnPublish = view.findViewById(R.id.btn_publish);
        TextView tvStatus = view.findViewById(R.id.tv_publish_status);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            btnPublish.setEnabled(loading == null || !loading);
            btnPublish.setText(loading != null && loading ? "Publishing..." : "Publish Now");
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(msg);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });

        btnPublish.setOnClickListener(v -> {
            tvStatus.setVisibility(View.GONE);
            viewModel.publish();
        });
    }
}
