package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.Track;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchBarFragment extends Fragment {

    //ActivityPartyBinding binding;
    private static final String NAME = SearchBarFragment.class.getName();
    public SearchForSongs searchForSongs;
    private static final String HOST = "api.spotify.com";
    private List<Track> tracks = new ArrayList<>();
    private int limit = 10;
    private String type = "track";
    private String token;

    public interface SearchForSongs {
        void searchForSongs(List<Track> tracks);
    }

    public SearchBarFragment(SearchForSongs searchForSongs, String token) {
        this.searchForSongs = searchForSongs;
        this.token = token;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_bar, container, false);
        //token = savedInstanceState.getBundle();

        Button searchButton = view.findViewById(R.id.searchButton);
        if(searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText searchText = view.findViewById(R.id.searchEditText);
                    if  (searchText != null && searchText.getText().toString().trim().equals("")) {
                        search(searchText.getText().toString().trim());
                    }
                }
            });
        }

        return view;
    }

    public void search(String query){
        OkHttpClient client = new OkHttpClient();
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
                final String data = response.body().string();

                // Read data in the worker thread
                extractSongs(data);
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
                Artist[] array = new Artist[artists.length()];
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
                                track.getString("name"),
                                array,
                                image,
                                track.getInt("duration_ms"),
                                track.getJSONObject("album").getString("name")));
                Log.d(NAME, tracks.get(i).toString());
            }
            searchForSongs.searchForSongs(tracks);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}