package com.example.aawee.trackwriter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
        mAdapter = new TrackAdapter(cursTr, this);
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
                    cv.put(TrackContract.GpsPointEntry.ACCURACY_NAME, pt.getAccuracy());
                    cv.put(TrackContract.GpsPointEntry.BEARING_NAME, pt.getBearing());
                    cv.put(TrackContract.GpsPointEntry.SPEED_NAME, pt.getSpeed());
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

            Intent intent;
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            if (intent.resolveActivity(this.getPackageManager()) != null) {
                startActivityForResult(
                        Intent.createChooser(intent, "Choose a kml-file"), 20);
            }





//            GpsTrack newTrack = KmlParser.parseOneTrackKml(this);
//
//            // insert parsed track into list
//            insertTrack(newTrack);
//            // update the list
//            cursTr = getTracks();
//            mAdapter.updateData(cursTr);



        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 20) {
                Uri uri = data.getData();
                Log.d("CHSnot", uri.toString());
                GpsTrack newTrack = KmlParser.parseOneTrackKml(this, uri);

                // insert parsed track into list
                insertTrack(newTrack);
                // update the list
                cursTr = getTracks();
                mAdapter.updateData(cursTr);

            }
        }
    }

    // hides the keyboard if anything other than EditText field is clicked
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();

        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }
    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

}
