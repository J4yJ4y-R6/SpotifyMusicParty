package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;

public class SettingsHostFragment extends Fragment {

    private static final String TAG = SettingsHostFragment.class.getName();
    private EditText changePartyName;
    private TextView ipAddressTextView;
    private TextView passwordTextView;
    private GetServerSettings getServerSettings;

    public interface GetServerSettings {
        String getIpAddress();
        String getPassword();
    }

    public SettingsHostFragment(GetServerSettings getServerSettings) {
        this.getServerSettings = getServerSettings;
    }

    public SettingsHostFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(ipAddressTextView != null) {
            String text = getString(R.string.text_ip_address) + ": " + getServerSettings.getIpAddress();;
            Log.d(TAG, "onStart IpAddress: " + text);
            ipAddressTextView.setText(text);
        }
        if(passwordTextView != null) {
            String text = getString(R.string.app_password) + ": " + getServerSettings.getPassword();;
            Log.d(TAG, "onStart Password: " + text);
            passwordTextView.setText(text);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings_host, container, false);

        ipAddressTextView = view.findViewById(R.id.ipAddressSettingsTextView);
        passwordTextView = view.findViewById(R.id.passwordSettingsTextView);

        Button savePartyNameButton = view.findViewById(R.id.savePartyNameButton);
        if (savePartyNameButton != null) {
            savePartyNameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changePartyName = view.findViewById(R.id.changePartyNameEditText);
                    if(changePartyName != null && changePartyName.getText().toString().equals("")) {
                        String newPartyName = changePartyName.getText().toString();
                        Log.d(TAG, "onClick: new Party Name set to: " + newPartyName);
                    }
                }
            });
        }
        return view;
    }
}