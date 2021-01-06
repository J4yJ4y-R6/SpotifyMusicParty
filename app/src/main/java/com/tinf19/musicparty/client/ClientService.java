package com.tinf19.musicparty.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tinf19.musicparty.receiver.ActionReceiver;
import com.tinf19.musicparty.receiver.VotedReceiver;
import com.tinf19.musicparty.server.HostActivity;
import com.tinf19.musicparty.server.HostService;
import com.tinf19.musicparty.util.ClientVoting;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.TokenRefresh;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for background communication with Spotify and all clients
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class ClientService extends Service {

    private static final String TAG = ClientService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private ClientServiceCallback clientServiceCallback;
    private NotificationManager votingManager;
    private ClientThread clientThread;
    private Socket clientSocket;
    private Track nowPlaying;
    private boolean stopped;
    /**
     * Boolean to decide if the service is connected for the first time.
     */
    private boolean first = true;
    /**
     * Boolean to decide if this client is currently displaying the
     * {@link com.tinf19.musicparty.fragments.VotingFragment}.
     */
    private boolean subscirbedVoting = false;
    /**
     * Spotify connection token which is refreshing every hour
     */
    private String token;
    /**
     * Intent for the service notification
     */
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentButton;
    /**
     * An ArrayList with all currently opened votings where this clients has not submitted his vote
     * yet.
     */
    private final ArrayList<Voting> currentVoting = new ArrayList<>();
    /**
     * A Map with all currently opened votings. The key is always the votingID.
     */
    private Map<Integer, ClientVoting> clientVotings = new HashMap<>();
    private HostService.PartyType partyType = HostService.PartyType.AllInParty;



    public interface ClientServiceCallback {
        void setTrack(Track track);
        void setPartyName(String partyName);
        void exitService(String text);
        void setPlaylist(List<Track> trackList);
        void setCurrentTrack(Track track);
        void setVotings(List<Voting> ClientVotings);
        void addVoting(ClientVoting voting);
        void showFragments();
        void notifyVotingAdapter(int id, Type type);
        void removeVoting(int id, Type type);
        void updateVotingButton(HostService.PartyType partyType);
    }

    /**
     * Binder for Bound Service
     * @author Jannik Junker
     * @author Silas Wessely
     * @see ClientService
     * @since 1.1
     */
    public class LocalBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }



    //Android lifecycle methods

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, ClientActivity.class).putExtra(Constants.FROM_NOTIFICATION, true);
        pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Intent intentAction = new Intent(this, ActionReceiver.class);
        pendingIntentButton = PendingIntent.getBroadcast(this,1,intentAction,PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_clientServiceTitle))
                .setSmallIcon(R.drawable.logo_service_notification)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.icon_exit_room, getString(R.string.text_leave),pendingIntentButton)
                .build();
        Log.d(TAG, "starting ClientService-Notification");
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(first) {
            Thread tokenRefresh = new Thread(new TokenRefresh(intent.getStringExtra(Constants.CODE), new TokenRefresh.TokenRefreshCallback() {
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
            VotedReceiver.registerCallback(new VotedReceiver.VotedCallback() {
                @Override
                public void notificationVotedYes(int id) {
                    ClientVoting voting = clientVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.YES, clientThread);
                    updateVotingNotification();
                }

                @Override
                public void notificationVotedIgnored(int id) {
                    ClientVoting voting = clientVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.IGNORED, clientThread);
                    updateVotingNotification();
                }

                @Override
                public void notificationVotedNo(int id) {
                    ClientVoting voting = clientVotings.get(id);
                    if(voting != null) voting.addVoting(Constants.NO, clientThread);
                    updateVotingNotification();
                }
            });

            NotificationChannel votingChannel = new NotificationChannel(Constants.VOTING_CHANNEL_ID,
                    "Voting notification channel", NotificationManager.IMPORTANCE_HIGH);
            votingChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            votingManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            votingManager.createNotificationChannel(votingChannel);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * When a Que-Voting gets startet this method will create a notification, so the user does not
     * have to vote from the fragment. Instead he can use the notification buttons. If already one
     * voting notification is displayed, the notification gets queued in
     * {@link ClientService#currentVoting}. Otherwise it will be displayed.
     * @param voting New voting to create the notification about this voting
     */
    private void createVotingNotification(Voting voting) {
        Arrays.stream(votingManager.getActiveNotifications()).forEach(n -> {
            if(votingManager.getActiveNotifications().length == 1  &&
                    n.getId() == Constants.NOTIFY_ID) {
                Log.d(TAG, "voting notification started for: " + voting.getId());
                showVotingNotification(voting);
                currentVoting.add(voting);
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
     * Generating and displaying the votingNotification with three buttons, so the user can vote in
     * the notification.
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
     * ArrayList {@link ClientService#currentVoting} is empty the notification will be
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

    /**If this client voted for the voting which is currently displayed in the votingNotification, the
     * notification will be updated. Otherwise it will only be removed from the currentVoting list
     * where all votings are listed were this client has not voted yet.
     * @param id Id of the last voted voting
     */
    public void notificationAfterVote(int id) {
        if(currentVoting != null)
            if(id == currentVoting.get(0).getId())
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



    //Getter


    /**
     * @return Get the current partyType
     */
    public HostService.PartyType getPartyType() { return partyType; }

    /**
     * @return Get the current life state of the service
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * @return Get the Spotify connection token
     */
    public String getToken() {
        return token;
    }


//    public Thread getCurrentThread() { return clientThread; }

    /**
     * @return Get all currently opened votings
     */
    public List<Voting> getClientVotings() { return new ArrayList<>(clientVotings.values()); }

    /**
     * @return Get the currently playing song
     */
    public Track getNowPlaying() { return nowPlaying; }

    //Setter

    /**
     * Changing the current party name after the host changed it
     * @param partyType New party type
     */
    public void setPartyType(HostService.PartyType partyType) { this.partyType = partyType; }

    /**
     * Set the {@link ClientServiceCallback}
     * @param clientServiceCallback Communication callback to the {@link ClientActivity}
     */
    public void setClientServiceCallback(ClientServiceCallback clientServiceCallback) {
        this.clientServiceCallback = clientServiceCallback;
    }

    /**
     * Set all currently opened votings
     * @param clientVotings List of all currently opened votings
     */
    public void setClientVotings(Map<Integer, ClientVoting> clientVotings) {
        this.clientVotings = clientVotings;
    }

    public void setSubscirbedVoting(boolean subscirbedVoting) {
        this.subscirbedVoting = subscirbedVoting;
    }

    //Service methods

    /**
     * Updating the service foreground notification after the party name has changed
     */
    public void updateServiceNotifaction() {
        Log.d(TAG, "update ClientService-Notification");
        String text = getString(R.string.service_clientMsg, clientThread.getPartyName());
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        Notification notificationUpdate = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle(text)
                .setSmallIcon(R.drawable.logo_service_notification)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.icon_exit_room, getString(R.string.text_end), pendingIntentButton)
                .build();
        mNotificationManager.notify(Constants.NOTIFY_ID, notificationUpdate);
    }

    /**
     * Creating a new ClientThread to communicate with the
     * {@link com.tinf19.musicparty.server.HostService}
     * @param ipAddress Connection ip-address inserted by the user
     * @param password Connection password inserted by the user
     * @param username Username inserted by the user
     */
    public void connect(String ipAddress, String password, String username){
        clientThread = new ClientThread(ipAddress, password, username);
        clientThread.start();
    }

    /**
     * @return Get ClientThread
     */
    public ClientThread getClientThread() {
        return clientThread;
    }

    /**
     * Leaving the connection to the server and closing the ClientThread
     * @throws IOException when the thread or the socket cannot be closed
     */
    public void exit() throws IOException {
        Log.d(TAG, "ClientThread exits service");
        stopped = true;
        clientThread.out.close();
        clientThread.input.close();
        clientSocket.close();
        clientServiceCallback.exitService(getString(R.string.service_serverClosed));
    }

    public void setTrack() {
        if(nowPlaying != null)
            clientServiceCallback.setTrack(nowPlaying);
    }


    /**
     * Subclass for managing the communication with the server
     * @author Jannik Junker
     * @author Silas Wessely
     * @see com.tinf19.musicparty.server.HostService
     * @since 1.1
     */
    class ClientThread extends Thread {

        private final String address;
        private final String password;
        private final String username;
        private BufferedReader input;
        private DataOutputStream out;
        private String partyName;


        /**
         * Constructor
         * @param address IP-Address to connect to the server in the same network
         * @param password Password for guarantee a save connection
         * @param username Username to identify the user from the server
         */
        public ClientThread(String address, String password, String username) {
            this.address = address;
            this.password = password;
            this.username = username;
        }

        /**
         * @return Get the current party name
         */
        public String getPartyName() {
            return partyName;
        }

        /**
         * Sending a command and a message to the server
         * @param commands communication command for the server
         * @param message attributes for mapping the command successfully
         * @throws IOException when the output-stream cannot write or flush
         */
        public void sendMessage(Commands commands, String message) throws IOException {
            Log.d(TAG, "sending command: " + commands.toString() + ", message: " + message);
            out.writeBytes(String.format("%s%s%s%s\n\r" , Constants.DELIMITER, commands.toString(), Constants.DELIMITER, message));
            out.flush();
        }

        /**
         * Splitting the server message to communicate with the server by different commands:
         * LOGIN:       logging in to the server with a username, ip-address and a password
         * QUIT:        quit the connection to the server
         * PLAYING:     get the current playing track from the server
         * PLAYLIST:    get a list of tracks which is equal to the current state of the playlist in
         *              the server
         * VOTING:      get a list of all currently opened votings
         * VOTE_RESULT: Asking for the current result of a specific voting
         * VOTE_ADDED:  After a voting was added in the server all clients will receive the
         *              information about this voting and save it locally in the clientVotings.
         *              If the client is currently displaying
         *              {@link com.tinf19.musicparty.fragments.VotingFragment} the RecyclerView will
         *              be updated.
         *              If the voting is a QUEUE-Voting the client will generate or queue a
         *              notification about this voting.
         * VOTE_CLOSED: After a voting was closed by the server all clients will receive the
         *              information about the id and the type of the voting. The voting will be
         *              removed from the clientVotings-List.
         *              If the client is currently displaying
         *              {@link com.tinf19.musicparty.fragments.VotingFragment} the RecyclerView will
         *              be updated.
         *              If the voting is a QUEUE-Voting the client will update or remove the
         *              votingNotification.
         */
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
                    if(clientSocket == null) clientServiceCallback.exitService(getString(R.string.service_clientConnectionError));
                }).start();
                clientSocket = new Socket(this.address, Constants.PORT);
                new Thread(() -> {
                    while(clientServiceCallback == null);
                    clientServiceCallback.showFragments();
                }).start();
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.ISO_8859_1));
                out = new DataOutputStream(clientSocket.getOutputStream());
                sendMessage(Commands.LOGIN, this.username + Constants.DELIMITER + this.password);
                Log.d(TAG, "Connect successful");
                while (!this.isInterrupted() && !clientSocket.isClosed())  {
                    String line = input.readLine();
                    if (line != null && !line.equals("")) {
                        Log.d(TAG, "run: " + line);
                        String [] parts = line.split(Constants.DELIMITER);
                        String attribute = "";
                        if (parts.length > 2)
                            attribute = parts[2];
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            switch (command) {
                                case LOGIN:
                                    Log.d(TAG, "logged in to: " + partyName);
                                    partyName = attribute;
                                    setPartyType(parts[3].equals(HostService.PartyType.AllInParty.toString()) ?
                                            HostService.PartyType.AllInParty :
                                            HostService.PartyType.VoteParty);
                                    if (parts.length > 4) {
                                        nowPlaying = new Track(parts[4]);
                                    }
                                    if(clientServiceCallback != null) {
                                        clientServiceCallback.setPartyName(partyName);
                                    }
                                    updateServiceNotifaction();
                                    break;
                                case QUIT:
                                    Log.d(TAG, "Server has been closed");
                                    exit();
                                    return;
                                case PARTY_TYPE:
                                    HostService.PartyType partyType = attribute.equals(HostService.PartyType.AllInParty.toString())
                                            ? HostService.PartyType.AllInParty
                                            : HostService.PartyType.VoteParty;
                                    setPartyType(partyType);
                                    if(clientServiceCallback != null)
                                        clientServiceCallback.updateVotingButton(partyType);
                                    break;
                                case PLAYING:
                                    nowPlaying = new Track(attribute);
                                     Log.d(TAG, "new track has been started: " + nowPlaying.getName());
                                     clientServiceCallback.setTrack(nowPlaying);
                                     break;
                                case PLAYLIST:
                                    List<Track> tracks = new ArrayList<>();
                                    for (int i = 3; i < parts.length; i++) {
                                        if(!parts[i].equals(""))
                                        tracks.add(new Track(parts[i]));
                                    }
                                    if(tracks.size() > 0) {
                                        clientServiceCallback.setCurrentTrack(tracks.get(0));
                                        tracks.remove(0);
                                    }
                                    Log.d(TAG, "client got playlist with length: " + tracks.size());
                                    clientServiceCallback.setPlaylist(tracks);
                                    break;
                                case VOTING:
                                    clientVotings.clear();
                                    for(int i = 3; i < parts.length; i++) {
                                        if(!parts[i].equals("")){
                                            ClientVoting voting = new ClientVoting(parts[i], (vote, id) -> {
                                                new Thread(() -> {
                                                    try {
                                                        sendMessage(Commands.VOTE, id + Constants.DELIMITER + vote);
                                                    } catch (IOException e) {
                                                        Log.e(TAG, e.getMessage(), e);
                                                    }
                                                }).start();
                                            });
                                            clientVotings.put(voting.getId(), voting);
                                        }
                                    }
                                    clientServiceCallback.setVotings(getClientVotings());
                                    setClientVotings(clientVotings);
                                    break;
                                case VOTE_RESULT:
                                    JSONObject tempObject = new JSONObject(attribute);
                                    int votingID = tempObject.getInt(Constants.ID);
                                    ClientVoting voting = clientVotings.get(votingID);
                                    if (voting != null) {
                                        voting.updateVotingResult(
                                                tempObject.getInt(Constants.YES_VOTE),
                                                tempObject.getInt(Constants.NO_VOTE),
                                                tempObject.getInt(Constants.GREY_VOTE));
                                        if (clientServiceCallback != null)
                                            clientServiceCallback.notifyVotingAdapter(votingID,
                                                    voting.getType());
                                    }
                                    break;
                                case VOTE_ADDED:
                                    ClientVoting newVoting = new ClientVoting(attribute, (vote, id) -> {
                                        new Thread(() -> {
                                            try {
                                                sendMessage(Commands.VOTE, id + Constants.DELIMITER + vote);
                                            } catch (IOException e) {
                                                Log.e(TAG, e.getMessage(), e);
                                            }
                                        }).start();
                                    });
                                    clientVotings.put(newVoting.getId(), newVoting);
                                    if(clientServiceCallback != null && subscirbedVoting)
                                        clientServiceCallback.addVoting(newVoting);
                                    if(newVoting.getType() == Type.QUE)
                                        createVotingNotification(newVoting);
                                    break;
                                case VOTE_CLOSED:
                                    int votingClosedId = Integer.parseInt(attribute);
                                    Voting votingClosed = clientVotings.get(0);
                                    if(votingClosed != null) {
                                        if (clientServiceCallback != null && subscirbedVoting)
                                            clientServiceCallback.removeVoting(votingClosedId,
                                                    votingClosed.getType());
                                        clientVotings.remove(votingClosedId);
                                        if (votingClosed.getType() == Type.QUE)
                                            notificationAfterVote(votingClosedId);
                                    }
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