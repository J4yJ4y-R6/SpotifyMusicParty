package com.tinf19.musicparty.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tinf19.musicparty.util.Constants;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) { context.sendBroadcast(new Intent(Constants.STOP)); }
}
