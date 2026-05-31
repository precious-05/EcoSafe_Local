package com.example.eco_front;

import java.util.ArrayList;
import java.util.List;

/** Aggregated incident statistics for the risk analysis dashboard. */
public class RiskAnalyticsSnapshot {

    public enum RiskLevel {
        NO_DATA, LOW, MEDIUM, HIGH
    }

    public int totalIncidents;
    public int fireCount;
    public int safeCount;
    public int last7DaysTotal;
    public int last7DaysFire;
    public int last30DaysTotal;
    public int last30DaysFire;
    public double averageConfidenceAll;
    public double averageConfidenceFire;
    public double averageConfidenceSafe;
    public int riskScore;
    public RiskLevel riskLevel = RiskLevel.NO_DATA;
    public String riskSummary;

    /** Day keys yyyy-MM-dd (oldest → newest). */
    public final List<String> last7DayKeys = new ArrayList<>();
    /** Short labels for charts (Mon, Tue, …). */
    public final List<String> last7DayLabels = new ArrayList<>();
    /** Total incidents per day aligned with last7DayLabels. */
    public final List<Integer> last7DayTotals = new ArrayList<>();
    /** Fire incidents per day aligned with last7DayLabels. */
    public final List<Integer> last7DayFires = new ArrayList<>();
    /** Average confidence per day (0 if no data). */
    public final List<Float> last7DayAvgConfidence = new ArrayList<>();

    public int incidentsWithLocation;
    public double mapCenterLat = 30.3753;
    public double mapCenterLon = 69.3451;
    public float mapZoom = 5f;
    public String locationInsight = "";
    public final List<GeoIncidentPoint> geoPoints = new ArrayList<>();
    /** Hotspots sorted by fire count (highest first). */
    public final List<LocationHotspot> hotspots = new ArrayList<>();
    public final List<String> hotspotLabels = new ArrayList<>();
    public final List<Integer> hotspotFireCounts = new ArrayList<>();
    public final List<Integer> hotspotTotalCounts = new ArrayList<>();

    public boolean hasLocationData() {
        return incidentsWithLocation > 0;
    }

    public boolean hasData() {
        return totalIncidents > 0;
    }
}
