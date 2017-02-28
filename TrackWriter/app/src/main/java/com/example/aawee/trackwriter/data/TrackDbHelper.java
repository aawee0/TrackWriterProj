package com.example.aawee.trackwriter.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Aawee on 31/01/2017.
 */

public class TrackDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tracks.db";

    private static final int DATABASE_VERSION = 21;

    public TrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create table of tracks
        final String SQL_CREATE_TRACK_TABLE = "CREATE TABLE " + TrackContract.GpsTrackEntry.TABLE_NAME +
                " (" + TrackContract.GpsTrackEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TrackContract.GpsTrackEntry.TRACK_NAME_NAME + " TEXT NOT NULL, " +
                TrackContract.GpsTrackEntry.CREATION_TIME_NAME + " LONG NOT NULL);";

        sqLiteDatabase.execSQL(SQL_CREATE_TRACK_TABLE);

        // Create table of points
        final String SQL_CREATE_POINT_TABLE = "CREATE TABLE " + TrackContract.GpsPointEntry.TABLE_NAME +
                " (" + TrackContract.GpsPointEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TrackContract.GpsPointEntry.TRACK_ID_NAME + " INTEGER NOT NULL, " +
                TrackContract.GpsPointEntry.LATITUDE_NAME + " DECIMAL(10,6) NOT NULL, " +
                TrackContract.GpsPointEntry.LONGITUDE_NAME + " DECIMAL(10,6) NOT NULL, " +
                TrackContract.GpsPointEntry.ACCURACY_NAME + " DECIMAL(4,2) NOT NULL, " +
                TrackContract.GpsPointEntry.BEARING_NAME + " DECIMAL(5,2) NOT NULL, " +
                TrackContract.GpsPointEntry.SPEED_NAME + " DECIMAL(5,2) NOT NULL, " +
                TrackContract.GpsPointEntry.CREATION_TIME_NAME + " LONG NOT NULL, " +
                " FOREIGN KEY (" + TrackContract.GpsPointEntry.TRACK_ID_NAME +
                ") REFERENCES " + TrackContract.GpsTrackEntry.TABLE_NAME + "(" + TrackContract.GpsTrackEntry._ID + ") );";

        sqLiteDatabase.execSQL(SQL_CREATE_POINT_TABLE);


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TrackContract.GpsTrackEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TrackContract.GpsPointEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
