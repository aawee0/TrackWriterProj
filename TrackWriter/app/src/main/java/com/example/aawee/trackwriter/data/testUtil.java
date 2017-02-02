package com.example.aawee.trackwriter.data;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aawee on 31/01/2017.
 */

public class testUtil {
    public static void insertFakeData (SQLiteDatabase db) {
        if (db==null) return;



        // create a fake TRACK
        ContentValues cv = new ContentValues();
        cv.put(TrackContract.GpsTrackEntry.TRACK_NAME_NAME, "Random point track");
        cv.put(TrackContract.GpsTrackEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));
        long trackID;

        // one more
        ContentValues cv2 = new ContentValues();
        cv2.put(TrackContract.GpsTrackEntry.TRACK_NAME_NAME, "Test track");
        cv2.put(TrackContract.GpsTrackEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));

        try {
            db.beginTransaction();
            db.delete(TrackContract.GpsTrackEntry.TABLE_NAME, null,null);

            trackID = db.insert(TrackContract.GpsTrackEntry.TABLE_NAME, null, cv);
            db.insert(TrackContract.GpsTrackEntry.TABLE_NAME, null, cv2);
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            // error
            Log.e("DBerr", e.getStackTrace().toString() );
            trackID = 0;
        }
        finally {
            db.endTransaction();
        }

        // create a list of fake POINTS
        List<ContentValues> list = new ArrayList<ContentValues>();

        cv = new ContentValues();
        cv.put(TrackContract.GpsPointEntry.TRACK_ID_NAME, trackID);
        //java.sql.Timestamp date = new java.sql.Timestamp (new java.util.Date().getTime());
        cv.put(TrackContract.GpsPointEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));
        cv.put(TrackContract.GpsPointEntry.LATITUDE_NAME, -170);
        cv.put(TrackContract.GpsPointEntry.LONGITUDE_NAME, 20);
        list.add(cv);

        cv = new ContentValues();
        cv.put(TrackContract.GpsPointEntry.TRACK_ID_NAME, trackID);
        //java.sql.Timestamp date = new java.sql.Timestamp (new java.util.Date().getTime());
        cv.put(TrackContract.GpsPointEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));
        cv.put(TrackContract.GpsPointEntry.LATITUDE_NAME, -160);
        cv.put(TrackContract.GpsPointEntry.LONGITUDE_NAME, 30);
        list.add(cv);

        cv = new ContentValues();
        cv.put(TrackContract.GpsPointEntry.TRACK_ID_NAME, trackID);
        //java.sql.Timestamp date = new java.sql.Timestamp (new java.util.Date().getTime());
        cv.put(TrackContract.GpsPointEntry.CREATION_TIME_NAME, (new java.util.Date().getTime()));
        cv.put(TrackContract.GpsPointEntry.LATITUDE_NAME, -180);
        cv.put(TrackContract.GpsPointEntry.LONGITUDE_NAME, 40);
        list.add(cv);


        try {
            db.beginTransaction();
            db.delete(TrackContract.GpsPointEntry.TABLE_NAME, null,null);
            for(ContentValues c: list) {
                long ins = db.insert(TrackContract.GpsPointEntry.TABLE_NAME, null, c);
            }
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            // error
            Log.e("DBerr", e.getStackTrace().toString() );
        }
        finally {
            db.endTransaction();
        }

    }
}
