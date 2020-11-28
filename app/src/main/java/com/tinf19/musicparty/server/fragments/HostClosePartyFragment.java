package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.tinf19.musicparty.R;

import org.json.JSONException;

public class HostClosePartyFragment extends Fragment {

    private static final String TAG = HostClosePartyFragment.class.getName();
    private ClosePartyCallback closePartyCallback;
    private EditText savePlaylistNameEditText;
    private boolean savePlaylist;

    public interface ClosePartyCallback {
        void denyEndParty();
        void acceptEndParty();
        void createPlaylistFromArrayList(String name) throws JSONException;
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof  ClosePartyCallback)
            closePartyCallback = (ClosePartyCallback) context;
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
                            try {
                                closePartyCallback.createPlaylistFromArrayList(playlistName);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if(savePlaylist) {
                                Toast.makeText(getContext(), getString(R.string.text_toastPlaylistNameNeeded), Toast.LENGTH_SHORT).show();
                            } else {
                                closePartyCallback.acceptEndParty();
                            }
                        }
                    }

                }
            });
        }

        Switch savePlaylistSwitch = view.findViewById(R.id.savePlaylistSwitch);
        savePlaylistNameEditText = view.findViewById(R.id.savePlaylistNameEditText);
        if(savePlaylistSwitch != null) {
            savePlaylistSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    savePlaylist = isChecked;
                    if(isChecked) {
                        if(savePlaylistNameEditText != null) {
                            savePlaylistNameEditText.setEnabled(true);
                            savePlaylistNameEditText.setVisibility(View.VISIBLE);
                        }
                    } else {
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