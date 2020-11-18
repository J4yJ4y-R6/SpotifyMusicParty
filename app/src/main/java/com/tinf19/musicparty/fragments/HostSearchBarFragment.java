package com.tinf19.musicparty.fragments;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.ForAllCallback;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HostSearchBarFragment extends Fragment {

    private static final String TAG = HostSearchBarFragment.class.getName();
    private HostSearchForSongs searchForSongs;
    private List<Track> tracks = new ArrayList<>();
    private AutoCompleteTextView searchText;
    private ImageButton searchButton;
    private ArrayAdapter<String> adapter;

    public interface HostSearchForSongs extends ForAllCallback {
        void searchForSongs(List<Track> tracks);
        void openSavedPlaylistsFragment();
    }

    public HostSearchBarFragment() {
        // Required empty public constructor
    }

    public HostSearchBarFragment(HostSearchForSongs searchForSongs) {
        this.searchForSongs = searchForSongs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostSearchForSongs)
            searchForSongs = (HostSearchForSongs) context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_search_bar, container, false);

        searchText = view.findViewById(R.id.hostSearchEditText);
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getRealSize(displaySize);
        searchText.setDropDownWidth(displaySize.x);
        searchText.setDropDownHeight(displaySize.y / 3);
        searchText.setDropDownVerticalOffset(10);
        if(searchText != null) {
            searchText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(!searchText.getText().toString().equals("")) search(s.toString(), false, "artist,track", "5");
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        searchButton = view.findViewById(R.id.hostSearchImageButton);
        if(searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchButton.setEnabled(false);
                    if  (searchText != null && !searchText.getText().toString().trim().equals("")) {
                        searchText.clearFocus();
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        search(searchText.getText().toString().trim(), true, "track", "15");
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
                public void onClick(View v) { searchForSongs.openSavedPlaylistsFragment(); }
            });
        }

        return view;
    }

    public void search(String query, boolean usage, String type, String limit) {
        String token = searchForSongs.getToken();
        if(token == null) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("type", type)
                .addQueryParameter("limit", limit )
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(searchButton != null && usage)
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
                if(usage)
                    extractSongs(data);
                else
                    showAutofills(data);
            }
        });
    }

    public void showAutofills(String data) {
        try {
            ArrayList<String> titles = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(data);
            Iterator<String> keys = jsonObject.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                if(jsonObject.get(key) instanceof JSONObject) {
                    JSONObject keysObject = jsonObject.getJSONObject(key);
                    JSONArray jsonArray = keysObject.getJSONArray("items");
                    for(int i = 0; i < jsonArray.length(); i++) {
                        JSONObject track = jsonArray.getJSONObject(i);
                        titles.add(track.getString("name"));
                    }
                }
            }
            if(searchText != null) {
                adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, titles);
                getActivity().runOnUiThread( () -> searchText.setAdapter(adapter));
            } else {
                Log.d(TAG, "showAutofills: not able to show the hints under searchText");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void clearSearch() {
        if(searchText != null)
            searchText.getText().clear();
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