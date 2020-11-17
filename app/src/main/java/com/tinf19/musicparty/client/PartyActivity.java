package com.tinf19.musicparty.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PatternMatcher;
import android.util.Log;

import android.widget.Toast;


import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.tinf19.musicparty.databinding.ActivityClientPartyBinding;
import com.tinf19.musicparty.fragments.ClientPlaylistFragment;
import com.tinf19.musicparty.fragments.LoadingFragment;
import com.tinf19.musicparty.server.HostActivity;
import com.tinf19.musicparty.server.ServerService;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.fragments.ExitConnectionFragment;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.fragments.SearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.fragments.ShowSongFragment;
import com.tinf19.musicparty.music.Track;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;


public class PartyActivity extends AppCompatActivity implements ShowSongFragment.PartyButtonClicked, ExitConnectionFragment.ConfirmExit, SearchBarFragment.SearchForSongs, SearchSongsOutputFragment.AddSongCallback, ClientService.PartyCallback {


    private static final int REQUEST_CODE = 1337;
    ActivityClientPartyBinding binding;
    private FragmentTransaction fragmentTransaction;
    private static final String TAG = PartyActivity.class.getName();
    //private static String token;
    private boolean mShouldUnbind;
    private ClientService mBoundService;

    private SearchSongsOutputFragment searchSongsOutputFragment;
    private ShowSongFragment showSongFragment;
    private ClientPlaylistFragment clientPlaylistFragment;
    private SearchBarFragment searchBarFragment;
    private ExitConnectionFragment exitConnectionFragment;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ClientService.LocalBinder)service).getService();
            loginToSpotify();
/*            String partyName = mBoundService.getClientThread().getPartyName();
            if(partyName != null) {
                setPartyName(partyName);
            }*/
            if(mBoundService.isStopped()) {
                exitService(getString(R.string.service_serverClosed));
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread(() -> {
                try {
                    if(mBoundService != null)
                        mBoundService.getClientThread().sendMessage(Commands.QUIT, "User left the channel");
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }).start();
            exitService(getString(R.string.service_serverDisconnected));
        }
    };

    void doBindService() {
        if (bindService(new Intent(this, ClientService.class),
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
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //token = getIntent().getStringExtra(Constants.TOKEN);
        binding = ActivityClientPartyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.STOP));

        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, new LoadingFragment(), "LoadingFragment").commitAllowingStateLoss();


        doBindService();

    }
    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(Constants.CLIENT_ID, AuthorizationResponse.Type.CODE, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{});
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
                    Log.d(TAG, "State: " + response.getState());
                    Intent serviceIntent = new Intent(PartyActivity.this, ClientService.class);
                    serviceIntent.putExtra(Constants.CODE, response.getCode());
                    serviceIntent.putExtra(Constants.ADDRESS, getIntent().getStringExtra(Constants.ADDRESS));
                    serviceIntent.putExtra(Constants.PASSWORD, getIntent().getStringExtra(Constants.PASSWORD));
                    serviceIntent.putExtra(Constants.USERNAME, getIntent().getStringExtra(Constants.USERNAME));
                    startService(serviceIntent);
                    if(mBoundService != null)
                        mBoundService.setPartyCallback(PartyActivity.this);
                    break;
                default:
                    // Handle other cases
                    Log.e(TAG, "Something went wrong");
            }
        }
    }


    public void showFragments() {
        Log.d(TAG, "showFragments: End of loading");
        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
        showSongFragment = new ShowSongFragment(this);
        clientPlaylistFragment = new ClientPlaylistFragment();
        searchBarFragment = new SearchBarFragment(this);
        exitConnectionFragment = new ExitConnectionFragment(this);

        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarFragmentFrame, searchBarFragment, "SearchBarFragment").commitAllowingStateLoss();
        showShowSongFragment();
    }

    @Override
    public void exitConnection() {
        animateFragmentChange(true, exitConnectionFragment, "ExitConnectionFragment");
    }

    @Override
    public void showPlaylist() {
        if(mBoundService != null) {
            try {
                mBoundService.getClientThread().sendMessage(Commands.PLAYLIST, "User ask for Playlist");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        animateFragmentChange(true, clientPlaylistFragment, "ClientPlaylistFragment");
    }


    @Override
    public void denyExit() {
        showShowSongFragment();
    }

    @Override
    public void acceptExit() {
        new Thread(()->{
            Log.d(TAG, "User tries to leave the party");
            try {
                if(mBoundService != null)
                mBoundService.getClientThread().sendMessage(Commands.QUIT, "User left the channel");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            exitService(getString(R.string.service_serverDisconnected));
        }).start();
    }

    @Override
    public String getPartyName() {
        if(mBoundService != null)
            return mBoundService.getClientThread().getPartyName();
        else
            return getString(R.string.text_hintPartyName);
    }

    @Override
    public void searchForSongs(List<Track> tracks) {
        Log.d("ShowSongFragment", "back to show");
        animateFragmentChange(true, searchSongsOutputFragment, "SearchSongOutputFragment");
        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
    }

    private void showShowSongFragment() {
        animateFragmentChange(false, showSongFragment, "ShowSongFragment");
        new Thread(()->{
            while(!showSongFragment.getStarted() || mBoundService == null || mBoundService.getClientThread().getPartyName() == null);
            if(mBoundService != null) {
                mBoundService.setTrack();
                Log.d(TAG, "Hidden: " + showSongFragment.isHidden());
                Log.d(TAG, "Partyname: " + mBoundService.getClientThread().getPartyName());
                setPartyName(mBoundService.getClientThread().getPartyName());
            }
        }).start();
    }

    public void animateFragmentChange(boolean direction, Fragment fragment, String tag) {
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(direction)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_up, R.anim.fragment_slide_out_up);
        else
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_down, R.anim.fragment_slide_in_down);
        fragmentTransaction.replace(R.id.showSongFragmentFrame, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if(showSongFragment.isVisible()) {
            animateFragmentChange(true, exitConnectionFragment, "ExitConnectionFragment");
        }
        else {
            searchBarFragment.clearSearch();
            showShowSongFragment();
        }
    }

    @Override
    public void addSong(Track track) {
        this.runOnUiThread(() -> Toast.makeText(PartyActivity.this, track.getName() + " " + getText(R.string.text_queAdded), Toast.LENGTH_SHORT).show());
            new Thread(() -> {
                try {
                    Log.d(TAG, "Trying to send message to server");
                    if(mBoundService != null)
                        mBoundService.getClientThread().sendMessage(Commands.QUEUE, track.serialize());
                } catch (IOException | JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }).start();

    }

    @Override
    public void setTrack(Track track) {
        Log.d(TAG, "Now Playing: " + track.toString());
        runOnUiThread(() -> showSongFragment.showSongs(track));
    }

    @Override
    public void setPartyName(String partyName) {
        runOnUiThread(() -> showSongFragment.setPartyName(partyName));
    }

    @Override
    public void exitService(String text) {
        mBoundService.getClientThread().interrupt();
        doUnbindService();
        stopService(new Intent(this, ClientService.class));
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PartyActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
        startActivity((new Intent(this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void setPlaylist(List<Track> trackList) {
        Log.d("ClientPlaylistFragment", "show playlist");
        this.runOnUiThread(() -> clientPlaylistFragment.showResult(trackList));
    }

    @Override
    public void setCurrentTrack(Track track) {
        Log.d(TAG, "set current track");
        this.runOnUiThread(() -> clientPlaylistFragment.setCurrentPlaying(track));
    }

    @Override
    public String getToken() {
        return mBoundService != null ? mBoundService.getToken() : null;
    }
}