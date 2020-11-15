package com.tinf19.musicparty.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.gson.JsonObject;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.DownloadImageTask;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;


public class ShowSavedPlaylistsFragment extends Fragment {

    private static final String HOST = "api.spotify.com";
    public static final int RESULT_LOAD_IMAGE = 1;
    private static final String TAG = ShowSavedPlaylistsFragment.class.getName();
    private SharedPreferences savePlaylistMemory;
    private ArrayList<ImageView> buttons = new ArrayList<>(9);;
    private ArrayList<TextView> headers = new ArrayList<>(9);;
    private ArrayList<ViewSwitcher> viewSwitchers = new ArrayList<>(9);
    private ArrayList<EditText> changeNames = new ArrayList<>(9);
    private ArrayList<String> idList;
    private FavoritePlaylistsCallback favoritePlaylistsCallback;
    private View view;
    private String playlistID;
    private String token;

    public interface FavoritePlaylistsCallback{
        void reloadFavoritePlaylistsFragment();
        void playFavoritePlaylist(String id, ArrayList<String> idList);
        void changePlaylistName(String name, String id);
        void changePlaylistCover(String id, Bitmap image);
        void deletePlaylist(String id);
    }

    public ShowSavedPlaylistsFragment(FavoritePlaylistsCallback favoritePlaylistsCallback, String token) {
        this.favoritePlaylistsCallback = favoritePlaylistsCallback;
        this.token = token;
    }

