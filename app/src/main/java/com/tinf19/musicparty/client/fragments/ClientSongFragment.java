package com.tinf19.musicparty.client.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinf19.musicparty.util.DownloadImageTask;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;


public class ClientSongFragment extends Fragment {

    private static final String TAG = ClientSongFragment.class.getName();
    public PartyButtonClicked partyButtonClicked;
    private ImageView songCover;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songAlbum;
    private TextView connectedToParty;
    private View rootView;
    private boolean started;

    public interface PartyButtonClicked {
        void exitConnection();
        void showPlaylist();
    }

    public ClientSongFragment(PartyButtonClicked partyButtonClicked) {
        this.partyButtonClicked = partyButtonClicked;
    }

    public ClientSongFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        started = true;
        if(songTitle != null) {
            songTitle.setSelected(true);
        }
        if(songArtist != null) {
            songArtist.setSelected(true);
        }
        if(songAlbum != null) {
            songAlbum.setSelected(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        started = false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof PartyButtonClicked)
            partyButtonClicked = (PartyButtonClicked) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_show_song, container, false);

        songCover = rootView.findViewById(R.id.songCoverImageView);
        songTitle = rootView.findViewById(R.id.songtitleTextView);
        songArtist = rootView.findViewById(R.id.artistTextView);
        songAlbum = rootView.findViewById(R.id.albumTextView);
        connectedToParty = rootView.findViewById(R.id.connectedTo);

        ImageButton exitButton = rootView.findViewById(R.id.exitButton);
        if(exitButton != null) {
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    partyButtonClicked.exitConnection();
                }
            });
        }

        ImageButton playlistButton = rootView.findViewById(R.id.playlistButtonImageButton);
        if(playlistButton != null) {
            playlistButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            partyButtonClicked.showPlaylist();
                        }
                    }).start();
                }
            });
        }
        return rootView;
    }

    public boolean getStarted() {
        return started;
    }

    public void showSongs(Track track) {

        if(track != null && songTitle.getHeight() > 150) {
            songTitle.setSingleLine(true);
            songTitle.setHeight(150);
        }
        Log.d(TAG, "showSongs: " + track);
        if(songCover != null) {
            String coverURL = "https://i.scdn.co/image/"+track.getCoverFull();
            new DownloadImageTask(songCover).execute(coverURL);
        }
        if(songTitle != null) songTitle.setText(track.getName());
        if(songArtist != null) songArtist.setText(track.getArtist(0).getName());
        if(songAlbum != null) songAlbum.setText(track.getAlbum());
    }

    public void setPartyName(String name) {
        String conTo = "Verbunden mit ";
        String partyName = conTo + name;
        if(connectedToParty != null && !name.equals("")) {
            connectedToParty.setText(partyName, TextView.BufferType.SPANNABLE);
            Spannable spannable = (Spannable)connectedToParty.getText();
            int start = conTo.length();
            int end = start + name.length();
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}