package com.tinf19.musicparty.server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tinf19.musicparty.client.PartyActivity;
import com.tinf19.musicparty.fragments.HostClosePartyFragment;
import com.tinf19.musicparty.fragments.HostPlaylistFragment;
import com.tinf19.musicparty.fragments.PartyPeopleFragment;
import com.tinf19.musicparty.fragments.SearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.fragments.SettingsHostFragment;
import com.tinf19.musicparty.fragments.ShowSongHostFragment;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.HostPlaylistRecycAdapter;
import com.tinf19.musicparty.util.WiFiDirectBroadcastReceiver;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HostActivity extends AppCompatActivity implements ServerService.SpotifyPlayerCallback, SearchBarFragment.SearchForSongs, ShowSongHostFragment.OpenHostFragments, SearchSongsOutputFragment.AddSongCallback, HostPlaylistFragment.PlaylistCallback, HostClosePartyFragment.ClosePartyCallback, PartyPeopleFragment.PartyPeopleList, SettingsHostFragment.GetServerSettings, HostPlaylistRecycAdapter.HostPlaylistAdapterCallback {

    private static final String TAG = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final String PASSWORD = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);

    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private ServerService mBoundService;

//    Music Party standard name
    private String partyName = "Music Party";
    private String token;
    private ArrayList<PartyPeople> partyPeople;
    private boolean mShouldUnbind;

    private ShowSongHostFragment showSongFragment;
    private SearchBarFragment searchBarFragment;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private HostClosePartyFragment hostClosePartyFragment;
    private SettingsHostFragment settingsHostFragment;
    private HostPlaylistFragment hostPlaylistFragment;
    private PartyPeopleFragment partyPeopleFragment;

    public interface ConnectionCallback {
        void afterConnection(SpotifyAppRemote appRemote);
    }


//    methods and objects for ServerService-Connection
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServerService.LocalBinder)service).getService();
            connect(appRemote -> {
                Intent serviceIntent = new Intent(HostActivity.this, ServerService.class);
                serviceIntent.putExtra(Constants.TOKEN, token);
                serviceIntent.putExtra(Constants.PASSWORD, PASSWORD);
                startService(serviceIntent);
            });
            // Tell the user about this for our demo.
            Toast.makeText(HostActivity.this, getString(R.string.service_serverConnected), Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(HostActivity.this, getString(R.string.service_serverDisconnected),
                    Toast.LENGTH_SHORT).show();
        }
    };

    private void connect(ConnectionCallback connectionCallback) {
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(false)
                        .build();
        SpotifyAppRemote.connect(HostActivity.this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        if(mBoundService != null)
                            mBoundService.setmSpotifyAppRemote(spotifyAppRemote);
                        Log.d(NAME, "Connected! Yay!");
                        //mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");
                        // Now you can start interacting with App Remote
                        connectionCallback.afterConnection(spotifyAppRemote);

                        if(mBoundService != null) {
                            mBoundService.setSpotifyPlayerCallback(HostActivity.this);
                            mBoundService.addEventListener();
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(NAME, throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    void doBindService() {
        if (bindService(new Intent(this, ServerService.class),
                mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        } else {
            Log.e(TAG, "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            mBoundService.setSpotifyPlayerCallback(null);
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    public void stopService() {
        if(mBoundService != null) {
            mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
            SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        }
        doUnbindService();
        stopService(new Intent(this, ServerService.class));
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }



//    Interaction with Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_party);

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

        token = getIntent().getStringExtra(Constants.TOKEN);

        doBindService();

        searchBarFragment = new SearchBarFragment(this, getIntent().getStringExtra(Constants.TOKEN));
        showSongFragment = new ShowSongHostFragment(this);
        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
        hostClosePartyFragment = new HostClosePartyFragment(this);
        settingsHostFragment = new SettingsHostFragment(this);
        hostPlaylistFragment = new HostPlaylistFragment(this, this);
        partyPeopleFragment = new PartyPeopleFragment(this);

        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongHostFragment").commitAllowingStateLoss();
        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarHostFragmentFrame, searchBarFragment, "SearchBarFragment").commitAllowingStateLoss();
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
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
            SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        doUnbindService();
        Log.d(TAG, "I got destroyed");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "I have been stopped");
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongFragment").commitAllowingStateLoss();
    }



    //  changing fragment source

    @Override
    public void searchForSongs(List<Track> tracks) {
        Log.d("ShowSongFragment", "back to show");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, searchSongsOutputFragment, "ShowSongFragment").commitAllowingStateLoss();
        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
    }

    @Override
    public void openSettingsFragment() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, settingsHostFragment, "SettingsHostFragment").commitAllowingStateLoss();
    }

    @Override
    public void openPeopleFragment() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, partyPeopleFragment, "PartyPeopleFragment").commitAllowingStateLoss();
    }

    @Override
    public void openPlaylistFragment() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, hostPlaylistFragment, "HostPlaylistFragment").commitAllowingStateLoss();
    }

    @Override
    public void openExitFragment() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, hostClosePartyFragment, "HostClosePartyFragment").commitAllowingStateLoss();
    }



