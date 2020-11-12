package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.tinf19.musicparty.R;

public class HostClosePartyFragment extends Fragment {

    private ClosePartyCallback closePartyCallback;
    private static final String TAG = HostClosePartyFragment.class.getName();
    private EditText savePlaylistNameEditText;
    private Switch savePlaylistSwitch;
    private boolean savePlaylist;

    public interface ClosePartyCallback {
        void denyEndParty();
        void acceptEndParty(boolean save);
        boolean savePlaylistInSharedPreferences(String name);
    }

    public HostClosePartyFragment(ClosePartyCallback closePartyCallback) {
        this.closePartyCallback = closePartyCallback;
    }

    public HostClosePartyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_host_close_party, container, false);

        Button denyEndPartyButton = view.findViewById(R.id.denyEndPartyButton);
        if(denyEndPartyButton != null) {
            denyEndPartyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePartyCallback.denyEndParty();
                }
            });
        }
        Button acceptEndPartyButton = view.findViewById(R.id.acceptEndPartyButton);
        if(acceptEndPartyButton != null) {
            acceptEndPartyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(savePlaylistNameEditText != null) {
                        String playlistName = savePlaylistNameEditText.getText().toString();
                        if(!playlistName.equals("")) {
                            if(!closePartyCallback.savePlaylistInSharedPreferences(playlistName))
                                Toast.makeText(getContext(), "Playlist konnte nicht gespeichert werden", Toast.LENGTH_SHORT).show();
                            else
                                closePartyCallback.acceptEndParty(true);
                        } else {
                            if(savePlaylist) {
                                Toast.makeText(getContext(), "Playlist konnte nicht gespeichert werden", Toast.LENGTH_SHORT).show();
                            } else {
                                closePartyCallback.acceptEndParty(false);
                            }
                        }
                    }

                }
            });
        }

        savePlaylistSwitch = view.findViewById(R.id.savePlaylistSwitch);
        savePlaylistNameEditText = view.findViewById(R.id.savePlaylistNameEditText);
        if(savePlaylistSwitch != null) {
            savePlaylistSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    savePlaylist = isChecked;
                    if(isChecked) {
                        Log.d(TAG, "onCheckedChanged: Playlist wird gespeichert");
                        if(savePlaylistNameEditText != null) {
                            savePlaylistNameEditText.setEnabled(true);
                            savePlaylistNameEditText.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "onCheckedChanged: Playlist wird nicht gespeichert");
                        if(savePlaylistNameEditText != null) {
                            savePlaylistNameEditText.setEnabled(false);
                            savePlaylistNameEditText.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        }

        return view;
    }

}