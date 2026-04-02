package com.example.freshguide.ui.user;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.ui.adapter.RoomDropdownAdapter;
import com.example.freshguide.ui.adapter.ScheduleEntryAdapter;
import com.example.freshguide.ui.adapter.TimeWheelAdapter;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.viewmodel.ScheduleViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private static final String TAG = "ScheduleFragment";

    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] CLASS_TYPES = {"On-site", "Online"};
    private static final String[] PLATFORMS = {"Zoom", "Google Meet", "Microsoft Teams", "Other"};
    private static final String[] REMINDER_OPTIONS = {
            "No reminder", "5 mins before", "10 mins before", "15 mins before", "30 mins before"
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && isAdded()) {
                    Toast.makeText(requireContext(), "Reminder notifications are disabled", Toast.LENGTH_SHORT).show();
                }
            });

    private ScheduleViewModel viewModel;
    private SessionManager sessionManager;
    private LiveData<List<ScheduleEntryEntity>> allSchedulesLiveData;

    private final List<ScheduleEntryEntity> allSchedules = new ArrayList<>();
    private final List<ScheduleEntryEntity> filteredDailySchedules = new ArrayList<>();

    private ScheduleEntryAdapter dailyAdapter;

    private RecyclerView dailyScheduleRecycler;
    private View dailyDaySelectorContainer;
    private View emptyState;
    private View cardSummary;

    private TextView[] dailyDayViews;
    private TextView tvSummaryCode;
    private TextView tvSummaryTitle;
    private TextView tvSummaryProfessor;
    private TextView tvSummaryTime;
    private TextView tvSummaryLabel;
    private TextView tvDate;
    private TextView tvEmptyStateMessage;

    private final List<RoomEntity> allRooms = new ArrayList<>();
    private final List<RoomEntity> roomOptions = new ArrayList<>();
    private final Map<Integer, String> roomNameMap = new HashMap<>();

    @Nullable
    private ConnectivityManager connectivityManager;
    @Nullable
    private ConnectivityManager.NetworkCallback scheduleNetworkCallback;
    private boolean networkCallbackRegistered = false;

    private int selectedDay = 1;

    private final List<ScheduleBlockBinding> scheduleBlocks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);
        sessionManager = SessionManager.getInstance(requireContext());

        ScheduleReminderHelper.ensureNotificationChannel(requireContext());

        tvDate = view.findViewById(R.id.tv_schedule_date);
        dailyScheduleRecycler = view.findViewById(R.id.recycler_daily_schedule);
        dailyDaySelectorContainer = view.findViewById(R.id.daily_day_selector_container);
        emptyState = view.findViewById(R.id.empty_state);
        cardSummary = view.findViewById(R.id.card_today_summary);

        tvSummaryCode = view.findViewById(R.id.tv_summary_course_code);
        tvSummaryTitle = view.findViewById(R.id.tv_summary_title);
        tvSummaryProfessor = view.findViewById(R.id.tv_summary_professor);
        tvSummaryTime = view.findViewById(R.id.tv_summary_time);
        tvSummaryLabel = view.findViewById(R.id.tv_summary_label);
        tvEmptyStateMessage = view.findViewById(R.id.tv_empty_state_message);

        dailyDayViews = new TextView[]{
                view.findViewById(R.id.daily_day_mon),
                view.findViewById(R.id.daily_day_tue),
                view.findViewById(R.id.daily_day_wed),
                view.findViewById(R.id.daily_day_thu),
                view.findViewById(R.id.daily_day_fri),
                view.findViewById(R.id.daily_day_sat)
        };

        tvDate.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));

        dailyAdapter = new ScheduleEntryAdapter();
        dailyAdapter.setOnScheduleClickListener(this::showScheduleDetailDialog);
        dailyScheduleRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        dailyScheduleRecycler.setAdapter(dailyAdapter);

        view.findViewById(R.id.btn_add_schedule).setOnClickListener(v -> showScheduleFormDialog(null));
        view.findViewById(R.id.btn_empty_add_schedule).setOnClickListener(v -> showScheduleFormDialog(null));

        selectedDay = getDefaultDay();
        setupDailyDaySelector();
        applyDailyDaySelectionUi();

        loadRoomOptions();
        observeAllSchedules();
        viewModel.syncSchedules();
    }

    private void observeAllSchedules() {
        if (allSchedulesLiveData != null) {
            allSchedulesLiveData.removeObservers(getViewLifecycleOwner());
        }

        allSchedulesLiveData = viewModel.getAllSchedules();
        allSchedulesLiveData.observe(getViewLifecycleOwner(), schedules -> {
            allSchedules.clear();
            if (schedules != null) {
                allSchedules.addAll(schedules);
            }

            dailyAdapter.setRoomNameMap(roomNameMap);
            updateSummaryCard(allSchedules);
            renderScheduleContent();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.syncSchedules();

        int today = getDefaultDay();
        if (selectedDay != today) {
            selectedDay = today;
            applyDailyDaySelectionUi();
            renderScheduleContent();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerScheduleNetworkCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterScheduleNetworkCallback();
    }

    private void registerScheduleNetworkCallback() {
        if (!isAdded() || networkCallbackRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        scheduleNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                viewModel.syncSchedules();
            }
        };

        try {
            connectivityManager.registerDefaultNetworkCallback(scheduleNetworkCallback);
            networkCallbackRegistered = true;
        } catch (Exception ignored) {
            networkCallbackRegistered = false;
        }
    }

    private void unregisterScheduleNetworkCallback() {
        if (!networkCallbackRegistered || connectivityManager == null || scheduleNetworkCallback == null) {
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(scheduleNetworkCallback);
        } catch (Exception ignored) {
        }

        networkCallbackRegistered = false;
        scheduleNetworkCallback = null;
        connectivityManager = null;
    }

    private void setupDailyDaySelector() {
        for (TextView dayView : dailyDayViews) {
            dayView.setClickable(false);
            dayView.setFocusable(false);
            dayView.setEnabled(false);
        }
    }

    private void applyDailyDaySelectionUi() {
        for (int i = 0; i < dailyDayViews.length; i++) {
            TextView dayView = dailyDayViews[i];
            boolean isToday = (i + 1) == selectedDay;

            if (isToday) {
                dayView.setBackgroundResource(R.drawable.bg_schedule_day_selected);
                dayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
            } else {
                dayView.setBackground(null);
                dayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
    }

    private void renderScheduleContent() {
        filteredDailySchedules.clear();

        for (ScheduleEntryEntity entry : allSchedules) {
            if (entry.dayOfWeek == selectedDay) {
                filteredDailySchedules.add(entry);
            }
        }

        dailyAdapter.setRoomNameMap(roomNameMap);
        dailyAdapter.setItems(filteredDailySchedules);

        boolean hasSchedulesForSelectedDay = !filteredDailySchedules.isEmpty();
        boolean hasAnySchedules = !allSchedules.isEmpty();

        dailyScheduleRecycler.setVisibility(hasSchedulesForSelectedDay ? View.VISIBLE : View.GONE);
        dailyDaySelectorContainer.setVisibility(View.VISIBLE);
        emptyState.setVisibility(hasSchedulesForSelectedDay ? View.GONE : View.VISIBLE);

        if (!hasSchedulesForSelectedDay) {
            if (!hasAnySchedules) {
                tvEmptyStateMessage.setText("No schedule yet.");
            } else {
                tvEmptyStateMessage.setText("No schedule found.");
            }
        }
    }

    private void updateSummaryCard(@Nullable List<ScheduleEntryEntity> schedules) {
        cardSummary.setVisibility(View.VISIBLE);

        if (schedules == null || schedules.isEmpty()) {
            tvSummaryLabel.setText("NO SCHEDULE YET");
            tvSummaryTitle.setText("GET STARTED");
            tvSummaryProfessor.setText("ADD YOUR FIRST CLASS TO BUILD YOUR SCHEDULE.");
            tvSummaryTime.setText("YOUR CLASSES WILL APPEAR HERE.");
            tvSummaryCode.setText("");
            return;
        }

        ScheduleEntryEntity todayEntry = findTodaySummaryEntry(schedules);

        if (todayEntry == null) {
            tvSummaryLabel.setText("YOU HAVE NO CLASS SCHEDULE TODAY");
            tvSummaryTitle.setText("FREE TIME");
            tvSummaryProfessor.setText("USE THIS TIME TO REST, STUDY, OR EXPLORE.");
            tvSummaryTime.setText("YOUR NEXT SCHEDULE WILL APPEAR HERE.");
            tvSummaryCode.setText("");
            return;
        }

        tvSummaryLabel.setText("YOU HAVE CLASS SCHEDULE TODAY");
        tvSummaryTitle.setText(todayEntry.title != null && !todayEntry.title.isBlank()
                ? todayEntry.title.toUpperCase(Locale.getDefault())
                : "CLASS");

        if (todayEntry.instructor != null && !todayEntry.instructor.isBlank()) {
            tvSummaryProfessor.setText(("PROF. " + todayEntry.instructor).toUpperCase(Locale.getDefault()));
        } else if (todayEntry.courseCode != null && !todayEntry.courseCode.isBlank()) {
            tvSummaryProfessor.setText(todayEntry.courseCode.toUpperCase(Locale.getDefault()));
        } else {
            tvSummaryProfessor.setText("TODAY'S CLASS");
        }

        tvSummaryTime.setText(
                (formatMinutes(todayEntry.startMinutes) + " - " + formatMinutes(todayEntry.endMinutes))
                        .toUpperCase(Locale.getDefault())
        );
        tvSummaryCode.setText("");
    }

    @Nullable
    private ScheduleEntryEntity findTodaySummaryEntry(@Nullable List<ScheduleEntryEntity> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }

        int today = getDefaultDay();
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        ScheduleEntryEntity nextClass = null;
        ScheduleEntryEntity fallback = null;

        for (ScheduleEntryEntity entry : schedules) {
            if (entry.dayOfWeek != today) {
                continue;
            }

            if (fallback == null || entry.startMinutes < fallback.startMinutes) {
                fallback = entry;
            }

            if (entry.startMinutes >= currentMinutes &&
                    (nextClass == null || entry.startMinutes < nextClass.startMinutes)) {
                nextClass = entry;
            }
        }

        return nextClass != null ? nextClass : fallback;
    }

    private void loadRoomOptions() {
        viewModel.loadRooms(rooms -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                allRooms.clear();
                allRooms.addAll(rooms);

                roomOptions.clear();
                roomNameMap.clear();

                for (RoomEntity room : allRooms) {
                    if (isCampusAreaCode(room.code)) {
                        continue;
                    }
                    roomOptions.add(room);
                    roomNameMap.put(room.id, buildRoomDisplay(room));
                }

                dailyAdapter.setRoomNameMap(roomNameMap);
                renderScheduleContent();
            });
        });
    }

    private void showScheduleFormDialog(@Nullable ScheduleEntryEntity existing) {
        if (!isAdded()) return;

        View formView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_schedule_form, null, false);

        EditText etSubjectName = formView.findViewById(R.id.et_subject_name);
        EditText etSubjectCode = formView.findViewById(R.id.et_subject_code);
        EditText etProfessor = formView.findViewById(R.id.et_professor);
        EditText etNotes = formView.findViewById(R.id.et_notes);

        MaterialAutoCompleteTextView dropdownClassType = formView.findViewById(R.id.dropdown_class_type);
        MaterialAutoCompleteTextView dropdownPlatform = formView.findViewById(R.id.dropdown_platform);
        MaterialAutoCompleteTextView dropdownReminder = formView.findViewById(R.id.dropdown_reminder);

        EditText etRoomSearch = formView.findViewById(R.id.et_room_search);
        ImageView btnClearRoomSearch = formView.findViewById(R.id.btn_clear_room_search);
        RecyclerView recyclerRoomDropdown = formView.findViewById(R.id.recycler_room_dropdown);

        LinearLayout roomGroup = formView.findViewById(R.id.room_group);
        LinearLayout onlineGroup = formView.findViewById(R.id.online_group);
        LinearLayout scheduleContainer = formView.findViewById(R.id.schedule_container);
        ImageView btnAddScheduleBlock = formView.findViewById(R.id.btn_add_schedule_block);

        TextView btnCancel = formView.findViewById(R.id.btn_sheet_cancel);
        TextView btnSave = formView.findViewById(R.id.btn_sheet_save);

        ArrayAdapter<String> classTypeAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown_simple,
                CLASS_TYPES
        );
        dropdownClassType.setAdapter(classTypeAdapter);

        ArrayAdapter<String> platformAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown_simple,
                PLATFORMS
        );
        dropdownPlatform.setAdapter(platformAdapter);

        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown_simple,
                REMINDER_OPTIONS
        );
        dropdownReminder.setAdapter(reminderAdapter);

        dropdownClassType.setOnItemClickListener((parent, view, position, id) -> {
            boolean online = position == 1;
            roomGroup.setVisibility(online ? View.GONE : View.VISIBLE);
            onlineGroup.setVisibility(online ? View.VISIBLE : View.GONE);
        });

        recyclerRoomDropdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        RoomDropdownAdapter roomDropdownAdapter = new RoomDropdownAdapter(room -> {
            etRoomSearch.setText(buildRoomDisplay(room));
            etRoomSearch.setTag(room);
            recyclerRoomDropdown.setVisibility(View.GONE);
            btnClearRoomSearch.setVisibility(View.VISIBLE);
        });
        recyclerRoomDropdown.setAdapter(roomDropdownAdapter);
        roomDropdownAdapter.submitList(new ArrayList<>(roomOptions));

        etRoomSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !roomOptions.isEmpty()) {
                recyclerRoomDropdown.setVisibility(View.VISIBLE);
            }
        });

        etRoomSearch.setOnClickListener(v -> {
            if (!roomOptions.isEmpty()) {
                recyclerRoomDropdown.setVisibility(View.VISIBLE);
            }
        });

        etRoomSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearRoomSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                List<RoomEntity> filtered = new ArrayList<>();
                for (RoomEntity room : roomOptions) {
                    String name = room.name != null ? room.name.toLowerCase(Locale.ROOT) : "";
                    String code = room.code != null ? room.code.toLowerCase(Locale.ROOT) : "";
                    if (query.isEmpty() || name.contains(query) || code.contains(query)) {
                        filtered.add(room);
                    }
                }

                roomDropdownAdapter.submitList(filtered);
                recyclerRoomDropdown.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearRoomSearch.setOnClickListener(v -> {
            etRoomSearch.setText("");
            etRoomSearch.setTag(null);
            btnClearRoomSearch.setVisibility(View.GONE);
            roomDropdownAdapter.submitList(new ArrayList<>(roomOptions));
            recyclerRoomDropdown.setVisibility(View.GONE);
        });

        int[] selectedColorIndex = new int[]{0};
        setupColorPicker(formView, selectedColorIndex, 0);

        scheduleBlocks.clear();
        scheduleContainer.removeAllViews();

        if (existing != null) {
            etSubjectName.setText(existing.title != null ? existing.title : "");
            etSubjectCode.setText(existing.courseCode != null ? existing.courseCode : "");
            etProfessor.setText(existing.instructor != null ? existing.instructor : "");
            etNotes.setText(existing.notes != null ? existing.notes : "");

            dropdownClassType.setText(existing.isOnline == 1 ? "Online" : "On-site", false);

            if (existing.isOnline == 1) {
                onlineGroup.setVisibility(View.VISIBLE);
                roomGroup.setVisibility(View.GONE);
                dropdownPlatform.setText(
                        existing.onlinePlatform != null ? existing.onlinePlatform : PLATFORMS[0],
                        false
                );
            } else {
                onlineGroup.setVisibility(View.GONE);
                roomGroup.setVisibility(View.VISIBLE);
                if (existing.roomId != null) {
                    for (RoomEntity room : roomOptions) {
                        if (room.id == existing.roomId) {
                            etRoomSearch.setText(buildRoomDisplay(room));
                            etRoomSearch.setTag(room);
                            btnClearRoomSearch.setVisibility(View.VISIBLE);
                            break;
                        }
                    }
                }
            }

            dropdownReminder.setText(
                    REMINDER_OPTIONS[reminderToPosition(existing.reminderMinutes)],
                    false
            );

            if (existing.colorHex != null) {
                for (int i = 0; i < getScheduleColors().length; i++) {
                    if (getScheduleColors()[i].equalsIgnoreCase(existing.colorHex)) {
                        selectedColorIndex[0] = i;
                        break;
                    }
                }
            }
            setupColorPicker(formView, selectedColorIndex, selectedColorIndex[0]);

            addScheduleBlock(scheduleContainer, existing.dayOfWeek, existing.startMinutes, existing.endMinutes, false);
        } else {
            dropdownClassType.setText(CLASS_TYPES[0], false);
            dropdownReminder.setText(
                    REMINDER_OPTIONS[reminderToPosition(sessionManager.getDefaultReminderMinutes())],
                    false
            );
            roomGroup.setVisibility(View.VISIBLE);
            onlineGroup.setVisibility(View.GONE);

            addScheduleBlock(scheduleContainer, selectedDay, -1, -1, false);
        }

        btnAddScheduleBlock.setOnClickListener(v -> addScheduleBlock(
                scheduleContainer,
                getDefaultDay(),
                -1,
                -1,
                true
        ));

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(),
                R.style.ThemeOverlay_FreshGuide_BottomSheet
        );
        dialog.setContentView(formView);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            try {
                String title = etSubjectName.getText().toString().trim();
                if (title.isEmpty()) {
                    etSubjectName.setError("Subject name is required");
                    return;
                }

                boolean online = "Online".contentEquals(dropdownClassType.getText());

                Integer roomId = null;
                String platform = null;

                if (online) {
                    platform = dropdownPlatform.getText() != null
                            ? dropdownPlatform.getText().toString().trim()
                            : null;

                    if (platform == null || platform.isEmpty()) {
                        Toast.makeText(requireContext(), "Please choose an online platform", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Object selectedRoom = etRoomSearch.getTag();
                    if (!(selectedRoom instanceof RoomEntity)) {
                        Toast.makeText(requireContext(), "Please choose a room location", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    roomId = ((RoomEntity) selectedRoom).id;
                }

                int reminderPosition = 0;
                String reminderValue = dropdownReminder.getText() != null
                        ? dropdownReminder.getText().toString()
                        : REMINDER_OPTIONS[0];
                for (int i = 0; i < REMINDER_OPTIONS.length; i++) {
                    if (REMINDER_OPTIONS[i].equalsIgnoreCase(reminderValue)) {
                        reminderPosition = i;
                        break;
                    }
                }

                int reminderMinutes = positionToReminder(reminderPosition);
                if (!sessionManager.isScheduleNotificationsEnabled()) {
                    reminderMinutes = 0;
                }

                int colorIndex = selectedColorIndex[0];
                if (colorIndex < 0 || colorIndex >= getScheduleColors().length) {
                    colorIndex = 0;
                }

                if (scheduleBlocks.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add a schedule block", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (ScheduleBlockBinding block : scheduleBlocks) {
                    if (block.selectedDay < 1 || block.selectedDay > 6) {
                        Toast.makeText(requireContext(), "Please choose a schedule day", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (block.startMinutes < 0 || block.endMinutes < 0) {
                        Toast.makeText(requireContext(), "Please select start and end time", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (block.endMinutes <= block.startMinutes) {
                        Toast.makeText(requireContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                long now = System.currentTimeMillis();
                int totalToSave = scheduleBlocks.size();
                int[] savedCount = {0};
                boolean[] errorShown = {false};

                for (int i = 0; i < scheduleBlocks.size(); i++) {
                    ScheduleBlockBinding block = scheduleBlocks.get(i);

                    ScheduleEntryEntity entry = new ScheduleEntryEntity(
                            title,
                            normalize(etSubjectCode.getText().toString()),
                            normalize(etProfessor.getText().toString()),
                            normalize(etNotes.getText().toString()),
                            getScheduleColors()[colorIndex],
                            block.selectedDay,
                            block.startMinutes,
                            block.endMinutes,
                            online ? 1 : 0,
                            roomId,
                            platform,
                            reminderMinutes,
                            (existing != null && i == 0) ? existing.createdAt : now,
                            now
                    );

                    if (existing != null && i == 0) {
                        entry.id = existing.id;
                        entry.remoteId = existing.remoteId;
                        entry.clientUuid = existing.clientUuid;
                        entry.ownerStudentId = existing.ownerStudentId;
                        entry.syncState = existing.syncState;
                        entry.pendingDelete = existing.pendingDelete;
                    }

                    maybeRequestNotificationPermission(entry.reminderMinutes);

                    viewModel.saveSchedule(entry, new ScheduleViewModel.OperationCallback() {
                        @Override
                        public void onSuccess(ScheduleEntryEntity savedEntry) {
                            if (!isAdded() || getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                savedCount[0]++;
                                if (savedCount[0] == totalToSave && !errorShown[0]) {
                                    dialog.dismiss();
                                    Toast.makeText(
                                            requireContext(),
                                            totalToSave > 1 ? "Schedules saved" : "Schedule saved",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "saveSchedule failed: " + message);
                            if (!isAdded() || getActivity() == null || errorShown[0]) return;
                            errorShown[0] = true;
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Create/Save schedule flow crashed", e);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Unable to save schedule right now", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = WindowManager.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(params);
            }
        });

        dialog.show();
    }

    private void addScheduleBlock(LinearLayout container,
                                  int initialDay,
                                  int initialStartMinutes,
                                  int initialEndMinutes,
                                  boolean removable) {
        View blockView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_schedule_block, container, false);

        ScheduleBlockBinding binding = new ScheduleBlockBinding();
        binding.root = blockView;
        binding.label = blockView.findViewById(R.id.tv_schedule_label);
        binding.delete = blockView.findViewById(R.id.btn_delete_schedule);
        binding.dayContainer = blockView.findViewById(R.id.day_container);
        binding.btnStart = blockView.findViewById(R.id.btn_start);
        binding.btnEnd = blockView.findViewById(R.id.btn_end);
        binding.selectedDay = initialDay;
        binding.startMinutes = initialStartMinutes;
        binding.endMinutes = initialEndMinutes;

        setupDayChips(binding);

        if (initialStartMinutes >= 0) {
            binding.btnStart.setText(formatMinutes(initialStartMinutes));
            binding.btnStart.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
        }

        if (initialEndMinutes >= 0) {
            binding.btnEnd.setText(formatMinutes(initialEndMinutes));
            binding.btnEnd.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
        }

        binding.btnStart.setOnClickListener(v -> showTimePickerForBlock(binding, true));
        binding.btnEnd.setOnClickListener(v -> showTimePickerForBlock(binding, false));

        binding.delete.setVisibility(removable ? View.VISIBLE : View.GONE);
        binding.delete.setOnClickListener(v -> {
            container.removeView(binding.root);
            scheduleBlocks.remove(binding);
            refreshScheduleLabels();
        });

        scheduleBlocks.add(binding);
        container.addView(blockView);
        refreshScheduleLabels();
    }

    private void refreshScheduleLabels() {
        for (int i = 0; i < scheduleBlocks.size(); i++) {
            scheduleBlocks.get(i).label.setText("Schedule " + (i + 1));
            scheduleBlocks.get(i).delete.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void setupDayChips(ScheduleBlockBinding binding) {
        binding.dayContainer.removeAllViews();
        binding.dayChips.clear();

        Typeface interMedium = ResourcesCompat.getFont(requireContext(), R.font.inter_medium);

        for (int i = 0; i < DAY_LABELS.length; i++) {
            int dayValue = i + 1;

            FrameLayout slot = new FrameLayout(requireContext());
            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(0, dpToPx(40));
            slotParams.weight = 1f;
            slot.setLayoutParams(slotParams);

            TextView chip = new TextView(requireContext());
            FrameLayout.LayoutParams chipParams = new FrameLayout.LayoutParams(dpToPx(44), dpToPx(32));
            chipParams.gravity = Gravity.CENTER;
            chip.setLayoutParams(chipParams);
            chip.setText(DAY_LABELS[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(12);
            chip.setTypeface(interMedium);

            boolean selected = dayValue == binding.selectedDay;

            chip.setBackgroundResource(selected
                    ? R.drawable.bg_form_chip_selected
                    : R.drawable.bg_form_chip_unselected);

            chip.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? android.R.color.white : R.color.green_primary
            ));

            chip.setScaleX(selected ? 1.05f : 1f);
            chip.setScaleY(selected ? 1.05f : 1f);

            chip.setOnClickListener(v -> {
                binding.selectedDay = dayValue;
                updateBlockDaySelection(binding);
            });

            slot.addView(chip);
            binding.dayContainer.addView(slot);
            binding.dayChips.add(chip);
        }
    }

    private void updateBlockDaySelection(ScheduleBlockBinding binding) {
        for (int i = 0; i < binding.dayChips.size(); i++) {
            TextView chip = binding.dayChips.get(i);
            boolean selected = (i + 1) == binding.selectedDay;

            chip.setBackgroundResource(selected
                    ? R.drawable.bg_form_chip_selected
                    : R.drawable.bg_form_chip_unselected);

            chip.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? android.R.color.white : R.color.green_primary
            ));

            chip.animate()
                    .scaleX(selected ? 1.05f : 1f)
                    .scaleY(selected ? 1.05f : 1f)
                    .setDuration(120)
                    .start();
        }
    }

    private void showTimePickerForBlock(ScheduleBlockBinding binding, boolean isStart) {
        if (!isAdded()) return;

        int value = isStart ? binding.startMinutes : binding.endMinutes;

        Calendar now = Calendar.getInstance();
        int fallbackHour24 = now.get(Calendar.HOUR_OF_DAY);
        int fallbackMinute = now.get(Calendar.MINUTE);

        int initialHour24 = value >= 0 ? value / 60 : fallbackHour24;
        int initialMinute = value >= 0 ? value % 60 : fallbackMinute;

        int initialAmPm = initialHour24 >= 12 ? 1 : 0;
        int initialHour12 = initialHour24 % 12;
        if (initialHour12 == 0) initialHour12 = 12;

        View pickerView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_time_picker_custom, null, false);

        RecyclerView rvHour = pickerView.findViewById(R.id.rv_hour);
        RecyclerView rvMinute = pickerView.findViewById(R.id.rv_minute);
        TextView btnAm = pickerView.findViewById(R.id.btn_am);
        TextView btnPm = pickerView.findViewById(R.id.btn_pm);
        TextView btnConfirm = pickerView.findViewById(R.id.btn_time_confirm);

        final int[] selectedAmPm = {initialAmPm};

        List<String> hourItems = Arrays.asList(
                "01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12"
        );

        List<String> minuteItems = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            minuteItems.add(String.format(Locale.getDefault(), "%02d", i));
        }

        TimeWheelAdapter hourAdapter = new TimeWheelAdapter(hourItems);
        TimeWheelAdapter minuteAdapter = new TimeWheelAdapter(minuteItems);

        setupWheelRecycler(rvHour, hourAdapter, initialHour12 - 1);
        setupWheelRecycler(rvMinute, minuteAdapter, initialMinute);

        updateAmPmButtons(btnAm, btnPm, selectedAmPm[0]);

        btnAm.setOnClickListener(v -> {
            selectedAmPm[0] = 0;
            updateAmPmButtons(btnAm, btnPm, selectedAmPm[0]);
        });

        btnPm.setOnClickListener(v -> {
            selectedAmPm[0] = 1;
            updateAmPmButtons(btnAm, btnPm, selectedAmPm[0]);
        });

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(),
                R.style.CustomBottomSheetDialog
        );

        dialog.setContentView(pickerView);
        dialog.setCancelable(true);

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            int selectedHourIndex = getCenteredAdapterPosition(rvHour);
            int selectedMinuteIndex = getCenteredAdapterPosition(rvMinute);

            int selectedHour12 = selectedHourIndex + 1;
            int selectedMinute = selectedMinuteIndex;

            int hour24;
            if (selectedAmPm[0] == 0) {
                hour24 = (selectedHour12 == 12) ? 0 : selectedHour12;
            } else {
                hour24 = (selectedHour12 == 12) ? 12 : selectedHour12 + 12;
            }

            int totalMinutes = hour24 * 60 + selectedMinute;

            if (isStart) {
                binding.startMinutes = totalMinutes;
                binding.btnStart.setText(formatMinutes(totalMinutes));
                binding.btnStart.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
            } else {
                binding.endMinutes = totalMinutes;
                binding.btnEnd.setText(formatMinutes(totalMinutes));
                binding.btnEnd.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateAmPmButtons(TextView btnAm, TextView btnPm, int selectedAmPm) {
        boolean isAmSelected = selectedAmPm == 0;

        btnAm.setBackgroundResource(isAmSelected
                ? R.drawable.bg_form_chip_selected
                : R.drawable.bg_form_chip_unselected);
        btnPm.setBackgroundResource(!isAmSelected
                ? R.drawable.bg_form_chip_selected
                : R.drawable.bg_form_chip_unselected);

        btnAm.setTextColor(ContextCompat.getColor(
                requireContext(),
                isAmSelected ? android.R.color.white : R.color.green_primary
        ));
        btnPm.setTextColor(ContextCompat.getColor(
                requireContext(),
                !isAmSelected ? android.R.color.white : R.color.green_primary
        ));
    }

    private void setupWheelRecycler(
            RecyclerView recyclerView,
            TimeWheelAdapter adapter,
            int initialPosition
    ) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
        );

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        recyclerView.setNestedScrollingEnabled(false);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.post(() -> {
            int itemHeight = dpToPx(72);
            int offset = recyclerView.getHeight() / 2 - itemHeight / 2;
            layoutManager.scrollToPositionWithOffset(initialPosition, offset);

            recyclerView.post(() -> {
                int centered = getCenteredAdapterPosition(recyclerView);
                adapter.setSelectedPosition(centered);
            });
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    rv.post(() -> {
                        int centered = getCenteredAdapterPosition(rv);
                        adapter.setSelectedPosition(centered);
                    });
                }
            }
        });
    }

//    private void updateWheelSelection(
//            RecyclerView recyclerView,
//            TimeWheelAdapter adapter,
//            LinearSnapHelper snapHelper
//    ) {
//        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
//        if (layoutManager == null) return;
//
//        View snapView = snapHelper.findSnapView(layoutManager);
//        if (snapView == null) return;
//
//        int position = layoutManager.getPosition(snapView);
//        if (position == RecyclerView.NO_POSITION) return;
//
//        adapter.setSelectedPosition(position);
//    }

    private int getCenteredAdapterPosition(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) return 0;

        int centerY = recyclerView.getHeight() / 2;

        View closestChild = null;
        int closestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            int childCenterY = (child.getTop() + child.getBottom()) / 2;
            int distance = Math.abs(childCenterY - centerY);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestChild = child;
            }
        }

        if (closestChild == null) return 0;

        int position = recyclerView.getChildAdapterPosition(closestChild);
        if (position == RecyclerView.NO_POSITION) return 0;

        return position;
    }

    private void showScheduleDetailDialog(ScheduleEntryEntity entry) {
        if (!isAdded()) return;

        View detailView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_schedule_detail, null, false);

        TextView tvCode = detailView.findViewById(R.id.tv_detail_code);
        TextView tvTitle = detailView.findViewById(R.id.tv_detail_title);
        TextView tvProfessor = detailView.findViewById(R.id.tv_detail_professor);
        TextView tvSchedule = detailView.findViewById(R.id.tv_detail_schedule);
        TextView tvLocation = detailView.findViewById(R.id.tv_detail_location);
        TextView tvReminder = detailView.findViewById(R.id.tv_detail_reminder);
        TextView tvNotes = detailView.findViewById(R.id.tv_detail_notes);

        tvCode.setText(entry.courseCode != null && !entry.courseCode.isBlank() ? entry.courseCode : "No code");
        tvTitle.setText(entry.title != null ? entry.title : "Class");
        tvProfessor.setText(entry.instructor != null && !entry.instructor.isBlank()
                ? "Prof. " + entry.instructor
                : "Professor not set");
        tvSchedule.setText(dayToLabel(entry.dayOfWeek) + " • " +
                formatMinutes(entry.startMinutes) + " - " + formatMinutes(entry.endMinutes));

        if (entry.isOnline == 1) {
            tvLocation.setText("Location: Online" +
                    (entry.onlinePlatform != null ? " • " + entry.onlinePlatform : ""));
        } else {
            tvLocation.setText("Location: " + resolveLocation(entry));
        }

        int reminder = entry.reminderMinutes;
        tvReminder.setText(reminder > 0
                ? "Reminder: " + reminder + " minutes before"
                : "Reminder: Off");
        tvNotes.setText(entry.notes != null && !entry.notes.isBlank() ? entry.notes : "No notes");

        AlertDialog detailDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(detailView)
                .create();

        TextView btnDelete = detailView.findViewById(R.id.btn_detail_delete);
        TextView btnEdit = detailView.findViewById(R.id.btn_detail_edit);
        TextView btnDirections = detailView.findViewById(R.id.btn_detail_directions);

        if (entry.isOnline == 1 || entry.roomId == null) {
            btnDirections.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            detailDialog.dismiss();
            showScheduleFormDialog(entry);
        });

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete schedule")
                    .setMessage("Remove this class from your schedule?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) ->
                            viewModel.deleteSchedule(entry, new ScheduleViewModel.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        detailDialog.dismiss();
                                        Toast.makeText(requireContext(), "Schedule deleted", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
                                }
                            }))
                    .show();
        });

        btnDirections.setOnClickListener(v -> {
            if (entry.roomId == null) return;
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("roomId", entry.roomId);
            args.putString("roomName", roomNameMap.get(entry.roomId));
            navController.navigate(R.id.action_schedule_to_roomDetail, args);
            detailDialog.dismiss();
        });

        detailDialog.show();
    }

    private void setupColorPicker(View formView, int[] selectedColorIndex, int currentSelected) {
        int[] colorViewIds = {
                R.id.color_0,
                R.id.color_1,
                R.id.color_2,
                R.id.color_3,
                R.id.color_4,
                R.id.color_5
        };

        for (int i = 0; i < colorViewIds.length; i++) {
            View colorView = formView.findViewById(colorViewIds[i]);
            int index = i;
            colorView.setOnClickListener(v -> {
                selectedColorIndex[0] = index;
                applyColorSelection(formView, selectedColorIndex[0]);
            });
        }

        selectedColorIndex[0] = currentSelected;
        applyColorSelection(formView, currentSelected);
    }

    private void applyColorSelection(View formView, int selectedIndex) {
        int[] colorViewIds = {
                R.id.color_0,
                R.id.color_1,
                R.id.color_2,
                R.id.color_3,
                R.id.color_4,
                R.id.color_5
        };

        for (int i = 0; i < colorViewIds.length; i++) {
            View colorView = formView.findViewById(colorViewIds[i]);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(getScheduleColors()[i]));
            drawable.setStroke(1, Color.parseColor(i == selectedIndex ? "#BDBDBD" : "#D0D0D0"));
            colorView.setBackground(drawable);

            float targetScale = i == selectedIndex ? 1.18f : 1f;
            colorView.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(180)
                    .setInterpolator(new OvershootInterpolator(0.7f))
                    .start();
        }
    }

    private void maybeRequestNotificationPermission(int reminderMinutes) {
        if (reminderMinutes <= 0) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private int getDefaultDay() {
        int weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (weekday) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            default:
                return 1;
        }
    }

    private String buildRoomDisplay(RoomEntity room) {
        String name = room.name != null ? room.name : "Room";
        if (room.code != null && !room.code.isBlank()) {
            return name + " (" + room.code + ")";
        }
        return name;
    }

    private String resolveLocation(ScheduleEntryEntity entry) {
        if (entry.roomId != null && roomNameMap.containsKey(entry.roomId)) {
            return roomNameMap.get(entry.roomId);
        }
        return "Room not set";
    }

    private boolean isCampusAreaCode(String code) {
        if (code == null) return false;
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return "COURT".equals(normalized) || "ENT".equals(normalized) || "EXIT".equals(normalized);
    }

    private int positionToReminder(int position) {
        switch (position) {
            case 1:
                return 5;
            case 2:
                return 10;
            case 3:
                return 15;
            case 4:
                return 30;
            default:
                return 0;
        }
    }

    private int reminderToPosition(int reminderMinutes) {
        switch (reminderMinutes) {
            case 5:
                return 1;
            case 10:
                return 2;
            case 15:
                return 3;
            case 30:
                return 4;
            default:
                return 0;
        }
    }

    private String dayToLabel(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > DAY_LABELS.length) {
            return "Mon";
        }
        return DAY_LABELS[dayOfWeek - 1];
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatMinutes(int minutes) {
        int hour24 = Math.max(0, Math.min(23, minutes / 60));
        int minute = Math.max(0, Math.min(59, minutes % 60));
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        String suffix = hour24 >= 12 ? "PM" : "AM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, suffix);
    }

    private String[] getScheduleColors() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            return new String[]{"#3A3A3A", "#4D2A3A", "#2A3D4D", "#3A2D4D", "#2D3A2D", "#4D4A2A"};
        } else {
            return new String[]{"#F2F2F2", "#F8D1E2", "#D7E8FF", "#EFD8F7", "#D7F1D5", "#EDF58F"};
        }
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class ScheduleBlockBinding {
        View root;
        TextView label;
        ImageView delete;
        LinearLayout dayContainer;
        TextView btnStart;
        TextView btnEnd;

        final List<TextView> dayChips = new ArrayList<>();

        int selectedDay = 1;
        int startMinutes = -1;
        int endMinutes = -1;
    }
}