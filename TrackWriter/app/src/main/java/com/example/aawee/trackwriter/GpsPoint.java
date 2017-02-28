package com.example.aawee.trackwriter;

/**
 * Created by Aawee on 30/01/2017.
 */

public class GpsPoint {
    private double latitude;
    private double longitude;
    private double accuracy;
    private double bearing;
    private double speed;
    private long timeReceived; // time, when the point was received from GPS receiver
    private long dbID; // id in database, if needed

    private boolean filtered;
    private double bearingAct;


    public GpsPoint(double lat, double lon, double acc, double brg, double spd, long date) {
        latitude = lat;
        longitude = lon;

        accuracy = acc;
        bearing = brg;
        speed = spd;

        timeReceived = date;
        filtered = false;

        bearingAct = 0.0;
    }

    public double getLatitude () {
        return latitude;
    }

    public double getLongitude () {
        return longitude;
    }

    public double getAccuracy () { return accuracy; }

    public double getBearing () { return bearing; }

    public double getSpeed () { return speed; }

    public boolean isFiltered () {return filtered; }

    public long getTimeCreated () { return timeReceived; }

    public void setDbID (long id) {dbID = id; }

    public void setFiltered () {filtered=true; }

    public void setBearingAct (double brgA) {bearingAct = brgA; }

    public double getBearingAct () {return bearingAct; }
}
