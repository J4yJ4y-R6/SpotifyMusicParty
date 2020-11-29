package com.tinf19.musicparty.util;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyHelper {

    private static final String TAG = SpotifyHelper.class.getName();
    private OkHttpClient client = new OkHttpClient();

    public interface SpotifyHelperCallback {
        void onFailure();
        void onResponse(Response response);
    }


    public void search(String query, String type, String limit, String token, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("type", type)
                .addQueryParameter("limit", limit )
                .build();
        get(token, completeURL, spotifyHelperCallback);
    }





    private void get(String token, HttpUrl url, SpotifyHelperCallback spotifyHelperCallback) {
        Log.d(TAG, "Making request to " + url.toString());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(Call call, Response response) {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }




}
