package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.musicparty.databinding.ActivityHostBinding;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HostActivity extends AppCompatActivity {

    private static final String NAME = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final String HOST = "api.spotify.com";
    private static final String PASSWORD = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
    private static final int PORT = 1403;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    Thread serverThread = null;
    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    private SpotifyAppRemote mSpotifyAppRemote;
    private ActivityHostBinding binding;
    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean pause;
    private String userID;
    private String token;
    private String playlistID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Que.getInstance().add(new com.example.musicparty.music.Track("3cfOd4CMv2snFaKAnMdnvK"));
        //Que.getInstance().add(new com.example.musicparty.music.Track("76nqCfJOcFFWBJN32PAksn"));

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        token = getIntent().getStringExtra("token");
        binding = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvIpAddress.setText(getIPAddress(true));
        binding.tvPassword.setText(PASSWORD);
        startServer();
        getUserID();
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(NAME, "I got destroyed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        HostActivity hostActivity = this;
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
                        //mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");

                        // Now you can start interacting with App Remote
                        //connected();
                        mSpotifyAppRemote.getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(playerState -> {
                                    final Track track = playerState.track;
                                    pause = playerState.isPaused;
                                    if (track != null) {
                                        Log.d(NAME, track.name + " by " + track.artist.name);
                                        //if (playerState.playbackPosition == 0)
                                            //nextSong();
                                        hostActivity.runOnUiThread(() -> {
                                            // Display requested url data as string into text view
                                            binding.tvPlaying.setText(String.format("%s by %s", track.name, track.artist.name));
                                        });
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

    private void getUserID() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("me")
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful. Got the username of the user.");
                }

                // Read data in the worker thread
                final String data = response.body().string();
                try {
                    userID = new JSONObject(data).getString("id");
                    Log.d(NAME, "UserID: " + userID);
                    createPlaylist("MusicParty");
                } catch (JSONException e) {
                    Log.e(NAME, e.getMessage(), e);
                }
            }
        });
    }

    private void createPlaylist(String name) throws JSONException {
        if (userID == null ) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment(userID)
                .addPathSegment("playlists")
                .build();
        JSONObject sampleObject = new JSONObject()
                .put("name", name)
                .put("public", false)
                .put("description", "A playlist for the MusicParty app.");
        RequestBody body = RequestBody.create(sampleObject.toString(), JSON);
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        Log.d(NAME, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(NAME, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful. New playlist has been created.");
                }
                String [] uri = response.header("Location").split("/");
                playlistID = uri[uri.length-1];
                Log.d(NAME, playlistID);
                try {
                    addItem("spotify:track:600HVBpzF1WfBdaRwbEvLz", "Frozen");
                    addItem("spotify:track:76nqCfJOcFFWBJN32PAksn", "Kings and Queens");
                } catch (JSONException e) {
                    Log.e(NAME, e.getMessage(), e);
                }
            }
        });
    }

    private void deletePlaylist() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("followers")
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(NAME, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful. Playlist has been deleted.");
                }
            }
        });
    }

    private void addItem(String uri, String name) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .addQueryParameter("uris", uri)
                .addQueryParameter("position", "0")
                .build();
        String [] uris = {uri};
        JSONObject sampleObject = new JSONObject()
                .put("uris", uris);
        //RequestBody body = RequestBody.create(sampleObject.toString(), JSON);
        RequestBody body = RequestBody.create(new byte[]{}, null);
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(NAME, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful. Track " + name + " hast been added.");
                    if (pause) mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:" + playlistID);
                }
            }
        });
    }

    public void togglePlay(View view) {
        if (pause) mSpotifyAppRemote.getPlayerApi().resume();
        else mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void nextSong(View view) {
        mSpotifyAppRemote.getPlayerApi().skipNext();
    }

    private String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    private void connected() {
        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:31OUF33qw8gZH6dmkss0Cz");
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
        Log.d(NAME, "I have been stopped");
        Intent serviceIntent = new Intent(this, ServerService.class);
        startService(serviceIntent);
        deletePlaylist();
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
                                    Log.d(NAME, "Added " + attribute + " to the queue");
                                    if(this.login)
                                        addItem("spotify:track:" + attribute, "Test");
                                    break;
                                default:
                                    Log.d(NAME, "No such command: " + command);
                            }
                        }
                    }
                } catch (IOException | JSONException e) {
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