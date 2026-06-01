package com.example.eco_front;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.annotations.SerializedName;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import java.util.List;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private PreviewView previewView;
    private ImageView ivImagePreview, ivSettings;
    private CardView cameraCardView, imagePreviewCard;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private boolean isBackCamera = true;

    private View btnCapture, btnGallery, btnSwitchCamera;
    private LinearLayout btnDetect;
    private LinearLayout navHome, navHistory, navEmergency, navRisk;
    private TextView tvResult, tvConfidence, tvStatusHint, tvRecommendationsTitle;
    private LinearLayout resultContainer, recommendationsSection, recommendationsContainer;
    private ProgressBar progressLoading;
    private ImageView ivResultIcon;

    private ApiService apiService;
    private File pendingImageFile;
    private MediaPlayer mediaPlayer;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double detectionLatitude = 0.0;
    private double detectionLongitude = 0.0;
    private boolean predictRequestStarted = false;

    public interface ApiService {
        @Multipart
        @POST("predict")
        Call<PredictionResponse> predictImage(
                @Part MultipartBody.Part image,
                @Part("latitude") Double latitude,
                @Part("longitude") Double longitude,
                @Part("user_id") String userId
        );
    }

    public static class PredictionResponse {
        @SerializedName("is_fire")
        boolean isFire;

        @SerializedName("result")
        String result;

        @SerializedName("confidence_percentage")
        double confidencePercentage;

        @SerializedName("message")
        String message;

        @SerializedName("incident_id")
        Integer incidentId;

        @SerializedName("location_received")
        boolean locationReceived;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupRetrofit();
        checkPermissions();
        setupNavigation();
        checkBackendStatus();
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingImageFile == null) {
            showLiveCamera();
        }
    }

    private void animateEntrance() {
        View welcomeContainer = findViewById(R.id.welcome_container);
        View previewFrame = findViewById(R.id.preview_frame);
        View bottomControls = findViewById(R.id.bottom_controls_container);
        View bottomNavBar = findViewById(R.id.bottom_nav_bar);
        View toolbar = findViewById(R.id.toolbar);

        if (welcomeContainer != null) {
            welcomeContainer.setAlpha(0f);
            welcomeContainer.setTranslationY(40f);
            welcomeContainer.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(100).start();
        }
        if (previewFrame != null) {
            previewFrame.setAlpha(0f);
            previewFrame.setTranslationY(60f);
            previewFrame.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(250).start();
        }
        if (bottomControls != null) {
            bottomControls.setAlpha(0f);
            bottomControls.setTranslationY(50f);
            bottomControls.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(400).start();
        }
        if (bottomNavBar != null) {
            bottomNavBar.setAlpha(0f);
            bottomNavBar.setTranslationY(50f);
            bottomNavBar.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(550).start();
        }
        if (toolbar != null) {
            toolbar.setAlpha(0f);
            toolbar.setTranslationY(-30f);
            toolbar.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(0).start();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        ivImagePreview = findViewById(R.id.iv_image_preview);
        ivSettings = findViewById(R.id.iv_settings);
        cameraCardView = findViewById(R.id.camera_card_view);
        imagePreviewCard = findViewById(R.id.image_preview_card);
        btnCapture = findViewById(R.id.btn_capture);
        btnGallery = findViewById(R.id.btn_gallery);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnDetect = findViewById(R.id.btn_detect);
        navHome = findViewById(R.id.nav_home);
        navHistory = findViewById(R.id.nav_history);
        navEmergency = findViewById(R.id.nav_emergency);
        navRisk = findViewById(R.id.nav_risk);
        tvResult = findViewById(R.id.tv_result);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvStatusHint = findViewById(R.id.tv_status_hint);
        tvRecommendationsTitle = findViewById(R.id.tv_recommendations_title);
        resultContainer = findViewById(R.id.result_container);
        recommendationsSection = findViewById(R.id.recommendations_section);
        recommendationsContainer = findViewById(R.id.recommendations_container);
        progressLoading = findViewById(R.id.progress_loading);
        ivResultIcon = findViewById(R.id.iv_result_icon);
    }

    private void setupRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        String dynamicBaseUrl = AppConfig.getBaseUrl(this);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(dynamicBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    private void checkPermissions() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new com.karumi.dexter.listener.multi.MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(com.karumi.dexter.MultiplePermissionsReport report) {
                        if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            startCamera();
                        } else {
                            Toast.makeText(HomeActivity.this, "Camera permission required", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        if (!hasLocationPermission()) {
                            Toast.makeText(HomeActivity.this,
                                    "Location permission is needed for accurate fire reporting",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<com.karumi.dexter.listener.PermissionRequest> permissions,
                            PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void switchCamera() {
        if (cameraProvider == null) return;

        if (isBackCamera) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            isBackCamera = false;
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            isBackCamera = true;
        }
        bindPreview();
        Toast.makeText(this, isBackCamera ? "Back Camera" : "Front Camera", Toast.LENGTH_SHORT).show();
    }

    private void setupNavigation() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnGallery.setOnClickListener(v -> openGallery());
        btnDetect.setOnClickListener(v -> runDetection());

        if (btnSwitchCamera != null) {
            btnSwitchCamera.setOnClickListener(v -> switchCamera());
        }

        if (ivSettings != null) {
            ivSettings.setOnClickListener(v -> showSettingsDialog());
        }

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "You are on the home screen", Toast.LENGTH_SHORT).show());

        navHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // UPDATED: Emergency button now opens EmergencyActivity
        navEmergency.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        navRisk.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RiskAnalysisActivity.class);
            startActivity(intent);
        });

        ImageView ivNotification = findViewById(R.id.iv_notification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, RiskAnalysisActivity.class)));
        }

        // Apply premium springy touch interactive effects
        setupInteractiveTouchEffects(
                btnCapture, btnGallery, btnSwitchCamera, btnDetect,
                navHome, navHistory, navEmergency, navRisk, ivNotification, ivSettings
        );
    }

    private void showSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Configure Backend IP / URL");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int paddingDp = 20;
        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int) (paddingDp * density);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        TextView label = new TextView(this);
        label.setText("Enter Laptop IP address (e.g. 192.168.106.56:8000) or dynamic ngrok link:");
        label.setTextColor(getColor(R.color.pastel_text_secondary));
        label.setTextSize(13);
        label.setPadding(0, 0, 0, (int) (8 * density));
        container.addView(label);

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(AppConfig.getBaseUrl(this));
        input.setSelection(input.getText().length());
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                AppConfig.setBaseUrl(HomeActivity.this, newUrl);
                setupRetrofit();
                checkBackendStatus();
                Toast.makeText(HomeActivity.this, "✓ Backend URL updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HomeActivity.this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupInteractiveTouchEffects(View... views) {
        for (View view : views) {
            if (view == null) continue;
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            // Elastic press state (like modern CSS active/hover state)
                            v.animate()
                             .scaleX(0.93f)
                             .scaleY(0.93f)
                             .alpha(0.82f)
                             .setDuration(120)
                             .setInterpolator(new android.view.animation.OvershootInterpolator(1.0f))
                             .start();
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            // Playful springy snap back
                            v.animate()
                             .scaleX(1.0f)
                             .scaleY(1.0f)
                             .alpha(1.0f)
                             .setDuration(220)
                             .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f))
                             .start();
                            break;
                    }
                    return false; // Crucial: let click listeners fire standard onClick!
                }
            });
        }
    }

    private void showImagePreview(File imageFile) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            Toast.makeText(this, "Could not load image preview", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingImageFile = imageFile;

        // Smooth crossfade: cameraCardView out, imagePreviewCard in
        cameraCardView.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .withEndAction(() -> {
                    cameraCardView.setVisibility(View.GONE);
                    
                    imagePreviewCard.setAlpha(0f);
                    imagePreviewCard.setScaleX(0.95f);
                    imagePreviewCard.setScaleY(0.95f);
                    imagePreviewCard.setVisibility(View.VISIBLE);
                    ivImagePreview.setImageBitmap(bitmap);
                    
                    imagePreviewCard.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                            .start();
                })
                .start();

        // Bounce-in the Detect Button
        btnDetect.setAlpha(0f);
        btnDetect.setScaleX(0.85f);
        btnDetect.setScaleY(0.85f);
        btnDetect.setVisibility(View.VISIBLE);
        btnDetect.animate()
                 .alpha(1f)
                 .scaleX(1f)
                 .scaleY(1f)
                 .setDuration(350)
                 .setStartDelay(100)
                 .setInterpolator(new android.view.animation.OvershootInterpolator(1.8f))
                 .start();

        // Fade-out the Switch Camera button
        if (btnSwitchCamera != null) {
            btnSwitchCamera.animate()
                           .alpha(0f)
                           .scaleX(0.85f)
                           .scaleY(0.85f)
                           .setDuration(200)
                           .withEndAction(() -> btnSwitchCamera.setVisibility(View.INVISIBLE))
                           .start();
        }
        
        hideDetectionResults();
        tvStatusHint.setText("Tap DETECT FIRE to analyze this image");
    }

    private void showLiveCamera() {
        pendingImageFile = null;

        // Smooth crossfade: imagePreviewCard out, cameraCardView in
        imagePreviewCard.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .withEndAction(() -> {
                    imagePreviewCard.setVisibility(View.GONE);
                    ivImagePreview.setImageDrawable(null);
                    
                    cameraCardView.setAlpha(0f);
                    cameraCardView.setScaleX(0.95f);
                    cameraCardView.setScaleY(0.95f);
                    cameraCardView.setVisibility(View.VISIBLE);
                    
                    cameraCardView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                            .start();
                })
                .start();

        // Fade-out the Detect Button
        btnDetect.animate()
                 .alpha(0f)
                 .scaleX(0.85f)
                 .scaleY(0.85f)
                 .setDuration(200)
                 .withEndAction(() -> btnDetect.setVisibility(View.GONE))
                 .start();

        // Bounce-in the Switch Camera button
        if (btnSwitchCamera != null) {
            btnSwitchCamera.setAlpha(0f);
            btnSwitchCamera.setScaleX(0.85f);
            btnSwitchCamera.setScaleY(0.85f);
            btnSwitchCamera.setVisibility(View.VISIBLE);
            btnSwitchCamera.animate()
                           .alpha(1f)
                           .scaleX(1f)
                           .scaleY(1f)
                           .setDuration(350)
                           .setInterpolator(new android.view.animation.OvershootInterpolator(1.8f))
                           .start();
        }
        
        hideDetectionResults();
    }

    private void hideDetectionResults() {
        resultContainer.setVisibility(View.GONE);
        recommendationsSection.setVisibility(View.GONE);
        recommendationsContainer.removeAllViews();
    }

    private void runDetection() {
        if (pendingImageFile == null || !pendingImageFile.exists()) {
            Toast.makeText(this, "Capture or select an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        predictRequestStarted = false;
        showLoading(true);
        tvStatusHint.setText("Getting location...");
        fetchLocationThenPredict(pendingImageFile);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void fetchLocationThenPredict(File imageFile) {
        if (!hasLocationPermission()) {
            detectionLatitude = 0.0;
            detectionLongitude = 0.0;
            uploadImageToBackend(imageFile, detectionLatitude, detectionLongitude);
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null && isValidCoordinates(location.getLatitude(), location.getLongitude())) {
                        detectionLatitude = location.getLatitude();
                        detectionLongitude = location.getLongitude();
                        uploadImageToBackend(imageFile, detectionLatitude, detectionLongitude);
                    } else {
                        tryLastKnownLocationForPredict(imageFile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentLocation failed", e);
                    tryLastKnownLocationForPredict(imageFile);
                });
    }

    @SuppressLint("MissingPermission")
    private void tryLastKnownLocationForPredict(File imageFile) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isValidCoordinates(location.getLatitude(), location.getLongitude())) {
                detectionLatitude = location.getLatitude();
                detectionLongitude = location.getLongitude();
                uploadImageToBackend(imageFile, detectionLatitude, detectionLongitude);
            } else {
                requestFreshLocationForPredict(imageFile);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get last location", e);
            requestFreshLocationForPredict(imageFile);
        });
    }

    private static boolean isValidCoordinates(double lat, double lon) {
        return (lat != 0.0 || lon != 0.0) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocationForPredict(File imageFile) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && isValidCoordinates(location.getLatitude(), location.getLongitude())) {
                    detectionLatitude = location.getLatitude();
                    detectionLongitude = location.getLongitude();
                }
                if (fusedLocationClient != null && locationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
                uploadImageToBackend(imageFile, detectionLatitude, detectionLongitude);
            }
        };

        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());

            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (fusedLocationClient != null && locationCallback != null) {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
                uploadImageToBackend(imageFile, detectionLatitude, detectionLongitude);
            }, 8000);
        } else {
            uploadImageToBackend(imageFile, 0.0, 0.0);
        }
    }

    private void uploadImageToBackend(File imageFile, double latitude, double longitude) {
        if (predictRequestStarted) {
            return;
        }
        predictRequestStarted = true;

        runOnUiThread(() -> tvStatusHint.setText("Analyzing image..."));

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", imageFile.getName(), requestBody);

        apiService.predictImage(part, latitude, longitude, "android_user").enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    navigateToResultActivity(response.body());
                } else {
                    predictRequestStarted = false;
                    tvStatusHint.setText("Backend error: " + response.code());
                    Toast.makeText(HomeActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                showLoading(false);
                predictRequestStarted = false;
                tvStatusHint.setText("Backend: Connection failed");
                Toast.makeText(HomeActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API error", t);
            }
        });
    }

    private void navigateToResultActivity(PredictionResponse result) {
        Intent intent = new Intent(HomeActivity.this, ResultActivity.class);
        intent.putExtra("image_path", pendingImageFile.getAbsolutePath());
        intent.putExtra("is_fire", result.isFire);
        intent.putExtra("confidence", result.confidencePercentage);
        intent.putExtra("latitude", detectionLatitude);
        intent.putExtra("longitude", detectionLongitude);
        if (result.incidentId != null && result.incidentId > 0) {
            intent.putExtra("incident_id", result.incidentId);
            intent.putExtra("backend_saved", true);
        }
        startActivity(intent);

        stopAlarmSound();
        pendingImageFile = null;
    }

    private void capturePhoto() {
        if (imageCapture == null) return;
        if (pendingImageFile != null) {
            showLiveCamera();
        }

        File photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File photoFile = File.createTempFile("fire_", ".jpg", photoDir);
            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                            showImagePreview(photoFile);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Toast.makeText(HomeActivity.this,
                                    "Failed to capture: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                File tempFile = bitmapToFile(bitmap);
                if (tempFile != null) {
                    showImagePreview(tempFile);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File bitmapToFile(Bitmap bitmap) {
        try {
            File file = File.createTempFile("gallery_", ".jpg", getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                fos.flush();
                return file;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void playAlarmSound() {
        runOnUiThread(() -> {
            try {
                if (mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer.release();
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing previous player", e);
                    }
                    mediaPlayer = null;
                }

                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                if (audioManager != null) {
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                }

                mediaPlayer = MediaPlayer.create(this, R.raw.alarm);

                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.start();
                    Log.d(TAG, "Alarm playing successfully");
                } else {
                    Log.e(TAG, "MediaPlayer.create returned null");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error playing alarm", e);
            }
        });
    }

    private void stopAlarmSound() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping alarm", e);
                }
                mediaPlayer = null;
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressLoading.setVisibility(View.VISIBLE);
            ivResultIcon.setVisibility(View.GONE);
            tvResult.setText("Analyzing...");
            tvResult.setTextColor(getColor(R.color.pastel_orange));
            tvConfidence.setText("");
            resultContainer.setVisibility(View.VISIBLE);
            recommendationsSection.setVisibility(View.GONE);
            btnCapture.setEnabled(false);
            btnGallery.setEnabled(false);
            btnDetect.setEnabled(false);
            if (btnSwitchCamera != null) btnSwitchCamera.setEnabled(false);
        } else {
            progressLoading.setVisibility(View.GONE);
            btnCapture.setEnabled(true);
            btnGallery.setEnabled(true);
            btnDetect.setEnabled(true);
            if (btnSwitchCamera != null) btnSwitchCamera.setEnabled(true);
        }
    }

    private void checkBackendStatus() {
        String dynamicBaseUrl = AppConfig.getBaseUrl(this);
        tvStatusHint.setText("Backend: Connecting...");
        OkHttpClient client = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(dynamicBaseUrl + "health")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> tvStatusHint.setText("Backend: Offline — start server"));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> tvStatusHint.setText("Backend: Ready — capture or upload an image"));
                } else {
                    runOnUiThread(() -> tvStatusHint.setText("Backend: Error " + response.code()));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}