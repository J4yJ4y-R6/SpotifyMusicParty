package com.tinf19.musicparty.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.DownloadImageTask;

import java.util.List;


public class SearchSongsOutputAdapter extends RecyclerView.Adapter<SearchSongsOutputAdapter.ViewHolder> implements SearchSongsOutputItemTouchHelper.SearchSongOutputItemTouchHelperCallback {

    public interface SearchSongOutputAdapterCallback {
        void returnSong(Track track);
    }

    private SearchSongOutputAdapterCallback searchSongOutputAdapterCallback;
    private List<Track> mDataset;
    private View textView;

    public class ViewHolder extends RecyclerView.ViewHolder implements SearchSongsOutputItemTouchHelper.SearchSongOutputItemTouchHelperViewHolderCallback {

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

    public SearchSongsOutputAdapter(List<Track> trackList, SearchSongOutputAdapterCallback searchSongOutputAdapterCallback) {
        this.searchSongOutputAdapterCallback = searchSongOutputAdapterCallback;
        mDataset = trackList;
    }


    //Android lifecycle methods

    @NonNull
    @Override
    public SearchSongsOutputAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        textView = inflater.inflate(R.layout.row_song_output, parent, false);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mDataset.get(position).getName();
        String artist = mDataset.get(position).getArtist(0).getName();
        String cover = "https://i.scdn.co/image/" + mDataset.get(position).getCover();
        TextView songTitleTV = holder.songTitleTextView;
        if(songTitleTV != null)
            songTitleTV.setText(name);
        TextView artistNameTV = holder.artistNameTextView;
        if(artistNameTV != null)
            artistNameTV.setText(artist);
        ImageView songCoverIV = holder.songCoverImageView;
        if(songCoverIV != null)
            new DownloadImageTask(songCoverIV).execute(cover);
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(textView.getContext(), textView.getContext().getString(R.string.text_songsOutputSwipeForQue), Toast.LENGTH_LONG).show();
        });

    }



    //Getter and Setter

    public void setDataset(List<Track> mDataset) { this.mDataset = mDataset; }

    @Override
    public int getItemCount() { return mDataset.size(); }

    @Override
    public void sendToPlaylist(int position) { searchSongOutputAdapterCallback.returnSong(mDataset.get(position)); }
}
