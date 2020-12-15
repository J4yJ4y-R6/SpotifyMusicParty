package com.tinf19.musicparty.receiver;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinf19.musicparty.server.HostService;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.HostVoting;

public class VotedYesReceiver extends BroadcastReceiver {

    public static final String TAG = VotedYesReceiver.class.getName();
    private static VotedYesCallback votedYesCallback;

    public interface VotedYesCallback {
        void notificationVotedYes(int id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "User voted yes in the notification  on: "+ intent.getIntExtra(Constants.ID, 0));
        if(votedYesCallback != null)
            votedYesCallback.notificationVotedYes(intent.getIntExtra(Constants.ID, 0));
    }

    public static void registerCallback(VotedYesCallback callback) {
        votedYesCallback = callback;
    }
}
