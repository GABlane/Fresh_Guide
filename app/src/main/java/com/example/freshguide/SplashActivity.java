package com.example.freshguide;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int STEP1_DURATION_MS  = 1000; // mark only
    private static final int STEP2_DURATION_MS  = 800;  // crossfade mark → full logo
    private static final int SPINNER_FADE_MS    = 400;  // spinner fade in
    private static final int LOADING_HOLD_MS    = 1200; // hold spinner before going to login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoMark     = findViewById(R.id.logoMark);
        ImageView logoFull     = findViewById(R.id.logoFull);
        ProgressBar spinner    = findViewById(R.id.loadingSpinner);

        // Step 1: Show mark only, then crossfade to full logo
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Fade out mark
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(STEP2_DURATION_MS);
            fadeOut.setFillAfter(true);
            logoMark.startAnimation(fadeOut);

            // Fade in full logo
            logoFull.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(STEP2_DURATION_MS);
            fadeIn.setFillAfter(true);
            logoFull.startAnimation(fadeIn);

            // Step 2: After full logo is visible, fade in the spinner
            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}

                @Override
                public void onAnimationEnd(Animation a) {
                    // Fade in spinner
                    spinner.setVisibility(View.VISIBLE);
                    AlphaAnimation spinnerFadeIn = new AlphaAnimation(0f, 1f);
                    spinnerFadeIn.setDuration(SPINNER_FADE_MS);
                    spinnerFadeIn.setFillAfter(true);
                    spinner.startAnimation(spinnerFadeIn);

                    // Step 3: Hold the loading state, then go to login
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, LOADING_HOLD_MS);
                }
            });

        }, STEP1_DURATION_MS);
    }
}
