package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class AdminBuildingFormFragment extends Fragment {

    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        int buildingId = getArguments() != null ? getArguments().getInt("buildingId", -1) : -1;
        boolean isEdit = buildingId != -1;

        EditText etName = view.findViewById(R.id.et_field1);
        EditText etCode = view.findViewById(R.id.et_field2);
        EditText etDescription = view.findViewById(R.id.et_field3);
        Button btnSave = view.findViewById(R.id.btn_save);
        TextView tvTitle = view.findViewById(R.id.tv_admin_form_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_admin_form_subtitle);
        TextView tvIntro = view.findViewById(R.id.tv_admin_form_intro_body);
        TextView tvField1Label = view.findViewById(R.id.tv_field1_label);
        TextView tvField2Label = view.findViewById(R.id.tv_field2_label);
        TextView tvField3Label = view.findViewById(R.id.tv_field3_label);

        tvTitle.setText(isEdit ? "Edit Building" : "New Building");
        tvSubtitle.setText("Keep building records aligned with the campus structures students browse.");
        tvIntro.setText("Use clear names and concise codes so maps, search, and linked floors stay in sync.");
        tvField1Label.setText("Building Name");
        tvField2Label.setText("Code");
        tvField3Label.setText("Description");

        etName.setHint("Building Name");
        etCode.setHint("Code (e.g. BLDG-A)");
        etDescription.setHint("Description");

        btnSave.setText(isEdit ? "Update Building" : "Create Building");

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String code = etCode.getText().toString().trim().toUpperCase(Locale.ROOT);
            String desc = etDescription.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            if (isEdit) {
                viewModel.updateBuilding(buildingId, name, code, desc);
            } else {
                viewModel.createBuilding(name, code, desc);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Navigation.findNavController(view).popBackStack();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });
    }
}
