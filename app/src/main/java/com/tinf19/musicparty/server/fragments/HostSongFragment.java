package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.server.HostService;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.DownloadImageTask;

/**
 * Fragment where the host sees the current playing song. He can also go to other fragments from
 * this one.
 * Also he can interact with the queue. He can pause / resume the currently playing song, skip to
 * the next one or go back to the last one.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostSongFragment extends Fragment {

    private static final String TAG = HostSongFragment.class.getName();
    private HostSongCallback hostSongCallback;
    private String partyName;
    private ImageButton playTrackImageButton;
    private TextView currentPlayingTitleTextView;
    private TextView currentPlayingAlbumTextView;
    private TextView currentPlayingArtistTextView;
    private ImageView currentPlayingCoverTextView;
    private TextView partyNameTextView;
    private LinearLayout playBarLinearLayout;
    private LinearLayout openVotingButtonLinearLayout;


    public interface HostSongCallback {
        void openSettingsFragment();
        void openPeopleFragment();
        void openPlaylistFragment();
        void openExitFragment();
        void openVotingFragment();
        void nextTrack();
        void lastTrack();
        void playTrack();
        boolean getPauseState();
        Track setShowNowPlaying();
        int getPartyPeopleSize();
        String getPartyPeoplePartyName();
        HostService.PartyType getPartyType();
    }


    /**
     * Constructor to set the callback
     * @param hostSongCallback Communication callback for
     *                         {@link com.tinf19.musicparty.server.HostActivity}.
     */
    public HostSongFragment(HostSongCallback hostSongCallback) { this.hostSongCallback = hostSongCallback; }

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public HostSongFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "set party name and the current track in TextView");
        this.partyName = hostSongCallback.getPartyPeoplePartyName();
        setPartyNameCount(hostSongCallback.getPartyPeopleSize());
        if(hostSongCallback != null) {
            setPlayTrackButtonImage(hostSongCallback.getPauseState());
            Track track = hostSongCallback.setShowNowPlaying();
            if(track != null) setNowPlaying(track);
            if(openVotingButtonLinearLayout != null) {
                if (hostSongCallback.getPartyType().equals(HostService.PartyType.VoteParty))
                    openVotingButtonLinearLayout.setVisibility(View.VISIBLE);
                else
                    openVotingButtonLinearLayout.setVisibility(View.GONE);
            }
        }
        if(currentPlayingTitleTextView != null) currentPlayingTitleTextView.setSelected(true);
        if(currentPlayingAlbumTextView != null) currentPlayingAlbumTextView.setSelected(true);
        if(currentPlayingArtistTextView != null) currentPlayingArtistTextView.setSelected(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostSongCallback)
            hostSongCallback = (HostSongCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_song, container, false);

        partyNameTextView = view.findViewById(R.id.partyOverviewTextView);

        currentPlayingTitleTextView = view.findViewById(R.id.songtitleHostTextView);
        currentPlayingArtistTextView = view.findViewById(R.id.artistHostTextView);
        currentPlayingAlbumTextView = view.findViewById(R.id.albumHostTextView);
        currentPlayingCoverTextView = view.findViewById(R.id.songCoverHostImageView);
        playBarLinearLayout = view.findViewById(R.id.hostPlayBarLinearLayout);
        openVotingButtonLinearLayout = view.findViewById(R.id.openVotingButtonLinearLayout);

        ImageButton openPlaylistButton = view.findViewById(R.id.playlistButtonHostImageButton);
        if(openPlaylistButton != null) {
            openPlaylistButton.setOnClickListener(v -> hostSongCallback.openPlaylistFragment());
        }

        ImageButton openSettingsButton = view.findViewById(R.id.optionsButton);
        if(openSettingsButton != null) {
            openSettingsButton.setOnClickListener(v -> hostSongCallback.openSettingsFragment());
        }

        ImageButton openVotingButton = view.findViewById(R.id.votingButtonHostImageButton);
        if(openVotingButton != null) {
            openVotingButton.setOnClickListener(v -> hostSongCallback.openVotingFragment());
        }

        ImageButton openPeopleButton = view.findViewById(R.id.partyPeopleButton);
        if(openPeopleButton != null) {
            openPeopleButton.setOnClickListener(v -> hostSongCallback.openPeopleFragment());
        }

        ImageButton openExitButton = view.findViewById(R.id.endPartyButton);
        if(openExitButton != null) {
            openExitButton.setOnClickListener(v -> hostSongCallback.openExitFragment());
        }

        ImageButton lastTrackImageButton = view.findViewById(R.id.lastTrackImageButton);
        if(lastTrackImageButton != null) {
            lastTrackImageButton.setOnClickListener(v -> {
                //buttonEffect(lastTrackImageButton);
                hostSongCallback.lastTrack();
                setPlayTrackButtonImage(false);
            });
        }

        playTrackImageButton = view.findViewById(R.id.playTrackImageButton);
        if(playTrackImageButton != null) {
            playTrackImageButton.setOnClickListener(v -> {
                //buttonEffect(playTrackImageButton);
                boolean pause = !hostSongCallback.getPauseState();
                hostSongCallback.playTrack();
                setPlayTrackButtonImage(pause);
            });
        }

        ImageButton nextTrackImageButton = view.findViewById(R.id.nextTrackImageButton);
        if(nextTrackImageButton != null) {
            nextTrackImageButton.setOnClickListener(v -> {
                //buttonEffect(nextTrackImageButton);
                hostSongCallback.nextTrack();
                setPlayTrackButtonImage(false);
            });
        }

        return view;
    }


    /**
     * Set the information header with the party name and the count of people currently connected
     * to the party.
     * @param count Count of people currently connected to the party
     */
    public void setPartyNameCount(int count) {
        if(partyNameTextView != null)
            partyNameTextView.setText(getString(R.string.text_hostsongfragment_header, partyName, count));
    }

    /**
     * Changing the image of the play button. If pause is true it will be the play icon and if it is
     * false it will change to the pause button
     * @param pause Boolean to decide whether the current song is paused.
     */
    public void setPlayTrackButtonImage(boolean pause) {
        if(playTrackImageButton != null) {
            if(pause) playTrackImageButton.setImageResource(R.drawable.icon_play_cycle);
            else playTrackImageButton.setImageResource(R.drawable.icon_pause_cycle);
        }
    }

    /**
     * Set the information about the currently playing song in the TextViews. This method will be
     * called when another songs starts playing.
     * @param nowPlaying Currently playing song of type {@link Track}
     */
    public void setNowPlaying(Track nowPlaying) {
        Log.d(TAG, "set current track to: " + nowPlaying.getName());
        if(playBarLinearLayout != null && playBarLinearLayout.getVisibility() == View.INVISIBLE) {
            Log.d(TAG, "welcome message gets changed to first song");
            playBarLinearLayout.setVisibility(View.VISIBLE);
            currentPlayingTitleTextView.setSingleLine(true);
            currentPlayingTitleTextView.setHeight(150);
        }
        if(currentPlayingTitleTextView != null) {
            currentPlayingTitleTextView.setText(nowPlaying.getName());
        } else Log.d(TAG, "setNowPlaying: " + nowPlaying.getName());
        if(currentPlayingArtistTextView != null) currentPlayingArtistTextView.setText(nowPlaying.getArtist(0).getName());
        if(currentPlayingAlbumTextView != null) currentPlayingAlbumTextView.setText(nowPlaying.getAlbum());
        if(currentPlayingCoverTextView != null) {
            String coverURL = Constants.IMAGE_URI + nowPlaying.getCoverFull();
            new DownloadImageTask(currentPlayingCoverTextView).execute(coverURL);
        }
    }

    /**
     * Changing the background color of the button after clicking it to give the user a feedback.
     * @param button Clicked button view
     */
    public static void buttonEffect(View button){
        button.setOnTouchListener((v, event) -> {
            v.performClick();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    v.getBackground().setColorFilter(Color.green(255), PorterDuff.Mode.SRC_ATOP);
                    v.invalidate();
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    v.getBackground().clearColorFilter();
                    v.invalidate();
                    break;
                }
            }
            return false;
        });
    }
}