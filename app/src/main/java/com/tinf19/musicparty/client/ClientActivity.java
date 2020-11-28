package com.tinf19.musicparty.client;

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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import android.widget.Toast;


import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.tinf19.musicparty.BuildConfig;
import com.tinf19.musicparty.databinding.ActivityClientPartyBinding;
import com.tinf19.musicparty.client.fragments.ClientPlaylistFragment;
import com.tinf19.musicparty.fragments.LoadingFragment;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.client.fragments.ClientExitConnectionFragment;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.client.fragments.ClientSearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.client.fragments.ClientSongFragment;
import com.tinf19.musicparty.music.Track;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;


public class ClientActivity extends AppCompatActivity {

    private static final String TAG = ClientActivity.class.getName();
    private ClientService mBoundService;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private ClientSongFragment clientSongFragment;
    private ClientPlaylistFragment clientPlaylistFragment;
    private ClientSearchBarFragment clientSearchBarFragment;
    private ClientExitConnectionFragment clientExitConnectionFragment;
    private boolean mShouldUnbind;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ClientService.LocalBinder)service).getService();
            loginToSpotify();
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
            exitService(getString(R.string.service_serverDisconnected, getPartyName()));
        }
    };



    //Android lifecycle methods

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String tag = "";
        if(searchSongsOutputFragment != null && searchSongsOutputFragment.isVisible())
            tag = searchSongsOutputFragment.getTag();
        if(clientSongFragment != null && clientSongFragment.isVisible())
            tag = clientSongFragment.getTag();
        if(clientPlaylistFragment != null && clientPlaylistFragment.isVisible())
            tag = clientPlaylistFragment.getTag();
        if(clientExitConnectionFragment != null && clientExitConnectionFragment.isVisible())
            tag = clientExitConnectionFragment.getTag();
        outState.putString(Constants.TAG, tag);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.tinf19.musicparty.databinding.ActivityClientPartyBinding binding = ActivityClientPartyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.STOP));

        if(savedInstanceState != null){
            String currentFragmentTag = savedInstanceState.getString(Constants.TAG, "ShowSongFragment");
            if(!currentFragmentTag.equals("")) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.showSongHostFragmentFrame, currentFragment, currentFragmentTag);
            }
        } else {
            Log.d(TAG, "Fragment has been changed to LoadingFragment");
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.showSongFragmentFrame, new LoadingFragment(), "LoadingFragment").commitAllowingStateLoss();
            doBindService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Constants.REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                case CODE:
                    Log.d(TAG, "got code from Spotify successfully");
                    Intent serviceIntent = new Intent(ClientActivity.this, ClientService.class);
                    serviceIntent.putExtra(Constants.CODE, response.getCode());
                    serviceIntent.putExtra(Constants.ADDRESS, getIntent().getStringExtra(Constants.ADDRESS));
                    serviceIntent.putExtra(Constants.PASSWORD, getIntent().getStringExtra(Constants.PASSWORD));
                    serviceIntent.putExtra(Constants.USERNAME, getIntent().getStringExtra(Constants.USERNAME));
                    startService(serviceIntent);
                    setServiceCallback();
                    break;
                case ERROR:
                    Log.e(TAG, "Spotify login error: " + response.getError());
                    break;
                default:
                    Log.e(TAG, "Something went wrong");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(clientSongFragment.isVisible()) {
            animateFragmentChange(true, clientExitConnectionFragment, "ExitConnectionFragment");
        }
        else {
            clientSearchBarFragment.clearSearch();
            showShowSongFragment();
        }
    }



    //Getter and Setter

    private String getPartyName() {
        if(mBoundService != null)
            return mBoundService.getClientThread().getPartyName();
        else
            return getString(R.string.text_hintPartyName);
    }

    private void setPartyName(String partyName) {
        Log.d(TAG, "party name got changed to: " + partyName);
        runOnUiThread(() -> clientSongFragment.setPartyName(partyName));
    }



    //Show or change Fragments in PartyActivity

    private void showShowSongFragment() {
        animateFragmentChange(false, clientSongFragment, "ShowSongFragment");
        new Thread(()->{
            while(!clientSongFragment.getStarted() || mBoundService == null || mBoundService.getClientThread().getPartyName() == null);
            if(mBoundService != null) {
                mBoundService.setTrack();
                setPartyName(mBoundService.getClientThread().getPartyName());
            }
        }).start();
    }

    public void animateFragmentChange(boolean direction, Fragment fragment, String tag) {
        Log.d(TAG, "Fragment has been changed to " + tag);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(direction)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_up, R.anim.fragment_slide_out_up);
        else
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_down, R.anim.fragment_slide_in_down);
        fragmentTransaction.replace(R.id.showSongFragmentFrame, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }



    //Service methods

    void doBindService() {
        if (bindService(new Intent(this, ClientService.class),
                mConnection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "service has been bound");
            mShouldUnbind = true;
        } else {
            Log.e(TAG, "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            Log.d(TAG, "service has been unbound");
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    private void exitService(String text) {
        Log.d(TAG, "exit service because: " + text + ". Go back to MainActivity.");
        mBoundService.getClientThread().interrupt();
        doUnbindService();
        stopService(new Intent(ClientActivity.this, ClientService.class));
        ClientActivity.this.runOnUiThread(() -> Toast.makeText(ClientActivity.this, text, Toast.LENGTH_SHORT).show());
        startActivity((new Intent(ClientActivity.this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(BuildConfig.CLIENT_ID, AuthorizationResponse.Type.CODE, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, Constants.REQUEST_CODE, request);
    }

    private void setServiceCallback() {
        if(mBoundService != null)
            mBoundService.setClientServiceCallback(new ClientService.ClientServiceCallback() {
                @Override
                public void setTrack(Track track) {
                    runOnUiThread(() -> clientSongFragment.showSongs(track));
                }

                @Override
                public void setPartyName(String partyName) {
                    ClientActivity.this.setPartyName(partyName);
                }

                @Override
                public void exitService(String text) {
                    ClientActivity.this.exitService(text);
                }

                @Override
                public void setPlaylist(List<Track> trackList) {
                    ClientActivity.this.runOnUiThread(() -> clientPlaylistFragment.showResult(trackList));
                }

                @Override
                public void setCurrentTrack(Track track) {
                    ClientActivity.this.runOnUiThread(() -> clientPlaylistFragment.setCurrentPlaying(track));
                }

                @Override
                public void showFragments() {
                    Log.d(TAG, "initializing global fragments with callbacks");
                    searchSongsOutputFragment = new SearchSongsOutputFragment(track -> {
                        ClientActivity.this.runOnUiThread(() -> Toast.makeText(ClientActivity.this, track.getName() + " " + getText(R.string.text_queAdded), Toast.LENGTH_SHORT).show());
                        new Thread(() -> {
                            try {
                                if(mBoundService != null)
                                    mBoundService.getClientThread().sendMessage(Commands.QUEUE, track.serialize());
                            } catch (IOException | JSONException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }).start();
                    });
                    clientSongFragment = new ClientSongFragment(new ClientSongFragment.PartyButtonClicked() {
                        @Override
                        public void exitConnection() {
                            animateFragmentChange(true, clientExitConnectionFragment, "ExitConnectionFragment");
                        }

                        @Override
                        public void showPlaylist() {
                            if(mBoundService != null) {
                                try {
                                    mBoundService.getClientThread().sendMessage(Commands.PLAYLIST, "User request the current Playlist");
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                            }
                            animateFragmentChange(true, clientPlaylistFragment, "ClientPlaylistFragment");
                        }
                    });
                    clientPlaylistFragment = new ClientPlaylistFragment();
                    clientSearchBarFragment = new ClientSearchBarFragment(new ClientSearchBarFragment.SearchForSongs() {
                        @Override
                        public void searchForSongs(List<Track> tracks) {
                            animateFragmentChange(true, searchSongsOutputFragment, "SearchSongOutputFragment");
                            ClientActivity.this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
                        }

                        @Override
                        public String getToken() {
                            return mBoundService != null ? mBoundService.getToken() : null;
                        }
                    });
                    clientExitConnectionFragment = new ClientExitConnectionFragment(new ClientExitConnectionFragment.ConfirmExit() {
                        @Override
                        public void denyExit() {
                            showShowSongFragment();
                        }

                        @Override
                        public void acceptExit() {
                            new Thread(()->{
                                try {
                                    if(mBoundService != null)
                                        mBoundService.getClientThread().sendMessage(Commands.QUIT, "User left the party");
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                                exitService(getString(R.string.service_serverDisconnected, getPartyName()));
                            }).start();
                        }

                        @Override
                        public String getPartyName() {
                            return ClientActivity.this.getPartyName();
                        }
                    });

                    Log.d(TAG, "Fragment has been changed to SearchBarFragment");
                    getSupportFragmentManager().beginTransaction().
                            replace(R.id.searchBarFragmentFrame, clientSearchBarFragment, "SearchBarFragment").commitAllowingStateLoss();
                    showShowSongFragment();
                }
            });
    }

}