package com.example.freshguide;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.style.UnderlineSpan;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.freshguide.viewmodel.LoginViewModel;

import java.util.regex.Pattern;


public class LoginActivity extends AppCompatActivity {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8}-(S|N|C)$");

    private boolean passwordVisible = false;
    private boolean adminMode = false;
    private boolean studentManualMode = false;
    private LoginViewModel viewModel;

    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputStudentId;
    private ImageButton btnTogglePassword;
    private Button btnSignIn;
    private Button btnCreateAccount;
    private ProgressBar progressBar;
    private View labelUsername;
    private View labelPassword;
    private View labelStudentId;
    private FrameLayout passwordRow;
    private TextView btnManualStudentInput;


    private final ActivityResultLauncher<Intent> qrScannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                String studentId = result.getData().getStringExtra(QrScannerActivity.EXTRA_STUDENT_ID);
                if (studentId == null || !STUDENT_ID_PATTERN.matcher(studentId).matches()) {
                    Toast.makeText(this, R.string.error_student_id_format, Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.loginStudent(studentId);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputUsername         = findViewById(R.id.inputUsername);
        inputPassword         = findViewById(R.id.inputPassword);
        inputStudentId        = findViewById(R.id.inputStudentId);
        btnTogglePassword     = findViewById(R.id.btnTogglePassword);
        btnSignIn             = findViewById(R.id.btnSignIn);
        btnCreateAccount      = findViewById(R.id.btnCreateAccount);
        progressBar           = findViewById(R.id.progress_bar);
        labelUsername         = findViewById(R.id.labelUsername);
        labelPassword         = findViewById(R.id.labelPassword);
        labelStudentId        = findViewById(R.id.labelStudentId);
        passwordRow           = findViewById(R.id.passwordRow);
        btnManualStudentInput = findViewById(R.id.btnManualStudentInput);

        // ── Partial underline: only "Enter Student ID" ─────────────────────
        String fullText = getString(R.string.manual_login);
        String underlinePart = "Enter Student ID";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(underlinePart);
        if (start >= 0) {
            spannable.setSpan(
                    new UnderlineSpan(),
                    start,
                    start + underlinePart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        btnManualStudentInput.setText(spannable);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // ── Default: password hidden → slashed eye ─────────────────────────
        // eye slashed = dots (hidden), eye open = plain text (visible)
        btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
        btnTogglePassword.setColorFilter(
                ContextCompat.getColor(this, R.color.text_hint),
                PorterDuff.Mode.SRC_IN);

        // ── Toggle password visibility ──────────────────────────────────────
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;

            if (passwordVisible) {
                // Plain text → show open eye (eye open = revealed)
                inputPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                btnTogglePassword.setImageResource(R.drawable.ic_eye);
                btnTogglePassword.setColorFilter(
                        ContextCompat.getColor(this, R.color.green_primary),
                        PorterDuff.Mode.SRC_IN);
            } else {
                // Dots → show slashed eye (eye slashed = hidden)
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
                btnTogglePassword.setColorFilter(
                        ContextCompat.getColor(this, R.color.text_hint),
                        PorterDuff.Mode.SRC_IN);
            }

            inputPassword.setSelection(inputPassword.getText().length());
        });

        // ── Primary button: Scan QR / Sign In Admin / Sign In Student ──────
        btnSignIn.setOnClickListener(v -> {
            inputUsername.setError(null);
            inputStudentId.setError(null);

            if (adminMode) {
                String email    = inputUsername.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                viewModel.loginAdmin(email, password);
                return;
            }

            if (studentManualMode) {
                String sid = inputStudentId.getText().toString().trim();
                if (!STUDENT_ID_PATTERN.matcher(sid).matches()) {
                    inputStudentId.setError(getString(R.string.error_student_id_format));
                    return;
                }
                viewModel.loginStudent(sid);
                return;
            }

            // Default: QR scan
            Intent scannerIntent = new Intent(this, QrScannerActivity.class);
            qrScannerLauncher.launch(scannerIntent);
        });

        // ── Secondary button: toggle admin / back to QR ────────────────────
        btnCreateAccount.setOnClickListener(v -> {
            if (studentManualMode) {
                setStudentManualMode(false);
                return;
            }
            setAdminMode(!adminMode);
        });

        // ── "Don't have an ID? Enter Student ID" ───────────────────────────
        btnManualStudentInput.setOnClickListener(v -> {
            setAdminMode(false);
            setStudentManualMode(!studentManualMode);
        });

        // ── Observe login state ────────────────────────────────────────────
        viewModel.getState().observe(this, state -> {
            boolean loading = state == LoginViewModel.State.LOADING;
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSignIn.setEnabled(!loading);
            btnCreateAccount.setEnabled(!loading);

            if (state == LoginViewModel.State.SUCCESS_STUDENT || state == LoginViewModel.State.SUCCESS_ADMIN) {
                boolean onboardingDone = getSharedPreferences(
                        OnboardingActivity.PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(OnboardingActivity.KEY_ONBOARDING_COMPLETE, false);

                Intent next = onboardingDone
                        ? new Intent(this, MainActivity.class)
                        : new Intent(this, OnboardingActivity.class);
                startActivity(next);
                finish();
            }
        });

        // ── Observe error messages ─────────────────────────────────────────
        viewModel.getErrorMessage().observe(this, err -> {
            if (err == null || err.isEmpty()) return;
            if (adminMode) {
                inputUsername.setError(err);
            } else if (studentManualMode) {
                inputStudentId.setError(err);
            } else {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
        });

        applyPatternAlpha();
        setAdminMode(false);
    }

    // ── Admin mode ─────────────────────────────────────────────────────────
    private void setAdminMode(boolean enabled) {
        adminMode = enabled;
        if (enabled) studentManualMode = false;

        int adminVis = enabled ? View.VISIBLE : View.GONE;
        if (labelUsername != null) labelUsername.setVisibility(adminVis);
        inputUsername.setVisibility(adminVis);
        if (labelPassword != null) labelPassword.setVisibility(adminVis);
        passwordRow.setVisibility(adminVis);

        // Student ID field always hidden when in admin mode
        if (labelStudentId != null) labelStudentId.setVisibility(View.GONE);
        inputStudentId.setVisibility(View.GONE);

        if (enabled) {
            inputUsername.setHint(R.string.hint_admin_email);
            btnSignIn.setText(R.string.btn_sign_in_admin);
            btnCreateAccount.setText(R.string.btn_switch_to_qr_login);
            btnManualStudentInput.setVisibility(View.GONE);

            // Reset eye icon to slashed (dots = hidden)
            passwordVisible = false;
            inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            btnTogglePassword.setColorFilter(
                    ContextCompat.getColor(this, R.color.text_hint),
                    PorterDuff.Mode.SRC_IN);

        } else if (!studentManualMode) {
            inputUsername.setText("");
            inputPassword.setText("");
            btnSignIn.setText(R.string.btn_scan_qr_login);
            btnCreateAccount.setText(R.string.btn_switch_to_admin_login);
            btnManualStudentInput.setVisibility(View.VISIBLE);
        }
    }

    // ── Student manual-ID mode ─────────────────────────────────────────────
    private void setStudentManualMode(boolean enabled) {
        studentManualMode = enabled;
        if (enabled) adminMode = false;

        int studentVis = enabled ? View.VISIBLE : View.GONE;
        if (labelStudentId != null) labelStudentId.setVisibility(studentVis);
        inputStudentId.setVisibility(studentVis);

        // Hide admin fields
        if (labelUsername != null) labelUsername.setVisibility(View.GONE);
        inputUsername.setVisibility(View.GONE);
        if (labelPassword != null) labelPassword.setVisibility(View.GONE);
        passwordRow.setVisibility(View.GONE);

        if (enabled) {
            btnSignIn.setText(R.string.btn_sign_in_student);
            btnCreateAccount.setText(R.string.btn_switch_to_qr_login);
            btnManualStudentInput.setVisibility(View.GONE);
        } else {
            inputStudentId.setText("");
            btnSignIn.setText(R.string.btn_scan_qr_login);
            btnCreateAccount.setText(R.string.btn_switch_to_admin_login);
            btnManualStudentInput.setVisibility(View.VISIBLE);
        }
    }

    private void applyPatternAlpha() {
        View patternView = findViewById(R.id.iv_pattern);
        patternView.getBackground().setAlpha(255);
    }
}