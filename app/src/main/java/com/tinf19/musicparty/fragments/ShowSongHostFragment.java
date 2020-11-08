package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.DownloadImageTask;

import java.util.ArrayList;

public class ShowSongHostFragment extends Fragment {

    private static final String TAG = ShowSongHostFragment.class.getName();
    private OpenHostFragments openHostFragments;
    private String partyName;
    private Track nowPlaying;
    private ImageButton playTrackImageButton;
    private TextView currentPlayingTitleTextView;
    private TextView currentPlayingAlbumTextView;
    private TextView currentPlayingArtistTextView;
    private ImageView currentPlayingCoverTextView;

    private TextView partyNameTextView;


    public interface OpenHostFragments {
        void openSettingsFragment();
        void openPeopleFragment();
        void openPlaylistFragment();
        void openExitFragment();
        void nextTrack();
        void lastTrack();
        void playTrack();
        boolean getPauseState();
        Track setShowNowPlaying();
        int getPartyPeopleSize();
        String getPartyPeoplePartyName();
    }


    public ShowSongHostFragment(OpenHostFragments openHostFragments) {
        this.openHostFragments = openHostFragments;
    }

    public ShowSongHostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        this.partyName = openHostFragments.getPartyPeoplePartyName();
        if(partyNameTextView != null) {
            String text = partyName + " mit " + openHostFragments.getPartyPeopleSize() + " Menschen";
            partyNameTextView.setText(text);
        }
        Log.d(TAG, "onStart: got party name: " + partyName);
        if(openHostFragments != null) {
            setPlayTrackButtonImage(openHostFragments.getPauseState());
            Track track = openHostFragments.setShowNowPlaying();
            if(track != null) setNowPlaying(track);
        }
        if(currentPlayingTitleTextView != null) currentPlayingTitleTextView.setSelected(true);
        if(currentPlayingAlbumTextView != null) currentPlayingAlbumTextView.setSelected(true);
        if(currentPlayingArtistTextView != null) currentPlayingArtistTextView.setSelected(true);
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

        nowPlaying = openHostFragments.setShowNowPlaying();
//        Log.d(TAG, "onCreateView: " + nowPlaying.getName());

        partyNameTextView = view.findViewById(R.id.partyOverviewTextView);

        currentPlayingTitleTextView = view.findViewById(R.id.songtitleHostTextView);
        currentPlayingArtistTextView = view.findViewById(R.id.artistHostTextView);
        currentPlayingAlbumTextView = view.findViewById(R.id.albumHostTextView);
        currentPlayingCoverTextView = view.findViewById(R.id.songCoverHostImageView);

        ImageButton openPlaylistButton = view.findViewById(R.id.playlistButtonHostImageButton);
        if(openPlaylistButton != null) {
            openPlaylistButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openHostFragments.openPlaylistFragment(); }
            });
        }

        ImageButton openSettingsButton = view.findViewById(R.id.optionsButton);
        if(openSettingsButton != null) {
            openSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openHostFragments.openSettingsFragment(); }
            });
        }

        ImageButton openPeopleButton = view.findViewById(R.id.partyPeopleButton);
        if(openPeopleButton != null) {
            openPeopleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openHostFragments.openPeopleFragment(); }
            });
        }

        ImageButton openExitButton = view.findViewById(R.id.endPartyButton);
        if(openExitButton != null) {
            openExitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { openHostFragments.openExitFragment(); }
            });
        }

        ImageButton lastTrackImageButton = view.findViewById(R.id.lastTrackImageButton);
        if(lastTrackImageButton != null) {
            lastTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play last track");
                    openHostFragments.lastTrack();
                    setPlayTrackButtonImage(false);
                }
            });
        }

        playTrackImageButton = view.findViewById(R.id.playTrackImageButton);
        if(playTrackImageButton != null) {
            playTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play track");
                    boolean pause = !openHostFragments.getPauseState();
                    openHostFragments.playTrack();
                    setPlayTrackButtonImage(pause);
                }
            });
        }

        ImageButton nextTrackImageButton = view.findViewById(R.id.nextTrackImageButton);
        if(nextTrackImageButton != null) {
            nextTrackImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: play next track");
                    openHostFragments.nextTrack();
                    setPlayTrackButtonImage(false);
                }
            });
        }

        return view;
    }

    private void setPlayTrackButtonImage(boolean pause) {
        if(playTrackImageButton != null) {
            if(pause) playTrackImageButton.setImageResource(R.drawable.ic_play_track_button);
            else playTrackImageButton.setImageResource(R.drawable.ic_pause_button);
        }
    }

    public void setNowPlaying(Track nowPlaying) {
        if(currentPlayingTitleTextView != null) currentPlayingTitleTextView.setText(nowPlaying.getName());
        if(currentPlayingArtistTextView != null) currentPlayingArtistTextView.setText(nowPlaying.getArtist(0).getName());
        if(currentPlayingAlbumTextView != null) currentPlayingAlbumTextView.setText(nowPlaying.getAlbum());
        if(currentPlayingCoverTextView != null) {
            String coverURL = "https://i.scdn.co/image/"+nowPlaying.getCover();
            new DownloadImageTask(currentPlayingCoverTextView).execute(coverURL);
        }
    }
}