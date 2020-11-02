package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaParser;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.musicparty.databinding.ActivityHostBinding;
import com.example.musicparty.music.Artist;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServerService.LocalBinder)service).getService();

            ConnectionParams connectionParams =
                    new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(false)
                            .build();
            SpotifyAppRemote.connect(HostActivity.this, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            mBoundService.setmSpotifyAppRemote(spotifyAppRemote);
                            Log.d(NAME, "Connected! Yay!");
                            //mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");
                            Intent serviceIntent = new Intent(HostActivity.this, ServerService.class);
                            serviceIntent.putExtra(Constants.TOKEN, token);
                            serviceIntent.putExtra(Constants.PASSWORD, PASSWORD);
                            startService(serviceIntent);
                            // Now you can start interacting with App Remote
                            //connected();
                            mBoundService.setSpotifyPlayerCallback(HostActivity.this);
                            mBoundService.addEventListener();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(NAME, throwable.getMessage(), throwable);

                            // Something went wrong when attempting to connect! Handle errors here
                        }
                    });

            // Tell the user about this for our demo.
            Toast.makeText(HostActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(HostActivity.this, "Service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

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
        SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        doUnbindService();
        Log.d(NAME, "I got destroyed");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void togglePlay(View view) {
        if (mBoundService.getPause()) mBoundService.getmSpotifyAppRemote().getPlayerApi().resume();
        else mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
    }

    public void stopService(View view) {
        mBoundService.getmSpotifyAppRemote().getPlayerApi().pause();
        SpotifyAppRemote.disconnect(mBoundService.getmSpotifyAppRemote());
        doUnbindService();
        stopService(new Intent(this, ServerService.class));
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void nextSong(View view) {
        mBoundService.getmSpotifyAppRemote().getPlayerApi().skipNext();
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

    private void connected() {
        mBoundService.getmSpotifyAppRemote().getPlayerApi().play("spotify:playlist:31OUF33qw8gZH6dmkss0Cz");
        mBoundService.getmSpotifyAppRemote().getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        Log.d(NAME, track.name + " by " + track.artist.name);
                    }
                });
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