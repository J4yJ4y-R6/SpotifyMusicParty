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

import com.google.android.material.snackbar.Snackbar;
import com.tinf19.musicparty.R;

import org.json.JSONException;

/**
 * Fragment where the host can end the party and save the current queue state to his
 * SharedPreferences so he can listen to it again.
 * @author Jannik Junker
 * @author Silas Wessely
 * @see android.content.SharedPreferences
 * @since 1.1
 */
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

    /**
     * Constructor to set the callback
     * @param hostClosePartyCallback Communication callback for
     * {@link com.tinf19.musicparty.server.HostActivity}
     */
    public HostClosePartyFragment(HostClosePartyCallback hostClosePartyCallback) { this.hostClosePartyCallback = hostClosePartyCallback; }

    /**
     * Empty-Constructor which is necessary in fragments
     */
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
                            Snackbar.make(this.requireView(),
                                    getString(R.string.text_toastPlaylistNameNeeded),
                                    Snackbar.LENGTH_SHORT).show();
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