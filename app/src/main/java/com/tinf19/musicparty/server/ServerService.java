package com.tinf19.musicparty.server;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.util.ActionReceiver;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.tinf19.musicparty.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

import static com.tinf19.musicparty.App.CHANNEL_ID;

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
    private String partyName;
    private List<Track> tracks = new ArrayList<>();
    private List<Track> playlist = new ArrayList<>();
    private ServerService mBoundService;
    private SpotifyAppRemote mSpotifyAppRemote;
    private boolean pause = true;
    private SpotifyPlayerCallback spotifyPlayerCallback;
    private  com.spotify.protocol.types.Track nowPlaying;
    private com.spotify.protocol.types.Track lastSongTitle;
    private boolean stopped;

    public interface SpotifyPlayerCallback {
        void setNowPlaying(Track nowPlaying);
        void setPeopleCount(int count);
        void setPlayImage(boolean pause);
    }

    public interface AfterDeleteCallback {
        void deleteFromDataset();
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

        token = intent.getStringExtra(Constants.TOKEN);
        password = intent.getStringExtra(Constants.PASSWORD);
        partyName = intent.getStringExtra(Constants.PARTYNAME);
        Log.d(NAME, "partyName: " + partyName);
        if (first) {
            getUserID();
            first = false;
        }

        Intent notificationIntent = new Intent(this, HostActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Intent intentAction = new Intent(this, ActionReceiver.class);
        PendingIntent pendingIntentButton = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_name))
                .setContentText(getString(R.string.service_serverMsg))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_exit_button, getString(R.string.text_end),pendingIntentButton)
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
        return ( mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) ? mSpotifyAppRemote : null;
    }

    public void setmSpotifyAppRemote(SpotifyAppRemote mSpotifyAppRemote) {
        this.mSpotifyAppRemote = mSpotifyAppRemote;
    }
    
    public List<PartyPeople> getPeopleList() {
        List<PartyPeople> tmpPeopleList = new ArrayList<>();
        for (CommunicationThread client: clientThreads) {
            tmpPeopleList.add(new PartyPeople(client.username, System.currentTimeMillis() - client.createdTime));
        }
        return tmpPeopleList;
    }

    public int getClientListSize() {
        return clientThreads.size();
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
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
                response.close();
            }
        });
    }

    public List<Track> getPlaylist() {
        return tracks;
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
                .put("description", getString(R.string.service_playlistDescription));
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
                    playlist.add(new Track("600HVBpzF1WfBdaRwbEvLz", "Frozen", new Artist[]{new Artist("tsads", "Disney")}, "test", 0, "Disner"));
                    addItem("spotify:track:600HVBpzF1WfBdaRwbEvLz", "Frozen");
                    //addItem("spotify:track:76nqCfJOcFFWBJN32PAksn", "Kings and Queens");
                } catch (JSONException e) {
                    Log.e(NAME, e.getMessage(), e);
                }
                response.close();
            }
        });
    }

    private void repeatMode(String state) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("me")
                .addPathSegment("player")
                .addPathSegment("repeat")
                .addQueryParameter("state", state)
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        RequestBody body = RequestBody.create(new byte[]{}, null);
        Request request = new Request.Builder()
                .url(completeURL)
                .put(body)
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
                    Log.d(NAME,"Request Successful. Repeat mode has set to " + state);
                }
                response.close();
            }
        });
    }

    private void getQueFromPlaylist() {

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
                response.close();
            }
        });
    }

    public void addItem(String uri, String name) throws JSONException {
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
        //String [] uris = {uri};
        //JSONObject sampleObject = new JSONObject()
        //        .put("uris", uris);
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
//                    try {
//                        deleteItem("spotify:track:600HVBpzF1WfBdaRwbEvLz", "Frozen", 0);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                    if (size == 1) {
                        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:" + playlistID);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(pause);
                                repeatMode("context");
                            }
                        }).start();
                    }
                }
                response.close();
            }
        });
    }

    public void deleteItem(String uri, String name, int position, AfterDeleteCallback callback) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .build();
        int index = size - tracks.size() + position;
        if(index < 0 || index >= size)
            return;
        JSONObject uris = new JSONObject()
                .put("uri", uri)
                .put("positions", new JSONArray().put(index));
        ;
        JSONObject sampleObject = new JSONObject()
               .put("tracks", new JSONArray().put(uris));
        RequestBody body = RequestBody.create(sampleObject.toString(), JSON);
        //RequestBody body = RequestBody.create(new byte[]{}, null);
        Log.d(NAME, "Try to delete track " + name);
        Log.d(NAME, "Making request to " + completeURL.toString());
        Log.d(NAME, "JSON Body: " +  sampleObject.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .delete(body)
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
                    Log.d(NAME,"Request Successful. Track " + name + " has been deleted.");
                    callback.deleteFromDataset();
                    playlist.remove(index);
                    tracks.remove(position);
                    size--;
                }
                response.close();
            }
        });
    }

    public void moveItem(int from, int to) throws JSONException {
        int position = size - tracks.size();
        Log.d(NAME, "moveItem: From " + from + " To: " + to + " Position: " + position);
        from = from + position;
        to = to + position;
        if (from < to) to++;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        JSONObject sampleObject = new JSONObject()
                .put("range_start", from)
                .put("insert_before", to);
        RequestBody body = RequestBody.create(sampleObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(completeURL)
                .put(body)
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
                    Log.d(NAME,"Request Successful. Track moved.");
                }
                response.close();
            }
        });
    }

    public void updatePlaylistName(String name) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        JSONObject sampleObject = new JSONObject()
                .put("name", name);
        RequestBody body = RequestBody.create(sampleObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(completeURL)
                .put(body)
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
                    Log.d(NAME,"Request Successful. Playlist name changed.");
                }
                response.close();
            }
        });
    }

    public void addItemToPlaylist(Track track) {
        playlist.add(track);
        tracks.add(track);
    }

    public void addItemToTrackList(Track track) {
        tracks.add(track);
    }

    public void togglePlayback() {
        if (pause && getmSpotifyAppRemote() != null) {
            Log.d(NAME, "Size: " + tracks.size() + " Playlist size: " + playlist.size());
            if(stopped && tracks.size() == 0) {
                lastSongTitle = null;
                getmSpotifyAppRemote().getPlayerApi().play("spotify:playlist:" + playlistID);
                tracks = new ArrayList<>(playlist);
            } else if(stopped) {
                getmSpotifyAppRemote().getPlayerApi().skipNext();
            } else {
                getmSpotifyAppRemote().getPlayerApi().resume();
            }
        }
        else if (getmSpotifyAppRemote() != null)
            getmSpotifyAppRemote().getPlayerApi().pause();
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
            if (client.login)
                client.sendMessage(command, message);
        }
    }

    public void addEventListener() {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final com.spotify.protocol.types.Track track = playerState.track;
                    nowPlaying = track;
                    if(lastSongTitle == null || (nowPlaying != null && !nowPlaying.name.equals(lastSongTitle.name))) {
                        if(tracks.size() == 0 && lastSongTitle != null && !stopped) {
                            stopped = true;
                            Log.d(NAME, "Playlist hast ended " + lastSongTitle.name + " Duration: " + lastSongTitle.duration);
                            mSpotifyAppRemote.getPlayerApi().skipPrevious();
                            mSpotifyAppRemote.getPlayerApi().pause();
                            pause = true;
//                            long postion = lastSongTitle.duration - 10000;
//                            Log.d(NAME, "SEEK TO: " + postion);
//                            mSpotifyAppRemote.getPlayerApi().seekTo(postion);
                            if(spotifyPlayerCallback != null)
                                spotifyPlayerCallback.setPlayImage(true);
                            return;
                        } else if(tracks.size() == 0 && lastSongTitle != null) {
                            return;
                        }
                        lastSongTitle = nowPlaying;
                        Log.d(NAME, "New song has been started " + track.uri.split(":")[2]);
                        stopped = false;
                        new Thread(()->{
                            try {
                                sendToAll(Commands.PLAYING, getNowPlaying().serialize());
                            } catch (IOException | JSONException e) {
                                Log.e(NAME, e.getMessage(), e);
                            }
                        }).start();
                        if(tracks.size() > 0 && tracks.get(0).getURI().equals(nowPlaying.uri)) {
                            tracks.remove(0);
                        }
                    }
                    pause = playerState.isPaused;
                    if (track != null && spotifyPlayerCallback != null) {
                        //Log.d(NAME, track.name + " by " + track.artist.name);
                        //if (playerState.playbackPosition == 0)
                        //nextSong();
                        spotifyPlayerCallback.setNowPlaying(getNowPlaying());
                    }
                });
    }

    public void setSpotifyPlayerCallback(SpotifyPlayerCallback spotifyPlayerCallback) {
        this.spotifyPlayerCallback = spotifyPlayerCallback;
    }

    private void startServer(){
        Log.d(NAME, "Try to start server");
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    public Track getNowPlaying(){
        return new Track(
                nowPlaying.uri.split(":")[2],
                nowPlaying.name,
                nowPlaying.artists,
                nowPlaying.imageUri.raw.split(":")[2],
                nowPlaying.duration,
                nowPlaying.album.name
        );
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
        private final long createdTime;

        public CommunicationThread(Socket socket) {
            Log.d(NAME, "New client request");
            this.clientSocket = socket;
            this.createdTime = System.currentTimeMillis();
        }

        public void sendMessage(Commands command, String message) throws IOException {
            Log.d(NAME, "~" + command.toString() + "~" + message);
            out.writeBytes("~" + command.toString() + "~" + message + "\n\r");
            out.flush();
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
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
                                    clientThreads.remove(this);
                                    if(spotifyPlayerCallback!= null) spotifyPlayerCallback.setPeopleCount(clientThreads.size());
                                    Log.d(NAME, "User " + username + " has left the party");
                                    close();
                                    return;
                                case LOGIN:
                                    if (parts.length > 3) {
                                        String pass = parts[3];
                                        Log.d(NAME, "New login attempt from user " + attribute +" with password: " + pass);
                                        if (login(pass)) {
                                            username = attribute;
                                            if (nowPlaying == null)
                                                sendMessage(Commands.LOGIN, partyName);
                                            else
                                                sendMessage(Commands.LOGIN, partyName + "~" + getNowPlaying().serialize());
                                            if(spotifyPlayerCallback!= null) spotifyPlayerCallback.setPeopleCount(clientThreads.size());
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
                                        addItemToPlaylist(track);
                                        addItem(track.getURI(), track.getName());
                                        //sendToAll(Commands.QUEUE, track.serialize());
                                    }
                                    break;
                                case PLAYING:
                                    if(nowPlaying != null) {
                                        sendMessage(Commands.PLAYING, getNowPlaying().serialize());
                                    }
                                    break;
                                case PLAYLIST:
                                    Log.d(NAME, "Show Playlist for user " + username);
                                    StringBuilder response = new StringBuilder();
                                    if(nowPlaying != null) {
                                        response.append("~");
                                        response.append(getNowPlaying().serialize());
                                    }
                                    for (Track track: tracks) {
                                        response.append("~");
                                        response.append(track.serialize());
                                    }
                                    sendMessage(Commands.PLAYLIST, response.toString());
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