package com.example.musicparty.music;

import android.os.CpuUsageInfo;

import com.example.musicparty.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Track {

    private final String id;
    private final String name;
    private final Artist [] artist;
    private final int duration;
    private final String cover;

    public Track(String id, String name, Artist [] artist, String cover, int duration) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.cover = cover;
        this.duration = duration;
    }

    public Track(String json) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.id = tempObject.getString(Constants.ID);
        this.name = tempObject.getString(Constants.NAME);
        this.duration = tempObject.getInt(Constants.NAME);
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

    public int getDuration() {
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
