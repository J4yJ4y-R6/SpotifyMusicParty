package com.tinf19.musicparty.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.databinding.ActivityJoinBinding;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.databinding.ActivityClientBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class JoinActivity extends AppCompatActivity {

    private static final String TAG = JoinActivity.class.getName();
    private final Random rand = new Random();
    private ActivityJoinBinding binding;
    private IntentIntegrator qrScan;
    private EditText ipAddressEditText;
    private EditText passwordEditText;



    //Android lifecycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ipAddressEditText = binding.etAddress;
        passwordEditText = binding.etPassword;

        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setPrompt(getResources().getString(R.string.text_qrButtonHint));
        qrScan.setBeepEnabled(false);

        if(getIntent().getData() != null) {
            String password = getIntent().getData().getQueryParameter(Constants.PASSWORD);
            String ip = getIntent().getData().getQueryParameter(Constants.ADDRESS);

            if (password != null && ip != null) {
                ipAddressEditText.setText(ip);
                passwordEditText.setText(password);
            }
        }

        TextView usernameDesciptionTextView = binding.usernameDescriptionTextView;
        binding.infoUsernameSymbolImageButton.setOnClickListener(v -> usernameDesciptionTextView.setVisibility(usernameDesciptionTextView.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE));

        TextView loginDescriptionTextView = binding.loginDescriptionTextView;
        binding.infoIpSymbolImageButton.setOnClickListener(v -> loginDescriptionTextView.setVisibility(loginDescriptionTextView.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE));

        binding.loginViaQRCodeImageButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: scan initialized");
            qrScan.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d(TAG, "onActivityResult: empty qr code response");
                Toast.makeText(this, getString(R.string.text_qrCodeNoResult), Toast.LENGTH_LONG).show();
            } else {
                try {
                    JSONObject obj = new JSONObject(result.getContents());
                    ipAddressEditText.setText(obj.getString("ipaddress"));
                    passwordEditText.setText(obj.getString("password"));
                    if(!binding.usernameEditText.getText().toString().equals("")) {
                        nextPage(binding.getRoot());
                    }

                } catch (JSONException e) {
                    Log.d(TAG, "Wrong QR-Code-Data: " + result.getContents());
                    Toast.makeText(this, getString(R.string.text_qrCodeNotMusicParty), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    
    public void nextPage(View view) {
        Intent intent = new Intent(this, ClientActivity.class);
        intent.putExtra(Constants.PASSWORD, binding.etPassword.getText().toString());
        intent.putExtra(Constants.ADDRESS, binding.etAddress.getText().toString());
        if (!binding.usernameEditText.getText().toString().equals(""))
            intent.putExtra(Constants.USERNAME, binding.usernameEditText.getText().toString());
        else
            intent.putExtra(Constants.USERNAME, randomIdentifier());
        view.getContext().startActivity(intent);
    }

    public String randomIdentifier() {
        StringBuilder builder = new StringBuilder();
        while(builder.toString().length() == 0) {
            int length = rand.nextInt(5)+5;
            for(int i = 0; i < length; i++) {
                builder.append(Constants.LEXICON.charAt(rand.nextInt(Constants.LEXICON.length())));
            }
        }
        Log.d(TAG, "generated random name: " + builder.toString());
        return builder.toString();
    }


}