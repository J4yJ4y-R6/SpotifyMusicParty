package com.tinf19.musicparty.server;

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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.PartyPerson;
import com.tinf19.musicparty.music.Que;
import com.tinf19.musicparty.receiver.ActionReceiver;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.SpotifyHelper;
import com.tinf19.musicparty.util.TokenRefresh;

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

import okhttp3.Response;

public class HostService extends Service implements Parcelable {

    private static final String TAG = HostService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private final SpotifyHelper spotifyHelper = new SpotifyHelper();
    private final List<Track> playlist = new ArrayList<>();
    private final List<CommunicationThread> clientThreads = new ArrayList<>();

    private Thread serverThread = null;
    private ServerSocket serverSocket;
    private Que que;
    private SpotifyAppRemote mSpotifyAppRemote;
    private HostServiceCallback hostServiceCallback;
    private com.spotify.protocol.types.Track nowPlaying;
    private com.spotify.protocol.types.Track lastSongTitle;
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentButton;

    private String password;
    private String userID;
    private String token;
    private String playlistID;
    private String partyName;
    private int size = 0;
    private boolean first = true;
    private boolean pause = true;
    private boolean newSong;
    private boolean stopped;
    private boolean previous;



    public interface HostServiceCallback {
        void setNowPlaying(Track nowPlaying);
        void setPeopleCount(int count);
        void setPlayImage(boolean pause);
        void showDefault();
        void connect(HostActivity.HostActivityCallback hostActivityCallback);
        void reloadPlaylistFragment();
        void addToSharedPreferances(String name, String id);
        void acceptEndParty();
        void notifyFavPlaylistAdapter();
    }

    public interface AfterCallback {
        void deleteFromDataset();
    }

    public class LocalBinder extends Binder {
        HostService getService() { return HostService.this; }
    }



    //Android lifecycle methods

