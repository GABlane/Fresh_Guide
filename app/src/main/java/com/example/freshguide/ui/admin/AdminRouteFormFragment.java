package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.RouteDto;
import com.example.freshguide.model.dto.RouteStepDto;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.repository.RouteRepository;
import com.example.freshguide.ui.adapter.RouteStepEditAdapter;
import com.example.freshguide.viewmodel.AdminViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminRouteFormFragment extends Fragment {

    private AdminViewModel viewModel;

    private TextInputEditText etDescription;
    private TextInputEditText etInstruction;
    private AutoCompleteTextView spinnerOrigin;
    private AutoCompleteTextView spinnerRoom;
    private TextView tvOriginValidation;
    private TextView tvRoomValidation;
    private TextView tvNoSteps;
    private RecyclerView recyclerSteps;
    private Button btnAddStep;
    private Button btnSave;
    private ProgressBar progressBar;

    private final List<RouteStepDto> steps = new ArrayList<>();
    private RouteStepEditAdapter stepsAdapter;

    private final Map<String, Integer> originDisplayToId = new HashMap<>();
    private final Map<Integer, String> originIdToDisplay = new HashMap<>();
    private final Map<String, Integer> roomDisplayToId = new HashMap<>();
    private final Map<Integer, String> roomIdToDisplay = new HashMap<>();

    private int selectedOriginId = -1;
    private int selectedRoomId = -1;

    private int routeId = -1;
    private boolean isEditMode = false;
    private boolean isSaving = false;
    private boolean allowExitWithoutPrompt = false;
    private boolean formLoading = false;

    private boolean optionsLoaded = false;
    private boolean baselineReady = false;
    private String baselineSignature = "";
    private RouteDto pendingRoute;

    private OnBackPressedCallback backPressedCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_route_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        routeId = getArguments() != null ? getArguments().getInt("routeId", -1) : -1;
        isEditMode = routeId != -1;

        initViews(view);
        setupBackHandling();
        setupStepsRecycler();
        setupFieldListeners();
        setupActionListeners();
        observeViewModel();

        viewModel.loadRouteFormOptions();
        if (isEditMode) {
            btnSave.setText("Update Route");
            viewModel.loadSingleRoute(routeId);
        } else {
            btnSave.setText("Create Route");
            baselineSignature = buildCurrentSignature();
            baselineReady = true;
        }
    }

    private void initViews(@NonNull View view) {
        etDescription = view.findViewById(R.id.et_description);
        etInstruction = view.findViewById(R.id.et_instruction);
        spinnerOrigin = view.findViewById(R.id.spinner_origin);
        spinnerRoom = view.findViewById(R.id.spinner_room);
        tvOriginValidation = view.findViewById(R.id.tv_origin_validation);
        tvRoomValidation = view.findViewById(R.id.tv_room_validation);
        tvNoSteps = view.findViewById(R.id.tv_no_steps);
        recyclerSteps = view.findViewById(R.id.recycler_steps);
        btnAddStep = view.findViewById(R.id.btn_add_step);
        btnSave = view.findViewById(R.id.btn_save);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupBackHandling() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (allowExitWithoutPrompt || !hasUnsavedChanges()) {
                    navigateBackSilently();
                    return;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Unsaved changes")
                        .setMessage("Discard your route updates?")
                        .setPositiveButton("Discard", (dialog, which) -> {
                            allowExitWithoutPrompt = true;
                            navigateBackSilently();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
    }

    private void navigateBackSilently() {
        if (backPressedCallback != null) {
            backPressedCallback.setEnabled(false);
        }
        View view = getView();
        if (view != null) {
            NavController navController = Navigation.findNavController(view);
            navController.popBackStack();
        }
    }

    private void setupStepsRecycler() {
        stepsAdapter = new RouteStepEditAdapter(steps, new RouteStepEditAdapter.StepActionListener() {
            @Override
            public void onEdit(int position) {
                showStepDialog(position);
            }

            @Override
            public void onDelete(int position) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete step")
                        .setMessage("Remove this step?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (position < 0 || position >= steps.size()) {
                                return;
                            }
                            steps.remove(position);
                            reorderSteps();
                            stepsAdapter.notifyItemRemoved(position);
                            stepsAdapter.notifyItemRangeChanged(position, steps.size() - position);
                            updateStepListVisibility();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onMoveUp(int position) {
                if (position <= 0 || position >= steps.size()) {
                    return;
                }
                Collections.swap(steps, position, position - 1);
                reorderSteps();
                stepsAdapter.notifyItemMoved(position, position - 1);
                stepsAdapter.notifyItemRangeChanged(position - 1, 2);
            }

            @Override
            public void onMoveDown(int position) {
                if (position < 0 || position >= steps.size() - 1) {
                    return;
                }
                Collections.swap(steps, position, position + 1);
                reorderSteps();
                stepsAdapter.notifyItemMoved(position, position + 1);
                stepsAdapter.notifyItemRangeChanged(position, 2);
            }
        });

        recyclerSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSteps.setItemAnimator(new DefaultItemAnimator());
        recyclerSteps.setAdapter(stepsAdapter);
        updateStepListVisibility();
    }

    private void setupFieldListeners() {
        spinnerOrigin.setOnItemClickListener((parent, view, position, id) -> {
            String key = String.valueOf(parent.getItemAtPosition(position));
            Integer selectedId = originDisplayToId.get(key);
            selectedOriginId = selectedId != null ? selectedId : -1;
            validateOriginRealtime();
        });

        spinnerRoom.setOnItemClickListener((parent, view, position, id) -> {
            String key = String.valueOf(parent.getItemAtPosition(position));
            Integer selectedId = roomDisplayToId.get(key);
            selectedRoomId = selectedId != null ? selectedId : -1;
            validateRoomRealtime();
        });

        spinnerOrigin.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                Integer id = originDisplayToId.get(value);
                selectedOriginId = id != null ? id : -1;
                if (value.isEmpty()) {
                    tvOriginValidation.setVisibility(View.GONE);
                } else if (id == null) {
                    setInvalidValidationText(tvOriginValidation, "Select a valid origin from the list");
                }
            }
        });

        spinnerRoom.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                Integer id = roomDisplayToId.get(value);
                selectedRoomId = id != null ? id : -1;
                if (value.isEmpty()) {
                    tvRoomValidation.setVisibility(View.GONE);
                } else if (id == null) {
                    setInvalidValidationText(tvRoomValidation, "Select a valid room from the list");
                }
            }
        });
    }

    private void setupActionListeners() {
        btnAddStep.setOnClickListener(v -> showStepDialog(-1));
        btnSave.setOnClickListener(v -> validateAndSaveRoute());
    }

    private void observeViewModel() {
        viewModel.getRouteFormOrigins().observe(getViewLifecycleOwner(), this::bindOriginOptions);
        viewModel.getRouteFormRooms().observe(getViewLifecycleOwner(), this::bindRoomOptions);

        viewModel.getCurrentRoute().observe(getViewLifecycleOwner(), route -> {
            if (!isEditMode || route == null) {
                return;
            }
            pendingRoute = route;
            populateRouteIfReady();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            formLoading = loading != null && loading;
            progressBar.setVisibility(formLoading ? View.VISIBLE : View.GONE);
            setFormEnabled(!formLoading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            if (isSaving) {
                isSaving = false;
            }
            View view = getView();
            if (view != null) {
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (!isSaving || message == null || message.trim().isEmpty()) {
                return;
            }
            isSaving = false;
            allowExitWithoutPrompt = true;
            View view = getView();
            if (view != null) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
            }
            navigateBackSilently();
        });
    }

    private void bindOriginOptions(List<OriginEntity> originList) {
        originDisplayToId.clear();
        originIdToDisplay.clear();

        List<String> labels = new ArrayList<>();
        if (originList != null) {
            for (OriginEntity origin : originList) {
                if (origin == null) {
                    continue;
                }
                String code = origin.code != null && !origin.code.trim().isEmpty() ? origin.code.trim() : "-";
                String name = origin.name != null && !origin.name.trim().isEmpty() ? origin.name.trim() : "Origin";
                String label = name + " (" + code + ")";
                labels.add(label);
                originDisplayToId.put(label, origin.id);
                originIdToDisplay.put(origin.id, label);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        spinnerOrigin.setAdapter(adapter);

        updateOptionsLoadedState();
    }

    private void bindRoomOptions(List<RoomEntity> roomList) {
        roomDisplayToId.clear();
        roomIdToDisplay.clear();

        List<String> labels = new ArrayList<>();
        if (roomList != null) {
            for (RoomEntity room : roomList) {
                if (room == null) {
                    continue;
                }
                String code = room.code != null && !room.code.trim().isEmpty() ? room.code.trim() : "NO-CODE";
                String name = room.name != null && !room.name.trim().isEmpty() ? room.name.trim() : "Room";
                String label = name + " (" + code + ")";
                labels.add(label);
                roomDisplayToId.put(label, room.id);
                roomIdToDisplay.put(room.id, label);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        spinnerRoom.setAdapter(adapter);

        updateOptionsLoadedState();
    }

    private void updateOptionsLoadedState() {
        optionsLoaded = !originDisplayToId.isEmpty() && !roomDisplayToId.isEmpty();

        if (!isEditMode) {
            if (!baselineReady) {
                baselineSignature = buildCurrentSignature();
                baselineReady = true;
            }
            return;
        }

        populateRouteIfReady();
    }

    private void populateRouteIfReady() {
        if (!isEditMode || !optionsLoaded || pendingRoute == null) {
            return;
        }

        RouteDto route = pendingRoute;
        pendingRoute = null;

        etDescription.setText(route.description != null ? route.description : "");
        etInstruction.setText(route.instruction != null ? route.instruction : "");

        selectedOriginId = route.originId;
        selectedRoomId = route.destinationRoomId;

        String originLabel = originIdToDisplay.get(route.originId);
        if (originLabel != null) {
            spinnerOrigin.setText(originLabel, false);
        }

        String roomLabel = roomIdToDisplay.get(route.destinationRoomId);
        if (roomLabel != null) {
            spinnerRoom.setText(roomLabel, false);
        }

        steps.clear();
        if (route.steps != null) {
            List<RouteStepDto> sorted = new ArrayList<>(route.steps);
            sorted.sort(Comparator.comparingInt(s -> s != null ? s.orderNum : Integer.MAX_VALUE));
            for (RouteStepDto step : sorted) {
                if (step == null) {
                    continue;
                }
                RouteStepDto copy = new RouteStepDto();
                copy.id = step.id;
                copy.routeId = step.routeId;
                copy.orderNum = step.orderNum;
                copy.instruction = step.instruction;
                copy.direction = step.direction;
                copy.landmark = step.landmark;
                steps.add(copy);
            }
            reorderSteps();
        }
        stepsAdapter.notifyDataSetChanged();
        updateStepListVisibility();

        validateOriginRealtime();
        validateRoomRealtime();

        baselineSignature = buildCurrentSignature();
        baselineReady = true;
    }

    private void validateOriginRealtime() {
        if (selectedOriginId <= 0) {
            setInvalidValidationText(tvOriginValidation, "Select a valid origin from the list");
            return;
        }

        viewModel.validateOriginLocal(selectedOriginId, new RouteRepository.ValidationCallback() {
            @Override
            public void onValid(String displayName) {
                setValidValidationText(tvOriginValidation, "Origin ready: " + displayName);
            }

            @Override
            public void onInvalid(String message) {
                setInvalidValidationText(tvOriginValidation, message);
            }
        });
    }

    private void validateRoomRealtime() {
        if (selectedRoomId <= 0) {
            setInvalidValidationText(tvRoomValidation, "Select a valid room from the list");
            return;
        }

        viewModel.validateRoomLocal(selectedRoomId, new RouteRepository.ValidationCallback() {
            @Override
            public void onValid(String displayName) {
                setValidValidationText(tvRoomValidation, "Destination ready: " + displayName);
            }

            @Override
            public void onInvalid(String message) {
                setInvalidValidationText(tvRoomValidation, message);
            }
        });
    }

    private void setValidValidationText(@NonNull TextView target, @NonNull String message) {
        target.setVisibility(View.VISIBLE);
        target.setText(message);
        target.setTextColor(requireContext().getColor(R.color.green_primary));
    }

    private void setInvalidValidationText(@NonNull TextView target, @NonNull String message) {
        target.setVisibility(View.VISIBLE);
        target.setText(message);
        target.setTextColor(requireContext().getColor(R.color.orange_accent));
    }

    private void showStepDialog(int editPosition) {
        boolean isNewStep = editPosition < 0;
        RouteStepDto currentStep = (!isNewStep && editPosition < steps.size()) ? steps.get(editPosition) : null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_step, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvStepOrder = dialogView.findViewById(R.id.tv_step_order);
        TextInputEditText etInstruction = dialogView.findViewById(R.id.et_instruction);
        AutoCompleteTextView spinnerDirection = dialogView.findViewById(R.id.spinner_direction);
        TextInputEditText etLandmark = dialogView.findViewById(R.id.et_landmark);

        String[] directions = new String[]{"Straight", "Left", "Right", "Up", "Down"};
        ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                directions
        );
        spinnerDirection.setAdapter(directionAdapter);

        if (isNewStep) {
            tvTitle.setText("Add Route Step");
            tvStepOrder.setText("Step #" + (steps.size() + 1));
        } else {
            tvTitle.setText("Edit Route Step");
            tvStepOrder.setText("Step #" + (editPosition + 1));
            if (currentStep != null) {
                etInstruction.setText(currentStep.instruction != null ? currentStep.instruction : "");
                etLandmark.setText(currentStep.landmark != null ? currentStep.landmark : "");
                spinnerDirection.setText(capitalizeDirection(currentStep.direction), false);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(isNewStep ? "Add" : "Update", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String instruction = etInstruction.getText() != null ? etInstruction.getText().toString().trim() : "";
            String direction = spinnerDirection.getText() != null ? spinnerDirection.getText().toString().trim() : "";
            String landmark = etLandmark.getText() != null ? etLandmark.getText().toString().trim() : "";

            if (instruction.isEmpty()) {
                etInstruction.setError("Instruction is required");
                return;
            }

            RouteStepDto target = new RouteStepDto();
            if (currentStep != null) {
                target.id = currentStep.id;
                target.routeId = currentStep.routeId;
            }
            target.instruction = instruction;
            target.direction = normalizeDirection(direction);
            target.landmark = landmark.isEmpty() ? null : landmark;

            if (isNewStep) {
                target.orderNum = steps.size() + 1;
                steps.add(target);
                stepsAdapter.notifyItemInserted(steps.size() - 1);
            } else {
                target.orderNum = currentStep != null ? currentStep.orderNum : (editPosition + 1);
                if (editPosition >= 0 && editPosition < steps.size()) {
                    steps.set(editPosition, target);
                    stepsAdapter.notifyItemChanged(editPosition);
                }
            }

            reorderSteps();
            updateStepListVisibility();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void reorderSteps() {
        for (int i = 0; i < steps.size(); i++) {
            RouteStepDto step = steps.get(i);
            if (step != null) {
                step.orderNum = i + 1;
            }
        }
    }

    private void updateStepListVisibility() {
        boolean hasSteps = !steps.isEmpty();
        recyclerSteps.setVisibility(hasSteps ? View.VISIBLE : View.GONE);
        tvNoSteps.setVisibility(hasSteps ? View.GONE : View.VISIBLE);
    }

    private void validateAndSaveRoute() {
        if (formLoading || isSaving) {
            return;
        }

        isSaving = true;

        if (selectedOriginId <= 0) {
            setInvalidValidationText(tvOriginValidation, "Select a valid origin from the list");
            isSaving = false;
            return;
        }

        if (selectedRoomId <= 0) {
            setInvalidValidationText(tvRoomValidation, "Select a valid room from the list");
            isSaving = false;
            return;
        }

        viewModel.validateOriginLocal(selectedOriginId, new RouteRepository.ValidationCallback() {
            @Override
            public void onValid(String displayName) {
                setValidValidationText(tvOriginValidation, "Origin ready: " + displayName);
                viewModel.validateRoomLocal(selectedRoomId, new RouteRepository.ValidationCallback() {
                    @Override
                    public void onValid(String roomDisplay) {
                        setValidValidationText(tvRoomValidation, "Destination ready: " + roomDisplay);
                        continueSaveAfterValidation();
                    }

                    @Override
                    public void onInvalid(String message) {
                        setInvalidValidationText(tvRoomValidation, message);
                        isSaving = false;
                    }
                });
            }

            @Override
            public void onInvalid(String message) {
                setInvalidValidationText(tvOriginValidation, message);
                isSaving = false;
            }
        });
    }

    private void continueSaveAfterValidation() {
        if (steps.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("No steps added")
                    .setMessage("Save this route without steps?")
                    .setPositiveButton("Save", (dialog, which) -> executeSave())
                    .setNegativeButton("Cancel", (dialog, which) -> isSaving = false)
                    .show();
            return;
        }
        executeSave();
    }

    private void executeSave() {
        RouteDto payload = new RouteDto();
        payload.id = routeId;
        payload.originId = selectedOriginId;
        payload.destinationRoomId = selectedRoomId;
        payload.description = etDescription.getText() != null ? etDescription.getText().toString().trim() : null;
        payload.instruction = etInstruction.getText() != null ? etInstruction.getText().toString().trim() : null;
        payload.steps = buildStepPayload();

        if (isEditMode) {
            viewModel.updateRoute(routeId, payload);
        } else {
            viewModel.createRoute(payload);
        }
    }

    @NonNull
    private List<RouteStepDto> buildStepPayload() {
        List<RouteStepDto> payload = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            RouteStepDto step = steps.get(i);
            if (step == null) {
                continue;
            }
            String instruction = step.instruction != null ? step.instruction.trim() : "";
            if (instruction.isEmpty()) {
                continue;
            }

            RouteStepDto copy = new RouteStepDto();
            copy.id = step.id;
            copy.routeId = step.routeId;
            copy.orderNum = i + 1;
            copy.instruction = instruction;
            copy.direction = normalizeDirection(step.direction);
            copy.landmark = step.landmark != null && !step.landmark.trim().isEmpty()
                    ? step.landmark.trim()
                    : null;
            payload.add(copy);
        }
        return payload;
    }

    private void setFormEnabled(boolean enabled) {
        etDescription.setEnabled(enabled);
        etInstruction.setEnabled(enabled);
        spinnerOrigin.setEnabled(enabled);
        spinnerRoom.setEnabled(enabled);
        btnAddStep.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        recyclerSteps.setEnabled(enabled);
    }

    private String buildCurrentSignature() {
        StringBuilder builder = new StringBuilder();
        builder.append(selectedOriginId).append('|');
        builder.append(selectedRoomId).append('|');
        builder.append(valueOf(etDescription)).append('|');
        builder.append(valueOf(etInstruction));

        for (RouteStepDto step : steps) {
            if (step == null) {
                continue;
            }
            builder.append("||");
            builder.append(step.orderNum).append('>');
            builder.append(step.instruction != null ? step.instruction.trim() : "").append('>');
            builder.append(normalizeDirection(step.direction)).append('>');
            builder.append(step.landmark != null ? step.landmark.trim() : "");
        }

        return builder.toString();
    }

    private String valueOf(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private boolean hasUnsavedChanges() {
        if (!baselineReady) {
            return false;
        }
        return !Objects.equals(baselineSignature, buildCurrentSignature());
    }

    @Nullable
    private String normalizeDirection(@Nullable String direction) {
        if (direction == null) {
            return null;
        }
        String normalized = direction.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return null;
        }
        switch (normalized) {
            case "straight":
            case "left":
            case "right":
            case "up":
            case "down":
                return normalized;
            default:
                return null;
        }
    }

    private String capitalizeDirection(@Nullable String direction) {
        String normalized = normalizeDirection(direction);
        if (normalized == null || normalized.isEmpty()) {
            return "";
        }
        return normalized.substring(0, 1).toUpperCase(Locale.US) + normalized.substring(1);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
