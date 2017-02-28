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
import com.example.aawee.trackwriter.tools.DistanceCalculator;
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
import java.util.Date;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    // CONSTANTS
    public static final int TRACK_DRAWING_WIDTH = 10;

    public static final double DISTANCE_FLT_THRESHOLD = 2.5;

    public static final double LOW_SPDACC_ACCURACY_THRESHOLD = 30.0;
    public static final double LOW_SPDACC_SPEED_THRESHOLD = 2.0;

    public static final double BEARING_FLT_THRESHOLD = 25.0;
    public static final double BEARING_FLT_SPEED_THRESHOLD = 0.25;

    public static final double BRGDIST_FLT_DIST_THRESHOLD = 50.0;
    public static final double BRGDIST_FLT_SPEED_THRESHOLD = 0.25;

    // FIELDS
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

        // load and filter
        loadAndFilterPoints();

        trackLoaded = true;

    }

    private void loadAndFilterPoints () {
        // main function for loading and filtering points of the track
        GpsPoint oldPt = null;

        int numFiltByDist = 0;
        int numFiltByAccSpd = 0;

        while (cursPt.moveToNext()) {
            // getting fields out of the cursor

            //testTrackID = cursPt.getInt(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.TRACK_ID_NAME));
            double ptLat = cursPt.getDouble(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LATITUDE_NAME));
            double ptLon = cursPt.getDouble(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.LONGITUDE_NAME));
            double ptAcc = cursPt.getDouble(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.ACCURACY_NAME));
            double ptBrg = cursPt.getDouble(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.BEARING_NAME));
            double ptSpd = cursPt.getDouble(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.SPEED_NAME));
            long ptDate = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry.CREATION_TIME_NAME));
            long pointID = cursPt.getLong(cursPt.getColumnIndexOrThrow(TrackContract.GpsPointEntry._ID));

            GpsPoint newPt = new GpsPoint(ptLat, ptLon, ptAcc, ptBrg, ptSpd, ptDate);
            newPt.setDbID(pointID);

            // FILTERS BEGIN
            // filtering by DISTANCE
            // CONSTANTS: 2.5 factor for distance difference, thresholds: 30.0 accuracy && 2.0 speed
            if (oldPt!=null) {
                // actual distance, in meters
                double distAct = DistanceCalculator.distance(oldPt.getLatitude(),oldPt.getLongitude(), ptLat, ptLon, "K")*1000.0;
                // time difference, seconds
                double timeDiff = (ptDate - oldPt.getTimeCreated())/(1000.0); // converting from milliseconds
                // estimated distance where speed is in km/hr
                double distEst = ( (ptSpd + oldPt.getSpeed())*timeDiff/2.0 );

                //Log.d("ANSnot", Double.toString(distAct) + " " + Double.toString(distEst) + " " + Double.toString(timeDiff) );

                if ( (distAct > DISTANCE_FLT_THRESHOLD * distEst) && (distEst != 0) ) {
                    //      Log.d("ANSnot", "Filtered: " + Integer.toString(cursPt.getPosition()) + " "
                    //              + Double.toString(distAct) + " > " + Double.toString(1.2*distEst) );
                    numFiltByDist++; // filtered by distance count
                    newPt.setFiltered();
                }
                else if ( ptAcc > LOW_SPDACC_ACCURACY_THRESHOLD && ptSpd < LOW_SPDACC_SPEED_THRESHOLD  ) {
                    numFiltByAccSpd++; // filtered by speed/accuracy count
                    newPt.setFiltered();
                }


            }

            // inserting new point into points array
            points.add(newPt);
            oldPt = newPt;
        }

        // filtering by BEARING
        // CONSTANTS: 25.0 bearing difference THRESHOLD, 0.25 is minimum speed (if lower, bearing may vary more)
        int numFiltByBrg = 0;
        for (int i=1; i<(points.size()-1); i++) {

            GpsPoint curPt = points.get(i);
            double brgEst = DistanceCalculator.avgBearing(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(),
                    curPt.getLatitude(), curPt.getLongitude(), points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());
            curPt.setBearingAct(brgEst);


            if (DistanceCalculator.angDistance(brgEst, curPt.getBearing()) > BEARING_FLT_THRESHOLD
                    && (curPt.getSpeed() > BEARING_FLT_SPEED_THRESHOLD)) {

                Log.d("ANSnot", " Pt " + Integer.toString(i)
                        + " (" + Double.toString(curPt.getLatitude()) + "," + Double.toString(curPt.getLongitude()) + ") "
                        + " with accuracy " + Double.toString(curPt.getAccuracy()) + ", speed " + Double.toString(curPt.getSpeed())
                        + " and time " + (new Date(curPt.getTimeCreated())).toString()
                        + "; bearing from gps data: " + Double.toString(curPt.getBearing())
                        + ", calculated from actual positions: " + Double.toString(brgEst) + ".");

                curPt.setFiltered();
                numFiltByBrg++;
            }
        }


        // filtering by BEARING + DISTANCE
        // CONSTANT: 50.0 distance difference THRESHOLD, 0.25 minimum speed (if lower, bearing may vary more)
        int numFiltByBrDs = 0;
        oldPt = points.get(1);
        for (int i=2; i<(points.size()-1); i++) {
            if (!points.get(i).isFiltered()) {

                GpsPoint curPt = points.get(i);

                // time difference, seconds
                double timeDiff = (curPt.getTimeCreated() - oldPt.getTimeCreated())/(1000.0);
                // estimated distance where speed is in m/s
                double distEst = ( (curPt.getSpeed() + oldPt.getSpeed())*timeDiff/2.0 );

                // estimated position of the current point according to speed, time, and bearing of the two points
                LatLng newPos = DistanceCalculator.pointByDistAng(oldPt.getLatitude(), oldPt.getLongitude(),
                        (distEst/(6371.0*1000.0)), oldPt.getBearing());

                // distance between estimated and actual positions, meters
                double distEstAct = DistanceCalculator.distance(newPos.latitude,newPos.longitude,
                        curPt.getLatitude(), curPt.getLongitude(), "K")*1000.0;

                //Log.d("ANSnot", Double.toString(distEstAct) + " " + Double.toString(newPos.latitude) + " " +
                //        Double.toString(newPos.longitude) + " " + Double.toString(curPt.getLatitude()) + " "
                //        + Double.toString(curPt.getLongitude()) );

                if (distEstAct > BRGDIST_FLT_DIST_THRESHOLD && (curPt.getSpeed() > BRGDIST_FLT_SPEED_THRESHOLD)) {
                    Log.d("ANSnot", " Pt " + Integer.toString(i)
                            + " (" + Double.toString(curPt.getLatitude()) + "," + Double.toString(curPt.getLongitude()) + ") "
                            + "; distance between estimated and actual: " + Double.toString(distEstAct)
                            + ", lat: " + Double.toString(newPos.latitude)
                            + " lon: " + Double.toString(newPos.longitude)
                            + ".");

                    curPt.setFiltered();
                    numFiltByBrDs++;
                }
                oldPt=curPt;
            }

        }


        // filter by CONNECTIVITY among remaining points:
        // if a group of unfiltered points is surrounded by a big number of filtered points,
        // this group of points gets filtered as well.
        int numFiltByConn = 0;

        // following integers needed to locate the interval of unfiltered points between filtered ones:
        // (clusterFiltBegin)fff...ffff(clusterUnfiltBegin)UUU...UUUU(clusterUnfiltEnd)ffff...fff
        // where "fff" are a filtered point sequences and "UUU" are unfiltered point sequences
        int clusterFiltBegin = -1;
        int clusterUnfiltBegin = -1;
        int clusterUnfiltEnd = -1;

        // array with indicators that an unfiltered point is marked as disconnected
        boolean ptDsc[] = new boolean[points.size()];

        // MAIN CYCLE that marks a cluster of N points as disconnected
        // if more than a half of its 2N neighbouring points are already filtered by previous methods
        // (N arbitrary points)(UUU..UUUU[N unfiltered points])(N arbitrary points)
        for (int i = 0; i < points.size(); i++) {

            boolean isFiltered = points.get(i).isFiltered();

            // value equals to -1 means it is not yet found
            if (clusterFiltBegin == -1) {
                if (isFiltered) clusterFiltBegin = i;
            } else if (clusterUnfiltBegin == -1) {
                if (!isFiltered) clusterUnfiltBegin = i;
            } else if (isFiltered) {
                // end of unfiltered point cluster found
                // next: check if its neighborhood has too many filtered points
                clusterUnfiltEnd = i;

                int numFiltNgh = 0; // number of filtered points in the neighborhood
                int clustLength = clusterUnfiltEnd - clusterUnfiltBegin;

                // loop through the points before the cluster and count filtered points
                int start = clusterUnfiltBegin - clustLength;
                if (start < 0) start = 0;
                for (int j = start; j < clusterUnfiltBegin; j++)
                    if (points.get(j).isFiltered()) numFiltNgh++;

                // loop through the points after the cluster
                int finish = clusterUnfiltEnd + clustLength;
                if (finish > points.size()) finish = points.size();
                for (int j = clusterUnfiltEnd; j < finish; j++)
                    if (points.get(j).isFiltered()) numFiltNgh++;

                if (numFiltNgh > clustLength)
                    for (int j = clusterUnfiltBegin; j < clusterUnfiltEnd; j++) ptDsc[j] = true;

                //Log.d("ANSnot", ": " + Integer.toString(clusterFiltBegin) + " " + Integer.toString(clusterUnfiltBegin) +
                //        " " + Integer.toString(clusterUnfiltEnd));

                // start over for the next interval
                clusterFiltBegin = i;
                clusterUnfiltBegin = -1;

            }

        }
        // final cycle that actually marks points as "filtered"
        for (int i = 0; i < points.size(); i++) if (ptDsc[i]) {
            points.get(i).setFiltered();
            numFiltByConn++;
        }
        // END of filtering by connectivity



        Log.d("ANSnot", "Points filtered by dist: " + Integer.toString(numFiltByDist)
                + " by acc/spd: " + Integer.toString(numFiltByAccSpd) + " by brg: " + Integer.toString(numFiltByBrg) +
                " by brg+dist: " + Integer.toString(numFiltByBrDs) + " by conn: " + Integer.toString(numFiltByConn));

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
            int i=0;
            for (GpsPoint x: points) {
                LatLng latLng = new LatLng(x.getLatitude(), x.getLongitude());
                builder.include(latLng);
                lineOptions.add(latLng);

                float markerCol; // color of marker
                if (x.isFiltered()) markerCol = BitmapDescriptorFactory.HUE_BLUE;
                else markerCol = BitmapDescriptorFactory.HUE_RED;

                //if (!x.isFiltered())
                    googleMap.addMarker(new MarkerOptions().position(latLng).title("Ac: " + Double.toString(x.getAccuracy()) +
                        " Br: " + Double.toString(x.getBearing()) // + " BrAct: " + Double.toString(x.getBearingAct())
                        + " Sp: " + Double.toString(x.getSpeed())  + " i: " + Integer.toString(i)
                    )
                        .icon(BitmapDescriptorFactory.defaultMarker(markerCol)));
                // Double.toString(x.getLatitude()) + ","  + Double.toString(x.getLongitude())
                i++;
            }

            lineOptions.width(TRACK_DRAWING_WIDTH);
            lineOptions.color(Color.RED);

            googleMap.addPolyline(lineOptions);

            // set listener, so that camera changes only when map has undergone layout
            googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    try {
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);

                        googleMap.animateCamera(cu);
                        googleMap.setOnCameraChangeListener(null);
                    }
                    catch (Error e) {
                        Log.e("MAPerr", e.getMessage());
                    }
                }
            });

        }

    }


//    public BitmapDrawable rotateDrawable(float angle)
//    {
//        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(),
//                R.drawable.common_full_open_on_phone);
//        // Create blank bitmap of equal size
//        Bitmap canvasBitmap = arrowBitmap.copy(Bitmap.Config.ARGB_8888, true);
//        canvasBitmap.eraseColor(0x00000000);
//
//        // Create canvas
//        Canvas canvas = new Canvas(canvasBitmap);
//
//        // Create rotation matrix
//        Matrix rotateMatrix = new Matrix();
//        rotateMatrix.setRotate(angle, canvas.getWidth()/2, canvas.getHeight()/2);
//
//        // Draw bitmap onto canvas using matrix
//        canvas.drawBitmap(arrowBitmap, rotateMatrix, null);
//
//        BitmapDrawable bmd = new BitmapDrawable();
//
//        return new BitmapDrawable(getResources(), canvasBitmap);
//    }


    // get points by track ID
    private Cursor getPoints (long track) {
        return mainDB.query(TrackContract.GpsPointEntry.TABLE_NAME, null,
                TrackContract.GpsPointEntry.TRACK_ID_NAME + "=" + Long.toString(track), null, null, null,
                TrackContract.GpsPointEntry.CREATION_TIME_NAME);
    }
}
