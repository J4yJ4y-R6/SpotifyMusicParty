package com.tinf19.musicparty.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.tinf19.musicparty.fragments.HostClosePartyFragment;
import com.tinf19.musicparty.fragments.HostPlaylistFragment;
import com.tinf19.musicparty.fragments.HostSearchBarFragment;
import com.tinf19.musicparty.fragments.PartyPeopleFragment;
import com.tinf19.musicparty.fragments.SearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.fragments.ServerLoadingFragment;
import com.tinf19.musicparty.fragments.SettingsHostFragment;
import com.tinf19.musicparty.fragments.ShowSavedPlaylistsFragment;
import com.tinf19.musicparty.fragments.ShowSongHostFragment;
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
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.tinf19.musicparty.util.Constants.STATE_COUNTER;

public class HostActivity extends AppCompatActivity implements ServerService.SpotifyPlayerCallback, SearchBarFragment.SearchForSongs, ShowSongHostFragment.OpenHostFragments, SearchSongsOutputFragment.AddSongCallback, HostPlaylistFragment.PlaylistCallback, HostClosePartyFragment.ClosePartyCallback, PartyPeopleFragment.PartyPeopleList, SettingsHostFragment.GetServerSettings, HostPlaylistRecycAdapter.HostPlaylistAdapterCallback, HostSearchBarFragment.HostSearchForSongs, ShowSavedPlaylistsFragment.FavoritePlaylistsCallback {

    private static final String TAG = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final int REQUEST_CODE = 1337;
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_SERVICE = "service";
    private static final String STATE_TAG = "tag";
    private int mCounter;
    private String password;
    private static HostActivity hostActivity;

    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private ServerService mBoundService;

    //private String token;
    private boolean mShouldUnbind;

    private FragmentTransaction fragmentTransaction;
    private ShowSongHostFragment showSongFragment;
    private HostSearchBarFragment hostSearchBarFragment;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private HostClosePartyFragment hostClosePartyFragment;
    private SettingsHostFragment settingsHostFragment;
    private HostPlaylistFragment hostPlaylistFragment;
    private PartyPeopleFragment partyPeopleFragment;
    private ShowSavedPlaylistsFragment showSavedPlaylistsFragment;


    public interface ConnectionCallback {
        void afterConnection(SpotifyAppRemote appRemote);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopService();
        }
    };

    //    methods and objects for ServerService-Connection
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServerService.LocalBinder) service).getService();
            loginToSpotify();
            /*connect(appRemote -> {
                Intent serviceIntent = new Intent(HostActivity.this, ServerService.class);
                serviceIntent.putExtra(Constants.TOKEN, token);
                serviceIntent.putExtra(Constants.PASSWORD, generatePassword());
                serviceIntent.putExtra(Constants.PARTYNAME, getString(R.string.text_partyName));
                startService(serviceIntent);
            });*/
            // Tell the user about this for our demo.
            if(mBoundService != null)
                Toast.makeText(HostActivity.this, getString(R.string.service_serverConnected, mBoundService.getPartyName()), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(HostActivity.this, getString(R.string.service_serverConnected, "MusicParty"), Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(HostActivity.this, getString(R.string.service_serverDisconnected),
                    Toast.LENGTH_SHORT).show();
        }
    };

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
        if(mBoundService != null &&  mBoundService.getmSpotifyAppRemote() != null) {
            Log.d(TAG, "stopService: Pausing music");
            mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
            SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        }
        doUnbindService();
        stopService(new Intent(this, ServerService.class));
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private String generatePassword() {
        if(password == null) password = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
        return password;
    }

    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.CODE, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "app-remote-control", "playlist-modify-private", "playlist-modify-public", "user-read-private", "ugc-image-upload"});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    //token = response.getAccessToken();
                    Log.d(TAG, "Expires in: " + response.getExpiresIn());
                    Log.d(TAG, "Token gained successful: " + response.getAccessToken());
                    break;
                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.e(TAG, "Spotify login error");
                    break;
                // Most likely auth flow was cancelled
                case CODE:
                    Log.d(TAG, "Code: " + response.getCode());
                    Log.d(TAG, "State: " + response.getState()); Intent serviceIntent = new Intent(HostActivity.this, ServerService.class);
                    serviceIntent.putExtra(Constants.CODE, response.getCode());
                    serviceIntent.putExtra(Constants.PASSWORD, generatePassword());
                    serviceIntent.putExtra(Constants.PARTYNAME, getString(R.string.text_partyName));
                    startService(serviceIntent);
                    if(mBoundService != null)
                        mBoundService.setSpotifyPlayerCallback(HostActivity.this);
                    break;
                default:
                    // Handle other cases
                    Log.e(TAG, "Something went wrong");
            }
        }
    }


