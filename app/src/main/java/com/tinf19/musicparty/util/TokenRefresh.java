package com.tinf19.musicparty.util;

import android.util.Log;

import com.tinf19.musicparty.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Spotify requires a unique token from each Spotify-User to make request to the Spotify-API.
 * This token needs to be refreshed every 60 Seconds. The TokenRefresh-Class is managing the refresh
 * request and set the new token.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class TokenRefresh implements Runnable {

    private final String CODE;
    private String token;
    private String refreshToken;
    private int duration;
    private TokenRefreshCallback tokenRefreshCallback;

    public interface TokenRefreshCallback {
        void afterConnection(String token);
        void afterRefresh(String token);
    }

    /**
     * Constructor to set the code and the callback
     * @param code Code necessary to get a Spotify-API-Token
     * @param tokenRefreshCallback Communication callback to set the new token
     */
    public TokenRefresh( String code, TokenRefreshCallback tokenRefreshCallback) {
        this.CODE = code;
        this.tokenRefreshCallback = tokenRefreshCallback;
    }

    @Override
    public void run() {
        try {
            setToken();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        while(true){
            try {
                if(duration > 60) Thread.sleep((duration - 60) * 1000);
                else return;
                refreshToken();
            } catch (InterruptedException | IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get a new token from the Spotify-API and set it in the activities.
     * @throws JSONException when jsonObject could not be created
     * @throws IOException when the response did not work
     */
    private void setToken() throws JSONException, IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host("accounts.spotify.com")
                .addPathSegment("api")
                .addPathSegment("token")
                .build();
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", CODE)
                .add("redirect_uri", Constants.REDIRECT_URI)
                .build();
        Request request = new Request.Builder()
                .url(completeURL)
                .post(body)
                .header("Authorization", Credentials.basic(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET))
                .build();
        Response response = client.newCall(request).execute();
        if(response.isSuccessful()) {
            JSONObject responseBody = new JSONObject(response.body().string());
            duration = responseBody.getInt("expires_in");
            token = responseBody.getString("access_token");
            refreshToken = responseBody.getString("refresh_token");
            tokenRefreshCallback.afterConnection(token);
        }
        response.close();
    }

    /**
     * Refreshing the token from the Spotify-API every 60 Minutes.
     * @throws JSONException when jsonObject could not be created
     * @throws IOException when the response did not work
     */
    private void refreshToken() throws JSONException, IOException {
        OkHttpClient client = new OkHttpClient();
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host("accounts.spotify.com")
                .addPathSegment("api")
                .addPathSegment("token")
                .build();
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        Request request = new Request.Builder()
                .url(completeURL)
                .post(body)
                .header("Authorization", Credentials.basic(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET))
                .build();
        Response response = client.newCall(request).execute();
        if(response.isSuccessful()) {
            Log.d(TokenRefresh.class.getName(), "token has been refreshed");
            JSONObject responseBody = new JSONObject(response.body().string());
            duration = responseBody.getInt("expires_in");
            token = responseBody.getString("access_token");
            tokenRefreshCallback.afterRefresh(token);
        }
        response.close();
    }
}
