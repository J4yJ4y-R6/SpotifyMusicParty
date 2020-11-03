package com.tinf19.musicparty.client;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.tinf19.musicparty.App.CHANNEL_ID;

public class ClientService extends Service {


    private static final String NAME = ClientService.class.getName();
    private static final int PORT = 1403;
    private static final short LOADING_TIME = 5;
    private boolean stopped;
    private final IBinder mBinder = new LocalBinder();
    private ClientThread clientThread;
    private Socket clientSocket;
    private boolean first = true;
    private List<Track> queue = new ArrayList<>();
    private PartyCallback partyCallback;
    private Track nowPlaying;

    public interface PartyCallback {
        void setTrack(Track track);
        void setPartyName(String partyName);
        void exitService(String text);
        void setPlaylist(List<Track> trackList);
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

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(getString(R.string.service_clientMsg))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(first) connect(intent.getStringExtra(Constants.ADDRESS), intent.getStringExtra(Constants.PASSWORD), intent.getStringExtra(Constants.USERNAME));
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
            Log.d(NAME, String.format("~%s~%s\n\r" , commands.toString(), message));
            out.writeBytes(String.format("~%s~%s\n\r" , commands.toString(), message));
            out.flush();
        }

        @Override
        public void run() {
            try {
                Log.d(NAME, "Try to login to " + address + ":" + PORT + " with password " + this.password);
                new Thread(() -> {
                    try {
                        Thread.sleep(LOADING_TIME*1000);
                    } catch (InterruptedException e) {
                        Log.e(NAME, e.getMessage(), e);
                    }
                    if(clientSocket == null) partyCallback.exitService(getString(R.string.service_clientConnectionError));
                }).start();
                clientSocket = new Socket(this.address, PORT);
                new Thread(() -> {
                    while(partyCallback == null);
                    partyCallback.showFragments();
                }).start();
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
                out = new DataOutputStream(clientSocket.getOutputStream());
                sendMessage(Commands.LOGIN, this.username + "~" + this.password);
                Log.d(NAME, "Connect successful");
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
                                    Log.d(NAME, partyName);
                                    if(partyCallback != null) {
                                        partyCallback.setPartyName(partyName);
                                        partyCallback.setTrack(nowPlaying);
                                    }
                                    break;
                                case QUIT:
                                    Log.d(NAME, "Server has been closed");
                                    exit();
                                    return;
                                case QUEUE:
                                    queue.add(new Track(attribute));
                                    Log.d(NAME, attribute);
                                    break;
                                case PLAYING:
                                     Log.d(NAME, "Playing: " + attribute);
                                     nowPlaying = new Track(attribute);
                                     partyCallback.setTrack(nowPlaying);
                                     break;
                                case PLAYLIST:
                                    Log.d(NAME, "Show Playlist");
                                    List<Track> tracks = new ArrayList<>();
                                    for (int i = 2; i < parts.length; i++) {
                                        if(!parts[i].equals(""))
                                        tracks.add(new Track(parts[i]));
                                    }
                                    partyCallback.setPlaylist(tracks);
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                Log.e(NAME, e.getMessage(), e);
                stopped = true;
            }
        }
    }
}