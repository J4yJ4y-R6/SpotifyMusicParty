package com.example.musicparty;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.musicparty.music.Track;

import java.util.ArrayList;
import java.util.List;


public class SearchSongsOutputFragment extends Fragment implements PartyAcRecycAdapter.SongCallback {

    private static final String NAME = SearchSongsOutputFragment.class.getName();
    private RecyclerView recyclerView;
    private PartyAcRecycAdapter mAdapter;
    AddSongCallback addSongCallback;

    public interface AddSongCallback {
        void addSong(Track track);
    }

    public SearchSongsOutputFragment(AddSongCallback addSongCallback) {
        this.addSongCallback = addSongCallback;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void showResult(List<Track> tracks) {
        Log.d(NAME, "Showing result");
        if(mAdapter != null) {
            mAdapter.setDataset(tracks);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_songs_output, container, false);

        recyclerView = view.findViewById(R.id.songsOutputRecyclerView);
        if(recyclerView != null) {
            mAdapter = new PartyAcRecycAdapter(new ArrayList<Track>(), this);
            recyclerView.setAdapter(mAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        return view;
    }


    @Override
    public void returnSong(Track track) {
        Log.d(NAME, "Clicked item " + track.getName());
        addSongCallback.addSong(track);
    }
}