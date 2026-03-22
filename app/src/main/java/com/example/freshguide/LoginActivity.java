package com.example.freshguide;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

        // Set up blurred background
        ImageView bgImage = findViewById(R.id.bg_image);
        Bitmap blurred = loadBlurredBackground();
        if (blurred != null) {
            bgImage.setImageBitmap(blurred);
        }

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

    private Bitmap loadBlurredBackground() {
        try {
            // Downsample to ~200px width for performance
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.bg_university, opts);

            int targetWidth = 200;
            opts.inSampleSize = opts.outWidth / targetWidth;
            if (opts.inSampleSize < 1) opts.inSampleSize = 1;

            opts.inJustDecodeBounds = false;
            Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.bg_university, opts);
            if (original == null) return null;

            // Apply StackBlur (radius 5 - lighter blur for more visibility)
            return stackBlur(original, 5);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap stackBlur(Bitmap src, int radius) {
        Bitmap bitmap = src.copy(src.getConfig(), true);
        if (radius < 1) return bitmap;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pixels[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pixels[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pixels[yi] = (0xff000000 & pixels[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
