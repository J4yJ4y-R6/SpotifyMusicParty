package com.tinf19.musicparty.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

public class VotedReceiver extends BroadcastReceiver {

    public static final String TAG = VotedReceiver.class.getName();
    private static VotedCallback votedCallback;

    public interface VotedCallback {
        void notificationVotedYes(int id);
        void notificationVotedIgnored(int id);
        void notificationVotedNo(int id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "User voted yes in the notification  on: "+ intent.getIntExtra(Constants.ID, 0));
        switch (intent.getStringExtra(Constants.VOTE)) {
            case Constants.YES_VOTE:
                if(votedCallback != null)
                    votedCallback.notificationVotedYes(intent.getIntExtra(Constants.ID, 0));
                break;
            case Constants.NO_VOTE:
                if(votedCallback != null)
                    votedCallback.notificationVotedNo(intent.getIntExtra(Constants.ID, 0));
                break;
            case Constants.GREY_VOTE:
                if(votedCallback != null)
                    votedCallback.notificationVotedIgnored(intent.getIntExtra(Constants.ID, 0));
                break;
        }
    }

    public static void registerCallback(VotedCallback callback) {
        votedCallback = callback;
    }
}