//    Methods for ShowSongFragment

    @Override
    public void setNowPlaying(Track nowPlaying) {
        showSongFragment.setNowPlaying(nowPlaying);
    }

    @Override
    public void setPeopleCount(int count) { this.runOnUiThread(()->showSongFragment.setPartyNameCount(count)); }

    @Override
    public int getPartyPeopleSize() {
        if(mBoundService != null) return mBoundService.getClientListSize();
        else return 0;
    }

    @Override
    public String getPartyPeoplePartyName() {
        return partyName;
    }

    @Override
    public void nextTrack() {
        if(mBoundService != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
    }

    @Override
    public void lastTrack() {
        if(mBoundService != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipPrevious();
    }

    @Override
    public void playTrack() {
        if (mBoundService != null && mBoundService.getPause()) mBoundService.getmSpotifyAppRemote().getPlayerApi().resume();
        else if(mBoundService != null)  mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
    }

    @Override
    public boolean getPauseState() {
        return mBoundService != null && mBoundService.getPause();
    }

    @Override
    public Track setShowNowPlaying() {
        if(mBoundService != null) return mBoundService.getNowPlaying();
        else return null;
    }



//    Methods for SearchSongsOutput

    @Override
    public void addSong(Track track) {
        this.runOnUiThread(() -> Toast.makeText(HostActivity.this, track.getName() + " " + getText(R.string.text_queAdded), Toast.LENGTH_SHORT).show());
        new Thread(() -> {
            try {
                Log.d(TAG, "Trying to send message to server");
                if(mBoundService != null) {
                    mBoundService.addItem(track.getURI(), track.getName());
                    mBoundService.addItemToTrackList(track);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }).start();
    }



//    Methods for HostPlaylist

    @Override
    public void showPlaylist() {
        if(mBoundService != null) {
            List<Track> trackList = mBoundService.getPlaylist();
            this.runOnUiThread(() -> hostPlaylistFragment.showResult(trackList));
        }
    }

    public void nextSong(View view) {
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
        else if(mBoundService != null)
            connect(appRemote -> appRemote.getPlayerApi().skipNext());

    @Override
    public Track getCurrentPlaying() {
        if(mBoundService != null) return mBoundService.getNowPlaying();
        else return null;
    }

    @Override
    public void swapPlaylistItems(int from, int to) {
        if(mBoundService != null) {
            try {
                mBoundService.moveItem(from, to);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



//    Methods for HostCloseParty

    @Override
    public void denyEndParty() {
        onBackPressed();
    }

    @Override
    public void acceptEndParty() {
        stopService();
    }



//    Methods for PartyPeople

    @Override
    public ArrayList<PartyPeople> getPartyPeopleList() {
        if(mBoundService != null) return (ArrayList<PartyPeople>) mBoundService.getPeopleList();
        else return new ArrayList<>();
    }



//    Methods for SettingsHost

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

    @Override
    public String getIpAddress() {
        return getIPAddress(true);
    }

    @Override
    public String getPassword() {
        return PASSWORD;
    }

    @Override
    public void setNewPartyName(String newPartyName) {
        this.partyName = newPartyName;
        if(mBoundService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mBoundService.sendToAll(Commands.LOGIN, partyName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}