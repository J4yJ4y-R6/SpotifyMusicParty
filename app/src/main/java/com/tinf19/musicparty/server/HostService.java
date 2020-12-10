package com.tinf19.musicparty.server;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tinf19.musicparty.adapter.VotingAdapter;
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
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.Response;

public class HostService extends Service implements Parcelable, VotingAdapter.VotingAdapterCallback {

    private static final String TAG = HostService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private final SpotifyHelper spotifyHelper = new SpotifyHelper();
    private final List<Track> playlist = new ArrayList<>();
    /**
     * A List of all client connected to the server. Each CommunicationThread has a
     * {@link Socket} and a timestamp from the time he connected to the server.
     */
    private final List<CommunicationThread> clientThreads = new ArrayList<>();
    private final List<CommunicationThread> subscribedClients = new ArrayList<>();
    private final Map<Integer, HostVoting> hostVotings = new HashMap<>();

    private Thread serverThread = null;
    private ServerSocket serverSocket;
    private Que que;
    private SpotifyAppRemote mSpotifyAppRemote;
    private HostServiceCallback hostServiceCallback;
    private com.spotify.protocol.types.Track nowPlaying;
    private com.spotify.protocol.types.Track lastSongTitle;
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentButton;
    public HostVoting.VotingCallback votingCallback;

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

    private PartyType partyType = PartyType.AllInParty;


    public interface HostServiceCallback {
        void setNowPlaying(Track nowPlaying);
        void setPeopleCount(int count);
        void setPlayImage(boolean pause);
        void showDefault();
        void connect(HostActivity.HostActivityCallback hostActivityCallback);
        void reloadPlaylistFragment();
        void addToSharedPreferances(String name, String id);
        void acceptEndParty();
        void notifyPlaylistAdapter();
        void notifyFavPlaylistAdapter();
        void notifyVotingAdapter(int id, Type type);
        void removeVoting(int id, Type type);
        void notifyVotingAdapter(Voting voting);
    }

    public enum PartyType {
        VoteParty,
        AllInParty
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
                    if(hostServiceCallback != null)
                        hostServiceCallback.reloadPlaylistFragment();
                    if (partyType == PartyType.VoteParty)
                        createVoting(track, Type.SKIP);
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
        votingCallback = new HostVoting.VotingCallback() {
            @Override
            public void skipAndClose(int id) {
                que.next();
                close(id);
            }

            @Override
            public void addAndClose(int id) {
                HostVoting voting = hostVotings.get(id);
                if(voting != null)
                    addItemToPlaylist(voting.getTrack());
                close(id);
                if(hostServiceCallback != null)
                    hostServiceCallback.notifyPlaylistAdapter();
            }

            @Override
            public int getClientCount() {
                return getClientListSize();
            }

            @Override
            public void close(int id) {
                HostVoting voting = hostVotings.get(id);
                if(voting != null) {
                    voting.closeVoting();
                    hostVotings.remove(id);
                    HostService.this.notifyClientsResult(voting, serverThread);
                    if(hostServiceCallback != null) {
                        hostServiceCallback.notifyVotingAdapter(id, voting.getType());
                        hostServiceCallback.removeVoting(id, voting.getType());
                    }
                }
            }

            @Override
            public void notifyClients(HostVoting voting, Thread thread) {
                HostService.this.notifyClientsResult(voting, thread);
            }
        };
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

    /**
     * If there is no next song in the queue, it will be restarted by the play button or by the next
     * song button. To restart the queue will be reseted by assigning the playlist array list to the
     * queue list. In the playlist array list all songs, played by the queue, are saved.
     */
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

    /**
     * When the host is deleting an item from the queue it also has to be deleted from the playlist
     * array list. So in both lists the song at the given position will be removed.
     * @param position Position of the song in the playlist and in the queue list
     * @param callback Communication callback to notify the
     *                 {@link com.tinf19.musicparty.server.adapter.HostPlaylistAdapter} that the
     *                 data has changed
     */
    public void deleteFromQue(int position, AfterCallback callback) {
        callback.deleteFromDataset();
        playlist.remove(position);
        que.remove(position);
    }

    public void next() {
        que.next();
    }

