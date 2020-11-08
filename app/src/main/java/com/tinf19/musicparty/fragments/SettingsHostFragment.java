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
    private String ipAddress;
    private String conPassword;
    private EditText changePartyName;

    public SettingsHostFragment(String password, String address) {
        this.ipAddress = address;
        this.conPassword = password;
    }

    public SettingsHostFragment() {
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
        View view = inflater.inflate(R.layout.fragment_settings_host, container, false);

        TextView ipAddressTextView = view.findViewById(R.id.ipAddressSettingsTextView);
        if(ipAddressTextView != null) {
            String text = "IP-Adresse: " + ipAddress;
            Log.d(TAG, "onCreateView: " + text);
            ipAddressTextView.setText(text);
        }
        TextView passwordTextView = view.findViewById(R.id.passwordSettingsTextView);
        if(passwordTextView != null) {
            String text = "Password: " + conPassword;
            Log.d(TAG, "onCreateView: " + text);
            passwordTextView.setText(text);
        }

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