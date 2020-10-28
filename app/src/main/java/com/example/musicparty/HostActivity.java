package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.IpSecManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.util.Log;

import com.example.musicparty.databinding.ActivityHostBinding;
import com.example.musicparty.databinding.ActivityMainBinding;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class HostActivity extends AppCompatActivity {

    private static final String NAME = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final String PASSWORD = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
    private static final int PORT = 1403;
    Thread serverThread = null;
    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    private SpotifyAppRemote mSpotifyAppRemote;
    private ActivityHostBinding binding;
    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        binding = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvIpAddress.setText("176.199.209.83");
        binding.tvPassword.setText(PASSWORD);
        startServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d(NAME, "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        //connected();
                        mSpotifyAppRemote.getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(playerState -> {
                                    final Track track = playerState.track;
                                    if (track != null) {
                                        Log.d(NAME, track.name + " by " + track.artist.name);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(NAME, throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void connected() {
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        Log.d(NAME, track.name + " by " + track.artist.name);
                    }
                });
    }

    private void startServer(){
        Log.d(NAME, "Try to start server");
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d(NAME, "Server Started on port " + PORT);
            } catch (IOException e) {
                Log.e(NAME, e.getMessage(), e);
            }
            if(null != serverSocket){
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        CommunicationThread commThread = new CommunicationThread(serverSocket.accept());
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        Log.e(NAME, e.getMessage(), e);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedReader input;
        private DataOutputStream out;
        private boolean login = false;

        public CommunicationThread(Socket socket) {
            Log.d(NAME, "New client request");
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                Log.e(NAME, e.getMessage(), e);
                return;
            }
            String line;
            while (true) {
                try {
                    line = input.readLine();
                    if (line != null){
                        String [] parts = line.split("~");
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            String attribute = "";
                            if (parts.length > 2)
                                attribute = parts[2];
                            switch (command) {
                                case QUIT:
                                    clientSocket.close();
                                    return;
                                case LOGIN:
                                    Log.d(NAME, "New login attempt with password: " + attribute);
                                    if (login(attribute))
                                        out.writeBytes("~LOGIN~Successful\n\n");
                                    else
                                        out.writeBytes("~LOGIN~Failed\n\n");
                                    out.flush();
                                    break;
                                case QUE:
                                    if(this.login)
                                        mSpotifyAppRemote.getPlayerApi().play(attribute);
                                    break;
                                default:
                                    Log.d(NAME, "No such command: " + command);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(NAME, e.getMessage(), e);
                    return;
                }
            }
        }

        private boolean login(String password) {
            if(password.equals(PASSWORD))
                login = true;
            return login;
        }
    }
}