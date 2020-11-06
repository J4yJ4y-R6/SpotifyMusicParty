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
import android.widget.Toast;

import com.tinf19.musicparty.fragments.LoadingFragment;
import com.tinf19.musicparty.fragments.SearchBarFragment;
import com.tinf19.musicparty.fragments.ShowSongFragment;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.WiFiDirectBroadcastReceiver;
import com.tinf19.musicparty.databinding.ActivityHostBinding;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HostActivity extends AppCompatActivity implements ServerService.SpotifyPlayerCallback {

    private static final String NAME = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final String PASSWORD = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
    private ActivityHostBinding binding;
    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private String token;
    private boolean mShouldUnbind;
    private ServerService mBoundService;

    public interface ConnectionCallback {
        void afterConnection(SpotifyAppRemote appRemote);
    }

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
            Log.e(NAME, "Error: The requested service doesn't " +
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        binding = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvIpAddress.setText(getIPAddress(true));
        binding.tvPassword.setText(PASSWORD);
        doBindService();


        Button partyActivity = findViewById(R.id.partyActivityButton);
        if (partyActivity != null) {
            partyActivity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HostActivity.this, HostPartyActivity.class);
                    intent.putExtra(Constants.TOKEN, token);
                    startActivity(intent);
                }
            });
        }
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
        Log.d(NAME, "I got destroyed");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void togglePlay(View view) {
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null) {
            if(mBoundService.getPause()) mBoundService.getmSpotifyAppRemote().getPlayerApi().resume();
            else mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
        } else if(mBoundService != null) {
            connect(appRemote -> {
                if(mBoundService.getPause()) appRemote.getPlayerApi().resume();
                else appRemote.getPlayerApi().pause();
            });
        }
    }

    public void stopService(View view) {
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null) {
            mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
            SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        }
        doUnbindService();
        stopService(new Intent(this, ServerService.class));
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void nextSong(View view) {
        if(mBoundService != null && mBoundService.getmSpotifyAppRemote() != null)
            mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
        else if(mBoundService != null)
            connect(appRemote -> appRemote.getPlayerApi().skipNext());
    }

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
    protected void onStop() {
        super.onStop();
        Log.d(NAME, "I have been stopped");
    }

    @Override
    public void setNowPlaying(String nowPlaying) {
        this.runOnUiThread(() -> {
            Log.d(NAME, nowPlaying);
            binding.tvPlaying.setText(nowPlaying);
        });
    }

}