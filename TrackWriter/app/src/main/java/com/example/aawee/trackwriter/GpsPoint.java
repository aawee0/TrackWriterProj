package com.example.aawee.trackwriter;

/**
 * Created by Aawee on 30/01/2017.
 */

public class GpsPoint {
    private double latitude;
    private double longitude;
    private long timeReceived; // time, when the point was received from GPS receiver
    private long dbID; // id in database, if needed

    public GpsPoint(double lat, double lon) {
        latitude = lat;
        longitude = lon;

        // put the time when created
        // timeCreated = new Date();
    }

    public GpsPoint(double lat, double lon, long date) {
        latitude = lat;
        longitude = lon;

        timeReceived = date;
    }

    public double getLatitude () {
        return latitude;
    }

    public double getLongitude () {
        return longitude;
    }

    public long getTimeCreated () { return timeReceived; }

    public void setDbID (long id) {dbID = id; }
}
