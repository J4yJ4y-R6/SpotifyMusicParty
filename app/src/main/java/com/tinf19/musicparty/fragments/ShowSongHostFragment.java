package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tinf19.musicparty.R;

public class ShowSongHostFragment extends Fragment {

    private static final String TAG = ShowSongHostFragment.class.getName();
    private OpenHostFragments openHostFragments;
    private String partyName  = "Coole Party";


    public interface OpenHostFragments {
        void openSettingsFragment();
        void openPeopleFragment();
        void openPlaylistFragment();
        void openExitFragment();
    }


    public ShowSongHostFragment(OpenHostFragments openHostFragments) {
        this.openHostFragments = openHostFragments;
    }

    public ShowSongHostFragment() {
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
        View view = inflater.inflate(R.layout.fragment_show_song_host, container, false);

        TextView partyNameTextView = view.findViewById(R.id.partyOverviewTextView);
        if(partyNameTextView != null) {
            String text = partyName + " 0";
            partyNameTextView.setText(text);
        }

        ImageButton openPlaylistButton = view.findViewById(R.id.playlistButtonHostImageButton);
        if(openPlaylistButton != null) {
            openPlaylistButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openHostFragments.openPlaylistFragment();
                }
            });
        }
        ImageButton openSettingsButton = view.findViewById(R.id.optionsButton);
        if(openSettingsButton != null) {
            openSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openHostFragments.openSettingsFragment();
                }
            });
        }
        ImageButton openPeopleButton = view.findViewById(R.id.partyPeopleButton);
        if(openPeopleButton != null) {
            openPeopleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openHostFragments.openPeopleFragment();
                }
            });
        }
        ImageButton openExitButton = view.findViewById(R.id.endPartyButton);
        if(openExitButton != null) {
            openExitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openHostFragments.openExitFragment(); }
            });
        }
        ImageButton lastTrackImageButton = view.findViewById(R.id.lastTrackImagebutton);
        if(lastTrackImageButton != null) {
            lastTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play last track");
                }
            });
        }
        ImageButton playTrackImageButton = view.findViewById(R.id.playTrackImageButton);
        if(playTrackImageButton != null) {
            playTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play track");
                }
            });
        }
        ImageButton nextTrackImageButton = view.findViewById(R.id.nextTrackImageButton);
        if(nextTrackImageButton != null) {
            nextTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play next track");
                }
            });
        }

        return view;
    }
}