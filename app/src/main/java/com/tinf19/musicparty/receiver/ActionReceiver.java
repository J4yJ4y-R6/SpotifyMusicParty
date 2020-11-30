package com.tinf19.musicparty.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

public class ActionReceiver extends BroadcastReceiver {

    private static final String TAG = ActionReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ActionReceiver stopped the connection");
        context.sendBroadcast(new Intent(Constants.STOP));
    }
}
