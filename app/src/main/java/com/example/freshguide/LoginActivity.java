package com.example.freshguide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText inputUsername = findViewById(R.id.inputUsername);
        EditText inputPassword = findViewById(R.id.inputPassword);
        ImageButton btnTogglePassword = findViewById(R.id.btnTogglePassword);
        Button btnSignIn = findViewById(R.id.btnSignIn);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Password visibility toggle
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                inputPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Keep cursor at end
            inputPassword.setSelection(inputPassword.getText().length());
        });

        // Sign in
        btnSignIn.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError("Username is required");
                return;
            }
            if (password.isEmpty()) {
                inputPassword.setError("Password is required");
                return;
            }

            // TODO: Replace with real auth logic
            Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();

            // First login → show onboarding, otherwise go straight to main
            SharedPreferences prefs = getSharedPreferences(
                    OnboardingActivity.PREFS_NAME, MODE_PRIVATE);
            boolean onboardingDone = prefs.getBoolean(
                    OnboardingActivity.KEY_ONBOARDING_COMPLETE, false);

            Intent next = onboardingDone
                    ? new Intent(LoginActivity.this, MainActivity.class)
                    : new Intent(LoginActivity.this, OnboardingActivity.class);
            startActivity(next);
            finish();
        });

        // Create account
        btnCreateAccount.setOnClickListener(v -> {
            // TODO: Navigate to registration screen
            Toast.makeText(this, "Registration coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}
