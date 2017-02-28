package com.example.aawee.trackwriter.tools;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Aawee on 15/02/2017.
 */

public class DistanceCalculator
{

    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        // distance between two LatLng points
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }

    public static LatLng pointByDistAng (double lat, double lon, double dist, double ang) {
        double angRad = Math.toRadians(ang);
        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);

        double lat2 = Math.asin( Math.sin(latR)*Math.cos(dist) + Math.cos(latR)*Math.sin(dist)*Math.cos(angRad) );
        double lon2 = lonR + Math.atan2(Math.sin(angRad)*Math.sin(dist)*Math.cos(latR), Math.cos(dist)-Math.sin(latR)*Math.sin(lat2));

        lon2 = (lon2+ 3*Math.PI) % (2*Math.PI) - Math.PI;
        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    // This function converts decimal degrees to radians
	private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

	// radians to decimal degrees
	private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    // bearing/direction of the line between two points
    public static double bearing(double lat1, double lon1, double lat2, double lon2){
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }

    // direction at a point : combined of two bearings of adjascent lines
    public static double avgBearing (double lat1, double lon1, double lat2, double lon2, double lat3, double lon3) {

        double brgPrev = bearing(lat1,lon1, lat2,lon2);
        double brgNext = bearing(lat2,lon2, lat3,lon3);
        double brgEst;
        if (Math.abs(brgPrev - brgNext) > 180.0) {
            brgEst = (180 + (brgNext + brgPrev)/2.0)%360;
        }
        else {
            brgEst = (brgNext+brgPrev)/2.0;
        }
        return brgEst;
    }

    // value of an angle between two directions
    public static double angDistance(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;       // This is either the distance or 360 - distance
        double distance = phi > 180 ? 360 - phi : phi;
        return distance;
    }
}