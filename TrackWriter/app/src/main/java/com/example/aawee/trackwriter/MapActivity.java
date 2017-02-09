package com.example.aawee.trackwriter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aawee.trackwriter.data.KmlParser;
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
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int TRACK_DRAWING_WIDTH = 10;

    private TextView mDisplayText;
    private MapFragment mMapFragment;
    private GoogleMap googleMap;

    private ArrayList<GpsPoint> points;
    String trackName;
    boolean trackLoaded;

    // database-related
    private SQLiteDatabase mainDB;
    private Cursor cursPt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        trackLoaded = false;
        //android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        // get the data from intent
        Intent intent = getIntent();
        long trackID = intent.getLongExtra("id_value_long", 1);
        trackName = intent.getStringExtra(Intent.EXTRA_TEXT);

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

            GpsPoint newPt = new GpsPoint(ptLat, ptLon, ptDate);
            newPt.setDbID(pointID);
            points.add(newPt);
        }

        trackLoaded = true;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.track_share) {
            if (trackLoaded) {
                // checking permission for some android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission(this);
                else setShareIntent();

            } else {
                Toast.makeText(getApplicationContext(), "The track hasn't been loaded yet. Please press the button again shortly.",
                        Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    private void setShareIntent () {
        // getting a kml-file from the track
        File fileToSend = KmlParser.exportTrackToKml(new GpsTrack(trackName, points));

        if(fileToSend != null){
            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_SUBJECT, "Sending exported Kml");
            email.putExtra(Intent.EXTRA_TEXT, "Here is the kml-file with the track I recorded! ;) ");
            email.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + fileToSend.getAbsoluteFile()));
            email.setType("text/xml");
            startActivity(Intent.createChooser(email , "Send Text File"));
        }
        else Log.d("SHRnot", "The received kml file is empty.");


    }


    private void checkPermission(android.app.Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {//Can add more as per requirement

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 20);

        } else {
            setShareIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 20:
                // when permission for writing is granted -- run "setShareIntent" function
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setShareIntent();
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (points.size()!=0) {
            // build bounds for screen
            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            // array of point coordinates to hand to route-drawer
            PolylineOptions lineOptions = new PolylineOptions();

            // create markers and extend bounds
            for (GpsPoint x: points) {
                LatLng latLng = new LatLng(x.getLatitude(), x.getLongitude());
                builder.include(latLng);
                lineOptions.add(latLng);
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            lineOptions.width(TRACK_DRAWING_WIDTH);
            lineOptions.color(Color.RED);

            googleMap.addPolyline(lineOptions);

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
                TrackContract.GpsPointEntry.CREATION_TIME_NAME);
    }
}
