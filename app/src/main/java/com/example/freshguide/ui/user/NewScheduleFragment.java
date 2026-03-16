package com.example.freshguide.ui.user;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.freshguide.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewScheduleFragment extends BottomSheetDialogFragment {

    private LinearLayout slotsContainer;
    private int slotCount = 1;
    private View selectedColorView = null;
    private FrameLayout selectedRingView = null;
    private int selectedFillColor = 0xFFD9D9D9;
    private int preselectDayIndex = -1;

    private EditText etSubjectName, etSubjectCode, etProfessor, etRoom;

    private static final int[][] COLOR_DATA = {
            { R.id.color_gray,   R.id.ring_gray,   0xFFD9D9D9, 0xFF888888 },
            { R.id.color_pink,   R.id.ring_pink,   0xFFFFB3C1, 0xFFFF69A0 },
            { R.id.color_blue,   R.id.ring_blue,   0xFFB3D9FF, 0xFF5BAEFF },
            { R.id.color_purple, R.id.ring_purple, 0xFFE8B3FF, 0xFFC46FFF },
            { R.id.color_green,  R.id.ring_green,  0xFFB3FFD1, 0xFF3CD67A },
            { R.id.color_yellow, R.id.ring_yellow, 0xFFFFF176, 0xFFF5D800 },
    };

    private static final int[] DAY_PILL_IDS = {
            R.id.slot_mon, R.id.slot_tue, R.id.slot_wed,
            R.id.slot_thu, R.id.slot_fri, R.id.slot_sat
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_schedule, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(null);
                int screenHeight = requireActivity().getWindowManager()
                        .getCurrentWindowMetrics().getBounds().height();
                bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.85);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);
                behavior.setHideable(true);
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View sheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss();
                    }
                    @Override
                    public void onSlide(@NonNull View sheet, float slideOffset) {}
                });
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        slotsContainer = view.findViewById(R.id.schedule_slots_container);
        etSubjectName  = view.findViewById(R.id.et_subject_name);
        etSubjectCode  = view.findViewById(R.id.et_subject_code);
        etProfessor    = view.findViewById(R.id.et_professor);
        etRoom         = view.findViewById(R.id.et_room);

        Bundle args = getArguments();
        if (args != null && args.containsKey("preselect_day")) {
            preselectDayIndex = args.getInt("preselect_day", -1);
        }

        for (int[] data : COLOR_DATA) {
            View colorView   = view.findViewById(data[0]);
            FrameLayout ring = view.findViewById(data[1]);
            int fill         = data[2];
            int stroke       = data[3];
            if (colorView != null) {
                colorView.setOnClickListener(v -> selectColor(v, ring, fill, stroke));
            }
        }

        View defaultColor   = view.findViewById(R.id.color_gray);
        FrameLayout defRing = view.findViewById(R.id.ring_gray);
        if (defaultColor != null) selectColor(defaultColor, defRing, 0xFFD9D9D9, 0xFF888888);

        addSlot();

        view.findViewById(R.id.btn_add_slot).setOnClickListener(v -> addSlot());
        view.findViewById(R.id.btn_create_schedule).setOnClickListener(v -> createSchedule());
    }

    private void createSchedule() {
        String name = etSubjectName != null ? etSubjectName.getText().toString().trim() : "";
        String code = etSubjectCode != null ? etSubjectCode.getText().toString().trim() : "";
        String prof = etProfessor   != null ? etProfessor.getText().toString().trim()   : "";
        String room = etRoom        != null ? etRoom.getText().toString().trim()         : "";

        if (name.isEmpty() || code.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please fill in Subject Name and Code", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScheduleFragment.TimeSlot> slots = new ArrayList<>();
        boolean[] selectedDays = new boolean[6];

        for (int i = 0; i < slotsContainer.getChildCount(); i++) {
            View slot    = slotsContainer.getChildAt(i);
            TextView tvS = slot.findViewById(R.id.tv_start_time);
            TextView tvE = slot.findViewById(R.id.tv_end_time);
            String start = tvS != null ? tvS.getText().toString() : "";
            String end   = tvE != null ? tvE.getText().toString() : "";
            if (!start.equals("Start Time") && !end.equals("End Time")) {
                slots.add(new ScheduleFragment.TimeSlot(start, end));
            }
            for (int d = 0; d < DAY_PILL_IDS.length; d++) {
                TextView pill = slot.findViewById(DAY_PILL_IDS[d]);
                if (pill != null && pill.getTag() instanceof Boolean
                        && (Boolean) pill.getTag()) {
                    selectedDays[d] = true;
                }
            }
        }

        if (slots.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please set at least one time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        int newStart = parseTimeToMinutes(slots.get(0).start);
        int newEnd   = parseTimeToMinutes(slots.get(0).end);

        if (newStart < 0 || newEnd <= newStart) {
            Toast.makeText(requireContext(),
                    "Invalid time range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for time conflicts
        String[] dayNames = {"Mon","Tue","Wed","Thu","Fri","Sat"};
        boolean newNoDays = true;
        for (boolean day : selectedDays) if (day) { newNoDays = false; break; }

        for (ScheduleFragment.ScheduleItem existing : ScheduleFragment.scheduleItems) {
            int existStart = parseTimeToMinutes(existing.startTime);
            int existEnd   = parseTimeToMinutes(existing.endTime);

            boolean existNoDays = true;
            for (boolean day : existing.days) if (day) { existNoDays = false; break; }

            for (int d = 0; d < 6; d++) {
                boolean newAppliesToDay   = newNoDays   ? (d == 0) : selectedDays[d];
                boolean existAppliesToDay = existNoDays ? (d == 0) : existing.days[d];

                if (newAppliesToDay && existAppliesToDay) {
                    if (newStart < existEnd && newEnd > existStart) {
                        Toast.makeText(requireContext(),
                                "Time conflict on " + dayNames[d] + " with " + existing.code,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        }

        ScheduleFragment.ScheduleItem item = new ScheduleFragment.ScheduleItem(
                code, name, prof, room,
                slots.get(0).start, slots.get(0).end,
                selectedFillColor
        );
        item.days = selectedDays;

        ScheduleFragment.addSchedule(item);

        Bundle result = new Bundle();
        result.putBoolean("schedule_added", true);
        getParentFragmentManager().setFragmentResult("schedule_result", result);

        dismiss();
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

    private void selectColor(View clicked, FrameLayout ring, int fillColor, int ringColor) {
        if (selectedColorView != null) {
            View prev = selectedColorView;
            ValueAnimator anim = ValueAnimator.ofInt(dpToPx(22), dpToPx(28));
            anim.setDuration(150);
            anim.addUpdateListener(a -> {
                int val = (int) a.getAnimatedValue();
                ViewGroup.LayoutParams lp = prev.getLayoutParams();
                lp.width = val; lp.height = val;
                prev.setLayoutParams(lp);
            });
            anim.start();
        }
        if (selectedRingView != null) clearRing(selectedRingView);

        View next = clicked;
        ValueAnimator anim = ValueAnimator.ofInt(dpToPx(28), dpToPx(22));
        anim.setDuration(150);
        anim.addUpdateListener(a -> {
            int val = (int) a.getAnimatedValue();
            ViewGroup.LayoutParams lp = next.getLayoutParams();
            lp.width = val; lp.height = val;
            next.setLayoutParams(lp);
        });
        anim.start();

        applyRing(ring, ringColor);
        selectedColorView = clicked;
        selectedRingView  = ring;
        selectedFillColor = fillColor;
    }

    private void applyRing(FrameLayout ring, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.TRANSPARENT);
        gd.setStroke(dpToPx(1.5f), strokeColor);
        ring.setBackground(gd);
    }

    private void clearRing(FrameLayout ring) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.TRANSPARENT);
        gd.setStroke(0, Color.TRANSPARENT);
        ring.setBackground(gd);
    }

    private int dpToPx(float dp) {
        return (int)(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void addSlot() {
        View slot = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_schedule_slot, slotsContainer, false);

        TextView label = slot.findViewById(R.id.tv_slot_label);
        label.setText("Schedule " + slotCount);

        ImageView deleteBtn = slot.findViewById(R.id.btn_delete_slot);
        if (slotCount > 1) {
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(v -> {
                slotsContainer.removeView(slot);
                slotCount--;
                relabelSlots();
            });
        }

        for (int pillId : DAY_PILL_IDS) {
            TextView pill = slot.findViewById(pillId);
            if (pill != null) {
                pill.setTag(false);
                pill.setOnClickListener(v -> toggleDayPill((TextView) v));
            }
        }

        // Auto-select preselected day on first slot
        if (preselectDayIndex >= 0 && slotCount == 1
                && preselectDayIndex < DAY_PILL_IDS.length) {
            TextView pill = slot.findViewById(DAY_PILL_IDS[preselectDayIndex]);
            if (pill != null) {
                pill.setTag(true);
                pill.setBackgroundResource(R.drawable.bg_day_pill_selected);
                pill.setTextColor(0xFFE4FFE2);
            }
        }

        LinearLayout btnStart = slot.findViewById(R.id.btn_start_time);
        TextView tvStart      = slot.findViewById(R.id.tv_start_time);
        if (btnStart != null) btnStart.setOnClickListener(v -> showScrollTimePicker(tvStart));

        LinearLayout btnEnd = slot.findViewById(R.id.btn_end_time);
        TextView tvEnd      = slot.findViewById(R.id.tv_end_time);
        if (btnEnd != null) btnEnd.setOnClickListener(v -> showScrollTimePicker(tvEnd));

        slotsContainer.addView(slot);
        slotCount++;
    }

    private void showScrollTimePicker(TextView target) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE);
        rootBg.setCornerRadius(dpToPx(24));
        root.setBackground(rootBg);

        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topRowLp.bottomMargin = dpToPx(12);
        topRow.setLayoutParams(topRowLp);

        TextView titleTv = new TextView(requireContext());
        titleTv.setText("Select a Time");
        titleTv.setTextColor(Color.parseColor("#4B8546"));
        titleTv.setTextSize(14);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView confirmBtn = new TextView(requireContext());
        confirmBtn.setText("Confirm");
        confirmBtn.setTextColor(Color.parseColor("#1AA514"));
        confirmBtn.setTextSize(12);
        confirmBtn.setPadding(dpToPx(18), dpToPx(8), dpToPx(18), dpToPx(8));
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setShape(GradientDrawable.RECTANGLE);
        confirmBg.setCornerRadius(dpToPx(20));
        confirmBg.setColor(Color.parseColor("#E4FFE2"));
        confirmBg.setStroke(dpToPx(1), Color.parseColor("#1AA514"));
        confirmBtn.setBackground(confirmBg);

        topRow.addView(titleTv);
        topRow.addView(confirmBtn);

        View divider = new View(requireContext());
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        divLp.bottomMargin = dpToPx(4);
        divider.setLayoutParams(divLp);

        LinearLayout pickersRow = new LinearLayout(requireContext());
        pickersRow.setOrientation(LinearLayout.HORIZONTAL);
        pickersRow.setGravity(Gravity.CENTER);
        pickersRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        NumberPicker hourPicker = buildPicker(null, 9, 1, 12);

        TextView colon = new TextView(requireContext());
        colon.setText(":");
        colon.setTextSize(36);
        colon.setTextColor(Color.parseColor("#1AA514"));
        colon.setGravity(Gravity.CENTER);
        colon.setPadding(dpToPx(2), 0, dpToPx(2), dpToPx(8));

        String[] minutes = new String[12];
        for (int i = 0; i < 12; i++)
            minutes[i] = String.format(Locale.getDefault(), "%02d", i * 5);
        NumberPicker minutePicker = buildPicker(minutes, 0, 0, 0);

        View gap = new View(requireContext());
        gap.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), 1));

        NumberPicker ampmPicker = buildPicker(new String[]{"AM", "PM"}, 0, 0, 0);

        NumberPicker.OnScrollListener scrollListener = (picker, scrollState) -> {
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE)
                stylePickerChildren(picker);
        };
        hourPicker.setOnScrollListener(scrollListener);
        minutePicker.setOnScrollListener(scrollListener);
        ampmPicker.setOnScrollListener(scrollListener);

        pickersRow.addView(hourPicker);
        pickersRow.addView(colon);
        pickersRow.addView(minutePicker);
        pickersRow.addView(gap);
        pickersRow.addView(ampmPicker);

        root.addView(topRow, topRowLp);
        root.addView(divider, divLp);
        root.addView(pickersRow);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), 0)
                .setView(root).create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(null);
                WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                lp.gravity = Gravity.BOTTOM;
                lp.width   = (int)(getResources().getDisplayMetrics().widthPixels * 0.92f);
                lp.height  = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.y       = dpToPx(24);
                dialog.getWindow().setAttributes(lp);
            }
            stylePickerChildren(hourPicker);
            stylePickerChildren(minutePicker);
            stylePickerChildren(ampmPicker);
        });

        confirmBtn.setOnClickListener(v -> {
            int h      = hourPicker.getValue();
            String min = minutes[minutePicker.getValue()];
            String ap  = ampmPicker.getValue() == 0 ? "AM" : "PM";
            target.setText(h + " : " + min + "  " + ap);
            target.setTextColor(Color.parseColor("#1AA514"));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void stylePickerChildren(NumberPicker picker) {
        picker.post(() -> {
            for (int i = 0; i < picker.getChildCount(); i++) {
                View child = picker.getChildAt(i);
                if (child instanceof EditText) {
                    EditText et = (EditText) child;
                    et.setTextColor(Color.parseColor("#1AA514"));
                    et.setTextSize(32);
                    et.setCursorVisible(false);
                    et.setBackground(null);
                }
            }
            picker.invalidate();
        });
    }

    private NumberPicker buildPicker(String[] displayedValues,
                                     int defaultVal, int min, int max) {
        NumberPicker p = new NumberPicker(requireContext());
        if (displayedValues != null) {
            p.setMinValue(0);
            p.setMaxValue(displayedValues.length - 1);
            p.setDisplayedValues(displayedValues);
            p.setValue(defaultVal);
        } else {
            p.setMinValue(min);
            p.setMaxValue(max);
            p.setValue(defaultVal);
        }
        try { Field f = NumberPicker.class.getDeclaredField("mTextColor"); f.setAccessible(true); f.set(p, Color.parseColor("#1AA514")); } catch (Exception ignored) {}
        try { Field f = NumberPicker.class.getDeclaredField("mTextSize");  f.setAccessible(true); f.set(p, (float) dpToPx(28)); } catch (Exception ignored) {}
        try { Field f = NumberPicker.class.getDeclaredField("mSelectionDivider"); f.setAccessible(true); f.set(p, null); } catch (Exception ignored) {}
        try { Field f = NumberPicker.class.getDeclaredField("mSelectionDividerHeight"); f.setAccessible(true); f.set(p, 0); } catch (Exception ignored) {}
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(240));
        p.setLayoutParams(lp);
        return p;
    }

    private void toggleDayPill(TextView pill) {
        boolean selected = (boolean) pill.getTag();
        if (selected) {
            pill.setTag(false);
            pill.setBackgroundResource(R.drawable.bg_day_pill_outline);
            pill.setTextColor(0xFF1AA514);
        } else {
            pill.setTag(true);
            pill.setBackgroundResource(R.drawable.bg_day_pill_selected);
            pill.setTextColor(0xFFE4FFE2);
        }
    }

    private void relabelSlots() {
        for (int i = 0; i < slotsContainer.getChildCount(); i++) {
            View slot = slotsContainer.getChildAt(i);
            TextView label = slot.findViewById(R.id.tv_slot_label);
            label.setText("Schedule " + (i + 1));
            ImageView deleteBtn = slot.findViewById(R.id.btn_delete_slot);
            deleteBtn.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        }
    }
}