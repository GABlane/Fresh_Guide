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

public class AdminFloorFormFragment extends Fragment {

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

        int floorId = getArguments() != null ? getArguments().getInt("floorId", -1) : -1;
        boolean isEdit = floorId != -1;

        EditText etName = view.findViewById(R.id.et_field1);
        EditText etNumber = view.findViewById(R.id.et_field2);
        EditText etBuildingId = view.findViewById(R.id.et_field3);
        Button btnSave = view.findViewById(R.id.btn_save);
        TextView tvTitle = view.findViewById(R.id.tv_admin_form_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_admin_form_subtitle);
        TextView tvIntro = view.findViewById(R.id.tv_admin_form_intro_body);
        TextView tvField1Label = view.findViewById(R.id.tv_field1_label);
        TextView tvField2Label = view.findViewById(R.id.tv_field2_label);
        TextView tvField3Label = view.findViewById(R.id.tv_field3_label);

        tvTitle.setText(isEdit ? "Edit Floor" : "New Floor");
        tvSubtitle.setText("Maintain floor metadata used in campus maps, filters, and room grouping.");
        tvIntro.setText("Match the building relationship and floor number students expect when moving through the app.");
        tvField1Label.setText("Floor Name");
        tvField2Label.setText("Floor Number");
        tvField3Label.setText("Building ID");

        etName.setHint("Floor Name");
        etNumber.setHint("Floor Number");
        etBuildingId.setHint("Building ID");
        etNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etBuildingId.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etBuildingId.setSingleLine(true);
        etBuildingId.setMinLines(1);
        etBuildingId.setMinHeight(0);
        etBuildingId.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        btnSave.setText(isEdit ? "Update Floor" : "Create Floor");

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String number = etNumber.getText().toString().trim();
            String buildingId = etBuildingId.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (number.isEmpty()) {
                etNumber.setError("Floor number is required");
                return;
            }
            if (buildingId.isEmpty()) {
                etBuildingId.setError("Building ID is required");
                return;
            }

            int numInt = Integer.parseInt(number);
            int bldgInt = Integer.parseInt(buildingId);

            if (isEdit) {
                viewModel.updateFloor(floorId, bldgInt, numInt, name);
            } else {
                viewModel.createFloor(bldgInt, numInt, name);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Navigation.findNavController(view).popBackStack();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Snackbar.make(view, err, Snackbar.LENGTH_LONG).show();
        });
    }
}
