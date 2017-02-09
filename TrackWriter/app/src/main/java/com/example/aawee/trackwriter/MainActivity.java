package com.example.aawee.trackwriter;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.TrackContract;
import com.example.aawee.trackwriter.data.TrackDbHelper;
import com.example.aawee.trackwriter.data.KmlParser;
import com.example.aawee.trackwriter.data.testUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TrackAdapter.ListItemClickListener {

    // interface elements
    private ImageButton recBtn; // record button
    private ImageButton stopBtn; // stop button
    private boolean recStarted; // indicator that recording started
    private GpsTrack curTrack; // current GPS track
    private TextView txtChg; // displaying text field
    // list-related
    private TrackAdapter mAdapter;
    private RecyclerView mTrackList;

    // location-related
    private LocationManager locationManager;
    private LocationListener locationListener;

    // database-related
    private SQLiteDatabase mainDB;
    Cursor cursTr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        recBtn = (ImageButton) findViewById(R.id.recButton);
        stopBtn = (ImageButton) findViewById(R.id.stopButton);
        txtChg = (TextView) findViewById(R.id.textView);
        recStarted = false;

        // action-bar
        //android.support.v7.app.ActionBar actionBar = getSupportActionBar();


        // list-related
        mTrackList = (RecyclerView) findViewById(R.id.track_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mTrackList.setLayoutManager(layoutManager);

        // location-related
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (recStarted) {
                    curTrack.addPoint(new GpsPoint(location.getLatitude(), location.getLongitude(),
                            location.getTime()));}

                Log.d("GPSnot", "Coordinates (" + location.getLatitude() + ", " + location.getLatitude() + ") received.");


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

        // database-related
        TrackDbHelper dbHelper = new TrackDbHelper(this);
        mainDB = dbHelper.getWritableDatabase();

        // insert some data for testing purposes (delete later)
        testUtil.insertFakeData(mainDB);

        //take out tracks, set an adapter
        cursTr = getTracks();
        mAdapter = new TrackAdapter(this, cursTr, this);
        mTrackList.setAdapter(mAdapter);

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




    public void startRec(View view) {
        // action for the record button
        configureGPS();
        curTrack = new GpsTrack("Test track", null); // choosing name is disabled for easier debugging
        recStarted = true;
        txtChg.setText("Recording. Press STOP to finish");
    }

    public void stopRec(View view) { // action for the stop button
        insertTrack(curTrack);

        cursTr = getTracks();
        mAdapter.updateData(cursTr);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            // PermissionCheck already called
            // having this so the is no error below
            return;
        }
        locationManager.removeUpdates(locationListener);

        recStarted = false;
        txtChg.setText("Finished. Press REC to start again");
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
                    mainDB.insert(TrackContract.GpsPointEntry.TABLE_NAME, null, cv);
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

    private Cursor getTracks () {
        // get all tracks from the database for displaying list
        return mainDB.query(TrackContract.GpsTrackEntry.TABLE_NAME, null, null, null, null, null,
                TrackContract.GpsTrackEntry._ID);
    }


    @Override
    public void onListItemClick(int clickedItemIndex) {

        // get current track information
        cursTr.moveToPosition(clickedItemIndex);
        long testTrackID = cursTr.getLong(cursTr.getColumnIndexOrThrow(TrackContract.GpsTrackEntry._ID));
        String trackName = cursTr.getString(cursTr.getColumnIndexOrThrow(TrackContract.GpsTrackEntry.TRACK_NAME_NAME));

        // create an intent for a screen with map
        Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);
        mapIntent.putExtra(Intent.EXTRA_TEXT, trackName);
        mapIntent.putExtra("id_value_long", testTrackID);

        startActivity(mapIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == R.id.import_button) { // action for import button
            GpsTrack newTrack = KmlParser.parseOneTrackKml(this);
            // received!
            // insert it into list
            insertTrack(newTrack);

            cursTr = getTracks();
            mAdapter.updateData(cursTr);
        }

        return true;
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
