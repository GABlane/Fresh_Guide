package com.example.freshguide.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.freshguide.R;

public class SplashArrowAnimationView extends View {

    private static final long INITIAL_DELAY_MS = 120L;
    private static final long VERTICAL_DROP_DURATION_MS = 840L;
    private static final long VERTICAL_TRIM_DELAY_MS = 680L;
    private static final long VERTICAL_TRIM_DURATION_MS = 430L;
    private static final long TOP_BAR_SLIDE_DELAY_MS = 1220L;
    private static final long TOP_BAR_SLIDE_DURATION_MS = 520L;
    private static final long TOP_BAR_TRIM_DELAY_MS = 1600L;
    private static final long TOP_BAR_TRIM_DURATION_MS = 320L;
    private static final long MID_BAR_SLIDE_DELAY_MS = 1820L;
    private static final long MID_BAR_SLIDE_DURATION_MS = 400L;
    private static final long MID_BAR_TRIM_DELAY_MS = 2140L;
    private static final long MID_BAR_TRIM_DURATION_MS = 280L;
    private static final long COMPLETION_HOLD_MS = 220L;

    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shapePath = new Path();
    private final PathInterpolator easeInterpolator =
            new PathInterpolator(0.22f, 0.9f, 0.24f, 1f);

    @Nullable
    private AnimatorSet currentAnimator;

    private float verticalDropProgress = 0f;
    private float verticalTrimProgress = 0f;
    private float topBarSlideProgress = 0f;
    private float topBarTrimProgress = 0f;
    private float middleBarSlideProgress = 0f;
    private float middleBarTrimProgress = 0f;
    private boolean animationStarted = false;

    public SplashArrowAnimationView(Context context) {
        this(context, null);
    }

