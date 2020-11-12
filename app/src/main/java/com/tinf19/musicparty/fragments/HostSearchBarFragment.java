package com.tinf19.musicparty.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

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

public class HostSearchBarFragment extends Fragment {

    private static final String TAG = SearchBarFragment.class.getName();
    public SearchForSongs searchForSongs;
    private static final String HOST = "api.spotify.com";
    private List<Track> tracks = new ArrayList<>();
    private int limit = 10;
    private String type = "track";
    private String token;
    private EditText searchText;
    private ImageButton searchButton;

    public interface SearchForSongs {
        void searchForSongs(List<Track> tracks);
        void openSavedPlaylistsFragment();
    }

    public HostSearchBarFragment() {
        // Required empty public constructor
    }

    public HostSearchBarFragment(SearchForSongs searchForSongs, String token) {
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
        View view = inflater.inflate(R.layout.fragment_host_search_bar, container, false);
        //token = savedInstanceState.getBundle();
        searchText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        if(searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchButton.setEnabled(false);
                    if  (searchText != null && !searchText.getText().toString().trim().equals("")) {
                        searchText.clearFocus();
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        search(searchText.getText().toString().trim());
                    } else {
                        searchButton.setEnabled(true);
                    }
                }
            });
        }
        ImageButton favoriteSavedPlaylistsImageButton = view.findViewById(R.id.hostSavedPlaylistsImageButton);
        if (favoriteSavedPlaylistsImageButton != null) {
            favoriteSavedPlaylistsImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: show saved playlists");
                    searchForSongs.openSavedPlaylistsFragment();
                }
            });
        }

        return view;
    }

    public void search(String query) {
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
        Log.d(TAG, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Log.d(TAG, request.headers().toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Do something when request failed
                if(searchButton != null)
                    getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
                e.printStackTrace();
                Log.d(TAG, "Request Failed.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    if(searchButton != null)
                        getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
                    throw new IOException("Error : " + response);
                }else {
                    Log.d(TAG,"Request Successful.");
                }
                final String data = response.body().string();
                response.close();

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
                Log.d(TAG, tracks.get(i).toString());
            }
            searchForSongs.searchForSongs(tracks);
            if(searchButton != null)
                getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}