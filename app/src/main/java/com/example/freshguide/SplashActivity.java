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

import com.example.freshguide.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int STEP1_DURATION_MS  = 1000;
    private static final int STEP2_DURATION_MS  = 800;
    private static final int SPINNER_FADE_MS    = 400;
    private static final int LOADING_HOLD_MS    = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoMark  = findViewById(R.id.logoMark);
        ImageView logoFull  = findViewById(R.id.logoFull);
        ProgressBar spinner = findViewById(R.id.loadingSpinner);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {


            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(STEP2_DURATION_MS);
            fadeOut.setFillAfter(true);
            logoMark.startAnimation(fadeOut);


            logoFull.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(STEP2_DURATION_MS);
            fadeIn.setFillAfter(true);
            logoFull.startAnimation(fadeIn);


            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {

                    spinner.setVisibility(View.VISIBLE);
                    AlphaAnimation spinnerFadeIn = new AlphaAnimation(0f, 1f);
                    spinnerFadeIn.setDuration(SPINNER_FADE_MS);
                    spinnerFadeIn.setFillAfter(true);
                    spinner.startAnimation(spinnerFadeIn);


                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        // =========================================================
                        // TESTING MODE: auto-create session and skip LoginActivity
                        // comment this after testing
                        // =========================================================
                        SessionManager session = SessionManager.getInstance(SplashActivity.this);
//                        session.saveSession(
//                                "debug_token_123",
//                                SessionManager.ROLE_STUDENT,
//                                "20230372-S",
//                                "Test Student"
//                        );

                        session.saveSession(
                                "debug_token_123",
                                SessionManager.ROLE_ADMIN,
                                "20230372-S",
                                "Test Student"
                        );
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));

                        // =========================================================
                        // ORIGINAL CODE: go to login
                        // Uncomment this after testing is done
                        // =========================================================
                        // startActivity(new Intent(SplashActivity.this, LoginActivity.class));

                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();

                    }, LOADING_HOLD_MS);
                }
            });

        }, STEP1_DURATION_MS);
    }
}