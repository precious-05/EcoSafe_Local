package com.example.eco_front;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResultActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ResultActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    // UI Elements
    private ImageView ivResultIcon, ivCapturedImage, ivBackBtn, ivConfidenceIcon;
    private TextView tvResultTitle, tvResultSubtitle, tvConfidencePercentage, tvLocation, tvTimestamp, tvRecommendationsTitle;
    private LinearLayout resultBadgeContainer, recommendationsSection, recommendationsContainer;
    private LinearLayout navSave, navShare, navEmergency, navNewDetection;

    // Data from HomeActivity
    private String imagePath;
    private boolean isFire;
    private double confidence;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String timestamp;
    private String detectionTime;
    private int serverIncidentId = -1;
    private boolean backendAlreadySaved = false;
    private boolean localSaveDone = false;

    // Location and Map
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;
    private boolean isLocationFetched = false;
    private boolean isWaitingForLocation = false;

    // MediaPlayer for alarm
    private MediaPlayer mediaPlayer;
    private boolean isAlertSent = false;
    private SQLiteDatabase localDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getIntentData();
        initViews();
        setupClickListeners();
        displayResult();
        loadCapturedImage();
        setupTimestamp();
        setupRecommendations();
        setupLocalDatabase();

        // Request location permission and refine location if needed
        if (isValidCoordinates(latitude, longitude)) {
            isLocationFetched = true;
            updateLocationUI();
        } else {
            tvLocation.setText("Getting location...");
            checkLocationPermissionAndGetLocation();
        }

        initMediaPlayer();

        if (isFire) {
            playAlarmSound();
        }

        // Auto-save to local database
        saveToLocalDatabase();
        saveToHistory();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        imagePath = intent.getStringExtra("image_path");
        isFire = intent.getBooleanExtra("is_fire", false);
        confidence = intent.getDoubleExtra("confidence", 0.0);
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        serverIncidentId = intent.getIntExtra("incident_id", -1);
        backendAlreadySaved = intent.getBooleanExtra("backend_saved", false);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        timestamp = sdf.format(new Date());

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        detectionTime = sdf2.format(new Date());
    }

    private void initViews() {
        ivResultIcon = findViewById(R.id.iv_result_icon);
        ivCapturedImage = findViewById(R.id.iv_captured_image);
        ivBackBtn = findViewById(R.id.iv_back_btn);
        ivConfidenceIcon = findViewById(R.id.iv_confidence_icon);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultSubtitle = findViewById(R.id.tv_result_subtitle);
        tvConfidencePercentage = findViewById(R.id.tv_confidence_percentage);
        tvLocation = findViewById(R.id.tv_location);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        tvRecommendationsTitle = findViewById(R.id.tv_recommendations_title);
        resultBadgeContainer = findViewById(R.id.result_badge_container);
        recommendationsSection = findViewById(R.id.recommendations_section);
        recommendationsContainer = findViewById(R.id.recommendations_container);

        navSave = findViewById(R.id.nav_save);
        navShare = findViewById(R.id.nav_share);
        navEmergency = findViewById(R.id.nav_emergency);
        navNewDetection = findViewById(R.id.nav_new_detection);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            Log.d(TAG, "✅ Map fragment found, getMapAsync called");
        } else {
            Log.e(TAG, "❌ Map fragment is NULL! Check map_view ID in XML");
        }
    }

    private void setupClickListeners() {
        ivBackBtn.setOnClickListener(v -> finish());

        navSave.setOnClickListener(v -> saveToHistory());
        navShare.setOnClickListener(v -> shareReport());
        navEmergency.setOnClickListener(v -> makeEmergencyCall());
        navNewDetection.setOnClickListener(v -> goToNewDetection());

        // Apply premium bouncy interaction effects
        UiUtils.setupPremiumBouncyCard(ivBackBtn, navSave, navShare, navEmergency, navNewDetection);
    }

    private void displayResult() {
        if (isFire) {
            tvResultTitle.setText("FIRE DETECTED");
            tvResultTitle.setTextColor(getColor(R.color.pastel_orange));
            tvResultSubtitle.setText("Alert authorities immediately");
            ivResultIcon.setImageResource(R.drawable.ic_fire_alert);
            ivConfidenceIcon.setImageResource(R.drawable.ic_fire_alert);
            ivConfidenceIcon.setColorFilter(getColor(R.color.pastel_orange), PorterDuff.Mode.SRC_IN);
        } else {
            tvResultTitle.setText("NO FIRE DETECTED");
            tvResultTitle.setTextColor(getColor(R.color.pastel_green));
            tvResultSubtitle.setText("Forest is safe");
            ivResultIcon.setImageResource(R.drawable.ic_shield_safe);
            ivConfidenceIcon.setImageResource(R.drawable.ic_shield_safe);
            ivConfidenceIcon.setColorFilter(getColor(R.color.pastel_green_light), PorterDuff.Mode.SRC_IN);
        }

        int confidenceInt = (int) Math.round(confidence);
        tvConfidencePercentage.setText(confidenceInt + "%");

        if (confidenceInt >= 70) {
            ivConfidenceIcon.setImageResource(android.R.drawable.star_big_on);
        } else if (confidenceInt >= 40) {
            ivConfidenceIcon.setImageResource(android.R.drawable.star_big_off);
        } else {
            ivConfidenceIcon.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    private void loadCapturedImage() {
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                ivCapturedImage.setImageBitmap(bitmap);
            }
        }
    }

    private void setupTimestamp() {
        tvTimestamp.setText("Detected on: " + timestamp);
    }

    private void setupRecommendations() {
        recommendationsSection.setVisibility(View.VISIBLE);

        if (isFire) {
            tvRecommendationsTitle.setText("Emergency Actions");
            addRecommendation("Evacuate immediately", "Move upwind and away from smoke");
            addRecommendation("Alert authorities", "Call forest department emergency");
            addRecommendation("Avoid smoke exposure", "Cover nose and mouth with damp cloth");
            addRecommendation("Do not fight alone", "Large wildfires need trained crews");
        } else {
            tvRecommendationsTitle.setText("Safety Recommendations");
            addRecommendation("Schedule regular patrols", "Monitor forest during dry conditions");
            addRecommendation("Report early signs", "Report smoke even without visible flames");
            addRecommendation("Maintain fire breaks", "Clear dry leaves near structures");
            addRecommendation("Stay prepared", "Keep emergency contacts ready");
        }
    }

    private void addRecommendation(String title, String body) {
        View card = getLayoutInflater().inflate(R.layout.item_recommendation, recommendationsContainer, false);
        TextView tvTitle = card.findViewById(R.id.tv_rec_title);
        TextView tvBody = card.findViewById(R.id.tv_rec_body);
        tvTitle.setText(title);
        tvBody.setText(body);
        recommendationsContainer.addView(card);
    }

    private void setupLocalDatabase() {
        try {
            File dbFile = new File(getFilesDir(), "incidents.db");
            localDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            localDatabase.execSQL("CREATE TABLE IF NOT EXISTS incidents (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "incident_id INTEGER," +
                    "image_path TEXT," +
                    "confidence REAL," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "timestamp TEXT," +
                    "status TEXT," +
                    "is_fire INTEGER," +
                    "alert_sent INTEGER DEFAULT 0," +
                    "synced INTEGER DEFAULT 0)");

            Log.d(TAG, "Local database setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up database", e);
        }
    }

    private void saveToLocalDatabase() {
        if (localDatabase == null || localSaveDone) return;

        if (isWaitingForLocation && !isLocationFetched) {
            new android.os.Handler().postDelayed(this::saveToLocalDatabase, 1000);
            return;
        }

        try {
            ContentValues values = new ContentValues();
            values.put("image_path", imagePath != null ? imagePath : "");
            values.put("confidence", confidence);
            values.put("latitude", latitude);
            values.put("longitude", longitude);
            values.put("timestamp", detectionTime);
            values.put("status", isFire ? "reported" : "safe");
            values.put("is_fire", isFire ? 1 : 0);
            values.put("alert_sent", isFire ? 1 : 0);
            if (serverIncidentId > 0) {
                values.put("incident_id", serverIncidentId);
            }
            values.put("synced", backendAlreadySaved ? 1 : 0);

            long id = localDatabase.insert("incidents", null, values);
            localSaveDone = true;
            Log.d(TAG, "Saved to local database with ID: " + id +
                    ", incident_id: " + serverIncidentId +
                    ", is_fire: " + isFire +
                    ", lat: " + latitude +
                    ", lon: " + longitude);
        } catch (Exception e) {
            Log.e(TAG, "Error saving to local database", e);
        }
    }

    // ✅ FIXED: Toast only when location fetch actually starts
    private void checkLocationPermissionAndGetLocation() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = false;
        try {
            isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "GPS check error", e);
        }

        if (!isGpsEnabled) {
            tvLocation.setText("GPS is off — enable location or continue without it");
            new AlertDialog.Builder(this)
                    .setTitle("Location Services Required")
                    .setMessage("Please enable GPS/Location services to report accurate fire location.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        tryFetchLocationWithoutGpsCheck();
                    })
                    .setNegativeButton("Continue without location", (dialog, which) -> {
                        tvLocation.setText("Location unavailable");
                        isWaitingForLocation = false;
                    })
                    .show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("EcoSafe-AI needs location access to report fire incidents accurately. This helps forest department reach the exact fire location.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST);
                        })
                        .setNegativeButton("Deny", (dialog, which) -> {
                            tvLocation.setText("Location permission denied");
                            Toast.makeText(this, "Location permission needed for accurate reporting", Toast.LENGTH_LONG).show();
                        })
                        .show();
            } else {
                tvLocation.setText("Requesting location permission...");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST);
            }
        } else {
            // ✅ Only show "Getting location" if not already fetched
            if (!isLocationFetched) {
                Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
            }
            startLocationUpdates();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isValidCoordinates(double lat, double lon) {
        return (lat != 0.0 || lon != 0.0) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    private void applyLocationFix(Location location, String sourceLabel) {
        if (location == null || !isValidCoordinates(location.getLatitude(), location.getLongitude())) {
            return;
        }
        isLocationFetched = true;
        isWaitingForLocation = false;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        updateLocationUI();
        updateMapLocation();
        if (sourceLabel != null) {
            Log.d(TAG, sourceLabel + ": " + latitude + ", " + longitude);
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /** Try getCurrentLocation / last known without requiring GPS provider (network may still work). */
    private void tryFetchLocationWithoutGpsCheck() {
        if (!hasLocationPermission()) {
            return;
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            tvLocation.setText("Location permission denied");
            isWaitingForLocation = false;
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        isWaitingForLocation = true;
        tvLocation.setText("Getting location...");

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (!isLocationFetched) {
                        applyLocationFix(location, "getCurrentLocation");
                    }
                    if (!isLocationFetched) {
                        requestLocationUpdatesFallback();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentLocation failed", e);
                    requestLocationUpdatesFallback();
                });
    }

    private void requestLocationUpdatesFallback() {
        if (!hasLocationPermission() || isLocationFetched) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(10000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(3)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || isLocationFetched) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    applyLocationFix(location, "location updates");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isWaitingForLocation || isLocationFetched) {
                return;
            }
            isWaitingForLocation = false;
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (!isLocationFetched && location != null) {
                    applyLocationFix(location, "last known location");
                    tvLocation.setText(String.format(Locale.getDefault(),
                            "Lat: %.4f, Lon: %.4f (last known)", latitude, longitude));
                } else if (!isLocationFetched) {
                    tvLocation.setText("Could not get location — enable GPS or set mock location on emulator");
                    if (isFire) {
                        Toast.makeText(ResultActivity.this,
                                "Location unavailable — enable GPS in settings",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }, 15000);
    }

    private void updateLocationUI() {
        if (isValidCoordinates(latitude, longitude)) {
            tvLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", latitude, longitude));
        } else {
            tvLocation.setText("Location unavailable");
        }
    }

    private void updateMapLocation() {
        if (googleMap != null && isValidCoordinates(latitude, longitude)) {
            LatLng currentLocation = new LatLng(latitude, longitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title(isFire ? "Fire Location" : "Report Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(isFire ?
                            BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
            Log.d(TAG, "Map updated with location");
        }
    }

    // ✅ FIXED: Removed success toast for cloud save (silent save)
    private void saveToHistory() {
        if (isAlertSent) return;

        // Fire incidents are already saved by /predict on the backend
        if (backendAlreadySaved || (isFire && serverIncidentId > 0)) {
            isAlertSent = true;
            Log.d(TAG, "Skipping cloud save — incident already stored on backend (id=" + serverIncidentId + ")");
            return;
        }

        isAlertSent = true;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("image_path", imagePath != null ? imagePath : "camera_capture");
                jsonBody.put("confidence", confidence);
                jsonBody.put("latitude", latitude);
                jsonBody.put("longitude", longitude);
                jsonBody.put("timestamp", detectionTime);
                jsonBody.put("status", isFire ? "reported" : "safe");
                jsonBody.put("user_id", "android_user");

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonBody.toString()
                );

                String dynamicBaseUrl = AppConfig.getBaseUrl(ResultActivity.this);
                Request request = new Request.Builder()
                        .url(dynamicBaseUrl + "incidents")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> Log.d(TAG, "Incident saved to cloud"));
                    } else {
                        Log.e(TAG, "Failed to save to cloud: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving to cloud", e);
                runOnUiThread(() ->
                        Toast.makeText(ResultActivity.this,
                                "Saved locally only (offline)", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shareReport() {
        String shareText = isFire ?
                String.format(Locale.getDefault(),
                        "EcoSafe-AI Alert: Fire detected!\nConfidence: %.1f%%\nLocation: %.4f, %.4f\nTime: %s\n\nStay safe!",
                        confidence, latitude, longitude, timestamp) :
                String.format(Locale.getDefault(),
                        "EcoSafe-AI Report: No fire detected\nConfidence: %.1f%%\nTime: %s\n\nForest is safe.",
                        confidence, timestamp);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "EcoSafe-AI Fire Detection Report");
        startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }

    private void makeEmergencyCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:1122"));
        startActivity(callIntent);
    }

    private void goToNewDetection() {
        stopAlarmSound();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(1.0f, 1.0f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing media player", e);
        }
    }

    private void playAlarmSound() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "Alarm playing");
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            } catch (Exception e) {
                Log.e(TAG, "Error resetting media player", e);
            }
            Log.d(TAG, "Alarm stopped");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d(TAG, "✅ onMapReady called - Map is ready!");

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (isValidCoordinates(latitude, longitude)) {
            LatLng location = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(isFire ? "Fire Location" : "Report Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(isFire ?
                            BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            Log.d(TAG, "Map updated with location: " + latitude + ", " + longitude);
        } else {
            LatLng defaultLocation = new LatLng(30.3753, 69.3451);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f));
            Log.d(TAG, "No location yet, showing default Pakistan location");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isWaitingForLocation = true;
                tvLocation.setText("Getting location...");
                Toast.makeText(this, "✓ Location permission granted", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            } else {
                tvLocation.setText("Location permission denied");
                Toast.makeText(this, "Location permission needed for accurate reporting", Toast.LENGTH_LONG).show();
            }
        }
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
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (localDatabase != null && localDatabase.isOpen()) {
            localDatabase.close();
        }
    }
}