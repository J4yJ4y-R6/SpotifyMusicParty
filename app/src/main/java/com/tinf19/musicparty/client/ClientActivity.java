package com.tinf19.musicparty.client;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.databinding.ActivityClientBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientActivity extends AppCompatActivity {

    ActivityClientBinding binding;
    private static final String TAG = ClientActivity.class.getName();
    private ImageButton scanQRCodeImageButton;
    private IntentIntegrator qrScan;
    private EditText ipAddressEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClientBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ipAddressEditText = binding.etAddress;
        passwordEditText = binding.etPassword;

        qrScan = new IntentIntegrator(this);

        scanQRCodeImageButton = binding.loginViaQRCodeImageButton;
        if(scanQRCodeImageButton != null) {
            scanQRCodeImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    qrScan.initiateScan();
                }
            });
        }
    }

    public void nextPage(View view) {
        Intent intent = new Intent(this, PartyActivity.class);
        intent.putExtra(Constants.TOKEN, getIntent().getStringExtra("token"));
        intent.putExtra(Constants.PASSWORD, binding.etPassword.getText().toString());
        intent.putExtra(Constants.ADDRESS, binding.etAddress.getText().toString());
        intent.putExtra(Constants.USERNAME, binding.usernameEditText.getText().toString());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Kein Ergebnis gefunden", Toast.LENGTH_LONG).show();
            } else {
                try {
                    JSONObject obj = new JSONObject(result.getContents());
//                    https://www.simplifiedcoding.net/android-qr-code-scanner-tutorial/
                    Log.d(TAG, "onActivityResult: " + obj.getString("ipaddress"));
                    Log.d(TAG, "onActivityResult: " + obj.getString("password"));
                    if(ipAddressEditText != null) {
                        //TODO: connect after scan or setText()
                        ipAddressEditText.setText(obj.getString("ipaddress"));
                        passwordEditText.setText(obj.getString("password"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}