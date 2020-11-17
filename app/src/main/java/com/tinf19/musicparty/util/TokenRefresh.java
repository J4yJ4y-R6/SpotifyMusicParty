package com.tinf19.musicparty.util;

import android.util.Log;

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

public class TokenRefresh implements Runnable {

    private final String CODE;
    private String token;
    private String refreshToken;
    private boolean stopped;
    private int duration;
    private TokenCallback tokenCallback;

    public interface TokenCallback{
        void afterConnection(String token);
        void afterRefresh(String token);
    }

    public TokenRefresh( String code, TokenCallback tokenCallback) {
        this.CODE = code;
        this.tokenCallback = tokenCallback;
    }

    @Override
    public void run() {
        try {
            setToken();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        while(!stopped){
            try {
                if(duration > 60) Thread.sleep((duration - 60) * 1000);
                else return;
                refreshToken();
            } catch (InterruptedException | IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

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
                .header("Authorization", Credentials.basic(Constants.CLIENT_ID, Constants.CLIENT_SECRET))
                .build();
        Response response = client.newCall(request).execute();
        if(response.isSuccessful()) {
            JSONObject responseBody = new JSONObject(response.body().string());
            duration = responseBody.getInt("expires_in");
            token = responseBody.getString("access_token");
            refreshToken = responseBody.getString("refresh_token");
            tokenCallback.afterConnection(token);
        }
        response.close();
    }

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
                .header("Authorization", Credentials.basic(Constants.CLIENT_ID, Constants.CLIENT_SECRET))
                .build();
        Response response = client.newCall(request).execute();
        if(response.isSuccessful()) {
            JSONObject responseBody = new JSONObject(response.body().string());
            duration = responseBody.getInt("expires_in");
            token = responseBody.getString("access_token");
            tokenCallback.afterRefresh(token);
        }
        response.close();
    }
}
