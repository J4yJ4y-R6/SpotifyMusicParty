package com.tinf19.musicparty.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.PartyPerson;
import com.tinf19.musicparty.music.Queue;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.receiver.ActionReceiver;
import com.tinf19.musicparty.receiver.VotedReceiver;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.SpotifyHelper;
import com.tinf19.musicparty.util.TokenRefresh;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.Response;

public class HostService extends Service implements Parcelable {

    private static final String TAG = HostService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private final SpotifyHelper spotifyHelper = new SpotifyHelper();
    private final List<Track> playlist = new ArrayList<>();
    /**
     * A List of all clients connected to the server. Each CommunicationThread has a
     * {@link Socket} and a timestamp from the time he connected to the server.
     */
    private final List<CommunicationThread> clientThreads = new ArrayList<>();
    /**
     * A sublist of the clientThreads with all clients currently displaying the
     * {@link com.tinf19.musicparty.fragments.VotingFragment}. It is used to decide which clients
     * get the result information about a voting after updating.
     */
    private final List<CommunicationThread> subscribedClients = new ArrayList<>();
    /**
     * A ArrayList of all votings where the host has not submitted his vote yet.
     */
    private final ArrayList<Voting> currentVoting = new ArrayList<>();
    /**
     * A Map with all currently opened votings. The key is always the votingID.
     */
    private static final Map<Integer, HostVoting> hostVotings = new HashMap<>();

