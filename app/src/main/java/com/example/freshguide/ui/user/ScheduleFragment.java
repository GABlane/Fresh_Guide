package com.example.freshguide.ui.user;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.freshguide.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment {

    private final int[] DAY_IDS = {
            R.id.pill_mon, R.id.pill_tue, R.id.pill_wed,
            R.id.pill_thu, R.id.pill_fri, R.id.pill_sat
    };

    private int selectedIndex = 0;
    private LinearLayout scheduleListContainer;
    private View rootView;

    private static final int DP_PER_HOUR = 60;
    private static final int NUM_DAYS    = 6;
    private static final int START_HOUR  = 7;

    public static class TimeSlot {
        public String start, end;
        public TimeSlot(String s, String e) { start = s; end = e; }
    }

    public static class ScheduleItem {
        public String code, name, professor, room, startTime, endTime;
        public int colorInt;
        public boolean[] days = new boolean[6];

        public ScheduleItem(String code, String name, String prof, String room,
                            String start, String end, int color) {
            this.code = code; this.name = name; this.professor = prof;
            this.room = room; this.startTime = start; this.endTime = end;
            this.colorInt = color;
        }
    }

    static final List<ScheduleItem> scheduleItems = new ArrayList<>();

    public static void addSchedule(ScheduleItem item) {
        scheduleItems.add(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_schedule, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleListContainer = view.findViewById(R.id.schedule_list_container);

        TextView tvDate = view.findViewById(R.id.tv_date);
        tvDate.setText(new SimpleDateFormat("EEEE, MMMM d",
                Locale.getDefault()).format(new Date()));

        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        selectedIndex = Math.max(0, Math.min(dow - 2, 5));

        for (int i = 0; i < DAY_IDS.length; i++) {
            final int idx = i;
            TextView pill = view.findViewById(DAY_IDS[i]);
            if (pill == null) continue;
            pill.setOnClickListener(v -> {
                selectedIndex = idx;
                updatePills(view);
            });
        }

        updatePills(view);

        scheduleListContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scheduleListContainer.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        renderSchedule();
                    }
                });

        View btnAdd = view.findViewById(R.id.btn_add_icon);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> openNewSchedule());

        View btnAddEmpty = view.findViewById(R.id.btn_add_schedule);
        if (btnAddEmpty != null) btnAddEmpty.setOnClickListener(v -> openNewSchedule());

        getParentFragmentManager().setFragmentResultListener(
                "schedule_result", getViewLifecycleOwner(), (key, bundle) -> {
                    if (bundle.getBoolean("schedule_added", false))
                        scheduleListContainer.post(this::renderSchedule);
                });
    }

    public void refreshSchedule() {
        if (scheduleListContainer != null)
            scheduleListContainer.post(this::renderSchedule);
    }

    private void openNewSchedule() {
        NewScheduleFragment sheet = new NewScheduleFragment();
        Bundle args = new Bundle();
        args.putInt("preselect_day", selectedIndex);
        sheet.setArguments(args);
        sheet.show(getParentFragmentManager(), "NewSchedule");
    }

    private void updatePills(View root) {
        for (int i = 0; i < DAY_IDS.length; i++) {
            TextView pill = root.findViewById(DAY_IDS[i]);
            if (pill == null) continue;
            if (i == selectedIndex) {
                pill.setBackgroundResource(R.drawable.bg_day_pill_active);
                pill.setTextColor(0xFF1AA514);
            } else {
                pill.setBackgroundResource(android.R.color.transparent);
                pill.setTextColor(0xFF2D2D2D);
            }
        }
    }

    private void renderSchedule() {
        if (scheduleListContainer == null) return;
        scheduleListContainer.removeAllViews();

        View emptyState       = rootView.findViewById(R.id.empty_state);
        View scrollView       = rootView.findViewById(R.id.h_scroll);
        MaterialCardView cardToday       = rootView.findViewById(R.id.card_today);
        MaterialCardView cardPlaceholder = rootView.findViewById(R.id.card_today_placeholder);

        if (scheduleItems.isEmpty()) {
            if (emptyState      != null) emptyState.setVisibility(View.VISIBLE);
            if (scrollView      != null) scrollView.setVisibility(View.GONE);
            if (cardToday       != null) cardToday.setVisibility(View.GONE);
            if (cardPlaceholder != null) cardPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        if (emptyState      != null) emptyState.setVisibility(View.GONE);
        if (scrollView      != null) scrollView.setVisibility(View.VISIBLE);
        if (cardToday       != null) cardToday.setVisibility(View.VISIBLE);
        if (cardPlaceholder != null) cardPlaceholder.setVisibility(View.GONE);

        // Today card
        ScheduleItem first = scheduleItems.get(0);
        TextView tvCode = rootView.findViewById(R.id.tv_today_code);
        TextView tvName = rootView.findViewById(R.id.tv_today_name);
        TextView tvProf = rootView.findViewById(R.id.tv_today_prof);
        TextView tvTime = rootView.findViewById(R.id.tv_today_time);
        if (tvCode != null) tvCode.setText(first.code);
        if (tvName != null) tvName.setText(first.name.toUpperCase());
        if (tvProf != null) tvProf.setText("PROF. " + first.professor.toUpperCase());
        if (tvTime != null) tvTime.setText(first.startTime + " - " + first.endTime);
        if (cardToday != null) {
            int lightColor = lightenColor(first.colorInt, 0.85f);
            GradientDrawable todayBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{ lightColor, lightenColor(first.colorInt, 0.5f) });
            todayBg.setCornerRadius(dpToPx(20));
            todayBg.setStroke(1, Color.parseColor("#848484"));
            cardToday.setBackground(todayBg);
        }

        float density   = requireContext().getResources().getDisplayMetrics().density;
        float pxPerMin  = (DP_PER_HOUR * density) / 60f;
        int   tStartMin = START_HOUR * 60;

        // No time axis — full width split into 6 equal columns
        int containerW = scheduleListContainer.getWidth();
        if (containerW <= 0)
            containerW = requireContext().getResources()
                    .getDisplayMetrics().widthPixels;
        int colW = containerW / NUM_DAYS;

        // Latest end time
        int maxEndMin = tStartMin;
        for (ScheduleItem item : scheduleItems) {
            int e = parseTimeToMinutes(item.endTime);
            if (e > maxEndMin) maxEndMin = e;
        }
        int endHour   = (maxEndMin / 60) + 1;
        int tEndMin   = endHour * 60;
        int timelineH = (int)((tEndMin - tStartMin) * pxPerMin) + dpToPx(8);

        RelativeLayout timeline = new RelativeLayout(requireContext());
        timeline.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, timelineH));

        // Cards only — no time labels or axis lines
        for (int dayIdx = 0; dayIdx < NUM_DAYS; dayIdx++) {
            List<ScheduleItem> dayItems = getItemsForDay(dayIdx);
            if (dayItems.isEmpty()) continue;

            // Column perfectly aligned to pill — no offset
            int colLeft  = dayIdx * colW;
            int colWidth = colW;

            // Sub-columns for overlapping items
            int[] colEnd  = new int[10];
            int[] itemCol = new int[dayItems.size()];
            int   maxCol  = 0;
            for (int i = 0; i < dayItems.size(); i++) {
                int s   = parseTimeToMinutes(dayItems.get(i).startTime);
                int col = 0;
                for (int c = 0; c < colEnd.length; c++) {
                    if (colEnd[c] <= s) { col = c; break; }
                }
                itemCol[i]  = col;
                colEnd[col] = parseTimeToMinutes(dayItems.get(i).endTime);
                if (col > maxCol) maxCol = col;
            }
            int numSubCols = maxCol + 1;
            int subGap     = numSubCols > 1 ? 1 : 0;
            int subW       = (colWidth - subGap * (numSubCols - 1)) / numSubCols;

            for (int i = 0; i < dayItems.size(); i++) {
                ScheduleItem item = dayItems.get(i);
                int startMin = parseTimeToMinutes(item.startTime);
                int endMin   = parseTimeToMinutes(item.endTime);
                if (startMin < tStartMin || endMin <= startMin) continue;

                int topPx    = (int)((startMin - tStartMin) * pxPerMin);
                int heightPx = (int)((endMin - startMin) * pxPerMin);
                int leftPx   = colLeft + itemCol[i] * (subW + subGap);
                int w        = Math.min(subW, colLeft + colWidth - leftPx);
                if (w <= 0) continue;

                LinearLayout bg = new LinearLayout(requireContext());
                bg.setOrientation(LinearLayout.VERTICAL);
                bg.setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));

                TextView tvStart = new TextView(requireContext());
                tvStart.setText(item.startTime);
                tvStart.setTextSize(7f);
                tvStart.setSingleLine(true);
                tvStart.setEllipsize(TextUtils.TruncateAt.END);
                tvStart.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView tvCode2 = new TextView(requireContext());
                tvCode2.setText(item.code);
                tvCode2.setTextSize(9f);
                tvCode2.setTypeface(Typeface.DEFAULT_BOLD);
                tvCode2.setGravity(Gravity.CENTER);
                tvCode2.setSingleLine(false);
                tvCode2.setMaxLines(2);
                tvCode2.setEllipsize(TextUtils.TruncateAt.END);
                tvCode2.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                TextView tvEnd = new TextView(requireContext());
                tvEnd.setText(item.endTime);
                tvEnd.setTextSize(7f);
                tvEnd.setSingleLine(true);
                tvEnd.setEllipsize(TextUtils.TruncateAt.END);
                tvEnd.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                int lightColor = lightenColor(item.colorInt, 0.85f);
                GradientDrawable gd = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{ lightColor, item.colorInt });
                gd.setCornerRadius(dpToPx(8));
                gd.setStroke(1, Color.parseColor("#848484"));
                bg.setBackground(gd);

                boolean light = isColorLight(item.colorInt);
                tvStart.setTextColor(light ? 0xFF888888 : 0xCCFFFFFF);
                tvCode2.setTextColor(light ? 0xFF333333 : 0xFFFFFFFF);
                tvEnd.setTextColor(light   ? 0xFF888888 : 0xCCFFFFFF);

                bg.addView(tvStart);
                bg.addView(tvCode2);
                bg.addView(tvEnd);

                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                        w, heightPx);
                rlp.leftMargin = leftPx;
                rlp.topMargin  = topPx;
                bg.setLayoutParams(rlp);
                timeline.addView(bg);
            }
        }

        scheduleListContainer.addView(timeline);
    }

    private int lightenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] * (1f - factor);
        hsv[2] = Math.min(1f, hsv[2] + (1f - hsv[2]) * factor);
        return Color.HSVToColor(hsv);
    }

    private int parseTimeToMinutes(String time) {
        try {
            time = time.replaceAll("\\s+", "").replace(":", "");
            boolean isPM = time.toUpperCase().contains("PM");
            boolean isAM = time.toUpperCase().contains("AM");
            time = time.toUpperCase().replace("AM","").replace("PM","");
            int hour, minute;
            if (time.length() <= 2) { hour = Integer.parseInt(time); minute = 0; }
            else {
                hour   = Integer.parseInt(time.substring(0, time.length() - 2));
                minute = Integer.parseInt(time.substring(time.length() - 2));
            }
            if (isPM && hour != 12) hour += 12;
            if (isAM && hour == 12) hour  = 0;
            return hour * 60 + minute;
        } catch (Exception e) { return -1; }
    }

    private List<ScheduleItem> getItemsForDay(int dayIdx) {
        List<ScheduleItem> result = new ArrayList<>();
        for (ScheduleItem item : scheduleItems) {
            boolean anyDaySet = false;
            for (boolean d : item.days) if (d) { anyDaySet = true; break; }
            if (anyDaySet) {
                if (item.days[dayIdx]) result.add(item);
            } else {
                if (dayIdx == 0) result.add(item);
            }
        }
        return result;
    }

    private boolean isColorLight(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255 > 0.6;
    }

    private int dpToPx(float dp) {
        return (int)(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}