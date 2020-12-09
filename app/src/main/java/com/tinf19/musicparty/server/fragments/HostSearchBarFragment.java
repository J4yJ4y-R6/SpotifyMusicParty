package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.ForAllCallback;
import com.tinf19.musicparty.util.SpotifyHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

/**
 * Fragment where the host can search for songs with the Spotify-API and show the response in
 * {@link com.tinf19.musicparty.fragments.SearchSongsOutputFragment}
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostSearchBarFragment extends Fragment {

    private static final String TAG = HostSearchBarFragment.class.getName();
    private final SpotifyHelper spotifyHelper = new SpotifyHelper();
    private final List<Track> tracks = new ArrayList<>();
    private HostSearchBarCallback hostSearchBarCallback;
    private AutoCompleteTextView searchText;
    private ImageButton searchButton;
    private ArrayAdapter<String> adapter;

    public interface HostSearchBarCallback extends ForAllCallback {
        void searchForSongs(List<Track> tracks);
        void openSavedPlaylistsFragment();
    }

    /**
     * Constructor to set the callback
     * @param hostSearchBarCallback Communication callback for
     * {@link com.tinf19.musicparty.server.HostActivity}
     */
    public HostSearchBarFragment(HostSearchBarCallback hostSearchBarCallback) { this.hostSearchBarCallback = hostSearchBarCallback; }

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public HostSearchBarFragment() { }



    //Android lifecycle methods

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostSearchBarCallback)
            hostSearchBarCallback = (HostSearchBarCallback) context;
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
                    if(!searchText.getText().toString().equals("")) {
                        spotifyHelper.search(s.toString(), "artist,track", "5", hostSearchBarCallback.getToken(), new SpotifyHelper.SpotifyHelperCallback() {
                            @Override
                            public void onFailure() { }

                            @Override
                            public void onResponse(Response response) {
                                try {
                                    if(!response.isSuccessful()) Log.d(TAG, "Request was not successful" + response.body().string());
                                    else {
                                        Log.d(TAG, "Request Successful.");
                                        showAutofills(response.body().string());
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage(), e);
                                } finally {
                                    response.close();
                                }
                            }
                        });
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        searchButton = view.findViewById(R.id.hostSearchImageButton);
        if(searchButton != null) {
            searchButton.setOnClickListener(v -> {
                searchButton.setEnabled(false);
                if  (searchText != null && !searchText.getText().toString().trim().equals("")) {
                    String searchQuery = searchText.getText().toString().trim();
                    Log.d(TAG, "search for songs with query: " + searchQuery);
                    searchText.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    spotifyHelper.search(searchQuery, "track", "15", hostSearchBarCallback.getToken(), new SpotifyHelper.SpotifyHelperCallback() {
                        @Override
                        public void onFailure() {
                            if(searchButton != null)
                                getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
                        }

                        @Override
                        public void onResponse(Response response) {
                            if(!response.isSuccessful()){
                                if(searchButton != null)
                                    getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
                                Log.e(TAG, "Request was not successful");
                            }else {
                                Log.d(TAG,"Request Successful.");
                            }
                            final String data;
                            try {
                                data = response.body().string();
                                extractSongs(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            response.close();
                        }
                    });
                } else {
                    searchButton.setEnabled(true);
                }
            });
        }
        ImageButton favoriteSavedPlaylistsImageButton = view.findViewById(R.id.hostSavedPlaylistsImageButton);
        if (favoriteSavedPlaylistsImageButton != null) {
            favoriteSavedPlaylistsImageButton.setOnClickListener(v -> hostSearchBarCallback.openSavedPlaylistsFragment());
        }

        return view;
    }

    /**
     * Showing guesses to automatically fill the search input field below the field
     * @param data Response data from the http-request with song titles
     */
    public void showAutofills(String data) {
        try {
            if(searchText != null) {
                adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, spotifyHelper.showAutofills(data));
                getActivity().runOnUiThread( () -> searchText.setAdapter(adapter));
            } else {
                Log.d(TAG, "not able to show the hints under searchText");
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Clearing the search input field
     */
    public void clearSearch() {
        if(searchText != null)
            searchText.getText().clear();
    }

    public void extractSongs(String data) {
        try {
            tracks.clear();
            tracks.addAll(spotifyHelper.extractSong(data));
            Log.d(TAG, "host searched for " + tracks.size() + "songs");
            hostSearchBarCallback.searchForSongs(tracks);
            if(searchButton != null)
                getActivity().runOnUiThread(() ->  searchButton.setEnabled(true));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}