    private Thread serverThread = null;
    private NotificationManager votingManager;
    private ServerSocket serverSocket;
    private Queue queue;
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
    private String partyName;
    private int votingTime = 2;
    private boolean first = true;
    private boolean pause = true;
    private boolean isPlayingContext;
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
        void removePlaylistFromFav(String id, int position);
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
        queue = new Queue(new Queue.QueCallback() {
            @Override
            public void playSong(Track track) {
                if(getmSpotifyAppRemote() != null) {
                    Log.d(TAG, "new song has been started: " + track.getName());
                    mSpotifyAppRemote.getPlayerApi().play(track.getURI());
                    new Thread(()->{
                        try {
                            sendToAll(Commands.PLAYING, queue.getNowPlaying().serialize());
                        } catch (JSONException e) {
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
            public void skipAndClose(int id, Thread thread) {
                queue.next();
                close(id, thread);
            }

            @Override
            public void addAndClose(int id, Thread thread) {
                HostVoting voting = hostVotings.get(id);
                if(voting != null)
                    addItemToPlaylist(voting.getTrack());
                close(id, thread);
                if(hostServiceCallback != null)
                    hostServiceCallback.notifyPlaylistAdapter();
            }

            @Override
            public int getClientCount() {
                return getClientListSize();
            }

            @Override
            public int getVotingTime() { return votingTime; }

            @Override
            public void close(int id, Thread thread) {
                HostVoting voting = hostVotings.get(id);
                if(voting != null) {
                    voting.closeVoting();
                    hostVotings.remove(id);
                    HostService.this.notifyClientsClosedVoting(voting.getId());
                    if(hostServiceCallback != null) {
                        //hostServiceCallback.notifyVotingAdapter(id, voting.getType());
                        hostServiceCallback.removeVoting(id, voting.getType());
                    }
                    if(thread != serverThread)
                        notificationAfterVote(id);
                }
            }

            @Override
            public void notifyClients(HostVoting voting, Thread thread) {
                if (hostServiceCallback != null)
                    hostServiceCallback.notifyVotingAdapter(
                            voting.getId(), voting.getType());
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
            VotedReceiver.registerCallback(new VotedReceiver.VotedCallback() {
                @Override
                public void notificationVotedYes(int id) {
                    HostVoting voting = hostVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.YES, serverThread);
                    updateVotingNotification();
                }

                @Override
                public void notificationVotedIgnored(int id) {
                    HostVoting voting = hostVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.IGNORED, serverThread);
                    updateVotingNotification();
                }

                @Override
                public void notificationVotedNo(int id) {
                    HostVoting voting = hostVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.NO, serverThread);
                    updateVotingNotification();
                }
            });
        }

        Intent notificationIntent = new Intent(this, HostActivity.class)
                .putExtra(Constants.FROM_NOTIFICATION, true);
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

        NotificationChannel votingChannel = new NotificationChannel(Constants.VOTING_CHANNEL_ID,
                "Voting notification channel", NotificationManager.IMPORTANCE_HIGH);
        votingChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        votingManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        votingManager.createNotificationChannel(votingChannel);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service has been destroyed with " + clientThreads.size() + " clients");
        lastSongTitle = null;
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
        Log.d(TAG, "queue has been restarted");
        if(queue.size() == 0) {
            lastSongTitle = null;
            nowPlaying = null;
            queue.setQueList(new ArrayList<>(playlist));
        }
        queue.setPlaylistEnded(false);
        queue.next();
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
        queue.remove(position);
    }

    public void next() {
        queue.next();
    }

    /**
     * In our implementation the back method will restart the currently playing song if it was
     * played at least two seconds. Otherwise it will start the last played song.
     */
    public void back() {
        if(playlist.size() - queue.size() - 2 >= 0)
            queue.back(playlist.get(playlist.size() - queue.size() - 2));
        else
            mSpotifyAppRemote.getPlayerApi().play(nowPlaying.uri);
    }

    /**
     * When an item is added to the queue it also has to be added to the playlist array list. So in
     * both list the {@link Track} will be added at the end of the list.
     * @param track to be added to the playlist
     */
    public void addItemToPlaylist(Track track) {
        Log.d(TAG, "added track (" + track.getName() + ") to playlist");
        playlist.add(track);
        queue.addItem(track);
        if(playlist.size() == 1)
            queue.next();
    }

    public void queueItem(Track track) {
        if(partyType == PartyType.VoteParty) {
            int votingID = createVoting(track, Type.QUEUE);
            HostVoting hostVoting = hostVotings.get(votingID);
            createVotingNotification(hostVoting);
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
            if(queue.isPlaylistEnded()) {
                Log.d(TAG, "Last song has been played. Queue has been restarted");
                restartQue();
            } else {
                Log.d(TAG, "Current Song has been resumed. Queue-Size: " + queue.size());
                getmSpotifyAppRemote().getPlayerApi().resume();
            }
        }
        else if (getmSpotifyAppRemote() != null){
            Log.d(TAG, "Current Song has been paused. Queue-Size: " + queue.size());
            getmSpotifyAppRemote().getPlayerApi().pause();
        }
    }

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
                            queue.pause();
                        else
                            queue.resume();
                    } else if(playlist.size() != 0 && !playerState.isPaused && (track.duration - playerState.playbackPosition) > Constants.CROSSFADE * 1000) {
                        queue.setTimer(track.duration - playerState.playbackPosition, true);
                    }

                    pause = playerState.isPaused;
                    if(playlist.size() != 0) {
                        nowPlaying = track;
                        if(lastSongTitle == null || !nowPlaying.uri.equals(lastSongTitle.uri)) {
                            int position = playlist.size()-1- queue.size();
                            if(position >= 0
                                    && !nowPlaying.uri.equals(playlist.get(position).getURI())
                                    && !isPlaylistEnded()) {
                                Log.d(TAG, "addEventListener: Different song has been started: " + nowPlaying.name);
                                mSpotifyAppRemote.getPlayerApi().pause();
                                getPlayingContext(8, nowPlaying.uri);
                            } else if(hostServiceCallback != null)
                                hostServiceCallback.setNowPlaying(getNowPlaying());
                            lastSongTitle = track;
                        }

                        if(hostServiceCallback != null)
                            hostServiceCallback.setPlayImage(pause);
                    } else if(!pause && lastSongTitle == null && track != null && !isPlayingContext) {
                        Log.d(TAG, "addEventListener: Different song has been started: " + track.name);
                        isPlayingContext = true;
                        mSpotifyAppRemote.getPlayerApi().pause();
                        getPlayingContext(8, track.uri);
                    }
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
                .setCategory(Constants.CATEGORY_SERVICE)
                .setGroup(Constants.NOTIFICATION_GROUP)
                .addAction(R.drawable.icon_exit_room, getString(R.string.text_end), pendingIntentButton)
                .build();
        Log.d(TAG, "service notification updated");
        mNotificationManager.notify(Constants.NOTIFY_ID, notificationUpdate);
    }

    /**
     * When a Queue-Voting gets startet this method will create a notification, so the user does not
     * have to vote from the fragment. Instead he can use the notification buttons. If already one
     * voting notification is displayed, the notification gets queued in
     * {@link HostService#currentVoting}. Otherwise it will be displayed.
     * @param voting New voting to create the notification about this voting
     */
    private void createVotingNotification(Voting voting) {
        Arrays.stream(votingManager.getActiveNotifications()).forEach(n -> {
            if(votingManager.getActiveNotifications().length == 1  &&
                    n.getId() == Constants.NOTIFY_ID) {
                Log.d(TAG, "voting notification started for: " + voting.getId());
                showVotingNotification(voting);
                currentVoting.add(voting);
                Log.d(TAG, "Current Voting added: " + currentVoting.get(0).getId());
            } else {
                if(n.getId() == Constants.VOTING_NOTIFY_ID) {
                    currentVoting.add(voting);
                    Log.d(TAG, "currently voting notification visible. In Queue: " +
                            currentVoting.size());
                }
            }
        });
    }

    /**
     * Generating and displaying the votingNotification with three buttons, so the user can vote in the
     * notification.
     * @param voting Voting to generate the notification about this voting.
     */
    private void showVotingNotification(Voting voting) {
        Intent votingNotificationIntent = new Intent(this, HostActivity.class)
                .putExtra(Constants.FROM_NOTIFICATION, true);
        PendingIntent votingPendingIntent = PendingIntent.getActivity(this,
                0, votingNotificationIntent, 0);
        Intent votedYesIntent = new Intent(this, VotedReceiver.class);
        votedYesIntent.putExtra(Constants.ID, voting.getId());
        votedYesIntent.putExtra(Constants.VOTE, Constants.YES_VOTE);
        Intent votedNoIntent = new Intent(this, VotedReceiver.class);
        votedNoIntent.putExtra(Constants.ID, voting.getId());
        votedNoIntent.putExtra(Constants.VOTE, Constants.NO_VOTE);
        Intent votedIgnoredIntent = new Intent(this, VotedReceiver.class);
        votedIgnoredIntent.putExtra(Constants.ID, voting.getId());
        votedIgnoredIntent.putExtra(Constants.VOTE, Constants.GREY_VOTE);
        PendingIntent votingYesIntentButton = PendingIntent.getBroadcast(this,1,
                votedYesIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent votingNoIntentButton = PendingIntent.getBroadcast(this,2,
                votedNoIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent votingIgnoreIntentButton = PendingIntent.getBroadcast(this,3,
                votedIgnoredIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this,
                Constants.VOTING_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_votingTitle,
                        voting.getTrack().getName()))
                .setSmallIcon(R.drawable.logo_service_notification)
                .setContentIntent(votingPendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.notification_votingMessageSecondLine)))
                .addAction(R.drawable.icon_thumb_up_nocirce, getString(R.string.text_yes),votingYesIntentButton)
                .addAction(R.drawable.icon_thumb_down_nocircle, getString(R.string.text_no), votingNoIntentButton)
                .addAction(R.drawable.icon_x, getString(R.string.text_ignored), votingIgnoreIntentButton)
                .build();
        votingManager.notify(Constants.VOTING_NOTIFY_ID, notification);
    }