    public SplashArrowAnimationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SplashArrowAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(ContextCompat.getColor(context, R.color.green_primary));
    }

    public void startRevealSequence(@Nullable Runnable onComplete) {
        if (animationStarted || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        animationStarted = true;

        ValueAnimator verticalDropAnimator = buildAnimator(
                INITIAL_DELAY_MS,
                VERTICAL_DROP_DURATION_MS,
                value -> verticalDropProgress = value
        );
        ValueAnimator verticalTrimAnimator = buildAnimator(
                VERTICAL_TRIM_DELAY_MS,
                VERTICAL_TRIM_DURATION_MS,
                value -> verticalTrimProgress = value
        );
        ValueAnimator topBarSlideAnimator = buildAnimator(
                TOP_BAR_SLIDE_DELAY_MS,
                TOP_BAR_SLIDE_DURATION_MS,
                value -> topBarSlideProgress = value
        );
        ValueAnimator topBarTrimAnimator = buildAnimator(
                TOP_BAR_TRIM_DELAY_MS,
                TOP_BAR_TRIM_DURATION_MS,
                value -> topBarTrimProgress = value
        );
        ValueAnimator middleBarSlideAnimator = buildAnimator(
                MID_BAR_SLIDE_DELAY_MS,
                MID_BAR_SLIDE_DURATION_MS,
                value -> middleBarSlideProgress = value
        );
        ValueAnimator middleBarTrimAnimator = buildAnimator(
                MID_BAR_TRIM_DELAY_MS,
                MID_BAR_TRIM_DURATION_MS,
                value -> middleBarTrimProgress = value
        );

        currentAnimator = new AnimatorSet();
        currentAnimator.playTogether(
                verticalDropAnimator,
                verticalTrimAnimator,
                topBarSlideAnimator,
                topBarTrimAnimator,
                middleBarSlideAnimator,
                middleBarTrimAnimator
        );
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                postDelayed(() -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }, COMPLETION_HOLD_MS);
            }
        });
        currentAnimator.start();
    }

    private ValueAnimator buildAnimator(long startDelay,
                                        long duration,
                                        ProgressConsumer consumer) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setStartDelay(startDelay);
        animator.setDuration(duration);
        animator.setInterpolator(easeInterpolator);
        animator.addUpdateListener(animation -> {
            consumer.accept((float) animation.getAnimatedValue());
            invalidate();
        });
        return animator;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float shaftWidth = width * 0.071f;
        float verticalCenterX = width * 0.405f;
        float verticalHeadLength = shaftWidth * 1.55f;
        float verticalHeadHalfWidth = shaftWidth * 0.85f;
        float finalVerticalTipY = height * 0.580f;
        float finalVerticalTopY = height * 0.400f;

        float verticalTipY = lerp(-height * 0.18f, finalVerticalTipY, verticalDropProgress);
        float verticalTopY = lerp(-height * 0.74f, finalVerticalTopY, verticalTrimProgress);

        drawVerticalArrow(
                canvas,
                verticalCenterX,
                verticalTopY,
                verticalTipY,
                shaftWidth,
                verticalHeadLength,
                verticalHeadHalfWidth
        );

        drawTopBar(canvas, width, shaftWidth, verticalCenterX, finalVerticalTopY);
        drawMiddleArm(canvas, width, shaftWidth, verticalCenterX, finalVerticalTopY);
    }

    private void drawTopBar(Canvas canvas,
                            float width,
                            float shaftWidth,
                            float verticalCenterX,
                            float finalVerticalTopY) {
        if (topBarSlideProgress <= 0f) {
            return;
        }

        float topBarThickness = shaftWidth * 0.95f;
        float topHeadLength = shaftWidth * 1.95f;
        float topHeadHalfHeight = shaftWidth * 0.80f;
        float topLeftTopXoff = shaftWidth * -0.55f;
        float topLeftBotXoff = shaftWidth * -1.25f;

        float topBarTopY = finalVerticalTopY;
        float topBarBottomY = topBarTopY + topBarThickness;
        float topBarTipX = lerp(-(width * 0.16f), width * 0.700f, topBarSlideProgress);
        float topBarLongLeftX = -(width * 0.72f);
        float topBarLeftTopX = lerp(topBarLongLeftX, verticalCenterX + topLeftTopXoff, topBarTrimProgress);
        float topBarLeftBottomX = lerp(topBarLongLeftX, verticalCenterX + topLeftBotXoff, topBarTrimProgress);

        drawHorizontalArrow(
                canvas,
                topBarLeftTopX,
                topBarLeftBottomX,
                topBarTopY,
                topBarBottomY,
                topBarTipX,
                topHeadLength,
                topHeadHalfHeight
        );
    }

    private void drawMiddleArm(Canvas canvas,
                               float width,
                               float shaftWidth,
                               float verticalCenterX,
                               float finalVerticalTopY) {
        if (middleBarSlideProgress <= 0f) {
            return;
        }


        float midGap = shaftWidth * 1.55f;
        float midHeight = shaftWidth * 1.00f;
        float finalLeftX = verticalCenterX + (shaftWidth * 0.25f);
        float finalRightTopX = verticalCenterX + (shaftWidth * 1.50f);
        float finalRightBottomX = verticalCenterX + (shaftWidth * 0.95f);

        float middleTopY = finalVerticalTopY + midGap;
        float middleBottomY = middleTopY + midHeight;
        float longLeftX = -(width * 0.72f);
        float middleLeftX = lerp(longLeftX, finalLeftX, middleBarTrimProgress);
        float middleRightTopX = lerp(-(width * 0.14f), finalRightTopX, middleBarSlideProgress);
        float middleRightBottomX = lerp(-(width * 0.17f), finalRightBottomX, middleBarSlideProgress);

        shapePath.reset();
        shapePath.moveTo(middleLeftX, middleTopY);
        shapePath.lineTo(middleRightTopX, middleTopY);
        shapePath.lineTo(middleRightBottomX, middleBottomY);
        shapePath.lineTo(middleLeftX, middleBottomY);
        shapePath.close();
        canvas.drawPath(shapePath, arrowPaint);
    }

    private void drawVerticalArrow(Canvas canvas,
                                   float centerX,
                                   float topY,
                                   float tipY,
                                   float shaftWidth,
                                   float headLength,
                                   float headHalfWidth) {
        if (tipY <= topY) {
            return;
        }

        float shaftBottomY = Math.max(topY, tipY - headLength);
        shapePath.reset();
        shapePath.moveTo(centerX - (shaftWidth * 0.5f), topY);
        shapePath.lineTo(centerX + (shaftWidth * 0.5f), topY);
        shapePath.lineTo(centerX + (shaftWidth * 0.5f), shaftBottomY);
        shapePath.lineTo(centerX + headHalfWidth, shaftBottomY);
        shapePath.lineTo(centerX, tipY);
        shapePath.lineTo(centerX - headHalfWidth, shaftBottomY);
        shapePath.lineTo(centerX - (shaftWidth * 0.5f), shaftBottomY);
        shapePath.close();
        canvas.drawPath(shapePath, arrowPaint);
    }

    private void drawHorizontalArrow(Canvas canvas,
                                     float leftTopX,
                                     float leftBottomX,
                                     float topY,
                                     float bottomY,
                                     float tipX,
                                     float headLength,
                                     float headHalfHeight) {
        float leftLimitX = Math.min(leftTopX, leftBottomX);
        if (tipX <= leftLimitX) {
            return;
        }

        float centerY = (topY + bottomY) * 0.5f;
        float shaftRightX = Math.max(leftLimitX, tipX - headLength);

        shapePath.reset();
        shapePath.moveTo(leftTopX, topY);
        shapePath.lineTo(shaftRightX, topY);
        shapePath.lineTo(shaftRightX, centerY - headHalfHeight);
        shapePath.lineTo(tipX, centerY);
        shapePath.lineTo(shaftRightX, centerY + headHalfHeight);
        shapePath.lineTo(shaftRightX, bottomY);
        shapePath.lineTo(leftBottomX, bottomY);
        shapePath.close();
        canvas.drawPath(shapePath, arrowPaint);
    }

    private float lerp(float start, float end, float progress) {
        return start + ((end - start) * progress);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
            currentAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    private interface ProgressConsumer {
        void accept(float value);
    }
}
