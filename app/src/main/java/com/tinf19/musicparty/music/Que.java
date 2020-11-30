package com.tinf19.musicparty.music;

import android.os.CountDownTimer;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import java.util.ArrayList;

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

    public Que(QueCallback queCallback) {
        this.queCallback = queCallback;
    }



    //Setter

    public void setQueList(ArrayList<Track> queList) {
        this.queList = queList;
    }

    public void setPlaylistEnded(boolean playlistEnded) {
        this.playlistEnded = playlistEnded;
    }



    //Getter

    public boolean isPlaylistEnded() {
        return playlistEnded;
    }

    public ArrayList<Track> getQueList() {
        return queList;
    }

    public Track getNowPlaying() {
        return nowPlaying;
    }



    //Timer-Control

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

    public void pause() {
        if (countDownTimer != null && !paused) {
            Log.d(TAG, "timer got paused, remaining time: " + remainingTime);
            countDownTimer.cancel();
            pauseTime = remainingTime;
            paused = true;
        }
    }

    public void resume() {
        if (countDownTimer != null && paused) {
            Log.d(TAG, "timer got resumed, remaining time: " + remainingTime);
            setTimer(remainingTime, true);
            paused = false;
        }
    }



    //Que-Control

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

    public void back(Track lastTrack) {
        Log.d(TAG, "last track, tracks remaining in que " + queList.size());
        queList.add(0, nowPlaying);
        if(nowPlaying.getDuration()-remainingTime < 2000)
            queList.add(0, lastTrack);
        next();
    }

    public void addItem(Track track) {
        this.queList.add(track);
    }

    public void remove(int index) {
        queList.remove(index);
    }

    public void clear() {
        queList.clear();
    }

    public int size() {
        return queList.size();
    }
}
