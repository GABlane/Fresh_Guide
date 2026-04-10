package com.example.freshguide.ui.user;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.freshguide.BuildConfig;
import com.example.freshguide.R;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.ui.adapter.RoomDropdownAdapter;
import com.example.freshguide.ui.adapter.TimeWheelAdapter;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.viewmodel.ScheduleViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
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

    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] CLASS_TYPES = {"On-site", "Online"};
    private static final String[] PLATFORMS = {"Zoom", "Google Meet", "Microsoft Teams", "Other"};
    private static final String[] REMINDER_OPTIONS = {
            "No reminder", "5 mins before", "10 mins before", "15 mins before", "30 mins before"
    };
    private static final int HOUR_WHEEL_VALUE_COUNT = 12;
    private static final int MINUTE_WHEEL_VALUE_COUNT = 60;
    private static final int HOUR_WHEEL_REPEAT_COUNT = 200;
    private static final int MIN_CLASS_START_MINUTES = 6 * 60;
    private static final int MAX_CLASS_END_MINUTES = 23 * 60;
    private static final int MIN_CLASS_DURATION_MINUTES = 3 * 60;
    private static final int SUNDAY_DAY_VALUE = 7;
    private static final String INVALID_CLASS_SCHEDULE_MESSAGE = "Invalid class schedule.";
    private static final String DUPLICATE_SCHEDULE_MESSAGE = "Schedule already exists.";
    private static final String SUNDAY_NOT_ALLOWED_MESSAGE = "Sunday classes are not allowed.";

    // ── Timeline sizing constants ──────────────────────────────────────────────
    private static final float DP_PER_MINUTE = 1.5f;
    private static final int MIN_BLOCK_HEIGHT_DP = 48;
    private static final int LABEL_WIDTH_DP = 36;
    private static final int DAY_COLUMN_WIDTH_DP = 68;
    private static final int DAY_COLUMN_COUNT = DAY_LABELS.length;

    private View summaryContent;

    @Nullable
    private String lastRenderedScheduleSignature = null;
    private int lastRenderedToday = -1;
    private boolean initialScheduleUiResolved = false;
    @Nullable
    private Runnable pendingInitialScheduleUiRunnable = null;


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

    // ── Views ──────────────────────────────────────────────────────────────────
    private View timelineScrollView;
    private FrameLayout timelineContainer;
    private View dailyDaySelectorContainer;
    private View emptyState;
    private View cardSummary;

    private TextView tvSummaryCode;
    private TextView tvSummaryTitle;
    private TextView tvSummaryProfessor;
    private TextView tvSummaryTime;
    private TextView tvSummaryLabel;
    private TextView tvDate;
    private TextView tvEmptyStateMessage;
    private TextView tvSummarySubjectCode;
    private TextView tvSummaryLocation;

    // ── Room data ──────────────────────────────────────────────────────────────
    private final List<RoomEntity> allRooms = new ArrayList<>();
    private final List<RoomEntity> roomOptions = new ArrayList<>();
    private final Map<Integer, String> roomNameMap = new HashMap<>();

    // ── Network ────────────────────────────────────────────────────────────────
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
//        dayHeaderHost = view.findViewById(R.id.day_header_host);

        ScheduleReminderHelper.ensureNotificationChannel(requireContext());

        // ── Bind views ────────────────────────────────────────────────────────
        tvDate = view.findViewById(R.id.tv_schedule_date);
        timelineScrollView = view.findViewById(R.id.timeline_scroll);
        timelineContainer = view.findViewById(R.id.timeline_container);
        dailyDaySelectorContainer = view.findViewById(R.id.daily_day_selector_container);
        emptyState = view.findViewById(R.id.empty_state);
        cardSummary = view.findViewById(R.id.card_today_summary);
        summaryContent = view.findViewById(R.id.layout_summary_content);

        tvSummaryCode      = view.findViewById(R.id.tv_summary_course_code);
        tvSummaryTitle     = view.findViewById(R.id.tv_summary_title);
        tvSummaryProfessor = view.findViewById(R.id.tv_summary_professor);
        tvSummaryTime      = view.findViewById(R.id.tv_summary_time);
        tvSummaryLabel     = view.findViewById(R.id.tv_summary_label);
        tvEmptyStateMessage = view.findViewById(R.id.tv_empty_state_message);

        tvSummarySubjectCode = view.findViewById(R.id.tv_summary_subject_code);
        tvSummaryLocation = view.findViewById(R.id.tv_summary_location);

        hideScheduleContentUntilResolved();

        tvDate.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));
        view.findViewById(R.id.btn_add_schedule).setOnClickListener(v -> showScheduleFormDialog(null));
        view.findViewById(R.id.btn_empty_add_schedule).setOnClickListener(v -> showScheduleFormDialog(null));

        selectedDay = getDefaultDay();
        setupDailyDaySelector();

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
            resolveScheduleUi(allSchedules);
        });
    }

    private final Handler summaryRefreshHandler = new Handler(Looper.getMainLooper());

    private final Runnable summaryRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            if (initialScheduleUiResolved) {
                updateSummaryCard(allSchedules);
            }
            summaryRefreshHandler.postDelayed(this, 60_000L);
        }
    };

    private void startSummaryAutoRefresh() {
        summaryRefreshHandler.removeCallbacks(summaryRefreshRunnable);
        summaryRefreshRunnable.run();
    }

    private void stopSummaryAutoRefresh() {
        summaryRefreshHandler.removeCallbacks(summaryRefreshRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.syncSchedules();

        int today = getDefaultDay();
        if (selectedDay != today) {
            selectedDay = today;
        }

        resolveScheduleUi(allSchedules);
        startSummaryAutoRefresh();
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

    @Override
    public void onPause() {
        super.onPause();
        stopSummaryAutoRefresh();
        if (pendingInitialScheduleUiRunnable != null) {
            summaryRefreshHandler.removeCallbacks(pendingInitialScheduleUiRunnable);
            pendingInitialScheduleUiRunnable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up all handler callbacks to prevent memory leaks
        summaryRefreshHandler.removeCallbacksAndMessages(null);
        unregisterScheduleNetworkCallback();
    }

    private void hideScheduleContentUntilResolved() {
        if (cardSummary != null) {
            cardSummary.setVisibility(View.INVISIBLE);
        }
        if (dailyDaySelectorContainer != null) {
            dailyDaySelectorContainer.setVisibility(View.INVISIBLE);
        }
        if (timelineScrollView != null) {
            timelineScrollView.setVisibility(View.INVISIBLE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(View.INVISIBLE);
        }
    }

    private void resolveScheduleUi(@Nullable List<ScheduleEntryEntity> schedules) {
        boolean hasSchedules = schedules != null && !schedules.isEmpty();

        if (!initialScheduleUiResolved) {
            if (hasSchedules) {
                if (pendingInitialScheduleUiRunnable != null) {
                    summaryRefreshHandler.removeCallbacks(pendingInitialScheduleUiRunnable);
                    pendingInitialScheduleUiRunnable = null;
                }
                initialScheduleUiResolved = true;
                updateSummaryCard(schedules);
                renderScheduleContent();
                return;
            }

            if (pendingInitialScheduleUiRunnable == null) {
                pendingInitialScheduleUiRunnable = () -> {
                    initialScheduleUiResolved = true;
                    updateSummaryCard(allSchedules);
                    renderScheduleContent();
                    pendingInitialScheduleUiRunnable = null;
                };
                summaryRefreshHandler.postDelayed(pendingInitialScheduleUiRunnable, 260L);
            }
            return;
        }

        updateSummaryCard(schedules);
        renderScheduleContent();
    }


    private void registerScheduleNetworkCallback() {
        if (!isAdded() || networkCallbackRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

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
        if (!networkCallbackRegistered || connectivityManager == null || scheduleNetworkCallback == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(scheduleNetworkCallback);
        } catch (Exception ignored) {}
        networkCallbackRegistered = false;
        scheduleNetworkCallback = null;
        connectivityManager = null;
    }


    private android.widget.HorizontalScrollView dayHeaderScroll;
    private LinearLayout dayHeaderRow;

    private void setupDailyDaySelector() {
        if (!isAdded() || dailyDaySelectorContainer == null) return;

        LinearLayout container = (LinearLayout) dailyDaySelectorContainer;
        container.removeAllViews();

        View gutter = new View(requireContext());
        LinearLayout.LayoutParams gutterParams = new LinearLayout.LayoutParams(dpToPx(LABEL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT);
        gutter.setLayoutParams(gutterParams);
        container.addView(gutter);

        dayHeaderScroll = new android.widget.HorizontalScrollView(requireContext());
        dayHeaderScroll.setHorizontalScrollBarEnabled(false);
        dayHeaderScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        dayHeaderScroll.setFillViewport(false);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        dayHeaderScroll.setLayoutParams(scrollParams);

        dayHeaderRow = new LinearLayout(requireContext());
        dayHeaderRow.setOrientation(LinearLayout.HORIZONTAL);

        float density = requireContext().getResources().getDisplayMetrics().density;
        int colWidthPx = (int) (DAY_COLUMN_WIDTH_DP * density);

        for (int i = 0; i < DAY_COLUMN_COUNT; i++) {
            TextView tvDay = new TextView(requireContext());
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(colWidthPx, ViewGroup.LayoutParams.MATCH_PARENT);
            tvDay.setLayoutParams(lp);
            tvDay.setText(DAY_LABELS[i]);
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setTextSize(13f);
            tvDay.setClickable(false);
            tvDay.setFocusable(false);
            dayHeaderRow.addView(tvDay);
        }

        dayHeaderScroll.addView(dayHeaderRow);
        container.addView(dayHeaderScroll);

        updateFixedDayHeaderUi();
    }

    private void updateFixedDayHeaderUi() {
        if (dayHeaderRow == null || !isAdded()) return;

        int today = getDefaultDay();
        for (int i = 0; i < dayHeaderRow.getChildCount(); i++) {
            View child = dayHeaderRow.getChildAt(i);
            if (!(child instanceof TextView)) continue;

            TextView tvDay = (TextView) child;
            boolean isToday = (i + 1) == today;

            tvDay.setTextColor(ContextCompat.getColor(requireContext(),
                    isToday ? R.color.green_primary : R.color.text_primary));
            tvDay.setBackground(isToday
                    ? ContextCompat.getDrawable(requireContext(), R.drawable.bg_schedule_day_selected)
                    : null);
        }
    }

    private void applyDailyDaySelectionUi() {
        renderScheduleContent();
    }


    private void renderScheduleContent() {
        boolean hasAnySchedules = !allSchedules.isEmpty();

        dailyDaySelectorContainer.setVisibility(hasAnySchedules ? View.VISIBLE : View.GONE);

        if (!hasAnySchedules) {
            timelineScrollView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            tvEmptyStateMessage.setText("No schedule yet.");
            return;
        }

        timelineScrollView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        int today = getDefaultDay();
        String signature = buildScheduleSignature(allSchedules);

        boolean needsFullRebuild =
                lastRenderedScheduleSignature == null
                        || !lastRenderedScheduleSignature.equals(signature)
                        || lastRenderedToday != today;

        if (needsFullRebuild) {
            buildTimeline(allSchedules);
            lastRenderedScheduleSignature = signature;
            lastRenderedToday = today;
        }
    }

    private String buildScheduleSignature(@Nullable List<ScheduleEntryEntity> entries) {
        if (entries == null || entries.isEmpty()) return "empty";

        StringBuilder sb = new StringBuilder();
        for (ScheduleEntryEntity e : entries) {
            sb.append(e.id).append('|')
                    .append(e.dayOfWeek).append('|')
                    .append(e.startMinutes).append('|')
                    .append(e.endMinutes).append('|')
                    .append(e.title).append('|')
                    .append(e.courseCode).append('|')
                    .append(e.colorHex).append(';');
        }
        return sb.toString();
    }

    private void buildTimeline(List<ScheduleEntryEntity> entries) {
        if (!isAdded() || timelineContainer == null || entries == null || entries.isEmpty()) return;

        timelineContainer.removeAllViews();
        timelineContainer.setAlpha(0f);
        if (dailyDaySelectorContainer != null) {
            dailyDaySelectorContainer.setAlpha(0f);
        }

        int minStart = Integer.MAX_VALUE;
        int maxEnd = Integer.MIN_VALUE;

        for (ScheduleEntryEntity e : entries) {
            minStart = Math.min(minStart, e.startMinutes);
            maxEnd = Math.max(maxEnd, e.endMinutes);
        }

        int startHour = 6;
        int endHour = 23;
        int timelineStartMinutes = startHour * 60;

        float density = requireContext().getResources().getDisplayMetrics().density;
        float pxPerMinute = DP_PER_MINUTE * density;
        int labelWidthPx = (int) (LABEL_WIDTH_DP * density);
        int colWidthPx = (int) (DAY_COLUMN_WIDTH_DP * density);

        int totalMinutes = (endHour - startHour + 1) * 60;
        int totalHeightPx = (int) (totalMinutes * pxPerMinute) + (int) (32 * density);
        int gridTotalWidth = colWidthPx * DAY_COLUMN_COUNT;

        int colorHint = ContextCompat.getColor(requireContext(), R.color.schedule_time_text);
        int gridLineColor = ContextCompat.getColor(requireContext(), R.color.schedule_grid_major);
        int todayHighlightColor = ContextCompat.getColor(requireContext(), R.color.schedule_header_bg);
        int today = getDefaultDay();
        int todayColIndex = Math.max(0, Math.min(DAY_COLUMN_COUNT - 1, today - 1));

        android.widget.HorizontalScrollView hScroll =
                new android.widget.HorizontalScrollView(requireContext());
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hScroll.setFillViewport(false);

        LinearLayout verticalWrapper = new LinearLayout(requireContext());
        verticalWrapper.setOrientation(LinearLayout.VERTICAL);
        verticalWrapper.setLayoutParams(new ViewGroup.LayoutParams(
                gridTotalWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        hScroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (dayHeaderScroll != null) {
                dayHeaderScroll.scrollTo(hScroll.getScrollX(), 0);
            }
        });

        updateFixedDayHeaderUi();


        FrameLayout grid = new FrameLayout(requireContext());
        grid.setLayoutParams(new LinearLayout.LayoutParams(gridTotalWidth, totalHeightPx));

        View todayHighlight = new View(requireContext());
        FrameLayout.LayoutParams hlLp =
                new FrameLayout.LayoutParams(colWidthPx, totalHeightPx);
        hlLp.leftMargin = todayColIndex * colWidthPx;
        todayHighlight.setLayoutParams(hlLp);
        todayHighlight.setBackgroundColor(todayHighlightColor);
        grid.addView(todayHighlight);

        for (int hour = startHour; hour <= endHour; hour++) {
            int topPx = (int) (((hour - startHour) * 60) * pxPerMinute) + (int) (8 * density);

            View gridLine = new View(requireContext());
            FrameLayout.LayoutParams lineLp =
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lineLp.topMargin = topPx;
            gridLine.setLayoutParams(lineLp);
            gridLine.setBackgroundColor(gridLineColor);
            grid.addView(gridLine);
        }

        int colInset = (int) (3 * density);
        int blockWidth = colWidthPx - colInset * 2;

        for (ScheduleEntryEntity entry : entries) {
            int dayIndex = entry.dayOfWeek - 1;
            if (dayIndex < 0 || dayIndex >= DAY_COLUMN_COUNT) continue;

            int topMinutes = entry.startMinutes - timelineStartMinutes;
            int durationMinutes = entry.endMinutes - entry.startMinutes;
            int topPx = (int) (topMinutes * pxPerMinute) + (int) (8 * density);
            int heightPx = Math.max(
                    (int) (durationMinutes * pxPerMinute),
                    (int) (MIN_BLOCK_HEIGHT_DP * density));

            int blockLeft = dayIndex * colWidthPx + colInset;

            View block = buildScheduleBlock(entry, heightPx, density);
            FrameLayout.LayoutParams blockLp = new FrameLayout.LayoutParams(blockWidth, heightPx);
            blockLp.topMargin = topPx;
            blockLp.leftMargin = blockLeft;
            block.setLayoutParams(blockLp);
            grid.addView(block);
        }

        verticalWrapper.addView(grid);
        hScroll.addView(verticalWrapper);

        LinearLayout outerRow = new LinearLayout(requireContext());
        outerRow.setOrientation(LinearLayout.HORIZONTAL);
        outerRow.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                totalHeightPx));

        FrameLayout labelColumn = new FrameLayout(requireContext());
        labelColumn.setLayoutParams(new LinearLayout.LayoutParams(
                labelWidthPx, totalHeightPx));

        for (int hour = startHour; hour <= endHour; hour++) {
            int minutesFromStart = (hour - startHour) * 60;
            int topPx = (int) (minutesFromStart * pxPerMinute)
                    + (int) (8 * density);

            TextView tvLabel = new TextView(requireContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    labelWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = topPx - (int) (7 * density);
            tvLabel.setLayoutParams(lp);
            tvLabel.setText(formatHour(hour));
            tvLabel.setTextSize(9f);
            tvLabel.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            tvLabel.setPaddingRelative(0, 0, (int) (5 * density), 0);
            tvLabel.setTextColor(colorHint);
            labelColumn.addView(tvLabel);
        }

        outerRow.addView(labelColumn);

        LinearLayout.LayoutParams hScrollLp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hScroll.setLayoutParams(hScrollLp);
        outerRow.addView(hScroll);

        timelineContainer.addView(outerRow);

        ViewGroup.LayoutParams cLp = timelineContainer.getLayoutParams();
        if (cLp == null) {
            cLp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    totalHeightPx + (int) (36 * density));
        }
        cLp.height = totalHeightPx;
        timelineContainer.setLayoutParams(cLp);

        hScroll.post(() -> {
            int targetScroll = todayColIndex * colWidthPx
                    - (hScroll.getWidth() - colWidthPx) / 2;
            int finalTargetScroll = Math.max(0, targetScroll);
            int previewOffset = Math.max(0, finalTargetScroll - (int) (12 * density));
            hScroll.scrollTo(previewOffset, 0);

            timelineContainer.post(() -> {
                timelineContainer.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .start();

                if (dailyDaySelectorContainer != null) {
                    dailyDaySelectorContainer.animate()
                            .alpha(1f)
                            .setDuration(220)
                            .start();
                }

                hScroll.postDelayed(() -> hScroll.smoothScrollTo(finalTargetScroll, 0), 40);
            });
        });
    }

    private View buildScheduleBlock(ScheduleEntryEntity entry, int heightPx, float density) {
        View block = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_schedule_timeline_block, timelineContainer, false);

        TextView tvStartTime = block.findViewById(R.id.tv_block_start_time);
        TextView tvEndTime   = block.findViewById(R.id.tv_block_end_time);
        TextView tvTitle     = block.findViewById(R.id.tv_block_title);
        TextView tvProfessor = block.findViewById(R.id.tv_block_professor);
        TextView tvCode      = block.findViewById(R.id.tv_block_code);
        TextView tvLocation  = block.findViewById(R.id.tv_block_location);

        // ── Populate text ──────────────────────────────────────────────────────
        tvStartTime.setText(formatMinutes(entry.startMinutes));
        tvEndTime.setText(formatMinutes(entry.endMinutes));
        tvTitle.setText(entry.title != null && !entry.title.isBlank() ? entry.title : "Class");

        String professor = (entry.instructor != null && !entry.instructor.isBlank())
                ? "Prof. " + entry.instructor
                : "";
        tvProfessor.setText(professor);
        tvProfessor.setVisibility(professor.isEmpty() ? View.GONE : View.VISIBLE);

        String code = (entry.courseCode != null && !entry.courseCode.isBlank()) ? entry.courseCode : "";
        tvCode.setText(code);
        tvCode.setVisibility(code.isEmpty() ? View.GONE : View.VISIBLE);

        String location;
        if (entry.isOnline == 1) {
            location = "Online" +
                    (entry.onlinePlatform != null && !entry.onlinePlatform.isBlank()
                            ? " • " + entry.onlinePlatform : "");
        } else if (entry.roomId != null && roomNameMap.containsKey(entry.roomId)) {
            location = roomNameMap.get(entry.roomId);
        } else {
            location = "";
        }
        tvLocation.setText(location);
        tvLocation.setVisibility(location.isEmpty() ? View.GONE : View.VISIBLE);

        String[][] gradients = getScheduleGradients();
        int bgSlot = resolveSchedulePaletteSlot(entry.colorHex);

        int effectiveBgColor;
        try {
            effectiveBgColor = Color.parseColor(gradients[bgSlot][0]);
        } catch (Exception e) {
            effectiveBgColor = ContextCompat.getColor(requireContext(), R.color.schedule_card_bg);
        }

        block.setBackground(buildGlassBackground(bgSlot, 10f * density));

        // ── Palette-aware text colors ───────────────────────────────────────────
        int primaryText = getPalettePrimaryTextColor(bgSlot);
        int secondaryText = getPaletteSecondaryTextColor(bgSlot);

        tvStartTime.setTextColor(secondaryText);
        tvEndTime.setTextColor(secondaryText);
        tvTitle.setTextColor(primaryText);
        tvProfessor.setTextColor(secondaryText);
        tvCode.setTextColor(secondaryText);
        tvLocation.setTextColor(secondaryText);

        // ── Compact mode: hide extras if block is too short ────────────────────
        boolean isCompact = heightPx < (int) (60 * density);
        if (isCompact) {
            tvEndTime.setVisibility(View.GONE);
            tvLocation.setVisibility(View.GONE);
        }

        boolean isSmall = heightPx < (int) (48 * density);
        if (isSmall) {
            tvCode.setVisibility(View.GONE);
        }

        boolean isTiny = heightPx < (int) (38 * density);
        if (isTiny) {
            tvProfessor.setVisibility(View.GONE);
            tvStartTime.setVisibility(View.GONE);
        }

        block.setOnClickListener(v -> showScheduleFormDialog(entry));
        return block;
    }

    /**  Summary card **/
    private void updateSummaryCard(@Nullable List<ScheduleEntryEntity> schedules) {
        if (summaryContent != null) {
            summaryContent.animate().cancel();
            summaryContent.setAlpha(1f);
            summaryContent.setTranslationY(0f);
        }
        SummaryState summaryState = resolveSummaryState(schedules);
        cardSummary.setVisibility(View.VISIBLE);

        if (summaryState.type == SummaryState.NO_SCHEDULE_YET) {
            tvSummaryLabel.setText("NO SCHEDULE YET");
            tvSummarySubjectCode.setText("");
            tvSummaryTitle.setText("GET STARTED");
            tvSummaryProfessor.setText("Add your first class to build your schedule.");
            tvSummaryLocation.setText("");
            tvSummaryTime.setText("Your classes will appear here.");
            tvSummaryCode.setText("");

            tvSummarySubjectCode.setVisibility(View.GONE);
            tvSummaryLocation.setVisibility(View.GONE);

            resetSummaryCardColors();
            return;
        }

        if (summaryState.type == SummaryState.FREE_TIME || summaryState.entry == null) {
            tvSummaryLabel.setText("TAKE YOUR TIME");
            tvSummarySubjectCode.setText("");
            tvSummaryTitle.setText("FREE TIME");
            tvSummaryProfessor.setText("Use this time to rest, study, or prepare for your next class.");
            tvSummaryLocation.setText("");
            tvSummaryTime.setText("No more classes right now.");
            tvSummaryCode.setText("");

            tvSummarySubjectCode.setVisibility(View.GONE);
            tvSummaryLocation.setVisibility(View.GONE);

            resetSummaryCardColors();
            return;
        }

        ScheduleEntryEntity entry = summaryState.entry;

        if (summaryState.type == SummaryState.ONGOING_NOW) {
            tvSummaryLabel.setText("YOU HAVE ONGOING CLASS NOW");
        } else {
            tvSummaryLabel.setText("NEXT CLASS");
        }

        String subjectCode = (entry.courseCode != null && !entry.courseCode.isBlank())
                ? entry.courseCode.toUpperCase(Locale.getDefault())
                : "";
        tvSummarySubjectCode.setText(subjectCode);
        tvSummarySubjectCode.setVisibility(subjectCode.isEmpty() ? View.GONE : View.VISIBLE);

        tvSummaryTitle.setText(entry.title != null && !entry.title.isBlank()
                ? entry.title.toUpperCase(Locale.getDefault())
                : "CLASS");

        if (entry.instructor != null && !entry.instructor.isBlank()) {
            tvSummaryProfessor.setText(("PROF. " + entry.instructor).toUpperCase(Locale.getDefault()));
        } else {
            tvSummaryProfessor.setText("");
        }

        String location;
        if (entry.isOnline == 1) {
            location = "ONLINE" + (
                    entry.onlinePlatform != null && !entry.onlinePlatform.isBlank()
                            ? " • " + entry.onlinePlatform.toUpperCase(Locale.getDefault())
                            : ""
            );
        } else {
            location = resolveLocation(entry).toUpperCase(Locale.getDefault());
        }

        tvSummaryLocation.setText(location);
        tvSummaryLocation.setVisibility(location.isEmpty() ? View.GONE : View.VISIBLE);

        tvSummaryTime.setText(
                (formatMinutes(entry.startMinutes) + " - " + formatMinutes(entry.endMinutes))
                        .toUpperCase(Locale.getDefault()));
        tvSummaryCode.setText("");

        applySummaryCardPalette(entry);
    }

    /** Restores the summary card to its default theme colors. */
    private void resetSummaryCardColors() {
        MaterialCardView card = (MaterialCardView) cardSummary;

        card.setCardBackgroundColor(Color.TRANSPARENT);
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border_default));
        card.setStrokeWidth(1);

        if (summaryContent != null) {
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{
                            ContextCompat.getColor(requireContext(), R.color.background_subtle),
                            ContextCompat.getColor(requireContext(), R.color.background_card)
                    }
            );
            bg.setCornerRadius(dpToPx(24));
            summaryContent.setBackground(bg);
        }

        tvSummaryTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvSummaryLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint));
        tvSummaryProfessor.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvSummaryTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint));

        tvSummarySubjectCode.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvSummaryLocation.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    private void applySummaryCardPalette(@NonNull ScheduleEntryEntity entry) {
        try {
            String[][] gradients = getScheduleGradients();
            int bgSlot = resolveSchedulePaletteSlot(entry.colorHex);

            int effectiveBgColor;
            try {
                effectiveBgColor = Color.parseColor(gradients[bgSlot][0]);
            } catch (Exception e) {
                effectiveBgColor = ContextCompat.getColor(requireContext(), R.color.background_subtle);
            }

            MaterialCardView card = (MaterialCardView) cardSummary;
            card.setCardBackgroundColor(Color.TRANSPARENT);
            card.setStrokeColor(adjustColorAlpha(lightenColor(effectiveBgColor, 1.05f), 0.22f));
            card.setStrokeWidth(1);

            if (summaryContent != null) {
                summaryContent.setBackground(buildGlassBackground(bgSlot, dpToPx(24)));
            }

            int primaryText = getPalettePrimaryTextColor(bgSlot);
            int secondaryText = getPaletteSecondaryTextColor(bgSlot);

            tvSummaryTitle.setTextColor(primaryText);
            tvSummaryLabel.setTextColor(secondaryText);
            tvSummaryProfessor.setTextColor(secondaryText);
            tvSummaryTime.setTextColor(secondaryText);
            tvSummarySubjectCode.setTextColor(secondaryText);
            tvSummaryLocation.setTextColor(secondaryText);

        } catch (Exception e) {
            resetSummaryCardColors();
        }
    }

    private int getPalettePrimaryTextColor(int slotIndex) {
        boolean isDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDark) {
            switch (slotIndex) {
                case 0: return Color.parseColor("#F2F2F2"); // neutral
                case 1: return Color.parseColor("#FFD6E5"); // rose
                case 2: return Color.parseColor("#D6E8FF"); // blue
                case 3: return Color.parseColor("#E7D6FF"); // violet
                case 4: return Color.parseColor("#D8F5DB"); // green
                case 5: return Color.parseColor("#F5EFC2"); // gold
                default: return ContextCompat.getColor(requireContext(), R.color.text_primary);
            }
        } else {
            switch (slotIndex) {
                case 0: return Color.parseColor("#333333"); // neutral
                case 1: return Color.parseColor("#8E2C52"); // dark rose
                case 2: return Color.parseColor("#2F5D9A"); // deep blue
                case 3: return Color.parseColor("#6E3FA3"); // plum
                case 4: return Color.parseColor("#2F7A39"); // forest
                case 5: return Color.parseColor("#7A6A16"); // olive-gold
                default: return ContextCompat.getColor(requireContext(), R.color.text_primary);
            }
        }
    }

    private int getPaletteSecondaryTextColor(int slotIndex) {
        boolean isDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDark) {
            switch (slotIndex) {
                case 0: return adjustColorAlpha(Color.parseColor("#EAEAEA"), 0.72f);
                case 1: return adjustColorAlpha(Color.parseColor("#FFD6E5"), 0.72f);
                case 2: return adjustColorAlpha(Color.parseColor("#D6E8FF"), 0.72f);
                case 3: return adjustColorAlpha(Color.parseColor("#E7D6FF"), 0.72f);
                case 4: return adjustColorAlpha(Color.parseColor("#D8F5DB"), 0.72f);
                case 5: return adjustColorAlpha(Color.parseColor("#F5EFC2"), 0.72f);
                default: return ContextCompat.getColor(requireContext(), R.color.text_secondary);
            }
        } else {
            switch (slotIndex) {
                case 0: return Color.parseColor("#666666");
                case 1: return Color.parseColor("#A24B6E");
                case 2: return Color.parseColor("#4F78B1");
                case 3: return Color.parseColor("#855DB5");
                case 4: return Color.parseColor("#4D8D56");
                case 5: return Color.parseColor("#958633");
                default: return ContextCompat.getColor(requireContext(), R.color.text_secondary);
            }
        }
    }

    private static class SummaryState {
        static final int NO_SCHEDULE_YET = 0;
        static final int FREE_TIME = 1;
        static final int NEXT_CLASS = 2;
        static final int ONGOING_NOW = 3;

        int type;
        @Nullable ScheduleEntryEntity entry;
    }

    @NonNull
    private SummaryState resolveSummaryState(@Nullable List<ScheduleEntryEntity> schedules) {
        SummaryState state = new SummaryState();

        if (schedules == null || schedules.isEmpty()) {
            state.type = SummaryState.NO_SCHEDULE_YET;
            state.entry = null;
            return state;
        }

        int today = getDefaultDay();
        int currentMinutes = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);

        List<ScheduleEntryEntity> todayEntries = new ArrayList<>();
        for (ScheduleEntryEntity entry : schedules) {
            if (entry.dayOfWeek == today) {
                todayEntries.add(entry);
            }
        }

        if (todayEntries.isEmpty()) {
            state.type = SummaryState.FREE_TIME;
            state.entry = null;
            return state;
        }

        ScheduleEntryEntity ongoing = null;
        ScheduleEntryEntity next = null;

        for (ScheduleEntryEntity entry : todayEntries) {
            if (currentMinutes >= entry.startMinutes && currentMinutes < entry.endMinutes) {
                if (ongoing == null || entry.startMinutes < ongoing.startMinutes) {
                    ongoing = entry;
                }
            } else if (entry.startMinutes > currentMinutes) {
                if (next == null || entry.startMinutes < next.startMinutes) {
                    next = entry;
                }
            }
        }

        if (ongoing != null) {
            state.type = SummaryState.ONGOING_NOW;
            state.entry = ongoing;
            return state;
        }

        if (next != null) {
            state.type = SummaryState.NEXT_CLASS;
            state.entry = next;
            return state;
        }

        state.type = SummaryState.FREE_TIME;
        state.entry = null;
        return state;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Room loading
    // ══════════════════════════════════════════════════════════════════════════

    private void loadRoomOptions() {
        viewModel.loadRooms(rooms -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                allRooms.clear();
                allRooms.addAll(rooms);

                roomOptions.clear();
                roomNameMap.clear();

                for (RoomEntity room : allRooms) {
                    if (isCampusAreaCode(room.code)) continue;
                    roomOptions.add(room);
                    roomNameMap.put(room.id, buildRoomDisplay(room));
                }

                renderScheduleContent();
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Schedule form dialog  (UNCHANGED from original)
    // ══════════════════════════════════════════════════════════════════════════

    private void showScheduleFormDialog(@Nullable ScheduleEntryEntity existing) {
        if (!isAdded()) return;

        View formView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_schedule_form, null, false);

        EditText etSubjectName = formView.findViewById(R.id.et_subject_name);
        EditText etSubjectCode = formView.findViewById(R.id.et_subject_code);
        EditText etProfessor   = formView.findViewById(R.id.et_professor);
        EditText etNotes       = formView.findViewById(R.id.et_notes);

        MaterialAutoCompleteTextView dropdownClassType = formView.findViewById(R.id.dropdown_class_type);
        MaterialAutoCompleteTextView dropdownPlatform  = formView.findViewById(R.id.dropdown_platform);
        MaterialAutoCompleteTextView dropdownReminder  = formView.findViewById(R.id.dropdown_reminder);

        EditText etRoomSearch            = formView.findViewById(R.id.et_room_search);
        ImageView btnClearRoomSearch     = formView.findViewById(R.id.btn_clear_room_search);
        RecyclerView recyclerRoomDropdown = formView.findViewById(R.id.recycler_room_dropdown);

        LinearLayout roomGroup        = formView.findViewById(R.id.room_group);
        LinearLayout onlineGroup      = formView.findViewById(R.id.online_group);
        LinearLayout scheduleContainer = formView.findViewById(R.id.schedule_container);
        ImageView btnAddScheduleBlock  = formView.findViewById(R.id.btn_add_schedule_block);

        TextView btnCancel = formView.findViewById(R.id.btn_sheet_cancel);
        TextView btnSave   = formView.findViewById(R.id.btn_sheet_save);

        TextView tvSheetTitle = formView.findViewById(R.id.tv_sheet_title);

        if (existing != null) {
            tvSheetTitle.setText("Edit Schedule");
            btnCancel.setText("Delete");
            btnCancel.setBackground(ContextCompat.getDrawable(requireContext(),
                    R.drawable.bg_schedule_delete_outline));
            btnCancel.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_accent));
            btnSave.setText("Save Changes");
        } else {
            tvSheetTitle.setText("New Schedule");
            btnSave.setText("Create Schedule");
        }

        dropdownClassType.setAdapter(new ArrayAdapter<>(
                requireContext(), R.layout.item_dropdown_simple, CLASS_TYPES));
        dropdownPlatform.setAdapter(new ArrayAdapter<>(
                requireContext(), R.layout.item_dropdown_simple, PLATFORMS));
        dropdownReminder.setAdapter(new ArrayAdapter<>(
                requireContext(), R.layout.item_dropdown_simple, REMINDER_OPTIONS));

        dropdownClassType.setOnItemClickListener((parent, view, position, id) -> {
            boolean online = position == 1;
            roomGroup.setVisibility(online ? View.GONE : View.VISIBLE);
            onlineGroup.setVisibility(online ? View.VISIBLE : View.GONE);
            if (online) {
                etRoomSearch.setText("");
                etRoomSearch.setTag(null);
                btnClearRoomSearch.setVisibility(View.GONE);
            } else {
                dropdownPlatform.setText("", false);
            }
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
            if (hasFocus && !roomOptions.isEmpty())
                recyclerRoomDropdown.setVisibility(View.VISIBLE);
        });
        etRoomSearch.setOnClickListener(v -> {
            if (!roomOptions.isEmpty())
                recyclerRoomDropdown.setVisibility(View.VISIBLE);
        });
        etRoomSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearRoomSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                List<RoomEntity> filtered = new ArrayList<>();
                for (RoomEntity room : roomOptions) {
                    String name = room.name != null ? room.name.toLowerCase(Locale.ROOT) : "";
                    String code = room.code != null ? room.code.toLowerCase(Locale.ROOT) : "";
                    if (query.isEmpty() || name.contains(query) || code.contains(query))
                        filtered.add(room);
                }
                roomDropdownAdapter.submitList(filtered);
                recyclerRoomDropdown.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        btnClearRoomSearch.setOnClickListener(v -> {
            etRoomSearch.setText("");
            etRoomSearch.setTag(null);
            btnClearRoomSearch.setVisibility(View.GONE);
            roomDropdownAdapter.submitList(new ArrayList<>(roomOptions));
            recyclerRoomDropdown.setVisibility(View.GONE);
        });

        int[] selectedColorIndex = {0};
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
                        existing.onlinePlatform != null ? existing.onlinePlatform : PLATFORMS[0], false);
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
                    REMINDER_OPTIONS[reminderToPosition(existing.reminderMinutes)], false);
            if (existing.colorHex != null) {
                selectedColorIndex[0] = resolveSchedulePaletteSlot(existing.colorHex);
            }

            setupColorPicker(formView, selectedColorIndex, selectedColorIndex[0]);
            addScheduleBlock(scheduleContainer, existing.dayOfWeek,
                    existing.startMinutes, existing.endMinutes, false);
        } else {
            dropdownClassType.setText(CLASS_TYPES[0], false);
            dropdownReminder.setText(
                    REMINDER_OPTIONS[reminderToPosition(
                            sessionManager.getDefaultReminderMinutes())], false);
            roomGroup.setVisibility(View.VISIBLE);
            onlineGroup.setVisibility(View.GONE);
            addScheduleBlock(scheduleContainer, getDefaultSchedulableDay(), -1, -1, false);
        }

        btnAddScheduleBlock.setOnClickListener(v ->
                addScheduleBlock(scheduleContainer, getDefaultSchedulableDay(), -1, -1, true));

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(), R.style.ThemeOverlay_FreshGuide_BottomSheet);
        dialog.setContentView(formView);

        btnCancel.setOnClickListener(v -> {
            if (existing == null) {
                dialog.dismiss();
                return;
            }
            confirmDeleteSchedule(existing, dialog);
        });

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
                            ? dropdownPlatform.getText().toString().trim() : null;
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
                        ? dropdownReminder.getText().toString() : REMINDER_OPTIONS[0];
                for (int i = 0; i < REMINDER_OPTIONS.length; i++) {
                    if (REMINDER_OPTIONS[i].equalsIgnoreCase(reminderValue)) {
                        reminderPosition = i;
                        break;
                    }
                }

                int reminderMinutes = positionToReminder(reminderPosition);
                if (!sessionManager.isScheduleNotificationsEnabled()) reminderMinutes = 0;

                int colorIndex = selectedColorIndex[0];
                if (colorIndex < 0 || colorIndex >= getScheduleColors().length) colorIndex = 0;

                if (scheduleBlocks.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add a schedule block", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (ScheduleBlockBinding block : scheduleBlocks) {
                    if (!isValidScheduleDay(block.selectedDay)) {
                        Toast.makeText(requireContext(), SUNDAY_NOT_ALLOWED_MESSAGE, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (block.startMinutes < 0 || block.endMinutes < 0) {
                        Toast.makeText(requireContext(), "Please select start and end time", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!isValidClassSchedule(block.startMinutes, block.endMinutes)) {
                        Toast.makeText(requireContext(),
                                INVALID_CLASS_SCHEDULE_MESSAGE,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (hasExactDuplicateSchedule(title, existing)) {
                    Toast.makeText(requireContext(),
                            DUPLICATE_SCHEDULE_MESSAGE,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hasScheduleOverlap(existing)) {
                    Toast.makeText(requireContext(),
                            "You already have a class at that time.",
                            Toast.LENGTH_SHORT).show();
                    return;
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
                        entry.id             = existing.id;
                        entry.remoteId       = existing.remoteId;
                        entry.clientUuid     = existing.clientUuid;
                        entry.ownerStudentId = existing.ownerStudentId;
                        entry.syncState      = existing.syncState;
                        entry.pendingDelete  = existing.pendingDelete;
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
                                    String successMessage = totalToSave > 1
                                            ? "Schedules saved"
                                            : "Schedule saved";
                                    if (BuildConfig.DEBUG
                                            && totalToSave == 1
                                            && savedEntry.reminderMinutes > 0
                                            && sessionManager.isScheduleNotificationsEnabled()) {
                                        successMessage = successMessage + "\n"
                                                + ScheduleReminderHelper.buildReminderScheduledMessage(savedEntry);
                                    }
                                    Toast.makeText(requireContext(),
                                            successMessage,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "saveSchedule failed: " + message);
                            if (!isAdded() || getActivity() == null || errorShown[0]) return;
                            errorShown[0] = true;
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Create/Save schedule flow crashed", e);
                if (isAdded())
                    Toast.makeText(requireContext(), "Unable to save schedule right now", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
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
        binding.root         = blockView;
        binding.label        = blockView.findViewById(R.id.tv_schedule_label);
        binding.delete       = blockView.findViewById(R.id.btn_delete_schedule);
        binding.dayContainer = blockView.findViewById(R.id.day_container);
        binding.btnStart     = blockView.findViewById(R.id.btn_start);
        binding.btnEnd       = blockView.findViewById(R.id.btn_end);
        binding.selectedDay  = initialDay;
        binding.startMinutes = initialStartMinutes;
        binding.endMinutes   = initialEndMinutes;

        setupDayChips(binding);

        if (initialStartMinutes >= 0) {
            binding.btnStart.setText(formatMinutes(initialStartMinutes));
            binding.btnStart.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
        }
        if (initialEndMinutes >= 0) {
            binding.btnEnd.setText(formatMinutes(initialEndMinutes));
            binding.btnEnd.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
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
            boolean enabled = isValidScheduleDay(dayValue);
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_form_chip_selected
                    : R.drawable.bg_form_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? android.R.color.white : R.color.green_primary));
            chip.setScaleX(selected ? 1.05f : 1f);
            chip.setScaleY(selected ? 1.05f : 1f);
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.35f);

            chip.setOnClickListener(v -> {
                if (!isValidScheduleDay(dayValue)) {
                    Toast.makeText(requireContext(), SUNDAY_NOT_ALLOWED_MESSAGE, Toast.LENGTH_SHORT).show();
                    return;
                }
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
            int dayValue = i + 1;
            boolean selected = dayValue == binding.selectedDay;
            boolean enabled = isValidScheduleDay(dayValue);
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_form_chip_selected
                    : R.drawable.bg_form_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? android.R.color.white : R.color.green_primary));
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.35f);
            chip.animate()
                    .scaleX(selected ? 1.05f : 1f)
                    .scaleY(selected ? 1.05f : 1f)
                    .setDuration(120)
                    .start();
        }
    }


    private void showTimePickerForBlock(ScheduleBlockBinding binding, boolean isStart) {
        if (!isAdded()) return;

        int savedValue = isStart ? binding.startMinutes : binding.endMinutes;
        Calendar now = Calendar.getInstance();
        int nowTotal  = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int initialTotalMinutes;
        if (savedValue >= 0) {
            initialTotalMinutes = savedValue;
        } else if (!isStart && binding.startMinutes >= 0) {
            initialTotalMinutes = binding.startMinutes + MIN_CLASS_DURATION_MINUTES;
        } else {
            initialTotalMinutes = nowTotal;
        }
        initialTotalMinutes = clampInitialTimeSelection(binding, isStart, initialTotalMinutes);

        int initialHour24 = initialTotalMinutes / 60;
        int initialMinute = initialTotalMinutes % 60;
        int initialAmPm   = initialHour24 >= 12 ? 1 : 0;
        int initialHour12 = initialHour24 % 12;
        if (initialHour12 == 0) initialHour12 = 12;

        View pickerView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_time_picker_custom, null, false);

        RecyclerView rvHour   = pickerView.findViewById(R.id.rv_hour);
        RecyclerView rvMinute = pickerView.findViewById(R.id.rv_minute);
        TextView btnAm        = pickerView.findViewById(R.id.btn_am);
        TextView btnPm        = pickerView.findViewById(R.id.btn_pm);
        TextView btnConfirm   = pickerView.findViewById(R.id.btn_time_confirm);

        final int[] selectedAmPm = {initialAmPm};

        List<String> hourItems = buildLoopingHourItems();
        List<String> minuteItems = buildLoopingMinuteItems();

        TimeWheelAdapter hourAdapter   = new TimeWheelAdapter(hourItems);
        TimeWheelAdapter minuteAdapter = new TimeWheelAdapter(minuteItems);

        int initialHourPosition = getLoopingHourInitialPosition(initialHour12);
        int initialMinutePosition = getLoopingMinuteInitialPosition(initialMinute);
        LinearSnapHelper hourSnapHelper   = setupWheelRecycler(rvHour,   hourAdapter,   initialHourPosition);
        LinearSnapHelper minuteSnapHelper = setupWheelRecycler(rvMinute, minuteAdapter, initialMinutePosition);

        updateAmPmButtons(btnAm, btnPm, selectedAmPm[0]);

        final int[] snappedHourIdx   = {initialHourPosition};
        final int[] snappedMinuteIdx = {initialMinutePosition};
        final boolean[] lastEndTimeValidity = {
                isValidTimeSelectionForBlock(binding, isStart, initialTotalMinutes)
        };
        final boolean[] hasUserInteracted = {false};
        final boolean[] invalidToastShown = {false};

        Runnable refreshConfirmState = () -> {
            normalizeLoopingHourWheel(rvHour, hourAdapter, hourSnapHelper);
            normalizeLoopingMinuteWheel(rvMinute, minuteAdapter, minuteSnapHelper);
            snappedHourIdx[0] = getSnappedAdapterPosition(rvHour, hourSnapHelper);
            snappedMinuteIdx[0] = getSnappedAdapterPosition(rvMinute, minuteSnapHelper);

            int totalMinutes = getTotalMinutesForSelection(
                    snappedHourIdx[0], snappedMinuteIdx[0], selectedAmPm[0]);
            boolean validSelection = isValidTimeSelectionForBlock(binding, isStart, totalMinutes);

            updateConfirmButtonState(btnConfirm, validSelection);
            if (validSelection) {
                invalidToastShown[0] = false;
            }
            lastEndTimeValidity[0] = validSelection;
        };

        btnAm.setOnClickListener(v -> {
            hasUserInteracted[0] = true;
            selectedAmPm[0] = 0;
            updateAmPmButtons(btnAm, btnPm, 0);
            refreshConfirmState.run();
            maybeShowInvalidEndTimeToast(hasUserInteracted[0],
                    lastEndTimeValidity[0], invalidToastShown);
        });
        btnPm.setOnClickListener(v -> {
            hasUserInteracted[0] = true;
            selectedAmPm[0] = 1;
            updateAmPmButtons(btnAm, btnPm, 1);
            refreshConfirmState.run();
            maybeShowInvalidEndTimeToast(hasUserInteracted[0],
                    lastEndTimeValidity[0], invalidToastShown);
        });

        RecyclerView.OnScrollListener validationListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    hasUserInteracted[0] = true;
                    rv.post(() -> {
                        refreshConfirmState.run();
                        maybeShowInvalidEndTimeToast(hasUserInteracted[0],
                                lastEndTimeValidity[0], invalidToastShown);
                    });
                }
            }
        };
        rvHour.addOnScrollListener(validationListener);
        rvMinute.addOnScrollListener(validationListener);

        BottomSheetDialog dialog = new BottomSheetDialog(
                requireContext(), R.style.CustomBottomSheetDialog);
        dialog.setContentView(pickerView);
        dialog.setCancelable(true);

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            int margin = dpToPx(10);
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
            lp.setMargins(margin, 0, margin, margin);
            bottomSheet.setLayoutParams(lp);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
            refreshConfirmState.run();
        });

        btnConfirm.setOnClickListener(v -> {
            if (!btnConfirm.isEnabled()) {
                return;
            }
            normalizeLoopingHourWheel(rvHour, hourAdapter, hourSnapHelper);
            normalizeLoopingMinuteWheel(rvMinute, minuteAdapter, minuteSnapHelper);
            int selectedHourIndex   = getSnappedAdapterPosition(rvHour,   hourSnapHelper);
            int selectedMinuteIndex = getSnappedAdapterPosition(rvMinute, minuteSnapHelper);
            int totalMinutes = getTotalMinutesForSelection(
                    selectedHourIndex, selectedMinuteIndex, selectedAmPm[0]);

            if (!isValidTimeSelectionForBlock(binding, isStart, totalMinutes)) {
                Toast.makeText(requireContext(),
                        INVALID_CLASS_SCHEDULE_MESSAGE,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (isStart) {
                binding.startMinutes = totalMinutes;
                binding.btnStart.setText(formatMinutes(totalMinutes));
                binding.btnStart.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
            } else {
                binding.endMinutes = totalMinutes;
                binding.btnEnd.setText(formatMinutes(totalMinutes));
                binding.btnEnd.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.schedule_time_button));
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateAmPmButtons(TextView btnAm, TextView btnPm, int selectedAmPm) {
        boolean isAmSelected = selectedAmPm == 0;
        btnAm.setBackgroundResource(isAmSelected
                ? R.drawable.bg_form_chip_selected : R.drawable.bg_form_chip_unselected);
        btnPm.setBackgroundResource(!isAmSelected
                ? R.drawable.bg_form_chip_selected : R.drawable.bg_form_chip_unselected);
        btnAm.setTextColor(ContextCompat.getColor(requireContext(),
                isAmSelected ? android.R.color.white : R.color.green_primary));
        btnPm.setTextColor(ContextCompat.getColor(requireContext(),
                !isAmSelected ? android.R.color.white : R.color.green_primary));
    }

    private LinearSnapHelper setupWheelRecycler(RecyclerView recyclerView,
                                                TimeWheelAdapter adapter,
                                                int initialPosition) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        recyclerView.setNestedScrollingEnabled(false);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.post(() -> {
            layoutManager.scrollToPositionWithOffset(initialPosition, 0);
            recyclerView.post(() -> {
                int snapped = getSnappedAdapterPosition(recyclerView, snapHelper);
                adapter.setSelectedPosition(
                        snapped != RecyclerView.NO_POSITION ? snapped : initialPosition);
            });
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    rv.post(() -> adapter.setSelectedPosition(
                            getSnappedAdapterPosition(rv, snapHelper)));
                }
            }
        });

        return snapHelper;
    }

    private int getSnappedAdapterPosition(RecyclerView recyclerView, LinearSnapHelper snapHelper) {
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (lm == null) return 0;
        View snapView = snapHelper.findSnapView(lm);
        if (snapView == null) return 0;
        int pos = lm.getPosition(snapView);
        return pos == RecyclerView.NO_POSITION ? 0 : pos;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Schedule detail dialog  (UNCHANGED from original)
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> buildLoopingHourItems() {
        List<String> items = new ArrayList<>(HOUR_WHEEL_VALUE_COUNT * HOUR_WHEEL_REPEAT_COUNT);
        for (int cycle = 0; cycle < HOUR_WHEEL_REPEAT_COUNT; cycle++) {
            for (int hour = 1; hour <= HOUR_WHEEL_VALUE_COUNT; hour++) {
                items.add(String.format(Locale.getDefault(), "%02d", hour));
            }
        }
        return items;
    }

    private List<String> buildLoopingMinuteItems() {
        List<String> items = new ArrayList<>(MINUTE_WHEEL_VALUE_COUNT * HOUR_WHEEL_REPEAT_COUNT);
        for (int cycle = 0; cycle < HOUR_WHEEL_REPEAT_COUNT; cycle++) {
            for (int minute = 0; minute < MINUTE_WHEEL_VALUE_COUNT; minute++) {
                items.add(String.format(Locale.getDefault(), "%02d", minute));
            }
        }
        return items;
    }

    private int getLoopingHourInitialPosition(int hour12) {
        int middleCycle = HOUR_WHEEL_REPEAT_COUNT / 2;
        return (middleCycle * HOUR_WHEEL_VALUE_COUNT) + (hour12 - 1);
    }

    private int getLoopingMinuteInitialPosition(int minute) {
        int middleCycle = HOUR_WHEEL_REPEAT_COUNT / 2;
        return (middleCycle * MINUTE_WHEEL_VALUE_COUNT) + minute;
    }

    private int getHourValueFromLoopPosition(int adapterPosition) {
        int normalizedPosition = adapterPosition % HOUR_WHEEL_VALUE_COUNT;
        if (normalizedPosition < 0) {
            normalizedPosition += HOUR_WHEEL_VALUE_COUNT;
        }
        return normalizedPosition + 1;
    }

    private int getMinuteValueFromLoopPosition(int adapterPosition) {
        int normalizedPosition = adapterPosition % MINUTE_WHEEL_VALUE_COUNT;
        if (normalizedPosition < 0) {
            normalizedPosition += MINUTE_WHEEL_VALUE_COUNT;
        }
        return normalizedPosition;
    }

    private void normalizeLoopingHourWheel(RecyclerView recyclerView,
                                           TimeWheelAdapter adapter,
                                           LinearSnapHelper snapHelper) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return;
        }

        int snappedPosition = getSnappedAdapterPosition(recyclerView, snapHelper);
        int normalizedPosition = getLoopingHourInitialPosition(
                getHourValueFromLoopPosition(snappedPosition));
        if (snappedPosition == normalizedPosition) {
            return;
        }

        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(normalizedPosition, 0);
        adapter.setSelectedPosition(normalizedPosition);
    }

    private void normalizeLoopingMinuteWheel(RecyclerView recyclerView,
                                             TimeWheelAdapter adapter,
                                             LinearSnapHelper snapHelper) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return;
        }

        int snappedPosition = getSnappedAdapterPosition(recyclerView, snapHelper);
        int normalizedPosition = getLoopingMinuteInitialPosition(
                getMinuteValueFromLoopPosition(snappedPosition));
        if (snappedPosition == normalizedPosition) {
            return;
        }

        ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(normalizedPosition, 0);
        adapter.setSelectedPosition(normalizedPosition);
    }

    private int getTotalMinutesForSelection(int selectedHourIndex,
                                            int selectedMinuteIndex,
                                            int selectedAmPm) {
        int selectedHour12 = getHourValueFromLoopPosition(selectedHourIndex);
        int selectedMinute = getMinuteValueFromLoopPosition(selectedMinuteIndex);
        int hour24 = (selectedAmPm == 0)
                ? (selectedHour12 == 12 ? 0 : selectedHour12)
                : (selectedHour12 == 12 ? 12 : selectedHour12 + 12);
        return hour24 * 60 + selectedMinute;
    }

    private void updateConfirmButtonState(TextView btnConfirm, boolean enabled) {
        btnConfirm.setEnabled(enabled);
        btnConfirm.setClickable(enabled);
        btnConfirm.setBackgroundResource(enabled
                ? R.drawable.bg_time_picker_confirm
                : R.drawable.bg_time_picker_confirm_disabled);
        btnConfirm.setTextColor(ContextCompat.getColor(requireContext(),
                enabled ? R.color.time_picker_confirm_text : R.color.time_picker_confirm_disabled_text));
        btnConfirm.setAlpha(enabled ? 1f : 0.9f);
    }

    private void maybeShowInvalidEndTimeToast(boolean hasUserInteracted,
                                              boolean currentSelectionValid,
                                              boolean[] invalidToastShown) {
        if (!hasUserInteracted || currentSelectionValid || invalidToastShown[0]) {
            return;
        }

        Toast.makeText(requireContext(),
                INVALID_CLASS_SCHEDULE_MESSAGE,
                Toast.LENGTH_SHORT).show();
        invalidToastShown[0] = true;
    }

    private int clampInitialTimeSelection(ScheduleBlockBinding binding, boolean isStart, int totalMinutes) {
        int minAllowed = isStart ? MIN_CLASS_START_MINUTES : MIN_CLASS_START_MINUTES + MIN_CLASS_DURATION_MINUTES;
        int maxAllowed = isStart ? getLatestAllowedStartMinutes(binding) : MAX_CLASS_END_MINUTES;
        if (maxAllowed < minAllowed) {
            return minAllowed;
        }
        return Math.max(minAllowed, Math.min(maxAllowed, totalMinutes));
    }

    private int getLatestAllowedStartMinutes(@NonNull ScheduleBlockBinding binding) {
        int latestAllowed = MAX_CLASS_END_MINUTES - MIN_CLASS_DURATION_MINUTES;
        if (binding.endMinutes >= 0) {
            latestAllowed = Math.min(latestAllowed, binding.endMinutes - MIN_CLASS_DURATION_MINUTES);
        }
        return latestAllowed;
    }

    private boolean isValidTimeSelectionForBlock(@NonNull ScheduleBlockBinding binding,
                                                 boolean isStart,
                                                 int totalMinutes) {
        if (isStart) {
            if (totalMinutes < MIN_CLASS_START_MINUTES) {
                return false;
            }
            if (totalMinutes > MAX_CLASS_END_MINUTES - MIN_CLASS_DURATION_MINUTES) {
                return false;
            }
            return binding.endMinutes < 0 || isValidScheduleRange(totalMinutes, binding.endMinutes);
        }

        if (totalMinutes > MAX_CLASS_END_MINUTES) {
            return false;
        }
        if (totalMinutes < MIN_CLASS_START_MINUTES + MIN_CLASS_DURATION_MINUTES) {
            return false;
        }
        return binding.startMinutes < 0 || isValidScheduleRange(binding.startMinutes, totalMinutes);
    }

    private boolean isValidScheduleDay(int dayOfWeek) {
        return dayOfWeek >= 1 && dayOfWeek < SUNDAY_DAY_VALUE;
    }

    private boolean isValidClassSchedule(int startMinutes, int endMinutes) {
        return startMinutes >= MIN_CLASS_START_MINUTES
                && endMinutes <= MAX_CLASS_END_MINUTES
                && isValidScheduleRange(startMinutes, endMinutes);
    }

    private boolean isValidScheduleRange(int startMinutes, int endMinutes) {
        return endMinutes > startMinutes
                && (endMinutes - startMinutes) >= MIN_CLASS_DURATION_MINUTES;
    }

    private boolean hasExactDuplicateSchedule(@NonNull String title,
                                              @Nullable ScheduleEntryEntity existing) {
        String normalizedTitle = normalizeKey(title);
        if (normalizedTitle == null) {
            return false;
        }

        List<PendingScheduleBlock> pendingBlocks = buildPendingScheduleBlocks(existing);
        for (int i = 0; i < pendingBlocks.size(); i++) {
            PendingScheduleBlock pendingBlock = pendingBlocks.get(i);

            for (ScheduleEntryEntity schedule : allSchedules) {
                if (pendingBlock.existingEntryId != null && schedule.id == pendingBlock.existingEntryId) {
                    continue;
                }
                if (isExactDuplicateSchedule(
                        normalizedTitle,
                        pendingBlock.dayOfWeek,
                        pendingBlock.startMinutes,
                        pendingBlock.endMinutes,
                        schedule
                )) {
                    return true;
                }
            }

            for (int j = i + 1; j < pendingBlocks.size(); j++) {
                PendingScheduleBlock otherBlock = pendingBlocks.get(j);
                if (pendingBlock.dayOfWeek == otherBlock.dayOfWeek
                        && pendingBlock.startMinutes == otherBlock.startMinutes
                        && pendingBlock.endMinutes == otherBlock.endMinutes) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasScheduleOverlap(@Nullable ScheduleEntryEntity existing) {
        List<PendingScheduleBlock> pendingBlocks = buildPendingScheduleBlocks(existing);

        for (int i = 0; i < pendingBlocks.size(); i++) {
            PendingScheduleBlock pendingBlock = pendingBlocks.get(i);

            for (ScheduleEntryEntity schedule : allSchedules) {
                if (pendingBlock.existingEntryId != null && schedule.id == pendingBlock.existingEntryId) {
                    continue;
                }
                if (isTimeOverlap(
                        pendingBlock.dayOfWeek,
                        pendingBlock.startMinutes,
                        pendingBlock.endMinutes,
                        schedule.dayOfWeek,
                        schedule.startMinutes,
                        schedule.endMinutes
                )) {
                    return true;
                }
            }

            for (int j = i + 1; j < pendingBlocks.size(); j++) {
                PendingScheduleBlock otherBlock = pendingBlocks.get(j);
                if (isTimeOverlap(
                        pendingBlock.dayOfWeek,
                        pendingBlock.startMinutes,
                        pendingBlock.endMinutes,
                        otherBlock.dayOfWeek,
                        otherBlock.startMinutes,
                        otherBlock.endMinutes
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    @NonNull
    private List<PendingScheduleBlock> buildPendingScheduleBlocks(@Nullable ScheduleEntryEntity existing) {
        List<PendingScheduleBlock> pendingBlocks = new ArrayList<>();
        for (int i = 0; i < scheduleBlocks.size(); i++) {
            ScheduleBlockBinding block = scheduleBlocks.get(i);
            Integer editingEntryId = (existing != null && i == 0) ? existing.id : null;
            pendingBlocks.add(new PendingScheduleBlock(
                    block.selectedDay,
                    block.startMinutes,
                    block.endMinutes,
                    editingEntryId
            ));
        }
        return pendingBlocks;
    }

    private boolean isTimeOverlap(int firstDay,
                                  int firstStartMinutes,
                                  int firstEndMinutes,
                                  int secondDay,
                                  int secondStartMinutes,
                                  int secondEndMinutes) {
        return firstDay == secondDay
                && firstStartMinutes < secondEndMinutes
                && secondStartMinutes < firstEndMinutes;
    }

    private boolean isExactDuplicateSchedule(@NonNull String normalizedTitle,
                                             int dayOfWeek,
                                             int startMinutes,
                                             int endMinutes,
                                             @NonNull ScheduleEntryEntity schedule) {
        return dayOfWeek == schedule.dayOfWeek
                && startMinutes == schedule.startMinutes
                && endMinutes == schedule.endMinutes
                && normalizedTitle.equals(normalizeKey(schedule.title));
    }

    private void confirmDeleteSchedule(@NonNull ScheduleEntryEntity entry,
                                       @NonNull Dialog hostDialog) {
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
                                    hostDialog.dismiss();
                                    Toast.makeText(requireContext(),
                                            "Schedule deleted", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(String message) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                message, Toast.LENGTH_SHORT).show());
                            }
                        }))
                .show();
    }

    private void showScheduleDetailDialog(ScheduleEntryEntity entry) {
        if (!isAdded()) return;

        View detailView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_schedule_detail, null, false);

        TextView tvCode      = detailView.findViewById(R.id.tv_detail_code);
        TextView tvTitle     = detailView.findViewById(R.id.tv_detail_title);
        TextView tvProfessor = detailView.findViewById(R.id.tv_detail_professor);
        TextView tvSchedule  = detailView.findViewById(R.id.tv_detail_schedule);
        TextView tvLocation  = detailView.findViewById(R.id.tv_detail_location);
        TextView tvReminder  = detailView.findViewById(R.id.tv_detail_reminder);
        TextView tvNotes     = detailView.findViewById(R.id.tv_detail_notes);

        tvCode.setText(entry.courseCode != null && !entry.courseCode.isBlank()
                ? entry.courseCode : "No code");
        tvTitle.setText(entry.title != null ? entry.title : "Class");
        tvProfessor.setText(entry.instructor != null && !entry.instructor.isBlank()
                ? "Prof. " + entry.instructor : "Professor not set");
        tvSchedule.setText(dayToLabel(entry.dayOfWeek) + " • " +
                formatMinutes(entry.startMinutes) + " - " + formatMinutes(entry.endMinutes));

        if (entry.isOnline == 1) {
            tvLocation.setText("Location: Online" +
                    (entry.onlinePlatform != null ? " • " + entry.onlinePlatform : ""));
        } else {
            tvLocation.setText("Location: " + resolveLocation(entry));
        }

        tvReminder.setText(entry.reminderMinutes > 0
                ? "Reminder: " + entry.reminderMinutes + " minutes before"
                : "Reminder: Off");
        tvNotes.setText(entry.notes != null && !entry.notes.isBlank() ? entry.notes : "No notes");

        AlertDialog detailDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(detailView).create();

        TextView btnDelete     = detailView.findViewById(R.id.btn_detail_delete);
        TextView btnEdit       = detailView.findViewById(R.id.btn_detail_edit);
        TextView btnDirections = detailView.findViewById(R.id.btn_detail_directions);

        if (entry.isOnline == 1 || entry.roomId == null) {
            btnDirections.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            detailDialog.dismiss();
            showScheduleFormDialog(entry);
        });

        btnDelete.setOnClickListener(v -> confirmDeleteSchedule(entry, detailDialog));

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
                R.id.color_0, R.id.color_1, R.id.color_2,
                R.id.color_3, R.id.color_4, R.id.color_5
        };
        for (int i = 0; i < colorViewIds.length; i++) {
            int index = i;
            formView.findViewById(colorViewIds[i]).setOnClickListener(v -> {
                selectedColorIndex[0] = index;
                applyColorSelection(formView, selectedColorIndex[0]);
            });
        }
        selectedColorIndex[0] = currentSelected;
        applyColorSelection(formView, currentSelected);
    }

    private void applyColorSelection(View formView, int selectedIndex) {
        int[] colorViewIds = {
                R.id.color_0, R.id.color_1, R.id.color_2,
                R.id.color_3, R.id.color_4, R.id.color_5
        };
        for (int i = 0; i < colorViewIds.length; i++) {
            View colorView = formView.findViewById(colorViewIds[i]);
            // Use a circular gradient swatch so the picker shows the actual gradient
            GradientDrawable drawable = buildGradientDrawable(i, colorView.getResources()
                    .getDisplayMetrics().density * 18f); // half of 36dp swatch = circle
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setStroke(
                    i == selectedIndex ? 3 : 1,
                    Color.parseColor(i == selectedIndex ? "#12770E" : "#D0D0D0"));
            colorView.setBackground(drawable);
            float targetScale = i == selectedIndex ? 1.22f : 1f;
            colorView.animate()
                    .scaleX(targetScale).scaleY(targetScale)
                    .setDuration(180)
                    .setInterpolator(new OvershootInterpolator(0.7f))
                    .start();
        }
    }

    private void maybeRequestNotificationPermission(int reminderMinutes) {
        if (reminderMinutes <= 0) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private int getDefaultDay() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return 1;
            case Calendar.TUESDAY:   return 2;
            case Calendar.WEDNESDAY: return 3;
            case Calendar.THURSDAY:  return 4;
            case Calendar.FRIDAY:    return 5;
            case Calendar.SATURDAY:  return 6;
            case Calendar.SUNDAY:    return 7;
            default:                 return 1;
        }
    }

    private int getDefaultSchedulableDay() {
        int defaultDay = getDefaultDay();
        return isValidScheduleDay(defaultDay) ? defaultDay : 1;
    }

    private String buildRoomDisplay(RoomEntity room) {
        String name = room.name != null ? room.name : "Room";
        if (room.code != null && !room.code.isBlank())
            return name + " (" + room.code + ")";
        return name;
    }

    private String resolveLocation(ScheduleEntryEntity entry) {
        if (entry.roomId != null && roomNameMap.containsKey(entry.roomId))
            return roomNameMap.get(entry.roomId);
        return "Room not set";
    }

    private boolean isCampusAreaCode(String code) {
        if (code == null) return false;
        String n = code.trim().toUpperCase(Locale.ROOT);
        return "COURT".equals(n) || "ENT".equals(n) || "EXIT".equals(n);
    }

    private int positionToReminder(int position) {
        switch (position) {
            case 1: return 5;
            case 2: return 10;
            case 3: return 15;
            case 4: return 30;
            default: return 0;
        }
    }

    private int reminderToPosition(int reminderMinutes) {
        switch (reminderMinutes) {
            case 5:  return 1;
            case 10: return 2;
            case 15: return 3;
            case 30: return 4;
            default: return 0;
        }
    }

    private String dayToLabel(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > DAY_LABELS.length) return "Mon";
        return DAY_LABELS[dayOfWeek - 1];
    }

    private String normalize(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizeKey(String value) {
        String normalizedValue = normalize(value);
        return normalizedValue != null
                ? normalizedValue.toLowerCase(Locale.ROOT)
                : null;
    }

    private String formatMinutes(int minutes) {
        int hour24 = Math.max(0, Math.min(23, minutes / 60));
        int minute = Math.max(0, Math.min(59, minutes % 60));
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s",
                hour12, minute, hour24 >= 12 ? "PM" : "AM");
    }

    /** Formats an hour integer (0–23) as a readable 12-hour label, e.g. "9 AM", "12 PM". */
    private String formatHour(int hour) {
        if (hour == 0 || hour == 24) return "12 AM";
        if (hour == 12)              return "12 PM";
        if (hour > 12)               return (hour - 12) + " PM";
        return hour + " AM";
    }

    private String[][] getScheduleGradientsForTheme(boolean isDark) {
        if (isDark) {
            return new String[][]{
                    {"#2C2C2C", "#3E3E3E"},   // Neutral slate
                    {"#4D1A2E", "#6B2D45"},   // Deep rose
                    {"#1A2E4D", "#2D4A6B"},   // Midnight blue
                    {"#2E1A4D", "#4A2D6B"},   // Violet dusk
                    {"#1A3D1C", "#2E5E30"},   // Forest green
                    {"#3D3A14", "#5E5820"}    // Golden earth
            };
        } else {
            return new String[][]{
                    {"#E8E8E8", "#F5F5F5"},   // Soft silver
                    {"#F8C5D8", "#FFE4EE"},   // Blush pink
                    {"#C5D8F8", "#E0EEFF"},   // Sky blue
                    {"#E5C5F8", "#F3E0FF"},   // Lavender
                    {"#C5F0C7", "#E0FFE2"},   // Mint green
                    {"#F0F0A0", "#FFFFF0"}    // Lemon cream
            };
        }
    }

    private String[][] getScheduleGradients() {
        boolean isDark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return getScheduleGradientsForTheme(isDark);
    }

    private int resolveSchedulePaletteSlot(@Nullable String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) return 0;

        int targetColor;
        try {
            targetColor = Color.parseColor(colorHex);
        } catch (Exception e) {
            return 0;
        }

        String[][] light = getScheduleGradientsForTheme(false);
        String[][] dark = getScheduleGradientsForTheme(true);

        for (int i = 0; i < light.length; i++) {
            try {
                int lightStart = Color.parseColor(light[i][0]);
                int lightEnd = Color.parseColor(light[i][1]);
                int darkStart = Color.parseColor(dark[i][0]);
                int darkEnd = Color.parseColor(dark[i][1]);

                if (targetColor == lightStart || targetColor == lightEnd
                        || targetColor == darkStart || targetColor == darkEnd) {
                    return i;
                }
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    /** Returns the flat start color for slot {@code index} (used for backward compat). */
    private String[] getScheduleColors() {
        String[][] g = getScheduleGradients();
        String[] flat = new String[g.length];
        for (int i = 0; i < g.length; i++) flat[i] = g[i][0];
        return flat;
    }

    /** Builds a diagonal gradient drawable for the given palette slot. */
    private GradientDrawable buildGradientDrawable(int slotIndex, float cornerRadius) {
        String[][] g = getScheduleGradients();
        int idx = Math.max(0, Math.min(slotIndex, g.length - 1));

        int topColor;
        int bottomColor;

        try {
            topColor = Color.parseColor(g[idx][1]);     // lighter
            bottomColor = Color.parseColor(g[idx][0]);  // darker
        } catch (Exception e) {
            topColor = ContextCompat.getColor(requireContext(), R.color.background_subtle);
            bottomColor = ContextCompat.getColor(requireContext(), R.color.background_card);
        }

        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor}
        );
        drawable.setCornerRadius(cornerRadius);
        return drawable;
    }

    private Drawable buildGlassBackground(int slotIndex, float cornerRadius) {
        String[][] g = getScheduleGradients();
        int idx = Math.max(0, Math.min(slotIndex, g.length - 1));

        int topColor;
        int bottomColor;

        try {
            topColor = Color.parseColor(g[idx][1]);
            bottomColor = Color.parseColor(g[idx][0]);
        } catch (Exception e) {
            topColor = ContextCompat.getColor(requireContext(), R.color.background_subtle);
            bottomColor = ContextCompat.getColor(requireContext(), R.color.background_card);
        }

        // Main vertical gradient
        GradientDrawable base = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        adjustColorAlpha(topColor, 0.94f),
                        adjustColorAlpha(bottomColor, 0.98f)
                }
        );
        base.setCornerRadius(cornerRadius);

        // Top glass highlight
        GradientDrawable highlight = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ContextCompat.getColor(requireContext(), R.color.glass_highlight_top),
                        ContextCompat.getColor(requireContext(), R.color.glass_highlight_bottom)
                }
        );
        highlight.setCornerRadius(cornerRadius);

        // Subtle border
        GradientDrawable stroke = new GradientDrawable();
        stroke.setColor(Color.TRANSPARENT);
        stroke.setCornerRadius(cornerRadius);
        stroke.setStroke(1, adjustColorAlpha(lightenColor(topColor, 1.08f), 0.35f));

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{base, highlight, stroke});
        return layerDrawable;
    }

    private int lightenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.round(Color.red(color) * factor));
        int g = Math.min(255, Math.round(Color.green(color) * factor));
        int b = Math.min(255, Math.round(Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    /** Multiplies alpha channel by {@code factor} (0–1). */
    private int adjustColorAlpha(int color, float factor) {
        int alpha = Math.min(255, Math.round(Color.alpha(color) * factor));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private static class ScheduleBlockBinding {
        View root;
        TextView label;
        ImageView delete;
        LinearLayout dayContainer;
        TextView btnStart;
        TextView btnEnd;

        final List<TextView> dayChips = new ArrayList<>();

        int selectedDay  = 1;
        int startMinutes = -1;
        int endMinutes   = -1;
    }

    private static class PendingScheduleBlock {
        final int dayOfWeek;
        final int startMinutes;
        final int endMinutes;
        @Nullable
        final Integer existingEntryId;

        private PendingScheduleBlock(int dayOfWeek,
                                     int startMinutes,
                                     int endMinutes,
                                     @Nullable Integer existingEntryId) {
            this.dayOfWeek = dayOfWeek;
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
            this.existingEntryId = existingEntryId;
        }
    }
}