    @Override
    public void onCreate() {
        super.onCreate();
        que = new Que(new Que.QueCallback() {
            @Override
            public void playSong(Track track) {
                if(getmSpotifyAppRemote() != null) {
                    Log.d(TAG, "new song has been started: " + track.getName());
                    mSpotifyAppRemote.getPlayerApi().play(track.getURI());
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
            public void setProgressBar(long timeRemaining) { }

            @Override
            public void stopPlayback() {
                Log.d(TAG, "stopping playback at the end of the playlist");
                mSpotifyAppRemote.getPlayerApi().seekTo(0);
                mSpotifyAppRemote.getPlayerApi().pause();
            }
        });
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        password = intent.getStringExtra(Constants.PASSWORD);
        partyName = intent.getStringExtra(Constants.PARTYNAME);
        if (first) {
            Thread tokenRefresh = new Thread(new TokenRefresh(intent.getStringExtra(Constants.CODE), new TokenRefresh.TokenRefreshCallback() {
                @Override
                public void afterConnection(String token) {
                    HostService.this.token = token;
                    if (hostServiceCallback != null) {
                        HostActivity.HostActivityCallback callback = new HostActivity.HostActivityCallback() {
                            @Override
                            public void afterConnection(SpotifyAppRemote appRemote) {
                                Log.d(TAG, "connected to spotify-app remote control");
                                mSpotifyAppRemote = appRemote;
                                addEventListener();
                            }

                            @Override
                            public void afterFailure() {
                                if (hostServiceCallback != null) hostServiceCallback.connect(this);
                            }
                        };
                        hostServiceCallback.connect(new HostActivity.HostActivityCallback() {
                            @Override
                            public void afterConnection(SpotifyAppRemote appRemote) {
                                Log.d(TAG, "connected to spotify-app remote control");
                                mSpotifyAppRemote = appRemote;
                                getUserID();
                                addEventListener();
                                if (hostServiceCallback != null)
                                    hostServiceCallback.showDefault();
                            }

                            @Override
                            public void afterFailure() {
                                if (hostServiceCallback != null)
                                    hostServiceCallback.connect(callback);
                            }
                        });
                    }
                }

                @Override
                public void afterRefresh(String token) {
                    Log.d(TAG, "afterRefresh: Token hast been refreshed");
                    HostService.this.token = token;
                }
            }));
            tokenRefresh.start();
            first = false;
        }

        Intent notificationIntent = new Intent(this, HostActivity.class).putExtra(Constants.FROM_NOTIFICATION, true);
        pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Intent intentAction = new Intent(this, ActionReceiver.class);
        pendingIntentButton = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(getString(R.string.service_serverMsg, partyName))
                .setContentText(getString(R.string.service_serverPeople, clientThreads.size()))
                .setSmallIcon(R.drawable.logo_service_notification)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.icon_exit_room, getString(R.string.text_end),pendingIntentButton)
                .build();
        Log.d(TAG, "service notification started");
        startForeground(Constants.NOTIFY_ID, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service has been destroyed with " + clientThreads.size() + " clients");
        lastSongTitle = null;
        stopped = false;
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



    //Service - Methods

    public void restartQue() {
        Log.d(TAG, "que has been restarted");
        if(que.size() == 0) {
            lastSongTitle = null;
            nowPlaying = null;
            que.setQueList(new ArrayList<>(playlist));
        }
        que.setPlaylistEnded(false);
        que.next();
    }

    public void deleteFromQue(int position, AfterCallback callback) {
        callback.deleteFromDataset();
        playlist.remove(position);
        que.remove(position);
    }

    public void next() {
        que.next();
    }

    public void back() {
        previous = true;
        if(playlist.size() - que.size() - 2 >= 0)
            que.back(playlist.get(playlist.size() - que.size() - 2));
        else
            mSpotifyAppRemote.getPlayerApi().play(nowPlaying.uri);
    }

    public void addItemToPlaylist(Track track) {
        Log.d(TAG, "added track (" + track.getName() + ") to playlist");
        playlist.add(track);
        que.addItem(track);
        if(playlist.size() == 1)
            que.next();
    }

    public void addItemToTrackList(Track track) { que.addItem(track); }

    public void togglePlayback() {
        if (pause && getmSpotifyAppRemote() != null) {
            if(que.isPlaylistEnded()) {
                Log.d(TAG, "Last song has been played. Queue has been restarted");
                restartQue();
            } else {
                Log.d(TAG, "Current Song has been resumed. Queue-Size: " + que.size());
                getmSpotifyAppRemote().getPlayerApi().resume();
            }
        }
        else if (getmSpotifyAppRemote() != null){
            Log.d(TAG, "Current Song has been paused. Queue-Size: " + que.size());
            getmSpotifyAppRemote().getPlayerApi().pause();
        }
    }

    public void addEventListener() {
        Log.d(TAG, "Playbackchanged-EventListener added to remote control");
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final com.spotify.protocol.types.Track track = playerState.track;
                    if(playerState.isPaused != pause) {
                        if(playerState.isPaused)
                            que.pause();
                        else
                            que.resume();
                    } else if(playlist.size() != 0 && !playerState.isPaused && (track.duration - playerState.playbackPosition) > Constants.CROSSFADE * 1000) {
                        que.setTimer(track.duration - playerState.playbackPosition, true);
                    }

                    pause = playerState.isPaused;
                    if(playlist.size() != 0) {
                        nowPlaying = track;
                        if(lastSongTitle == null || !nowPlaying.uri.equals(lastSongTitle.uri)) {
                            if(hostServiceCallback != null)
                                hostServiceCallback.setNowPlaying(getNowPlaying());
                            lastSongTitle = track;
                        }

                        if(hostServiceCallback != null)
                            hostServiceCallback.setPlayImage(pause);
                    }
                    /*if(playlistID != null) {
                        nowPlaying = track;

                        if((lastSongTitle == null && !playerState.isPaused) || (nowPlaying != null && !nowPlaying.uri.equals(lastSongTitle.uri))) {
                            if(tracks.size() == 0 && lastSongTitle != null && !stopped) {
                                stopped = true;
                                Log.d(TAG, "Playlist hast ended " + lastSongTitle.name + " Duration: " + lastSongTitle.duration);
                                mSpotifyAppRemote.getPlayerApi().skipPrevious();
                                mSpotifyAppRemote.getPlayerApi().pause();
                                pause = true;
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

    public void updateServiceNotifaction() {
        String text = getString(R.string.service_serverMsg, partyName);
        String peopleCount = getString(R.string.service_serverPeople, clientThreads.size());
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        Notification notificationUpdate = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(text)
                .setContentText(peopleCount)
                .setSmallIcon(R.drawable.logo_service_notification)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.icon_exit_room, getString(R.string.text_end), pendingIntentButton)
                .build();
        Log.d(TAG, "service notification updated");
        mNotificationManager.notify(Constants.NOTIFY_ID, notificationUpdate);
    }



    // Getter

    public boolean isPlaylistEnded() { return que.isPlaylistEnded(); }

    public SpotifyAppRemote getmSpotifyAppRemote() { return ( mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) ? mSpotifyAppRemote : null; }
    
    public List<PartyPerson> getPeopleList() {
        List<PartyPerson> tmpPeopleList = new ArrayList<>();
        for (CommunicationThread client: clientThreads) {
            tmpPeopleList.add(new PartyPerson(client.username, System.currentTimeMillis() - client.createdTime));
        }
        return tmpPeopleList;
    }

    public boolean isFirst() {
        return first;
    }

    public int getClientListSize() {
        return clientThreads.size();
    }

    public String getPartyName() {
        return partyName;
    }

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

    public void setHostServiceCallback(HostServiceCallback hostServiceCallback) {
        this.hostServiceCallback = hostServiceCallback;
    }




    // HTTP Requests

    private void getUserID() {
        spotifyHelper.getUserID(token, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request failed");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. Got the username of the user.");
                }
                try {
                    final String data = response.body().string();
                    userID = new JSONObject(data).getString("id");
                } catch (JSONException | IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                response.close();
            }
        });
    }

    public void createPlaylist(String name, boolean exit) throws JSONException {
        if (userID == null ) return;
        spotifyHelper.createPlaylist(token, name, userID, getString(R.string.service_playlistDescription, partyName), new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. New playlist has been created.");
                }
                String [] uri = response.header("Location").split("/");
                String id = uri[uri.length-1];
                Log.d(TAG, id);
                if(hostServiceCallback != null) {
                    hostServiceCallback.addToSharedPreferances(name, id);
                }
                try {
                    addItemsToPlaylist(id, 0, exit);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        spotifyHelper.getQueFromPlaylist(token, id, page, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    try {
                        Log.d(TAG, "Request successfully");
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
                        Log.d(TAG, "added " + que.size() + " elements" );
                        if(page == 0 && que.size() > 0)
                            que.next();
                        if(count > 100 * page)
                            getQueFromPlaylist(id, page + 1);
                    } catch (JSONException | IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                response.close();
            }
        });
    }

    public void checkPlaylistFollowStatus(String id) throws JSONException {
        spotifyHelper.checkPlaylistFollowStatus(token, id, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "failed to follow playlist with id: " + id);
            }

            @Override
            public void onResponse(Response response) {
                if (!response.isSuccessful()) {
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } else {
                    Log.d(TAG, "followed successfully playlist: " + id);
                }
            }
        });
    }

    public void deletePlaylist(String id) {
        spotifyHelper.deletePlaylist(token, id, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. Playlist has been deleted.");
                }
                response.close();
            }
        });
    }

    public void addItemsToPlaylist(String id, int page, boolean exit) throws JSONException {
        spotifyHelper.addItemsToPlaylist(token, playlist, id, page, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    if(playlist.size() >= ((page+1) * 100)) {
                        try {
                            addItemsToPlaylist(id, page+1, exit);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(hostServiceCallback != null && exit)
                            hostServiceCallback.acceptEndParty();
                    }
                }
                response.close();
            }
        });
    }

    public void deleteItem(String uri, String name, int position, AfterCallback callback) throws JSONException {
        spotifyHelper.deleteItem(token, playlistID, uri, size, que.size(), position, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. Track " + name + " has been deleted.");
                    callback.deleteFromDataset();
                    playlist.remove(size - que.size() + position);
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
        spotifyHelper.moveItem(token, playlistID, from, to, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request failed");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. Track moved.");
                }
                response.close();
            }
        });
    }

    public void updatePlaylistCover(String id, Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byte[] encoded = Base64.encode(byteArray, Base64.NO_WRAP);
        spotifyHelper.updatePlaylistCover(token, userID, id, encoded, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request failed");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }else {
                    Log.d(TAG,"Request Successful. Playlist cover changed");
                    hostServiceCallback.notifyFavPlaylistAdapter();
                }
                response.close();
            }
        });
    }

    public void updatePlaylistName(String name, String id) throws JSONException {
        spotifyHelper.updatePlaylistName(token, name, id, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request failed");
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()){
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
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

    public static final Creator<HostService> CREATOR = new Creator<HostService>() {
        @Override
        public HostService createFromParcel(Parcel in) {
            return new HostService();
        }

        @Override
        public HostService[] newArray(int size) {
            return new HostService[size];
        }
    };



    // Interaction with Server

    private void startServer(){
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    class ServerThread implements Runnable {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Server Started on port " + Constants.PORT);
                serverSocket = new ServerSocket(Constants.PORT);
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
        private final Socket clientSocket;
        private final long createdTime;
        private BufferedReader input;
        private DataOutputStream out;
        private boolean login = false;
        private String username;

        public CommunicationThread(Socket socket) {
            this.clientSocket = socket;
            this.createdTime = System.currentTimeMillis();
        }

        public void sendMessage(Commands command, String message) throws IOException {
            Log.d(TAG, "Send Message to User: " + username + ", Command: " + command.toString() + ", Message: " + message);
            out.writeBytes(Constants.DELIMITER + command.toString() + Constants.DELIMITER + message + "\n\r");
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
                        String [] parts = line.split(Constants.DELIMITER);
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            String attribute = "";
                            if (parts.length > 2)
                                attribute = parts[2];
                            switch (command) {
                                case QUIT:
                                    clientThreads.remove(this);
                                    if(hostServiceCallback != null) hostServiceCallback.setPeopleCount(clientThreads.size());
                                    Log.d(TAG, "User " + username + " has left the party");
                                    updateServiceNotifaction();
                                    close();
                                    return;
                                case LOGIN:
                                    if (parts.length > 3) {
                                        String password = parts[3];
                                        Log.d(TAG, "New login attempt from user " + attribute +" with password: " + password);
                                        if (login(password)) {
                                            username = attribute;
                                            if (getNowPlaying() == null)
                                                sendMessage(Commands.LOGIN, partyName);
                                            else
                                                sendMessage(Commands.LOGIN, partyName + Constants.DELIMITER + getNowPlaying().serialize());
                                            if(hostServiceCallback != null) hostServiceCallback.setPeopleCount(clientThreads.size());
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
                                        hostServiceCallback.reloadPlaylistFragment();
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
                                        response.append(Constants.DELIMITER);
                                        response.append(getNowPlaying().serialize());
                                    }
                                    for (Track track: que.getQueList()) {
                                        response.append(Constants.DELIMITER);
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