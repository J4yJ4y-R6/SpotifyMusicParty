package com.tinf19.musicparty.util;

import android.util.Log;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Artist;
import com.tinf19.musicparty.music.Playlist;
import com.tinf19.musicparty.music.Track;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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

    public void getUserID(String token, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("me")
                .build();
        get(token, completeURL, spotifyHelperCallback);
    }

    public void createPlaylist(String token, String name, @NotNull String userID,  String description, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment(userID)
                .addPathSegment("playlists")
                .build();
        JSONObject sampleObject = new JSONObject()
                .put("name", name)
                .put("public", false)
                .put("description", description);
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        post(token, completeURL, body, spotifyHelperCallback);
    }

    public void getQueFromPlaylist(String token, String id, int page, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("tracks")
                .addQueryParameter("offset", String.valueOf(100 * page))
                .build();
        get(token, completeURL, spotifyHelperCallback);
    }

    public void checkPlaylistFollowStatus(String token, String id, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("followers")
                .build();
        Log.d(TAG, "Follow playlist with id:  " + id + ": " + completeURL.toString());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("public", false);
        RequestBody body = RequestBody.create(jsonObject.toString(), Constants.JSON);
        put(token, "application/json", completeURL, body, spotifyHelperCallback);
    }

    public void deletePlaylist(String token, String id, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("followers")
                .build();
        delete(token, completeURL, spotifyHelperCallback);
    }

    public void addItemsToPlaylist(String token, List<Track> playlist, String id, int page, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("tracks")
                .build();
        JSONArray jsonArray = new JSONArray();
        for(int i = page * 100; i < (Math.min(playlist.size(), (page + 1) * 100)); i++) {
            jsonArray.put(playlist.get(i).getURI());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uris", jsonArray);
        RequestBody body = RequestBody.create(jsonObject.toString(), Constants.JSON);
        post(token, completeURL, body, spotifyHelperCallback);
    }

    public void deleteItem(String token, String id, String uri, int playlistSize, int queSize, int position, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("tracks")
                .build();
        int index = playlistSize - queSize + position;
        if(index < 0 || index >= playlistSize)
            return;
        JSONObject uris = new JSONObject()
                .put("uri", uri)
                .put("positions", new JSONArray().put(index));
        ;
        JSONObject sampleObject = new JSONObject()
                .put("tracks", new JSONArray().put(uris));
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        delete(token, completeURL, body, spotifyHelperCallback);
    }

    public void moveItem(String token, String id, int from, int to, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("tracks")
                .build();
        Log.d(TAG, "Making request to " + completeURL.toString());
        JSONObject sampleObject = new JSONObject()
                .put("range_start", from)
                .put("insert_before", to);
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        put(token, "application/json", completeURL, body, spotifyHelperCallback);
    }

    public void updatePlaylistCover(String token, String userID, String id, byte[] encoded, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null || userID == null) return;
        HttpUrl completeUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("users")
                .addPathSegment(userID)
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("images")
                .build();
        RequestBody body = RequestBody.create(encoded);
        put(token, "image/jpeg", completeUrl, body, spotifyHelperCallback);
    }

    public void updatePlaylistName(String token, String name, String id, SpotifyHelperCallback spotifyHelperCallback) throws JSONException {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .build();
        JSONObject sampleObject = new JSONObject()
                .put("name", name);
        RequestBody body = RequestBody.create(sampleObject.toString(), Constants.JSON);
        put(token, "application/json", completeURL, body, spotifyHelperCallback);
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
                Log.e(TAG, e.getMessage(), e);
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(Call call, Response response) {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }

    private void post(String token, HttpUrl url, RequestBody body, SpotifyHelperCallback spotifyHelperCallback) {
        Log.d(TAG, "Making request to " + url.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, e.getMessage(), e);
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }

    private void put(String token, String contentType, HttpUrl url, RequestBody body, SpotifyHelperCallback spotifyHelperCallback) {
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", contentType)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, e.getMessage(), e);
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }

    private void delete(String token, HttpUrl url, SpotifyHelperCallback spotifyHelperCallback) {
        Log.d(TAG, "Making request to " + url.toString());
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, e.getMessage(), e);
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }

    private void delete(String token, HttpUrl url, RequestBody body, SpotifyHelperCallback spotifyHelperCallback) {
        Log.d(TAG, "Making request to " + url.toString());
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, e.getMessage(), e);
                spotifyHelperCallback.onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                spotifyHelperCallback.onResponse(response);
            }
        });
    }

    public ArrayList<Track> extractSong(String data) throws JSONException {
        ArrayList<Track> tracks = new ArrayList<>();
        JSONObject jsonObject = null;
        jsonObject = new JSONObject(data);
        jsonObject = jsonObject.getJSONObject("tracks");
        JSONArray jsonArray = jsonObject.getJSONArray("items");
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject track = jsonArray.getJSONObject(i);
            JSONArray artists = track.getJSONArray("artists");
            Artist[] array = new Artist[artists.length()];
            for (int j = 0; j < array.length; j++) {
                JSONObject artist = artists.getJSONObject(j);
                array[j] = new Artist(artist.getString("id"), artist.getString("name"));
            }
            String[] image = track
                    .getJSONObject("album")
                    .getJSONArray("images")
                    .getJSONObject(2)
                    .getString("url").split("/");
            String[] imageFull = track
                    .getJSONObject("album")
                    .getJSONArray("images")
                    .getJSONObject(1)
                    .getString("url").split("/");
            tracks.add(
                    new com.tinf19.musicparty.music.Track(
                            track.getString("id"),
                            track.getString("name"),
                            array,
                            image[image.length - 1],
                            imageFull[imageFull.length - 1],
                            track.getInt("duration_ms"),
                            track.getJSONObject("album").getString("name")));
        }
        return tracks;
    }

    public ArrayList<String> showAutofills(String data) throws JSONException {
        ArrayList<String> titles = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(data);
        Iterator<String> keys = jsonObject.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            if(jsonObject.get(key) instanceof JSONObject) {
                JSONObject keysObject = jsonObject.getJSONObject(key);
                JSONArray jsonArray = keysObject.getJSONArray("items");
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject track = jsonArray.getJSONObject(i);
                    titles.add(track.getString("name"));
                }
            }
        }
        return titles;
    }


}
