package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import android.widget.Toast;


import com.example.musicparty.databinding.ActivityPartyBinding;
import com.example.musicparty.music.Track;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;


public class PartyActivity extends AppCompatActivity implements ShowSongFragment.ExitButtonClicked, ExitConnectionFragment.ConfirmExit, SearchBarFragment.SearchForSongs, SearchSongsOutputFragment.AddSongCallback, ClientService.PartyCallback {


    ActivityPartyBinding binding;
    private static final String NAME = PartyActivity.class.getName();
    private static String token;
    private boolean mShouldUnbind;
    private ClientService mBoundService;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private ShowSongFragment showSongFragment;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ClientService.LocalBinder)service).getService();
            mBoundService.setPartyCallback(PartyActivity.this);
            String partyName = mBoundService.getClientThread().getPartyName();
            if(partyName != null) {
                setPartyName(partyName);
            }
            if(mBoundService.isStopped()) {
                exitService("Server has been closed");
            }
            // Tell the user about this for our demo.
            //Toast.makeText(PartyActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(PartyActivity.this, "Service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        if (bindService(new Intent(this, ClientService.class),
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
        token = getIntent().getStringExtra(Constants.TOKEN);
        binding = ActivityPartyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
        showSongFragment = new ShowSongFragment(this);

        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarFragmentFrame, new SearchBarFragment(this, token), "SearchBarFragment").commitAllowingStateLoss();

        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, showSongFragment , "ShowSongFragment").commitAllowingStateLoss();

      
         Intent serviceIntent = new Intent(this, ClientService.class);
        serviceIntent.putExtra(Constants.TOKEN, token);
        serviceIntent.putExtra(Constants.ADDRESS, getIntent().getStringExtra(Constants.ADDRESS));
        serviceIntent.putExtra(Constants.PASSWORD, getIntent().getStringExtra(Constants.PASSWORD));
        serviceIntent.putExtra(Constants.USERNAME, getIntent().getStringExtra(Constants.USERNAME));
        startService(serviceIntent);
        doBindService();

    }

    @Override
    public void exitConnection() {
        getSupportFragmentManager().beginTransaction().
               replace(R.id.showSongFragmentFrame, new ExitConnectionFragment(this), "ExitConnectionFragment").commitAllowingStateLoss();
    }



    @Override
    public void denyExit() {
        showShowSongFragment();
    }

    @Override
    public void acceptExit() {
        new Thread(()->{
            Log.d(NAME, "User tries to leave the party");
            try {
                mBoundService.getClientThread().sendMessage(Commands.QUIT, "User left the channel");
            } catch (IOException e) {
                Log.e(NAME, e.getMessage(), e);
            }
            exitService("You left the session");
        }).start();
    }

    @Override
    public String getPartyName() {
        return mBoundService.getClientThread().getPartyName();
    }

    @Override
    public void searchForSongs(List<Track> tracks) {
        Log.d("ShowSongFragment", "back to show");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, searchSongsOutputFragment, "ShowSongFragment").commitAllowingStateLoss();
        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
    }

    private void showShowSongFragment() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, showSongFragment , "ShowSongFragment").commitAllowingStateLoss();
        new Thread(()->{
            while(!showSongFragment.getStarted());
            mBoundService.setTrack();
            Log.d(NAME, "Hidden: " + showSongFragment.isHidden());
            Log.d(NAME, "Partyname: " + mBoundService.getClientThread().getPartyName());
            setPartyName(mBoundService.getClientThread().getPartyName());
        }).start();
    }

    public void search(View view) {
        //binding.etSearch.getText().toString());
    }

    @Override
    public void onBackPressed() {
        showShowSongFragment();
    }

    @Override
    public void addSong(Track track) {
            new Thread(() -> {
                try {
                    Log.d(NAME, "Trying to send message to server");
                    mBoundService.getClientThread().sendMessage(Commands.QUEUE, track.serialize());
                } catch (IOException | JSONException e) {
                    Log.e(NAME, e.getMessage(), e);
                }
            }).start();

    }

    @Override
    public void setTrack(Track track) {
        Log.d(NAME, "Now Playing: " + track.toString());
        showSongFragment.showSongs(track);
    }

    @Override
    public void setPartyName(String partyName) {
        showSongFragment.setPartyName(partyName);
    }

    @Override
    public void exitService(String text) {
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
}