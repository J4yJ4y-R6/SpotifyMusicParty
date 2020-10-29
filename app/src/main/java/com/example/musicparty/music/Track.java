package com.example.musicparty.music;

public class Track {

    private final String id;
    private final String name;
    private final Artist [] artist;
    private final int duration;
    private final String cover;

    public Track(String uri, String name, Artist [] artist, String cover, int duration) {
        this.id = uri;
        this.name = name;
        this.artist = artist;
        this.cover = cover;
        this.duration = duration;
    }

    public Track(String id) {
        this.id = id;
        this.name = "Test";
        this.cover = "Test";
        this.duration = 10;
        this.artist = new Artist[0];
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
}
