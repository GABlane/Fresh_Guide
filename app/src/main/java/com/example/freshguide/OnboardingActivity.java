package com.example.freshguide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "freshguide_prefs";
    public static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView btnSkip;
    private TextView pageCounter;
    private LinearLayout dotsContainer;

    private List<OnboardingPage> pages;

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
                R.drawable.bg_onboarding_image,
                "Find Any Room on Campus",
                "Search rooms by name, building,\ndepartment, or room type"
            ),
            new OnboardingPage(
                R.drawable.bg_onboarding_image,
                "Navigate to Safety",
                "Follow highlighted exit routes and\ndirectional arrows to the nearest exit"
            ),
            new OnboardingPage(
                R.drawable.bg_onboarding_image,
                "Stay Prepared",
                "Access safety reminders and evacuation\ninstructions anytime, even offline"
            )
        );

        viewPager.setAdapter(new OnboardingAdapter(pages));
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUI(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void updateUI(int position) {
        pageCounter.setText((position + 1) + " OF " + pages.size());
        setupDots(position);

        boolean isLast = position == pages.size() - 1;
        btnNext.setText(isLast ? "Get Started" : "Next");
        btnSkip.setVisibility(isLast ? View.GONE : View.VISIBLE);
    }

    private void setupDots(int activeIndex) {
        dotsContainer.removeAllViews();
        int size = (int) (8 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < pages.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(i == activeIndex ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dotsContainer.addView(dot);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
