package com.example.aawee.trackwriter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.aawee.trackwriter.data.TrackContract;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Aawee on 1/02/2017.
 */

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    //private List<String> mDataSet;
    private Cursor mCursor;

    final private ListItemClickListener mOnClickListener;

    public TrackAdapter(Cursor cursor, ListItemClickListener listener) {
        mCursor = cursor;

        mOnClickListener = listener;

        //if(itemNames==null) mDataSet = new ArrayList<String>();
        //else mDataSet=itemNames;
    }

    public void updateData (Cursor cursor) {
        mCursor = cursor;
        this.notifyDataSetChanged();
    }

    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflating
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.track_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem,parent, shouldAttachToParentImmediately);
        TrackViewHolder viewHolder = new TrackViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {
        if(!mCursor.moveToPosition(position)) return;

        long trID = mCursor.getLong(mCursor.getColumnIndex(TrackContract.GpsTrackEntry._ID));
        String name = mCursor.getString(mCursor.getColumnIndex(TrackContract.GpsTrackEntry.TRACK_NAME_NAME));
        Date date = new Date(mCursor.getLong(mCursor.getColumnIndex(TrackContract.GpsTrackEntry.CREATION_TIME_NAME)));

        holder.listItemView.setText(name); // adding ID for naming (on debugging stage)

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        //System.out.println(dateFormat.format(date));

        holder.listTimeView.setText(dateFormat.format(date));
    }


    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex);

    }

    class TrackViewHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener{
        TextView listItemView;
        TextView listTimeView;

        public TrackViewHolder(View itemView) {
            super(itemView);
            listItemView = (TextView) itemView.findViewById(R.id.track_name);
            listTimeView = (TextView) itemView.findViewById(R.id.track_time);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onListItemClick(clickedPosition);
        }
    }
}
