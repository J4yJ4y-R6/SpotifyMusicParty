package com.tinf19.musicparty.music;

import org.jetbrains.annotations.NotNull;

public class Playlist {

    public final String id;
    public String name;
    public String coverURL;

    public Playlist(String id, String name, String coverURL) {
        this.id = id;
        this.name = name;
        this.coverURL = coverURL;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

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
