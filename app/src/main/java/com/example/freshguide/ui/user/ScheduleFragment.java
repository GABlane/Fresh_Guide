package com.example.freshguide.ui.user;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.ui.adapter.ScheduleEntryAdapter;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.viewmodel.ScheduleViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private static final String TAG = "ScheduleFragment";

    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final int[] FORM_DAY_VIEW_IDS = {
            R.id.form_day_mon,
            R.id.form_day_tue,
            R.id.form_day_wed,
            R.id.form_day_thu,
            R.id.form_day_fri,
            R.id.form_day_sat
    };

    private String[] getScheduleColors() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            return new String[]{"#3A3A3A", "#4D2A3A", "#2A3D4D", "#3A2D4D", "#2D3A2D", "#4D4A2A"};
        } else {
            return new String[]{"#F2F2F2", "#F8D1E2", "#D7E8FF", "#EFD8F7", "#D7F1D5", "#EDF58F"};
        }
    }

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
    private TextView tvDate;
    private TextView tvSummaryLabel;
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

        selectedDay = normalizeDay(sessionManager.getSelectedScheduleDay(getDefaultDay()));
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

        int restoredDay = normalizeDay(sessionManager.getSelectedScheduleDay(getDefaultDay()));
        if (selectedDay != restoredDay) {
            selectedDay = restoredDay;
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
        for (int i = 0; i < dailyDayViews.length; i++) {
            TextView dayView = dailyDayViews[i];
            int day = i + 1;

            dayView.setOnClickListener(v -> {
                selectedDay = day;
                sessionManager.setSelectedScheduleDay(day);
                applyDailyDaySelectionUi();
                renderScheduleContent();
            });
        }
    }

    private void applyDailyDaySelectionUi() {
        for (int i = 0; i < dailyDayViews.length; i++) {
            TextView dayView = dailyDayViews[i];
            boolean selected = (i + 1) == selectedDay;

            dayView.setBackgroundResource(
                    selected ? R.drawable.bg_schedule_day_selected : R.drawable.bg_schedule_day_plain
            );
            dayView.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? R.color.green_primary : R.color.text_primary
            ));
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
                tvEmptyStateMessage.setText("No schedules yet.");
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

        Spinner spinnerClassType = formView.findViewById(R.id.spinner_class_type);
        Spinner spinnerRoom = formView.findViewById(R.id.spinner_room_location);
        Spinner spinnerPlatform = formView.findViewById(R.id.spinner_platform);
        Spinner spinnerReminder = formView.findViewById(R.id.spinner_reminder);

        LinearLayout roomGroup = formView.findViewById(R.id.room_group);
        LinearLayout onlineGroup = formView.findViewById(R.id.online_group);
        TextView btnPickStart = formView.findViewById(R.id.btn_pick_start);
        TextView btnPickEnd = formView.findViewById(R.id.btn_pick_end);

        String[] classTypes = new String[]{"On-site", "Online"};
        spinnerClassType.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                classTypes
        ));

        List<String> roomItems = new ArrayList<>();
        roomItems.add("Select Room");
        for (RoomEntity room : roomOptions) {
            roomItems.add(buildRoomDisplay(room));
        }
        spinnerRoom.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roomItems
        ));

        String[] platforms = new String[]{"Zoom", "Google Meet", "Microsoft Teams", "Other"};
        spinnerPlatform.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                platforms
        ));

        String[] reminderOptions = new String[]{
                "No reminder", "5 mins before", "10 mins before", "15 mins before", "30 mins before"
        };
        spinnerReminder.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                reminderOptions
        ));

        spinnerClassType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean online = position == 1;
                roomGroup.setVisibility(online ? View.GONE : View.VISIBLE);
                onlineGroup.setVisibility(online ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        int[] selectedDayHolder = new int[]{selectedDay};
        setupFormDaySelection(formView, selectedDayHolder, selectedDay);

        int[] startMinutesHolder = new int[]{-1};
        int[] endMinutesHolder = new int[]{-1};
        btnPickStart.setOnClickListener(v -> showTimePicker(startMinutesHolder, btnPickStart));
        btnPickEnd.setOnClickListener(v -> showTimePicker(endMinutesHolder, btnPickEnd));

        int[] selectedColorIndex = new int[]{0};
        setupColorPicker(formView, selectedColorIndex, 0);

        if (existing == null) {
            spinnerReminder.setSelection(reminderToPosition(sessionManager.getDefaultReminderMinutes()));
        }

        if (existing != null) {
            etSubjectName.setText(existing.title != null ? existing.title : "");
            etSubjectCode.setText(existing.courseCode != null ? existing.courseCode : "");
            etProfessor.setText(existing.instructor != null ? existing.instructor : "");
            etNotes.setText(existing.notes != null ? existing.notes : "");

            selectedDayHolder[0] = existing.dayOfWeek;
            setupFormDaySelection(formView, selectedDayHolder, existing.dayOfWeek);

            startMinutesHolder[0] = existing.startMinutes;
            endMinutesHolder[0] = existing.endMinutes;

            btnPickStart.setText(formatMinutes(existing.startMinutes));
            btnPickStart.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));

            btnPickEnd.setText(formatMinutes(existing.endMinutes));
            btnPickEnd.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));

            if (existing.colorHex != null) {
                for (int i = 0; i < getScheduleColors().length; i++) {
                    if (getScheduleColors()[i].equalsIgnoreCase(existing.colorHex)) {
                        selectedColorIndex[0] = i;
                        break;
                    }
                }
            }

            setupColorPicker(formView, selectedColorIndex, selectedColorIndex[0]);

            int typeIndex = existing.isOnline == 1 ? 1 : 0;
            spinnerClassType.setSelection(typeIndex);

            if (existing.isOnline == 1) {
                int platformIndex = 0;
                if (existing.onlinePlatform != null) {
                    for (int i = 0; i < platforms.length; i++) {
                        if (platforms[i].equalsIgnoreCase(existing.onlinePlatform)) {
                            platformIndex = i;
                            break;
                        }
                    }
                }
                spinnerPlatform.setSelection(platformIndex);
            } else if (existing.roomId != null) {
                int roomSelection = 0;
                for (int i = 0; i < roomOptions.size(); i++) {
                    if (roomOptions.get(i).id == existing.roomId) {
                        roomSelection = i + 1;
                        break;
                    }
                }
                spinnerRoom.setSelection(roomSelection);
            }

            spinnerReminder.setSelection(reminderToPosition(existing.reminderMinutes));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(formView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton(existing == null ? "Create Schedule" : "Save Changes", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String title = etSubjectName.getText().toString().trim();
                if (title.isEmpty()) {
                    etSubjectName.setError("Subject name is required");
                    return;
                }

                if (startMinutesHolder[0] < 0 || endMinutesHolder[0] < 0) {
                    Toast.makeText(requireContext(), "Please select start and end time", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (endMinutesHolder[0] <= startMinutesHolder[0]) {
                    Toast.makeText(requireContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean online = spinnerClassType.getSelectedItemPosition() == 1;
                Integer roomId = null;
                String platform = null;

                if (online) {
                    platform = spinnerPlatform.getSelectedItem() != null
                            ? String.valueOf(spinnerPlatform.getSelectedItem())
                            : null;
                } else {
                    int roomPosition = spinnerRoom.getSelectedItemPosition();
                    if (roomPosition <= 0 || roomOptions.isEmpty() || roomPosition - 1 >= roomOptions.size()) {
                        Toast.makeText(requireContext(), "Please choose a room location", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    roomId = roomOptions.get(roomPosition - 1).id;
                }

                long now = System.currentTimeMillis();
                int reminderMinutes = positionToReminder(spinnerReminder.getSelectedItemPosition());
                if (!sessionManager.isScheduleNotificationsEnabled()) {
                    reminderMinutes = 0;
                }

                int colorIndex = selectedColorIndex[0];
                if (colorIndex < 0 || colorIndex >= getScheduleColors().length) {
                    colorIndex = 0;
                }

                ScheduleEntryEntity entry = new ScheduleEntryEntity(
                        title,
                        normalize(etSubjectCode.getText().toString()),
                        normalize(etProfessor.getText().toString()),
                        normalize(etNotes.getText().toString()),
                        getScheduleColors()[colorIndex],
                        selectedDayHolder[0],
                        startMinutesHolder[0],
                        endMinutesHolder[0],
                        online ? 1 : 0,
                        roomId,
                        platform,
                        reminderMinutes,
                        existing != null ? existing.createdAt : now,
                        now
                );

                if (existing != null) {
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
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "Schedule saved", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "saveSchedule failed: " + message);
                        if (!isAdded() || getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Create/Save schedule flow crashed", e);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Unable to save schedule right now", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        dialog.show();
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

    private void showTimePicker(int[] holder, TextView target) {
        int initialHour = holder[0] >= 0 ? holder[0] / 60 : 9;
        int initialMinute = holder[0] >= 0 ? holder[0] % 60 : 0;

        android.app.TimePickerDialog dialog = new android.app.TimePickerDialog(
                requireContext(),
                (TimePicker view, int hourOfDay, int minute) -> {
                    holder[0] = (hourOfDay * 60) + minute;
                    target.setText(formatMinutes(holder[0]));
                    target.setTextColor(ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
                },
                initialHour,
                initialMinute,
                false
        );
        dialog.show();
    }

    private void setupFormDaySelection(View formView, int[] selectedDayHolder, int selectedDayValue) {
        for (int i = 0; i < FORM_DAY_VIEW_IDS.length; i++) {
            TextView tv = formView.findViewById(FORM_DAY_VIEW_IDS[i]);
            int day = i + 1;
            tv.setOnClickListener(v -> {
                selectedDayHolder[0] = day;
                applyFormDayUi(formView, selectedDayHolder[0]);
            });
        }

        selectedDayHolder[0] = selectedDayValue;
        applyFormDayUi(formView, selectedDayHolder[0]);
    }

    private void applyFormDayUi(View formView, int selectedDayValue) {
        for (int i = 0; i < FORM_DAY_VIEW_IDS.length; i++) {
            TextView tv = formView.findViewById(FORM_DAY_VIEW_IDS[i]);
            boolean selected = (i + 1) == selectedDayValue;
            tv.setBackgroundResource(selected ? R.drawable.bg_schedule_day_selected : R.drawable.bg_schedule_day_plain);
            tv.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? R.color.green_primary : R.color.text_primary
            ));
        }
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
            drawable.setStroke(i == selectedIndex ? 3 : 1,
                    Color.parseColor(i == selectedIndex ? "#5A5A5A" : "#D0D0D0"));
            colorView.setBackground(drawable);
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

    private int normalizeDay(int value) {
        if (value < 1) return 1;
        if (value > 6) return 6;
        return value;
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
}