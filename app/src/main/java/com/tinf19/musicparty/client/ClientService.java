package com.tinf19.musicparty.client;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tinf19.musicparty.adapter.VotingAdapter;
import com.tinf19.musicparty.receiver.ActionReceiver;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for background communication with Spotify and all clients
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class ClientService extends Service implements VotingAdapter.VotingAdapterCallback {

    private static final String TAG = ClientService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private ClientServiceCallback clientServiceCallback;
    private ClientThread clientThread;
    private Thread sendMessageThread;
    private Socket clientSocket;
    private Track nowPlaying;
    private boolean stopped;
    /**
     * Boolean to decide if the service is connected for the first time.
     */
    private boolean first = true;
    /**
     * Spotify connection token which is refreshing every hour
     */
    private String token;
    /**
     * Intent for the service notification
     */
    private PendingIntent pendingIntent;
    private PendingIntent pendingIntentButton;
    private Map<Integer, ClientVoting> clientVotings = new HashMap<>();
    private HostService.PartyType partyType = HostService.PartyType.AllInParty;



    public interface ClientServiceCallback {
        void setTrack(Track track);
        void setPartyName(String partyName);
        void exitService(String text);
        void setPlaylist(List<Track> trackList);
        void setCurrentTrack(Track track);
        void setVotings(List<Voting> ClientVotings);
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
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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

    @Override
    public Thread getCurrentThread() { return clientThread; }

    /**
     * @return Get all currently opened votings
     */
    public List<Voting> getClientVotings() { return new ArrayList<>(clientVotings.values()); }

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

    public void fetchVotingResult() {
        clientVotings.keySet().forEach(v-> {
            try {
                clientThread.sendMessage(Commands.VOTERESULT, String.valueOf(v));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
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
         * PLAYING:     get the current playling track from the server
         * PLAYLIST:    get a list of tracks which is equal to the current state of the playlist in
         *              the server
         * VOTING:      get a list of all currently opened votings
         * VOTE_RESULT: Asking for the current result of a specific voting
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
                                    setPartyType(parts[3].equals("AllInParty") ?
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
                                case PARTYTYPE:
                                    HostService.PartyType partyType = attribute.equals("AllInParty")
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
                                case VOTERESULT:
                                    JSONObject tempObject = new JSONObject(attribute);
                                    int votingID = tempObject.getInt(Constants.ID);
                                    ClientVoting voting = clientVotings.get(votingID);
                                    if (voting != null) {
                                        if(tempObject.getBoolean(Constants.FINISHED_VOTE)) {
                                            if (clientServiceCallback != null)
                                                clientServiceCallback.removeVoting(voting.getId(), voting.getType());
                                            clientVotings.remove(votingID);
                                        } else {
                                            voting.updateVotingResult(
                                                    tempObject.getInt(Constants.YES_VOTE),
                                                    tempObject.getInt(Constants.NO_VOTE),
                                                    tempObject.getInt(Constants.GREY_VOTE));
                                            if (clientServiceCallback != null)
                                                clientServiceCallback.notifyVotingAdapter(votingID
                                                        , voting.getType());
                                        }
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