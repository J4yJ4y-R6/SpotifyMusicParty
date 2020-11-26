package com.tinf19.musicparty;

import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.tinf19.musicparty.client.ClientActivity;
import com.tinf19.musicparty.databinding.ActivityMainBinding;
import com.tinf19.musicparty.server.HostActivity;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.tinf19.musicparty.util.Constants;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = MainActivity.class.getName();
    private static String token;


    @Override
    protected void onStart() {
        super.onStart();
        ImageView imageView = findViewById(R.id.animatedLogo);
        animate(imageView);
    }

    public void animate(ImageView v) {
        Drawable d = v.getDrawable();
        if(d instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat) d;
            avd.start();
        } else if(d instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d;
            avd.start();
        }
    }

    public void animateClick(View view) {
        ImageView v = (ImageView) view;
        Drawable d = v.getDrawable();
        if(d instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat) d;
            avd.start();
        } else if(d instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d;
            avd.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //loginToSpotify();
        binding.button.setEnabled(true);
        binding.button2.setEnabled(true);
    }

    public void changeHost(View view){
        Intent intent = new Intent(this, HostActivity.class);
        //intent.putExtra(Constants.TOKEN, token);
        startActivity(intent);
    }

    public void changeClient(View view){
        Intent intent = new Intent(this, ClientActivity.class);
        //intent.putExtra(Constants.TOKEN, token);
        startActivity(intent);
    }

    public void loginToSpotify() {
        Log.d(TAG, "Trying to get auth token");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(BuildConfig.CLIENT_ID, AuthorizationResponse.Type.TOKEN, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{"streaming", "app-remote-control", "playlist-modify-private", "playlist-modify-public", "user-read-private", "ugc-image-upload"});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, Constants.REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == Constants.REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    token = response.getAccessToken();
                    Log.d(TAG, "Expires in: " + response.getExpiresIn());
                    Log.d(TAG, "Token gained successful: " + token);
                    binding.button.setEnabled(true);
                    binding.button2.setEnabled(true);
                    break;
                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.e(TAG, "Spotify login error");
                    break;
                // Most likely auth flow was cancelled
                case CODE:
                    Log.d(TAG, "Code: " + response.getCode());
                    Log.d(TAG, "State: " + response.getState());
                    break;
                default:
                    // Handle other cases
                    Log.e(TAG, "Something went wrong");
            }
        }
    }


}