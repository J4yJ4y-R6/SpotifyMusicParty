package com.tinf19.musicparty.client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;

import java.util.List;

/**
 * ClientPlaylistAdapter
 * @author Jannik Junker
 * @author Silas Wessely
 * @see RecyclerView.Adapter
 * @since 1.1
 */
public class ClientPlaylistAdapter extends RecyclerView.Adapter<ClientPlaylistAdapter.ViewHolder> {
    
    private List<Track> mDataset;

    /**
     * This ViewHolder is assigning the objects from row_client_playlist.xml to the global view-variables
     * @see RecyclerView.ViewHolder
     * @see TextView
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView songTitleTextView;
        public TextView artistNameTextView;

        /**
         * Constructor
         * @param itemView parent view from row_client_playlist.xml
         */
        public ViewHolder(View itemView) {
            super(itemView);
            songTitleTextView = (TextView) itemView.findViewById(R.id.playlistSongTitleTextView);
            artistNameTextView = (TextView) itemView.findViewById(R.id.playlistArtistNameTextView);
        }
    }

    /**
     * Constructor
     * @param trackList {@link List} of type {@link Track} from the server-playlist
     */
    public ClientPlaylistAdapter(List<Track> trackList) {
        this.mDataset = trackList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View textView = inflater.inflate(R.layout.row_client_playlist, parent, false);
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

    /**
     * Set a new List of data to the adapter
     * @param mDataset {@link List} of type {@link Track} from a new request to the server-playlist
     */
    public void setDataset(List<Track> mDataset) {
        this.mDataset = mDataset;
    }
}
