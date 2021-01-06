package com.tinf19.musicparty.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinf19.musicparty.util.Constants;

/**
 * The ActionReceiver is a BroadcastReceiver which is listening to the notification button.
 * The action depends on the service.
 * In the {@link com.tinf19.musicparty.server.HostService} the button will end the party and close
 * the connection to all clients.
 * In the {@link com.tinf19.musicparty.client.ClientService} the user leaves the party when he
 * clicks the button.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ActionReceiver.class.getName(), "ActionReceiver opened HostClosePartyFragment");
        context.sendBroadcast(new Intent(Constants.STOP));
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }
}
