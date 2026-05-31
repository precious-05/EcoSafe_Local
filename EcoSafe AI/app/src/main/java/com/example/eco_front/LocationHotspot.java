package com.example.eco_front;

/** Clustered geographic zone built from nearby incident coordinates. */
public class LocationHotspot {
    public final double latitude;
    public final double longitude;
    public final int totalIncidents;
    public final int fireIncidents;
    public final String label;

    public LocationHotspot(double latitude, double longitude, int totalIncidents,
                           int fireIncidents, String label) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalIncidents = totalIncidents;
        this.fireIncidents = fireIncidents;
        this.label = label;
    }
}
