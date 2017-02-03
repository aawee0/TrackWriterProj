package com.example.aawee.trackwriter;

/**
 * Created by Aawee on 30/01/2017.
 */

public class GpsPoint {
    private double latitude;
    private double longitude;
    private long timeCreated; // time, when the point was received from GPS receiver
    private long dbID; // id in database, if needed

    public GpsPoint(double lat, double lon) {
        latitude = lat;
        longitude = lon;

        // put the time when created
        // timeCreated = new Date();
    }

    public GpsPoint(double lat, double lon, long date, long id) {
        latitude = lat;
        longitude = lon;

        timeCreated = date;
        dbID = id;
    }

    public double getLatitude () {
        return latitude;
    }

    public double getLongitude () {
        return longitude;
    }
}