    /**
     * In our implementation the back method will restart the currently playing song if it was
     * played at least two seconds. Otherwise it will start the last played song.
     */
    public void back() {
        previous = true;
        if(playlist.size() - que.size() - 2 >= 0)
            que.back(playlist.get(playlist.size() - que.size() - 2));
        else
            mSpotifyAppRemote.getPlayerApi().play(nowPlaying.uri);
    }

    /**
     * When an item is added to the queue it also has to be added to the playlist array list. So in
     * both list the {@link Track} will be added at the end of the list.
     * @param track
     */
    public void addItemToPlaylist(Track track) {
        Log.d(TAG, "added track (" + track.getName() + ") to playlist");
        playlist.add(track);
        que.addItem(track);
        if(playlist.size() == 1)
            que.next();
    }

    public void addItemToTrackList(Track track) { que.addItem(track); }

    public void queueItem(Track track) {
        if(partyType == PartyType.VoteParty) {
            createVoting(track, Type.QUE);
        }
        else {
            addItemToPlaylist(track);
            if(hostServiceCallback != null)
                hostServiceCallback.reloadPlaylistFragment();
        }
    }

    /**
     * This method is the local control for the play button. If the current song is paused, it is
     * necessary to check whether the playlist is at the end or not.If the playlist is at the end,
     * it will restart from the beginning. If not the current song will resume. If the current song
     * is not paused it will get paused.
     */
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

    //TODO: javadoc fertig machen

    /**
     * A Listener which is reacting on the {@link android.content.BroadcastReceiver} which is
     * listening for changes in the Spotify-Remote-Control. With this method the user gets the
     * opportunity to control the player outside the MusicParty-App (ex. in the Spotify-App).
     * So if the Playback-Change-BroadcastReceiver triggers this method has to handle it.
     * ??????
     */
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

    /**
     * Updating the service notification after the party name changed or the count of currently
     * connected clients has changed. So the host always knows how many people have joined hin party
     */
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

    /**
     * @return Get the current partyType which decides how songs will be added or skipped in the
     * queue.
     */
    public PartyType getPartyType() { return partyType; }

    /**
     * @return Get true if the queue is at the end or false if there is a next song
     */
    public boolean isPlaylistEnded() { return que.isPlaylistEnded(); }

    /**
     * @return Get the Spotify-Remote-Control if is connected
     */
    public SpotifyAppRemote getmSpotifyAppRemote() { return ( mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) ? mSpotifyAppRemote : null; }

    /**
     * @return Transforming the CommunicationThreads in PartyPerson so they can be displayed at the
     * {@link com.tinf19.musicparty.server.fragments.HostPartyPeopleFragment}.
     */
    public List<PartyPerson> getPeopleList() {
        List<PartyPerson> tmpPeopleList = new ArrayList<>();
        for (CommunicationThread client: clientThreads) {
            tmpPeopleList.add(new PartyPerson(client.username, System.currentTimeMillis() - client.createdTime));
        }
        return tmpPeopleList;
    }

    /**
     * @return Get true if the host has started the party or false if the token has been refreshed
     * once.
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * @return Get the count of all currently connected clients
     */
    public int getClientListSize() {
        return clientThreads.size();
    }

    /**
     * @return Get the party name which can be changed by the host in the
     * {@link com.tinf19.musicparty.server.fragments.HostSettingsFragment}.
     */
    public String getPartyName() {
        return partyName;
    }

    /**
     * @return Get the Spotify-Connection-Token
     */
    public String getToken() {
        return token;
    }

    /**
     * @return Get the currently playing song as a {@link Track}.
     */
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

    /**
     * @return Get the current state of the queue list with all songs that are going to be played
     */
    public List<Track> getPlaylist() {
        return que.getQueList();
    }

    /**
     * @return Get true if the current song is paused or false if it's currently playing
     */
    public boolean getPause() {
        return pause;
    }

    @Override
    public Thread getCurrentThread() { return serverThread; }

    /**
     * @return Get all votings which are not ignored by the host
     */
    public List<Voting> getHostVotings() { return hostVotings.values().stream().filter(v -> !v.containsIgnored(serverThread)).collect(Collectors.toList()); }

    // Setter

    /**
     * Change the current party type
     * @param partyType New party type
     */
    public void setPartyType(PartyType partyType) throws IOException {
        this.partyType = partyType;
        sendToAll(Commands.PARTYTYPE, partyType.toString());
    }

