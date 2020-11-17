package com.tinf19.musicparty.util;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;

import java.util.List;


public class PartyAcRecycAdapter extends RecyclerView.Adapter<PartyAcRecycAdapter.ViewHolder> implements SearchSongsOutputItemTouchHelperCallback.ItemTouchHelperAdapter{

    public interface SongCallback{
        void returnSong(Track track);
    }

    private static final String TAG = PartyAcRecycAdapter.class.getName();
    private SongCallback songCallback;
    private List<Track> mDataset;
    private View textView;

    public class ViewHolder extends RecyclerView.ViewHolder implements SearchSongsOutputItemTouchHelperCallback.ItemTouchHelperViewHolder {

        public TextView songTitleTextView;
        public TextView artistNameTextView;
        public ImageView songCoverImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            songTitleTextView = (TextView) itemView.findViewById(R.id.searchSongTitleTextView);
            artistNameTextView = (TextView) itemView.findViewById(R.id.searchArtistNameTextView);
            songCoverImageView = (ImageView) itemView.findViewById(R.id.songCover);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.button_green));
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }

    public PartyAcRecycAdapter(List<Track> trackList, SongCallback songCallback) {
        this.songCallback = songCallback;
        mDataset = trackList;
    }

    public void setDataset(List<Track> mDataset) {
        this.mDataset = mDataset;
    }

    @NonNull
    @Override
    public PartyAcRecycAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        textView = inflater.inflate(R.layout.partyacrecyclerview_row, parent, false);

        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mDataset.get(position).getName();
        String artist = mDataset.get(position).getArtist(0).getName();
        String cover = mDataset.get(position).getCover();
        TextView songTitleTV = holder.songTitleTextView;
        if(songTitleTV != null)
            songTitleTV.setText(name);
        TextView artistNameTV = holder.artistNameTextView;
        if(artistNameTV != null)
            artistNameTV.setText(artist);
        ImageView songCoverIV = holder.songCoverImageView;
        if(songCoverIV != null)
            new DownloadImageTask(songCoverIV).execute(cover);
//        holder.itemView.setOnClickListener(v -> {
//            songCallback.returnSong(mDataset.get(position));
//        });

    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public void sendToPlaylist(int position) {
        songCallback.returnSong(mDataset.get(position));
    }
}
