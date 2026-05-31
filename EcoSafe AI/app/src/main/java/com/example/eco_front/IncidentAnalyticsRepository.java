package com.example.eco_front;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads local incident history and builds analytics for the risk dashboard.
 */
public final class IncidentAnalyticsRepository {

    private static final String[] TIMESTAMP_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "dd MMM yyyy, hh:mm a"
    };

    private IncidentAnalyticsRepository() {
    }

    public static RiskAnalyticsSnapshot load(Context context) {
        RiskAnalyticsSnapshot snapshot = new RiskAnalyticsSnapshot();
        prepareLast7DayBuckets(snapshot);

        File dbFile = new File(context.getFilesDir(), "incidents.db");
        if (!dbFile.exists()) {
            finalizeRisk(snapshot);
            return snapshot;
        }

        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            cursor = database.rawQuery(
                    "SELECT confidence, timestamp, is_fire, status, latitude, longitude "
                            + "FROM incidents ORDER BY timestamp ASC",
                    null);

            double confidenceSum = 0;
            double latSum = 0;
            double lonSum = 0;
            Map<String, HotspotBucket> hotspotBuckets = new LinkedHashMap<>();
            double fireConfidenceSum = 0;
            double safeConfidenceSum = 0;
            int fireConfidenceCount = 0;
            int safeConfidenceCount = 0;

            long now = System.currentTimeMillis();
            long sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000;
            long thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000;

            Map<String, DayBucket> dayBuckets = buildDayBucketMap(snapshot);

            while (cursor.moveToNext()) {
                double confidence = cursor.getDouble(0);
                String timestamp = cursor.getString(1);
                int isFireColumn = cursor.getInt(2);
                String status = cursor.getString(3);
                boolean isFire = isFireColumn == 1 || "reported".equalsIgnoreCase(status);
                double latitude = cursor.getDouble(4);
                double longitude = cursor.getDouble(5);

                if (isValidCoordinates(latitude, longitude)) {
                    snapshot.incidentsWithLocation++;
                    latSum += latitude;
                    lonSum += longitude;
                    snapshot.geoPoints.add(new GeoIncidentPoint(latitude, longitude, isFire, confidence));
                    accumulateHotspot(hotspotBuckets, latitude, longitude, isFire);
                }

                snapshot.totalIncidents++;
                confidenceSum += confidence;
                if (isFire) {
                    snapshot.fireCount++;
                    fireConfidenceSum += confidence;
                    fireConfidenceCount++;
                } else {
                    snapshot.safeCount++;
                    safeConfidenceSum += confidence;
                    safeConfidenceCount++;
                }

                Date incidentDate = parseTimestamp(timestamp);
                long incidentMs = incidentDate != null ? incidentDate.getTime() : 0L;

                if (incidentMs >= sevenDaysAgo) {
                    snapshot.last7DaysTotal++;
                    if (isFire) {
                        snapshot.last7DaysFire++;
                    }
                }
                if (incidentMs >= thirtyDaysAgo) {
                    snapshot.last30DaysTotal++;
                    if (isFire) {
                        snapshot.last30DaysFire++;
                    }
                }

                if (incidentDate != null) {
                    String dayKey = dayKey(incidentDate);
                    DayBucket bucket = dayBuckets.get(dayKey);
                    if (bucket != null) {
                        bucket.total++;
                        if (isFire) {
                            bucket.fires++;
                        }
                        bucket.confidenceSum += confidence;
                        bucket.confidenceCount++;
                    }
                }
            }

            if (snapshot.totalIncidents > 0) {
                snapshot.averageConfidenceAll = confidenceSum / snapshot.totalIncidents;
            }
            if (fireConfidenceCount > 0) {
                snapshot.averageConfidenceFire = fireConfidenceSum / fireConfidenceCount;
            }
            if (safeConfidenceCount > 0) {
                snapshot.averageConfidenceSafe = safeConfidenceSum / safeConfidenceCount;
            }

            for (int i = 0; i < snapshot.last7DayKeys.size(); i++) {
                String key = snapshot.last7DayKeys.get(i);
                DayBucket bucket = dayBuckets.get(key);
                if (bucket == null) {
                    snapshot.last7DayTotals.add(0);
                    snapshot.last7DayFires.add(0);
                    snapshot.last7DayAvgConfidence.add(0f);
                } else {
                    snapshot.last7DayTotals.add(bucket.total);
                    snapshot.last7DayFires.add(bucket.fires);
                    float avg = bucket.confidenceCount > 0
                            ? (float) (bucket.confidenceSum / bucket.confidenceCount)
                            : 0f;
                    snapshot.last7DayAvgConfidence.add(avg);
                }
            }

            buildLocationAnalytics(snapshot, hotspotBuckets, latSum, lonSum);
        } catch (Exception ignored) {
            // Return partial snapshot
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }

        finalizeRisk(snapshot);
        return snapshot;
    }

    private static boolean isValidCoordinates(double lat, double lon) {
        return (lat != 0.0 || lon != 0.0) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    private static void accumulateHotspot(Map<String, HotspotBucket> buckets,
                                          double latitude, double longitude, boolean isFire) {
        String key = gridKey(latitude, longitude);
        HotspotBucket bucket = buckets.get(key);
        if (bucket == null) {
            bucket = new HotspotBucket(gridLat(latitude), gridLon(longitude));
            buckets.put(key, bucket);
        }
        bucket.total++;
        if (isFire) {
            bucket.fires++;
        }
    }

    private static double gridLat(double lat) {
        return Math.round(lat * 100.0) / 100.0;
    }

    private static double gridLon(double lon) {
        return Math.round(lon * 100.0) / 100.0;
    }

    private static String gridKey(double lat, double lon) {
        return gridLat(lat) + "," + gridLon(lon);
    }

    private static void buildLocationAnalytics(RiskAnalyticsSnapshot snapshot,
                                               Map<String, HotspotBucket> buckets,
                                               double latSum, double lonSum) {
        if (snapshot.incidentsWithLocation > 0) {
            snapshot.mapCenterLat = latSum / snapshot.incidentsWithLocation;
            snapshot.mapCenterLon = lonSum / snapshot.incidentsWithLocation;
            snapshot.mapZoom = snapshot.incidentsWithLocation == 1 ? 14f : 11f;
        }

        List<HotspotBucket> sorted = new ArrayList<>(buckets.values());
        Collections.sort(sorted, (a, b) -> {
            if (b.fires != a.fires) {
                return Integer.compare(b.fires, a.fires);
            }
            return Integer.compare(b.total, a.total);
        });

        int zoneIndex = 1;
        for (HotspotBucket bucket : sorted) {
            String label = String.format(Locale.getDefault(), "Zone %d · %.2f, %.2f",
                    zoneIndex, bucket.latitude, bucket.longitude);
            snapshot.hotspots.add(new LocationHotspot(
                    bucket.latitude, bucket.longitude, bucket.total, bucket.fires, label));
            zoneIndex++;
        }

        int chartLimit = Math.min(5, sorted.size());
        for (int i = 0; i < chartLimit; i++) {
            HotspotBucket bucket = sorted.get(i);
            snapshot.hotspotLabels.add(String.format(Locale.getDefault(), "Z%d", i + 1));
            snapshot.hotspotFireCounts.add(bucket.fires);
            snapshot.hotspotTotalCounts.add(bucket.total);
        }

        if (snapshot.incidentsWithLocation == 0) {
            snapshot.locationInsight = "No GPS-tagged incidents yet. Enable location when detecting fires.";
        } else if (!sorted.isEmpty() && sorted.get(0).fires > 0) {
            HotspotBucket top = sorted.get(0);
            snapshot.locationInsight = String.format(Locale.getDefault(),
                    "%d fire alert(s) clustered near %.2f°, %.2f° — highest geographic risk zone.",
                    top.fires, top.latitude, top.longitude);
        } else {
            snapshot.locationInsight = String.format(Locale.getDefault(),
                    "%d geo-tagged scan(s) across %d zone(s). No fire clusters detected.",
                    snapshot.incidentsWithLocation, sorted.size());
        }
    }

    private static void finalizeRisk(RiskAnalyticsSnapshot snapshot) {
        if (!snapshot.hasData()) {
            snapshot.riskScore = 0;
            snapshot.riskLevel = RiskAnalyticsSnapshot.RiskLevel.NO_DATA;
            snapshot.riskSummary = "No incident data yet. Run fire detections to build your risk profile.";
            return;
        }

        double fireRate7d = snapshot.last7DaysTotal > 0
                ? (double) snapshot.last7DaysFire / snapshot.last7DaysTotal
                : (double) snapshot.fireCount / snapshot.totalIncidents;

        double fireRate30d = snapshot.last30DaysTotal > 0
                ? (double) snapshot.last30DaysFire / snapshot.last30DaysTotal
                : fireRate7d;

        double confidenceFactor = snapshot.averageConfidenceFire > 0
                ? snapshot.averageConfidenceFire / 100.0
                : 0.5;

        int score = (int) Math.round(
                (fireRate7d * 55.0) + (fireRate30d * 25.0) + (confidenceFactor * 20.0));
        snapshot.riskScore = Math.min(100, Math.max(0, score));

        String locationNote = "";
        if (snapshot.hasLocationData() && !snapshot.hotspots.isEmpty()
                && snapshot.hotspots.get(0).fireIncidents > 0) {
            LocationHotspot top = snapshot.hotspots.get(0);
            locationNote = String.format(Locale.getDefault(),
                    " Hotspot: %.2f°, %.2f°.", top.latitude, top.longitude);
        }

        if (snapshot.riskScore >= 65 || snapshot.last7DaysFire >= 3) {
            snapshot.riskLevel = RiskAnalyticsSnapshot.RiskLevel.HIGH;
            snapshot.riskSummary = "Elevated wildfire exposure detected. Increase patrols and stay alert."
                    + locationNote;
        } else if (snapshot.riskScore >= 35 || snapshot.last7DaysFire >= 1) {
            snapshot.riskLevel = RiskAnalyticsSnapshot.RiskLevel.MEDIUM;
            snapshot.riskSummary = "Moderate risk based on recent detections. Monitor dry zones closely."
                    + locationNote;
        } else {
            snapshot.riskLevel = RiskAnalyticsSnapshot.RiskLevel.LOW;
            snapshot.riskSummary = "Forest conditions look stable. Continue routine monitoring."
                    + locationNote;
        }
    }

    private static void prepareLast7DayBuckets(RiskAnalyticsSnapshot snapshot) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat labelFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            snapshot.last7DayKeys.add(keyFormat.format(calendar.getTime()));
            snapshot.last7DayLabels.add(labelFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private static Map<String, DayBucket> buildDayBucketMap(RiskAnalyticsSnapshot snapshot) {
        Map<String, DayBucket> map = new LinkedHashMap<>();
        for (String key : snapshot.last7DayKeys) {
            map.put(key, new DayBucket());
        }
        return map;
    }

    private static String dayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private static Date parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        for (String format : TIMESTAMP_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                return sdf.parse(timestamp);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static final class DayBucket {
        int total;
        int fires;
        double confidenceSum;
        int confidenceCount;
    }

    private static final class HotspotBucket {
        final double latitude;
        final double longitude;
        int total;
        int fires;

        HotspotBucket(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
