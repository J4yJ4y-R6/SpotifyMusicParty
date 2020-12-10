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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;


import com.google.android.material.snackbar.Snackbar;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.tinf19.musicparty.BuildConfig;
import com.tinf19.musicparty.client.fragments.ClientPlaylistFragment;
import com.tinf19.musicparty.databinding.ActivityClientBinding;
import com.tinf19.musicparty.fragments.LoadingFragment;
import com.tinf19.musicparty.fragments.VotingFragment;
import com.tinf19.musicparty.server.HostService;
import com.tinf19.musicparty.util.ClientVoting;
import com.tinf19.musicparty.util.Commands;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.client.fragments.ClientExitConnectionFragment;
import com.tinf19.musicparty.MainActivity;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.client.fragments.ClientSearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.client.fragments.ClientSongFragment;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for Client with communication to the service and the server
 * @author Jannik Junker
 * @author Silas Wessely
 * @see AppCompatActivity
 * @see android.app.Service
 * @since 1.1
 */
public class ClientActivity extends AppCompatActivity {

    private static final String TAG = ClientActivity.class.getName();
    /**
     * The Client Bound {@link android.app.Service} for connecting background connection with the
     * {@link com.tinf19.musicparty.server.HostService}
     */
    private ClientService mBoundService;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private ClientSongFragment clientSongFragment;
    private ClientPlaylistFragment clientPlaylistFragment;
    private ClientSearchBarFragment clientSearchBarFragment;
    private ClientExitConnectionFragment clientExitConnectionFragment;
    private VotingFragment votingFragment;
    /**
     * Identify if Service is currently bounded
     */
    private boolean mShouldUnbind;

