package com.example.freshguide;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class QrScannerActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "extra_student_id";
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8}-(S|N|C)$");

    // ── Views ──────────────────────────────────────────────────────────────
    private PreviewView previewView;
    private TextView    tvScannerStatus;
    private ImageView   btnFlashToggle;
    private View        scanLine;

    // ── Camera / ML Kit ────────────────────────────────────────────────────
    private ExecutorService       cameraExecutor;
    private BarcodeScanner        barcodeScanner;
    private ProcessCameraProvider cameraProvider;
    private Camera                camera;

    // ── State ──────────────────────────────────────────────────────────────
    private volatile boolean frameInProgress;
    private boolean          resultSent;
    private boolean          isFlashOn = false;

    // ── Scan-line animator ─────────────────────────────────────────────────
    private ObjectAnimator scanLineAnimator;

    // ── Camera permission launcher ─────────────────────────────────────────
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this,
                            "Camera permission is required to scan your ID",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    // ── Gallery / Photo Picker launcher ───────────────────────────────────
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            processGalleryImage(uri);
                        }
                    });

    // ══════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_qr_scanner);

        // ── Bind views ──────────────────────────────────────────────────
        previewView     = findViewById(R.id.preview_view);
        tvScannerStatus = findViewById(R.id.tv_scanner_status);
        btnFlashToggle  = findViewById(R.id.btn_flash_toggle);
        scanLine        = findViewById(R.id.scan_line);

        // ── Listeners ───────────────────────────────────────────────────
        findViewById(R.id.btn_close_scanner).setOnClickListener(v -> finish());
        btnFlashToggle.setOnClickListener(v -> toggleFlash());
        findViewById(R.id.btn_gallery).setOnClickListener(v -> openGallery());

        // ── Camera + ML Kit ─────────────────────────────────────────────
        cameraExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        requestCameraOrStart();
        startScanLineAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanLineAnimator != null) scanLineAnimator.cancel();
        if (cameraProvider   != null) cameraProvider.unbindAll();
        if (barcodeScanner   != null) barcodeScanner.close();
        if (cameraExecutor   != null) cameraExecutor.shutdown();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Flash toggle
    // ══════════════════════════════════════════════════════════════════════

    private void toggleFlash() {
        if (camera == null) return;

        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);

        btnFlashToggle.animate()
                .scaleX(0.78f).scaleY(0.78f).setDuration(80)
                .withEndAction(() -> {
                    btnFlashToggle.setImageResource(
                            isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
                    btnFlashToggle.setBackgroundResource(
                            isFlashOn ? R.drawable.bg_flash_btn_on : R.drawable.bg_flash_btn_off);
                    btnFlashToggle.animate()
                            .scaleX(1f).scaleY(1f).setDuration(130).start();
                }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Gallery / image picker
    // ══════════════════════════════════════════════════════════════════════

    private void openGallery() {
        galleryLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
    }

    private void processGalleryImage(@NonNull Uri uri) {
        if (resultSent) return;

        tvScannerStatus.setText("Scanning image…");

        if (scanLineAnimator != null) scanLineAnimator.pause();

        new Thread(() -> {
            try {
                InputImage image = InputImage.fromFilePath(this, uri);
                barcodeScanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            boolean found = false;
                            for (Barcode barcode : barcodes) {
                                String raw = barcode.getRawValue();
                                if (raw == null) continue;
                                String normalized = raw.trim().toUpperCase();
                                if (STUDENT_ID_PATTERN.matcher(normalized).matches()) {
                                    onStudentIdScanned(normalized);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                runOnUiThread(() -> {
                                    tvScannerStatus.setText(R.string.qr_scanner_hint);
                                    if (scanLineAnimator != null) scanLineAnimator.resume();
                                    Toast.makeText(this,
                                            "No valid student ID QR found in this image.",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            tvScannerStatus.setText(R.string.qr_scanner_hint);
                            if (scanLineAnimator != null) scanLineAnimator.resume();
                            Toast.makeText(this,
                                    "Could not read image. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    tvScannerStatus.setText(R.string.qr_scanner_hint);
                    if (scanLineAnimator != null) scanLineAnimator.resume();
                    Toast.makeText(this, "Failed to open image.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Scan-line animation
    // ══════════════════════════════════════════════════════════════════════

    private void startScanLineAnimation() {
        if (scanLine == null) return;
        scanLine.post(() -> {
            View container = (View) scanLine.getParent();
            if (container == null) return;
            int travel = container.getHeight() - scanLine.getHeight() - dpToPx(28);
            scanLineAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, travel);
            scanLineAnimator.setDuration(1800);
            scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
            scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
            scanLineAnimator.setInterpolator(new LinearInterpolator());
            scanLineAnimator.start();
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Camera
    // ══════════════════════════════════════════════════════════════════════

    private void requestCameraOrStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Barcode analysis (live camera)
    // ══════════════════════════════════════════════════════════════════════

    @ExperimentalGetImage
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (resultSent || frameInProgress) { imageProxy.close(); return; }
        if (imageProxy.getImage() == null)  { imageProxy.close(); return; }

        frameInProgress = true;
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String raw = barcode.getRawValue();
                        if (raw == null) continue;
                        String normalized = raw.trim().toUpperCase();
                        if (STUDENT_ID_PATTERN.matcher(normalized).matches()) {
                            onStudentIdScanned(normalized);
                            return;
                        }
                    }
                })
                .addOnFailureListener(ignored -> { })
                .addOnCompleteListener(task -> {
                    frameInProgress = false;
                    imageProxy.close();
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Result delivery
    // ══════════════════════════════════════════════════════════════════════

    private void onStudentIdScanned(@NonNull String studentId) {
        if (resultSent) return;
        resultSent = true;

        if (isFlashOn && camera != null) {
            camera.getCameraControl().enableTorch(false);
        }

        runOnUiThread(() -> {
            tvScannerStatus.setText(R.string.qr_scanner_status_success);

            View frame = findViewById(R.id.scan_frame_container);
            if (frame != null) {
                frame.animate().scaleX(1.07f).scaleY(1.07f).setDuration(140)
                        .withEndAction(() ->
                                frame.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                        .start();
            }
        });

        scanLine.postDelayed(() -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_STUDENT_ID, studentId);
            setResult(RESULT_OK, data);
            finish();
        }, 600);
    }
}