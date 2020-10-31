package com.example.musicparty;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.musicparty.databinding.ActivityPartyBinding;
import com.spotify.android.appremote.api.UserApi;

public class SearchBarFragment extends Fragment {

    //ActivityPartyBinding binding;
    public SearchForSongs searchForSongs;

    public interface SearchForSongs {
        void searchForSongs();
    }

    public SearchBarFragment(SearchForSongs searchForSongs) {
        this.searchForSongs = searchForSongs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_bar, container, false);

        Button searchButton = view.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForSongs.searchForSongs();
            }
        });

        return view;
    }
}