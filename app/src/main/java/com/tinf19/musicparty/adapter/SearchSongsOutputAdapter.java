package com.tinf19.musicparty.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.DownloadImageTask;

import java.util.List;


/**
 * SearchSongsOutputAdapter
 * @author Jannik Junker
 * @author Silas Wessely
 * @see RecyclerView.Adapter
 * @since 1.1
 */
public class SearchSongsOutputAdapter extends RecyclerView.Adapter<SearchSongsOutputAdapter.ViewHolder> implements SearchSongsOutputItemTouchHelper.SearchSongOutputItemTouchHelperCallback {

    public interface SearchSongOutputAdapterCallback {
        void returnSong(Track track);
    }

    private final SearchSongOutputAdapterCallback searchSongOutputAdapterCallback;
    private List<Track> mDataset;
    private View textView;


    /**
     * This ViewHolder is assigning the objects from row_song_output.xml to the global view-variables
     * @see RecyclerView.ViewHolder
     * @see TextView
     * @see ImageView
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements SearchSongsOutputItemTouchHelper.SearchSongOutputItemTouchHelperViewHolderCallback {

        public TextView songTitleTextView;
        public TextView artistNameTextView;
        public ImageView songCoverImageView;

        /**
         * Constructor
         * @param itemView parent view from row_song_output.xml
         */
        public ViewHolder(View itemView) {
            super(itemView);
            songTitleTextView = (TextView) itemView.findViewById(R.id.searchSongTitleTextView);
            artistNameTextView = (TextView) itemView.findViewById(R.id.searchArtistNameTextView);
            songCoverImageView = (ImageView) itemView.findViewById(R.id.songCover);
        }

        /**
         * Changes the background-color from a selected item in the RecyclerView to button_green from color.xml
         */
        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ContextCompat.getColor(textView.getContext(), R.color.button_green));
        }

        /**
         * Changes the background-color from a released item in the RecyclerView to transparent
         */
        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }


    /**
     * Constructor
     * @param trackList {@link List} of type {@link Track} from the search httpRequest
     * @param searchSongOutputAdapterCallback Callback to send the swiped element to the server-playlist
     */
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
            Snackbar.make(textView, textView.getContext().getString(R.string.text_songsOutputSwipeForQue), Snackbar.LENGTH_LONG).show();
        });

    }



    //Getter and Setter

    /**
     * Set a new List of data to the adapter
     * @param mDataset {@link List} of type {@link Track} from a new search request
     */
    public void setDataset(List<Track> mDataset) { this.mDataset = mDataset; }

    @Override
    public int getItemCount() { return mDataset.size(); }

    /**
     * Sending a song to the server-playlist
     * @param position position of the current song in the dataset
     */
    @Override
    public void sendToPlaylist(int position) { searchSongOutputAdapterCallback.returnSong(mDataset.get(position)); }
}
