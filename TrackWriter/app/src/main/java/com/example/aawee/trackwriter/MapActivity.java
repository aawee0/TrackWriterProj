package com.example.aawee.trackwriter;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.TrackContract;
import com.example.aawee.trackwriter.data.TrackDbHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private TextView mDisplayText;
    private MapFragment mMapFragment;
    private GoogleMap googleMap;

    private ArrayList<GpsPoint> points;

    // database-related
    private SQLiteDatabase mainDB;
    private Cursor cursPt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        // get the data from intent
        Intent intent = getIntent();
        long trackID = intent.getLongExtra("id_value_long", 1);
        String trackName = intent.getStringExtra(Intent.EXTRA_TEXT);

        // initialize the text view
        mDisplayText = (TextView) findViewById(R.id.map_text);
        mDisplayText.setText(trackName + " " + Long.toString(trackID));

        // initialize the map
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        // initialize the database
        TrackDbHelper dbHelper = new TrackDbHelper(this);
        mainDB = dbHelper.getWritableDatabase();

        // fill the array with points information
        points = new ArrayList<GpsPoint>();
        cursPt = getPoints(trackID);

        while (cursPt.moveToNext()) {
            //testTrackID = cursPt.getInt(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.TRACK_ID_NAME));
            double ptLat = Double.parseDouble(cursPt.getString(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LATITUDE_NAME)));
            double ptLon = Double.parseDouble(cursPt.getString(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LONGITUDE_NAME)));
            long ptDate = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.CREATION_TIME_NAME));
            long pointID = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry._ID));

            GpsPoint newPt = new GpsPoint(ptLat, ptLon, ptDate, pointID);
            points.add(newPt);
        }



    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (points.size()!=0) {
            // build bounds for screen
            final LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // create markers and extend bounds
            for (GpsPoint x: points) {
                LatLng latLng = new LatLng(x.getLatitude(), x.getLongitude());
                builder.include(latLng);
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            // set listener, so that camera changes only when map has undergone layout
            googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(),100);

                    googleMap.animateCamera(cu);
                    googleMap.setOnCameraChangeListener(null);
                }
            });



        }




    }

    // get points by track ID
    private Cursor getPoints (long track) {
        return mainDB.query(TrackContract.GpsPointEntry.TABLE_NAME, null,
                TrackContract.GpsPointEntry.TRACK_ID_NAME + "=" + Long.toString(track), null, null, null,
                TrackContract.GpsPointEntry._ID);
    }
}
