package com.example.musicparty;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.style.TtsSpan;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_show_song, container, false);

        songCover = view.findViewById(R.id.songCoverImageView);
        songTitle = view.findViewById(R.id.songtitleTextView);
        songArtist = view.findViewById(R.id.artistTextView);
        songAlbum = view.findViewById(R.id.albumTextView);
        connectedToParty = view.findViewById(R.id.connectedTo);

        ImageButton exitButton = view.findViewById(R.id.exitButton);
        if(exitButton != null) {
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exitButtonClicked.exitConnection();
                }
            });
        }
        return view;
    }

    public void showSongs(Track track) {
        if(songCover != null) {
            String coverURL = "https://i.scdn.co/image/"+track.getCover().split(":")[2];
            new DownloadImageTask(songCover).execute(coverURL);
        }
        if(songTitle != null) {
            songTitle.setText(track.getName());
        }
        if(songArtist != null) {
            songArtist.setText(track.getArtist(0).getName());
        }
        if(songAlbum != null) {
            songAlbum.setText("Musteralbum");
        }
    }

    public void setPartyName(String name) {

        //TODO: Format String partyName
        String partyName = "Verbunden mit " + name;
        if(connectedToParty != null) {
            connectedToParty.setText(partyName);
        }
    }
}