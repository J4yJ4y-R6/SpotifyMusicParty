package com.tinf19.musicparty.music;

import android.util.Log;

import com.tinf19.musicparty.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Track object to create a Song which is given by the Spotify-API.
 * Track is generated to work with a local object of the Spotify-
 * {@link com.spotify.protocol.types.Track}
 * <br>
 * The attribute artist can be an array or a String depending on how many people were working on
 * the song together. If there is only one artist, in the constructor will be generated a array so
 * the classes can work with it the same way.
 * <br>
 * The attribute cover is a String with the url to the picture of the cover with a small resolution.
 * The attrubute coverFull ist a String with the url to the picture of the cover with a high
 * resolution to display in {@link com.tinf19.musicparty.client.fragments.ClientSongFragment} and in
 * {@link com.tinf19.musicparty.server.fragments.HostSongFragment}.
 * <br>
 * The attribute id is used to generate the Track-URI which is used to play the song with the
 * spotify remote control.
 * @author Jannik Junker
 * @author Silas Wessely
 * @see com.spotify.protocol.types.Track
 * @see Artist
 * @since 1.1
 */
public class Track {

    private static final String TAG = Track.class.getName();
    /**
     * Array with all artist involved in the track
     */
    private final Artist [] artist;
    private final long duration;
    private final String id;
    private final String name;
    private final String cover;
    private final String coverFull;
    private final String album;

    /**
     * Constructor to set all track attributes (Track has at least two artists)
     * @param id Track-ID given by Spotify-API
     * @param name Track-Title given by Spotify-API
     * @param artist Array with all artists ({@link Artist}) involved in the track given by Spotify-API
     * @param cover Url of small Track-Cover given by Spotify-API
     * @param coverFull Url of big Track-Cover given by Spotify-API
     * @param duration Duration of the track given by Spotify-API
     * @param album Album-Name where the track was published on given by Spotify-API
     */
    public Track(String id, String name, Artist [] artist, String cover, String coverFull, long duration, String album) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.cover = cover;
        this.coverFull = coverFull;
        this.duration = duration;
        this.album = album;
    }

    /**
     * Constructor to set all track attributes (Track only has one artist)
     * @param id Track-ID given by Spotify-API
     * @param name Track-Title given by Spotify-API
     * @param artist Artist ({@link com.spotify.protocol.types.Artist}) of the track given by
     *               Spotify-API
     * @param cover Url of small Track-Cover given by Spotify-API
     * @param coverFull Url of big Track-Cover given by Spotify-API
     * @param duration Duration of the track given by Spotify-API
     * @param album Album-Name where the track was published on given by Spotify-API
     */
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

    /**
     * Constructor to set all track attributes (Track given as json format)
     * @param json JSON-String with all attributes of the Track
     */
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

    /**
     * @return Get Track-ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return Get Track-Title
     */
    public String getName() {
        return name;
    }

    /**
     * @return Get url of small Track-Cover
     */
    public String getCover() {
        return cover;
    }

    /**
     * @return Get url of big Track-Cover
     */
    public String getCoverFull() {
        return coverFull;
    }

    /**
     * @param index Index of artist in the Artist-Array
     * @return Get artist at an given index from Artist-Array of the track
     */
    public Artist getArtist(int index) {
        return artist[index];
    }

    /**
     * @return Get duration of the track
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return get album title where has the track was published on
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return Get Spotify-URI of the track
     */
    public String getURI() {
        return "spotify:track:" + id;
    }

    /**
     * Serializing the JSONObject
     * @return String with serialized jsonObject
     * @throws JSONException when the serialize failed
     */
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
