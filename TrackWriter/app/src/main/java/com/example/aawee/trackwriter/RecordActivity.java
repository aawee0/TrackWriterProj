package com.example.aawee.trackwriter;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.TrackContract;
import com.example.aawee.trackwriter.data.TrackDbHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * Created by Aawee on 9/02/2017.
 */

public class RecordActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int TRACK_DRAWING_WIDTH = 10;

    private TextView mDisplayText;
    private MapFragment mMapFragment;
    private GoogleMap googleMap;

    private GpsTrack curTrack; // current GPS track
    private LatLng prevLatLng;

    // build bounds for screen
    LatLngBounds.Builder builder;

    // location-related
    private LocationManager locationManager;
    private LocationListener locationListener;

    // database-related
    private SQLiteDatabase mainDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // get the data from intent
        Intent intent = getIntent();
        String trackName = intent.getStringExtra(Intent.EXTRA_TEXT);

        // initialize the text view
        mDisplayText = (TextView) findViewById(R.id.record_text);
        mDisplayText.setText(trackName);

        // initialize screen bounds builder and line drawer
        builder = new LatLngBounds.Builder();
        prevLatLng = null;

        // initialize the map
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.record_map);
        mMapFragment.getMapAsync(this);

        // initialize the database
        TrackDbHelper dbHelper = new TrackDbHelper(this);
        mainDB = dbHelper.getWritableDatabase();


        // location-related
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // add received GPS point to the current track
                curTrack.addPoint(new GpsPoint(location.getLatitude(), location.getLongitude(),
                        location.getAccuracy(), location.getBearing(), location.getSpeed(), location.getTime()));

                Log.d("GPSnot", "Coordinates (" + location.getLatitude() + ", " + location.getLongitude() + ") with accuracy " +
                        location.getAccuracy() + " bearing " + location.getBearing() +
                        " and speed " + location.getSpeed() + " received.");


                // create marker on the map
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.addMarker(new MarkerOptions().position(latLng).title(Double.toString(location.getLatitude()) + ","
                        + Double.toString(location.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // draw line between current and previous point
                if (prevLatLng!=null) {
                    PolylineOptions polylineOptions = new PolylineOptions().add(latLng,prevLatLng);
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(TRACK_DRAWING_WIDTH);
                    googleMap.addPolyline(polylineOptions);
                }

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));


                // update screen bounds
//                builder.include(latLng);
//                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(),100);
//                googleMap.animateCamera(cu);
//                googleMap.setOnCameraChangeListener(null);

                prevLatLng = latLng;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };


        configureGPS();
        curTrack = new GpsTrack(trackName, null); // choosing name is disabled for easier debugging

    }



    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
    }


    void configureGPS() {
        // check if permission is granted, otherwise request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 10);
                return;
            }

        // run the GPS location updates
        locationManager.requestLocationUpdates("gps", 100, 5, locationListener); // add constants!
    }

    public void stopRec(View view) { // action for the stop button
        insertTrack(curTrack);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            // PermissionCheck already called
            // having this so the is no error below
            return;
        }
        locationManager.removeUpdates(locationListener);

        finish();
    }

    @Override
    public void onBackPressed() {
        stopRec(findViewById(R.id.stopButton));
    }

    private void insertTrack (GpsTrack gpsTrack) {
        ContentValues cv = new ContentValues();
        cv.put(TrackContract.GpsTrackEntry.TRACK_NAME_NAME, gpsTrack.getTrackName());
        cv.put(TrackContract.GpsTrackEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));
        long trackID;

        try {
            mainDB.beginTransaction();
            // insert track
            trackID = mainDB.insert(TrackContract.GpsTrackEntry.TABLE_NAME, null, cv);

            // get points from track, put in content values, then to database
            ArrayList<GpsPoint> points = gpsTrack.getPoints();
            cv = new ContentValues();

            if (points.size()>0) {
                for(GpsPoint pt: points) {
                    cv.put(TrackContract.GpsPointEntry.TRACK_ID_NAME, trackID);
                    cv.put(TrackContract.GpsPointEntry.CREATION_TIME_NAME, pt.getTimeCreated());
                    cv.put(TrackContract.GpsPointEntry.LATITUDE_NAME, pt.getLatitude());
                    cv.put(TrackContract.GpsPointEntry.LONGITUDE_NAME, pt.getLongitude());
                    cv.put(TrackContract.GpsPointEntry.ACCURACY_NAME, pt.getAccuracy());
                    cv.put(TrackContract.GpsPointEntry.BEARING_NAME, pt.getBearing());
                    cv.put(TrackContract.GpsPointEntry.SPEED_NAME, pt.getSpeed());
                    mainDB.insert(TrackContract.GpsPointEntry.TABLE_NAME, null, cv);

                    Log.d("GPSnot", "Coords: " + Double.toString(pt.getLongitude()) );
                }
            }
            else Log.d("DBnot", "The track named " + gpsTrack.getTrackName() + " is empty.");

            mainDB.setTransactionSuccessful();
        }
        catch (SQLException e) {
            // error
            Log.e("DBerr", e.getStackTrace().toString() );
        }
        finally {
            mainDB.endTransaction();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                // when permission for GPS is granted -- run "configureGPS" function
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureGPS();
                }
                break;
            default:
                break;
        }
    }
}