    /**
     * Set the playlist id when a external playlist has been started from Spotify or the
     * {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}.
     * @param id Playlist-Id of the started playlist given by the Spotify-API
     */
    public void setPlaylistID(String id) { this.playlistID = id; }

    /**
     * Set the party name after changing it in the
     * {@link com.tinf19.musicparty.server.fragments.HostSettingsFragment}.
     * @param partyName New party name
     */
    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    /**
     * Set callback
     * @param hostServiceCallback Communication callback for {@link HostActivity}.
     */
    public void setHostServiceCallback(HostServiceCallback hostServiceCallback) {
        this.hostServiceCallback = hostServiceCallback;
    }




    // HTTP Requests

    /**
     * Get the user id from the Spotify-API saving it in the global variable userID.
     */
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

    /**
     * Making a HttpRequest to the Spotify-API to create a new Spotify-Playlist from the current
     * state of the playlist array list. Afterwards the playlist id and name are going to be saved
     * in the SharedPreferences.
     * @param name Name of the new playlist
     * @param exit Boolean to decide whether the party shall be closed after creating a new playlist
     *             or not.
     * @throws JSONException when the call was not successful
     */
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

    /**
     * Clearing the to lists because it will be filled with songs from another playlist.
     * @param id Id of the started playlist.
     */
    public void getQueFromPlaylist(String id) {
        que.clear();
        playlist.clear();
        getQueFromPlaylist(id, 0);
    }

    /**
     * Making a HttpRequest to get all songs from a existing playlist to fill them into the playlist
     * and the queue lists.
     * @param id Id of the started playlist
     * @param page The Spotify-API only allows to get a maximum of 100
     *             {@link com.spotify.protocol.types.Track} within one request. So the page is
     *             counting the calls to this method.
     */
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

    /**
     * Making a HttpRequest to follow the Spotify-Playlist so the user sees it in the Spotify-
     * Client
     * @param id Id of the playlist which shound be followed
     * @throws JSONException when the call was not successful
     */
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

    /**
     * Making a HttpRequest to unfollow the Spotify-Playlist so the user does not see the Playlist
     * in the Spotify-Client
     * @param id Id of the playlist which should be unfollowed
     */
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

    /**
     * Making a HttpRequest to add Songs at the end of a specific playlist.
     * @param id Id of the playlist
     * @param page The Spotify-API only allows to add a maximum of 100
     *             {@link com.spotify.protocol.types.Track} within one request. So the page is
     *             counting the calls to this method.
     * @param exit Host closes the party after adding the items to the playlist if exit is true
     *             otherwise the party stays open.
     * @throws JSONException when the call was not successful
     */
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

    /**
     * Making a HttpRequest to delete a Song from the current Spotify-Playlist.
     * Currently not in usage because queue is managed by {@link Que}
     * @param uri Track-URI which identifies the song in the Spotify-API
     * @param name Song-Title to log the deletion
     * @param position The position of the song in the current Spotify-Playlist
     * @param callback Communication callback for
     *                 {@link com.tinf19.musicparty.server.fragments.HostPlaylistFragment} to notify the adapter
     *                 that the dataset has changed.
     * @throws JSONException when the call was not successful
     */
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

    /**
     * Making a HttpRequest to swap two songs in the current Spotify-Playlist.
     * Currently not in usage because queue is managed by {@link Que}
     * @param from Position of the first song
     * @param to Position of the second song
     * @throws JSONException when the call was not successful
     */
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

    /**
     * Making a HttpRequest to update the cover of a chosen playlist. The user choose the playlist
     * in the {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}
     * and the new image from his gallery. To update the cover, the image has to be converted into a
     * encoded byte array.
     * @param id Id of the chosen playlist
     * @param image Chosen image as a Bitmap.
     */
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

    /**
     * Making a HttpRequest to update the name of a chosen playlist. The user enters the new name in
     * the {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}.
     * @param name New playlist name - entered by the host
     * @param id Id of the chosen playlist
     * @throws JSONException when the call was not successful
     */
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


    /**
     * Evaluating all votings after the PartyType was changed to a All-In-Party.
     */
    public void evaluateAllVotings() {
        ArrayList<HostVoting> tempVotings = new ArrayList<>(hostVotings.values());
        for(HostVoting voting: tempVotings){
            voting.evaluateVoting();
            voting.closeVoting();
        }
    }

