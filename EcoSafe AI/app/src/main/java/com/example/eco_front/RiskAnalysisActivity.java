package com.example.eco_front;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RiskAnalysisActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PieChart chartRiskGauge;
    private PieChart chartFireSafe;
    private BarChart chartWeekly;
    private LineChart chartConfidence;
    private HorizontalBarChart chartLocationZones;

    private TextView tvRiskLevel;
    private TextView tvRiskSummary;
    private TextView tvDataHint;
    private TextView tvLocationInsight;
    private TextView tvStatGeo;
    private TextView tvStatTotal;
    private TextView tvStatFire;
    private TextView tvStatSafe;
    private TextView tvStat7d;
    private LinearLayout hotspotsContainer;

    private GoogleMap googleMap;
    private RiskAnalyticsSnapshot pendingSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_analysis);

        bindViews();
        setupCharts();
        setupMap();
        loadAnalytics();
    }

    private void bindViews() {
        ImageView ivBack = findViewById(R.id.iv_back_btn);
        ImageView ivGuide = findViewById(R.id.iv_forest_guide);

        chartRiskGauge = findViewById(R.id.chart_risk_gauge);
        chartFireSafe = findViewById(R.id.chart_fire_safe);
        chartWeekly = findViewById(R.id.chart_weekly);
        chartConfidence = findViewById(R.id.chart_confidence);
        chartLocationZones = findViewById(R.id.chart_location_zones);

        tvRiskLevel = findViewById(R.id.tv_risk_level);
        tvRiskSummary = findViewById(R.id.tv_risk_summary);
        tvDataHint = findViewById(R.id.tv_data_hint);
        tvLocationInsight = findViewById(R.id.tv_location_insight);
        tvStatGeo = findViewById(R.id.tv_stat_geo);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatFire = findViewById(R.id.tv_stat_fire);
        tvStatSafe = findViewById(R.id.tv_stat_safe);
        tvStat7d = findViewById(R.id.tv_stat_7d);
        hotspotsContainer = findViewById(R.id.hotspots_container);

        ivBack.setOnClickListener(v -> finish());
        ivGuide.setOnClickListener(v ->
                startActivity(new Intent(this, ForestGuideActivity.class)));

        // Apply premium bouncy interaction effects
        UiUtils.setupPremiumBouncyCard(ivBack, ivGuide);
    }

    private void setupCharts() {
        RiskChartHelper.stylePieChart(chartRiskGauge);
        RiskChartHelper.stylePieChart(chartFireSafe);
        RiskChartHelper.styleBarChart(chartWeekly);
        RiskChartHelper.styleLineChart(chartConfidence);
        RiskChartHelper.styleHorizontalBarChart(chartLocationZones);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_risk_hotspots);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void loadAnalytics() {
        executor.execute(() -> {
            RiskAnalyticsSnapshot snapshot = IncidentAnalyticsRepository.load(this);
            runOnUiThread(() -> bindSnapshot(snapshot));
        });
    }

    private void bindSnapshot(RiskAnalyticsSnapshot data) {
        pendingSnapshot = data;

        tvStatTotal.setText(String.valueOf(data.totalIncidents));
        tvStatFire.setText(String.valueOf(data.fireCount));
        tvStatSafe.setText(String.valueOf(data.safeCount));
        tvStat7d.setText(String.valueOf(data.last7DaysFire));

        tvRiskSummary.setText(data.riskSummary);
        tvLocationInsight.setText(data.locationInsight);
        tvStatGeo.setText(getString(R.string.stat_geo_tagged, data.incidentsWithLocation));

        if (data.hasData()) {
            tvDataHint.setText(String.format(Locale.getDefault(),
                    "Based on %d stored incident(s) · Avg confidence %.0f%%",
                    data.totalIncidents, data.averageConfidenceAll));
        } else {
            tvDataHint.setText("No incidents in local database yet");
        }

        applyRiskBadge(data);
        bindHotspotList(data);
        RiskChartHelper.bindRiskGaugePie(chartRiskGauge, data.riskScore);
        RiskChartHelper.bindFireSafePie(chartFireSafe, data);
        RiskChartHelper.bindWeeklyBarChart(chartWeekly, data);
        RiskChartHelper.bindConfidenceLine(chartConfidence, data);
        RiskChartHelper.bindLocationZonesBar(chartLocationZones, data);
        bindMapIfReady();
    }

    private void bindHotspotList(RiskAnalyticsSnapshot data) {
        hotspotsContainer.removeAllViews();
        if (!data.hasLocationData() || data.hotspots.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No location clusters yet — allow GPS when scanning.");
            empty.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.pastel_text_secondary));
            empty.setTextSize(12f);
            hotspotsContainer.addView(empty);
            return;
        }

        int limit = Math.min(4, data.hotspots.size());
        for (int i = 0; i < limit; i++) {
            LocationHotspot hotspot = data.hotspots.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_risk_hotspot, hotspotsContainer, false);
            View dot = row.findViewById(R.id.view_hotspot_dot);
            TextView label = row.findViewById(R.id.tv_hotspot_label);
            TextView counts = row.findViewById(R.id.tv_hotspot_counts);

            label.setText(hotspot.label);
            counts.setText(String.format(Locale.getDefault(), "%d fire · %d total",
                    hotspot.fireIncidents, hotspot.totalIncidents));

            dot.setBackgroundResource(hotspot.fireIncidents > 0
                    ? R.drawable.risk_badge_high
                    : R.drawable.risk_badge_low);
            hotspotsContainer.addView(row);
        }
    }

    private void bindMapIfReady() {
        if (googleMap == null || pendingSnapshot == null) {
            return;
        }
        googleMap.clear();
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        RiskAnalyticsSnapshot data = pendingSnapshot;
        if (!data.hasLocationData()) {
            LatLng fallback = new LatLng(30.3753, 69.3451);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 5f));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasBounds = false;

        for (GeoIncidentPoint point : data.geoPoints) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            boundsBuilder.include(latLng);
            hasBounds = true;

            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(point.isFire ? "Fire detection" : "Safe scan")
                    .snippet(String.format(Locale.getDefault(), "Confidence %.0f%%", point.confidence))
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            point.isFire ? BitmapDescriptorFactory.HUE_ORANGE
                                    : BitmapDescriptorFactory.HUE_GREEN)));
        }

        for (LocationHotspot hotspot : data.hotspots) {
            if (hotspot.fireIncidents <= 0) {
                continue;
            }
            LatLng center = new LatLng(hotspot.latitude, hotspot.longitude);
            int radius = 400 + (hotspot.fireIncidents * 150);
            googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeColor(Color.parseColor("#CCFF6B35"))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor("#44FF6B35")));
        }

        if (hasBounds && data.geoPoints.size() > 1) {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(), 80));
            } catch (Exception e) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(data.mapCenterLat, data.mapCenterLon), data.mapZoom));
            }
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(data.mapCenterLat, data.mapCenterLon), data.mapZoom));
        }
    }

    private void applyRiskBadge(RiskAnalyticsSnapshot data) {
        int backgroundRes;
        String label;
        switch (data.riskLevel) {
            case HIGH:
                backgroundRes = R.drawable.risk_badge_high;
                label = "HIGH RISK";
                break;
            case MEDIUM:
                backgroundRes = R.drawable.risk_badge_medium;
                label = "MEDIUM RISK";
                break;
            case NO_DATA:
                backgroundRes = R.drawable.glass_card_chart;
                label = "NO DATA";
                break;
            case LOW:
            default:
                backgroundRes = R.drawable.risk_badge_low;
                label = "LOW RISK";
                break;
        }
        tvRiskLevel.setText(label);
        tvRiskLevel.setBackgroundResource(backgroundRes);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        bindMapIfReady();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
