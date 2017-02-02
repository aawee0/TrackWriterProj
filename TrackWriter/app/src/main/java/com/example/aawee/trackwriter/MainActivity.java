package com.example.aawee.trackwriter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.TrackContract;
import com.example.aawee.trackwriter.data.TrackDbHelper;
import com.example.aawee.trackwriter.data.testUtil;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        recBtn = (ImageButton) findViewById(R.id.recButton);
        stopBtn = (ImageButton) findViewById(R.id.stopButton);
        recStarted = false;
        curTrack = new GpsTrack("TestTrack" ,null);
        txtChg = (TextView) findViewById(R.id.textView);

        // list-related
        mTrackList = (RecyclerView) findViewById(R.id.track_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mTrackList.setLayoutManager(layoutManager);
        //mTrackList.setHasFixedSize(true);
        //List<String> strs = new ArrayList<String>(){{add("One String"); add("Two String"); add("Three String"); add("Four String"); }};
        mAdapter = new TrackAdapter(0,null);
        mTrackList.setAdapter(mAdapter);

        // location-related
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (recStarted) curTrack.addPoint( new GpsPoint(location.getLatitude(),location.getLongitude()) );

                txtChg.setText("");
                for (int i=0; i< curTrack.size(); i++ ) {
                    txtChg.append("(" + curTrack.getPoint(i).getLatitude() + " , " + curTrack.getPoint(i).getLongitude() + "), \n");
                }
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

        //configure();

        // database-related
        TrackDbHelper dbHelper = new TrackDbHelper(this);
        mainDB = dbHelper.getWritableDatabase();
        testUtil.insertFakeData(mainDB);

        //take out TRACK

        Cursor cursTr = getTracks();
        long testTrackID=1;
        String trackName = new String();
        while (cursTr.moveToNext()) {
            testTrackID = cursTr.getLong(cursTr.getColumnIndexOrThrow(TrackContract.GpsTrackEntry._ID));
            trackName = cursTr.getString(cursTr.getColumnIndexOrThrow(TrackContract.GpsTrackEntry.TRACK_NAME_NAME));
            mAdapter.addItem(trackName);
            Log.d("Debug", "Item name: " + trackName );
            //txtChg.setText("Track ID: " + Long.toString(testTrackID) + ". \n \n");
        }
        mAdapter.notifyDataSetChanged();


        Cursor cursPt = getPoints(testTrackID);
        long testDat;
        double testLat;
        double testLon;
        long testPtId;
        while (cursPt.moveToNext()) {
            //testTrackID = cursPt.getInt(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.TRACK_ID_NAME));

            testPtId = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry._ID));
            testDat = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.CREATION_TIME_NAME));
            testLat = Double.parseDouble(cursPt.getString(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LATITUDE_NAME)));
            testLon = Double.parseDouble(cursPt.getString(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LONGITUDE_NAME)));

            txtChg.append(
                    "\nPoint: " + Long.toString(testPtId) +
                    //"\nTime: " + Long.toString(testDat) +
                    "\nLatitude: " + Double.toString(testLat) +
                    "\nLongitude: " + Double.toString(testLon) + ". \n");


        }



    }


    void configure(){
        // check if permission is granted, otherwise request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 10);
            return;
        }
        // run the coordinate updates
        locationManager.requestLocationUpdates("gps", 100, 5, locationListener);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                // when permission is granted -- run "configure" again
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    configure();
                }
                break;
            default:
                break;
     }
    }


    public void startRec(View view) {
        recStarted = true;
    }

    public void stopRec(View view) {
        recStarted = false;
    }


    private Cursor getPoints (long track) {
        return mainDB.query(TrackContract.GpsPointEntry.TABLE_NAME, null,
                TrackContract.GpsPointEntry.TRACK_ID_NAME + "=" + Long.toString(track), null, null, null,
                TrackContract.GpsPointEntry._ID);
    }

    private Cursor getTracks () {
        return mainDB.query(TrackContract.GpsTrackEntry.TABLE_NAME, null, null, null, null, null,
                TrackContract.GpsTrackEntry._ID);
    }


}
