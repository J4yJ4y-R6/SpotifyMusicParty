package com.example.musicparty;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicparty.music.Track;

public class ShowSongFragment extends Fragment {

    public ExitButtonClicked exitButtonClicked;
    private ImageView songCover;
    private TextView songTitle;
    private TextView songArtist;
    private TextView songAlbum;
    private TextView connectedToParty;
    private View rootView;
    private boolean started;

    public interface ExitButtonClicked {
        void exitConnection();
    }

    public ShowSongFragment(ExitButtonClicked exitButtonClicked) {
        this.exitButtonClicked = exitButtonClicked;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(ShowSongFragment.class.getName(), "I have been started");
        started = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        started = false;
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
                    exitButtonClicked.exitConnection();
                }
            });
        }
        return rootView;
    }

    public boolean getStarted() {
        return started;
    }

    public void showSongs(Track track) {
        Log.d(ShowSongFragment.class.getName(), "Now Playing: " + track.toString());
        if(songCover != null) {
            String coverURL = "https://i.scdn.co/image/"+track.getCover();
            new DownloadImageTask(songCover).execute(coverURL);
        }
        if(songTitle != null) {
            songTitle.setText(track.getName());
        }
        if(songArtist != null) {
            songArtist.setText(track.getArtist(0).getName());
        }
        if(songAlbum != null) {
            songAlbum.setText(track.getAlbum());
        }
    }

    public void setPartyName(String name) {
        //TODO: Format String partyName
        String conTo = "Verbunden mit ";
        String partyName = conTo + name;
        if(connectedToParty != null) {
            connectedToParty.setText(partyName, TextView.BufferType.SPANNABLE);
            Spannable spannable = (Spannable)connectedToParty.getText();
            int start = conTo.length();
            int end = start + name.length();
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}