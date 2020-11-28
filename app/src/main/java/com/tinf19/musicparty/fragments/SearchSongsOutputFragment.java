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

import com.tinf19.musicparty.adapter.SearchSongsOutputAdapter;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.adapter.SearchSongsOutputItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.List;


public class SearchSongsOutputFragment extends Fragment implements SearchSongsOutputAdapter.SongCallback {

    private static final String TAG = SearchSongsOutputFragment.class.getName();
    private RecyclerView recyclerView;
    private SearchSongsOutputAdapter mAdapter;
    AddSongCallback addSongCallback;

    public interface AddSongCallback {
        void addSong(Track track);
    }

    public SearchSongsOutputFragment(AddSongCallback addSongCallback) {
        this.addSongCallback = addSongCallback;
    }

    public SearchSongsOutputFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void showResult(List<Track> tracks) {
        if(mAdapter != null) {
            mAdapter.setDataset(tracks);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof AddSongCallback)
            addSongCallback = (AddSongCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_songs_output, container, false);

        recyclerView = view.findViewById(R.id.songsOutputRecyclerView);
        if(recyclerView != null) {
            mAdapter = new SearchSongsOutputAdapter(new ArrayList<Track>(), this);
            recyclerView.setAdapter(mAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
            ItemTouchHelper.Callback swipeController = new SearchSongsOutputItemTouchHelperCallback(mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeController);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        return view;
    }


    @Override
    public void returnSong(Track track) {
        Log.d(TAG, "Clicked item " + track.getName());
        addSongCallback.addSong(track);
    }
}