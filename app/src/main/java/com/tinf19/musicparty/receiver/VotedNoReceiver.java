package com.tinf19.musicparty.receiver;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinf19.musicparty.server.HostService;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.HostVoting;

public class VotedNoReceiver extends BroadcastReceiver {

    public static final String TAG = VotedNoReceiver.class.getName();
    private static VotedNoCallback votedNoCallback;

    public interface VotedNoCallback {
        void notificationVotedNo(int id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "User voted yes in the notification  on: " + intent.getIntExtra(Constants.ID, 0));
        if (votedNoCallback != null)
            votedNoCallback.notificationVotedNo(intent.getIntExtra(Constants.ID, 0));
    }

    public static void registerCallback(VotedNoCallback callback) {
        votedNoCallback = callback;
    }
}