package com.example.freshguide.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.example.freshguide.R;

public class RoutePathOverlayView extends View {

    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint originCircleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint originCircleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint actionCuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint actionCueCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path routePath = new Path();
    private final float pinSizePx;
    private final float pinTipOffsetPx;
    private final float originCircleRadiusPx;
    private final float dashOnPx;
    private final float dashOffPx;
    private final float dashCyclePx;
    private final float actionCueBaseRadiusPx;
    private final float actionCueTravelRadiusPx;

    @Nullable
    private PointF startPoint;
    @Nullable
    private PointF endPoint;
    @Nullable
    private Float bendX;
    @Nullable
    private PointF interactiveAnchorPoint;
    @Nullable
    private ValueAnimator routeAnimator;

    private float dashPhasePx;
    private float pulseProgress;

    @Nullable
    private Drawable originPinDrawable;
    @Nullable
    private Drawable destinationPinDrawable;

    public RoutePathOverlayView(Context context) {
        this(context, null);
    }

    public RoutePathOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dashOnPx = dpToPx(6);
        dashOffPx = dpToPx(5);
        dashCyclePx = dashOnPx + dashOffPx;
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(dpToPx(2));
        routePaint.setColor(ContextCompat.getColor(context, R.color.green_primary));
        routePaint.setPathEffect(new DashPathEffect(new float[]{dashOnPx, dashOffPx}, 0f));
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);

        originCircleFillPaint.setStyle(Paint.Style.FILL);
        originCircleFillPaint.setColor(ContextCompat.getColor(context, R.color.green_primary));

        originCircleStrokePaint.setStyle(Paint.Style.STROKE);
        originCircleStrokePaint.setStrokeWidth(dpToPx(2));
        originCircleStrokePaint.setColor(ContextCompat.getColor(context, android.R.color.white));

        actionCuePaint.setStyle(Paint.Style.STROKE);
        actionCuePaint.setStrokeWidth(dpToPx(2));
        actionCuePaint.setColor(ContextCompat.getColor(context, R.color.green_primary));

        actionCueCenterPaint.setStyle(Paint.Style.FILL);
        actionCueCenterPaint.setColor(ContextCompat.getColor(context, R.color.green_primary));

        originPinDrawable = null;
        destinationPinDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_map_room_pin_red);
        pinSizePx = dpToPx(28);
        pinTipOffsetPx = dpToPx(1);
        originCircleRadiusPx = dpToPx(8);
        actionCueBaseRadiusPx = dpToPx(10);
        actionCueTravelRadiusPx = dpToPx(8);

        setWillNotDraw(false);
    }

    public void clearRoute() {
        startPoint = null;
        endPoint = null;
        bendX = null;
        interactiveAnchorPoint = null;
        stopRouteAnimation();
        invalidate();
    }

    public void setRoute(@Nullable PointF startPoint, @Nullable PointF endPoint, @Nullable Float bendX) {
        setRoute(startPoint, endPoint, bendX, null);
    }

    public void setRoute(@Nullable PointF startPoint,
                         @Nullable PointF endPoint,
                         @Nullable Float bendX,
                         @Nullable PointF interactiveAnchorPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.bendX = bendX;
        this.interactiveAnchorPoint = interactiveAnchorPoint;
        updateRouteAnimationState();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (startPoint == null || endPoint == null) {
            return;
        }

        routePath.reset();
        routePath.moveTo(startPoint.x, startPoint.y);

        if (bendX != null
                && (Math.abs(startPoint.x - bendX) > dpToPx(4)
                || Math.abs(endPoint.x - bendX) > dpToPx(4))) {
            routePath.lineTo(bendX, startPoint.y);
            routePath.lineTo(bendX, endPoint.y);
        }

        routePath.lineTo(endPoint.x, endPoint.y);
        canvas.drawPath(routePath, routePaint);

        drawOriginMarker(canvas, startPoint);
        drawPin(canvas, endPoint, destinationPinDrawable);
        drawInteractiveCue(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateRouteAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRouteAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Overlay is visual-only; never consume touch so map scrolling stays available.
        return false;
    }

    private void drawOriginMarker(@NonNull Canvas canvas, @NonNull PointF anchor) {
        canvas.drawCircle(anchor.x, anchor.y, originCircleRadiusPx, originCircleFillPaint);
        canvas.drawCircle(anchor.x, anchor.y, originCircleRadiusPx, originCircleStrokePaint);
    }

    private void drawInteractiveCue(@NonNull Canvas canvas) {
        if (interactiveAnchorPoint == null) {
            return;
        }

        float radius = actionCueBaseRadiusPx + (actionCueTravelRadiusPx * pulseProgress);
        int alpha = Math.max(36, Math.round(180f * (1f - pulseProgress)));
        actionCuePaint.setAlpha(alpha);
        canvas.drawCircle(interactiveAnchorPoint.x, interactiveAnchorPoint.y, radius, actionCuePaint);
        canvas.drawCircle(interactiveAnchorPoint.x, interactiveAnchorPoint.y, dpToPx(6), actionCueCenterPaint);
        canvas.drawCircle(interactiveAnchorPoint.x, interactiveAnchorPoint.y, dpToPx(6), originCircleStrokePaint);
    }

    private void drawPin(@NonNull Canvas canvas, @NonNull PointF anchor, @Nullable Drawable drawable) {
        if (drawable == null) {
            return;
        }
        int halfWidth = Math.round(pinSizePx / 2f);
        int left = Math.round(anchor.x) - halfWidth;
        int top = Math.round(anchor.y - pinSizePx + pinTipOffsetPx);
        int right = left + Math.round(pinSizePx);
        int bottom = top + Math.round(pinSizePx);
        drawable.setBounds(left, top, right, bottom);
        drawable.draw(canvas);
    }

    private void updateRouteAnimationState() {
        if (startPoint == null || endPoint == null || !isAttachedToWindow()) {
            stopRouteAnimation();
            return;
        }
        if (routeAnimator != null) {
            return;
        }

        routeAnimator = ValueAnimator.ofFloat(0f, 1f);
        routeAnimator.setDuration(950L);
        routeAnimator.setInterpolator(new LinearInterpolator());
        routeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        routeAnimator.addUpdateListener(animator -> {
            float value = (float) animator.getAnimatedValue();
            dashPhasePx = -(dashCyclePx * value);
            pulseProgress = value;
            routePaint.setPathEffect(new DashPathEffect(new float[]{dashOnPx, dashOffPx}, dashPhasePx));
            postInvalidateOnAnimation();
        });
        routeAnimator.start();
    }

    private void stopRouteAnimation() {
        if (routeAnimator != null) {
            routeAnimator.cancel();
            routeAnimator.removeAllUpdateListeners();
            routeAnimator = null;
        }
        dashPhasePx = 0f;
        pulseProgress = 0f;
        routePaint.setPathEffect(new DashPathEffect(new float[]{dashOnPx, dashOffPx}, 0f));
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
