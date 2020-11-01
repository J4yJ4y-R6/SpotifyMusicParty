package com.example.musicparty;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicparty.databinding.ActivityMainBinding;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.spotify.sdk.android.auth.AuthorizationRequest;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String NAME = MainActivity.class.getName();
    private static final String CLIENT_ID = "f4789369fed34bf4a880172871b7c4e4";
    private static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    private static final int REQUEST_CODE = 1337;
    private static String token;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loginToSpotify();
    }

    public void changeHost(View view){
        Intent intent = new Intent(this, HostActivity.class);
        intent.putExtra(Constants.TOKEN, token);
        startActivity(intent);
    }

    public void changeClient(View view){
        Intent intent = new Intent(this, ClientActivity.class);
        intent.putExtra(Constants.TOKEN, token);
        startActivity(intent);
    }

    public void loginToSpotify() {
        Log.d(NAME, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "app-remote-control", "playlist-modify-private", "playlist-modify-public", "user-read-private"});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    token = response.getAccessToken();
                    Log.d(NAME, "Expires in: " + response.getExpiresIn());
                    Log.d(NAME, "Token gained successful: " + token);
                    binding.button.setEnabled(true);
                    binding.button2.setEnabled(true);
                    break;
                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.e(NAME, "Spotify login error");
                    break;
                // Most likely auth flow was cancelled
                case CODE:
                    Log.d(NAME, "Code: " + response.getCode());
                    Log.d(NAME, "State: " + response.getState());
                    break;
                default:
                    // Handle other cases
                    Log.e(NAME, "Something went wrong");
            }
        }
    }


}