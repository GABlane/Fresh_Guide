package com.example.freshguide.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.freshguide.R;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyScheduleGridView extends ViewGroup {

    public interface OnScheduleClickListener {
        void onScheduleClick(ScheduleEntryEntity entry);
    }

    private static final int DAY_COUNT = 6;
    private static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final int GRID_START_MINUTES = 8 * 60;
    private static final int GRID_END_MINUTES = 18 * 60;
    private static final int SLOT_MINUTES = 30;

    private final Paint headerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timeAxisBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint verticalLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<ScheduleEntryEntity> schedules = new ArrayList<>();
    private final Map<Integer, String> roomNameMap = new HashMap<>();
    private final List<BlockMeta> blockMetas = new ArrayList<>();

    private OnScheduleClickListener onScheduleClickListener;

    private final int headerHeightPx;
    private final int timeAxisWidthPx;
    private final int slotHeightPx;
    private final int blockMarginPx;
    private final int textInsetPx;
    private final int minimumBlockHeightPx;

    public WeeklyScheduleGridView(@NonNull Context context) {
        this(context, null);
    }

    public WeeklyScheduleGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        headerHeightPx = dp(42);
        timeAxisWidthPx = dp(58);
        slotHeightPx = dp(46);
        blockMarginPx = dp(2);
        textInsetPx = dp(6);
        minimumBlockHeightPx = dp(36);

        initPaints();
        setWillNotDraw(false);
    }

    public void setOnScheduleClickListener(@Nullable OnScheduleClickListener listener) {
        this.onScheduleClickListener = listener;
    }

    public void setSchedules(@Nullable List<ScheduleEntryEntity> entries) {
        schedules.clear();
        if (entries != null) {
            for (ScheduleEntryEntity entry : entries) {
                if (entry.endMinutes > GRID_START_MINUTES && entry.startMinutes < GRID_END_MINUTES) {
                    schedules.add(entry);
                }
            }
        }
        rebuildBlocks();
    }

    public void setRoomNameMap(@Nullable Map<Integer, String> rooms) {
        roomNameMap.clear();
        if (rooms != null) {
            roomNameMap.putAll(rooms);
        }
        rebuildBlocks();
    }

    private void initPaints() {
        Context context = getContext();
        headerBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.schedule_header_bg));
        timeAxisBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.schedule_bg));

        dayLabelPaint.setColor(ContextCompat.getColor(context, R.color.schedule_day_label));
        dayLabelPaint.setTextSize(sp(12));
        dayLabelPaint.setFakeBoldText(true);
        dayLabelPaint.setTextAlign(Paint.Align.CENTER);

        hourLabelPaint.setColor(ContextCompat.getColor(context, R.color.schedule_time_text));
        hourLabelPaint.setTextSize(sp(11));
        hourLabelPaint.setTextAlign(Paint.Align.LEFT);

        majorLinePaint.setColor(ContextCompat.getColor(context, R.color.schedule_grid_major));
        majorLinePaint.setStrokeWidth(dpF(1));

        minorLinePaint.setColor(ContextCompat.getColor(context, R.color.schedule_grid_minor));
        minorLinePaint.setStrokeWidth(dpF(1));

        verticalLinePaint.setColor(ContextCompat.getColor(context, R.color.schedule_grid_minor));
        verticalLinePaint.setStrokeWidth(dpF(1));
    }

    private void rebuildBlocks() {
        removeAllViews();
        blockMetas.clear();

        if (schedules.isEmpty()) {
            requestLayout();
            invalidate();
            return;
        }

        blockMetas.addAll(computeBlockMeta(schedules));
        for (BlockMeta meta : blockMetas) {
            MaterialCardView card = buildCard(meta.entry);
            meta.card = card;
            addView(card);
        }

        requestLayout();
        invalidate();
    }

    private List<BlockMeta> computeBlockMeta(List<ScheduleEntryEntity> allSchedules) {
        List<BlockMeta> result = new ArrayList<>();

        for (int day = 1; day <= DAY_COUNT; day++) {
            List<ScheduleEntryEntity> dayEntries = new ArrayList<>();
            for (ScheduleEntryEntity entry : allSchedules) {
                if (entry.dayOfWeek == day) {
                    dayEntries.add(entry);
                }
            }

            dayEntries.sort(Comparator
                    .comparingInt((ScheduleEntryEntity e) -> e.startMinutes)
                    .thenComparingInt(e -> e.endMinutes));

            List<BlockMeta> dayMetas = new ArrayList<>();
            List<BlockMeta> active = new ArrayList<>();
            Map<Integer, Integer> groupMaxColumn = new HashMap<>();
            int nextGroupId = 1;

            for (ScheduleEntryEntity entry : dayEntries) {
                pruneInactive(active, entry.startMinutes);

                BlockMeta meta = new BlockMeta(entry);
                meta.groupId = active.isEmpty() ? nextGroupId++ : active.get(0).groupId;
                meta.column = findNextColumn(active);

                active.add(meta);
                dayMetas.add(meta);

                int currentMax = groupMaxColumn.containsKey(meta.groupId)
                        ? groupMaxColumn.get(meta.groupId)
                        : -1;
                if (meta.column > currentMax) {
                    groupMaxColumn.put(meta.groupId, meta.column);
                }
            }

            for (BlockMeta meta : dayMetas) {
                int maxColumn = groupMaxColumn.containsKey(meta.groupId)
                        ? groupMaxColumn.get(meta.groupId)
                        : 0;
                meta.columnsInGroup = maxColumn + 1;
            }

            result.addAll(dayMetas);
        }

        return result;
    }

    private void pruneInactive(List<BlockMeta> active, int nextStartMinutes) {
        for (int i = active.size() - 1; i >= 0; i--) {
            if (active.get(i).entry.endMinutes <= nextStartMinutes) {
                active.remove(i);
            }
        }
    }

    private int findNextColumn(List<BlockMeta> active) {
        int column = 0;
        while (true) {
            boolean used = false;
            for (BlockMeta meta : active) {
                if (meta.column == column) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return column;
            }
            column++;
        }
    }

    private MaterialCardView buildCard(ScheduleEntryEntity entry) {
        Context context = getContext();
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dpF(8));
        card.setCardElevation(0f);
        card.setStrokeColor(ContextCompat.getColor(context, R.color.schedule_card_stroke));
        card.setStrokeWidth(dp(1));
        int defaultBg = ContextCompat.getColor(context, R.color.schedule_card_bg);
        int backgroundColor = parseColorOrDefault(entry.colorHex, String.format("#%06X", (0xFFFFFF & defaultBg)));
        card.setCardBackgroundColor(backgroundColor);
        card.setClickable(true);
        card.setFocusable(true);
        card.setUseCompatPadding(false);

        // Determine text color based on background brightness
        int textColor = getContrastingTextColor(backgroundColor);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(textInsetPx, textInsetPx, textInsetPx, textInsetPx);

        TextView tvCode = buildBlockText(11, true, textColor);
        TextView tvTitle = buildBlockText(12, true, textColor);
        TextView tvTime = buildBlockText(10, false, textColor);
        TextView tvLocation = buildBlockText(10, false, textColor);

        tvCode.setText(entry.courseCode != null && !entry.courseCode.isBlank() ? entry.courseCode : "No code");
        tvTitle.setText(entry.title != null && !entry.title.isBlank() ? entry.title : "Class");
        tvTime.setText(formatMinutes(entry.startMinutes) + " - " + formatMinutes(entry.endMinutes));
        tvLocation.setText(resolveLocation(entry));

        container.addView(tvCode);
        container.addView(tvTitle);
        container.addView(tvTime);
        container.addView(tvLocation);

        card.addView(container, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        card.setOnClickListener(v -> {
            if (onScheduleClickListener != null) {
                onScheduleClickListener.onScheduleClick(entry);
            }
        });
        return card;
    }

    private TextView buildBlockText(int textSizeSp, boolean bold, int textColor) {
        Context context = getContext();
        TextView view = new TextView(context);
        view.setTextColor(textColor);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        view.setMaxLines(1);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setIncludeFontPadding(false);
        view.setTypeface(view.getTypeface(), bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return view;
    }

    /**
     * Calculate contrasting text color based on background brightness.
     * Uses relative luminance formula from WCAG guidelines.
     * @param backgroundColor The background color to contrast against
     * @return Dark text for light backgrounds, light text for dark backgrounds
     */
    private int getContrastingTextColor(int backgroundColor) {
        // Extract RGB components
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);

        // Calculate relative luminance using WCAG formula
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

        // If background is light (luminance > 0.5), use dark text; otherwise use light text
        return luminance > 0.5 ? Color.parseColor("#1C1B1F") : Color.parseColor("#E6E6E6");
    }

    private String resolveLocation(ScheduleEntryEntity entry) {
        if (entry.isOnline == 1) {
            if (entry.onlinePlatform != null && !entry.onlinePlatform.isBlank()) {
                return "Online • " + entry.onlinePlatform;
            }
            return "Online";
        }
        if (entry.roomId != null && roomNameMap.containsKey(entry.roomId)) {
            return roomNameMap.get(entry.roomId);
        }
        return "Room not set";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int fallbackWidth = dp(360);
        int measuredWidth = widthMode == MeasureSpec.UNSPECIFIED ? fallbackWidth : widthSize;
        int desiredHeight = headerHeightPx + getSlotCount() * slotHeightPx;

        int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);

        int dayWidth = getDayColumnWidth(measuredWidth);
        for (BlockMeta meta : blockMetas) {
            if (meta.card == null) continue;

            int columnCount = Math.max(1, meta.columnsInGroup);
            float overlapColumnWidth = (float) dayWidth / columnCount;
            int blockWidth = Math.max(dp(30), Math.round(overlapColumnWidth) - (blockMarginPx * 2));

            int blockHeight = Math.max(
                    minimumBlockHeightPx,
                    minutesToPixels(getVisibleEnd(meta.entry) - getVisibleStart(meta.entry)) - (blockMarginPx * 2)
            );

            int childWidthSpec = MeasureSpec.makeMeasureSpec(blockWidth, MeasureSpec.EXACTLY);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(blockHeight, MeasureSpec.EXACTLY);
            meta.card.measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int dayWidth = getDayColumnWidth(getWidth());
        int contentBottom = headerHeightPx + getSlotCount() * slotHeightPx;

        for (BlockMeta meta : blockMetas) {
            if (meta.card == null) continue;

            int dayIndex = Math.max(0, Math.min(DAY_COUNT - 1, meta.entry.dayOfWeek - 1));
            int dayLeft = timeAxisWidthPx + (dayIndex * dayWidth);

            int columnCount = Math.max(1, meta.columnsInGroup);
            float overlapColumnWidth = (float) dayWidth / columnCount;
            int columnLeft = dayLeft + Math.round(meta.column * overlapColumnWidth);

            int left = columnLeft + blockMarginPx;
            int top = headerHeightPx + minutesToPixels(getVisibleStart(meta.entry) - GRID_START_MINUTES) + blockMarginPx;

            int right = Math.min(
                    dayLeft + dayWidth - blockMarginPx,
                    left + meta.card.getMeasuredWidth()
            );
            int bottom = Math.min(contentBottom - blockMarginPx, top + meta.card.getMeasuredHeight());

            meta.card.layout(left, top, right, bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int dayWidth = getDayColumnWidth(width);
        int contentBottom = headerHeightPx + getSlotCount() * slotHeightPx;

        canvas.drawRect(0, 0, width, headerHeightPx, headerBackgroundPaint);
        canvas.drawRect(0, headerHeightPx, timeAxisWidthPx, height, timeAxisBackgroundPaint);

        canvas.drawLine(0, headerHeightPx, width, headerHeightPx, majorLinePaint);

        float dayLabelBaseLine = headerHeightPx / 2f - (dayLabelPaint.ascent() + dayLabelPaint.descent()) / 2f;
        for (int i = 0; i < DAY_COUNT; i++) {
            float dayCenterX = timeAxisWidthPx + (i * dayWidth) + (dayWidth / 2f);
            canvas.drawText(DAY_LABELS[i], dayCenterX, dayLabelBaseLine, dayLabelPaint);
        }

        for (int i = 0; i <= DAY_COUNT; i++) {
            float x = timeAxisWidthPx + (i * dayWidth);
            canvas.drawLine(x, 0, x, contentBottom, verticalLinePaint);
        }

        int slotCount = getSlotCount();
        for (int slot = 0; slot <= slotCount; slot++) {
            int minutesFromStart = slot * SLOT_MINUTES;
            float y = headerHeightPx + (slot * slotHeightPx);
            boolean isHourLine = (minutesFromStart % 60) == 0;

            canvas.drawLine(
                    timeAxisWidthPx,
                    y,
                    width,
                    y,
                    isHourLine ? majorLinePaint : minorLinePaint
            );

            if (isHourLine) {
                int absoluteMinutes = GRID_START_MINUTES + minutesFromStart;
                String label = formatMinutes(absoluteMinutes);
                float labelY = y + dp(12) - hourLabelPaint.descent();
                canvas.drawText(label, dp(6), labelY, hourLabelPaint);
            }
        }
    }

    private int getDayColumnWidth(int totalWidth) {
        int available = Math.max(0, totalWidth - timeAxisWidthPx);
        return Math.max(dp(42), available / DAY_COUNT);
    }

    private int getSlotCount() {
        return (GRID_END_MINUTES - GRID_START_MINUTES) / SLOT_MINUTES;
    }

    private int getVisibleStart(@NonNull ScheduleEntryEntity entry) {
        return Math.max(entry.startMinutes, GRID_START_MINUTES);
    }

    private int getVisibleEnd(@NonNull ScheduleEntryEntity entry) {
        return Math.min(entry.endMinutes, GRID_END_MINUTES);
    }

    private int minutesToPixels(int minutes) {
        return Math.round((minutes / (float) SLOT_MINUTES) * slotHeightPx);
    }

    private int parseColorOrDefault(@Nullable String input, @NonNull String fallback) {
        try {
            return Color.parseColor(input);
        } catch (Exception e) {
            return Color.parseColor(fallback);
        }
    }

    private String formatMinutes(int minutes) {
        int hour24 = Math.max(0, Math.min(23, minutes / 60));
        int minute = Math.max(0, Math.min(59, minutes % 60));
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        String suffix = hour24 >= 12 ? "PM" : "AM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, suffix);
    }

    private int dp(int value) {
        return Math.round(dpF(value));
    }

    private float dpF(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private static class BlockMeta {
        final ScheduleEntryEntity entry;
        int groupId;
        int column;
        int columnsInGroup;
        @Nullable MaterialCardView card;

        BlockMeta(@NonNull ScheduleEntryEntity entry) {
            this.entry = entry;
        }
    }
}
