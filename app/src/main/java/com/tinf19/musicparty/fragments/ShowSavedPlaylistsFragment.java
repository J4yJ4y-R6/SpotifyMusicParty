package com.tinf19.musicparty.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.gson.JsonObject;
import com.tinf19.musicparty.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class ShowSavedPlaylistsFragment extends Fragment {

    private static final String TAG = ShowSavedPlaylistsFragment.class.getName();
    private SharedPreferences savePlaylistMemory;
    private ArrayList<ImageButton> buttons = new ArrayList<>(9);;
    private ArrayList<TextView> headers = new ArrayList<>(9);;
    private ArrayList<ViewSwitcher> viewSwitchers = new ArrayList<>(9);
    private ArrayList<EditText> changeNames = new ArrayList<>(9);
    private ArrayList<String> idList;
    private FavoritePlaylistsCallback favoritePlaylistsCallback;
    private View view;

    public interface FavoritePlaylistsCallback{
        void reloadFavoritePlaylistsFragment();
        void playFavoritePlaylist(String id, ArrayList<String> idList);
        void changePlaylistName(String name, String id);
    }

    public ShowSavedPlaylistsFragment(FavoritePlaylistsCallback favoritePlaylistsCallback) {
        this.favoritePlaylistsCallback = favoritePlaylistsCallback;
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
        for(int i = 0; i < headers.size(); i++) {
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

    private void setPlaylists(TextView header, ImageButton button, ViewSwitcher viewSwitcher, EditText changeName, int key) {
        try {
            String response =  savePlaylistMemory.getString("" + key, "");
            if(!response.equals("")) {
                JSONObject element = new JSONObject(response);
                String name = element.getString("name");
                String id = element.getString("id");
                Log.d(TAG, "setPlaylists: " + key + ": " + element.toString());
                if(changeName != null && viewSwitcher != null && header != null && button != null) {
                    header.setText(name);
                    viewSwitcher.setVisibility(View.VISIBLE);
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                new AlertDialog.Builder(getContext())
                                        .setTitle(name)
                                        .setMessage("Möchtest du die Playlist löschen, abspielen oder umbenenen?")
                                        .setPositiveButton("", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if(viewSwitcher.getCurrentView() instanceof EditText && changeName != null) {
                                                    String newName = changeName.getText().toString();
                                                    if(!newName.equals("")) {
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
                                                }
                                                viewSwitcher.showNext();
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
                                                SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                                editor.remove("" + key);
                                                editor.apply();
                                                if(key < 8) {
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
                                                Toast.makeText(getContext(), "Die " + name + " wurde erfolgreich aus deinen Favoriten gelöscht", Toast.LENGTH_SHORT).show();
                                                favoritePlaylistsCallback.reloadFavoritePlaylistsFragment();
                                            }
                                        })
                                        .setNegativeButtonIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_trash_can_button))
                                        .show();
                        }
                    });
                }
            }
            else if(header != null && button != null && response.equals("")) {
                viewSwitcher.setVisibility(View.INVISIBLE);
                button.setVisibility(View.INVISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}