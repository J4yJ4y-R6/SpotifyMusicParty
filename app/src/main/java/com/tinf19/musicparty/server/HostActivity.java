package com.tinf19.musicparty.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.SpotifyConnectionTerminatedException;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.tinf19.musicparty.BuildConfig;
import com.tinf19.musicparty.fragments.LoadingFragment;
import com.tinf19.musicparty.server.fragments.HostClosePartyFragment;
import com.tinf19.musicparty.server.fragments.HostPlaylistFragment;
import com.tinf19.musicparty.server.fragments.HostSearchBarFragment;
import com.tinf19.musicparty.server.fragments.HostPartyPeopleFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.server.fragments.HostSettingsFragment;
import com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment;
import com.tinf19.musicparty.server.fragments.HostSongFragment;
import com.tinf19.musicparty.music.PartyPerson;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.fragments.VotingFragment;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.server.adapter.HostPlaylistAdapter;
import com.tinf19.musicparty.server.adapter.HostFavoritePlaylistsAdapter;
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;
import com.tinf19.musicparty.util.WiFiDirectBroadcastReceiver;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Activity to ensure the communication between every action the host does and the service which is
 * managing the background communication when the app is closed and with the Spotify-API. In this
 * way the connection between the server and the clients can be hold up, even if the host switches
 * between apps.
 * <br>
 * <b>Main exercice of the activity</b>
 * Also this activity is managing the visibility off all fragments, so the host can switch between
 * then without losing any information. There are two {@link android.widget.FrameLayout} in this
 * activity. The upper one is constantly visible and displays a input field for searching about
 * songs in the Spotify-API and a button formed as a heart which is opening the
 * {@link HostFavoritePlaylistsFragment} where all saved playlist from passed parties are shown.
 * The lower, bigger fragment is change its content depending on the user input. But mainly it is
 * used to diplay information about the currently playing song and the current state of the queue.
 * In this way the host is always able to check on these information.
 * These fragments are initialized in the {@link HostActivity#onCreate(Bundle)} method. Closely
 * every fragment has its own callback to communicate with the {@link HostActivity}. So the main
 * exercise of this activity is to hand off the given information to the {@link HostService}.
 * <br>
 * <b>Connection between server and client</b>
 * At the initialization of this activity the app is connecting to Spoitfy by opening a login mask
 * given by the Spotify-API. If the user has signed in once, these information will be saved and the
 * at the next app start, it will automatically connect to Spoitfy. In the next step the activity is
 * opening a server and simultaneously with it a port {@value #PORT} so clients can connect to it.
 * Here clients are users who choose "join party" in the {@link MainActivity}. The connection can is
 * build with a {@link java.net.Socket} and can only build up when the end devices are connected to
 * the same network. The client is typing in a IP-Address and a password and only if they are
 * similar to the once offered by the host, a connection will build up.
 * <br>
 * <b>Connection between server and Spotify-Remote-Control</b>
 * The Spotify-Remote-Control controls actions in the Spotify-App. It offers the opportunity to
 * pause and resume a song, play the next song in the queue or go one song backwards to listen to it
 * again. In our integration the back-button will go to the start of the currently playing song if
 * it is playing at least for two seconds. If the played duration is below that time, the last song
 * will be started. In the {@link HostPlaylistFragment} the host can change the queue order or
 * delete songs from the queue. And from the {@link HostFavoritePlaylistsFragment} the host can
 * start any stored playlist from as passed party.
 * The connection to the remote control ist opened in the {@link HostActivity#mConnection} with its
 * own callback for controlling. Highly necessary for this are the CLIENT_ID which is stored
 * secretly and die REDIRECT_URI: {@value REDIRECT_URI}. These are also used to authenticate in
 * front of the Spotify-API
 * @author Jannik Junker
 * @author Silas Wessely
 * @see android.app.Service
 * @see SpotifyAppRemote
 * @see Constants
 * @see java.net.Socket
 * @since 1.1
 */
public class HostActivity extends AppCompatActivity {

    private static final String TAG = HostActivity.class.getName();
    public static final int PORT = Constants.PORT;
    public static final String REDIRECT_URI = Constants.REDIRECT_URI;
    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    /**
     * The server Bound {@link android.app.Service} for connecting background connection with the
     * {@link com.tinf19.musicparty.client.ClientService} and the {@link SpotifyAppRemote}
     */
    private HostService mBoundService;
    /**
     * Indentify if the service is currently bound
     */
    private boolean mShouldUnbind;
    /**
     * A boolean to decide in the {@link HostActivity#onStart()} method whether the service should
     * be stopped or not. If it is true the service get stopped if not the activity and the service
     * will keep running.
     */
    private boolean stopped;
    private HostSongFragment showSongFragment;
    private HostSearchBarFragment hostSearchBarFragment;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private HostClosePartyFragment hostClosePartyFragment;
    private HostSettingsFragment hostSettingsFragment;
    private HostPlaylistFragment hostPlaylistFragment;
    private HostPartyPeopleFragment hostPartyPeopleFragment;
    private HostFavoritePlaylistsFragment hostFavoritePlaylistsFragment;
    private VotingFragment votingFragment;
    private LoadingFragment loadingFragment;
    private String password;


    public interface HostActivityCallback {
        void afterConnection(SpotifyAppRemote appRemote);
        void afterFailure();
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "user stopped server by service-notification");
            stopService();
        }
    };


    //Android lifecycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_host);

        registerReceiver(broadcastReceiver, new IntentFilter(Constants.STOP));

        hostSearchBarFragment = new HostSearchBarFragment(new HostSearchBarFragment.HostSearchBarCallback() {
            @Override
            public void searchForSongs(List<Track> tracks) {
                animateFragmentChange(true, searchSongsOutputFragment, "SearchSongOutputFragment");
                HostActivity.this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
            }

            @Override
            public void openSavedPlaylistsFragment() {
                animateFragmentChange(true, hostFavoritePlaylistsFragment, "ShowSavedPlaylistFragment");
            }

            @Override
            public String getToken() {
                return getHostToken();
            }
        });
        showSongFragment = new HostSongFragment(new HostSongFragment.HostSongCallback() {
            @Override
            public void openSettingsFragment() {
                animateFragmentChange(true, hostSettingsFragment, "SettingsHostFragment");
            }

            @Override
            public void openPeopleFragment() {
                animateFragmentChange(true, hostPartyPeopleFragment, "PartyPeopleFragment");
            }

            @Override
            public void openPlaylistFragment() {
                animateFragmentChange(true, hostPlaylistFragment, "HostPlaylistFragment");
            }

            @Override
            public void openExitFragment() {
                animateFragmentChange(true, hostClosePartyFragment, "HostClosePartyFragment");
            }

            @Override
            public void openVotingFragment() {
                animateFragmentChange(true, votingFragment, "VotingFragment");
            }

            @Override
            public void nextTrack() {
                if (mBoundService != null)
                    if(!mBoundService.isPlaylistEnded())
                        mBoundService.next();
                    else
                        mBoundService.restartQue();
            }

            @Override
            public void lastTrack() {
                if(mBoundService != null) {
                    mBoundService.back();
                }
            }

            @Override
            public void playTrack() {
                if(mBoundService != null)
                    mBoundService.togglePlayback();
            }

            @Override
            public int getPartyPeopleSize() {
                if (mBoundService != null) {
                    int partySize = mBoundService.getClientListSize();
                    Log.d(TAG, "current party size: " + partySize);
                    return partySize;
                }
                else return 0;
            }

            @Override
            public String getPartyPeoplePartyName() {
                String partyName = mBoundService != null ? mBoundService.getPartyName() : getString(R.string.text_partyName);
                Log.d(TAG, "current party name: " + partyName);
                return partyName;
            }

            @Override
            public HostService.PartyType getPartyType() {
                return mBoundService != null ? mBoundService.getPartyType() :
                        HostService.PartyType.AllInParty;
            }

            @Override
            public boolean getPauseState() {
                return mBoundService != null && mBoundService.getPause();
            }

            @Override
            public Track setShowNowPlaying() {
                if (mBoundService != null) return mBoundService.getNowPlaying();
                else return null;
            }
        });
        searchSongsOutputFragment = new SearchSongsOutputFragment(track -> {
            HostActivity.this.runOnUiThread(() -> Snackbar.make(findViewById(
                    R.id.showSongHostFragmentFrame), track.getName() + " " + getText(R.string.text_queAdded),
                    Snackbar.LENGTH_SHORT).show());
            new Thread(() -> {
                if (mBoundService != null) {
                    mBoundService.addItemToPlaylist(track);
                }
            }).start();
        });
        hostClosePartyFragment = new HostClosePartyFragment(new HostClosePartyFragment.HostClosePartyCallback() {
            @Override
            public void denyEndParty() {
                Log.d(TAG, "host has denied closing the party");
                onBackPressed();
            }

            @Override
            public void acceptEndParty() { stopService(); }

            @Override
            public void createPlaylistFromArrayList(String name) throws JSONException {
                if(mBoundService != null)
                    mBoundService.createPlaylist(name, true);
            }
        });
        hostSettingsFragment = new HostSettingsFragment(new HostSettingsFragment.HostSettingsCallback() {
            @Override
            public String getIpAddress() {
                return getIPAddress(true);
            }

            @Override
            public String getPassword() {
                return generatePassword();
            }

            @Override
            public void setNewPartyName(String newPartyName) {
                if (mBoundService != null) {
                    Log.d(TAG, "party name has changed to: " + newPartyName);
                    mBoundService.setPartyName(newPartyName);
                    new Thread(() -> {
                        try {
                            mBoundService.sendToAll(Commands.LOGIN, mBoundService.getPartyName());
                            mBoundService.updateServiceNotifaction();
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }).start();
                }
            }

            @Override
            public void changePartyType(HostService.PartyType partyType) {
                if(mBoundService != null) {
                    try {
                        mBoundService.setPartyType(partyType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        hostPlaylistFragment = new HostPlaylistFragment(new HostPlaylistFragment.HostPlaylistCallback() {
            @Override
            public void showPlaylist() {
                if (mBoundService != null) {
                    List<Track> trackList = mBoundService.getPlaylist();
                    runOnUiThread(() -> hostPlaylistFragment.showResult(trackList));
                }
            }

            @Override
            public Track getCurrentPlaying() {
                if (mBoundService != null) return mBoundService.getNowPlaying();
                else return null;
            }
        }, new HostPlaylistAdapter.HostPlaylistAdapterCallback() {

            @Override
            public void swapPlaylistItems(int from, int to) {
                /*if (mBoundService != null) {
                    try {
                        mBoundService.moveItem(from, to);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }*/
            }

            @Override
            public void removeItem(Track toRemove, int position, HostService.AfterCallback callback) {
                if(mBoundService != null) {
                    mBoundService.deleteFromQue( position, () -> runOnUiThread(callback::deleteFromDataset));
                }
            }
        });
        hostPartyPeopleFragment = new HostPartyPeopleFragment(() -> {
            if (mBoundService != null) return (ArrayList<PartyPerson>) mBoundService.getPeopleList();
            else return new ArrayList<>();
        });
        hostFavoritePlaylistsFragment = new HostFavoritePlaylistsFragment(new HostFavoritePlaylistsFragment.HostFavoritePlaylistCallback() {
            @Override
            public void changePlaylistCover(String id, Bitmap image) {
                if (mBoundService != null) {
                    new Thread(() -> {
                        mBoundService.updatePlaylistCover(id, image);
                    }).start();
                }
            }

            @Override
            public String getToken() {
                return getHostToken();
            }
        }, new HostFavoritePlaylistsAdapter.HostFavoritePlaylistAdapterCallback() {
            @Override
            public void playFavoritePlaylist(String id, ArrayList<String> idList) {
                if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)  {
                    mBoundService.setPlaylistID(id);
                    mBoundService.getQueFromPlaylist(id);
                    try {
                        mBoundService.checkPlaylistFollowStatus(id);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }

            @Override
            public void changePlaylistName(String name, String id) {
                if(mBoundService != null) {
                    new Thread(() -> {
                        try {
                            mBoundService.updatePlaylistName(name, id);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }).start();
                }
            }

            @Override
            public void deletePlaylist(String id) {
                notifyFavPlaylistAdapter();
                if(mBoundService != null)
                    mBoundService.deletePlaylist(id);
            }
        });
        votingFragment = new VotingFragment(
                () -> mBoundService != null ? mBoundService.getCurrentThread() : null,
                new VotingFragment.VotingCallback() {
                    @Override
                    public List<Voting> getVotings() {
                        return mBoundService != null ? mBoundService.getHostVotings()
                                : new ArrayList<>();
                    }

                    @Override
                    public void stopTimer() {
                        //
                    }
                });
        loadingFragment = new LoadingFragment(getString(R.string.text_loadingServer));

        if(savedInstanceState != null) {
            password = savedInstanceState.getString(Constants.PASSWORD, "0000");
            mBoundService = savedInstanceState.getParcelable(Constants.SERVICE);
            String currentFragmentTag = savedInstanceState.getString(Constants.TAG, "ShowSongHostFragment");
            if(!currentFragmentTag.equals("")) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.showSongHostFragmentFrame, currentFragment, currentFragmentTag);
            }
        } else {
            if(!getIntent().getBooleanExtra(Constants.FROM_NOTIFICATION, false))
                getSupportFragmentManager().beginTransaction().
                    replace(R.id.showSongHostFragmentFrame, loadingFragment, "LoadingFragment").commitAllowingStateLoss();
            else
                showDefaultFragments();

        }

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        doBindService();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.PASSWORD, password);
        String tag = "";
        if(hostFavoritePlaylistsFragment != null && hostFavoritePlaylistsFragment.isVisible())
            tag = hostFavoritePlaylistsFragment.getTag();
        if(showSongFragment != null && showSongFragment.isVisible())
            tag = showSongFragment.getTag();
        if(hostSettingsFragment != null && hostSettingsFragment.isVisible())
            tag = hostSettingsFragment.getTag();
        if(searchSongsOutputFragment != null && searchSongsOutputFragment.isVisible())
            tag = searchSongsOutputFragment.getTag();
        if(hostPartyPeopleFragment != null && hostPartyPeopleFragment.isVisible())
            tag = hostPartyPeopleFragment.getTag();
        if(hostPlaylistFragment != null && hostPlaylistFragment.isVisible())
            tag = hostPlaylistFragment.getTag();
        if(hostClosePartyFragment != null && hostClosePartyFragment.isVisible())
            tag = hostClosePartyFragment.getTag();
        outState.putString(Constants.TAG, tag);
        outState.putParcelable(Constants.SERVICE, mBoundService);
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
        doUnbindService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(stopped) {
            stopService();
        }
    }

    @Override
    public void onBackPressed() {
        if(showSongFragment.isVisible()) {
            animateFragmentChange(true, hostClosePartyFragment, "ExitConnectionFragment");
        } else if(loadingFragment.isVisible()){
          stopService();
        } else {
            hostSearchBarFragment.clearSearch();
            animateFragmentChange(false, showSongFragment, "ShowSongHostFragment");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == Constants.REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                case CODE:
                    Log.d(TAG, "got code from Spotify successfully");
                    Intent serviceIntent = new Intent(HostActivity.this, HostService.class);
                    serviceIntent.putExtra(Constants.CODE, response.getCode());
                    serviceIntent.putExtra(Constants.PASSWORD, generatePassword());
                    serviceIntent.putExtra(Constants.PARTYNAME, getString(R.string.text_partyName));
                    startService(serviceIntent);
                    break;
                case ERROR:
                    // Handle error response
                    Log.e(TAG, "Spotify login error: " + response.getError());
                    stopService();
                    break;
                default:
                    // Handle other cases
                    Log.e(TAG, "Something went wrong");
            }
        }
    }



    //Service methods

    private ServiceConnection mConnection = new ServiceConnection() {
        /**
         * Assigning the service after logging in to Spotify or closing the service after ending
         * the party
         * @param className Class name of the service
         * @param service Service binder to assign the service
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service has been connected");
            mBoundService = ((HostService.LocalBinder) service).getService();
            mBoundService.setHostServiceCallback(new HostService.HostServiceCallback() {

                @Override
                public void setNowPlaying(Track nowPlaying) {
                    if(showSongFragment.isVisible())
                        runOnUiThread(() ->showSongFragment.setNowPlaying(nowPlaying));
                }

                @Override
                public void setPeopleCount(int count) {
                    if(showSongFragment.isVisible())
                        runOnUiThread(() -> showSongFragment.setPartyNameCount(count));
                }

                @Override
                public void setPlayImage(boolean pause) {
                    if(showSongFragment.isVisible())
                        showSongFragment.setPlayTrackButtonImage(pause);
                }

                @Override
                public void connect(HostActivityCallback hostActivityCallback) {
                    runOnUiThread(() -> {
                        ConnectionParams connectionParams =
                                new ConnectionParams.Builder(BuildConfig.CLIENT_ID)
                                        .setRedirectUri(Constants.REDIRECT_URI)
                                        .showAuthView(false)
                                        .build();
                        SpotifyAppRemote.connect(HostActivity.this, connectionParams,
                                new Connector.ConnectionListener() {
                                    @Override
                                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                        Log.d(TAG, "connected to spotify remote control");
                                        hostActivityCallback.afterConnection(spotifyAppRemote);
                                        if(mBoundService != null && mBoundService.isFirst()) {
                                            HostActivity.this.runOnUiThread(()-> Snackbar.make(
                                                    findViewById(R.id.showSongFragmentFrame),
                                                    getString(R.string.service_serverMsg, getString(R.string.text_partyName)),
                                                    Snackbar.LENGTH_SHORT).show());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        if(throwable instanceof SpotifyConnectionTerminatedException) {
                                            Log.d(TAG, "connection to spotify remote control failed");
                                            hostActivityCallback.afterFailure();
                                        } else if(throwable instanceof CouldNotFindSpotifyApp) {
                                            final String appPackageName = "com.spotify.music";
                                            stopped = true;
                                            HostActivity.this.runOnUiThread(()-> Snackbar.make(
                                                    findViewById(R.id.showSongFragmentFrame),
                                                    getString(R.string.text_noSpotifyFound),
                                                    Snackbar.LENGTH_SHORT).show());
                                            try {
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                            } catch (android.content.ActivityNotFoundException anfe) {
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                            }
                                        }
                                        else
                                            Log.e(TAG, throwable.getMessage(), throwable);
                                    }
                                });
                    });
                }

                @Override
                public void notifyFavPlaylistAdapter() {
                    Log.d(TAG, "favorite playlists has been changed");
                    HostActivity.this.notifyFavPlaylistAdapter();
                }

                @Override
                public void notifyVotingAdapter(int id, Type type) {
                    Log.d(TAG, "voting submitted has been changed");
                    if(votingFragment.isVisible())
                        votingFragment.notifySingleVote(id, type);
                }

                @Override
                public void removeVoting(int id, Type type) {
                    if(votingFragment.isVisible())
                        votingFragment.removeSingleVote(id, type);
                }

                @Override
                public void addToSharedPreferances(String name, String id) {
                    Log.d(TAG, "playlist " + name + " has been added to favorites");
                    SharedPreferences savePlaylistMemory = getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
                    if(!savePlaylistMemory.getString("29", "").equals(""))
                        Snackbar.make(findViewById(R.id.showSongFragmentFrame),
                                getString(R.string.text_toastPlaylistNotSaved),
                                Snackbar.LENGTH_LONG).show();
                    SharedPreferences.Editor editor = savePlaylistMemory.edit();
                    JSONObject playlist = new JSONObject();
                    try {
                        playlist.put("name", name);
                        playlist.put("id", id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    for(int i = 0; i < 30; i++) {
                        try {
                            if(savePlaylistMemory.getString("" + i, null) == null || id.equals(new JSONObject(savePlaylistMemory.getString("" + i, "")).getString("id"))) {
                                editor.putString("" + i, playlist.toString());
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    editor.apply();
                }

                @Override
                public void reloadPlaylistFragment() {
                    if(hostPlaylistFragment.isVisible()) hostPlaylistFragment.updateRecyclerView();
                }

                @Override
                public void showDefault() {
                    showDefaultFragments();
                }

                @Override
                public void acceptEndParty() {
                    stopService();
                }
            });
            if(mBoundService.isFirst())
                loginToSpotify();
            else {
                if(getIntent().getBooleanExtra(Constants.FROM_NOTIFICATION, false) && showSongFragment != null && showSongFragment.isVisible()) {
                    showSongFragment.setNowPlaying(mBoundService.getNowPlaying());
                    showSongFragment.setPartyNameCount(mBoundService.getClientListSize());
                }
            }
        }

        /**
         * Deleting the current service from the class
         * @param className Class name of the service
         */
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service has been disconnected");
            String partyName = mBoundService != null ? mBoundService.getPartyName() : "MusicParty";
            Snackbar.make(findViewById(R.id.showSongFragmentFrame), getString(
                    R.string.service_serverDisconnected, partyName), Snackbar.LENGTH_SHORT).show();
            mBoundService = null;
        }
    };

    /**
     * The server binds the service and set mShouldUnbind true, so the server knows a service is
     * connected
     */
    void doBindService() {
        if (bindService(new Intent(this, HostService.class),
                mConnection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "Service has been bound");
            mShouldUnbind = true;
        } else {
            Log.e(TAG, "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    /**
     * The server unbinds the service and set mShouldUnbind false, so the server knows no service
     * is connected
     */
    void doUnbindService() {
        if (mShouldUnbind) {
            Log.d(TAG, "Service has been unbound");
            // Release information about the service's state.
            mBoundService.setHostServiceCallback(null);
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    /**
     * The service is disconnecting the Spotify-Remote-Control after pausing the currently playing
     * song. Afterwards he gets unbound and stopped. The App will return to the {@link MainActivity}
     */
    public void stopService() {
        Log.d(TAG, "spotify remote control disconnected");
        if(mBoundService != null &&  mBoundService.getmSpotifyAppRemote() != null) {
            mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
            SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        }
        doUnbindService();
        stopService(new Intent(this, HostService.class));
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    /**
     * The service is opening the log-in-mask from Spotify to insert his credentials and connect
     * to his Spotify account.
     */
    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(BuildConfig.CLIENT_ID, AuthorizationResponse.Type.CODE, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "app-remote-control", "playlist-modify-private", "playlist-modify-public", "user-read-private", "ugc-image-upload"});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, Constants.REQUEST_CODE, request);
    }



    //Fragment methods

    /**
     * This method is animating the change between two fragments. The animation type depends on the
     * direction given as an attribute but it's always sliding in or out.
     * @param direction Animation direction
     * @param fragment New fragment to open in the bigger Framelayout
     * @param tag Tag of the new fragment
     */
    public void animateFragmentChange(boolean direction, Fragment fragment, String tag) {
        Log.d(TAG, "Fragment has been changed to " + tag);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(direction)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_up, R.anim.fragment_slide_out_up);
        else
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_down, R.anim.fragment_slide_in_down);
        fragmentTransaction.replace(R.id.showSongHostFragmentFrame, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Showing the default fragments at the activity start. The default fragment for the lower
     * FrameLayout is the {@link HostSongFragment} where all information about the current song are
     * displayed and the control bar to switch to another fragment.
     */
    private void showDefaultFragments() {
        Log.d(TAG, "Fragment has been changed to: HostSongFragment");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongHostFragment").commitAllowingStateLoss();
        Log.d(TAG, "TopFragment has been changed to: HostSearchBarFragment");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarHostFragmentFrame, hostSearchBarFragment, "HostSearchBarFragment").commitAllowingStateLoss();
    }

    /**
     * Reloading the {@link HostFavoritePlaylistsFragment} because a playlist cover got updated or
     * a playlist has been deleted from the SharedPreferences.
     */
    private void notifyFavPlaylistAdapter() {
        Fragment frg = null;
        frg = getSupportFragmentManager().findFragmentByTag("ShowSavedPlaylistFragment");
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.detach(frg);
        ft.attach(frg);
        ft.commit();
    }

    /**
     * @return Get the current Spotify-Token which is necessary for API-Requests and needs to be
     * refreshed every 60 Minutes
     */
    private String getHostToken() { return mBoundService != null ? mBoundService.getToken() : null; }

    /**
     * @return Gernerating a random number with 4 chars which is used as the password to join the
     * party
     */
    private String generatePassword() {
        if(password == null) password = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
        return password;
    }

    /**
     * @param useIPv4 Decide whether the host has a IPv4-Address or an IPv6-Address
     * @return Get the IP-Address from the host
     */
    private String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }
}