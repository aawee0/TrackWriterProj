package com.example.aawee.trackwriter.data;

import android.provider.BaseColumns;

/**
 * Created by Aawee on 30/01/2017.
 */

public class TrackContract {

    public static final class GpsPointEntry implements BaseColumns{
        public static final String TABLE_NAME = "gps_point_table";

        public static final String TRACK_ID_NAME = "track_id";
        public static final String CREATION_TIME_NAME = "time";
        public static final String LATITUDE_NAME = "latitude";
        public static final String LONGITUDE_NAME = "longitude";
    }

    public static final class GpsTrackEntry implements BaseColumns{
        public static final String TABLE_NAME = "gps_track_table";

        public static final String TRACK_NAME_NAME = "name";

        public static final String CREATION_TIME_NAME = "start_time";
    }

}
