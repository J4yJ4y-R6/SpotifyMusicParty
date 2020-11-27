package com.tinf19.musicparty.music;

import android.os.CountDownTimer;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import java.util.ArrayList;

public class Que {

    public interface CountDownCallback {
        void playSong(Track track);
        void setProgressBar(long timeRemaining);
        void stopPlayback();
    }

    private final CountDownCallback countDownCallback;
    private ArrayList<Track> queList = new ArrayList<>();
    public CountDownTimer countDownTimer;
    private Track nowPlaying;
    private boolean playlistEnded;
    private long remainingTime;
    private long pauseTime;
    private boolean paused;

    public Que(CountDownCallback countDownCallback) {
        this.countDownCallback = countDownCallback;
    }

    public void next() {
        Log.d(Que.class.getName(), "next: " + queList.size());
        if(queList.size() > 0) {
            nowPlaying = queList.remove(0);
            countDownCallback.playSong(nowPlaying);
            setTimer(nowPlaying.getDuration(), true);
        } else {
            Log.d(Que.class.getName(), "next: Stopping " +queList.size());
            playlistEnded = true;
            countDownCallback.stopPlayback();
        }
    }

    public void setTimer(long duration, boolean start) {
       /* if(countDownTimer != null) {
            Log.d(Que.class.getName(), "setTImer: " + this);
            countDownTimer.cancel();
        }
        if(duration - 1000* Constants.CROSSFADE < 0)
            return;
        countDownTimer = new CountDownTimer(duration - 1000* Constants.CROSSFADE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countDownCallback.setProgressBar(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                Log.d(Que.class.getName(), "onFinish: " + Que.this);
                next();
            }
        };
        countDownTimer.start();
        if(!start)
            countDownTimer.pause();*/
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


    public void setQueList(ArrayList<Track> queList) {
        this.queList = queList;
    }

    public ArrayList<Track> getQueList() {
        return queList;
    }

    public void pause() {
        if (countDownTimer != null && !paused) {
            countDownTimer.cancel();
            pauseTime = remainingTime;
            paused = true;
        }
    }

    public void resume() {
        if (countDownTimer != null && paused) {
            setTimer(remainingTime, true);
            paused = false;
        }
    }

    public void addItem(Track track) {
        this.queList.add(track);
    }

    public void clear() {
        queList.clear();
    }

    public int size() {
        return queList.size();
    }

    public void remove(int index) {
        queList.remove(index);
    }

    public Track getNowPlaying() {
        return nowPlaying;
    }

    public boolean isPlaylistEnded() {
        return playlistEnded;
    }

    public void setPlaylistEnded(boolean playlistEnded) {
        this.playlistEnded = playlistEnded;
    }

    public void back(Track lastTrack) {
        Log.d(Que.class.getName(), "back: " + nowPlaying.getName() + " " + (nowPlaying.getDuration()-remainingTime));
        queList.add(0, nowPlaying);
        if(nowPlaying.getDuration()-remainingTime < 2000)
            queList.add(0, lastTrack);
        next();
    }
}
