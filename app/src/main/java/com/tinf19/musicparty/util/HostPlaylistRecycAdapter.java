package com.tinf19.musicparty.util;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HostPlaylistRecycAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private ArrayList mdataset;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView songTitleTextView;
        private TextView songArtistTextView;

        public MyViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            songTitleTextView 
        }
    }


}
