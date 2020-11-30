package com.tinf19.musicparty.music;

import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Track {

    private static final String TAG = Track.class.getName();
    private final String id;
    private final String name;
    private final Artist [] artist;
    private final long duration;
    private final String cover;
    private final String coverFull;
    private final String album;

    public Track(String id, String name, Artist [] artist, String cover, String coverFull, long duration, String album) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.cover = cover;
        this.coverFull = coverFull;
        this.duration = duration;
        this.album = album;
    }

    public Track(String id, String name, List<com.spotify.protocol.types.Artist> artist, String cover, String coverFull, long duration, String album) {
        Artist [] artists = new Artist[artist.size()];
        for (int i = 0; i < artist.size(); i++) {
            artists[i] = new Artist(artist.get(i).uri, artist.get(i).name);
        }
        this.id = id;
        this.name = name;
        this.artist = artists;
        this.cover = cover;
        this.coverFull = coverFull;
        this.duration = duration;
        this.album = album;
    }

    public Track(String json) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.id = tempObject.getString(Constants.ID);
        this.name = tempObject.getString(Constants.NAME);
        this.duration = tempObject.getLong(Constants.DURATION);
        this.cover = tempObject.getString(Constants.COVER);
        this.album = tempObject.getString(Constants.ALBUM);
        this.coverFull = tempObject.getString(Constants.COVER_FULL);
        JSONArray array = tempObject.getJSONArray(Constants.ARTIST);
        Artist[] tempArtist = new Artist[array.length()];
        for (int i = 0; i < array.length(); i++) {
            tempArtist[i] = new Artist(
                    array.getJSONObject(i).getString(Constants.ID),
                    array.getJSONObject(i).getString(Constants.NAME)
            );
        }
        this.artist = tempArtist;

    }



    //Getter

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCover() {
        return cover;
    }

    public String getCoverFull() {
        return coverFull;
    }

    public Artist getArtist(int index) {
        return artist[index];
    }

    public long getDuration() {
        return duration;
    }

    public String getAlbum() {
        return album;
    }

    public String getURI() {
        return "spotify:track:" + id;
    }

    public String serialize() throws JSONException {
        Log.d(TAG, "serialize artist (" + name + ") from json");
        JSONObject tempObject = new JSONObject();
        JSONArray artistTemp = new JSONArray();
        for (Artist x : artist) {
            artistTemp = artistTemp.put(new JSONObject(x.serialize()));
        }
        tempObject = tempObject
                .put(Constants.NAME, name)
                .put(Constants.COVER, cover)
                .put(Constants.ID, id)
                .put(Constants.DURATION, duration)
                .put(Constants.ARTIST, artistTemp)
                .put(Constants.ALBUM, album)
                .put(Constants.COVER_FULL, coverFull);
        return tempObject.toString();
    }

    @NotNull
    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                ", cover='" + cover + '\'' +
                '}';
    }
}
