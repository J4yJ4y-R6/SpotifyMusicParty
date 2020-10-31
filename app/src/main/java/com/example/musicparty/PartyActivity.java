package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


import com.example.musicparty.databinding.ActivityPartyBinding;
import com.example.musicparty.music.Artist;
import com.example.musicparty.music.Track;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PartyActivity extends AppCompatActivity implements ShowSongFragment.ExitButtonClicked, ExitConnectionFragment.ConfirmExit, SearchBarFragment.SearchForSongs, PartyAcRecycAdapter.SongCallback, ClientService.ClientCallback {


    ActivityPartyBinding binding;
    private static final String NAME = PartyActivity.class.getName();
    private static final String HOST = "api.spotify.com";
    private static final int PORT = 1403;
    private static String token;
    private int limit = 10;
    private String type = "track";
    private Thread clientThread;
    private Socket clientSocket;
    private PartyAcRecycAdapter mAdapter;
    private RecyclerView recyclerView;
    private boolean mShouldUnbind;
    private ClientService mBoundService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ClientService.LocalBinder)service).getService();
            mBoundService.setCallback(PartyActivity.this);

            // Tell the user about this for our demo.
            Toast.makeText(PartyActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
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
            mBoundService.setCallback(null);
            unbindService(mConnection);
            mShouldUnbind = false;
        }
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

        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarFragmentFrame, new SearchBarFragment(this), "SearchBarFragment").commitAllowingStateLoss();

        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, new ShowSongFragment(this), "ShowSongFragment").commitAllowingStateLoss();

      
         Intent serviceIntent = new Intent(this, ClientService.class);
        serviceIntent.putExtra(Constants.TOKEN, token);
        serviceIntent.putExtra(Constants.ADDRESS, getIntent().getStringExtra(Constants.ADDRESS));
        serviceIntent.putExtra(Constants.PASSWORD, getIntent().getStringExtra(Constants.PASSWORD));
        startService(serviceIntent);
        doBindService();
      
         /*recyclerView = (RecyclerView) binding.searchOutputRecyclerView;
        //List<String> myDataset = Arrays.asList("Silas", "Jannik");
        mAdapter = new PartyAcRecycAdapter(new ArrayList<Track>(), this);
        recyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);*/
    }

    @Override
    public void exitConnection() {
        getSupportFragmentManager().beginTransaction().
               replace(R.id.showSongFragmentFrame, new ExitConnectionFragment(this), "ExitConnectionFragment").commitAllowingStateLoss();
    }

    @Override
    public void denyExit() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, new ShowSongFragment(this), "ShowSongFragment").commitAllowingStateLoss();
    }

    @Override
    public void searchForSongs() {
        Log.d("ShowSongFragment", "back to show");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongFragmentFrame, new SearchSongsOutputFragment(), "ShowSongFragment").commitAllowingStateLoss();
    }
 

    public void search(View view) {
        mBoundService.search(binding.etSearch.getText().toString());
    }

    @Override
    public void returnSong(int i) {
        Log.d(NAME, "Item pressed: " + i);
        Log.d(NAME, mBoundService.getTracks().get(i).toString());
        Toast.makeText(this,  mBoundService.getTracks().get(i).getName() + " has been added to queue!", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void updateView(List<Track> tracks) {
        this.runOnUiThread(() -> {
            mAdapter.setmDataset(tracks);
            binding.testrecycler.getAdapter().notifyDataSetChanged();
        });
    }
}