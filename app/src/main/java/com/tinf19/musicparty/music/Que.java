package com.tinf19.musicparty.music;

import android.util.Log;

import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.CountDownTimer;

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

    public Que(CountDownCallback countDownCallback) {
        this.countDownCallback = countDownCallback;
    }

    public void next() {
        if(queList.size() > 0) {
            nowPlaying = queList.remove(0);
            countDownCallback.playSong(nowPlaying);
            setTimer(nowPlaying.getDuration(), true);
        } else
            countDownCallback.stopPlayback();
    }

    public void setTimer(long duration, boolean start) {
        if(countDownTimer != null)
            countDownTimer.cancel();
        if(duration - 1000* Constants.CROSSFADE < 0)
            return;
        countDownTimer = new CountDownTimer(duration - 1000* Constants.CROSSFADE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countDownCallback.setProgressBar(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                Log.d(Que.class.getName(), "onFinish: ");
                next();
            }
        };
        countDownTimer.start();
        if(!start)
            countDownTimer.pause();
    }


    public void setQueList(ArrayList<Track> queList) {
        this.queList = queList;
    }

    public ArrayList<Track> getQueList() {
        return queList;
    }

    public void pause() {
        if (countDownTimer != null)
            countDownTimer.pause();
    }

    public void resume() {
        if (countDownTimer != null)
            countDownTimer.resume();
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

    public void back(Track lastTrack) {
        Log.d(Que.class.getName(), "back: " + countDownTimer.getPassedTime());
        queList.add(0, nowPlaying);
        if(countDownTimer.getPassedTime() < 2000)
            queList.add(0, lastTrack);
        next();
    }
}
