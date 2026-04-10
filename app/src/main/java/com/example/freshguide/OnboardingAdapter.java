package com.example.freshguide;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

    private final List<OnboardingPage> pages;
    private int activePosition = RecyclerView.NO_POSITION;

    public OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    public void setActivePosition(int activePosition) {
        if (this.activePosition == activePosition) {
            return;
        }

        int previousPosition = this.activePosition;
        this.activePosition = activePosition;

        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition);
        }
        if (activePosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(activePosition);
        }
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(pages.get(position), position == activePosition);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        holder.clearAnimations();
        super.onViewRecycled(holder);
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final TextView eyebrow;
        private final TextView title;
        private final TextView description;
        private final MaterialCardView heroPanel;
        private final MaterialCardView heroBadge;
        private final MaterialCardView primaryChip;
        private final MaterialCardView secondaryChip;
        private final MaterialCardView tertiaryChip;
        private final ImageView heroIcon;
        private final ImageView heroWatermark;
        private final ImageView primaryChipIcon;
        private final ImageView secondaryChipIcon;
        private final ImageView tertiaryChipIcon;
        private final TextView primaryChipText;
        private final TextView secondaryChipText;
        private final TextView tertiaryChipText;
        private final View glowPrimary;
        private final View glowSecondary;
        private AnimatorSet entryAnimator;
        private final List<Animator> ambientAnimators = new ArrayList<>();

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            eyebrow = itemView.findViewById(R.id.onboardingEyebrow);
            title = itemView.findViewById(R.id.onboardingTitle);
            description = itemView.findViewById(R.id.onboardingDescription);
            heroPanel = itemView.findViewById(R.id.heroPanel);
            heroBadge = itemView.findViewById(R.id.heroBadge);
            primaryChip = itemView.findViewById(R.id.primaryChip);
            secondaryChip = itemView.findViewById(R.id.secondaryChip);
            tertiaryChip = itemView.findViewById(R.id.tertiaryChip);
            heroIcon = itemView.findViewById(R.id.onboardingImage);
            heroWatermark = itemView.findViewById(R.id.heroWatermark);
            primaryChipIcon = itemView.findViewById(R.id.primaryChipIcon);
            secondaryChipIcon = itemView.findViewById(R.id.secondaryChipIcon);
            tertiaryChipIcon = itemView.findViewById(R.id.tertiaryChipIcon);
            primaryChipText = itemView.findViewById(R.id.primaryChipText);
            secondaryChipText = itemView.findViewById(R.id.secondaryChipText);
            tertiaryChipText = itemView.findViewById(R.id.tertiaryChipText);
            glowPrimary = itemView.findViewById(R.id.heroGlowPrimary);
            glowSecondary = itemView.findViewById(R.id.heroGlowSecondary);
        }

        void bind(OnboardingPage page, boolean isActive) {
            Context context = itemView.getContext();
            int accentColor = ContextCompat.getColor(context, page.getAccentColorRes());
            int surfaceColor = ContextCompat.getColor(context, R.color.background_card);
            int borderColor = ContextCompat.getColor(context, R.color.border_subtle);
            int chipSurface = blendColors(surfaceColor, accentColor, 0.08f);
            int chipStroke = withAlpha(accentColor, 40);
            int badgeFill = blendColors(surfaceColor, accentColor, 0.18f);
            int badgeStroke = withAlpha(accentColor, 92);
            int watermarkColor = withAlpha(accentColor, 34);

            clearAnimations();

            eyebrow.setText(page.getEyebrow());
            title.setText(page.getTitle());
            description.setText(page.getDescription());

            heroPanel.setCardBackgroundColor(surfaceColor);
            heroPanel.setStrokeColor(blendColors(borderColor, accentColor, 0.18f));

            heroBadge.setCardBackgroundColor(badgeFill);
            heroBadge.setStrokeColor(badgeStroke);

            heroIcon.setImageResource(page.getHeroIconRes());
            heroIcon.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
            heroWatermark.setColorFilter(watermarkColor, PorterDuff.Mode.SRC_IN);

            glowPrimary.setBackground(createGlowDrawable(accentColor, 138f));
            glowSecondary.setBackground(createGlowDrawable(blendColors(accentColor,
                    ContextCompat.getColor(context, R.color.orange_accent), 0.35f), 112f));

            bindChip(primaryChip, primaryChipIcon, primaryChipText,
                    page.getPrimaryChipIconRes(), page.getPrimaryChipLabel(), chipSurface, chipStroke, accentColor);
            bindChip(secondaryChip, secondaryChipIcon, secondaryChipText,
                    page.getSecondaryChipIconRes(), page.getSecondaryChipLabel(), chipSurface, chipStroke, accentColor);
            bindChip(tertiaryChip, tertiaryChipIcon, tertiaryChipText,
                    page.getTertiaryChipIconRes(), page.getTertiaryChipLabel(), chipSurface, chipStroke, accentColor);

            if (isActive) {
                playEntryAnimation();
            } else {
                applyRestingState();
            }
        }

        void clearAnimations() {
            if (entryAnimator != null) {
                entryAnimator.cancel();
                entryAnimator = null;
            }

            itemView.animate().cancel();
            eyebrow.animate().cancel();
            heroPanel.animate().cancel();
            title.animate().cancel();
            description.animate().cancel();
            primaryChip.animate().cancel();
            secondaryChip.animate().cancel();
            tertiaryChip.animate().cancel();
            heroBadge.animate().cancel();

            for (Animator animator : ambientAnimators) {
                animator.cancel();
            }
            ambientAnimators.clear();
        }

        private void bindChip(
                MaterialCardView chip,
                ImageView iconView,
                TextView textView,
                int iconRes,
                String label,
                int fillColor,
                int strokeColor,
                int accentColor) {
            chip.setCardBackgroundColor(fillColor);
            chip.setStrokeColor(strokeColor);

            iconView.setImageResource(iconRes);
            iconView.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
            textView.setText(label);
        }

        private void playEntryAnimation() {
            prepareView(eyebrow, 20f, 0.96f);
            prepareView(heroPanel, 30f, 0.94f);
            prepareView(title, 22f, 0.98f);
            prepareView(description, 18f, 0.99f);
            prepareView(primaryChip, 14f, 0.95f);
            prepareView(secondaryChip, 14f, 0.95f);
            prepareView(tertiaryChip, 14f, 0.95f);
            prepareView(heroBadge, 0f, 0.86f);

            entryAnimator = new AnimatorSet();
            entryAnimator.playTogether(
                    createFadeUpAnimator(eyebrow, 0L),
                    createFadeUpAnimator(heroPanel, 80L),
                    createFadeUpAnimator(title, 150L),
                    createFadeUpAnimator(description, 220L),
                    createFadeUpAnimator(primaryChip, 220L),
                    createFadeUpAnimator(secondaryChip, 280L),
                    createFadeUpAnimator(tertiaryChip, 340L),
                    createBadgeAnimator()
            );
            entryAnimator.start();

            startAmbientMotion(heroBadge, -10f, 3200L, 700L);
            startAmbientMotion(primaryChip, -8f, 3600L, 760L);
            startAmbientMotion(secondaryChip, 7f, 3900L, 860L);
            startAmbientMotion(tertiaryChip, -6f, 3400L, 940L);
        }

        private void applyRestingState() {
            setDefaultState(eyebrow);
            setDefaultState(heroPanel);
            setDefaultState(title);
            setDefaultState(description);
            setDefaultState(primaryChip);
            setDefaultState(secondaryChip);
            setDefaultState(tertiaryChip);
            setDefaultState(heroBadge);
        }

        private void prepareView(View view, float translationDp, float scale) {
            view.setAlpha(0f);
            view.setTranslationY(dp(translationDp));
            view.setScaleX(scale);
            view.setScaleY(scale);
        }

        private void setDefaultState(View view) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        }

        private AnimatorSet createFadeUpAnimator(View view, long delayMs) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
            ObjectAnimator translate = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.getTranslationY(), 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, view.getScaleX(), 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, view.getScaleY(), 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(alpha, translate, scaleX, scaleY);
            animatorSet.setStartDelay(delayMs);
            animatorSet.setDuration(460L);
            return animatorSet;
        }

        private AnimatorSet createBadgeAnimator() {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(heroBadge, View.ALPHA, 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(heroBadge, View.SCALE_X, heroBadge.getScaleX(), 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(heroBadge, View.SCALE_Y, heroBadge.getScaleY(), 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(alpha, scaleX, scaleY);
            animatorSet.setStartDelay(170L);
            animatorSet.setDuration(520L);
            return animatorSet;
        }

        private void startAmbientMotion(View view, float travelDp, long durationMs, long delayMs) {
            float distance = dp(travelDp);
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, distance, 0f);
            animator.setDuration(durationMs);
            animator.setStartDelay(delayMs);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            ambientAnimators.add(animator);
            animator.start();
        }

        private GradientDrawable createGlowDrawable(int color, float radiusDp) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            drawable.setGradientRadius(dp(radiusDp));
            drawable.setColors(new int[]{
                    withAlpha(color, 78),
                    withAlpha(color, 18),
                    Color.TRANSPARENT
            });
            return drawable;
        }

        private int dp(float value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private int withAlpha(int color, int alpha) {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        private int blendColors(int startColor, int endColor, float ratio) {
            float clampedRatio = Math.max(0f, Math.min(1f, ratio));
            int red = Math.round(Color.red(startColor) + ((Color.red(endColor) - Color.red(startColor)) * clampedRatio));
            int green = Math.round(Color.green(startColor) + ((Color.green(endColor) - Color.green(startColor)) * clampedRatio));
            int blue = Math.round(Color.blue(startColor) + ((Color.blue(endColor) - Color.blue(startColor)) * clampedRatio));
            return Color.rgb(red, green, blue);
        }
    }
}
