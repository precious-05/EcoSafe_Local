package com.example.eco_front;

/** Single incident with GPS coordinates for map plotting. */
public class GeoIncidentPoint {
    public final double latitude;
    public final double longitude;
    public final boolean isFire;
    public final double confidence;

    public GeoIncidentPoint(double latitude, double longitude, boolean isFire, double confidence) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFire = isFire;
        this.confidence = confidence;
    }
}
