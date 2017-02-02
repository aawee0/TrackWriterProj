package com.example.aawee.trackwriter;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Aawee on 30/01/2017.
 */

public class GpsTrack {
    private String trackName;
    private ArrayList<GpsPoint> points;
    private Date timeCreated;


    public GpsTrack(String name, ArrayList<GpsPoint> listOfPoints) {


        // inserts a list of points into the track
        if (listOfPoints!=null) points = listOfPoints;
        else points = new ArrayList<GpsPoint>();

        // put the time when created
        timeCreated = new Date();
    }

    public ArrayList<GpsPoint> getPoints () {
        return points;
    }

    public GpsPoint getPoint (int i) {
        return points.get(i);
    }


    public void addPoint (GpsPoint point) {
        points.add(point);
    }

    public int size () {
        return points.size();
    }

}
