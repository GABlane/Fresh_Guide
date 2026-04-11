package com.example.freshguide;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.freshguide.ui.view.SplashArrowAnimationView;
import com.example.freshguide.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long CIRCLE_REVEAL_DURATION_MS = 460L;

    private boolean handoffStarted = false;
    private View splashRoot;
    private SplashArrowAnimationView splashArrowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashRoot      = findViewById(R.id.splashRoot);
        splashArrowView = findViewById(R.id.splashArrowView);

        splashArrowView.post(() ->
                splashArrowView.startRevealSequence(this::finishSplash));
    }


    private void finishSplash() {
        if (handoffStarted || splashRoot == null || splashArrowView == null) {
            return;
        }
        handoffStarted = true;

        int[] loc = new int[2];
        splashArrowView.getLocationInWindow(loc);
        int cx = loc[0] + splashArrowView.getWidth()  / 2;
        int cy = loc[1] + splashArrowView.getHeight() / 2;

        View revealOverlay = new View(this);
        revealOverlay.setBackgroundColor(
                ContextCompat.getColor(this, R.color.splash_reveal_surface));

        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        decor.addView(revealOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        float maxRadius = (float) Math.hypot(decor.getWidth(), decor.getHeight());
        Animator reveal = ViewAnimationUtils.createCircularReveal(
                revealOverlay, cx, cy, 0f, maxRadius);
        reveal.setDuration(CIRCLE_REVEAL_DURATION_MS);
        reveal.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));

        splashArrowView.animate()
                .alpha(0f)
                .setDuration(CIRCLE_REVEAL_DURATION_MS / 2)
                .start();

        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navigateFromSplash();
            }
        });

        reveal.start();
    }


    private void navigateFromSplash() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Intent nextIntent;
        if (OnboardingActivity.shouldShow(this)) {
            nextIntent = new Intent(SplashActivity.this, OnboardingActivity.class);
        } else if (SessionManager.getInstance(SplashActivity.this).isLoggedIn()) {
            nextIntent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(nextIntent);

        overridePendingTransition(0, 0);
        finish();
    }
}
