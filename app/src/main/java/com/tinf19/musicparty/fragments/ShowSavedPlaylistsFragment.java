package com.tinf19.musicparty.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.text.BoringLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Playlist;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.ForAllCallback;
import com.tinf19.musicparty.util.ShowSavedPlaylistRecycAdapter;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;

public class ShowSavedPlaylistsFragment extends Fragment implements ShowSavedPlaylistRecycAdapter.GalleryCallback {

    private static final String TAG = ShowSavedPlaylistsFragment.class.getName();
    private String token;
    private SharedPreferences savePlaylistMemory;
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private ShowSavedPlaylistsFragment.ShowSavedPlaylistCallback favoritePlaylistsCallback;
    private ShowSavedPlaylistRecycAdapter.FavoritePlaylistCallback favoritePlaylistCallback;
    private String playlistID;
    private String playlistCoverUrl;
    private ShowSavedPlaylistRecycAdapter showSavedPlaylistRecycAdapter;

    public interface ShowSavedPlaylistCallback extends ForAllCallback {
        void changePlaylistCover(String id, Bitmap image);
    }

    public ShowSavedPlaylistsFragment(ShowSavedPlaylistCallback favoritePlaylistsCallback, ShowSavedPlaylistRecycAdapter.FavoritePlaylistCallback favoritePlaylistCallback) {
        this.favoritePlaylistsCallback = favoritePlaylistsCallback;
        this.favoritePlaylistCallback = favoritePlaylistCallback;
    }

    public ShowSavedPlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.TOKEN, token);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        playlists.clear();
        savePlaylistMemory = getContext().getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
        ArrayList<String> idList = new ArrayList<>();
        for(int i = 0; i < savePlaylistMemory.getAll().size(); i++) {
            try {
                JSONObject playlist = new JSONObject(savePlaylistMemory.getString("" + i, ""));
                String id = playlist.getString("id");
                idList.add(id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < savePlaylistMemory.getAll().size(); i++) {
            setPlaylists(i);
        }
        while(playlists.size() < savePlaylistMemory.getAll().size());
        showSavedPlaylistRecycAdapter.setPlaylists(playlists, idList);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof  ShowSavedPlaylistCallback)
            favoritePlaylistsCallback = (ShowSavedPlaylistCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_show_saved_playlists, container, false);

        if(savedInstanceState != null)
            token = savedInstanceState.getString(Constants.TOKEN, "");

        RecyclerView recyclerView = view.findViewById(R.id.gridRecyclerview);
        showSavedPlaylistRecycAdapter = new ShowSavedPlaylistRecycAdapter(new ArrayList<>(), this, favoritePlaylistCallback);
        if(recyclerView != null) {
            recyclerView.setAdapter(showSavedPlaylistRecycAdapter);
            if(getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT)
                recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            else
                recyclerView.setLayoutManager(new StaggeredGridLayoutManager(5, StaggeredGridLayoutManager.VERTICAL));
        }

        return view;
    }

    public int getScreenOrientation()
    {
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int orientation;
        if(screenWidth < screenHeight){
            orientation = Configuration.ORIENTATION_PORTRAIT;
        } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
        }
        return orientation;
    }

    private void getPlaylistCoverUrl(String id, String name) {
        String token = favoritePlaylistsCallback.getToken();
        if(token == null) return;
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("images")
                .build();
        Request request = new Request.Builder()
                .url(completeURL)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.d(TAG, "onFailure: failed to get autofillHints");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.d(TAG, "onResponse: " + response.body().string());
                    throw new IOException("Error : " + response);
                } else {
                    try {
                        final String data = response.body().string();
                        response.close();
                        JSONArray jsonArray = new JSONArray(data);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        playlistCoverUrl = jsonObject.getString("url");
                        Playlist playlist = new Playlist(id, name, playlistCoverUrl);
                        Log.d(TAG, "onResponse: " + playlist.toString());
                        playlists.add(playlist);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setPlaylists(int key) {
        try {
            String response = savePlaylistMemory.getString("" + key, "");
            if (!response.equals("")) {
                JSONObject element = new JSONObject(response);
                String name = element.getString("name");
                String id = element.getString("id");
                getPlaylistCoverUrl(id, name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openGalleryForUpload(Intent intent, String playlistID) {
        this.playlistID = playlistID;
        startActivityForResult(intent, Constants.RESULT_LOAD_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                if(selectedImage.getByteCount() > 250000) {
                    Toast.makeText(getActivity(), "Dein Bild ist zu groß. Die Maximalgröße für Playlist-Cover ist 250KB", Toast.LENGTH_LONG).show();
                } else {
                    if(playlistID != null) {
                        favoritePlaylistsCallback.changePlaylistCover(playlistID, selectedImage);
                        playlistID = "";
                    }
                    else
                        Log.d(TAG, "onActivityResult: Image could not be updated because no playlist attached");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getActivity(), "You have picked Image", Toast.LENGTH_LONG).show();
        }
    }
}