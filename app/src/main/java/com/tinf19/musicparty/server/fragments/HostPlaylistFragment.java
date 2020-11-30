package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.DownloadImageTask;
import com.tinf19.musicparty.server.Adapter.HostPlaylistAdapter;
import com.tinf19.musicparty.server.Adapter.HostPlaylistItemMoveHelper;

import java.util.ArrayList;
import java.util.List;

public class HostPlaylistFragment extends Fragment {

    private static final String TAG = HostPlaylistFragment.class.getName();
    private RecyclerView recyclerView;
    private TextView currentSongTitleTextView;
    private TextView currentSongArtistTextView;
    private ImageView currentSongCoverImageView;
    private HostPlaylistAdapter hostPlaylistAdapter;
    private HostPlaylistCallback hostPlaylistCallback;
    private HostPlaylistAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback;

    public interface HostPlaylistCallback {
        void showPlaylist();
        Track getCurrentPlaying();
    }

    public HostPlaylistFragment(HostPlaylistCallback hostPlaylistCallback, HostPlaylistAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback) {
        this.hostPlaylistCallback = hostPlaylistCallback;
        this.hostPlaylistAdapterCallback = hostPlaylistAdapterCallback;
    }

    public HostPlaylistFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        hostPlaylistCallback.showPlaylist();
        Track currentPlaying = hostPlaylistCallback.getCurrentPlaying();
        Log.d(TAG, "set current track to " + currentPlaying.getName());
        if(currentPlaying != null) {
            if (currentSongTitleTextView != null)
                currentSongTitleTextView.setText(currentPlaying.getName());
            if (currentSongArtistTextView != null)
                currentSongArtistTextView.setText(currentPlaying.getArtist(0).getName());
            if (currentSongCoverImageView != null) {
                String coverURL = "https://i.scdn.co/image/" + currentPlaying.getCoverFull();
                new DownloadImageTask(currentSongCoverImageView).execute(coverURL);
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostPlaylistAdapter.HostPlaylistAdapterCallback) {
            hostPlaylistAdapterCallback = (HostPlaylistAdapter.HostPlaylistAdapterCallback) context;
        }
        if(context instanceof HostPlaylistCallback)
            hostPlaylistCallback = (HostPlaylistCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_host_playlist, container, false);
        recyclerView = view.findViewById(R.id.hostPlaylistRecyclerView);
        if(recyclerView != null) {
            hostPlaylistAdapter = new HostPlaylistAdapter(new ArrayList<Track>(), hostPlaylistAdapterCallback);
            ItemTouchHelper touchHelper = new ItemTouchHelper(new HostPlaylistItemMoveHelper(hostPlaylistAdapter));
            touchHelper.attachToRecyclerView(recyclerView);
            recyclerView.setAdapter(hostPlaylistAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        currentSongTitleTextView = view.findViewById(R.id.currentSongTitleHostTextView);
        currentSongArtistTextView = view.findViewById(R.id.currentSongArtistHostTextView);
        currentSongCoverImageView = view.findViewById(R.id.currentSongCoverHostImageView);

        return view;
    }

    public void showResult(List<Track> tracks) {
        if(hostPlaylistAdapter != null) {
            Log.d(TAG, "playlist has been updated with new size: " + tracks.size());
            hostPlaylistAdapter.setDataset(tracks);
            hostPlaylistAdapter.notifyDataSetChanged();
        }
    }

    public void updateRecyclerView() {
        Log.d(TAG, "playlist has been updated - notify RecyclerView");
        hostPlaylistAdapter.notifyDataSetChanged();
    }
}