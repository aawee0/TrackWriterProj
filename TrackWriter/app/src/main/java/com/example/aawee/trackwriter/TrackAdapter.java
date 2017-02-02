package com.example.aawee.trackwriter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aawee on 1/02/2017.
 */

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private int mNumberItems;
    private List<String> mDataSet;

    public TrackAdapter(int numberOfItems, List<String> itemNames) {
        mNumberItems=numberOfItems;
        if(itemNames==null) mDataSet = new ArrayList<String>();
        else mDataSet=itemNames;
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
        holder.bind(position);
    }


    @Override
    public int getItemCount() {
        return mNumberItems;
    }

    public void addItem (String newItemName) {
        mDataSet.add(newItemName);
        mNumberItems++;
        notifyItemInserted(mDataSet.size()-1);
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView listItemView;

        public TrackViewHolder(View itemView) {
            super(itemView);
            listItemView = (TextView) itemView.findViewById(R.id.track_number);
        }

        void bind (int listIndex) {
            listItemView.setText(mDataSet.get(listIndex));
        }
    }
}