//    Interaction with Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_host_party);
        hostActivity = this;

        registerReceiver(broadcastReceiver, new IntentFilter(Constants.STOP));

        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, new ServerLoadingFragment(), "ServerLoadingFragment").commitAllowingStateLoss();
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

        //token = getIntent().getStringExtra(Constants.TOKEN);

        doBindService();


    }

    @Override
    public void showDefault() {
        hostSearchBarFragment = new HostSearchBarFragment(this);
        showSongFragment = new ShowSongHostFragment(this);
        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
        hostClosePartyFragment = new HostClosePartyFragment(this);
        settingsHostFragment = new SettingsHostFragment(this);
        hostPlaylistFragment = new HostPlaylistFragment(this, this);
        partyPeopleFragment = new PartyPeopleFragment(this);

        if(savedInstanceState != null) {
            mCounter = savedInstanceState.getInt(STATE_COUNTER, 0);
            password = savedInstanceState.getString(STATE_PASSWORD, "0000");
            mBoundService = savedInstanceState.getParcelable(STATE_SERVICE);
            String currentFragmentTag = savedInstanceState.getString(STATE_TAG, "ShowSongHostFragment");
            if(!currentFragmentTag.equals("")) {
                Log.d(TAG, "onCreate: " + currentFragmentTag);
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                Log.d(TAG, "onCreate: " + currentFragment.toString());
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.showSongHostFragmentFrame, currentFragment, currentFragmentTag);
            }
        } else {
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongHostFragment").commitAllowingStateLoss();
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.searchBarHostFragmentFrame, hostSearchBarFragment, "HostSearchBarFragment").commitAllowingStateLoss();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COUNTER, mCounter);
        outState.putString(STATE_PASSWORD, password);
        String tag = "";
        if(showSavedPlaylistsFragment != null && showSavedPlaylistsFragment.isVisible())
            tag = showSavedPlaylistsFragment.getTag();
        if(showSongFragment != null && showSongFragment.isVisible())
            tag = showSongFragment.getTag();
        if(settingsHostFragment != null && settingsHostFragment.isVisible())
            tag = settingsHostFragment.getTag();
        if(searchSongsOutputFragment != null && searchSongsOutputFragment.isVisible())
            tag = searchSongsOutputFragment.getTag();
        if(partyPeopleFragment != null && partyPeopleFragment.isVisible())
            tag = partyPeopleFragment.getTag();
        if(hostPlaylistFragment != null && hostPlaylistFragment.isVisible())
            tag = hostPlaylistFragment.getTag();
        if(hostClosePartyFragment != null && hostClosePartyFragment.isVisible())
            tag = hostClosePartyFragment.getTag();
        outState.putString(STATE_TAG, tag);
        outState.putParcelable(STATE_SERVICE, mBoundService);
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
        if (mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
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
        if(showSongFragment.isVisible()) {
            animateFragmentChange(true, hostClosePartyFragment, "ExitConnectionFragment");
        }
        else {
            hostSearchBarFragment.clearSearch();
            animateFragmentChange(false, showSongFragment, "ShowSongFragment");
        }
    }


    //  changing fragment source

    public void animateFragmentChange(boolean direction, Fragment fragment, String tag) {
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(direction)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_up, R.anim.fragment_slide_out_up);
        else
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_down, R.anim.fragment_slide_in_down);
        fragmentTransaction.replace(R.id.showSongHostFragmentFrame, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void searchForSongs(List<Track> tracks) {
        Log.d("ShowSongFragment", "back to show");
        animateFragmentChange(true, searchSongsOutputFragment, "SearchSongOutputFragment");
        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
    }

    @Override
    public void openSavedPlaylistsFragment() {
        animateFragmentChange(true, showSavedPlaylistsFragment, "ShowSavedPlaylistFragment");
    }

    @Override
    public String getToken() {
        return mBoundService != null ? mBoundService.getToken() : null;
    }

    @Override
    public void openSettingsFragment() {
        animateFragmentChange(true, settingsHostFragment, "SettingsHostFragment");
    }

    @Override
    public void openPeopleFragment() {
        animateFragmentChange(true, partyPeopleFragment, "PartyPeopleFragment");
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
    public void reloadFavoritePlaylistsFragment() {
        Fragment frg = null;
        frg = getSupportFragmentManager().findFragmentByTag("ShowSavedPlaylistFragment");
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.detach(frg);
        ft.attach(frg);
        ft.commit();
    }

    @Override
    public void deletePlaylist(String id) {
        if(mBoundService != null)
            mBoundService.deletePlaylist(id);
    }

    //    Methods for ShowSongFragment

    @Override
    public void setNowPlaying(Track nowPlaying) {
        showSongFragment.setNowPlaying(nowPlaying);
    }

    @Override
    public void setPeopleCount(int count) {
        this.runOnUiThread(() -> showSongFragment.setPartyNameCount(count));
    }

    @Override
    public void setPlayImage(boolean pause) {
        showSongFragment.setPlayTrackButtonImage(pause);
    }

    @Override
    public int getPartyPeopleSize() {
        if (mBoundService != null) return mBoundService.getClientListSize();
        else return 0;
    }

    @Override
    public String getPartyPeoplePartyName() {
        return mBoundService != null ? mBoundService.getPartyName() : getString(R.string.text_partyName);
    }

    @Override
    public void nextTrack() {
        if (mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
    }

    @Override
    public void lastTrack() {
        if(mBoundService != null &&  mBoundService.getmSpotifyAppRemote() != null) {
            mBoundService.addItemToTrackList(mBoundService.getNowPlaying());
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipPrevious();
        }
    }

    @Override
    public void playTrack() {
        if(mBoundService != null)
            mBoundService.togglePlayback();
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

    @Override
    public void connect(HostActivity.ConnectionCallback connectionCallback) {
        runOnUiThread(() -> {
            Log.d(TAG, "connect: Trying to connect");
            ConnectionParams connectionParams =
                    new ConnectionParams.Builder(Constants.CLIENT_ID)
                            .setRedirectUri(Constants.REDIRECT_URI)
                            .showAuthView(false)
                            .build();
            SpotifyAppRemote.connect(HostActivity.this, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            Log.d(TAG, "Connected! Yay!");
                            //mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");
                            // Now you can start interacting with App Remote
                            connectionCallback.afterConnection(spotifyAppRemote);
                            //if(mBoundService!= null) mBoundService.setmSpotifyAppRemote(spotifyAppRemote);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(TAG, throwable.getMessage(), throwable);

                            // Something went wrong when attempting to connect! Handle errors here
                        }
                    });
        });
    }

//    Methods for SearchSongsOutput

    @Override
    public void addSong(Track track) {
        this.runOnUiThread(() -> Toast.makeText(HostActivity.this, track.getName() + " " + getText(R.string.text_queAdded), Toast.LENGTH_SHORT).show());
        new Thread(() -> {
            try {
                Log.d(TAG, "Trying to send message to server");
                if (mBoundService != null) {
                    mBoundService.addItem(track.getURI(), track.getName());
                    mBoundService.addItemToPlaylist(track);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }).start();
    }


//    Methods for HostPlaylist

    @Override
    public void showPlaylist() {
        if (mBoundService != null) {
            List<Track> trackList = mBoundService.getPlaylist();
            this.runOnUiThread(() -> hostPlaylistFragment.showResult(trackList));
        }
    }

    public void nextSong(View view) {
        if (mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
        //else if (mBoundService != null)
            //connect(appRemote -> appRemote.getPlayerApi().skipNext());
    }

    @Override
    public Track getCurrentPlaying() {
        if (mBoundService != null) return mBoundService.getNowPlaying();
        else {
            return null;
        }
    }

    @Override
    public void swapPlaylistItems(int from, int to) {
        if (mBoundService != null) {
            try {
                mBoundService.moveItem(from, to);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void removeItem(Track toRemove, int position, ServerService.AfterDeleteCallback callback) {
        if(mBoundService != null) {
            try {
                mBoundService.deleteItem(toRemove.getURI(), toRemove.getName(), position, () -> runOnUiThread(callback::deleteFromDataset));
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    }


//    Methods for HostCloseParty

    @Override
    public void denyEndParty() {
        onBackPressed();
    }

    @Override
    public void acceptEndParty(boolean save) {
        if(!save && mBoundService != null) mBoundService.deletePlaylist(mBoundService.getPlaylistID());
        stopService();
    }

    @Override
    public boolean savePlaylistInSharedPreferences(String name) {
        SharedPreferences savePlaylistMemory = this.getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
        if(!savePlaylistMemory.getString("9", "").equals(""))
            return false;
        SharedPreferences.Editor editor = savePlaylistMemory.edit();
        JSONObject playlist = new JSONObject();
        try {
            Log.d(TAG, "savePlaylistInSharedPreferences: " + name);
            playlist.put("name", name);
            if(mBoundService != null) {
                String id = mBoundService.getPlaylistID();
                playlist.put("id", id);
                mBoundService.updatePlaylistName(name, id);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < 9; i++) {
            if(savePlaylistMemory.getString("" + i, null) == null) {
                editor.putString("" + i, playlist.toString());
                break;
            }
        }
        editor.apply();
        return true;
    }


//    Methods for PartyPeople

    @Override
    public ArrayList<PartyPeople> getPartyPeopleList() {
        if (mBoundService != null) return (ArrayList<PartyPeople>) mBoundService.getPeopleList();
        else return new ArrayList<>();
    }



//    Methods for ShowSavedPlaylistFragment

    @Override
    public void playFavoritePlaylist(String id, ArrayList<String> idList) {
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)  {
            String playlistID = mBoundService.getPlaylistID();
            if(!idList.contains(playlistID))
                mBoundService.deletePlaylist(playlistID);
            mBoundService.getmSpotifyAppRemote().getPlayerApi().play("spotify:playlist:"+id);
            mBoundService.setPlaylistID(id);
        }
        //TODO: Spotify App geschlossen -> neue Verbindung
    }

    @Override
    public void changePlaylistName(String name, String id) {
        if(mBoundService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mBoundService.updatePlaylistName(name, id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void changePlaylistCover(String id, Bitmap image) {
        if(mBoundService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mBoundService.updatePlaylistCover(id, image, HostActivity.this);
                }
            }).start();
        }
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
        } catch (Exception ignored) {
        } // for now eat exceptions
        return "";
    }

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
            mBoundService.setPartyName(newPartyName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mBoundService.sendToAll(Commands.LOGIN, mBoundService.getPartyName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}