    public ShowSavedPlaylistsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        savePlaylistMemory = getContext().getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
        idList = new ArrayList<>();
        for(int i = 0; i < 1; i++) {
            try {
                JSONObject playlist = new JSONObject(savePlaylistMemory.getString("" + i, ""));
                String id = playlist.getString("id");
                idList.add(id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < headers.size(); i++) {
            setPlaylists(headers.get(i), buttons.get(i), viewSwitchers.get(i), changeNames.get(i), i);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_show_saved_playlists, container, false);

        headers.add(0, view.findViewById(R.id.gridZeroZeroHeaderTextView));
        buttons.add(0, view.findViewById(R.id.gridZeroZeroImageButton));
        viewSwitchers.add(0, view.findViewById(R.id.gridZeroZeroSwitcher));
        changeNames.add(0, view.findViewById(R.id.gridZeroZeroHeaderEditText));
        headers.add(1, view.findViewById(R.id.gridZeroOneTextView));
        buttons.add(1, view.findViewById(R.id.gridZeroOneImageButton));
        viewSwitchers.add(1, view.findViewById(R.id.gridZeroOneSwitcher));
        changeNames.add(1, view.findViewById(R.id.gridZeroOneHeaderEditText));
        headers.add(2, view.findViewById(R.id.gridZeroTwoTextView));
        buttons.add(2, view.findViewById(R.id.gridZeroTwoImageButton));
        viewSwitchers.add(2, view.findViewById(R.id.gridZeroTwoSwitcher));
        changeNames.add(2, view.findViewById(R.id.gridZeroTwoHeaderEditText));
        headers.add(3, view.findViewById(R.id.gridOneZeroTextView));
        buttons.add(3, view.findViewById(R.id.gridOneZeroImageButton));
        viewSwitchers.add(3, view.findViewById(R.id.gridOneZeroSwitcher));
        changeNames.add(3, view.findViewById(R.id.gridOneZeroHeaderEditText));
        headers.add(4, view.findViewById(R.id.gridOneOneTextView));
        buttons.add(4, view.findViewById(R.id.gridOneOneImageButton));
        viewSwitchers.add(4, view.findViewById(R.id.gridOneOneSwitcher));
        changeNames.add(4, view.findViewById(R.id.gridOneOneHeaderEditText));
        headers.add(5, view.findViewById(R.id.gridOneTwoTextView));
        buttons.add(5, view.findViewById(R.id.gridOneTwoImageButton));
        viewSwitchers.add(5, view.findViewById(R.id.gridOneTwoSwitcher));
        changeNames.add(5, view.findViewById(R.id.gridOneTwoHeaderEditText));
        headers.add(6, view.findViewById(R.id.gridTwoZeroTextView));
        buttons.add(6, view.findViewById(R.id.gridTwoZeroImageButton));
        viewSwitchers.add(6, view.findViewById(R.id.gridTwoZeroSwitcher));
        changeNames.add(6, view.findViewById(R.id.gridTwoZeroHeaderEditText));
        headers.add(7, view.findViewById(R.id.gridTwoOneTextView));
        buttons.add(7, view.findViewById(R.id.gridTwoOneImageButton));
        viewSwitchers.add(7, view.findViewById(R.id.gridTwoOneSwitcher));
        changeNames.add(7, view.findViewById(R.id.gridTwoOneHeaderEditText));
        headers.add(8, view.findViewById(R.id.gridTwoTwoTextView));
        buttons.add(8, view.findViewById(R.id.gridTwoTwoImageButton));
        viewSwitchers.add(8, view.findViewById(R.id.gridTwoTwoSwitcher));
        changeNames.add(8, view.findViewById(R.id.gridTwoTwoHeaderEditText));

        return view;
    }

    private void getPlaylistCoverUrl(String id, ImageView button) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("images")
                .build();
        Log.d(TAG, "Search for autofill in " + completeURL.toString());
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
                        Log.d(TAG, "onResponse: got autofill hints");
                        final String data = response.body().string();
                        response.close();
                        JSONArray jsonArray = new JSONArray(data);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        String url = jsonObject.getString("url");
                        new DownloadImageTask(button).execute(url);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setPlaylists(TextView header, ImageView button, ViewSwitcher viewSwitcher, EditText changeName, int key) {
        try {
            String response =  savePlaylistMemory.getString("" + key, "");
            if(!response.equals("")) {
                JSONObject element = new JSONObject(response);
                String name = element.getString("name");
                String id = element.getString("id");

                getPlaylistCoverUrl(id, button);

                Log.d(TAG, "setPlaylists: " + key + ": " + element.toString());
                if(changeName != null && viewSwitcher != null && header != null && button != null) {
                    header.setText(name);
                    viewSwitcher.setVisibility(View.VISIBLE);
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!(viewSwitcher.getCurrentView() instanceof EditText)) {
                                new AlertDialog.Builder(getContext())
                                        .setTitle(name)
                                        .setMessage(getString(R.string.text_favoritePlaylistsDialogWindow))
                                        .setPositiveButton("", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                new AlertDialog.Builder(getContext())
                                                        .setTitle(getString(R.string.text_editPlaylist_dialog))
                                                        .setMessage(getString(R.string.text_chooseEditOption_dialog))
                                                        .setPositiveButton("", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                viewSwitcher.showNext();
                                                            }
                                                        })
                                                        .setPositiveButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_edit_button))
                                                        .setNegativeButton("", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Log.d(TAG, "onClick: edit cover");
                                                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                                                //TODO wie kann man das mit dem Intent übergeben
                                                                playlistID = id;
                                                                photoPickerIntent.setType("image/*");
                                                                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMAGE);
                                                            }
                                                        })
                                                        .setNegativeButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_edit_playlistname_button))
                                                        .show();
                                            }
                                        })
                                        .setPositiveButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_edit_button))
                                        .setNeutralButton("", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                favoritePlaylistsCallback.playFavoritePlaylist(id, idList);
                                            }
                                        })
                                        .setNeutralButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_play_track_button))
                                        .setNegativeButton("", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                new AlertDialog.Builder(getContext())
                                                        .setTitle(getString(R.string.text_delete))
                                                        .setMessage(getString(R.string.text_chooseDeleteOption_dialog))
                                                        .setPositiveButton("", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                //window is closing
                                                            }
                                                        })
                                                        .setPositiveButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_deny_button))
                                                        .setNegativeButton("", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                                                editor.remove("" + key);
                                                                editor.apply();
                                                                if (key < 8) {
                                                                    int counter = key;
                                                                    while (counter <= 8) {
                                                                        String nextPlaylist = savePlaylistMemory.getString("" + (counter + 1), "");
                                                                        if (!nextPlaylist.equals(""))
                                                                            editor.putString("" + counter, nextPlaylist);
                                                                        else {
                                                                            editor.remove("" + counter);
                                                                        }
                                                                        editor.apply();
                                                                        counter++;
                                                                    }
                                                                }
                                                                String toastMessage = name + getString(R.string.text_toastPlaylistDeleted);
                                                                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                                                                favoritePlaylistsCallback.reloadFavoritePlaylistsFragment();
                                                                favoritePlaylistsCallback.deletePlaylist(id);
                                                            }
                                                        })
                                                        .setNegativeButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_accept_button))
                                                        .show();
                                            }
                                        })
                                        .setNegativeButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_trash_can_button))
                                        .show();
                            } else {
                                new AlertDialog.Builder(getContext())
                                        .setTitle(R.string.text_editPlaylist_dialog)
                                        .setMessage(getString(R.string.text_acceptEditOption_dialog))
                                        .setPositiveButton("", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String newName = changeName.getText().toString();
                                                if (!newName.equals("")) {
                                                    JSONObject playlist = new JSONObject();
                                                    try {
                                                        playlist.put("name", newName);
                                                        playlist.put("id", id);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                                    editor.putString("" + key, playlist.toString());
                                                    editor.apply();
                                                    Log.d(TAG, "onClick: new name is: " + newName);
                                                    favoritePlaylistsCallback.changePlaylistName(newName, id);
                                                    header.setText(newName);
                                                }
                                                viewSwitcher.showNext();
                                            }
                                        })
                                        .setPositiveButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_edit_button))
                                        .show();
                            }
                        }
                    });
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(resultCode == RESULT_OK && data != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                Log.d(TAG, "onActivityResult: " + selectedImage.getByteCount());
                if(selectedImage.getByteCount() > 250000) {
                    Toast.makeText(getActivity(), "Dein Bild ist zu groß. Die Maximalgröße für Playlist-Cover ist 250KB", Toast.LENGTH_LONG).show();
                } else {
                    if(playlistID != null) {
                        favoritePlaylistsCallback.changePlaylistCover(playlistID, selectedImage);
                        playlistID = "";
                    }
                    else
                        Log.d(TAG, "onActivityResult: error");
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