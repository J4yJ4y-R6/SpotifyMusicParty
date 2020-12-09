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
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This is a helper class which is doing all http request to interact with the Spotify-API.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class SpotifyHelper {

    private static final String TAG = SpotifyHelper.class.getName();
    private final OkHttpClient client = new OkHttpClient();

    public interface SpotifyHelperCallback {
        void onFailure();
        void onResponse(Response response);
    }


    /**
     * Searching for songs in the Spoitfy-API to show the results in the RecyclerView of
     * {@link com.tinf19.musicparty.fragments.SearchSongsOutputFragment}.
     * @param query Entered string from the user to search for
     * @param type Search type can be set to tracks or tracks and artists
     * @param limit Count of results the search want to get
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * Searching for a playlist cover in the Spoitfy-API.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param id Playlist-id to identify which cover is search for
     * @param spotifyHelperCallback Callback to handle the request results
     */
    public void getPlaylistCoverUrl(String token, String id, SpotifyHelperCallback spotifyHelperCallback) {
        if(token == null) return;
        HttpUrl completeURL = new HttpUrl.Builder()
                .scheme("https")
                .host(Constants.HOST)
                .addPathSegment("v1")
                .addPathSegment("playlists")
                .addPathSegment(id)
                .addPathSegment("images")
                .build();
        get(token, completeURL, spotifyHelperCallback);
    }

    /**
     * Get the Spotify-User-Id from the Spotify-API.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * Creating a now Spotify-Playlist after the host asked to save the current queue state.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param name Playlist-Name entered by the host
     * @param userID Host Spotify-User-Id
     * @param description Playlist-Description
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * Follow the playlist after starting it from
     * {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment}.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param id Playlist-id to identify which follow status is search for
     * @param spotifyHelperCallback Callback to handle the request results
     * @throws JSONException when jsonObject could not be created
     */
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

    /**
     * Unfollow a existing Spotify-Playlist when the host deleted it from his SharedPreferences.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param id Playlist-id to identify which playlist should be unfollowed is search for
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param playlist {@link List} with all songs currently saved in the queue
     * @param id Playlist-Id to identify to which playlist the tracks shall be added
     * @param page The Spotify-API only allow to upload 100 songs with one request so the page
     *             counts the calls done
     * @param spotifyHelperCallback Callback to handle the request results
     * @throws JSONException when jsonObject could not be created
     */
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

    /**
     * Deleting a song from a Spotify-Playlist.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param id Playlist-Id to identify from which playlist the song shall be deleted
     * @param uri Uri to identify the song which shall be deleted in the Spotify-Playlist
     * @param playlistSize Size of a List with every song played at this party
     * @param queSize Size of the queue with all songs coming next
     * @param position Position to identify the song which shall be deleted in the List
     * @param spotifyHelperCallback Callback to handle the request results
     * @throws JSONException when jsonObject could not be created
     */
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

    /**
     * Swapping two items in the Spotify-Playlist
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param id Playlist-Id to identify from which playlist two song swap
     * @param from Position of the first song
     * @param to Position of the second song
     * @param spotifyHelperCallback Callback to handle the request results
     * @throws JSONException when jsonObject could not be created
     */
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

    /**
     * Updating a playlist cover with the Spotify-API.
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param userID Host Spotify-User-Id
     * @param id Playlist-Id to identify from which playlist two song swap
     * @param encoded Encoded string of the new playlist cover image
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * Updating a playlist name with the Spotify-API
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param name New playlist name
     * @param id Playlist-Id to identify from which playlist name will be changed
     * @param spotifyHelperCallback Callback to handle the request results
     * @throws JSONException when jsonObject could not be created
     */
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



    /**
     * GET-HTTP-Request
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param url HttpUrl for the request
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * POST-HTTP-Request
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param url HttpUrl for the request
     * @param body RequestBody with the information to be posted to the Spotify-API
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * PUT-HTTP-Request
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param contentType Deciding what type of content put to the request
     * @param url HttpUrl for the request
     * @param body RequestBody with the information to be put to the Spotify-API
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * DELETE-HTTP-Request wihtout body
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param url HttpUrl for the request
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * GET-HTTP-Request
     * @param token Spotify-token which is unique for every Spotify-user and needs to be refreshed
     *              every hour.
     * @param url HttpUrl for the request
     * @param body RequestBody with the information about which item should be deleted
     * @param spotifyHelperCallback Callback to handle the request results
     */
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

    /**
     * Extracting the songs from a jsonObject and returning an {@link ArrayList} to work with in the
     * {@link com.tinf19.musicparty.fragments.SearchSongsOutputFragment}.
     * @param data JsonString with all songs returned from the http request
     * @return Get an {@link ArrayList} with all songs returned from the http request
     * @throws JSONException when the jsonObject could not be created
     */
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

    /**
     * Showing all songs from a jsonObject in the autofill hints in the
     * {@link com.tinf19.musicparty.client.fragments.ClientSearchBarFragment} or
     * {@link com.tinf19.musicparty.server.fragments.HostSearchBarFragment}.
     * @param data JsonString with all songs returned from the http request
     * @return Get an {@link ArrayList} with all songs returned from the http request
     * @throws JSONException when the jsonObject could not be created
     */
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
