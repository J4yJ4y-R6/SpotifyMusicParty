package com.tinf19.musicparty.client;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.tinf19.musicparty.util.ActionReceiver;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.TokenRefresh;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClientService extends Service {

    private static final String TAG = ClientService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private PartyCallback partyCallback;
    private ClientThread clientThread;
    private Thread tokenRefresh;
    private Socket clientSocket;
    private Track nowPlaying;
    private List<Track> queue = new ArrayList<>();
    private boolean stopped;
    private boolean first = true;
    private String token;


    public interface PartyCallback {
        void setTrack(Track track);
        void setPartyName(String partyName);
        void exitService(String text);
        void setPlaylist(List<Track> trackList);
        void setCurrentTrack(Track track);
        void showFragments();
    }

    public class LocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    public ClientService() {
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setPartyCallback(PartyCallback partyCallback) {
        this.partyCallback = partyCallback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, PartyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Intent intentAction = new Intent(this, ActionReceiver.class);
        PendingIntent pendingIntentButton = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);


        //TODO: Service Text später Namen hinzufügen
        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(getString(R.string.service_clientMsg, "MusicParty"))
                .setSmallIcon(R.drawable.ic_service_notification_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_exit_button, getString(R.string.text_leave),pendingIntentButton)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(first) {
            tokenRefresh = new Thread(new TokenRefresh(intent.getStringExtra(Constants.CODE), new TokenRefresh.TokenCallback() {
                @Override
                public void afterConnection(String token) {
                    Log.d(TAG, "afterConnection: Token has been gained");
                    ClientService.this.token = token;
                    connect(intent.getStringExtra(Constants.ADDRESS), intent.getStringExtra(Constants.PASSWORD), intent.getStringExtra(Constants.USERNAME));
                }

                @Override
                public void afterRefresh(String token) {
                    Log.d(TAG, "afterRefresh: Token has been refreshed");
                    ClientService.this.token = token;
                }
            }));
            tokenRefresh.start();
            first = false;
        }
        //password = intent.getStringExtra("password");

        return START_NOT_STICKY;
    }

    public void connect(String ipAddress, String password, String username){
        clientThread = new ClientThread(ipAddress, password, username);
        clientThread.start();
    }

    public ClientThread getClientThread() {
        return clientThread;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void exit() throws IOException {
        stopped = true;
        clientThread.out.close();
        clientThread.input.close();
        clientSocket.close();
        partyCallback.exitService(getString(R.string.service_serverClosed));
    }

    public void setTrack() {
        new Thread(()->{
            if(nowPlaying != null)
                partyCallback.setTrack(nowPlaying);
        }).start();
    }

    public String getToken() {
        return token;
    }

    class ClientThread extends Thread {

        private String address;
        private BufferedReader input;
        private DataOutputStream out;
        private String password;
        private String line;
        private String username;
        private String partyName;


        public ClientThread(String address, String password, String username) {
            this.address = address;
            this.password = password;
            this.username = username;
        }

        public String getPartyName() {
            return partyName;
        }

        public void sendMessage(Commands commands, String message) throws IOException {
            Log.d(TAG, String.format("~%s~%s\n\r" , commands.toString(), message));
            out.writeBytes(String.format("~%s~%s\n\r" , commands.toString(), message));
            out.flush();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "Try to login to " + address + ":" + Constants.PORT + " with password " + this.password);
                new Thread(() -> {
                    try {
                        Thread.sleep(Constants.LOADING_TIME*1000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                    if(clientSocket == null) partyCallback.exitService(getString(R.string.service_clientConnectionError));
                }).start();
                clientSocket = new Socket(this.address, Constants.PORT);
                new Thread(() -> {
                    while(partyCallback == null);
                    partyCallback.showFragments();
                }).start();
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
                out = new DataOutputStream(clientSocket.getOutputStream());
                sendMessage(Commands.LOGIN, this.username + "~" + this.password);
                Log.d(TAG, "Connect successful");
                while (!this.isInterrupted() && !clientSocket.isClosed())  {
                    line = input.readLine();
                    if (line != null) {
                        String [] parts = line.split("~");
                        String attribute = "";
                        if (parts.length > 2)
                            attribute = parts[2];
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            switch (command) {
                                case LOGIN:
                                    partyName = attribute;
                                    if (parts.length > 3) {
                                        nowPlaying = new Track(parts[3]);
                                    }
                                    Log.d(TAG, partyName);
                                    if(partyCallback != null) {
                                        partyCallback.setPartyName(partyName);
                                    }
                                    break;
                                case QUIT:
                                    Log.d(TAG, "Server has been closed");
                                    exit();
                                    return;
                                case QUEUE:
                                    queue.add(new Track(attribute));
                                    Log.d(TAG, attribute);
                                    break;
                                case PLAYING:
                                     Log.d(TAG, "Playing: " + attribute);
                                     nowPlaying = new Track(attribute);
                                     partyCallback.setTrack(nowPlaying);
                                     break;
                                case PLAYLIST:
                                    List<Track> tracks = new ArrayList<>();
                                    for (int i = 3; i < parts.length; i++) {
                                        if(!parts[i].equals(""))
                                        tracks.add(new Track(parts[i]));
                                    }
                                    Log.d(TAG, tracks.get(0).getName());
                                    partyCallback.setCurrentTrack(tracks.get(0));
                                    tracks.remove(0);
                                    partyCallback.setPlaylist(tracks);
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                stopped = true;
            }
        }
    }
}