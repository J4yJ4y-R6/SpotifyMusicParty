package com.tinf19.musicparty.client.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.databinding.FragmentClientPlaylistBinding;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.client.adapter.ClientPlaylistAdapter;
import com.tinf19.musicparty.util.DownloadImageTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment where the client can see the current playlist
 */
public class ClientPlaylistFragment extends Fragment {

    private static final String TAG = ClientPlaylistFragment.class.getName();
    private ClientPlaylistAdapter clientPlaylistAdapter;
    private ImageView currentSongCoverImageView;
    private TextView currentSongTitleTextView;
    private TextView currentSongArtistTextView;

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public ClientPlaylistFragment() { }



    //Android lifecycle methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_client_playlist, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.clientPlaylistRecyclerView);
        if(recyclerView != null) {
            clientPlaylistAdapter = new ClientPlaylistAdapter(new ArrayList<Track>());
            recyclerView.setAdapter(clientPlaylistAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        currentSongTitleTextView = view.findViewById(R.id.currentSongTitleTextView);
        currentSongArtistTextView = view.findViewById(R.id.currentSongArtistTextView);
        currentSongCoverImageView = view.findViewById(R.id.currentSongCoverImageView);

        return view;
    }


    /**
     * Set the information about the currently playing track in the views
     * @param track currently playing track
     */
    public void setCurrentPlaying(Track track) {
        if(track != null) {
            Log.d(TAG, "current track has been changed to: " + track.getName());
            if (currentSongTitleTextView != null) {
                currentSongTitleTextView.setText(track.getName());
            }
            if (currentSongArtistTextView != null) {
                currentSongArtistTextView.setText(track.getArtist(0).getName());
            }
            if (currentSongCoverImageView != null) {
                String coverURL = "https://i.scdn.co/image/" + track.getCoverFull();
                new DownloadImageTask(currentSongCoverImageView).execute(coverURL);
            }
        }
    }

    /**
     * Display the tracklist in the playlist RecyclerView
     * @param tracks {@link List} with all tracks from the playlist
     */
    public void showResult(List<Track> tracks) {
        if(clientPlaylistAdapter != null) {
            Log.d(TAG, "playlist has been updated with new size: " + tracks.size());
            clientPlaylistAdapter.setDataset(tracks);
            clientPlaylistAdapter.notifyDataSetChanged();
        }
    }
}