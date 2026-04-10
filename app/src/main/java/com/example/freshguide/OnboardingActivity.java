package com.example.freshguide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

import com.example.freshguide.util.SessionManager;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "freshguide_prefs";
    public static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private ViewPager2 viewPager;
    private AppCompatButton btnNext;
    private TextView btnSkip;
    private TextView pageCounter;
    private LinearLayout dotsContainer;
    private OnboardingAdapter adapter;

    private List<OnboardingPage> pages;

    public static boolean shouldShow(Context context) {
        return !getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }

    public static void markComplete(Context context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager      = findViewById(R.id.viewPager);
        btnNext        = findViewById(R.id.btnNext);
        btnSkip        = findViewById(R.id.btnSkip);
        pageCounter    = findViewById(R.id.pageCounter);
        dotsContainer  = findViewById(R.id.dotsContainer);

        pages = Arrays.asList(
                new OnboardingPage(
                        R.color.green_primary,
                        R.drawable.ic_search,
                        getString(R.string.onboarding_eyebrow_search),
                        getString(R.string.onboarding_title_search),
                        getString(R.string.onboarding_description_search),
                        R.drawable.ic_find_room,
                        getString(R.string.onboarding_chip_search_rooms),
                        R.drawable.ic_department,
                        getString(R.string.onboarding_chip_search_floors),
                        R.drawable.ic_search_history,
                        getString(R.string.onboarding_chip_search_recent)
                ),
                new OnboardingPage(
                        R.color.orange_accent,
                        R.drawable.ic_directions,
                        getString(R.string.onboarding_eyebrow_route),
                        getString(R.string.onboarding_title_route),
                        getString(R.string.onboarding_description_route),
                        R.drawable.ic_search_pin,
                        getString(R.string.onboarding_chip_route_origin),
                        R.drawable.ic_chevron_right,
                        getString(R.string.onboarding_chip_route_steps),
                        R.drawable.ic_emergency_exit,
                        getString(R.string.onboarding_chip_route_exit)
                ),
                new OnboardingPage(
                        R.color.profile_gradient_mid,
                        R.drawable.ic_my_schedule,
                        getString(R.string.onboarding_eyebrow_schedule),
                        getString(R.string.onboarding_title_schedule),
                        getString(R.string.onboarding_description_schedule),
                        R.drawable.ic_bookmark_filled,
                        getString(R.string.onboarding_chip_schedule_saved),
                        R.drawable.ic_my_schedule,
                        getString(R.string.onboarding_chip_schedule_week),
                        R.drawable.ic_nav_profile,
                        getString(R.string.onboarding_chip_schedule_profile)
                )
        );

        adapter = new OnboardingAdapter(pages);
        viewPager.setAdapter(adapter);
        setupViewPager();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUI(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        updateUI(0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int current = viewPager.getCurrentItem();
                if (current > 0) {
                    viewPager.setCurrentItem(current - 1, true);
                    return;
                }
                finish();
            }
        });
    }

    private void setupViewPager() {
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        viewPager.setOffscreenPageLimit(pages.size());
        viewPager.setPadding(dpToPx(18), 0, dpToPx(18), 0);

        View child = viewPager.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
            ((RecyclerView) child).setClipToPadding(false);
        }

        viewPager.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            float progress = Math.max(0f, 1f - absPosition);
            float scale = 0.92f + (progress * 0.08f);

            page.setAlpha(0.6f + (progress * 0.4f));
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setTranslationX(-position * dpToPx(18));
            page.setTranslationY(absPosition * dpToPx(14));
        });
    }

    private void updateUI(int position) {
        pageCounter.setText(getString(R.string.onboarding_page_counter, position + 1, pages.size()));
        setupDots(position);
        adapter.setActivePosition(position);

        boolean isLast = position == pages.size() - 1;
        btnNext.setText(isLast
                ? R.string.onboarding_button_get_started
                : R.string.onboarding_button_next);
        btnSkip.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        btnSkip.setEnabled(!isLast);
    }

    private void setupDots(int activeIndex) {
        dotsContainer.removeAllViews();
        int height = dpToPx(8);
        int activeWidth = dpToPx(28);
        int inactiveWidth = dpToPx(8);
        int margin = dpToPx(4);

        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(this);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == activeIndex ? activeWidth : inactiveWidth,
                    height
            );
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dotsContainer.addView(dot);
        }
    }

    private void finishOnboarding() {
        markComplete(this);
        startActivity(createPostOnboardingIntent());
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private Intent createPostOnboardingIntent() {
        Class<?> destination = SessionManager.getInstance(this).isLoggedIn()
                ? MainActivity.class
                : LoginActivity.class;
        return new Intent(this, destination);
    }

    private int dpToPx(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
