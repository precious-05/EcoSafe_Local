package com.example.eco_front;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class IncidentDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView ivBackBtn, ivFullImage;
    private LinearLayout statusBadge;
    private TextView tvStatus, tvConfidence, tvMessage, tvCoordinates, tvDatetime, tvIncidentId, tvAlertStatus;

    private GoogleMap googleMap;
    private SQLiteDatabase database;

    private double latitude;
    private double longitude;
    private boolean isFire;
    private boolean alertSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);

        initViews();
        setupDatabase();
        loadIncidentData();
        setupListeners();
        setupMap();
    }

    private void initViews() {
        ivBackBtn = findViewById(R.id.iv_back_btn);
        ivFullImage = findViewById(R.id.iv_full_image);
        statusBadge = findViewById(R.id.status_badge);
        tvStatus = findViewById(R.id.tv_status);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvMessage = findViewById(R.id.tv_message);
        tvCoordinates = findViewById(R.id.tv_coordinates);
        tvDatetime = findViewById(R.id.tv_datetime);
        tvIncidentId = findViewById(R.id.tv_incident_id);
        tvAlertStatus = findViewById(R.id.tv_alert_status);
    }

    private void setupDatabase() {
        try {
            File dbFile = new File(getFilesDir(), "incidents.db");
            database = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadIncidentData() {
        int incidentId = getIntent().getIntExtra("incident_id", -1);

        if (incidentId == -1) {
            Toast.makeText(this, "Incident not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Cursor cursor = database.rawQuery("SELECT * FROM incidents WHERE id = ?",
                new String[]{String.valueOf(incidentId)});

        if (cursor.moveToFirst()) {
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            double confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence"));
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            isFire = cursor.getInt(cursor.getColumnIndexOrThrow("is_fire")) == 1;
            alertSent = cursor.getInt(cursor.getColumnIndexOrThrow("alert_sent")) == 1;
            int serverId = cursor.getInt(cursor.getColumnIndexOrThrow("incident_id"));

            // Set data to views
            tvConfidence.setText(String.format(Locale.getDefault(), "%.1f%%", confidence));
            tvCoordinates.setText(String.format(Locale.getDefault(),
                    "Lat: %.6f, Lon: %.6f", latitude, longitude));

            try {
                SimpleDateFormat[] formats = {
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                };
                long time = -1;
                for (SimpleDateFormat inputFormat : formats) {
                    try {
                        time = inputFormat.parse(timestamp).getTime();
                        break;
                    } catch (Exception ignored) {
                    }
                }
                if (time > 0) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    tvDatetime.setText(outputFormat.format(time));
                } else {
                    tvDatetime.setText(timestamp);
                }
            } catch (Exception e) {
                tvDatetime.setText(timestamp);
            }

            tvIncidentId.setText("#" + (serverId > 0 ? serverId : incidentId));

            if (isFire) {
                statusBadge.setBackgroundResource(R.drawable.status_badge_fire);
                tvStatus.setText("Fire Detected");
                tvMessage.setText("Fire detected in this area. Alert has been sent to forest department.");

                if (alertSent) {
                    tvAlertStatus.setText("Sent to Authorities");
                    tvAlertStatus.setTextColor(getColor(android.R.color.holo_green_light));
                } else {
                    tvAlertStatus.setText("Pending");
                    tvAlertStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                }
            } else {
                statusBadge.setBackgroundResource(R.drawable.status_badge_safe);
                tvStatus.setText("No Fire");
                tvMessage.setText("No fire detected in this area. Forest is safe.");
                tvAlertStatus.setText("No Alert Sent");
                tvAlertStatus.setTextColor(getColor(android.R.color.darker_gray));
            }

            // Load image
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        ivFullImage.setImageBitmap(bitmap);
                    }
                }
            }
        }
        cursor.close();
    }

    private void setupListeners() {
        ivBackBtn.setOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (latitude != 0.0 && longitude != 0.0) {
            LatLng location = new LatLng(latitude, longitude);
            this.googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(isFire ? "Fire Location" : "Report Location"));
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        } else {
            LatLng defaultLocation = new LatLng(30.3753, 69.3451);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}