package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.ClientPlaylistRecycAdapter;
import com.tinf19.musicparty.util.PartyAcRecycAdapter;

import java.util.ArrayList;
import java.util.List;

public class ClientPlaylistFragment extends Fragment {

    private static final String TAG = ClientPlaylistFragment.class.getName();
    private RecyclerView recyclerView;
    private ClientPlaylistRecycAdapter clientPlaylistRecycAdapter;

    public ClientPlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_client_playlist, container, false);

        recyclerView = view.findViewById(R.id.clientPlaylistRecyclerView);
        if(recyclerView != null) {
            clientPlaylistRecycAdapter = new ClientPlaylistRecycAdapter(new ArrayList<Track>());
            recyclerView.setAdapter(clientPlaylistRecycAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        return view;
    }

    public void showResult(List<Track> tracks) {
        Log.d(TAG, "showResult: show Playlist");
        if(clientPlaylistRecycAdapter != null) {
            clientPlaylistRecycAdapter.setDataset(tracks);
            clientPlaylistRecycAdapter.notifyDataSetChanged();
        }
    }
}