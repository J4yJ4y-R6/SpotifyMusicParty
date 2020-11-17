package com.tinf19.musicparty.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;

import java.util.List;

public class ClientPlaylistRecycAdapter extends RecyclerView.Adapter<ClientPlaylistRecycAdapter.ViewHolder> {

    private static final String TAG = ClientPlaylistRecycAdapter.class.getName();
    private List<Track> mDataset;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView songTitleTextView;
        public TextView artistNameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            songTitleTextView = (TextView) itemView.findViewById(R.id.playlistSongTitleTextView);
            artistNameTextView = (TextView) itemView.findViewById(R.id.playlistArtistNameTextView);
        }
    }

    public ClientPlaylistRecycAdapter(List<Track> trackList) {
        this.mDataset = trackList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View textView = inflater.inflate(R.layout.client_playlist_recyc_view_row, parent, false);


        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mDataset.get(position).getName();
        String artist = mDataset.get(position).getArtist(0).getName();
        TextView songTitleTV = holder.songTitleTextView;
        if(songTitleTV != null)
            songTitleTV.setText(name);
        TextView artistNameTV = holder.artistNameTextView;
        if(artistNameTV != null)
            artistNameTV.setText(artist);
    }

    @Override
    public int getItemCount() { return mDataset.size(); }

    public void setDataset(List<Track> mDataset) {
        this.mDataset = mDataset;
    }
}
