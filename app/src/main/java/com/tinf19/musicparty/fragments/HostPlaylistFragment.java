package com.tinf19.musicparty.fragments;

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
import com.tinf19.musicparty.util.HostPlaylistRecycAdapter;
import com.tinf19.musicparty.util.HostPlaylistItemMoveCallback;

import java.util.ArrayList;
import java.util.List;

public class HostPlaylistFragment extends Fragment {

    private static final String TAG = HostPlaylistFragment.class.getName();
    private RecyclerView recyclerView;
    private TextView currentSongTitleTextView;
    private TextView currentSongArtistTextView;
    private ImageView currentSongCoverImageView;
    private HostPlaylistRecycAdapter hostPlaylistRecycAdapter;
    private PlaylistCallback playlistCallback;
    private HostPlaylistRecycAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback;

    public interface PlaylistCallback {
        void showPlaylist();
        Track getCurrentPlaying();
    }

    public HostPlaylistFragment(PlaylistCallback playlistCallback, HostPlaylistRecycAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback) {
        this.playlistCallback = playlistCallback;
        this.hostPlaylistAdapterCallback = hostPlaylistAdapterCallback;
    }

    public HostPlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        playlistCallback.showPlaylist();
        Track currentPlaying = playlistCallback.getCurrentPlaying();
        if(currentSongTitleTextView != null) currentSongTitleTextView.setText(currentPlaying.getName());
        if(currentSongArtistTextView != null) currentSongArtistTextView.setText(currentPlaying.getArtist(0).getName());
        if(currentSongCoverImageView != null) {
            String coverURL = "https://i.scdn.co/image/" + currentPlaying.getCover();
            new DownloadImageTask(currentSongCoverImageView).execute(coverURL);
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostPlaylistRecycAdapter.HostPlaylistAdapterCallback) {
            hostPlaylistAdapterCallback = (HostPlaylistRecycAdapter.HostPlaylistAdapterCallback) context;
        }
        if(context instanceof PlaylistCallback)
            playlistCallback = (PlaylistCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_host_playlist, container, false);

        recyclerView = view.findViewById(R.id.hostPlaylistRecyclerView);
        if(recyclerView != null) {
            hostPlaylistRecycAdapter = new HostPlaylistRecycAdapter(new ArrayList<Track>(), hostPlaylistAdapterCallback);
            ItemTouchHelper.Callback callback =
                    new HostPlaylistItemMoveCallback(hostPlaylistRecycAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerView);
            recyclerView.setAdapter(hostPlaylistRecycAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        currentSongTitleTextView = view.findViewById(R.id.currentSongTitleHostTextView);
        currentSongArtistTextView = view.findViewById(R.id.currentSongArtistHostTextView);
        currentSongCoverImageView = view.findViewById(R.id.currentSongCoverHostImageView);

        return view;
    }

    public void showResult(List<Track> tracks) {
        if(hostPlaylistRecycAdapter != null) {
            hostPlaylistRecycAdapter.setDataset(tracks);
            hostPlaylistRecycAdapter.notifyDataSetChanged();
        }
    }
}