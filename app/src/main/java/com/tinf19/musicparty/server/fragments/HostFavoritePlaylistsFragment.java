package com.tinf19.musicparty.server.fragments;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Playlist;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.ForAllCallback;
import com.tinf19.musicparty.server.adapter.HostFavoritePlaylistsAdapter;
import com.tinf19.musicparty.util.SpotifyHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.Response;

import static android.app.Activity.RESULT_OK;


/**
 * Fragment where the host can see the playlist he saved from passed parties.
 * The id and the name of the playlist are saved in SharedPreferences
 * By clicking one of the cards the host is able to interact with the playlist by
 * {@link android.app.Dialog}. His options are:
 * <ol>
 *     <li>Play the playlist.</li>
 *     <li>Deleting the playlist from his Shared Preferences. By that he will unfollow the playlist
 *     in Spotify automatically so he cannot see it in the Spotify-App. This action has to be
 *     confirmed in another {@link android.app.Dialog}.</li>
 *     <li>Changing the name of the playlist. The name will change in the SharedPreferences and
 *     in Spotify synchronous. This action has to be confirmed.</li>
 * </ol>
 * All of these on click methods are set by the {@link HostFavoritePlaylistsAdapter}.
 * @auhtor Jannik Junker
 * @author Silas Wessely
 * @see SharedPreferences
 * @see android.app.Dialog
 * @since 1.1
 */
public class HostFavoritePlaylistsFragment extends Fragment implements HostFavoritePlaylistsAdapter.GalleryCallback {

    private static final String TAG = HostFavoritePlaylistsFragment.class.getName();
    private final SpotifyHelper spotifyHelper = new SpotifyHelper();
    private SharedPreferences savePlaylistMemory;
    private Playlist[] playlists;
    private HostFavoritePlaylistCallback favoritePlaylistsCallback;
    private HostFavoritePlaylistsAdapter.HostFavoritePlaylistAdapterCallback hostFavoritePlaylistCallback;
    private String playlistID;
    private String playlistCoverUrl;
    public HostFavoritePlaylistsAdapter hostFavoritePlaylistsAdapter;
    private int counter;

    public interface HostFavoritePlaylistCallback extends ForAllCallback {
        void changePlaylistCover(String id, Bitmap image);
    }

    /**
     * Constructor to set the callbacks
     * @param favoritePlaylistsCallback Communication callback for
     * {@link com.tinf19.musicparty.server.HostActivity}
     * @param hostFavoritePlaylistAdapterCallback Communication callback which is given by the
     *                                            {@link HostFavoritePlaylistsAdapter}
     */
    public HostFavoritePlaylistsFragment(HostFavoritePlaylistCallback favoritePlaylistsCallback, HostFavoritePlaylistsAdapter.HostFavoritePlaylistAdapterCallback hostFavoritePlaylistAdapterCallback) {
        this.favoritePlaylistsCallback = favoritePlaylistsCallback;
        this.hostFavoritePlaylistCallback = hostFavoritePlaylistAdapterCallback;
    }

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public HostFavoritePlaylistsFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        savePlaylistMemory = getContext().getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
        playlists = new Playlist[savePlaylistMemory.getAll().size()];
        counter = 0;
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
        new Thread(() -> {
            while(counter < savePlaylistMemory.getAll().size());
            Log.d(TAG, savePlaylistMemory.getAll().size() + " playlists are assigned to the RecyclerView");
            getActivity().runOnUiThread(() -> {
                hostFavoritePlaylistsAdapter.setPlaylists(new ArrayList<>(Arrays.asList(playlists)), idList);
                hostFavoritePlaylistsAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostFavoritePlaylistCallback)
            favoritePlaylistsCallback = (HostFavoritePlaylistCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_favorite_playlists, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.skipVotingRecyclerview);
        hostFavoritePlaylistsAdapter = new HostFavoritePlaylistsAdapter(new ArrayList<>(), this, hostFavoritePlaylistCallback);
        if(recyclerView != null) {
            recyclerView.setAdapter(hostFavoritePlaylistsAdapter);
            if(getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT)
                recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            else
                recyclerView.setLayoutManager(new StaggeredGridLayoutManager(5, StaggeredGridLayoutManager.VERTICAL));
        }

        return view;
    }

    /**
     * Updating the dataset because an image has changed or a playlist got deleted.
     */
    public void updateRecyclerView() {
        Log.d(TAG, "notifying the adapter that the dataset has changed");
        getActivity().runOnUiThread( () -> {
            try {
                Thread.sleep(2000);
                hostFavoritePlaylistsAdapter.notifyDataSetChanged();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @return Get thee current orientation of the screen.
     */
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
        Log.d(TAG, "current screen orientation is: " + orientation);
        return orientation;
    }

    /**
     * For each playlist we are getting the url of the playlist cover with a http request in the
     * {@link SpotifyHelper}.
     * @param id Playlist-ID
     * @param name Playlist-Name
     * @param key Pointer for the playlists array for which playlist we are trying to get the cover
     *            url
     */
    private void getPlaylistCoverUrl(String id, String name, int key) {
        String token = favoritePlaylistsCallback.getToken();
        spotifyHelper.getPlaylistCoverUrl(token, id, new SpotifyHelper.SpotifyHelperCallback() {
            @Override
            public void onFailure() {
                Log.d(TAG, "Request Failed");
            }

            @Override
            public void onResponse(Response response) {
                if (!response.isSuccessful()) {
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } else {
                    try {
                        Log.d(TAG, "Request successfully");
                        final String data = response.body().string();
                        response.close();
                        JSONArray jsonArray = new JSONArray(data);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        playlistCoverUrl = jsonObject.getString("url");
                        Playlist playlist = new Playlist(id, name, playlistCoverUrl);
                        playlists[key] = playlist;
                        counter++;

                    } catch (JSONException | IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        });
    }

    /**
     * Set all playlist in the playlists array to attach them to the RecyclerView with the
     * {@link HostFavoritePlaylistsAdapter}.
     * @param key Pointer for the playlists array for which playlist we are trying to get the cover
     *            url
     */
    private void setPlaylists(int key) {
        try {
            String response = savePlaylistMemory.getString("" + key, "");
            if (!response.equals("")) {
                JSONObject element = new JSONObject(response);
                String name = element.getString("name");
                String id = element.getString("id");
                getPlaylistCoverUrl(id, name, key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opening the local gallery to pick a new playlist cover.
     * @param intent Intent from the ImagePicker
     * @param playlistID Id from the playlist which cover gets to be changed
     */
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
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                int imgWidth = selectedImage.getWidth();
                int imgHeight = selectedImage.getHeight();
                if(imgWidth > imgHeight) {
                    int dif = (imgWidth - imgHeight) / 2;
                    selectedImage = Bitmap.createBitmap(selectedImage, dif, 0, imgWidth - dif, imgHeight);
                }
                else {
                    int dif = (imgHeight - imgWidth) / 2;
                    selectedImage = Bitmap.createBitmap(selectedImage, 0, dif, imgWidth, imgHeight - dif);
                }
                if(selectedImage.getByteCount() > 250000) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedImage, 250, 250, false);
                    if(scaledBitmap.getByteCount() > 250000)
                        Toast.makeText(getActivity(), "Dein Bild ist zu groß. Die Maximalgröße für Playlist-Cover ist 250KB", Toast.LENGTH_LONG).show();
                    else {
                        if(playlistID != null) {
                            favoritePlaylistsCallback.changePlaylistCover(playlistID, scaledBitmap);
                            playlistID = "";
                        }
                        else
                            Log.d(TAG, "onActivityResult: Image could not be updated because no playlist attached");
                    }
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