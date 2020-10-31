package com.example.musicparty.music;

import android.os.CpuUsageInfo;

import com.example.musicparty.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Track {

    private final String id;
    private final String name;
    private final Artist [] artist;
    private final long duration;
    private final String cover;

    public Track(String id, String name, Artist [] artist, String cover, long duration) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.cover = cover;
        this.duration = duration;
    }

    public Track(String id, String name, List<com.spotify.protocol.types.Artist> artist, String cover, long duration) {
        Artist [] artists = new Artist[artist.size()];
        for (int i = 0; i < artist.size(); i++) {
            artists[i] = new Artist(artist.get(i).uri, artist.get(i).name);
        }
        this.id = id;
        this.name = name;
        this.artist = artists;
        this.cover = cover;
        this.duration = duration;
    }

    public Track(String json) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.id = tempObject.getString(Constants.ID);
        this.name = tempObject.getString(Constants.NAME);
        this.duration = tempObject.getLong(Constants.DURATION);
        this.cover = tempObject.getString(Constants.COVER);
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCover() {
        return cover;
    }

    public Artist getArtist(int index) {
        return artist[index];
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                ", cover='" + cover + '\'' +
                '}';
    }

    public String getURI() {
        return "spotify:track:" + id;
    }

    public String serialize() throws JSONException {
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
                .put(Constants.ARTIST, artistTemp);
        return tempObject.toString();
    }
}