    /**
     * Update the current voting notification when the user voted for the previous one. If the
     * ArrayList {@link HostService#currentVoting} is empty the notification will be
     * dismissed.
     */
    public void updateVotingNotification(){
        if(currentVoting.size() > 1) {
            currentVoting.remove(0);
            showVotingNotification(currentVoting.get(0));
        } else {
            currentVoting.remove(0);
            votingManager.cancel(Constants.VOTING_NOTIFY_ID);
        }
    }

    /**If the host voted for the voting which is currently displayed in the votingNotification, the
     * notification will be updated. Otherwise it will only be removed from the currentVoting list
     * where all votings are listed were the host has not voted yet.
     * @param id Id of the last voted voting
     */
    public void notificationAfterVote(int id) {
        if(currentVoting != null)
            if(currentVoting.size() > 0 && id == currentVoting.get(0).getId())
                updateVotingNotification();
            else{
                int toRemove = 0;
                for(int i = 0; i < currentVoting.size(); i++) {
                    if(id == currentVoting.get(i).getId()) {
                        toRemove = i;
                        break;
                    }
                }
                if(toRemove > 0)
                    currentVoting.remove(toRemove);
            }
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
     * @return Get the new voting
     */
    public int createVoting(Track track, Type type){
        HostVoting newVoting = new HostVoting(type, track, Constants.THRESHOLD_VALUE,
                votingCallback);
        Log.d(TAG, "New " + type.toString() + "-Voting created for: " + newVoting.getTrack()
                .getName());
        hostVotings.put(newVoting.getId(), newVoting);
        if(hostServiceCallback != null)
            hostServiceCallback.notifyVotingAdapter(newVoting);
        try {
            notifyClientsNewVoting(newVoting);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return newVoting.getId();
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
    public boolean isPlaylistEnded() { return queue.isPlaylistEnded(); }

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
        return queue.getQueList();
    }

    /**
     * @return Get true if the current song is paused or false if it's currently playing
     */
    public boolean getPause() {
        return pause;
    }

    public Thread getCurrentThread() { return serverThread; }

    /**
     * @return Get all votings which are not ignored by the host
     */
    public List<Voting> getHostVotings() { return hostVotings.values().stream().filter(v -> v.ignoredNotIncluded(serverThread)).collect(Collectors.toList()); }

    /**
     * @return Get the current voting time
     */
    public int getVotingTime() { return votingTime; }



    // Setter

    /**
     * Change the current party type
     * @param partyType New party type
     */
    public void setPartyType(PartyType partyType) {
        this.partyType = partyType;
        sendToAll(Commands.PARTY_TYPE, partyType.toString());
    }
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

    /**
     * Set the voting time
     * @param votingTime New voting time
     */
    public void setVotingTime(int votingTime) {
        Log.d(TAG, "new VotingTime: " + votingTime);
        this.votingTime = votingTime;
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
        queue.clear();
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
                            queue.addItem(tmpTrack);
                            playlist.add(tmpTrack);
                        }
                        Log.d(TAG, "added " + items.length() + " elements to the playlist" );
                        if(page == 0 && queue.size() > 0)
                            queue.next();
                        if(count > 100 * (page + 1))
                            getQueFromPlaylist(id, page + 1);
                    } catch (JSONException | IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                response.close();
            }
        });
    }

    private void getPlayingContext(int time, String uri) {
        spotifyHelper.getPlayingContext(token, new SpotifyHelper.SpotifyHelperCallback() {
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
                        Log.d(TAG, "Request successfully " + uri);
                        String result = response.body().string();
                        JSONObject body = new JSONObject(result);
                        if(!body.isNull("context")
                                && !body.isNull("item")
                                && body.getJSONObject("item").getString("uri").equals(uri)) {
                            JSONObject context = body.getJSONObject("context");
                            if(context.getString("type").equals("playlist")) {
                                String [] parts = context.getString("uri").split(":");
                                getQueFromPlaylist(parts[parts.length-1]);
                            } else if (playlist.size() != 0){
                                restartQue();
                            }
                            isPlayingContext = false;
                        } else if (time >= 0) {
                            getPlayingContext(time-1, uri);
                        } else {
                            restartQue();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                response.close();
            }
        });
    }

    public void checkPlaylistExists(String id, int position) throws JSONException {
        spotifyHelper.checkPlaylistExists(token, id, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure()  {
                Log.d(TAG, "failed to get status of playlist with id: " + id);
            }

            @Override
            public void onResponse(Response response) {
                if(!response.isSuccessful()) {
                    try {
                        Log.d(TAG, response.body().string());
                        Log.d(TAG, "Removed Playlist from Shared Preferences");
                        hostServiceCallback.removePlaylistFromFav(id, position);
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } else {
                    try {
                        checkPlaylistFollowStatus(id);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
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

    public void swapItem(int from, int to) {
        int position = playlist.size() - queue.size();
        Log.d(TAG, "swapItem: From " + from + " To: " + to + " Position: " + position);
        from = from + position;
        to = to + position;
        Log.d(TAG, "swapItem: " + playlist.get(from).getName() + " TO: " + playlist.get(to).getName());
        Collections.swap(playlist, from, to);
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
        serverThread = new Thread(new ServerThread());
        serverThread.start();
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
     * @throws IOException if client does not exist anymore
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
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            try {
                sendToClientList(subscribedClients, Commands.VOTE_RESULT, voting.serializeResult());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Notify all subscribed clients when a new voting has been added
     * @param voting New Voting
     * @throws JSONException when the serializing failed
     */
    private void notifyClientsNewVoting(HostVoting voting) throws JSONException {
        sendToClientList(clientThreads, Commands.VOTE_ADDED, voting.serialize(serverThread));
    }

    private void notifyClientsClosedVoting(int id) {
        sendToClientList(clientThreads, Commands.VOTE_CLOSED, String.valueOf(id));
    }

    /**
     * Sending a command and a message to all clients
     * @param command Communication command for actions in the client
     * @param message Attributes for mapping the command successfully
     */
    public void sendToAll(Commands command, String message) {
        sendToClientList(clientThreads, command, message);
    }

    /**
     * Sending a command and a message to a specific list of clients
     * @param clientThreads A list of clients that should receive the message
     * @param command Communication command for actions in the client
     * @param message Attributes for mapping the command successfully
     */
    public void sendToClientList(List<CommunicationThread> clientThreads, Commands command, String message) {
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
         * VOTING:      Returning all currently opened votings for Queue and for Skip
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
                                    for (Track track: queue.getQueList()) {
                                        response.append(Constants.DELIMITER);
                                        response.append(track.serialize());
                                    }
                                    sendMessage(Commands.PLAYLIST, response.toString());
                                    break;
                                case VOTING:
                                    StringBuilder votingResponse = new StringBuilder();
                                    for(HostVoting hostVoting : hostVotings.values()) {
                                        if(hostVoting.ignoredNotIncluded(this)) {
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
                                            /*if (hostServiceCallback != null)
                                                hostServiceCallback.notifyVotingAdapter(
                                                        Integer.parseInt(attribute), voting.getType());*/
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
