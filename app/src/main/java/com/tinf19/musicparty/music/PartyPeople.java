package com.tinf19.musicparty.music;

public class PartyPeople {

    private String username;
    private long duration;

    public PartyPeople(String username, long duration) {
        this.username = username;
        this.duration = duration;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
