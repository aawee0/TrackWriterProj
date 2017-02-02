package com.example.aawee.trackwriter;

import java.util.Date;

/**
 * Created by Aawee on 30/01/2017.
 */

public class GpsPoint {
    private double latitude;
    private double longitude;
    private Date timeCreated;

    public GpsPoint(double lat, double lon) {
        latitude = lat;
        longitude = lon;

        // put the time when created
        timeCreated = new Date();
    }

    public double getLatitude () {
        return latitude;
    }

    public double getLongitude () {
        return longitude;
    }
}
