package com.example.freshguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freshguide.ui.view.SplashArrowAnimationView;
import com.example.freshguide.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final boolean ENABLE_STUDENT_TEST_BYPASS = true;
    private static final String TEST_ADMIN_TOKEN = "debug_admin_token";
    private static final String TEST_STUDENT_TOKEN = "local_debug_student_token";
    private static final String LEGACY_DEBUG_TOKEN = "debug_token_123";
    private static final String TEST_STUDENT_ID = "20230372-S";
    private static final String TEST_STUDENT_NAME = "Test Student";
    private static final long SPLASH_EXIT_DURATION_MS = 260L;

    private boolean handoffStarted = false;
    private View splashRoot;
    private SplashArrowAnimationView splashArrowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashRoot = findViewById(R.id.splashRoot);
        splashArrowView = findViewById(R.id.splashArrowView);
        splashArrowView.post(() -> splashArrowView.startRevealSequence(this::finishSplash));
    }

    private void finishSplash() {
        if (handoffStarted || splashRoot == null || splashArrowView == null) {
            return;
        }
        handoffStarted = true;

        float exitLift = getResources().getDisplayMetrics().density * 12f;
        PathInterpolator exitInterpolator = new PathInterpolator(0.3f, 0f, 0.2f, 1f);

        splashRoot.animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .setInterpolator(exitInterpolator)
                .start();

        splashArrowView.animate()
                .alpha(0f)
                .translationY(-exitLift)
                .scaleX(0.965f)
                .scaleY(0.965f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .setInterpolator(exitInterpolator)
                .withEndAction(this::navigateFromSplash)
                .start();
    }

    private void navigateFromSplash() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        SessionManager session = SessionManager.getInstance(SplashActivity.this);
        String token = session.getToken();

        if (TEST_ADMIN_TOKEN.equals(token)
                || TEST_STUDENT_TOKEN.equals(token)
                || LEGACY_DEBUG_TOKEN.equals(token)) {
            session.clearSession();
        }

        Intent nextIntent;
        if (BuildConfig.DEBUG && ENABLE_STUDENT_TEST_BYPASS) {
            session.saveSession(
                    TEST_STUDENT_TOKEN,
                    SessionManager.ROLE_STUDENT,
                    TEST_STUDENT_ID,
                    TEST_STUDENT_NAME
            );
            nextIntent = new Intent(SplashActivity.this, MainActivity.class);
        } else if (session.isLoggedIn()) {
            nextIntent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(nextIntent);
        overridePendingTransition(R.anim.splash_next_enter, R.anim.splash_current_exit);
        finish();
    }
}
