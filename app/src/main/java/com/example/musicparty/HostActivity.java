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

public class HostActivity extends AppCompatActivity {

    private static final String NAME = HostActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final String PASSWORD = String.valueOf((new Random()).nextInt((9999 - 1000) + 1) + 1000);
    private static SpotifyAppRemote mSpotifyAppRemote;
    private ActivityHostBinding binding;
    private Channel channel;
    private WifiP2pManager manager;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean pause;
    private String token;
    private boolean mShouldUnbind;
    private ServerService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServerService.LocalBinder)service).getService();

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

        com.example.musicparty.music.Track test = new com.example.musicparty.music.Track(
                "rsadrf3241ssd",
                "Test Track",
                new Artist[]{new Artist("asdas", "Jannik")},
                "URLSSAD",
                12);
        try {
            Log.d(NAME, test.serialize());
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
        doUnbindService();
        Log.d(NAME, "I got destroyed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        HostActivity hostActivity = this;
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d(NAME, "Connected! Yay!");
                        //mSpotifyAppRemote.getPlayerApi().play("spotify:track:3cfOd4CMv2snFaKAnMdnvK");
                        Intent serviceIntent = new Intent(hostActivity, ServerService.class);
                        serviceIntent.putExtra(Constants.TOKEN, token);
                        serviceIntent.putExtra(Constants.PASSWORD, PASSWORD);
                        startService(serviceIntent);
                        // Now you can start interacting with App Remote
                        //connected();
                        mSpotifyAppRemote.getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(playerState -> {
                                    final Track track = playerState.track;
                                    if(playerState.playbackPosition == 0) {
                                        Log.d(NAME, "New song has been started");
                                        new Thread(()->{
                                            try {
                                                mBoundService.sendToAll(Commands.PLAYING, new com.example.musicparty.music.Track(
                                                        track.uri.split(":")[2],
                                                        track.name,
                                                        track.artists,
                                                        track.imageUri.raw,
                                                        track.duration
                                                        ).serialize());
                                            } catch (IOException | JSONException e) {
                                                Log.e(NAME, e.getMessage(), e);
                                            }
                                        }).start();
                                    }
                                    pause = playerState.isPaused;
                                    if (track != null) {
                                        Log.d(NAME, track.name + " by " + track.artist.name);
                                        //if (playerState.playbackPosition == 0)
                                            //nextSong();
                                        hostActivity.runOnUiThread(() -> {
                                            // Display requested url data as string into text view
                                            binding.tvPlaying.setText(String.format("%s by %s", track.name, track.artist.name));
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(NAME, throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    public static SpotifyAppRemote getmSpotifyAppRemote() {
        return mSpotifyAppRemote;
    }

    public void togglePlay(View view) {
        if (pause) mSpotifyAppRemote.getPlayerApi().resume();
        else mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void stopService(View view) {
        stopService(new Intent(this, ServerService.class));
        mSpotifyAppRemote.getPlayerApi().pause();
        startActivity(new Intent(this, MainActivity.class));
    }

    public void nextSong(View view) {
        mSpotifyAppRemote.getPlayerApi().skipNext();
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
        mSpotifyAppRemote.getPlayerApi().play("spotify:playlist:31OUF33qw8gZH6dmkss0Cz");
        mSpotifyAppRemote.getPlayerApi()
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
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        Log.d(NAME, "I have been stopped");
    }
}