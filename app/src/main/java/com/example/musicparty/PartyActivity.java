package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

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

public class PartyActivity extends AppCompatActivity {

    ActivityPartyBinding binding;
    private static final String NAME = PartyActivity.class.getName();
    private static final String HOST = "api.spotify.com";
    private static final int PORT = 1403;
    private static String token;
    private int limit = 10;
    private String type = "track";
    private Thread clientThread;
    private Socket clientSocket;
    private List<Track> tracks = new ArrayList<>();
    private PartyAcRecycAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        token = getIntent().getStringExtra("token");
        binding = ActivityPartyBinding.inflate(getLayoutInflater());
        connect();
        setContentView(binding.getRoot());
        
        RecyclerView recyclerView = (RecyclerView) binding.testrecycler;
        //List<String> myDataset = Arrays.asList("Silas", "Jannik");
        mAdapter = new PartyAcRecycAdapter(new ArrayList<Track>());
        recyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    public void search(View view){
        String query = binding.etSearch.getText().toString();
        OkHttpClient client = new OkHttpClient();
        PartyActivity partyActivity = this;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("type", type)
                .addQueryParameter("limit", String.valueOf(limit))
                .build();
        Log.d(NAME, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Log.d(NAME, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                e.printStackTrace();
                Log.d(NAME, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(NAME,"Request Successful.");
                }

                // Read data in the worker thread
                final String data = response.body().string();

                // Display the requested data on UI in main thread
                partyActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Display requested url data as string into text view
                        //binding.tvResult.setText(data);
                        extractSongs(data);
                    }
                });
            }
        });
    }

    public void extractSongs(String data) {
        try {
            tracks.clear();
            JSONObject jsonObject = new JSONObject(data);
            jsonObject = jsonObject.getJSONObject("tracks");
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject track = jsonArray.getJSONObject(i);
                JSONArray artists = track.getJSONArray("artists");
                Artist [] array = new Artist[artists.length()];
                for(int j = 0; j < array.length; j++) {
                    JSONObject artist = artists.getJSONObject(j);
                    array[j] = new Artist(artist.getString("id"), artist.getString("name"));
                }
                String image = track
                        .getJSONObject("album")
                        .getJSONArray("images")
                        .getJSONObject(2)
                        .getString("url");
                tracks.add(
                        new Track(
                                track.getString("id"),
                                track.getString("name"), array,
                                image,
                                track.getInt("duration_ms")
                        ));
                Log.d(NAME, tracks.get(i).toString());
            }
            mAdapter.setmDataset(tracks);
            binding.testrecycler.getAdapter().notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void activateBinding() {
        setContentView(binding.getRoot());
    }

    public void connect(){
        clientThread = new Thread(new ClientThread(getIntent().getStringExtra("address"), getIntent().getStringExtra("password")));
        clientThread.start();
    }

    class ClientThread implements Runnable {

        private String address;
        private BufferedReader input;
        private DataOutputStream out;
        private String password;
        private String line;

        public ClientThread(String address, String password) {
            this.address = address;
            this.password = password;
        }

        @Override
        public void run() {
            try {
                Log.d(NAME, "Try to login to " + address + ":" + PORT + " with password " + this.password);
                clientSocket = new Socket(this.address, PORT);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new DataOutputStream(clientSocket.getOutputStream());
                out.writeBytes("~LOGIN~" + this.password + "\n\r");
                out.flush();
                Log.d(NAME, "Connect successful");
                while (true)  {
                    line = input.readLine();
                    if (line != null) {
                        String [] parts = line.split("~");
                        String attribute = "";
                        if (parts.length > 2)
                            attribute = parts[2];
                        if (parts.length > 1) {
                            Commands command = Commands.valueOf(parts[1]);
                            switch (command) {
                                case LOGIN:
                                    Log.d(NAME, attribute);
                                    break;
                                case QUIT:
                                    clientSocket.close();
                                    return;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(NAME, e.getMessage(), e);

            }
        }
    }
}