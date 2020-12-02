package com.tinf19.musicparty.music;

import android.os.CountDownTimer;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import java.util.ArrayList;

/**
 * Que object to create a queue which is used like a Spotify playlist.
 * The queue is used as a ArrayList filled with tracks.
 * While a track is playing a synchronous timer is running which counts the time that a track is
 * playing. The timer gets controlled synchronous with the track.
 * The queue object can pause, resume, skip and go back to last track.
 * Also tracks can get added or removed from the current queue state.
 * Also it can be cleared when the user is starting another playlist from Spotify or
 * {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}.
 * @author Jannik Junker
 * @author Silas Wessely
 * @see Playlist
 * @since 1.1
 */
public class Que {

    public interface QueCallback {
        void playSong(Track track);
        void setProgressBar(long timeRemaining);
        void stopPlayback();
    }

    private static final String TAG = Que.class.getName();
    private final QueCallback queCallback;
    private ArrayList<Track> queList = new ArrayList<>();
    public CountDownTimer countDownTimer;
    private Track nowPlaying;
    private boolean playlistEnded;
    private long remainingTime;
    private long pauseTime;
    private boolean paused;

    /**
     * Constructor to set the callback
     * @param queCallback Communication callback for {@link com.tinf19.musicparty.server.HostService}
     */
    public Que(QueCallback queCallback) {
        this.queCallback = queCallback;
    }



    //Setter

    /**
     * Set queue after restarting it
     * @param queList list of type {@link Track}
     */
    public void setQueList(ArrayList<Track> queList) {
        this.queList = queList;
    }

    /**
     * Set the boolean if the playlist is at the end
     * @param playlistEnded Boolean if playlist is at the end
     */
    public void setPlaylistEnded(boolean playlistEnded) {
        this.playlistEnded = playlistEnded;
    }



    //Getter

    /**
     * @return Get true if playlist is at the and or false if it is not
     */
    public boolean isPlaylistEnded() {
        return playlistEnded;
    }

    /**
     * @return Get a ArrayList with all tracks currently in the queue
     */
    public ArrayList<Track> getQueList() {
        return queList;
    }

    /**
     * @return Get currently playing track
     */
    public Track getNowPlaying() {
        return nowPlaying;
    }



    //Timer-Control

    /**
     * Set timer duration equals zero when a new track started.
     * Set timer synchronously to the already played duration when a track is paused or resumed.
     * @param duration Duration track already played or zero if a new track started
     * @param start True when a new track started, false when a track get paused or resumed.
     */
    public void setTimer(long duration, boolean start) {
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }
        if(duration - 1000* Constants.CROSSFADE < 0)
            return;
        countDownTimer = new CountDownTimer(duration - 1000* Constants.CROSSFADE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished + 1000* Constants.CROSSFADE;
            }

            @Override
            public void onFinish() {
                next();
            }
        };
        countDownTimer.start();
        if(!start)
            pause();
    }

    /**
     * Pause the currently playing track
     */
    public void pause() {
        if (countDownTimer != null && !paused) {
            Log.d(TAG, "timer got paused, remaining time: " + remainingTime);
            countDownTimer.cancel();
            pauseTime = remainingTime;
            paused = true;
        }
    }

    /**
     * Resume the currently playing track
     */
    public void resume() {
        if (countDownTimer != null && paused) {
            Log.d(TAG, "timer got resumed, remaining time: " + remainingTime);
            setTimer(remainingTime, true);
            paused = false;
        }
    }



    //Que-Control

    /**
     * Skip to the next track in the queue
     */
    public void next() {
        Log.d(TAG, "next track, tracks remaining in que: " + queList.size());
        if(queList.size() > 0) {
            nowPlaying = queList.remove(0);
            queCallback.playSong(nowPlaying);
            setTimer(nowPlaying.getDuration(), true);
        } else {
            playlistEnded = true;
            queCallback.stopPlayback();
        }
    }

    /**
     * Play last track when the currently playing track was played less then two seconds or go back
     * to start of the track when it is played at least two seconds
     * @param lastTrack Las {@link Track} in the queue
     */
    public void back(Track lastTrack) {
        Log.d(TAG, "last track, tracks remaining in que " + queList.size());
        queList.add(0, nowPlaying);
        if(nowPlaying.getDuration()-remainingTime < 2000)
            queList.add(0, lastTrack);
        next();
    }

    /**
     * Add track to the end of the queue
     * @param track {@link Track} to add to queue
     */
    public void addItem(Track track) {
        this.queList.add(track);
    }

    /**
     * Remove track from the queue at a given position
     * @param index Position of the track which will be removed
     */
    public void remove(int index) {
        queList.remove(index);
    }

    /**
     * Clear playlist when the host starts another playlist from Spotify-App or from
     * {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}
     */
    public void clear() {
        queList.clear();
    }

    /**
     * @return Get current size of the queue.
     */
    public int size() {
        return queList.size();
    }
}
