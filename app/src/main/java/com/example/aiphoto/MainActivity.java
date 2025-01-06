package com.example.aiphoto;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AiPhoto";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private PreviewView previewView;
    private ImageView capturedImageView;
    private TextView resultTextView;
    private Button captureButton;
    private Button shareButton;

    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        capturedImageView = findViewById(R.id.capturedImageView);
        resultTextView = findViewById(R.id.resultTextView);
        captureButton = findViewById(R.id.captureButton);
        shareButton = findViewById(R.id.shareButton);

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set up output directory
        outputDirectory = getOutputDirectory();

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Set up capture button
        captureButton.setOnClickListener(v -> takePhoto());

        // Set up share button
        shareButton.setOnClickListener(v -> shareResult());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up the image capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Select back camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        // Create time-stamped output file
        File photoFile = new File(outputDirectory, 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg");

        // Set up image capture metadata
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Set up image capture listener
        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Photo capture succeeded: " + savedUri;
                        Log.d(TAG, msg);
                        runOnUiThread(() -> {
                            // Display captured image
                            capturedImageView.setImageURI(savedUri);
                            capturedImageView.setVisibility(View.VISIBLE);
                            previewView.setVisibility(View.GONE);

                            // Analyze image
                            ApiClient apiClient = new ApiClient(MainActivity.this);
                            apiClient.analyzeImage(photoFile, new ApiClient.ApiCallback() {
                                @Override
                                public void onSuccess(String analysis, String description) {
                                    runOnUiThread(() -> {
                                        resultTextView.setText(analysis + "\n\n" + description);
                                        shareButton.setEnabled(true);
                                    });
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    runOnUiThread(() -> {
                                        resultTextView.setText("Error: " + errorMessage);
                                        shareButton.setEnabled(false);
                                    });
                                }
                            });
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private File getOutputDirectory() {
        File mediaDir = getExternalMediaDirs()[0];
        if (mediaDir != null) {
            mediaDir = new File(mediaDir, "AiPhoto");
            mediaDir.mkdirs();
        }
        if (mediaDir == null || !mediaDir.exists()) {
            mediaDir = getFilesDir();
        }
        return mediaDir;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void shareResult() {
        String shareText = resultTextView.getText().toString();
        if (shareText.isEmpty()) {
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
