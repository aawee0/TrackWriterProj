package com.example.aawee.trackwriter;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.KmlParser;
import com.example.aawee.trackwriter.data.TrackContract;
import com.example.aawee.trackwriter.data.TrackDbHelper;
import com.example.aawee.trackwriter.data.testUtil;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TrackAdapter.ListItemClickListener {

    // interface elements
    private TextView txtChg; // displaying text field
    private EditText txtFld; // input field for new track name

    // list-related
    private TrackAdapter mAdapter;
    private RecyclerView mTrackList;

    // database-related
    private SQLiteDatabase mainDB;
    Cursor cursTr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        txtChg = (TextView) findViewById(R.id.textView);
        txtFld = (EditText) findViewById(R.id.editText);

        // action-bar
        //android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        // list-related
        mTrackList = (RecyclerView) findViewById(R.id.track_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mTrackList.setLayoutManager(layoutManager);

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

    @Override
    public void onResume() {
        super.onResume();

        cursTr = getTracks();
        mAdapter.updateData(cursTr);
    }

    public void startRec(View view) {
        // action for the record button - open another screen with record activity
        Intent recordIntent = new Intent(MainActivity.this, RecordActivity.class);
        recordIntent.putExtra(Intent.EXTRA_TEXT, txtFld.getText().toString());
        startActivity(recordIntent);
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
            Log.e("DBerr", e.getMessage() );
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

            // insert parsed track into list
            insertTrack(newTrack);
            // update the list
            cursTr = getTracks();
            mAdapter.updateData(cursTr);
        }
        return true;
    }


}
