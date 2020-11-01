package com.example.musicparty;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.musicparty.music.Track;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.musicparty.App.CHANNEL_ID;

public class ServerService extends Service {

    private static final String NAME = ServerService.class.getName();
    private static final int PORT = 1403;
    private static final String HOST = "api.spotify.com";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private final IBinder mBinder = new LocalBinder();
    private String password;
    Thread serverThread = null;
    private ServerSocket serverSocket;
    private List<CommunicationThread> clientThreads = new ArrayList<>();
    private Socket tempClientSocket;
    private String userID;
    private String token;
    private String playlistID;
    private int size = 0;
    private boolean first = true;
    private String partyName = "Coole Party";
    private List<Track> tracks = new ArrayList<>();
    private ServerService mBoundService;
    private SpotifyAppRemote mSpotifyAppRemote;
    private boolean pause;

    public interface SpotifyPlayerCallback {
        void setNowPlaying(String nowPlaying);
    }

    public class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        token = intent.getStringExtra("token");
        password = intent.getStringExtra("password");
        if (first) {
            getUserID();
            first = false;
        }

        Intent notificationIntent = new Intent(this, HostActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Party")
                .setContentText("A music party is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(ServerService.class.getName(), "I have been destroyed " + clientThreads.size());
        deletePlaylist();
        new Thread(() -> {
            try {
                serverSocket.close();
                stopAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        serverThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public SpotifyAppRemote getmSpotifyAppRemote() {
        return mSpotifyAppRemote;
    }

    public void setmSpotifyAppRemote(SpotifyAppRemote mSpotifyAppRemote) {
        this.mSpotifyAppRemote = mSpotifyAppRemote;
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
                    Log.d(NAME, response.body().string());
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

    public boolean getPause() {
        return pause;
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
                    Log.d(NAME,"Request Successful. Track " + name + " has been added.");
                    size++;
                    if (size == 1) mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:" + playlistID);
                }
            }
        });
    }

    private void stopAll() throws IOException {
        Log.d(NAME, "Stopping server");
        for(CommunicationThread client : clientThreads) {
            client.sendMessage(Commands.QUIT, "Session has been closed");
            client.close();
        }
    }

    public void sendToAll(Commands command, String message) throws IOException {
        for(CommunicationThread client : clientThreads) {
            client.sendMessage(command, message);
        }
    }

    public void addEventListener(SpotifyPlayerCallback spotifyPlayerCallback) {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final com.spotify.protocol.types.Track track = playerState.track;
                    if(playerState.playbackPosition == 0) {
                        Log.d(NAME, "New song has been started");
                        new Thread(()->{
                            try {
                                sendToAll(Commands.PLAYING, new com.example.musicparty.music.Track(
                                        track.uri.split(":")[2],
                                        track.name,
                                        track.artists,
                                        track.imageUri.raw,
                                        track.duration,
                                        track.album.name
                                ).serialize());
                            } catch (IOException | JSONException e) {
                                Log.e(NAME, e.getMessage(), e);
                            }
                        }).start();
                    }
                    pause = playerState.isPaused;
                    if (track != null) {
                        //Log.d(NAME, track.name + " by " + track.artist.name);
                        //if (playerState.playbackPosition == 0)
                        //nextSong();
                        spotifyPlayerCallback.setNowPlaying(String.format("%s by %s", track.name, track.artist.name));
                    }
                });
    }

    private void startServer(){
        Log.d(NAME, "Try to start server");
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
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
                        clientThreads.add(new CommunicationThread(serverSocket.accept()));
                        clientThreads.get(clientThreads.size()-1).start();
                    } catch (IOException e) {
                        Log.e(NAME, e.getMessage(), e);
                    }
                }
            }
        }
    }

    class CommunicationThread extends Thread {

        private Socket clientSocket;
        private BufferedReader input;
        private DataOutputStream out;
        private boolean login = false;
        private String username;

        public CommunicationThread(Socket socket) {
            Log.d(NAME, "New client request");
            this.clientSocket = socket;
        }

        public void sendMessage(Commands command, String message) throws IOException {
            out.writeBytes("~" + command.toString() + "~" + message + "\n\r");
            out.flush();
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
                                    close();
                                    return;
                                case LOGIN:
                                    if (parts.length > 3) {
                                        String pass = parts[3];
                                        Log.d(NAME, "New login attempt from user " + attribute +" with password: " + pass);
                                        if (login(pass)) {
                                            username = attribute;
                                            sendMessage(Commands.LOGIN, partyName);
                                        } else {
                                            sendMessage(Commands.QUIT, "Login Failed");
                                            close();
                                            return;
                                        }
                                    }
                                    break;
                                case QUEUE:
                                    Log.d(NAME, "Added " + attribute + " to the queue");
                                    if(this.login) {
                                        Track track = new Track(attribute);
                                        tracks.add(track);
                                        addItem(track.getURI(), track.getName());
                                        sendToAll(Commands.QUEUE, track.serialize());
                                    }
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

        private void close() throws IOException {
            out.close();
            input.close();
            clientSocket.close();
        }

        private boolean login(String input) {
            if(input.equals(password))
                login = true;
            return login;
        }
    }
}