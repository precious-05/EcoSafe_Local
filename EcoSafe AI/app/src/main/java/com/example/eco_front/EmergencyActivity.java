package com.example.eco_front;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity {

    private ImageView ivBackBtn;
    private LinearLayout cardForest, cardRescue, cardPolice;
    private LinearLayout btnCopyLocation, btnShareLocation;
    private TextView tvLatitude, tvLongitude;

    // Default coordinates (Pakistan - you can change these)
    private double latitude = 30.3753;
    private double longitude = 69.3451;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        initViews();
        setupClickListeners();
        displayLocation();

        // Apply premium bouncy interaction effects
        UiUtils.setupPremiumBouncyCard(cardForest, cardRescue, cardPolice, btnCopyLocation, btnShareLocation);
    }

    private void initViews() {
        ivBackBtn = findViewById(R.id.iv_back_btn);

        cardForest = findViewById(R.id.card_forest);
        cardRescue = findViewById(R.id.card_rescue);
        cardPolice = findViewById(R.id.card_police);

        btnCopyLocation = findViewById(R.id.btn_copy_location);
        btnShareLocation = findViewById(R.id.btn_share_location);

        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
    }

    private void setupClickListeners() {
        ivBackBtn.setOnClickListener(v -> finish());

        // Forest Department - 1084
        cardForest.setOnClickListener(v -> {
            makePhoneCall("1084");
        });

        // Rescue 1122 - 1122
        cardRescue.setOnClickListener(v -> {
            makePhoneCall("1122");
        });

        // Police - 15
        cardPolice.setOnClickListener(v -> {
            makePhoneCall("15");
        });

        // Copy Location
        btnCopyLocation.setOnClickListener(v -> {
            copyLocationToClipboard();
        });

        // Share Location
        btnShareLocation.setOnClickListener(v -> {
            shareLocation();
        });
    }

    private void makePhoneCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    private void displayLocation() {
        String latText = String.format(Locale.getDefault(), "Latitude: %.4f", latitude);
        String lonText = String.format(Locale.getDefault(), "Longitude: %.4f", longitude);

        tvLatitude.setText(latText);
        tvLongitude.setText(lonText);
    }

    private void copyLocationToClipboard() {
        String locationText = String.format(Locale.getDefault(),
                "EcoSafe-AI Emergency Location\nLatitude: %.4f, Longitude: %.4f",
                latitude, longitude);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Emergency Location", locationText);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Location copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareLocation() {
        String shareText = String.format(Locale.getDefault(),
                "EcoSafe-AI Emergency Location\nLatitude: %.4f\nLongitude: %.4f\n\nGoogle Maps: https://maps.google.com/?q=%.4f,%.4f",
                latitude, longitude, latitude, longitude);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Location");
        startActivity(Intent.createChooser(shareIntent, "Share Location"));
    }
}