    private final ServiceConnection mConnection = new ServiceConnection() {
        /**
         * Assigning the service after connecting to the host and logging in to Spotify or closing
         * the service after the host stopped the server
         * @param className Class name of the service
         * @param service Service binder to assign the service
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "service has been connected");
            mBoundService = ((ClientService.LocalBinder) service).getService();
            loginToSpotify();
            if (mBoundService.isStopped()) {
                exitService(getString(R.string.service_serverClosed));
            }
        }

        /**
         * Deleteing the current service from the class
         * @param className Class name of the service
         */
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "service has been disconnected");
            mBoundService = null;
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread(() -> {
                Log.d(TAG, "user disconnected from server by service-notification");
                try {
                    if (mBoundService != null)
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String tag = "";
        if (searchSongsOutputFragment != null && searchSongsOutputFragment.isVisible())
            tag = searchSongsOutputFragment.getTag();
        if (clientSongFragment != null && clientSongFragment.isVisible())
            tag = clientSongFragment.getTag();
        if (clientPlaylistFragment != null && clientPlaylistFragment.isVisible())
            tag = clientPlaylistFragment.getTag();
        if (clientExitConnectionFragment != null && clientExitConnectionFragment.isVisible())
            tag = clientExitConnectionFragment.getTag();
        outState.putString(Constants.TAG, tag);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.tinf19.musicparty.databinding.ActivityClientBinding binding = ActivityClientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.STOP));

        if (savedInstanceState != null) {
            String currentFragmentTag = savedInstanceState.getString(Constants.TAG, "ShowSongFragment");
            if (!currentFragmentTag.equals("")) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                if(currentFragment != null)
                    getSupportFragmentManager().beginTransaction().
                            replace(R.id.showSongHostFragmentFrame, currentFragment, currentFragmentTag);
            }
        } else {
            if (!getIntent().getBooleanExtra(Constants.FROM_NOTIFICATION, false)) {
                Log.d(TAG, "Fragment has been changed to LoadingFragment");
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.showSongFragmentFrame, new LoadingFragment(getString(R.string.text_loadingClient)), "LoadingFragment").commitAllowingStateLoss();
            } else
                showDefaultFragments();
            doBindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
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
        if (clientExitConnectionFragment != null && clientSongFragment.isVisible()) {
            animateFragmentChange(true, clientExitConnectionFragment, "ExitConnectionFragment");
        } else {
            if(clientSearchBarFragment != null)
                clientSearchBarFragment.clearSearch();
            showShowSongFragment();
        }
    }


    //Getter and Setter

    /**
     * @return Get current party name from server if the service is connected or get default
     * party name: Party Name.
     */
    private String getPartyName() {
        if (mBoundService != null)
            return mBoundService.getClientThread().getPartyName();
        else
            return getString(R.string.text_hintPartyName);
    }

    /**
     * Set new party name in the {@link ClientSongFragment}
     *
     * @param partyName New party name after change
     */
    private void setPartyName(String partyName) {
        Log.d(TAG, "party name got changed to: " + partyName);
        if (clientSongFragment != null && clientSongFragment.isVisible())
            runOnUiThread(() -> clientSongFragment.setPartyName(partyName));
    }


    //Show or change Fragments in PartyActivity

    /**
     * Opening the {@link ClientSongFragment}.
     * In a new Thread: Set the currently playing track and the party name in the
     * {@link ClientSongFragment} after the fragment has been started and the server is connected
     * with a party name
     */
    private void showShowSongFragment() {
        if(clientSongFragment != null)
            animateFragmentChange(false, clientSongFragment, "ShowSongFragment");
        new Thread(() -> {
            while (!clientSongFragment.getStarted() || mBoundService == null || mBoundService.getClientThread().getPartyName() == null);
            if (mBoundService != null) {
                mBoundService.setTrack();
                setPartyName(mBoundService.getClientThread().getPartyName());
            }
        }).start();
    }

    /**
     * Change the current visible fragment in the big fragment in activity_client.xml and adding
     * an animation to the change.
     *
     * @param direction Animation direction if the fragment should slide in or out
     * @param fragment  Fragment which shall be opened
     * @param tag       Tag of the new fragment
     */
    public void animateFragmentChange(boolean direction, Fragment fragment, String tag) {
        Log.d(TAG, "Fragment has been changed to " + tag);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (direction)
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_up, R.anim.fragment_slide_out_up);
        else
            fragmentTransaction.setCustomAnimations(R.anim.fragment_slide_out_down, R.anim.fragment_slide_in_down);
        if(fragment != null) {
            fragmentTransaction.replace(R.id.showSongFragmentFrame, fragment, tag);
        }fragmentTransaction.commitAllowingStateLoss();
    }


    //Service methods

    /**
     * The client binds the service and set mShouldUnbind true, so the client knows a service is
     * connected
     */
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

    /**
     * The client unbinds the service and set mShouldUnbind false, so the client knows no service
     * is connected
     */
    void doUnbindService() {
        if (mShouldUnbind) {
            Log.d(TAG, "service has been unbound");
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    /**
     * The client is leaving the the service with a message and the app returns to the
     * {@link MainActivity}
     *
     * @param text Reason for leaving the service
     */
    private void exitService(String text) {
        Log.d(TAG, "exit service because: " + text + ". Go back to MainActivity.");
        mBoundService.getClientThread().interrupt();
        doUnbindService();
        stopService(new Intent(ClientActivity.this, ClientService.class));
        ClientActivity.this.runOnUiThread(() -> Snackbar.make(findViewById(R.id.showSongFragmentFrame), text, Snackbar.LENGTH_SHORT).show());
        startActivity((new Intent(ClientActivity.this, MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    /**
     * The client is opening the log-in-mask from Spotify to insert his credentials and connect
     * to his Spotify account.
     */
    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(BuildConfig.CLIENT_ID, AuthorizationResponse.Type.CODE, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, Constants.REQUEST_CODE, request);
    }

    /**
     * Set {@link com.tinf19.musicparty.client.ClientService.ClientServiceCallback}
     */
    private void setServiceCallback() {
        if (mBoundService != null)
            mBoundService.setClientServiceCallback(new ClientService.ClientServiceCallback() {
                @Override
                public void setTrack(Track track) {
                    if(clientSongFragment != null && clientSongFragment.isVisible())
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
                    if(clientPlaylistFragment != null && clientPlaylistFragment.isVisible())
                        ClientActivity.this.runOnUiThread(() -> clientPlaylistFragment.showResult(trackList));
                }

                @Override
                public void setCurrentTrack(Track track) {
                    if(clientPlaylistFragment != null && clientPlaylistFragment.isVisible())
                        ClientActivity.this.runOnUiThread(() -> clientPlaylistFragment.setCurrentPlaying(track));
                }

                @Override
                public void setVotings(List<Voting> clientVotings) {
                    if(votingFragment != null && votingFragment.isVisible())
                        ClientActivity.this.runOnUiThread(() -> votingFragment.showVotings(clientVotings));
                }

                @Override
                public void addVoting(ClientVoting voting) {
                    if(votingFragment != null && votingFragment.isVisible()) {
                        votingFragment.addItemToDataset(voting);
                        runOnUiThread( () -> votingFragment.notifyAllVotes(voting.getType()));
                    }
                }

                /**
                 * Initializing the global fragments with callbacks and opening the default
                 * fragments
                 * {@link SearchSongsOutputFragment.SearchSongsOutputCallback}: Queueing a track in
                 * the server
                 * {@link ClientSongFragment.ClientSongCallback#exitConnection()}: Opening the exit
                 * fragment in the big fragment in activity_client.xml
                 * {@link ClientSongFragment.ClientSongCallback#showPlaylist()}: Asking the server
                 * for the playlist at the current state and opening the playlist fragment in the
                 * big fragment in activity_client.xml
                 * {@link ClientSearchBarFragment.ClientSearchBarCallback#searchForSongs(List)}:
                 * Display a new list of tracks in the SearchSongOutputFragment and opening it in
                 * the big fragment in activity_client.xml
                 * {@link ClientSearchBarFragment.ClientSearchBarCallback#getToken()}: Get current
                 * Spotify token
                 * {@link ClientExitConnectionFragment.ClientExitConnectionCallback#denyExit()}:
                 * Opening the ClientSongFragment in the big fragment in activity_client.xml
                 * {@link com.tinf19.musicparty.adapter.VotingAdapter.VotingAdapterCallback}
                 * Get the client or server thread for voting
                 * {@link com.tinf19.musicparty.fragments.VotingFragment.VotingCallback}
                 * Get a List of all currently opened votings.
                 * {@link ClientExitConnectionFragment.ClientExitConnectionCallback#acceptExit()}:
                 * Disconnection vom the server and the service
                 * {@link ClientExitConnectionFragment.ClientExitConnectionCallback#getPartyName()}:
                 * Get the current party name
                 */
                @Override
                public void showFragments() {
                    Log.d(TAG, "initializing global fragments with callbacks");
                    searchSongsOutputFragment = new SearchSongsOutputFragment(track -> {
                        ClientActivity.this.runOnUiThread(() -> Snackbar.
                                make(findViewById(R.id.showSongFragmentFrame), track.getName() +
                                        " " + getText(R.string.text_queAdded),
                                        Snackbar.LENGTH_SHORT).show());
                        new Thread(() -> {
                            try {
                                if (mBoundService != null)
                                    mBoundService.getClientThread().sendMessage(Commands.QUEUE, track.serialize());
                            } catch (IOException | JSONException e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }).start();
                    });
                    clientSongFragment = new ClientSongFragment(new ClientSongFragment.ClientSongCallback() {
                        @Override
                        public void exitConnection() {
                            animateFragmentChange(true, clientExitConnectionFragment, "ExitConnectionFragment");
                        }

                        @Override
                        public void showPlaylist() {
                            if (mBoundService != null) {
                                try {
                                    mBoundService.getClientThread().sendMessage(Commands.PLAYLIST, "User request the current Playlist");
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                            }
                            animateFragmentChange(true, clientPlaylistFragment, "ClientPlaylistFragment");
                        }

                        @Override
                        public void openVotingFragment() {
                            if (mBoundService != null) {
                                try {
                                    mBoundService.getClientThread().sendMessage(Commands.VOTING, "User request all current votings");
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                            }
                            animateFragmentChange(true, votingFragment, "VotingFragment");
                            if(mBoundService != null) {
                                new Thread(() -> {
                                    try {
                                        mBoundService.getClientThread().sendMessage(Commands.SUBSCRIBE, "Subscribed the update event");
                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage(), e);
                                    }
                                }).start();
                            }
                        }

                        @Override
                        public HostService.PartyType getPartyType() {
                            return mBoundService != null ? mBoundService.getPartyType() :
                                    HostService.PartyType.AllInParty;
                        }

                        @Override
                        public Track getNowPlaying() {
                            return mBoundService != null ? mBoundService.getNowPlaying() : null;
                        }
                    });
                    clientPlaylistFragment = new ClientPlaylistFragment();
                    clientSearchBarFragment = new ClientSearchBarFragment(new ClientSearchBarFragment.ClientSearchBarCallback() {
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
                    clientExitConnectionFragment = new ClientExitConnectionFragment(new ClientExitConnectionFragment.ClientExitConnectionCallback() {
                        @Override
                        public void denyExit() {
                            showShowSongFragment();
                        }

                        @Override
                        public void acceptExit() {
                            new Thread(() -> {
                                try {
                                    if (mBoundService != null)
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
                    votingFragment = new VotingFragment(
                            () -> mBoundService != null ? mBoundService.getClientThread() : null,
                            new VotingFragment.VotingCallback() {
                                @Override
                                public List<Voting> getVotings() {
                                    return mBoundService != null ? mBoundService.getClientVotings()
                                            : new ArrayList<>();
                                }

                                @Override
                                public void stopTimer() {
                                    if(mBoundService != null) {
                                        new Thread(() -> {
                                            try {
                                                mBoundService.getClientThread().sendMessage(Commands.UNSUBSCRIBE, "Unsubscribed the update event");
                                            } catch (IOException e) {
                                                Log.e(TAG, e.getMessage(), e);
                                            }
                                        }).start();
                                    }
                                }
                            });
                    showDefaultFragments();
                }

                @Override
                public void notifyVotingAdapter(int id, Type type) {
                    Log.d(TAG, "Voting with id: " + id + "has changed");
                    if(votingFragment != null && votingFragment.isVisible())
                        votingFragment.notifySingleVote(id, type);
                }

                @Override
                public void removeVoting(int id, Type type) {
                    if(votingFragment != null && votingFragment.isVisible())
                       runOnUiThread(() ->  votingFragment.removeSingleVote(id, type));
                }

                @Override
                public void updateVotingButton(HostService.PartyType partyType) {
                    if(clientSongFragment != null && clientSongFragment.isVisible()) {
                        Snackbar.make(clientSongFragment.requireView(), getString(
                                R.string.snackbar_partyTypeChanged, partyType), Snackbar.LENGTH_LONG)
                                .show();
                        runOnUiThread(() -> clientSongFragment.toggleVotingButton(partyType));
                    }
                }
            });
    }

    /**
     * Showing the default fragments, when the party starts
     */
    private void showDefaultFragments() {
        Log.d(TAG, "Fragment has been changed to SearchBarFragment");
        if(clientSearchBarFragment != null)
            getSupportFragmentManager().beginTransaction().
                    replace(R.id.searchBarFragmentFrame, clientSearchBarFragment, "SearchBarFragment").commitAllowingStateLoss();
        showShowSongFragment();
    }
}