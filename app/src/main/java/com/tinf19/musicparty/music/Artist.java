package com.tinf19.musicparty.music;

import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Artist object to create a Person with id and name which is given by the Spotify-API.
 * It is used to print out the information about an artist, who has created the track.
 * @author Jannik Junker
 * @author Silas Wessely
 * @see com.spotify.protocol.types.Artist
 * @since 1.1
 */
public class Artist {
    private static final String TAG = Artist.class.getName();
    private final String id;
    private final String name;

    /**
     * Constructor to set the artist attributes
     * @param id Artist-ID given by the Spotify-API
     * @param name Artist-Name given by the Spotify-API
     */
    public Artist(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return Get artist id
     */
    public String getId() {
        return id;
    }

    /**
     * @return get the artist name
     */
    public String getName() {
        return name;
    }

    /**
     * Serializing the JSONObject
     * @return String with serialized jsonObject
     * @throws JSONException when the serialize failed
     */
    public String serialize() throws JSONException {
        Log.d(TAG, "serialize artist (" + name + ") from json");
        JSONObject tempObject = new JSONObject();
        tempObject = tempObject
                .put(Constants.ID, id)
                .put(Constants.NAME, name);
        return tempObject.toString();
    }
}
