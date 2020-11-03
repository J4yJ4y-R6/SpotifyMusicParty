package com.tinf19.musicparty.music;

import com.tinf19.musicparty.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class Artist {
    private final String id;
    private final String name;

    public Artist(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String serialize() throws JSONException {
        JSONObject tempObject = new JSONObject();
        tempObject = tempObject
                .put(Constants.ID, id)
                .put(Constants.NAME, name);
        return tempObject.toString();
    }
}