    /**
     * Create a new Voting
     * @param track Track which is voted about
     * @param type Type of the voting
     */
    public void createVoting(Track track, Type type){
        HostVoting newVoting = new HostVoting(type, track, Constants.THRESHOLD_VALUE,
                votingCallback);
        Log.d(TAG, "New " + type.toString() + "-Voting created for: " + newVoting.getTrack()
                .getName());
        hostVotings.put(newVoting.getId(), newVoting);
        if(hostServiceCallback != null)
            hostServiceCallback.notifyVotingAdapter(newVoting);
        try {
            notifyClientsNewVoting(newVoting);
        } catch (JSONException | IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
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

    /**
     * Opening a server where all clients can connect with.
     */
    private void startServer(){
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    /**
     * The ServerThread is unique at a party and is managing the connection between the host and the
     * clients.
     */
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

    /**
     * Disconnecting all clients from the sever after the host closed the party.
     * @throws IOException
     */
    private void stopAll() throws IOException {
        for(CommunicationThread client : clientThreads) {
            client.sendMessage(Commands.QUIT, "Session has been closed");
            client.close();
        }
    }

    /**
     * Notify all subscribed clients when a voting result change
     * @param voting The voting that has been changed
     * @param thread The {@link CommunicationThread} that voted and has not to get notified
     */
    private void notifyClientsResult(HostVoting voting, Thread thread) {
        if(thread instanceof CommunicationThread) {
            List<CommunicationThread> tempList = new ArrayList<>(subscribedClients);
            tempList.remove((CommunicationThread) thread);
            try {
                sendToClientList(tempList, Commands.VOTE_RESULT, voting.serializeResult());
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            try {
                sendToClientList(subscribedClients, Commands.VOTE_RESULT, voting.serializeResult());
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Notify all subscribed clients when a new voting has been added
     * @param voting New Voting
     * @throws JSONException when the serializing failed
     * @throws IOException when the message could not be send to all subscribed clients
     */
    private void notifyClientsNewVoting(HostVoting voting) throws JSONException, IOException {
        sendToClientList(subscribedClients, Commands.VOTE_ADDED, voting.serialize(serverThread));
    }

    /**
     * Sending a command and a message to all clients
     * @param command Communication command for actions in the client
     * @param message Attributes for mapping the command successfully
     * @throws IOException when the Output-Stream in
     * {@link CommunicationThread#sendMessage(Commands, String)} is not writing bytes.
     */
    public void sendToAll(Commands command, String message) throws IOException {
        sendToClientList(clientThreads, command, message);
    }

    /**
     * Sending a command and a message to a specific list of clients
     * @param clientThreads A list of clients that should receive the message
     * @param command Communication command for actions in the client
     * @param message Attributes for mapping the command successfully
     * @throws IOException when the Output-Stream in
     * {@link CommunicationThread#sendMessage(Commands, String)} is not writing bytes.
     */
    public void sendToClientList(List<CommunicationThread> clientThreads, Commands command, String message) throws IOException {
        for(CommunicationThread client : clientThreads) {
            if (client.login)
                client.sendMessage(command, message);
        }
    }


    /**
     * Each CommunicationThread represents a client connection and is managing the communication
     * between client and host.
     */
    class CommunicationThread extends Thread {
        private final Socket clientSocket;
        private final long createdTime;
        private BufferedReader input;
        private DataOutputStream out;
        private boolean login = false;
        private String username;
        private SendMessageLooper sendMessageLooper;

        /**
         * Constructor to set the client-attributes
         * @param socket {@link Socket} to connect to the serverSocket.
         */
        public CommunicationThread(Socket socket) {
            this.clientSocket = socket;
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * Sending a command and a message to a client
         * @param command Communication command for actions in the client
         * @param message Attributes for mapping the command successfully
         */
        public void sendMessage(Commands command, String message) {
            if(sendMessageLooper != null) {
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.COMMAND, command.toString());
                bundle.putString(Constants.MESSAGE, message);
                msg.setData(bundle);
                sendMessageLooper.mHandler.sendMessage(msg);
            }
        }

        /**
         * Sending a command and a message to a client
         * @param command Communication command for actions in the client
         * @param message Attributes for mapping the command successfully
         * @throws IOException when the Output-Stream is not writing bytes.
         */
        private void sendMessageLooper(Commands command, String message) throws IOException {
            Log.d(TAG, "Send Message to User: " + username + ", Command: " + command.toString() + ", Message: " + message);
            out.writeBytes(Constants.DELIMITER + command.toString() + Constants.DELIMITER + message + "\n\r");
            out.flush();
        }


        /**
         * Splitting the client message to communicate with the client by different commands:
         * QUIT:        A client has been disconnected. The server has to recognize this and change
         *              the current count of {@link PartyPerson}.
         * LOGIN:       Checking the login credentials from the login-request and open the
         *              connection if they are correct. Otherwise the Client gets a message that
         *              the login failed.
         * QUEUE:       Adding a new song at the end of the queue and reload the
         *              {@link com.tinf19.musicparty.server.fragments.HostPlaylistFragment} if it
         *              is opened.
         * PLAYING:     After starting a new song all clients get the new {@link Track}-Object to
         *              change all information about the currently playing song.
         * PLAYLIST:    Returning the current queue state after a client request.
         * VOTING:      Returning all currently opened votings for Que and for Skip
         * VOTE:        Process a submitted voting by a client.
         * VOTE_RESULT: Returning the current result of a specific voting
         */
        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
                out = new DataOutputStream(clientSocket.getOutputStream());
                sendMessageLooper = new SendMessageLooper();
                sendMessageLooper.start();
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
                                    subscribedClients.remove(this);
                                    hostVotings.values().forEach(v->v.removeThread(this));
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
                                                sendMessage(Commands.LOGIN, partyName
                                                        + Constants.DELIMITER + getPartyType());
                                            else
                                                sendMessage(Commands.LOGIN, partyName +
                                                        Constants.DELIMITER + getPartyType() +
                                                        Constants.DELIMITER +
                                                        getNowPlaying().serialize());
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
                                        queueItem(track);
                                        //addItem(track.getURI(), track.getName());
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
                                case VOTING:
                                    StringBuilder votingResponse = new StringBuilder();
                                    for(HostVoting hostVoting : hostVotings.values()) {
                                        if(!hostVoting.containsIgnored(this)) {
                                            votingResponse.append(Constants.DELIMITER);
                                            votingResponse.append(hostVoting.serialize(this));
                                        }
                                    }
                                    sendMessage(Commands.VOTING, votingResponse.toString());
                                    break;
                                case VOTE:
                                    if(parts.length > 3) {
                                        HostVoting voting = hostVotings.get(Integer.parseInt(attribute));
                                        if(voting != null) {
                                            voting.addVoting(Integer.parseInt(parts[3]), this);
                                            if (hostServiceCallback != null)
                                                hostServiceCallback.notifyVotingAdapter(
                                                        Integer.parseInt(attribute), voting.getType());
                                        }
                                        break;
                                    }
                                    break;
                                case VOTE_RESULT:
                                    HostVoting voting = hostVotings.get(Integer.parseInt(attribute));
                                    if(voting != null)
                                        sendMessage(Commands.VOTE_RESULT, voting.serializeResult());
                                    break;
                                case SUBSCRIBE:
                                    subscribedClients.add(this);
                                    break;
                                case UNSUBSCRIBE:
                                    subscribedClients.remove(this);
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

        /**
         * Closing the client connection to the server
         * @throws IOException when the input and output streams could not be closed.
         */
        private void close() throws IOException {
            out.close();
            input.close();
            clientSocket.close();
        }

        /**
         * Check the login-credentials to verify a login request
         * @param input Entered password from the login-request
         * @return True if the password is correct or false if it is wrong.
         */
        private boolean login(String input) {
            if(input.equals(password))
                login = true;
            return login;
        }

        class SendMessageLooper extends Thread {
            public Handler mHandler;

            @Override
            public void run() {
                Looper.prepare();

                mHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        String command = msg.getData().getString(Constants.COMMAND);
                        String message = msg.getData().getString(Constants.MESSAGE);
                        try {
                            CommunicationThread.this.sendMessageLooper(Commands.valueOf(command), (String) message);
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                };

                Looper.loop();
            }
        }
    }
}
