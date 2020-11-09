package com.tinf19.musicparty.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class SettingsHostFragment extends Fragment {

    private static final String TAG = SettingsHostFragment.class.getName();
    private EditText changePartyName;
    private TextView ipAddressTextView;
    private TextView passwordTextView;
    private GetServerSettings getServerSettings;
    private String partyName = "Music Party";

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
        ImageButton shareButton = view.findViewById(R.id.shareButtonSettingsImageButton);

        //TODO: check on Strings in edittext?
        if(shareButton != null) {
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "*Verbindung zu " + partyName + ":* \n" + ipAddressTextView.getText() + "\n" + passwordTextView.getText());
                    sendIntent.setType("text/plain");

                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                }
            });
        }

        ImageButton shareQRButton = view.findViewById(R.id.shareQRButtonSettingsImageButton);

        //TODO: check on QR Code exists?
        if(shareQRButton != null) {
            shareQRButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO: Generate File somehow
                    File imageFile = new File("test");
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    String ext = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1);
                    String type = mime.getMimeTypeFromExtension(ext);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));
                    sendIntent.setType(type);

                    Intent shareIntent = Intent.createChooser(sendIntent, null);
//                    startActivity(shareIntent);
                }
            });
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