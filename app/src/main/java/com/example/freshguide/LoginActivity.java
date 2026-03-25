package com.example.freshguide;

import android.content.Intent;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Configuration;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.freshguide.viewmodel.LoginViewModel;

import java.util.Locale;
import java.util.regex.Pattern;


public class LoginActivity extends AppCompatActivity {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8}-(S|N|C)$");

    private boolean passwordVisible = false;
    private boolean adminMode = false;
    private LoginViewModel viewModel;

    private EditText inputUsername;
    private EditText inputPassword;
    private ImageButton btnTogglePassword;
    private Button btnSignIn;
    private Button btnCreateAccount;
    private TextView btnManualStudentInput;
    private ProgressBar progressBar;
    private android.widget.TextView tvLoginHint;
    private View labelUsername;
    private View labelPassword;
    private FrameLayout passwordRow;


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

        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnManualStudentInput = findViewById(R.id.btn_manual_student_input);
        progressBar = findViewById(R.id.progress_bar);
        tvLoginHint = findViewById(R.id.tv_login_hint);
        labelUsername = findViewById(R.id.labelUsername);
        labelPassword = findViewById(R.id.labelPassword);
        passwordRow = findViewById(R.id.passwordRow);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                inputPassword.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        btnSignIn.setOnClickListener(v -> {
            inputUsername.setError(null);

            if (adminMode) {
                String email = inputUsername.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                viewModel.loginAdmin(email, password);
                return;
            }

            Intent scannerIntent = new Intent(this, QrScannerActivity.class);
            qrScannerLauncher.launch(scannerIntent);
        });

        btnCreateAccount.setOnClickListener(v -> setAdminMode(!adminMode));

        if (BuildConfig.DEBUG && btnManualStudentInput != null) {
            btnManualStudentInput.setVisibility(View.VISIBLE);
            btnManualStudentInput.setOnClickListener(v -> showManualStudentIdDialog());
        }

        viewModel.getState().observe(this, state -> {
            boolean loading = state == LoginViewModel.State.LOADING;
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSignIn.setEnabled(!loading);
            btnCreateAccount.setEnabled(!loading);
            if (btnManualStudentInput != null) {
                btnManualStudentInput.setEnabled(!loading);
            }

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

        viewModel.getErrorMessage().observe(this, err -> {
            if (err == null || err.isEmpty()) {
                return;
            }
            if (adminMode) {
                inputUsername.setError(err);
            } else {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
        });

        applyPatternAlpha();

        setAdminMode(false);
    }

    private void setAdminMode(boolean enabled) {
        adminMode = enabled;

        int visibility = enabled ? View.VISIBLE : View.GONE;
        if (labelUsername != null) {
            labelUsername.setVisibility(visibility);
        }
        inputUsername.setVisibility(visibility);
        if (labelPassword != null) {
            labelPassword.setVisibility(visibility);
        }
        passwordRow.setVisibility(visibility);

        if (btnManualStudentInput != null) {
            boolean showManual = BuildConfig.DEBUG && !enabled;
            btnManualStudentInput.setVisibility(showManual ? View.VISIBLE : View.GONE);
        }

        if (enabled) {
            tvLoginHint.setText(R.string.login_hint_admin_mode);
            inputUsername.setHint(R.string.hint_admin_email);
            btnSignIn.setText(R.string.btn_sign_in_admin);
            btnCreateAccount.setText(R.string.btn_switch_to_qr_login);
        } else {
            tvLoginHint.setText(R.string.login_hint_qr_mode);
            inputUsername.setText("");
            inputPassword.setText("");
            btnSignIn.setText(R.string.btn_scan_qr_login);
            btnCreateAccount.setText(R.string.btn_switch_to_admin_login);
        }
    }

    private void showManualStudentIdDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setHint(R.string.register_student_id_hint);

        new AlertDialog.Builder(this)
                .setTitle(R.string.manual_input_title)
                .setMessage(R.string.manual_input_subtitle)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.manual_input_continue, (dialog, which) -> {
                    String studentId = input.getText().toString().trim().toUpperCase(Locale.ROOT);
                    if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
                        Toast.makeText(this, R.string.error_student_id_format, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.loginStudent(studentId);
                })
                .show();
    }

    private void applyPatternAlpha() {
        View patternView = findViewById(R.id.iv_pattern);
        patternView.getBackground().setAlpha(255);
    }

}
