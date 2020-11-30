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
import com.tinf19.musicparty.adapter.SearchSongsOutputItemTouchHelper;

import java.util.ArrayList;
import java.util.List;


public class SearchSongsOutputFragment extends Fragment {

    private static final String TAG = SearchSongsOutputFragment.class.getName();
    private SearchSongsOutputAdapter mAdapter;
    private SearchSongsOutputCallback searchSongsOutputCallback;

    public interface SearchSongsOutputCallback {
        void addSong(Track track);
    }

    public SearchSongsOutputFragment(SearchSongsOutputCallback searchSongsOutputCallback) {
        this.searchSongsOutputCallback = searchSongsOutputCallback;
    }

    public SearchSongsOutputFragment() { }



    //Android lifecycle methods

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof SearchSongsOutputCallback)
            searchSongsOutputCallback = (SearchSongsOutputCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_songs_output, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.songsOutputRecyclerView);
        if(recyclerView != null) {
            mAdapter = new SearchSongsOutputAdapter(new ArrayList<>(), track -> searchSongsOutputCallback.addSong(track));
            recyclerView.setAdapter(mAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
            ItemTouchHelper.Callback swipeController = new SearchSongsOutputItemTouchHelper(mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeController);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        return view;
    }



    public void showResult(List<Track> tracks) {
        if(mAdapter != null) {
            Log.d(TAG, "show list of songs in the output-fragment");
            mAdapter.setDataset(tracks);
            mAdapter.notifyDataSetChanged();
        }
    }
}