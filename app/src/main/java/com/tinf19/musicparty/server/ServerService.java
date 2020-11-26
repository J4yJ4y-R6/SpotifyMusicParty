package com.tinf19.musicparty.server;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.music.Que;
import com.tinf19.musicparty.util.ActionReceiver;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.ShowSavedPlaylistRecycAdapter;
import com.tinf19.musicparty.util.TokenRefresh;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServerService extends Service implements Parcelable, Que.CountDownCallback {

    private static final String TAG = ServerService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private String password;
    private Thread serverThread = null;
    private Thread tokenRefresh = null;
    private ServerSocket serverSocket;
    private List<CommunicationThread> clientThreads = new ArrayList<>();
    private String userID;
    private String token;
    private String playlistID;
    private int size = 0;
    private boolean first = true;
    private String partyName;
    //private List<Track> tracks = new ArrayList<>();
    private Que que;
    private List<Track> playlist = new ArrayList<>();
    private SpotifyAppRemote mSpotifyAppRemote;
    private boolean pause = true;
    private boolean newSong;
    private SpotifyPlayerCallback spotifyPlayerCallback;
    private  com.spotify.protocol.types.Track nowPlaying;
    private com.spotify.protocol.types.Track lastSongTitle;
    private boolean stopped;
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentButton;

    @Override
    public void playSong(Track track) {
        if(getmSpotifyAppRemote() != null) {
            mSpotifyAppRemote.getPlayerApi().play(track.getURI());
            Log.d(TAG, "New song has been started: " + track.getName());
            new Thread(()->{
                try {
                    sendToAll(Commands.PLAYING, que.getNowPlaying().serialize());
                } catch (IOException | JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }).start();
        }
    }

    @Override
    public void setProgressBar(long timeRemaining) {

    }

    @Override
    public void stopPlayback() {

    }

    public interface SpotifyPlayerCallback {
        void setNowPlaying(Track nowPlaying);
        void setPeopleCount(int count);
        void setPlayImage(boolean pause);
        void showDefault();
        void connect(HostActivity.ConnectionCallback connectionCallback);
        void reloadPlaylistFragment();
    }

    public interface AfterCallback {
        void deleteFromDataset();
    }

    public class LocalBinder extends Binder {
        ServerService getService() { return ServerService.this; }
    }

    // Service - Interaction

    @Override
    public void onCreate() {
        super.onCreate();
        que = new Que(this);
        Log.d(TAG, "Stopped: " + stopped + " Lastsong: " + (lastSongTitle != null ? lastSongTitle.name : "Nichts"));
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        password = intent.getStringExtra(Constants.PASSWORD);
        partyName = intent.getStringExtra(Constants.PARTYNAME);
        if (first) {
            tokenRefresh = new Thread(new TokenRefresh(intent.getStringExtra(Constants.CODE), new TokenRefresh.TokenCallback() {
                @Override
                public void afterConnection(String token) {
                    Log.d(TAG, "afterConnection: Token has been " + token);
                    ServerService.this.token = token;
                    if(spotifyPlayerCallback != null) {
                        HostActivity.ConnectionCallback callback = new HostActivity.ConnectionCallback() {
                            @Override
                            public void afterConnection(SpotifyAppRemote appRemote) {
                                mSpotifyAppRemote = appRemote;
                                addEventListener();
                                Log.d(TAG, "afterConnection: App remote" + (appRemote != null));
                            }

                            @Override
                            public void afterFailure() {
                                if(spotifyPlayerCallback != null) spotifyPlayerCallback.connect(this);
                            }
                        };
                        spotifyPlayerCallback.connect( new HostActivity.ConnectionCallback() {
                            @Override
                            public void afterConnection(SpotifyAppRemote appRemote) {
                                mSpotifyAppRemote = appRemote;
                                Log.d(TAG, "afterConnection: App remote" + (appRemote != null));
                                getUserID();
                                addEventListener();
                                if (spotifyPlayerCallback != null)
                                    spotifyPlayerCallback.showDefault();
                            }

                            @Override
                            public void afterFailure() {
                                if(spotifyPlayerCallback != null) spotifyPlayerCallback.connect(callback);
                            }
                        });
                    }
                }

                @Override
                public void afterRefresh(String token) {
                    Log.d(TAG, "afterRefresh: Token hast been refreshed");
                    ServerService.this.token = token;
                }
            }));
            tokenRefresh.start();
            first = false;
        }

        Intent notificationIntent = new Intent(this, HostActivity.class);
        pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Intent intentAction = new Intent(this, ActionReceiver.class);
        pendingIntentButton = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(getString(R.string.service_serverMsg, partyName))
                .setContentText(getString(R.string.service_serverPeople, clientThreads.size()))
                .setSmallIcon(R.drawable.ic_service_notification_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_exit_button, getString(R.string.text_end),pendingIntentButton)
                .build();
        startForeground(Constants.NOTIFY_ID, notification);

        Log.d(TAG, "onStartCommand: first notify");

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(ServerService.class.getName(), "I have been destroyed " + clientThreads.size());
        lastSongTitle = null;
        //nowPlaying = null;
        stopped = false;
//        deletePlaylist(playlistID);
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

    public void next() {
        que.next();
    }

    public void back() {
        if(playlist.size() - que.size() - 2 >= 0)
            que.back(playlist.get(playlist.size() - que.size() - 2));
        else
            mSpotifyAppRemote.getPlayerApi().play(nowPlaying.uri);
    }

    // Getter

    public SpotifyAppRemote getmSpotifyAppRemote() { return ( mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) ? mSpotifyAppRemote : null; }
    
    public List<PartyPeople> getPeopleList() {
        List<PartyPeople> tmpPeopleList = new ArrayList<>();
        for (CommunicationThread client: clientThreads) {
            tmpPeopleList.add(new PartyPeople(client.username, System.currentTimeMillis() - client.createdTime));
        }
        return tmpPeopleList;
    }

    public boolean isFirst() {
        return first;
    }

    public int getClientListSize() {
        return clientThreads.size();
    }

    public String getPlaylistID() {
        return playlistID;
    }

    public String getPartyName() {
        return partyName;
    }

    //public List<Track> getTracks() { return tracks;}

    public String getToken() {
        return token;
    }

    public Track getNowPlaying(){
        return nowPlaying != null ? new Track(
                nowPlaying.uri.split(":")[2],
                nowPlaying.name,
                nowPlaying.artists,
                "0",
                nowPlaying.imageUri.raw.split(":")[2],
                nowPlaying.duration,
                nowPlaying.album.name
        ) : null;
    }

    public List<Track> getPlaylist() {
        return que.getQueList();
    }

    public boolean getPause() {
        return pause;
    }



    // Setter

    public void setPlaylistID(String id) { this.playlistID = id; }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public void setSpotifyPlayerCallback(SpotifyPlayerCallback spotifyPlayerCallback) {
        Log.d(TAG, "setSpotifyPlayerCallback: " + spotifyPlayerCallback);
        this.spotifyPlayerCallback = spotifyPlayerCallback;
    }




    // HTTP Requests

    private void getUserID() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("me")
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Got the username of the user.");
                }

                // Read data in the worker thread
                final String data = response.body().string();
                try {
                    userID = new JSONObject(data).getString("id");
                    Log.d(TAG, "UserID: " + userID);
                    //createPlaylist("MusicParty");
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                response.close();
            }
        });
    }

    private void createPlaylist(String name) throws JSONException {
        if (userID == null ) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment(userID)
                .addPathSegment("playlists")
                .build();
        JSONObject sampleObject = new JSONObject()
                .put("name", name)
                .put("public", false)
                .put("description", getString(R.string.service_playlistDescription, partyName));
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        Log.d(TAG, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        Log.d(TAG, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. New playlist has been created.");
                }
                String [] uri = response.header("Location").split("/");
                playlistID = uri[uri.length-1];
                Log.d(TAG, playlistID);
//                try {
//                    playlist.add(new Track("600HVBpzF1WfBdaRwbEvLz", "Frozen", new Artist[]{new Artist("tsads", "Disney")}, "test", 0, "Disner"));
//                    addItem("spotify:track:600HVBpzF1WfBdaRwbEvLz", "Frozen");
//                } catch (JSONException e) {
//                    Log.e(TAG, e.getMessage(), e);
//                }
                response.close();
            }
        });
    }

    public void getQueFromPlaylist(String id) {
        que.clear();
        playlist.clear();
        getQueFromPlaylist(id, 0);
    }

    private void getQueFromPlaylist(String id, int page) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("tracks")
                .addQueryParameter("offset", String.valueOf(100 * page))
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        Log.d(TAG, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    try {
                        JSONObject body = new JSONObject(response.body().string());
                        JSONArray items = body.getJSONArray("items");
                        int count = body.getInt("total");

                        for(int i = 0; i < items.length(); i++) {
                            JSONObject track = items.getJSONObject(i).getJSONObject("track");
                            JSONArray artists = track.getJSONArray("artists");
                            Artist[] array = new Artist[artists.length()];
                            for(int j = 0; j < array.length; j++) {
                                JSONObject artist = artists.getJSONObject(j);
                                array[j] = new Artist(artist.getString("id"), artist.getString("name"));
                            }
                            String[] image = track
                                    .getJSONObject("album")
                                    .getJSONArray("images")
                                    .getJSONObject(2)
                                    .getString("url").split("/");
                            String[] imageFull = track
                                    .getJSONObject("album")
                                    .getJSONArray("images")
                                    .getJSONObject(1)
                                    .getString("url").split("/");
                            Track tmpTrack =
                                    new Track(
                                            track.getString("id"),
                                            track.getString("name"),
                                            array,
                                            image[image.length-1],
                                            imageFull[imageFull.length-1],
                                            track.getInt("duration_ms"),
                                            track.getJSONObject("album").getString("name"));
                            que.addItem(tmpTrack);
                            playlist.add(tmpTrack);
                        }
                        Log.d(TAG, "onResponse: added " + que.size() + " elements" );
                        if(page == 0 && que.size() > 0)
                            que.next();
                        if(count > 100 * page)
                            getQueFromPlaylist(id, page + 1);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                response.close();
            }
        });
    }

    public void checkPlaylistFollowStatus(String id) throws JSONException {
        String token = getToken();
        if(token == null) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("followers")
                .build();
        Log.d(TAG, "Follow playlist with id:  " + id + ": " + completeURL.toString());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("public", false);
        RequestBody body = RequestBody.create(jsonObject.toString(), Constants.JSON);
        Request request = new Request.Builder()
                .url(completeURL)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.d(TAG, "onFailure: failed to follow playlist with id: " + id);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Error : " + response);
                } else {
                    Log.d(TAG, "onResponse: followed successfully playlist: " + id);
                }
            }
        });
    }

    public void deletePlaylist(String id) {
        if(id == null) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("followers")
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
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
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Playlist has been deleted.");
                }
                response.close();
            }
        });
    }

    public void addItem(String uri, String name) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .addQueryParameter("uris", uri)
                .build();
        RequestBody body = RequestBody.create(new byte[]{}, null);
        Log.d(TAG, "Making request to " + completeURL.toString());
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
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Track " + name + " has been added.");
                    size++;
                    if (size == 1) {
                        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:" + playlistID);
                        mSpotifyAppRemote.getPlayerApi().setRepeat(0);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(pause);
                                //repeatMode("context");
                            }
                        }).start();
                    }
                }
                response.close();
            }
        });
    }

    public void deleteFromQue(int position, AfterCallback callback) {
        callback.deleteFromDataset();
        playlist.remove(position);
        que.remove(position);
    }

    public void deleteItem(String uri, String name, int position, AfterCallback callback) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .build();
        int index = size - que.size() + position;
        if(index < 0 || index >= size)
            return;
        JSONObject uris = new JSONObject()
                .put("uri", uri)
                .put("positions", new JSONArray().put(index));
        ;
        JSONObject sampleObject = new JSONObject()
               .put("tracks", new JSONArray().put(uris));
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        Log.d(TAG, "Making request to " + completeURL.toString());
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
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Track " + name + " has been deleted.");
                    callback.deleteFromDataset();
                    playlist.remove(index);
                    que.remove(position);
                    size--;
                }
                response.close();
            }
        });
    }

    public void moveItem(int from, int to) throws JSONException {
        int position = size - que.size();
        Log.d(TAG, "moveItem: From " + from + " To: " + to + " Position: " + position);
        from = from + position;
        to = to + position;
        if (from < to) to++;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(playlistID)
                .addPathSegment("tracks")
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        JSONObject sampleObject = new JSONObject()
                .put("range_start", from)
                .put("insert_before", to);
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
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
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Track moved.");
                }
                response.close();
            }
        });
    }

    public void updatePlaylistCover(String id, Bitmap image, ShowSavedPlaylistRecycAdapter adapter) {
        OkHttpClient client = new OkHttpClient();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byte[] encoded = Base64.encode(byteArray, Base64.NO_WRAP);
        HttpUrl completeUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment(userID)
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("images")
                .build();
        RequestBody body = RequestBody.create(encoded);
        Request request = new Request.Builder()
                .url(completeUrl)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "image/jpeg")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    Log.d(TAG, response.body().string());
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Playlist cover changed to " + response.body().string());
                    adapter.notifyDataSetChanged();
                }
                response.close();
            }
        });

    }

    public void updatePlaylistName(String name, String id) throws JSONException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        JSONObject sampleObject = new JSONObject()
                .put("name", name);
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
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
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful. Playlist name changed.");
                }
                response.close();
            }
        });
    }



    // Parcel

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) { }

    public static final Creator<ServerService> CREATOR = new Creator<ServerService>() {
        @Override
        public ServerService createFromParcel(Parcel in) {
            return new ServerService();
        }

        @Override
        public ServerService[] newArray(int size) {
            return new ServerService[size];
        }
    };


    // In-App Lists

    public void addItemToPlaylist(Track track) {
        playlist.add(track);
        que.addItem(track);
        if(playlist.size() == 1)
            que.next();
    }

    public void addItemToTrackList(Track track) { que.addItem(track); }

    public void togglePlayback() {
        if (pause && getmSpotifyAppRemote() != null) {
            Log.d(TAG, "Size: " + que.size() + " Playlist size: " + playlist.size());
            if(stopped && que.size() == 0) {
                lastSongTitle = null;
                getmSpotifyAppRemote().getPlayerApi().play("spotify:playlist:" + playlistID);
                que.setQueList(new ArrayList<>(playlist));
            } else if(stopped) {
                getmSpotifyAppRemote().getPlayerApi().skipNext();
            } else {
                getmSpotifyAppRemote().getPlayerApi().resume();
            }
        }
        else if (getmSpotifyAppRemote() != null)
            getmSpotifyAppRemote().getPlayerApi().pause();
    }

    public void addEventListener() {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final com.spotify.protocol.types.Track track = playerState.track;
                    que.setTimer(track.duration - playerState.playbackPosition, !playerState.isPaused);
                    if(playerState.isPaused != pause) {
                        if(playerState.isPaused)
                            que.pause();
                        else
                            que.resume();
                    }

                    pause = playerState.isPaused;
                    if(playlist.size() != 0 && (lastSongTitle == null || !nowPlaying.uri.equals(lastSongTitle.uri))) {
                        nowPlaying = playerState.track;
                        if(spotifyPlayerCallback != null) {
                            spotifyPlayerCallback.setNowPlaying(getNowPlaying());
                        }
                        lastSongTitle = nowPlaying;
                    }
                    /*if(playlistID != null) {
                        nowPlaying = track;

                        if((lastSongTitle == null && !playerState.isPaused) || (nowPlaying != null && !nowPlaying.uri.equals(lastSongTitle.uri))) {
                            //printPlaylist(playlist);
                            //printPlaylist(tracks);
                            Log.d(TAG, "addEventListener: " + (playlist.size() - 2 - tracks.size()));
                            //if(playlist.size() > 1 && playlist.get(playlist.size() - 2 - tracks.size()).getURI().equals(nowPlaying.uri));
                                //tracks.add(0, playlist.get(playlist.size() - 2 - tracks.size()));

                            if(tracks.size() == 0 && lastSongTitle != null && !stopped) {
                                stopped = true;
                                Log.d(TAG, "Playlist hast ended " + lastSongTitle.name + " Duration: " + lastSongTitle.duration);
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
                            Log.d(TAG, "New song has been started " + track.uri.split(":")[2]);
                            stopped = false;
                            new Thread(()->{
                                try {
                                    sendToAll(Commands.PLAYING, getNowPlaying().serialize());
                                } catch (IOException | JSONException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                            }).start();

                            if(tracks.size() > 0 && tracks.get(0).getURI().equals(nowPlaying.uri)) {
                                tracks.remove(0);
                            }
                        }
                        pause = playerState.isPaused;
                        if(tracks.size() > 0 && nowPlaying.uri.equals(tracks.get(0).getURI()))
                            tracks.remove(0);
                        Log.d(TAG, "addEventListener: " + track + " - " + spotifyPlayerCallback);
                        if (track != null && spotifyPlayerCallback != null) {
                            Log.d(TAG, track.name + " by " + track.artist.name);
                            //if (playerState.playbackPosition == 0)
                            //nextSong();
                            spotifyPlayerCallback.setNowPlaying(getNowPlaying());
                        }

                        if(spotifyPlayerCallback != null) spotifyPlayerCallback.setPlayImage(pause);
                    }*/
                });
    }

    // Interaction with Server

    private void startServer(){
        Log.d(TAG, "Try to start server");
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    public void updateServiceNotifaction() {
        String text = getString(R.string.service_serverMsg, partyName);
        String peopleCount = getString(R.string.service_serverPeople, clientThreads.size());
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        Notification notificationUpdate = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(text)
                .setContentText(peopleCount)
                .setSmallIcon(R.drawable.ic_service_notification_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_exit_button, getString(R.string.text_end), pendingIntentButton)
                .build();
        mNotificationManager.notify(Constants.NOTIFY_ID, notificationUpdate);
    }


    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(Constants.PORT);
                Log.d(TAG, "Server Started on port " + Constants.PORT);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            if(null != serverSocket){
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        clientThreads.add(new CommunicationThread(serverSocket.accept()));
                        clientThreads.get(clientThreads.size()-1).start();
                        updateServiceNotifaction();
                    } catch (IOException e) {
                        Log.d(TAG, "Server has been closed");
                    }
                }
            }
        }
    }


    // Interaction with Clients

    private void stopAll() throws IOException {
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

    class CommunicationThread extends Thread {
        private Socket clientSocket;
        private BufferedReader input;
        private DataOutputStream out;
        private boolean login = false;
        private String username;
        private final long createdTime;

        public CommunicationThread(Socket socket) {
            this.clientSocket = socket;
            this.createdTime = System.currentTimeMillis();
        }

        public void sendMessage(Commands command, String message) throws IOException {
            Log.d(TAG, "~" + command.toString() + "~" + message);
            out.writeBytes("~" + command.toString() + "~" + message + "\n\r");
            out.flush();
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
                out = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
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
                                    Log.d(TAG, "User " + username + " has left the party");
                                    updateServiceNotifaction();
                                    close();
                                    return;
                                case LOGIN:
                                    if (parts.length > 3) {
                                        String pass = parts[3];
                                        Log.d(TAG, "New login attempt from user " + attribute +" with password: " + pass);
                                        if (login(pass)) {
                                            username = attribute;
                                            if (getNowPlaying() == null)
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
                                    Log.d(TAG, "Added " + attribute + " to the queue");
                                    if(this.login) {
                                        Track track = new Track(attribute);
                                        addItemToPlaylist(track);
                                        //addItem(track.getURI(), track.getName());
                                        spotifyPlayerCallback.reloadPlaylistFragment();
                                        //sendToAll(Commands.QUEUE, track.serialize());
                                    }
                                    break;
                                case PLAYING:
                                    if(getNowPlaying() != null && this.login) {
                                        sendMessage(Commands.PLAYING, getNowPlaying().serialize());
                                    }
                                    break;
                                case PLAYLIST:
                                    StringBuilder response = new StringBuilder();
                                    if(getNowPlaying() != null) {
                                        response.append("~");
                                        response.append(getNowPlaying().serialize());
                                    }
                                    for (Track track: que.getQueList()) {
                                        response.append("~");
                                        response.append(track.serialize());
                                    }
                                    sendMessage(Commands.PLAYLIST, response.toString());
                                    break;
                                default:
                                    Log.d(TAG, "No such command: " + command);
                            }
                        }
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
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