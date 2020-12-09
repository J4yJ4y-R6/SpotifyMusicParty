package com.tinf19.musicparty.music;

import org.jetbrains.annotations.NotNull;

/**
 * Playlist object to set the type of a list which is used in the
 * {@link com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment} to set the
 * favorite playlists in the adapter. Also it is used to create a temporarily playlist object to
 * fill the playlist list with objects
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class Playlist {

    public final String id;
    public String name;
    public String coverURL;

    /**
     * Constructor to set the playlist attributes
     * @param id Playlist-ID given by the Spotify-API
     * @param name Playlist-Name given by the Spotify-API
     * @param coverURL Url of the playlist cover given by the Spotify-API
     */
    public Playlist(String id, String name, String coverURL) {
        this.id = id;
        this.name = name;
        this.coverURL = coverURL;
    }

    /**
     * @return Get playlist id
     */
    public String getId() {
        return id;
    }

    /**
     * @return Get playlist name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Get playlist cover url
     */
    public String getCoverURL() {
        return coverURL;
    }

    @NotNull
    @Override
    public String toString() {
        return "Playlist{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", coverURL='" + coverURL + '\'' +
                '}';
    }
}
