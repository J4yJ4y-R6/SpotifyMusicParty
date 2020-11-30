package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tinf19.musicparty.R;

import org.json.JSONException;

public class HostClosePartyFragment extends Fragment {

    private static final String TAG = HostClosePartyFragment.class.getName();
    private HostClosePartyCallback hostClosePartyCallback;
    private EditText savePlaylistNameEditText;
    private boolean savePlaylist;

    public interface HostClosePartyCallback {
        void denyEndParty();
        void acceptEndParty();
        void createPlaylistFromArrayList(String name) throws JSONException;
    }

    public HostClosePartyFragment(HostClosePartyCallback hostClosePartyCallback) { this.hostClosePartyCallback = hostClosePartyCallback; }

    public HostClosePartyFragment() { }



    //Android lifecycle methods

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostClosePartyCallback)
            hostClosePartyCallback = (HostClosePartyCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_host_close_party, container, false);

        Button denyEndPartyButton = view.findViewById(R.id.denyEndPartyButton);
        if(denyEndPartyButton != null) {
            denyEndPartyButton.setOnClickListener(v -> hostClosePartyCallback.denyEndParty());
        }
        Button acceptEndPartyButton = view.findViewById(R.id.acceptEndPartyButton);
        if(acceptEndPartyButton != null) {
            acceptEndPartyButton.setOnClickListener(v -> {
                if(savePlaylistNameEditText != null) {
                    String playlistName = savePlaylistNameEditText.getText().toString();
                    if(!playlistName.equals("")) {
                        try {
                            hostClosePartyCallback.createPlaylistFromArrayList(playlistName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(savePlaylist) {
                            Toast.makeText(getContext(), getString(R.string.text_toastPlaylistNameNeeded), Toast.LENGTH_SHORT).show();
                        } else {
                            hostClosePartyCallback.acceptEndParty();
                        }
                    }
                }

            });
        }

        SwitchCompat savePlaylistSwitch = view.findViewById(R.id.savePlaylistSwitch);
        savePlaylistNameEditText = view.findViewById(R.id.savePlaylistNameEditText);
        if(savePlaylistSwitch != null) {
            savePlaylistSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                savePlaylist = isChecked;
                if(isChecked) {
                    Log.d(TAG, "showing EditText for new playlist name");
                    if(savePlaylistNameEditText != null) {
                        savePlaylistNameEditText.setEnabled(true);
                        savePlaylistNameEditText.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.d(TAG, "hiding EditText for new playlist name");
                    if(savePlaylistNameEditText != null) {
                        savePlaylistNameEditText.setEnabled(false);
                        savePlaylistNameEditText.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        return view;
    }

}